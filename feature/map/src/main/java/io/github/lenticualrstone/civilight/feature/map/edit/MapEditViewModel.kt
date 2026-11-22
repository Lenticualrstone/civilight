package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapLabel
import io.github.lenticualrstone.civilight.model.map.MapRegion
import io.github.lenticualrstone.civilight.model.map.Route
import io.github.lenticualrstone.civilight.model.position.PointRef
import kotlinx.coroutines.Job

/**
 * 지도 편집 (WLD-40 … 44): every user action is one `update` (one undo step, `되돌리기` toast) over
 * `domain:world` `MapEdits`. The editor state itself (tool, drafts) is saveable screen state.
 */
class MapEditViewModel(projectKey: Key, val mapKey: Key, env: MapEnv) : MapScreenViewModel(projectKey, env) {
    private fun placeName(d: ProjectDoc, placeKey: Key?): String? = placeKey?.let { d.index.place(it)?.name }

    private fun <T> applied(edit: MapEdit<T>): Pair<ProjectDoc, T>? = (edit as? MapEdit.Applied)?.let { it.doc to it.value }

    // --- Pins (WLD-41) ---

    fun addPin(placeKey: Key, at: Point, onDone: (Key) -> Unit): Job {
        val ctx = editContext()
        val name = doc.value?.let { placeName(it, placeKey) }
        return commitValue(MapCopy.step(MapCopy.PIN, name), onDone) { d ->
            applied(MapEdits.addPin(d, mapKey, placeKey, at, ctx))?.let { (doc, pin) -> doc to pin.key }
        }
    }

    /** `새 장소`: the place and its pin in one step (its `지역` prefilled from nesting). */
    fun createPlaceWithPin(name: String, at: Point, onDone: (Key) -> Unit): Job {
        val ctx = editContext()
        return commitValue(MapCopy.step(MapCopy.PIN, name.trim()), onDone) { d ->
            applied(MapEdits.createPlaceWithPin(d, mapKey, name, at, ctx))?.let { (doc, pin) -> doc to pin.key }
        }
    }

    fun movePin(pinKey: Key, to: Point, onDone: () -> Unit): Job {
        val ctx = editContext()
        val name = doc.value?.let { d -> placeName(d, d.maps[mapKey]?.pins?.firstOrNull { it.key == pinKey }?.placeKey) }
        return commit(MapCopy.step(MapCopy.BTN_MOVE_PIN, name), onDone) { d -> MapEdits.movePin(d, mapKey, pinKey, to, ctx) }
    }

    // --- Regions (WLD-42) ---

    /** `닫기`: the region with the properties chosen while drawing; an empty name becomes `영역 N`. */
    fun closeRegion(draft: RegionDraftState, onDone: (Key) -> Unit): Job {
        val ctx = editContext()
        val count = doc.value?.maps?.get(mapKey)?.regions?.size ?: 0
        val name = draft.name.trim().ifEmpty { MapCopy.defaultRegionName(count + 1) }
        return commitValue(MapCopy.step(MapCopy.REGION, name), onDone) { d ->
            applied(
                MapEdits.addRegion(
                    d, mapKey, draft.draft, name, draft.color, ctx,
                    kind = draft.kind, factionKey = draft.factionKey, layerKey = draft.layerKey,
                    revealPoint = draft.revealUnitKey?.let(PointRef::unitStart),
                ),
            )?.let { (doc, region) -> doc to region.key }
        }
    }

    private fun regionName(regionKey: Key): String? = doc.value?.maps?.get(mapKey)?.regions?.firstOrNull { it.key == regionKey }?.name

    private fun routeTitle(routeKey: Key): String? = doc.value?.let { d ->
        d.maps[mapKey]?.let { m -> m.routes.firstOrNull { it.key == routeKey }?.let { io.github.lenticualrstone.civilight.domain.world.map.Routes.sheet(d, m, it).title } }
    }

    private fun labelText(labelKey: Key): String? = doc.value?.maps?.get(mapKey)?.labels?.firstOrNull { it.key == labelKey }?.text

    fun moveVertex(regionKey: Key, index: Int, to: Point): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.REGION, regionName(regionKey))) { d -> MapEdits.moveVertex(d, mapKey, regionKey, index, to, ctx) }
    }

    fun updateRegion(regionKey: Key, f: (MapRegion) -> MapRegion): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.REGION, regionName(regionKey))) { d -> MapEdits.updateRegion(d, mapKey, regionKey, ctx, f) }
    }

    // --- Routes (WLD-43) ---

    fun finishRoute(points: List<io.github.lenticualrstone.civilight.model.map.RoutePoint>, onDone: (Key) -> Unit): Job {
        val ctx = editContext()
        return commitValue(MapCopy.ROUTE_DRAWING, onDone) { d ->
            applied(MapEdits.addRoute(d, mapKey, points, ctx))?.let { (doc, route) -> doc to route.key }
        }
    }

    fun updateRoute(routeKey: Key, f: (Route) -> Route): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.ROUTE, routeTitle(routeKey))) { d -> MapEdits.updateRoute(d, mapKey, routeKey, ctx, f) }
    }

    // --- Labels (WLD-44) ---

    fun addLabel(text: String, at: Point, onDone: (Key) -> Unit): Job {
        val ctx = editContext()
        return commitValue(MapCopy.step(MapCopy.LABEL, text.trim()), onDone) { d ->
            applied(MapEdits.addLabel(d, mapKey, text, at, ctx))?.let { (doc, label) -> doc to label.key }
        }
    }

    fun updateLabel(labelKey: Key, f: (MapLabel) -> MapLabel): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.LABEL, labelText(labelKey))) { d ->
            MapEdits.updateMap(d, mapKey, ctx) { m -> m.copy(labels = m.labels.map { if (it.key == labelKey) f(it) else it }) }
        }
    }

    /** `곡선 라벨` on or off, or one control point moved. */
    fun setLabelCurve(labelKey: Key, curve: List<Point>?): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.LABEL, labelText(labelKey))) { d -> MapEdits.setLabelCurve(d, mapKey, labelKey, curve, ctx) }
    }

    // --- Any overlay ---

    /** `보이는 시점` from the start of a unit (`1-7부터`), or none. */
    fun setReveal(itemKey: Key, unitKey: Key?, label: String): Job {
        val ctx = editContext()
        return commit(label) { d -> MapEdits.setRevealPoint(d, mapKey, itemKey, unitKey?.let(PointRef::unitStart), ctx) }
    }

    fun setLayer(itemKey: Key, layerKey: Key?, label: String): Job {
        val ctx = editContext()
        return commit(label) { d -> MapEdits.setLayerOf(d, mapKey, itemKey, layerKey, ctx) }
    }

    /** A layer shown or hidden on this map (the `레이어` tab). */
    fun setLayerVisible(layerKey: Key, visible: Boolean): Job {
        val ctx = editContext()
        val name = doc.value?.maps?.get(mapKey)?.layers?.firstOrNull { it.key == layerKey }?.name
        return commit(MapCopy.step(MapCopy.LAYER, name)) { d -> MapEdits.setLayerVisible(d, mapKey, layerKey, visible, ctx) }
    }
}
