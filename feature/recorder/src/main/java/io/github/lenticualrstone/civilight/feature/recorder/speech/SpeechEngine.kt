package io.github.lenticualrstone.civilight.feature.recorder.speech

import io.github.lenticualrstone.civilight.model.recorder.RecorderTranscription
import io.github.lenticualrstone.civilight.model.recorder.TranscriptionMode

/** What a running [SpeechEngine] reports. All callbacks arrive on the main thread. */
interface SpeechListener {
    /** The unconfirmed tail changed (drawn `ink3`). */
    fun onPartial(text: String)

    /** A segment was confirmed (drawn `ink`); the tail clears. */
    fun onFinal(text: String)

    /** Input level 0…1 (smoothed RMS of about the last 100 ms) for the glow bands (LRC-10). */
    fun onLevel(level: Float)

    /** After [SpeechEngine.stop]: the last final result (if any) was delivered. */
    fun onFinished()
}

/**
 * Speech to text for `녹음 중` (LRC-07, LRC-08, LRC-11, LRC-12). An engine never ends a session on
 * silence by itself (LRC-08): it keeps listening (segmented sessions) until [stop].
 */
interface SpeechEngine {
    /** False when no engine can transcribe here: the recording still runs and saves with empty text (LRC-11). */
    val available: Boolean

    fun start(listener: SpeechListener)

    /**
     * `일시정지`: stops listening and releases any audio input the engine opened itself (a platform
     * recognizer is cancelled, not only muted), so a paused take holds no microphone (U-13).
     */
    fun pause()

    /** Listens again into the same transcript after [pause]. */
    fun resume()

    /** Ends listening; the remaining text arrives as one final result, then [SpeechListener.onFinished]. */
    fun stop()
}

/**
 * **needsExternal stub** (LRC-11, LRC-12; engine and provider undecided, spec §6 Q1). No on-device
 * or cloud recognizer is bound yet, so this engine reports itself unavailable: the recording
 * screen runs the timer and shows the no-engine line (`TODO_COPY_LRC_05`), and `멈추고 저장`
 * saves a voice record with empty text that can be typed later (LRC-28). Replace it with a real
 * engine through [SpeechEngines.factory]; nothing else changes.
 */
class UnavailableSpeechEngine : SpeechEngine {
    private var listener: SpeechListener? = null

    override val available: Boolean = false

    override fun start(listener: SpeechListener) {
        this.listener = listener
    }

    override fun pause() = Unit

    override fun resume() = Unit

    override fun stop() {
        listener?.onFinished()
        listener = null
    }
}

/**
 * **needsExternal stub** (LRC-12; the cloud provider is undecided, spec §6 Q1). `클라우드` in
 * `설정 · 라이프 레코더 › 받아쓰기` would send the audio to the provider; none is bound, so this engine
 * reports itself unavailable and [EngineChoice] falls back to the on-device engine for the take.
 */
class CloudSpeechEngine : SpeechEngine by UnavailableSpeechEngine()

/** The engines the app uses, per transcription mode. A later batch (or a test) swaps [factory]. */
object SpeechEngines {
    @Volatile
    var factory: (TranscriptionMode) -> SpeechEngine = { mode ->
        when (mode) {
            TranscriptionMode.CLOUD -> CloudSpeechEngine()
            TranscriptionMode.ON_DEVICE -> UnavailableSpeechEngine()
        }
    }
}

/** The engine one take runs and the mode it stands for (stored as the record's `transcribedBy`). */
class ChosenEngine(val mode: TranscriptionMode, val engine: SpeechEngine)

/**
 * Which engine a take uses (LRC-11, LRC-12): `클라우드` only when it is chosen in the settings, the
 * device is online and a cloud engine is there; otherwise on-device for that take (no network at
 * recording time → on-device, and nothing is uploaded later, `[PROPOSED]`).
 */
object EngineChoice {
    fun mode(setting: RecorderTranscription, online: Boolean): TranscriptionMode =
        if (setting == RecorderTranscription.CLOUD && online) TranscriptionMode.CLOUD else TranscriptionMode.ON_DEVICE

    fun choose(setting: RecorderTranscription, online: Boolean, factory: (TranscriptionMode) -> SpeechEngine): ChosenEngine {
        if (mode(setting, online) == TranscriptionMode.CLOUD) {
            val cloud = factory(TranscriptionMode.CLOUD)
            if (cloud.available) return ChosenEngine(TranscriptionMode.CLOUD, cloud)
        }
        return ChosenEngine(TranscriptionMode.ON_DEVICE, factory(TranscriptionMode.ON_DEVICE))
    }
}
