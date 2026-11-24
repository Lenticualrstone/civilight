package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.liveBlocksOf
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.DiscardReason
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.trash.TrashReason

/** Where a block goes when its scene is not live (SCR-39: `씬도 되살리기` · `다른 씬 끝에`). */
sealed interface SceneChoice {
    /** Restore the scene (and any trashed ancestor) first, then the block at its place. */
    data object RestoreScene : SceneChoice

    /** Append the block at the end of another live scene. */
    data class OtherSceneEnd(val sceneKey: Key) : SceneChoice
}

/** What a restore brought back, for the toast (CST-43). */
data class RestoreReport(
    val item: ItemId?,
    val name: String?,
    /** References that point at the item again, by kind. */
    val relinked: Map<RefKind, Int>,
    /** References the author changed while the item was in the trash (`그대로 둔 것`), by kind. */
    val keptChanged: Map<RefKind, Int>,
    /** Records re-attached to a restored block or unit (§5.5). */
    val reattached: List<Key>,
)

/** What [RestoreOps.settleRestore] did after the direct marks were cleared. */
data class SettledRestore(
    val doc: ProjectDoc,
    /** Records that went back to their original anchor (§5.5). */
    val reattached: List<Key>,
    /** Restored records whose anchor referent went away while they were in the trash. */
    val relocations: List<Relocation>,
)

sealed interface RestoreOutcome {
    data class Restored(val doc: ProjectDoc, val report: RestoreReport, val change: DocChange) : RestoreOutcome

    /** Hidden things need authentication first (LIB-38). */
    data object NeedsAuth : RestoreOutcome

    /** A block whose scene is not live: offer `씬도 되살리기` · `다른 씬 끝에` (LIB T-14). */
    data class NeedsSceneChoice(val sceneKey: Key, val blockKeys: List<Key>) : RestoreOutcome

    data object NotFound : RestoreOutcome
}

object RestoreOps {
    /**
     * `되살리기` of a whole trash entry (§5.3): clears the direct mark(s) and deletes the entry;
     * everything derived comes back, references re-resolve by key, orphaned records re-attach,
     * `deleted.item` acknowledgements of the item are deleted. A merged item comes back from its
     * snapshot as a separate item (TC-20-19). Hidden things need [authenticated].
     */
    fun restore(
        doc: ProjectDoc,
        entryKey: Key,
        ctx: TrashContext,
        authenticated: Boolean = false,
        sceneChoice: SceneChoice? = null,
    ): RestoreOutcome {
        val entry = doc.index.trashEntry(entryKey) ?: return RestoreOutcome.NotFound
        if (TrashGates.needsAuth(doc, entry) && !authenticated) return RestoreOutcome.NeedsAuth
        if (entry.kind.known == TrashKind.BLOCK_GROUP) {
            return restoreBlocks(doc, entryKey, entry.targetKeys, ctx, authenticated, sceneChoice)
        }
        // (0.2.0, N-44) A tag: NotFound while a live tag has its name. Callers with a screen check
        // TagTrash.canRestore first and say why (TrashCopy.TAG_NAME_TAKEN, feature:library TrashDecisions).
        if (entry.kind.known == TrashKind.TAG) {
            return when (val r = TagTrash.restore(doc, entryKey, ctx)) {
                is TagRestore.Restored -> RestoreOutcome.Restored(r.doc, RestoreReport(null, r.tag.name, emptyMap(), emptyMap(), emptyList()), DocChange(doc, r.doc))
                else -> RestoreOutcome.NotFound
            }
        }
        val kind = ItemKind.of(entry.kind) ?: return RestoreOutcome.NotFound
        val before = doc
        var d = doc
        for (key in entry.targetKeys) {
            val item = ItemId(kind, key)
            d = if (entry.reason.known == TrashReason.MERGED) MergeOps.restoreMerged(d, item, ctx) else d.setMark(item, null, null, ctx.now, ctx.deviceId)
        }
        d = d.removeEntry(entry.key)
        val settled = settleRestore(before, d, ctx)
        val item = entry.targetKeys.singleOrNull()?.let { ItemId(kind, it) }
        return RestoreOutcome.Restored(settled.doc, report(settled.doc, entry, item, settled.reattached), DocChange(before, settled.doc))
    }

    /**
     * The steps every restore runs after the direct marks are cleared, for marks cleared elsewhere
     * too (domain:story's `SceneTrash.restore`): `deleted.item` acknowledgements of items that are
     * live again are deleted (§2.9); records still `orphaned` towards a referent that is live again
     * re-attach (§5.5); records that come back from their own mark are settled against the
     * manuscript as it is now ([Anchors.settleRestored]).
     */
    fun settleRestore(before: ProjectDoc, unmarked: ProjectDoc, ctx: TrashContext): SettledRestore {
        val liveBefore = LiveRows.itemKeys(before)
        val liveAfter = LiveRows.itemKeys(unmarked)
        var d = unmarked.dropDeletedItemAcks(liveAfter - liveBefore)
        val (reattached, back) = Anchors.reattach(before, d, ctx.now, ctx.deviceId)
        d = reattached
        val unmarkedRecords = before.changes.records.filter { it.deletedAt != null }.map { it.key }
            .filter { key -> d.index.record(key)?.deletedAt == null }.toSet()
        val (settled, records) = Anchors.settleRestored(d, unmarkedRecords, ctx.now, ctx.deviceId)
        return SettledRestore(settled, back + records.reattached, records.relocations)
    }

    /**
     * Restores some blocks of a block group (each block row has its own `되살리기`, LIB-38). Position
     * (SCR-39): right after the remembered previous neighbour, else right before the remembered next
     * one, else at the stored ordinal; when the scene is not live the caller picks a [SceneChoice].
     * The block keeps its number and voice state; its `버린 ID (deleted)` rows are removed; records
     * orphaned towards it re-attach.
     */
    fun restoreBlocks(
        doc: ProjectDoc,
        entryKey: Key,
        blockKeys: List<Key>,
        ctx: TrashContext,
        authenticated: Boolean = false,
        sceneChoice: SceneChoice? = null,
    ): RestoreOutcome {
        val entry = doc.index.trashEntry(entryKey) ?: return RestoreOutcome.NotFound
        if (TrashGates.needsAuth(doc, entry) && !authenticated) return RestoreOutcome.NeedsAuth
        val keys = blockKeys.filter { it in entry.targetKeys && doc.index.block(it) != null }
        if (keys.isEmpty()) return RestoreOutcome.NotFound
        val before = doc
        var d = doc
        val sceneKey = doc.index.block(keys.first())!!.sceneKey
        if (!d.liveness.isUnitLive(sceneKey)) {
            when (sceneChoice) {
                null -> return RestoreOutcome.NeedsSceneChoice(sceneKey, keys)
                SceneChoice.RestoreScene -> d = restoreUnitChain(d, sceneKey, ctx)
                is SceneChoice.OtherSceneEnd -> if (!d.liveness.isUnitLive(sceneChoice.sceneKey)) return RestoreOutcome.NotFound
            }
        }
        // Restore in the order they stood in the scene so neighbours come back before their followers.
        val ordered = keys.mapNotNull(d.index::block).sortedWith(compareBy<Block> { it.trashPlacement?.ordinal ?: 0 }.thenBy { it.order })
        for (block in ordered) {
            d = if (sceneChoice is SceneChoice.OtherSceneEnd && !before.liveness.isUnitLive(block.sceneKey)) {
                appendToScene(d, block.key, sceneChoice.sceneKey, ctx)
            } else {
                placeInScene(d, block.key, ctx)
            }
            d = d.removeDiscardedOnRestore(block.key)
        }
        val remaining = entry.targetKeys - keys.toSet()
        d = if (remaining.isEmpty()) d.removeEntry(entry.key) else d.replaceEntry(entry.copy(targetKeys = remaining))
        val settled = settleRestore(before, d, ctx)
        val report = RestoreReport(null, null, emptyMap(), emptyMap(), settled.reattached)
        return RestoreOutcome.Restored(settled.doc, report, DocChange(before, settled.doc))
    }

    /** Clears the marks of [unitKey] and its trashed ancestors, deleting their entries. */
    private fun restoreUnitChain(doc: ProjectDoc, unitKey: Key, ctx: TrashContext): ProjectDoc {
        var d = doc
        val unit = d.index.unit(unitKey) ?: return d
        for (u in listOf(unit) + d.index.ancestors(unit)) {
            if (u.deletedAt == null) continue
            val entryKey = u.trashEntryKey
            d = d.setMark(ItemId.unit(u.key), null, null, ctx.now, ctx.deviceId)
            if (entryKey != null) {
                val entry = d.index.trashEntry(entryKey)
                if (entry != null) {
                    val rest = entry.targetKeys - u.key
                    d = if (rest.isEmpty()) d.removeEntry(entryKey) else d.replaceEntry(entry.copy(targetKeys = rest))
                }
            }
        }
        return d
    }

    private fun placeInScene(doc: ProjectDoc, blockKey: Key, ctx: TrashContext): ProjectDoc {
        val block = doc.index.block(blockKey) ?: return doc
        val live = doc.liveBlocksOf(block.sceneKey)
        val placement = block.trashPlacement
        val prevIndex = placement?.prevKey?.let { p -> live.indexOfFirst { it.key == p } } ?: -1
        val nextIndex = placement?.nextKey?.let { n -> live.indexOfFirst { it.key == n } } ?: -1
        val insertAt = when {
            prevIndex >= 0 -> prevIndex + 1
            nextIndex >= 0 -> nextIndex
            else -> (placement?.ordinal ?: live.size).coerceIn(0, live.size)
        }
        val lower = live.getOrNull(insertAt - 1)?.order
        val upper = live.getOrNull(insertAt)?.order
        val taken = doc.index.blocksOfScene(block.sceneKey).filter { it.key != blockKey }.map { it.order }.toSet()
        val order = freeOrder(lower, upper, block.order, taken)
        return doc.updateBlock(blockKey) {
            it.copy(order = order, deletedAt = null, trashEntryKey = null, trashPlacement = null, updatedAt = ctx.now, updatedBy = ctx.deviceId)
        }
    }

    /** `다른 씬 끝에`: the block moves to the end of [sceneKey]; in another numbering unit it takes that unit's next number. */
    private fun appendToScene(doc: ProjectDoc, blockKey: Key, sceneKey: Key, ctx: TrashContext): ProjectDoc {
        val block = doc.index.block(blockKey) ?: return doc
        val last = doc.index.blocksOfScene(sceneKey).maxOfOrNull { it.order }
        val fromUnit = doc.index.numberingUnitKeyOfScene(block.sceneKey)
        val toUnit = doc.index.numberingUnitKeyOfScene(sceneKey)
        var d = doc
        var number = block.number
        if (toUnit != null && toUnit != fromUnit) {
            val (next, updated) = d.takeNextBlockNumber(toUnit)
            number = next
            d = updated
        }
        d = d.updateBlock(blockKey) {
            it.copy(
                sceneKey = sceneKey, order = OrderKeys.after(last), number = number,
                deletedAt = null, trashEntryKey = null, trashPlacement = null, updatedAt = ctx.now, updatedBy = ctx.deviceId,
            )
        }
        return d.regroupBlocks()
    }

    /** A key strictly between [lower] and [upper], preferring [own] when it already fits, never one of [taken]. */
    internal fun freeOrder(lower: OrderKey?, upper: OrderKey?, own: OrderKey, taken: Set<OrderKey>): OrderKey {
        fun fits(k: OrderKey) = (lower == null || k > lower) && (upper == null || k < upper)
        if (fits(own) && own !in taken) return own
        var lo = lower
        var candidate = OrderKeys.between(lo, upper)
        while (candidate in taken) {
            lo = candidate
            candidate = OrderKeys.between(lo, upper)
        }
        return candidate
    }

    private fun report(doc: ProjectDoc, entry: TrashEntry, item: ItemId?, reattached: List<Key>): RestoreReport {
        if (item == null) return RestoreReport(null, null, emptyMap(), emptyMap(), reattached)
        val now = ReferenceScanner(doc).referencesTo(item)
        val stored = TrashOps.refSitesOf(entry).toSet()
        val nowIds = now.map { it.id }.toSet()
        val kept = stored.filter { it !in nowIds }.mapNotNull { id -> RefKind.entries.firstOrNull { id.startsWith(it.name + ":") } }
        return RestoreReport(
            item = item,
            name = doc.itemName(item),
            relinked = now.groupingBy { it.kind }.eachCount(),
            keptChanged = kept.groupingBy { it }.eachCount(),
            reattached = reattached,
        )
    }
}

internal fun ProjectDoc.removeEntry(entryKey: Key): ProjectDoc =
    copy(trash = trash.copy(entries = trash.entries.filter { it.key != entryKey }))

internal fun ProjectDoc.replaceEntry(entry: TrashEntry): ProjectDoc =
    copy(trash = trash.copy(entries = trash.entries.map { if (it.key == entry.key) entry else it }))

/** `deleted.item` acknowledgements about [itemKeys] (deleted when the item is restored, §2.9). */
internal fun ProjectDoc.dropDeletedItemAcks(itemKeys: Set<Key>): ProjectDoc {
    val keep = checks.acknowledgements.filterNot { it.checkType == CheckType.DELETED_ITEM && it.subjectKeys.firstOrNull() in itemKeys }
    return if (keep.size == checks.acknowledgements.size) this else copy(checks = checks.copy(acknowledgements = keep))
}

/** Removes the `버린 ID (deleted)` rows its deletion made for a restored block. */
internal fun ProjectDoc.removeDiscardedOnRestore(blockKey: Key): ProjectDoc = copy(
    episodes = episodes.mapValues { (_, file) ->
        val keep = file.discardedVoiceIds.filterNot { it.blockKey == blockKey && it.reason.known == DiscardReason.DELETED }
        if (keep.size == file.discardedVoiceIds.size) file else file.copy(discardedVoiceIds = keep)
    },
)

/** Issues the next block number of a numbering unit (§4.1) and advances its counter. */
internal fun ProjectDoc.takeNextBlockNumber(numberingUnitKey: Key): Pair<Int, ProjectDoc> {
    if (numberingUnitKey == key && index.unit(numberingUnitKey) == null) {
        val next = project.counters.nextBlockNumber ?: 1
        return next to copy(project = project.copy(counters = project.counters.copy(nextBlockNumber = next + 1)))
    }
    val unit = index.unit(numberingUnitKey) ?: return 1 to this
    val next = unit.nextBlockNumber ?: 1
    return next to copy(
        story = story.copy(units = story.units.mapIf({ it.key == numberingUnitKey }) { it.copy(nextBlockNumber = next + 1) }),
    )
}
