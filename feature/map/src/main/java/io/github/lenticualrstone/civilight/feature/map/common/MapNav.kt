package io.github.lenticualrstone.civilight.feature.map.common

import io.github.lenticualrstone.civilight.domain.world.map.MapScales
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.MapView
import io.github.lenticualrstone.civilight.navigation.Navigator
import io.github.lenticualrstone.civilight.navigation.NodeMap
import io.github.lenticualrstone.civilight.navigation.Route

/** Which screen shows a map: node maps open as 노드 지도 (WLD-50), every other map as 지도 보기. */
object MapNav {
    fun routeOf(doc: ProjectDoc?, projectKey: Key, mapKey: Key): Route {
        // (0.2.0, D11, WLD-75) Node maps open view-only in 지도 보기 too; the node editor is hidden.
        val map = doc?.maps?.get(mapKey)
        return if (map != null && MapScales.isNodeMap(map) && MapOffers.editable(map)) NodeMap(projectKey, mapKey) else MapView(projectKey, mapKey)
    }

    /** Opens a map (list card, `하위 지도` row). */
    fun open(navigator: Navigator, doc: ProjectDoc?, projectKey: Key, mapKey: Key) = navigator.push(routeOf(doc, projectKey, mapKey))

    /**
     * A breadcrumb part (WLD-48): goes back to that map when it is already on the back stack (zoom
     * out to it), else opens it.
     */
    fun crumb(navigator: Navigator, doc: ProjectDoc?, projectKey: Key, mapKey: Key) {
        val target = routeOf(doc, projectKey, mapKey)
        val onStack = navigator.backStack.any { it is MapView && it.mapKey == mapKey || it is NodeMap && it.mapKey == mapKey }
        if (onStack) navigator.popTo(false) { it is MapView && it.mapKey == mapKey || it is NodeMap && it.mapKey == mapKey } else navigator.push(target)
    }
}
