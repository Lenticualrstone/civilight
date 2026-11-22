package io.github.lenticualrstone.civilight.domain.world.item

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.domain.world.map.MapGeometry
import io.github.lenticualrstone.civilight.domain.world.map.RegionShapes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import java.text.Normalizer

/** A row of the place picker: `잿더미 시장 · 씬 4개`. */
data class PickRow(val ref: PlaceRef, val name: String, val text: String)

/**
 * The place / region picker other areas open (WLD-52): list mode with scene counts and search, the
 * row `새 장소 · 지도에 핀을 꽂으며 만들기`, and map mode (tap a pin, or inside a region in `지역`
 * mode). Returns `PlaceRef(PLACE)` or `PlaceRef(REGION)`.
 */
class PlacePicker(private val doc: ProjectDoc, private val hiddenUnlocked: Boolean = false) {
    companion object {
        const val NEW_PLACE_ROW = "새 장소 · 지도에 핀을 꽂으며 만들기"
    }

    private fun scenesUsing(ref: PlaceRef): Int = doc.manuscript.liveScenes.count { it.place?.sameAs(ref) == true }

    private fun nfc(s: String) = Normalizer.normalize(s, Normalizer.Form.NFC)

    /** Places (or, with [regions], live map regions) matching [query], each with its scene count. */
    fun rows(query: String = "", regions: Boolean = false): List<PickRow> {
        val q = nfc(query.trim())
        val items: List<Pair<PlaceRef, String>> = if (regions) {
            doc.maps.values.filter { doc.liveness.isMapLive(it.key) }.sortedWith(compareBy({ it.order }, { it.key }))
                .flatMap { m -> m.regions.map { PlaceRef(PlaceRefKind.REGION, it.key) to it.name } }
        } else {
            doc.world.places.filter { it.deletedAt == null && (!it.hidden || hiddenUnlocked) }
                .sortedWith(compareBy({ it.order }, { it.key }))
                .map { PlaceRef(PlaceRefKind.PLACE, it.key) to it.name }
        }
        return items.filter { q.isEmpty() || nfc(it.second).contains(q) }
            .map { (ref, name) -> PickRow(ref, name, "$name${Copy.SEP}씬 ${scenesUsing(ref)}개") }
    }

    /** Map mode: the pin under [at] picks its place; in `지역` mode the region holding [at] (shape at [lens]). */
    fun pickAt(mapKey: Key, at: Point, regions: Boolean = false, lens: LensPoint = LensPoint.Latest): PlaceRef? {
        val map = doc.maps[mapKey]?.takeIf { doc.liveness.isMapLive(it.key) } ?: return null
        if (regions) {
            return map.regions.firstOrNull { MapGeometry.contains(RegionShapes.shapeAt(doc, mapKey, it, lens), at) }
                ?.let { PlaceRef(PlaceRefKind.REGION, it.key) }
        }
        val pin = MapEdits.pinNear(map.copy(pins = map.pins.filter { doc.liveness.isOverlayLive(map.key, it) }), at) ?: return null
        val place = doc.index.place(pin.placeKey) ?: return null
        if (place.hidden && !hiddenUnlocked) return null
        return PlaceRef(PlaceRefKind.PLACE, place.key)
    }

    /** The confirm bar of a bulk pick: `씬 4개 → 잿더미 시장`. */
    fun confirmBar(sceneCount: Int, ref: PlaceRef): String {
        val name = when (ref.kind.known) {
            PlaceRefKind.PLACE -> doc.index.place(ref.key)?.name
            PlaceRefKind.REGION -> doc.index.region(ref.key)?.second?.name
            null -> null
        }.orEmpty()
        return "씬 ${sceneCount}개 → $name"
    }
}
