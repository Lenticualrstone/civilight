package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ChoiceOption
import io.github.lenticualrstone.civilight.model.script.DirectionCue
import io.github.lenticualrstone.civilight.model.script.DiscardReason
import io.github.lenticualrstone.civilight.model.script.StagePosition
import io.github.lenticualrstone.civilight.model.text.RecorderSource
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.isBlankText
import io.github.lenticualrstone.civilight.model.text.plainText

/** Where a block goes. */
sealed interface BlockPosition {
    /** Directly below a live block. */
    data class After(val blockKey: Key) : BlockPosition

    /** First in a scene. */
    data class SceneStart(val sceneKey: Key) : BlockPosition

    /** Last in a scene. */
    data class SceneEnd(val sceneKey: Key) : BlockPosition
}

/**
 * Insert, Enter/split, Backspace/merge, kind and speaker changes (SCR-12…20). Every function
 * returns the new doc; nothing is changed in place.
 */
object BlockOps {
    /** Kinds whose text is runs. */
    val TEXT_KINDS: Set<BlockKind> = setOf(BlockKind.DIALOGUE, BlockKind.NARRATION, BlockKind.SYSTEM, BlockKind.MEMO)

    data class Inserted(val doc: ProjectDoc, val blockKey: Key, val number: Int, val inheritedSpeaker: Boolean)

    /** Scene and preceding live block of a position (null when the position is invalid). */
    internal fun resolve(doc: ProjectDoc, position: BlockPosition): Pair<Key, Key?>? = when (position) {
        is BlockPosition.After -> doc.index.block(position.blockKey)
            ?.takeIf { doc.manuscript.isLiveBlock(it.key) }?.let { it.sceneKey to it.key }
        is BlockPosition.SceneStart -> position.sceneKey.takeIf { doc.manuscript.isLiveUnit(it) }?.let { it to null }
        is BlockPosition.SceneEnd -> position.sceneKey.takeIf { doc.manuscript.isLiveUnit(it) }
            ?.let { it to doc.manuscript.liveBlocksOf(it).lastOrNull()?.key }
    }

    /**
     * `직전 화자` (SCR-18): the speaker of the nearest live dialogue block above the position, inside
     * the same numbering unit (`[PROPOSED]`); null when there is none.
     */
    fun inheritedSpeaker(doc: ProjectDoc, position: BlockPosition): Key? {
        val (sceneKey, prevKey) = resolve(doc, position) ?: return null
        val unit = doc.numberingUnitOfScene(sceneKey)
        val blocks = Numbering.liveBlocksOfUnit(doc, unit)
        val above = if (prevKey != null) {
            blocks.subList(0, blocks.indexOfFirst { it.key == prevKey } + 1)
        } else {
            val sceneIndex = doc.manuscript.sceneIndex(sceneKey) ?: return null
            blocks.filter { (doc.manuscript.sceneIndex(it.sceneKey) ?: Int.MAX_VALUE) < sceneIndex }
        }
        return above.lastOrNull { it.kind.known == BlockKind.DIALOGUE && it.speakerKey != null }?.speakerKey
    }

    /**
     * Inserts a new block (SCR-12): new key, the numbering unit's next number, automatic voice. A
     * dialogue block takes the inherited speaker unless [speakerKey] is given.
     */
    fun insert(
        doc: ProjectDoc,
        position: BlockPosition,
        kind: BlockKind,
        ctx: EditContext,
        runs: List<TextRun> = emptyList(),
        speakerKey: Key? = null,
        inheritSpeaker: Boolean = true,
        cues: List<DirectionCue> = emptyList(),
        options: List<ChoiceOption>? = null,
        recorderSource: RecorderSource? = null,
    ): Inserted {
        val (sceneKey, prevKey) = requireNotNull(resolve(doc, position)) { "not a live position: $position" }
        val inherited = if (kind == BlockKind.DIALOGUE && speakerKey == null && inheritSpeaker) inheritedSpeaker(doc, position) else null
        val (numbered, numbers) = Numbering.issue(doc, doc.numberingUnitOfScene(sceneKey), 1)
        val block = Block(
            key = ctx.newKey(), sceneKey = sceneKey, order = doc.orderKeysAfter(sceneKey, prevKey, 1).single(),
            number = numbers.single(), kind = kind.open(),
            speakerKey = if (kind == BlockKind.DIALOGUE) speakerKey ?: inherited else null,
            runs = if (kind in TEXT_KINDS) Runs.normalize(runs) else emptyList(),
            cues = if (kind == BlockKind.DIRECTION) cues else emptyList(),
            options = if (kind == BlockKind.CHOICE) options ?: emptyOptions(ctx) else emptyList(),
            recorderSource = if (kind == BlockKind.MEMO) recorderSource else null,
            createdAt = ctx.now, updatedAt = ctx.now, updatedBy = ctx.device,
        )
        return Inserted(numbered.addBlocks(listOf(block)), block.key, block.number, inherited != null)
    }

    /** Two empty options (a choice block keeps at least 2). */
    fun emptyOptions(ctx: EditContext): List<ChoiceOption> {
        val orders = OrderKeys.evenlySpaced(2)
        return orders.map { ChoiceOption(key = ctx.newKey(), order = it) }
    }

    // --- Enter (SCR-15) ---

    sealed interface EnterResult {
        val doc: ProjectDoc

        /** Enter at the end: a new block of the same kind (and speaker) below. */
        data class NewBlock(override val doc: ProjectDoc, val blockKey: Key) : EnterResult

        /** Enter in the middle: the text after the caret became a new block. */
        data class Split(override val doc: ProjectDoc, val newBlockKey: Key) : EnterResult

        /** Not a text block (choice options use [addOption]). */
        data class Nothing(override val doc: ProjectDoc) : EnterResult
    }

    /** Enter at caret [offset] in a text block. */
    fun enter(doc: ProjectDoc, blockKey: Key, offset: Int, ctx: EditContext): EnterResult {
        val block = doc.index.block(blockKey)?.takeIf { doc.manuscript.isLiveBlock(it.key) } ?: return EnterResult.Nothing(doc)
        if (block.kind.known !in TEXT_KINDS) return EnterResult.Nothing(doc)
        if (offset >= Runs.length(block.runs)) {
            val ins = insert(
                doc, BlockPosition.After(blockKey), block.kind.known!!, ctx,
                speakerKey = block.speakerKey, inheritSpeaker = false,
            )
            val withStage = block.stage?.let { s -> ins.doc.index.block(ins.blockKey)!!.copy(stage = s) }
            return EnterResult.NewBlock(withStage?.let(ins.doc::replaceBlock) ?: ins.doc, ins.blockKey)
        }
        return split(doc, blockKey, offset, ctx)
    }

    /**
     * Split at [offset] (Enter mid-line): the text before stays (key, number, voice, records); the
     * text after becomes a new block of the same kind and speaker with a new key, the next number
     * and an automatic voice ID. Whitespace at the cut is trimmed on both sides.
     */
    fun split(doc: ProjectDoc, blockKey: Key, offset: Int, ctx: EditContext): EnterResult {
        val block = doc.index.block(blockKey)?.takeIf { doc.manuscript.isLiveBlock(it.key) } ?: return EnterResult.Nothing(doc)
        if (block.kind.known !in TEXT_KINDS) return EnterResult.Nothing(doc)
        val (front, back) = Runs.splitAt(block.runs, offset)
        val (numbered, numbers) = Numbering.issue(doc, doc.numberingUnitOf(block), 1)
        val newBlock = block.copy(
            key = ctx.newKey(), order = doc.orderKeysAfter(block.sceneKey, block.key, 1).single(), number = numbers.single(),
            runs = Runs.trimStart(back), voice = null, recorderSource = null,
            createdAt = ctx.now, updatedAt = ctx.now, updatedBy = ctx.device, ext = null,
        )
        val frontRuns = Runs.trimEnd(front)
        val out = numbered.replaceBlock(block.copy(runs = frontRuns, updatedAt = ctx.now, updatedBy = ctx.device))
            .addBlocks(listOf(newBlock))
            .let { Containers.splitRejections(it, block.key, newBlock.key, frontRuns.plainText) }
        return EnterResult.Split(out, newBlock.key)
    }

    // --- Backspace (SCR-15) ---

    sealed interface BackspaceResult {
        val doc: ProjectDoc

        /** Nothing happens (first block of a scene, caret not at the start, non-text kinds). */
        data class Nothing(override val doc: ProjectDoc) : BackspaceResult

        /** An empty block was removed with no toast and no trash entry; the caret goes to [caretBlockKey]'s end. */
        data class RemovedEmpty(override val doc: ProjectDoc, val caretBlockKey: Key?) : BackspaceResult

        /** Merged into the previous block. [toast] is `◆ 1개 #042로` or null. */
        data class Merged(override val doc: ProjectDoc, val intoKey: Key, val caretOffset: Int, val toast: String?) : BackspaceResult

        /** The block had no text but anchored records: deleted like any non-empty block (SCR-38). */
        data class Deleted(val result: BlockTrash.DeleteResult) : BackspaceResult {
            override val doc: ProjectDoc get() = result.doc
        }
    }

    /**
     * Backspace in a block. [atStart] = the caret is at offset 0. An empty block (no content, no
     * record) is removed; a block whose text was erased but which anchors records goes to the trash;
     * at the start of a non-empty block the block merges into the previous one, never across a scene
     * head. An empty first block of a scene stays (the caret never crosses the head, `ED`).
     */
    fun backspace(doc: ProjectDoc, blockKey: Key, atStart: Boolean, ctx: EditContext): BackspaceResult {
        val block = doc.index.block(blockKey)?.takeIf { doc.manuscript.isLiveBlock(it.key) } ?: return BackspaceResult.Nothing(doc)
        val scene = doc.manuscript.liveBlocksOf(block.sceneKey)
        val i = scene.indexOfFirst { it.key == blockKey }
        val previous = if (i > 0) scene[i - 1] else null
        if (!block.hasContent()) {
            if (doc.recordsAnchoredOn(blockKey).isNotEmpty()) {
                return BackspaceResult.Deleted(BlockTrash.delete(doc, listOf(blockKey), ctx))
            }
            if (previous == null) return BackspaceResult.Nothing(doc)
            val result = BlockTrash.delete(doc, listOf(blockKey), ctx)
            return BackspaceResult.RemovedEmpty(result.doc, previous.key)
        }
        if (!atStart || previous == null) return BackspaceResult.Nothing(doc)
        if (block.kind.known !in TEXT_KINDS || previous.kind.known !in TEXT_KINDS) return BackspaceResult.Nothing(doc)
        return mergeIntoPrevious(doc, previous, block, ctx)
    }

    /**
     * Merge (Backspace at start, Q-13 default): the later block's runs are appended to [previous]
     * (one space between); [previous] keeps key, number, kind, speaker and voice; the later block is
     * removed (not trashed); its records move to [previous] unflagged; its fixed voice ID is discarded
     * (`merged`); acknowledgements and `아님` decisions follow.
     */
    private fun mergeIntoPrevious(doc: ProjectDoc, previous: Block, later: Block, ctx: EditContext): BackspaceResult {
        val caret = Runs.length(Runs.trimEnd(previous.runs)) + if (previous.runs.isBlankText()) 0 else 1
        val merged = previous.copy(runs = Runs.join(previous.runs, later.runs), updatedAt = ctx.now, updatedBy = ctx.device)
        val moved = doc.recordsAnchoredOn(later.key)
        var out = doc.replaceBlock(merged).removeBlocks(setOf(later.key))
            .mapRecords { r ->
                if ((r.anchor as? Anchor.AtBlock)?.blockKey == later.key && r.deletedAt == null) {
                    r.copy(anchor = Anchor.AtBlock(previous.key), updatedAt = ctx.now, updatedBy = ctx.device)
                } else r
            }
            .addDiscarded(doc.numberingUnitOf(later), VoiceIds.discardedOf(later, DiscardReason.MERGED, ctx.now))
        out = Containers.mergeContainers(out, later.key, previous.key, previous.runs.plainText)
        val diamonds = moved.count { !it.isReveal }
        val reveals = moved.count { it.isReveal }
        return BackspaceResult.Merged(out, previous.key, caret, Toasts.recordsTo(diamonds, reveals, Labels.number(previous.number)))
    }

    // --- Kind, speaker, stage (SCR-13, SCR-19, SCR-25) ---

    /** SCR-13 `[PROPOSED]`: text kinds convert freely; direction and choice only to or from an empty block. */
    fun canChangeKind(block: Block, to: BlockKind): Boolean {
        val from = block.kind.known ?: return false
        if (from == to) return false
        if (from in TEXT_KINDS && to in TEXT_KINDS) return true
        return !block.hasContent()
    }

    /**
     * Converts a block in place: key, number and records stay ("기록은 내용에 붙지 종류에 붙지
     * 않는다"); text runs carry over between text kinds. Leaving dialogue drops the speaker, stage and
     * automatic voice; a fixed ID becomes a `버린 ID` (kindChanged). A new dialogue block gets an
     * automatic ID and no speaker (`ED`: the author picks one).
     */
    fun changeKind(doc: ProjectDoc, blockKey: Key, to: BlockKind, ctx: EditContext): ProjectDoc {
        val block = doc.index.block(blockKey) ?: return doc
        if (!canChangeKind(block, to)) return doc
        val leavesVoice = block.kind.known == BlockKind.DIALOGUE || block.kind.known == BlockKind.CHOICE
        val discarded = if (leavesVoice) VoiceIds.discardedOf(block, DiscardReason.KIND_CHANGED, ctx.now) else emptyList()
        val converted = block.copy(
            kind = to.open(),
            speakerKey = if (to == BlockKind.DIALOGUE) block.speakerKey else null,
            stage = if (to == BlockKind.DIALOGUE) block.stage else null,
            voice = if (to == BlockKind.DIALOGUE && block.kind.known == BlockKind.DIALOGUE) block.voice else null,
            runs = if (to in TEXT_KINDS) block.runs else emptyList(),
            cues = if (to == BlockKind.DIRECTION) block.cues else emptyList(),
            options = if (to == BlockKind.CHOICE) block.options.ifEmpty { emptyOptions(ctx) } else emptyList(),
            recorderSource = if (to == BlockKind.MEMO) block.recorderSource else null,
            updatedAt = ctx.now, updatedBy = ctx.device,
        )
        return doc.replaceBlock(converted).addDiscarded(doc.numberingUnitOf(block), discarded)
    }

    /**
     * Sets the speaker of a dialogue block. An automatic ID follows at once (it is computed); a fixed
     * ID stays and is flagged `◇ 화자와 다름` by the checks.
     */
    fun changeSpeaker(doc: ProjectDoc, blockKey: Key, speakerKey: Key?, ctx: EditContext): ProjectDoc {
        val block = doc.index.block(blockKey) ?: return doc
        if (block.kind.known != BlockKind.DIALOGUE) return doc
        return doc.replaceBlock(block.copy(speakerKey = speakerKey, updatedAt = ctx.now, updatedBy = ctx.device))
    }

    /** `무대 위치` of a dialogue block (null = not chosen). */
    fun setStage(doc: ProjectDoc, blockKey: Key, stage: StagePosition?, ctx: EditContext): ProjectDoc {
        val block = doc.index.block(blockKey)?.takeIf { it.kind.known == BlockKind.DIALOGUE } ?: return doc
        return doc.replaceBlock(block.copy(stage = stage?.open(), updatedAt = ctx.now, updatedBy = ctx.device))
    }

    /** Replaces the runs of a text block (typing). */
    fun setRuns(doc: ProjectDoc, blockKey: Key, runs: List<TextRun>, ctx: EditContext): ProjectDoc {
        val block = doc.index.block(blockKey)?.takeIf { it.kind.known in TEXT_KINDS } ?: return doc
        return doc.replaceBlock(block.copy(runs = runs, updatedAt = ctx.now, updatedBy = ctx.device))
    }

    // --- Choice options (SCR-09 `[PROPOSED]` editing) ---

    /** Options of a choice block in display order. */
    fun orderedOptions(block: Block): List<ChoiceOption> =
        block.options.sortedWith(compareBy<ChoiceOption> { it.order }.thenBy { it.key })

    /** Enter at the end of an option: a new empty option right after it. */
    fun addOption(doc: ProjectDoc, blockKey: Key, afterOptionKey: Key, ctx: EditContext): Pair<ProjectDoc, Key>? {
        val block = doc.index.block(blockKey)?.takeIf { it.kind.known == BlockKind.CHOICE } ?: return null
        val options = orderedOptions(block)
        val i = options.indexOfFirst { it.key == afterOptionKey }.takeIf { it >= 0 } ?: return null
        val option = ChoiceOption(key = ctx.newKey(), order = OrderKeys.between(options[i].order, options.getOrNull(i + 1)?.order))
        return doc.replaceBlock(block.copy(options = block.options + option, updatedAt = ctx.now, updatedBy = ctx.device)) to option.key
    }

    /** Backspace in an empty option removes it; the block keeps at least 2 options. */
    fun removeOption(doc: ProjectDoc, blockKey: Key, optionKey: Key, ctx: EditContext): ProjectDoc {
        val block = doc.index.block(blockKey)?.takeIf { it.kind.known == BlockKind.CHOICE } ?: return doc
        val option = block.options.firstOrNull { it.key == optionKey } ?: return doc
        if (block.options.size <= 2 || !option.runs.isBlankText()) return doc
        return doc.replaceBlock(block.copy(options = block.options - option, updatedAt = ctx.now, updatedBy = ctx.device))
            .addDiscarded(
                doc.numberingUnitOf(block),
                VoiceIds.discardedOf(block.copy(voice = null, options = listOf(option)), DiscardReason.DELETED, ctx.now),
            )
    }

    /** Text or speaker of one option (Q-05: how the speaker is chosen is open; the data allows it). */
    fun updateOption(doc: ProjectDoc, blockKey: Key, optionKey: Key, ctx: EditContext, transform: (ChoiceOption) -> ChoiceOption): ProjectDoc {
        val block = doc.index.block(blockKey)?.takeIf { it.kind.known == BlockKind.CHOICE } ?: return doc
        return doc.replaceBlock(
            block.copy(options = block.options.map { if (it.key == optionKey) transform(it) else it }, updatedAt = ctx.now, updatedBy = ctx.device),
        )
    }
}
