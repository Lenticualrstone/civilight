package io.github.lenticualrstone.civilight.domain.story.travel

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.ScaleUnit
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import kotlin.math.hypot
import kotlin.math.roundToLong

/** Distance between two scene places (WLD-66 as used by STO-29). */
sealed interface Distance {
    /** Same place: no check. */
    data object SamePlace : Distance

    /** Along a route that connects both pins. */
    data class Route(val km: Double, val mapKey: Key) : Distance

    /** Straight line (a lower bound), flag `경로 없음`. */
    data class Straight(val km: Double, val mapKey: Key) : Distance

    /** Map without scale, a node map, or no common map: `◇ 축척 없음 · 점검 못 함`. */
    data class NoScale(val mapKey: Key?) : Distance
}

/**
 * Map distances (WLD-66): route length when a route connects the two pins, else the straight line
 * between nearest points (regions use their polygons); pins on child maps are lifted to their
 * ancestor pins on the common parent map; no detour multiplier. Coordinates are isotropic in the
 * base's width (`ED`: `y` scaled by `h/w` when the base size is known). Distances are rounded to
 * metres (`ED`) so that `floor(d / k)` in [TravelModes.requiredMinutes] is stable.
 */
class Distances(private val doc: ProjectDoc) {
    private data class Pt(val x: Double, val y: Double)

    /** A location on one map: a point (pin) or a polygon (region); [lifted] = a parent pin standing for it. */
    private data class Loc(val map: WorldMap, val points: List<Pt>, val pinKey: Key?, val lifted: Boolean, val depth: Int) {
        val isPoint: Boolean get() = points.size == 1
    }

    fun between(a: PlaceRef, b: PlaceRef): Distance {
        if (a.sameAs(b)) return Distance.SamePlace
        val la = locations(a)
        val lb = locations(b)
        var best: Distance? = null
        var bestRank = Int.MAX_VALUE
        for (x in la) for (y in lb) {
            val ca = chain(x)
            val cb = chain(y)
            for (p in ca) {
                val q = cb.firstOrNull { it.map.key == p.map.key } ?: continue
                val rank = p.depth + q.depth
                val d = onMap(p, q)
                val better = when {
                    best == null || rank < bestRank -> true
                    rank > bestRank -> false
                    else -> preference(d) < preference(best) || (preference(d) == preference(best) && km(d) < km(best))
                }
                if (better) {
                    best = d
                    bestRank = rank
                }
                break
            }
        }
        return best ?: Distance.NoScale(null)
    }

    private fun preference(d: Distance) = when (d) {
        is Distance.Route -> 0
        is Distance.Straight -> 1
        Distance.SamePlace -> 0
        is Distance.NoScale -> 2
    }

    private fun km(d: Distance) = when (d) {
        is Distance.Route -> d.km
        is Distance.Straight -> d.km
        else -> Double.MAX_VALUE
    }

    private fun locations(ref: PlaceRef): List<Loc> = when (ref.kind.known) {
        PlaceRefKind.PLACE -> placeLocations(ref.key, HashSet())
        PlaceRefKind.REGION -> doc.index.region(ref.key)
            ?.takeIf { (map, region) -> doc.liveness.isOverlayLive(map.key, region) }
            ?.let { (map, region) -> listOf(Loc(map, region.vertices.map { Pt(it.x.toDouble(), it.y.toDouble()) }, null, false, 0)) }
            .orEmpty()
        null -> emptyList()
    }

    /** Pins of a place; a place without a pin uses its parent place's (`ED`). */
    private fun placeLocations(placeKey: Key, seen: MutableSet<Key>): List<Loc> {
        if (!seen.add(placeKey)) return emptyList()
        val pins = doc.maps.values.flatMap { map ->
            map.pins.filter { it.placeKey == placeKey && doc.liveness.isOverlayLive(map.key, it) }
                .map { Loc(map, listOf(Pt(it.x.toDouble(), it.y.toDouble())), it.key, false, 0) }
        }
        if (pins.isNotEmpty()) return pins
        val parent = doc.index.place(placeKey)?.parentPlaceKey ?: return emptyList()
        return placeLocations(parent, seen)
    }

    /** The location and its stand-ins on every ancestor map (via `WorldMap.parent`). */
    private fun chain(start: Loc): List<Loc> {
        val out = mutableListOf(start)
        var current = start
        val seen = mutableSetOf(start.map.key)
        while (true) {
            val parent = current.map.parent ?: break
            val parentMap = doc.maps[parent.mapKey]?.takeIf { doc.liveness.isMapLive(it.key) } ?: break
            if (!seen.add(parentMap.key)) break
            val pin = parentMap.pins.firstOrNull { it.key == parent.pinKey } ?: break
            current = Loc(parentMap, listOf(Pt(pin.x.toDouble(), pin.y.toDouble())), pin.key, true, current.depth + 1)
            out += current
        }
        return out
    }

    private fun onMap(a: Loc, b: Loc): Distance {
        val map = a.map
        if (map.baseKind.known == MapBaseKind.NODE || map.format?.known == MapFormat.NODE) return Distance.NoScale(map.key)
        val perUnit = kmPerUnit(map) ?: return Distance.NoScale(map.key)
        if (!a.lifted && !b.lifted && a.pinKey != null && b.pinKey != null) {
            routeLength(map, a.pinKey, b.pinKey)?.let { return Distance.Route(round(it * perUnit), map.key) }
        }
        return Distance.Straight(round(geometryDistance(map, a.points, b.points) * perUnit), map.key)
    }

    /** km per map unit from `축척 맞추기` (WLD-46); null when it cannot be expressed in km. */
    fun kmPerUnit(map: WorldMap): Double? {
        val scale = map.scale ?: return null
        val unitDist = dist(map, Pt(scale.a.x.toDouble(), scale.a.y.toDouble()), Pt(scale.b.x.toDouble(), scale.b.y.toDouble()))
        if (unitDist <= 0.0 || scale.value <= 0.0) return null
        val km = when (scale.unit.known) {
            ScaleUnit.KM -> scale.value
            ScaleUnit.DAY -> {
                val mode = scale.travelModeKey?.let { k -> doc.project.travelModes.firstOrNull { it.key == k } } ?: TravelModes.default(doc)
                mode?.let { scale.value * it.kmPerDay } ?: return null
            }
            ScaleUnit.LI -> scale.value * KM_PER_LI
            ScaleUnit.CUSTOM, null -> return null
        }
        return km / unitDist
    }

    /** Shortest length along a live route through both pins (between their positions on it). */
    private fun routeLength(map: WorldMap, pinA: Key, pinB: Key): Double? {
        val pinsByKey = map.pins.associateBy { it.key }
        return map.routes.filter { doc.liveness.isOverlayLive(map.key, it) }.mapNotNull { route ->
            val pts = route.points.map { p ->
                p.pinKey?.let { k -> pinsByKey[k]?.let { Pt(it.x.toDouble(), it.y.toDouble()) } } ?: Pt((p.x ?: 0f).toDouble(), (p.y ?: 0f).toDouble())
            }
            val i = route.points.indexOfFirst { it.pinKey == pinA }
            val j = route.points.indexOfFirst { it.pinKey == pinB }
            if (i < 0 || j < 0) return@mapNotNull null
            val (from, to) = if (i <= j) i to j else j to i
            (from until to).sumOf { dist(map, pts[it], pts[it + 1]) }
        }.minOrNull()
    }

    private fun geometryDistance(map: WorldMap, a: List<Pt>, b: List<Pt>): Double {
        if (a.size == 1 && b.size == 1) return dist(map, a[0], b[0])
        if (a.size >= 3 && a.size != 1 && b.any { inside(it, a) }) return 0.0
        if (b.size >= 3 && a.any { inside(it, b) }) return 0.0
        var best = Double.MAX_VALUE
        for (p in a) best = minOf(best, toPolyline(map, p, b))
        for (p in b) best = minOf(best, toPolyline(map, p, a))
        return best
    }

    private fun toPolyline(map: WorldMap, p: Pt, poly: List<Pt>): Double {
        if (poly.size == 1) return dist(map, p, poly[0])
        var best = Double.MAX_VALUE
        for (i in poly.indices) best = minOf(best, toSegment(map, p, poly[i], poly[(i + 1) % poly.size]))
        return best
    }

    private fun toSegment(map: WorldMap, p: Pt, a: Pt, b: Pt): Double {
        val sy = yScale(map)
        val ax = a.x; val ay = a.y * sy; val bx = b.x; val by = b.y * sy; val px = p.x; val py = p.y * sy
        val dx = bx - ax
        val dy = by - ay
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0.0, 1.0)
        return hypot(px - (ax + t * dx), py - (ay + t * dy))
    }

    private fun inside(p: Pt, poly: List<Pt>): Boolean {
        var c = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val pi = poly[i]
            val pj = poly[j]
            if ((pi.y > p.y) != (pj.y > p.y) && p.x < (pj.x - pi.x) * (p.y - pi.y) / (pj.y - pi.y) + pi.x) c = !c
            j = i
        }
        return c
    }

    private fun yScale(map: WorldMap): Double = map.baseSize?.takeIf { it.w > 0 }?.let { it.h.toDouble() / it.w } ?: 1.0

    private fun dist(map: WorldMap, a: Pt, b: Pt): Double = hypot(b.x - a.x, (b.y - a.y) * yScale(map))

    private fun round(km: Double): Double = (km * 1000).roundToLong() / 1000.0

    companion object {
        /** `ED`: one 리 = 0.4 km. */
        const val KM_PER_LI = 0.4
    }
}
