package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.domain.world.map.RegionDraft
import io.github.lenticualrstone.civilight.domain.world.map.RouteDraft
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapColors
import io.github.lenticualrstone.civilight.feature.map.canvas.MapHit
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.map.RoutePoint
import io.github.lenticualrstone.civilight.model.map.WorldMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The base tool set `이동 · 핀 · 영역 · 경로 · 라벨 · 레이어` (WLD-40, DS-55). Only the chosen tool acts on taps. */
enum class EditTool(val label: String, val icon: IconId) {
    MOVE(MapCopy.TOOL_MOVE, IconId.Move),
    PIN(MapCopy.TOOL_PIN, IconId.Pin),
    REGION(MapCopy.TOOL_REGION, IconId.Area),
    ROUTE(MapCopy.TOOL_ROUTE, IconId.Route),
    LABEL(MapCopy.TOOL_LABEL, IconId.Label),
    LAYER(MapCopy.TOOL_LAYER, IconId.Layers),
}

/** `새 영역 · 그리는 중`: the vertices so far and the properties chosen while drawing (WLD-42). */
@Serializable
data class RegionDraftState(
    val vertices: List<Point> = emptyList(),
    val name: String = "",
    val kind: String = MapCopy.REGION_KINDS.first(),
    val factionKey: Key? = null,
    /** `보이는 시점` as the unit it starts at. */
    val revealUnitKey: Key? = null,
    val layerKey: Key? = null,
    val color: String = MapColors.REGION_TOKENS.first(),
) {
    val draft: RegionDraft get() = RegionDraft(vertices)
}

/** A route point as saved state (`{x, y}` or `{pinKey}`). */
@Serializable
data class RoutePointState(val x: Float? = null, val y: Float? = null, val pinKey: Key? = null) {
    fun toModel(): RoutePoint = if (pinKey != null) RoutePoint.pin(pinKey) else RoutePoint.at(x ?: 0f, y ?: 0f)

    companion object {
        fun of(p: RoutePoint) = RoutePointState(p.x, p.y, p.pinKey)
    }
}

/** What a tap asks the screen to open. */
enum class EditorEffect {
    /** `핀 · 장소 고르기` for the tapped spot. */
    PICK_PLACE,

    /** Label text for the tapped spot. */
    LABEL_TEXT,
}

/** A finished tap: the next state, an effect to open, and a commit for the view model. */
data class TapOutcome(val state: EditorState, val effect: EditorEffect? = null, val commit: EditorCommit? = null)

/** Edits a tap commits at once (each one undo step). */
sealed interface EditorCommit {
    data class MoveVertex(val regionKey: Key, val index: Int, val to: Point) : EditorCommit
    data class MoveCurveHandle(val labelKey: Key, val index: Int, val to: Point) : EditorCommit
}

/**
 * The map editor's state (WLD-40 … 44) as one saveable value, and its reducer. Drafts survive a
 * fold or process death (WLD-37: `in edit mode the tool bar and in-progress vertices stay`).
 * Moving uses visible tap-to-place: `핀 옮기기` (then tap, `완료`), a chosen vertex or curve handle
 * (then tap). There are no hidden gestures and no freehand drawing.
 */
@Serializable
data class EditorState(
    val tool: EditTool = EditTool.MOVE,
    /** The selected overlay (properties in `속성`). */
    val selected: Key? = null,
    val region: RegionDraftState? = null,
    val route: List<RoutePointState>? = null,
    /** The tapped spot waiting for `핀 · 장소 고르기`. */
    val pendingPin: Point? = null,
    /** The tapped spot waiting for label text. */
    val pendingLabel: Point? = null,
    /** `핀 옮기기`: the pin and its new spot before `완료`. */
    val movingPin: Key? = null,
    val movingTo: Point? = null,
    /** The chosen vertex of the selected region, placed by the next tap. */
    val vertex: Int? = null,
    /** The chosen curve handle of the selected label, placed by the next tap. */
    val handle: Int? = null,
    /** Inner side panel tab: 0 `속성`, 1 `레이어`, 2 `목록`. */
    val panelTab: Int = 0,
) {
    val routeDraft: RouteDraft get() = RouteDraft(route.orEmpty().map { it.toModel() })

    fun encode(): String = JSON.encodeToString(serializer(), this)

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }

        fun decode(text: String?): EditorState = text?.let { runCatching { JSON.decodeFromString(serializer(), it) }.getOrNull() } ?: EditorState()
    }
}

/** The reducer. Pure; the screen applies the outcome and hands commits to the view model. */
object Editor {
    /** Choosing a tool ends pending picks and moves; drafts stay until closed or finished. */
    fun chooseTool(s: EditorState, tool: EditTool): EditorState = s.copy(
        tool = tool,
        pendingPin = null,
        pendingLabel = null,
        movingPin = null,
        movingTo = null,
        vertex = null,
        handle = null,
        panelTab = if (tool == EditTool.LAYER) 1 else if (s.panelTab == 1) 0 else s.panelTab,
    )

    /**
     * A tap on the base at [p] that hit [hit]; [handleIndex] is the handle under the tap when the
     * selected item shows handles: a curve handle of a label when [curveHandle], else a vertex of a
     * region.
     */
    fun tap(s: EditorState, map: WorldMap, p: Point, hit: MapHit?, handleIndex: Int? = null, curveHandle: Boolean = false): TapOutcome {
        if (s.movingPin != null) return TapOutcome(s.copy(movingTo = p))
        return when (s.tool) {
            EditTool.MOVE, EditTool.LAYER -> moveTap(s, p, hit, handleIndex, curveHandle)
            EditTool.PIN -> if (hit is MapHit.Pin) TapOutcome(s.copy(selected = hit.key, panelTab = 0)) else TapOutcome(s.copy(pendingPin = p), EditorEffect.PICK_PLACE)
            EditTool.REGION -> {
                val draft = s.region ?: RegionDraftState()
                TapOutcome(s.copy(region = draft.copy(vertices = draft.draft.add(p).vertices), selected = null, panelTab = 0))
            }
            EditTool.ROUTE -> {
                val next = s.routeDraft.add(map, p)
                TapOutcome(s.copy(route = next.points.map(RoutePointState::of), selected = null, panelTab = 0))
            }
            EditTool.LABEL -> if (hit is MapHit.Label) TapOutcome(s.copy(selected = hit.key, panelTab = 0)) else TapOutcome(s.copy(pendingLabel = p), EditorEffect.LABEL_TEXT)
        }
    }

    private fun moveTap(s: EditorState, p: Point, hit: MapHit?, handleIndex: Int?, curveHandle: Boolean): TapOutcome {
        val selected = s.selected
        if (selected != null && s.vertex != null) {
            return TapOutcome(s.copy(vertex = null), commit = EditorCommit.MoveVertex(selected, s.vertex, p))
        }
        if (selected != null && s.handle != null) {
            return TapOutcome(s.copy(handle = null), commit = EditorCommit.MoveCurveHandle(selected, s.handle, p))
        }
        if (selected != null && handleIndex != null) {
            return TapOutcome(if (curveHandle) s.copy(handle = handleIndex) else s.copy(vertex = handleIndex))
        }
        return TapOutcome(s.copy(selected = hit?.key, vertex = null, handle = null, panelTab = if (hit != null) 0 else s.panelTab))
    }

    /** Chooses a handle of the selected item directly (handle tapped); the next map tap places it. */
    fun chooseVertex(s: EditorState, index: Int): EditorState = s.copy(vertex = if (s.vertex == index) null else index, handle = null)

    fun chooseHandle(s: EditorState, index: Int): EditorState = s.copy(handle = if (s.handle == index) null else index, vertex = null)

    /** `되돌리기` of the region draft: the last vertex goes. */
    fun undoVertex(s: EditorState): EditorState {
        val r = s.region ?: return s
        val left = r.draft.undo().vertices
        return s.copy(region = if (left.isEmpty() && r.name.isEmpty()) null else r.copy(vertices = left))
    }

    fun undoRoutePoint(s: EditorState): EditorState {
        val left = s.routeDraft.undo().points
        return s.copy(route = left.takeIf { it.isNotEmpty() }?.map(RoutePointState::of))
    }

    fun editRegionDraft(s: EditorState, f: (RegionDraftState) -> RegionDraftState): EditorState = s.copy(region = f(s.region ?: RegionDraftState()))

    /** `핀 옮기기` from a pin's panel. */
    fun startMovePin(s: EditorState, pinKey: Key): EditorState = s.copy(movingPin = pinKey, movingTo = null, selected = pinKey)

    fun endMovePin(s: EditorState): EditorState = s.copy(movingPin = null, movingTo = null)

    /** After a commit: region closed, route finished, pin or label placed. */
    fun regionClosed(s: EditorState, regionKey: Key?): EditorState = s.copy(region = null, selected = regionKey)

    fun routeFinished(s: EditorState, routeKey: Key?): EditorState = s.copy(route = null, selected = routeKey)

    fun pinPlaced(s: EditorState, pinKey: Key?): EditorState = s.copy(pendingPin = null, selected = pinKey ?: s.selected)

    fun labelPlaced(s: EditorState, labelKey: Key?): EditorState = s.copy(pendingLabel = null, selected = labelKey ?: s.selected)

    fun select(s: EditorState, key: Key?): EditorState = s.copy(selected = key, vertex = null, handle = null, panelTab = 0)

    /** The vertex bar `꼭짓점 N · 되돌리기 · 닫기`. */
    fun vertexCount(s: EditorState): Int = s.region?.vertices?.size ?: 0
}
