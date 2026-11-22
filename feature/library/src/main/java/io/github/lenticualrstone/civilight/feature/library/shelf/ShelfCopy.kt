package io.github.lenticualrstone.civilight.feature.library.shelf

/** Copy of the 서재 and its sheets (LIB-01 … LIB-03; boards `Library`, `LibUnlocked`, `LibMenu`, `ConfirmDelete`). */
object ShelfCopy {
    const val SMALL = "Civilight"
    const val TITLE = "서재"
    const val NEW_PROJECT = "새 프로젝트"
    const val RELOCK = "다시 잠그기"
    const val SEARCH_HINT = "모든 프로젝트에서 찾기"
    const val WRITING = "쓰는 중"
    const val GROUP_OPEN = "열림"
    const val UNHIDE = "숨김 풀기"
    const val LIBRARY_SECTION = "라이브러리 · 프로젝트 밖"
    const val RECORDER = "라이프 레코더"
    const val RECORDER_NO_RECORDS = "기록 0"
    const val CLOSE = "닫기"
    const val CANCEL = "취소"

    /** `이름 바꾸기` sheet: field label reused from LIB-09 (spec: rename copy not in sources). */
    const val RENAME_TITLE = "이름 바꾸기"
    const val NAME_LABEL = "이름"
    const val RENAME_CONFIRM = "바꾸기"

    /** Accessible label of a row's ⋯ button; the boards draw only the icon. */
    const val TODO_COPY_LIB02_MORE_LABEL = "TODO_COPY_LIB02_MORE_LABEL"
}
