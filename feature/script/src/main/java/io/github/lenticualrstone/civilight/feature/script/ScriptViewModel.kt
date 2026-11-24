package io.github.lenticualrstone.civilight.feature.script

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.CharacterActions
import io.github.lenticualrstone.civilight.domain.script.BlockOps
import io.github.lenticualrstone.civilight.domain.script.EditContext
import io.github.lenticualrstone.civilight.domain.script.VoiceIds
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.script.Manuscript
import io.github.lenticualrstone.civilight.domain.script.Runs
import io.github.lenticualrstone.civilight.domain.script.SceneText
import io.github.lenticualrstone.civilight.domain.script.SceneMemo
import io.github.lenticualrstone.civilight.domain.script.SceneTimeSource
import io.github.lenticualrstone.civilight.domain.script.StoryTimes
import io.github.lenticualrstone.civilight.feature.script.editor.FieldChange
import io.github.lenticualrstone.civilight.feature.script.editor.FieldDiff
import io.github.lenticualrstone.civilight.feature.script.editor.FieldOwner
import io.github.lenticualrstone.civilight.feature.script.editor.FieldSync
import io.github.lenticualrstone.civilight.feature.script.editor.Judgement
import io.github.lenticualrstone.civilight.feature.script.editor.TypedAhead
import io.github.lenticualrstone.civilight.feature.script.editor.RunEdit
import io.github.lenticualrstone.civilight.feature.script.layers.LayerRow
import io.github.lenticualrstone.civilight.feature.script.layers.LayersModel
import io.github.lenticualrstone.civilight.feature.script.panel.PanelBuild
import io.github.lenticualrstone.civilight.feature.script.panel.PanelContent
import io.github.lenticualrstone.civilight.feature.script.panel.PanelRequest
import io.github.lenticualrstone.civilight.feature.script.panel.PanelTab
import io.github.lenticualrstone.civilight.feature.script.panel.SpeakerKeep
import io.github.lenticualrstone.civilight.feature.script.select.ScriptClipboard
import io.github.lenticualrstone.civilight.feature.script.select.SelectRules
import io.github.lenticualrstone.civilight.feature.script.terms.TermMarksModel
import io.github.lenticualrstone.civilight.feature.script.links.LinkFixes
import io.github.lenticualrstone.civilight.feature.script.ui.LinkSheetKey
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import kotlinx.coroutines.flow.mapLatest
import io.github.lenticualrstone.civilight.feature.script.select.SelectState
import io.github.lenticualrstone.civilight.feature.script.state.EditorRules
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRowKeys
import io.github.lenticualrstone.civilight.feature.script.state.EditorState
import io.github.lenticualrstone.civilight.feature.script.state.InsertAt
import io.github.lenticualrstone.civilight.feature.script.state.OpenTarget
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRow
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRows
import io.github.lenticualrstone.civilight.feature.script.state.ScriptTarget
import io.github.lenticualrstone.civilight.feature.script.state.SpeakerTarget
import io.github.lenticualrstone.civilight.feature.script.ui.DirectionSheetKey
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.domain.recorder.hooks.TargetLines
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.app.ProjectAppState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.script.ScriptPreview
import io.github.lenticualrstone.civilight.model.script.StagePosition
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.store.ReadOnlyProjectException
import io.github.lenticualrstone.civilight.store.project.SaveState
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.picker.PickKind
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.project.ProjectViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the manuscript draws: one episode's rows, built off the main thread. */
data class ScriptContent(
    val doc: ProjectDoc,
    val unitKey: Key,
    val rows: List<ScriptRow>,
    val time: SceneTimeSource,
    val readOnly: Boolean,
    /** Term marks of this build (SCR-56): candidate popovers read it. */
    val terms: TermMarksModel.Source? = null,
)

/**
 * The script screen's state holder (SCR-01, 12, 14–20). A thin shell over the pure layer:
 * [ScriptTarget], [ScriptRows], [EditorRules], [ScriptOps], and the editor diff ([FieldDiff],
 * [RunEdit]).
 *
 * - **Text.** While a block is typed into, the text field ([fieldValue]) is the source of truth and the
 *   runs ([editRuns]) follow it; they are written to the store after a short pause (one undo step
 *   per block and pause, `mergeKey`), and before every structural operation.
 * - **Operations** run one at a time on a queue, each inside one store update, so typing, Enter and
 *   Backspace never interleave and every operation is one undo step (SCR-17).
 * - **Read-only** projects (the example) never get a caret; edits return `READ_ONLY`.
 */
@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class ScriptViewModel(
    private val route: Script,
    private val deps: FeatureDeps,
) : ProjectViewModel(route.projectKey, deps.projects) {

    private val _editor = MutableStateFlow(EditorState())

    /** The fold restore point was looked at for this view model (SCR-66): it applies once, to a fresh one. */
    internal var foldRestoreChecked = false
    val editor: StateFlow<EditorState> = _editor.asStateFlow()

    private val _target = MutableStateFlow<OpenTarget?>(null)

    /** Where the screen opened; the UI scrolls there once. */
    val target: StateFlow<OpenTarget?> = _target.asStateFlow()

    private val unlocked = deps.unlock.state.map { it.itemsOpen(route.projectKey) }.distinctUntilChanged()

    /**
     * The `용어 후보` index (SCR-56): built 0.5 s after the text stops changing, only while the layer
     * is on; otherwise pending (the margin `▲` never waits for it).
     */
    private val candidateIndex: StateFlow<CandidateIndex> = combine(state.filterIsInstance<ProjectLoad.Ready>(), unlocked) { r, open -> r.doc to open }
        .mapLatest { (doc, open) ->
            if (!doc.project.termCandidates) {
                // One shared instance: the state flow drops it as unchanged, so rows are not rebuilt twice.
                NO_CANDIDATES
            } else {
                delay(RESCAN_DELAY_MS)
                CandidateIndex.build(doc, TermAccess(open))
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, NO_CANDIDATES)

    val content: StateFlow<ScriptContent?> = combine(
        state.filterIsInstance<ProjectLoad.Ready>(),
        _target.filterNotNull(),
        unlocked,
        candidateIndex,
    ) { ready, target, open, index ->
        val doc = ready.doc
        val time = StoryTimes.source(doc)
        val view = doc.app.view.script
        val terms = TermMarksModel.Source(doc, index, open, candidatesOn = doc.project.termCandidates)
        ScriptContent(
            doc, target.unitKey,
            ScriptRows.build(doc, target.unitKey, time, view.layers, LayersModel.effective(view.preview), open, ready.readOnly, deps.clock.zone, terms),
            time, ready.readOnly, terms,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The text field of the block with the caret: sentinel + raw text (see [FieldDiff]). */
    var fieldValue by mutableStateOf(TextFieldValue(FieldDiff.fieldText(""), TextRange(1)), neverEqualPolicy())
        private set

    /** Runs of the block with the caret, kept equal to the field's raw text. */
    var editRuns by mutableStateOf<List<TextRun>>(emptyList())
        private set

    /** Typed runs not yet written: a block's text, or one option line's ([optionKey], SCR-09). */
    private data class PendingText(val blockKey: Key, val optionKey: Key?, val runs: List<TextRun>) {
        val mergeKey: String get() = if (optionKey == null) "type:$blockKey" else "type:$blockKey:$optionKey"
    }

    private var pending: PendingText? = null
    private var committedRuns: List<TextRun>? = null
    private var commitJob: Job? = null
    private var materializing = false
    private val hasPending = MutableStateFlow(false)

    /** `저장됨` vs `저장 중` (SCR-14): no typing waits for the store and the store has saved. */
    val saved: StateFlow<Boolean> = combine(
        hasPending,
        state.flatMapLatest { if (it is ProjectLoad.Ready) projectStore?.saveState ?: flowOf(SaveState.Saved) else flowOf(SaveState.Saved) },
    ) { waiting, save -> !waiting && save !is SaveState.Pending }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** The toolbar's undo is enabled (SCR-17). */
    val canUndo: StateFlow<Boolean> = state
        .flatMapLatest { if (it is ProjectLoad.Ready) projectStore?.undoState?.map { u -> u.canUndo } ?: flowOf(false) else flowOf(false) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _entered = MutableStateFlow<Set<Key>>(emptySet())

    /** The blocks just created; each enters from 8 dp below (SCR-12, SCR-67). A set, so quick adds do not cut each other. */
    val entered: StateFlow<Set<Key>> = _entered.asStateFlow()

    /** The enter motion of [blockKey] has played. */
    fun enteredShown(blockKey: Key) {
        _entered.update { it - blockKey }
    }

    private val _scrollTo = MutableSharedFlow<String>(extraBufferCapacity = 8)

    /** Row keys to bring into view (new blocks, caret moves after an operation). */
    val scrollTo: SharedFlow<String> = _scrollTo.asSharedFlow()

    private val _sheetRequests = MutableSharedFlow<SheetKey>(extraBufferCapacity = 4)

    /** Sheets an operation asks the screen to open (the cue sheet of a new direction block, SCR-08). */
    val sheetRequests: SharedFlow<SheetKey> = _sheetRequests.asSharedFlow()

    private val _panelTab = MutableStateFlow(PanelTab.TIME)

    /** The panel's tab (SCR-31), remembered per project in `_app/view.json` (`[PROPOSED]`). */
    val panelTab: StateFlow<PanelTab> = _panelTab.asStateFlow()

    private val _panelRequest = MutableStateFlow(PanelRequest(PanelTab.TIME, null, visible = false))

    /** The shown panel tab's content for the selected block, built off the main thread (SCR-31 … SCR-34). */
    val panel: StateFlow<PanelContent?> = combine(
        content.filterNotNull(),
        _editor.map { it.selected }.distinctUntilChanged(),
        _panelRequest,
        unlocked,
        deps.stores.recorder.state.map { it.library }.distinctUntilChanged(),
    ) { c, selected, request, open, recorder ->
        if (!request.visible) return@combine null
        val recordsOpen = deps.unlock.state.value.recordsOpen
        PanelBuild.of(
            c.doc, request.tab, selected, request.headerScene, c.time, c.doc.app.view.script.layers, open, c.readOnly, deps.clock.zone,
            today = deps.clock.today(),
            recorderLine = { scene ->
                TargetLines.line(recorder, c.doc, TargetRef(route.projectKey, RecorderTargetKind.UNIT, scene), recordsOpen)
            },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val ops = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private var busy = false

    /**
     * A layer or `대사만` can take the selected block (or the empty scene's line) out of the drawn
     * rows: the caret leaves it and the selection goes, so no field edits a row that is not there.
     * Rows lag the store, so only a block the rows' own document holds counts.
     */
    private fun dropHiddenPosition(c: ScriptContent) {
        val next = EditorRules.dropHidden(_editor.value, c.doc, c.rows.mapTo(HashSet()) { it.key }) ?: return
        flushPending()
        _editor.value = next
    }

    // --- Reading ---

    /** The newest document (the store's, ahead of [doc] right after an update). */
    internal val latestDoc: ProjectDoc? get() = projectStore?.doc?.value ?: doc.value

    val readOnly: Boolean get() = isReadOnly

    /** Caret in raw offsets of [editRuns]. */
    val rawCaret: Int get() = FieldDiff.rawOffset(fieldValue.selection.end).coerceAtMost(Runs.length(editRuns))

    /** Whose speaker the pill, the toolbar and the picker show and change. */
    fun speakerTarget(): SpeakerTarget? = latestDoc?.let { ScriptOps.speakerTarget(it, _editor.value) }

    /**
     * The `새 씬` sheet for the current position (SCR-49 routing): checked blocks in select mode, the
     * caret's offset while typing. Typed text is written first. The screen follows an open
     * `NewUnitSheet` ([followNewScene]). Null in read-only projects (the row is dimmed).
     */
    fun newSceneSheet(headerScene: Key?): SheetKey? {
        if (isReadOnly) return null
        val d = latestDoc ?: return null
        val unit = _target.value?.unitKey ?: return null
        val e = _editor.value
        val select = selectFlow.value
        val checked = if (select.active) content.value?.rows?.let { SelectRules.ordered(select, it) }.orEmpty() else emptyList()
        val offset = if (e.caretBlock != null && e.caretOption == null) rawCaret else null
        flushNow()
        return ScriptOps.newSceneSheet(d, route.projectKey, e.selected, headerScene ?: e.virtualScene, unit, checked, offset)
    }

    /** The pending follow of a scene made by `다음 씬 만들기` / `앞에 새 씬` (SCR-49, [followNewScene]). */
    internal var newSceneJob: Job? = null

    internal fun actionCtx(): ActionContext = ActionContext(
        now = deps.clock.now(),
        zone = deps.clock.zone,
        device = deps.stores.deviceId.value,
        newKey = Keys::newKey,
    )

    /** For the per-feature op files (`ScriptRecordOps`, `ScriptSelectOps`, …): the editor state, scrolling, the deps. */
    internal val editorFlow: MutableStateFlow<EditorState> get() = _editor
    internal val featureDeps: FeatureDeps get() = deps
    internal val unitKey: Key? get() = _target.value?.unitKey

    internal fun requestScroll(rowKey: String) {
        _scrollTo.tryEmit(rowKey)
    }

    /**
     * The `변화 기록` sheet's draft (SCR-41). Its pickers stack over the sheet (only the top sheet is
     * composed), so the draft lives here; it goes when the record is written or another block's sheet opens.
     */
    /** `블록 고르기` (SCR-36): active and the checked blocks (drawn blocks only). */
    internal val selectFlow = MutableStateFlow(SelectState())
    val select: StateFlow<SelectState> = selectFlow.asStateFlow()

    /** The block clipboard of this project (SCR-40). */
    val clipboard: StateFlow<ScriptClipboard.Entry?> =
        ScriptClipboard.entry
            .map { it?.takeIf { e -> e.projectKey == route.projectKey } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)


    /** The pending `씬 메모` write (SCR-35, [editSceneMemo]). */
    internal var memoJob: Job? = null
    internal var pendingMemo: PendingMemo? = null

    internal val changeDraft = MutableStateFlow<io.github.lenticualrstone.civilight.feature.script.change.ChangeDraft?>(null)

    // --- (0.2.0, U-14) The input bar's draft (SCR-70) and its view state ---

    /** The draft: runs, chips, mark. Mirrored into [DraftMemory] at every change (SCR-70 persistence). */
    internal val draftFlow = MutableStateFlow(io.github.lenticualrstone.civilight.feature.script.input.Draft())
    val draft: StateFlow<io.github.lenticualrstone.civilight.feature.script.input.Draft> = draftFlow.asStateFlow()

    /** The scene the last `다음 씬` tap made (double-tap guard, `NextScene.emptyAfter`). */
    internal var lastNextScene: Key? = null

    /** A refused `다음 씬` tap: the screen flashes the place label (`씬 {n} 끝에`, SCR-71) so the tap shows. */
    internal val placeFlash = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val placeFlashes: SharedFlow<Unit> = placeFlash.asSharedFlow()

    /** Blocks with the `새로 · 나눔 · 합침` look (SCR-04), until the next send, tap, swipe or `고치기`. */
    internal val freshFlow = MutableStateFlow<Map<Key, String>>(emptyMap())
    val fresh: StateFlow<Map<Key, String>> = freshFlow.asStateFlow()

    internal fun clearFresh() {
        if (freshFlow.value.isNotEmpty()) freshFlow.value = emptyMap()
    }

    /** Row keys to place directly above the input bar (a send, `고치기`, `이 뒤에`, `다음 씬`; SCR-71). */
    internal val aboveInput = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val scrollAboveInput: SharedFlow<String> = aboveInput.asSharedFlow()

    /** The scene in view (SCR-71), as the screen last measured it (낭독 starts there). */
    var sceneInViewKey: Key? = null

    /** Commands picked in `찾기` (SCR-68): the screen runs them. */
    internal val commandRequests = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** The input bar's draft restored from the screen's saved state (process death) or the process memory. */
    internal var draftOpened = false

    /** The hidden gate's last value (the draft follows it: a locked block's text never stays in the field). */
    internal var gateOpen = false

    internal fun requestSheet(key: SheetKey) {
        _sheetRequests.tryEmit(key)
    }

    internal fun ctx(): EditContext = EditContext(
        now = deps.clock.now(),
        zone = deps.clock.zone,
        device = deps.stores.deviceId.value,
        newKey = Keys::newKey,
    )

    // --- Opening and following the document ---

    private fun open(d: ProjectDoc) {
        _panelTab.value = PanelTab.fromStored(d.app.view.script.panel.tab)
        val t = ScriptTarget.resolve(d, route.sceneKey, route.blockKey, route.caretAtEnd)
        _target.value = t
        // (0.2.0, SCR-14) No caret in blocks: the target block is selected, the draft comes back or starts.
        openDraft(d, t)
        val block = t.blockKey ?: return
        if (route.blockKey != null) _editor.value = EditorRules.select(_editor.value, block)
    }

    private fun reconcile(d: ProjectDoc? = latestDoc) {
        d ?: return
        val before = _editor.value
        val after = EditorRules.reconcile(before, d)
        if (after != before) _editor.value = after
        val key = after.caretBlock ?: return
        val block = d.index.block(key) ?: return
        val option = after.caretOption
        val runs = if (option != null) block.options.firstOrNull { it.key == option }?.runs ?: return else block.runs
        if (pending == null && runs != committedRuns) {
            // Changed elsewhere (undo, another screen): the field follows the document.
            editRuns = runs
            committedRuns = runs
            seedField(runs, rawCaret)
        }
    }

    internal fun enqueue(op: suspend () -> Unit) {
        ops.trySend(op)
    }

    /** Runs [op] inside one store update; null when nothing applies or the project is read-only. */
    internal suspend fun runOp(label: String, op: (ProjectDoc) -> ScriptOps.Outcome?): ScriptOps.Outcome? {
        var outcome: ScriptOps.Outcome? = null
        val result = update(label) { d -> op(d)?.also { outcome = it }?.doc ?: d }
        return if (result == EditResult.APPLIED) outcome else null
    }

    // --- Caret and selection ---

    /**
     * (0.2.0, SCR-14) A block tap only selects: no caret, no keyboard. Tapping the selected block again
     * clears the selection. In `대사만` a tap goes back to `쓰기` at the block.
     */
    fun tapBlock(blockKey: Key, isText: Boolean, rawOffset: Int?) {
        if (!writing()) {
            leavePreviewAt(blockKey)
            return
        }
        _editor.update { if (it.selected == blockKey) it.copy(selected = null) else EditorRules.select(it, blockKey) }
        clearFresh()
    }

    /** A tap on an option line selects its choice block (SCR-14). */
    fun tapOption(blockKey: Key, optionKey: Key) = tapBlock(blockKey, isText = false, rawOffset = null)

    /** A tap on empty manuscript space clears the selection (SCR-14 `[DEFAULT]`). */
    fun tapEmptyScene(sceneKey: Key) {
        _editor.update { it.copy(selected = null) }
    }

    internal fun enterVirtual(sceneKey: Key) {
        editRuns = emptyList()
        committedRuns = null
        seedField(emptyList(), 0)
        _editor.update { EditorRules.editVirtual(it, sceneKey) }
    }

    /** `완료` (SCR-14): the keyboard goes, the block stays selected. */
    fun done() {
        val key = _editor.value.caretBlock
        val offset = rawCaret
        flushPending()
        _editor.update(EditorRules::stopEditing)
        if (key != null) enqueue { saveLastPosition(key, offset) }
    }

    /**
     * The field of row [rowKey] lost focus (command field tapped, row scrolled away). A field that
     * lost focus because the caret already moved to another row (Enter, split, merge, the first
     * line of an empty scene) is ignored, so the new row keeps the caret.
     */
    fun onFieldFocusLost(rowKey: String, optionKey: Key? = null) {
        // Moving between option lines of one choice block swaps the field: not a focus loss.
        val state = _editor.value
        if (state.caretRowKey == rowKey && state.caretOption == optionKey) done()
    }

    /** A jump from the command field (SCR-68 `#043`): the block is selected, no caret. */
    fun selectBlock(blockKey: Key) {
        flushPending()
        _editor.update { EditorRules.select(it, blockKey) }
    }

    /** A row of the panel's `이 씬 안` (SCR-32 `[PROPOSED]`): the block is selected and scrolled to. */
    fun selectAndShow(blockKey: Key) {
        selectBlock(blockKey)
        _scrollTo.tryEmit(ScriptRowKeys.block(blockKey))
    }

    /** The picker target of a dialogue block (the panel's `화자 · 바꾸기`). */
    fun speakerTargetOf(blockKey: Key): SpeakerTarget? {
        val block = latestDoc?.index?.block(blockKey) ?: return null
        return SpeakerTarget(blockKey, block.speakerKey, block.sceneKey)
    }

    private fun startEditingIn(d: ProjectDoc, blockKey: Key, rawOffset: Int, hint: Key?, optionKey: Key? = null) {
        val block = d.index.block(blockKey) ?: return
        val runs = if (optionKey != null) block.options.firstOrNull { it.key == optionKey }?.runs ?: return else block.runs
        editRuns = runs
        committedRuns = runs
        seedField(runs, rawOffset)
        _editor.update { EditorRules.edit(it, blockKey, optionKey).copy(inheritedHint = hint) }
        fieldSync.caretIn(FieldOwner(ScriptRowKeys.block(blockKey), optionKey))
    }

    private fun seedField(runs: List<TextRun>, rawCaret: Int) {
        val raw = runs.plainText
        fieldValue = TextFieldValue(FieldDiff.fieldText(raw), TextRange(FieldDiff.fieldOffset(rawCaret.coerceIn(0, raw.length))))
    }

    // --- Typing (SCR-14, SCR-15) ---

    /** What the text fields' rejected Enters left behind (QA #5): see [FieldSync]. */
    private val fieldSync = FieldSync()

    /**
     * Every change of the caret block's text field. [from] names the field that emitted it (its
     * row and option line), so a change from a field whose Enter was just taken is not read as an
     * edit of the block that took the caret.
     */
    fun onFieldChange(value: TextFieldValue, from: FieldOwner? = null) {
        val state = _editor.value
        if (!state.editing || isReadOnly) return
        val oldRaw = editRuns.plainText
        val judged = fieldSync.judge(from, value.text, value.selection.end)
        if (judged is Judgement.Ahead) {
            // Typed after Enter: it belongs to the new block. While the Enter is in flight the field keeps what
            // it holds (exactly what the sync has seen); once the new block has the caret, the field shown is
            // the new block's and a stale field's value must not replace it.
            if (!fieldSync.advanced) fieldValue = value
            judged.edits.forEach { edit -> enqueue { replayTypedAhead(edit) } }
            return
        }
        when (val change = FieldDiff.classify(oldRaw, value.text, value.selection.end, state.parenMode, parens = caretTakesParens())) {
            FieldChange.None -> {
                val moved = FieldDiff.rawOffset(value.selection.end) != rawCaret
                fieldValue = value.withCaretAfterSentinel()
                if (moved && state.parenMode) _editor.update { it.copy(parenMode = false) }
            }
            is FieldChange.Text -> {
                val block = state.caretBlock
                if (block != null && LinkFixes.opensPicker(change.inserted, change.start, change.end)) {
                    // SCR-55: a typed `@` opens the link picker at the caret instead of being written.
                    fieldValue = fieldValue
                    _sheetRequests.tryEmit(LinkSheetKey(route.projectKey, block, change.start, state.caretOption))
                } else {
                    applyText(change, value, state)
                }
            }
            is FieldChange.Enter -> {
                if (from != null) {
                    // The field is not given its old text back: it takes a published value only at its next
                    // recomposition, and a key typed before that is built on this buffer (QA #5). It keeps the
                    // line break until the new block has the caret (see [FieldSync]).
                    fieldValue = value
                    fieldSync.rejectEnter(from, oldRaw, change.at)
                } else {
                    // The old text back, without the composition the Enter ended.
                    fieldValue = fieldValue.copy(composition = null)
                }
                enter(change.at)
            }
            FieldChange.BackspaceAtStart -> {
                fieldValue = fieldValue.copy(composition = null)
                backspaceAtStart()
            }
            is FieldChange.OpenParen -> {
                fieldValue = fieldValue.copy(selection = TextRange(FieldDiff.fieldOffset(change.at)))
                _editor.update { it.copy(parenMode = true) }
            }
            is FieldChange.CloseParen -> {
                fieldValue = fieldValue.copy(selection = TextRange(FieldDiff.fieldOffset(change.at)))
                _editor.update { it.copy(parenMode = false) }
            }
        }
    }

    private fun TextFieldValue.withCaretAfterSentinel(): TextFieldValue {
        val start = selection.start.coerceAtLeast(1)
        val end = selection.end.coerceAtLeast(1)
        return if (start == selection.start && end == selection.end) this else copy(selection = TextRange(start, end))
    }

    private fun applyText(change: FieldChange.Text, value: TextFieldValue, state: EditorState) {
        val runs = RunEdit.replace(editRuns, change.start, change.end, change.inserted, state.parenMode)
        editRuns = runs
        val expected = value.text.replace(FieldDiff.SENTINEL.toString(), "")
        if (runs.plainText == expected && value.text.startsWith(FieldDiff.SENTINEL)) {
            fieldValue = value.withCaretAfterSentinel()
        } else {
            seedField(runs, change.start + change.inserted.length)
        }
        afterTyping(state)
    }

    /** An edit that did not come from the field itself (typed-ahead text): the field is seeded with the result. */
    private fun insertTyped(change: FieldChange.Text, state: EditorState) {
        val runs = RunEdit.replace(editRuns, change.start, change.end, change.inserted, state.parenMode)
        editRuns = runs
        seedField(runs, change.start + change.inserted.length)
        afterTyping(state)
    }

    private fun afterTyping(state: EditorState) {
        if (state.inheritedHint != null && state.inheritedHint == state.selected) _editor.update { it.copy(inheritedHint = null) }
        onTextEdited()
    }

    private fun onTextEdited() {
        val state = _editor.value
        if (state.virtualScene != null) {
            if (!materializing) {
                materializing = true
                enqueue { materialize() }
            }
            return
        }
        val key = state.selected ?: return
        pending = PendingText(key, state.caretOption, editRuns)
        hasPending.value = true
        commitJob?.cancel()
        commitJob = viewModelScope.launch {
            delay(COMMIT_DELAY_MS)
            enqueue { commitNow() }
        }
    }

    /** Writes typed runs to the store (one undo step per block and pause). */
    internal suspend fun commitNow(label: String = ScriptCopy.UNDO_TYPING, merge: Boolean = true) {
        commitJob?.cancel()
        val p = pending ?: return
        pending = null
        hasPending.value = false
        val open = featureDeps.unlock.state.value.itemsOpen(route.projectKey)
        update(label, if (merge) p.mergeKey else null) { d ->
            val written = if (p.optionKey != null) ScriptOps.setOptionRuns(d, p.blockKey, p.optionKey, p.runs, ctx())
            else ScriptOps.setRuns(d, p.blockKey, p.runs, ctx())
            // WLD-22 (world lane's request): a `아님` the edit made stale goes in the same undo step.
            TermMarksModel.releaseStale(written, p.blockKey, p.optionKey, open)
        }
        val here = _editor.value.let { it.selected == p.blockKey && it.caretOption == p.optionKey }
        if (here) committedRuns = p.runs
        saveLastPosition(p.blockKey, if (here && p.optionKey == null) rawCaret else Runs.length(p.runs))
    }

    /** Queues the typed text of the block being left. */
    internal fun flushPending() {
        if (pending != null) enqueue { commitNow() }
    }

    /** The first character in an empty scene creates its first line (SCR-01 `[PROPOSED]`). */
    private suspend fun materialize() {
        val state = _editor.value
        val scene = state.virtualScene
        if (scene == null) {
            materializing = false
            return
        }
        val runs = editRuns
        val outcome = runOp(ScriptCopy.UNDO_ADD_BLOCK) { d -> ScriptOps.materialize(d, scene, state.pendingSpeaker, runs, ctx()) }
        materializing = false
        val key = outcome?.caret?.blockKey ?: return
        _entered.update { it + key }
        committedRuns = runs
        _editor.update { it.copy(selected = key, virtualScene = null, editing = true, pendingSpeaker = null, inheritedHint = null) }
        if (editRuns != runs) {
            pending = PendingText(key, null, editRuns)
            hasPending.value = true
            enqueue { commitNow() }
        }
        _scrollTo.tryEmit("block:$key")
    }

    private fun enter(at: Int) {
        enqueue {
            val before = caretOwner()
            try {
                commitNow()
                val key = _editor.value.caretBlock ?: return@enqueue
                val option = _editor.value.caretOption
                if (option != null) {
                    apply(runOp(ScriptCopy.UNDO_ADD_OPTION) { d -> ScriptOps.enterOption(d, key, option, at, ctx()) })
                    return@enqueue
                }
                val atEnd = at >= Runs.length(editRuns)
                val label = if (atEnd) ScriptCopy.UNDO_ADD_BLOCK else ScriptCopy.UNDO_SPLIT
                apply(runOp(label) { d -> ScriptOps.enter(d, key, at, ctx()) })
            } finally {
                // An Enter that changed nothing leaves the caret in the same field: it gets its old text back.
                val stayed = caretOwner() == before
                fieldSync.landed(stayed)
                if (stayed && _editor.value.editing) seedField(editRuns, at.coerceIn(0, Runs.length(editRuns)))
            }
        }
    }

    /** The field that holds the caret: its row and option line. */
    private fun caretOwner(): FieldOwner? = _editor.value.let { e -> e.caretRowKey?.let { FieldOwner(it, e.caretOption) } }

    /** Text typed after an Enter, written to the block or option line the Enter made (runs after the Enter's operation). */
    private fun replayTypedAhead(edit: TypedAhead) {
        val state = _editor.value
        if (!fieldSync.advanced || !state.editing || isReadOnly || state.virtualScene != null) return
        when (edit) {
            is TypedAhead.Enter -> enter(edit.offset.coerceIn(0, Runs.length(editRuns)))
            is TypedAhead.Edit -> {
                val start = edit.offset.coerceIn(0, Runs.length(editRuns))
                val end = (start + edit.length).coerceIn(start, Runs.length(editRuns))
                insertTyped(FieldChange.Text(start, end, edit.text), state)
            }
        }
    }

    private fun backspaceAtStart() {
        enqueue {
            commitNow()
            val key = _editor.value.caretBlock ?: return@enqueue
            val option = _editor.value.caretOption
            if (option != null) {
                apply(runOp(ScriptCopy.UNDO_DELETE) { d -> ScriptOps.backspaceOption(d, key, option, ctx()) })
                return@enqueue
            }
            val empty = editRuns.isEmpty()
            val label = if (empty) ScriptCopy.UNDO_DELETE else ScriptCopy.UNDO_MERGE
            apply(runOp(label) { d -> ScriptOps.backspaceAtStart(d, key, ctx()) })
        }
    }

    /** Moves the caret or selection after an operation and shows its toast. */
    internal fun apply(outcome: ScriptOps.Outcome?) {
        outcome ?: return
        outcome.created?.let { created -> _entered.update { it + created } }
        val caret = outcome.caret
        val select = outcome.select
        when {
            // (0.2.0, SCR-14) No caret in blocks: an operation that used to place one selects the block.
            caret != null -> {
                _editor.update { EditorRules.select(it, caret.blockKey) }
                _scrollTo.tryEmit("block:${caret.blockKey}")
            }
            select != null -> {
                _editor.update { EditorRules.select(it, select) }
                _scrollTo.tryEmit("block:$select")
            }
            else -> _editor.update(EditorRules::stopEditing)
        }
        outcome.toast?.let(::showUndoToast)
    }

    // --- Commands ---

    /** A block from the add sheet or a slash shortcut (SCR-12), below the current block. */
    fun addBlock(kind: BlockKind, headerScene: Key?) {
        if (isReadOnly) return
        enqueue {
            commitNow()
            val state = _editor.value
            val d = latestDoc ?: return@enqueue
            val at = state.virtualScene?.let { InsertAt.SceneStart(it) } ?: ScriptOps.insertTarget(d, state.selected, headerScene) ?: return@enqueue
            val speaker = state.pendingSpeaker.takeIf { kind == BlockKind.DIALOGUE }
            // SCR-26 `[PROPOSED]`: a block of a hidden kind turns its layer on; a new block is written in `쓰기`.
            // Done before the insert, so no row snapshot holds the new block with its layer still off
            // (that would make `dropHiddenPosition` take the selection off it).
            updateApp { app ->
                val script = app.view.script
                app.copy(view = app.view.copy(script = script.copy(layers = Manuscript.layersAfterInsert(script.layers, kind), preview = ScriptPreview.WRITE)))
            }
            val outcome = runOp(ScriptCopy.UNDO_ADD_BLOCK) { doc -> ScriptOps.insert(doc, at, kind, speaker, ctx()) } ?: return@enqueue
            if (speaker != null) _editor.update { it.copy(pendingSpeaker = null) }
            apply(outcome)
            // SCR-08: a new direction block has nothing to show until it has a cue.
            if (kind == BlockKind.DIRECTION) outcome.select?.let { _sheetRequests.tryEmit(DirectionSheetKey(route.projectKey, it)) }
        }
    }

    // --- Delete (SCR-38), kind (SCR-13), cues (SCR-08) ---

    /** Deletes blocks without asking; the toast reports where records went (SCR-38). */
    fun deleteBlocks(blockKeys: Collection<Key>) {
        if (isReadOnly || blockKeys.isEmpty()) return
        enqueue {
            commitNow()
            apply(runOp(ScriptCopy.UNDO_DELETE) { d -> ScriptOps.deleteBlocks(d, blockKeys, ctx()) })
        }
    }

    /** `종류 바꾸기` (SCR-13): converts the block in place; it stays selected. */
    fun changeKind(blockKey: Key, kind: BlockKind) {
        if (isReadOnly) return
        enqueue {
            commitNow()
            apply(runOp(ScriptCopy.UNDO_KIND) { d -> ScriptOps.changeKind(d, blockKey, kind, ctx()) })
        }
    }

    /** `+ 연출` (SCR-08): a cue of [kind] with an asset, or a screen effect. */
    fun addCue(blockKey: Key, kind: CueKind, assetKey: Key?, effect: String?) = cueOp { d -> ScriptOps.addCue(d, blockKey, kind, assetKey, effect, ctx()) }

    /** `바꾸기` of one cue (SCR-08). */
    fun replaceCue(blockKey: Key, cueKey: Key, assetKey: Key?, effect: String?) =
        cueOp { d -> ScriptOps.replaceCue(d, blockKey, cueKey, assetKey, effect, ctx()) }

    /** `빼기` of one cue (SCR-08); the last one takes the empty block with it. */
    fun removeCue(blockKey: Key, cueKey: Key) = cueOp { d -> ScriptOps.removeCue(d, blockKey, cueKey, ctx()) }

    private fun cueOp(op: (ProjectDoc) -> ScriptOps.Outcome?) {
        if (isReadOnly) return
        enqueue {
            commitNow()
            apply(runOp(ScriptCopy.UNDO_CUE, op))
        }
    }

    /** `줄바꿈` (SCR-15, Q-03 default): a line break inside the current text block. */
    fun lineBreak() {
        if (isReadOnly) return
        val state = _editor.value
        if (state.virtualScene != null) return
        if (state.caretBlock == null) {
            val key = state.selected ?: return
            enqueue {
                commitNow()
                val d = latestDoc ?: return@enqueue
                val block = d.index.block(key) ?: return@enqueue
                if (block.kind.known !in io.github.lenticualrstone.civilight.domain.script.BlockOps.TEXT_KINDS) return@enqueue
                startEditingIn(d, key, Runs.length(block.runs), hint = null)
                insertBreakAtCaret()
            }
            return
        }
        insertBreakAtCaret()
    }

    private fun insertBreakAtCaret() {
        val key = _editor.value.caretBlock ?: return
        val at = rawCaret
        val runs = RunEdit.replace(editRuns, at, at, "\n")
        editRuns = runs
        seedField(runs, at + 1)
        commitJob?.cancel()
        pending = PendingText(key, _editor.value.caretOption, runs)
        hasPending.value = true
        enqueue { commitNow(ScriptCopy.UNDO_LINE_BREAK, merge = false) }
    }

    /** Toolbar `( )` (SCR-16): typed text goes into a paren run at the caret. */
    fun openParen() {
        if (_editor.value.editing && caretTakesParens()) _editor.update { it.copy(parenMode = true) }
    }

    /**
     * `괄호 지문` are written inside dialogue lines (SCR-07), and inside choice options, which can be
     * spoken (SCR-09). Other kinds keep `(` and `)` as plain text (`ED`).
     */
    fun caretTakesParens(): Boolean {
        val d = latestDoc ?: return false
        return ScriptOps.takesParens(ScriptOps.currentKind(d, _editor.value))
    }

    /**
     * `@` picker result (SCR-16 → SCR-55): a link run holding the picked text at raw [rawOffset] of
     * [blockKey] (where the caret was when `@` was tapped); the caret goes after it.
     */
    fun insertLink(blockKey: Key, rawOffset: Int, kind: PickKind, key: Key, text: String, optionKey: Key? = null) {
        if (isReadOnly || text.isEmpty()) return
        val target = when (kind) {
            PickKind.CHARACTER -> LinkTargetKind.CHARACTER
            PickKind.TERM -> LinkTargetKind.TERM
            PickKind.PLACE -> LinkTargetKind.PLACE
            else -> return
        }
        val link = TextRun.Link(Keys.newKey(), LinkTarget(target, key), text)
        if (_editor.value.caretBlock == blockKey && _editor.value.caretOption == optionKey) {
            insertRunAt(blockKey, rawOffset, link)
            return
        }
        enqueue {
            commitNow()
            val d = latestDoc ?: return@enqueue
            val block = d.index.block(blockKey)?.takeIf { d.manuscript.isLiveBlock(it.key) } ?: return@enqueue
            val runs = if (optionKey != null) block.options.firstOrNull { it.key == optionKey }?.runs ?: return@enqueue else block.runs
            startEditingIn(d, blockKey, rawOffset.coerceIn(0, Runs.length(runs)), hint = null, optionKey = optionKey)
            insertRunAt(blockKey, rawOffset, link)
        }
    }

    private fun insertRunAt(blockKey: Key, rawOffset: Int, link: TextRun.Link) {
        val at = rawOffset.coerceIn(0, Runs.length(editRuns))
        val (front, back) = Runs.splitAt(editRuns, at)
        val runs = Runs.normalize(front + link + back)
        editRuns = runs
        seedField(runs, Runs.length(front) + link.text.length)
        if (_editor.value.selected == blockKey) onTextEdited()
    }

    /** `지운 인물 · 노아 · 되살리기` in the picker (SCR-19, rule 20): the character comes back from the trash. */
    fun restoreSpeaker(characterKey: Key, authenticated: Boolean) {
        if (isReadOnly) return
        enqueue {
            commitNow()
            var toast: String? = null
            val context = actionCtx()
            update(ScriptCopy.UNDO_RESTORE) { d ->
                val result = CharacterActions.restore(d, characterKey, context, authenticated)
                val outcome = result.outcome
                if (outcome is RestoreOutcome.Restored) {
                    toast = result.toast?.message
                    outcome.doc
                } else d
            }
            toast?.let(::showUndoToast)
        }
    }

    /** Picker result (SCR-19): the dialogue block's speaker, or the speaker of the next dialogue insert. */
    fun pickSpeaker(target: SpeakerTarget, characterKey: Key) {
        if (isReadOnly) return
        val block = target.blockKey
        if (block == null) {
            _editor.update { it.copy(pendingSpeaker = characterKey) }
            enqueue { rememberSpeaker(characterKey) }
            return
        }
        enqueue {
            commitNow()
            update(ScriptCopy.UNDO_SPEAKER) { d -> ScriptOps.changeSpeaker(d, block, characterKey, ctx()) }
            _editor.update { if (it.inheritedHint == block) it.copy(inheritedHint = null) else it }
            rememberSpeaker(characterKey)
        }
    }

    private suspend fun rememberSpeaker(characterKey: Key) {
        updateApp { app ->
            val script = app.view.script
            app.copy(view = app.view.copy(script = script.copy(recentSpeakers = ScriptOps.recentSpeakers(script.recentSpeakers, characterKey))))
        }
    }

    /** Toolbar undo (SCR-17): the last step of the project's history. */
    fun undoStep() {
        if (isReadOnly) return
        enqueue {
            commitNow()
            undo()
        }
    }

    /** `렌즈를 이 씬으로 맞추기` (SCR-02): the app lens at the end of this scene, with its toast. */
    fun setLensToScene(sceneKey: Key) {
        val store = projectStore ?: return
        val d = latestDoc ?: return
        deps.lens(store).setLensToScene(sceneKey)
        deps.toasts.show(SceneText.lensToast(d, sceneKey), actionLabel = null)
    }

    // --- 표시 sheet (SCR-26 … SCR-28) ---

    internal fun writing(): Boolean = LayersModel.isEditable(latestDoc?.app?.view?.script?.preview ?: ScriptPreview.WRITE)

    /** A layer switch of the `표시` sheet: view state only (no undo step, also in read-only projects). */
    fun setLayer(row: LayerRow, on: Boolean) {
        viewModelScope.launch {
            updateApp { app -> app.copy(view = app.view.copy(script = app.view.script.copy(layers = LayersModel.set(app.view.script.layers, row, on)))) }
        }
    }

    /** `쓰기 · 대사만` (SCR-27). Leaving `쓰기` ends typing; the block stays selected. */
    fun setPreview(preview: ScriptPreview) {
        if (preview == ScriptPreview.READ_ALOUD) return
        if (!LayersModel.isEditable(preview)) {
            flushPending()
            _editor.update(EditorRules::stopEditing)
        }
        viewModelScope.launch {
            updateApp { app -> app.copy(view = app.view.copy(script = app.view.script.copy(preview = preview))) }
        }
    }

    /** A tap on a line in `대사만` (SCR-27 `[PROPOSED]`): back to `쓰기` with that block selected. */
    fun leavePreviewAt(blockKey: Key) {
        flushPending()
        _editor.update { EditorRules.select(it, blockKey) }
        viewModelScope.launch {
            updateApp { app -> app.copy(view = app.view.copy(script = app.view.script.copy(preview = ScriptPreview.WRITE))) }
            _scrollTo.tryEmit(ScriptRowKeys.block(blockKey))
        }
    }

    // --- Panel (SCR-30 … SCR-34) ---

    /** The screen's panel state: which tab is shown, the scene under the header, whether a panel is on screen. */
    fun setPanelRequest(headerScene: Key?, visible: Boolean) {
        _panelRequest.value = PanelRequest(_panelTab.value, headerScene, visible)
    }

    /** Switches the panel tab and remembers it for the project (SCR-31 `[PROPOSED]`). */
    fun setPanelTab(tab: PanelTab) {
        if (tab !in PanelTab.BUILT) return
        _panelTab.value = tab
        _panelRequest.update { it.copy(tab = tab) }
        viewModelScope.launch {
            updateApp { app ->
                val script = app.view.script
                app.copy(view = app.view.copy(script = script.copy(panel = script.panel.copy(tab = tab.stored))))
            }
        }
    }

    /** A swipe button (SCR-30): the block is selected (no caret) and the panel shows [tab]. */
    fun openPanelAt(blockKey: Key, tab: PanelTab) {
        selectBlock(blockKey)
        setPanelTab(tab)
        _scrollTo.tryEmit(ScriptRowKeys.block(blockKey))
    }

    /** `무대 위치` (SCR-25): one undo step. */
    fun setStage(blockKey: Key, stage: StagePosition) {
        if (isReadOnly) return
        enqueue {
            commitNow()
            update(ScriptCopy.STAGE_FIELD) { d -> BlockOps.setStage(d, blockKey, stage, ctx()) }
        }
    }

    /** `이대로 두기` on a deleted speaker's line (SCR-34, rule 20): one undo step, no toast. */
    fun keepSpeaker(characterKey: Key, blockKey: Key) {
        if (isReadOnly) return
        enqueue {
            commitNow()
            update(ScriptCopy.UNDO_KEEP) { d -> SpeakerKeep.keep(d, characterKey, blockKey, actionCtx().trash) }
        }
    }

    /** `보이스 ID` typed with the format `직접` (SCR-23): one undo step. */
    fun setManualVoice(blockKey: Key, id: String) {
        if (isReadOnly) return
        enqueue {
            commitNow()
            update(ScriptCopy.VOICE_FIELD) { d -> VoiceIds.setManualId(d, blockKey, id, ctx()) }
        }
    }

    /** Writes pending text and a typed `씬 메모` now (screen stops). */
    fun flushNow() {
        flushPending()
        flushMemo()
    }

    internal fun showUndoToast(text: String) {
        val count = projectStore?.undoState?.value?.undoCount ?: return
        deps.toasts.show(text) {
            enqueue {
                commitNow()
                // Only the step the toast reports: nothing may have been recorded since.
                if (projectStore?.undoState?.value?.undoCount == count) undo()
            }
        }
    }

    internal suspend fun saveLastPosition(blockKey: Key, offset: Int) {
        if (isReadOnly) return
        val now = deps.clock.now()
        updateApp { app ->
            val activity = app.activity.activity.copy(lastEditedBlockKey = blockKey, lastEditedOffset = offset, lastEditedAt = now)
            app.copy(activity = app.activity.copy(activity = activity))
        }
    }

    private suspend fun saveDraft(d: io.github.lenticualrstone.civilight.feature.script.input.Draft) {
        if (isReadOnly) return
        val encoded = persistedDraft(d)
        updateApp { app -> app.copy(view = app.view.copy(script = app.view.script.copy(draft = encoded))) }
    }

    override fun onCleared() {
        val p = pending
        val store = projectStore
        if (draftOpened && store != null && !store.readOnly) {
            val encoded = persistedDraft(draftFlow.value)
            deps.appScope.launch {
                runCatching { store.updateApp { app -> app.copy(view = app.view.copy(script = app.view.script.copy(draft = encoded))) } }
            }
        }
        val memo = pendingMemo
        if (memo != null && store != null && !store.readOnly) {
            val context = ctx()
            deps.appScope.launch {
                runCatching { store.update(ScriptCopy.MOD_MEMO, memo.mergeKey) { d -> SceneMemo.set(d, memo.sceneKey, memo.text, context) } }
            }
        }
        if (p != null && store != null && !store.readOnly) {
            val context = ctx()
            deps.appScope.launch {
                runCatching {
                    store.update(ScriptCopy.UNDO_TYPING, p.mergeKey) { d ->
                        if (p.optionKey != null) ScriptOps.setOptionRuns(d, p.blockKey, p.optionKey, p.runs, context)
                        else ScriptOps.setRuns(d, p.blockKey, p.runs, context)
                    }
                }
            }
        }
        super.onCleared()
    }

    /*
     * Kept last in the class body: Kotlin runs property initialisers and `init` blocks in textual
     * order, and viewModelScope (Dispatchers.Main.immediate) runs each launch body inside the
     * constructor. Every field these collectors touch (`draftFlow`, `gateOpen`, `selectFlow`, …)
     * must already hold its value, and no later `= false` may reset what they set (0.2.0 UP-05).
     */
    init {
        viewModelScope.launch {
            for (op in ops) {
                busy = true
                try {
                    op()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: ReadOnlyProjectException) {
                    // The example project: nothing changes.
                } finally {
                    busy = false
                    reconcile()
                }
            }
        }
        viewModelScope.launch {
            doc.filterNotNull().collect { d ->
                if (_target.value == null) open(d)
                if (!busy) reconcile(d)
            }
        }
        viewModelScope.launch { content.filterNotNull().collect(::dropHiddenPosition) }
        // The draft follows the gate too: on re-lock a 고치기 of a now-locked block leaves the field (T-U14-25).
        viewModelScope.launch {
            unlocked.collect {
                gateOpen = it
                content.value?.let { c -> reconcileDraft(c.doc, c.unitKey) }
            }
        }
        viewModelScope.launch { content.filterNotNull().collect { c -> reconcileDraft(c.doc, c.unitKey) } }
        // (0.2.0, U-14) The unsent draft survives process death: `_app/view.json › script.draft`, no undo step.
        viewModelScope.launch {
            draftFlow.drop(1).debounce(DRAFT_SAVE_MS).collect { d -> if (draftOpened) saveDraft(d) }
        }
        // Checked blocks that are gone or no longer drawn drop out after every rebuild (SCR-36).
        viewModelScope.launch {
            content.filterNotNull().collect { c ->
                selectFlow.update { SelectRules.reconcile(it, SelectRules.drawnKeys(c.rows)) }
            }
        }
    }

    companion object {
        /** Pause after which typing is written (SCR-17 `[PROPOSED]` coalescing). */
        const val COMMIT_DELAY_MS = 500L

        /** Pause after which the unsent draft is kept in `_app/view.json`. */
        const val DRAFT_SAVE_MS = 400L

        /** Candidate re-scan after the text stops changing (rule 21 §2, ~0.5 s). */
        const val RESCAN_DELAY_MS = 500L

        /** The index while the `용어 후보` layer is off. */
        private val NO_CANDIDATES: CandidateIndex = CandidateIndex.pending()
    }
}
