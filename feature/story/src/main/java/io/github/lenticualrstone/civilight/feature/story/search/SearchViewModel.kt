package io.github.lenticualrstone.civilight.feature.story.search

import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.domain.checks.CheckInputs
import io.github.lenticualrstone.civilight.domain.checks.WritingChecks
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.story.search.ProjectSearch
import io.github.lenticualrstone.civilight.domain.story.search.SearchContext
import io.github.lenticualrstone.civilight.domain.story.search.SearchFilter
import io.github.lenticualrstone.civilight.domain.story.search.SearchResult
import io.github.lenticualrstone.civilight.domain.story.search.LineSpeaker
import io.github.lenticualrstone.civilight.domain.story.search.MoreResult
import io.github.lenticualrstone.civilight.domain.story.search.SearchMore
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

/**
 * What the search overlay shows: the result for the query and filter (null while the query is empty), and the
 * [gallery] scope when it was opened from a gallery's command bar (GAL-04).
 */
data class SearchUi(
    val query: String,
    val filter: SearchFilter,
    val result: SearchResult?,
    val context: SearchContext,
    val gallery: GalleryScope? = null,
    /** (0.2.0, N-13) The picked `그 밖 ▾` kind and its result (null while the query is empty). */
    val more: SearchMore? = null,
    val moreResult: MoreResult? = null,
    /** The picked `화자 ▾` speaker under `대사`, and the choices of that chip. */
    val speakerKey: Key? = null,
    val speakers: List<LineSpeaker> = emptyList(),
)

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
    private val narrow = MutableStateFlow(Narrow())

    /** `그 밖 ▾` kind and `화자 ▾` speaker together, so one change reruns the search once. */
    private data class Narrow(val more: SearchMore? = null, val speakerKey: Key? = null)

    /** (N-13) A `그 밖 ▾` kind; null = back to the six chips. */
    fun setMore(more: SearchMore?) {
        narrow.value = narrow.value.copy(more = more)
    }

    /** (N-13) A `화자 ▾` speaker; null = every speaker. */
    fun setSpeaker(key: Key?) {
        narrow.value = narrow.value.copy(speakerKey = key)
    }

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
            WritingChecks.run(doc, CheckInputs.of(s, services.clock.now(), services.clock.zone, hiddenUnlocked = open)).count(CheckFilter.ALL)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val inputs = combine(query.debounce(QUERY_DEBOUNCE_MS), filter, below, narrow) { q, f, b, n -> Inputs(q, f, b, n) }

    private data class Inputs(val query: String, val filter: SearchFilter, val below: NavKey?, val narrow: Narrow)

    val ui: StateFlow<SearchUi?> = combine(state, services.unlock, openChecks, inputs) { load, unlock, checks, input ->
        val ready = load as? ProjectLoad.Ready ?: return@combine null
        val (q, f, b, n) = input
        val context = SearchModel.context(ready.doc, b, itemsLocked(unlock), checks)
        val scope = SearchModel.galleryScope(b)
        // (0.2.0 WLD-72) A term with no stored reveal reads its automatic one, as the world cards do.
        val search = ProjectSearch(ready.doc, context) { key -> io.github.lenticualrstone.civilight.domain.world.term.TermReveals.autoAnchor(ready.doc, key) }
        val more = n.more.takeIf { scope == null }
        val speakers = if (scope == null && SearchModel.showsSpeaker(f, more)) search.lineSpeakers() else emptyList()
        val speaker = n.speakerKey?.takeIf { k -> speakers.any { it.key == k } }
        // Gallery titles, descriptions and tags are part of the command-bar index (55 §3.5, GAL-04): under
        // `전체` here, and alone when the overlay was opened over a gallery.
        val result = when {
            scope != null -> if (q.isBlank()) null else SearchModel.galleryResult(ready.doc, q, context.locked, scope)
            more != null -> null
            !SearchModel.listsWithoutQuery(q, f) -> null
            speaker != null -> search.searchLines(q, speaker)
            else -> SearchModel.withoutLensCommands(search.search(q, f, includeGallery = true), b)
        }
        val moreResult = if (more != null && q.isNotBlank()) search.searchMore(q, more) else null
        SearchUi(q, f, result, context, scope, more, moreResult, speaker, speakers)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private companion object {
        const val QUERY_DEBOUNCE_MS = 120L
        const val CHECK_DEBOUNCE_MS = 300L
    }
}
