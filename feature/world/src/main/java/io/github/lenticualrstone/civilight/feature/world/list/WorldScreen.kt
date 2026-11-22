package io.github.lenticualrstone.civilight.feature.world.list

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

@Composable
private fun WorldList(
    ui: WorldListUi?,
    tab: io.github.lenticualrstone.civilight.navigation.WorldTab,
    padding: PaddingValues,
    selectedKey: String?,
    readOnly: Boolean,
    onSegment: (WorldSegment) -> Unit,
    onHint: () -> Unit,
    onRow: (WorldListItem.Row) -> Unit,
    onUnlock: () -> Unit,
    onCreate: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item(key = "segments") {
            SegmentedControl(
                options = WorldSegment.labels,
                selectedIndex = WorldSegment.of(tab).ordinal,
                onSelect = { onSegment(WorldSegment.entries[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (tab == io.github.lenticualrstone.civilight.navigation.WorldTab.TERMS) {
            item(key = "hint") {
                TextAction(
                    text = WorldCopy.TERM_HINT,
                    onClick = onHint,
                    modifier = Modifier.padding(top = 4.dp),
                    color = colors.ink3,
                    style = type.small,
                )
            }
        }
        val items = ui?.items.orEmpty()
        itemsIndexed(items, key = { i, item -> if (item is WorldListItem.Row) item.id else "${item.id}@$i" }) { _, item ->
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
                    trailing = { RightColumn(item, pending = ui?.candidatesPending == true) },
                )
            }
        }
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
