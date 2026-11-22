package io.github.lenticualrstone.civilight.feature.map.canvas

import io.github.lenticualrstone.civilight.domain.world.gen.MapGenerator
import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.model.map.MapStyle
import io.github.lenticualrstone.civilight.model.map.PolyCell
import io.github.lenticualrstone.civilight.model.map.PolyMap
import io.github.lenticualrstone.civilight.model.map.PolyProvince
import io.github.lenticualrstone.civilight.model.map.StampKind
import io.github.lenticualrstone.civilight.model.map.Terrain
import io.github.lenticualrstone.civilight.model.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Generated-map pictures (WLD-57), polygon cells (WLD-59) and stamp glyphs (WLD-60). */
class GenRasterTest {
    private val palette = BasePalette(
        dark = true,
        art = listOf(0xFF3B4A66.toInt(), 0xFFA0674C.toInt(), 0xFFD9A27F.toInt(), 0xFFC98A62.toInt(), 0xFF2B3242.toInt()),
        bg = 0xFF0B0C0E.toInt(), tint = 0xFF1A1B1E.toInt(), ink = 0xFFEDEDED.toInt(), ink2 = 0xFFBBBBBB.toInt(), ink3 = 0xFF888888.toInt(),
    )
    private val world = MapGenerator.storedWorld(MapFx.doc().maps.getValue(K.MAP_WORLD))!!

    private fun blueDominant(c: Int): Boolean {
        val r = (c ushr 16) and 0xFF; val g = (c ushr 8) and 0xFF; val b = c and 0xFF
        return b > r && b > g
    }

    @Test
    fun `WLD-57 색면 keeps blue hues off the land and follows the theme`() {
        val soft = GenRaster.world(world, MapStyle.SOFT, palette)
        assertEquals(world.w * world.h, soft.pixels.size)
        val land = (0 until world.w * world.h).filter { world.isLand(it) }
        assertTrue(land.isNotEmpty())
        land.forEach { assertFalse("land cell $it is blue", blueDominant(soft.pixels[it])) }
        val light = GenRaster.world(world, MapStyle.SOFT, palette.copy(dark = false, art = palette.art.map { GenRaster.mix(it, -1, 0.5f) }))
        assertNotEquals(soft.pixels.toList(), light.pixels.toList())
    }

    @Test
    fun `WLD-57 the four styles draw four different pictures, 색 uses the fixed terrain colours`() {
        val pictures = MapStyle.entries.map { GenRaster.world(world, it, palette).pixels.toList() }
        assertEquals(4, pictures.distinct().size)
        val color = GenRaster.world(world, MapStyle.COLOR, palette)
        assertEquals(GenRaster.terrainArgb(world.terrain(0)), color.pixels[0])
        assertEquals(GenRaster.sepia(GenRaster.terrainArgb(Terrain.PLAIN)), GenRaster.sepia(GenRaster.terrainArgb(Terrain.PLAIN)))
    }

    @Test
    fun `WLD-57 등고선 adds contour segments and detailed terrain keeps its own rivers and roads`() {
        assertTrue(GenRaster.contourSegments(world).isNotEmpty())
        val withContours = GenRaster.lines(world, MapStyle.SOFT, palette, contours = true, worldLines = true, hex = false)
        val without = GenRaster.lines(world, MapStyle.SOFT, palette, contours = false, worldLines = true, hex = false)
        assertTrue(withContours.size > without.size)
        val own = GenRaster.lines(world, MapStyle.SOFT, palette, contours = false, worldLines = false, hex = false)
        assertEquals(without.size - world.rivers.size - world.roads.size, own.size)
    }

    @Test
    fun `WLD-59 polygon cells take their province colour, borders are darker`() {
        val poly = PolyMap(
            cells = listOf(PolyCell(Point(0.25f, 0.5f), true), PolyCell(Point(0.75f, 0.5f), true)),
            cellProvince = mapOf(0 to "a", 1 to "b"),
            provinces = listOf(PolyProvince("a", "", "map-art-2"), PolyProvince("b", "", "#112233")),
        )
        val owner = IntArray(40 * 20)
        val r = GenRaster.poly(poly, null, MapStyle.SOFT, palette, 40, 20, owner)
        assertEquals(palette.art[1], r.pixels[5 * 40 + 2])
        assertEquals(0xFF112233.toInt(), r.pixels[5 * 40 + 37])
        assertEquals(0, owner[0])
        assertEquals(1, owner[39])
        assertNotEquals(palette.art[1], r.pixels[5 * 40 + 19])
        assertEquals(1, GenRaster.cellAt(poly, 0.9f, 0.1f))
    }

    @Test
    fun `WLD-60 every stamp has a glyph and sizes grow`() {
        StampKind.entries.forEach { k -> assertTrue(StampGlyphs.strokes(k).all { it.size >= 2 }) }
        assertTrue(StampGlyphs.radius(1) < StampGlyphs.radius(2) && StampGlyphs.radius(2) < StampGlyphs.radius(3))
        val turned = StampGlyphs.place(listOf(Point(0f, -1f)), 10f, 10f, 2f, 90)
        assertEquals(12f, turned[0].first, 1e-4f)
        assertEquals(10f, turned[0].second, 1e-4f)
    }
}
