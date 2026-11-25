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
    fun `경로 is step 1, on the tool bar's 경로 cell`() {
        assertTrue(EditTool.ROUTE in EditTool.BASE)
        assertEquals(EditTool.ROUTE.label, titles[MapTourSteps.ROUTE])
        assertEquals(MapTourSteps.ROUTE, MapTourSteps.stepOfTool(EditTool.ROUTE))
        assertEquals(listOf(0, 1, 2), TourStepFilter.shown(titles.size, emptySet()))
        // 상세 지형 · 편집 has no 경로 cell: the step is skipped there.
        assertFalse(EditTool.ROUTE in EditTool.DETAIL)
        assertEquals(listOf(MapTourSteps.TOOLBAR, MapTourSteps.REVEAL), TourStepFilter.shown(titles.size, setOf(MapTourSteps.ROUTE)))
    }
}
