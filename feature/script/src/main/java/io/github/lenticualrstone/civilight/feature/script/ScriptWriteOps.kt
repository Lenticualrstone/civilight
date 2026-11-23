package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.actions.BlockFieldActions
import io.github.lenticualrstone.civilight.domain.actions.MarkActions
import io.github.lenticualrstone.civilight.domain.actions.SceneActions
import io.github.lenticualrstone.civilight.domain.actions.VersionActions
import io.github.lenticualrstone.civilight.domain.script.BlockOps
import io.github.lenticualrstone.civilight.domain.script.BlockPosition
import io.github.lenticualrstone.civilight.domain.script.KindChange
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.Replace
import io.github.lenticualrstone.civilight.domain.script.Runs
import io.github.lenticualrstone.civilight.domain.script.ScriptWrite
import io.github.lenticualrstone.civilight.feature.script.input.Draft
import io.github.lenticualrstone.civilight.feature.script.input.DraftEffect
import io.github.lenticualrstone.civilight.feature.script.input.DraftMark
import io.github.lenticualrstone.civilight.feature.script.input.Drafts
import io.github.lenticualrstone.civilight.feature.script.input.WriteCopy
import io.github.lenticualrstone.civilight.feature.script.state.EditorRules
import io.github.lenticualrstone.civilight.feature.script.state.OpenTarget
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRowKeys
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.game.GameTools
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import io.github.lenticualrstone.civilight.model.version.VersionOrigin
import io.github.lenticualrstone.civilight.ui.picker.PickKind
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/*
 * (0.2.0, U-14) The input bar's operations (SCR-70 … SCR-74), the swipe buttons (SCR-30) and the 0.2.0 block
 * fields (SCR-77, SCR-78), 바꾸기 (SCR-75) and versions (SCR-76). Each document write is one store update, so
 * one undo step.
 */

/**
 * The draft of each project while the process lives (leaving and reopening the script, fold). Process death
 * is covered by `_app/view.json › script.draft` inside the project store ([persistedDraft], written by the
 * view model) and by the screen's saved state ([restoreSavedDraft]). Never written outside the project store.
 */
internal object DraftMemory {
    private val drafts = ConcurrentHashMap<Key, Draft>()

    fun get(projectKey: Key): Draft? = drafts[projectKey]

    fun put(projectKey: Key, draft: Draft) {
        drafts[projectKey] = draft
    }
}

internal fun ScriptViewModel.setDraft(d: Draft) {
    draftFlow.value = d
    DraftMemory.put(projectKey, d)
}

/** At open: the draft of this project, else `대사` with the speaker above the end of the target scene (SCR-18). */
internal fun ScriptViewModel.openDraft(d: ProjectDoc, t: OpenTarget) {
    if (draftOpened) return
    draftOpened = true
    val stored = DraftMemory.get(projectKey) ?: Drafts.decode(d.app.view.script.draft)
    if (stored != null) {
        draftFlow.value = stored
        DraftMemory.put(projectKey, stored)
        return
    }
    val speaker = t.sceneKey?.takeIf { d.manuscript.isLiveUnit(it) }?.let { BlockOps.inheritedSpeaker(d, BlockPosition.SceneEnd(it)) }
    draftFlow.value = Draft(kind = BlockKind.DIALOGUE, speakerKey = speaker)
}

/** What `_app/view.json` keeps for [d]: null for an empty draft (the next open starts fresh). */
internal fun persistedDraft(d: Draft): String? =
    if (d.runs.isEmpty() && d.cues.isEmpty() && d.mark == DraftMark.None && d.aside == null) null else Drafts.encode(d)

/** The screen's saved draft after process death (the process memory is empty then). */
fun ScriptViewModel.restoreSavedDraft(encoded: String?) {
    if (DraftMemory.get(projectKey) != null) return
    val saved = Drafts.decode(encoded) ?: return
    setDraft(saved)
}

/** The draft follows the document (SCR-72, SCR-73, SCR-18). */
internal fun ScriptViewModel.reconcileDraft(doc: ProjectDoc, unitKey: Key) {
    val before = draftFlow.value
    val after = Drafts.reconcile(before, doc, unitKey, gateOpen)
    if (after != before) setDraft(after)
}

/** A change of the field's text or caret; returns what the screen opens (the `@` picker). */
fun ScriptViewModel.onDraftText(text: String, selStart: Int, selEnd: Int): DraftEffect {
    if (readOnly) return DraftEffect.NONE
    val (next, effect) = Drafts.textChanged(draftFlow.value, text, selStart, selEnd)
    setDraft(next)
    return effect
}

fun ScriptViewModel.pickChipSpeaker(key: Key?) = setDraft(Drafts.pickSpeaker(draftFlow.value, key))

fun ScriptViewModel.pickChipKind(kind: BlockKind) = setDraft(Drafts.pickKind(draftFlow.value, kind))

fun ScriptViewModel.openDraftParen() = setDraft(Drafts.openParen(draftFlow.value))

/** A picked `@` item goes into the draft as a link run (SCR-55). */
fun ScriptViewModel.insertDraftLink(kind: PickKind, key: Key, text: String) {
    val target = when (kind) {
        PickKind.CHARACTER -> LinkTargetKind.CHARACTER
        PickKind.TERM -> LinkTargetKind.TERM
        PickKind.PLACE -> LinkTargetKind.PLACE
        else -> return
    }
    setDraft(Drafts.insertLink(draftFlow.value, TextRun.Link(Keys.newKey(), LinkTarget(target, key), text)))
}

/** A picked cue becomes a cue chip of the draft (SCR-08, switch on). */
fun ScriptViewModel.addDraftCue(kind: io.github.lenticualrstone.civilight.model.script.CueKind, assetKey: Key?, effect: String?) {
    if (readOnly) return
    val cue = io.github.lenticualrstone.civilight.model.script.DirectionCue(
        key = Keys.newKey(), kind = OpenEnum.of(kind), assetKey = assetKey, effect = effect,
    )
    setDraft(Drafts.addCue(draftFlow.value, cue))
}

fun ScriptViewModel.removeDraftCue(cueKey: Key) = setDraft(Drafts.removeCue(draftFlow.value, cueKey))

/** `✕` of the mark (SCR-72, SCR-73). */
fun ScriptViewModel.closeMark() = setDraft(Drafts.close(draftFlow.value))

// --- Swipe buttons (SCR-30) ---

/** Right swipe `고치기` (SCR-72): the block's text, speaker and kind move into the input. */
fun ScriptViewModel.swipeEdit(blockKey: Key) {
    if (readOnly) return
    val d = latestDoc ?: return
    val block = d.index.block(blockKey)?.takeIf { d.manuscript.isLiveBlock(it.key) && it.kind.known != null } ?: return
    // SCR-55: hidden text never enters the field (the swipe has no `고치기` for such a block either).
    if (io.github.lenticualrstone.civilight.feature.script.state.ScriptRows.editLocked(d, block, gateOpen)) return
    clearFresh()
    setDraft(Drafts.startEdit(draftFlow.value, block))
    editorFlow.update { EditorRules.select(it, blockKey) }
    aboveInput.tryEmit(ScriptRowKeys.block(blockKey))
}

/** Right swipe `이 뒤에` (SCR-73). */
fun ScriptViewModel.swipeAfter(blockKey: Key) {
    if (readOnly) return
    clearFresh()
    setDraft(Drafts.insertAfter(draftFlow.value, blockKey))
    aboveInput.tryEmit(ScriptRowKeys.block(blockKey))
}

/** Right swipe `종류` and `속성 · 종류 · 바꾸기` (SCR-13): converts at once, one undo step. */
fun ScriptViewModel.convertKind(blockKey: Key, kind: BlockKind) {
    if (readOnly) return
    clearFresh()
    enqueue {
        apply(runOp(ScriptCopy.UNDO_KIND) { d -> KindChange.apply(d, blockKey, kind, ctx()).takeIf { it !== d }?.let { ScriptOps.Outcome(it, select = blockKey) } })
    }
}

/** Right swipe `지우기` (SCR-38): deleted at once with its toast. */
fun ScriptViewModel.swipeDelete(blockKey: Key) {
    clearFresh()
    deleteBlocks(listOf(blockKey))
}

// --- Send, 고치기, 나누기, 합치기 (SCR-70, SCR-72) ---

/**
 * `보내기` or the `고치기` button. A send goes after the `이 뒤에` block, else to the end of [targetScene] (the
 * scene in view, SCR-71). An unchanged `고치기` ends with no undo step.
 */
fun ScriptViewModel.sendDraft(targetScene: Key?) {
    if (readOnly) return
    val draft = draftFlow.value
    enqueue {
        // A second tap queued before this ran finds the draft already sent: nothing is sent twice.
        if (!Drafts.stillPending(draft, draftFlow.value)) return@enqueue
        val doc = latestDoc ?: return@enqueue
        val game = GameTools.on(doc)
        when (val m = draft.mark) {
            is DraftMark.Edit -> {
                val write = Drafts.toWrite(draft, game)
                when (ScriptWrite.editResult(doc, m.blockKey, write, ctx())) {
                    // Nothing changed: 고치기 ends with no undo step.
                    ScriptWrite.EditResult.Unchanged -> {
                        if (draftFlow.value == draft) setDraft(Drafts.sent(draft))
                        return@enqueue
                    }
                    // Refused (block gone, not applicable): the typed text stays in the field (SCR-72).
                    ScriptWrite.EditResult.Refused -> return@enqueue
                    is ScriptWrite.EditResult.Changed -> Unit
                }
                val out = runOp(WriteCopy.UNDO_EDIT) { d ->
                    (ScriptWrite.editResult(d, m.blockKey, write, ctx()) as? ScriptWrite.EditResult.Changed)?.let { ScriptOps.Outcome(it.doc) }
                } ?: return@enqueue
                // Text typed while the write was queued stays (only the sent draft ends 고치기).
                if (draftFlow.value.mark == draft.mark) setDraft(Drafts.sent(draftFlow.value))
                editorFlow.update { EditorRules.select(it, m.blockKey) }
                aboveInput.tryEmit(ScriptRowKeys.block(m.blockKey))
                val block = out.doc.index.block(m.blockKey)
                if (block != null) saveLastPosition(m.blockKey, Runs.length(block.runs))
            }
            else -> {
                val position = (m as? DraftMark.After)?.let { BlockPosition.After(it.blockKey) }
                    ?: targetScene?.let { BlockPosition.SceneEnd(it) } ?: return@enqueue
                var created: Key? = null
                runOp(WriteCopy.UNDO_SEND) { d ->
                    ScriptWrite.send(d, position, Drafts.toWrite(draft, game), game, ctx())?.let {
                        created = it.blockKey
                        ScriptOps.Outcome(it.doc, created = it.blockKey)
                    }
                } ?: return@enqueue
                val key = created ?: return@enqueue
                // Only the sent text leaves the field; what was typed meanwhile is kept.
                setDraft(if (draftFlow.value.runs == draft.runs) Drafts.sent(draftFlow.value) else draftFlow.value.copy(mark = DraftMark.None))
                markNew(key, FRESH_SENT)
                editorFlow.update { it.copy(selected = null) }
                aboveInput.tryEmit(ScriptRowKeys.block(key))
                val block = latestDoc?.index?.block(key)
                if (block != null) saveLastPosition(key, Runs.length(block.runs))
            }
        }
    }
}

/** `나누기` at the input caret (SCR-72). */
fun ScriptViewModel.splitDraft() {
    if (readOnly) return
    val draft = draftFlow.value
    val m = draft.mark as? DraftMark.Edit ?: return
    if (!Drafts.canSplit(draft)) return
    enqueue {
        if (!Drafts.stillPending(draft, draftFlow.value)) return@enqueue
        val doc = latestDoc ?: return@enqueue
        val game = GameTools.on(doc)
        val (front, back) = Drafts.splitParts(draft)
        var result: ScriptWrite.Split? = null
        runOp(ScriptCopy.UNDO_SPLIT) { d ->
            ScriptWrite.split(d, m.blockKey, Drafts.toWrite(draft, game).copy(runs = front), back, ctx())?.let {
                result = it
                ScriptOps.Outcome(it.doc, created = it.backKey)
            }
        } ?: return@enqueue
        val r = result ?: return@enqueue
        setDraft(Drafts.sent(draftFlow.value))
        editorFlow.update { EditorRules.select(it, m.blockKey) }
        markNew(r.backKey, FRESH_SPLIT)
        aboveInput.tryEmit(ScriptRowKeys.block(r.backKey))
        showUndoToast(ScriptWrite.splitToast(r.frontNumber, r.backNumber))
        val block = latestDoc?.index?.block(r.backKey)
        if (block != null) saveLastPosition(r.backKey, Runs.length(block.runs))
    }
}

/** `합치기` into the previous block (SCR-72; only same kind and speaker). */
fun ScriptViewModel.mergeDraft() {
    if (readOnly) return
    val draft = draftFlow.value
    val m = draft.mark as? DraftMark.Edit ?: return
    enqueue {
        if (!Drafts.stillPending(draft, draftFlow.value)) return@enqueue
        val doc = latestDoc ?: return@enqueue
        val game = GameTools.on(doc)
        var result: ScriptWrite.Merged? = null
        runOp(ScriptCopy.UNDO_MERGE) { d ->
            ScriptWrite.merge(d, m.blockKey, Drafts.toWrite(draft, game), ctx())?.let {
                result = it
                ScriptOps.Outcome(it.doc)
            }
        } ?: return@enqueue
        val r = result ?: return@enqueue
        setDraft(Drafts.sent(draftFlow.value))
        editorFlow.update { it.copy(selected = null) }
        markNew(r.intoKey, FRESH_MERGED)
        aboveInput.tryEmit(ScriptRowKeys.block(r.intoKey))
        showUndoToast(r.toast)
        val block = latestDoc?.index?.block(r.intoKey)
        if (block != null) saveLastPosition(r.intoKey, Runs.length(block.runs))
    }
}

/**
 * `다음 씬` (SCR-74): a new scene directly after [targetScene] at once, through `SceneActions` with the
 * `다음 씬 만들기` defaults; in an empty episode, 씬 1 with no place and no time. The draft is kept.
 */
fun ScriptViewModel.nextScene(targetScene: Key?) {
    if (readOnly) return
    val unit = unitKey ?: return
    enqueue {
        // SCR-74 [DEFAULT]: no new scene while the target (or the scene just made after it) has no block. The bar
        // dims the button by the same rule; a tap that still lands (the document moved) shows that empty scene.
        latestDoc?.let { d ->
            NextScene.emptyAfter(d, unit, projectKey, targetScene, lastNextScene)?.let { empty ->
                aboveInput.tryEmit(ScriptRowKeys.head(empty))
                placeFlash.tryEmit(Unit)
                return@enqueue
            }
        }
        var created: Key? = null
        runOp(WriteCopy.UNDO_NEXT_SCENE) { d ->
            NextScene.create(d, unit, projectKey, targetScene, actionCtx())?.let { (out, key) ->
                created = key
                ScriptOps.Outcome(out)
            }
        } ?: return@enqueue
        val key = created ?: return@enqueue
        lastNextScene = key
        aboveInput.tryEmit(ScriptRowKeys.head(key))
    }
}

/** The document step of `다음 씬` (SCR-74; pure, JVM tested). */
object NextScene {
    /**
     * The empty scene a `다음 씬` tap would pile onto, or null when the tap may create (SCR-74 `[DEFAULT]`): the
     * live [targetScene] with no live block, or [lastCreated] (made by the previous tap right after the same
     * target) still empty.
     * An empty episode (no live scene in [unitKey]) may always create its first scene.
     */
    fun emptyAfter(doc: ProjectDoc, unitKey: Key, projectKey: Key, targetScene: Key?, lastCreated: Key?): Key? {
        fun emptyLive(key: Key?): Boolean = key != null && doc.manuscript.isLiveUnit(key) && doc.manuscript.liveBlocksOf(key).isEmpty()
        val anyScene = doc.manuscript.liveScenes.any { (doc.index.numberingUnitKeyOfScene(it.key) ?: projectKey) == unitKey }
        if (!anyScene) return null
        val order = doc.manuscript.liveScenes.map { it.key }
        val afterTarget = targetScene?.let { order.getOrNull(order.indexOf(it) + 1).takeIf { _ -> it in order } }
        return when {
            emptyLive(targetScene) -> targetScene
            lastCreated != null && afterTarget == lastCreated && emptyLive(lastCreated) -> lastCreated
            else -> null
        }
    }

    /**
     * A new scene directly after [targetScene] with the `다음 씬 만들기` defaults (its place, `같은 때`, the
     * project's transition mode); with no live target (an empty episode) the episode's first scene, with no
     * place and no time. [unitKey] is the open episode ([projectKey] in a one-level structure).
     */
    fun create(
        doc: ProjectDoc,
        unitKey: Key,
        projectKey: Key,
        targetScene: Key?,
        ctx: io.github.lenticualrstone.civilight.domain.actions.ActionContext,
    ): Pair<ProjectDoc, Key>? {
        val scene = targetScene?.let { doc.index.unit(it) }?.takeIf { doc.manuscript.isLiveUnit(it.key) }
        return if (scene != null) {
            val pre = SceneActions.prefillAfter(doc, scene.key)
            SceneActions.create(doc, scene.parentKey, scene.key, ctx, place = pre.place, time = pre.time, transitionMode = pre.transitionMode)
        } else {
            val parent = unitKey.takeIf { it != projectKey }
            val last = doc.index.children(parent).lastOrNull()?.key
            SceneActions.create(doc, parent, last, ctx)
        }
    }
}

/**
 * Whether `다음 씬` would be on by SCR-74's `[DEFAULT]` (off while the target scene has no live block, except in an
 * empty episode). The screen dims by [NextScene.emptyAfter], which also covers the scene the last tap made.
 */
fun nextSceneEnabled(doc: ProjectDoc, targetScene: Key?): Boolean =
    targetScene == null || doc.manuscript.liveBlocksOf(targetScene).isNotEmpty()

internal fun ScriptViewModel.markNew(key: Key, tag: String) {
    freshFlow.value = mapOf(key to tag)
}

internal const val FRESH_SENT = WriteCopy.FRESH_SENT
internal const val FRESH_SPLIT = WriteCopy.FRESH_SPLIT
internal const val FRESH_MERGED = WriteCopy.FRESH_MERGED

// --- 속성 rows (SCR-77, SCR-78) ---

/** `이 블록만 · 표시 이름` (N-43): trimmed, blank = none; one undo step. */
fun ScriptViewModel.setBlockDisplayName(blockKey: Key, value: String) {
    if (readOnly) return
    enqueue {
        runOp(WriteCopy.UNDO_NAME) { d ->
            BlockFieldActions.setDisplayNameOverride(d, blockKey, value, actionCtx()).takeIf { it !== d }?.let { ScriptOps.Outcome(it) }
        }
    }
}

/** `할 일` on a memo (N-41). */
fun ScriptViewModel.setBlockTodo(blockKey: Key, on: Boolean) {
    if (readOnly) return
    enqueue { runOp(WriteCopy.UNDO_TODO) { d -> ScriptOps.Outcome(BlockFieldActions.setTodo(d, blockKey, on, actionCtx())) } }
}

/** The memo to-do checked done (`끝냄`) or open again (N-41). */
fun ScriptViewModel.setBlockTodoDone(blockKey: Key, done: Boolean) {
    if (readOnly) return
    enqueue { runOp(WriteCopy.UNDO_TODO) { d -> ScriptOps.Outcome(BlockFieldActions.setTodoDone(d, blockKey, done, actionCtx())) } }
}

/** `복선 심기` (N-41): a foreshadowing planted on [blockKey]. */
fun ScriptViewModel.plantMark(blockKey: Key, name: String, payoffUnitKey: Key?) {
    if (readOnly) return
    enqueue { runOp(WriteCopy.UNDO_MARK) { d -> MarkActions.plant(d, blockKey, name, payoffUnitKey, actionCtx())?.first?.let { ScriptOps.Outcome(it) } } }
}

/** `다시 언급` (N-41). */
fun ScriptViewModel.mentionMark(foreshadowKey: Key, blockKey: Key) {
    if (readOnly) return
    enqueue { runOp(WriteCopy.UNDO_MARK) { d -> ScriptOps.Outcome(MarkActions.mention(d, foreshadowKey, blockKey, actionCtx())) } }
}

/** `거두기` (N-41). */
fun ScriptViewModel.payoffMark(foreshadowKey: Key, blockKey: Key) {
    if (readOnly) return
    enqueue { runOp(WriteCopy.UNDO_MARK) { d -> ScriptOps.Outcome(MarkActions.payoff(d, foreshadowKey, blockKey, actionCtx())) } }
}

// --- 바꾸기 (SCR-75) ---

/** Replaces exactly [hits] (one undo step) and shows `{k}곳 바꿈` with `되돌리기`. */
fun ScriptViewModel.replaceHits(hits: List<Replace.Hit>, query: String, with: String) {
    if (readOnly || hits.isEmpty()) return
    val unlocked = gateOpen
    enqueue {
        var count = 0
        runOp(WriteCopy.UNDO_REPLACE) { d ->
            val r = Replace.applyCounted(d, hits, query, with, ctx(), unlocked)
            count = r.count
            r.doc.takeIf { it !== d && r.count > 0 }?.let { ScriptOps.Outcome(it) }
        } ?: return@enqueue
        // The toast counts the places that really changed.
        showUndoToast(WriteCopy.replaced(count))
    }
}

// --- Versions (SCR-76) ---

/** `판 남기기` of the open episode; [name] blank takes `11월 23일 · 초고`. */
fun ScriptViewModel.keepVersion(name: String) {
    if (readOnly) return
    val unit = unitKey ?: return
    enqueue {
        runOp(WriteCopy.KEEP_VERSION) { d -> ScriptOps.Outcome(VersionActions.capture(d, unit, name, VersionOrigin.MANUAL, actionCtx()).first) }
    }
}

/** `이 판 글로` on one block (one undo step). */
fun ScriptViewModel.restoreVersionText(versionKey: Key, blockKey: Key) {
    if (readOnly) return
    val unit = unitKey ?: return
    enqueue {
        runOp(WriteCopy.UNDO_VERSION_TEXT) { d ->
            VersionActions.restoreBlockText(d, unit, versionKey, blockKey, actionCtx())?.takeIf { it !== d }?.let { ScriptOps.Outcome(it) }
        } ?: return@enqueue
        latestDoc?.index?.block(blockKey)?.let { showUndoToast("${Labels.number(it.number)} · ${WriteCopy.UNDO_VERSION_TEXT}") }
    }
}

/** The default name of a new version (`11월 23일 · 초고`). */
fun ScriptViewModel.defaultVersionName(): String = VersionActions.defaultName(ctx().today, VersionOrigin.MANUAL)

/** Plain text of the draft (tests and the read-only check). */
internal val Draft.plain: String get() = runs.plainText
