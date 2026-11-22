package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.records.RecordFixes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.Trash
import io.github.lenticualrstone.civilight.navigation.TrashFilter

// The script's record fixes (SCR-39, SCR-42, SCR-43) on the view model's op queue: each one store
// update, so each is one undo step.

/** `다른 블록에 잇기` / `블록에 잇기`: [recordKey] anchors on [blockKey], which is then selected and shown. */
fun ScriptViewModel.relinkRecord(recordKey: Key, blockKey: Key) {
    if (readOnly) return
    enqueue {
        commitNow()
        apply(runOp(ScriptCopy.UNDO_RELINK) { d -> RecordFixes.relink(d, recordKey, blockKey, ctx()) })
    }
}

/** `이대로 두기` on an orphaned record (SCR-43). */
fun ScriptViewModel.keepRecord(recordKey: Key) {
    if (readOnly) return
    enqueue {
        commitNow()
        runOp(ScriptCopy.UNDO_KEEP) { d -> RecordFixes.keep(d, recordKey, ctx()) }
    }
}

/** `기록 지우기` (SCR-43): to the trash, with the toast's `되돌리기`. */
fun ScriptViewModel.trashRecord(recordKey: Key) {
    if (readOnly) return
    enqueue {
        commitNow()
        runOp(ScriptCopy.UNDO_TRASH_RECORD) { d -> RecordFixes.trash(d, recordKey, ctx()) }?.toast?.let(::showUndoToast)
    }
}

/**
 * `되살리기` on the trash note (SCR-39): the deleted block comes back at its place. When its scene
 * is in the trash too, the trash screen's `블록` filter takes over (it asks `씬도 되살리기 · 다른 씬 끝에`).
 */
fun ScriptViewModel.restoreDeletedBlock(blockKey: Key, authenticated: Boolean) {
    if (readOnly) return
    enqueue {
        commitNow()
        var result: RecordFixes.Restore = RecordFixes.Restore.Nothing
        val context = actionCtx()
        update(ScriptCopy.UNDO_RESTORE) { d ->
            result = RecordFixes.restoreBlock(d, blockKey, context, authenticated)
            (result as? RecordFixes.Restore.Done)?.outcome?.doc ?: d
        }
        when (val r = result) {
            is RecordFixes.Restore.Done -> apply(r.outcome)
            RecordFixes.Restore.NeedsSceneChoice -> featureDeps.navigator.push(Trash(TrashFilter.BLOCK))
            else -> Unit
        }
    }
}
