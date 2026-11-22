package io.github.lenticualrstone.civilight.feature.recorder.speech

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

    fun pause()

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

/** The engine the app uses. A later batch (or a test) swaps [factory]. */
object SpeechEngines {
    @Volatile
    var factory: () -> SpeechEngine = { UnavailableSpeechEngine() }
}
