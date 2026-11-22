package io.github.lenticualrstone.civilight.feature.story.move

/** Copy of `씬 옮기기` (STO-08; board `RSSceneMove`, content SCR-54). Title, sub and rows' values come from `domain:story` `SceneMove`. */
object MoveCopy {
    /** The ⋯ row and the destination picker's title (the picker itself is not drawn, R12 §3). */
    const val MOVE = "옮기기"
    const val CANCEL = "취소"

    const val CHANGES = "바뀌는 것"
    const val ROW_RECORDS = "기록"
    const val ROW_BLOCKS = "블록 번호"
    const val ROW_VOICE = "보이스 ID"
    const val ROW_TIME = "때"

    /** Position rows of the picker: `맨 앞`, `씬 3 뒤` (the words of `RSSceneMove` and STO-05). */
    const val FIRST = "맨 앞"
    const val AFTER = "뒤"
}
