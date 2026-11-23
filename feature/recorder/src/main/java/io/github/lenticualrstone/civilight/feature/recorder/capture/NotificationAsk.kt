package io.github.lenticualrstone.civilight.feature.recorder.capture

/**
 * (0.2.0) U-13 · D32 · LRC-09: when the recording screen asks for `POST_NOTIFICATIONS`, so the ongoing
 * recording notification (the way back to the recorder while a take runs in the background) is shown on
 * Android 13 and later. Asked once per app run, when a take first reaches `녹음 중`; never before the
 * microphone is granted, never over the mic reason line or a tour card. A refusal changes nothing: the take still records.
 */
object NotificationAsk {
    /** Android 13 (API 33), where posting notifications became a runtime permission. */
    const val FIRST_SDK = 33

    /** Whether to show the system request now. */
    fun shouldAsk(sdkInt: Int, granted: Boolean, stage: CaptureStage, askedThisRun: Boolean, touring: Boolean = false): Boolean =
        sdkInt >= FIRST_SDK && !granted && !askedThisRun && !touring && stage == CaptureStage.RECORDING

    /** Set once the request was shown in this process, so later takes do not ask again. */
    @Volatile
    var askedThisRun: Boolean = false
}
