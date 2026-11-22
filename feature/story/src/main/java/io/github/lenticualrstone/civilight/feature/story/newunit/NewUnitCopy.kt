package io.github.lenticualrstone.civilight.feature.story.newunit

/** Copy of `다음 씬 만들기` · `앞에 새 씬` (STO-05; layout SCR-49 / SCR-53, R22 §4). */
object NewUnitCopy {
    const val TITLE_AFTER = "다음 씬 만들기"
    const val TITLE_BEFORE = "앞에 새 씬"

    /** Sub line `1-7 · 씬 3 뒤` / `1-7 · 씬 4 앞`. */
    const val AFTER = "뒤"
    const val BEFORE = "앞"

    const val FIELD_TITLE = "제목"
    const val FIELD_PLACE = "장소"
    const val FIELD_TIME = "때"
    const val FIELD_TRANSITION = "전환 문구"

    const val WRITE = "쓰기"
    const val CLEAR = "비우기"
    const val CHANGE = "바꾸기"

    const val NO_PLACE = "장소 없음"
    const val NO_TIME = "때 없음"

    const val MODE_AUTO = "자동"
    const val MODE_DIRECT = "직접"
    const val MODE_NONE = "없음"

    const val CREATE = "만들기"

    /** 때 chips (STO-20 / STO-21 words) and the base word of the line `1-7 씬 3 기준 · +1일`. */
    const val SAME_DAY = "같은 날"
    const val PLUS_ONE = "+1일"
    const val PLUS_TWO = "+2일"
    const val BASE = "기준"

    /** Place picker title (WLD-52). */
    const val PICK_PLACE = "장소 고르기"

    /** Undo label of the creation (the `새 씬` action of STO-05). */
    const val UNDO_LABEL = "새 씬"
}
