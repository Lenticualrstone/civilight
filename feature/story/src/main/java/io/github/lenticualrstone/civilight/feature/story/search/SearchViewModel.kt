package io.github.lenticualrstone.civilight.feature.story.search

import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.domain.checks.CheckInputs
import io.github.lenticualrstone.civilight.domain.checks.ProjectChecks
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.story.search.ProjectSearch
import io.github.lenticualrstone.civilight.domain.story.search.SearchContext
import io.github.lenticualrstone.civilight.domain.story.search.SearchFilter
import io.github.lenticualrstone.civilight.domain.story.search.SearchResult
import io.github.lenticualrstone.civilight.feature.story.common.StoryServices
import io.github.lenticualrstone.civilight.feature.story.common.StoryViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/** What the search overlay shows: the result for the query and filter (null while the query is empty). */
data class SearchUi(val query: String, val filter: SearchFilter, val result: SearchResult?, val context: SearchContext)

/**
 * 검색 · 명령 (STO-37, STO-38): runs `domain:story` [ProjectSearch] off the main thread, debounced, for
 * the query the field holds. The field's text is the composable's state (Hangul composition); this
 * view model only receives it.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    projectKey: Key,
    services: StoryServices,
    initialQuery: String,
    private val settings: StateFlow<AppSettings>,
) : StoryViewModel(projectKey, services) {
    private val query = MutableStateFlow(initialQuery)
    private val filter = MutableStateFlow(SearchFilter.ALL)
    private val below = MutableStateFlow<NavKey?>(null)

    fun setQuery(text: String) {
        query.value = text
    }

    fun setFilter(f: SearchFilter) {
        filter.value = f
    }

    /** The screen under the overlay (the script changes the command set). */
    fun setBelow(key: NavKey?) {
        below.value = key
    }

    /**
     * `확인할 항목 N` for `› 점검 센터 열기`: open ▲ and ◇ of the current project state, with the same
     * STO-42 switches and hidden-lock state as 점검 센터's `전체 N`.
     */
    private val openChecks: StateFlow<Int> = combine(state.map { (it as? ProjectLoad.Ready)?.doc }.filterNotNull(), settings, services.unlock) { doc, s, unlock ->
        Triple(doc, s, !itemsLocked(unlock))
    }
        .distinctUntilChanged { a, b -> a.first === b.first && a.second == b.second && a.third == b.third }
        .debounce(CHECK_DEBOUNCE_MS)
        .mapLatest { (doc, s, open) ->
            ProjectChecks.run(doc, CheckInputs.of(s, services.clock.now(), services.clock.zone, hiddenUnlocked = open)).count(CheckFilter.ALL)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val inputs = combine(query.debounce(QUERY_DEBOUNCE_MS), filter, below) { q, f, b -> Triple(q, f, b) }

    val ui: StateFlow<SearchUi?> = combine(state, services.unlock, openChecks, inputs) { load, unlock, checks, (q, f, b) ->
        val ready = load as? ProjectLoad.Ready ?: return@combine null
        val context = SearchModel.context(ready.doc, b, itemsLocked(unlock), checks)
        val result = if (q.isBlank()) null else ProjectSearch(ready.doc, context).search(q, f)
        SearchUi(q, f, result, context)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private companion object {
        const val QUERY_DEBOUNCE_MS = 120L
        const val CHECK_DEBOUNCE_MS = 300L
    }
}
