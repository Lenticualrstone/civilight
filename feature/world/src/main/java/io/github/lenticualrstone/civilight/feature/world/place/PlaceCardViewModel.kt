package io.github.lenticualrstone.civilight.feature.world.place

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.world.item.WorldItems
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.WorldViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.project.EditResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/** The place card (WLD-09). */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaceCardViewModel(projectKey: Key, val placeKey: Key, env: WorldEnv) : WorldViewModel(projectKey, env) {
    val ui: StateFlow<PlaceCardUi?> = combine(doc.filterNotNull(), hiddenOpen, readOnly) { d, h, r -> Triple(d, h, r) }
        .mapLatest { (d, h, r) -> PlaceCardState.build(d, projectKey, placeKey, h, r) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** `지역`: the parent place, or none (`비우기`). A choice that would make a loop changes nothing. */
    suspend fun setParent(parentKey: Key?): EditResult {
        val ctx = editContext()
        return apply(WorldCopy.ROW_PARENT) { d -> WorldItems.setParentPlace(d, placeKey, parentKey, ctx) ?: d }
    }
}

/** Places that may become the `지역` of [placeKey]: not itself and not one of its descendants. */
object ParentChoices {
    fun excluded(doc: ProjectDoc, placeKey: Key): Set<Key> {
        val out = hashSetOf(placeKey)
        var grew = true
        while (grew) {
            grew = false
            for (p in doc.world.places) {
                val parent = p.parentPlaceKey ?: continue
                if (parent in out && out.add(p.key)) grew = true
            }
        }
        return out
    }
}
