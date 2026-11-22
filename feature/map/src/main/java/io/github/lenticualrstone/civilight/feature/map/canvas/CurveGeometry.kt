package io.github.lenticualrstone.civilight.feature.map.canvas

import io.github.lenticualrstone.civilight.model.Point
import kotlin.math.atan2
import kotlin.math.hypot

/** One glyph of a curved label: its centre on screen and the tangent angle in degrees. */
data class GlyphPlacement(val x: Float, val y: Float, val angle: Float)

/**
 * `곡선 라벨` (WLD-44): a label's control points (start, bend, end) make a quadratic curve; glyphs are
 * set along it, centred, each rotated to the tangent. Pure so the layout is tested on the JVM.
 */
object CurveGeometry {
    /** The default control points of a new curve around a label at [at]: a gentle arch [span] wide. */
    fun defaultCurve(at: Point, span: Float = 0.16f): List<Point> = listOf(
        Point((at.x - span / 2).coerceIn(0f, 1f), at.y.coerceIn(0f, 1f)),
        Point(at.x.coerceIn(0f, 1f), (at.y - span / 4).coerceIn(0f, 1f)),
        Point((at.x + span / 2).coerceIn(0f, 1f), at.y.coerceIn(0f, 1f)),
    )

    /** Samples a quadratic curve through 3 control points (a polyline for other counts). */
    fun sample(control: List<ScreenPoint>, steps: Int = 48): List<ScreenPoint> {
        if (control.size != 3) return control
        val (a, b, c) = control
        return (0..steps).map { i ->
            val t = i.toFloat() / steps
            val u = 1 - t
            ScreenPoint(u * u * a.x + 2 * u * t * b.x + t * t * c.x, u * u * a.y + 2 * u * t * b.y + t * t * c.y)
        }
    }

    fun length(path: List<ScreenPoint>): Float = path.zipWithNext { p, q -> hypot(q.x - p.x, q.y - p.y) }.sum()

    /**
     * Places glyphs of [widths] along [path], the text centred on the path's middle. Glyphs past
     * either end continue along the end segment's direction.
     */
    fun placeAlong(path: List<ScreenPoint>, widths: List<Float>): List<GlyphPlacement> {
        if (path.size < 2 || widths.isEmpty()) return emptyList()
        val total = widths.sum()
        var s = (length(path) - total) / 2
        val out = ArrayList<GlyphPlacement>(widths.size)
        for (w in widths) {
            out += pointAt(path, s + w / 2)
            s += w
        }
        return out
    }

    /** The point at arc length [s] with the tangent there. */
    fun pointAt(path: List<ScreenPoint>, s: Float): GlyphPlacement {
        var rest = s
        for (i in 0 until path.size - 1) {
            val p = path[i]
            val q = path[i + 1]
            val seg = hypot(q.x - p.x, q.y - p.y)
            val last = i == path.size - 2
            if ((rest <= seg || last) && (rest >= 0 || i == 0)) {
                val t = if (seg == 0f) 0f else rest / seg
                val angle = Math.toDegrees(atan2((q.y - p.y).toDouble(), (q.x - p.x).toDouble())).toFloat()
                return GlyphPlacement(p.x + (q.x - p.x) * t, p.y + (q.y - p.y) * t, angle)
            }
            rest -= seg
        }
        val p = path.first()
        return GlyphPlacement(p.x, p.y, 0f)
    }
}
