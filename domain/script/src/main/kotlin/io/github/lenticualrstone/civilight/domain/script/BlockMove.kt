package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.liveScenes
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * Moving blocks (SCR-37) and the clipboard (SCR-40). Keys stay on a move, so records,
 * acknowledgements and `아님` decisions ride along.
 */
object BlockMove {
    data class MoveResult(val doc: ProjectDoc, val movedKeys: List<Key>, val toast: String?)

    /**
     * Moves live blocks (taken in manuscript order) to [destination]. Same numbering unit: numbers
     * and voice IDs unchanged. Another unit: the destination's next numbers in manuscript order of
     * the moved blocks; automatic IDs follow, fixed IDs stay (the checks flag them).
     */
    fun move(doc: ProjectDoc, blockKeys: Collection<Key>, destination: BlockPosition, ctx: EditContext): MoveResult {
        val keys = blockKeys.toSet()
        val moving = doc.manuscript.liveBlocks.filter { it.key in keys }
        if (moving.isEmpty()) return MoveResult(doc, emptyList(), null)
        val (sceneKey, prevKey) = resolveDestination(doc, destination, keys) ?: return MoveResult(doc, emptyList(), null)
        val orders = doc.orderKeysAfter(sceneKey, prevKey, moving.size, excluding = keys)
        val toUnit = doc.numberingUnitOfScene(sceneKey)
        val crossing = moving.filter { doc.numberingUnitOf(it) != toUnit }
        val (numbered, numbers) = Numbering.issue(doc, toUnit, crossing.size)
        val newNumbers = crossing.zip(numbers).associate { (b, n) -> b.key to n }
        val moved = moving.mapIndexed { i, b ->
            b.copy(sceneKey = sceneKey, order = orders[i], number = newNumbers[b.key] ?: b.number, updatedAt = ctx.now, updatedBy = ctx.device)
        }
        var out = numbered.replaceBlocks(moved.associateBy { it.key })
        if (crossing.isNotEmpty()) out = out.regroupBlocks()
        return MoveResult(out, moving.map { it.key }, moveToast(doc, out, moving, sceneKey))
    }

    /** A destination that is one of the moved blocks falls back to the nearest block above it that stays. */
    private fun resolveDestination(doc: ProjectDoc, destination: BlockPosition, moving: Set<Key>): Pair<Key, Key?>? {
        if (destination is BlockPosition.After && destination.blockKey in moving) {
            val anchor = doc.index.block(destination.blockKey) ?: return null
            val scene = doc.manuscript.liveBlocksOf(anchor.sceneKey)
            val i = scene.indexOfFirst { it.key == anchor.key }
            val prev = scene.subList(0, i).lastOrNull { it.key !in moving }
            return anchor.sceneKey to prev?.key
        }
        if (destination is BlockPosition.SceneEnd) {
            val last = doc.manuscript.liveBlocksOf(destination.sceneKey).lastOrNull { it.key !in moving }
            return destination.sceneKey.takeIf { doc.manuscript.isLiveUnit(it) }?.let { it to last?.key }
        }
        return BlockOps.resolve(doc, destination)
    }

    private fun moveToast(before: ProjectDoc, after: ProjectDoc, moving: List<Block>, toScene: Key): String? {
        val fromScene = moving.first().sceneKey
        if (moving.all { it.sceneKey == toScene }) return null
        val sameUnit = before.numberingUnitOfScene(fromScene) == after.numberingUnitOfScene(toScene)
        val from = if (sameUnit) "씬 ${Labels.sceneOrdinal(before, fromScene)}" else Labels.episodeCodeOfScene(before, fromScene)
        val to = if (sameUnit) "씬 ${Labels.sceneOrdinal(after, toScene)}" else Labels.episodeCodeOfScene(after, toScene)
        val keys = moving.map { it.key }.toSet()
        val records = before.changes.records.filter { it.deletedAt == null && (it.anchor as? Anchor.AtBlock)?.blockKey in keys }
        val parts = mutableListOf<String>()
        Toasts.moved(records.count { !it.isReveal }, from, to)?.let(parts::add)
        for (reveal in records.filter { it.isReveal }) {
            val term = (reveal.target as ChangeTarget.TermReveal).termKey
            val newUses = newPreRevealUses(before, after, term)
            if (newUses > 0) parts += Toasts.revealMoved(from, to, newUses)
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    /** Links to [termKey] in game-output blocks that are before the reveal after the move but were not before it. */
    fun newPreRevealUses(before: ProjectDoc, after: ProjectDoc, termKey: Key): Int =
        termLinks(after, termKey).count { (blockKey, _) ->
            isBeforeReveal(after, termKey, blockKey) && !isBeforeReveal(before, termKey, blockKey)
        }

    private fun isBeforeReveal(doc: ProjectDoc, termKey: Key, blockKey: Key): Boolean {
        val reveal = doc.changes.records.firstOrNull {
            it.deletedAt == null && (it.target as? ChangeTarget.TermReveal)?.termKey == termKey
        } ?: return false
        val rank = doc.manuscript.blockRankOf(blockKey) ?: return false
        return rank < doc.manuscript.rankOf(reveal.anchor)
    }

    /** (block key, link key) of links to a term in live game-output blocks (memos and cues never count). */
    fun termLinks(doc: ProjectDoc, termKey: Key): List<Pair<Key, Key>> = doc.manuscript.liveBlocks
        .filter { it.kind.known != BlockKind.MEMO && it.kind.known != BlockKind.DIRECTION }
        .flatMap { b ->
            (b.runs + b.options.flatMap { it.runs }).filterIsInstance<TextRun.Link>()
                .filter { it.target.key == termKey }.map { b.key to it.linkKey }
        }

    // --- Clipboard (SCR-40) ---

    /** Blocks on the app clipboard. A cut is pasted as a move (Q-19: cut blocks stay until pasted). */
    data class Clipboard(val blocks: List<Block>, val cut: Boolean, val hadRecords: Boolean)

    fun copy(doc: ProjectDoc, blockKeys: Collection<Key>): Clipboard = clip(doc, blockKeys, cut = false)
    fun cut(doc: ProjectDoc, blockKeys: Collection<Key>): Clipboard = clip(doc, blockKeys, cut = true)

    private fun clip(doc: ProjectDoc, blockKeys: Collection<Key>, cut: Boolean): Clipboard {
        val keys = blockKeys.toSet()
        val blocks = doc.manuscript.liveBlocks.filter { it.key in keys }
        return Clipboard(blocks, cut, blocks.any { doc.recordsAnchoredOn(it.key).isNotEmpty() })
    }

    data class PasteResult(val doc: ProjectDoc, val blockKeys: List<Key>, val toast: String?)

    /**
     * Pastes below [destination]. A cut moves the original blocks; a copy inserts new blocks with new
     * keys (also for options, cues and links), the destination's next numbers and automatic voice
     * IDs; records are never copied (toast `◆는 복사되지 않음`).
     */
    fun paste(doc: ProjectDoc, clipboard: Clipboard, destination: BlockPosition, ctx: EditContext): PasteResult {
        if (clipboard.cut) {
            val r = move(doc, clipboard.blocks.map { it.key }, destination, ctx)
            return PasteResult(r.doc, r.movedKeys, r.toast)
        }
        val (sceneKey, prevKey) = BlockOps.resolve(doc, destination) ?: return PasteResult(doc, emptyList(), null)
        val orders = doc.orderKeysAfter(sceneKey, prevKey, clipboard.blocks.size)
        val (numbered, numbers) = Numbering.issue(doc, doc.numberingUnitOfScene(sceneKey), clipboard.blocks.size)
        val copies = clipboard.blocks.mapIndexed { i, b ->
            b.copy(
                key = ctx.newKey(), sceneKey = sceneKey, order = orders[i], number = numbers[i],
                runs = Runs.remintLinks(b.runs, ctx.newKey), voice = null, recorderSource = null,
                cues = b.cues.map { it.copy(key = ctx.newKey()) },
                options = b.options.map { it.copy(key = ctx.newKey(), runs = Runs.remintLinks(it.runs, ctx.newKey), voice = null) },
                createdAt = ctx.now, deletedAt = null, trashEntryKey = null, trashPlacement = null,
                updatedAt = ctx.now, updatedBy = ctx.device, ext = null,
            )
        }
        return PasteResult(numbered.addBlocks(copies), copies.map { it.key }, if (clipboard.hadRecords) Toasts.NOT_COPIED else null)
    }

    /**
     * Plain text from outside the app (Q-19 default): one narration block per non-empty line; a line
     * that looks like a scene head stays narration and never splits the scene (rule 22 §4).
     */
    fun pastePlainText(doc: ProjectDoc, text: String, destination: BlockPosition, ctx: EditContext): PasteResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var out = doc
        var position = destination
        val keys = mutableListOf<Key>()
        for (line in lines) {
            val ins = BlockOps.insert(out, position, BlockKind.NARRATION, ctx, runs = listOf(TextRun.Plain(line)))
            out = ins.doc
            keys += ins.blockKey
            position = BlockPosition.After(ins.blockKey)
        }
        return PasteResult(out, keys, null)
    }

    /** Count of live scenes (pasting never changes it). */
    fun sceneCount(doc: ProjectDoc): Int = doc.liveScenes.size
}
