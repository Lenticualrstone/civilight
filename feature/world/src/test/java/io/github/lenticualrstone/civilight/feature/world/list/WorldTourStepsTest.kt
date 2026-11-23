package io.github.lenticualrstone.civilight.feature.world.list

import io.github.lenticualrstone.civilight.domain.library.onboarding.ModuleTours
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LIB-19 world tour holes (emulator QA 2026-11-23: its cards floated with nothing marked). */
class WorldTourStepsTest {
    private fun row(key: String, candidates: String? = null) =
        WorldListItem.Row(WorldKind.TERM, key, key, "", "첫 공개 1-7", candidates)

    @Test
    fun `step ids follow the tour's copy, and the hint line is what 공개 시점 explains`() {
        val steps = ModuleTours.steps(TourModule.WORLD_TIMELINE)
        assertEquals(WorldTourSteps.COUNT, steps.size)
        assertEquals("공개 시점", steps[WorldTourSteps.REVEAL].title)
        assertEquals("용어 잇기", steps[WorldTourSteps.LINK].title)
        assertTrue(WorldCopy.TERM_HINT.startsWith("공개 시점"))
    }

    @Test
    fun `every step has the always-shown segments as fallback`() {
        assertEquals((0 until WorldTourSteps.COUNT).toList(), WorldTourSteps.TABS_FALLBACK.sorted())
    }

    @Test
    fun `용어 잇기 points at the first row with candidates`() {
        val items = listOf(WorldListItem.Section("공개됨"), row("a"), row("b", "◇ 후보 1"), row("c", "◇ 후보 2"))
        assertEquals("b", WorldTourSteps.linkRowKey(items))
        assertNull(WorldTourSteps.linkRowKey(listOf(row("a"), WorldListItem.Empty("용어 없음", "용어 만들기"))))
        assertNull(WorldTourSteps.linkRowKey(emptyList()))
    }
}
