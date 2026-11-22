package io.github.lenticualrstone.civilight.feature.map.reveal

import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-49 시점별 공개 (boards `MapReveal`, `MMapReveal`; spec case WLD-T56, fixture conflict 19). */
class RevealStateTest {
    private val doc = MapFx.doc().let { d -> d.copy(maps = d.maps + (K.MAP_LOWER_DISTRICT to d.maps.getValue(K.MAP_LOWER_DISTRICT).copy(revealViewDim = false))) }
    private val at15 = LensPoint.UnitEnd(K.EPISODE_1_5)

    @Test
    fun `WLD-T56 at 1-5 four pins and one region show, 상층 관문 waits`() {
        // 보이는 것만 (the map's `숨은 것 흐리게` off).
        val ui = RevealState.build(doc, K.MAP_LOWER_DISTRICT, at15, hiddenOpen = false)!!
        assertEquals("보임 · 핀 4 · 영역 1", ui.visibleChip)
        assertEquals("아직 공개 전 · 1", ui.pendingChip)
        assertEquals("1-5 시점 · 보이는 것만", ui.modeLine)
        assertEquals("세계 지도 › 하층 구역 지도", ui.small)
        assertFalse(ui.dim)
        assertEquals(4, ui.scene.pins.size)
        assertNull(ui.scene.pin(K.PIN_UPPER_GATE))
    }

    @Test
    fun `숨은 것 흐리게 draws the waiting pin dimmed with its point`() {
        val d = doc.copy(maps = doc.maps + (K.MAP_LOWER_DISTRICT to doc.maps.getValue(K.MAP_LOWER_DISTRICT).copy(revealViewDim = true)))
        val ui = RevealState.build(d, K.MAP_LOWER_DISTRICT, at15, hiddenOpen = false)!!
        assertTrue(ui.dim)
        val gate = ui.scene.pin(K.PIN_UPPER_GATE)!!
        assertTrue(gate.dim)
        assertEquals("상층 관문 · 1-7", gate.name)
    }

    @Test
    fun `the scrubber has one stop per episode and marks the lens`() {
        val ui = RevealState.build(doc, K.MAP_LOWER_DISTRICT, at15, hiddenOpen = false)!!
        val codes = ui.stops.map { it.code }
        assertTrue(codes.containsAll(listOf("1-1", "1-3", "1-5", "1-7", "1-9", "2-3")))
        assertEquals(listOf("1-5"), ui.stops.filter { it.current }.map { it.code })
        assertEquals(RevealState.episodes(doc).size, ui.stops.size)
    }

    @Test
    fun `at 최신 nothing on the map waits and no stop is current`() {
        val ui = RevealState.build(doc, K.MAP_LOWER_DISTRICT, LensPoint.Latest, hiddenOpen = false)!!
        assertEquals("보이는 것만", ui.modeLine)
        assertTrue(ui.stops.none { it.current })
        assertEquals(5, ui.scene.pins.size)
    }

    @Test
    fun `an unknown map has no reveal view`() {
        assertNull(RevealState.build(doc, "nope", at15, false))
    }
}
