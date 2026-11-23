package io.github.lenticualrstone.civilight.feature.cast.profile

import io.github.lenticualrstone.civilight.domain.cast.field.FieldRow
import io.github.lenticualrstone.civilight.navigation.ProfileTab

/**
 * The cast tour's targets on the character record (LIB-19, board `TourCast`; copy in `ModuleTours`):
 * which element each step's hole goes around. `값이 바뀌는 때` points at a field row of the record's
 * header ([timedValueRow]: one with `◆` history first, else the first editable one, whose tap opens
 * `이 값 고치기 · 여기서부터 바뀜`); with no such row its fallback is the lens chip, which picks the point
 * the values are shown at (emulator QA 2026-11-23: the card floated with nothing marked).
 */
object CastTourSteps {
    /** `시점 렌즈`: the lens chip of the record's header (floating pill or glass bar). */
    const val LENS: Int = 0

    /** `값이 바뀌는 때`: a header field row ([timedValueRow]), else the lens chip (fallback). */
    const val TIMED_VALUE: Int = 1

    /** `관계`: the `관계` tab of the record's tab row. */
    const val RELATIONS: Int = 2

    /**
     * The label of the header row that is the `값이 바뀌는 때` hole among the [shown] rows: the first
     * with `◆` history, else the first field row (one with a definition, so a tap edits its value), else null.
     */
    fun timedValueRow(shown: List<FieldRow>): String? {
        val fields = shown.filter { it.defKey != null }
        return (fields.firstOrNull { it.hasHistory } ?: fields.firstOrNull())?.label
    }

    /** The tour step a record tab at [tabIndex] is the hole of, or null. */
    fun stepOfTab(tabIndex: Int): Int? = if (tabIndex == ProfileTab.RELATIONS.ordinal) RELATIONS else null
}
