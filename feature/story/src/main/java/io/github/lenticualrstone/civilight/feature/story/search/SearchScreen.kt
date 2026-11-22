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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.NoResults
import io.github.lenticualrstone.civilight.designsystem.component.SearchField
import io.github.lenticualrstone.civilight.designsystem.component.SearchHit
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.story.search.Highlight
import io.github.lenticualrstone.civilight.domain.story.search.SearchFilter
import io.github.lenticualrstone.civilight.domain.story.search.SearchItem
import io.github.lenticualrstone.civilight.domain.story.search.SearchResult
import io.github.lenticualrstone.civilight.domain.story.search.SimilarName
import io.github.lenticualrstone.civilight.feature.story.common.storyServices
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.RenumberSheet
import io.github.lenticualrstone.civilight.navigation.ProjectSearch
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHostLayout
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost

/**
 * 검색 · 명령 (STO-36 … STO-38; boards `Search`, `EmptySearch`): full height over the blurred screen
 * (the shell's overlay scene), query field with `닫기`, single-choice chips, sections in chip order,
 * whole-sentence hits with the match boxed inline, `숨긴 항목 제외`, and the empty state with `비슷한
 * 이름` and `용어로 만들기`. Inner screens: the same, 560 wide (`ED`).
 */
@Composable
fun SearchScreen(route: ProjectSearch, deps: FeatureDeps) {
    val vm = entryViewModel { SearchViewModel(route.projectKey, deps.storyServices(), route.query, deps.settings) }
    val ui by vm.ui.collectAsState()
    val navigator = deps.navigator
    val p = route.projectKey
    val store = currentProjectStore
    val sheets = rememberSheetHost()
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(route.query, TextRange(route.query.length))) }
    var filterName by rememberSaveable { mutableStateOf(SearchFilter.ALL.name) }
    val filter = SearchFilter.valueOf(filterName)
    val below = navigator.backStack.let { stack -> stack.getOrNull(stack.size - 2) }
    LaunchedEffect(below) { vm.setBelow(below) }
    LaunchedEffect(filter) { vm.setFilter(filter) }
    // The overlay opens ready to type (STO-36: the command bar tap goes straight to the query).
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    fun go(tap: SearchTap?) {
        when (tap) {
            null -> Unit
            is SearchTap.Open -> navigator.replace(tap.route)
            SearchTap.Lens -> sheets.open(LensSheet(p))
            is SearchTap.LensToScene -> {
                deps.lens(store).setLensToScene(tap.sceneKey)
                navigator.pop()
            }
            is SearchTap.Renumber -> sheets.open(RenumberSheet(p, tap.episodeKey))
        }
    }

    // `RenumberSheet` is SCRIPT's shared sheet (registered by feature:script), so it opens here too.
    SheetHostLayout(sheets) {
        Box(Modifier.fillMaxSize().background(CivilightTheme.colors.bg.copy(alpha = SCRIM)), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxSize().statusBarsPadding().imePadding().padding(horizontal = 16.dp)) {
                SearchField(
                    value = field,
                    onValueChange = {
                        field = it
                        vm.setQuery(it.text)
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).focusRequester(focus),
                    onClose = { navigator.pop() },
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SearchFilter.entries.forEach { f -> FilterChip(f.word, f == filter, { filterName = f.name }) }
                }
                val u = ui
                val result = u?.result
                if (u != null && result != null && field.text.isNotBlank()) {
                    Results(
                        result = result,
                        onItem = { item -> go(SearchModel.tap(p, item, u.context)) },
                        onSimilar = { go(SearchModel.tap(p, it)) },
                        onCreateTerm = { navigator.replace(SearchModel.createTerm(p)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Results(result: SearchResult, onItem: (SearchItem) -> Unit, onSimilar: (SimilarName) -> Unit, onCreateTerm: () -> Unit) {
    val colors = CivilightTheme.colors
    val empty = result.empty
    if (empty != null) {
        NoResults(
            title = empty.title,
            context = empty.line,
            actionLabel = empty.createAction,
            onAction = onCreateTerm,
            similar = if (empty.similar.isEmpty()) null else {
                {
                    SectionLabel(empty.similarHeader)
                    empty.similar.forEach { s ->
                        Row(
                            Modifier.fillMaxWidth().topHairline(colors.line).pressable(onClick = { onSimilar(s) }, scaleOnPress = false).heightIn(min = 44.dp).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CivilightText(s.name, CivilightTheme.type.itemTitleCompact, color = colors.ink, maxLines = 1)
                            CivilightText(listOfNotNull(s.kind.title, s.sub).joinToString(" · "), CivilightTheme.type.small, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
                        }
                    }
                }
            },
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        result.sections.forEach { section ->
            item(key = "h:${section.kind}") { SectionLabel(section.header, Modifier.padding(top = 14.dp)) }
            section.items.forEachIndexed { i, item -> row(section.kind.name, i, item, onItem) }
        }
        result.footer?.let { footer ->
            item(key = "footer") { CivilightText(footer, CivilightTheme.type.small, Modifier.padding(top = 14.dp), color = colors.ink3) }
        }
    }
}

internal fun LazyListScope.row(section: String, index: Int, item: SearchItem, onItem: (SearchItem) -> Unit) {
    item(key = "$section:$index:${item.key}") {
        when (item) {
            is SearchItem.LineItem -> SearchHit(
                location = item.location,
                sentence = item.sentence.text,
                matches = item.sentence.hits,
                trailingWarning = item.flag,
                onClick = { onItem(item) },
            )
            is SearchItem.TermItem -> NameRow(item.name, item.sub, item.action) { onItem(item) }
            is SearchItem.CharacterItem -> NameRow(item.name, item.intro, null) { onItem(item) }
            is SearchItem.SceneItem -> NameRow(item.title, item.sub, null) { onItem(item) }
            is SearchItem.NamedItem -> NameRow(item.name, null, null) { onItem(item) }
            is SearchItem.CommandItem -> CommandRow(item) { onItem(item) }
        }
    }
}

/** A name with its hit boxed (`hl` fill), a small sub line and an optional right word (`카드`). */
@Composable
private fun NameRow(name: Highlight, sub: String?, trailing: String?, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier.fillMaxWidth().topHairline(colors.line).pressable(onClick = onClick, scaleOnPress = false).heightIn(min = 48.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(boxed(name, colors.hl), style = type.itemTitleCompact.merge(androidx.compose.ui.text.TextStyle(color = colors.ink)), maxLines = 1)
            sub?.let { CivilightText(it, type.small, color = colors.ink3, maxLines = 1) }
        }
        trailing?.let { CivilightText(it, type.small, color = colors.ink2, maxLines = 1) }
    }
}

/** `› 시점 바꾸기 | 지금: 최신 · 1-7`. */
@Composable
private fun CommandRow(item: SearchItem.CommandItem, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier.fillMaxWidth().topHairline(colors.line).pressable(onClick = onClick, scaleOnPress = false).heightIn(min = 48.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText(boxed(Highlight("› ${item.label.text}", item.label.hits.map { (it.first + 2)..(it.last + 2) }), colors.hl), style = type.command.merge(androidx.compose.ui.text.TextStyle(color = colors.ink)), maxLines = 1)
        item.sub?.let { CivilightText(it, type.small, Modifier.weight(1f), color = colors.ink3, maxLines = 1) }
    }
}

private fun boxed(h: Highlight, fill: androidx.compose.ui.graphics.Color): AnnotatedString = buildAnnotatedString {
    append(h.text)
    h.hits.forEach { r -> if (r.first >= 0 && r.last < h.text.length) addStyle(SpanStyle(background = fill), r.first, r.last + 1) }
}

private const val SCRIM = 0.82f
