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
    fun `only the 경로 cell is a hole, and the editor's tool bar has it`() {
        assertEquals(MapTourSteps.ROUTE, MapTourSteps.stepOfTool(EditTool.ROUTE))
        EditTool.entries.filter { it != EditTool.ROUTE }.forEach { assertNull(it.name, MapTourSteps.stepOfTool(it)) }
        assertTrue(EditTool.ROUTE in EditTool.BASE)
        assertEquals("경로", EditTool.ROUTE.label)
    }
}
