package io.github.lenticualrstone.civilight.feature.recorder.capture

import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.capture.CancelledRecording
import io.github.lenticualrstone.civilight.domain.recorder.capture.MemoSheet
import io.github.lenticualrstone.civilight.domain.recorder.put.Insertion
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.recorder.EntryContext
import io.github.lenticualrstone.civilight.model.recorder.RecordingSession

/**
 * Where a capture is (one at a time in the app):
 * - [IDLE]: nothing open
 * - [MIC_REASON]: the reason line before the system prompt (LRC-06)
 * - [MIC_OFF]: `마이크 꺼짐` (LRC-06)
 * - [RECORDING]: `녹음 중`, running or paused (LRC-07)
 * - [PROCESSING]: `정리 중` (LRC-13); the record exists from here on
 * - [TYPING]: the text memo sheet (LRC-15)
 */
enum class CaptureStage { IDLE, MIC_REASON, MIC_OFF, RECORDING, PROCESSING, TYPING }

/** A device unlock the capture screen must ask for before going on (LRC-05). */
sealed interface UnlockRequest {
    /** `잠금 화면에서 녹음` is off: unlock, then record. */
    data class Start(val entry: EntryContext) : UnlockRequest

    /** The link picker on the lock screen lists names: unlock, then open it. */
    data class Picker(val picker: PickerRequest) : UnlockRequest
}

/** Which link list the open link picker edits. */
enum class PickerTarget { SESSION, MEMO }

/** The link picker (LRC-20): `+ 연결` adds ([replaceLinkKeys] empty); a chip tap replaces that chip's links. */
data class PickerRequest(val target: PickerTarget, val replaceLinkKeys: List<Key> = emptyList())

/** Everything the capture screens draw. Domain types hold the rules' state (session, memo sheet). */
data class CaptureState(
    val stage: CaptureStage = CaptureStage.IDLE,
    val entry: EntryContext? = null,
    val session: RecordingSession? = null,
    /** `받아쓰는 중 · 문장 나누기` and the glow sweep, until the engine delivered its last result. */
    val finalising: Boolean = false,
    /** No speech engine here (LRC-11): the recording runs and saves with empty text. */
    val engineUnavailable: Boolean = false,
    /** The record of `정리 중` keeps its audio (LRC-14): picks the note under the chips. */
    val audioKept: Boolean = false,
    val memo: MemoSheet? = null,
    val memoCaret: Int = 0,
    /** Bumped when the memo text changed other than by typing (an `@` pick): the field reloads from the runs. */
    val memoReload: Int = 0,
    val picker: PickerRequest? = null,
    /** The memo turns into a recording once the mic permission is granted (LRC-16). */
    val voiceFromMemo: Boolean = false,
    val env: RecorderEnv = RecorderEnv(emptyList()),
    /** A device unlock to ask for now (lock-screen capture, LRC-05). */
    val unlock: UnlockRequest? = null,
    /** The device was unlocked during this capture: the link picker no longer asks. */
    val deviceUnlocked: Boolean = false,
) {
    /** A capture is open, or waits for the device unlock before starting. */
    val active: Boolean get() = stage != CaptureStage.IDLE || unlock is UnlockRequest.Start
}

/** What `취소` did (LRC-07). */
sealed interface CancelOutcome {
    /** A first take: nothing saved; the toast's `되돌리기` brings it back into `정리 중`. */
    data class Discarded(val cancelled: CancelledRecording) : CancelOutcome

    /** A continued take: only the new take went; `정리 중` again. */
    data object BackToProcessing : CancelOutcome

    data object Nothing : CancelOutcome
}

/** `여기 넣기` (LRC-34) done: the project key and what to undo. */
data class PutHereOutcome(val projectKey: Key, val insertion: Insertion)
