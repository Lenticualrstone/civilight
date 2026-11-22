package io.github.lenticualrstone.civilight.feature.world.list

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
    // LIB-13 `세계 용어 하나 적기`: open the new-term form once on arrival (shell.md core pass, item 3).
    var createHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(route.createTerm) {
        if (route.createTerm && !createHandled) {
            createHandled = true
            vm.selectTab(io.github.lenticualrstone.civilight.navigation.WorldTab.TERMS)
            sheets.open(CreateItemSheet(io.github.lenticualrstone.civilight.navigation.WorldTab.TERMS))
        }
    }

    CivilightScreen(
        title = WorldCopy.WORLD_TITLE,
        smallLine = doc?.project?.name,
        back = worldBack(route, deps),
        lens = doc?.let(::lensChipLabel),
        onLens = { sheets.open(LensSheet(route.projectKey)) },
        actions = {
            CivilightIconButton(
                icon = IconId.Plus,
                contentDescription = WorldCopy.createLabel(kind),
                onClick = { sheets.open(CreateItemSheet(tab)) },
                enabled = !readOnly,
            )
        },
        commandBar = { WorldCommandBar(deps, route.projectKey, WorldCopy.SEARCH_WORLD) },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
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
                onCreate = { sheets.open(CreateItemSheet(tab)) },
            )
        }
    }
}

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
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val motion = CivilightTheme.motion
    val inner = LocalWindowLayout.current.isInner
    val view = WorldView(tab, showCards)
    // Card view: the list's own lock and empty lines stay, the rows become horizontal card rows.
    val body = remember(ui, cardRows, view) {
        val all = ui?.items.orEmpty()
        WorldBody(view, if (showCards) all.filter { it is WorldListItem.Lock || it is WorldListItem.Empty } else all, cardRows, ui?.candidatesPending == true)
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding())) {
            SegmentedControl(
                options = WorldSegment.labels,
                selectedIndex = WorldSegment.of(tab).ordinal,
                onSelect = { onSegment(WorldSegment.entries[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    FoldReveal(visible = tab == io.github.lenticualrstone.civilight.navigation.WorldTab.TERMS) {
                        TextAction(
                            text = WorldCopy.TERM_HINT,
                            onClick = onHint,
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
            if (shown.view.cards) {
                CardRowsBody(shown, grows, bottom, selectedKey, inner, readOnly, onCard, onUnlock, onCreate)
            } else {
                RowsBody(shown, bottom, selectedKey, readOnly, onRow, onUnlock, onCreate)
            }
        }
    }
}

@Composable
private fun RowsBody(
    body: WorldBody,
    bottom: androidx.compose.ui.unit.Dp,
    selectedKey: String?,
    readOnly: Boolean,
    onRow: (WorldListItem.Row) -> Unit,
    onUnlock: () -> Unit,
    onCreate: () -> Unit,
) {
    val keys = remember(body.items) { WorldViewSwitch.uniqueKeys(body.items.map { it.id }) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = bottom),
    ) {
        itemsIndexed(body.items, key = { i, _ -> keys[i] }) { _, item ->
            Box(civilightItem()) {
                ListItemView(item, selectedKey, readOnly, body.pending, onRow, onUnlock, onCreate)
            }
        }
    }
}

@Composable
private fun CardRowsBody(
    body: WorldBody,
    grows: Boolean,
    bottom: androidx.compose.ui.unit.Dp,
    selectedKey: String?,
    inner: Boolean,
    readOnly: Boolean,
    onCard: (WorldCardUi) -> Unit,
    onUnlock: () -> Unit,
    onCreate: () -> Unit,
) {
    val keys = remember(body.items) { WorldViewSwitch.uniqueKeys(body.items.map { it.id }) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottom),
    ) {
        // Card rows keep the content inset of the shared row; the other lines get the list's 24 dp.
        items(body.cardRows, key = { it.id }) { row ->
            Box(civilightItem()) {
                Box(Modifier.growIn(grows)) { WorldCardRow(row, selectedKey, inner, onCard) }
            }
        }
        itemsIndexed(body.items, key = { i, _ -> keys[i] }) { _, item ->
            Box(civilightItem().padding(horizontal = 24.dp)) {
                ListItemView(item, selectedKey, readOnly, body.pending, onRow = {}, onUnlock, onCreate)
            }
        }
    }
}

@Composable
private fun ListItemView(
    item: WorldListItem,
    selectedKey: String?,
    readOnly: Boolean,
    pending: Boolean,
    onRow: (WorldListItem.Row) -> Unit,
    onUnlock: () -> Unit,
    onCreate: () -> Unit,
) {
    when (item) {
        is WorldListItem.Section -> SectionLabel(item.title, Modifier.padding(top = 18.dp, bottom = 6.dp))
        is WorldListItem.Lock -> LockRow(
            title = item.text,
            actionLabel = WorldCopy.UNLOCK,
            onAction = onUnlock,
            modifier = Modifier.padding(top = 12.dp),
        )
        is WorldListItem.Empty -> EmptyBlock(
            title = item.line,
            modifier = Modifier.padding(top = 48.dp),
            primaryLabel = if (readOnly) null else item.button,
            onPrimary = if (readOnly) null else onCreate,
        )
        is WorldListItem.Row -> ListRow(
            title = item.title,
            subtitle = item.fragment.ifBlank { null },
            onClick = { onRow(item) },
            selected = item.key == selectedKey,
            trailing = { RightColumn(item, pending = pending) },
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
private fun RightColumn(row: WorldListItem.Row, pending: Boolean) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val firstColor = if (row.warning != null) colors.warn else colors.ink3
    Column(
        modifier = Modifier.widthIn(max = 168.dp),
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
