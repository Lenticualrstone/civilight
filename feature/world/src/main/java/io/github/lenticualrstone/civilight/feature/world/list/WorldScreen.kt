package io.github.lenticualrstone.civilight.feature.world.list

import androidx.compose.foundation.lazy.rememberLazyListState
import io.github.lenticualrstone.civilight.designsystem.component.ListCreateRow
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.feature.world.common.WorldCreateCard
import io.github.lenticualrstone.civilight.feature.world.common.WorldCreateCards
import io.github.lenticualrstone.civilight.feature.world.common.WorldCreateDraft
import io.github.lenticualrstone.civilight.designsystem.component.createdHighlight
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.ui.picker.PlacePickerSheet
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import io.github.lenticualrstone.civilight.designsystem.card.CardViewMotion
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import io.github.lenticualrstone.civilight.designsystem.component.ViewToggle
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.WorldCommandBar
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.WorldTargets
import io.github.lenticualrstone.civilight.feature.world.common.selectedCardKey
import io.github.lenticualrstone.civilight.feature.world.common.worldBack
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.MapList
import io.github.lenticualrstone.civilight.navigation.World
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import io.github.lenticualrstone.civilight.ui.nav.openDetail
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import io.github.lenticualrstone.civilight.ui.tour.TourHold
import io.github.lenticualrstone.civilight.ui.tour.tourTarget

/**
 * 세계 (WLD-01 … WLD-04, boards `World`, `TwoWorld`, `ColWorld`): header `<project>` · `세계` with the
 * lens chip and `+`, segments `장소 · 용어 · 세력 · 지도`, the `용어` hint line, the list of the tab,
 * and the command bar. In two panes the open card's row is selected (the shell draws the card).
 */
@Composable
fun WorldScreen(route: World, deps: FeatureDeps) {
    val vm = entryViewModel { WorldListViewModel(route.projectKey, route.tab, WorldEnv.of(deps)) }
    val ui by vm.ui.collectAsState()
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val sheets = rememberSheetHost()
    val gate = rememberHiddenGate(route.projectKey)
    val selected = if (LocalPaneRole.current == PaneRole.LIST) selectedCardKey(deps) else null
    val tab = ui?.tab ?: route.tab
    val kind = WorldTabs.kindOf(tab)
    val settings by deps.settings.collectAsState()
    val showCards = WorldCardRows.showCards(settings.viewByScreen, tab)
    val scope = rememberCoroutineScope()
    val cardRows = remember(doc, ui) { val d = doc; val u = ui; if (d != null && u != null) WorldCardRows.build(d, u.items) else emptyList() }
    // LIB-19 world tour: it waits for the list, so its holes are known before a card shows (WorldTourSteps).
    if (ui == null) TourHold()
    // (0.2.0, U-15, WLD-70) The create card over the list: its draft (null = closed) and the row just made.
    var createText by rememberSaveable { mutableStateOf<String?>(null) }
    val draft = WorldCreateDraft.decode(createText)
    var highlightKey by remember { mutableStateOf<String?>(null) }
    val hiddenOpen = gate.open
    // LIB-13 `세계 용어 하나 적기`: open the new-term card once on arrival (shell.md core pass, item 3).
    var createHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(route.createTerm, readOnly) {
        if (route.createTerm && !createHandled && !readOnly) {
            createHandled = true
            vm.selectTab(io.github.lenticualrstone.civilight.navigation.WorldTab.TERMS)
            createText = WorldCreateDraft.of(WorldKind.TERM).encode()
        }
    }
    fun create(open: Boolean) {
        val d = draft ?: return
        val k = d.worldKind ?: return
        scope.launch {
            val newKey = vm.createInPlace(k, WorldCreateCards.item(d)) ?: return@launch
            createText = null
            if (open) deps.navigator.openDetail(WorldTargets.card(route.projectKey, k, newKey)) else highlightKey = newKey
        }
    }

    CivilightScreen(
        title = WorldCopy.WORLD_TITLE,
        smallLine = doc?.project?.name,
        back = worldBack(route, deps),
        lens = doc?.let(::lensChipLabel),
        onLens = { sheets.open(LensSheet(route.projectKey)) },
        commandBar = { WorldCommandBar(deps, route.projectKey, WorldCopy.SEARCH_WORLD) },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is CreateParentPickSheet -> doc?.let { d ->
                    {
                        PlacePickerSheet(
                            doc = d,
                            gate = gate,
                            title = WorldCopy.PICK_PLACE,
                            onPick = { ref ->
                                WorldCreateDraft.decode(createText)?.let { createText = it.copy(parentKey = ref.key).encode() }
                                close()
                            },
                            onClose = ::close,
                            selected = draft?.parentKey?.let { PlaceRef(PlaceRefKind.PLACE, it) },
                            includeRegions = false,
                        )
                    }
                }
                is CreateItemSheet -> {
                    {
                        val sheetKind = WorldTabs.kindOf(key.tab)
                        CreateItemSheetContent(
                            kind = sheetKind,
                            readOnly = readOnly,
                            create = { name, description -> vm.create(sheetKind, name, description) },
                            onCreated = { newKey -> deps.navigator.openDetail(WorldTargets.card(route.projectKey, sheetKind, newKey)) },
                        )
                    }
                }
                else -> null
            }
        },
    ) { padding ->
        CenterColumn {
            WorldList(
                ui = ui,
                tab = tab,
                padding = padding,
                selectedKey = selected,
                readOnly = readOnly,
                showCards = showCards,
                cardRows = cardRows,
                onView = { cards ->
                    scope.launch { deps.stores.settings.update { it.copy(viewByScreen = WorldCardRows.withView(it.viewByScreen, tab, cards)) } }
                },
                onCard = { card -> deps.navigator.openDetail(WorldTargets.card(route.projectKey, card.kind, card.key)) },
                onSegment = { segment ->
                    val t = segment.tab
                    if (t == null) deps.navigator.push(MapList(route.projectKey)) else vm.selectTab(t)
                },
                onHint = { deps.navigator.push(CheckCenter(route.projectKey, CheckFilter.TERM.name)) },
                onRow = { row -> deps.navigator.openDetail(WorldTargets.card(route.projectKey, row.kind, row.key)) },
                onUnlock = gate.unlock,
                onCreate = { createText = WorldCreateDraft.of(kind).encode() },
                onCandidate = { row ->
                    // (0.2.0, WLD-73) The card opens scrolled to its `후보` group.
                    io.github.lenticualrstone.civilight.feature.world.term.TermCardScroll.request(route.projectKey, row.key)
                    deps.navigator.openDetail(WorldTargets.card(route.projectKey, WorldKind.TERM, row.key))
                },
                highlightKey = highlightKey,
                onHighlightDone = { highlightKey = null },
            )
        }
        // The card is hidden while its own `상위 지역` picker is on top, and comes back when it closes.
        if (draft != null && !readOnly && sheets.top !is CreateParentPickSheet) {
            WorldCreateCard(
                draft = draft,
                doc = doc,
                hiddenOpen = hiddenOpen,
                onChange = { createText = it.encode() },
                onCancel = { createText = null },
                onCreate = { create(open = false) },
                onCreateAndOpen = { create(open = true) },
                onPickParent = { sheets.open(CreateParentPickSheet) },
            )
        }
    }
}

/** (0.2.0, WLD-70) The `상위 지역` picker of the place create card (feature-local; its own picker has no create row). */
@kotlinx.serialization.Serializable
data object CreateParentPickSheet : io.github.lenticualrstone.civilight.navigation.SheetKey

/** One body of the list: the data of a [WorldView], kept per view so a leaving body keeps drawing its own tab. */
private data class WorldBody(val view: WorldView, val items: List<WorldListItem>, val cardRows: List<WorldCardRowUi>, val pending: Boolean)

@Composable
private fun WorldList(
    ui: WorldListUi?,
    tab: io.github.lenticualrstone.civilight.navigation.WorldTab,
    padding: PaddingValues,
    selectedKey: String?,
    readOnly: Boolean,
    showCards: Boolean,
    cardRows: List<WorldCardRowUi>,
    onView: (Boolean) -> Unit,
    onCard: (WorldCardUi) -> Unit,
    onSegment: (WorldSegment) -> Unit,
    onHint: () -> Unit,
    onRow: (WorldListItem.Row) -> Unit,
    onUnlock: () -> Unit,
    onCreate: () -> Unit,
    onCandidate: (WorldListItem.CandidateRow) -> Unit,
    highlightKey: String?,
    onHighlightDone: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val motion = CivilightTheme.motion
    val inner = LocalWindowLayout.current.isInner
    val view = WorldView(tab, showCards)
    // Card view: the list's own lock and empty lines stay, the rows become horizontal card rows.
    val body = remember(ui, cardRows, view) {
        val all = ui?.items.orEmpty()
        WorldBody(view, if (showCards) WorldCardRows.cardViewLines(all) else all, cardRows, ui?.candidatesPending == true)
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding())) {
            SegmentedControl(
                options = WorldSegment.labels,
                selectedIndex = WorldSegment.of(tab).ordinal,
                onSelect = { onSegment(WorldSegment.entries[it]) },
                // LIB-19 world tour: the segments back up every step whose own element is not on screen.
                modifier = WorldTourSteps.TABS_FALLBACK.fold(Modifier.fillMaxWidth()) { m, step -> m.tourTarget(step, fallback = true) },
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    FoldReveal(visible = tab == io.github.lenticualrstone.civilight.navigation.WorldTab.TERMS) {
                        TextAction(
                            text = WorldCopy.TERM_HINT,
                            onClick = onHint,
                            modifier = Modifier.tourTarget(WorldTourSteps.REVEAL),
                            color = colors.ink3,
                            style = type.small,
                        )
                    }
                }
                ViewToggle(
                    showCards = showCards,
                    onChange = onView,
                    groupLabel = WorldCopy.VIEW_GROUP,
                    listLabel = WorldCopy.VIEW_LIST,
                    cardLabel = WorldCopy.VIEW_CARDS,
                )
            }
        }
        // WLD-05 / WLD-68: `목록 ↔ 카드` is the shared card-view motion, a change of tab a crossfade.
        val bodyTransition = updateTransition(body, label = "worldBody")
        bodyTransition.AnimatedContent(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = { WorldViewSwitch.transition(initialState.view, targetState.view, motion) },
            contentKey = { it.view },
        ) { shown ->
            // The cards grow in place only in the composition that brings them in (the `목록 → 카드` flip), and
            // stop being "new" once the transition has settled, so a card scrolled in later does not grow.
            val growStart = remember { WorldViewSwitch.growsIn(bodyTransition.currentState.view, bodyTransition.targetState.view, motion.reduced) }
            var grows by remember { mutableStateOf(growStart) }
            LaunchedEffect(Unit) {
                if (growStart) {
                    snapshotFlow { bodyTransition.currentState.view == shown.view }.first { it }
                    grows = false
                }
            }
            val bottom = padding.calculateBottomPadding() + 24.dp
            val lines = ListLines(selectedKey, readOnly, onUnlock, onCreate, onCandidate, highlightKey, onHighlightDone, shown.view.tab)
            if (shown.view.cards) {
                CardRowsBody(shown, grows, bottom, inner, onCard, lines)
            } else {
                RowsBody(shown, bottom, onRow, lines)
            }
        }
    }
}

/** What every list line needs besides its item (U-15 create row, highlight, the candidate rows). */
private data class ListLines(
    val selectedKey: String?,
    val readOnly: Boolean,
    val onUnlock: () -> Unit,
    val onCreate: () -> Unit,
    val onCandidate: (WorldListItem.CandidateRow) -> Unit,
    val highlightKey: String?,
    val onHighlightDone: () -> Unit,
    val tab: io.github.lenticualrstone.civilight.navigation.WorldTab,
)

/**
 * (0.2.0, U-15.1, WLD-70) The list's first row `＋ 새 장소 · ＋ 새 용어 · ＋ 새 세력` (class b, icon `plus`),
 * under the tabs and the hint line and above the sections; not drawn in a read-only project (U-15.10).
 */
@Composable
private fun CreateLine(lines: ListLines, modifier: Modifier = Modifier) {
    ListCreateRow(WorldCreateCards.createKind(WorldTabs.kindOf(lines.tab)), lines.onCreate, modifier)
}

@Composable
private fun RowsBody(
    body: WorldBody,
    bottom: androidx.compose.ui.unit.Dp,
    onRow: (WorldListItem.Row) -> Unit,
    lines: ListLines,
) {
    val keys = remember(body.items) { WorldViewSwitch.uniqueKeys(body.items.map { it.id }) }
    val linkKey = remember(body.items) { WorldTourSteps.linkRowKey(body.items) }
    val listState = rememberLazyListState()
    val offset = if (lines.readOnly) 0 else 1
    LaunchedEffect(lines.highlightKey, body.items) {
        val i = body.items.indexOfFirst { (it as? WorldListItem.Row)?.key == lines.highlightKey }
        if (lines.highlightKey != null && i >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == i + offset }) {
            listState.animateScrollToItem(i + offset)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = bottom),
    ) {
        if (!lines.readOnly) item(key = "create") { Box(civilightItem()) { CreateLine(lines, Modifier.padding(top = 4.dp)) } }
        itemsIndexed(body.items, key = { i, _ -> keys[i] }) { _, item ->
            Box(civilightItem()) {
                ListItemView(item, lines, body.pending, onRow, tourLink = linkKey != null && (item as? WorldListItem.Row)?.key == linkKey)
            }
        }
    }
}

@Composable
private fun CardRowsBody(
    body: WorldBody,
    grows: Boolean,
    bottom: androidx.compose.ui.unit.Dp,
    inner: Boolean,
    onCard: (WorldCardUi) -> Unit,
    lines: ListLines,
) {
    val selectedKey = lines.selectedKey
    val keys = remember(body.items) { WorldViewSwitch.uniqueKeys(body.items.map { it.id }) }
    // (U-15.6) Card rows draw no `만들기` highlight: drop the key here so it does not flash later in list view.
    LaunchedEffect(lines.highlightKey, body.items) {
        if (lines.highlightKey != null && body.items.none { (it as? WorldListItem.Row)?.key == lines.highlightKey }) lines.onHighlightDone()
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottom),
    ) {
        // (0.2.0, WLD-05) In card view the `＋ 새 <kind>` row stays above the card rows.
        if (!lines.readOnly) item(key = "create") { Box(civilightItem().padding(horizontal = 24.dp)) { CreateLine(lines, Modifier.padding(top = 4.dp)) } }
        // Card rows keep the content inset of the shared row; the other lines get the list's 24 dp.
        items(body.cardRows, key = { it.id }) { row ->
            Box(civilightItem()) {
                Box(Modifier.growIn(grows)) { WorldCardRow(row, selectedKey, inner, onCard) }
            }
        }
        itemsIndexed(body.items, key = { i, _ -> keys[i] }) { _, item ->
            Box(civilightItem().padding(horizontal = 24.dp)) {
                ListItemView(item, lines, body.pending, onRow = {})
            }
        }
    }
}

@Composable
private fun ListItemView(
    item: WorldListItem,
    lines: ListLines,
    pending: Boolean,
    onRow: (WorldListItem.Row) -> Unit,
    tourLink: Boolean = false,
) {
    val selectedKey = lines.selectedKey
    val onUnlock = lines.onUnlock
    when (item) {
        is WorldListItem.Section -> SectionLabel(item.title, Modifier.padding(top = 18.dp, bottom = 6.dp))
        is WorldListItem.Lock -> LockRow(
            title = item.text,
            actionLabel = WorldCopy.UNLOCK,
            onAction = onUnlock,
            modifier = Modifier.padding(top = 12.dp),
        )
        // (0.2.0, WLD-02) The empty line stays under the create row; its `<kind> 만들기` button is superseded.
        is WorldListItem.Empty -> EmptyBlock(
            title = item.line,
            modifier = Modifier.padding(top = 48.dp),
        )
        is WorldListItem.CandidateRow -> ListRow(
            title = item.title,
            subtitle = item.fragment,
            onClick = { lines.onCandidate(item) },
            trailing = item.right?.let { right ->
                { CivilightText(right, CivilightTheme.type.small, color = CivilightTheme.colors.ink3, textAlign = TextAlign.End) }
            },
        )
        is WorldListItem.Row -> ListRow(
            title = item.title,
            subtitle = item.fragment.ifBlank { null },
            onClick = { onRow(item) },
            selected = item.key == selectedKey,
            modifier = Modifier.createdHighlight(item.key == lines.highlightKey, lines.onHighlightDone),
            // LIB-19 world tour: the first `◇ 후보` column is the `용어 잇기` hole.
            trailing = { RightColumn(item, pending = pending, modifier = if (tourLink) Modifier.tourTarget(WorldTourSteps.LINK) else Modifier) },
        )
    }
}

/**
 * A card row that grows in place from its top-left corner (WLD-68, `CardViewMotion`): [grows] is true only for
 * the rows composed by the `목록 → 카드` flip, so the start scale is set in the composition that brings the card
 * in (never one frame later), and the grow waits for the list text to leave (the plan's text-in delay).
 * Reduced motion and every other change: drawn at rest.
 */
@Composable
private fun Modifier.growIn(grows: Boolean): Modifier {
    val motion = CivilightTheme.motion
    val scale = remember { Animatable(if (grows) WorldViewSwitch.GROW_FROM else 1f) }
    LaunchedEffect(Unit) {
        if (grows) scale.animateTo(1f, motion.move(CivilightMotion.enterLong, delayMs = CardViewMotion.plan(motion.reduced).textInDelayMs))
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
        transformOrigin = TransformOrigin(0f, 0f)
    }
}

/** The row's right column (`.small`, right-aligned): `첫 공개 1-7 · ◇ 후보 1` / `▲ 앞에서 1회 사용`. */
@Composable
private fun RightColumn(row: WorldListItem.Row, pending: Boolean, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val firstColor = if (row.warning != null) colors.warn else colors.ink3
    Column(
        modifier = modifier.widthIn(max = 168.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        val line = buildAnnotatedString {
            withStyle(SpanStyle(color = firstColor)) { append(row.right) }
            if (row.candidates != null) {
                withStyle(SpanStyle(color = firstColor)) { append(WorldCopy.SEP) }
                withStyle(SpanStyle(color = colors.ink3.copy(alpha = if (pending) 0.5f else 1f))) { append(row.candidates) }
            }
        }
        BasicText(line, style = type.small.merge(textAlign = TextAlign.End))
        if (row.warning != null) CivilightText(row.warning, type.small, color = colors.warn, textAlign = TextAlign.End)
    }
}
