package io.github.lenticualrstone.civilight.feature.map.canvas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-38 large images: overview plus subsampled tiles; never a full decode above 4096 px. */
class TilePlanTest {
    @Test
    fun `images above the threshold are tiled`() {
        assertFalse(TilePlan.needsTiling(4096, 3000))
        assertTrue(TilePlan.needsTiling(8000, 6000))
        assertTrue(TilePlan.needsTiling(3000, 4097))
    }

    @Test
    fun `overview sample brings the long edge under the limit`() {
        assertEquals(1, TilePlan.sampleToFit(2048, 2048))
        assertEquals(4, TilePlan.sampleToFit(8000, 2048))
        assertEquals(8, TilePlan.sampleToFit(16_384, 2048))
    }

    @Test
    fun `tile sample follows the zoom`() {
        assertEquals(1, TilePlan.sampleForScale(1f))
        assertEquals(1, TilePlan.sampleForScale(0.6f))
        assertEquals(2, TilePlan.sampleForScale(0.5f))
        assertEquals(4, TilePlan.sampleForScale(0.2f))
        assertEquals(1, TilePlan.sampleForScale(0f))
    }

    @Test
    fun `tiles cover the visible part on an aligned grid`() {
        val tiles = TilePlan.tiles(8000, 6000, 1000, 1000, 2100, 1600, 2)
        assertEquals(listOf(0, 1024, 2048), tiles.map { it.left }.distinct())
        assertTrue(tiles.all { it.sample == 2 && (it.right - it.left) <= 1024 })
        assertEquals(1024, tiles.first().right)
        assertTrue(tiles.any { it.left <= 1000 && it.right >= 1000 && it.top <= 1000 && it.bottom >= 1000 })
        assertEquals(emptyList<Tile>(), TilePlan.tiles(100, 100, 50, 50, 50, 60, 1))
        val edge = TilePlan.tiles(1000, 700, 0, 0, 1000, 700, 1)
        assertEquals(1000, edge.maxOf { it.right })
        assertEquals(700, edge.maxOf { it.bottom })
    }
}
