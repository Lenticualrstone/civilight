package io.github.lenticualrstone.civilight.feature.map.view

import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-37 (boards `Map`, `MapC`): header, breadcrumb, chips, scale bar, place sheet. */
class MapViewStateTest {
    private val doc = MapFx.doc()

    private fun build(pin: String? = null, filter: String? = null, moving: Pair<String, Point?>? = null, mapKey: String = K.MAP_LOWER_DISTRICT) =
        MapViewState.build(doc, mapKey, LensPoint.Latest, false, filter, pin, moving)!!

    @Test
    fun `header, breadcrumb and scale bar of 하층 구역 지도`() {
        val ui = build()
        assertEquals("하층 구역 지도", ui.title)
        assertEquals("세계 · 지도 · 먹선", ui.small)
        assertEquals(listOf("세계 지도", "하층 구역 지도"), ui.crumbs.map { it.text })
        assertEquals("1일 · 15 km", ui.scale)
        assertEquals(listOf("전체 핀", "길"), ui.chips.map { it.label })
        assertTrue(ui.chips.first().selected)
        assertFalse(ui.canReplaceBase)
        assertNull(ui.place)
    }

    @Test
    fun `a top-level map has no breadcrumb`() {
        val ui = build(mapKey = K.MAP_WORLD)
        assertTrue(ui.crumbs.isEmpty())
        assertEquals("세계 · 지도 · 양피지", ui.small)
        assertEquals("1일 · 15 km", ui.scale)
    }

    @Test
    fun `tapping 하층 거리 opens its place sheet`() {
        val place = build(pin = K.PIN_LOWER_STREET).place!!
        assertEquals("하층 거리", place.name)
        assertEquals(K.PLACE_LOWER_STREET, place.placeKey)
        assertTrue(place.eyebrow.startsWith("장소"))
        assertTrue(place.appearance.isNotBlank())
        assertTrue(place.childMaps.isEmpty())
    }

    @Test
    fun `a pin with a child map lists it (Q-02 visible control)`() {
        val market = build(pin = K.PIN_ASH_MARKET).place!!
        assertEquals(listOf(K.MAP_ASH_MARKET to "잿더미 시장"), market.childMaps)
    }

    @Test
    fun `a filtered-out pin has no sheet and the chip shows the filter`() {
        val ui = build(pin = K.PIN_LOWER_STREET, filter = K.mapLayer(2))
        assertNull(ui.place)
        assertTrue(ui.chips.last().selected)
        assertEquals(build(filter = "gone").chips.first().selected, true)
    }

    @Test
    fun `핀 옮기기 shows the pin at the tapped spot`() {
        val ui = build(moving = K.PIN_SOCIETY_GATE to Point(0.7f, 0.7f))
        assertEquals("핀 옮기기 · 결사의 문", ui.moving!!.title)
        assertEquals(Point(0.7f, 0.7f), ui.scene.pin(K.PIN_SOCIETY_GATE)!!.at)
        assertNull(build(moving = K.PIN_SOCIETY_GATE to null).moving!!.to)
    }

    @Test
    fun `a trashed or unknown map has no view`() {
        assertNull(MapViewState.build(doc, "nope", LensPoint.Latest, false, null, null, null))
    }
}
