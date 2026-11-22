package io.github.lenticualrstone.civilight.feature.story.menu

/**
 * Row labels of the ⋯ menus (STO-07, R22 §7, SCR-47 `RSHeadMenu`). They equal the labels
 * `domain:story` `SceneHeads.menu` writes, which is how its rows are mapped to actions.
 */
object MenuCopy {
    const val PLACE_TIME = "장소와 때"
    const val TRANSITION = "전환 문구"
    const val MERGE_PREVIOUS = "앞 씬과 합치기"
    const val SPLIT_HERE = "여기서 나누기"
    const val DELETE_SCENE = "씬 지우기"
    const val NEW_SCENE_BEFORE = "앞에 새 씬"

    /** Scene ⋯ `옮기기` (STO-08, R12 §3). */
    const val MOVE = "옮기기"

    /** Episode ⋯ `번호 다시 매기기 · 1-7` (STO-07 → SCR-22 `RTRenumber`). */
    const val RENUMBER = "번호 다시 매기기"
    const val CANCEL = "취소"
}
