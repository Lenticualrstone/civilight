package io.github.lenticualrstone.civilight.feature.story.bulk

import io.github.lenticualrstone.civilight.feature.story.common.StoryServices
import io.github.lenticualrstone.civilight.feature.story.common.StoryViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.story.PlaceRef

/**
 * 장소 · 때 한꺼번에 (STO-31 … STO-35). Every apply is one undo step through [apply], recomputed on the doc
 * it is applied to (`BulkPlaceForm`, `BulkWhenForm`, `BulkSuggestForm`). Selection, filter, chapter and the
 * open `제안 받기` page are screen state (`rememberSaveable`).
 */
class BulkViewModel(projectKey: Key, services: StoryServices) : StoryViewModel(projectKey, services) {
    fun placeSame(selected: List<Key>, place: PlaceRef) =
        apply(BulkCopy.SET_PLACE) { d, ctx -> BulkPlaceForm.applySame(d, selected, place, ctx) }

    fun placeEach(choices: Map<Key, PlaceRef>) =
        apply(BulkCopy.SET_PLACE) { d, ctx -> BulkPlaceForm.applyEach(d, choices, ctx) }

    fun times(selected: List<Key>, form: BulkWhenForm) =
        apply(BulkCopy.SET_TIME) { d, ctx -> form.plan(d, selected, ctx).result }

    fun put(sceneKey: Key) = apply(BulkCopy.PUT) { d, ctx -> BulkSuggestForm.put(d, sceneKey, ctx) }

    fun putAll(sceneKeys: List<Key>) = apply(BulkCopy.PUT) { d, ctx -> BulkSuggestForm.putAll(d, sceneKeys, ctx) }
}
