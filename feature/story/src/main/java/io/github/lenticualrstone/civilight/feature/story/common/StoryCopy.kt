package io.github.lenticualrstone.civilight.feature.story.common

/**
 * Copy shared by the story screens (20-story-time, boards `Home`, `Story`, `RSHeadMenu`,
 * `UndoToast`). Every value is UI copy and is checked by `StoryCopyLintTest`.
 */
object StoryCopy {
    const val READ_ONLY = "읽기 전용"
    const val UNTITLED = "미정"
    const val BACK = "뒤로"
    const val UNDO = "되돌리기"

    /** `씬 3 지움` (toast subject and undo label, R12 §2). */
    const val DELETED = "지움"

    /** Small tag on an unlocked hidden scene's row (R19 §6, STO-11). */
    const val HIDDEN_TAG = "숨김"

    /** `완료` (STO-06 status word). */
    const val DONE = "완료"

    /**
     * ⋯ rows no board draws (20-story-time §6 Q34), worded by the orchestrator on 2026-11-23: a new
     * episode / chapter (STO-05) and the author-set `완료` flag on and off (STO-06).
     */
    const val NEW_EPISODE = "새 에피소드"
    const val NEW_CHAPTER = "새 장"
    const val MARK_DONE = "완료 표시"
    const val UNMARK_DONE = "완료 표시 빼기"

    /** `씬 3 지움`. */
    fun deleted(unitLabel: String): String = "$unitLabel $DELETED"
}

/** Prefix of copy constants that are not drawn yet (DECISIONS §3). Rows labelled with one are left out of the UI. */
const val TODO_COPY_PREFIX = "TODO_COPY_"

/** True for a label the UI may show (not an undrawn `TODO_COPY_…` constant). */
fun isDrawnCopy(label: String): Boolean = !label.startsWith(TODO_COPY_PREFIX)
