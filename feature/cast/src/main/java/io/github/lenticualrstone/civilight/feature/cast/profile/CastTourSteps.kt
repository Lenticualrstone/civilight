package io.github.lenticualrstone.civilight.feature.cast.profile

import io.github.lenticualrstone.civilight.navigation.ProfileTab

/**
 * The cast tour's targets on the character record (LIB-19, board `TourCast`; copy in `ModuleTours`):
 * which element each step's hole goes around. `값이 바뀌는 때` points at no single element on entry
 * (a value with a change may be anywhere in the record, or nowhere), so its card sits in the middle.
 */
object CastTourSteps {
    /** `시점 렌즈`: the lens chip of the record's header (floating pill or glass bar). */
    const val LENS: Int = 0

    /** `값이 바뀌는 때`: no hole (see above). */
    const val TIMED_VALUE: Int = 1

    /** `관계`: the `관계` tab of the record's tab row. */
    const val RELATIONS: Int = 2

    /** The tour step a record tab at [tabIndex] is the hole of, or null. */
    fun stepOfTab(tabIndex: Int): Int? = if (tabIndex == ProfileTab.RELATIONS.ordinal) RELATIONS else null
}
