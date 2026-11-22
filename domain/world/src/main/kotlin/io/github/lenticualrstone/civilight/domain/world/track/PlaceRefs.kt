package io.github.lenticualrstone.civilight.domain.world.track

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapRegion
import io.github.lenticualrstone.civilight.model.map.Pin
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.domain.world.map.MapGeometry

/** Lookups shared by the map ↔ timeline readings: live pins of a place, regions holding it, names. */
internal object PlaceRefs {
    /** Live pins of a place on live maps, maps in order. */
    fun livePins(doc: ProjectDoc, placeKey: Key): List<Pair<WorldMap, Pin>> =
        doc.maps.values.filter { doc.liveness.isMapLive(it.key) }.sortedWith(compareBy({ it.order }, { it.key })).flatMap { map ->
            map.pins.filter { it.placeKey == placeKey && doc.liveness.isOverlayLive(map.key, it) }.map { map to it }
        }

    /** Live regions whose shape holds one of the place's live pins (the place's `지역` on a map). */
    fun regionsHolding(doc: ProjectDoc, placeKey: Key): List<Pair<WorldMap, MapRegion>> =
        livePins(doc, placeKey).flatMap { (map, pin) ->
            map.regions.filter { MapGeometry.contains(it.vertices, Point(pin.x, pin.y)) }.map { map to it }
        }

    /** True when the ref's place or region exists and is live (a deleted place is `NO_LOCATION`). */
    fun isLive(doc: ProjectDoc, ref: PlaceRef?): Boolean = when (ref?.kind?.known) {
        PlaceRefKind.PLACE -> doc.liveness.isPlaceLive(ref.key)
        PlaceRefKind.REGION -> doc.index.region(ref.key)?.let { doc.liveness.isMapLive(it.first.key) } ?: false
        null -> false
    }

    /** The name of a live ref, or null. */
    fun name(doc: ProjectDoc, ref: PlaceRef?): String? = when (ref?.kind?.known) {
        PlaceRefKind.PLACE -> doc.index.place(ref.key)?.name
        PlaceRefKind.REGION -> doc.index.region(ref.key)?.second?.name
        null -> null
    }

    /** True when [ref] names [placeKey] directly or through a region holding the place's pin. */
    fun refersTo(doc: ProjectDoc, ref: PlaceRef?, placeKey: Key, regionKeys: Set<Key>): Boolean = when (ref?.kind?.known) {
        PlaceRefKind.PLACE -> ref.key == placeKey
        PlaceRefKind.REGION -> ref.key in regionKeys
        null -> false
    }
}
