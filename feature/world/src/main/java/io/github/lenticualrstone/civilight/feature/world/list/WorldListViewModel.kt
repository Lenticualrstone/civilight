package io.github.lenticualrstone.civilight.feature.world.list

import io.github.lenticualrstone.civilight.designsystem.component.CreateCard
import io.github.lenticualrstone.civilight.domain.world.item.NewWorldItem
import io.github.lenticualrstone.civilight.domain.world.item.WorldCreate
import io.github.lenticualrstone.civilight.feature.world.common.WorldCreateCards
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.world.item.CreateResult
import io.github.lenticualrstone.civilight.domain.world.item.WorldItems
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.TermIndexes
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.WorldViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.WorldTab
import io.github.lenticualrstone.civilight.ui.project.EditResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The 세계 list (WLD-01 … WLD-04, WLD-11 create). */
@OptIn(ExperimentalCoroutinesApi::class)
class WorldListViewModel(projectKey: Key, private val routeTab: WorldTab, env: WorldEnv) : WorldViewModel(projectKey, env) {
    private val indexHandle = TermIndexes.acquire(projectKey, env.appScope)

    /** The shown tab; null until the project's remembered tab is known. */
    private val tabState = MutableStateFlow<WorldTab?>(null)

    val ui: StateFlow<WorldListUi?> = combine(doc.filterNotNull(), tabState.filterNotNull(), indexHandle.index, hiddenOpen) { d, t, i, h ->
        Inputs(d, t, i, h)
    }
        .mapLatest { WorldListState.build(it.doc, it.tab, it.index, it.hiddenOpen) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private data class Inputs(
        val doc: io.github.lenticualrstone.civilight.model.doc.ProjectDoc,
        val tab: WorldTab,
        val index: io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex,
        val hiddenOpen: Boolean,
    )

    init {
        viewModelScope.launch {
            val first = doc.filterNotNull().first()
            tabState.value = WorldTabs.initial(routeTab, first.app.view.worldTab)
        }
        viewModelScope.launch {
            combine(doc.filterNotNull(), hiddenOpen) { d, h -> d to h }.collect { (d, h) -> indexHandle.submit(d, TermAccess(h)) }
        }
    }

    /** Shows [tab] and remembers it for the project (`_app/`, also in the read-only example). */
    fun selectTab(tab: WorldTab) {
        tabState.value = tab
        viewModelScope.launch {
            updateApp { app -> if (app.view.worldTab == tab.name) app else app.copy(view = app.view.copy(worldTab = tab.name)) }
        }
    }

    /**
     * `만들기` (WLD-11): a new term, place or faction with [name] and optional [description]; returns
     * its key, or null when nothing was made (empty name, read-only project).
     */
    suspend fun create(kind: WorldKind, name: String, description: String): Key? {
        var created: Key? = null
        val ctx = editContext()
        val result = apply(WorldCopy.createLabel(kind)) { d ->
            val r = when (kind) {
                WorldKind.TERM -> WorldItems.createTerm(d, name, ctx, description)
                WorldKind.PLACE -> WorldItems.createPlace(d, name, ctx, description)
                WorldKind.FACTION -> WorldItems.createFaction(d, name, ctx, description)
            }
            when (r) {
                is CreateResult.Created -> r.doc.also { created = r.key }
                CreateResult.EmptyName -> d
            }
        }
        return if (result == EditResult.APPLIED) created else null
    }

    /**
     * (0.2.0, U-15, WLD-70) `만들기` / `만들고 열기` of the create card: one edit and one undo step through
     * `domain:world` [WorldCreate]; returns the new key, or null when nothing was made.
     */
    suspend fun createInPlace(kind: WorldKind, item: NewWorldItem): Key? {
        var created: Key? = null
        val ctx = editContext()
        val result = apply(CreateCard.title(WorldCreateCards.createKind(kind))) { d ->
            when (val r = WorldCreate.create(d, kind, item, ctx)) {
                is CreateResult.Created -> r.doc.also { created = r.key }
                CreateResult.EmptyName -> d
            }
        }
        return if (result == EditResult.APPLIED) created else null
    }

    override fun onCleared() {
        indexHandle.release()
        super.onCleared()
    }
}
