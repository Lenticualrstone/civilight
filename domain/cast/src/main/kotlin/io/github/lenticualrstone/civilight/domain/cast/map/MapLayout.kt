package io.github.lenticualrstone.civilight.domain.cast.map

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** A 2D vector in layout units (dp at zoom 1). */
data class Vec(val x: Double, val y: Double) {
    operator fun plus(o: Vec) = Vec(x + o.x, y + o.y)
    operator fun minus(o: Vec) = Vec(x - o.x, y - o.y)
    operator fun times(s: Double) = Vec(x * s, y * s)
    val length: Double get() = sqrt(x * x + y * y)
}

/** A placed node card. */
data class NodeBox(val key: Key, val center: Vec, val width: Double, val height: Double) {
    val left: Double get() = center.x - width / 2
    val top: Double get() = center.y - height / 2

    /** Where the ray from the centre toward [toward] leaves the card. */
    fun edgePoint(toward: Vec): Vec {
        val d = toward - center
        if (d.x == 0.0 && d.y == 0.0) return center
        val tx = if (d.x == 0.0) Double.POSITIVE_INFINITY else (width / 2) / abs(d.x)
        val ty = if (d.y == 0.0) Double.POSITIVE_INFINITY else (height / 2) / abs(d.y)
        return center + d * min(tx, ty)
    }
}

/** Geometry of one line: endpoints on the card borders and label anchors. */
data class LineGeometry(
    val relationKey: Key,
    val start: Vec,
    val end: Vec,
    /** Middle (`사이` label). */
    val mid: Vec,
    /** Near A's end (A's mind label). */
    val nearA: Vec,
    /** Near B's end (B's mind label). */
    val nearB: Vec,
    /** A is drawn left of B: A's mind reads `경계 →`, B's `← 보호`. */
    val aIsLeft: Boolean,
)

data class LayoutConfig(
    val width: Double = 800.0,
    val height: Double = 800.0,
    val nodeWidth: Double = 104.0,
    val nodeHeight: Double = 52.0,
    val iterations: Int = 300,
    /** Mind labels sit this far along the line from their holder. */
    val labelFraction: Double = 0.22,
)

data class MapLayoutResult(val nodes: Map<Key, NodeBox>, val lines: List<LineGeometry>, val config: LayoutConfig) {
    /** Positions as 0–1 points for saving (CST Q14: positions saved per project, like canvas nodes). */
    fun normalized(): Map<Key, Point> = nodes.mapValues { (_, n) ->
        Point((n.center.x / config.width).toFloat(), (n.center.y / config.height).toFloat())
    }
}

/**
 * Pure layout of the relationship map: saved positions are kept; the other nodes start on a circle
 * in node order and settle with a deterministic force-directed pass (no randomness, so the same
 * input gives the same picture). Lines are clipped to the card borders.
 */
object MapLayout {
    fun layout(view: RelationMapView, saved: Map<Key, Point> = emptyMap(), config: LayoutConfig = LayoutConfig()): MapLayoutResult {
        val keys = view.nodes.map { it.key }
        val edges = view.lines.map { it.endA to it.endB }
        val positions = positions(keys, edges, saved, config)
        val boxes = keys.associateWith { NodeBox(it, positions.getValue(it), config.nodeWidth, config.nodeHeight) }
        val lines = view.lines.mapNotNull { line ->
            val a = boxes[line.endA] ?: return@mapNotNull null
            val b = boxes[line.endB] ?: return@mapNotNull null
            geometry(line.relationKey, a, b, config.labelFraction)
        }
        return MapLayoutResult(boxes, lines, config)
    }

    fun geometry(relationKey: Key, a: NodeBox, b: NodeBox, labelFraction: Double = 0.22): LineGeometry {
        val start = a.edgePoint(b.center)
        val end = b.edgePoint(a.center)
        val d = end - start
        return LineGeometry(
            relationKey = relationKey,
            start = start,
            end = end,
            mid = start + d * 0.5,
            nearA = start + d * labelFraction,
            nearB = end - d * labelFraction,
            aIsLeft = a.center.x <= b.center.x,
        )
    }

    /** Node centres in layout units. */
    fun positions(keys: List<Key>, edges: List<Pair<Key, Key>>, saved: Map<Key, Point>, config: LayoutConfig): Map<Key, Vec> {
        if (keys.isEmpty()) return emptyMap()
        val w = config.width
        val h = config.height
        val center = Vec(w / 2, h / 2)
        val radius = 0.35 * min(w, h)
        val n = keys.size
        val pos = HashMap<Key, Vec>()
        keys.forEachIndexed { i, key ->
            val s = saved[key]
            pos[key] = if (s != null) Vec(s.x * w, s.y * h)
            else if (n == 1) center
            else {
                val angle = 2 * PI * i / n - PI / 2
                center + Vec(cos(angle), sin(angle)) * radius
            }
        }
        val pinned = keys.filter { it in saved }.toSet()
        if (pinned.size == n) return pos
        val k = sqrt(w * h / n) * 0.6
        val index = keys.withIndex().associate { (i, key) -> key to i }
        val validEdges = edges.filter { (a, b) -> a in index && b in index && a != b }
        var temperature = w / 10
        val cooling = temperature / (config.iterations + 1)
        repeat(config.iterations) {
            val disp = keys.associateWith { Vec(0.0, 0.0) }.toMutableMap()
            for (i in 0 until n) for (j in i + 1 until n) {
                val a = keys[i]
                val b = keys[j]
                var d = pos.getValue(a) - pos.getValue(b)
                if (d.length < 1e-6) d = Vec(1.0 + i, 1.0 + j) * 1e-3
                val dist = max(d.length, 1e-3)
                val f = k * k / dist
                val push = d * (f / dist)
                disp[a] = disp.getValue(a) + push
                disp[b] = disp.getValue(b) - push
            }
            for ((a, b) in validEdges) {
                val d = pos.getValue(a) - pos.getValue(b)
                val dist = max(d.length, 1e-3)
                val f = dist * dist / k
                val pull = d * (f / dist)
                disp[a] = disp.getValue(a) - pull
                disp[b] = disp.getValue(b) + pull
            }
            for (key in keys) {
                if (key in pinned) continue
                val d = disp.getValue(key)
                val len = max(d.length, 1e-9)
                val moved = pos.getValue(key) + d * (min(len, temperature) / len)
                pos[key] = Vec(
                    moved.x.coerceIn(config.nodeWidth / 2, w - config.nodeWidth / 2),
                    moved.y.coerceIn(config.nodeHeight / 2, h - config.nodeHeight / 2),
                )
            }
            temperature = max(temperature - cooling, 0.5)
        }
        return pos
    }
}
