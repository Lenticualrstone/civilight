package io.github.lenticualrstone.civilight.feature.map.pick

import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.feature.map.canvas.MapHit
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-52 map mode: a pin picks its place, a region (in `지역` mode) picks the region. */
class PickStateTest {
    private val doc = MapFx.doc()

    @Test
    fun `the first map holds the scene's place or region`() {
        assertEquals(K.MAP_LOWER_DISTRICT, PickState.initialMap(doc, K.SCENE_1_7_1))
        assertEquals(K.MAP_LOWER_DISTRICT, PickState.initialMap(doc, K.SCENE_1_5_1))
        assertTrue(PickState.initialRegions(doc, K.SCENE_1_5_1))
        assertFalse(PickState.initialRegions(doc, K.SCENE_1_7_1))
        assertEquals(K.MAP_WORLD, PickState.initialMap(doc, null))
        assertEquals(listOf(K.MAP_WORLD, K.MAP_LOWER_DISTRICT), PickState.pickableMaps(doc).map { it.key })
    }

    @Test
    fun `tapping a pin picks its place and the bar confirms`() {
        val ref = PickState.pick(doc, K.MAP_LOWER_DISTRICT, Point(0.45f, 0.40f), MapHit.Pin(K.PIN_ASH_MARKET), false, LensPoint.Latest, false)
        assertEquals(PlaceRef(PlaceRefKind.PLACE, K.PLACE_ASH_MARKET), ref)
        val ui = PickState.build(doc, K.MAP_LOWER_DISTRICT, LensPoint.Latest, false, false, ref, K.SCENE_1_7_1)!!
        assertEquals("씬 1개 → 잿더미 시장", ui.bar)
        assertEquals(K.PIN_ASH_MARKET, ui.selectedKey)
        assertEquals("잿더미 시장", PickState.build(doc, K.MAP_LOWER_DISTRICT, LensPoint.Latest, false, false, ref, null)!!.bar)
    }

    @Test
    fun `in 지역 mode a tap inside a region picks it`() {
        val ref = PickState.pick(doc, K.MAP_LOWER_DISTRICT, Point(0.5f, 0.58f), null, true, LensPoint.Latest, false)
        assertEquals(PlaceRef(PlaceRefKind.REGION, K.REGION_LOWER_DISTRICT), ref)
        val ui = PickState.build(doc, K.MAP_LOWER_DISTRICT, LensPoint.Latest, false, true, ref, K.SCENE_1_5_1)!!
        assertEquals("씬 1개 → 하층 구역", ui.bar)
        assertEquals(K.REGION_LOWER_DISTRICT, ui.selectedKey)
        assertNull(PickState.pick(doc, K.MAP_LOWER_DISTRICT, Point(0.9f, 0.9f), null, true, LensPoint.Latest, false))
    }

    @Test
    fun `rule 19 - a hidden place cannot be picked while locked, nor a pin not yet revealed`() {
        val hidden = doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == K.PLACE_ASH_MARKET) it.copy(hidden = true) else it }))
        val tap = Point(0.45f, 0.40f)
        assertNull(PickState.pick(hidden, K.MAP_LOWER_DISTRICT, tap, MapHit.Pin(K.PIN_ASH_MARKET), false, LensPoint.Latest, false))
        assertEquals(
            PlaceRef(PlaceRefKind.PLACE, K.PLACE_ASH_MARKET),
            PickState.pick(hidden, K.MAP_LOWER_DISTRICT, tap, MapHit.Pin(K.PIN_ASH_MARKET), false, LensPoint.Latest, true),
        )
        val plain = doc.maps.getValue(K.MAP_LOWER_DISTRICT).copy(revealViewDim = false)
        val undimmed = doc.copy(maps = doc.maps + (plain.key to plain))
        assertNull(PickState.pick(undimmed, K.MAP_LOWER_DISTRICT, Point(0.416667f, 0.30f), MapHit.Pin(K.PIN_UPPER_GATE), false, LensPoint.UnitEnd(K.EPISODE_1_5), false))
        assertNull(PickState.pick(doc, K.MAP_LOWER_DISTRICT, tap, null, false, LensPoint.Latest, false))
    }

    @Test
    fun `the switcher lists pickable maps, node maps excluded`() {
        val ui = PickState.build(doc, K.MAP_WORLD, LensPoint.Latest, false, false, null, null)!!
        assertEquals(listOf("세계 지도", "하층 구역 지도"), ui.maps.map { it.name })
        assertTrue(ui.maps.first().selected)
        assertNull(ui.bar)
        assertNull(PickState.build(doc, K.MAP_ASH_MARKET, LensPoint.Latest, false, false, null, null))
    }

    @Test
    fun `WLD-41 every pickable map offers a new pin, a node map is not pickable`() {
        val generated = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
        assertTrue(io.github.lenticualrstone.civilight.feature.map.common.MapOffers.canDropPin(generated))
        val ref = PickState.pick(doc, K.MAP_LOWER_DISTRICT, Point(0.45f, 0.40f), MapHit.Pin(K.PIN_ASH_MARKET), false, LensPoint.Latest, false)
        assertEquals(PlaceRef(PlaceRefKind.PLACE, K.PLACE_ASH_MARKET), ref)
        assertTrue(PickState.canDropPin(doc, K.MAP_LOWER_DISTRICT))
        assertTrue(PickState.canDropPin(doc, K.MAP_WORLD))
        assertFalse(PickState.canDropPin(doc, K.MAP_ASH_MARKET))
    }
}
