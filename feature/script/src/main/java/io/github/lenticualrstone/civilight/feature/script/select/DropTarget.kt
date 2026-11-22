package io.github.lenticualrstone.civilight.feature.script.select

import io.github.lenticualrstone.civilight.domain.script.BlockPosition
import io.github.lenticualrstone.civilight.model.Key

/**
 * Handle drag in select mode (SCR-37 `[PROPOSED]`): the row a dragged block is dropped on decides
 * the gap it goes into. Dragged down, the blocks go below the row under the finger; dragged up,
 * above it. A scene head is a gap too (dropped below a head = first in that scene; above a head =
 * last in the scene before), so blocks cross scene heads (any scene of the episode).
 */
object DropTarget {
    /** The select list as the drag sees it, in order. */
    sealed interface Row {
        val sceneKey: Key

        data class Head(override val sceneKey: Key) : Row
        data class Block(override val sceneKey: Key, val blockKey: Key) : Row
    }

    /**
     * Where the blocks go when the row at [from] is dropped on the row at [to]; null when nothing moves
     * (dropped on itself, or between rows that are all moving).
     */
    fun of(rows: List<Row>, from: Int, to: Int, moving: Set<Key>): BlockPosition? {
        if (from == to || from !in rows.indices || to !in rows.indices) return null
        return if (to > from) after(rows, to, moving) else before(rows, to, moving)
    }

    /** The gap below row [i]. */
    private fun after(rows: List<Row>, i: Int, moving: Set<Key>): BlockPosition? = when (val r = rows[i]) {
        is Row.Head -> BlockPosition.SceneStart(r.sceneKey)
        is Row.Block -> if (r.blockKey !in moving) BlockPosition.After(r.blockKey) else before(rows, i, moving)
    }

    /** The gap above row [i]: below the nearest row above it that is not moving. */
    private fun before(rows: List<Row>, i: Int, moving: Set<Key>): BlockPosition? {
        var j = i - 1
        while (j >= 0) {
            when (val r = rows[j]) {
                is Row.Head -> return if (rows[i] is Row.Head) BlockPosition.SceneEnd(r.sceneKey) else BlockPosition.SceneStart(r.sceneKey)
                is Row.Block -> if (r.blockKey !in moving) {
                    return BlockPosition.After(r.blockKey)
                }
            }
            j--
        }
        // Above the first row of the list: first in that row's scene.
        return BlockPosition.SceneStart(rows[i].sceneKey)
    }
}
