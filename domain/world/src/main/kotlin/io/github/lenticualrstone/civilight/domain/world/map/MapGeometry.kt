package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.model.Point
import kotlin.math.sqrt

/**
 * Plane geometry in the map's 0–1 space (x right, y down). Distances are measured in that space
 * directly, the same space scale calibration uses (WLD-46), so km = length × km per unit.
 */
object MapGeometry {
    fun clamp(p: Point): Point = Point(p.x.coerceIn(0f, 1f), p.y.coerceIn(0f, 1f))

    fun distance(a: Point, b: Point): Double {
        val dx = (a.x - b.x).toDouble()
        val dy = (a.y - b.y).toDouble()
        return sqrt(dx * dx + dy * dy)
    }

    /** Length of a polyline. */
    fun length(points: List<Point>): Double = points.zipWithNext { a, b -> distance(a, b) }.sum()

    /** Signed area by the shoelace formula (positive for counter-clockwise in a y-up frame). */
    fun signedArea(polygon: List<Point>): Double {
        if (polygon.size < 3) return 0.0
        var sum = 0.0
        for (i in polygon.indices) {
            val a = polygon[i]
            val b = polygon[(i + 1) % polygon.size]
            sum += a.x.toDouble() * b.y - b.x.toDouble() * a.y
        }
        return sum / 2
    }

    fun area(polygon: List<Point>): Double = kotlin.math.abs(signedArea(polygon))

    /** Even-odd rule; points on the boundary count as inside. */
    fun contains(polygon: List<Point>, p: Point): Boolean {
        if (polygon.size < 3) return false
        if (polygon.indices.any { segmentDistance(p, polygon[it], polygon[(it + 1) % polygon.size]) < EPS }) return true
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val a = polygon[i]
            val b = polygon[j]
            if ((a.y > p.y) != (b.y > p.y)) {
                val x = (b.x - a.x).toDouble() * (p.y - a.y) / (b.y - a.y) + a.x
                if (p.x < x) inside = !inside
            }
            j = i
        }
        return inside
    }

    /** Distance from [p] to segment [a]–[b]. */
    fun segmentDistance(p: Point, a: Point, b: Point): Double {
        val abx = (b.x - a.x).toDouble()
        val aby = (b.y - a.y).toDouble()
        val len2 = abx * abx + aby * aby
        if (len2 == 0.0) return distance(p, a)
        val t = (((p.x - a.x) * abx + (p.y - a.y) * aby) / len2).coerceIn(0.0, 1.0)
        val cx = a.x + t * abx
        val cy = a.y + t * aby
        val dx = p.x - cx
        val dy = p.y - cy
        return sqrt(dx * dx + dy * dy)
    }

    private fun orientation(a: Point, b: Point, c: Point): Double =
        (b.x - a.x).toDouble() * (c.y - a.y) - (b.y - a.y).toDouble() * (c.x - a.x)

    /** True when segments [a1]–[a2] and [b1]–[b2] cross or touch. */
    fun segmentsIntersect(a1: Point, a2: Point, b1: Point, b2: Point): Boolean {
        val d1 = orientation(b1, b2, a1)
        val d2 = orientation(b1, b2, a2)
        val d3 = orientation(a1, a2, b1)
        val d4 = orientation(a1, a2, b2)
        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) return true
        return segmentDistance(a1, b1, b2) < EPS || segmentDistance(a2, b1, b2) < EPS ||
            segmentDistance(b1, a1, a2) < EPS || segmentDistance(b2, a1, a2) < EPS
    }

    /** Distance from a point to a polygon: 0 inside, else to the nearest edge. */
    fun pointToPolygon(p: Point, polygon: List<Point>): Double {
        if (polygon.size < 3) return polygon.minOfOrNull { distance(p, it) } ?: Double.POSITIVE_INFINITY
        if (contains(polygon, p)) return 0.0
        return polygon.indices.minOf { segmentDistance(p, polygon[it], polygon[(it + 1) % polygon.size]) }
    }

    /** Nearest distance between two polygons: 0 when they touch or overlap (WLD-66). */
    fun polygonToPolygon(a: List<Point>, b: List<Point>): Double {
        if (a.any { contains(b, it) } || b.any { contains(a, it) }) return 0.0
        var best = Double.POSITIVE_INFINITY
        for (i in a.indices) {
            val a1 = a[i]
            val a2 = a[(i + 1) % a.size]
            for (j in b.indices) {
                val b1 = b[j]
                val b2 = b[(j + 1) % b.size]
                if (segmentsIntersect(a1, a2, b1, b2)) return 0.0
                best = minOf(best, segmentDistance(a1, b1, b2), segmentDistance(a2, b1, b2), segmentDistance(b1, a1, a2), segmentDistance(b2, a1, a2))
            }
        }
        return best
    }

    private const val EPS = 1e-9
}
