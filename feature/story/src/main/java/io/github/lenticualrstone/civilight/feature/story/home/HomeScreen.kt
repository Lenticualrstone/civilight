package io.github.lenticualrstone.civilight.feature.story.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import io.github.lenticualrstone.civilight.designsystem.component.ImmersiveDetailScaffold
import io.github.lenticualrstone.civilight.designsystem.component.ImmersiveHeaderButton
import io.github.lenticualrstone.civilight.designsystem.component.ImmersiveMath
import io.github.lenticualrstone.civilight.designsystem.component.ReportBottomChrome
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.bottomHairline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.component.selectedRowBackground
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.ValueSwap
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
import io.github.lenticualrstone.civilight.ui.scaffold.CommandScope
import io.github.lenticualrstone.civilight.ui.scaffold.LocalCommandBarHost
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.CenterColumn

/**
 * 프로젝트 홈 (STO-01) on the immersive detail frame (`Immersive.dc.html` kind `project`): the cover fills the
 * screen (LIB-83), the name is the title that docks into the head, the meta line the subtitle, and the home
 * sections sit on the glass panel. Cover `Home` 475 × 751, inner 704 × 933.
 *
 * Command bar (DS-65): alone in the window it is the immersive bottom bar, so the resting title sits above it;
 * in a `목록 + 상세` pane it goes to the frame's one bar, and the frame is inset by that bar's height.
 */
@Composable
fun HomeScreen(route: ProjectHome, deps: FeatureDeps) {
    val vm = entryViewModel { HomeViewModel(route.projectKey, deps.storyServices()) }
    val ui by vm.ui.collectAsState()
    val settings by deps.settings.collectAsState()
    val sheets = rememberSheetHost()
    val navigator = deps.navigator
    // DS-65: a pane of `목록 + 상세` hands its command bar to the frame host (as `CivilightScreen` decides).
    val inPane = LocalCommandBarHost.current != null && CommandScope.of(LocalPaneRole.current) != null
    val inDetailPane = inPane && LocalPaneRole.current == PaneRole.DETAIL
    val commandBar: @Composable () -> Unit = {
        CommandBar(
            hint = ui?.commandHint.orEmpty(),
            onClick = { navigator.push(ProjectSearch(route.projectKey)) },
            // S-8 + 0.5.0 Labs gate: recorder opens from home only when Life Recorder is enabled in Labs.
            onMic = if (settings.lifeRecorderEnabled) ({ navigator.push(Recording(projectKey = route.projectKey)) }) else null,
        )
    }
    CenterColumn {
        CivilightScreen(
            // The head always names the project (QA U-06), also right after the wizard; empty only while loading.
            title = ui?.name.orEmpty(),
            // The immersive frame draws its own head; the common glass head stays empty.
            topBar = {},
            commandBar = if (inPane) commandBar else null,
            sheets = sheets,
        ) { padding ->
            // Loading -> content swaps on the kind (loaded or not), never on the data, so an edit does
            // not cross-fade the whole screen (motion audit, feature:story 7).
            ContentSwap(ui != null, Modifier.fillMaxSize()) { loaded ->
                val u = ui
                if (!loaded || u == null) {
                    Box(Modifier.fillMaxSize().background(CivilightTheme.colors.bg))
                } else {
                    if (!inPane) ReportBottomChrome(ImmersiveMath.BAR_ROW_DP.dp)
                    ImmersiveDetailScaffold(
                        // Masked as before: home is reachable only while a hidden project is open (STO-01, LIB-89).
                        title = u.name,
                        subtitle = u.meta,
                        // DS-70 A13: back leads to the 서재; the `목록 + 상세` detail pane has no back (the list is beside it).
                        onBack = if (inDetailPane) null else ({ if (!navigator.popTo(Library)) navigator.resetTo(Library) }),
                        backLabel = HomeCopy.BACK_TO_LIBRARY,
                        modifier = if (inPane) Modifier.padding(bottom = padding.calculateBottomPadding()) else Modifier,
                        // In a pane the padding above already holds the frame bar and the navigation inset.
                        bottomInset = if (inPane) 0.dp else null,
                        hero = { HomeHero(u.cover) },
                        actions = {
                            // U-08 LIB-83: the visible way to the cover picker (never on the read-only example).
                            if (u.coverEditable) {
                                ImmersiveHeaderButton(IconId.Pencil, u.coverLabel, {
                                    sheets.open(ImagePickSheet(u.projectKey, ImageOwnerKind.PROJECT, u.projectKey, ImageSlot.PROJECT_COVER))
                                })
                            }
                        },
                        bottomBar = if (inPane) null else commandBar,
                    ) {
                        HomeContent(
                            ui = u,
                            onResume = { navigator.push(Script(u.projectKey, blockKey = it.blockKey, caretAtEnd = true)) },
                            onEpisode = { navigator.openDetail(StoryStructure(u.projectKey, focusUnitKey = it.key)) },
                            onShortcut = { shortcut ->
                                when (val target = shortcut.route(u.projectKey)) {
                                    null -> Unit
                                    is StoryStructure -> navigator.openDetail(target)
                                    else -> navigator.push(target)
                                }
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
}

/** The home sections on the immersive panel, in [HomeModel.slots] order (the panel scrolls; gutters are its). */
@Composable
private fun HomeContent(
    ui: HomeUi,
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
    Column(Modifier.fillMaxWidth()) {
        HomeModel.slots(ui).forEach { slot ->
            key(slot.key) {
                when (slot) {
                    // The name is the immersive title; its item keeps the summary line and the one-liner, which
                    // fold so the item changes height smoothly when `NewReady` ends (`목록 닫기`).
                    HomeSlot.Name -> Column(Modifier.fillMaxWidth()) {
                        FoldRevealValue(start?.takeIf { ui.newReady }, label = "startSummary") { s -> StartSummary(ui, s) }
                        FoldRevealValue(ui.oneLiner?.takeIf { !ui.newReady }, label = "oneLiner") { line ->
                            CivilightText(line, type.leadCompact, color = colors.ink2)
                        }
                    }
                    HomeSlot.Stats -> HomeStats(ui)
                    HomeSlot.Resume -> ui.resume?.let { ResumeBlock(it, onResume) }
                    // The block folds away on `목록 닫기` and back in on a new project (P1, fold tokens).
                    HomeSlot.Start -> FoldRevealValue(start, label = "startBlock") { s ->
                        StartBlockView(s, onStartRoute, onDismissStart)
                    }
                    HomeSlot.ListHeader -> Column(Modifier.padding(top = 28.dp, bottom = 6.dp)) {
                        ui.listHeader?.let { ChapterHeaderLabel(it) }
                    }
                    is HomeSlot.Episode -> EpisodeLine(slot.row, onEpisode, selected = slot.row.key == selectedKey)
                    HomeSlot.Work -> Column(Modifier.padding(top = 28.dp)) {
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
            val icon = when (shortcut.label) {
                "인물" -> IconId.User
                "세계" -> IconId.Book
                "지도" -> IconId.Map
                "캔버스" -> IconId.Layers
                "갤러리" -> IconId.Image
                "페이지" -> IconId.File
                "연표" -> IconId.Clock
                "점검" -> IconId.Check
                "설정" -> IconId.Settings
                else -> IconId.Folder
            }
            ListRow(
                title = shortcut.label,
                leading = { CivilightIcon(icon, contentDescription = null, size = 18.dp, tint = colors.ink2) },
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
    CivilightText(start.summary, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
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
