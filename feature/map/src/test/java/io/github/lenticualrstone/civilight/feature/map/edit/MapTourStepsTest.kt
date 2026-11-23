package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.domain.library.onboarding.ModuleTours
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LIB-19 map tour holes (emulator QA: its cards sat in the middle without a hole). */
class MapTourStepsTest {
    private val titles = ModuleTours.steps(TourModule.MAP).map { it.title }

    @Test
    fun `step ids follow the tour's copy`() {
        assertEquals(3, titles.size)
        assertEquals("도구 줄", titles[MapTourSteps.TOOLBAR])
        assertEquals("경로", titles[MapTourSteps.ROUTE])
        assertEquals("보이는 시점", titles[MapTourSteps.REVEAL])
    }

    @Test
    fun `보이는 시점 points at the ⋯ menu, whose 시점별 공개 row opens the map's reveal points`() {
        assertEquals(2, MapTourSteps.REVEAL)
        assertEquals("시점별 공개", io.github.lenticualrstone.civilight.feature.map.MapCopy.REVEAL_TITLE)
        assertEquals("보이는 시점", io.github.lenticualrstone.civilight.feature.map.MapCopy.REVEAL_POINT)
    }

    @Test
    fun `only the 경로 cell is a hole, and the editor's tool bar has it`() {
        assertEquals(MapTourSteps.ROUTE, MapTourSteps.stepOfTool(EditTool.ROUTE))
        EditTool.entries.filter { it != EditTool.ROUTE }.forEach { assertNull(it.name, MapTourSteps.stepOfTool(it)) }
        assertTrue(EditTool.ROUTE in EditTool.BASE)
        assertEquals("경로", EditTool.ROUTE.label)
    }
}
