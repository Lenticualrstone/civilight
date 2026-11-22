package io.github.lenticualrstone.civilight.feature.script.records

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.SceneActions
import io.github.lenticualrstone.civilight.domain.script.Anchors
import io.github.lenticualrstone.civilight.domain.script.EditContext
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.Records
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * The three fixes of a `근거 지워짐` record (SCR-43) and `블록에 잇기` of a point-seat record (SCR-42),
 * over `domain:script` `Anchors` (the anchor engine, DECISIONS §1 rows "block edits" and the trash
 * status file's note that the orphan fix paths are domain:script's). Each is one undo step.
 */
object RecordFixes {
    /** `다른 블록에 잇기` / `블록에 잇기`: the record anchors on [blockKey]; that block is selected. */
    fun relink(doc: ProjectDoc, recordKey: Key, blockKey: Key, ctx: EditContext): ScriptOps.Outcome? {
        if (doc.index.record(recordKey)?.deletedAt != null) return null
        val out = Anchors.anchorToBlock(doc, recordKey, blockKey, ctx).takeIf { it !== doc } ?: return null
        return ScriptOps.Outcome(out, select = blockKey)
    }

    /** `이대로 두기`: the record stays on its current block; restoring the old block no longer re-attaches it. */
    fun keep(doc: ProjectDoc, recordKey: Key, ctx: EditContext): ScriptOps.Outcome? {
        val out = Anchors.keepHere(doc, recordKey, ctx).takeIf { it !== doc } ?: return null
        return ScriptOps.Outcome(out)
    }

    /** `기록 지우기`: the record goes to the trash (filter `변화`, 30 days), with a toast and `되돌리기`. */
    fun trash(doc: ProjectDoc, recordKey: Key, ctx: EditContext): ScriptOps.Outcome? {
        val record = doc.index.record(recordKey)?.takeIf { it.deletedAt == null } ?: return null
        val out = Anchors.trashRecord(doc, recordKey, ctx).takeIf { it !== doc } ?: return null
        return ScriptOps.Outcome(out, toast = ScriptCopy.recordTrashed(Records.shortTitle(doc, record)))
    }

    /** Result of `되살리기` on the trash note (SCR-39). */
    sealed interface Restore {
        data class Done(val outcome: ScriptOps.Outcome) : Restore

        /** The block's scene is in the trash too: the trash screen asks `씬도 되살리기 · 다른 씬 끝에`. */
        data object NeedsSceneChoice : Restore

        /** A hidden scene's block: authentication first. */
        data object NeedsAuth : Restore

        data object Nothing : Restore
    }

    /**
     * `되살리기` of a deleted original block (SCR-39) through `domain:actions` `SceneActions.restoreBlocks`
     * (DECISIONS §1): it returns between its neighbours with its number and voice state; records still
     * flagged on it re-attach. The block is selected; the toast is the trash area's restore message.
     */
    fun restoreBlock(doc: ProjectDoc, blockKey: Key, ctx: ActionContext, authenticated: Boolean): Restore {
        val block = doc.index.block(blockKey)?.takeIf { it.deletedAt != null } ?: return Restore.Nothing
        val entry = block.trashEntryKey ?: return Restore.Nothing
        return when (val r = SceneActions.restoreBlocks(doc, entry, listOf(blockKey), ctx, authenticated)) {
            is RestoreOutcome.Restored -> Restore.Done(
                ScriptOps.Outcome(r.doc, select = blockKey, toast = ScriptCopy.blockRestored(Labels.number(block.number))),
            )
            is RestoreOutcome.NeedsSceneChoice -> Restore.NeedsSceneChoice
            RestoreOutcome.NeedsAuth -> Restore.NeedsAuth
            RestoreOutcome.NotFound -> Restore.Nothing
        }
    }
}
