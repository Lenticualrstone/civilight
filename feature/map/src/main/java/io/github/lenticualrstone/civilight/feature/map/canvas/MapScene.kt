package io.github.lenticualrstone.civilight.feature.map.canvas

import io.github.lenticualrstone.civilight.domain.world.map.MapReveal
import io.github.lenticualrstone.civilight.domain.world.map.OverlayKind
import io.github.lenticualrstone.civilight.domain.world.map.RegionShapes
import io.github.lenticualrstone.civilight.domain.world.map.Routes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapOverlay
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.position.LensPoint

/** How a map is shown: what is drawn and what taps do. */
enum class SceneMode {
    /** 지도 보기: items not yet revealed at the lens are left out (or dimmed with `숨은 것 흐리게`). */
    VIEW,

    /** 편집: every live item, the ones not yet revealed dimmed, so they can still be edited. */
    EDIT,

    /** 지도에서 고르기: as [VIEW]. */
    PICK,
}

data class ScenePin(val key: Key, val placeKey: Key, val name: String, val at: Point, val dim: Boolean)

data class SceneRegion(val key: Key, val name: String, val color: String, val shape: List<Point>, val dim: Boolean)

data class SceneRoute(val key: Key, val points: List<Point>, val label: String, val dim: Boolean)

data class SceneLabel(val key: Key, val text: String, val at: Point, val curve: List<Point>?, val dim: Boolean)

data class SceneNode(val key: Key, val name: String, val sub: String?, val at: Point, val dim: Boolean)

data class SceneLink(val key: Key, val a: Point, val b: Point, val label: String?, val dim: Boolean)

/** Everything drawn over a map's base, in 0–1 coordinates, in drawing order. */
data class MapScene(
    val regions: List<SceneRegion> = emptyList(),
    val routes: List<SceneRoute> = emptyList(),
    val links: List<SceneLink> = emptyList(),
    val labels: List<SceneLabel> = emptyList(),
    val nodes: List<SceneNode> = emptyList(),
    val pins: List<ScenePin> = emptyList(),
) {
    fun pin(key: Key): ScenePin? = pins.firstOrNull { it.key == key }

    fun region(key: Key): SceneRegion? = regions.firstOrNull { it.key == key }
}

/**
 * Builds the [MapScene] of a map at a lens from the domain readings: live overlays only, hidden
 * places and factions masked while locked, switched-off layers left out (`MapReveal.at`), region
 * shapes at the lens (`RegionShapes.shapeAt`, WLD-65), route chips from `Routes.sheet` (WLD-43).
 * [layerFilter] keeps only the overlays of one layer (the view's layer chips; null = `전체 핀`).
 */
object MapScenes {
    fun build(
        doc: ProjectDoc,
        map: WorldMap,
        lens: LensPoint,
        hiddenOpen: Boolean,
        mode: SceneMode,
        layerFilter: Key? = null,
    ): MapScene {
        val reveal = MapReveal.at(doc, map, lens, hiddenOpen)
        val byKey = reveal.items.associateBy { it.key }

        /** Null when the item is not drawn; else whether it is dimmed. */
        fun dimOf(key: Key, layerKey: Key?): Boolean? {
            val item = byKey[key] ?: return null
            if (layerFilter != null && layerKey != layerFilter) return null
            return when {
                item.visible -> false
                mode == SceneMode.EDIT -> true
                map.revealViewDim -> true
                else -> null
            }
        }

        val pins = map.pins.mapNotNull { pin ->
            val dim = dimOf(pin.key, pin.layerKey) ?: return@mapNotNull null
            val name = doc.index.place(pin.placeKey)?.name.orEmpty()
            ScenePin(pin.key, pin.placeKey, name, Point(pin.x, pin.y), dim)
        }
        val regions = map.regions.mapNotNull { r ->
            val dim = dimOf(r.key, r.layerKey) ?: return@mapNotNull null
            SceneRegion(r.key, r.name, r.color, RegionShapes.shapeAt(doc, map.key, r, lens), dim)
        }
        val routes = map.routes.mapNotNull { r ->
            val dim = dimOf(r.key, r.layerKey) ?: return@mapNotNull null
            val points = Routes.points(map, r)
            if (points.size < 2) return@mapNotNull null
            SceneRoute(r.key, points, Routes.sheet(doc, map, r).mapLabel, dim)
        }
        val labels = map.labels.mapNotNull { l ->
            val dim = dimOf(l.key, l.layerKey) ?: return@mapNotNull null
            SceneLabel(l.key, l.text, Point(l.x, l.y), l.curve, dim)
        }
        val nodes = map.nodes.mapNotNull { n ->
            val dim = dimOf(n.key, null) ?: return@mapNotNull null
            SceneNode(n.key, n.name, n.sub, Point(n.x, n.y), dim)
        }
        val nodeAt = nodes.associateBy { it.key }
        val links = map.nodeLinks.mapNotNull { l ->
            val dim = dimOf(l.key, null) ?: return@mapNotNull null
            val a = nodeAt[l.aNodeKey] ?: return@mapNotNull null
            val b = nodeAt[l.bNodeKey] ?: return@mapNotNull null
            SceneLink(l.key, a.at, b.at, l.label, dim || a.dim || b.dim)
        }
        return MapScene(regions, routes, links, labels, nodes, pins)
    }

    /** The overlay kind of a key on [map], or null. */
    fun kindOf(map: WorldMap, key: Key): OverlayKind? = when {
        map.pins.any { it.key == key } -> OverlayKind.PIN
        map.regions.any { it.key == key } -> OverlayKind.REGION
        map.routes.any { it.key == key } -> OverlayKind.ROUTE
        map.labels.any { it.key == key } -> OverlayKind.LABEL
        map.stamps.any { it.key == key } -> OverlayKind.STAMP
        map.nodes.any { it.key == key } -> OverlayKind.NODE
        map.nodeLinks.any { it.key == key } -> OverlayKind.NODE_LINK
        else -> null
    }

    /** The layer key of an overlay (pins, regions, routes, labels, stamps). */
    fun layerOf(map: WorldMap, key: Key): Key? = overlay(map, key)?.let {
        when (it) {
            is io.github.lenticualrstone.civilight.model.map.Pin -> it.layerKey
            is io.github.lenticualrstone.civilight.model.map.MapRegion -> it.layerKey
            is io.github.lenticualrstone.civilight.model.map.Route -> it.layerKey
            is io.github.lenticualrstone.civilight.model.map.MapLabel -> it.layerKey
            is io.github.lenticualrstone.civilight.model.map.Stamp -> it.layerKey
            else -> null
        }
    }

    fun overlay(map: WorldMap, key: Key): MapOverlay? =
        map.pins.firstOrNull { it.key == key } ?: map.regions.firstOrNull { it.key == key }
            ?: map.routes.firstOrNull { it.key == key } ?: map.labels.firstOrNull { it.key == key }
            ?: map.stamps.firstOrNull { it.key == key } ?: map.nodes.firstOrNull { it.key == key }
            ?: map.nodeLinks.firstOrNull { it.key == key }
}
