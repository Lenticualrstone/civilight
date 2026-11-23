package io.github.lenticualrstone.civilight.feature.cast.settings

import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings

/** Copy of the `인물 항목` order mode and delete confirm (30-cast.md §7.6, CST-54, CST-55). */
object FieldOrderCopy {
    /** Section trailing button and undo label. */
    const val REORDER = FieldSettings.REORDER_ACTION
    const val BTN_DONE = "완료"
    const val BTN_FORWARD = "앞으로"
    const val BTN_BACKWARD = "뒤로"
    const val ORDER_NOTE = FieldSettings.ORDER_NOTE
}
