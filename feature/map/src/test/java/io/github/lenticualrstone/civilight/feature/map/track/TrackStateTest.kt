package io.github.lenticualrstone.civilight.feature.map.track

import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.open
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 인물 동선 (WLD-62) on the example project. */
class TrackStateTest {
    private val doc = MapFx.doc()
    private fun place(key: String) = PlaceRef(PlaceRefKind.PLACE.open(), key)

    @Test
    fun `WLD-62 the board's header, chips and 리엔 · 씬 순서 rows in time order`() {
        val ui = TrackState.build(doc, null, listOf(K.RIEN), LensPoint.Latest, false, null)!!
        assertEquals(K.MAP_LOWER_DISTRICT, ui.mapKey)
        assertEquals("세계 지도 › 하층 구역 지도", ui.small)
        assertTrue(ui.chips.single { it.key == K.RIEN }.focused)
        assertEquals("리엔 · 씬 순서", ui.listTitle)
        val first = ui.rows.first()
        assertEquals("1-2", first.episode)
        assertEquals("하층 거리", first.place)
        assertEquals("씬 1 · 1일째 새벽", first.detail)
        // WLD-T58: the flashback 1-4 씬 2 is no stop.
        assertTrue(ui.rows.none { it.sceneKey == K.SCENE_1_4_2 })
        assertTrue(ui.places.any { it.startsWith("하층 거리 · ") })
        assertEquals("전체 핀", ui.layerChips.first().label)
    }

    @Test
    fun `WLD-62 legs follow the route between 결사의 문 and 잿더미 시장, others are straight 추정`() {
        val ui = TrackState.build(doc, K.MAP_LOWER_DISTRICT, listOf(K.RIEN), LensPoint.Latest, false, null)!!
        assertTrue(ui.lines.isNotEmpty())
        val routed = ui.lines.filter { it.label == null }
        assertTrue(routed.isNotEmpty())
        assertTrue(routed.all { !it.dashed && it.points.size > 2 })
        val straight = ui.lines.filter { it.label == "추정" }
        assertTrue(straight.isNotEmpty())
        assertTrue(straight.all { it.dashed && it.points.size == 2 })
        assertTrue(ui.stops.all { it.focused })
    }

    @Test
    fun `WLD-T58 the position at 1-5 is the latest stop up to the lens`() {
        val ui = TrackState.build(doc, K.MAP_LOWER_DISTRICT, listOf(K.RIEN), LensPoint.UnitEnd(K.EPISODE_1_5), false, null)!!
        val pos = ui.positions.single()
        assertEquals("리엔", pos.name)
        val tracks = TrackState.tracks(doc, false)
        val stop = tracks.positionAt(tracks.track(K.RIEN)!!, LensPoint.UnitEnd(K.EPISODE_1_5))!!
        assertEquals(TrackState.pointOn(doc, doc.maps.getValue(K.MAP_LOWER_DISTRICT), stop.place, LensPoint.Latest), pos.at)
        assertTrue(ui.scrubber.single { it.current }.code == "1-5")
    }

    @Test
    fun `a place on a sub-map stands at its ancestor pin on the parent map`() {
        val world = doc.maps.getValue(K.MAP_WORLD)
        val upper = world.pins.first { it.key == K.PIN_WORLD_UPPER_CITY }
        assertEquals(Point(upper.x, upper.y), TrackState.pointOn(doc, world, place(K.PLACE_LOWER_STREET), LensPoint.Latest))
        val lower = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
        val pin = lower.pins.first { it.placeKey == K.PLACE_LOWER_STREET }
        assertEquals(Point(pin.x, pin.y), TrackState.pointOn(doc, lower, place(K.PLACE_LOWER_STREET), LensPoint.Latest))
        assertNotNull(TrackState.pointOn(doc, lower, PlaceRef(PlaceRefKind.REGION.open(), K.REGION_LOWER_DISTRICT), LensPoint.Latest))
        assertNull(TrackState.pointOn(doc, lower, place("nowhere"), LensPoint.Latest))
    }

    @Test
    fun `chips add, focus and remove characters`() {
        assertEquals(listOf("a", "b"), TrackState.tapChip(listOf("a"), "b"))
        assertEquals(listOf("a"), TrackState.tapChip(listOf("a", "b"), "b"))
        assertEquals(listOf("b", "a"), TrackState.tapChip(listOf("a", "b"), "a"))
        assertEquals(listOf(K.RIEN), TrackState.initial(doc, false, K.RIEN))
        assertFalse(TrackState.initial(doc, false, null).isEmpty())
        val ui = TrackState.build(doc, null, emptyList(), LensPoint.Latest, false, null)!!
        assertNull(ui.listTitle)
        assertTrue(ui.lines.isEmpty())
    }
}
