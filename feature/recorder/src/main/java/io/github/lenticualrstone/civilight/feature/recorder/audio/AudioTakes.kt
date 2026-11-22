package io.github.lenticualrstone.civilight.feature.recorder.audio

import android.media.MediaRecorder
import io.github.lenticualrstone.civilight.model.Key
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sqrt

/** A record's take so far: the audio file, its media type and size, and its loudness envelope. */
class TakeFile(val path: String, val mime: String, val bytes: Long, val envelope: ByteArray)

/**
 * The microphone side of a recording (LRC-14): one growing audio file per record, written one
 * segment at a time. `이어서 말하기` starts a new segment that [finish] appends to the same take, so
 * the record keeps one file; `취소` during a continued take [drop]s only the new segment. A segment
 * cut off by process death is appended at start-up ([recoverSegment], LRC-09).
 */
interface AudioTakes {
    /** A segment is being recorded now. */
    val capturing: Boolean

    /** Starts a segment for [recordKey]; false when the microphone could not be opened (the take goes on without audio). */
    fun start(recordKey: Key): Boolean

    fun pause()

    fun resume()

    /** The input level 0…1 now; also kept for the waveform envelope. Called by the hub's timer. */
    fun sample(): Float

    /** Ends the running segment and appends it to [recordKey]'s take; false when nothing was captured. */
    fun finish(recordKey: Key): Boolean

    /** Ends the running segment and drops it; the take keeps what it had. */
    fun drop(recordKey: Key)

    /** [recordKey]'s take so far, or null when nothing was captured. */
    fun take(recordKey: Key): TakeFile?

    /** Deletes the take, segment and envelope of [recordKey] (after its audio was kept, or when it is not kept). */
    fun discard(recordKey: Key)

    /** A segment left behind by process death is appended to [recordKey]'s take. */
    fun recoverSegment(recordKey: Key)

    /** Start-up: every take except those of [keep] is deleted (cancelled takes nobody brought back). */
    fun prune(keep: Set<Key>)
}

/** No microphone capture (tests, or before the platform takes are installed). */
object NoAudioTakes : AudioTakes {
    override val capturing: Boolean = false
    override fun start(recordKey: Key): Boolean = false
    override fun pause() = Unit
    override fun resume() = Unit
    override fun sample(): Float = 0f
    override fun finish(recordKey: Key): Boolean = false
    override fun drop(recordKey: Key) = Unit
    override fun take(recordKey: Key): TakeFile? = null
    override fun discard(recordKey: Key) = Unit
    override fun recoverSegment(recordKey: Key) = Unit
    override fun prune(keep: Set<Key>) = Unit
}

/** File layout of the takes folder (`files/recorder/takes`). */
object TakeFiles {
    /** ADTS AAC: frames stand alone, so segments append byte-wise and a cut-off file still plays up to its last frame. */
    const val MIME = "audio/aac"
    const val EXT = "aac"

    fun take(dir: File, key: Key) = File(dir, "$key.$EXT")

    fun segment(dir: File, key: Key) = File(dir, "$key.part.$EXT")

    fun envelope(dir: File, key: Key) = File(dir, "$key.env")

    /** Appends [from] to [to] and deletes [from]; false when [from] holds nothing. */
    fun appendAndDelete(from: File, to: File): Boolean {
        if (!from.isFile || from.length() == 0L) {
            from.delete()
            return false
        }
        FileOutputStream(to, true).use { out -> from.inputStream().use { it.copyTo(out) } }
        from.delete()
        return true
    }

    /** The record key a takes-folder file belongs to. */
    fun keyOf(file: File): String = file.name.substringBefore('.')
}

/**
 * [AudioTakes] over the platform `MediaRecorder` (AAC in ADTS, mono, 16 kHz, 32 kbps). The
 * recording permission is asked by the `Recording` route before any start; when the microphone
 * cannot be opened the take goes on without audio. Not run on a device yet.
 */
class MediaRecorderTakes(
    private val dir: File,
    @Suppress("DEPRECATION") private val newRecorder: () -> MediaRecorder = { MediaRecorder() },
) : AudioTakes {
    private var recorder: MediaRecorder? = null
    private var segmentKey: Key? = null
    private val samples = java.io.ByteArrayOutputStream()

    override val capturing: Boolean get() = recorder != null

    override fun start(recordKey: Key): Boolean {
        segmentKey?.let(::drop)
        dir.mkdirs()
        val part = TakeFiles.segment(dir, recordKey)
        part.delete()
        val r = newRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(1)
            r.setAudioSamplingRate(SAMPLE_RATE)
            r.setAudioEncodingBitRate(BIT_RATE)
            r.setOutputFile(part.path)
            r.prepare()
            r.start()
            recorder = r
            segmentKey = recordKey
            samples.reset()
            true
        } catch (e: Exception) {
            runCatching { r.release() }
            part.delete()
            false
        }
    }

    override fun pause() {
        runCatching { recorder?.pause() }
    }

    override fun resume() {
        runCatching { recorder?.resume() }
    }

    override fun sample(): Float {
        val r = recorder ?: return 0f
        val amplitude = runCatching { r.maxAmplitude }.getOrDefault(0)
        // Square root: speech sits low on a linear amplitude scale.
        val level = sqrt((amplitude / 32767f).coerceIn(0f, 1f))
        samples.write(Waveforms.byteOf(level).toInt())
        return level
    }

    /** Stops the platform recorder; true when it wrote a usable segment. */
    private fun stopRecorder(): Boolean {
        val r = recorder ?: return false
        recorder = null
        segmentKey = null
        val ok = runCatching { r.stop() }.isSuccess
        runCatching { r.release() }
        return ok
    }

    override fun finish(recordKey: Key): Boolean {
        if (segmentKey != recordKey) return false
        val ok = stopRecorder()
        val part = TakeFiles.segment(dir, recordKey)
        if (!ok) {
            part.delete()
            samples.reset()
            return false
        }
        val appended = TakeFiles.appendAndDelete(part, TakeFiles.take(dir, recordKey))
        if (appended) TakeFiles.envelope(dir, recordKey).appendBytes(samples.toByteArray())
        samples.reset()
        return appended
    }

    override fun drop(recordKey: Key) {
        if (segmentKey == recordKey) stopRecorder()
        TakeFiles.segment(dir, recordKey).delete()
        samples.reset()
    }

    override fun take(recordKey: Key): TakeFile? {
        val file = TakeFiles.take(dir, recordKey)
        if (!file.isFile || file.length() == 0L) return null
        val env = TakeFiles.envelope(dir, recordKey).takeIf { it.isFile }?.readBytes() ?: ByteArray(0)
        return TakeFile(file.path, TakeFiles.MIME, file.length(), Waveforms.envelopeOf(env))
    }

    override fun discard(recordKey: Key) {
        drop(recordKey)
        TakeFiles.take(dir, recordKey).delete()
        TakeFiles.envelope(dir, recordKey).delete()
    }

    override fun recoverSegment(recordKey: Key) {
        if (segmentKey == recordKey) return
        TakeFiles.appendAndDelete(TakeFiles.segment(dir, recordKey), TakeFiles.take(dir, recordKey))
    }

    override fun prune(keep: Set<Key>) {
        val files = dir.listFiles() ?: return
        for (f in files) if (TakeFiles.keyOf(f) !in keep && TakeFiles.keyOf(f) != segmentKey) f.delete()
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val BIT_RATE = 32_000
    }
}
