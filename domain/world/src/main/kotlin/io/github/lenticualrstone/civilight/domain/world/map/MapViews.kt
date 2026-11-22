package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.Pin
import io.github.lenticualrstone.civilight.model.map.WorldMap

/** A breadcrumb part; [dotted] when the parent pin's place is deleted (`지운 장소 · 잿더미 시장`). */
data class Crumb(val mapKey: Key?, val text: String, val dotted: Boolean)

/** How a place's pin stands (WLD-14 restore case). */
sealed interface PinStatus {
    data class OnMap(val mapKey: Key, val pin: Pin) : PinStatus

    /** The pin's map is in the trash: `◇ 지도 없음` with `지도 되살리기 · 핀 다시 놓기`. */
    data class MapMissing(val mapKey: Key, val pin: Pin, val text: String = "◇ 지도 없음", val actions: List<String> = listOf("지도 되살리기", "핀 다시 놓기")) : PinStatus
}

/** Map list, breadcrumbs and pin lookups (WLD-36, WLD-48, WLD-09). */
object MapViews {
    /** Live maps in order. */
    fun liveMaps(doc: ProjectDoc): List<WorldMap> =
        doc.maps.values.filter { it.deletedAt == null }.sortedWith(compareBy({ it.order }, { it.key }))

    /** `지도 · N`. */
    fun sectionTitle(doc: ProjectDoc): String = "지도${Copy.SEP}${liveMaps(doc).size}"

    /** Card small line: `생성 · 핀 9 · 영역 4`, or `세계 지도 › 핀 · 이미지` for a sub-map. */
    fun cardLine(doc: ProjectDoc, map: WorldMap): String {
        val base = when (map.baseKind.known) {
            MapBaseKind.GENERATED -> "생성"
            MapBaseKind.IMAGE -> "이미지"
            MapBaseKind.SKETCH -> "스케치"
            MapBaseKind.NODE -> "노드"
            null -> "지도"
        }
        val parent = map.parent?.let { doc.maps[it.mapKey] }
        if (parent != null) return "${parent.name} › 핀${Copy.SEP}$base"
        val pins = map.pins.count { doc.liveness.isOverlayLive(map.key, it) }
        return "$base${Copy.SEP}핀 $pins${Copy.SEP}영역 ${map.regions.size}"
    }

    /** Breadcrumb from the top map down to [map] (`세계 지도 › 하층 구역 지도`). */
    fun breadcrumb(doc: ProjectDoc, map: WorldMap): List<Crumb> {
        val chain = ArrayList<Crumb>()
        val seen = HashSet<Key>()
        var current: WorldMap? = map
        while (current != null && seen.add(current.key)) {
            val parent = current.parent
            val pinPlace = parent?.let { p -> doc.maps[p.mapKey]?.pins?.firstOrNull { it.key == p.pinKey }?.placeKey }
            val placeTrashed = pinPlace != null && !doc.liveness.isPlaceLive(pinPlace)
            chain += if (placeTrashed) {
                Crumb(current.key, "지운 장소${Copy.SEP}${doc.index.place(pinPlace!!)?.name.orEmpty()}", dotted = true)
            } else {
                Crumb(current.key, current.name, dotted = false)
            }
            current = parent?.let { doc.maps[it.mapKey] }
        }
        return chain.reversed()
    }

    fun breadcrumbText(doc: ProjectDoc, map: WorldMap): String = breadcrumb(doc, map).joinToString(" › ") { it.text }

    /** Child maps of a pin (live). */
    fun childMaps(doc: ProjectDoc, pinKey: Key): List<WorldMap> = doc.index.childMaps(pinKey).filter { it.deletedAt == null }

    /** Every pin of a place and whether its map is there (the place card's `지도` tab and restore case). */
    fun pinsOf(doc: ProjectDoc, placeKey: Key): List<PinStatus> = doc.maps.values.sortedWith(compareBy({ it.order }, { it.key })).flatMap { map ->
        map.pins.filter { it.placeKey == placeKey }.map { pin ->
            if (doc.liveness.isMapLive(map.key)) PinStatus.OnMap(map.key, pin) else PinStatus.MapMissing(map.key, pin)
        }
    }
}
