package io.github.lenticualrstone.civilight.feature.map.canvas

import io.github.lenticualrstone.civilight.model.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-44 `곡선 라벨`: glyphs centred along the curve, rotated to its tangent. */
class CurveGeometryTest {
    @Test
    fun `a straight path centres the text and keeps glyphs level`() {
        val path = listOf(ScreenPoint(0f, 0f), ScreenPoint(100f, 0f))
        val glyphs = CurveGeometry.placeAlong(path, listOf(10f, 10f))
        assertEquals(45f, glyphs[0].x, 1e-3f)
        assertEquals(55f, glyphs[1].x, 1e-3f)
        assertTrue(glyphs.all { it.angle == 0f && it.y == 0f })
    }

    @Test
    fun `a quadratic curve bends the glyphs`() {
        val path = CurveGeometry.sample(listOf(ScreenPoint(0f, 100f), ScreenPoint(50f, 0f), ScreenPoint(100f, 100f)))
        assertEquals(49, path.size)
        assertEquals(50f, path[24].x, 1e-3f)
        assertEquals(50f, path[24].y, 1e-3f)
        val glyphs = CurveGeometry.placeAlong(path, listOf(8f, 8f, 8f))
        assertTrue(glyphs.first().angle < 0f)
        assertTrue(glyphs.last().angle > 0f)
    }

    @Test
    fun `default curve is three points around the label`() {
        val c = CurveGeometry.defaultCurve(Point(0.5f, 0.5f))
        assertEquals(3, c.size)
        assertEquals(0.42f, c[0].x, 1e-4f)
        assertEquals(0.46f, c[1].y, 1e-4f)
        assertEquals(0.58f, c[2].x, 1e-4f)
    }
}
