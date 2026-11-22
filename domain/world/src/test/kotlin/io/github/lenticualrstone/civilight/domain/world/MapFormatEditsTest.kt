package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.K
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.ctx
import io.github.lenticualrstone.civilight.domain.world.map.DetailEdits
import io.github.lenticualrstone.civilight.domain.world.map.HexCell
import io.github.lenticualrstone.civilight.domain.world.map.HexEdits
import io.github.lenticualrstone.civilight.domain.world.map.HexMaps
import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.map.AttachedLayer
import io.github.lenticualrstone.civilight.model.map.DrawnLineKind
import io.github.lenticualrstone.civilight.model.map.HexGrid
import io.github.lenticualrstone.civilight.model.map.HexOrientation
import io.github.lenticualrstone.civilight.model.map.LineOrigin
import io.github.lenticualrstone.civilight.model.map.StampKind
import io.github.lenticualrstone.civilight.model.map.Terrain
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Format editing helpers of the map lane (WLD-58 hex, WLD-60 detailed terrain). */
class MapFormatEditsTest {
    private val doc = ExampleProject.build()

    // --- WLD-58 ---

    @Test
    fun `WLD-58 a hex line runs cell by cell between the two taps`() {
        val grid = HexGrid(cols = 32, rows = 24)
        val row = HexEdits.line(grid, HexCell(5, 2), HexCell(5, 7))
        assertEquals((2..7).map { HexCell(5, it) }, row)
        val slant = HexEdits.line(grid, HexCell(0, 0), HexCell(6, 3))
        assertEquals(HexCell(0, 0), slant.first())
        assertEquals(HexCell(6, 3), slant.last())
        assertEquals(HexEdits.distance(grid, HexCell(0, 0), HexCell(6, 3)) + 1, slant.size)
        // Neighbouring cells only: the line has no gaps.
        slant.zipWithNext().forEach { (a, b) -> assertEquals(1, HexEdits.distance(grid, a, b)) }
        assertEquals(listOf(HexCell(3, 3)), HexEdits.line(grid, HexCell(3, 3), HexCell(3, 3)))
    }

    @Test
    fun `WLD-58 flat grids draw lines too`() {
        val grid = HexGrid(cols = 10, rows = 10, orientation = HexOrientation.FLAT.open())
        val line = HexEdits.line(grid, HexCell(1, 1), HexCell(6, 5))
        line.zipWithNext().forEach { (a, b) -> assertEquals(1, HexEdits.distance(grid, a, b)) }
        assertEquals(HexCell(6, 5), line.last())
    }

    @Test
    fun `WLD-58 painting a line, orientation and coordinates are one edit each`() {
        val mapKey = K.MAP_WORLD
        val grid = doc.maps.getValue(mapKey).hex!!
        val cells = HexEdits.line(grid, HexCell(0, 0), HexCell(0, 3))
        val painted = HexEdits.paintCells(doc, mapKey, cells, Terrain.SNOW, ctx())!!
        val after = painted.maps.getValue(mapKey).hex!!
        cells.forEach { assertEquals(Terrain.SNOW, HexMaps.terrainAt(after, HexMaps.index(after, it))) }
        assertNull(HexEdits.paintCells(doc, mapKey, listOf(HexCell(99, 99)), Terrain.SNOW, ctx()))

        val flat = HexEdits.setOrientation(doc, mapKey, HexOrientation.FLAT, ctx())!!
        assertEquals(HexOrientation.FLAT, flat.maps.getValue(mapKey).hex!!.orientation.known)
        assertNotNull(flat.maps.getValue(mapKey).scale)
        assertNull(HexEdits.setOrientation(flat, mapKey, HexOrientation.FLAT, ctx()))

        val none = HexEdits.setCoordEvery(doc, mapKey, 0, ctx())!!
        assertEquals(0, none.maps.getValue(mapKey).hex!!.coordEvery)
        assertEquals("24 km · 뾰족 · 32×24", HexEdits.infoLine(HexGrid()))
    }

    // --- WLD-60 ---

    @Test
    fun `WLD-60 lines the author draws or moves are flagged for the re-roll guard`() {
        val mapKey = K.MAP_LOWER_DISTRICT
        val added = DetailEdits.addLine(doc, mapKey, DrawnLineKind.RIVER, listOf(Point(0.1f, 0.1f), Point(0.2f, 0.3f)), ctx())
        added as MapEdit.Applied
        assertEquals(LineOrigin.AUTHOR, added.value.origin.known)
        assertTrue(added.value.editedByAuthor)
        assertTrue(DetailEdits.addLine(doc, mapKey, DrawnLineKind.ROAD, listOf(Point(0.1f, 0.1f)), ctx()) is MapEdit.Refused)

        val line = doc.maps.getValue(mapKey).drawnLines.first()
        val moved = DetailEdits.moveLinePoint(doc, mapKey, line.key, 0, Point(0.5f, 0.5f), ctx())!!
        val after = moved.maps.getValue(mapKey).drawnLines.first { it.key == line.key }
        assertTrue(after.editedByAuthor)
        assertEquals(Point(0.5f, 0.5f), after.points[0])
        assertNull(DetailEdits.moveLinePoint(doc, mapKey, line.key, 99, Point(0.5f, 0.5f), ctx()))
    }

    @Test
    fun `WLD-60 built stamps on a river attach to it, terrain stamps never do`() {
        val mapKey = K.MAP_LOWER_DISTRICT
        val drawn = (DetailEdits.addLine(doc, mapKey, DrawnLineKind.RIVER, listOf(Point(0.6f, 0.1f), Point(0.6f, 0.9f)), ctx()) as MapEdit.Applied).doc
        val bridge = DetailEdits.placeStamp(drawn, mapKey, StampKind.BRIDGE, Point(0.605f, 0.5f), ctx()) as MapEdit.Applied
        assertEquals(AttachedLayer.RIVER, bridge.value.attachedLayer.known)
        assertEquals(AttachedLayer.RIVER, bridge.doc.maps.getValue(mapKey).stamps.first { it.key == bridge.value.key }.attachedLayer.known)
        val hill = DetailEdits.placeStamp(drawn, mapKey, StampKind.HILL, Point(0.605f, 0.5f), ctx()) as MapEdit.Applied
        assertEquals(AttachedLayer.NONE, hill.value.attachedLayer.known)
        val far = DetailEdits.moveStamp(bridge.doc, mapKey, bridge.value.key, Point(0.9f, 0.9f), ctx())!!
        val stamp = far.maps.getValue(mapKey).stamps.first { it.key == bridge.value.key }
        assertEquals(AttachedLayer.NONE, stamp.attachedLayer.known)
        assertEquals(0.9f, stamp.x, 1e-6f)
    }

    @Test
    fun `WLD-60 the stamp chip reads kind, size, rotation and place`() {
        val mapKey = K.MAP_LOWER_DISTRICT
        val placed = MapEdits.placeStamp(doc, mapKey, StampKind.CASTLE, Point(0.3f, 0.3f), ctx(), placeKey = K.PLACE_SOCIETY_GATE) as MapEdit.Applied
        assertEquals("성 · 크기 2 · 0° · 장소 ‘결사의 문’", DetailEdits.stampChip(placed.doc, placed.value))
        assertEquals(12, StampKind.entries.map(DetailEdits::stampName).distinct().size)
    }
}
