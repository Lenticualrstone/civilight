package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.marks.Marks
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ChoiceOption
import io.github.lenticualrstone.civilight.model.script.DirectionCue
import io.github.lenticualrstone.civilight.model.script.DiscardReason
import io.github.lenticualrstone.civilight.model.script.DiscardedVoiceId
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.isBlankText
import io.github.lenticualrstone.civilight.model.text.plainText

/*
 * (0.2.0, U-14) The input-bar write paths of the script (SCR-70, SCR-72, SCR-73, SCR-13, SCR-09, SCR-15).
 * Additive: the 0.1.0 Enter / Backspace paths of [BlockOps] stay for their tests; the input bar calls these.
 */

/**
 * The input bar's draft as the domain sees it. [runs] are the field's runs, with a [TextRun.Break] per
 * line break (under `선택지`, one option per line). [speakerKey] is the speaker chip (used for `대사` only).
 * [cues]: the draft's cue chips with `게임 제작 도구` on; null with the switch off, so stored cues are
 * never touched while hidden (scope §8 rule 1).
 */
data class WriteDraft(
    val kind: BlockKind,
    val runs: List<TextRun>,
    val speakerKey: Key? = null,
    val cues: List<DirectionCue>? = null,
    val coSpeakerKeys: List<Key> = emptyList(),
)

/** SCR-70 validation and the draft's lines (pure). */
object DraftRules {
    /** Least option lines a `선택지` send needs (U-14.11). */
    const val MIN_CHOICE_LINES: Int = 2

    /** [runs] split at line breaks; a run never spans two lines. */
    fun lines(runs: List<TextRun>): List<List<TextRun>> {
        val out = mutableListOf<MutableList<TextRun>>(mutableListOf())
        for (run in runs) if (run is TextRun.Break) out += mutableListOf<TextRun>() else out.last() += run
        return out
    }

    /** Non-blank lines, each trimmed: a `선택지` draft's options (SCR-09). */
    fun choiceLines(runs: List<TextRun>): List<List<TextRun>> =
        lines(runs).map { Runs.trimStart(Runs.trimEnd(it)) }.filter { !it.isBlankText() }

    /** Leading and trailing whitespace and line breaks trimmed (SCR-70 send step 1). */
    fun normalized(runs: List<TextRun>): List<TextRun> = Runs.normalize(Runs.trimStart(Runs.trimEnd(runs)))

    /** Whether the draft holds content for its kind (SCR-70 content rule; T-U14-03). */
    fun hasContent(draft: WriteDraft, gameTools: Boolean): Boolean = when (draft.kind) {
        BlockKind.CHOICE -> choiceLines(draft.runs).size >= MIN_CHOICE_LINES
        BlockKind.DIRECTION -> !draft.runs.isBlankText() || (gameTools && !draft.cues.isNullOrEmpty())
        else -> !draft.runs.isBlankText()
    }
}

/** `합치기` check (SCR-72, U-14.19): why it is off, or null when it is on. */
object MergeCheck {
    enum class Reason { SCENE_START, KIND, SPEAKER }

    /** The live block directly before [blockKey] in the same scene, or null (the scene's first block). */
    fun previous(doc: ProjectDoc, blockKey: Key): Block? {
        val block = doc.index.block(blockKey) ?: return null
        val scene = doc.manuscript.liveBlocksOf(block.sceneKey)
        val i = scene.indexOfFirst { it.key == blockKey }
        return if (i > 0) scene[i - 1] else null
    }

    /** Kind first, then speaker; the chip values decide, so it follows the chips live (T-U14-11). */
    fun reason(doc: ProjectDoc, blockKey: Key, chipKind: BlockKind, chipSpeaker: Key?): Reason? {
        val prev = previous(doc, blockKey) ?: return Reason.SCENE_START
        if (prev.kind.known != chipKind) return Reason.KIND
        if (chipKind == BlockKind.DIALOGUE && prev.speakerKey != chipSpeaker) return Reason.SPEAKER
        return null
    }
}

/** SCR-13 conversion check and conversion (pure, JVM). */
object KindChange {
    enum class Blocked { HAS_CUES, CHOICE_LINES, UNKNOWN }

    /** Kinds whose text is runs, `연출` included (SCR-08: the direction text line). */
    val RUN_KINDS: Set<BlockKind> = BlockOps.TEXT_KINDS + BlockKind.DIRECTION

    /** Null when allowed (or the same kind); else why not. */
    fun check(block: Block, to: BlockKind): Blocked? {
        val from = block.kind.known ?: return Blocked.UNKNOWN
        if (from == to) return null
        if (from == BlockKind.DIRECTION && block.cues.isNotEmpty()) return Blocked.HAS_CUES
        if (to == BlockKind.CHOICE && from != BlockKind.CHOICE && DraftRules.choiceLines(block.runs).size < DraftRules.MIN_CHOICE_LINES) {
            return Blocked.CHOICE_LINES
        }
        return null
    }

    /**
     * Converts [blockKey] in place (swipe `종류`, `속성 · 종류 · 바꾸기`): key, number, records and the per-block
     * display name stay; runs carry over between run kinds; text → `선택지` splits at line breaks; `선택지` →
     * text joins the options with one break. Leaving `대사` (or `선택지`) discards fixed voice IDs (`KIND_CHANGED`).
     * Returns [doc] itself when blocked or unchanged.
     */
    fun apply(doc: ProjectDoc, blockKey: Key, to: BlockKind, ctx: EditContext): ProjectDoc {
        val block = doc.index.block(blockKey)?.takeIf { doc.manuscript.isLiveBlock(it.key) } ?: return doc
        if (block.kind.known == to || check(block, to) != null) return doc
        val (converted, discarded) = convert(block, to, ctx, speakerKey = null, choiceRuns = block.runs)
        return doc.replaceBlock(converted).addDiscarded(doc.numberingUnitOf(block), discarded)
    }

    /** The block as [to] (no doc write); [choiceRuns] are split into options when converting to `선택지`. */
    internal fun convert(
        block: Block,
        to: BlockKind,
        ctx: EditContext,
        speakerKey: Key?,
        choiceRuns: List<TextRun>,
    ): Pair<Block, List<DiscardedVoiceId>> {
        val from = block.kind.known
        val discarded = mutableListOf<DiscardedVoiceId>()
        val runs: List<TextRun>
        val options: List<ChoiceOption>
        when {
            to == BlockKind.CHOICE && from == BlockKind.CHOICE -> {
                runs = emptyList(); options = block.options
            }
            to == BlockKind.CHOICE -> {
                val lines = DraftRules.choiceLines(choiceRuns)
                val orders = OrderKeys.evenlySpaced(lines.size)
                runs = emptyList()
                options = lines.mapIndexed { i, l -> ChoiceOption(key = ctx.newKey(), order = orders[i], runs = l) }
            }
            from == BlockKind.CHOICE -> {
                val joined = mutableListOf<TextRun>()
                BlockOps.orderedOptions(block).forEachIndexed { i, o ->
                    if (i > 0) joined += TextRun.Break
                    joined += o.runs
                }
                runs = Runs.normalize(joined)
                options = emptyList()
            }
            else -> {
                runs = block.runs; options = emptyList()
            }
        }
        val leavesDialogue = from == BlockKind.DIALOGUE && to != BlockKind.DIALOGUE
        val leavesChoice = from == BlockKind.CHOICE && to != BlockKind.CHOICE
        if (leavesDialogue) discarded += VoiceIds.discardedOf(block.copy(options = emptyList()), DiscardReason.KIND_CHANGED, ctx.now)
        if (leavesChoice) discarded += VoiceIds.discardedOf(block.copy(voice = null), DiscardReason.KIND_CHANGED, ctx.now)
        val converted = block.copy(
            kind = to.open(),
            speakerKey = if (to == BlockKind.DIALOGUE) (if (from == BlockKind.DIALOGUE) block.speakerKey else speakerKey) else null,
            stage = if (to == BlockKind.DIALOGUE) block.stage else null,
            voice = if (to == BlockKind.DIALOGUE && from == BlockKind.DIALOGUE) block.voice else null,
            runs = if (to in RUN_KINDS) runs else emptyList(),
            cues = if (to == BlockKind.DIRECTION) block.cues else emptyList(),
            options = options,
            recorderSource = if (to == BlockKind.MEMO) block.recorderSource else null,
            todo = if (to == BlockKind.MEMO) block.todo else null,
            updatedAt = ctx.now, updatedBy = ctx.device,
        )
        return converted to discarded
    }
}

/** SCR-09 line ↔ option mapping on `고치기` (T-U14-13). */
object ChoiceLines {
    /**
     * Line i updates the option at order position i (key, stored speaker and voice kept); extra lines become
     * new options; options past the last line are removed and their fixed voice IDs are discarded (`DELETED`).
     */
    fun apply(block: Block, lines: List<List<TextRun>>, ctx: EditContext): Pair<List<ChoiceOption>, List<DiscardedVoiceId>> {
        val old = BlockOps.orderedOptions(block)
        val kept = old.take(lines.size).mapIndexed { i, o -> o.copy(runs = lines[i]) }
        var last = kept.lastOrNull()?.order
        val added = lines.drop(old.size).map { l ->
            val order = OrderKeys.between(last, null)
            last = order
            ChoiceOption(key = ctx.newKey(), order = order, runs = l)
        }
        val removed = old.drop(lines.size)
        val discarded = if (removed.isEmpty()) emptyList() else {
            VoiceIds.discardedOf(block.copy(voice = null, options = removed), DiscardReason.DELETED, ctx.now)
        }
        return (kept + added) to discarded
    }
}

/** Korean particles for the 0.2.0 toasts and replace marks (SCR-72, SCR-75). */
object Particles {
    /** `을` or `를` after a number as read in Korean, by its last digit (T-U14-16). */
    fun objectAfterNumber(n: Int): String = when (n % 10) {
        0, 1, 3, 6, 7, 8 -> "을"
        else -> "를"
    }

    /** Longest first, so `으로` wins over `로` and `이나` over `이`. */
    private val PARTICLES = listOf("으로", "이나", "이랑", "이", "가", "은", "는", "을", "를", "과", "와", "로", "나", "랑", "아", "야")

    /** The particle that starts [following], or null. */
    fun particleAt(following: String): String? = PARTICLES.firstOrNull { following.startsWith(it) }

    /**
     * `조사 확인` (SCR-75): a particle follows, both last syllables are Hangul and their 받침 presence differs
     * (a final ㄹ counts as none before `으로 · 로`). The particle itself is never changed.
     */
    fun needsCheck(old: String, new: String, following: String): Boolean {
        val p = particleAt(following) ?: return false
        val a = old.lastOrNull() ?: return false
        val b = new.lastOrNull() ?: return false
        if (!isSyllable(a) || !isSyllable(b)) return false
        val ro = p == "으로" || p == "로"
        return hasFinal(a, ro) != hasFinal(b, ro)
    }

    private fun isSyllable(c: Char): Boolean = c in '가'..'힣'

    private fun hasFinal(c: Char, rieulIsNone: Boolean): Boolean {
        val jong = (c - '가') % 28
        if (jong == 0) return false
        return !(rieulIsNone && jong == RIEUL)
    }

    private const val RIEUL = 8
}

/** The input bar's document writes (U-14): send, `고치기`, `나누기`, `합치기`. Each is one store update. */
object ScriptWrite {
    data class Sent(val doc: ProjectDoc, val blockKey: Key, val number: Int)

    /**
     * Send (SCR-70): the normalized draft becomes a new block at [position] (`이 뒤에` or the scene end) with
     * the counter's next number; a `대사` takes the speaker chip, never an inherited one. Null when the draft
     * has no content or the position is gone.
     */
    fun send(doc: ProjectDoc, position: BlockPosition, draft: WriteDraft, gameTools: Boolean, ctx: EditContext): Sent? {
        if (!DraftRules.hasContent(draft, gameTools)) return null
        if (BlockOps.resolve(doc, position) == null) return null
        val runs = DraftRules.normalized(draft.runs)
        val options = if (draft.kind == BlockKind.CHOICE) {
            val lines = DraftRules.choiceLines(draft.runs)
            val orders = OrderKeys.evenlySpaced(lines.size)
            lines.mapIndexed { i, l -> ChoiceOption(key = ctx.newKey(), order = orders[i], runs = l) }
        } else null
        val ins = BlockOps.insert(
            doc, position, draft.kind, ctx,
            runs = if (draft.kind in KindChange.RUN_KINDS) runs else emptyList(),
            speakerKey = if (draft.kind == BlockKind.DIALOGUE) draft.speakerKey else null,
            inheritSpeaker = false,
            cues = if (draft.kind == BlockKind.DIRECTION && gameTools) draft.cues.orEmpty() else emptyList(),
            options = options,
        )
        // BlockOps.insert of 0.1.0 keeps runs for text kinds only; the 연출 text line and co-speakers are set here.
        val placed = ins.doc.index.block(ins.blockKey)!!
        val coSpeakers = if (draft.kind == BlockKind.DIALOGUE) draft.coSpeakerKeys.filter { it != draft.speakerKey }.distinct() else emptyList()
        val updated = placed.copy(
            runs = if (draft.kind == BlockKind.DIRECTION) runs else placed.runs,
            coSpeakerKeys = coSpeakers,
        )
        val out = if (updated != placed) ins.doc.replaceBlock(updated) else ins.doc
        return Sent(out, ins.blockKey, ins.number)
    }

    /**
     * `고치기` send (SCR-72, `ScriptEdit.apply`): [blockKey] takes the draft's text, kind and speaker; it keeps
     * its key, number, records and marks. With [WriteDraft.cues] null the stored cues stay exactly. Returns
     * [doc] itself when nothing changed (no undo step) or the block is gone.
     */
    fun edit(doc: ProjectDoc, blockKey: Key, draft: WriteDraft, ctx: EditContext): ProjectDoc =
        (editResult(doc, blockKey, draft, ctx) as? EditResult.Changed)?.doc ?: doc

    /** What a `고치기` send does: [EditResult.Unchanged] ends `고치기` quietly; [EditResult.Refused] keeps the typed text. */
    sealed interface EditResult {
        data class Changed(val doc: ProjectDoc) : EditResult

        data object Unchanged : EditResult

        /** The block is gone or not editable, or the draft cannot be applied (no content, a cue-holding `연출` leaving `연출`). */
        data object Refused : EditResult
    }

    /** [edit] with its three outcomes kept apart, so a refused send never discards the draft (SCR-72). */
    fun editResult(doc: ProjectDoc, blockKey: Key, draft: WriteDraft, ctx: EditContext): EditResult {
        if (!DraftRules.hasContent(draft, gameTools = draft.cues != null)) return EditResult.Refused
        val (updated, discarded) = edited(doc, blockKey, draft, ctx) ?: return EditResult.Refused
        val block = doc.index.block(blockKey)!!
        if (sameContent(block, updated)) return EditResult.Unchanged
        return EditResult.Changed(doc.replaceBlock(updated).addDiscarded(doc.numberingUnitOf(block), discarded))
    }

    private fun edited(doc: ProjectDoc, blockKey: Key, draft: WriteDraft, ctx: EditContext): Pair<Block, List<DiscardedVoiceId>>? {
        val block = doc.index.block(blockKey)?.takeIf { doc.manuscript.isLiveBlock(it.key) } ?: return null
        val from = block.kind.known ?: return null
        val to = draft.kind
        // A 연출 block holding cues never leaves 연출 (SCR-13), even while the cues are hidden.
        if (from == BlockKind.DIRECTION && to != BlockKind.DIRECTION && block.cues.isNotEmpty()) return null
        val (converted, discarded) = if (from == to) block to emptyList() else KindChange.convert(block, to, ctx, draft.speakerKey, draft.runs)
        val extra = mutableListOf<DiscardedVoiceId>()
        val out = when (to) {
            BlockKind.CHOICE -> {
                val (options, gone) = ChoiceLines.apply(if (from == BlockKind.CHOICE) block else converted.copy(options = emptyList()), DraftRules.choiceLines(draft.runs), ctx)
                extra += gone
                converted.copy(options = options)
            }
            else -> converted.copy(
                runs = DraftRules.normalized(draft.runs),
                speakerKey = if (to == BlockKind.DIALOGUE) draft.speakerKey else null,
                coSpeakerKeys = if (to == BlockKind.DIALOGUE) draft.coSpeakerKeys.filter { it != draft.speakerKey }.distinct() else emptyList(),
                cues = if (to == BlockKind.DIRECTION) draft.cues ?: block.cues else converted.cues,
            )
        }
        return out.copy(updatedAt = ctx.now, updatedBy = ctx.device) to (discarded + extra)
    }

    private fun sameContent(a: Block, b: Block): Boolean =
        a.copy(updatedAt = 0L, updatedBy = "") == b.copy(updatedAt = 0L, updatedBy = "")

    data class Split(val doc: ProjectDoc, val frontKey: Key, val backKey: Key, val frontNumber: Int, val backNumber: Int)

    /**
     * `나누기` (SCR-72): [blockKey] is first updated as a `고치기` send with [front]; a new block of the same kind
     * and speaker follows it with [backRuns] (the counter's next number, no records, no cues, an automatic
     * voice ID). Not for `선택지`, and both sides must show text. Null when not possible.
     */
    fun split(doc: ProjectDoc, blockKey: Key, front: WriteDraft, backRuns: List<TextRun>, ctx: EditContext): Split? {
        if (front.kind == BlockKind.CHOICE) return null
        val frontRuns = DraftRules.normalized(front.runs)
        val back = DraftRules.normalized(backRuns)
        if (frontRuns.isBlankText() || back.isBlankText()) return null
        val (updated, discarded) = edited(doc, blockKey, front.copy(runs = frontRuns), ctx) ?: return null
        val old = doc.index.block(blockKey)!!
        val withFront = doc.replaceBlock(updated).addDiscarded(doc.numberingUnitOf(old), discarded)
        val ins = BlockOps.insert(
            withFront, BlockPosition.After(blockKey), front.kind, ctx,
            runs = back, speakerKey = updated.speakerKey, inheritSpeaker = false,
        )
        val placed = ins.doc.index.block(ins.blockKey)!!
        var out = ins.doc.replaceBlock(placed.copy(runs = back, stage = updated.stage, coSpeakerKeys = updated.coSpeakerKeys))
        out = Containers.splitRejections(out, blockKey, ins.blockKey, frontRuns.plainText)
        return Split(out, blockKey, ins.blockKey, updated.number, ins.number)
    }

    data class Merged(val doc: ProjectDoc, val intoKey: Key, val intoNumber: Int, val removedNumber: Int, val toast: String)

    /**
     * `합치기` (SCR-72): [blockKey] (with the draft's text) joins the previous block P of its scene, only when
     * [MergeCheck.reason] is null for the draft's kind and speaker. P keeps key, number, kind, speaker and
     * voice; the block is removed (not trashed), its records and marks move to P unflagged, its fixed voice ID
     * is discarded (`MERGED`).
     */
    fun merge(doc: ProjectDoc, blockKey: Key, draft: WriteDraft, ctx: EditContext): Merged? {
        val later = doc.index.block(blockKey)?.takeIf { doc.manuscript.isLiveBlock(it.key) } ?: return null
        if (MergeCheck.reason(doc, blockKey, draft.kind, draft.speakerKey) != null) return null
        val prev = MergeCheck.previous(doc, blockKey) ?: return null
        val mergedBlock = when (draft.kind) {
            BlockKind.CHOICE -> {
                var last = BlockOps.orderedOptions(prev).lastOrNull()?.order
                val added = DraftRules.choiceLines(draft.runs).map { l ->
                    val order = OrderKeys.between(last, null)
                    last = order
                    ChoiceOption(key = ctx.newKey(), order = order, runs = l)
                }
                prev.copy(options = prev.options + added)
            }
            BlockKind.DIRECTION -> prev.copy(
                runs = Runs.join(prev.runs, DraftRules.normalized(draft.runs)),
                cues = prev.cues + (draft.cues ?: later.cues),
            )
            else -> prev.copy(runs = Runs.join(prev.runs, DraftRules.normalized(draft.runs)))
        }.copy(updatedAt = ctx.now, updatedBy = ctx.device)
        val moved = doc.recordsAnchoredOn(later.key)
        var out = doc.replaceBlock(mergedBlock).removeBlocks(setOf(later.key))
            .mapRecords { r ->
                if ((r.anchor as? Anchor.AtBlock)?.blockKey == later.key && r.deletedAt == null) {
                    r.copy(anchor = Anchor.AtBlock(prev.key), updatedAt = ctx.now, updatedBy = ctx.device)
                } else r
            }
            .addDiscarded(doc.numberingUnitOf(later), VoiceIds.discardedOf(later, DiscardReason.MERGED, ctx.now))
        out = Containers.mergeContainers(out, later.key, prev.key, prev.runs.plainText)
        out = Marks.moveAnchors(out, later.key, prev.key)
        val diamonds = moved.count { !it.isReveal }
        val reveals = moved.count { it.isReveal }
        val base = mergedToast(later.number, prev.number)
        val records = Toasts.recordsTo(diamonds, reveals, Labels.number(prev.number))
        return Merged(out, prev.key, prev.number, later.number, if (records != null) "$base · $records" else base)
    }

    /** `#058을 #045에 합침` (SCR-72). */
    fun mergedToast(removed: Int, into: Int): String =
        "${Labels.number(removed)}${Particles.objectAfterNumber(removed)} ${Labels.number(into)}에 합침"

    /** `#044 나눔 · 뒤 글 → #058` (SCR-72). */
    fun splitToast(front: Int, back: Int): String = "${Labels.number(front)} 나눔 · 뒤 글 → ${Labels.number(back)}"

    /** A block's text as the input field takes it on `고치기`: runs, or a choice's option lines joined by breaks. */
    fun draftRunsOf(block: Block): List<TextRun> = when (block.kind.known) {
        BlockKind.CHOICE -> {
            val out = mutableListOf<TextRun>()
            BlockOps.orderedOptions(block).forEachIndexed { i, o ->
                if (i > 0) out += TextRun.Break
                out += o.runs
            }
            out
        }
        else -> block.runs
    }
}
