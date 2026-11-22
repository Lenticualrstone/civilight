package io.github.lenticualrstone.civilight.feature.script.fold

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import io.github.lenticualrstone.civilight.feature.script.panel.PanelTab
import io.github.lenticualrstone.civilight.feature.script.state.EditorState
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRow

/**
 * One scroll state per panel tab, shared by the inner side panel and the cover sheet, so a tab keeps
 * its scroll when the panel changes shape (SCR-66) or another tab is shown (SCR-31 `[PROPOSED]`).
 * Starts from `script.panel.scroll`; [offsets] is written back when the screen stops.
 */
class PanelScrollMemory(stored: Map<String, Int>) {
    private val states: Map<PanelTab, ScrollState> = PanelTab.BUILT.associateWith { ScrollState(FoldRules.scrollOf(stored, it)) }

    fun state(tab: PanelTab): ScrollState = states[tab] ?: states.getValue(PanelTab.TIME)

    fun offsets(): Map<PanelTab, Int> = states.mapValues { it.value.value }
}

@Composable
fun rememberPanelScrollMemory(stored: Map<String, Int>): PanelScrollMemory = remember { PanelScrollMemory(stored) }

/** The last anchor seen, and the one to put back after a width change (plain fields: read in effects only). */
private class AnchorHolder {
    var width: Float = -1f
    var anchor: FoldRules.Anchor? = null
    var pending: FoldRules.Anchor? = null
}

/**
 * SCR-66 on the manuscript: the anchor (the caret's line, else the selected block, else the top row)
 * keeps its offset when the width changes; the blocks above it re-wrap and grow upward.
 */
@Composable
fun KeepAnchorOnResize(listState: LazyListState, rows: List<ScriptRow>, editor: EditorState, widthDp: Float) {
    val holder = remember { AnchorHolder() }
    val currentEditor by rememberUpdatedState(editor)
    // Composition runs before the new layout: the anchor seen at the old width is the one to keep.
    if (holder.width >= 0f && holder.width != widthDp && holder.pending == null) holder.pending = holder.anchor
    holder.width = widthDp
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.key.toString() to it.offset } }
            .collect { visible -> if (holder.pending == null) holder.anchor = FoldRules.anchorOf(visible, currentEditor) }
    }
    LaunchedEffect(widthDp) {
        val anchor = holder.pending ?: return@LaunchedEffect
        withFrameNanos { }
        holder.pending = null
        val (index, offset) = FoldRules.restoreScroll(rows.map { it.key }, anchor) ?: return@LaunchedEffect
        listState.scrollToItem(index)
        if (offset != 0) listState.scrollBy(-offset.toFloat())
    }
}

/**
 * SCR-66 on the panel: when the layout crosses between cover and inner, the open sheet becomes the
 * side panel and back ([FoldRules.onLayoutChange]).
 */
@Composable
fun PanelHandover(
    sideLayout: Boolean,
    sheetOpen: Boolean,
    panelOpen: Boolean,
    editing: Boolean,
    selecting: Boolean,
    onHandover: (FoldRules.Handover) -> Unit,
) {
    var was by rememberSaveable { mutableStateOf(sideLayout) }
    val latestSheetOpen by rememberUpdatedState(sheetOpen)
    val latestPanelOpen by rememberUpdatedState(panelOpen)
    LaunchedEffect(sideLayout) {
        val handover = FoldRules.onLayoutChange(was, sideLayout, latestSheetOpen, latestPanelOpen, editing, selecting)
        was = sideLayout
        handover?.let(onHandover)
    }
}
