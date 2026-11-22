package io.github.lenticualrstone.civilight.feature.story.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxMark
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import androidx.compose.ui.text.style.TextDecoration
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CommandBar
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.LensChip
import io.github.lenticualrstone.civilight.designsystem.component.RatingBadge
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.bottomHairline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.ValueSwap
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.feature.story.common.ChapterHeaderText
import io.github.lenticualrstone.civilight.feature.story.common.StoryCopy
import io.github.lenticualrstone.civilight.feature.story.common.storyServices
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.LensSheet
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.ProjectSearch
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.StoryStructure
import io.github.lenticualrstone.civilight.navigation.popTo
import io.github.lenticualrstone.civilight.navigation.resetTo
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import io.github.lenticualrstone.civilight.ui.nav.openDetail
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.theme.ThemeBridge
import io.github.lenticualrstone.civilight.ui.window.CenterColumn

/** 프로젝트 홈 (STO-01; cover `Home`, inner `TwoHome` list pane, `ColHome` column). */
@Composable
fun HomeScreen(route: ProjectHome, deps: FeatureDeps) {
    val vm = entryViewModel { HomeViewModel(route.projectKey, deps.storyServices()) }
    val ui by vm.ui.collectAsState()
    val sheets = rememberSheetHost()
    val navigator = deps.navigator
    val inDetailPane = LocalPaneRole.current == PaneRole.DETAIL
    CenterColumn {
        CivilightScreen(
            title = if (ui?.newReady == true) HomeCopy.NEW_PROJECT else HomeCopy.PROJECT,
            smallLine = HomeCopy.LIBRARY,
            back = if (inDetailPane) null else IconAction(HomeCopy.LIBRARY) { if (!navigator.popTo(Library)) navigator.resetTo(Library) },
            actions = {
                // `NewReady` draws neither: the rating sits in the summary line under the name. The group
                // folds in and out when the state changes (motion audit, feature:story 8).
                FoldRevealValue(ui?.takeIf { !it.newReady }, horizontal = true) { u ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RatingBadge(ThemeBridge.rating(u.rating))
                        Spacer(Modifier.width(4.dp))
                        LensChip(u.lens.text, u.lens.state, { sheets.open(LensSheet(route.projectKey)) })
                    }
                }
            },
            commandBar = {
                CommandBar(
                    hint = ui?.commandHint.orEmpty(),
                    onClick = { navigator.push(ProjectSearch(route.projectKey)) },
                    onMic = { navigator.push(Recording(projectKey = route.projectKey)) },
                )
            },
            sheets = sheets,
        ) { padding ->
            // Loading -> content swaps on the kind (loaded or not), never on the data, so an edit does
            // not cross-fade the whole screen (motion audit, feature:story 7).
            ContentSwap(ui != null, Modifier.fillMaxSize()) { loaded ->
                val u = ui
                if (!loaded || u == null) {
                    Box(Modifier.fillMaxSize().background(CivilightTheme.colors.bg))
                } else {
                    HomeContent(
                        ui = u,
                        padding = padding,
                        onResume = { navigator.push(Script(u.projectKey, blockKey = it.blockKey, caretAtEnd = true)) },
                        onEpisode = { navigator.openDetail(StoryStructure(u.projectKey, focusUnitKey = it.key)) },
                        onShortcut = { shortcut ->
                            val target = shortcut.route(u.projectKey)
                            if (target is StoryStructure) navigator.openDetail(target) else navigator.push(target)
                        },
                        onStartRoute = { navigator.push(it) },
                        onDismissStart = { vm.dismissStart() },
                        selectedKey = HomeModel.selectedKey(navigator.backStack, route.projectKey),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    ui: HomeUi,
    padding: PaddingValues,
    onResume: (HomeResume) -> Unit,
    onEpisode: (HomeRow) -> Unit,
    onShortcut: (HomeShortcut) -> Unit,
    onStartRoute: (Route) -> Unit,
    onDismissStart: () -> Unit,
    selectedKey: String? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val start = ui.start
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 24.dp),
    ) {
        item(key = "name") {
            Column(civilightItem()) {
                CivilightText(ui.name, type.displayM, color = colors.ink)
                if (start != null && ui.newReady) {
                    StartSummary(ui, start)
                } else {
                    ui.oneLiner?.let { CivilightText(it, type.leadCompact, Modifier.padding(top = 12.dp), color = colors.ink2) }
                }
            }
        }
        if (!ui.newReady) homeHead(ui, onResume)
        if (start != null) {
            // The block folds away on `목록 닫기` and back in on a new project (P1, fold tokens).
            item(key = "start") { StartBlockView(start, onStartRoute, onDismissStart, civilightItem(CivilightMotion.foldOpen, fadeOut = CivilightMotion.foldClose)) }
        }
        if (ui.rows.isNotEmpty()) {
            item(key = "listHeader") {
                Column(civilightItem().padding(top = 28.dp, bottom = 6.dp)) {
                    ui.listHeader?.let { ChapterHeaderLabel(it) }
                }
            }
            ui.rows.forEach { row -> item(key = "row:${row.key}") { EpisodeLine(row, onEpisode, selected = row.key == selectedKey, modifier = civilightItem()) } }
        }
        item(key = "work") {
            Column(civilightItem().padding(top = 28.dp)) {
                SectionLabel(HomeCopy.WORK)
                ShortcutGrid(ui.shortcuts, onShortcut, Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** `Ⅰ 1장 · 등불지기` in the `sec` section style, the numeral in the serif face. */
@Composable
private fun ChapterHeaderLabel(text: String) {
    ChapterHeaderText(text, CivilightTheme.type.sec, color = CivilightTheme.colors.ink3)
}

/** Stats and `이어 쓰기` (board `Home`); not drawn in the `NewReady` state. */
private fun LazyListScope.homeHead(ui: HomeUi, onResume: (HomeResume) -> Unit) {
    item(key = "stats") {
        val colors = CivilightTheme.colors
        val type = CivilightTheme.type
        Row(civilightItem().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            ui.stats.forEach { stat ->
                Column {
                    ValueSwap(stat.value, type.stat, color = colors.ink)
                    ValueSwap(stat.label, type.small, color = colors.ink3)
                }
            }
        }
    }
    ui.resume?.let { resume ->
        item(key = "resume") { ResumeBlock(resume, onResume, civilightItem()) }
    }
}

@Composable
private fun ResumeBlock(resume: HomeResume, onResume: (HomeResume) -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier = modifier
            .padding(top = 30.dp)
            .fillMaxWidth()
            .topHairline(colors.line)
            .bottomHairline(colors.line)
            .pressable(onClick = { onResume(resume) }, scaleOnPress = false, onClickLabel = HomeCopy.RESUME)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            CivilightText(HomeCopy.RESUME, type.micro, color = colors.acc, maxLines = 1)
            ValueSwap(resume.where, type.heading, Modifier.padding(top = 4.dp), color = colors.ink)
            ValueSwap(resume.quote, type.command, Modifier.padding(top = 2.dp), color = colors.ink2)
        }
        CivilightIcon(IconId.Next, contentDescription = null)
    }
}

@Composable
private fun EpisodeLine(row: HomeRow, onEpisode: (HomeRow) -> Unit, selected: Boolean = false, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val titleStyle = type.itemTitle.merge(TextStyle(fontWeight = if (row.current) FontWeight.Bold else FontWeight.Normal))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .selectedRowBackground(selected, colors.sel, colors.acc)
            .pressable(onClick = { onEpisode(row) }, scaleOnPress = false)
            .heightIn(min = 44.dp)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText(row.code, type.number, color = colors.ink3, maxLines = 1)
        CivilightText(row.title, titleStyle, Modifier.weight(1f, fill = !row.hiddenTag).padding(start = 16.dp, end = 12.dp), color = if (row.current) colors.ink else colors.ink2, maxLines = 1)
        if (row.hiddenTag) {
            CivilightText(StoryCopy.HIDDEN_TAG, type.small, Modifier.weight(1f).padding(end = 12.dp), color = colors.ink3, maxLines = 1)
        }
        ValueSwap(row.status, type.small, color = if (row.writing) colors.acc else colors.ink3)
    }
}

@Composable
private fun ShortcutGrid(shortcuts: List<HomeShortcut>, onShortcut: (HomeShortcut) -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val style = CivilightTheme.type.itemTitle.merge(TextStyle(fontWeight = FontWeight.Normal))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        HomeModel.shortcutRows(shortcuts).forEach { line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                line.forEach { shortcut ->
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .pressable(onClick = { onShortcut(shortcut) }, scaleOnPress = false),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        CivilightText(shortcut.label, style, color = if (shortcut.dim) colors.ink3 else colors.ink, maxLines = 1)
                    }
                }
                repeat(3 - line.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** `[R-15] 연재 장편 · 장 › 에피소드 › 씬 · 1200년` under the name (board `NewReady`). */
@Composable
private fun StartSummary(ui: HomeUi, start: HomeStart) {
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        RatingBadge(ThemeBridge.rating(ui.rating))
        CivilightText(start.summary, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
    }
}

/**
 * `바로 쓰기` + `쓰기`, then `시작해 볼 것 · 1/4 | 목록 닫기` and the four rows (board `NewReady`).
 * Each row opens its place; done rows are struck through behind an `acc` box.
 */
@Composable
private fun StartBlockView(start: HomeStart, onRoute: (Route) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(modifier) {
        start.quickWrite?.let { line ->
            Row(
                Modifier.padding(top = 26.dp).fillMaxWidth().topHairline(colors.line).padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    CivilightText(HomeCopy.QUICK_WRITE, type.micro, color = colors.acc, maxLines = 1)
                    CivilightText(line, type.heading, Modifier.padding(top = 4.dp), color = colors.ink)
                }
                CivilightButton(HomeCopy.WRITE, { onRoute(start.quickWriteRoute) })
            }
        }
        Row(Modifier.padding(top = 26.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CivilightText(start.header, type.sec, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
            TextAction(HomeCopy.DISMISS, onDismiss, color = colors.ink3, style = type.small)
        }
        Column(Modifier.padding(top = 6.dp)) {
            start.items.forEach { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .topHairline(colors.line)
                        .pressable(onClick = { onRoute(item.route) }, scaleOnPress = false, onClickLabel = item.title)
                        .heightIn(min = 44.dp)
                        .padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CheckboxMark(item.done, dense = true)
                    Column(Modifier.weight(1f)) {
                        CivilightText(
                            item.title,
                            type.itemTitle.merge(TextStyle(fontWeight = FontWeight.Normal, textDecoration = if (item.done) TextDecoration.LineThrough else null)),
                            color = if (item.done) colors.ink3 else colors.ink,
                        )
                        if (item.sub.isNotEmpty()) CivilightText(item.sub, type.small, color = colors.ink3, maxLines = 1)
                    }
                    CivilightIcon(IconId.Next, contentDescription = null, tint = colors.ink3)
                }
            }
        }
    }
}
