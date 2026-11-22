package io.github.lenticualrstone.civilight.feature.map.hex

import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.domain.world.map.HexCell
import io.github.lenticualrstone.civilight.domain.world.map.HexEdits
import io.github.lenticualrstone.civilight.domain.world.map.HexMaps
import io.github.lenticualrstone.civilight.domain.world.map.MapReveal
import io.github.lenticualrstone.civilight.domain.world.map.Travel
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBase
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBases
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScene
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScenes
import io.github.lenticualrstone.civilight.feature.map.canvas.SceneMode
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.HexGrid
import io.github.lenticualrstone.civilight.model.map.HexOrientation
import io.github.lenticualrstone.civilight.model.map.Terrain
import io.github.lenticualrstone.civilight.model.position.LensPoint
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** `한 칸 · 줄 · 채우기 · 고르기` (WLD-58); `되돌리기` is an action of the same bar, not a mode. */
enum class HexTool(val label: String, val icon: IconId) {
    ONE(MapCopy.HEX_TOOL_ONE, IconId.Brush),
    LINE(MapCopy.HEX_TOOL_LINE, IconId.Line),
    FILL(MapCopy.HEX_TOOL_FILL, IconId.Fill),
    PICK(MapCopy.HEX_TOOL_PICK, IconId.Select),
}

/** A cell as saved state. */
@Serializable
data class CellState(val row: Int, val col: Int) {
    fun cell(): HexCell = HexCell(row, col)

    companion object {
        fun of(c: HexCell) = CellState(c.row, c.col)
    }
}

/** 칸 칠하기 state, saveable as JSON: tool, chosen terrain, the `고르기` cell, the `줄` start. */
@Serializable
data class HexPaintState(
    val tool: HexTool = HexTool.ONE,
    val terrain: Terrain = Terrain.FOREST,
    val selected: CellState? = null,
    val lineFrom: CellState? = null,
) {
    fun encode(): String = JSON.encodeToString(serializer(), this)

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }

        fun decode(text: String?): HexPaintState = text?.let { runCatching { JSON.decodeFromString(serializer(), it) }.getOrNull() } ?: HexPaintState()

        /** The palette `바다 · 평원 · 숲 · 언덕 · 산 · 사막 · 늪 · 설원`. */
        val PALETTE: List<Terrain> = listOf(Terrain.SEA, Terrain.PLAIN, Terrain.FOREST, Terrain.HILL, Terrain.MOUNTAIN, Terrain.DESERT, Terrain.SWAMP, Terrain.SNOW)

        /** `좌표 표시` choices (0 = none). */
        val COORD_CHOICES: List<Int> = listOf(0, 2, 3, 5)
    }
}

/** What a tap paints. */
sealed interface HexCommit {
    data class Paint(val cell: HexCell, val terrain: Terrain) : HexCommit
    data class Line(val cells: List<HexCell>, val terrain: Terrain) : HexCommit
    data class Fill(val cell: HexCell, val terrain: Terrain) : HexCommit
}

data class HexTapOutcome(val state: HexPaintState, val commit: HexCommit? = null)

/**
 * The 칸 칠하기 reducer (WLD-58). `한 칸` paints the tapped cell; `줄` takes two taps and paints the
 * straight line between them (Q-23 default); `채우기` floods the connected cells of the tapped
 * cell's terrain; `고르기` shows the cell info. Pure.
 */
object HexPaint {
    fun chooseTool(s: HexPaintState, tool: HexTool): HexPaintState = s.copy(tool = tool, lineFrom = null)

    fun chooseTerrain(s: HexPaintState, terrain: Terrain): HexPaintState = s.copy(terrain = terrain, tool = if (s.tool == HexTool.PICK) HexTool.ONE else s.tool)

    fun tap(s: HexPaintState, grid: HexGrid, p: Point): HexTapOutcome {
        val cell = HexMaps.cellAt(grid, p) ?: return HexTapOutcome(s)
        return when (s.tool) {
            HexTool.ONE -> HexTapOutcome(s.copy(selected = CellState.of(cell)), HexCommit.Paint(cell, s.terrain))
            HexTool.FILL -> HexTapOutcome(s.copy(selected = CellState.of(cell)), HexCommit.Fill(cell, s.terrain))
            HexTool.PICK -> HexTapOutcome(s.copy(selected = CellState.of(cell)))
            HexTool.LINE -> {
                val from = s.lineFrom?.cell()
                if (from == null) HexTapOutcome(s.copy(lineFrom = CellState.of(cell), selected = CellState.of(cell)))
                else HexTapOutcome(s.copy(lineFrom = null, selected = CellState.of(cell)), HexCommit.Line(HexEdits.line(grid, from, cell), s.terrain))
            }
        }
    }
}

/** The `고르기` card: `O09 | 숲 · 하층 구역 | 칸 24 km · 장소 없음` and whether `장소 만들기` applies. */
data class HexCellUi(val cell: HexCell, val text: String, val canCreatePlace: Boolean)

/** 칸 칠하기 screen state (boards `MapHex`, `MMapHexFill`). */
data class HexPaintUi(
    /** `세계 지도 · 육각`. */
    val small: String,
    val title: String,
    /** `24 km · 뾰족 · 32×24`. */
    val info: String,
    val base: MapBase,
    val scene: MapScene,
    val grid: HexGrid,
    val cell: HexCellUi?,
    /** `24 km ›`. */
    val cellSize: String,
    val orientation: Int,
    /** `3칸마다` or `없음`. */
    val coords: String,
)

object HexPaintUiBuilder {
    fun coordsLabel(every: Int): String = if (every <= 0) MapCopy.NONE else MapCopy.coordEvery(every)

    fun build(doc: ProjectDoc, mapKey: Key, lens: LensPoint, hiddenOpen: Boolean, s: HexPaintState): HexPaintUi? {
        val map = doc.maps[mapKey]?.takeIf { doc.liveness.isMapLive(it.key) } ?: return null
        val grid = map.hex ?: return null
        // Rule 19: hidden places and factions stay out of the cell card while locked.
        val shownMap = if (hiddenOpen) map else map.copy(
            pins = map.pins.filterNot { MapReveal.isMasked(doc, it, false) },
            regions = map.regions.filterNot { MapReveal.isMasked(doc, it, false) },
        )
        val cell = (if (s.tool == HexTool.PICK) s.selected else null)?.cell()?.let { c ->
            HexMaps.info(doc, shownMap, c)?.let { HexCellUi(c, it.text, it.placeName == null) }
        }
        return HexPaintUi(
            small = MapCopy.hexSmall(map.name),
            title = MapCopy.HEX_TITLE,
            info = HexEdits.infoLine(grid),
            base = MapBases.of(map),
            scene = MapScenes.build(doc, map, lens, hiddenOpen, SceneMode.VIEW),
            grid = grid,
            cell = cell,
            cellSize = MapCopy.km(Travel.number(grid.cellKm)),
            orientation = if (grid.orientation.known == HexOrientation.FLAT) 1 else 0,
            coords = coordsLabel(grid.coordEvery),
        )
    }
}
