package io.github.lenticualrstone.civilight.feature.world.time

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.designsystem.component.ListCreateRow
import io.github.lenticualrstone.civilight.feature.world.common.WorldCreateCard
import io.github.lenticualrstone.civilight.feature.world.common.WorldCreateCards
import io.github.lenticualrstone.civilight.feature.world.common.WorldCreateDraft
import io.github.lenticualrstone.civilight.designsystem.component.createdHighlight
import io.github.lenticualrstone.civilight.ui.picker.UnitPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.feature.world.common.dimmed
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.world.time.TimelineView
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.WorldCommandBar
import io.github.lenticualrstone.civilight.feature.world.common.WorldEnv
import io.github.lenticualrstone.civilight.feature.world.common.worldBack
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.navigation.EventCreateSheet
import io.github.lenticualrstone.civilight.navigation.EventDetail
import io.github.lenticualrstone.civilight.navigation.TimelineMap
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.navigation.Timeline
import io.github.lenticualrstone.civilight.navigation.WorldCalendar
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.lens.lensChipLabel
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import io.github.lenticualrstone.civilight.ui.nav.openDetail
import io.github.lenticualrstone.civilight.ui.picker.EventPickerSheet
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.serialization.Serializable

/** `기준 사건` chip: pick the anchor event (WLD-34; feature-local). */
@Serializable
data object AnchorPickSheet : SheetKey

/** Width of the time column (board `Timeline`: mono 12 in 64 px, 10 px gap). */
private val TIME_WIDTH = 64.dp
private val TIME_GAP = 10.dp

/**
 * 연표 (WLD-31, boards `Timeline`, `TwoTimeline` list pane, `ColTimeline`): header
 * `세계 시간과 이야기 시간` · `연표` · lens chip · `+` (사건 만들기), the info line with
 * `나이 자동 계산`, the view chips, and the line list with diamond dots. A tap on an event row opens
 * the event detail beside the list (CNV-33). In `시간 없이` the module is off.
 */
@Composable
fun TimelineScreen(route: Timeline, deps: FeatureDeps) {
    val vm = entryViewModel { TimelineViewModel(route.projectKey, WorldEnv.of(deps)) }
    val ui by vm.ui.collectAsState()
    val doc by vm.doc.collectAsState()
    val readOnly by vm.readOnly.collectAsState()
    val sheets = rememberSheetHost()
    // The first view follows the project's `때 표기` (WLD-34); the chips change it for this screen.
    var viewName by rememberSaveable { mutableStateOf<String?>(null) }
    var anchorKey by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(doc != null) { if (viewName == null) doc?.let { viewName = TimelineState.defaultView(it).name } }
    LaunchedEffect(viewName, anchorKey) { viewName?.let { vm.setView(TimelineView.valueOf(it), anchorKey) } }
    val selected = (deps.navigator.backStack.lastOrNull() as? EventDetail)?.eventKey.takeIf { LocalPaneRole.current == PaneRole.LIST }
    val timeline = ui
    val gate = rememberHiddenGate(route.projectKey)
    val scope = rememberCoroutineScope()
    // (0.2.0, U-15, WLD-74) The `새 사건` create card: its draft (null = closed) and the row just made.
    var createText by rememberSaveable { mutableStateOf<String?>(null) }
    val draft = WorldCreateDraft.decode(createText)
    var highlightKey by remember { mutableStateOf<String?>(null) }
    fun create(open: Boolean) {
        val d = draft ?: return
        scope.launch {
            val key = vm.save(WorldCreateCards.eventDraft(d), eventKey = null) ?: return@launch
            createText = null
            if (open) deps.navigator.openDetail(EventDetail(route.projectKey, key)) else highlightKey = key
        }
    }

    CivilightScreen(
        title = timeline?.title ?: WorldCopy.TIMELINE_TITLE,
        smallLine = timeline?.smallLine,
        back = worldBack(route, deps),
        actions = {
            if (timeline?.available == true) {
                // WLD-64: the 연표 + 지도 screen (its only entry).
                CivilightIconButton(
                    icon = IconId.Pin,
                    contentDescription = WorldCopy.TIMELINE_MAP,
                    onClick = { deps.navigator.push(TimelineMap(route.projectKey)) },
                )
            }
        },
        commandBar = { WorldCommandBar(deps, route.projectKey, timeline?.searchHint ?: WorldCopy.SEARCH_TIMELINE, EntrySurface.OTHER_IN_PROJECT) },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is CreateScenePickSheet -> doc?.let { d ->
                    {
                        UnitPickerSheet(
                            doc = d, gate = gate, title = WorldCopy.PICK_SCENE,
                            onPick = { picked ->
                                WorldCreateDraft.decode(createText)?.let { createText = WorldCreateCards.withScene(it, picked).encode() }
                                close()
                            },
                            onClose = ::close,
                            selectedKey = draft?.sceneKey,
                            levels = io.github.lenticualrstone.civilight.feature.world.term.RevealOptions.sceneLevels(d),
                        )
                    }
                }
                is CreatePeoplePickSheet -> doc?.let { d ->
                    {
                        PeopleTicks(
                            doc = d,
                            selected = draft?.characterKeys.orEmpty(),
                            gate = gate,
                            onToggle = { key ->
                                WorldCreateDraft.decode(createText)?.let { cur ->
                                    val keys = if (key in cur.characterKeys) cur.characterKeys - key else cur.characterKeys + key
                                    createText = cur.copy(characterKeys = keys).encode()
                                }
                            },
                            onClose = ::close,
                        )
                    }
                }
                is AnchorPickSheet -> doc?.let { d ->
                    {
                        EventPickerSheet(
                            doc = d,
                            title = WorldCopy.ANCHOR_PICK,
                            onPick = { picked ->
                                anchorKey = picked
                                viewName = TimelineView.ANCHOR.name
                                close()
                            },
                            onClose = ::close,
                            selectedKey = anchorKey,
                        )
                    }
                }
                else -> null
            }
        },
    ) { padding ->
        CenterColumn {
            // P3: no timeline yet, `시간 없이`, and the line list swap as bodies.
            ContentSwap(timeline?.available, Modifier.fillMaxSize(), label = "timelineBody") { available ->
                when {
                    timeline == null || available == null -> Unit
                    !available -> EmptyBlock(
                        title = WorldCopy.TIMELINE_OFF,
                        modifier = Modifier.padding(top = padding.calculateTopPadding() + 48.dp),
                        primaryLabel = WorldCopy.WORLD_TIME,
                        onPrimary = { deps.navigator.push(WorldCalendar(route.projectKey)) },
                    )
                    else -> TimelineList(
                        ui = timeline,
                        padding = padding,
                        selectedKey = selected,
                        onChip = { chip ->
                            if (chip.view == TimelineView.ANCHOR) sheets.open(AnchorPickSheet) else viewName = chip.view.name
                        },
                        onRow = { row -> row.eventKey?.let { deps.navigator.openDetail(EventDetail(route.projectKey, it)) } },
                        onCreate = if (readOnly) null else ({ createText = WorldCreateDraft.event().encode() }),
                        highlightKey = highlightKey,
                        onHighlightDone = { highlightKey = null },
                    )
                }
            }
        }
        // The card is hidden while its `씬 고르기` picker is on top, and comes back when it closes.
        if (draft != null && !readOnly && timeline?.available == true && sheets.top !is CreateScenePickSheet && sheets.top !is CreatePeoplePickSheet) {
            WorldCreateCard(
                draft = draft,
                doc = doc,
                hiddenOpen = gate.open,
                onChange = { createText = it.encode() },
                onCancel = { createText = null },
                onCreate = { create(open = false) },
                onCreateAndOpen = { create(open = true) },
                onPickScene = { sheets.open(CreateScenePickSheet) },
                onPickPeople = { sheets.open(CreatePeoplePickSheet) },
            )
        }
    }
}

/** (0.2.0, WLD-74) `씬 고르기` of the event create card (feature-local). */
@Serializable
data object CreateScenePickSheet : SheetKey

/** (0.2.0, WLD-74) `관련 인물` of the event create card (feature-local; no create row inside). */
@Serializable
data object CreatePeoplePickSheet : SheetKey

@Composable
private fun TimelineList(
    ui: TimelineUi,
    padding: PaddingValues,
    selectedKey: String?,
    onChip: (ViewChipUi) -> Unit,
    onRow: (TimelineRowUi) -> Unit,
    onCreate: (() -> Unit)?,
    highlightKey: String?,
    onHighlightDone: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    // (U-15.6, WLD-70) After `만들기` the list scrolls to the new event only if its row is out of view.
    LaunchedEffect(highlightKey, ui.rows) {
        val i = TimelineScroll.rowIndex(ui.rows, highlightKey, hasCreate = onCreate != null) ?: return@LaunchedEffect
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == i }) listState.animateScrollToItem(i)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item(key = "info") {
            Row(civilightItem().fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                CivilightText(ui.infoLine, type.small, Modifier.weight(1f), color = colors.ink3)
                CivilightText(WorldCopy.AGES_AUTO, type.small, Modifier.padding(start = 8.dp), color = colors.ink3)
            }
        }
        item(key = "chips") {
            ChipFlow(civilightItem().padding(top = 10.dp)) {
                ui.chips.forEach { chip -> FilterChip(label = chip.label, selected = chip.selected, onClick = { onChip(chip) }) }
            }
        }
        // (0.2.0, U-15.4, WLD-74) The list's first row `＋ 새 사건` (class b), above the first event; not in a
        // read-only project.
        if (onCreate != null) {
            item(key = "create") { ListCreateRow(CreateKind.EVENT, onCreate, civilightItem().padding(top = 6.dp)) }
        }
        items(ui.rows, key = { it.id }) { row ->
            if (row.kind == TimelineRowKind.DIVIDER) {
                Box(
                    civilightItem()
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 10.dp)
                        .topHairline(colors.line)
                        .padding(vertical = 8.dp),
                ) {
                    CivilightText(row.title, type.sec, color = colors.ink3)
                }
            } else {
                TimelineLine(row, selected = row.eventKey != null && row.eventKey == selectedKey, onClick = row.eventKey?.let { { onRow(row) } }, modifier = civilightItem().createdHighlight(row.eventKey != null && row.eventKey == highlightKey, onHighlightDone))
            }
        }
        ui.footer?.let { footer ->
            item(key = "footer") { CivilightText(footer, type.small, civilightItem().padding(top = 14.dp), color = colors.ink3) }
        }
    }
}

/**
 * One row of the line list: a diamond on the vertical line (`acc` and larger on the lens row),
 * the mono time in 64 dp, the title (Myeongjo 700 16; world events `ink2`) and the sub line.
 */
@Composable
private fun TimelineLine(row: TimelineRowUi, selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val dot = if (row.current) 9.dp else 5.dp
    val hl by animatedFraction(selected)
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(PressAction.Click(onClick), role = Role.Button, scaleOnPress = false) else Modifier)
            .drawBehind { drawRect(colors.hl.copy(alpha = colors.hl.alpha * hl)) }
            .heightIn(min = 44.dp)
            .height(IntrinsicSize.Min)
            .dimmed(row.dimmed),
    ) {
        // The line and its diamond (board: 1 px `line` with the dot centred on it).
        Box(Modifier.width(24.dp).fillMaxHeight()) {
            Box(Modifier.padding(start = 6.dp).width(1.dp).fillMaxHeight().background(colors.line))
            Box(
                Modifier
                    .offset(x = 6.dp - dot / 2, y = if (row.current) 14.dp else 16.dp)
                    .size(dot)
                    .rotate(45f)
                    .background(if (row.current) colors.acc else colors.ink3),
            )
        }
        Column(Modifier.weight(1f).padding(top = 10.dp, bottom = 14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(TIME_GAP), verticalAlignment = Alignment.Top) {
                Column(Modifier.width(TIME_WIDTH)) {
                    CivilightText(row.time, type.number, color = colors.ink3)
                    row.timeSub?.let { CivilightText(it, type.small, color = colors.ink3) }
                }
                CivilightText(
                    row.title,
                    type.itemTitle,
                    Modifier.weight(1f),
                    color = if (row.kind == TimelineRowKind.WORLD_EVENT) colors.ink2 else colors.ink,
                )
            }
            if (row.sub.isNotEmpty()) {
                CivilightText(row.sub, type.small, Modifier.padding(start = TIME_WIDTH + TIME_GAP, top = 2.dp), color = colors.ink3)
            }
        }
    }
}
