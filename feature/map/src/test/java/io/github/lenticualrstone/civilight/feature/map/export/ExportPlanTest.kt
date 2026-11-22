package io.github.lenticualrstone.civilight.feature.map.export

import io.github.lenticualrstone.civilight.model.app.LargeMapExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-51 지도 내보내기 4K · 8K: output sizes and band reading of large bases. */
class ExportPlanTest {
    @Test
    fun `4K is a 3840 px long edge and 8K 7680 px`() {
        assertEquals(3840, ExportPlan.longEdge(LargeMapExport.K4))
        assertEquals(7680, ExportPlan.longEdge(LargeMapExport.K8))
        assertEquals(3840 to 2560, ExportPlan.size(1.5f, 3840))
        assertEquals(2560 to 3840, ExportPlan.size(2f / 3f, 3840))
        assertEquals(7680 to 7680, ExportPlan.size(Float.NaN, 7680))
    }

    @Test
    fun `a small base is read in one band, a large one in subsampled bands`() {
        assertEquals(listOf(Band(0, 2000, 1)), ExportPlan.bands(3000, 2000, 3840))
        val bands = ExportPlan.bands(16000, 12000, 3840)
        assertTrue(bands.size > 1)
        assertEquals(0, bands.first().top)
        assertEquals(12000, bands.last().bottom)
        assertTrue(bands.all { it.sample == bands.first().sample && it.sample >= 2 })
        bands.zipWithNext().forEach { (a, b) -> assertEquals(a.bottom, b.top) }
        assertTrue(ExportPlan.bands(0, 10, 100).isEmpty())
    }

    @Test
    fun `file names are safe and carry the size`() {
        assertEquals("하층_구역_지도-4K.png", ExportPlan.fileName("하층 구역 지도", LargeMapExport.K4))
        assertEquals("a_b-8K.png", ExportPlan.fileName("a/b", LargeMapExport.K8))
        assertEquals("map-4K.png", ExportPlan.fileName("  ", LargeMapExport.K4))
    }
}
