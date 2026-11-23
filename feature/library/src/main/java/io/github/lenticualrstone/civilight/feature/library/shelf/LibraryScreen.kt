@file:OptIn(ExperimentalSharedTransitionApi::class)

package io.github.lenticualrstone.civilight.feature.library.shelf

import io.github.lenticualrstone.civilight.ui.scaffold.LocalSheetRegistry
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.navigation.ImagePickSheet
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.key
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.card.CardMetrics
import io.github.lenticualrstone.civilight.designsystem.card.CardViewMotion
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
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.Unhide
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.designsystem.theme.rememberLastNonNull
import io.github.lenticualrstone.civilight.designsystem.theme.rememberUnhide
import io.github.lenticualrstone.civilight.designsystem.theme.unhide
import io.github.lenticualrstone.civilight.domain.library.newproject.ProjectDeleteDialog
import io.github.lenticualrstone.civilight.domain.library.newproject.ProjectMenuAction
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.tour.ModuleTourHost
import io.github.lenticualrstone.civilight.feature.library.tour.tourTarget
import io.github.lenticualrstone.civilight.ui.tour.TourHold
import io.github.lenticualrstone.civilight.ui.tour.TourStepAbsent
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.feature.library.common.sheetBody
import io.github.lenticualrstone.civilight.feature.library.newproject.startNewProject
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.ExportSheet
import io.github.lenticualrstone.civilight.navigation.LibrarySearch
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.navigation.ProjectRoute
import io.github.lenticualrstone.civilight.navigation.Settings
import io.github.lenticualrstone.civilight.navigation.RecorderInbox
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
    val motion = CivilightTheme.motion
    services.attach(LocalContext.current)
    LaunchedEffect(Unit) { services.backups.load() }
    val vm = entryViewModel { LibraryViewModel(services) }
    val ui by vm.state.collectAsState()
    val view by vm.view.collectAsState()
    val docs by vm.documents.collectAsState()
    val sheets = rememberSheetHost()
    val sheetRegistry = LocalSheetRegistry.current
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
    // CNV-43 unhide: the open group comes out of the blur when the lock opens and goes back into it when
    // the group locks again (header tap, leaving, screen off, 5 minutes). A group already open when the
    // screen first composes (process restore) is clear at once. The lock state drives it, not the rows:
    // `숨김 풀기` of the last hidden project empties the group while the lock stays open.
    val unlock by deps.unlock.state.collectAsState()
    val unhide = rememberUnhide(unlock.hiddenGroupOpen)
    var groupSnapshot by remember { mutableStateOf<OpenGroupUi?>(null) }
    val reversing by remember(unhide) { derivedStateOf { unhide.value > 0f } }
    // A lock that lands while the app is stopped (leaving, screen off) plays nothing: Compose runs no frames
    // then, so the first frame after the return would show the hidden names clear and only then blur them.
    // The reverse is for a lock that lands while the screen is on (header tap, 5 minutes); any other is a cut.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val groupOpenNow by rememberUpdatedState(unlock.hiddenGroupOpen)
    var stoppedWhileOpen by remember { mutableStateOf(false) }
    var cutEpoch by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP && groupOpenNow) stoppedWhileOpen = true }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // Back on screen with the group still open (no lock came with the return): later locks animate again.
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            withFrameNanos { }
            withFrameNanos { }
            if (groupOpenNow) stoppedWhileOpen = false
        }
    }
    val cut = stoppedWhileOpen && !unlock.hiddenGroupOpen
    LaunchedEffect(ui.openGroup, unlock.hiddenGroupOpen, reversing, cut) {
        val group = ui.openGroup
        when {
            group != null -> groupSnapshot = group
            // Opened-and-emptied, a cut, or the reverse finished: the hidden titles are not kept in state.
            unlock.hiddenGroupOpen || cut || !reversing -> groupSnapshot = null
        }
        if (cut) {
            stoppedWhileOpen = false
            cutEpoch++
        }
    }
    // The kept group plays the reverse in place, inert; once clear the lock row takes over.
    val closingGroup = groupSnapshot.takeIf { ui.openGroup == null && !unlock.hiddenGroupOpen && reversing && !cut }
    val lastLock = rememberLastNonNull(ui.lockedGroup)
    val groupKind = when {
        ui.openGroup != null || closingGroup != null -> GroupKind.OPEN
        ui.lockedGroup != null -> GroupKind.LOCKED
        else -> GroupKind.NONE
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
            // (0.2.0, LIB-92) The ⋯ sheet no longer offers `수위 등급`; nothing to open.
            ProjectMenuAction.RATING -> close()
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
            // U-08 LIB-81: the shared cover picker over the 서재 (`ImagePickSheet` below).
            ProjectMenuAction.COVER -> {
                close()
                // Each opening gets a fresh session (a store the library closed meanwhile is never reused).
                vm.coverPickers.release(key)
                openSheet(ImagePickSheet(key, ImageOwnerKind.PROJECT, key, ImageSlot.PROJECT_COVER))
            }
        }
    }

    // LIB-19 `서재 · 숨기기` tour (copy 2026-11-23): each step spotlights its control (`LibraryTourSteps`),
    // or the 설정 button while that control is not on screen (no hidden projects). With no project shown,
    // `프로젝트 숨기기` is left out (`LibraryTourSteps.absent`), not pointed at 설정.
    val tourMenuRow = LibraryTourSteps.menuRow(ui.rows.map { it.key }, ui.openGroup?.rows.orEmpty().map { it.key })
    ModuleTourHost(TourModule.LIBRARY_HIDING, deps) {
    // Until the stores answered the rows are unknown: the tour waits instead of starting on the fallback.
    if (!ui.loaded) TourHold() else LibraryTourSteps.absent(tourMenuRow).forEach { step -> key(step) { TourStepAbsent(step) } }
    CenterColumn {
        CivilightScreen(
            title = ShelfCopy.TITLE,
            smallLine = ShelfCopy.SMALL,
            actions = {
                // (0.2.0, LIB-93) `목록` · <chosen 3D view> left of `+`; not drawn while the style is `목록` or has no
                // renderer yet (ShelfViews), so the toggle never switches between two identical lists.
                view.toggle?.let { toggle ->
                    ViewToggle(showCards = toggle.styleOn, onChange = vm::showStyle, listLabel = toggle.listLabel, cardLabel = toggle.styleLabel)
                }
                // `ED`: no board draws the way into 설정 (LIB-63); a labelled icon button.
                CivilightIconButton(
                    IconId.Props,
                    ShelfCopy.SETTINGS,
                    { navigator.push(Settings) },
                    LibraryTourSteps.SETTINGS_FALLBACK.fold(Modifier.tourTarget(LibraryTourSteps.SETTINGS)) { m, step -> m.tourTarget(step, fallback = true) },
                )
                // LIB-72 `ED` (§6 Q23, undrawn): 여러 프로젝트 내보내기 from the 서재 header; folds in from the start.
                FoldReveal(ui.exportable.isNotEmpty(), horizontal = true) {
                    CivilightIconButton(IconId.Download, ExportCopy.HEADER, { sheets.open(MultiExportSheet) })
                }
                // `+` ↔ `다시 잠그기` swap in place (board `LibUnlocked`, §6 Q30).
                ContentSwap(ui.headerRelock, label = "headerAction") { relock ->
                    if (relock) {
                        CivilightButton(ShelfCopy.RELOCK, vm::relock, variant = ButtonVariant.COMPACT_GHOST, icon = IconId.Lock)
                    } else {
                        CivilightIconButton(IconId.Plus, ShelfCopy.NEW_PROJECT, { services.startNewProject() })
                    }
                }
            },
            commandBar = {
                CommandBar(
                    hint = ShelfCopy.SEARCH_HINT,
                    onClick = { navigator.push(LibrarySearch()) },
                )
            },
            sheets = sheets,
            localSheets = { key ->
                when (key) {
                    is ShelfMenuSheet -> sheetBody {
                        docs[key.projectKey]?.takeIf { ui.shows(key.projectKey) }?.let { doc ->
                            ProjectMenuContent(doc, isExample(key.projectKey), deps.stores.blobs::contains) { action ->
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
                    // U-08 LIB-81 · LIB-89: the cover picker (drawn by canvas) only while the 서재 may show the
                    // project, also before the stores answered after a restore, and with its view model in a
                    // per-project store the 서재 lets go of once the project leaves it.
                    is ImagePickSheet -> sheetBody {
                        if (ui.shows(key.projectKey)) {
                            // Project content over the 서재: the window is protected as for its ⋯ sheet (LIB-26, LIB-89).
                            docs[key.projectKey]?.let { ProtectWindow(WindowProtection.forProject(it.project.protections)) }
                            sheetRegistry.contentFor(key)?.let { content ->
                                CompositionLocalProvider(LocalViewModelStoreOwner provides vm.coverPickers.ownerFor(key.projectKey)) {
                                    content(this, key)
                                }
                            }
                        }
                    }
                    else -> null
                }
            },
        ) { padding ->
          BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = ShelfCards.isWide(maxWidth.value.toInt())
            val inset = CardMetrics.inset(wide).dp
            // (0.2.0, LIB-93) The chosen 3D view when it has a renderer and there are projects; else the list.
            val renderer = view.rendered?.let(ShelfViewRenderers::of)
            val cardsOn = renderer != null && ShelfViews.drawsView(view, ui)
            // Loading → content (audit row 7): nothing is drawn until the stores answered, then it fades in.
            ContentSwap(ui.loaded, Modifier.fillMaxSize(), label = "shelfLoaded") { loaded ->
              if (loaded) {
                // CNV-45 `목록 ↔ 카드`: thumbnails grow in place into the cards (one shared cover each); list
                // text leaves first, card text comes in after; reduced motion crossfades.
                // `key(cut, …)`: a cut starts the list afresh, so no row or card runs a fade for a lock that already happened.
                key(cut, cutEpoch) {
                SharedTransitionLayout {
                    val transition = this
                    AnimatedContent(cardsOn, transitionSpec = { CardViewMotion.transform(motion) }, label = "shelfView") { showCards ->
                        val covers = SharedCovers.of(transition, this, motion)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = padding.calculateTopPadding() + 8.dp,
                                bottom = padding.calculateBottomPadding() + 16.dp,
                            ),
                        ) {
                            if (showCards) {
                                item(key = "cards") {
                                    // The extension point (ShelfViewRenderers): wave 3 draws each 3D view from the masked ShelfUi.
                                    renderer?.invoke(
                                        ShelfViewInput(
                                            ui = ui,
                                            wide = wide,
                                            onOpen = ::openProject,
                                            onMenu = { sheets.open(ShelfMenuSheet(it)) },
                                            onUnlock = vm::unlockHiddenGroup,
                                            onNewProject = { services.startNewProject() },
                                        ),
                                    )
                                }
                                item(key = "recents") {
                                    RecentsBlock(ui.recents, wide, inset) { item ->
                                        openProject(item.projectKey)
                                        navigator.push(RecentsModel.routeOf(item))
                                    }
                                }
                            } else {
                                item(key = "section") { SectionLabel(ui.sectionLabel, civilightItem().padding(horizontal = SIDE)) }
                                items(ui.rows, key = { it.key }) { row ->
                                    ProjectRow(
                                        row,
                                        onOpen = { openProject(row.key) },
                                        onMenu = { sheets.open(ShelfMenuSheet(row.key)) },
                                        modifier = civilightItem().padding(horizontal = SIDE),
                                        selected = row.key == selectedKey,
                                        thumbModifier = covers.cover(row.key.toString(), CardViewMotion.THUMB_RADIUS_DP),
                                        menuModifier = if (row.key == tourMenuRow) Modifier.tourTarget(LibraryTourSteps.HIDE) else Modifier,
                                    )
                                }
                                ui.empty?.let { empty ->
                                    item(key = "empty") {
                                        Column(civilightItem().padding(horizontal = SIDE)) {
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
                                // The hidden group: lock row ↔ open group crossfade; a re-lock plays the unhide in reverse
                                // over the kept rows first, then the lock row takes over.
                                if (groupKind != GroupKind.NONE) {
                                    item(key = "group") {
                                        ContentSwap(groupKind, Modifier.padding(horizontal = SIDE), label = "hiddenGroup") { kind ->
                                            when (kind) {
                                                GroupKind.LOCKED -> lastLock?.let { group ->
                                                    // DS-70 B21: `지문으로 보기` is icon `fingerprint` + text.
                                                    LockRow(
                                                        title = group.title,
                                                        subtitle = group.sub,
                                                        actionLabel = group.button,
                                                        onAction = vm::unlockHiddenGroup,
                                                        modifier = Modifier.padding(top = 12.dp).tourTarget(LibraryTourSteps.HIDDEN_GROUP),
                                                        actionIcon = IconId.Fingerprint,
                                                    )
                                                }
                                                GroupKind.OPEN -> (ui.openGroup ?: groupSnapshot)?.let { group ->
                                                    val closing = ui.openGroup == null
                                                    OpenGroup(
                                                        group = group,
                                                        unhide = unhide,
                                                        covers = if (closing) SharedCovers.None else covers,
                                                        onOpen = if (closing) ({ _ -> }) else ::openProject,
                                                        onMenu = if (closing) ({ _ -> }) else ({ sheets.open(ShelfMenuSheet(it)) }),
                                                        onUnhide = if (closing) ({ _ -> }) else vm::unhide,
                                                        selectedKey = selectedKey.takeUnless { closing },
                                                        closing = closing,
                                                        tourMenuRow = tourMenuRow.takeUnless { closing },
                                                    )
                                                }
                                                GroupKind.NONE -> Unit
                                            }
                                        }
                                    }
                                }
                            }
                            // Both views end with the library section (LIB-01, board `LibCards`).
                            item(key = "librarySection") { SectionLabel(ShelfCopy.LIBRARY_SECTION, civilightItem().padding(top = 30.dp, start = SIDE, end = SIDE)) }
                            item(key = "recorder") {
                                ListRow(
                                    title = ShelfCopy.RECORDER,
                                    subtitle = ui.recorderSub,
                                    leading = { RecorderTile() },
                                    trailing = { CivilightIcon(IconId.Next, contentDescription = null, tint = CivilightTheme.colors.ink3) },
                                    onClick = { navigator.push(RecorderInbox()) },
                                    modifier = civilightItem().padding(top = 8.dp, start = SIDE, end = SIDE),
                                )
                            }
                        }
                    }
                }
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

/** What the hidden group's slot shows: the lock row, the open group (also while it re-locks), or nothing. */
private enum class GroupKind { NONE, LOCKED, OPEN }

/**
 * The open hidden group (`LibUnlocked`): tinted box, unlock icon, `숨긴 프로젝트 <M> · 열림` with the
 * re-lock note, then the hidden projects with `숨김 풀기`. Each row protects the window while shown
 * (LIB-26, S-10; LIB-29 recents preview). The rows draw the [unhide] progress (CNV-43: blur 7 dp and
 * alpha .55 → 1 while the lock opens, the reverse when it locks again); [closing] is that reverse over the
 * kept group, inert and silent to accessibility.
 */
@Composable
private fun OpenGroup(
    group: OpenGroupUi,
    unhide: Unhide,
    covers: SharedCovers,
    onOpen: (Key) -> Unit,
    onMenu: (Key) -> Unit,
    onUnhide: (Key) -> Unit,
    selectedKey: Key?,
    closing: Boolean = false,
    tourMenuRow: Key? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .then(if (closing) Modifier.clearAndSetSemantics { } else Modifier)
            .padding(top = 12.dp)
            // LIB-19 `숨긴 프로젝트`: the open group is the tour's hole (not while it plays the re-lock).
            .then(if (closing) Modifier else Modifier.tourTarget(LibraryTourSteps.HIDDEN_GROUP))
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
        Column(Modifier.unhide(unhide)) {
            group.rows.forEach { row ->
                OpenGroupRow(row, covers, onOpen, onMenu, onUnhide, row.key == selectedKey, row.key == tourMenuRow)
            }
        }
    }
}

/** The 서재's side margin (boards: 24). */
private val SIDE = 24.dp

@Composable
private fun OpenGroupRow(
    row: ProjectRowUi,
    covers: SharedCovers,
    onOpen: (Key) -> Unit,
    onMenu: (Key) -> Unit,
    onUnhide: (Key) -> Unit,
    selected: Boolean,
    tourMenu: Boolean = false,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    ProtectWindow(WindowProtection.forProject(row.protections))
    ProjectRow(
        row = row,
        onOpen = { onOpen(row.key) },
        onMenu = { onMenu(row.key) },
        selected = selected,
        thumbModifier = covers.cover(row.key.toString(), CardViewMotion.THUMB_RADIUS_DP),
        extra = { TextAction(ShelfCopy.UNHIDE, { onUnhide(row.key) }, color = colors.ink3, style = type.small) },
        menuModifier = if (tourMenu) Modifier.tourTarget(LibraryTourSteps.HIDE) else Modifier,
    )
}
