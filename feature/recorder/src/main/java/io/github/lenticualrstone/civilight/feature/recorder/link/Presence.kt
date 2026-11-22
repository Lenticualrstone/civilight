package io.github.lenticualrstone.civilight.feature.recorder.link

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/** One entry of a list that animates its members in and out: [alive] false = leaving (kept until its exit is done). */
data class Present<T>(val item: T, val key: Any, val alive: Boolean)

/**
 * The list shown while members come and go (pure, JVM-tested): [next] in order, plus every member of
 * [previous] that is no longer in [next], left in place as a leaving entry until its exit has played.
 */
object Presence {
    fun <T> merge(previous: List<Present<T>>, next: List<T>, key: (T) -> Any): List<Present<T>> {
        val result = next.map { Present(it, key(it), true) }.toMutableList()
        val live = result.map { it.key }.toSet()
        previous.forEachIndexed { index, entry ->
            if (entry.key !in live) result.add(index.coerceAtMost(result.size), entry.copy(alive = false))
        }
        return result
    }
}

/**
 * The members of [items] drawn as direct children of the parent (a chip row), each folding in from its
 * start edge on `foldOpen` and out on `foldClose` (fade only when reduced); a member that was there at
 * the first composition is simply shown. Neighbours follow the folding width, so a chip row never jumps.
 */
@Composable
fun <T> PresenceItems(items: List<T>, key: (T) -> Any, content: @Composable (T) -> Unit) {
    val motion = CivilightTheme.motion
    var entries by remember { mutableStateOf(items.map { Present(it, key(it), true) }) }
    val seen = remember { items.map(key).toMutableSet() }
    val shown = Presence.merge(entries, items, key)
    SideEffect { if (entries != shown) entries = shown }
    for (entry in shown) {
        key(entry.key) {
            val state = remember { MutableTransitionState(entry.key in seen).also { seen.add(entry.key) } }
            state.targetState = entry.alive
            AnimatedVisibility(
                visibleState = state,
                enter = motion.enter(CivilightMotion.foldOpen, expand = true, horizontal = true),
                exit = motion.exit(CivilightMotion.foldClose, shrink = true, horizontal = true),
            ) { content(entry.item) }
            if (!entry.alive && state.isIdle && !state.currentState) {
                SideEffect {
                    seen.remove(entry.key)
                    entries = entries.filterNot { it.key == entry.key && !it.alive }
                }
            }
        }
    }
}
