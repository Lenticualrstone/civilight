package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open

/** A record moved by a delete (SCR-43): from its old anchor to the first live block after it, or unplaced. */
data class Relocation(val recordKey: Key, val from: Anchor, val to: Anchor)

/** Anchor relocation and re-attachment (§5.5). */
internal object Anchors {
    /** True when the referent of [anchor] is live; event, unplaced and unknown anchors never relocate. */
    fun referentLive(doc: ProjectDoc, anchor: Anchor): Boolean = when (anchor) {
        is Anchor.AtBlock -> doc.liveness.isBlockLive(anchor.blockKey)
        is Anchor.UnitStart -> doc.liveness.isUnitLive(anchor.unitKey)
        is Anchor.UnitEnd -> doc.liveness.isUnitLive(anchor.unitKey)
        is Anchor.WorldEvent, Anchor.Unplaced, is Anchor.Unknown -> true
    }

    /**
     * Every record (not itself in the trash) whose anchor referent was live in [before] and is not
     * in [after] moves to the first live block after its old position (same scene → next scene →
     * next episode), else `unplaced`; it becomes `orphaned` and remembers the first original anchor.
     */
    fun relocate(before: ProjectDoc, after: ProjectDoc, at: Timestamp, by: String): Pair<ProjectDoc, List<Relocation>> {
        val moves = mutableListOf<Relocation>()
        val records = after.changes.records.map { record ->
            if (record.deletedAt != null) return@map record
            if (!referentLive(before, record.anchor) || referentLive(after, record.anchor)) return@map record
            val target = after.manuscript.relocationTarget(record.anchor)
            moves += Relocation(record.key, record.anchor, target)
            val original = if (record.anchorState.known == AnchorState.ORPHANED) record.originalAnchor ?: record.anchor else record.anchor
            record.copy(
                anchor = target, anchorState = AnchorState.ORPHANED.open(), originalAnchor = original,
                updatedAt = at, updatedBy = by,
            )
        }
        if (moves.isEmpty()) return after to emptyList()
        return after.copy(changes = after.changes.copy(records = records)) to moves
    }

    /**
     * Records still `orphaned` towards a referent that became live between [before] and [after] go
     * back: `anchor := originalAnchor`, `normal`, no original. Records in the trash stay as they are.
     */
    fun reattach(before: ProjectDoc, after: ProjectDoc, at: Timestamp, by: String): Pair<ProjectDoc, List<Key>> {
        val back = mutableListOf<Key>()
        val records = after.changes.records.map { record ->
            val original = record.originalAnchor
            if (record.deletedAt != null || record.anchorState.known != AnchorState.ORPHANED || original == null) return@map record
            if (referentLive(before, original) || !referentLive(after, original)) return@map record
            back += record.key
            record.copy(anchor = original, anchorState = AnchorState.NORMAL.open(), originalAnchor = null, updatedAt = at, updatedBy = by)
        }
        if (back.isEmpty()) return after to emptyList()
        return after.copy(changes = after.changes.copy(records = records)) to back
    }

    /**
     * Records that come back from their own trash mark (`기록 지우기` undone, §5.4) missed every
     * relocation and re-attachment while they were in the trash. Each one is settled against the
     * current manuscript: an `orphaned` record whose original referent is live again re-attaches;
     * otherwise a record whose anchor referent is not live relocates (§5.5). An orphaned record
     * whose original is still gone stays as it is.
     */
    fun settleRestored(doc: ProjectDoc, recordKeys: Set<Key>, at: Timestamp, by: String): Pair<ProjectDoc, SettledRecords> {
        val moves = mutableListOf<Relocation>()
        val back = mutableListOf<Key>()
        val records = doc.changes.records.map { record ->
            if (record.key !in recordKeys || record.deletedAt != null) return@map record
            val original = record.originalAnchor
            when {
                record.anchorState.known == AnchorState.ORPHANED && original != null && referentLive(doc, original) -> {
                    back += record.key
                    record.copy(anchor = original, anchorState = AnchorState.NORMAL.open(), originalAnchor = null, updatedAt = at, updatedBy = by)
                }
                !referentLive(doc, record.anchor) -> {
                    val target = doc.manuscript.relocationTarget(record.anchor)
                    moves += Relocation(record.key, record.anchor, target)
                    val first = if (record.anchorState.known == AnchorState.ORPHANED) original ?: record.anchor else record.anchor
                    record.copy(anchor = target, anchorState = AnchorState.ORPHANED.open(), originalAnchor = first, updatedAt = at, updatedBy = by)
                }
                else -> record
            }
        }
        if (moves.isEmpty() && back.isEmpty()) return doc to SettledRecords(emptyList(), emptyList())
        return doc.copy(changes = doc.changes.copy(records = records)) to SettledRecords(moves, back)
    }
}

/** What [Anchors.settleRestored] did. */
internal data class SettledRecords(val relocations: List<Relocation>, val reattached: List<Key>)
