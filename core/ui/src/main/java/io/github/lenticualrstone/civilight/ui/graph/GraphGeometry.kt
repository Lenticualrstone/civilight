package io.github.lenticualrstone.civilight.ui.graph

import io.github.lenticualrstone.civilight.designsystem.theme.ColorRole
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/** What a tap or a press landed on. */
sealed interface GraphHit {
    data class Node(val key: String) : GraphHit

    data class Line(val key: String) : GraphHit

    /** The body of a frame (묶음) away from its cards. */
    data class Frame(val key: String) : GraphHit

    data object Empty : GraphHit
}

/** Stroke of a line: colour role and width (CNV-01, T-CNV-08). */
data class StrokeSpec(val color: ColorRole, val widthDp: Float, val dashed: Boolean)

/** Pure geometry of the graph surface: rectangles, clipped segments, label places, hit tests. */
object GraphGeometry {
    /** Touch targets are at least 44 dp on screen (DS-11). */
    const val HIT_MIN_DP = 44f

    /** Label places along the visible part of a line (`CastMap`: near-end labels at about 0.28 / 0.72). */
    const val T_MIDDLE = 0.5f
    const val T_NEAR_FROM = 0.28f
    const val T_NEAR_TO = 0.72f

    /** Dashed relation lines: dash 6 dp, gap 5 dp. */
    const val DASH = 6f
    const val GAP = 5f

    fun rectOf(node: GraphNode, sizes: Map<String, Sz>): Rect =
        Rect.at(node.topLeft, node.frameSize ?: sizes[node.key] ?: Sz.DEFAULT)

    /** Where the straight line from the centre of [rect] toward [toward] leaves the rectangle. */
    fun edgePoint(rect: Rect, toward: Pt): Pt {
        val c = rect.center
        val d = toward - c
        if (d.x == 0f && d.y == 0f) return c
        val hw = rect.width / 2f
        val hh = rect.height / 2f
        val sx = if (d.x != 0f) hw / abs(d.x) else Float.MAX_VALUE
        val sy = if (d.y != 0f) hh / abs(d.y) else Float.MAX_VALUE
        val s = minOf(sx, sy, 1f)
        return c + d * s
    }

    /** The part of the centre-to-centre line between two cards that lies outside both. */
    fun visibleSegment(a: Rect, b: Rect): Pair<Pt, Pt> = edgePoint(a, b.center) to edgePoint(b, a.center)

    fun pointAt(a: Pt, b: Pt, t: Float): Pt = a + (b - a) * t

    fun distance(a: Pt, b: Pt): Float = hypot(a.x - b.x, a.y - b.y)

    /** Distance from [p] to the segment [a]–[b]. */
    fun distanceToSegment(p: Pt, a: Pt, b: Pt): Float {
        val ab = b - a
        val len2 = ab.x * ab.x + ab.y * ab.y
        if (len2 == 0f) return distance(p, a)
        val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / len2).coerceIn(0f, 1f)
        return distance(p, a + ab * t)
    }

    /** A rectangle grown so it is at least [HIT_MIN_DP] wide and tall on screen at [zoom]. */
    fun hitRect(rect: Rect, zoom: Float): Rect {
        val min = HIT_MIN_DP / zoom
        val dx = max(0f, (min - rect.width) / 2f)
        val dy = max(0f, (min - rect.height) / 2f)
        return Rect(rect.left - dx, rect.top - dy, rect.right + dx, rect.bottom + dy)
    }

    /**
     * What is under [p] (content dp) at [zoom]: cards first (the closest centre wins when grown targets
     * overlap), then lines within half a touch target of their stroke, then frames, else empty.
     */
    fun hit(scene: GraphScene, sizes: Map<String, Sz>, p: Pt, zoom: Float): GraphHit {
        val cards = scene.nodes.filter { it.look != NodeLook.FRAME }
        val onCard = cards
            .map { it to rectOf(it, sizes) }
            .filter { (_, r) -> hitRect(r, zoom).contains(p) }
            .minByOrNull { (_, r) -> distance(r.center, p) }
        if (onCard != null) {
            // A card drawn on top of a grown neighbour target still wins when the point is really on it.
            val exact = cards.lastOrNull { rectOf(it, sizes).contains(p) }
            return GraphHit.Node((exact ?: onCard.first).key)
        }
        val tolerance = HIT_MIN_DP / 2f / zoom
        val line = scene.lines
            .mapNotNull { l ->
                val a = scene.nodesByKey[l.from] ?: return@mapNotNull null
                val b = scene.nodesByKey[l.to] ?: return@mapNotNull null
                val ra = rectOf(a, sizes)
                val rb = rectOf(b, sizes)
                l to distanceToSegment(p, ra.center, rb.center)
            }
            .filter { (_, d) -> d <= tolerance }
            .minByOrNull { (_, d) -> d }
        if (line != null) return GraphHit.Line(line.first.key)
        val frame = scene.nodes.lastOrNull { it.look == NodeLook.FRAME && rectOf(it, sizes).contains(p) }
        if (frame != null) return GraphHit.Frame(frame.key)
        return GraphHit.Empty
    }

    /** Bounding box of every node, or null when there is none. */
    fun bounds(scene: GraphScene, sizes: Map<String, Sz>): Rect? =
        scene.nodes.map { rectOf(it, sizes) }.reduceOrNull { acc, r -> acc.union(r) }

    /** Stroke of a line (T-CNV-08): `ink3` 1.2 dp, or `acc` when selected (2.2 dp on cover, 2 dp inner). */
    fun stroke(line: GraphLine, selected: Boolean, cover: Boolean): StrokeSpec = StrokeSpec(
        color = if (selected) ColorRole.ACC else ColorRole.INK3,
        widthDp = if (!selected) 1.2f else if (cover) 2.2f else 2f,
        dashed = line.stroke == LineStroke.DASHED,
    )

    /** Middle label text; a pair kind reads left to right on screen (`후배 ↔ 선배`). */
    fun middleText(word: MiddleWord, fromCenter: Pt, toCenter: Pt): String {
        val other = word.toSide ?: return word.fromSide
        return if (fromCenter.x <= toCenter.x) "${word.fromSide} ↔ $other" else "$other ↔ ${word.fromSide}"
    }

    /** End label text with an arrow toward the partner end (`경계 →` left of the partner, `← 보호` right of it). */
    fun endText(word: EndWord, fromCenter: Pt, toCenter: Pt): String {
        val holder = if (word.nearFrom) fromCenter else toCenter
        val partner = if (word.nearFrom) toCenter else fromCenter
        return if (holder.x <= partner.x) "${word.word} →" else "← ${word.word}"
    }

    /** Anchor of an end label along the visible segment. */
    fun endAnchor(word: EndWord, visible: Pair<Pt, Pt>): Pt =
        pointAt(visible.first, visible.second, if (word.nearFrom) T_NEAR_FROM else T_NEAR_TO)

    fun middleAnchor(visible: Pair<Pt, Pt>): Pt = pointAt(visible.first, visible.second, T_MIDDLE)
}
