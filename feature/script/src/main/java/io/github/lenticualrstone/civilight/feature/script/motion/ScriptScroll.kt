package io.github.lenticualrstone.civilight.feature.script.motion

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.Motion

/**
 * Scrolls the manuscript to a row it was asked to show (a jump from the command field, a new block,
 * the panel's `이 씬 안`): the row glides under the top bar on the `enter` curve. A row far off screen is
 * first put half a screen away, so the glide is never longer than that. Reduced motion: a cut.
 */
object ScriptScroll {
    /** Distance (px) from the viewport start the row has to travel to sit under the bar. */
    fun travel(rowOffset: Int, beforeContentPadding: Int): Float = (rowOffset - beforeContentPadding).toFloat()

    /** How far off its place a row is put before it glides: half the viewport. */
    fun glidePx(viewportHeight: Int): Int = (viewportHeight / 2).coerceAtLeast(0)

    suspend fun show(listState: LazyListState, index: Int, motion: Motion) {
        if (motion.reduced) {
            listState.scrollToItem(index)
            return
        }
        var info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        if (info == null) {
            val info0 = listState.layoutInfo
            listState.scrollToItem(index, -glidePx(info0.viewportEndOffset - info0.viewportStartOffset))
            info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        }
        if (info == null) {
            listState.scrollToItem(index)
            return
        }
        val distance = travel(info.offset, listState.layoutInfo.beforeContentPadding)
        if (distance != 0f) listState.animateScrollBy(distance, motion.spec(CivilightMotion.enter))
    }
}
