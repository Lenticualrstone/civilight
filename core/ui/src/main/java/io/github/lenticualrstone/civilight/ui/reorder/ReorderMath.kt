package io.github.lenticualrstone.civilight.ui.reorder

/**
 * Pure part of the shared `순서 바꾸기` rows (board `ImmPlaceRules`): where a lifted row would drop, and where the
 * accent line goes. JVM-tested; [ReorderItem] only feeds it measured heights and the finger's offset.
 *
 * The rows of one list are laid out one under the other with no gap (hairlines are inside the rows), so a row's
 * top is the sum of the heights above it.
 */
object ReorderMath {
    /**
     * The index the row at [from] lands on when dragged by [offset] px (positive = down): it passes a neighbour once
     * its centre crosses that neighbour's centre. Heights not measured yet (≤ 0) stop the walk. Returns [from] when
     * nothing moves or [from] is out of range.
     */
    fun target(heights: List<Float>, from: Int, offset: Float): Int {
        if (from !in heights.indices || offset == 0f) return from
        val tops = FloatArray(heights.size)
        var y = 0f
        heights.forEachIndexed { i, h ->
            tops[i] = y
            y += h.coerceAtLeast(0f)
        }
        val center = tops[from] + heights[from] / 2f + offset
        var to = from
        if (offset > 0f) {
            for (i in from + 1..heights.lastIndex) {
                if (heights[i] <= 0f || center <= tops[i] + heights[i] / 2f) break
                to = i
            }
        } else {
            for (i in from - 1 downTo 0) {
                if (heights[i] <= 0f || center >= tops[i] + heights[i] / 2f) break
                to = i
            }
        }
        return to
    }

    /** Where the drop line is drawn on the row at [index] while the row at [from] would land on [to]. */
    enum class Line { NONE, TOP, BOTTOM }

    /**
     * Moving up, the line sits on top of the row the dragged one will replace; moving down, under it. No line on the
     * dragged row itself or when nothing moves.
     */
    fun line(index: Int, from: Int, to: Int): Line = when {
        from < 0 || to < 0 || from == to || index != to -> Line.NONE
        to < from -> Line.TOP
        else -> Line.BOTTOM
    }

    /** `위로 옮기기` target, or null on the first row. */
    fun up(index: Int): Int? = (index - 1).takeIf { index > 0 }

    /** `아래로 옮기기` target, or null on the last row of [size]. */
    fun down(index: Int, size: Int): Int? = (index + 1).takeIf { index in 0 until size - 1 }
}
