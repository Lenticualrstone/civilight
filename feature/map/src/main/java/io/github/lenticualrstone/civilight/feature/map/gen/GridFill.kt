package io.github.lenticualrstone.civilight.feature.map.gen

import kotlin.math.floor

/**
 * Lays fixed-width tiles (the DS `FormatTile` is 166 dp, a candidate is 150 dp) across a row without leaving a band of
 * empty width at the end (QA 2026-11-23 #7: the style grid stopped short of the right margin). It picks the number of
 * columns that fit with at least [MIN_GAP] between them and spreads what is left over evenly between the columns, so
 * the first tile sits at the left margin and the last at the right.
 */
object GridFill {
    const val MIN_GAP: Float = 10f

    /** How many [tile]-wide columns fit in [available] with at least [minGap] between them (at least 1). */
    fun columns(available: Float, tile: Float, minGap: Float = MIN_GAP): Int {
        if (available <= 0f || tile <= 0f) return 1
        return floor((available + minGap) / (tile + minGap)).toInt().coerceAtLeast(1)
    }

    /** The gap between [columns] tiles so that they span exactly [available]; [minGap] when there is one column. */
    fun gap(available: Float, tile: Float, columns: Int, minGap: Float = MIN_GAP): Float {
        if (columns <= 1) return minGap
        return ((available - columns * tile) / (columns - 1)).coerceAtLeast(minGap)
    }

    /** The tiles split into rows of [columns]. */
    fun <T> rows(items: List<T>, columns: Int): List<List<T>> = items.chunked(columns.coerceAtLeast(1))
}
