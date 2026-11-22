package io.github.lenticualrstone.civilight.feature.map.poly

import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.domain.world.map.PolyMaps
import io.github.lenticualrstone.civilight.domain.world.item.RefLabel
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.GenRaster
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBase
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBases
import io.github.lenticualrstone.civilight.feature.map.canvas.MapColors
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScene
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScenes
import io.github.lenticualrstone.civilight.feature.map.canvas.SceneMode
import io.github.lenticualrstone.civilight.feature.map.edit.EditState
import io.github.lenticualrstone.civilight.feature.map.edit.LayerRowUi
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.PolyMap
import io.github.lenticualrstone.civilight.model.position.LensPoint
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** `고르기 · 칸 넣기 · 합치기 · 나누기 · 땅 · 바다 · 레이어` (WLD-59, board `MapPoly`). */
enum class PolyTool(val label: String, val icon: IconId) {
    PICK(MapCopy.POLY_TOOL_PICK, IconId.Select),
    ASSIGN(MapCopy.POLY_TOOL_ASSIGN, IconId.Brush),
    MERGE(MapCopy.POLY_TOOL_MERGE, IconId.Merge),
    SPLIT(MapCopy.POLY_TOOL_SPLIT, IconId.Split),
    LAND(MapCopy.POLY_TOOL_LAND, IconId.Area),
    LAYER(MapCopy.TOOL_LAYER, IconId.Layers),
}

/**
 * 다각형 · 지역 나누기 state, saveable as JSON: the tool, the selected province, the cells chosen
 * for `나누기`, how many cells `칸 넣기` added in a row (the floating `칸 1 → 하층 구역`) and the
 * side panel tab (0 `지역`, 1 `레이어`).
 */
@Serializable
data class PolyState(
    val tool: PolyTool = PolyTool.PICK,
    val selected: Key? = null,
    val split: List<Int> = emptyList(),
    val assigned: Int = 0,
    val panelTab: Int = 0,
) {
    fun encode(): String = JSON.encodeToString(serializer(), this)

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }

        fun decode(text: String?): PolyState = text?.let { runCatching { JSON.decodeFromString(serializer(), it) }.getOrNull() } ?: PolyState()
    }
}

/** What a tap writes. */
sealed interface PolyCommit {
    data class Assign(val cell: Int, val provinceKey: Key) : PolyCommit
    data class Merge(val from: Key, val into: Key) : PolyCommit
    data class ToggleLand(val cell: Int) : PolyCommit
}

data class PolyTapOutcome(val state: PolyState, val commit: PolyCommit? = null)

/**
 * The reducer of 다각형 편집 (WLD-59). `고르기` selects the province under the tap; `칸 넣기`
 * gives the tapped land cell to the selected province; `합치기` merges the tapped province into the
 * selected one; `나누기` picks cells of the selected province for a new one (`[PROPOSED]`); `땅 ·
 * 바다` flips a cell. Pure.
 */
object PolyEditor {
    fun provinceAt(poly: PolyMap, cell: Int): Key? = if (poly.cells.getOrNull(cell)?.land == true) poly.cellProvince[cell] else null

    fun chooseTool(s: PolyState, tool: PolyTool): PolyState =
        s.copy(tool = tool, split = emptyList(), assigned = 0, panelTab = if (tool == PolyTool.LAYER) 1 else if (s.panelTab == 1) 0 else s.panelTab)

    fun select(s: PolyState, key: Key?): PolyState = s.copy(selected = key, split = emptyList(), assigned = 0, panelTab = 0)

    fun tap(s: PolyState, poly: PolyMap, p: Point): PolyTapOutcome {
        val cell = GenRaster.cellAt(poly, p.x, p.y) ?: return PolyTapOutcome(s)
        val province = provinceAt(poly, cell)
        val selected = s.selected
        return when (s.tool) {
            PolyTool.PICK, PolyTool.LAYER -> PolyTapOutcome(s.copy(selected = province, split = emptyList(), assigned = 0))
            PolyTool.ASSIGN -> when {
                selected == null || poly.cells[cell].land.not() || province == selected -> PolyTapOutcome(s)
                else -> PolyTapOutcome(s.copy(assigned = s.assigned + 1), PolyCommit.Assign(cell, selected))
            }
            PolyTool.MERGE -> when {
                selected == null -> PolyTapOutcome(s.copy(selected = province))
                province == null || province == selected -> PolyTapOutcome(s)
                else -> PolyTapOutcome(s, PolyCommit.Merge(province, selected))
            }
            PolyTool.SPLIT -> when {
                selected == null -> PolyTapOutcome(s.copy(selected = province))
                province != selected -> PolyTapOutcome(s)
                cell in s.split -> PolyTapOutcome(s.copy(split = s.split - cell))
                else -> PolyTapOutcome(s.copy(split = s.split + cell))
            }
            PolyTool.LAND -> PolyTapOutcome(s, PolyCommit.ToggleLand(cell))
        }
    }

    /** After `나누기`: the new province is selected. */
    fun splitDone(s: PolyState, newKey: Key?): PolyState = s.copy(split = emptyList(), selected = newKey ?: s.selected)
}

/** A province row `<이름> | 칸 N`. */
data class ProvinceRowUi(val key: Key, val name: String, val cells: String, val color: String, val selected: Boolean)

/** The selected province's properties: 이름 · 색 · 연결 세력. */
data class ProvincePropsUi(val key: Key, val name: String, val color: String, val faction: String, val cellCount: Int)

/** 다각형 · 지역 나누기 (WLD-59). */
data class PolyUi(
    /** `세계 지도 › 지방`. */
    val small: String,
    val title: String,
    val base: MapBase,
    val scene: MapScene,
    val poly: PolyMap,
    val rows: List<ProvinceRowUi>,
    val props: ProvincePropsUi?,
    /** `칸 1 → 하층 구역` while `칸 넣기` runs. */
    val assignChip: String?,
    /** `칸 N` of the cells chosen for `나누기`, and whether `나누기` can run. */
    val splitBar: Pair<String, Boolean>?,
    /** Sites of the selected province's cells and of the cells chosen for `나누기` (0–1). */
    val selectedSites: List<Point>,
    val splitSites: List<Point>,
    val layers: List<LayerRowUi>,
)

object PolyUiBuilder {
    /** The province's name, or `지역 N` for a generated province without one. */
    fun nameOf(poly: PolyMap, key: Key): String {
        val i = poly.provinces.indexOfFirst { it.key == key }
        val p = poly.provinces.getOrNull(i) ?: return ""
        return p.name.ifBlank { MapCopy.defaultProvinceName(i + 1) }
    }

    /** The colour a new province gets: the next `map-art` token. */
    fun nextColor(poly: PolyMap): String = MapColors.REGION_TOKENS[poly.provinces.size % MapColors.REGION_TOKENS.size]

    fun build(doc: ProjectDoc, mapKey: Key, lens: LensPoint, hiddenOpen: Boolean, s: PolyState): PolyUi? {
        val map = doc.maps[mapKey]?.takeIf { doc.liveness.isMapLive(it.key) } ?: return null
        val poly = map.poly ?: return null
        val selected = s.selected?.takeIf { k -> poly.provinces.any { it.key == k } }
        val rows = poly.provinces.map { p -> ProvinceRowUi(p.key, nameOf(poly, p.key), MapCopy.cells(PolyMaps.cellCount(poly, p.key)), p.color, p.key == selected) }
        val props = selected?.let { k ->
            val p = poly.provinces.first { it.key == k }
            val hiddenFaction = !hiddenOpen && p.factionKey?.let { doc.index.faction(it)?.hidden } == true
            val faction = if (hiddenFaction) RefLabel.Hidden().text else EditState.factionLabel(doc, p.factionKey)
            ProvincePropsUi(k, nameOf(poly, k), p.color, faction, PolyMaps.cellCount(poly, k))
        }
        val selectedCells = poly.cellProvince.filterValues { it == selected }.keys.filter { poly.cells.getOrNull(it)?.land == true }
        return PolyUi(
            small = MapCopy.crumbs(listOf(map.name, MapCopy.POLY_CRUMB)),
            title = MapCopy.POLY_TITLE,
            base = MapBases.of(map),
            scene = MapScenes.build(doc, map, lens, hiddenOpen, SceneMode.VIEW),
            poly = poly,
            rows = rows,
            props = props,
            assignChip = if (s.tool == PolyTool.ASSIGN && props != null && s.assigned > 0) MapCopy.assigned(s.assigned, props.name) else null,
            splitBar = if (s.tool == PolyTool.SPLIT && props != null) MapCopy.cells(s.split.size) to (s.split.isNotEmpty() && s.split.size < props.cellCount) else null,
            selectedSites = selectedCells.map { poly.cells[it].site },
            splitSites = s.split.mapNotNull { poly.cells.getOrNull(it)?.site },
            layers = EditState.layerRows(doc, map),
        )
    }
}
