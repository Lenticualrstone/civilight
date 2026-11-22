package io.github.lenticualrstone.civilight.feature.story.where

/**
 * Copy of `씬 · 장소와 때` (STO-20, boards `SceneWhere`, `RTFlashback`) and `때 바꾸기` (STO-23, board
 * `RTCascade`). Sheet titles, base lines, result lines and cascade rows come from `domain:story`.
 */
object WhereCopy {
    const val FIELD_PLACE = "장소"
    const val PICK_ON_MAP = "지도에서 고르기 ›"
    const val PICK_PLACE = "장소 고르기"
    const val NO_PLACE = "장소 없음"

    const val FLASHBACK = "회상"
    const val FLASHBACK_SUB = "연쇄 · 이동 점검에서 빠짐"

    const val FIELD_TIME = "때"
    const val SAME_DAY = "같은 날"
    const val PLUS_ONE = "+1일"
    const val PLUS_TWO = "+2일"
    const val DIRECT = "직접"

    /** `직접` fixed point: `이야기` [field] `일째`. */
    const val STORY = "이야기"
    const val DAY_UNIT = "일째"

    /** Exact-time field format hint (the board's value). */
    const val CLOCK_HINT = "05:40"

    const val PRECISION = "정밀도"
    const val EXACT = "정확"
    const val APPROX = "대략"

    const val REBASE_TO_PREVIOUS = "기준을 직전 씬으로"
    const val ON_TIMELINE = "연표에서"

    const val TRANSITION = "전환 문구"
    const val MODE_AUTO = "자동"
    const val MODE_DIRECT = "직접"
    const val MODE_NONE = "없음"

    const val MATCH = "맞음"

    // STO-11 / R20 §2: a deleted place or silent member; R19 §6 locked hidden items.
    const val RESTORE = "되살리기"
    const val KEEP = "이대로 두기"
    const val REMOVE = "빼기"
    const val HIDDEN_ITEM = "숨긴 항목"
    const val HIDDEN_CHARACTER = "숨긴 인물"

    /** Narrow-spot mark of an item attached to something deleted (R20 §2). */
    const val DELETED_MARK = "◇"

    // STO-26 (board `SceneWhere`): `등장 | 리엔 · 하르트 | (대사) | + 세라`.
    const val CAST = "등장"
    const val SPOKEN = "(대사)"

    // STO-24 (board `RTFlashback`).
    const val FLASHBACK_BASE = "때 · 기준"
    const val FLASHBACK_NO_BASE = "회상은 직전 씬 기준 없음"
    const val ANCHOR_EVENT = "기준 사건"
    const val CHANGE_LINK = "바꾸기 ›"
    const val BEFORE = "전"
    const val ON_THE_DAY = "그날"
    const val AFTER = "후"

    /** The `직접` year field (the result line's word `연도 1195`). */
    const val YEAR = "연도"

    /** Unit of the `직접` date's day field with the world calendar on (`서리달 26일`, Q14 `ED`). */
    const val DATE_DAY_UNIT = "일"

    // RTCascade.
    const val CANCEL = "취소"
    const val APPLY = "바꾸기"
    const val WITH_FOLLOWING = "뒤도 같이"
    const val THIS_ONLY = "이 씬만"
    const val ARROW = "→"

    /** Undo label of every 때 / 장소 change made in this sheet. */
    const val UNDO_LABEL = "장소와 때"
}
