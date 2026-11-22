package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.cast.refs.DeletedItems
import io.github.lenticualrstone.civilight.domain.cast.refs.ReferenceSnapshot
import io.github.lenticualrstone.civilight.domain.trash.DeleteImpact
import io.github.lenticualrstone.civilight.domain.trash.DocChange
import io.github.lenticualrstone.civilight.domain.trash.Impact
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.domain.trash.PurgeOps
import io.github.lenticualrstone.civilight.domain.trash.PurgeResult
import io.github.lenticualrstone.civilight.domain.trash.Relocation
import io.github.lenticualrstone.civilight.domain.trash.RestoreOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.Toast
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashReason

/** What `휴지통으로` did to a character (CST-40). */
data class CharacterDeleteResult(
    val doc: ProjectDoc,
    /** The character's entry, holding the cast reference snapshot (`ext["castRefs"]`) and the trash reference sites. */
    val entry: TrashEntry,
    /** The impact as it was before the delete (sheet and toast counts). */
    val impact: DeleteImpact,
    /** `인물 노아 지움 · 참조 20 · 점검 센터` with `되돌리기`. */
    val toast: Toast,
    val relocations: List<Relocation>,
    val change: DocChange,
)

/** What `되살리기` of a character did, with the toast text (CST-43). */
data class CharacterRestoreResult(
    /** [RestoreOutcome.Restored] or why nothing happened (`NeedsAuth`, `NotFound`). */
    val outcome: RestoreOutcome,
    /**
     * `노아 되살림 · 다시 이어짐 · 대사 11 · @ 3 · 씬 등장 2 · 그대로 둔 것 · 대사 3` from the cast
     * snapshot written at delete time; the trash report when the entry has none; null when nothing
     * was restored.
     */
    val toast: Toast?,
)

/** A character's `영구 삭제` (CST-44) and its effect on the recorder library (links keep the name). */
data class CharacterPurgeResult(val purge: PurgeResult, val recorder: RecorderLibrary) {
    val doc: ProjectDoc get() = purge.doc
}

/**
 * Character delete, restore and purge (DECISIONS §1): `domain:trash` does the marks, anchors,
 * acknowledgements and owned rows; this object adds what only `domain:cast` knows: the reference
 * snapshot ([DeletedItems.snapshot]) stored in the trash entry, so the restore toast can say what
 * re-linked and what the author changed meanwhile (`그대로 둔 것`).
 */
object CharacterActions {
    /** The impact sheet input of deleting [characterKey] (CST-40). */
    fun impact(doc: ProjectDoc, characterKey: Key, recorder: RecorderLibrary? = null): DeleteImpact =
        Impact.of(doc, ItemId.character(characterKey), recorder)

    /**
     * Moves a live character to the trash. Null when it is unknown or already in the trash. The
     * entry carries [DeletedItems.withSnapshot] of the references just before the delete.
     */
    fun delete(doc: ProjectDoc, characterKey: Key, ctx: ActionContext, recorder: RecorderLibrary? = null): CharacterDeleteResult? {
        val character = doc.index.character(characterKey) ?: return null
        if (character.deletedAt != null) return null
        val impact = impact(doc, characterKey, recorder)
        val snapshot = DeletedItems.snapshot(doc, characterKey)
        val result = TrashOps.delete(doc, ItemId.character(characterKey), ctx.trash)
        val entry = result.entries.singleOrNull() ?: return null
        val withSnapshot = DeletedItems.withSnapshot(entry, snapshot)
        val d = result.doc.copy(trash = result.doc.trash.copy(entries = result.doc.trash.entries.map { if (it.key == entry.key) withSnapshot else it }))
        return CharacterDeleteResult(
            doc = d,
            entry = withSnapshot,
            impact = impact,
            toast = Messages.deleteToast(impact),
            relocations = result.relocations,
            change = DocChange(doc, d),
        )
    }

    /** `되돌리기` right after [delete]. */
    fun undo(current: ProjectDoc, result: CharacterDeleteResult): ProjectDoc = result.change.revert(current)

    /** The cast snapshot stored with [entry], or null (entries written before this action existed, merges). */
    fun snapshotOf(entry: TrashEntry): ReferenceSnapshot? = runCatching { DeletedItems.snapshotOf(entry) }.getOrNull()

    /**
     * `되살리기` of a trashed character through `domain:trash` [RestoreOps.restore] (a merged one comes
     * back from its `mergedOriginal`). Hidden characters need [authenticated].
     */
    fun restore(doc: ProjectDoc, characterKey: Key, ctx: ActionContext, authenticated: Boolean = false): CharacterRestoreResult {
        val character = doc.index.character(characterKey) ?: return CharacterRestoreResult(RestoreOutcome.NotFound, null)
        val entry = character.trashEntryKey?.let(doc.index::trashEntry) ?: return CharacterRestoreResult(RestoreOutcome.NotFound, null)
        val snapshot = snapshotOf(entry)
        val outcome = RestoreOps.restore(doc, entry.key, ctx.trash, authenticated)
        if (outcome !is RestoreOutcome.Restored) return CharacterRestoreResult(outcome, null)
        val toast = if (snapshot != null && entry.reason.known != TrashReason.MERGED) {
            Toast(DeletedItems.restoreReport(outcome.doc, characterKey, snapshot).toast)
        } else {
            Messages.restoreToast(outcome.report)
        }
        return CharacterRestoreResult(outcome, toast)
    }

    /**
     * `영구 삭제` of a trashed character: `domain:trash` [PurgeOps.purge] (owned rows, name tag,
     * `deleted.item` acknowledgements kept while referenced), then the recorder library's links to it
     * get `purgedAt` and keep the name ([PurgeActions.applyProjectPurge]).
     */
    fun purge(doc: ProjectDoc, recorder: RecorderLibrary, characterKey: Key, ctx: ActionContext): CharacterPurgeResult? {
        val entryKey = doc.index.character(characterKey)?.trashEntryKey ?: return null
        val result = PurgeOps.purge(doc, entryKey, ctx.trash)
        if (result.purgedEntries.isEmpty()) return null
        val labelled = PurgeActions.withPlainUnitLabels(doc, result)
        return CharacterPurgeResult(labelled, PurgeActions.applyProjectPurge(recorder, doc.key, labelled, ctx))
    }
}
