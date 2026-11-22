package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.script.BlockMove
import io.github.lenticualrstone.civilight.domain.script.BlockPosition
import io.github.lenticualrstone.civilight.feature.script.select.MoveCopy
import io.github.lenticualrstone.civilight.feature.script.select.ScriptClipboard
import io.github.lenticualrstone.civilight.feature.script.select.SelectOps
import io.github.lenticualrstone.civilight.feature.script.select.SelectRules
import io.github.lenticualrstone.civilight.feature.script.state.EditorRules
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRow
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRowKeys
import io.github.lenticualrstone.civilight.model.Key
import kotlinx.coroutines.flow.update

// `블록 고르기` (SCR-36) and what its bar does (SCR-37 move, SCR-38 delete, SCR-40 cut · copy · paste).

/** Enters select mode: typing ends (its text is written), the selected block starts checked. */
fun ScriptViewModel.enterSelect(rows: List<ScriptRow>) {
    if (readOnly) return
    flushPending()
    val start = editorFlow.value.selected
    editorFlow.update(EditorRules::stopEditing)
    selectFlow.value = SelectRules.enter(start, SelectRules.drawnKeys(rows))
}

/** `완료`: leaves the mode; the checks go. */
fun ScriptViewModel.leaveSelect() {
    selectFlow.value = io.github.lenticualrstone.civilight.feature.script.select.SelectState()
}

fun ScriptViewModel.toggleChecked(blockKey: Key, rows: List<ScriptRow>) {
    selectFlow.update { SelectRules.toggle(it, blockKey, SelectRules.drawnKeys(rows)) }
}

/** `모두`: every drawn block of the scene under the header (again: none of them). */
fun ScriptViewModel.checkAll(rows: List<ScriptRow>, sceneKey: Key?) {
    selectFlow.update { SelectRules.all(it, rows, sceneKey) }
}

/** `옮기기` (picker) and the handle drag: [keys] in manuscript order to [destination]. */
fun ScriptViewModel.moveBlocks(keys: List<Key>, destination: BlockPosition) {
    if (readOnly || keys.isEmpty()) return
    enqueue {
        commitNow()
        val outcome = runOp(MoveCopy.MOVE) { d -> SelectOps.move(d, keys, destination, ctx()) } ?: return@enqueue
        outcome.toast?.let(::showUndoToast)
        requestScroll(ScriptRowKeys.block(keys.first()))
    }
}

/** `지우기` (SCR-38): no question; the domain toast says where records went. The checks go. */
fun ScriptViewModel.deleteChecked(keys: List<Key>) {
    if (keys.isEmpty()) return
    deleteBlocks(keys)
    selectFlow.update { it.copy(checked = emptySet()) }
}

/**
 * `잘라내기` / `복사` (SCR-40): the checked blocks go on the app clipboard and the mode ends, so a
 * block can be chosen as the paste position. A cut leaves its blocks in place until pasted (Q-19).
 */
fun ScriptViewModel.clipChecked(keys: List<Key>, cut: Boolean) {
    val d = latestDoc ?: return
    if (keys.isEmpty()) return
    ScriptClipboard.put(projectKey, if (cut) BlockMove.cut(d, keys) else BlockMove.copy(d, keys))
    leaveSelect()
}

/** Paste below the selected block (SCR-40); a cut is pasted once. */
fun ScriptViewModel.pasteBelow(blockKey: Key) {
    if (readOnly) return
    val entry = ScriptClipboard.of(projectKey) ?: return
    enqueue {
        commitNow()
        val outcome = runOp(MoveCopy.PASTE) { d -> SelectOps.paste(d, entry.clipboard, blockKey, ctx()) } ?: return@enqueue
        ScriptClipboard.afterPaste(entry)
        apply(outcome)
    }
}
