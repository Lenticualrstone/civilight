package io.github.lenticualrstone.civilight.feature.script.select

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.lenticualrstone.civilight.domain.script.BlockPosition
import io.github.lenticualrstone.civilight.feature.script.layout.ScriptLayout
import io.github.lenticualrstone.civilight.model.Key
import kotlinx.coroutines.delay

/**
 * Select mode's list (SCR-36): heads as dividers, block rows with checkbox, text and grip. Dragging a
 * grip moves the checked blocks (or that one block when it is not checked, SCR-37 `[PROPOSED]`); the
 * list scrolls by itself while the row is held near its top or bottom edge. The drop gap comes from
 * [DropTarget]; `옮기기` in the bar is the same move without a drag.
 */
@Composable
fun SelectList(
    rows: List<SelectRowModel>,
    listState: LazyListState,
    padding: PaddingValues,
    layout: ScriptLayout,
    onToggle: (Key) -> Unit,
    onDrop: (moving: List<Key>, position: BlockPosition) -> Unit,
) {
    var dragKey by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val edge = with(LocalDensity.current) { 56.dp.toPx() }
    val step = with(LocalDensity.current) { 12.dp.toPx() }

    fun draggedInfo() = dragKey?.let { k -> listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == k } }

    // Held near an edge: scroll, and keep the row under the finger by moving the offset with it.
    LaunchedEffect(dragKey) {
        while (dragKey != null) {
            val info = draggedInfo()
            if (info != null) {
                val top = info.offset + offset
                val bottom = top + info.size
                val view = listState.layoutInfo
                val delta = when {
                    top < view.viewportStartOffset + edge -> -step
                    bottom > view.viewportEndOffset - edge -> step
                    else -> 0f
                }
                if (delta != 0f) {
                    val used = listState.scrollBy(delta)
                    offset += used
                }
            }
            delay(16)
        }
    }

    fun drop() {
        val key = dragKey ?: return
        val info = draggedInfo()
        val from = rows.indexOfFirst { it.key == key }
        val dragged = rows.getOrNull(from) as? SelectRowModel.Block
        dragKey = null
        val shift = offset
        offset = 0f
        if (info == null || dragged == null) return
        val center = info.offset + shift + info.size / 2f
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key != key && center >= it.offset && center < it.offset + it.size }
            ?: return
        val to = rows.indexOfFirst { it.key == target.key }
        val moving = if (dragged.checked) rows.filterIsInstance<SelectRowModel.Block>().filter { it.checked }.map { it.blockKey } else listOf(dragged.blockKey)
        val dropRows = rows.map {
            when (it) {
                is SelectRowModel.Head -> DropTarget.Row.Head(it.sceneKey)
                is SelectRowModel.Block -> DropTarget.Row.Block(it.sceneKey, it.blockKey)
            }
        }
        DropTarget.of(dropRows, from, to, moving.toSet())?.let { onDrop(moving, it) }
    }

    val column = if (layout.wide) Modifier.padding(start = layout.columnStart.dp).width(layout.columnWidth.dp) else Modifier.fillMaxWidth()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxHeight().then(column),
        contentPadding = PaddingValues(top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(rows, key = { it.key }, contentType = { it::class }) { row ->
            val dragging = row.key == dragKey
            val lift = if (dragging) Modifier.zIndex(1f).graphicsLayer { translationY = offset } else Modifier
            when (row) {
                is SelectRowModel.Head -> SelectHeadView(row, lift)
                is SelectRowModel.Block -> SelectBlockRowView(
                    row,
                    onToggle = { onToggle(row.blockKey) },
                    grip = GripDrag(
                        onStart = { k ->
                            dragKey = k
                            offset = 0f
                        },
                        onDrag = { dy -> offset += dy },
                        onEnd = ::drop,
                        onCancel = {
                            dragKey = null
                            offset = 0f
                        },
                    ),
                    modifier = lift,
                )
            }
        }
    }
}
