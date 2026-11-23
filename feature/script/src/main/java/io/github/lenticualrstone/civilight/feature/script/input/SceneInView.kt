package io.github.lenticualrstone.civilight.feature.script.input

import io.github.lenticualrstone.civilight.model.Key

/**
 * (0.2.0, U-14 #21, SCR-71) The scene of the manuscript item directly above the input bar. The position line
 * and `씬 n 끝에` both use it, so they always name the same scene. A locked hidden scene is never a target.
 */
object SceneInView {
    enum class Kind { HEAD, BLOCK, HIDDEN_ROW }

    /** One laid-out item: its scene, kind and vertical extent (any unit, same as [target]'s `inputTop`). */
    data class Item(val sceneKey: Key, val kind: Kind, val top: Float, val bottom: Float)

    fun target(items: List<Item>, inputTop: Float): Key? {
        val candidates = items.filter { it.kind != Kind.HIDDEN_ROW }
        candidates.lastOrNull { it.top < inputTop }?.let { return it.sceneKey }
        return candidates.firstOrNull { it.top >= inputTop }?.sceneKey
    }
}

/**
 * (0.2.0, U-14 #16, #17, SCR-71) Bottom anchoring: when the input bar's top edge moves (the field grows, the tool
 * or mark row appears, the keyboard opens), the manuscript scrolls by the same amount in the same frame, so the
 * item directly above the bar keeps its distance to it. A user drag suspends it (DS-69).
 */
object BottomAnchor {
    /** Pixels to scroll forward when the bar's top moved from [oldInputTop] to [newInputTop]; 0 while dragging. */
    fun delta(oldInputTop: Int?, newInputTop: Int, dragging: Boolean): Float {
        if (oldInputTop == null || dragging) return 0f
        return (oldInputTop - newInputTop).toFloat()
    }
}
