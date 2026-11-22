package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.cast.relation.KindChip
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationDrafts
import io.github.lenticualrstone.civilight.domain.cast.relation.SheetSlot
import io.github.lenticualrstone.civilight.domain.cast.relation.SlotDraft
import io.github.lenticualrstone.civilight.feature.script.change.ChangeCommit
import io.github.lenticualrstone.civilight.feature.script.change.ChangeDraft
import io.github.lenticualrstone.civilight.feature.script.change.ChangeSheetKey
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.domain.world.common.EditContext as WorldContext

// The `◆ 변화 기록` sheet (SCR-41, SCR-45) on the view model: the draft and `기록`.

/** The sheet of [blockKey]: a fresh draft unless one for that block is still open. */
fun ScriptViewModel.changeSheetFor(blockKey: Key): ChangeSheetKey? {
    if (readOnly) return null
    val d = latestDoc ?: return null
    if (!d.manuscript.isLiveBlock(blockKey)) return null
    if (changeDraft.value?.blockKey != blockKey) changeDraft.value = ChangeDraft.start(d, blockKey)
    return ChangeSheetKey(projectKey, blockKey)
}

/** The draft of [blockKey] (started when the sheet came back after process death). */
fun ScriptViewModel.changeDraftOf(blockKey: Key): ChangeDraft? {
    val current = changeDraft.value
    if (current?.blockKey == blockKey) return current
    val d = latestDoc ?: return null
    return ChangeDraft.start(d, blockKey).also { changeDraft.value = it }
}

fun ScriptViewModel.editChange(transform: (ChangeDraft) -> ChangeDraft) {
    changeDraft.value = changeDraft.value?.let(transform)
}

/** `기록`: one undo step; the `◆` / `◇` shows on the block at once. */
fun ScriptViewModel.recordChange(draft: ChangeDraft) {
    if (readOnly) return
    enqueue {
        commitNow()
        val action = actionCtx()
        val world = WorldContext(newKey = action.newKey, now = { action.now }, device = action.device)
        update(ChangeCopyUndo.RECORD) { d -> ChangeCommit.record(d, draft, action.edit, world) ?: d }
        if (changeDraft.value?.blockKey == draft.blockKey) changeDraft.value = null
    }
}

/** `+ 새 종류` (CST-32 via `RelationDrafts.addKind`): its own undo step; the new chip is picked for [slot]. */
fun ScriptViewModel.addRelationKind(slot: SheetSlot, name: String) {
    if (readOnly || name.isBlank()) return
    enqueue {
        var chip: KindChip? = null
        val action = actionCtx()
        update(ChangeCopyUndo.ADD_KIND) { d ->
            RelationDrafts.addKind(d, slot, name, action.edit)?.let { (next, c) -> chip = c; next } ?: d
        }
        val picked = chip ?: return@enqueue
        editChange { draft ->
            val old = draft.slotDrafts[slot] ?: SlotDraft()
            draft.copy(slotDrafts = draft.slotDrafts + (slot to old.copy(kindKey = picked.kindKey, partnerHoldsFirst = picked.partnerHoldsFirst)))
        }
    }
}

/** Undo step names of the change sheet (its own words). */
internal object ChangeCopyUndo {
    const val RECORD = RelationDrafts.CHANGE_TITLE
    const val ADD_KIND = "새 종류"
}
