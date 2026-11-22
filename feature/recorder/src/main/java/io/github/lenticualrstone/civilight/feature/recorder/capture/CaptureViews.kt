package io.github.lenticualrstone.civilight.feature.recorder.capture

import io.github.lenticualrstone.civilight.designsystem.component.GlowMode
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.capture.MemoSheetView
import io.github.lenticualrstone.civilight.domain.recorder.capture.ProcessingScreen
import io.github.lenticualrstone.civilight.domain.recorder.capture.Recording
import io.github.lenticualrstone.civilight.domain.recorder.capture.RecordingScreen
import io.github.lenticualrstone.civilight.domain.recorder.capture.TextMemo
import io.github.lenticualrstone.civilight.domain.recorder.picker.LinkCandidate
import io.github.lenticualrstone.civilight.domain.recorder.picker.LinkPicker
import io.github.lenticualrstone.civilight.domain.recorder.picker.PickerMode
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.feature.recorder.memo.AtMarks
import io.github.lenticualrstone.civilight.model.recorder.RecordingPhase

/**
 * `녹음 중` as drawn: the domain screen plus the no-engine line (LRC-11, `TODO_COPY_LRC_05`).
 * [canToText] is false on the lock screen (no `글로 쓰기` there, LRC-05 `[PROPOSED]`).
 */
data class RecordingView(val screen: RecordingScreen, val engineNote: String?, val paused: Boolean, val canToText: Boolean = true)

/** `정리 중` as drawn; `여기 넣기` only where it can run. */
data class ProcessingView(val screen: ProcessingScreen)

/** The text memo sheet as drawn: the domain view, the `@` candidates of the open query and the `@` marks of the field. */
data class MemoView(val sheet: MemoSheetView, val atCandidates: List<LinkCandidate>, val atOpen: Boolean, val marks: AtMarks)

/**
 * The voice glow (LRC-10): none when `녹음 중 빛 띠` is off; bands following the level while
 * recording, still while paused or with reduced motion (`빛 띠 정지`), the sweep while `정리 중`
 * finalises, and none once it has finished.
 */
object GlowModes {
    fun of(state: CaptureState, glowOn: Boolean, reducedMotion: Boolean): GlowMode? = when {
        !glowOn -> null
        state.stage == CaptureStage.RECORDING ->
            if (reducedMotion || state.session?.phase != RecordingPhase.RECORDING) GlowMode.STILL else GlowMode.RECORDING
        state.stage == CaptureStage.PROCESSING && state.finalising -> if (reducedMotion) GlowMode.STILL else GlowMode.PROCESSING
        else -> null
    }
}

/** What each capture surface shows, from [CaptureState] only (no Android, JVM-tested). */
object CaptureViews {
    /** Candidates listed under the memo field while an `@` query is open. */
    const val AT_MAX = 6

    fun recording(state: CaptureState): RecordingView? {
        val session = state.session ?: return null
        if (state.stage != CaptureStage.RECORDING) return null
        return RecordingView(
            screen = Recording.recordingScreen(session, state.env),
            engineNote = if (state.engineUnavailable) RecorderCopy.TODO_NO_ENGINE else null,
            paused = session.phase == RecordingPhase.PAUSED,
            canToText = !session.entry.onLockScreen,
        )
    }

    /**
     * `여기 넣기 · #044 뒤` stays only when the insert can run: the project is there, writable
     * (not the read-only example) and not in the trash, the text is not blank, and the last
     * result is in (LRC-33 `[PROPOSED]` empty text cannot be inserted).
     */
    fun processing(state: CaptureState): ProcessingView? {
        val session = state.session ?: return null
        if (state.stage != CaptureStage.PROCESSING) return null
        val screen = Recording.processingScreen(session, state.env, state.finalising, state.audioKept)
        val project = session.entry.projectKey?.let(state.env::project)
        val canPut = project != null && !project.readOnly && !project.trashed && !state.finalising &&
            Runs.text(session.confirmedRuns).isNotBlank()
        return ProcessingView(screen.copy(putHere = screen.putHere.takeIf { canPut }))
    }

    fun memo(state: CaptureState): MemoView? {
        val memo = state.memo ?: return null
        val query = TextMemo.atQuery(memo, state.memoCaret)
        val candidates = query?.let {
            LinkPicker.search(state.env, it.query, memo.draft.pendingLinks, PickerMode.AT).take(AT_MAX)
        }.orEmpty()
        return MemoView(TextMemo.view(memo, state.env), candidates, query != null, AtMarks.of(memo.draft.runs))
    }
}
