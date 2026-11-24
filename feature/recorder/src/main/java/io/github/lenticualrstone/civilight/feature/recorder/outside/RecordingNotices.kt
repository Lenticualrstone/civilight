package io.github.lenticualrstone.civilight.feature.recorder.outside

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.ShownCopy
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureStage
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureState
import io.github.lenticualrstone.civilight.model.recorder.RecordingPhase

/**
 * The ongoing notification while a recording runs (LRC-09). It never carries the transcript or a
 * link label: a link may point into a hidden or R-18 project, and notifications never show hidden
 * names (rule 19 §3). Its text is `TODO_COPY_LRC_04` until copy exists.
 */
data class RecordingNotice(val title: String, val text: String, val channelName: String)

object RecordingNotices {
    /** Kept alive while a take is recording or paused; `정리 중` and the memo need no microphone. */
    fun keepsAlive(state: CaptureState): Boolean = state.stage == CaptureStage.RECORDING && state.session != null

    fun of(state: CaptureState): RecordingNotice? =
        if (keepsAlive(state)) RecordingNotice(RecorderCopy.APP_NAME, text(state.session?.phase), RecorderCopy.APP_NAME) else null

    /** (0.2.0) `녹음 중` while the microphone is open; a paused take reads `일시정지` (the microphone is closed). */
    fun text(phase: RecordingPhase?): String =
        if (phase == RecordingPhase.PAUSED) RecorderCopy.PAUSED else ShownCopy.RECORDING
}
