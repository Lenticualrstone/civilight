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
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.bottomHairline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonCopy
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.ValueSwap
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.feature.story.common.ChapterHeaderText
import io.github.lenticualrstone.civilight.feature.story.common.StoryCopy
import io.github.lenticualrstone.civilight.feature.story.common.storyServices
import io.github.lenticualrstone.civilight.navigation.ImagePickSheet
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.navigation.Library
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
            // The head always names the project (QA U-06), also right after the wizard; empty only while loading.
            title = ui?.name.orEmpty(),
            // DS-70 A13: icon only, read `서재로`. No rating badge (S-4) and no lens chip (S-1): home is always 최신.
            back = if (inDetailPane) null else IconAction(IconButtonCopy.TO_LIBRARY) { if (!navigator.popTo(Library)) navigator.resetTo(Library) },
            commandBar = {
                CommandBar(
                    hint = ui?.commandHint.orEmpty(),
                    onClick = { navigator.push(ProjectSearch(route.projectKey)) },
                    // S-8: inside the app the recorder opens only from here.
                    onMic = { navigator.push(Recording(projectKey = route.projectKey)) },
                )
            },
            sheets = sheets,
            localSheets = { key ->
                when (key) {
                    is HomeMarksSheet -> { { HomeMarksContent(key, deps) } }
                    else -> null
                }
            },
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
                            when (val target = shortcut.route(u.projectKey)) {
                                null -> sheets.open(HomeMarksSheet(u.projectKey))
                                is StoryStructure -> navigator.openDetail(target)
                                else -> navigator.push(target)
                            }
                        },
                        onStartRoute = { navigator.push(it) },
                        onDismissStart = { vm.dismissStart() },
                        selectedKey = HomeModel.selectedKey(navigator.backStack, route.projectKey),
                        // U-08 LIB-83: the head cover opens the shared cover picker over home.
                        onCover = { sheets.open(ImagePickSheet(u.projectKey, ImageOwnerKind.PROJECT, u.projectKey, ImageSlot.PROJECT_COVER)) },
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
    onCover: () -> Unit = {},
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val start = ui.start
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 24.dp),
    ) {
        HomeModel.slots(ui).forEach { slot ->
            item(key = slot.key) {
                when (slot) {
                    // U-08 LIB-83: the cover (3:4, 84 × 112) left of the name, 16 apart.
                    HomeSlot.Name -> Row(civilightItem(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        HomeCoverImage(ui, onCover)
                        Column(Modifier.weight(1f)) {
                            CivilightText(ui.name, type.displayM, color = colors.ink)
                            // The summary line and the one-liner fold, so the name item changes height smoothly
                            // when `NewReady` ends (`목록 닫기`).
                            FoldRevealValue(start?.takeIf { ui.newReady }, label = "startSummary") { s -> StartSummary(ui, s) }
                            FoldRevealValue(ui.oneLiner?.takeIf { !ui.newReady }, label = "oneLiner") { line ->
                                CivilightText(line, type.leadCompact, Modifier.padding(top = 12.dp), color = colors.ink2)
                            }
                        }
                    }
                    HomeSlot.Stats -> HomeStats(ui, civilightItem())
                    HomeSlot.Resume -> ui.resume?.let { ResumeBlock(it, onResume, civilightItem()) }
                    // The block folds away on `목록 닫기` and back in on a new project (P1, fold tokens). The
                    // item itself stays, so its height shrinks with the fold and the rows below follow it.
                    HomeSlot.Start -> FoldRevealValue(start, civilightItem(fadeIn = null, fadeOut = null), label = "startBlock") { s ->
                        StartBlockView(s, onStartRoute, onDismissStart)
                    }
                    HomeSlot.ListHeader -> Column(civilightItem().padding(top = 28.dp, bottom = 6.dp)) {
                        ui.listHeader?.let { ChapterHeaderLabel(it) }
                    }
                    is HomeSlot.Episode -> EpisodeLine(slot.row, onEpisode, selected = slot.row.key == selectedKey, modifier = civilightItem())
                    HomeSlot.Work -> Column(civilightItem().padding(top = 28.dp)) {
                        SectionLabel(HomeCopy.WORK)
                        ShortcutList(ui.shortcuts, onShortcut, Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

/** `Ⅰ 1장 · 등불지기` in the `sec` section style, the numeral in the serif face. */
@Composable
private fun ChapterHeaderLabel(text: String) {
    ChapterHeaderText(text, CivilightTheme.type.sec, color = CivilightTheme.colors.ink3)
}

/** The stats row of board `Home`; on `NewReady` only a 1-level project's `k/n 씬` ([HomeModel.shownStats]). */
@Composable
private fun HomeStats(ui: HomeUi, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        HomeModel.shownStats(ui).forEach { stat ->
            Column {
                ValueSwap(stat.value, type.stat, color = colors.ink)
                ValueSwap(stat.label, type.small, color = colors.ink3)
            }
        }
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

/**
 * The `작업` shortcuts as DS list rows (QA U-07: the old text grid read as plain text): one row each, with
 * the hairline, a pressed state and the trailing `›` the `시작해 볼 것` rows use. `설정` stays the last row.
 */
@Composable
private fun ShortcutList(shortcuts: List<HomeShortcut>, onShortcut: (HomeShortcut) -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    Column(modifier) {
        shortcuts.forEach { shortcut ->
            ListRow(
                title = shortcut.label,
                onClick = { onShortcut(shortcut) },
                compact = true,
                trailing = { CivilightIcon(IconId.Next, contentDescription = null, tint = colors.ink3) },
            )
        }
    }
}

/** `연재 장편 · 장 › 에피소드 › 씬 · 1200년` under the name (board `NewReady`); no rating badge (S-4). */
@Composable
private fun StartSummary(ui: HomeUi, start: HomeStart) {
    CivilightText(start.summary, CivilightTheme.type.small, Modifier.padding(top = 8.dp), color = CivilightTheme.colors.ink3)
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
