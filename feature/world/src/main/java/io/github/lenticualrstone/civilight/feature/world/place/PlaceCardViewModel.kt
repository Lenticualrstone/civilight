package io.github.lenticualrstone.civilight.feature.world.place

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.actions.AffiliationActions
import io.github.lenticualrstone.civilight.domain.world.item.OrderList
import io.github.lenticualrstone.civilight.domain.world.item.WorldItems
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.WorldViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.lens.LensRules
import io.github.lenticualrstone.civilight.ui.project.EditResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/** The place card (WLD-09) and its `소속 세력 및 인물` (board `ImmPlaceRules`). */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaceCardViewModel(projectKey: Key, val placeKey: Key, env: WorldEnv) : WorldViewModel(projectKey, env) {
    private val include = MutableStateFlow(true)

    /** `하위 지역 포함` (default on; a view choice, not stored). */
    val includeSubPlaces: StateFlow<Boolean> = include.asStateFlow()

    val ui: StateFlow<PlaceCardUi?> = combine(doc.filterNotNull(), hiddenOpen, readOnly, include) { d, h, r, i -> Inputs(d, h, r, i) }
        .mapLatest { (d, h, r, i) -> PlaceCardState.build(d, projectKey, placeKey, h, r, i) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private data class Inputs(val doc: ProjectDoc, val hidden: Boolean, val readOnly: Boolean, val include: Boolean)

    fun setIncludeSubPlaces(on: Boolean) {
        include.value = on
    }

    /** `지역`: the parent place, or none (`비우기`). A choice that would make a loop changes nothing. */
    suspend fun setParent(parentKey: Key?): EditResult {
        val ctx = editContext()
        return apply(WorldCopy.ROW_PARENT) { d -> WorldItems.setParentPlace(d, placeKey, parentKey, ctx) ?: d }
    }

    /**
     * One move in `소속 세력 및 인물` (one undo step). The shown list is rebuilt from the document being edited, with
     * the same lens, `하위 지역 포함` and lock, so the move lands on what the user sees. Returns the undo label when
     * something moved, else null (no toast).
     */
    suspend fun move(list: OrderList, key: Key, toIndex: Int): String? {
        val ctx = actionContext()
        val open = hiddenOpen.value
        val sub = include.value
        val label = AffiliationActions.undoLabel(list)
        var moved = false
        val r = apply(label) { d ->
            val next = AffiliationMoves.move(d, placeKey, list, key, toIndex, sub, open) { doc, section, k, to ->
                when (list) {
                    is OrderList.Factions -> AffiliationActions.moveFaction(doc, section, k, to, ctx)
                    is OrderList.Members -> AffiliationActions.moveMember(doc, section, list.factionKey, k, to, ctx)
                    is OrderList.Loose -> AffiliationActions.moveLoose(doc, section, k, to, ctx)
                }
            }
            moved = next !== d
            next
        }
        return label.takeIf { r == EditResult.APPLIED && moved }
    }
}

/** Pure wiring of a move: the section at the document's lens, then the domain move. */
object AffiliationMoves {
    fun move(
        doc: ProjectDoc,
        placeKey: Key,
        list: OrderList,
        key: Key,
        toIndex: Int,
        includeSubPlaces: Boolean,
        hiddenOpen: Boolean,
        apply: (ProjectDoc, io.github.lenticualrstone.civilight.domain.world.item.AffiliationSection, Key, Int) -> ProjectDoc,
    ): ProjectDoc {
        val lens = LensRules.effective(doc, doc.app.view.lens.point)
        val section = AffiliationActions.section(doc, placeKey, lens, includeSubPlaces, hiddenOpen) ?: return doc
        return apply(doc, section, key, toIndex)
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
