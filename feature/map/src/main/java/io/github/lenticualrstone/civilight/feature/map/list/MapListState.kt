package io.github.lenticualrstone.civilight.feature.map.list

import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import io.github.lenticualrstone.civilight.domain.world.map.MapViews
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBase
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBases
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScene
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScenes
import io.github.lenticualrstone.civilight.feature.map.canvas.SceneMode
import io.github.lenticualrstone.civilight.feature.map.common.RouteLines
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapParent
import io.github.lenticualrstone.civilight.model.position.LensPoint
import java.time.ZoneId

/** A map card (196 × 147 thumbnail, name, small line `생성 · 핀 9 · 영역 4`). */
data class MapCardUi(val key: Key, val name: String, val line: String, val base: MapBase, val scene: MapScene)

/**
 * A `최근 고친 곳` row (board `MapNew`): `하층 구역 · 영역 ‘상층 관문 구역’` / `오늘 08:40 · 세력 영역`. Opens
 * [mapKey], centred on [pinKey] when the row is a pin.
 */
data class RecentEditUi(val key: Key, val mapKey: Key, val title: String, val sub: String, val pinKey: Key? = null)

/** 지도 목록 (WLD-36, board `MapNew`). */
data class MapListUi(
    /** `잿빛 새벽의 등불 · 세계`. */
    val small: String,
    /** `지도 · 2`. */
    val section: String,
    val cards: List<MapCardUi>,
    /** `최근 고친 곳`: the last edited pins, regions, routes and labels, newest first (empty: no section). */
    val recent: List<RecentEditUi> = emptyList(),
)

/** The `상위` line of the new-map sheet started from a pin: `세계 지도 · 핀 ‘상층’`. */
data class ParentUi(val parent: MapParent, val tag: String)

object MapListState {
    /** Rows of `최근 고친 곳` (board: two; three fit the cover without scrolling past the cards). */
    const val RECENT_COUNT: Int = 3

    fun build(doc: ProjectDoc, lens: LensPoint, hiddenOpen: Boolean, now: Timestamp = 0L, zone: ZoneId = ZoneId.systemDefault()): MapListUi {
        val maps = MapViews.liveMaps(doc)
        val cards = maps.map { map ->
            MapCardUi(
                key = map.key,
                name = map.name,
                line = MapViews.cardLine(doc, map),
                base = MapBases.of(map),
                scene = MapScenes.build(doc, map, lens, hiddenOpen, SceneMode.VIEW),
            )
        }
        return MapListUi(
            small = MapCopy.listSmall(doc.project.name),
            section = MapViews.sectionTitle(doc),
            cards = cards,
            recent = recent(doc, cards, now, zone),
        )
    }

    /**
     * `최근 고친 곳`: overlays with an edit time, newest first. Only what the map's card draws is listed, so a
     * hidden place's pin or a region of a hidden faction never shows (rule 19), and an item not yet revealed at
     * the lens shows only when the map draws it dimmed (`revealViewDim`); names come from the drawn scene (a
     * route end on a hidden place reads `지점`).
     */
    fun recent(doc: ProjectDoc, cards: List<MapCardUi>, now: Timestamp, zone: ZoneId): List<RecentEditUi> {
        data class Edit(val at: Timestamp, val row: RecentEditUi)
        val edits = ArrayList<Edit>()
        for (card in cards) {
            val map = doc.maps[card.key] ?: continue
            val scene = card.scene
            val drawnPins = scene.pins.mapTo(HashSet()) { it.key }
            fun add(at: Timestamp, key: Key, kind: String, name: String, detail: String, pinKey: Key? = null) {
                if (at <= 0L || name.isBlank()) return
                val time = TimeLabels.relative(at, now, zone)
                edits += Edit(at, RecentEditUi(key, map.key, MapCopy.recentTitle(map.name, kind, name), MapCopy.recentSub(time, detail), pinKey))
            }
            map.regions.forEach { r -> scene.region(r.key)?.let { add(r.updatedAt, r.key, MapCopy.REGION, it.name, r.kind.ifBlank { MapCopy.REGION }) } }
            map.routes.forEach { r ->
                // Board `NmMapNew`: `경로 ‘결사의 문 → 상층 관문’` over `어제 · 도보 2일 4시간` (`지점 N` without a scale).
                // The title comes from the sheet, not from splitting the chip label, so a place name with ` · ` stays whole.
                if (scene.routes.any { it.key == r.key }) {
                    val sheet = RouteLines.sheet(doc, map, r, drawnPins)
                    val rest = sheet.mapLabel.removePrefix(sheet.title).removePrefix(MapCopy.SEP)
                    val detail = rest.split(MapCopy.SEP).first().ifEmpty { MapCopy.routeDraftBar(r.points.size, null) }
                    add(r.updatedAt, r.key, MapCopy.ROUTE, sheet.title, detail)
                }
            }
            map.pins.forEach { p -> scene.pin(p.key)?.let { add(p.updatedAt, p.key, MapCopy.PIN, it.name, MapCopy.PIN, pinKey = p.key) } }
            map.labels.forEach { l -> scene.labels.firstOrNull { it.key == l.key }?.let { add(l.updatedAt, l.key, MapCopy.LABEL, it.text, MapCopy.LABEL) } }
        }
        return edits.sortedByDescending { it.at }.take(RECENT_COUNT).map { it.row }
    }

    /** The parent line for a child map of [pinKey] on [mapKey], or null when either is gone. */
    fun parent(doc: ProjectDoc, mapKey: Key?, pinKey: Key?): ParentUi? {
        if (mapKey == null || pinKey == null) return null
        val map = doc.maps[mapKey]?.takeIf { doc.liveness.isMapLive(it.key) } ?: return null
        val pin = map.pins.firstOrNull { it.key == pinKey } ?: return null
        val place = doc.index.place(pin.placeKey)?.name.orEmpty()
        return ParentUi(MapParent(mapKey, pinKey), MapCopy.parentTag(map.name, place))
    }
}
