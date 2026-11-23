package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** (0.2.0, WLD-75) Default names of new maps. */
object MapNames {
    const val PREFIX = "지도"

    /**
     * `지도 N` for a new map: N is the smallest number ≥ 1 for which no live map of the project is named
     * `지도 N` (trashed maps do not hold a number).
     */
    fun nextDefault(doc: ProjectDoc): String {
        val taken = doc.maps.values.asSequence()
            .filter { doc.liveness.isMapLive(it.key) }
            .map { it.name.trim() }
            .toSet()
        var n = 1
        while ("$PREFIX $n" in taken) n++
        return "$PREFIX $n"
    }
}
