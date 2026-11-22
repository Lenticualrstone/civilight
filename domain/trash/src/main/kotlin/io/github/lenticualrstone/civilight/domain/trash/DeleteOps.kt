package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.checks.Acknowledgement
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.liveBlocksOf
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.DiscardReason
import io.github.lenticualrstone.civilight.model.script.DiscardedVoiceId
import io.github.lenticualrstone.civilight.model.script.TrashPlacement
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.trash.DeletionLogEntry
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/** What one delete did. Undo with [TrashOps.undo] (row-level revert of [change]). */
data class DeleteResult(
    val doc: ProjectDoc,
    /** New entries, and block-group entries extended by this delete (their current value). */
    val entries: List<TrashEntry>,
    /** Items that got a direct mark. */
    val trashed: List<ItemId>,
    /** Empty blocks removed outright (no entry, no toast, SCR-38). */
    val hardDeleted: List<Key>,
    /** Items not deleted: unknown, already in the trash, or (blocks) not live. */
    val skipped: List<ItemId>,
    val relocations: List<Relocation>,
    /** Acknowledgements dropped because a subject stopped being live (STO-41); undo brings them back. */
    val removedAcks: List<Acknowledgement>,
    val discardedVoiceIds: List<DiscardedVoiceId>,
    val change: DocChange,
)

/** What [TrashOps.settleDelete] did after the direct marks were set. */
data class SettledDelete(
    val doc: ProjectDoc,
    val relocations: List<Relocation>,
    /** Acknowledgements dropped because a subject stopped being live (STO-41). */
    val removedAcks: List<Acknowledgement>,
)

/** Delete and its undo (§5.2) as pure functions over a [ProjectDoc]. */
object TrashOps {
    /** Key in [TrashEntry.ext] of the reference sites that pointed at the item when it was deleted. */
    const val REF_SITES = "refSites"

    fun delete(doc: ProjectDoc, item: ItemId, ctx: TrashContext): DeleteResult = delete(doc, listOf(item), ctx)

    /**
     * Puts [items] in the trash together (TC-20-11): one direct mark and one entry per item; blocks
     * one `blockGroup` entry per scene per local day (an entry of the same scene and day is
     * extended); empty blocks (no content, no record anchored on them) are removed outright. Owned
     * rows are not marked (§5.1); anchors of records whose referent stops being live relocate
     * (§5.5); acknowledgements whose subject stops being live are dropped, except `deleted.item`.
     */
    fun delete(doc: ProjectDoc, items: List<ItemId>, ctx: TrashContext): DeleteResult {
        val before = doc
        val now = ctx.now
        val today = ctx.today
        val projectHidden = doc.project.protections.hidden
        var d = doc
        val newEntries = mutableListOf<TrashEntry>()
        val trashed = mutableListOf<ItemId>()
        val skipped = mutableListOf<ItemId>()

        for (item in items.filter { it.kind != ItemKind.BLOCK }.distinct()) {
            val row = d.rowOf(item)
            if (row == null || row.deletedAt != null) {
                skipped += item
                continue
            }
            val entry = TrashEntry(
                key = ctx.newKey(),
                kind = item.kind.trashKind.open(),
                projectKey = doc.key,
                targetKeys = listOf(item.key),
                hiddenAtDelete = d.itemHidden(item) || projectHidden,
                deletedAt = now,
                purgeOn = TrashEntry.purgeDateFor(today),
            )
            d = d.setMark(item, now, entry.key, now, ctx.deviceId)
            newEntries += entry
            trashed += item
        }

        // Blocks.
        val anchoredBlocks = doc.changes.records.filter { it.deletedAt == null }
            .mapNotNull { (it.anchor as? Anchor.AtBlock)?.blockKey }.toSet()
        val blockItems = items.filter { it.kind == ItemKind.BLOCK }.distinct()
        val blocks = blockItems.mapNotNull { item ->
            doc.index.block(item.key)?.takeIf { doc.liveness.isLive(it) }.also { if (it == null) skipped += item }
        }
        val (empty, nonEmpty) = blocks.partition { !it.hasContent() && it.key !in anchoredBlocks }
        val updatedGroups = mutableMapOf<Key, TrashEntry>()
        val discarded = mutableListOf<DiscardedVoiceId>()
        for ((sceneKey, sceneBlocks) in nonEmpty.groupBy { it.sceneKey }) {
            val liveInScene = doc.liveBlocksOf(sceneKey)
            val existing = d.trash.entries.firstOrNull {
                it.kind.known == TrashKind.BLOCK_GROUP && it.groupSceneKey == sceneKey && it.groupDate == today
            }
            val entry = existing?.copy(targetKeys = existing.targetKeys + sceneBlocks.map { it.key }.filter { it !in existing.targetKeys })
                ?: TrashEntry(
                    key = ctx.newKey(),
                    kind = TrashKind.BLOCK_GROUP.open(),
                    projectKey = doc.key,
                    targetKeys = sceneBlocks.map { it.key },
                    groupSceneKey = sceneKey,
                    groupDate = today,
                    hiddenAtDelete = (doc.index.unit(sceneKey)?.hidden ?: false) || projectHidden,
                    deletedAt = now,
                    purgeOn = TrashEntry.purgeDateFor(today),
                )
            if (existing == null) newEntries += entry else updatedGroups[existing.key] = entry
            for (block in sceneBlocks) {
                val i = liveInScene.indexOfFirst { it.key == block.key }
                val placement = TrashPlacement(
                    prevKey = liveInScene.getOrNull(i - 1)?.key,
                    nextKey = liveInScene.getOrNull(i + 1)?.key,
                    ordinal = i.coerceAtLeast(0),
                )
                d = d.markBlock(block.key, now, entry.key, placement, ctx.deviceId)
                trashed += ItemId.block(block.key)
                discarded += block.fixedVoiceIds(now)
            }
        }
        d = d.addDiscarded(discarded)

        // Empty blocks: removed outright; logged for future sync.
        if (empty.isNotEmpty()) {
            val emptyKeys = empty.map { it.key }.toSet()
            d = d.copy(
                episodes = d.episodes.mapValues { (_, file) -> file.copy(blocks = file.blocks.filter { it.key !in emptyKeys }) },
                deletions = d.deletions.copy(entries = d.deletions.entries + empty.map { DeletionLogEntry(it.key, "block", now) }),
            )
        }

        d = d.copy(
            trash = d.trash.copy(
                entries = d.trash.entries.map { updatedGroups[it.key] ?: it } + newEntries,
            ),
        )

        // Anchors and acknowledgements.
        val settled = settleDelete(before, d, ctx)
        d = settled.doc
        val moves = settled.relocations
        val removedAcks = settled.removedAcks

        // Reference snapshot (restore report, TC-20-05).
        val scanner = ReferenceScanner(d)
        val snapshots = trashed.filter { it.kind != ItemKind.BLOCK }.associate { item ->
            item.key to scanner.referencesTo(item).map { it.id }
        }
        if (snapshots.values.any { it.isNotEmpty() }) {
            d = d.copy(
                trash = d.trash.copy(
                    entries = d.trash.entries.map { entry ->
                        val sites = entry.targetKeys.singleOrNull()?.let(snapshots::get)
                        if (entry.key in newEntries.map { it.key } && !sites.isNullOrEmpty()) entry.withRefSites(sites) else entry
                    },
                ),
            )
        }

        val finalEntries = d.trash.entries.filter { e -> e.key in newEntries.map { it.key } || e.key in updatedGroups.keys }
        return DeleteResult(
            doc = d,
            entries = finalEntries,
            trashed = trashed,
            hardDeleted = empty.map { it.key },
            skipped = skipped,
            relocations = moves,
            removedAcks = removedAcks,
            discardedVoiceIds = discarded,
            change = DocChange(before, d),
        )
    }

    /** `되돌리기` of a delete: everything it changed goes back (marks, entries, anchors, acks, discarded IDs, empty blocks). */
    fun undo(current: ProjectDoc, result: DeleteResult): ProjectDoc = result.change.revert(current)

    /**
     * The steps every delete runs after the direct marks are set, for marks set elsewhere too
     * (domain:story's `SceneTrash.delete`, which also rebases 때, STO-10): records whose anchor
     * referent was live in [before] and is not in [marked] relocate (§5.5), and acknowledgements
     * whose subject stopped being live are dropped, except `deleted.item` (§2.9).
     */
    fun settleDelete(before: ProjectDoc, marked: ProjectDoc, ctx: TrashContext): SettledDelete {
        val (relocated, moves) = Anchors.relocate(before, marked, ctx.now, ctx.deviceId)
        val (withoutAcks, removedAcks) = dropAcksOfNonLive(before, relocated)
        return SettledDelete(withoutAcks, moves, removedAcks)
    }

    /** Reference site ids stored with an entry at delete time. */
    fun refSitesOf(entry: TrashEntry): List<String> =
        (entry.ext?.get(REF_SITES) as? JsonArray)?.map { it.jsonPrimitive.content }.orEmpty()

    private fun TrashEntry.withRefSites(sites: List<String>): TrashEntry =
        copy(ext = JsonObject((ext ?: JsonObject(emptyMap())) + (REF_SITES to JsonArray(sites.map(::JsonPrimitive)))))

    /** Drops acknowledgements whose subjects stopped being live between [before] and [after], except `deleted.item`. */
    internal fun dropAcksOfNonLive(before: ProjectDoc, after: ProjectDoc): Pair<ProjectDoc, List<Acknowledgement>> {
        val liveBefore = LiveRows.of(before).all
        val liveAfter = LiveRows.of(after).all
        val (drop, keep) = after.checks.acknowledgements.partition { ack ->
            ack.checkType != CheckType.DELETED_ITEM && ack.subjectKeys.any { it in liveBefore && it !in liveAfter }
        }
        if (drop.isEmpty()) return after to emptyList()
        return after.copy(checks = after.checks.copy(acknowledgements = keep)) to drop
    }
}

/** `버린 ID` rows for the fixed voice IDs of a deleted block and its spoken options (SCR-38). */
internal fun Block.fixedVoiceIds(at: Long): List<DiscardedVoiceId> {
    val own = voice?.takeIf { it.mode.known == VoiceMode.FIXED }?.fixedId
        ?.let { listOf(DiscardedVoiceId(it, key, null, at, DiscardReason.DELETED.open())) }.orEmpty()
    val options = options.mapNotNull { option ->
        option.voice?.takeIf { it.mode.known == VoiceMode.FIXED }?.fixedId
            ?.let { DiscardedVoiceId(it, key, option.key, at, DiscardReason.DELETED.open()) }
    }
    return own + options
}

/** Files discarded IDs under the episode file that holds their block. */
internal fun ProjectDoc.addDiscarded(rows: List<DiscardedVoiceId>): ProjectDoc {
    if (rows.isEmpty()) return this
    var episodes = episodes
    for ((group, list) in rows.groupBy { index.episodeGroupOfBlock(it.blockKey) }) {
        val key = group ?: continue
        val file = episodes.getValue(key)
        episodes = episodes + (key to file.copy(discardedVoiceIds = file.discardedVoiceIds + list))
    }
    return copy(episodes = episodes)
}
