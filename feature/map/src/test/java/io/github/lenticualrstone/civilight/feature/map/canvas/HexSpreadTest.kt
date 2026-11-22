package io.github.lenticualrstone.civilight.feature.map.canvas

import io.github.lenticualrstone.civilight.model.Point
import org.junit.Assert.assertEquals
import org.junit.Test

class HexSpreadTest {
    @Test
    fun `changed cells are the ones whose value differs`() {
        assertEquals(listOf(1, 3), HexSpread.changed(listOf("a", "b", "c", "d"), listOf("a", "x", "c", "y")))
        assertEquals(emptyList<Int>(), HexSpread.changed(listOf("a"), listOf("a", "b")))
        assertEquals(emptyList<Int>(), HexSpread.changed(listOf("a", "b"), listOf("a", "b")))
    }

    @Test
    fun `a fill starts at the middle and spreads outwards`() {
        val centres = listOf(Point(0.4f, 0.5f), Point(0.5f, 0.5f), Point(0.6f, 0.5f))
        val starts = HexSpread.starts(listOf(10, 11, 12), centres)
        assertEquals(0f, starts.getValue(11), 1e-5f)
        assertEquals(1f, starts.getValue(10), 1e-5f)
        assertEquals(1f, starts.getValue(12), 1e-5f)
    }

    @Test
    fun `one painted cell has no spread`() {
        assertEquals(mapOf(7 to 0f), HexSpread.starts(listOf(7), listOf(Point(0.2f, 0.2f))))
        assertEquals(emptyMap<Int, Float>(), HexSpread.starts(emptyList(), emptyList()))
    }

    @Test
    fun `every cell is done by the end and the last starts late`() {
        assertEquals(0f, HexSpread.local(0f, 0f), 0f)
        assertEquals(1f, HexSpread.local(HexSpread.WINDOW, 0f), 1e-5f)
        assertEquals(0f, HexSpread.local(0.5f, 1f), 0f)
        assertEquals(1f, HexSpread.local(1f, 1f), 1e-5f)
        assertEquals(1f, HexSpread.local(1f, 0.3f), 0f)
    }
}
