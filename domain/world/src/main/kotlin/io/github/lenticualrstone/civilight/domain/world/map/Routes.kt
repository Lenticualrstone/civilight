package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.Pin
import io.github.lenticualrstone.civilight.model.map.Route
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.project.TravelMode

/** A route end attached to a pin, and whether that pin is live (a deleted place leaves a dotted end). */
data class RouteEnd(val pinKey: Key, val placeKey: Key?, val live: Boolean)

/** The route sheet (WLD-43). */
data class RouteSheet(
    /** `결사의 문 → 상층 관문`. */
    val title: String,
    /** `2일 4시간`, or null without a scale. */
    val duration: String?,
    /** `38 km · 지점 3`, or `축척 없음`. */
    val detail: String,
    /** `축척 맞추기` when there is no scale. */
    val action: String?,
    /** `도보 15 km · 8시간 · 말 · 배 · 직접`: the chosen mode with its numbers, the others by name. */
    val modeChips: List<String>,
    /** Chip on the map: `결사의 문 → 상층 관문 · 도보 2일 4시간 · 38 km`. */
    val mapLabel: String,
    val km: Double?,
)

/** Route geometry and labels. */
object Routes {
    /** Points of a route; a pin point follows its pin (a missing pin drops out). */
    fun points(map: WorldMap, route: Route): List<Point> = route.points.mapNotNull { rp ->
        val pinKey = rp.pinKey
        val x = rp.x
        val y = rp.y
        when {
            pinKey != null -> map.pins.firstOrNull { it.key == pinKey }?.let { Point(it.x, it.y) }
            x != null && y != null -> Point(x, y)
            else -> null
        }
    }

    /** Length in map units. */
    fun length(map: WorldMap, route: Route): Double = MapGeometry.length(points(map, route))

    /** Length in km, or null without a scale. */
    fun km(doc: ProjectDoc, map: WorldMap, route: Route): Double? = MapScales.kmPerUnit(doc, map)?.let { it * length(map, route) }

    /**
     * Length between two pins along the route (the part between their first occurrences), or null
     * when the route does not pass both.
     */
    fun lengthBetween(map: WorldMap, route: Route, pinA: Key, pinB: Key): Double? {
        val i = route.points.indexOfFirst { it.pinKey == pinA }
        val j = route.points.indexOfFirst { it.pinKey == pinB }
        if (i < 0 || j < 0) return null
        val part = route.points.subList(minOf(i, j), maxOf(i, j) + 1)
        return MapGeometry.length(points(map, route.copy(points = part)))
    }

    /** The pin ends of a route with their liveness. */
    fun ends(doc: ProjectDoc, map: WorldMap, route: Route): List<RouteEnd> =
        listOfNotNull(route.points.firstOrNull(), route.points.lastOrNull().takeIf { route.points.size > 1 })
            .mapNotNull { it.pinKey }
            .map { key ->
                val pin: Pin? = map.pins.firstOrNull { it.key == key }
                RouteEnd(key, pin?.placeKey, pin != null && doc.liveness.isOverlayLive(map.key, pin))
            }

    private fun endName(doc: ProjectDoc, map: WorldMap, route: Route, first: Boolean): String {
        val rp = if (first) route.points.firstOrNull() else route.points.lastOrNull()
        val pin = rp?.pinKey?.let { k -> map.pins.firstOrNull { it.key == k } } ?: return "지점"
        return doc.index.place(pin.placeKey)?.name ?: "지점"
    }

    /** The sheet of a route with the chosen mode (the route's shown mode, else the project default). */
    fun sheet(doc: ProjectDoc, map: WorldMap, route: Route, chosen: TravelMode? = null): RouteSheet {
        val mode = chosen ?: Travel.mode(doc, route.shownModeKey) ?: Travel.defaultMode(doc)
        val title = "${endName(doc, map, route, true)} → ${endName(doc, map, route, false)}"
        val km = km(doc, map, route)
        val duration = km?.let { Travel.formatDuration(Travel.requiredMinutes(it, mode)) }
        val points = route.points.size
        val chips = Travel.modes(doc).map { if (it.key == mode.key) Travel.modeLabel(it) else it.name } + "직접"
        return RouteSheet(
            title = title,
            duration = duration,
            detail = if (km == null) "축척 없음" else "${Travel.number(Math.round(km).toDouble())} km${Copy.SEP}지점 $points",
            action = if (km == null) "축척 맞추기" else null,
            modeChips = chips,
            mapLabel = if (km == null) title else "$title${Copy.SEP}${mode.name} $duration${Copy.SEP}${Travel.number(Math.round(km).toDouble())} km",
            km = km,
        )
    }
}
