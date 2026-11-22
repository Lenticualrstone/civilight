package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.HexGrid
import io.github.lenticualrstone.civilight.model.map.HexOrientation
import io.github.lenticualrstone.civilight.model.map.Terrain
import io.github.lenticualrstone.civilight.model.open
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * More hex editing for 칸 칠하기 (WLD-58), next to [HexMaps]: the `줄` tool (Q-23 default: the
 * straight line of cells between two tapped cells), painting several cells in one step, and the
 * settings rows `방향 | 뾰족 · 평평` and `좌표 표시`.
 */
object HexEdits {
    private data class Cube(val x: Int, val y: Int, val z: Int)

    private fun toCube(grid: HexGrid, c: HexCell): Cube =
        if (grid.orientation.known != HexOrientation.FLAT) {
            val x = c.col - (c.row - (c.row and 1)) / 2
            Cube(x, -x - c.row, c.row)
        } else {
            val z = c.row - (c.col - (c.col and 1)) / 2
            Cube(c.col, -c.col - z, z)
        }

    private fun toCell(grid: HexGrid, c: Cube): HexCell =
        if (grid.orientation.known != HexOrientation.FLAT) HexCell(c.z, c.x + (c.z - (c.z and 1)) / 2)
        else HexCell(c.z + (c.x - (c.x and 1)) / 2, c.x)

    /** Steps between two cells. */
    fun distance(grid: HexGrid, a: HexCell, b: HexCell): Int {
        val p = toCube(grid, a)
        val q = toCube(grid, b)
        return max(abs(p.x - q.x), max(abs(p.y - q.y), abs(p.z - q.z)))
    }

    private fun round(x: Double, y: Double, z: Double): Cube {
        var rx = x.roundToInt()
        var ry = y.roundToInt()
        var rz = z.roundToInt()
        val dx = abs(rx - x)
        val dy = abs(ry - y)
        val dz = abs(rz - z)
        if (dx > dy && dx > dz) rx = -ry - rz else if (dy > dz) ry = -rx - rz else rz = -rx - ry
        return Cube(rx, ry, rz)
    }

    /** `줄`: every cell on the straight line from [a] to [b], both included, in order. */
    fun line(grid: HexGrid, a: HexCell, b: HexCell): List<HexCell> {
        val n = distance(grid, a, b)
        if (n == 0) return listOf(a)
        val p = toCube(grid, a)
        val q = toCube(grid, b)
        // A tiny nudge keeps ties on one side, so the line never zig-zags.
        val e = 1e-6
        return (0..n).map { i ->
            val t = i.toDouble() / n
            round(
                p.x + (q.x - p.x) * t + e,
                p.y + (q.y - p.y) * t + e,
                p.z + (q.z - p.z) * t - 2 * e,
            ).let { toCell(grid, it) }
        }.filter { HexMaps.contains(grid, it) }.distinct()
    }

    /** Paints [cells] in one step (the `줄` tool). Null when the map has no grid or no cell is inside it. */
    fun paintCells(doc: ProjectDoc, mapKey: Key, cells: List<HexCell>, terrain: Terrain, ctx: EditContext): ProjectDoc? {
        val grid = doc.maps[mapKey]?.hex ?: return null
        val inside = cells.filter { HexMaps.contains(grid, it) }
        if (inside.isEmpty()) return null
        return MapEdits.updateMap(doc, mapKey, ctx) { m ->
            m.copy(hex = grid.copy(overrides = grid.overrides + inside.associate { HexMaps.index(grid, it) to terrain.open() }))
        }
    }

    /**
     * `방향 | 뾰족 · 평평`: the same cells drawn the other way. The grid keeps its rows and columns and
     * the scale is set again from the cell size, because the layout of the cells changes.
     */
    fun setOrientation(doc: ProjectDoc, mapKey: Key, orientation: HexOrientation, ctx: EditContext): ProjectDoc? {
        val grid = doc.maps[mapKey]?.hex ?: return null
        if (grid.orientation.known == orientation) return null
        val turned = MapEdits.updateMap(doc, mapKey, ctx) { it.copy(hex = grid.copy(orientation = orientation.open())) } ?: return null
        return HexMaps.setCellKm(turned, mapKey, grid.cellKm, ctx)
    }

    /** `좌표 표시`: every [every]-th row and column (0 = none). */
    fun setCoordEvery(doc: ProjectDoc, mapKey: Key, every: Int, ctx: EditContext): ProjectDoc? {
        val grid = doc.maps[mapKey]?.hex ?: return null
        if (every < 0 || grid.coordEvery == every) return null
        return MapEdits.updateMap(doc, mapKey, ctx) { it.copy(hex = grid.copy(coordEvery = every)) }
    }

    /** `24 km · 뾰족 · 32×24` (the info line under the title). */
    fun infoLine(grid: HexGrid): String {
        val dir = if (grid.orientation.known == HexOrientation.FLAT) "평평" else "뾰족"
        return "${Travel.number(grid.cellKm)} km · $dir · ${grid.cols}×${grid.rows}"
    }
}
