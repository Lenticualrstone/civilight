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
 * segment at a time. A run of segments goes from [start] to [finish] (or [drop]): `일시정지` closes
 * the microphone ([pause]) and [resume] opens it again into the next segment of the same run, so a
 * paused take never holds the microphone (U-13). [finish] appends the run's segments in order to the
 * record's take; `이어서 말하기` starts a new run that [finish] appends to the same take, so the
 * record keeps one file; `취소` during a continued take [drop]s only that run. Segments cut off by
 * process death are appended at start-up ([recoverSegment], LRC-09).
 */
interface AudioTakes {
    /** The microphone is open now (a run is recording and not paused). */
    val capturing: Boolean

    /** Starts a run for [recordKey] and opens the microphone; false when it could not be opened (the take goes on without audio). */
    fun start(recordKey: Key): Boolean

    /** Closes and releases the microphone; what the run recorded so far stays pending for [finish]. */
    fun pause()

    /** Opens the microphone again into the next segment of the paused run. */
    fun resume()

    /** The input level 0…1 now; also kept for the waveform envelope. Called by the hub's timer. */
    fun sample(): Float

    /** Ends the run (closing the microphone if open) and appends its segments to [recordKey]'s take; false when nothing was captured. */
    fun finish(recordKey: Key): Boolean

    /** Ends the run (closing the microphone if open) and drops its segments; the take keeps what it had. */
    fun drop(recordKey: Key)

    /** [recordKey]'s take so far, or null when nothing was captured. */
    fun take(recordKey: Key): TakeFile?

    /** Deletes the take, segments and envelope of [recordKey] (after its audio was kept, or when it is not kept). */
    fun discard(recordKey: Key)

    /** Segments left behind by process death are appended, in order, to [recordKey]'s take. */
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

    /** The single segment file of builds before U-13 (still appended first at recovery). */
    fun segment(dir: File, key: Key) = File(dir, "$key.part.$EXT")

    /** Segment [index] of [key]'s current run: one per stretch between pauses. */
    fun part(dir: File, key: Key, index: Int) = File(dir, "$key.part.$index.$EXT")

    /** [key]'s pending segment files in recording order (an old single segment first, then by index). */
    fun parts(dir: File, key: Key): List<File> {
        val prefix = "$key.part."
        val numbered = dir.listFiles().orEmpty()
            .mapNotNull { f ->
                if (!f.name.startsWith(prefix) || !f.name.endsWith(".$EXT")) return@mapNotNull null
                f.name.removePrefix(prefix).removeSuffix(".$EXT").toIntOrNull()?.let { it to f }
            }
            .sortedBy { it.first }
            .map { it.second }
        return listOfNotNull(segment(dir, key).takeIf { it.isFile }) + numbered
    }

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
 * One stretch of microphone capture into one file: opened by [start], closed and released by [stop].
 * The seam between [MediaRecorderTakes] and the platform recorder (JVM tests use a fake).
 */
interface MicSegment {
    /** Opens the microphone and records into [path]; false when it could not be opened (then nothing stays open). */
    fun start(path: String): Boolean

    /** The peak input amplitude (0…32767) since the last call. */
    fun maxAmplitude(): Int

    /** Stops and releases the microphone; true when the file holds a usable segment. Called once, after a true [start]. */
    fun stop(): Boolean
}

/** Makes the [MicSegment] of each stretch (one per start or resume). */
fun interface MicSegments {
    fun next(): MicSegment
}

/** [MicSegment] over the platform `MediaRecorder` (AAC in ADTS, mono, 16 kHz, 32 kbps). */
class PlatformMicSegment(private val newRecorder: () -> MediaRecorder) : MicSegment {
    private var recorder: MediaRecorder? = null

    override fun start(path: String): Boolean {
        val r = runCatching(newRecorder).getOrNull() ?: return false
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(1)
            r.setAudioSamplingRate(SAMPLE_RATE)
            r.setAudioEncodingBitRate(BIT_RATE)
            r.setOutputFile(path)
            r.prepare()
            r.start()
            recorder = r
            true
        } catch (e: Exception) {
            runCatching { r.release() }
            false
        }
    }

    override fun maxAmplitude(): Int = recorder?.let { r -> runCatching { r.maxAmplitude }.getOrDefault(0) } ?: 0

    override fun stop(): Boolean {
        val r = recorder ?: return false
        recorder = null
        val ok = runCatching { r.stop() }.isSuccess
        runCatching { r.release() }
        return ok
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val BIT_RATE = 32_000
    }
}

/**
 * [AudioTakes] over [MicSegment]s, by default the platform `MediaRecorder`. `MediaRecorder.pause()`
 * keeps the microphone open (the system's mic indicator stays on), so a pause here stops and
 * releases the recorder and a resume opens a new one into the next segment file; ADTS frames stand
 * alone, so the segments join byte-wise into one take with no gap. The recording permission is asked
 * by the `Recording` route before any start; when the microphone cannot be opened the take goes on
 * without audio. Not run on a device yet.
 */
class MediaRecorderTakes(
    private val dir: File,
    private val newSegment: MicSegments,
) : AudioTakes {
    constructor(
        dir: File,
        @Suppress("DEPRECATION") newRecorder: () -> MediaRecorder = { MediaRecorder() },
    ) : this(dir, newSegment = { PlatformMicSegment(newRecorder) })

    /** The open microphone; null while paused or between runs. */
    private var mic: MicSegment? = null

    /** The record whose run is pending (from [start] to [finish] or [drop]), paused or not. */
    private var runKey: Key? = null
    private var nextPart = 0
    private val samples = java.io.ByteArrayOutputStream()

    override val capturing: Boolean get() = mic != null

    override fun start(recordKey: Key): Boolean {
        runKey?.let(::drop)
        dir.mkdirs()
        TakeFiles.parts(dir, recordKey).forEach { it.delete() }
        samples.reset()
        if (!openPart(recordKey, 0)) return false
        runKey = recordKey
        nextPart = 1
        return true
    }

    /** Opens the microphone into segment [index] of [key]; false (and no file) when it cannot be opened. */
    private fun openPart(key: Key, index: Int): Boolean {
        val file = TakeFiles.part(dir, key, index)
        file.delete()
        val segment = runCatching { newSegment.next() }.getOrNull()
        if (segment == null || !runCatching { segment.start(file.path) }.getOrDefault(false)) {
            file.delete()
            return false
        }
        mic = segment
        return true
    }

    /** Closes the open microphone once; an unusable last segment is deleted. */
    private fun closeMic() {
        val segment = mic ?: return
        mic = null
        val ok = runCatching { segment.stop() }.getOrDefault(false)
        if (!ok) runKey?.let { TakeFiles.part(dir, it, nextPart - 1).delete() }
    }

    override fun pause() = closeMic()

    override fun resume() {
        val key = runKey ?: return
        if (mic != null) return
        if (openPart(key, nextPart)) nextPart++
    }

    override fun sample(): Float {
        val segment = mic ?: return 0f
        val amplitude = runCatching { segment.maxAmplitude() }.getOrDefault(0)
        // Square root: speech sits low on a linear amplitude scale.
        val level = sqrt((amplitude / 32767f).coerceIn(0f, 1f))
        samples.write(Waveforms.byteOf(level).toInt())
        return level
    }

    override fun finish(recordKey: Key): Boolean {
        if (runKey != recordKey) return false
        closeMic()
        runKey = null
        nextPart = 0
        val take = TakeFiles.take(dir, recordKey)
        var appended = false
        for (part in TakeFiles.parts(dir, recordKey)) if (TakeFiles.appendAndDelete(part, take)) appended = true
        if (appended) TakeFiles.envelope(dir, recordKey).appendBytes(samples.toByteArray())
        samples.reset()
        return appended
    }

    override fun drop(recordKey: Key) {
        if (runKey == recordKey) {
            closeMic()
            runKey = null
            nextPart = 0
            samples.reset()
        }
        TakeFiles.parts(dir, recordKey).forEach { it.delete() }
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
        if (runKey == recordKey) return
        val take = TakeFiles.take(dir, recordKey)
        for (part in TakeFiles.parts(dir, recordKey)) TakeFiles.appendAndDelete(part, take)
    }

    override fun prune(keep: Set<Key>) {
        val files = dir.listFiles() ?: return
        for (f in files) if (TakeFiles.keyOf(f) !in keep && TakeFiles.keyOf(f) != runKey) f.delete()
    }
}
