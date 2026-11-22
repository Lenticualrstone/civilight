package io.github.lenticualrstone.civilight.feature.map.canvas

import io.github.lenticualrstone.civilight.model.Point
import kotlin.math.hypot

/**
 * How painted hex cells change colour (motion audit row 10): all together within one `standard` run (200 ms, under the
 * 300 ms cap), but a cell starts later the further it is from the middle of the change, so a fill spreads outwards.
 * Pure, so it is tested on the JVM.
 */
object HexSpread {
    /** The part of the run one cell's own change takes; the rest is the spread. */
    const val WINDOW = 0.4f

    /** The indices whose value differs between [previous] and [now] (same length; otherwise none). */
    fun <T> changed(previous: List<T>, now: List<T>): List<Int> =
        if (previous.size != now.size) emptyList() else now.indices.filter { previous[it] != now[it] }

    /**
     * For each of [indices] (cell centres in [centres], same order) the moment it starts, 0 (first) to 1 (last), by its
     * distance from the middle of all of them.
     */
    fun starts(indices: List<Int>, centres: List<Point>): Map<Int, Float> {
        if (indices.isEmpty()) return emptyMap()
        val cx = centres.map { it.x }.average().toFloat()
        val cy = centres.map { it.y }.average().toFloat()
        val dist = centres.map { hypot((it.x - cx).toDouble(), (it.y - cy).toDouble()).toFloat() }
        val far = dist.max()
        return indices.indices.associate { n -> indices[n] to if (far <= 0f) 0f else dist[n] / far }
    }

    /** One cell's change (0 not started, 1 done) at the run's [progress] (0–1) for a cell that starts at [start]. */
    fun local(progress: Float, start: Float): Float = ((progress - start * (1f - WINDOW)) / WINDOW).coerceIn(0f, 1f)
}
