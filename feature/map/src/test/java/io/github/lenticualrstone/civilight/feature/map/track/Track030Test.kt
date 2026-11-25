package io.github.lenticualrstone.civilight.feature.map.track

import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.feature.map.reveal.RevealStop
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * (0.3.0) 인물 동선 back on screen: its own scrubber point (never the project lens), `재생` from 최신, and the
 * `▲ 이동 시간` card only while the travel check is on, masked while locked (rule 19).
 */
class Track030Test {
    private val doc = MapFx.doc()

    @Test
    fun `재생 from 최신 starts at the first episode, from the middle it plays on`() {
        val stops = listOf(RevealStop("a", "1-1", false), RevealStop("b", "1-2", false), RevealStop("c", "1-3", true))
        assertEquals("a", TrackState.playStart(stops))
        val middle = listOf(RevealStop("a", "1-1", false), RevealStop("b", "1-2", true), RevealStop("c", "1-3", false))
        assertNull(TrackState.playStart(middle))
        assertEquals("c", TrackState.nextStop(middle))
        assertNull(TrackState.nextStop(stops))
        assertNull(TrackState.playStart(emptyList()))
        // At 최신 no episode is marked: 재생 starts at the first at once.
        assertEquals("a", TrackState.playStart(stops.map { it.copy(current = false) }))
    }

    @Test
    fun `the scrubber follows the screen's own point, so a step moves the position`() {
        val latest = TrackState.build(doc, K.MAP_LOWER_DISTRICT, listOf(K.RIEN), LensPoint.Latest, false, null)!!
        val first = TrackState.playStart(latest.scrubber)
        assertNotNull("재생 from 최신 starts somewhere", first)
        val start = TrackState.build(doc, K.MAP_LOWER_DISTRICT, listOf(K.RIEN), LensPoint.UnitEnd(first!!), false, null)!!
        assertEquals(first, start.scrubber.single { it.current }.unitKey)
        // Every step of 재생 lands on the next episode until the end.
        var ui = start
        var steps = 0
        while (true) {
            val next = TrackState.nextStop(ui.scrubber) ?: break
            ui = TrackState.build(doc, K.MAP_LOWER_DISTRICT, listOf(K.RIEN), LensPoint.UnitEnd(next), false, null)!!
            assertEquals(next, ui.scrubber.single { it.current }.unitKey)
            steps++
        }
        assertEquals(latest.scrubber.size - 1 - latest.scrubber.indexOfFirst { it.unitKey == first }, steps)
    }

    @Test
    fun `the 이동 시간 card shows only with the travel check on, and not once accepted`() {
        val off = TrackState.build(doc, K.MAP_LOWER_DISTRICT, listOf(K.RIEN, K.HART), LensPoint.Latest, false, null)!!
        assertNull("off by default", off.warning)
        // The example project has accepted (`맞음`) its one ▲, so it stays quiet.
        val accepted = TrackState.build(doc, K.MAP_LOWER_DISTRICT, listOf(K.RIEN, K.HART), LensPoint.Latest, false, null, checkTravel = true)!!
        assertNull(accepted.warning)
        val open = doc.copy(checks = doc.checks.copy(acknowledgements = emptyList()))
        val on = TrackState.build(open, K.MAP_LOWER_DISTRICT, listOf(K.RIEN, K.HART), LensPoint.Latest, false, null, checkTravel = true)!!
        val w = on.warning!!
        assertEquals("하르트 · 1-7 → 1-7", w.who)
        assertEquals("도보 2일 4시간 · 있음 9시간 40분 – 12시간 40분", w.detail)
        // Only for a shown character.
        assertNull(TrackState.build(open, K.MAP_LOWER_DISTRICT, listOf(K.RIEN), LensPoint.Latest, false, null, checkTravel = true)!!.warning)
    }

    @Test
    fun `the row of the stop at the scrubber point is marked, one at a time`() {
        val latest = TrackState.build(doc, K.MAP_LOWER_DISTRICT, listOf(K.RIEN), LensPoint.Latest, false, null)!!
        assertEquals(1, latest.rows.count { it.current })
        assertTrue("at 최신 the last stop", latest.rows.last().current)
        val first = latest.rows.first()
        val early = TrackState.build(doc, K.MAP_LOWER_DISTRICT, listOf(K.RIEN), LensPoint.UnitEnd(first.sceneKey), false, null)!!
        assertTrue(early.rows.first().current)
        assertEquals(1, early.rows.count { it.current })
    }

    @Test
    fun `while locked the hidden 상층 관문 is in no stop, row, place or card`() {
        val gate = doc.world.places.first { it.key == K.PLACE_UPPER_GATE }
        val hidden = doc.copy(
            world = doc.world.copy(places = doc.world.places.map { if (it.key == K.PLACE_UPPER_GATE) it.copy(hidden = true) else it }),
            checks = doc.checks.copy(acknowledgements = emptyList()),
        )
        val sel = listOf(K.RIEN, K.HART)
        // Open: 하르트's ▲ runs 상층 관문 → 결사의 문.
        assertNotNull(TrackState.warningOf(hidden, sel, hiddenOpen = true))
        val ui = TrackState.build(hidden, K.MAP_LOWER_DISTRICT, sel, LensPoint.Latest, false, null, checkTravel = true)!!
        assertNull("the pair has a hidden end", ui.warning)
        assertNull(TrackState.warningOf(hidden, sel, hiddenOpen = false))
        assertTrue(ui.rows.none { gate.name in it.place || gate.name in it.detail })
        assertTrue(ui.places.none { gate.name in it })
        val gatePin = hidden.maps.getValue(K.MAP_LOWER_DISTRICT).pins.first { it.placeKey == K.PLACE_UPPER_GATE }
        assertTrue(ui.stops.none { it.at.x == gatePin.x && it.at.y == gatePin.y })
        assertTrue(ui.lines.none { l -> l.points.any { it.x == gatePin.x && it.y == gatePin.y } })
        assertTrue(ui.scene.pins.none { it.placeKey == K.PLACE_UPPER_GATE })
    }
}
