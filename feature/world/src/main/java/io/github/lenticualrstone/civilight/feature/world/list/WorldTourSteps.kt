package io.github.lenticualrstone.civilight.feature.world.list

import io.github.lenticualrstone.civilight.model.Key

/**
 * The world tour's targets on 세계 (LIB-19, board `TourWorld`; copy in `ModuleTours`): which element
 * each step's hole goes around (emulator QA 2026-11-23: every card floated with nothing marked).
 * - `공개 시점`: the `용어` hint line `공개 시점 전 사용 → 점검 센터` (on the `용어` tab).
 * - `용어 잇기`: the right column of the first term row with `◇ 후보` (list view).
 * Both fall back to the `장소 · 용어 · 세력 · 지도` segments ([TABS_FALLBACK]), always on screen, while
 * their own element is not (another tab, card view, no candidates). The tour waits while the list loads.
 */
object WorldTourSteps {
    /** `공개 시점`. */
    const val REVEAL: Int = 0

    /** `용어 잇기`. */
    const val LINK: Int = 1

    const val COUNT: Int = 2

    /** The steps the segments back up: every step. */
    val TABS_FALLBACK: List<Int> = listOf(REVEAL, LINK)

    /** The row whose right column is the `용어 잇기` hole: the first term row with `◇ 후보`, or null. */
    fun linkRowKey(items: List<WorldListItem>): Key? =
        items.firstOrNull { it is WorldListItem.Row && it.candidates != null }?.let { (it as WorldListItem.Row).key }
}
