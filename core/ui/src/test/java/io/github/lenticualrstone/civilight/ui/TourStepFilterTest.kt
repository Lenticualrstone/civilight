package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.ui.tour.TourStepFilter
import org.junit.Assert.assertEquals
import org.junit.Test

/** LIB-19 (emulator QA round 2): a step whose element is not on screen is left out of the tour. */
class TourStepFilterTest {
    @Test
    fun `nothing absent - every step in order`() {
        assertEquals(listOf(0, 1, 2, 3), TourStepFilter.shown(4, emptySet()))
    }

    @Test
    fun `an absent step drops out and the rest keep their ids`() {
        // The read-only example has no command field: `명령 칸` (0) goes, the card counts 1/3.
        val shown = TourStepFilter.shown(4, setOf(0))
        assertEquals(listOf(1, 2, 3), shown)
        assertEquals(3, shown.size)
    }

    @Test
    fun `ids beyond the steps are ignored, all absent - nothing to show`() {
        assertEquals(listOf(0, 2), TourStepFilter.shown(3, setOf(1, 7)))
        assertEquals(emptyList<Int>(), TourStepFilter.shown(2, setOf(0, 1)))
    }
}
