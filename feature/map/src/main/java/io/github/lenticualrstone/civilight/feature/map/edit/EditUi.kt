package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.feature.map.common.MapOffers
import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.domain.world.map.DetailEdits
import io.github.lenticualrstone.civilight.domain.world.map.MapGeometry
import io.github.lenticualrstone.civilight.domain.world.map.RegionShapes
import io.github.lenticualrstone.civilight.domain.story.lens.LensView
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RegionShapeValue
import io.github.lenticualrstone.civilight.model.map.DrawnLineKind
import io.github.lenticualrstone.civilight.model.map.StampKind
import io.github.lenticualrstone.civilight.domain.world.map.MapScales
import io.github.lenticualrstone.civilight.domain.world.map.MapViews
import io.github.lenticualrstone.civilight.domain.world.map.RouteSheet
import io.github.lenticualrstone.civilight.domain.world.map.Routes
import io.github.lenticualrstone.civilight.domain.world.map.Travel
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBase
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBases
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScene
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScenes
import io.github.lenticualrstone.civilight.feature.map.canvas.SceneMode
import io.github.lenticualrstone.civilight.feature.map.view.MapViewState
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.Route
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.PointRef

/**
 * A travel-mode chip of the route sheet: `도보 15 km · 8시간` (chosen) or `말`; [index] in
 * `Travel.modes` order (null = `직접`, the mode editor). Keys are not used, because a project
 * without stored modes reads fresh default keys each time (WLD-47).
 */
data class ModeChipUi(val index: Int?, val label: String, val selected: Boolean)

/** `속성` of what is selected or being drawn. */
sealed interface PropsUi {
    val title: String

    data class PinProps(
        val key: Key,
        val placeKey: Key,
        override val title: String,
        val reveal: String,
        val layer: String,
        /** Live child maps of the pin (`하위 지도`, WLD-48). */
        val childMaps: List<Pair<Key, String>> = emptyList(),
    ) : PropsUi

    data class RegionProps(
        val key: Key,
        override val title: String,
        val kind: String,
        val faction: String,
        val reveal: String,
        val layer: String,
        val color: String,
        /** Vertices of the shape in force at the lens, as handles (WLD-65). */
        val vertices: List<Point>,
        /** `◆` shapes of the region (WLD-65 `비교`): `1-9 ◆` and the shape from there. */
        val shapes: List<Pair<String, List<Point>>> = emptyList(),
    ) : PropsUi

    /** `새 영역 · 그리는 중` with the vertex bar `꼭짓점 N · 되돌리기 · 닫기`. */
    data class RegionDraftProps(
        override val title: String,
        val name: String,
        val kind: String,
        val faction: String,
        val reveal: String,
        val layer: String,
        val color: String,
        val vertexBar: String,
        val canClose: Boolean,
    ) : PropsUi

    /** The route sheet (`경로 · 닫기`): title, duration, `38 km · 지점 3` or `축척 없음`, mode chips. */
    data class RouteProps(
        val key: Key,
        override val title: String,
        val sheet: RouteSheet,
        val modes: List<ModeChipUi>,
        val reveal: String,
        val layer: String,
    ) : PropsUi

    /** The route being drawn: `지점 3 · 12 km`. */
    data class RouteDraftProps(override val title: String, val bar: String, val canFinish: Boolean) : PropsUi

    data class StampProps(override val title: String, val data: StampPropsData) : PropsUi

    /** A selected river or road (WLD-60): its points as handles. */
    data class LineProps(val key: Key, override val title: String, val points: List<Point>) : PropsUi

    /** The `강 · 길` line being drawn: `지점 3 · 12 km`. */
    data class LineDraftProps(override val title: String, val bar: String, val canFinish: Boolean) : PropsUi

    data class LabelProps(
        val key: Key,
        override val title: String,
        val reveal: String,
        val layer: String,
        /** `곡선 라벨` is offered on detailed-terrain maps (WLD-44). */
        val canCurve: Boolean,
        val curve: List<Point>?,
    ) : PropsUi
}

/** A selected stamp (WLD-60): chip `성 · 크기 2 · 0° · 장소 ‘결사의 문’`, size, `장소 연결`. */
data class StampPropsData(
    val key: Key,
    val chip: String,
    val size: Int,
    val rotation: Int,
    val place: String,
    val reveal: String,
    val layer: String,
    val at: Point,
)

/** A row of the `레이어` tab (WLD-45): name, shown or not, `보이는 시점`, and order buttons. */
data class LayerRowUi(
    val key: Key,
    val name: String,
    val visible: Boolean,
    val reveal: String = MapCopy.NONE,
    val canUp: Boolean = false,
    val canDown: Boolean = false,
)

/** A `목록` row: `핀 · 결사의 문`. */
data class ItemRowUi(val key: Key, val text: String, val selected: Boolean)

/** 지도 편집 (WLD-40, boards `MapEdit`, `MapRouteC`, `TourMap`). */
data class EditUi(
    val mapKey: Key,
    /** `하층 구역 지도 · 편집`. */
    val title: String,
    /** `세계 지도 › 하층 구역` (or `세계 · 지도`). */
    val small: String,
    val base: MapBase,
    val scene: MapScene,
    val scale: String?,
    val props: PropsUi?,
    val layers: List<LayerRowUi>,
    val items: List<ItemRowUi>,
    /** Draft vertices and route points to draw (0–1). */
    val regionDraft: List<Point>,
    val routeDraft: List<Point>,
    /** The `강 · 길` line being drawn. */
    val lineDraft: List<Point> = emptyList(),
    /** `범례` (WLD-60): `산 · 3`, `강 · 2` …. */
    val legend: List<String> = emptyList(),
    /** The `◆` shape drawn for `비교` (WLD-65), and the shape in force it is compared with. */
    val compare: Pair<List<Point>, List<Point>>? = null,
    /** The pin being moved: its name bar and new spot. */
    val moving: Pair<String, Point?>?,
)

object EditState {
    /** `1-7부터` or `없음`. */
    fun revealLabel(doc: ProjectDoc, point: PointRef?): String =
        point?.let { StoryCodes(doc).pointLabel(it) }?.let(MapCopy::revealFrom) ?: MapCopy.NONE

    fun revealLabelOfUnit(doc: ProjectDoc, unitKey: Key?): String = revealLabel(doc, unitKey?.let(PointRef::unitStart))

    fun layerLabel(map: WorldMap, layerKey: Key?): String = layerKey?.let { k -> map.layers.firstOrNull { it.key == k }?.name } ?: MapCopy.NONE

    fun factionLabel(doc: ProjectDoc, factionKey: Key?): String =
        factionKey?.let { doc.index.faction(it)?.name }?.let(MapCopy::factionLink) ?: MapCopy.NONE

    /** Mode chips: the project's modes (the chosen one with its numbers) and `직접`. */
    fun modeChips(doc: ProjectDoc, route: Route): List<ModeChipUi> {
        val modes = Travel.modes(doc)
        val chosen = modes.firstOrNull { it.key == route.shownModeKey }
            ?: modes.firstOrNull { it.key == doc.project.defaultTravelModeKey } ?: modes.first()
        return modes.mapIndexed { i, m ->
            val on = m.key == chosen.key
            ModeChipUi(i, if (on) Travel.modeLabel(m) else m.name, on)
        } + ModeChipUi(null, MapCopy.CUSTOM_MODE, false)
    }

    fun build(doc: ProjectDoc, mapKey: Key, lens: LensPoint, hiddenOpen: Boolean, s: EditorState): EditUi? {
        val map = doc.maps[mapKey]?.takeIf { doc.liveness.isMapLive(it.key) } ?: return null
        var scene = MapScenes.build(doc, map, lens, hiddenOpen, SceneMode.EDIT)
        val moving = s.movingPin?.let { k -> scene.pin(k)?.let { MapCopy.movingPin(it.name) to s.movingTo } }
        if (s.movingPin != null && s.movingTo != null) {
            scene = scene.copy(pins = scene.pins.map { if (it.key == s.movingPin) it.copy(at = s.movingTo) else it })
        }
        val crumbs = MapViews.breadcrumbText(doc, map)
        return EditUi(
            mapKey = map.key,
            title = MapCopy.editTitle(map.name),
            small = if (map.parent != null) crumbs else MapCopy.viewSmall(MapViewState.styleName(map)),
            base = MapBases.of(map),
            scene = scene,
            scale = if (MapOffers.showsScale(map)) MapScales.barLabel(doc, map) else null,
            props = props(doc, map, scene, lens, s),
            layers = layerRows(doc, map),
            items = items(scene, s.selected),
            regionDraft = s.region?.vertices.orEmpty(),
            routeDraft = Routes.points(map, Route(key = "", points = s.routeDraft.points)),
            lineDraft = s.line.orEmpty(),
            legend = legend(map),
            compare = compareShapes(doc, map, lens, s),
            moving = moving,
        )
    }

    /** `범례` of a detailed-terrain map: each stamp kind with its count, then `강 · N` and `길 · N`. */
    fun legend(map: WorldMap): List<String> {
        val stamps = map.stamps.mapNotNull { it.kind.known }.groupingBy { it }.eachCount()
        val rows = StampKind.entries.filter { it in stamps }.map { "${DetailEdits.stampName(it)}${MapCopy.SEP}${stamps.getValue(it)}" }
        val rivers = map.drawnLines.count { it.kind.known == DrawnLineKind.RIVER }
        val roads = map.drawnLines.count { it.kind.known == DrawnLineKind.ROAD }
        return rows + listOfNotNull(
            "${MapCopy.TOOL_RIVER}${MapCopy.SEP}$rivers".takeIf { rivers > 0 },
            "${MapCopy.TOOL_ROAD}${MapCopy.SEP}$roads".takeIf { roads > 0 },
        )
    }

    /** `◆` shapes of a region (WLD-65): `1-9 ◆` and the vertices recorded there, in history order. */
    fun shapeHistory(doc: ProjectDoc, mapKey: Key, regionKey: Key): List<Pair<String, List<Point>>> {
        val view = LensView(doc)
        return view.history(ChangeTarget.RegionShape(mapKey, regionKey)).mapNotNull { r ->
            val v = runCatching { r.valueAs(RegionShapeValue.serializer()) }.getOrNull()?.vertices ?: return@mapNotNull null
            "${view.anchorLabel(r.anchor)} ◆" to v
        }
    }

    private fun compareShapes(doc: ProjectDoc, map: WorldMap, lens: LensPoint, s: EditorState): Pair<List<Point>, List<Point>>? {
        val index = s.compare ?: return null
        val region = map.regions.firstOrNull { it.key == s.selected } ?: return null
        val other = shapeHistory(doc, map.key, region.key).getOrNull(index)?.second ?: return null
        return other to RegionShapes.shapeAt(doc, map.key, region, lens)
    }

    private fun props(doc: ProjectDoc, map: WorldMap, scene: MapScene, lens: LensPoint, s: EditorState): PropsUi? {
        val region = s.region
        if (s.tool == io.github.lenticualrstone.civilight.feature.map.edit.EditTool.REGION && region != null) {
            return PropsUi.RegionDraftProps(
                title = MapCopy.NEW_REGION_DRAWING,
                name = region.name,
                kind = region.kind,
                faction = factionLabel(doc, region.factionKey),
                reveal = revealLabelOfUnit(doc, region.revealUnitKey),
                layer = layerLabel(map, region.layerKey),
                color = region.color,
                vertexBar = region.draft.bar,
                canClose = region.draft.canClose,
            )
        }
        if (s.tool == io.github.lenticualrstone.civilight.feature.map.edit.EditTool.ROUTE && s.route != null) {
            val draft = s.routeDraft
            val km = MapScales.kmPerUnit(doc, map)?.let { Travel.number(Math.round(it * draft.length(map)).toDouble()) }
            return PropsUi.RouteDraftProps(MapCopy.ROUTE_DRAWING, MapCopy.routeDraftBar(draft.points.size, km), draft.points.size >= 2)
        }
        val line = s.line
        if ((s.tool == EditTool.RIVER || s.tool == EditTool.ROAD) && line != null) {
            val km = MapScales.kmPerUnit(doc, map)?.let { k ->
                Travel.number(Math.round(k * line.zipWithNext { a, b -> MapGeometry.distance(a, b) }.sum()).toDouble())
            }
            val title = if (s.tool == EditTool.RIVER) MapCopy.TOOL_RIVER else MapCopy.TOOL_ROAD
            return PropsUi.LineDraftProps(title, MapCopy.routeDraftBar(line.size, km), line.size >= 2)
        }
        val key = s.selected ?: return null
        scene.stamps.firstOrNull { it.key == key }?.let { st ->
            val stamp = map.stamps.first { it.key == key }
            val place = stamp.placeKey?.let { doc.index.place(it)?.name } ?: MapCopy.NONE
            return PropsUi.StampProps(
                DetailEdits.stampName(st.kind),
                StampPropsData(key, DetailEdits.stampChip(doc, stamp), stamp.size, stamp.rotation, place, revealLabel(doc, stamp.revealPoint), layerLabel(map, stamp.layerKey), st.at),
            )
        }
        scene.lines.firstOrNull { it.key == key }?.let { l ->
            return PropsUi.LineProps(key, if (l.kind == DrawnLineKind.RIVER) MapCopy.TOOL_RIVER else MapCopy.TOOL_ROAD, l.points)
        }
        val drawn = scene.pin(key) != null || scene.region(key) != null || scene.routes.any { it.key == key } || scene.labels.any { it.key == key }
        if (!drawn) return null
        map.pins.firstOrNull { it.key == key }?.let { pin ->
            return PropsUi.PinProps(
                key, pin.placeKey, doc.index.place(pin.placeKey)?.name.orEmpty(),
                revealLabel(doc, pin.revealPoint), layerLabel(map, pin.layerKey),
                MapViews.childMaps(doc, pin.key).map { it.key to it.name },
            )
        }
        map.regions.firstOrNull { it.key == key }?.let { r ->
            // Vertex handles edit the shape in force at the lens; a move asks `이 값 고치기 · 여기서부터
            // 바뀜` when the lens or the shape's `◆` history calls for it (WLD-65).
            val shown = RegionShapes.shapeAt(doc, map.key, r, lens)
            return PropsUi.RegionProps(
                r.key, r.name, r.kind.ifEmpty { MapCopy.NONE }, factionLabel(doc, r.factionKey),
                revealLabel(doc, r.revealPoint), layerLabel(map, r.layerKey), r.color,
                shown, shapeHistory(doc, map.key, r.key),
            )
        }
        map.routes.firstOrNull { it.key == key }?.let { r ->
            val sheet = Routes.sheet(doc, map, r)
            return PropsUi.RouteProps(r.key, sheet.title, sheet, modeChips(doc, r), revealLabel(doc, r.revealPoint), layerLabel(map, r.layerKey))
        }
        map.labels.firstOrNull { it.key == key }?.let { l ->
            return PropsUi.LabelProps(
                l.key, l.text, revealLabel(doc, l.revealPoint), layerLabel(map, l.layerKey),
                canCurve = map.format?.known == MapFormat.DETAIL, curve = l.curve,
            )
        }
        return null
    }

    /** The `레이어` tab rows in order (WLD-45). */
    fun layerRows(doc: ProjectDoc, map: WorldMap): List<LayerRowUi> {
        val layers = MapEdits.layers(map)
        return layers.mapIndexed { i, l -> LayerRowUi(l.key, l.name, l.visible, revealLabel(doc, l.revealPoint), i > 0, i < layers.lastIndex) }
    }

    private fun items(scene: MapScene, selected: Key?): List<ItemRowUi> {
        fun row(key: Key, kind: String, name: String) = ItemRowUi(key, "$kind${MapCopy.SEP}$name", key == selected)
        return scene.pins.map { row(it.key, MapCopy.PIN, it.name) } +
            scene.regions.map { row(it.key, MapCopy.REGION, it.name) } +
            scene.routes.map { row(it.key, MapCopy.ROUTE, it.label.substringBefore(MapCopy.SEP)) } +
            scene.labels.map { row(it.key, MapCopy.LABEL, it.text) }
    }
}
