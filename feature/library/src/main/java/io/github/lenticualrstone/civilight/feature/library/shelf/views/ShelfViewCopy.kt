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
    const val PREV = IconButtonCopy.PREV
    const val NEXT = IconButtonCopy.NEXT

    /** 별자리 cluster labels: the 형식 words of LIB-09. */
    const val FORMAT_SERIAL = NewProjectCopy.FORMAT_SERIAL
    const val FORMAT_SHORT = NewProjectCopy.FORMAT_SHORT
    const val FORMAT_SETTING = NewProjectCopy.FORMAT_SETTING

    /** `3 / 8`: the item in front and the count, under the view. */
    fun position(index: Int, count: Int): String = "${index + 1} / $count"
}
