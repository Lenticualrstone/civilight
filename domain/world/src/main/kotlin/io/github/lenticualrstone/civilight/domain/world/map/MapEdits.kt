package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.item.CreateResult
import io.github.lenticualrstone.civilight.domain.world.item.WorldItems
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RegionShapeValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.MapLabel
import io.github.lenticualrstone.civilight.model.map.MapLayer
import io.github.lenticualrstone.civilight.model.map.MapParent
import io.github.lenticualrstone.civilight.model.map.MapRegion
import io.github.lenticualrstone.civilight.model.map.MapScale
import io.github.lenticualrstone.civilight.model.map.MapSize
import io.github.lenticualrstone.civilight.model.map.MapStyle
import io.github.lenticualrstone.civilight.model.map.NodeMapLink
import io.github.lenticualrstone.civilight.model.map.NodeMapNode
import io.github.lenticualrstone.civilight.model.map.Pin
import io.github.lenticualrstone.civilight.model.map.Route
import io.github.lenticualrstone.civilight.model.map.RoutePoint
import io.github.lenticualrstone.civilight.model.map.Stamp
import io.github.lenticualrstone.civilight.model.map.StampKind
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.PointRef

/** Result of a map edit that can be refused. */
sealed interface MapEdit<out T> {
    data class Applied<T>(val doc: ProjectDoc, val value: T) : MapEdit<T>
    data class Refused(val reason: MapRefusal) : MapEdit<Nothing>
}

enum class MapRefusal {
    NO_MAP,
    NO_ITEM,
    /** One pin per place per map (WLD-41 default). */
    PLACE_ALREADY_PINNED,
    /** A region needs at least 3 vertices; a route at least 2 points. */
    TOO_FEW_POINTS,
    EMPTY_NAME,
    /** Stamps come in sizes 1 · 2 · 3. */
    BAD_SIZE,
}

/**
 * Map overlay edits (WLD-36 … WLD-50). All coordinates are clamped to [0, 1]; overlay data is
 * never baked into the base, so replacing the base image keeps every overlay. Every edit returns a
 * new doc, so undo is the previous doc.
 */
object MapEdits {
    /** Rotation snaps to 15° (WLD-60). */
    fun snapRotation(degrees: Int): Int = Math.floorMod(Math.round(degrees / 15.0).toInt() * 15, 360)

    /** Updates one map; null when the map is unknown. */
    fun updateMap(doc: ProjectDoc, mapKey: Key, ctx: EditContext, f: (WorldMap) -> WorldMap): ProjectDoc? {
        val map = doc.maps[mapKey] ?: return null
        return doc.copy(maps = doc.maps + (mapKey to f(map).copy(updatedAt = ctx.now(), updatedBy = ctx.device)))
    }

    private fun <T> applied(doc: ProjectDoc?, value: T): MapEdit<T> =
        if (doc == null) MapEdit.Refused(MapRefusal.NO_MAP) else MapEdit.Applied(doc, value)

    // --- Maps ---

    /** A new map (`새 지도`); default name `지도 N`. A child of a pin when [parent] is given. */
    fun newMap(
        doc: ProjectDoc,
        baseKind: MapBaseKind,
        ctx: EditContext,
        name: String? = null,
        parent: MapParent? = null,
        style: MapStyle? = null,
    ): MapEdit<WorldMap> {
        val map = WorldMap(
            key = ctx.newKey(),
            order = OrderKeys.after(doc.maps.values.maxOfOrNull { it.order }),
            name = name?.trim()?.takeIf { it.isNotEmpty() } ?: "지도 ${doc.maps.size + 1}",
            parent = parent,
            baseKind = baseKind.open(),
            format = if (baseKind == MapBaseKind.NODE) MapFormat.NODE.open() else null,
            style = style?.open(),
            updatedAt = ctx.now(), updatedBy = ctx.device,
        )
        return MapEdit.Applied(doc.copy(maps = doc.maps + (map.key to map)), map)
    }

    /** `지도 이미지 바꾸기`: a new base; overlays keep their 0–1 coordinates. */
    fun replaceBase(doc: ProjectDoc, mapKey: Key, imageHash: String, size: MapSize?, ctx: EditContext): ProjectDoc? =
        updateMap(doc, mapKey, ctx) { it.copy(baseImageHash = imageHash, baseSize = size) }

    fun setScale(doc: ProjectDoc, mapKey: Key, scale: MapScale?, ctx: EditContext): ProjectDoc? =
        updateMap(doc, mapKey, ctx) { it.copy(scale = scale) }

    // --- Pins ---

    fun addPin(
        doc: ProjectDoc,
        mapKey: Key,
        placeKey: Key,
        at: Point,
        ctx: EditContext,
        layerKey: Key? = null,
        revealPoint: PointRef? = null,
    ): MapEdit<Pin> {
        val map = doc.maps[mapKey] ?: return MapEdit.Refused(MapRefusal.NO_MAP)
        if (map.pins.any { it.placeKey == placeKey }) return MapEdit.Refused(MapRefusal.PLACE_ALREADY_PINNED)
        val p = MapGeometry.clamp(at)
        val pin = Pin(ctx.newKey(), placeKey, p.x, p.y, layerKey, revealPoint, ctx.now(), ctx.device)
        return applied(updateMap(doc, mapKey, ctx) { it.copy(pins = it.pins + pin) }, pin)
    }

    /**
     * `새 장소`: creates a place and its pin. The place's `지역` is prefilled from nesting: on a
     * sub-map whose parent pin belongs to place P, the new place's parent is P (WLD-09 default).
     */
    fun createPlaceWithPin(doc: ProjectDoc, mapKey: Key, name: String, at: Point, ctx: EditContext): MapEdit<Pin> {
        val map = doc.maps[mapKey] ?: return MapEdit.Refused(MapRefusal.NO_MAP)
        val parentPlace = map.parent?.let { p -> doc.maps[p.mapKey]?.pins?.firstOrNull { it.key == p.pinKey }?.placeKey }
        return when (val created = WorldItems.createPlace(doc, name, ctx, parentPlaceKey = parentPlace)) {
            CreateResult.EmptyName -> MapEdit.Refused(MapRefusal.EMPTY_NAME)
            is CreateResult.Created -> addPin(created.doc, mapKey, created.key, at, ctx)
        }
    }

    /** `핀 옮기기 · 완료`. */
    fun movePin(doc: ProjectDoc, mapKey: Key, pinKey: Key, to: Point, ctx: EditContext): ProjectDoc? {
        val p = MapGeometry.clamp(to)
        if (doc.maps[mapKey]?.pins?.none { it.key == pinKey } != false) return null
        return updateMap(doc, mapKey, ctx) { m -> m.copy(pins = m.pins.map { if (it.key == pinKey) it.copy(x = p.x, y = p.y) else it }) }
    }

    fun removePin(doc: ProjectDoc, mapKey: Key, pinKey: Key, ctx: EditContext): ProjectDoc? =
        updateMap(doc, mapKey, ctx) { m -> m.copy(pins = m.pins.filterNot { it.key == pinKey }) }

    /** The pin under [at] within [radius] (map units), for snapping route points and picking. */
    fun pinNear(map: WorldMap, at: Point, radius: Double = SNAP_RADIUS): Pin? =
        map.pins.map { it to MapGeometry.distance(Point(it.x, it.y), at) }.filter { it.second <= radius }.minByOrNull { it.second }?.first

    // --- Regions ---

    fun addRegion(
        doc: ProjectDoc,
        mapKey: Key,
        draft: RegionDraft,
        name: String,
        color: String,
        ctx: EditContext,
        kind: String = "",
        factionKey: Key? = null,
        placeKey: Key? = null,
        layerKey: Key? = null,
        revealPoint: PointRef? = null,
    ): MapEdit<MapRegion> {
        if (!draft.canClose) return MapEdit.Refused(MapRefusal.TOO_FEW_POINTS)
        val region = MapRegion(
            key = ctx.newKey(), name = name.trim(), kind = kind, factionKey = factionKey, placeKey = placeKey, color = color,
            layerKey = layerKey, revealPoint = revealPoint, vertices = draft.vertices, updatedAt = ctx.now(), updatedBy = ctx.device,
        )
        return applied(updateMap(doc, mapKey, ctx) { it.copy(regions = it.regions + region) }, region)
    }

    /** Edits the base shape (`이 값 고치기` with no `◆` in force). */
    fun moveVertex(doc: ProjectDoc, mapKey: Key, regionKey: Key, index: Int, to: Point, ctx: EditContext): ProjectDoc? {
        val region = doc.maps[mapKey]?.regions?.firstOrNull { it.key == regionKey } ?: return null
        if (index !in region.vertices.indices) return null
        val vertices = region.vertices.toMutableList().also { it[index] = MapGeometry.clamp(to) }
        return updateRegion(doc, mapKey, regionKey, ctx) { it.copy(vertices = vertices) }
    }

    fun updateRegion(doc: ProjectDoc, mapKey: Key, regionKey: Key, ctx: EditContext, f: (MapRegion) -> MapRegion): ProjectDoc? =
        updateMap(doc, mapKey, ctx) { m -> m.copy(regions = m.regions.map { if (it.key == regionKey) f(it) else it }) }

    /** `여기서부터 바뀜` on a region's shape (WLD-65): a `regionShape` record from [anchor]. */
    fun changeShapeFrom(doc: ProjectDoc, mapKey: Key, regionKey: Key, anchor: Anchor, vertices: List<Point>, ctx: EditContext): MapEdit<ChangeRecord> {
        if (vertices.size < 3) return MapEdit.Refused(MapRefusal.TOO_FEW_POINTS)
        if (doc.maps[mapKey]?.regions?.none { it.key == regionKey } != false) return MapEdit.Refused(MapRefusal.NO_ITEM)
        val now = ctx.now()
        val record = ChangeRecord(
            key = ctx.newKey(), target = ChangeTarget.RegionShape(mapKey, regionKey),
            value = ChangeRecord.encodeValue(RegionShapeValue.serializer(), RegionShapeValue(vertices.map(MapGeometry::clamp))),
            anchor = anchor, createdAt = now, updatedAt = now, updatedBy = ctx.device,
        )
        return MapEdit.Applied(doc.copy(changes = doc.changes.copy(records = doc.changes.records + record)), record)
    }

    // --- Routes ---

    /** A route through [points] (pins or free points). */
    fun addRoute(
        doc: ProjectDoc,
        mapKey: Key,
        points: List<RoutePoint>,
        ctx: EditContext,
        layerKey: Key? = null,
        revealPoint: PointRef? = null,
        shownModeKey: Key? = null,
    ): MapEdit<Route> {
        if (points.size < 2) return MapEdit.Refused(MapRefusal.TOO_FEW_POINTS)
        val route = Route(ctx.newKey(), points.map(::clampPoint), layerKey, revealPoint, shownModeKey, ctx.now(), ctx.device)
        return applied(updateMap(doc, mapKey, ctx) { it.copy(routes = it.routes + route) }, route)
    }

    private fun clampPoint(p: RoutePoint): RoutePoint {
        val x = p.x
        val y = p.y
        return if (p.pinKey == null && x != null && y != null) RoutePoint.at(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f)) else p
    }

    fun updateRoute(doc: ProjectDoc, mapKey: Key, routeKey: Key, ctx: EditContext, f: (Route) -> Route): ProjectDoc? =
        updateMap(doc, mapKey, ctx) { m -> m.copy(routes = m.routes.map { if (it.key == routeKey) f(it) else it }) }

    /** `다른 핀에 잇기`: route points attached to [fromPin] now follow [toPin]. */
    fun reattachRouteEnds(doc: ProjectDoc, mapKey: Key, fromPin: Key, toPin: Key, ctx: EditContext): ProjectDoc? =
        updateMap(doc, mapKey, ctx) { m ->
            m.copy(routes = m.routes.map { r -> r.copy(points = r.points.map { if (it.pinKey == fromPin) RoutePoint.pin(toPin) else it }) })
        }

    // --- Labels ---

    fun addLabel(doc: ProjectDoc, mapKey: Key, text: String, at: Point, ctx: EditContext, layerKey: Key? = null, revealPoint: PointRef? = null): MapEdit<MapLabel> {
        val clean = text.trim()
        if (clean.isEmpty()) return MapEdit.Refused(MapRefusal.EMPTY_NAME)
        val p = MapGeometry.clamp(at)
        val label = MapLabel(ctx.newKey(), clean, p.x, p.y, null, layerKey, revealPoint, ctx.now(), ctx.device)
        return applied(updateMap(doc, mapKey, ctx) { it.copy(labels = it.labels + label) }, label)
    }

    /** `곡선 라벨`: control points (null = straight). */
    fun setLabelCurve(doc: ProjectDoc, mapKey: Key, labelKey: Key, curve: List<Point>?, ctx: EditContext): ProjectDoc? =
        updateMap(doc, mapKey, ctx) { m -> m.copy(labels = m.labels.map { if (it.key == labelKey) it.copy(curve = curve?.map(MapGeometry::clamp)) else it }) }

    // --- Layers ---

    fun addLayer(doc: ProjectDoc, mapKey: Key, name: String, ctx: EditContext): MapEdit<MapLayer> {
        val map = doc.maps[mapKey] ?: return MapEdit.Refused(MapRefusal.NO_MAP)
        val clean = name.trim()
        if (clean.isEmpty()) return MapEdit.Refused(MapRefusal.EMPTY_NAME)
        val layer = MapLayer(ctx.newKey(), OrderKeys.after(map.layers.maxOfOrNull { it.order }), clean, updatedAt = ctx.now(), updatedBy = ctx.device)
        return applied(updateMap(doc, mapKey, ctx) { it.copy(layers = it.layers + layer) }, layer)
    }

    fun updateLayer(doc: ProjectDoc, mapKey: Key, layerKey: Key, ctx: EditContext, f: (MapLayer) -> MapLayer): ProjectDoc? =
        updateMap(doc, mapKey, ctx) { m -> m.copy(layers = m.layers.map { if (it.key == layerKey) f(it) else it }) }

    fun renameLayer(doc: ProjectDoc, mapKey: Key, layerKey: Key, name: String, ctx: EditContext): ProjectDoc? =
        name.trim().takeIf { it.isNotEmpty() }?.let { n -> updateLayer(doc, mapKey, layerKey, ctx) { it.copy(name = n) } }

    fun setLayerVisible(doc: ProjectDoc, mapKey: Key, layerKey: Key, visible: Boolean, ctx: EditContext): ProjectDoc? =
        updateLayer(doc, mapKey, layerKey, ctx) { it.copy(visible = visible) }

    /** Moves a layer between two others (null = start / end). */
    fun moveLayer(doc: ProjectDoc, mapKey: Key, layerKey: Key, after: Key?, before: Key?, ctx: EditContext): ProjectDoc? {
        val map = doc.maps[mapKey] ?: return null
        val a = after?.let { k -> map.layers.firstOrNull { it.key == k }?.order }
        val b = before?.let { k -> map.layers.firstOrNull { it.key == k }?.order }
        val order = runCatching { OrderKeys.between(a, b) }.getOrNull() ?: return null
        return updateLayer(doc, mapKey, layerKey, ctx) { it.copy(order = order) }
    }

    /** Layers in order. */
    fun layers(map: WorldMap): List<MapLayer> = map.layers.sortedWith(compareBy({ it.order }, { it.key }))

    /** Puts an overlay on a layer (null = none); pins, regions, routes, labels and stamps have one layer at most. */
    fun setLayerOf(doc: ProjectDoc, mapKey: Key, itemKey: Key, layerKey: Key?, ctx: EditContext): ProjectDoc? =
        updateMap(doc, mapKey, ctx) { m ->
            m.copy(
                pins = m.pins.map { if (it.key == itemKey) it.copy(layerKey = layerKey) else it },
                regions = m.regions.map { if (it.key == itemKey) it.copy(layerKey = layerKey) else it },
                routes = m.routes.map { if (it.key == itemKey) it.copy(layerKey = layerKey) else it },
                labels = m.labels.map { if (it.key == itemKey) it.copy(layerKey = layerKey) else it },
                stamps = m.stamps.map { if (it.key == itemKey) it.copy(layerKey = layerKey) else it },
            )
        }

    /** `보이는 시점` of any overlay or layer (WLD-49). */
    fun setRevealPoint(doc: ProjectDoc, mapKey: Key, itemKey: Key, point: PointRef?, ctx: EditContext): ProjectDoc? =
        updateMap(doc, mapKey, ctx) { m ->
            m.copy(
                layers = m.layers.map { if (it.key == itemKey) it.copy(revealPoint = point) else it },
                pins = m.pins.map { if (it.key == itemKey) it.copy(revealPoint = point) else it },
                regions = m.regions.map { if (it.key == itemKey) it.copy(revealPoint = point) else it },
                routes = m.routes.map { if (it.key == itemKey) it.copy(revealPoint = point) else it },
                labels = m.labels.map { if (it.key == itemKey) it.copy(revealPoint = point) else it },
                stamps = m.stamps.map { if (it.key == itemKey) it.copy(revealPoint = point) else it },
                nodes = m.nodes.map { if (it.key == itemKey) it.copy(revealPoint = point) else it },
                nodeLinks = m.nodeLinks.map { if (it.key == itemKey) it.copy(revealPoint = point) else it },
            )
        }

    // --- Stamps ---

    fun placeStamp(
        doc: ProjectDoc,
        mapKey: Key,
        kind: StampKind,
        at: Point,
        ctx: EditContext,
        size: Int = 2,
        rotation: Int = 0,
        placeKey: Key? = null,
    ): MapEdit<Stamp> {
        if (size !in 1..3) return MapEdit.Refused(MapRefusal.BAD_SIZE)
        val p = MapGeometry.clamp(at)
        val stamp = Stamp(
            key = ctx.newKey(), kind = kind.open(), x = p.x, y = p.y, size = size, rotation = snapRotation(rotation),
            placeKey = placeKey, updatedAt = ctx.now(), updatedBy = ctx.device,
        )
        return applied(updateMap(doc, mapKey, ctx) { it.copy(stamps = it.stamps + stamp) }, stamp)
    }

    fun updateStamp(doc: ProjectDoc, mapKey: Key, stampKey: Key, ctx: EditContext, f: (Stamp) -> Stamp): ProjectDoc? =
        updateMap(doc, mapKey, ctx) { m ->
            m.copy(stamps = m.stamps.map { if (it.key == stampKey) f(it).let { s -> s.copy(rotation = snapRotation(s.rotation), size = s.size.coerceIn(1, 3)) } else it })
        }

    // --- Node maps ---

    fun addNode(doc: ProjectDoc, mapKey: Key, name: String, at: Point, ctx: EditContext, sub: String? = null, placeKey: Key? = null): MapEdit<NodeMapNode> {
        val clean = name.trim()
        if (clean.isEmpty()) return MapEdit.Refused(MapRefusal.EMPTY_NAME)
        val p = MapGeometry.clamp(at)
        val node = NodeMapNode(ctx.newKey(), clean, sub?.trim()?.takeIf { it.isNotEmpty() }, placeKey, p.x, p.y, null, ctx.now(), ctx.device)
        return applied(updateMap(doc, mapKey, ctx) { it.copy(nodes = it.nodes + node) }, node)
    }

    fun linkNodes(doc: ProjectDoc, mapKey: Key, a: Key, b: Key, ctx: EditContext, label: String? = null): MapEdit<NodeMapLink> {
        val map = doc.maps[mapKey] ?: return MapEdit.Refused(MapRefusal.NO_MAP)
        if (a == b || map.nodes.none { it.key == a } || map.nodes.none { it.key == b }) return MapEdit.Refused(MapRefusal.NO_ITEM)
        val link = NodeMapLink(ctx.newKey(), a, b, label?.trim()?.takeIf { it.isNotEmpty() }, null, ctx.now(), ctx.device)
        return applied(updateMap(doc, mapKey, ctx) { it.copy(nodeLinks = it.nodeLinks + link) }, link)
    }

    /** Pick radius (map units) for snapping a tap to a pin. */
    const val SNAP_RADIUS = 0.02
}

/**
 * A region being drawn (`새 영역 · 그리는 중`): taps add vertices, `되돌리기` removes the last,
 * `닫기` (≥ 3 vertices, button only) closes it.
 */
data class RegionDraft(val vertices: List<Point> = emptyList()) {
    fun add(p: Point): RegionDraft = copy(vertices = vertices + MapGeometry.clamp(p))
    fun undo(): RegionDraft = copy(vertices = vertices.dropLast(1))
    val canClose: Boolean get() = vertices.size >= 3

    /** `꼭짓점 N · 되돌리기 · 닫기`. */
    val bar: String get() = "꼭짓점 ${vertices.size} · 되돌리기 · 닫기"
}

/** A route being drawn: each tap adds a segment; points may snap to pins (WLD-43). */
data class RouteDraft(val points: List<RoutePoint> = emptyList()) {
    /** Adds a tap, snapped to a pin of [map] within the pick radius. */
    fun add(map: WorldMap, at: Point): RouteDraft {
        val pin = MapEdits.pinNear(map, at)
        val p = MapGeometry.clamp(at)
        return copy(points = points + (pin?.let { RoutePoint.pin(it.key) } ?: RoutePoint.at(p.x, p.y)))
    }

    fun undo(): RouteDraft = copy(points = points.dropLast(1))

    /** Length so far in map units (the distance counter). */
    fun length(map: WorldMap): Double = Routes.length(map, Route(key = "", points = points))
}
