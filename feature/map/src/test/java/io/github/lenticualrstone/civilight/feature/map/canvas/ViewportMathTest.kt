package io.github.lenticualrstone.civilight.feature.map.canvas

import io.github.lenticualrstone.civilight.model.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewportMathTest {
    private val cover = ViewportMath(390f, 844f, 4f / 3f)

    @Test
    fun `zoom 1 fits the base inside the view`() {
        assertEquals(390f, cover.fitW, 0.01f)
        assertEquals(292.5f, cover.fitH, 0.01f)
        val inner = ViewportMath(800f, 800f, 2f)
        assertEquals(800f, inner.fitW, 0.01f)
        val tall = ViewportMath(800f, 400f, 1f)
        assertEquals(400f, tall.fitW, 0.01f)
        assertEquals(400f, tall.fitH, 0.01f)
    }

    @Test
    fun `screen and map points convert both ways`() {
        val v = Viewport(0.3f, 0.6f, 2.5f)
        val p = Point(0.42f, 0.37f)
        val back = cover.toMap(v, cover.toScreen(v, p))
        assertEquals(p.x, back.x, 1e-4f)
        assertEquals(p.y, back.y, 1e-4f)
        val centre = cover.toScreen(v, v.cx, v.cy)
        assertEquals(195f, centre.x, 0.01f)
        assertEquals(422f, centre.y, 0.01f)
    }

    @Test
    fun `pinch keeps the point under the fingers and clamps the zoom`() {
        val v = Viewport()
        val fingers = ScreenPoint(300f, 500f)
        val before = cover.toMap(v, fingers)
        val zoomed = cover.transform(v, fingers, 0f, 0f, 2f)
        assertEquals(2f, zoomed.zoom, 1e-4f)
        val after = cover.toMap(zoomed, fingers)
        assertEquals(before.x, after.x, 1e-3f)
        assertEquals(before.y, after.y, 1e-3f)
        assertEquals(Viewport.MAX_ZOOM, cover.transform(zoomed, fingers, 0f, 0f, 100f).zoom, 1e-4f)
        assertEquals(Viewport.MIN_ZOOM, cover.transform(zoomed, fingers, 0f, 0f, 0.01f).zoom, 1e-4f)
    }

    @Test
    fun `panning moves the centre opposite to the fingers and stays on the base`() {
        val v = Viewport(0.5f, 0.5f, 2f)
        val moved = cover.transform(v, ScreenPoint(195f, 422f), 78f, 0f, 1f)
        assertEquals(0.4f, moved.cx, 1e-4f)
        val far = cover.transform(v, ScreenPoint(195f, 422f), 10_000f, -10_000f, 1f)
        assertEquals(0f, far.cx, 0f)
        assertEquals(1f, far.cy, 0f)
    }

    @Test
    fun `zoom label and saved state`() {
        assertEquals(120, Viewport(zoom = 1.2f).percent)
        val v = Viewport(0.1f, 0.2f, 3f)
        assertEquals(v, Viewport.fromArray(v.toArray()))
        assertEquals(Viewport(), Viewport.fromArray(null))
        assertEquals(Viewport(0.55f, 0.35f, 2f), Viewport(zoom = 2f).centeredOn(Point(0.55f, 0.35f)))
    }

    @Test
    fun `a screen opens fitted, not at the fill zoom (QA 9)`() {
        val v = cover.resolve(Viewport.UNSET)
        assertEquals(1f, v.zoom, 0f)
        assertEquals(Viewport(0.5f, 0.5f, 1f), v)
        assertEquals(Viewport(), v)
        assertEquals(Viewport(0.2f, 0.3f, 2f), cover.resolve(Viewport(0.2f, 0.3f, 2f)))
        // the fill zoom is still there for callers that want it: 296 % on a tall phone view, the QA figure
        assertEquals(844f / 292.5f, cover.fillZoom(), 1e-3f)
        assertEquals(1f, ViewportMath(400f, 300f, 4f / 3f).fillZoom(), 1e-4f)
    }

    @Test
    fun `lerp moves the centre linearly and the zoom geometrically`() {
        val a = Viewport(0.2f, 0.4f, 1f)
        val b = Viewport(0.6f, 0.8f, 4f)
        assertEquals(a, Viewport.lerp(a, b, 0f))
        assertEquals(b, Viewport.lerp(a, b, 1f))
        val mid = Viewport.lerp(a, b, 0.5f)
        assertEquals(0.4f, mid.cx, 1e-5f)
        assertEquals(0.6f, mid.cy, 1e-5f)
        assertEquals(2f, mid.zoom, 1e-4f)
        assertEquals(b, Viewport.lerp(Viewport.UNSET, b, 0.3f))
    }

    @Test
    fun `a soft pinch may pass the limits and settles back on release`() {
        val v = Viewport(0.5f, 0.5f, 1f)
        val c = ScreenPoint(195f, 422f)
        val hard = cover.transform(v, c, 0f, 0f, 0.5f)
        assertEquals(Viewport.MIN_ZOOM, hard.zoom, 0f)
        val soft = cover.transform(v, c, 0f, 0f, 0.5f, soft = true)
        assertEquals(Viewport.SOFT_MIN_ZOOM, soft.zoom, 1e-5f)
        assertTrue(cover.overshoots(soft))
        assertEquals(Viewport.MIN_ZOOM, cover.clamp(soft).zoom, 0f)
        val high = cover.transform(Viewport(0.5f, 0.5f, 7f), c, 0f, 0f, 3f, soft = true)
        assertEquals(Viewport.SOFT_MAX_ZOOM, high.zoom, 1e-5f)
        assertEquals(Viewport.MAX_ZOOM, cover.clamp(high).zoom, 0f)
        assertFalse(cover.overshoots(Viewport(zoom = 8f)))
        assertEquals(cover.transform(v, c, 12f, -7f, 1.3f), cover.transform(v, c, 12f, -7f, 1.3f, soft = true))
    }

    @Test
    fun `taps outside the base are not on it`() {
        assertTrue(cover.onBase(Point(0f, 1f)))
        assertFalse(cover.onBase(cover.toMap(Viewport(), ScreenPoint(195f, 10f))))
    }

    @Test
    fun `visible rect of a zoomed view`() {
        val r = cover.visibleRect(Viewport(0.5f, 0.5f, 2f))
        assertEquals(0.25f, r[0], 1e-4f)
        assertEquals(0.75f, r[2], 1e-4f)
        assertEquals(0f, r[1], 0f)
        assertEquals(1f, r[3], 0f)
    }
}
