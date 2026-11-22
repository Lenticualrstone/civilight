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

    /** `완료` (STO-06 status word). */
    const val DONE = "완료"

    // Not drawn (20-story-time §6 Q34): raised under Requests in docs/status/feature-story.md.
    const val TODO_COPY_STO_NEW_EPISODE = "TODO_COPY_STO_NEW_EPISODE"
    const val TODO_COPY_STO_NEW_CHAPTER = "TODO_COPY_STO_NEW_CHAPTER"
    const val TODO_COPY_STO_MARK_DONE = "TODO_COPY_STO_MARK_DONE"
    const val TODO_COPY_STO_UNMARK_DONE = "TODO_COPY_STO_UNMARK_DONE"

    /** `씬 3 지움`. */
    fun deleted(unitLabel: String): String = "$unitLabel $DELETED"
}
