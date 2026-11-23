package io.github.lenticualrstone.civilight.feature.map.track

import io.github.lenticualrstone.civilight.domain.library.onboarding.ModuleTours
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** LIB-19 `인물 동선 · 장소와 때` tour on 인물 동선 (emulator QA 2026-11-23: its cards floated with nothing marked). */
class TrackTourStepsTest {
    @Test
    fun `step ids follow the tour's copy`() {
        val titles = ModuleTours.steps(TourModule.ROUTE_PLACE_TIME).map { it.title }
        assertEquals(TrackTourSteps.COUNT, titles.size)
        assertEquals("장소와 때", titles[TrackTourSteps.PLACE_TIME])
        assertEquals("인물 동선", titles[TrackTourSteps.TRACK])
    }

    @Test
    fun `every step has the always-shown panel tabs as fallback`() {
        assertEquals((0 until TrackTourSteps.COUNT).toList(), TrackTourSteps.TABS_FALLBACK.sorted())
    }

    @Test
    fun `the tour waits without a map, and the first row shows a scene's place and 때`() {
        assertTrue(TrackTourSteps.hold(null))
        val ui = TrackState.build(MapFx.doc(), null, listOf(K.RIEN), LensPoint.Latest, false, null)!!
        assertFalse(TrackTourSteps.hold(ui))
        val first = ui.rows.first()
        assertEquals("하층 거리", first.place)
        assertEquals("씬 1 · 1일째 새벽", first.detail)
        // The scrubber (`인물 동선`) has its stops.
        assertTrue(ui.scrubber.isNotEmpty())
    }
}
