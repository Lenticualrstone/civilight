package io.github.lenticualrstone.civilight.feature.library.trash

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.CharacterActions
import io.github.lenticualrstone.civilight.domain.actions.PurgeActions
import io.github.lenticualrstone.civilight.domain.actions.SceneActions
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.domain.trash.PurgeOps
import io.github.lenticualrstone.civilight.domain.trash.PurgeResult
import io.github.lenticualrstone.civilight.domain.trash.RestoreOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.SceneChoice
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.trash.TrashKind

/** What `되살리기` of one entry inside a project did, and the toast it shows (null: none). */
data class ItemRestore(val outcome: RestoreOutcome, val toast: String?)

/**
 * What `영구 삭제` / `비우기` / the 30-day purge did inside one project: the document to write, and
 * [result] for the recorder links of purged things (null when nothing was purged).
 */
data class ItemPurge(val doc: ProjectDoc, val result: PurgeResult?) {
    val purged: Int get() = result?.purgedEntries?.size ?: 0

    /** Recorder links to purged things get `purgedAt` and keep their label (§5.3). */
    fun applyTo(recorder: RecorderLibrary, ctx: ActionContext): RecorderLibrary =
        result?.let { PurgeActions.applyProjectPurge(recorder, doc.key, it, ctx) } ?: recorder
}

/**
 * The one dispatch from a trash row to its owner's write path (DECISIONS §1): scenes and block
 * groups through `domain:actions` [SceneActions] (때 rebase), characters through [CharacterActions]
 * (cast reference snapshot, CST-43 toast), every other item through `domain:trash` [RestoreOps].
 * Purges go through [PurgeOps] with the recorder links of purged things keeping their label
 * ([PurgeActions.applyProjectPurge]), as `CharacterActions.purge` does.
 */
object TrashDecisions {
    fun restoreItem(
        doc: ProjectDoc,
        entryKey: Key,
        ctx: ActionContext,
        authenticated: Boolean,
        sceneChoice: SceneChoice? = null,
    ): ItemRestore {
        val entry = doc.index.trashEntry(entryKey) ?: return ItemRestore(RestoreOutcome.NotFound, null)
        return when (entry.kind.known) {
            TrashKind.SCENE -> {
                val outcome = SceneActions.restoreEntry(doc, entryKey, ctx, authenticated)
                ItemRestore(outcome, (outcome as? RestoreOutcome.Restored)?.let { toastOf(it) })
            }
            // Blocks come back in place without a toast of their own (`ED`; the sources give none).
            TrashKind.BLOCK_GROUP -> ItemRestore(SceneActions.restoreEntry(doc, entryKey, ctx, authenticated, sceneChoice), null)
            TrashKind.CHARACTER -> {
                val key = entry.targetKeys.singleOrNull() ?: return ItemRestore(RestoreOutcome.NotFound, null)
                val result = CharacterActions.restore(doc, key, ctx, authenticated)
                ItemRestore(result.outcome, result.toast?.message)
            }
            else -> {
                val outcome = RestoreOps.restore(doc, entryKey, ctx.trash, authenticated, sceneChoice)
                ItemRestore(outcome, (outcome as? RestoreOutcome.Restored)?.let { toastOf(it) })
            }
        }
    }

    /** One block of an expanded block group (its own `되살리기`, R12 §7). */
    fun restoreBlocks(
        doc: ProjectDoc,
        entryKey: Key,
        blockKeys: List<Key>,
        ctx: ActionContext,
        authenticated: Boolean,
        sceneChoice: SceneChoice? = null,
    ): ItemRestore = ItemRestore(SceneActions.restoreBlocks(doc, entryKey, blockKeys, ctx, authenticated, sceneChoice), null)

    /** `영구 삭제` of [entryKeys] (one row, `비우기`, or the entries due today). */
    fun purge(doc: ProjectDoc, entryKeys: List<Key>, ctx: ActionContext): ItemPurge {
        if (entryKeys.isEmpty()) return ItemPurge(doc, null)
        val result = PurgeActions.withPlainUnitLabels(doc, PurgeOps.purgeEntries(doc, entryKeys, ctx.trash))
        if (result.purgedEntries.isEmpty()) return ItemPurge(doc, null)
        return ItemPurge(result.doc, result)
    }

    /** Entries of [doc] whose 30 days are over (`d ≤ 0`, T-08); the daily purge's share of the 휴지통. */
    fun dueEntries(doc: ProjectDoc, ctx: ActionContext): List<Key> =
        doc.trash.entries.filter(ctx.trash::isDue).map { it.key }

    private fun toastOf(restored: RestoreOutcome.Restored): String? =
        restored.report.name?.takeIf { it.isNotBlank() }?.let { Messages.restoreToast(restored.report).message }
}
