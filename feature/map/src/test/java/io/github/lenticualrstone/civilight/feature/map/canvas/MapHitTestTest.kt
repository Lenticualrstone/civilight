package io.github.lenticualrstone.civilight.feature.map.canvas

import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Taps by a 44 dp target (here 132 px at density 3), whatever the zoom (DS-11). */
class MapHitTestTest {
    private val doc = MapFx.doc()
    private val lower = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
    private val scene = MapScenes.build(doc, lower, LensPoint.Latest, false, SceneMode.EDIT)
    private val math = ViewportMath(1200f, 2400f, 4f / 3f)
    private val zoomed = Viewport(0.3f, 0.4f, 4f)
    private val touch = 132f

    private fun tapAt(p: Point, dx: Float = 0f, dy: Float = 0f): MapHit? {
        val s = math.toScreen(zoomed, p)
        return MapHitTest.hit(scene, math, zoomed, ScreenPoint(s.x + dx, s.y + dy), touch)
    }

    @Test
    fun `a pin is hit within half the target`() {
        assertEquals(MapHit.Pin(K.PIN_ASH_MARKET), tapAt(Point(0.45f, 0.40f), 50f, 0f))
        assertEquals(MapHit.Pin(K.PIN_SOCIETY_GATE), tapAt(Point(0.20f, 0.40f)))
    }

    @Test
    fun `the pin target is centred on the icon and covers the name tag`() {
        val tip = math.toScreen(zoomed, Point(0.45f, 0.40f))
        assertEquals(MapHit.Pin(K.PIN_ASH_MARKET), MapHitTest.hit(scene, math, zoomed, ScreenPoint(tip.x, tip.y - 90f), touch))
        assertEquals(MapHit.Pin(K.PIN_ASH_MARKET), MapHitTest.hit(scene, math, zoomed, ScreenPoint(tip.x + 150f, tip.y - 36f), touch))
    }

    @Test
    fun `routes then regions`() {
        assertEquals(MapHit.Route(K.ROUTE_GATE_TO_UPPER_GATE), tapAt(Point(0.20f, 0.34f)))
        assertEquals(MapHit.Region(K.REGION_LOWER_DISTRICT), tapAt(Point(0.50f, 0.58f)))
        assertNull(tapAt(Point(0.80f, 0.80f)))
    }

    @Test
    fun `handles by index`() {
        val handles = listOf(Point(0.1f, 0.1f), Point(0.3f, 0.4f))
        val s = math.toScreen(zoomed, handles[1])
        assertEquals(1, MapHitTest.handle(handles, math, zoomed, ScreenPoint(s.x + 30f, s.y), touch))
        assertNull(MapHitTest.handle(handles, math, zoomed, ScreenPoint(s.x + 300f, s.y), touch))
    }

    @Test
    fun `terrain lines and stamps are hit only in the detailed-terrain editor`() {
        // The drawn road runs through the region 하층 구역 here.
        val onRoad = math.toScreen(zoomed, Point(0.335f, 0.415f))
        assertEquals(MapHit.Region(K.REGION_LOWER_DISTRICT), MapHitTest.hit(scene, math, zoomed, onRoad, touch))
        assertEquals(MapHit.Line(K.drawnLine(1)), MapHitTest.hit(scene, math, zoomed, onRoad, touch, terrain = true))
        // The tower stamp sits by the 상층 관문 pin: pins win; without the pin the stamp is hit.
        val tower = math.toScreen(zoomed, Point(0.42f, 0.28f))
        assertEquals(MapHit.Pin(K.PIN_UPPER_GATE), MapHitTest.hit(scene, math, zoomed, tower, touch, terrain = true))
        assertEquals(MapHit.Stamp(K.stamp(4)), MapHitTest.hit(scene.copy(pins = emptyList()), math, zoomed, tower, touch, terrain = true))
        assertEquals(null, MapHitTest.hit(scene.copy(pins = emptyList(), regions = emptyList()), math, zoomed, tower, touch))
    }
}
