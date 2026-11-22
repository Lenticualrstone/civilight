package io.github.lenticualrstone.civilight.feature.map.canvas

import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.map.StampKind
import kotlin.math.cos
import kotlin.math.sin

/**
 * The 12 stamps of detailed terrain (WLD-60) as stroked polylines in a unit box (−1 … 1, y down),
 * shared by the map canvas and the export. Size steps `1 · 2 · 3` scale the box; rotation turns it.
 */
object StampGlyphs {
    private fun p(x: Float, y: Float) = Point(x, y)

    private fun circle(cx: Float, cy: Float, r: Float, n: Int = 12): List<Point> =
        (0..n).map { i -> val a = 2 * Math.PI * i / n; p(cx + r * cos(a).toFloat(), cy + r * sin(a).toFloat()) }

    fun strokes(kind: StampKind): List<List<Point>> = when (kind) {
        StampKind.MOUNTAIN -> listOf(listOf(p(-1f, 0.8f), p(-0.15f, -0.8f), p(0.25f, 0f), p(0.5f, -0.4f), p(1f, 0.8f)))
        StampKind.HILL -> listOf((0..8).map { i -> val a = Math.PI * i / 8; p(-cos(a).toFloat(), 0.6f - 0.9f * sin(a).toFloat()) })
        StampKind.FOREST -> listOf(circle(-0.4f, -0.1f, 0.45f), circle(0.4f, -0.1f, 0.45f), listOf(p(-0.4f, 0.35f), p(-0.4f, 0.9f)), listOf(p(0.4f, 0.35f), p(0.4f, 0.9f)))
        StampKind.CITY -> listOf(
            listOf(p(-1f, 0.8f), p(-1f, -0.2f), p(-0.3f, -0.2f), p(-0.3f, 0.8f)),
            listOf(p(-0.3f, 0.8f), p(-0.3f, -0.8f), p(0.4f, -0.8f), p(0.4f, 0.8f)),
            listOf(p(0.4f, 0.8f), p(0.4f, 0f), p(1f, 0f), p(1f, 0.8f), p(-1f, 0.8f)),
        )
        StampKind.TOWN -> listOf(listOf(p(-0.7f, 0.8f), p(-0.7f, -0.1f), p(0f, -0.8f), p(0.7f, -0.1f), p(0.7f, 0.8f), p(-0.7f, 0.8f)))
        StampKind.CASTLE -> listOf(
            listOf(
                p(-0.9f, 0.8f), p(-0.9f, -0.8f), p(-0.5f, -0.8f), p(-0.5f, -0.5f), p(-0.15f, -0.5f), p(-0.15f, -0.8f),
                p(0.15f, -0.8f), p(0.15f, -0.5f), p(0.5f, -0.5f), p(0.5f, -0.8f), p(0.9f, -0.8f), p(0.9f, 0.8f), p(-0.9f, 0.8f),
            ),
            listOf(p(-0.25f, 0.8f), p(-0.25f, 0.2f), p(0.25f, 0.2f), p(0.25f, 0.8f)),
        )
        StampKind.RUIN -> listOf(listOf(p(-0.9f, 0.8f), p(-0.9f, -0.3f), p(-0.5f, -0.6f), p(-0.5f, 0.1f)), listOf(p(-0.1f, 0.8f), p(-0.1f, -0.1f), p(0.3f, 0.2f)), listOf(p(0.6f, 0.8f), p(0.6f, -0.8f), p(0.9f, -0.5f)), listOf(p(-1f, 0.8f), p(1f, 0.8f)))
        StampKind.TOWER -> listOf(listOf(p(-0.4f, 0.8f), p(-0.3f, -0.6f), p(-0.5f, -0.6f), p(0f, -1f), p(0.5f, -0.6f), p(0.3f, -0.6f), p(0.4f, 0.8f), p(-0.4f, 0.8f)))
        StampKind.BRIDGE -> listOf((0..8).map { i -> val a = Math.PI * i / 8; p(-cos(a).toFloat(), 0.3f - 0.6f * sin(a).toFloat()) }, listOf(p(-1f, -0.3f), p(1f, -0.3f)), listOf(p(-1f, 0.3f), p(-1f, 0.8f)), listOf(p(1f, 0.3f), p(1f, 0.8f)))
        StampKind.PORT -> listOf(listOf(p(0f, -0.8f), p(0f, 0.8f)), listOf(p(-0.5f, -0.4f), p(0.5f, -0.4f)), (0..8).map { i -> val a = Math.PI * i / 8; p(-0.8f * cos(a).toFloat(), 0.2f + 0.6f * sin(a).toFloat()) }, circle(0f, -0.85f, 0.15f, 8))
        StampKind.CAVE -> listOf((0..8).map { i -> val a = Math.PI * i / 8; p(-0.9f * cos(a).toFloat(), 0.8f - 1.4f * sin(a).toFloat()) }, listOf(p(-1f, 0.8f), p(1f, 0.8f)), (0..6).map { i -> val a = Math.PI * i / 6; p(-0.4f * cos(a).toFloat(), 0.8f - 0.7f * sin(a).toFloat()) })
        StampKind.MARKER -> listOf(listOf(p(-0.35f, 0.8f), p(-0.35f, -0.5f), p(0f, -0.85f), p(0.35f, -0.5f), p(0.35f, 0.8f), p(-0.35f, 0.8f)), listOf(p(-0.7f, 0.8f), p(0.7f, 0.8f)))
    }

    /** Radius of a stamp in map widths for size 1 · 2 · 3. */
    fun radius(size: Int): Float = 0.008f * (size.coerceIn(1, 3) + 1)

    /** A stroke turned by [degrees] and moved to ([cx], [cy]) with radius [r] (any unit). */
    fun place(stroke: List<Point>, cx: Float, cy: Float, r: Float, degrees: Int): List<Pair<Float, Float>> {
        val a = Math.toRadians(degrees.toDouble())
        val c = cos(a).toFloat()
        val s = sin(a).toFloat()
        return stroke.map { q -> (cx + r * (q.x * c - q.y * s)) to (cy + r * (q.x * s + q.y * c)) }
    }
}

/** Automatic marks of hex cells (WLD-58 `표식(산 · 숲)은 지형 따라 자동`), in a unit box. */
object HexMarks {
    fun of(t: io.github.lenticualrstone.civilight.model.map.Terrain): List<List<Point>>? = when (t) {
        io.github.lenticualrstone.civilight.model.map.Terrain.MOUNTAIN,
        io.github.lenticualrstone.civilight.model.map.Terrain.PEAK -> StampGlyphs.strokes(StampKind.MOUNTAIN)
        io.github.lenticualrstone.civilight.model.map.Terrain.HILL -> StampGlyphs.strokes(StampKind.HILL)
        io.github.lenticualrstone.civilight.model.map.Terrain.FOREST -> StampGlyphs.strokes(StampKind.FOREST)
        else -> null
    }
}
