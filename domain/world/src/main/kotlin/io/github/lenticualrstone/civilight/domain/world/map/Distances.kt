package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.Pin
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind

enum class DistanceKind { ROUTE, STRAIGHT, ZERO }

/** Why no distance: no scale (`◇ 축척 없음 · 점검 못 함`) or no location (deleted place, no pin). */
enum class DistanceProblem { NO_SCALE, NO_LOCATION }

/** Result of [Distances.distanceKm] (WLD-66); [km] is null exactly when [unavailable] is set. */
data class Distance(val km: Double?, val kind: DistanceKind?, val unavailable: DistanceProblem?, val mapKey: Key? = null) {
    companion object {
        fun problem(p: DistanceProblem) = Distance(null, null, p)
    }
}

/**
 * Distances between places and regions for the character track and the travel check (WLD-66):
 * same place → 0; two pins joined by a route → the shortest route part; else the straight line
 * (a lower bound); a region → the nearest points of the shapes; pins on different maps → their
 * ancestor pins on the nearest common ancestor map; no scale or a node map → `NO_SCALE`; a deleted
 * or unplaced place → `NO_LOCATION` (never from trashed coordinates). No detour factor.
 */
class Distances(private val doc: ProjectDoc, private val lens: LensPoint = LensPoint.Latest) {

    /** Where something is on one map. */
    private sealed interface Loc {
        val map: WorldMap

        data class AtPin(override val map: WorldMap, val pin: Pin) : Loc
        data class AtPoint(override val map: WorldMap, val point: Point) : Loc
        data class InRegion(override val map: WorldMap, val shape: List<Point>) : Loc
    }

    private fun liveMap(key: Key): WorldMap? = doc.maps[key]?.takeIf { doc.liveness.isMapLive(key) }

    /** Direct locations of a ref, or null when the ref's item is not live. */
    private fun locations(ref: PlaceRef): List<Loc>? = when (ref.kind.known) {
        PlaceRefKind.PLACE -> {
            if (!doc.liveness.isPlaceLive(ref.key)) null
            else doc.maps.values.filter { doc.liveness.isMapLive(it.key) }.sortedWith(compareBy({ it.order }, { it.key })).flatMap { map ->
                map.pins.filter { it.placeKey == ref.key && doc.liveness.isOverlayLive(map.key, it) }.map { Loc.AtPin(map, it) } +
                    map.nodes.filter { it.placeKey == ref.key }.map { Loc.AtPoint(map, Point(it.x, it.y)) }
            }
        }
        PlaceRefKind.REGION -> {
            val (map, region) = doc.index.region(ref.key) ?: return null
            if (!doc.liveness.isMapLive(map.key)) null
            else listOf(Loc.InRegion(map, RegionShapes.shapeAt(doc, map.key, region, lens)))
        }
        null -> null
    }

    /** [loc] and its ancestors: the parent pin on the parent map, recursively. */
    private fun chain(loc: Loc): List<Loc> {
        val out = mutableListOf(loc)
        val seen = mutableSetOf(loc.map.key)
        var map = loc.map
        while (true) {
            val parent = map.parent ?: break
            val parentMap = liveMap(parent.mapKey) ?: break
            val pin = parentMap.pins.firstOrNull { it.key == parent.pinKey } ?: break
            if (!seen.add(parentMap.key) || !doc.liveness.isOverlayLive(parentMap.key, pin)) break
            out += Loc.AtPin(parentMap, pin)
            map = parentMap
        }
        return out
    }

    private data class Option(val cost: Int, val distance: Distance)

    fun distanceKm(from: PlaceRef, to: PlaceRef): Distance {
        val a = locations(from) ?: return Distance.problem(DistanceProblem.NO_LOCATION)
        val b = locations(to) ?: return Distance.problem(DistanceProblem.NO_LOCATION)
        if (from.sameAs(to)) return Distance(0.0, DistanceKind.ZERO, null)
        if (a.isEmpty() || b.isEmpty()) return Distance.problem(DistanceProblem.NO_LOCATION)
        val options = ArrayList<Option>()
        for (la in a) for (lb in b) {
            val ca = chain(la)
            val cb = chain(lb)
            for ((i, x) in ca.withIndex()) for ((j, y) in cb.withIndex()) {
                if (x.map.key == y.map.key) options += Option(i + j, measure(x, y))
            }
        }
        if (options.isEmpty()) return Distance.problem(DistanceProblem.NO_LOCATION)
        return options.sortedWith(
            compareBy<Option> { it.cost }
                .thenBy { if (it.distance.unavailable == null) 0 else 1 }
                .thenBy { it.distance.km ?: Double.MAX_VALUE },
        ).first().distance
    }

    private fun measure(x: Loc, y: Loc): Distance {
        val map = x.map
        val kmPerUnit = MapScales.kmPerUnit(doc, map) ?: return Distance(null, null, DistanceProblem.NO_SCALE, map.key)
        if (x is Loc.AtPin && y is Loc.AtPin) {
            if (x.pin.key == y.pin.key) return Distance(0.0, DistanceKind.ZERO, null, map.key)
            val routed = map.routes.mapNotNull { Routes.lengthBetween(map, it, x.pin.key, y.pin.key) }.minOrNull()
            if (routed != null) return Distance(routed * kmPerUnit, DistanceKind.ROUTE, null, map.key)
        }
        val units = when {
            x is Loc.InRegion && y is Loc.InRegion -> MapGeometry.polygonToPolygon(x.shape, y.shape)
            x is Loc.InRegion -> MapGeometry.pointToPolygon(pointOf(y), x.shape)
            y is Loc.InRegion -> MapGeometry.pointToPolygon(pointOf(x), y.shape)
            else -> MapGeometry.distance(pointOf(x), pointOf(y))
        }
        val kind = if (units == 0.0) DistanceKind.ZERO else DistanceKind.STRAIGHT
        return Distance(units * kmPerUnit, kind, null, map.key)
    }

    private fun pointOf(loc: Loc): Point = when (loc) {
        is Loc.AtPin -> Point(loc.pin.x, loc.pin.y)
        is Loc.AtPoint -> loc.point
        is Loc.InRegion -> loc.shape.first()
    }
}
