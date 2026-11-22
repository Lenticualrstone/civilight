package io.github.lenticualrstone.civilight.feature.story.structure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CommandBar
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.story.common.StoryCopy
import io.github.lenticualrstone.civilight.feature.story.common.storyServices
import io.github.lenticualrstone.civilight.feature.story.menu.StructureMenuContent
import io.github.lenticualrstone.civilight.feature.story.menu.StructureMenuSheet
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.NewUnitSheet
import io.github.lenticualrstone.civilight.navigation.ProjectSearch
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.StoryStructure
import io.github.lenticualrstone.civilight.navigation.UnitMenuSheet
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn

/** 스토리 구조 (STO-04; cover `Story`, inner right pane of `TwoHome`). No lens chip (R11 §3). */
@Composable
fun StructureScreen(route: StoryStructure, deps: FeatureDeps) {
    val vm = entryViewModel { StructureViewModel(route.projectKey, deps.storyServices()) }
    val load by vm.state.collectAsState()
    val unlock by deps.unlock.state.collectAsState()
    val ready = load as? ProjectLoad.Ready
    val locked = vm.itemsLocked(unlock)
    var saved by rememberSaveable { mutableStateOf<List<String>?>(null) }
    val initial = remember(ready != null) { ready?.let { StructureModel.initialExpanded(it.doc, route.focusUnitKey) } }
    val expanded = saved ?: initial.orEmpty()
    val ui = remember(ready, expanded, locked) { ready?.let { StructureModel.build(it.doc, expanded, locked, it.readOnly) } }
    val sheets = rememberSheetHost()
    val navigator = deps.navigator
    val p = route.projectKey
    CenterColumn {
        CivilightScreen(
            title = StructureCopy.TITLE,
            smallLine = ui?.projectName,
            back = if (LocalPaneRole.current == PaneRole.DETAIL) null else IconAction(StoryCopy.BACK) { navigator.pop() },
            actions = { CivilightIconButton(IconId.More, StructureCopy.MENU, { sheets.open(StructureMenuSheet(p)) }) },
            commandBar = {
                CommandBar(
                    hint = ui?.commandHint.orEmpty(),
                    onClick = { navigator.push(ProjectSearch(p)) },
                    onMic = { navigator.push(Recording(projectKey = p)) },
                )
            },
            bottomExtraRow = {
                Spacer(Modifier.weight(1f))
                CivilightButton(
                    text = StructureCopy.NEW_SCENE,
                    onClick = { ui?.newScene?.let { sheets.open(NewUnitSheet(p, parentKey = it.parentKey, afterKey = it.afterKey)) } },
                    variant = ButtonVariant.PRIMARY,
                    enabled = ui?.newScene != null && ui.readOnly.not(),
                )
            },
            sheets = sheets,
            localSheets = { key ->
                when (key) {
                    is StructureMenuSheet -> { { StructureMenuContent(key, deps) } }
                    else -> null
                }
            },
        ) { padding ->
            if (ui == null) {
                Box(Modifier.fillMaxSize().background(CivilightTheme.colors.bg))
            } else {
                StructureList(
                    ui = ui,
                    padding = padding,
                    onToggle = { key -> saved = StructureModel.toggle(expanded, key) },
                    onScene = { key -> navigator.push(Script(p, sceneKey = key)) },
                    onMenu = { key -> sheets.open(UnitMenuSheet(p, key)) },
                )
            }
        }
    }
}

@Composable
private fun StructureList(
    ui: StructureUi,
    padding: PaddingValues,
    onToggle: (Key) -> Unit,
    onScene: (Key) -> Unit,
    onMenu: (Key) -> Unit,
) {
    val colors = CivilightTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentPadding = PaddingValues(start = 24.dp, end = 12.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
    ) {
        item(key = "levels") {
            CivilightText(ui.levelLine, CivilightTheme.type.small, Modifier.padding(end = 12.dp), color = colors.ink3)
        }
        items(ui.items, key = { it.id }) { item ->
            when (item) {
                is GroupItem -> GroupHeader(item, onToggle)
                is EpisodeItem -> EpisodeLine(item, onToggle, onMenu)
                is SceneItem -> SceneLine(item, onScene, onMenu)
            }
        }
    }
}

/** `Ⅰ 1장 · 등불지기 | 7/10`; a planned one is `ink2` with `0/8 · 계획` and its synopsis. */
@Composable
private fun GroupHeader(item: GroupItem, onToggle: (Key) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val row = item.row
    val toggle = if (item.expandable) Modifier.pressable(onClick = { onToggle(row.key) }, scaleOnPress = false) else Modifier
    Column(Modifier.fillMaxWidth().padding(top = 26.dp, start = (item.depth * 16).dp, end = 12.dp)) {
        Row(Modifier.fillMaxWidth().then(toggle).heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            CivilightText(row.header, type.title, Modifier.weight(1f), color = if (row.planned) colors.ink2 else colors.ink, maxLines = 1)
            CivilightText(row.progress, type.number, color = if (row.planned) colors.ink3 else colors.ink2, maxLines = 1)
        }
        row.synopsis?.let { CivilightText(it, type.leadCompact, Modifier.padding(top = 4.dp), color = colors.ink2) }
    }
}

/** `⠿ 1-7 | 잿빛 새벽의 등불 | 씬 4개 · 5,210자 | ⋯`; unwritten episodes are `ink3`. */
@Composable
private fun EpisodeLine(item: EpisodeItem, onToggle: (Key) -> Unit, onMenu: (Key) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val row = item.row
    val bold = row.expanded || item.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .pressable(onClick = { onToggle(row.key) }, scaleOnPress = false)
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CivilightIcon(IconId.Grip, contentDescription = null, size = 16.dp, tint = colors.ink3)
        CivilightText(row.code, type.number, Modifier.widthIn(min = 30.dp), color = colors.ink3, maxLines = 1)
        CivilightText(
            row.title,
            type.itemTitle.merge(TextStyle(fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)),
            Modifier.weight(1f),
            color = if (row.unwritten) colors.ink3 else colors.ink,
            maxLines = 1,
        )
        CivilightText(row.meta, type.small, color = colors.ink3, maxLines = 1)
        CivilightIconButton(IconId.More, StructureCopy.MENU, { onMenu(row.key) })
    }
}

/** `1일째 새벽 | 씬 1 · 새벽 순찰 | 1,820자 | ⋯`; the scene being written has the `sel` fill and `acc` bar. */
@Composable
private fun SceneLine(item: SceneItem, onScene: (Key) -> Unit, onMenu: (Key) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val row = item.row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectedRowBackground(row.current, colors.sel, colors.acc)
            .pressable(onClick = { onScene(row.key) }, scaleOnPress = false)
            .heightIn(min = 44.dp)
            .padding(start = if (item.indented) 46.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CivilightText(row.time, type.numberHeader, Modifier.widthIn(min = 64.dp), color = colors.ink3, maxLines = 1)
        CivilightText(row.label, type.itemTitleCompact.merge(TextStyle(fontWeight = FontWeight.Normal)), Modifier.weight(1f), color = colors.ink, maxLines = 1)
        CivilightText(row.meta, type.small, color = if (row.current) colors.acc else colors.ink3, maxLines = 1)
        CivilightIconButton(IconId.More, StructureCopy.MENU, { onMenu(row.key) })
    }
}
