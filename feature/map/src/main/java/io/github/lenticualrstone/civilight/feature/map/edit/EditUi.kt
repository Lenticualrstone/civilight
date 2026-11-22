package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
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

/** A travel-mode chip of the route sheet: `도보 15 km · 8시간` (chosen) or `말`; null key = `직접`. */
data class ModeChipUi(val modeKey: Key?, val label: String, val selected: Boolean)

/** `속성` of what is selected or being drawn. */
sealed interface PropsUi {
    val title: String

    data class PinProps(val key: Key, val placeKey: Key, override val title: String, val reveal: String, val layer: String) : PropsUi

    data class RegionProps(
        val key: Key,
        override val title: String,
        val kind: String,
        val faction: String,
        val reveal: String,
        val layer: String,
        val color: String,
        /** Vertices of the shape, as handles. */
        val vertices: List<Point>,
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

data class LayerRowUi(val key: Key, val name: String, val visible: Boolean)

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
        val chosen = Travel.mode(doc, route.shownModeKey) ?: Travel.defaultMode(doc)
        return Travel.modes(doc).map { m ->
            val on = m.key == chosen.key
            ModeChipUi(m.key, if (on) Travel.modeLabel(m) else m.name, on)
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
            scale = MapScales.barLabel(doc, map),
            props = props(doc, map, scene, lens, s),
            layers = MapEdits.layers(map).map { LayerRowUi(it.key, it.name, it.visible) },
            items = items(scene, s.selected),
            regionDraft = s.region?.vertices.orEmpty(),
            routeDraft = Routes.points(map, Route(key = "", points = s.routeDraft.points)),
            moving = moving,
        )
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
        val key = s.selected ?: return null
        val drawn = scene.pin(key) != null || scene.region(key) != null || scene.routes.any { it.key == key } || scene.labels.any { it.key == key }
        if (!drawn) return null
        map.pins.firstOrNull { it.key == key }?.let { pin ->
            return PropsUi.PinProps(
                key, pin.placeKey, doc.index.place(pin.placeKey)?.name.orEmpty(),
                revealLabel(doc, pin.revealPoint), layerLabel(map, pin.layerKey),
            )
        }
        map.regions.firstOrNull { it.key == key }?.let { r ->
            // Vertex handles edit the base shape, so they show only while the base shape is in force
            // at the lens (a later `◆` shape is WLD-65's to edit).
            val shown = io.github.lenticualrstone.civilight.domain.world.map.RegionShapes.shapeAt(doc, map.key, r, lens)
            return PropsUi.RegionProps(
                r.key, r.name, r.kind.ifEmpty { MapCopy.NONE }, factionLabel(doc, r.factionKey),
                revealLabel(doc, r.revealPoint), layerLabel(map, r.layerKey), r.color,
                if (shown == r.vertices) r.vertices else emptyList(),
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

    private fun items(scene: MapScene, selected: Key?): List<ItemRowUi> {
        fun row(key: Key, kind: String, name: String) = ItemRowUi(key, "$kind${MapCopy.SEP}$name", key == selected)
        return scene.pins.map { row(it.key, MapCopy.PIN, it.name) } +
            scene.regions.map { row(it.key, MapCopy.REGION, it.name) } +
            scene.routes.map { row(it.key, MapCopy.ROUTE, it.label.substringBefore(MapCopy.SEP)) } +
            scene.labels.map { row(it.key, MapCopy.LABEL, it.text) }
    }
}
