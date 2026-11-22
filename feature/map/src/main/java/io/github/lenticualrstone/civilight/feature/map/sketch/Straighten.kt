package io.github.lenticualrstone.civilight.feature.map.sketch

import io.github.lenticualrstone.civilight.feature.map.canvas.TilePlan
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.asset.Blob
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/** What `펴기` produced: the original photo, the straightened image and the corners used. */
data class SketchResult(val photo: Blob, val straightened: Blob, val corners: List<Point>)

/**
 * `펴기` (WLD-39 `[PROPOSED]`): four corner handles on the photo (0–1 of the photo, order TL TR BR
 * BL) and a perspective correction into a rectangle. Pure parts: default corners, tap-to-place,
 * the output size. No ML or cloud service.
 */
object Straighten {
    /** Handles start a little inside the photo so each one can be grabbed. */
    const val INSET = 0.08f

    fun defaultCorners(): List<Point> = listOf(
        Point(INSET, INSET), Point(1 - INSET, INSET), Point(1 - INSET, 1 - INSET), Point(INSET, 1 - INSET),
    )

    /** Moves corner [index] to [to], kept on the photo. */
    fun move(corners: List<Point>, index: Int, to: Point): List<Point> =
        corners.mapIndexed { i, p -> if (i == index) Point(to.x.coerceIn(0f, 1f), to.y.coerceIn(0f, 1f)) else p }

    /**
     * Pixel size of the straightened image for a photo of [photoW] × [photoH]: the longer of each
     * pair of opposite edges, capped so the long edge stays within the full-decode limit.
     */
    fun outputSize(corners: List<Point>, photoW: Int, photoH: Int): Pair<Int, Int> {
        fun d(a: Point, b: Point) = hypot(((a.x - b.x) * photoW).toDouble(), ((a.y - b.y) * photoH).toDouble())
        val (tl, tr, br, bl) = corners
        var w = max(d(tl, tr), d(bl, br))
        var h = max(d(tl, bl), d(tr, br))
        val long = max(w, h)
        if (long > TilePlan.FULL_DECODE_MAX) {
            val k = TilePlan.FULL_DECODE_MAX / long
            w *= k
            h *= k
        }
        return w.roundToInt().coerceAtLeast(1) to h.roundToInt().coerceAtLeast(1)
    }

    /** Corners as `x0, y0, x1, y1 …` for `rememberSaveable`. */
    fun toArray(corners: List<Point>): FloatArray = corners.flatMap { listOf(it.x, it.y) }.toFloatArray()

    fun fromArray(a: FloatArray?): List<Point> =
        if (a == null || a.size != 8) defaultCorners() else (0 until 4).map { Point(a[it * 2], a[it * 2 + 1]) }
}
