package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.domain.world.map.LayerOrder
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.domain.world.map.MapNesting
import io.github.lenticualrstone.civilight.domain.world.map.TravelModeEdits
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
import io.github.lenticualrstone.civilight.domain.world.map.DetailEdits
import io.github.lenticualrstone.civilight.model.map.DrawnLineKind
import io.github.lenticualrstone.civilight.model.map.Stamp
import io.github.lenticualrstone.civilight.model.map.StampKind
import io.github.lenticualrstone.civilight.model.position.LensPoint

/**
 * 지도 편집 (WLD-40 … 44): every user action is one `update` (one undo step, `되돌리기` toast) over
 * `domain:world` `MapEdits`. The editor state itself (tool, drafts) is saveable screen state.
 */
open class MapEditViewModel(projectKey: Key, val mapKey: Key, env: MapEnv) : MapScreenViewModel(projectKey, env) {
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

    protected fun regionName(regionKey: Key): String? = doc.value?.maps?.get(mapKey)?.regions?.firstOrNull { it.key == regionKey }?.name

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

    /**
     * A route's mode chip (WLD-43, WLD-47) by its index in `Travel.modes` order: the modes are
     * stored first when the project has none, so the kept key is stable.
     */
    fun setRouteMode(routeKey: Key, index: Int): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.ROUTE, routeTitle(routeKey))) { d ->
            val stored = TravelModeEdits.ensureStored(d, ctx)
            val mode = TravelModeEdits.storedAt(stored, index) ?: return@commit null
            MapEdits.updateRoute(stored, mapKey, routeKey, ctx) { it.copy(shownModeKey = mode.key) }
        }
    }

    /** A mode made in the editor from a route's `직접` becomes that route's mode. */
    fun setRouteModeKey(routeKey: Key, modeKey: Key): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.ROUTE, routeTitle(routeKey))) { d -> MapEdits.updateRoute(d, mapKey, routeKey, ctx) { it.copy(shownModeKey = modeKey) } }
    }

    // --- Layers (WLD-45) ---

    private fun layerName(layerKey: Key): String? = doc.value?.maps?.get(mapKey)?.layers?.firstOrNull { it.key == layerKey }?.name

    fun addLayer(name: String): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.LAYER, name.trim())) { d -> (MapEdits.addLayer(d, mapKey, name, ctx) as? MapEdit.Applied)?.doc }
    }

    fun renameLayer(layerKey: Key, name: String): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.LAYER, name.trim())) { d -> MapEdits.renameLayer(d, mapKey, layerKey, name, ctx) }
    }

    fun moveLayer(layerKey: Key, up: Boolean): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.LAYER, layerName(layerKey))) { d ->
            if (up) LayerOrder.moveUp(d, mapKey, layerKey, ctx) else LayerOrder.moveDown(d, mapKey, layerKey, ctx)
        }
    }

    // --- Child maps (WLD-48) ---

    /** `하위 지도 · 고르기`: [childKey] becomes the child map of [pinKey]. */
    fun nestMap(childKey: Key, pinKey: Key): Job {
        val ctx = editContext()
        val name = doc.value?.maps?.get(childKey)?.name
        return commit(MapCopy.step(MapCopy.CHILD_MAP, name)) { d -> MapNesting.setParent(d, childKey, mapKey, pinKey, ctx) }
    }

    // --- WLD-60 detailed terrain ---

    private fun stampName(stampKey: Key): String? =
        doc.value?.maps?.get(mapKey)?.stamps?.firstOrNull { it.key == stampKey }?.kind?.known?.let(DetailEdits::stampName)

    /** `도장 · 탭 → 놓기`: one step `도장 · 성`. */
    fun placeStamp(at: Point, kind: StampKind, size: Int, onDone: (Key) -> Unit): Job {
        val ctx = editContext()
        return commitValue(MapCopy.step(MapCopy.STAMP, DetailEdits.stampName(kind)), onDone) { d ->
            applied(DetailEdits.placeStamp(d, mapKey, kind, at, ctx, size))?.let { (doc, stamp) -> doc to stamp.key }
        }
    }

    fun moveStamp(stampKey: Key, to: Point): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.STAMP, stampName(stampKey))) { d -> DetailEdits.moveStamp(d, mapKey, stampKey, to, ctx) }
    }

    fun updateStamp(stampKey: Key, f: (Stamp) -> Stamp): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.STAMP, stampName(stampKey))) { d ->
            if (d.maps[mapKey]?.stamps?.none { it.key == stampKey } != false) null else MapEdits.updateStamp(d, mapKey, stampKey, ctx, f)
        }
    }

    /** `위 손잡이 → 회전`: the angle from the stamp to [to], snapped to 15° (`MapEdits.snapRotation`). */
    fun rotateStampToward(stampKey: Key, to: Point): Job {
        val stamp = doc.value?.maps?.get(mapKey)?.stamps?.firstOrNull { it.key == stampKey }
        val degrees = stamp?.let { StampHandles.rotationToward(Point(it.x, it.y), to) } ?: 0
        return updateStamp(stampKey) { it.copy(rotation = degrees) }
    }

    /** `모서리 → 크기`: the size step nearest to the distance from the stamp to [to]. */
    fun resizeStampToward(stampKey: Key, to: Point): Job {
        val stamp = doc.value?.maps?.get(mapKey)?.stamps?.firstOrNull { it.key == stampKey }
        val size = stamp?.let { StampHandles.sizeToward(Point(it.x, it.y), to) } ?: 2
        return updateStamp(stampKey) { it.copy(size = size) }
    }

    /** `강 · 길 · 완료`: one step `강` or `길`. */
    fun finishLine(kind: DrawnLineKind, points: List<Point>, onDone: (Key) -> Unit): Job {
        val ctx = editContext()
        val name = if (kind == DrawnLineKind.RIVER) MapCopy.TOOL_RIVER else MapCopy.TOOL_ROAD
        return commitValue(name, onDone) { d -> applied(DetailEdits.addLine(d, mapKey, kind, points, ctx))?.let { (doc, line) -> doc to line.key } }
    }

    fun moveLinePoint(lineKey: Key, index: Int, to: Point): Job {
        val ctx = editContext()
        val kind = doc.value?.maps?.get(mapKey)?.drawnLines?.firstOrNull { it.key == lineKey }?.kind?.known
        val name = if (kind == DrawnLineKind.ROAD) MapCopy.TOOL_ROAD else MapCopy.TOOL_RIVER
        return commit(name) { d -> DetailEdits.moveLinePoint(d, mapKey, lineKey, index, to, ctx) }
    }

    // --- WLD-65 region shape at the lens ---

    private fun storyContext() = io.github.lenticualrstone.civilight.domain.story.scene.EditContext(
        newKey = io.github.lenticualrstone.civilight.model.Keys::newKey, now = env.clock.now(), device = env.deviceId(),
    )

    /** `이 값 고치기` (or the plain base edit): the shape in force at [lens] with one vertex moved. */
    fun overwriteShape(regionKey: Key, lens: LensPoint, index: Int, to: Point): Job {
        val ctx = editContext()
        val story = storyContext()
        return commit(MapCopy.step(MapCopy.REGION, regionName(regionKey))) { d ->
            val shape = ShapeEdits.moved(d, mapKey, regionKey, lens, index, to) ?: return@commit null
            ShapeEdits.overwrite(d, mapKey, regionKey, lens, shape, ctx, story)
        }
    }

    /** `여기서부터 바뀜`: a new `◆` shape with one vertex moved. */
    fun changeShapeFromHere(regionKey: Key, lens: LensPoint, index: Int, to: Point): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.REGION, regionName(regionKey))) { d ->
            val shape = ShapeEdits.moved(d, mapKey, regionKey, lens, index, to) ?: return@commit null
            ShapeEdits.fromHere(d, mapKey, regionKey, lens, shape, ctx)
        }
    }

    /** A layer shown or hidden on this map (the `레이어` tab). */
    fun setLayerVisible(layerKey: Key, visible: Boolean): Job {
        val ctx = editContext()
        val name = doc.value?.maps?.get(mapKey)?.layers?.firstOrNull { it.key == layerKey }?.name
        return commit(MapCopy.step(MapCopy.LAYER, name)) { d -> MapEdits.setLayerVisible(d, mapKey, layerKey, visible, ctx) }
    }
}
