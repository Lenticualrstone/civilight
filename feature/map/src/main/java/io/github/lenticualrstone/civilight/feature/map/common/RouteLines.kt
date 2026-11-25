package io.github.lenticualrstone.civilight.feature.map.common

import io.github.lenticualrstone.civilight.domain.world.map.RouteSheet
import io.github.lenticualrstone.civilight.domain.world.map.Routes
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.Route
import io.github.lenticualrstone.civilight.model.map.WorldMap

/**
 * The route sheet and map chip as the map screens show them (WLD-43, boards `MapRouteC`, `MapEdit`).
 *
 * - An end whose pin is not drawn (a hidden place while locked, rule 19) reads `지점`, never the place name.
 * - With a scale: `38 km · 지점 3`, `2일 4시간`, the mode chips (`도보 15 km · 8시간 · 말 · 배 · 직접`) and the map
 *   chip `A → B · 도보 2일 4시간 · 38 km` (board `MapRouteC`).
 * - Without one: `축척 없음`, no duration, `축척 맞추기`, the mode chips, and the map chip is the title alone.
 * - Where no scale applies ([MapOffers.showsScale] false: a node map) the sheet keeps only title and `지점 N`.
 */
object RouteLines {
    fun sheet(doc: ProjectDoc, map: WorldMap, route: Route, drawnPins: Set<Key>): RouteSheet {
        val full = Routes.sheet(doc, map, route)
        val title = "${endName(doc, map, route, true, drawnPins)}${MapCopy.ARROW}${endName(doc, map, route, false, drawnPins)}"
        val mapLabel = title + full.mapLabel.removePrefix(full.title)
        if (MapOffers.showsScale(map)) return full.copy(title = title, mapLabel = mapLabel)
        return RouteSheet(
            title = title,
            duration = null,
            detail = MapCopy.routeDraftBar(route.points.size, null),
            action = null,
            modeChips = emptyList(),
            mapLabel = title,
            km = null,
        )
    }

    /** The draft bar while drawing: `지점 3`, with `· 12 km` only while distances are shown. */
    fun draftBar(map: WorldMap, points: Int, km: String?): String =
        MapCopy.routeDraftBar(points, km.takeIf { MapOffers.showsScale(map) })

    private fun endName(doc: ProjectDoc, map: WorldMap, route: Route, first: Boolean, drawnPins: Set<Key>): String {
        val rp = if (first) route.points.firstOrNull() else route.points.lastOrNull()
        val pin = rp?.pinKey?.takeIf { it in drawnPins }?.let { k -> map.pins.firstOrNull { it.key == k } } ?: return MapCopy.ROUTE_POINT
        return doc.index.place(pin.placeKey)?.name ?: MapCopy.ROUTE_POINT
    }
}
