package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.domain.world.item.PlacePicker
import io.github.lenticualrstone.civilight.domain.world.item.WorldCards
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** A place row of `핀 · 장소 고르기`: name, `씬 4개` (or `이 지도에 핀 있음`), and whether it is taken. */
data class PinPickRow(val placeKey: Key, val name: String, val sub: String, val pinned: Boolean)

data class PinPickModel(val rows: List<PinPickRow>, val lockRow: String?)

/**
 * Rows of the pin's place picker (WLD-41) from the WLD-52 list (`PlacePicker.rows`): live places
 * matching the query with their scene counts; hidden places stay out while locked and show as one
 * `숨긴 장소 N` row. A place already pinned on this map is left out (one pin per place per map).
 */
object PinPick {
    fun model(doc: ProjectDoc, mapKey: Key, query: String, hiddenOpen: Boolean): PinPickModel {
        val pinned = doc.maps[mapKey]?.pins?.filter { doc.liveness.isOverlayLive(mapKey, it) }?.map { it.placeKey }?.toSet().orEmpty()
        val rows = PlacePicker(doc, hiddenOpen).rows(query)
            .filterNot { it.ref.key in pinned }
            .map { r -> PinPickRow(r.ref.key, r.name, r.text.removePrefix(r.name + MapCopy.SEP), false) }
        return PinPickModel(rows, if (hiddenOpen) null else WorldCards(doc, false).placeList().lockRow)
    }
}
