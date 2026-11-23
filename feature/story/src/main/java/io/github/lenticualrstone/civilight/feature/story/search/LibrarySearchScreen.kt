package io.github.lenticualrstone.civilight.feature.story.search

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.NoResults
import io.github.lenticualrstone.civilight.designsystem.component.SearchField
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.domain.story.search.SearchFilter
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.LibrarySearch
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.store.library.LibraryStore
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.UnlockState
import io.github.lenticualrstone.civilight.ui.security.WindowProtection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 모든 프로젝트에서 찾기 (LIB-07 scope, STO-37 engine): reads every live project once (the open session's
 * document when there is one, else the file) and searches them off the main thread, debounced. Holds
 * only app-scoped services (the library store, project sessions, unlock state, settings).
 */
@OptIn(FlowPreview::class)
class LibrarySearchViewModel(
    private val library: LibraryStore,
    private val projects: ProjectSessions,
    unlock: StateFlow<UnlockState>,
    settings: StateFlow<AppSettings>,
    initialQuery: String,
) : ViewModel() {
    private val query = MutableStateFlow(initialQuery)
    private val filter = MutableStateFlow(SearchFilter.ALL)
    private val docs = MutableStateFlow<List<CrossProject>>(emptyList())

    init {
        viewModelScope.launch {
            library.ensureLoaded()
            val read = withContext(Dispatchers.IO) {
                library.state.value.entries.mapNotNull { e ->
                    val doc: ProjectDoc? = projects.peekDoc(e.key) ?: runCatching { library.readDoc(e.key) }.getOrNull()
                    doc?.let { CrossProject(it, isExample = e.isExample, trashed = e.deletedAt != null) }
                }
            }
            docs.value = read
        }
    }

    fun setQuery(text: String) {
        query.value = text
    }

    fun setFilter(f: SearchFilter) {
        filter.value = f
    }

    val ui: StateFlow<CrossSearchUi?> = combine(docs, unlock, settings, query.debounce(QUERY_DEBOUNCE_MS), filter) { all, u, s, q, f ->
        LibrarySearchModel.search(all, LibrarySearchModel.access(u), s, u::itemsOpen, q, f)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private companion object {
        const val QUERY_DEBOUNCE_MS = 120L
    }
}

/**
 * `LibrarySearch` (서재 command bar): the 검색 overlay over the 서재 with chips `전체 · 인물 · 용어 · 대사 · 씬`,
 * one section group per project (its name, or the masked label), `숨긴 프로젝트 제외` and the empty state.
 * Results of a protected project keep its window protection on while shown. A tap opens that project
 * (프로젝트 홈) and then the target, so back returns to the project.
 */
@Composable
fun LibrarySearchScreen(route: LibrarySearch, deps: FeatureDeps) {
    val vm = entryViewModel { LibrarySearchViewModel(deps.stores.library, deps.projects, deps.unlock.state, deps.settings, route.query) }
    val ui by vm.ui.collectAsState()
    val navigator = deps.navigator
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(route.query, TextRange(route.query.length))) }
    var filterName by rememberSaveable { mutableStateOf(SearchFilter.ALL.name) }
    val filter = SearchFilter.valueOf(filterName)
    LaunchedEffect(filter) { vm.setFilter(filter) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    fun open(projectKey: Key, route: io.github.lenticualrstone.civilight.navigation.Route) {
        navigator.replace(ProjectHome(projectKey))
        navigator.push(route)
    }

    Box(Modifier.fillMaxSize().background(CivilightTheme.colors.bg.copy(alpha = SCRIM)), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize().statusBarsPadding().imePadding().padding(horizontal = 16.dp)) {
            // DS-70 A5: the close is icon only, read `찾기 닫기`.
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                SearchField(
                    value = field,
                    onValueChange = {
                        field = it
                        vm.setQuery(it.text)
                    },
                    modifier = Modifier.weight(1f).focusRequester(focus),
                    onClose = { navigator.pop() },
                )
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LibrarySearchModel.filters.forEach { f -> FilterChip(f.word, f == filter, { filterName = f.name }) }
            }
            val u = ui
            if (u != null && field.text.isNotBlank()) {
                if (u.emptyLine != null) {
                    NoResults(title = SearchCopy.NO_RESULTS, context = u.emptyLine)
                } else {
                    u.sections.map { it.protections }.distinct().forEach { ProtectWindow(WindowProtection.forProject(it)) }
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                        u.sections.forEach { section ->
                            item(key = "p:${section.projectKey}") {
                                CivilightText(section.title, CivilightTheme.type.itemTitle, civilightItem().padding(top = 18.dp), color = CivilightTheme.colors.ink)
                            }
                            section.result.sections.forEach { s ->
                                item(key = "h:${section.projectKey}:${s.kind}") { SectionLabel(s.header, civilightItem().padding(top = 10.dp)) }
                                rows("${section.projectKey}:${s.kind.name}", s.items) { picked ->
                                    when (val tap = SearchModel.tap(section.projectKey, picked, section.context)) {
                                        is SearchTap.Open -> open(section.projectKey, tap.route)
                                        else -> Unit
                                    }
                                }
                            }
                        }
                        u.footer?.let { footer ->
                            item(key = "footer") { CivilightText(footer, CivilightTheme.type.small, civilightItem().padding(top = 14.dp), color = CivilightTheme.colors.ink3) }
                        }
                    }
                }
            }
        }
    }
}

/** Copy of the cross-project search (board `EmptySearch`). */
object SearchCopy {
    const val NO_RESULTS = "결과 없음"

    /** The `갤러리` section of the project search (55 §3.5). */
    const val GALLERY = "갤러리"

    /** Hint line of the search opened from a character's gallery (GAL-04, board `GalSearch`). */
    const val GALLERY_FIND = "갤러리에서 찾기 · 제목 · 설명 · 태그"

    /** The same from the project gallery (GAL-04, GAL-10). */
    const val ALL_GALLERIES_FIND = "모든 갤러리 · 제목 · 설명 · 태그"

    /** (0.2.0, N-13) `그 밖 ▾` (picks `장소 · 세력 · 사건 · 페이지`) and `화자 ▾` under `대사`; a picked value reads `장소 ▾`. */
    const val MORE = "그 밖"
    const val SPEAKER = "화자"
    const val CHEVRON = " ▾"

    /** First row of the `화자 ▾` sheet: every speaker (the chip's own word under `대사`). */
    const val ALL_SPEAKERS = "전체"
}

private const val SCRIM = 0.82f
