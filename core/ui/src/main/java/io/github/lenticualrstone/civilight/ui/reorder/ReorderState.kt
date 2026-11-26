package io.github.lenticualrstone.civilight.ui.reorder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler

/** One row of one list: [list] tells the lists of a screen apart (`factions`, `members:<faction>`, `loose`). */
data class ReorderKey(val list: String, val key: Any)

/**
 * The shared `순서 바꾸기` mode of a screen (board `ImmPlaceRules`). The text button ([toggle]) and a long press on a
 * row ([lift]) enter the same state; in it a tap picks a row ([select]). Only the picked row shows its grip and
 * floats; it moves within its own list only. One state serves every list of the screen, so only one row is picked.
 *
 * [offset] and [target] change every drag frame: read them only in `graphicsLayer` / draw lambdas.
 */
@Stable
class ReorderState {
    /** The mode is on (button reads `완료`). */
    var ordering: Boolean by mutableStateOf(false)
        private set

    /** The picked row, or null. */
    var selected: ReorderKey? by mutableStateOf(null)
        private set

    /** A finger is moving the picked row. */
    var dragging: Boolean by mutableStateOf(false)
        private set

    internal val offsetState = mutableFloatStateOf(0f)
    internal val targetState = mutableIntStateOf(-1)

    /** Index of the picked row in its list when the drag started (-1 when not dragging). */
    internal var from: Int = -1
        private set

    /** Measured row heights per list (px), read by the gesture only. */
    private val heights = HashMap<String, HashMap<Any, Float>>()

    val offset: Float get() = offsetState.floatValue
    val target: Int get() = targetState.intValue

    /** `순서 바꾸기` ↔ `완료`. Turning it on picks nothing (a tap or long press picks the row). */
    fun toggle() {
        if (ordering) exit() else ordering = true
    }

    /** Leaves the mode (`완료`, Back, the screen's content changed under it). */
    fun exit() {
        ordering = false
        selected = null
        endDrag()
    }

    /** A tap in the mode picks [key]; a tap on the picked row keeps it. */
    fun select(key: ReorderKey) {
        ordering = true
        if (selected != key) {
            endDrag()
            selected = key
        }
    }

    /** A long press: enters the mode with [key] picked, ready to be dragged. */
    fun lift(key: ReorderKey) = select(key)

    fun isSelected(list: String, key: Any): Boolean = selected == ReorderKey(list, key)

    /** Drops the pick when its row is no longer shown (lens, lock, `하위 지역 포함`). */
    fun keepShown(list: String, keys: Collection<Any>) {
        val s = selected ?: return
        if (s.list == list && s.key !in keys) {
            selected = null
            endDrag()
        }
    }

    internal fun measure(list: String, key: Any, height: Float) {
        heights.getOrPut(list) { HashMap() }[key] = height
    }

    internal fun heightsOf(list: String, keys: List<Any>): List<Float> {
        val m = heights[list].orEmpty()
        return keys.map { m[it] ?: 0f }
    }

    internal fun startDrag(from: Int) {
        this.from = from
        offsetState.floatValue = 0f
        targetState.intValue = from
        dragging = true
    }

    /** Adds [dy] and returns the new target index (unchanged when nothing crossed). */
    internal fun drag(dy: Float, heights: List<Float>): Int {
        offsetState.floatValue += dy
        val to = ReorderMath.target(heights, from, offsetState.floatValue)
        targetState.intValue = to
        return to
    }

    /** Ends the drag; returns `from to target`, or null when there was no drag. */
    internal fun endDrag(): Pair<Int, Int>? {
        val result = if (dragging) from to targetState.intValue else null
        dragging = false
        from = -1
        offsetState.floatValue = 0f
        targetState.intValue = -1
        return result
    }
}

@Composable
fun rememberReorderState(key: Any = Unit): ReorderState = remember(key) { ReorderState() }

/** Back closes the mode first (before the screen pops). */
@Composable
fun ReorderBackHandler(state: ReorderState) {
    LocalBackHandler.current(state.ordering) { state.exit() }
}
