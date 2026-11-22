package io.github.lenticualrstone.civilight.domain.world.gen

import kotlin.math.sqrt

/** Coastline tracing (marching squares over cell centres) and polyline distances. */
internal object Contours {
    // Edge ids: horizontal edge between cell centres (c, r)–(c+1, r) and vertical (c, r)–(c, r+1).
    private fun hEdge(w: Int, c: Int, r: Int): Int = (r * (w + 1) + c) * 2
    private fun vEdge(w: Int, c: Int, r: Int): Int = (r * (w + 1) + c) * 2 + 1

    private fun pointOf(grid: Grid, edge: Int): GenPoint {
        val cell = edge / 2
        val c = cell % (grid.w + 1)
        val r = cell / (grid.w + 1)
        return if (edge % 2 == 0) GenPoint.of((c + 1.0) / grid.w, (r + 0.5) / grid.h)
        else GenPoint.of((c + 0.5) / grid.w, (r + 1.0) / grid.h)
    }

    /** The land/sea boundary as polylines (segments chained by shared edges), in a deterministic order. */
    fun trace(grid: Grid, land: BooleanArray): List<List<GenPoint>> {
        val w = grid.w
        val segments = ArrayList<IntArray>()
        for (r in 0 until grid.h - 1) for (c in 0 until w - 1) {
            fun at(cc: Int, rr: Int) = if (land[rr * w + cc]) 1 else 0
            val code = at(c, r) * 8 + at(c + 1, r) * 4 + at(c + 1, r + 1) * 2 + at(c, r + 1)
            val top = hEdge(w, c, r)
            val bottom = hEdge(w, c, r + 1)
            val left = vEdge(w, c, r)
            val right = vEdge(w, c + 1, r)
            val pairs: List<Pair<Int, Int>> = when (code) {
                1, 14 -> listOf(left to bottom)
                2, 13 -> listOf(bottom to right)
                3, 12 -> listOf(left to right)
                4, 11 -> listOf(top to right)
                5 -> listOf(left to top, bottom to right)
                6, 9 -> listOf(top to bottom)
                7, 8 -> listOf(left to top)
                10 -> listOf(top to right, left to bottom)
                else -> emptyList()
            }
            for ((a, b) in pairs) segments += intArrayOf(a, b)
        }
        val byEdge = HashMap<Int, MutableList<Int>>()
        segments.forEachIndexed { i, s ->
            byEdge.getOrPut(s[0]) { mutableListOf() } += i
            byEdge.getOrPut(s[1]) { mutableListOf() } += i
        }
        val used = BooleanArray(segments.size)
        val lines = ArrayList<List<GenPoint>>()
        for (start in segments.indices) {
            if (used[start]) continue
            used[start] = true
            val chain = ArrayDeque(listOf(segments[start][0], segments[start][1]))
            fun extend(atEnd: Boolean) {
                while (true) {
                    val edge = if (atEnd) chain.last() else chain.first()
                    val next = byEdge[edge].orEmpty().firstOrNull { !used[it] } ?: return
                    used[next] = true
                    val other = if (segments[next][0] == edge) segments[next][1] else segments[next][0]
                    if (atEnd) chain.addLast(other) else chain.addFirst(other)
                }
            }
            extend(atEnd = true)
            extend(atEnd = false)
            lines += chain.map { pointOf(grid, it) }
        }
        return lines
    }

    fun distanceToPolyline(p: GenPoint, line: List<GenPoint>): Double {
        if (line.isEmpty()) return Double.MAX_VALUE
        if (line.size == 1) return dist(p, line[0])
        var best = Double.MAX_VALUE
        for (i in 0 until line.size - 1) best = minOf(best, segmentDistance(p, line[i], line[i + 1]))
        return best
    }

    private fun dist(a: GenPoint, b: GenPoint): Double {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun segmentDistance(p: GenPoint, a: GenPoint, b: GenPoint): Double {
        val abx = b.x - a.x
        val aby = b.y - a.y
        val len2 = abx * abx + aby * aby
        if (len2 == 0.0) return dist(p, a)
        val t = (((p.x - a.x) * abx + (p.y - a.y) * aby) / len2).coerceIn(0.0, 1.0)
        val dx = p.x - (a.x + t * abx)
        val dy = p.y - (a.y + t * aby)
        return sqrt(dx * dx + dy * dy)
    }
}
