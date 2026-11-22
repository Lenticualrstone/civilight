package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.HexGrid
import io.github.lenticualrstone.civilight.model.map.HexOrientation
import io.github.lenticualrstone.civilight.model.map.Terrain
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.open
import kotlin.math.sqrt

/** A hex cell: row and column, both 0-based. */
data class HexCell(val row: Int, val col: Int)

/** `고르기` info of one cell: `O09 | 숲 · 하층 구역 | 칸 24 km · 장소 없음`. */
data class HexCellInfo(val cell: HexCell, val label: String, val terrain: Terrain?, val regionName: String?, val placeName: String?, val text: String)

/**
 * Hex map editing (WLD-58): coordinates `O09` (row letter, two-digit column), painting one cell,
 * flood fill of connected cells of one terrain, cell info and `장소 만들기`. Pointy grids use odd
 * rows shifted right, flat grids odd columns shifted down.
 */
object HexMaps {
    /** Terrain names of the palette (`바다 · 평원 · 숲 · 언덕 · 산 · 사막 · 늪 · 설원`; generated `깊은 바다`, `봉우리`). */
    fun terrainName(t: Terrain): String = when (t) {
        Terrain.DEEP -> "깊은 바다"
        Terrain.SEA -> "바다"
        Terrain.PLAIN -> "평원"
        Terrain.FOREST -> "숲"
        Terrain.HILL -> "언덕"
        Terrain.MOUNTAIN -> "산"
        Terrain.PEAK -> "봉우리"
        Terrain.DESERT -> "사막"
        Terrain.SWAMP -> "늪"
        Terrain.SNOW -> "설원"
    }

    fun index(grid: HexGrid, cell: HexCell): Int = cell.row * grid.cols + cell.col
    fun cellOf(grid: HexGrid, index: Int): HexCell = HexCell(index / grid.cols, index % grid.cols)
    fun contains(grid: HexGrid, cell: HexCell): Boolean = cell.row in 0 until grid.rows && cell.col in 0 until grid.cols

    /** Row letters: A … Z, then AA, AB … */
    fun rowLetters(row: Int): String {
        var n = row
        val sb = StringBuilder()
        do {
            sb.insert(0, 'A' + n % 26)
            n = n / 26 - 1
        } while (n >= 0)
        return sb.toString()
    }

    /** `O09` for row 14, column 9. */
    fun label(cell: HexCell): String = rowLetters(cell.row) + cell.col.toString().padStart(2, '0')

    /** Coordinates are drawn on every [HexGrid.coordEvery]-th row and column (`3칸마다`). */
    fun showsLabel(grid: HexGrid, cell: HexCell): Boolean =
        grid.coordEvery > 0 && cell.row % grid.coordEvery == 0 && cell.col % grid.coordEvery == 0

    /** Terrain in effect: the author's paint over the generated cell. */
    fun terrainAt(grid: HexGrid, index: Int): Terrain? = (grid.overrides[index] ?: grid.cells.getOrNull(index))?.known

    fun neighbours(grid: HexGrid, cell: HexCell): List<HexCell> {
        val (r, c) = cell
        val pointy = grid.orientation.known != HexOrientation.FLAT
        val deltas = if (pointy) {
            if (r % 2 == 0) listOf(0 to -1, 0 to 1, -1 to -1, -1 to 0, 1 to -1, 1 to 0)
            else listOf(0 to -1, 0 to 1, -1 to 0, -1 to 1, 1 to 0, 1 to 1)
        } else {
            if (c % 2 == 0) listOf(-1 to 0, 1 to 0, -1 to -1, 0 to -1, -1 to 1, 0 to 1)
            else listOf(-1 to 0, 1 to 0, 0 to -1, 1 to -1, 0 to 1, 1 to 1)
        }
        return deltas.map { (dr, dc) -> HexCell(r + dr, c + dc) }.filter { contains(grid, it) }
    }

    /** Centre of a cell in the map's 0–1 space. */
    fun center(grid: HexGrid, cell: HexCell): Point {
        val s3 = sqrt(3.0)
        return if (grid.orientation.known != HexOrientation.FLAT) {
            val width = grid.cols * s3 + if (grid.rows > 1) s3 / 2 else 0.0
            val height = (grid.rows - 1) * 1.5 + 2.0
            val x = (cell.col + 0.5 + if (cell.row % 2 == 1) 0.5 else 0.0) * s3
            val y = 1.0 + cell.row * 1.5
            Point((x / width).toFloat(), (y / height).toFloat())
        } else {
            val width = (grid.cols - 1) * 1.5 + 2.0
            val height = grid.rows * s3 + if (grid.cols > 1) s3 / 2 else 0.0
            val x = 1.0 + cell.col * 1.5
            val y = (cell.row + 0.5 + if (cell.col % 2 == 1) 0.5 else 0.0) * s3
            Point((x / width).toFloat(), (y / height).toFloat())
        }
    }

    /** The cell whose centre is nearest to [p]. */
    fun cellAt(grid: HexGrid, p: Point): HexCell? =
        (0 until grid.rows * grid.cols).map { cellOf(grid, it) }.minByOrNull { MapGeometry.distance(center(grid, it), p) }

    private fun withGrid(doc: ProjectDoc, mapKey: Key, ctx: EditContext, f: (HexGrid) -> HexGrid): ProjectDoc? {
        val grid = doc.maps[mapKey]?.hex ?: return null
        return MapEdits.updateMap(doc, mapKey, ctx) { it.copy(hex = f(grid)) }
    }

    /** `한 칸`: paints one cell. */
    fun paint(doc: ProjectDoc, mapKey: Key, cell: HexCell, terrain: Terrain, ctx: EditContext): ProjectDoc? = withGrid(doc, mapKey, ctx) { g ->
        if (!contains(g, cell)) g else g.copy(overrides = g.overrides + (index(g, cell) to terrain.open()))
    }

    /**
     * `채우기`: the connected cells of the tapped cell's terrain become [terrain]. Returns the new
     * doc and the filled cells in spreading order (by distance from the tap, for the animation).
     */
    fun fill(doc: ProjectDoc, mapKey: Key, start: HexCell, terrain: Terrain, ctx: EditContext): Pair<ProjectDoc, List<HexCell>>? {
        val grid = doc.maps[mapKey]?.hex ?: return null
        if (!contains(grid, start)) return null
        val from = terrainAt(grid, index(grid, start))
        val order = ArrayList<HexCell>()
        val seen = HashSet<HexCell>()
        val queue = ArrayDeque(listOf(start))
        seen += start
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            order += cell
            for (n in neighbours(grid, cell)) {
                if (n !in seen && terrainAt(grid, index(grid, n)) == from) {
                    seen += n
                    queue += n
                }
            }
        }
        val updated = withGrid(doc, mapKey, ctx) { g -> g.copy(overrides = g.overrides + order.associate { index(g, it) to terrain.open() }) } ?: return null
        return updated to order
    }

    /** `고르기` info line of a cell. */
    fun info(doc: ProjectDoc, map: WorldMap, cell: HexCell): HexCellInfo? {
        val grid = map.hex ?: return null
        if (!contains(grid, cell)) return null
        val terrain = terrainAt(grid, index(grid, cell))
        val center = center(grid, cell)
        val region = map.regions.firstOrNull { MapGeometry.contains(it.vertices, center) }?.name
        val place = map.pins.filter { doc.liveness.isOverlayLive(map.key, it) && cellAt(grid, Point(it.x, it.y)) == cell }
            .firstNotNullOfOrNull { doc.index.place(it.placeKey)?.name }
        val label = label(cell)
        val terrainPart = listOfNotNull(terrain?.let(::terrainName), region).joinToString(Copy.SEP)
        val text = "$label | $terrainPart | 칸 ${Travel.number(grid.cellKm)} km${Copy.SEP}${place ?: "장소 없음"}"
        return HexCellInfo(cell, label, terrain, region, place, text)
    }

    /** `장소 만들기`: a new place with a pin at the cell centre. */
    fun createPlaceAt(doc: ProjectDoc, mapKey: Key, cell: HexCell, name: String, ctx: EditContext): MapEdit<io.github.lenticualrstone.civilight.model.map.Pin> {
        val grid = doc.maps[mapKey]?.hex ?: return MapEdit.Refused(MapRefusal.NO_MAP)
        return MapEdits.createPlaceWithPin(doc, mapKey, name, center(grid, cell), ctx)
    }

    /** `칸 크기`: also defines the map scale (one cell width = [km]). */
    fun setCellKm(doc: ProjectDoc, mapKey: Key, km: Double, ctx: EditContext): ProjectDoc? {
        val grid = doc.maps[mapKey]?.hex ?: return null
        val a = center(grid, HexCell(0, 0))
        val b = center(grid, HexCell(0, 1.coerceAtMost(grid.cols - 1)))
        val scale = if (a == b) null else MapScales.scale(a, b, km, io.github.lenticualrstone.civilight.model.map.ScaleUnit.KM)
        return MapEdits.updateMap(doc, mapKey, ctx) { it.copy(hex = grid.copy(cellKm = km), scale = scale ?: it.scale) }
    }
}
