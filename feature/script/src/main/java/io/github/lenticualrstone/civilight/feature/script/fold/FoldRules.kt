package io.github.lenticualrstone.civilight.feature.script.fold

import io.github.lenticualrstone.civilight.feature.script.panel.PanelTab
import io.github.lenticualrstone.civilight.feature.script.state.EditorState
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRowKeys
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.script.ScriptRestore

/**
 * Fold and unfold continuity (SCR-66, boards `MFoldScript`, `FoldSpec`): the cover bottom sheet and
 * the inner side panel are the same element. Only its shape changes; its tab and per-tab scroll
 * stay. A closed panel stays closed. While typing, the caret and the keyboard stay, so folding then
 * opens no sheet. The anchor row keeps its height on screen.
 */
object FoldRules {
    /** What the screen does when the layout crosses between cover and inner. */
    data class Handover(
        /** Open the cover panel sheet (the side panel was showing). */
        val openSheet: Boolean = false,
        /** Close the cover panel sheet (the side panel takes over). */
        val closeSheet: Boolean = false,
        /** The inner panel's open state from now on. */
        val panelOpen: Boolean,
    )

    /**
     * [wasSide] / [isSide]: whether the layout draws the side panel before and after (`ScriptLayout.panel`
     * for an open panel). [sheetOpen]: the cover panel sheet is the top sheet. Null when the layout did
     * not cross.
     */
    fun onLayoutChange(
        wasSide: Boolean,
        isSide: Boolean,
        sheetOpen: Boolean,
        panelOpen: Boolean,
        editing: Boolean,
        selecting: Boolean,
    ): Handover? = when {
        wasSide == isSide -> null
        // Unfold: the sheet becomes the side panel; no sheet = a closed panel stays closed.
        isSide -> Handover(closeSheet = sheetOpen, panelOpen = sheetOpen)
        // Fold: an open side panel becomes the sheet, unless typing (caret and keyboard stay) or selecting.
        else -> Handover(openSheet = panelOpen && !editing && !selecting, panelOpen = panelOpen)
    }

    /** The row to keep at its height: the caret's row, else the selected block, else the first visible row. */
    data class Anchor(val rowKey: String, val offsetPx: Int)

    /** [visible] = (row key, offset from the list's start in px) of the drawn rows, top first. */
    fun anchorOf(visible: List<Pair<String, Int>>, editor: EditorState): Anchor? {
        val wanted = listOfNotNull(editor.caretRowKey, editor.selected?.let(ScriptRowKeys::block))
        for (key in wanted) visible.firstOrNull { it.first == key }?.let { return Anchor(it.first, it.second) }
        return visible.firstOrNull()?.let { Anchor(it.first, it.second) }
    }

    /**
     * Where to scroll after the width changed: the anchor's index and how far to scroll back so its
     * top sits at the same offset again (rows above it re-wrap and grow upward). Null when the row is gone.
     */
    fun restoreScroll(rowKeys: List<String>, anchor: Anchor): Pair<Int, Int>? {
        val index = rowKeys.indexOf(anchor.rowKey).takeIf { it >= 0 } ?: return null
        return index to anchor.offsetPx
    }

    /**
     * The restore point written when the screen stops (SCR-66: a device that closes apps on fold):
     * the episode, the block with the caret (or the selected one), the caret offset and the block at
     * the top of the viewport. The tab is remembered on its own (`script.panel.tab`).
     */
    fun restorePoint(unitKey: Key, editor: EditorState, caretOffset: Int?, topRowKey: String?): ScriptRestore =
        ScriptRestore(
            unitKey = unitKey,
            selectedBlockKey = editor.caretBlock ?: editor.selected,
            caretOffset = caretOffset.takeIf { editor.caretBlock != null },
            scrollAnchor = topRowKey?.let(ScriptRowKeys::blockKeyOf),
        )

    /**
     * What a restored entry applies: only a point of the same episode. The route's own target (a
     * `blockKey`, `caretAtEnd`) already put a selection on the new view model, so the saved point
     * wins over it: it is where the writer was when the app went.
     */
    fun shouldRestore(point: ScriptRestore?, unitKey: Key): Boolean = point != null && point.unitKey == unitKey

    /** Per-tab scroll offsets as stored in `script.panel.scroll` (tab serial name → offset). */
    fun storedScroll(offsets: Map<PanelTab, Int>): Map<String, Int> =
        offsets.filterValues { it > 0 }.mapKeys { (tab, _) -> serialName(tab) }

    fun scrollOf(stored: Map<String, Int>, tab: PanelTab): Int = stored[serialName(tab)] ?: 0

    private fun serialName(tab: PanelTab): String = tab.stored.name.lowercase()
}
