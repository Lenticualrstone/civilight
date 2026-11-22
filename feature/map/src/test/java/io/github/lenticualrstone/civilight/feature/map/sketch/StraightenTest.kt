package io.github.lenticualrstone.civilight.feature.map.sketch

import io.github.lenticualrstone.civilight.model.Point
import org.junit.Assert.assertEquals
import org.junit.Test

/** WLD-39 `펴기`: corners, tap-to-place moves, output size. */
class StraightenTest {
    @Test
    fun `corners start inside the photo and move within it`() {
        val c = Straighten.defaultCorners()
        assertEquals(Point(0.08f, 0.08f), c[0])
        assertEquals(Point(0.92f, 0.92f), c[2])
        val moved = Straighten.move(c, 1, Point(1.4f, -0.2f))
        assertEquals(Point(1f, 0f), moved[1])
        assertEquals(c[0], moved[0])
        assertEquals(moved, Straighten.fromArray(Straighten.toArray(moved)))
        assertEquals(c, Straighten.fromArray(floatArrayOf(1f)))
    }

    @Test
    fun `output size takes the longer opposite edges and caps the long edge`() {
        val square = listOf(Point(0f, 0f), Point(1f, 0f), Point(1f, 1f), Point(0f, 1f))
        assertEquals(4000 to 3000, Straighten.outputSize(square, 4000, 3000))
        val skew = listOf(Point(0.1f, 0.1f), Point(0.6f, 0.1f), Point(0.5f, 0.9f), Point(0.1f, 0.9f))
        assertEquals(1000 to 1612, Straighten.outputSize(skew, 2000, 2000))
        val big = Straighten.outputSize(square, 8000, 6000)
        assertEquals(4096, big.first)
        assertEquals(3072, big.second)
    }
}
