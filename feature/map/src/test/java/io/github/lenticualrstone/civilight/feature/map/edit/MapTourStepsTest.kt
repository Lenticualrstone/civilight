package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.domain.library.onboarding.ModuleTours
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.ui.tour.TourStepFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** LIB-19 map tour holes (emulator QA: its cards sat in the middle without a hole). */
class MapTourStepsTest {
    private val titles = ModuleTours.steps(TourModule.MAP).map { it.title }

    @Test
    fun `step ids follow the tour's copy`() {
        assertEquals("도구 줄", titles[MapTourSteps.TOOLBAR])
        assertEquals("보이는 시점", titles[MapTourSteps.REVEAL])
    }

    @Test
    fun `보이는 시점 points at the ⋯ menu, whose 시점별 공개 row opens the map's reveal points`() {
        assertEquals("시점별 공개", io.github.lenticualrstone.civilight.feature.map.MapCopy.REVEAL_TITLE)
        assertEquals("보이는 시점", io.github.lenticualrstone.civilight.feature.map.MapCopy.REVEAL_POINT)
    }

    @Test
    fun `S-9 WLD-75 the tour shows only steps the editor marks, never 경로`() {
        assertFalse(EditTool.ROUTE in EditTool.BASE)
        val shown = TourStepFilter.shown(titles.size, MapTourSteps.absent().toSet())
        assertEquals(listOf(MapTourSteps.TOOLBAR, MapTourSteps.REVEAL), shown)
        assertTrue(shown.none { titles[it] == EditTool.ROUTE.label })
    }

    @Test
    fun `with the 경로 copy dropped, 보이는 시점 is step 1 and nothing is absent`() {
        val twoStep = titles.filterNot { it == EditTool.ROUTE.label }
        assertEquals(1, twoStep.indexOf("보이는 시점"))
        assertEquals(listOf(0, 1), TourStepFilter.shown(twoStep.size, emptySet()))
    }
}
