package io.github.lenticualrstone.civilight.feature.library.shelf.views

import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonCopy
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectCopy

/**
 * Copy of the 서재 3D views (LIB-93). The picked-item words are the DS-70 c-class list (`열기` · `꺼내기` ·
 * `앞으로` · `제자리` · `돌려놓기`); the ◀ ▶ names are the DS-70 `이전` · `다음`.
 */
object ShelfViewCopy {
    const val OPEN = "열기"
    const val PULL_OUT = "꺼내기"
    const val IN_PLACE = "제자리"
    const val PUT_BACK = "돌려놓기"
    const val FORWARD = "앞으로"

    /** 원고 더미 (board `scr_u16_lib_b` desk dock): spread the pile, gather it, lift a sheet, put it down. */
    const val SPREAD = "펼치기"
    const val GATHER = "모으기"
    const val LIFT = "들어 보기"
    const val PUT_DOWN = "내려놓기"

    /** 색인 카드 서랍: the label on the drawer front and the hint under it (board `scr_u16_lib_c`). */
    const val DRAWER_LABEL = "서재"
    const val DRAWER_HINT = "옆으로 밀기 · 탭하면 꺼내기"

    /** 색인 카드: the key of the 형식 row on a card. */
    const val FORM_KEY = "형식"
    const val PREV = IconButtonCopy.PREV
    const val NEXT = IconButtonCopy.NEXT

    /** 별자리 cluster labels: the 형식 words of LIB-09. */
    const val FORMAT_SERIAL = NewProjectCopy.FORMAT_SERIAL
    const val FORMAT_SHORT = NewProjectCopy.FORMAT_SHORT
    const val FORMAT_SETTING = NewProjectCopy.FORMAT_SETTING

    /** 별자리 cluster label: `연재 장편 · 2`. */
    fun groupCount(label: String, count: Int): String = "$label · $count"

    /** `3 / 8`: the item in front and the count, under the view. */
    fun position(index: Int, count: Int): String = "${index + 1} / $count"
}
