package io.github.lenticualrstone.civilight.feature.map.hex

import io.github.lenticualrstone.civilight.domain.world.map.HexCell
import io.github.lenticualrstone.civilight.domain.world.map.HexMaps
import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.HexGrid
import io.github.lenticualrstone.civilight.model.map.Terrain
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 칸 칠하기 (WLD-58): the reducer, the screen state and the spec cases WLD-T64, WLD-T65. */
class HexPaintTest {
    private val grid32 = HexGrid(cols = 32, rows = 24, cells = List(32 * 24) { Terrain.PLAIN.open() })

    private fun docWith(grid: HexGrid): ProjectDoc {
        val doc = MapFx.doc()
        val map = doc.maps.getValue(K.MAP_WORLD).copy(hex = grid, pins = emptyList(), regions = emptyList())
        return doc.copy(maps = doc.maps + (map.key to map))
    }

    private fun tapAt(s: HexPaintState, grid: HexGrid, cell: HexCell) = HexPaint.tap(s, grid, HexMaps.center(grid, cell))

    @Test
    fun `한 칸 paints the tapped cell with the chosen terrain`() {
        val s = HexPaint.chooseTerrain(HexPaintState(), Terrain.DESERT)
        val out = tapAt(s, grid32, HexCell(3, 4))
        assertEquals(HexCommit.Paint(HexCell(3, 4), Terrain.DESERT), out.commit)
        assertEquals(CellState(3, 4), out.state.selected)
    }

    @Test
    fun `줄 takes two taps and paints the straight line between them`() {
        val s = HexPaint.chooseTool(HexPaintState(), HexTool.LINE)
        val first = tapAt(s, grid32, HexCell(5, 2))
        assertNull(first.commit)
        assertEquals(CellState(5, 2), first.state.lineFrom)
        val second = tapAt(first.state, grid32, HexCell(5, 6))
        val commit = second.commit as HexCommit.Line
        assertEquals((2..6).map { HexCell(5, it) }, commit.cells)
        assertNull(second.state.lineFrom)
        // Choosing another tool drops a half-made line.
        assertNull(HexPaint.chooseTool(first.state, HexTool.ONE).lineFrom)
    }

    @Test
    fun `WLD-T64 hex flood fill through the tool`() {
        // Row 1, columns 1–5 are forest, touching other forest only through plain.
        val cells = List(8 * 6) { i -> if (i / 8 == 1 && i % 8 in 1..5 || i / 8 == 4 && i % 8 == 2) Terrain.FOREST.open() else Terrain.PLAIN.open() }
        val grid = HexGrid(cols = 8, rows = 6, cells = cells)
        val doc = docWith(grid)
        val s = HexPaint.chooseTerrain(HexPaint.chooseTool(HexPaintState(), HexTool.FILL), Terrain.PLAIN)
        val commit = tapAt(s, grid, HexCell(1, 3)).commit as HexCommit.Fill
        val (filled, order) = HexMaps.fill(doc, K.MAP_WORLD, commit.cell, commit.terrain, MapFx.ctx())!!
        assertEquals((1..5).map { HexCell(1, it) }.toSet(), order.toSet())
        val after = filled.maps.getValue(K.MAP_WORLD).hex!!
        assertEquals(Terrain.FOREST, HexMaps.terrainAt(after, 4 * 8 + 2))
    }

    @Test
    fun `WLD-T65 고르기 shows the cell label and 장소 만들기`() {
        val doc = docWith(grid32)
        val s = tapAt(HexPaint.chooseTool(HexPaintState(), HexTool.PICK), grid32, HexCell(14, 9)).state
        val ui = HexPaintUiBuilder.build(doc, K.MAP_WORLD, LensPoint.Latest, false, s)!!
        val cell = ui.cell!!
        assertEquals("O09 | 평원 | 칸 24 km · 장소 없음", cell.text)
        assertTrue(cell.text.endsWith("칸 24 km · 장소 없음"))
        assertTrue(cell.canCreatePlace)
        assertEquals("세계 지도 · 육각", ui.small)
        assertEquals("칸 칠하기", ui.title)
        assertEquals("24 km · 뾰족 · 32×24", ui.info)
        assertEquals("24 km", ui.cellSize)
        assertEquals("3칸마다", ui.coords)
        assertEquals(0, ui.orientation)
        // Other tools show no cell card.
        assertNull(HexPaintUiBuilder.build(doc, K.MAP_WORLD, LensPoint.Latest, false, HexPaint.chooseTool(s, HexTool.ONE))!!.cell)
    }

    @Test
    fun `the palette, coordinates choices and saved state`() {
        assertEquals(listOf("바다", "평원", "숲", "언덕", "산", "사막", "늪", "설원"), HexPaintState.PALETTE.map(HexMaps::terrainName))
        assertEquals("없음", HexPaintUiBuilder.coordsLabel(0))
        val s = HexPaintState(HexTool.LINE, Terrain.SNOW, CellState(1, 2), CellState(3, 4))
        assertEquals(s, HexPaintState.decode(s.encode()))
        assertEquals(HexPaintState(), HexPaintState.decode("not json"))
        assertEquals(24.5, HexPaintViewModel.parseKm(" 24,5 ")!!, 1e-9)
        assertNull(HexPaintViewModel.parseKm("0"))
    }
}
