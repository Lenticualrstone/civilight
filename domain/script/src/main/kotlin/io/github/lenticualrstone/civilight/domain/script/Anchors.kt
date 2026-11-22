package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.checks.AckKind
import io.github.lenticualrstone.civilight.model.checks.Acknowledgement
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind

/**
 * Anchors of `◆` records and `◇` first reveals on blocks (SCR-42/43, §5.5): relocation when the
 * referent stops being live, re-attachment on restore, and the three fixes of an orphaned record.
 */
object Anchors {
    /** One record moved by [relocateOrphans]. */
    data class Relocation(val recordKey: Key, val from: Anchor, val to: Anchor, val isReveal: Boolean)

    private fun ProjectDoc.isReferentLive(anchor: Anchor): Boolean = when (anchor) {
        is Anchor.AtBlock -> manuscript.isLiveBlock(anchor.blockKey)
        is Anchor.UnitStart -> manuscript.isLiveUnit(anchor.unitKey)
        is Anchor.UnitEnd -> manuscript.isLiveUnit(anchor.unitKey)
        else -> true
    }

    /**
     * Moves every record (not directly trashed) whose anchor referent is no longer live to the next
     * surviving block (same scene → next scene → next episode), else `unplaced`, and flags it
     * `orphaned` with the original anchor (the first original is kept). Never moves earlier. Run it
     * once after all deletion marks of an operation are applied.
     */
    fun relocateOrphans(doc: ProjectDoc, ctx: EditContext): Pair<ProjectDoc, List<Relocation>> {
        val moves = mutableListOf<Relocation>()
        val updated = doc.changes.records.map { r ->
            if (r.deletedAt != null || doc.isReferentLive(r.anchor)) return@map r
            if (r.anchor !is Anchor.AtBlock && r.anchor !is Anchor.UnitStart && r.anchor !is Anchor.UnitEnd) return@map r
            val target = doc.manuscript.relocationTarget(r.anchor)
            moves += Relocation(r.key, r.anchor, target, r.isReveal)
            r.copy(
                anchor = target,
                anchorState = AnchorState.ORPHANED.open(),
                originalAnchor = if (r.anchorState.known == AnchorState.ORPHANED && r.originalAnchor != null) r.originalAnchor else r.anchor,
                updatedAt = ctx.now, updatedBy = ctx.device,
            )
        }
        return doc.copy(changes = doc.changes.copy(records = updated)) to moves
    }

    /**
     * Re-attaches every `orphaned` record whose original referent is live again (undo of a delete,
     * restore from the trash): anchor := original, `normal`. Records the author re-anchored or kept
     * meanwhile are no longer `orphaned` and stay where they are.
     */
    fun reattachOrphans(doc: ProjectDoc, ctx: EditContext): ProjectDoc = doc.mapRecords { r ->
        val original = r.originalAnchor
        if (r.anchorState.known != AnchorState.ORPHANED || original == null || !doc.isReferentLive(original)) r
        else r.copy(anchor = original, anchorState = AnchorState.NORMAL.open(), originalAnchor = null, updatedAt = ctx.now, updatedBy = ctx.device)
    }

    /** True for records drawn dotted at their block: `orphaned`, and `kept` (rule 20 §3 look, Q-20 default). */
    fun isDotted(record: ChangeRecord): Boolean =
        record.anchorState.known == AnchorState.ORPHANED || record.anchorState.known == AnchorState.KEPT

    /**
     * Candidates of `다른 블록에 잇기` / `블록에 잇기` (SCR-43, Q-21 default): the block right before
     * the current anchor first, then the other live blocks of that scene in manuscript order. For a
     * point-seat record, the live blocks of the first live scene of its unit.
     */
    fun relinkCandidates(doc: ProjectDoc, recordKey: Key): List<Block> {
        val record = doc.index.record(recordKey) ?: return emptyList()
        return when (val anchor = record.anchor) {
            is Anchor.AtBlock -> {
                val current = doc.index.block(anchor.blockKey) ?: return emptyList()
                val scene = doc.manuscript.liveBlocksOf(current.sceneKey)
                val i = scene.indexOfFirst { it.key == current.key }
                val before = if (i > 0) scene[i - 1] else null
                listOfNotNull(before) + scene.filter { it.key != current.key && it.key != before?.key }
            }
            is Anchor.UnitStart, is Anchor.UnitEnd -> {
                val unitKey = if (anchor is Anchor.UnitStart) anchor.unitKey else (anchor as Anchor.UnitEnd).unitKey
                firstLiveSceneOf(doc, unitKey)?.let { doc.manuscript.liveBlocksOf(it) }.orEmpty()
            }
            Anchor.Unplaced -> doc.manuscript.liveBlocks.takeLast(1)
            else -> emptyList()
        }
    }

    /** The first live scene (manuscript order) at or under [unitKey]. */
    fun firstLiveSceneOf(doc: ProjectDoc, unitKey: Key): Key? {
        val unit = doc.index.unit(unitKey) ?: return null
        if (unit.level == doc.project.structure.sceneLevel) return unitKey.takeIf { doc.manuscript.isLiveUnit(it) }
        return doc.manuscript.liveScenes.firstOrNull { s -> doc.index.ancestors(s).any { it.key == unitKey } }?.key
    }

    /**
     * `다른 블록에 잇기` / `블록에 잇기`: the record anchors on [blockKey] (seat block), `normal`, and
     * its remembered original is cleared. A `keep` acknowledgement of it is dropped.
     */
    fun anchorToBlock(doc: ProjectDoc, recordKey: Key, blockKey: Key, ctx: EditContext): ProjectDoc {
        if (!doc.manuscript.isLiveBlock(blockKey)) return doc
        return doc.mapRecords {
            if (it.key != recordKey) it
            else it.copy(
                anchor = Anchor.AtBlock(blockKey), anchorState = AnchorState.NORMAL.open(), originalAnchor = null,
                updatedAt = ctx.now, updatedBy = ctx.device,
            )
        }.dropAcks(CheckType.CHANGE_ANCHOR_DELETED, recordKey)
    }

    /**
     * `이대로 두기`: the record stays where it is, `kept`; the original is forgotten (restoring the
     * old block no longer re-attaches) and a `keep` acknowledgement is stored.
     */
    fun keepHere(doc: ProjectDoc, recordKey: Key, ctx: EditContext): ProjectDoc {
        val record = doc.index.record(recordKey) ?: return doc
        val ack = Acknowledgement(
            key = ctx.newKey(), checkType = CheckType.CHANGE_ANCHOR_DELETED, subjectKeys = listOf(recordKey),
            fingerprint = fingerprintOf(record), kind = AckKind.KEEP.open(), createdAt = ctx.now,
            updatedAt = ctx.now, updatedBy = ctx.device,
        )
        return doc.mapRecords {
            if (it.key != recordKey) it
            else it.copy(anchorState = AnchorState.KEPT.open(), originalAnchor = null, updatedAt = ctx.now, updatedBy = ctx.device)
        }.dropAcks(CheckType.CHANGE_ANCHOR_DELETED, recordKey)
            .let { it.copy(checks = it.checks.copy(acknowledgements = it.checks.acknowledgements + ack)) }
    }

    /** Fingerprint of a `change.anchorDeleted` item: the anchor the record sits on. */
    fun fingerprintOf(record: ChangeRecord): String = when (val a = record.anchor) {
        is Anchor.AtBlock -> "anchor:block:${a.blockKey}"
        is Anchor.UnitStart -> "anchor:unitStart:${a.unitKey}"
        is Anchor.UnitEnd -> "anchor:unitEnd:${a.unitKey}"
        is Anchor.WorldEvent -> "anchor:event:${a.eventKey}"
        else -> "anchor:unplaced"
    }

    /** `기록 지우기`: the record goes to the trash (filter `변화`, 30 days). */
    fun trashRecord(doc: ProjectDoc, recordKey: Key, ctx: EditContext): ProjectDoc {
        val record = doc.index.record(recordKey) ?: return doc
        if (record.deletedAt != null) return doc
        val entry = TrashEntry(
            key = ctx.newKey(), kind = TrashKind.CHANGE.open(), projectKey = doc.key, targetKeys = listOf(recordKey),
            deletedAt = ctx.now, purgeOn = TrashEntry.purgeDateFor(ctx.today),
        )
        return doc.mapRecords {
            if (it.key != recordKey) it else it.copy(deletedAt = ctx.now, trashEntryKey = entry.key, updatedAt = ctx.now, updatedBy = ctx.device)
        }.dropAcks(CheckType.CHANGE_ANCHOR_DELETED, recordKey).upsertTrashEntries(listOf(entry))
    }

    private fun ProjectDoc.dropAcks(checkType: String, subject: Key): ProjectDoc = copy(
        checks = checks.copy(acknowledgements = checks.acknowledgements.filterNot { it.checkType == checkType && subject in it.subjectKeys }),
    )
}
