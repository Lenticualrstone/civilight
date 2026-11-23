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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.NoResults
import io.github.lenticualrstone.civilight.designsystem.component.SearchField
import io.github.lenticualrstone.civilight.designsystem.component.SearchHit
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.text.searchHitDecorations
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.story.search.GalleryHit
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
    // Opened from a gallery's command bar (GAL-04): gallery images only, the hint line instead of chips.
    val galleryScope = SearchModel.galleryScope(below)
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
            is SearchTap.ReplaceBelow -> {
                navigator.pop()
                navigator.replace(tap.route)
            }
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
                if (galleryScope != null) {
                    CivilightText(galleryScope.hint, CivilightTheme.type.small, Modifier.padding(top = 8.dp, bottom = 10.dp), color = CivilightTheme.colors.ink3, maxLines = 1)
                } else {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SearchFilter.entries.forEach { f -> FilterChip(f.word, f == filter, { filterName = f.name }) }
                    }
                }
                val u = ui
                val result = u?.result
                // QA #11: the `명령` chip lists the commands with an empty query too (SearchModel.listsWithoutQuery).
                val shows = if (galleryScope != null) field.text.isNotBlank() && u?.gallery == galleryScope else SearchModel.listsWithoutQuery(field.text, filter)
                if (u != null && result != null && shows) {
                    Results(
                        result = result,
                        scoped = galleryScope != null,
                        onItem = { item -> go(SearchModel.tap(p, item, u.context)) },
                        onGallery = { hit -> go(SearchModel.tap(p, hit, galleryScope)) },
                        onSimilar = { go(SearchModel.tap(p, it)) },
                        onCreateTerm = { navigator.replace(SearchModel.createTerm(p)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Results(
    result: SearchResult,
    scoped: Boolean,
    onItem: (SearchItem) -> Unit,
    onGallery: (GalleryHit) -> Unit,
    onSimilar: (SimilarName) -> Unit,
    onCreateTerm: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val empty = result.empty
    if (empty != null) {
        // A gallery search makes no term (board `GalSearch`).
        NoResults(
            title = empty.title,
            context = empty.line,
            actionLabel = if (scoped) null else empty.createAction,
            onAction = if (scoped) null else onCreateTerm,
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
            item(key = "h:${section.kind}") { SectionLabel(section.header, civilightItem().padding(top = 14.dp)) }
            rows(section.kind.name, section.items, onItem)
        }
        SearchModel.gallerySections(result, scoped).forEach { section ->
            item(key = "h:gallery:${section.header.substringBefore(' ')}") { SectionLabel(section.header, civilightItem().padding(top = 14.dp)) }
            section.hits.forEach { hit ->
                item(key = SearchModel.galleryKey(hit)) { GalleryHitRow(hit, civilightItem()) { onGallery(hit) } }
            }
        }
        result.footer?.let { footer ->
            item(key = "footer") { CivilightText(footer, CivilightTheme.type.small, civilightItem().padding(top = 14.dp), color = colors.ink3) }
        }
    }
}

/**
 * The rows of one section, each its own keyed item. The key is the item's own (kind + key, never the
 * index), so a row that stays keeps its place while others come and go (motion audit, feature:story 3).
 */
internal fun LazyListScope.rows(section: String, items: List<SearchItem>, onItem: (SearchItem) -> Unit) {
    val keys = SearchModel.itemKeys(section, items)
    items(items.size, key = { keys[it] }) { i ->
        val item = items[i]
        val modifier = civilightItem()
        when (item) {
            is SearchItem.LineItem -> SearchHit(
                location = item.location,
                sentence = item.sentence.text,
                matches = item.sentence.hits,
                trailingWarning = item.flag,
                modifier = modifier,
                onClick = { onItem(item) },
            )
            is SearchItem.TermItem -> NameRow(item.name, item.sub, item.action, modifier) { onItem(item) }
            is SearchItem.CharacterItem -> NameRow(item.name, item.intro, null, modifier) { onItem(item) }
            is SearchItem.SceneItem -> NameRow(item.title, item.sub, null, modifier) { onItem(item) }
            is SearchItem.NamedItem -> NameRow(item.name, null, null, modifier) { onItem(item) }
            is SearchItem.CommandItem -> CommandRow(item, modifier) { onItem(item) }
        }
    }
}

/** A name with its hit boxed (`hl` fill), a small sub line and an optional right word (`카드`). */
@Composable
private fun NameRow(name: Highlight, sub: String?, trailing: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier.fillMaxWidth().topHairline(colors.line).pressable(onClick = onClick, scaleOnPress = false).heightIn(min = 48.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            HitText(name.text, name.hits, type.itemTitleCompact)
            sub?.let { CivilightText(it, type.small, color = colors.ink3, maxLines = 1) }
        }
        trailing?.let { CivilightText(it, type.small, color = colors.ink2, maxLines = 1) }
    }
}

/** `› 시점 바꾸기 | 지금: 최신 · 1-7`. */
@Composable
private fun CommandRow(item: SearchItem.CommandItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier.fillMaxWidth().topHairline(colors.line).pressable(onClick = onClick, scaleOnPress = false).heightIn(min = 48.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HitText("› ${item.label.text}", item.label.hits.map { (it.first + 2)..(it.last + 2) }, type.command)
        item.sub?.let { CivilightText(it, type.small, Modifier.weight(1f), color = colors.ink3, maxLines = 1) }
    }
}

/**
 * A name or command with its hit boxed inline. It goes through the DS `DecoratedText` with `revealBoxes`,
 * so the box grows left to right like every other search hit (CNV-44; motion audit, feature:story 10).
 */
@Composable
private fun HitText(text: String, hits: List<IntRange>, style: TextStyle, modifier: Modifier = Modifier) {
    val decorations = remember(text, hits) { searchHitDecorations(text, hits) }
    DecoratedText(
        text = AnnotatedString(text),
        style = style,
        modifier = modifier,
        color = CivilightTheme.colors.ink,
        decorations = decorations,
        revealBoxes = true,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private const val SCRIM = 0.82f
