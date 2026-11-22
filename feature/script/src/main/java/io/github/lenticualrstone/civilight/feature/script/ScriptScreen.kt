package io.github.lenticualrstone.civilight.feature.script

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.GlassScaffold
import io.github.lenticualrstone.civilight.designsystem.component.ToastHost
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.domain.script.BlockOps
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.ScriptCommands
import io.github.lenticualrstone.civilight.feature.script.editor.CaretEvent
import io.github.lenticualrstone.civilight.feature.script.editor.WritingLineTracker
import io.github.lenticualrstone.civilight.feature.script.layout.ScriptLayout
import io.github.lenticualrstone.civilight.feature.script.layers.LayersModel
import io.github.lenticualrstone.civilight.feature.script.layers.LayersSheet
import io.github.lenticualrstone.civilight.feature.script.layers.LayersSheetKey
import io.github.lenticualrstone.civilight.feature.script.panel.PanelActions
import io.github.lenticualrstone.civilight.feature.script.panel.PanelSheet
import io.github.lenticualrstone.civilight.feature.script.panel.PanelSheetKey
import io.github.lenticualrstone.civilight.feature.script.panel.PanelTab
import io.github.lenticualrstone.civilight.feature.script.panel.PanelTabBody
import io.github.lenticualrstone.civilight.feature.script.panel.ScriptSidePanel
import io.github.lenticualrstone.civilight.feature.script.records.Relink
import io.github.lenticualrstone.civilight.feature.script.select.ClipboardStrip
import io.github.lenticualrstone.civilight.feature.script.select.MoveSheet
import io.github.lenticualrstone.civilight.feature.script.select.MoveSheetKey
import io.github.lenticualrstone.civilight.feature.script.select.SelectBottomBar
import io.github.lenticualrstone.civilight.feature.script.select.SelectList
import io.github.lenticualrstone.civilight.feature.script.select.SelectRowModel
import io.github.lenticualrstone.civilight.feature.script.select.SelectRows
import io.github.lenticualrstone.civilight.feature.script.select.SelectRules
import io.github.lenticualrstone.civilight.feature.script.select.SelectTopBar
import io.github.lenticualrstone.civilight.feature.script.change.ChangeActions
import io.github.lenticualrstone.civilight.feature.script.change.ChangePickSheet
import io.github.lenticualrstone.civilight.feature.script.change.ChangePickSheetKey
import io.github.lenticualrstone.civilight.feature.script.change.ChangeSheet
import io.github.lenticualrstone.civilight.feature.script.change.ChangeSheetKey
import io.github.lenticualrstone.civilight.feature.script.records.RelinkSheet
import io.github.lenticualrstone.civilight.feature.script.records.RelinkSheetKey
import io.github.lenticualrstone.civilight.feature.script.state.BlockRow
import io.github.lenticualrstone.civilight.feature.script.state.CommandAction
import io.github.lenticualrstone.civilight.feature.script.state.CommandSuggestions
import io.github.lenticualrstone.civilight.feature.script.ui.CommandSuggestionRows
import io.github.lenticualrstone.civilight.feature.script.state.CommandRow
import io.github.lenticualrstone.civilight.feature.script.state.CommandRouting
import io.github.lenticualrstone.civilight.feature.script.state.EditorState
import io.github.lenticualrstone.civilight.feature.script.state.EmptySceneRow
import io.github.lenticualrstone.civilight.feature.script.state.HeaderModel
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRow
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRowKeys
import io.github.lenticualrstone.civilight.feature.script.state.SpeakerLook
import io.github.lenticualrstone.civilight.feature.script.state.SpeakerModel
import io.github.lenticualrstone.civilight.feature.script.state.SpeakerTarget
import io.github.lenticualrstone.civilight.feature.script.ui.BlockAddSheet
import io.github.lenticualrstone.civilight.feature.script.ui.BlockAddSheetKey
import io.github.lenticualrstone.civilight.feature.script.ui.CueActions
import io.github.lenticualrstone.civilight.feature.script.ui.DirectionSheet
import io.github.lenticualrstone.civilight.feature.script.ui.DirectionSheetKey
import io.github.lenticualrstone.civilight.feature.script.ui.KindSheet
import io.github.lenticualrstone.civilight.feature.script.ui.KindSheetKey
import io.github.lenticualrstone.civilight.feature.script.ui.EditableBlock
import io.github.lenticualrstone.civilight.feature.script.ui.EditingTopBar
import io.github.lenticualrstone.civilight.feature.script.ui.KeyboardToolbar
import io.github.lenticualrstone.civilight.feature.script.ui.LinkSheet
import io.github.lenticualrstone.civilight.feature.script.ui.LinkSheetKey
import io.github.lenticualrstone.civilight.feature.script.ui.Manuscript
import io.github.lenticualrstone.civilight.feature.script.ui.ManuscriptActions
import io.github.lenticualrstone.civilight.feature.script.ui.PositionSheet
import io.github.lenticualrstone.civilight.feature.script.ui.PositionSheetKey
import io.github.lenticualrstone.civilight.feature.script.ui.ScriptBottomBar
import io.github.lenticualrstone.civilight.feature.script.ui.ScriptCommandField
import io.github.lenticualrstone.civilight.feature.script.ui.ScriptTopBar
import io.github.lenticualrstone.civilight.feature.script.ui.SpeakerSheet
import io.github.lenticualrstone.civilight.feature.script.ui.SpeakerSheetKey
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.feature.script.fold.FoldMotion
import io.github.lenticualrstone.civilight.feature.script.fold.FoldRules
import io.github.lenticualrstone.civilight.feature.script.fold.KeepAnchorOnResize
import io.github.lenticualrstone.civilight.feature.script.fold.PanelHandover
import io.github.lenticualrstone.civilight.feature.script.fold.PanelScrollMemory
import androidx.compose.animation.AnimatedVisibility
import io.github.lenticualrstone.civilight.feature.script.assets.AssetForm
import io.github.lenticualrstone.civilight.feature.script.assets.AssetFormSheet
import io.github.lenticualrstone.civilight.feature.script.assets.AssetFormSheetKey
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.NewUnitSheet
import io.github.lenticualrstone.civilight.navigation.CheckCenterFilter
import io.github.lenticualrstone.civilight.navigation.RecorderRecordDetail
import io.github.lenticualrstone.civilight.feature.script.links.BlockTap
import io.github.lenticualrstone.civilight.feature.script.terms.TermCopy
import io.github.lenticualrstone.civilight.feature.script.links.CandidateWindow
import io.github.lenticualrstone.civilight.feature.script.links.LinkCardActions
import io.github.lenticualrstone.civilight.feature.script.links.LinkCardSheet
import io.github.lenticualrstone.civilight.feature.script.links.LinkCardSheetKey
import io.github.lenticualrstone.civilight.feature.script.links.LinkFixes
import io.github.lenticualrstone.civilight.domain.script.Links
import io.github.lenticualrstone.civilight.domain.world.term.TermCandidate
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.feature.script.readonly.ReadOnlyFooter
import io.github.lenticualrstone.civilight.feature.script.readonly.ReadOnlyFrame
import io.github.lenticualrstone.civilight.feature.script.readonly.ReadOnlyTopBar
import io.github.lenticualrstone.civilight.navigation.RenumberSheet
import io.github.lenticualrstone.civilight.navigation.ProjectSearch
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.SceneWhereSheet
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.ScriptAssets
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.navigation.UnitMenuSheet
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.scaffold.LocalToasts
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHostLayout
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
import io.github.lenticualrstone.civilight.ui.window.FoldPosture
import io.github.lenticualrstone.civilight.feature.script.layout.TabletopLayout
import io.github.lenticualrstone.civilight.feature.script.layout.foldInFrameDp
import io.github.lenticualrstone.civilight.feature.script.layout.rememberTabletopFoldPx
import io.github.lenticualrstone.civilight.feature.script.panel.TabletopPanel
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.foundation.layout.height
import kotlinx.coroutines.launch

/**
 * The script screen (SCR-01, SCR-14 … SCR-20): one episode's manuscript under the 118 dp glass
 * header, the command row and command field at the bottom; while typing, the 76 dp editing header
 * and the keyboard toolbar. Built on `GlassScaffold` (not `CivilightScreen`) because the script
 * swaps its bars while typing; the toast host is drawn here the same way `CivilightScreen` does.
 */
@Composable
fun ScriptScreen(route: Script, deps: FeatureDeps) {
    val vm = entryViewModel { ScriptViewModel(route, deps) }
    val content by vm.content.collectAsState()
    val editor by vm.editor.collectAsState()
    val sheets = rememberSheetHost()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flushNow() }
    val c = content
    // SCR-31 / SCR-66: one scroll per panel tab, shared by the side panel and the cover sheet.
    val panelScroll = remember(c != null) { PanelScrollMemory(c?.doc?.app?.view?.script?.panel?.scroll.orEmpty()) }
    val gate = rememberHiddenGate(route.projectKey)
    val panelActions = { open: (SheetKey) -> Unit, leave: () -> Unit -> scriptPanelActions(route, deps, vm, gate.open, open, leave) }
    SheetHostLayout(sheets, localSheets = { key -> scriptSheet(key, vm, c?.doc, editor, panelActions, panelScroll) }) {
        if (c == null) {
            Box(Modifier.fillMaxSize().background(CivilightTheme.colors.bg))
        } else {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                ScriptFrame(route, deps, vm, c, editor, sheets, maxWidth.value, maxHeight.value, panelScroll)
            }
        }
    }
}

/** Content of the script's own sheets (shell.md §5: feature-local keys). */
private fun scriptSheet(
    key: SheetKey,
    vm: ScriptViewModel,
    doc: ProjectDoc?,
    editor: EditorState,
    panelActions: (open: (SheetKey) -> Unit, leave: () -> Unit) -> PanelActions,
    panelScroll: PanelScrollMemory,
): (@Composable SheetScope.() -> Unit)? {
    doc ?: return null
    return when (key) {
        is LayersSheetKey -> {
            {
                val view = doc.app.view.script
                LayersSheet(view.layers, view.preview, onLayer = vm::setLayer, onPreview = vm::setPreview)
            }
        }
        is PanelSheetKey -> {
            {
                val tab by vm.panelTab.collectAsState()
                val panel by vm.panel.collectAsState()
                val actions = panelActions(::open, ::close)
                PanelSheet(tab, vm::setPanelTab, scrollOf = panelScroll::state) { shown ->
                    panel?.takeIf { it.tab == shown }?.let { PanelTabBody(it, wide = false, actions = actions) }
                }
            }
        }
        is BlockAddSheetKey -> {
            {
                val position = ScriptOps.addPosition(doc, editor, key.headerScene)
                BlockAddSheet(
                    subline = position?.let { ScriptCommands.addSheetSubline(doc, it) } ?: ScriptCopy.ADD_BELOW,
                    currentKind = ScriptOps.currentKind(doc, editor),
                    readOnly = vm.readOnly,
                    onPick = { row ->
                        val kind = row.kind
                        if (kind != null) vm.addBlock(kind, key.headerScene) else vm.newSceneSheet(key.headerScene)?.let(::open)
                    },
                )
            }
        }
        is SpeakerSheetKey -> {
            {
                SpeakerSheet(
                    key = key,
                    doc = doc,
                    sceneCast = ScriptOps.sceneCast(doc, key.sceneKey),
                    onPick = { picked -> vm.pickSpeaker(key.target, picked) },
                    onRestoreDeleted = if (vm.readOnly) null else vm::restoreSpeaker,
                )
            }
        }
        is LinkSheetKey -> {
            { LinkSheet(key, doc) { kind, target, text -> vm.insertLink(key.blockKey, key.rawOffset, kind, target, text, key.optionKey) } }
        }
        is KindSheetKey -> {
            {
                val block = doc.index.block(key.blockKey)
                KindSheet(
                    subline = block?.let { io.github.lenticualrstone.civilight.domain.script.Labels.number(it.number) }.orEmpty(),
                    choices = ScriptOps.kindChoices(doc, key.blockKey),
                    readOnly = vm.readOnly,
                    onPick = { kind -> vm.changeKind(key.blockKey, kind) },
                )
            }
        }
        is DirectionSheetKey -> {
            {
                DirectionSheet(
                    key, doc, vm.readOnly,
                    CueActions(
                        add = { kind, asset, effect -> vm.addCue(key.blockKey, kind, asset, effect) },
                        replace = { cue, asset, effect -> vm.replaceCue(key.blockKey, cue, asset, effect) },
                        remove = { cue -> vm.removeCue(key.blockKey, cue) },
                        addAsset = { kind -> AssetForm.kindOf(kind)?.let { open(AssetFormSheetKey(key.projectKey, kind = it)) } },
                    ),
                )
            }
        }
        is PositionSheetKey -> {
            { PositionSheet(key) { scene -> vm.setLensToScene(scene) } }
        }
        is ChangeSheetKey -> {
            {
                val draft by vm.changeDraft.collectAsState()
                // After process death the sheet comes back without its draft: start a fresh one.
                LaunchedEffect(key) { vm.changeDraftOf(key.blockKey) }
                ChangeSheet(
                    key, doc, draft?.takeIf { it.blockKey == key.blockKey },
                    ChangeActions(edit = vm::editChange, record = vm::recordChange, addKind = vm::addRelationKind),
                )
            }
        }
        is ChangePickSheetKey -> {
            {
                val scene = doc.index.block(key.blockKey)?.sceneKey
                ChangePickSheet(key, doc, ScriptOps.sceneCast(doc, scene), vm::editChange)
            }
        }
        is MoveSheetKey -> {
            {
                val unlocked = rememberHiddenGate(key.projectKey).open
                MoveSheet(key, doc, unlocked) { position -> vm.moveBlocks(key.blockKeys, position) }
            }
        }
        is LinkCardSheetKey -> {
            {
                val gate = rememberHiddenGate(key.projectKey)
                LinkCardSheet(
                    key, doc, gate, vm.readOnly,
                    LinkCardActions(
                        relink = { kind, target -> vm.relinkLink(key.blockKey, key.linkKey, kind, target) },
                        unlink = { vm.unlinkLink(key.blockKey, key.linkKey) },
                        restore = { kind, target -> vm.restoreLinkTarget(kind, target, authenticated = gate.open) },
                        keep = { target -> vm.keepLink(target, key.blockKey) },
                    ),
                )
            }
        }
        is AssetFormSheetKey -> {
            { AssetFormSheet(key) }
        }
        is RelinkSheetKey -> {
            {
                val unlocked = rememberHiddenGate(key.projectKey).open
                RelinkSheet(Relink.of(doc, key.recordKey, unlocked), vm.readOnly) { block -> vm.relinkRecord(key.recordKey, block) }
            }
        }
        else -> null
    }
}

@Composable
private fun ScriptFrame(
    route: Script,
    deps: FeatureDeps,
    vm: ScriptViewModel,
    c: ScriptContent,
    editor: EditorState,
    sheets: SheetHost,
    widthDp: Float,
    heightDp: Float,
    panelScroll: PanelScrollMemory,
) {
    val doc = c.doc
    // SCR-03: the side panel is open by default on inner screens; `닫기` closes it for this entry.
    var panelOpen by rememberSaveable { mutableStateOf(true) }
    val panelTab by vm.panelTab.collectAsState()
    val panel by vm.panel.collectAsState()
    // SCR-36 `[PROPOSED]`: select mode hides the side panel; the column takes the area.
    val select by vm.select.collectAsState()
    val selecting = select.active
    val clipboard by vm.clipboard.collectAsState()
    val selectListState = rememberLazyListState()
    // SCR-69: in tabletop posture the screen splits at the fold: manuscript above, panel tabs and
    // the command area below (select mode keeps the whole height for its list and bars).
    var frameTopPx by remember { mutableFloatStateOf(0f) }
    val foldPx = rememberTabletopFoldPx()
    val densityScale = androidx.compose.ui.platform.LocalDensity.current.density
    val tabletop = foldPx?.let { TabletopLayout.of(FoldPosture.TABLETOP, heightDp, foldInFrameDp(it, frameTopPx, densityScale)) }
    val split = tabletop?.takeIf { !selecting }
    val layout = ScriptLayout.of(widthDp, panelOpen && !selecting && tabletop == null)
    // SCR-27: `대사만` is a read-only view of the same episode.
    val preview = doc.app.view.script.preview
    val previewOnly = !LayersModel.isEditable(preview)
    // SCR-30: one block's swipe buttons are open at a time; scrolling closes them.
    var revealedRow by remember { mutableStateOf<String?>(null) }
    val wide = layout.wide
    val saved by vm.saved.collectAsState()
    val canUndo by vm.canUndo.collectAsState()
    val entered by vm.entered.collectAsState()
    val target by vm.target.collectAsState()
    val toasts = LocalToasts.current
    val toast by toasts.current.collectAsState()
    val gate = rememberHiddenGate(route.projectKey)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val reduced = CivilightTheme.reducedMotion
    val density = androidx.compose.ui.platform.LocalDensity.current
    val fieldFocus = remember { FocusRequester() }
    val commandFocus = remember { FocusRequester() }
    var commandTyping by rememberSaveable { mutableStateOf(false) }
    var commandText by rememberSaveable { mutableStateOf("") }
    // SCR-56: the `용어 후보` whose small window is open (block, candidate).
    var openCandidate by remember { mutableStateOf<Pair<Key, TermCandidate>?>(null) }
    val rows = c.rows
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) revealedRow = null }

    // --- Where the header is (rule 22 §6) ---
    val selectRows = if (selecting) SelectRows.of(doc, rows, select, doc.app.view.script.layers, gate.open) else emptyList()
    val sceneUnderBar by remember(rows, selecting) {
        derivedStateOf { if (selecting) sceneUnderSelectBar(selectListState, selectRows) else sceneUnderTopBar(listState, rows) }
    }
    // In select mode the header (and `모두`) follow the scene under the bar, never a selected block
    // scrolled out of view: `모두` must not check blocks the writer cannot see.
    val headerState = if (selecting) editor.copy(selected = null, virtualScene = null) else editor
    val header = HeaderModel.of(doc, c.unitKey, c.time, headerState, sceneUnderBar, editor.editing, saved)
    val headerScene = header.sceneKey
    val coverPanelOpen = sheets.top is PanelSheetKey
    LaunchedEffect(layout.panel, coverPanelOpen, headerScene, split != null) {
        vm.setPanelRequest(headerScene, layout.panel || coverPanelOpen || split != null)
    }

    // --- Open at the target once (SCR-01 entry) ---
    var openedAtTarget by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(target, rows.isNotEmpty()) {
        val t = target ?: return@LaunchedEffect
        if (openedAtTarget || rows.isEmpty()) return@LaunchedEffect
        val key = t.blockKey?.let(ScriptRowKeys::block) ?: t.sceneKey?.let(ScriptRowKeys::head)
        val index = key?.let { k -> rows.indexOfFirst { it.key == k } } ?: -1
        if (index >= 0) listState.scrollToItem(index)
        openedAtTarget = true
    }
    // Rows are rebuilt off the main thread, so a requested row may not be in [rows] yet: the request
    // waits in state until it is.
    var pendingScroll by remember { mutableStateOf<String?>(null) }

    // --- Fold and unfold (SCR-66) ---
    // A restored entry (the app was closed, e.g. on fold) whose view model is new gets its block,
    // caret and scroll back; never a fresh entry, which opens at its route target.
    val restoredEntry = remember { openedAtTarget }
    LaunchedEffect(rows.isNotEmpty()) {
        if (rows.isEmpty() || vm.foldRestoreChecked) return@LaunchedEffect
        vm.foldRestoreChecked = true
        val point = doc.app.view.script.restore
        if (restoredEntry && point != null && FoldRules.shouldRestore(point, c.unitKey)) {
            vm.restoreTo(point)?.let { pendingScroll = it }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        val top = listState.layoutInfo.visibleItemsInfo.firstOrNull { ScriptRowKeys.blockKeyOf(it.key.toString()) != null }?.key?.toString()
        vm.saveFoldState(c.unitKey, top, panelScroll.offsets())
    }
    KeepAnchorOnResize(listState, rows, editor, widthDp)
    PanelHandover(
        // The tabletop's lower half shows the panel, as the side panel does.
        sideLayout = tabletop != null || ScriptLayout.of(widthDp, panelOpen = true).panel,
        sheetOpen = sheets.top is PanelSheetKey,
        panelOpen = panelOpen,
        editing = editor.editing,
        selecting = selecting,
    ) { handover ->
        if (handover.closeSheet && sheets.top is PanelSheetKey) sheets.close()
        panelOpen = handover.panelOpen
        if (handover.openSheet) sheets.open(PanelSheetKey(route.projectKey))
    }
    LaunchedEffect(vm) { vm.scrollTo.collect { key -> pendingScroll = key } }
    LaunchedEffect(vm) { vm.sheetRequests.collect { key -> sheets.open(key) } }
    // SCR-49 / SCR-53: a scene made by story's `NewUnitSheet` over the script takes the caret.
    val newUnitOpen = sheets.top is NewUnitSheet
    LaunchedEffect(newUnitOpen) { if (newUnitOpen) vm.followNewScene(c.unitKey) else vm.stopFollowingNewScene() }
    LaunchedEffect(pendingScroll, rows) {
        val key = pendingScroll ?: return@LaunchedEffect
        if (rows.none { it.key == key }) return@LaunchedEffect
        // Under the cover panel sheet the row goes to the top, so it stays visible above the sheet (SCR-31).
        bringIntoView(listState, rows, key, force = sheets.top is PanelSheetKey)
        pendingScroll = null
    }

    // --- Caret focus: the caret row's field holds focus while no sheet is open ---
    val caretRow = editor.caretRowKey
    val sheetOpen = sheets.top != null
    // The caret's row exists, and for a choice block also the caret's option line (rows lag the store).
    val caretRowReady = caretRow != null && rows.any { row ->
        row.key == caretRow && editor.caretOption.let { option -> option == null || (row as? BlockRow)?.options?.any { it.optionKey == option } == true }
    }
    // Keyed on the option line too: moving the caret to another option swaps the field (SCR-09).
    LaunchedEffect(caretRow, editor.caretOption, sheetOpen, caretRowReady) {
        if (caretRow == null || sheetOpen || !caretRowReady) return@LaunchedEffect
        // The field exists only while its row is composed: bring it into view first.
        val index = rows.indexOfFirst { it.key == caretRow }
        if (index >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.key == caretRow }) listState.scrollToItem(index)
        repeat(FOCUS_TRIES) {
            withFrameNanos { }
            // Not attached yet (the row is being composed): throws, or returns false on newer Compose.
            val result = runCatching { fieldFocus.requestFocus() }
            if (result.isSuccess && result.getOrNull() != false) {
                // A field that kept focus under a sheet does not bring the keyboard back by itself.
                keyboard?.show()
                return@LaunchedEffect
            }
        }
    }

    // --- Fixed writing line (SCR-14) ---
    val tracker = remember { WritingLineTracker() }
    val frame = remember { FrameBounds() }
    val dragged by listState.interactionSource.collectIsDraggedAsState()
    if (!editor.editing) tracker.reset()
    fun onCaret(top: Float, bottom: Float, event: CaretEvent) {
        frame.caretTop = top
        frame.caretBottom = bottom
        val kind = if (dragged || (listState.isScrollInProgress && !frame.selfScrolling)) CaretEvent.USER_SCROLL else event
        val delta = tracker.onCaret(kind, top, bottom, frame.visibleTop, frame.visibleBottom)
        if (kotlin.math.abs(delta) >= 1f) {
            scope.launch {
                frame.selfScrolling = true
                try {
                    listState.scrollBy(delta)
                } finally {
                    frame.selfScrolling = false
                }
            }
        }
    }

    // --- Actions ---
    /**
     * SCR-55: a link tapped outside typing. A live target opens its card in its area, a locked hidden
     * one asks for the unlock, a deleted one opens the link card (`다른 항목에 잇기 · 글로 풀기 · …`).
     */
    fun openLink(blockKey: Key, link: TextRun.Link): Boolean = when (Links.state(doc, link, gate.open)) {
        Links.State.Live -> LinkFixes.liveRoute(route.projectKey, link)?.let { deps.navigator.push(it); true } ?: false
        is Links.State.Hidden -> {
            gate.unlock()
            true
        }
        is Links.State.Deleted, is Links.State.Purged -> {
            sheets.open(LinkCardSheetKey(route.projectKey, blockKey, link.linkKey))
            true
        }
    }
    fun openSpeaker(t: SpeakerTarget) {
        if (!vm.readOnly) sheets.open(SpeakerSheetKey.of(route.projectKey, t))
    }
    fun openAddSheet() = sheets.open(BlockAddSheetKey(route.projectKey, headerScene))

    /**
     * A sheet opened while typing: the keyboard goes down so the sheet is not behind it (sheets do
     * not move above the keyboard); the caret stays and the keyboard returns when the sheet closes.
     */
    fun openOverKeyboard(key: SheetKey) {
        keyboard?.hide()
        sheets.open(key)
    }
    fun openLayers() = openOverKeyboard(LayersSheetKey(route.projectKey))

    /** A swipe button (SCR-30): the side panel on inner screens, the panel sheet on cover. */
    fun openPanel(blockKey: Key, tab: PanelTab) {
        revealedRow = null
        if (editor.editing) {
            focusManager.clearFocus()
            keyboard?.hide()
        }
        vm.openPanelAt(blockKey, tab)
        when {
            split != null -> Unit // SCR-69: the lower half already shows the panel.
            ScriptLayout.of(widthDp, panelOpen = true).panel -> panelOpen = true
            else -> sheets.open(PanelSheetKey(route.projectKey))
        }
    }
    fun openNewScene() {
        vm.newSceneSheet(headerScene)?.let(sheets::open)
    }
    /** `블록 고르기` (SCR-36): typing ends and the keyboard goes. */
    fun startSelect() {
        focusManager.clearFocus()
        keyboard?.hide()
        vm.enterSelect(rows)
    }
    fun stopCommand() {
        commandTyping = false
        commandText = ""
        focusManager.clearFocus()
    }
    fun submitCommand(text: String) {
        when (val action = CommandRouting.route(doc, c.unitKey, text)) {
            CommandAction.Nothing -> return
            CommandAction.OpenAddSheet -> openAddSheet()
            is CommandAction.AddBlock -> vm.addBlock(action.kind, headerScene)
            CommandAction.NewScene -> openNewScene()
            is CommandAction.Jump -> {
                action.blockKey?.let(vm::selectBlock)
                scope.launch { bringIntoView(listState, rows, action.rowKey, force = true) }
            }
            CommandAction.SetLens -> headerScene?.let(vm::setLensToScene)
            is CommandAction.Renumber -> sheets.open(RenumberSheet(route.projectKey, action.unitKey))
            CommandAction.SelectMode -> startSelect()
            CommandAction.OpenCheckCenter -> deps.navigator.push(CheckCenter(route.projectKey))
            CommandAction.OpenAssets -> deps.navigator.push(ScriptAssets(route.projectKey))
            is CommandAction.Search -> deps.navigator.push(ProjectSearch(route.projectKey, action.query))
        }
        stopCommand()
    }
    // System back while typing leaves editing first (as `완료`); with the command field open, closes it.
    LocalBackHandler.current(editor.editing && !sheetOpen) {
        vm.done()
        focusManager.clearFocus()
    }
    LocalBackHandler.current(commandTyping && !sheetOpen) { stopCommand() }
    LocalBackHandler.current(selecting && !sheetOpen) { vm.leaveSelect() }


    val speakerTarget = vm.speakerTarget()
    val speakerName = speakerTarget?.speakerKey?.let { Labels.speakerName(doc, it) }

    /** The bottom of the script: select bar, keyboard toolbar, or the command row and field (SCR-69: in the lower half). */
    val bottomContent: @Composable () -> Unit = {
        if (selecting) {
            val checked = SelectRules.ordered(select, rows)
            SelectBottomBar(
                bar = SelectRows.bar(doc, select),
                onMove = { sheets.open(MoveSheetKey(route.projectKey, checked)) },
                onDelete = { vm.deleteChecked(checked) },
                onCut = { vm.clipChecked(checked, cut = true) },
                onCopy = { vm.clipChecked(checked, cut = false) },
                onNewScene = if (c.readOnly) null else ({
                    // The checked blocks decide the sheet; select mode ends so the new scene can take the caret.
                    val key = vm.newSceneSheet(headerScene)
                    vm.leaveSelect()
                    key?.let(sheets::open)
                }),
            )
        } else if (editor.editing) {
            val kind = ScriptOps.currentKind(doc, editor)
            KeyboardToolbar(
                kindLabel = Labels.kindLabel(kind),
                speakerName = speakerName,
                showSpeaker = kind == BlockKind.DIALOGUE,
                onKind = { openOverKeyboard(BlockAddSheetKey(route.projectKey, headerScene)) },
                onSpeaker = { speakerTarget?.let { t -> if (!vm.readOnly) openOverKeyboard(SpeakerSheetKey.of(route.projectKey, t)) } },
                onParen = if (ScriptOps.takesParens(kind)) vm::openParen else null,
                parenOn = editor.parenMode,
                onLink = {
                    val block = editor.caretBlock
                    if (block != null) openOverKeyboard(LinkSheetKey(route.projectKey, block, vm.rawCaret, editor.caretOption))
                },
                onUndo = vm::undoStep,
                canUndo = canUndo,
                onChange = editor.caretBlock?.takeIf { !vm.readOnly }?.let { block ->
                    { vm.changeSheetFor(block)?.let(::openOverKeyboard) }
                },
            )
        } else {
            val selectedKind = ScriptOps.currentKind(doc, editor)
            val selectedRow = editor.selected?.let { key -> rows.firstOrNull { it.key == ScriptRowKeys.block(key) } as? BlockRow }
            Box(Modifier.windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))) {
                ScriptBottomBar(
                    row = CommandRow.of(editor, c.readOnly, previewOnly, panelShown = layout.panel || split != null, wide = wide),
                    speakerName = speakerName,
                    onSpeaker = { speakerTarget?.let(::openSpeaker) },
                    onLayers = ::openLayers,
                    onLineBreak = vm::lineBreak,
                    lineBreakEnabled = selectedKind != null && selectedKind in BlockOps.TEXT_KINDS,
                    timeHasChange = selectedRow?.hasChange == true,
                    onPanelTab = { tab -> editor.selected?.let { openPanel(it, tab) } },
                ) {
                    if (c.readOnly) {
                        // SCR-60: `예시 홈으로 · 온보딩 끝내기` in place of the command field.
                        ReadOnlyFooter(
                            onHome = { ReadOnlyFrame.goHome(deps, route.projectKey) },
                            onFinish = { ReadOnlyFrame.finish(deps, route.projectKey) },
                        )
                        return@ScriptBottomBar
                    }
                    // SCR-40: blocks on the clipboard paste below the selected block.
                    clipboard?.let { entry ->
                        val below = editor.selected?.takeIf { !c.readOnly && doc.manuscript.isLiveBlock(it) }
                        ClipboardStrip(entry, canPaste = below != null) { below?.let(vm::pasteBelow) }
                    }
                    // SCR-68: matching commands as rows while typing (a visible path to `에셋` · `블록 고르기`).
                    if (commandTyping) CommandSuggestionRows(CommandSuggestions.of(doc, c.unitKey, commandText), ::submitCommand)
                    ScriptCommandField(
                        typing = commandTyping,
                        text = commandText,
                        onStart = { commandTyping = true },
                        onText = { commandText = it },
                        onSubmit = ::submitCommand,
                        onClose = ::stopCommand,
                        onMic = {
                            deps.navigator.push(
                                Recording(
                                    surface = RECORDER_SURFACE,
                                    projectKey = route.projectKey,
                                    targetKind = headerScene?.let { RECORDER_TARGET_SCENE },
                                    targetKey = headerScene,
                                    caretBlockKey = editor.selected,
                                ),
                            )
                        },
                        focusRequester = commandFocus,
                    )
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { frameTopPx = it.positionInWindow().y },
    ) {
        // SCR-03: header, manuscript and bottom bar span the area left of the panel only.
        Box(
            (if (split != null) Modifier.height(split.upper.dp) else Modifier.fillMaxHeight())
                .width(layout.areaWidth.dp)
                // The writing line (SCR-14) works inside this area: in tabletop posture, the upper half.
                .onGloballyPositioned { frame.rootTop = it.positionInRoot().y; frame.height = it.size.height.toFloat() },
        ) {
        GlassScaffold(
            topBar = {
                if (selecting) {
                    SelectTopBar(header.line, onAll = { vm.checkAll(rows, headerScene) }, onDone = vm::leaveSelect)
                } else if (editor.editing) {
                    EditingTopBar(header.line) {
                        vm.done()
                        focusManager.clearFocus()
                        keyboard?.hide()
                    }
                } else if (c.readOnly) {
                    // SCR-60: the read-only example's banner header (board `OnbExampleScript`).
                    ReadOnlyTopBar(header.line)
                } else {
                    ScriptTopBar(
                        title = doc.project.name,
                        line = header.line,
                        positionLabel = header.positionLabel,
                        wide = wide,
                        onPositionLabel = {
                            val scene = header.sceneKey ?: return@ScriptTopBar
                            sheets.open(PositionSheetKey(route.projectKey, scene, header.positionLabel.orEmpty()))
                        },
                        onLayers = ::openLayers,
                    )
                }
            },
            bottomBar = { if (split == null) bottomContent() },
            overlay = { padding ->
                Box(
                    Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding() + TOAST_GAP),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    ToastHost(message = toast?.text, actionLabel = toast?.actionLabel, onAction = toast?.onAction?.let { { toasts.act() } })
                }
            },
        ) { padding ->
            frame.updateBars(padding)
            LaunchedEffect(frame.visibleBottom, editor.editing) {
                if (editor.editing && frame.caretBottom > 0f) onCaret(frame.caretTop, frame.caretBottom, CaretEvent.VIEWPORT)
            }
            Box(Modifier.fillMaxSize()) {
            if (selecting) {
                SelectList(
                    rows = selectRows,
                    listState = selectListState,
                    padding = padding,
                    layout = layout,
                    onToggle = { key -> vm.toggleChecked(key, rows) },
                    onDrop = vm::moveBlocks,
                )
            } else Manuscript(
                rows = rows,
                editor = editor,
                showParens = doc.app.view.script.layers.paren,
                readOnly = c.readOnly,
                entered = entered,
                listState = listState,
                padding = padding,
                layout = layout,
                actions = ManuscriptActions(
                    onTapBlock = { row, offset ->
                        if (commandTyping) stopCommand()
                        vm.tapBlock(row.blockKey, row.isText, offset)
                        // SCR-08: a direction block's cues are edited in its sheet.
                        if (row.kind == BlockKind.DIRECTION) sheets.open(DirectionSheetKey(route.projectKey, row.blockKey))
                    },
                    onTapOption = { row, option ->
                        if (commandTyping) stopCommand()
                        option.optionKey?.let { vm.tapOption(row.blockKey, it) }
                    },
                    onSpeaker = { row -> openSpeaker(SpeakerTarget(row.blockKey, row.speaker?.characterKey, row.sceneKey)) },
                    onTapEmptyScene = { scene ->
                        if (commandTyping) stopCommand()
                        vm.tapEmptyScene(scene)
                    },
                    onOpenHead = { scene -> sheets.open(SceneWhereSheet(route.projectKey, scene)) },
                    onHeadMenu = { scene ->
                        if (commandTyping) stopCommand()
                        // The menu's split, merge and delete act on the store: typed text goes first.
                        vm.flushNow()
                        openOverKeyboard(UnitMenuSheet(route.projectKey, scene))
                    },
                    onUnlockHidden = gate.unlock,
                    onNewScene = if (c.readOnly) null else {
                        { sheets.open(NewUnitSheet(route.projectKey, parentKey = c.unitKey.takeIf { it != route.projectKey })) }
                    },
                    emptySpeaker = { row -> emptySpeaker(doc, editor, row) },
                    onEntered = vm::enteredShown,
                    editableBlock = { row, modifier ->
                        EditableBlock(
                            kind = row.kind ?: BlockKind.NARRATION,
                            speaker = row.speaker,
                            number = row.number,
                            speakerMarks = row.speakerMarks,
                            memoPrefix = row.memoPrefix,
                            value = vm.fieldValue,
                            runs = vm.editRuns,
                            linkLooks = row.linkLooks,
                            parenMode = editor.parenMode,
                            inheritedHint = editor.inheritedHint == row.blockKey,
                            focusRequester = fieldFocus,
                            onValueChange = vm::onFieldChange,
                            onFocusLost = { option -> if (sheets.top == null) vm.onFieldFocusLost(row.key, option) },
                            onSpeaker = { openOverKeyboard(SpeakerSheetKey.of(route.projectKey, SpeakerTarget(row.blockKey, row.speaker?.characterKey, row.sceneKey))) },
                            onCaret = ::onCaret,
                            modifier = modifier,
                            slot = row.slot(selected = true),
                            options = if (row.kind == BlockKind.CHOICE) row.options else emptyList(),
                            caretOption = editor.caretOption,
                            onTapOption = { option -> option.optionKey?.let { vm.tapOption(row.blockKey, it) } },
                            addressHints = row.addressHints,
                        )
                    },
                    editableEmpty = { row, modifier ->
                        EditableBlock(
                            kind = BlockKind.DIALOGUE,
                            speaker = emptySpeaker(doc, editor, row) ?: SpeakerModel(ScriptCopy.NO_SPEAKER, null, SpeakerLook.UNSET, null),
                            number = null,
                            speakerMarks = emptyList(),
                            memoPrefix = null,
                            value = vm.fieldValue,
                            runs = vm.editRuns,
                            linkLooks = emptyMap(),
                            parenMode = editor.parenMode,
                            inheritedHint = editor.pendingSpeaker == null && row.speaker != null,
                            focusRequester = fieldFocus,
                            onValueChange = vm::onFieldChange,
                            onFocusLost = { option -> if (sheets.top == null) vm.onFieldFocusLost(row.key, option) },
                            onSpeaker = { speakerTarget?.let { t -> openOverKeyboard(SpeakerSheetKey.of(route.projectKey, t)) } },
                            onCaret = ::onCaret,
                            modifier = modifier,
                        )
                    },
                    onPanel = { row, tab -> openPanel(row.blockKey, tab) },
                    onReveal = { key -> revealedRow = key },
                    onTapTarget = { row, tap ->
                        if (commandTyping) stopCommand()
                        when (tap) {
                            is BlockTap.Candidate -> {
                                openCandidate = row.blockKey to tap.candidate
                                true
                            }
                            is BlockTap.Link -> openLink(row.blockKey, tap.link)
                            is BlockTap.Source -> {
                                // SCR-59: the record opens in the recorder (audio plays there).
                                deps.navigator.push(RecorderRecordDetail(tap.recordKey))
                                true
                            }
                        }
                    },
                    openCandidate = { row -> openCandidate?.takeIf { it.first == row.blockKey }?.second },
                    candidateWindow = { row ->
                        val open = openCandidate?.takeIf { it.first == row.blockKey }?.second
                        val window = open?.let { c.terms?.popover(it) }
                        if (open != null && window != null) {
                            CandidateWindow(
                                window, c.readOnly,
                                onLink = {
                                    vm.linkCandidate(open)
                                    openCandidate = null
                                },
                                onReject = {
                                    vm.rejectCandidate(open)
                                    openCandidate = null
                                },
                                onDismiss = { openCandidate = null },
                            )
                        }
                    },
                    onMarginWarning = { deps.navigator.push(CheckCenter(route.projectKey, CheckCenterFilter.TERM)) },
                ),
                headMenuOpen = (sheets.top as? UnitMenuSheet)?.unitKey,
                previewOnly = previewOnly,
                revealedRow = revealedRow,
            )
            // SCR-27: while `대사만` is on, a chip under the header says so and opens `표시`; SCR-56: so does
            // `표시 · 용어 후보 · 켬` while the candidate layer is on.
            val headerChips = listOfNotNull(
                LayersModel.headerChip(preview),
                TermCopy.CHIP.takeIf { doc.app.view.script.layers.termCandidates && !previewOnly },
            )
            if (headerChips.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = padding.calculateTopPadding() + 4.dp)
                        .padding(horizontal = if (layout.wide) layout.columnStart.dp + 24.dp else 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    headerChips.forEach { chip -> FilterChip(label = chip, selected = true, onClick = ::openLayers) }
                }
            }
            }
        }
        }
        // SCR-66: the panel comes in from 48 dp right on unfold (360 ms decelerate) and fades on fold
        // (240 ms accelerate); reduced motion: a 120 ms fade.
        AnimatedVisibility(
            visible = layout.panel && !selecting,
            modifier = Modifier.align(Alignment.TopEnd),
            enter = FoldMotion.panelEnter(reduced, density),
            exit = FoldMotion.panelExit(reduced),
        ) {
            val actions = scriptPanelActions(route, deps, vm, gate.open, open = ::openOverKeyboard, leave = {})
            ScriptSidePanel(
                tab = panelTab,
                onSelectTab = vm::setPanelTab,
                onClose = { panelOpen = false },
                scrollOf = panelScroll::state,
            ) { tab ->
                panel?.takeIf { it.tab == tab }?.let { PanelTabBody(it, wide = true, actions = actions) }
            }
        }
        // SCR-69: the lower half in tabletop posture: panel tabs, then the command area (the keyboard comes up here).
        if (split != null) {
            val actions = scriptPanelActions(route, deps, vm, gate.open, open = ::openOverKeyboard, leave = {})
            TabletopPanel(
                tab = panelTab,
                onSelectTab = vm::setPanelTab,
                scrollOf = panelScroll::state,
                modifier = Modifier.padding(top = (split.upper + split.gap).dp).height(split.lower.dp).fillMaxWidth(),
                commandArea = bottomContent,
            ) { tab ->
                panel?.takeIf { it.tab == tab }?.let { PanelTabBody(it, wide = true, actions = actions) }
            }
        }
    }
}

/** The speaker on an empty scene's line: a held pick for the next dialogue, else the inherited one (SCR-18). */
private fun emptySpeaker(doc: ProjectDoc, editor: EditorState, row: EmptySceneRow): SpeakerModel? {
    val pending = editor.pendingSpeaker?.takeIf { editor.virtualScene == row.sceneKey }
    if (pending != null) {
        val name = Labels.speakerName(doc, pending) ?: return row.speaker
        return SpeakerModel(name, null, SpeakerLook.NAMED, pending)
    }
    return row.speaker
}

/** The scene of the first row whose bottom is below the top bar's edge (rule 22 §6). */
private fun sceneUnderTopBar(listState: LazyListState, rows: List<ScriptRow>): Key? {
    val info = listState.layoutInfo
    val first = info.visibleItemsInfo.firstOrNull { it.offset + it.size > 0 } ?: return rows.firstOrNull()?.sceneKey
    return rows.getOrNull(first.index)?.sceneKey
}

/** Select mode: the scene of the first select row under the top bar. */
private fun sceneUnderSelectBar(listState: LazyListState, rows: List<SelectRowModel>): Key? {
    val first = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.offset + it.size > 0 } ?: return null
    for (i in first.index downTo 0) {
        when (val r = rows.getOrNull(i)) {
            is SelectRowModel.Head -> return r.sceneKey
            is SelectRowModel.Block -> return r.sceneKey
            null -> Unit
        }
    }
    return null
}

/** Scrolls [key] into view when it is not (fully) on screen, or always with [force]. */
private suspend fun bringIntoView(listState: LazyListState, rows: List<ScriptRow>, key: String, force: Boolean = false) {
    val index = rows.indexOfFirst { it.key == key }
    if (index < 0) return
    val visible = listState.layoutInfo.visibleItemsInfo.any { it.key == key }
    if (force || !visible) listState.animateScrollToItem(index)
}

/** Bar and caret geometry in root pixels, for the fixed writing line. */
private class FrameBounds {
    var rootTop = 0f
    var height = 0f
    var topBar = 0f
    var bottomBar = 0f
    var caretTop = 0f
    var caretBottom = 0f
    var selfScrolling = false
    val visibleTop: Float get() = rootTop + topBar
    val visibleBottom: Float get() = rootTop + height - bottomBar
}

@Composable
private fun FrameBounds.updateBars(padding: PaddingValues) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    with(density) {
        topBar = padding.calculateTopPadding().toPx()
        bottomBar = padding.calculateBottomPadding().toPx()
    }
}

/** Frames to wait for the caret row's field to attach before focusing it. */
private const val FOCUS_TRIES = 6

/** A toast sits 6 dp above the bottom bar (DS-27, as `CivilightScreen`). */
private val TOAST_GAP = 6.dp

/** `Recording.surface` for the script's mic (LRC-01; the recorder lane's route contract). */
private const val RECORDER_SURFACE = "script"
