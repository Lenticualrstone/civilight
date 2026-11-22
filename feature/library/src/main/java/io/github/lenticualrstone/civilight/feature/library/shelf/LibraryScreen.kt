package io.github.lenticualrstone.civilight.feature.library.shelf

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CommandBar
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.component.ViewToggle
import io.github.lenticualrstone.civilight.feature.library.export.ExportCopy
import io.github.lenticualrstone.civilight.feature.library.export.MultiExportContent
import io.github.lenticualrstone.civilight.feature.library.export.MultiExportSheet
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.library.newproject.ProjectDeleteDialog
import io.github.lenticualrstone.civilight.domain.library.newproject.ProjectMenuAction
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.tour.ModuleTourHost
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.feature.library.common.sheetBody
import io.github.lenticualrstone.civilight.feature.library.newproject.startNewProject
import io.github.lenticualrstone.civilight.feature.library.rating.RatingPickContent
import io.github.lenticualrstone.civilight.feature.library.rating.RatingPickSheet
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.ExportSheet
import io.github.lenticualrstone.civilight.navigation.LibrarySearch
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.ProjectRoute
import io.github.lenticualrstone.civilight.navigation.Settings
import io.github.lenticualrstone.civilight.navigation.RecorderInbox
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.RestoreBackup
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import io.github.lenticualrstone.civilight.ui.nav.openDetail
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.WindowProtection
import io.github.lenticualrstone.civilight.ui.window.CenterColumn

/**
 * 서재 (LIB-01; boards `Library`, `LibUnlocked`, `OnbEmpty`): header `Civilight` / `서재` with `+`
 * (or `다시 잠그기` while the hidden group is open), `프로젝트 · N`, project rows, the hidden group,
 * `라이브러리 · 프로젝트 밖` with the 라이프 레코더 row, and the `모든 프로젝트에서 찾기` command bar.
 */
@Composable
internal fun LibraryScreen(services: LibraryServices) {
    val deps = services.deps
    services.attach(LocalContext.current)
    LaunchedEffect(Unit) { services.backups.load() }
    val vm = entryViewModel { LibraryViewModel(services) }
    val ui by vm.state.collectAsState()
    val docs by vm.documents.collectAsState()
    val sheets = rememberSheetHost()
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    val navigator = deps.navigator
    val entries by deps.stores.library.state.collectAsState()
    fun isExample(key: Key) = entries.entry(key)?.isExample == true

    // A hidden project's sheet or delete dialog never outlives the open group (re-lock, process death).
    LaunchedEffect(ui, sheets.sheets) {
        repeat(ShelfState.sheetsToClose(ui, sheets.sheets)) { sheets.close() }
        if (ui.loaded && deleting?.let(ui::shows) == false) deleting = null
    }

    fun openProject(key: Key) = navigator.openDetail(ProjectHome(key))
    // `MUnlock` reversed (`다시 잠글 때 반대로`): armed only by the header `다시 잠그기` tap, with the group
    // as it was; drawn only once the group is really locked, and for [UNBLUR_MS] at most.
    val reduced = CivilightTheme.reducedMotion
    var relocking by remember { mutableStateOf<OpenGroupUi?>(null) }
    LaunchedEffect(relocking) {
        if (relocking != null) {
            delay(UNBLUR_MS.toLong())
            relocking = null
        }
    }
    val closingGroup = relocking?.takeIf { ui.openGroup == null && !reduced }
    fun relockFromHeader() {
        if (!reduced) relocking = ui.openGroup
        vm.relock()
    }

    // LIB-06 `목록 + 상세`: the row whose project fills the right pane is marked.
    val selectedKey: Key? = if (LocalPaneRole.current == PaneRole.LIST) {
        navigator.backStack.filterIsInstance<ProjectRoute>().lastOrNull()?.projectKey
    } else {
        null
    }

    /** `열기 · 마지막 위치`: 프로젝트 홈, then the script at the last edited block when there is one (`ED`). */
    fun openAtLastPosition(key: Key, doc: ProjectDoc) {
        openProject(key)
        val block = doc.app.activity.activity.lastEditedBlockKey?.let(doc.index::block)
        if (block != null && doc.liveness.isLive(block)) navigator.push(Script(key, blockKey = block.key, caretAtEnd = true))
    }

    fun onMenuAction(key: Key, doc: ProjectDoc, action: ProjectMenuAction, close: () -> Unit, openSheet: (SheetKey) -> Unit) {
        when (action) {
            ProjectMenuAction.OPEN -> {
                close()
                openAtLastPosition(key, doc)
            }
            ProjectMenuAction.RENAME -> {
                close()
                openSheet(RenameSheet(key))
            }
            ProjectMenuAction.RATING -> {
                close()
                openSheet(RatingPickSheet(key))
            }
            ProjectMenuAction.HIDE -> {
                close()
                vm.hide(key)
            }
            ProjectMenuAction.UNHIDE -> {
                close()
                vm.unhide(key)
            }
            ProjectMenuAction.BACKUP_EXPORT -> {
                close()
                openSheet(ExportSheet(key))
            }
            ProjectMenuAction.DELETE -> {
                close()
                deleting = key
            }
        }
    }

    // LIB-19 `서재 · 숨기기` tour: dormant until its copy and anchors exist (§6 Q15).
    ModuleTourHost(TourModule.LIBRARY_HIDING, deps) {
    CenterColumn {
        CivilightScreen(
            title = ShelfCopy.TITLE,
            smallLine = ShelfCopy.SMALL,
            actions = {
                // LIB-04: `목록 | 카드` left of `+`, in both views (board `LibCards`).
                ViewToggle(showCards = ui.showCards, onChange = vm::showCards)
                // `ED`: no board draws the way into 설정 (LIB-63); a labelled icon button.
                CivilightIconButton(IconId.Props, ShelfCopy.SETTINGS, { navigator.push(Settings) })
                // LIB-72 `ED` (§6 Q23, undrawn): 여러 프로젝트 내보내기 from the 서재 header.
                if (ui.exportable.isNotEmpty()) CivilightIconButton(IconId.Download, ExportCopy.HEADER, { sheets.open(MultiExportSheet) })
                if (ui.headerRelock) {
                    CivilightButton(ShelfCopy.RELOCK, ::relockFromHeader, variant = ButtonVariant.COMPACT_GHOST, icon = IconId.Lock)
                } else {
                    CivilightIconButton(IconId.Plus, ShelfCopy.NEW_PROJECT, { services.startNewProject() })
                }
            },
            commandBar = {
                CommandBar(
                    hint = ShelfCopy.SEARCH_HINT,
                    onClick = { navigator.push(LibrarySearch()) },
                    onMic = { navigator.push(Recording(surface = "commandBar")) },
                    micLabel = ShelfCopy.RECORDER,
                )
            },
            sheets = sheets,
            localSheets = { key ->
                when (key) {
                    is ShelfMenuSheet -> sheetBody {
                        docs[key.projectKey]?.takeIf { ui.shows(key.projectKey) }?.let { doc ->
                            ProjectMenuContent(doc, isExample(key.projectKey)) { action ->
                                onMenuAction(key.projectKey, doc, action, { close() }, { open(it) })
                            }
                        }
                    }
                    is RenameSheet -> sheetBody {
                        docs[key.projectKey]?.takeIf { ui.shows(key.projectKey) }?.let { doc -> RenameContent(doc.project.name) { vm.rename(key.projectKey, it) } }
                    }
                    MultiExportSheet -> sheetBody {
                        MultiExportContent(ui.exportable.mapNotNull { docs[it] }, services)
                    }
                    is RatingPickSheet -> sheetBody {
                        docs[key.projectKey]?.takeIf { ui.shows(key.projectKey) }?.let { doc -> RatingPickContent(key.projectKey, RatingLabels.effective(doc.project.rating)) }
                    }
                    else -> null
                }
            },
        ) { padding ->
          BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = ShelfCards.isWide(maxWidth.value.toInt())
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = SIDE,
                    end = SIDE,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
            ) {
                if (ui.loaded && ui.showCards && ui.empty == null) {
                    item(key = "cards") {
                        ProjectCardsBlock(
                            ui = ui,
                            wide = wide,
                            side = SIDE,
                            onOpen = ::openProject,
                            onMenu = { sheets.open(ShelfMenuSheet(it)) },
                            onUnlock = vm::unlockHiddenGroup,
                            onNewProject = { services.startNewProject() },
                        )
                    }
                    item(key = "recents") {
                        RecentsBlock(ui.recents, wide, SIDE) { item ->
                            openProject(item.projectKey)
                            navigator.push(RecentsModel.routeOf(item))
                        }
                    }
                } else if (ui.loaded) {
                    item(key = "section") { SectionLabel(ui.sectionLabel) }
                    items(ui.rows, key = { it.key }) { row ->
                        ProjectRow(row, onOpen = { openProject(row.key) }, onMenu = { sheets.open(ShelfMenuSheet(row.key)) }, selected = row.key == selectedKey)
                    }
                    ui.empty?.let { empty ->
                        item(key = "empty") {
                            Column {
                                EmptyBlock(
                                    title = empty.title,
                                    subtitle = empty.sub,
                                    primaryLabel = empty.buttons[0],
                                    onPrimary = { services.startNewProject() },
                                    secondaryLabel = empty.buttons[1],
                                    onSecondary = { navigator.push(RestoreBackup) },
                                )
                                empty.notice?.let {
                                    // `OnbEmpty`: a small trash mark before `예시 프로젝트 삭제됨` (LIB-18).
                                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CivilightIcon(IconId.Trash, contentDescription = null, size = 14.dp, tint = CivilightTheme.colors.ink3)
                                        CivilightText(it, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
                                    }
                                }
                            }
                        }
                    }
                    // `MUnlock` reversed: right after a re-lock the rows blur out where the group was, then the lock row.
                    closingGroup?.let { group ->
                        item(key = "closing") { OpenGroup(group, onOpen = {}, onMenu = {}, onUnhide = {}, selectedKey = null, closing = true) }
                    }
                    ui.lockedGroup?.takeIf { closingGroup == null }?.let { group ->
                        item(key = "locked") {
                            LockRow(
                                title = group.title,
                                subtitle = group.sub,
                                actionLabel = group.button,
                                onAction = vm::unlockHiddenGroup,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                    ui.openGroup?.let { group ->
                        item(key = "open") {
                            OpenGroup(group, onOpen = ::openProject, onMenu = { sheets.open(ShelfMenuSheet(it)) }, onUnhide = vm::unhide, selectedKey = selectedKey)
                        }
                    }
                }
                // Both views end with the library section (LIB-01, board `LibCards`).
                if (ui.loaded) {
                    item(key = "librarySection") { SectionLabel(ShelfCopy.LIBRARY_SECTION, Modifier.padding(top = 30.dp)) }
                    item(key = "recorder") {
                        ListRow(
                            title = ShelfCopy.RECORDER,
                            subtitle = ui.recorderSub,
                            leading = { RecorderTile() },
                            trailing = { CivilightIcon(IconId.Next, contentDescription = null, tint = CivilightTheme.colors.ink3) },
                            onClick = { navigator.push(RecorderInbox()) },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
          }
        }
    }
    }

    // Drawn once the project's document is read (after a restart it loads again); cleared above
    // when the project leaves the visible rows.
    deleting?.let { key ->
        val doc = docs[key]?.takeIf { ui.shows(key) } ?: return@let
        DeleteProjectDialog(
            dialog = ProjectDeleteDialog.of(doc),
            protections = doc.project.protections,
            onConfirm = {
                deleting = null
                vm.moveToTrash(key)
            },
            onDismiss = { deleting = null },
        )
    }
}

/** The mic tile of the 라이프 레코더 row: 44 × 44, radius 14, `pri` fill. */
@Composable
private fun RecorderTile() {
    val colors = CivilightTheme.colors
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.pri), contentAlignment = Alignment.Center) {
        CivilightIcon(IconId.Mic, contentDescription = null, tint = colors.onpri)
    }
}

/**
 * The open hidden group (`LibUnlocked`): tinted box, unlock icon, `숨긴 프로젝트 <M> · 열림` with the
 * re-lock note, then the hidden projects with `숨김 풀기`. Each row protects the window while shown
 * (LIB-26, S-10; LIB-29 recents preview).
 */
@Composable
private fun OpenGroup(
    group: OpenGroupUi,
    onOpen: (Key) -> Unit,
    onMenu: (Key) -> Unit,
    onUnhide: (Key) -> Unit,
    selectedKey: Key?,
    closing: Boolean = false,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(16.dp)
    // `MUnlock`: the opened rows come out of a 7 px blur in 320 ms (none with reduced motion);
    // [closing] plays it backwards over the whole group (`다시 잠글 때 반대로`), inert and silent.
    var clear by remember { mutableStateOf(closing) }
    LaunchedEffect(Unit) { clear = !closing }
    val still = clear || (CivilightTheme.reducedMotion && !closing)
    val blur by animateDpAsState(if (still) 0.dp else 7.dp, tween(UNBLUR_MS), label = "unblur")
    val fade by animateFloatAsState(if (still) 1f else if (closing) 0f else 0.55f, tween(UNBLUR_MS), label = "unblurFade")
    val rowsLook = if (closing) Modifier else Modifier.blur(blur).alpha(fade)
    Column(
        Modifier
            .then(if (closing) Modifier.blur(blur).alpha(fade).clearAndSetSemantics { } else Modifier)
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.sel)
            .insetOutline(colors.line, 1.dp, shape)
            .padding(start = 16.dp, end = 16.dp, top = 2.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CivilightIcon(IconId.Unlock, contentDescription = null, tint = colors.acc)
            CivilightText(group.header, type.label.merge(TextStyle(fontWeight = FontWeight.SemiBold)), Modifier.weight(1f), color = colors.ink, maxLines = 1)
            CivilightText(group.note, type.small, color = colors.ink3, maxLines = 1)
        }
        Column(rowsLook) {
            group.rows.forEach { row ->
                OpenGroupRow(row, onOpen, onMenu, onUnhide, row.key == selectedKey)
            }
        }
    }
}

private const val UNBLUR_MS = 320


/** The 서재's side margin (boards: 24). */
private val SIDE = 24.dp

@Composable
private fun OpenGroupRow(row: ProjectRowUi, onOpen: (Key) -> Unit, onMenu: (Key) -> Unit, onUnhide: (Key) -> Unit, selected: Boolean) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    ProtectWindow(WindowProtection.forProject(row.protections))
    ProjectRow(
        row = row,
        onOpen = { onOpen(row.key) },
        onMenu = { onMenu(row.key) },
        selected = selected,
        extra = { TextAction(ShelfCopy.UNHIDE, { onUnhide(row.key) }, color = colors.ink3, style = type.small) },
    )
}
