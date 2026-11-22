package io.github.lenticualrstone.civilight.feature.recorder.audio

import android.media.MediaPlayer
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.recorder.RecorderAudio
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** The platform side of playback: one file at a time. */
interface Playback {
    /** Plays [path] from [fromMs]; [onComplete] runs at its end. False when it cannot be played. */
    fun start(path: String, fromMs: Long, onComplete: () -> Unit): Boolean

    /** Pauses and returns the position. */
    fun pause(): Long

    fun position(): Long

    fun stop()
}

/**
 * Playback of kept audio (LRC-14): tap play ↔ pause; only one record plays at a time; a recording
 * starting or hidden records locking again stops it. One per process (in `RecorderServices`), so a
 * row and the record screen show the same state.
 */
class RecordPlayer(
    private val scope: CoroutineScope,
    private val playback: Playback,
    private val fileOf: (hash: String) -> String?,
) {
    /** What plays: [recordKey] (null = nothing), whether it runs, its position and length. */
    data class State(val recordKey: Key? = null, val playing: Boolean = false, val positionMs: Long = 0, val durationMs: Long = 0) {
        fun playing(key: Key) = playing && recordKey == key

        fun positionOf(key: Key): Long = if (recordKey == key) positionMs else 0
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private var ticker: Job? = null

    /** The play button of [recordKey]: pause while it plays, else play (resuming a paused one where it was). */
    fun toggle(recordKey: Key, audio: RecorderAudio) {
        val now = _state.value
        if (now.recordKey == recordKey && now.playing) {
            val at = playback.pause()
            stopTicker()
            _state.value = now.copy(playing = false, positionMs = at)
            return
        }
        val from = if (now.recordKey == recordKey) now.positionMs else 0L
        if (now.recordKey != null && now.recordKey != recordKey) playback.stop()
        val path = fileOf(audio.hash)
        if (path == null) {
            _state.value = State()
            return
        }
        val started = playback.start(path, from) { finished(recordKey) }
        _state.value = if (started) State(recordKey, true, from, audio.durationMs) else State()
        if (started) startTicker()
    }

    /** Stops whatever plays. */
    fun stop() {
        if (_state.value.recordKey == null) return
        playback.stop()
        stopTicker()
        _state.value = State()
    }

    /** Stops when the record that plays (or is paused) no longer qualifies ([keep] false): deleted or gone ([playable]). */
    fun stopUnless(keep: (Key) -> Boolean) {
        val key = _state.value.recordKey ?: return
        if (!keep(key)) stop()
    }

    /** Follows the library: stops a record that went to the trash or no longer exists (LRC-43). */
    fun follow(library: RecorderLibrary) = stopUnless { playable(library, it) }

    private fun finished(recordKey: Key) {
        if (_state.value.recordKey != recordKey) return
        stopTicker()
        _state.value = State(recordKey, false, 0, _state.value.durationMs)
    }

    private fun startTicker() {
        stopTicker()
        ticker = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                val s = _state.value
                if (!s.playing) break
                _state.value = s.copy(positionMs = playback.position())
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    companion object {
        private const val TICK_MS = 100L

        /** A record can keep playing while it exists and is not in the trash. */
        fun playable(library: RecorderLibrary, recordKey: Key): Boolean = library.record(recordKey)?.let { it.deletedAt == null } == true
    }
}

/** [Playback] over the platform `MediaPlayer`. Not run on a device yet. */
class MediaPlayback : Playback {
    private var player: MediaPlayer? = null

    override fun start(path: String, fromMs: Long, onComplete: () -> Unit): Boolean {
        stop()
        val p = MediaPlayer()
        return try {
            p.setDataSource(path)
            p.prepare()
            if (fromMs > 0) p.seekTo(fromMs.toInt())
            p.setOnCompletionListener {
                stop()
                onComplete()
            }
            p.start()
            player = p
            true
        } catch (e: Exception) {
            runCatching { p.release() }
            false
        }
    }

    override fun pause(): Long {
        val p = player ?: return 0
        runCatching { p.pause() }
        return runCatching { p.currentPosition.toLong() }.getOrDefault(0)
    }

    override fun position(): Long = player?.let { runCatching { it.currentPosition.toLong() }.getOrNull() } ?: 0

    override fun stop() {
        val p = player ?: return
        player = null
        runCatching { p.stop() }
        runCatching { p.release() }
    }
}
