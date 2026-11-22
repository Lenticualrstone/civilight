package io.github.lenticualrstone.civilight.feature.library.trash

/**
 * Copy of the 휴지통 screen (LIB-38, LIB-39; board `Trash`). Row titles, sub lines, chips and the
 * restore toasts come from `domain:trash` (`TrashListing`, `TrashCopy`, `Messages`).
 */
object TrashScreenCopy {
    const val SMALL = "30일 뒤 자동 삭제"
    const val TITLE = "휴지통"
    const val EMPTY_TRASH = "비우기"
    const val RESTORE = "되살리기"
    const val PURGE = "영구 삭제"
    const val CANCEL = "취소"
    const val SCENE_TOO = "씬도 되살리기"
    const val OTHER_SCENE_END = "다른 씬 끝에"

    /** Restore toast of a whole project, on the `<name> 되살림` pattern of CST-43 (`ED`). */
    const val RESTORED_SUFFIX = " 되살림"

    /** `영구 삭제` confirm (LIB-39: "confirm (copy: not in sources)"). */
    const val PURGE_CONFIRM = "영구 삭제 · 되돌릴 수 없음"

    /** `비우기` confirm (LIB-39: "confirm (copy: not in sources)"). */
    const val EMPTY_CONFIRM = "휴지통 전체 영구 삭제 · 되돌릴 수 없음"

    /** An empty trash (no board; LIB-70 lists no trash empty state). */
    const val EMPTY = "휴지통 비어 있음"

    /** Accessible label of a block group's expand toggle (the board draws no control). */
    const val EXPAND_LABEL = "블록 펼치기"
    const val COLLAPSE_LABEL = "블록 접기"

    fun restored(name: String): String = name + RESTORED_SUFFIX
}
