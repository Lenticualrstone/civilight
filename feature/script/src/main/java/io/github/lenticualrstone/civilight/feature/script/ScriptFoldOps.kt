package io.github.lenticualrstone.civilight.feature.script

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.feature.script.fold.FoldRules
import io.github.lenticualrstone.civilight.feature.script.panel.PanelTab
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRowKeys
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.script.ScriptRestore
import kotlinx.coroutines.launch

// Fold and process-death continuity (SCR-66, SCR-31 per-tab scroll).

/**
 * Writes the restore point and the panel's per-tab scroll to `_app/view.json` (view state: no undo
 * step, also in read-only projects). Called when the screen stops, never per scroll frame.
 */
fun ScriptViewModel.saveFoldState(unitKey: Key, topRowKey: String?, panelScroll: Map<PanelTab, Int>) {
    val point = FoldRules.restorePoint(unitKey, editor.value, rawCaret, topRowKey)
    val scroll = FoldRules.storedScroll(panelScroll)
    viewModelScope.launch {
        updateApp { app ->
            val script = app.view.script
            val next = script.copy(restore = point, panel = script.panel.copy(scroll = scroll))
            if (next == script) app else app.copy(view = app.view.copy(script = next))
        }
    }
}

/**
 * Puts a restore point back on a restored entry (the device closed the app on fold): the block with
 * the caret at its offset (writable projects, text blocks), else the block selected; the screen
 * scrolls to [ScriptRestore.scrollAnchor]. Returns the row to scroll to.
 */
fun ScriptViewModel.restoreTo(point: ScriptRestore): String? {
    val doc = latestDoc ?: return null
    val block = point.selectedBlockKey?.takeIf { doc.manuscript.isLiveBlock(it) }
    if (block != null) {
        val offset = point.caretOffset
        val isText = doc.index.block(block)?.kind?.known in io.github.lenticualrstone.civilight.domain.script.BlockOps.TEXT_KINDS
        if (offset != null && isText && !readOnly) tapBlock(block, isText = true, rawOffset = offset) else selectBlock(block)
    }
    val anchor = point.scrollAnchor?.takeIf { doc.manuscript.isLiveBlock(it) } ?: block
    return anchor?.let(ScriptRowKeys::block)
}
