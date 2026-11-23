package io.github.lenticualrstone.civilight.ui.tour

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Where each step of the enclosing tooltip tour (LIB-19) has its target, in root coordinates (filled by
 * [tourTarget]), and whether that tour is showing. Shared so every feature module can mark its targets
 * and step out of typing while its tour runs (the tour host lives in `feature:library`).
 */
class TourTargets {
    val rects = mutableStateMapOf<Int, Rect>()

    /**
     * Second-choice holes (filled by `tourTarget(step, fallback = true)`): used for a step only while its
     * own target ([rects]) is missing or off screen, so its card still points at the nearest control.
     */
    val fallbacks = mutableStateMapOf<Int, Rect>()

    /**
     * Steps this content has nothing for (filled by [TourStepAbsent]): the tour leaves them out and
     * counts only the rest (`1/3`), so a card never explains an element that is not on screen.
     */
    val absent = mutableStateMapOf<Int, Boolean>()

    /** How many [TourHold]s are composed: while above 0 the tour waits (the content is still loading). */
    var holds: Int by mutableIntStateOf(0)

    /** True while the tour over this content is shown (set by the tour host). */
    var running: Boolean by mutableStateOf(false)
}

val LocalTourTargets = compositionLocalOf<TourTargets?> { null }

/**
 * Which tour steps are shown, in order, as their original 0-based ids (the ids [tourTarget] marks):
 * every step of [count] except the [absent] ones.
 */
object TourStepFilter {
    fun shown(count: Int, absent: Set<Int>): List<Int> = (0 until count).filter { it !in absent }
}

/**
 * Declares that this content has no target for tour step [step] (while it stays composed): the
 * enclosing tour skips that step. Nothing happens outside a tour host.
 */
@Composable
fun TourStepAbsent(step: Int) {
    val targets = LocalTourTargets.current ?: return
    DisposableEffect(targets, step) {
        targets.absent[step] = true
        onDispose { targets.absent.remove(step) }
    }
}

/**
 * Holds the enclosing tour back while this stays composed: content that is still loading cannot yet
 * say which steps it has ([TourStepAbsent]), so no card shows a step that is about to drop out.
 */
@Composable
fun TourHold() {
    val targets = LocalTourTargets.current ?: return
    DisposableEffect(targets) {
        targets.holds += 1
        onDispose { targets.holds -= 1 }
    }
}

/** Whether a tour is showing over this content (false outside any tour host). */
@Composable
fun tourRunning(): Boolean = LocalTourTargets.current?.running == true

/**
 * Marks this element as the hole of tour step [step] (0-based) while a tour host encloses it. The rect
 * goes when the element leaves (unless another element took the step since), so a hole never points at
 * a place where nothing is drawn. With [fallback] the element is the step's second choice
 * ([TourTargets.fallbacks]): the hole goes there only while the step's own target is missing or off
 * screen. Give each step at most one element per tier (the last one placed wins).
 */
fun Modifier.tourTarget(step: Int, fallback: Boolean = false): Modifier = composed {
    val targets = LocalTourTargets.current
    if (targets == null) {
        this
    } else {
        val map = if (fallback) targets.fallbacks else targets.rects
        val mine = remember { arrayOfNulls<Rect>(1) }
        DisposableEffect(targets, step, fallback) {
            onDispose { if (mine[0] != null && map[step] == mine[0]) map.remove(step) }
        }
        onGloballyPositioned {
            val rect = it.boundsInRoot()
            mine[0] = rect
            map[step] = rect
        }
    }
}
