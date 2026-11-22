package io.github.lenticualrstone.civilight.feature.script

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
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
import io.github.lenticualrstone.civilight.designsystem.component.GlassScaffold
import io.github.lenticualrstone.civilight.designsystem.component.ToastHost
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.domain.script.BlockOps
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.ScriptCommands
import io.github.lenticualrstone.civilight.feature.script.editor.CaretEvent
import io.github.lenticualrstone.civilight.feature.script.editor.WritingLineTracker
import io.github.lenticualrstone.civilight.feature.script.state.BlockRow
import io.github.lenticualrstone.civilight.feature.script.state.CommandAction
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
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.navigation.NewUnitSheet
import io.github.lenticualrstone.civilight.navigation.ProjectSearch
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.SceneWhereSheet
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.scaffold.LocalToasts
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHostLayout
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
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
    SheetHostLayout(sheets, localSheets = { key -> scriptSheet(key, vm, c?.doc, editor) }) {
        if (c == null) {
            Box(Modifier.fillMaxSize().background(CivilightTheme.colors.bg))
        } else {
            ScriptFrame(route, deps, vm, c, editor, sheets)
        }
    }
}

/** Content of the script's own sheets (shell.md §5: feature-local keys). */
private fun scriptSheet(key: SheetKey, vm: ScriptViewModel, doc: ProjectDoc?, editor: EditorState): (@Composable SheetScope.() -> Unit)? {
    doc ?: return null
    return when (key) {
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
            { LinkSheet(key, doc) { kind, target, text -> vm.insertLink(key.blockKey, key.rawOffset, kind, target, text) } }
        }
        is PositionSheetKey -> {
            { PositionSheet(key) { scene -> vm.setLensToScene(scene) } }
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
) {
    val doc = c.doc
    val wide = LocalWindowLayout.current.isInner
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
    val fieldFocus = remember { FocusRequester() }
    val commandFocus = remember { FocusRequester() }
    var commandTyping by rememberSaveable { mutableStateOf(false) }
    var commandText by rememberSaveable { mutableStateOf("") }
    val rows = c.rows

    // --- Where the header is (rule 22 §6) ---
    val sceneUnderBar by remember(rows) { derivedStateOf { sceneUnderTopBar(listState, rows) } }
    val header = HeaderModel.of(doc, c.unitKey, c.time, editor, sceneUnderBar, editor.editing, saved)
    val headerScene = header.sceneKey

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
    LaunchedEffect(vm) { vm.scrollTo.collect { key -> pendingScroll = key } }
    LaunchedEffect(pendingScroll, rows) {
        val key = pendingScroll ?: return@LaunchedEffect
        if (rows.none { it.key == key }) return@LaunchedEffect
        bringIntoView(listState, rows, key)
        pendingScroll = null
    }

    // --- Caret focus: the caret row's field holds focus while no sheet is open ---
    val caretRow = editor.caretRowKey
    val sheetOpen = sheets.top != null
    val caretRowReady = caretRow != null && rows.any { it.key == caretRow }
    LaunchedEffect(caretRow, sheetOpen, caretRowReady) {
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
    fun openNewScene() {
        vm.newSceneSheet(headerScene)?.let(sheets::open)
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

    val speakerTarget = vm.speakerTarget()
    val speakerName = speakerTarget?.speakerKey?.let { Labels.speakerName(doc, it) }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { frame.rootTop = it.positionInRoot().y; frame.height = it.size.height.toFloat() },
    ) {
        GlassScaffold(
            topBar = {
                if (editor.editing) {
                    EditingTopBar(header.line) {
                        vm.done()
                        focusManager.clearFocus()
                        keyboard?.hide()
                    }
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
                        onLayers = null,
                    )
                }
            },
            bottomBar = {
                if (editor.editing) {
                    val kind = ScriptOps.currentKind(doc, editor)
                    KeyboardToolbar(
                        kindLabel = Labels.kindLabel(kind),
                        speakerName = speakerName,
                        showSpeaker = kind == BlockKind.DIALOGUE,
                        onKind = { openOverKeyboard(BlockAddSheetKey(route.projectKey, headerScene)) },
                        onSpeaker = { speakerTarget?.let { t -> if (!vm.readOnly) openOverKeyboard(SpeakerSheetKey.of(route.projectKey, t)) } },
                        onParen = vm::openParen,
                        parenOn = editor.parenMode,
                        onLink = {
                            val block = editor.caretBlock
                            if (block != null) openOverKeyboard(LinkSheetKey(route.projectKey, block, vm.rawCaret))
                        },
                        onUndo = vm::undoStep,
                        canUndo = canUndo,
                    )
                } else {
                    val selectedKind = ScriptOps.currentKind(doc, editor)
                    Box(Modifier.windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))) {
                        ScriptBottomBar(
                            showRow = editor.hasPosition && !c.readOnly,
                            speakerName = speakerName,
                            onSpeaker = { speakerTarget?.let(::openSpeaker) },
                            onLayers = null,
                            onLineBreak = vm::lineBreak,
                            lineBreakEnabled = selectedKind != null && selectedKind in BlockOps.TEXT_KINDS,
                        ) {
                            ScriptCommandField(
                                typing = commandTyping,
                                text = commandText,
                                onStart = { commandTyping = true },
                                onText = { commandText = it },
                                onSubmit = ::submitCommand,
                                onClose = ::stopCommand,
                                onMic = {
                                    deps.navigator.push(
                                        Recording(surface = RECORDER_SURFACE, projectKey = route.projectKey, caretBlockKey = editor.selected),
                                    )
                                },
                                focusRequester = commandFocus,
                            )
                        }
                    }
                }
            },
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
            Manuscript(
                rows = rows,
                editor = editor,
                showParens = doc.app.view.script.layers.paren,
                readOnly = c.readOnly,
                entered = entered,
                listState = listState,
                padding = padding,
                wide = wide,
                actions = ManuscriptActions(
                    onTapBlock = { row, offset ->
                        if (commandTyping) stopCommand()
                        vm.tapBlock(row.blockKey, row.isText, offset)
                    },
                    onSpeaker = { row -> openSpeaker(SpeakerTarget(row.blockKey, row.speaker?.characterKey, row.sceneKey)) },
                    onTapEmptyScene = { scene ->
                        if (commandTyping) stopCommand()
                        vm.tapEmptyScene(scene)
                    },
                    onOpenHead = { scene -> sheets.open(SceneWhereSheet(route.projectKey, scene)) },
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
                            onFocusLost = { if (sheets.top == null) vm.onFieldFocusLost(row.key) },
                            onSpeaker = { openOverKeyboard(SpeakerSheetKey.of(route.projectKey, SpeakerTarget(row.blockKey, row.speaker?.characterKey, row.sceneKey))) },
                            onCaret = ::onCaret,
                            modifier = modifier,
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
                            onFocusLost = { if (sheets.top == null) vm.onFieldFocusLost(row.key) },
                            onSpeaker = { speakerTarget?.let { t -> openOverKeyboard(SpeakerSheetKey.of(route.projectKey, t)) } },
                            onCaret = ::onCaret,
                            modifier = modifier,
                        )
                    },
                ),
            )
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

/** `Recording.surface` for the command-field mic (LRC-01). */
private const val RECORDER_SURFACE = "commandBar"
