package io.github.lenticualrstone.civilight.feature.map.track

import io.github.lenticualrstone.civilight.model.Point
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackPathTest {
    private val a = Point(0.1f, 0.1f)
    private val b = Point(0.5f, 0.1f)
    private val c = Point(0.5f, 0.5f)

    @Test
    fun `a chip walks along the route that joins the stops, in either direction`() {
        val leg = TrackLine(listOf(a, b, c), label = null, focused = true, dashed = false)
        assertEquals(listOf(a, b, c), TrackPath.between(listOf(leg), a, c))
        assertEquals(listOf(c, b, a), TrackPath.between(listOf(leg), c, a))
    }

    @Test
    fun `with no leg between them the way is straight`() {
        val other = TrackLine(listOf(a, b), null, focused = false, dashed = true)
        assertEquals(listOf(a, c), TrackPath.between(listOf(other), a, c))
        assertEquals(listOf(a, c), TrackPath.between(emptyList(), a, c))
    }

    @Test
    fun `a point along a path is by length`() {
        val p = TrackPath.pointAt(listOf(a, b, c), 0.5f)
        assertEquals(0.5f, p.x, 1e-5f)
        assertEquals(0.1f, p.y, 1e-5f)
        assertEquals(a, TrackPath.pointAt(listOf(a, b, c), 0f))
        assertEquals(c, TrackPath.pointAt(listOf(a, b, c), 1f))
        val q = TrackPath.pointAt(listOf(a, b, c), 0.75f)
        assertEquals(0.5f, q.x, 1e-5f)
        assertEquals(0.3f, q.y, 1e-5f)
        assertEquals(a, TrackPath.pointAt(listOf(a), 0.6f))
    }
}
