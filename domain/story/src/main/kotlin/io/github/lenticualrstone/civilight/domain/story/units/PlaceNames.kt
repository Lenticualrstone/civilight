package io.github.lenticualrstone.civilight.domain.story.units

import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind

/**
 * A scene place for display: the place or region name; [deleted] when the place (or the region's
 * map) is not live, shown dotted as `지운 장소 · 잿더미 시장` (R20 §2).
 */
data class PlaceLabel(val name: String, val deleted: Boolean) {
    /** `지운 장소 · 잿더미 시장` for a deleted place, else the name. */
    val wide: String get() = if (deleted) "지운 장소 · $name" else name
}

/** Names of scene places and regions. */
class PlaceNames(private val doc: ProjectDoc) {
    fun label(ref: PlaceRef?): PlaceLabel? {
        ref ?: return null
        return when (ref.kind.known) {
            PlaceRefKind.PLACE -> doc.index.place(ref.key)?.let { PlaceLabel(it.name, !doc.liveness.isPlaceLive(it.key)) }
            PlaceRefKind.REGION -> doc.index.region(ref.key)?.let { (map, region) ->
                PlaceLabel(region.name, !doc.liveness.isMapLive(map.key))
            }
            null -> null
        }
    }

    /** The name, or null without a place. */
    fun name(ref: PlaceRef?): String? = label(ref)?.name

    /** True when the scene names a place that is not live (excluded from travel checks). */
    fun isDeleted(ref: PlaceRef?): Boolean = label(ref)?.deleted ?: (ref != null)

    /** `장소 없음` when empty. */
    fun nameOrNone(ref: PlaceRef?): String = name(ref) ?: "장소 없음"
}
