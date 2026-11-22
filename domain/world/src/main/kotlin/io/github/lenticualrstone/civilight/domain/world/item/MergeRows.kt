package io.github.lenticualrstone.civilight.domain.world.item

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * World-specific rows of the merge sheet (WLD-15). The merge itself (references, trash entry, undo)
 * is the shared shell's (`domain:trash`); this area supplies only what its rows say.
 */
object MergeRows {
    /** Route ends on maps where both places have a pin, which move to the target's pin. */
    fun routeEnds(doc: ProjectDoc, mergedPlace: Key, targetPlace: Key): Int = doc.maps.values.sumOf { map ->
        val merged = map.pins.firstOrNull { it.placeKey == mergedPlace } ?: return@sumOf 0
        if (map.pins.none { it.placeKey == targetPlace }) return@sumOf 0
        map.routes.sumOf { r -> r.points.count { it.pinKey == merged.key } }
    }

    /** `경로 끝 2 · 잿더미 시장 핀으로` (`[PROPOSED]` copy), or null when no route end moves. */
    fun routeEndRow(doc: ProjectDoc, mergedPlace: Key, targetPlace: Key): String? {
        val n = routeEnds(doc, mergedPlace, targetPlace)
        if (n == 0) return null
        return "경로 끝 $n${Copy.SEP}${doc.index.place(targetPlace)?.name.orEmpty()} 핀으로"
    }
}
