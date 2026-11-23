package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.marks.Marks
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.checks.Acknowledgement
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.DiscardReason
import io.github.lenticualrstone.civilight.model.script.DiscardedVoiceId
import io.github.lenticualrstone.civilight.model.script.TrashPlacement
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Block delete, block trash and restore (SCR-38/39, §5.2–§5.5). Deleting never asks: a non-empty
 * block goes to the trash for 30 days, an empty one is removed; records are relocated, never deleted.
 */
object BlockTrash {
    /** A block is empty when it shows nothing and anchors no record (SCR-04). */
    fun isEmpty(doc: ProjectDoc, block: Block): Boolean =
        !block.hasContent() && doc.recordsAnchoredOn(block.key).isEmpty() && !Marks.carriesMark(doc, block.key)

    data class DeleteResult(
        val doc: ProjectDoc,
        /** Blocks moved to the trash, in manuscript order. */
        val trashedKeys: List<Key>,
        /** Empty blocks removed for good (kept here for undo). */
        val removedBlocks: List<Block>,
        val relocations: List<Anchors.Relocation>,
        val discarded: List<DiscardedVoiceId>,
        val removedAcks: List<Acknowledgement>,
        /** Toast message, or null when only empty blocks were removed. */
        val toast: String?,
    )

    /**
     * Deletes blocks (select mode `지우기`, Backspace). Non-empty blocks get a trash mark with their
     * placement (neighbours and ordinal among the scene's live blocks before this delete) and join
     * the scene's `blockGroup` entry of today; empty blocks are removed. Fixed voice IDs become
     * `버린 ID` (deleted); records on the deleted blocks move to the next surviving block.
     */
    fun delete(doc: ProjectDoc, blockKeys: Collection<Key>, ctx: EditContext): DeleteResult {
        val keys = blockKeys.toSet()
        val blocks = doc.manuscript.liveBlocks.filter { it.key in keys }
        val (empty, full) = blocks.partition { isEmpty(doc, it) }

        val entries = LinkedHashMap<Key, TrashEntry>()
        val marked = full.associate { b ->
            val scene = doc.manuscript.liveBlocksOf(b.sceneKey)
            val i = scene.indexOfFirst { it.key == b.key }
            val entry = entries.getOrPut(b.sceneKey) { blockGroupFor(doc, b.sceneKey, ctx) }
            entries[b.sceneKey] = entry.copy(targetKeys = entry.targetKeys + b.key)
            b.key to b.copy(
                deletedAt = ctx.now, trashEntryKey = entry.key,
                trashPlacement = TrashPlacement(prevKey = scene.getOrNull(i - 1)?.key, nextKey = scene.getOrNull(i + 1)?.key, ordinal = i),
                updatedAt = ctx.now, updatedBy = ctx.device,
            )
        }
        val discarded = blocks.flatMap { VoiceIds.discardedOf(it, DiscardReason.DELETED, ctx.now) }
        var out = doc.replaceBlocks(marked).removeBlocks(empty.map { it.key }.toSet()).upsertTrashEntries(entries.values.toList())
        for ((unit, ids) in discarded.groupBy { id -> doc.numberingUnitOf(blocks.first { it.key == id.blockKey }) }) {
            out = out.addDiscarded(unit, ids)
        }
        val (withoutAcks, removedAcks) = Containers.dropAcksOf(out, blocks.map { it.key }.toSet())
        val (relocated, moves) = Anchors.relocateOrphans(withoutAcks, ctx)
        val toast = if (full.isEmpty()) null else deleteToast(relocated, full, moves)
        return DeleteResult(relocated, full.map { it.key }, empty, moves, discarded, removedAcks, toast)
    }

    private fun deleteToast(doc: ProjectDoc, full: List<Block>, moves: List<Anchors.Relocation>): String {
        val diamonds = moves.count { !it.isReveal }
        val reveals = moves.count { it.isReveal }
        val single = full.singleOrNull()
        val target = moves.firstOrNull()?.to?.let { to ->
            (to as? Anchor.AtBlock)?.let { doc.index.block(it.blockKey) }?.let { Labels.blockRef(doc, it, single?.let(doc::numberingUnitOf)) }
        }
        return Toasts.blocksDeleted(full.size, single?.let { Labels.number(it.number) }, diamonds, reveals, target)
    }

    /** Today's `blockGroup` entry of a scene, or a new one (one per scene per local day). */
    private fun blockGroupFor(doc: ProjectDoc, sceneKey: Key, ctx: EditContext): TrashEntry =
        doc.trash.entries.firstOrNull {
            it.kind.known == TrashKind.BLOCK_GROUP && it.groupSceneKey == sceneKey && it.groupDate == ctx.today
        } ?: TrashEntry(
            key = ctx.newKey(), kind = TrashKind.BLOCK_GROUP.open(), projectKey = doc.key, targetKeys = emptyList(),
            groupSceneKey = sceneKey, groupDate = ctx.today,
            hiddenAtDelete = doc.index.unit(sceneKey)?.hidden == true,
            deletedAt = ctx.now, purgeOn = TrashEntry.purgeDateFor(ctx.today),
        )

    // --- Undo (SCR-17) ---

    /**
     * Undo of [result]: trashed blocks come back at their original order keys, removed empty blocks
     * are added back, discarded IDs and trash entries made by the delete are removed, removed
     * acknowledgements return, and every record still `orphaned` towards them re-attaches.
     */
    fun undoDelete(doc: ProjectDoc, result: DeleteResult, ctx: EditContext): ProjectDoc {
        val keys = result.trashedKeys.toSet()
        var out = doc.mapBlocks { b ->
            if (b.key !in keys) b else b.copy(deletedAt = null, trashEntryKey = null, trashPlacement = null, updatedAt = ctx.now, updatedBy = ctx.device)
        }.addBlocks(result.removedBlocks)
        for (k in keys) out = out.removeFromTrash(k)
        out = out.removeDiscarded { d -> result.discarded.any { it.id == d.id && it.blockKey == d.blockKey && it.discardedAt == d.discardedAt } }
        out = out.copy(checks = out.checks.copy(acknowledgements = out.checks.acknowledgements + result.removedAcks))
        return Anchors.reattachOrphans(out, ctx)
    }

    // --- Restore from the trash (SCR-39) ---

    /** What to do when the block's scene is not live (`씬도 되살리기` · `다른 씬 끝에`). */
    sealed interface SceneChoice {
        /** Restore the scene (the nearest directly trashed unit on its path) too. */
        data object RestoreScene : SceneChoice

        /** Append at the end of another live scene. */
        data class AppendTo(val sceneKey: Key) : SceneChoice
    }

    sealed interface RestoreResult {
        data class Restored(val doc: ProjectDoc) : RestoreResult

        /** The block's scene is in the trash: ask `씬도 되살리기` · `다른 씬 끝에`. */
        data class NeedsSceneChoice(val sceneKey: Key) : RestoreResult {
            val options: List<String> get() = listOf("씬도 되살리기", "다른 씬 끝에")
        }

        data object NotInTrash : RestoreResult
    }

    /**
     * Restores a trashed block: between its remembered neighbours (after the previous one when it is
     * live, before the next one when only that is live; Q-18 default), else at its stored ordinal.
     * It keeps its number and voice state; its `버린 ID` from the deletion is removed; records still
     * `orphaned` towards it re-attach.
     */
    fun restore(doc: ProjectDoc, blockKey: Key, ctx: EditContext, choice: SceneChoice? = null): RestoreResult {
        val block = doc.index.block(blockKey) ?: return RestoreResult.NotInTrash
        if (block.deletedAt == null) return RestoreResult.NotInTrash
        var out = doc
        var target = block.sceneKey
        if (!doc.manuscript.isLiveUnit(block.sceneKey)) {
            when (choice) {
                null -> return RestoreResult.NeedsSceneChoice(block.sceneKey)
                SceneChoice.RestoreScene -> out = restoreUnitPath(out, block.sceneKey)
                is SceneChoice.AppendTo -> {
                    if (!doc.manuscript.isLiveUnit(choice.sceneKey)) return RestoreResult.NeedsSceneChoice(block.sceneKey)
                    target = choice.sceneKey
                }
            }
        }
        val others = setOf(blockKey)
        val live = out.manuscript.liveBlocksOf(target)
        val placement = block.trashPlacement
        val prev = placement?.prevKey?.takeIf { k -> live.any { it.key == k } && target == block.sceneKey }
        val next = placement?.nextKey?.takeIf { k -> live.any { it.key == k } && target == block.sceneKey }
        val order = when {
            choice is SceneChoice.AppendTo -> out.orderKeysAtEnd(target, 1, others).single()
            prev != null -> out.orderKeysAfter(target, prev, 1, others).single()
            next != null -> out.orderKeysBefore(target, next, 1, others).single()
            else -> {
                val ordinal = (placement?.ordinal ?: live.size).coerceIn(0, live.size)
                if (ordinal == 0) live.firstOrNull()?.let { out.orderKeysBefore(target, it.key, 1, others).single() }
                    ?: out.orderKeysAtEnd(target, 1, others).single()
                else out.orderKeysAfter(target, live[ordinal - 1].key, 1, others).single()
            }
        }
        val fromUnit = doc.numberingUnitOf(block)
        val toUnit = out.numberingUnitOfScene(target)
        var number = block.number
        if (fromUnit != toUnit) {
            val (numbered, numbers) = Numbering.issue(out, toUnit, 1)
            out = numbered
            number = numbers.single()
        }
        out = out.replaceBlock(
            block.copy(
                sceneKey = target, order = order, number = number,
                deletedAt = null, trashEntryKey = null, trashPlacement = null, updatedAt = ctx.now, updatedBy = ctx.device,
            ),
        ).removeFromTrash(blockKey)
            .removeDiscarded { it.blockKey == blockKey && it.reason.known == DiscardReason.DELETED }
        if (fromUnit != toUnit) out = out.regroupBlocks()
        return RestoreResult.Restored(Anchors.reattachOrphans(out, ctx))
    }

    /** Clears the trash mark of the scene or of the nearest directly trashed unit above it. */
    private fun restoreUnitPath(doc: ProjectDoc, sceneKey: Key): ProjectDoc {
        val scene = doc.index.unit(sceneKey) ?: return doc
        val path = listOf(scene) + doc.index.ancestors(scene)
        var out = doc
        for (u in path.filter { it.deletedAt != null }) {
            out = out.updateUnit(u.key) { it.copy(deletedAt = null, trashEntryKey = null) }.removeFromTrash(u.key)
        }
        return out
    }

    // --- Trash screen rows (trash area renders them) ---

    /** One block group row: `1-7 씬 3 · 블록 4개` · `블록 · 잿빛 새벽의 등불 · 오늘 지움` · `30일 남음`. */
    data class GroupRow(val entryKey: Key, val title: String, val sub: String, val remaining: String, val blockKeys: List<Key>) {
        val buttons: List<String> get() = listOf("되살리기", "영구 삭제")
    }

    /** Rows of `blockGroup` entries (blocks inside a trashed scene belong to the scene entry). */
    fun groupRows(doc: ProjectDoc, today: LocalDate, zone: ZoneId): List<GroupRow> =
        doc.trash.entries.filter { it.kind.known == TrashKind.BLOCK_GROUP && it.groupSceneKey != null }.map { e ->
            val deletedOn = Instant.ofEpochMilli(e.deletedAt).atZone(zone).toLocalDate()
            val days = ChronoUnit.DAYS.between(today, e.purgeOn)
            val ago = ChronoUnit.DAYS.between(deletedOn, today)
            val whenText = when (ago) {
                0L -> "오늘 지움"
                1L -> "어제 지움"
                else -> "${ago}일 전 지움"
            }
            GroupRow(
                entryKey = e.key,
                title = "${Labels.sceneLocation(doc, e.groupSceneKey!!)} · 블록 ${e.targetKeys.size}개",
                sub = "블록 · ${doc.project.name} · $whenText",
                remaining = "${days}일 남음",
                blockKeys = e.targetKeys,
            )
        }
}
