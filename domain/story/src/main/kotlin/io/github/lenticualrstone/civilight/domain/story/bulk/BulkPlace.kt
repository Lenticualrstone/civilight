package io.github.lenticualrstone.civilight.domain.story.bulk

import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.domain.story.withUnits
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind

/** `모두 같은 장소` · `씬마다 차례로`. */
enum class PlaceMode { ALL_SAME, ONE_BY_ONE }

/** One row of the place list: `잿더미 시장 | 씬 4개`. */
data class PlaceOption(val ref: PlaceRef, val name: String, val sceneCount: Int) {
    val countText: String get() = "씬 ${sceneCount}개"
}

/** `장소 정하기` (STO-32, `BulkPlace`). Applying is one undo step (the previous doc). */
object BulkPlace {
    fun title(count: Int): String = "장소 정하기 · 씬 ${count}개"

    /** `씬 4개 → 잿더미 시장`. */
    fun footer(count: Int, name: String): String = "씬 ${count}개 → $name"

    /**
     * Live places (or regions of live maps for [PlaceRefKind.REGION]) with the number of live scenes in
     * [scope] that use them, by count (desc), then the place order (`ED`).
     */
    fun options(doc: ProjectDoc, scope: List<Key>, kind: PlaceRefKind = PlaceRefKind.PLACE): List<PlaceOption> {
        val used = scope.mapNotNull { doc.index.unit(it)?.place }.filter { it.kind.known == kind }.groupingBy { it.key }.eachCount()
        val all: List<Pair<PlaceRef, String>> = when (kind) {
            PlaceRefKind.PLACE -> doc.world.places
                .filter { doc.liveness.isPlaceLive(it.key) }
                .sortedWith(compareBy({ it.order }, { it.key }))
                .map { PlaceRef(PlaceRefKind.PLACE, it.key) to it.name }
            PlaceRefKind.REGION -> doc.maps.values
                .filter { doc.liveness.isMapLive(it.key) }
                .sortedWith(compareBy({ it.order }, { it.key }))
                .flatMap { m -> m.regions.map { PlaceRef(PlaceRefKind.REGION, it.key) to it.name } }
        }
        return all.map { (ref, name) -> PlaceOption(ref, name, used[ref.key] ?: 0) }
            .withIndex()
            .sortedWith(compareByDescending<IndexedValue<PlaceOption>> { it.value.sceneCount }.thenBy { it.index })
            .map { it.value }
    }

    /** `모두 같은 장소`: every selected scene gets [place]. */
    fun applySame(doc: ProjectDoc, selected: List<Key>, place: PlaceRef, ctx: EditContext? = null): ProjectDoc =
        applyEach(doc, selected.associateWith { place }, ctx)

    /** `씬마다 차례로`: each scene gets its own choice. */
    fun applyEach(doc: ProjectDoc, choices: Map<Key, PlaceRef>, ctx: EditContext? = null): ProjectDoc =
        doc.withUnits(choices.mapNotNull { (k, p) -> doc.index.unit(k)?.let { k to it.copy(place = p) } }.toMap(), ctx)
}
