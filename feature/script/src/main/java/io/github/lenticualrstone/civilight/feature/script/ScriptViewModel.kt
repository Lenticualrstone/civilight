package io.github.lenticualrstone.civilight.feature.script

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.CharacterActions
import io.github.lenticualrstone.civilight.domain.script.EditContext
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.script.Manuscript
import io.github.lenticualrstone.civilight.domain.script.Runs
import io.github.lenticualrstone.civilight.domain.script.SceneText
import io.github.lenticualrstone.civilight.domain.script.SceneTimeSource
import io.github.lenticualrstone.civilight.domain.script.StoryTimes
import io.github.lenticualrstone.civilight.feature.script.editor.FieldChange
import io.github.lenticualrstone.civilight.feature.script.editor.FieldDiff
import io.github.lenticualrstone.civilight.feature.script.editor.RunEdit
import io.github.lenticualrstone.civilight.feature.script.state.EditorRules
import io.github.lenticualrstone.civilight.feature.script.state.EditorState
import io.github.lenticualrstone.civilight.feature.script.state.InsertAt
import io.github.lenticualrstone.civilight.feature.script.state.OpenTarget
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRow
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRows
import io.github.lenticualrstone.civilight.feature.script.state.ScriptTarget
import io.github.lenticualrstone.civilight.feature.script.state.SpeakerTarget
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.app.ProjectAppState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
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
@OptIn(ExperimentalCoroutinesApi::class)
class ScriptViewModel(
    private val route: Script,
    private val deps: FeatureDeps,
) : ProjectViewModel(route.projectKey, deps.projects) {

    private val _editor = MutableStateFlow(EditorState())
    val editor: StateFlow<EditorState> = _editor.asStateFlow()

    private val _target = MutableStateFlow<OpenTarget?>(null)

    /** Where the screen opened; the UI scrolls there once. */
    val target: StateFlow<OpenTarget?> = _target.asStateFlow()

    private val unlocked = deps.unlock.state.map { it.itemsOpen(route.projectKey) }.distinctUntilChanged()

    val content: StateFlow<ScriptContent?> = combine(
        state.filterIsInstance<ProjectLoad.Ready>(),
        _target.filterNotNull(),
        unlocked,
    ) { ready, target, open ->
        val doc = ready.doc
        val time = StoryTimes.source(doc)
        val view = doc.app.view.script
        ScriptContent(
            doc, target.unitKey,
            ScriptRows.build(doc, target.unitKey, time, view.layers, view.preview, open, ready.readOnly, deps.clock.zone),
            time, ready.readOnly,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The text field of the block with the caret: sentinel + raw text (see [FieldDiff]). */
    var fieldValue by mutableStateOf(TextFieldValue(FieldDiff.fieldText(""), TextRange(1)), neverEqualPolicy())
        private set

    /** Runs of the block with the caret, kept equal to the field's raw text. */
    var editRuns by mutableStateOf<List<TextRun>>(emptyList())
        private set

    private data class PendingText(val blockKey: Key, val runs: List<TextRun>)

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

    private val _entered = MutableStateFlow<Key?>(null)

    /** The block that was just created; it enters from 8 dp below (SCR-12, SCR-67). */
    val entered: StateFlow<Key?> = _entered.asStateFlow()

    /** The enter motion of [blockKey] has played. */
    fun enteredShown(blockKey: Key) {
        _entered.compareAndSet(blockKey, null)
    }

    private val _scrollTo = MutableSharedFlow<String>(extraBufferCapacity = 8)

    /** Row keys to bring into view (new blocks, caret moves after an operation). */
    val scrollTo: SharedFlow<String> = _scrollTo.asSharedFlow()

    private val ops = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private var busy = false

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
    }

    // --- Reading ---

    /** The newest document (the store's, ahead of [doc] right after an update). */
    private val latestDoc: ProjectDoc? get() = projectStore?.doc?.value ?: doc.value

    val readOnly: Boolean get() = isReadOnly

    /** Caret in raw offsets of [editRuns]. */
    val rawCaret: Int get() = FieldDiff.rawOffset(fieldValue.selection.end).coerceAtMost(Runs.length(editRuns))

    /** Whose speaker the pill, the toolbar and the picker show and change. */
    fun speakerTarget(): SpeakerTarget? = latestDoc?.let { ScriptOps.speakerTarget(it, _editor.value) }

    /** The `새 씬` sheet for the current position (SCR-49 routing). */
    fun newSceneSheet(headerScene: Key?): SheetKey? {
        val d = latestDoc ?: return null
        val unit = _target.value?.unitKey ?: return null
        return ScriptOps.newSceneSheet(d, route.projectKey, _editor.value.selected, headerScene ?: _editor.value.virtualScene, unit)
    }

    private fun actionCtx(): ActionContext = ActionContext(
        now = deps.clock.now(),
        zone = deps.clock.zone,
        device = deps.stores.deviceId.value,
        newKey = Keys::newKey,
    )

    private fun ctx(): EditContext = EditContext(
        now = deps.clock.now(),
        zone = deps.clock.zone,
        device = deps.stores.deviceId.value,
        newKey = Keys::newKey,
    )

    // --- Opening and following the document ---

    private fun open(d: ProjectDoc) {
        val t = ScriptTarget.resolve(d, route.sceneKey, route.blockKey, route.caretAtEnd)
        _target.value = t
        val block = t.blockKey ?: return
        if (t.caretOffset != null && !isReadOnly && t.caretOffset >= 0) {
            startEditingIn(d, block, t.caretOffset, hint = null)
        } else {
            _editor.value = EditorRules.select(_editor.value, block)
        }
    }

    private fun reconcile(d: ProjectDoc? = latestDoc) {
        d ?: return
        val before = _editor.value
        val after = EditorRules.reconcile(before, d)
        if (after != before) _editor.value = after
        val key = after.caretBlock ?: return
        val block = d.index.block(key) ?: return
        if (pending == null && block.runs != committedRuns) {
            // Changed elsewhere (undo, another screen): the field follows the document.
            editRuns = block.runs
            committedRuns = block.runs
            seedField(block.runs, rawCaret)
        }
    }

    private fun enqueue(op: suspend () -> Unit) {
        ops.trySend(op)
    }

    /** Runs [op] inside one store update; null when nothing applies or the project is read-only. */
    private suspend fun runOp(label: String, op: (ProjectDoc) -> ScriptOps.Outcome?): ScriptOps.Outcome? {
        var outcome: ScriptOps.Outcome? = null
        val result = update(label) { d -> op(d)?.also { outcome = it }?.doc ?: d }
        return if (result == EditResult.APPLIED) outcome else null
    }

    // --- Caret and selection ---

    /** A tap on a block: the caret goes there for a text block (SCR-14), else the block is selected. */
    fun tapBlock(blockKey: Key, isText: Boolean, rawOffset: Int?) {
        if (isReadOnly || !isText) {
            flushPending()
            _editor.update { EditorRules.select(it, blockKey) }
            return
        }
        flushPending()
        enqueue {
            val d = latestDoc ?: return@enqueue
            val block = d.index.block(blockKey)?.takeIf { d.manuscript.isLiveBlock(it.key) } ?: return@enqueue
            startEditingIn(d, blockKey, rawOffset ?: Runs.length(block.runs), hint = _editor.value.inheritedHint?.takeIf { it == blockKey })
        }
    }

    /** A tap on the virtual first line of an empty scene. */
    fun tapEmptyScene(sceneKey: Key) {
        if (isReadOnly) return
        flushPending()
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
    fun onFieldFocusLost(rowKey: String) {
        if (_editor.value.caretRowKey == rowKey) done()
    }

    /** A jump from the command field (SCR-68 `#043`): the block is selected, no caret. */
    fun selectBlock(blockKey: Key) {
        flushPending()
        _editor.update { EditorRules.select(it, blockKey) }
    }

    private fun startEditingIn(d: ProjectDoc, blockKey: Key, rawOffset: Int, hint: Key?) {
        val block = d.index.block(blockKey) ?: return
        editRuns = block.runs
        committedRuns = block.runs
        seedField(block.runs, rawOffset)
        _editor.update { EditorRules.edit(it, blockKey).copy(inheritedHint = hint) }
    }

    private fun seedField(runs: List<TextRun>, rawCaret: Int) {
        val raw = runs.plainText
        fieldValue = TextFieldValue(FieldDiff.fieldText(raw), TextRange(FieldDiff.fieldOffset(rawCaret.coerceIn(0, raw.length))))
    }

    // --- Typing (SCR-14, SCR-15) ---

    /** Every change of the caret block's text field. */
    fun onFieldChange(value: TextFieldValue) {
        val state = _editor.value
        if (!state.editing || isReadOnly) return
        val oldRaw = editRuns.plainText
        when (val change = FieldDiff.classify(oldRaw, value.text, value.selection.end, state.parenMode)) {
            FieldChange.None -> {
                val moved = FieldDiff.rawOffset(value.selection.end) != rawCaret
                fieldValue = value.withCaretAfterSentinel()
                if (moved && state.parenMode) _editor.update { it.copy(parenMode = false) }
            }
            is FieldChange.Text -> applyText(change, value, state)
            is FieldChange.Enter -> {
                fieldValue = fieldValue
                enter(change.at)
            }
            FieldChange.BackspaceAtStart -> {
                fieldValue = fieldValue
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
        pending = PendingText(key, editRuns)
        hasPending.value = true
        commitJob?.cancel()
        commitJob = viewModelScope.launch {
            delay(COMMIT_DELAY_MS)
            enqueue { commitNow() }
        }
    }

    /** Writes typed runs to the store (one undo step per block and pause). */
    private suspend fun commitNow(label: String = ScriptCopy.UNDO_TYPING, merge: Boolean = true) {
        commitJob?.cancel()
        val p = pending ?: return
        pending = null
        hasPending.value = false
        update(label, if (merge) "type:${p.blockKey}" else null) { d -> ScriptOps.setRuns(d, p.blockKey, p.runs, ctx()) }
        if (_editor.value.selected == p.blockKey) committedRuns = p.runs
        saveLastPosition(p.blockKey, if (_editor.value.selected == p.blockKey) rawCaret else Runs.length(p.runs))
    }

    /** Queues the typed text of the block being left. */
    private fun flushPending() {
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
        _entered.value = key
        committedRuns = runs
        _editor.update { it.copy(selected = key, virtualScene = null, editing = true, pendingSpeaker = null, inheritedHint = null) }
        if (editRuns != runs) {
            pending = PendingText(key, editRuns)
            hasPending.value = true
            enqueue { commitNow() }
        }
        _scrollTo.tryEmit("block:$key")
    }

    private fun enter(at: Int) {
        enqueue {
            commitNow()
            val key = _editor.value.caretBlock ?: return@enqueue
            val atEnd = at >= Runs.length(editRuns)
            val label = if (atEnd) ScriptCopy.UNDO_ADD_BLOCK else ScriptCopy.UNDO_SPLIT
            apply(runOp(label) { d -> ScriptOps.enter(d, key, at, ctx()) })
        }
    }

    private fun backspaceAtStart() {
        enqueue {
            commitNow()
            val key = _editor.value.caretBlock ?: return@enqueue
            val empty = editRuns.isEmpty()
            val label = if (empty) ScriptCopy.UNDO_DELETE else ScriptCopy.UNDO_MERGE
            apply(runOp(label) { d -> ScriptOps.backspaceAtStart(d, key, ctx()) })
        }
    }

    /** Moves the caret or selection after an operation and shows its toast. */
    private fun apply(outcome: ScriptOps.Outcome?) {
        outcome ?: return
        outcome.created?.let { _entered.value = it }
        val caret = outcome.caret
        val select = outcome.select
        when {
            caret != null -> {
                startEditingIn(outcome.doc, caret.blockKey, caret.offset, hint = caret.blockKey.takeIf { outcome.inherited })
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
            val outcome = runOp(ScriptCopy.UNDO_ADD_BLOCK) { doc -> ScriptOps.insert(doc, at, kind, speaker, ctx()) } ?: return@enqueue
            if (speaker != null) _editor.update { it.copy(pendingSpeaker = null) }
            updateApp { app -> app.withLayers(Manuscript.layersAfterInsert(app.view.script.layers, kind)) }
            apply(outcome)
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
        pending = PendingText(key, runs)
        hasPending.value = true
        enqueue { commitNow(ScriptCopy.UNDO_LINE_BREAK, merge = false) }
    }

    /** Toolbar `( )` (SCR-16): typed text goes into a paren run at the caret. */
    fun openParen() {
        if (_editor.value.editing) _editor.update { it.copy(parenMode = true) }
    }

    /**
     * `@` picker result (SCR-16 → SCR-55): a link run holding the picked text at raw [rawOffset] of
     * [blockKey] (where the caret was when `@` was tapped); the caret goes after it.
     */
    fun insertLink(blockKey: Key, rawOffset: Int, kind: PickKind, key: Key, text: String) {
        if (isReadOnly || text.isEmpty()) return
        val target = when (kind) {
            PickKind.CHARACTER -> LinkTargetKind.CHARACTER
            PickKind.TERM -> LinkTargetKind.TERM
            PickKind.PLACE -> LinkTargetKind.PLACE
            else -> return
        }
        val link = TextRun.Link(Keys.newKey(), LinkTarget(target, key), text)
        if (_editor.value.caretBlock == blockKey) {
            insertRunAt(blockKey, rawOffset, link)
            return
        }
        enqueue {
            commitNow()
            val d = latestDoc ?: return@enqueue
            val block = d.index.block(blockKey)?.takeIf { d.manuscript.isLiveBlock(it.key) } ?: return@enqueue
            startEditingIn(d, blockKey, rawOffset.coerceIn(0, Runs.length(block.runs)), hint = null)
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

    /** Writes pending text now (screen stops). */
    fun flushNow() = flushPending()

    private fun showUndoToast(text: String) {
        val count = projectStore?.undoState?.value?.undoCount ?: return
        deps.toasts.show(text) {
            enqueue {
                commitNow()
                // Only the step the toast reports: nothing may have been recorded since.
                if (projectStore?.undoState?.value?.undoCount == count) undo()
            }
        }
    }

    private suspend fun saveLastPosition(blockKey: Key, offset: Int) {
        if (isReadOnly) return
        val now = deps.clock.now()
        updateApp { app ->
            val activity = app.activity.activity.copy(lastEditedBlockKey = blockKey, lastEditedOffset = offset, lastEditedAt = now)
            app.copy(activity = app.activity.copy(activity = activity))
        }
    }

    override fun onCleared() {
        val p = pending
        val store = projectStore
        if (p != null && store != null && !store.readOnly) {
            val context = ctx()
            deps.appScope.launch {
                runCatching { store.update(ScriptCopy.UNDO_TYPING, "type:${p.blockKey}") { d -> ScriptOps.setRuns(d, p.blockKey, p.runs, context) } }
            }
        }
        super.onCleared()
    }

    private fun ProjectAppState.withLayers(layers: io.github.lenticualrstone.civilight.model.script.ScriptLayers): ProjectAppState =
        copy(view = view.copy(script = view.script.copy(layers = layers)))

    companion object {
        /** Pause after which typing is written (SCR-17 `[PROPOSED]` coalescing). */
        const val COMMIT_DELAY_MS = 500L
    }
}
