package io.github.lenticualrstone.civilight.feature.canvas.graph

/**
 * The shared graph engine (CNV-01): what the surface draws. It knows nothing about projects,
 * canvases or relations, so the canvas (CNV-02 … CNV-12) and the cast area's 전체 관계도 can feed it
 * the same way ("노드 · 선 · 상호작용이 같고, 차이는 담는 것뿐이다"). All coordinates are content dp at
 * zoom 1.
 */

/** A point or vector in content dp. */
data class Pt(val x: Float, val y: Float) {
    operator fun plus(o: Pt) = Pt(x + o.x, y + o.y)
    operator fun minus(o: Pt) = Pt(x - o.x, y - o.y)
    operator fun times(s: Float) = Pt(x * s, y * s)
    operator fun div(s: Float) = Pt(x / s, y / s)

    companion object {
        val ZERO = Pt(0f, 0f)
    }
}

/** A node's size in dp (measured on screen; [DEFAULT] until it is). */
data class Sz(val w: Float, val h: Float) {
    companion object {
        /** A one-word card: padding 14 × 10 around a 20 + 15 dp two-line text. */
        val DEFAULT = Sz(76f, 55f)
    }
}

/** An axis-aligned rectangle in content dp. */
data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val center: Pt get() = Pt((left + right) / 2f, (top + bottom) / 2f)

    fun contains(p: Pt): Boolean = p.x in left..right && p.y in top..bottom

    fun inflate(by: Float): Rect = Rect(left - by, top - by, right + by, bottom + by)

    fun union(o: Rect): Rect = Rect(minOf(left, o.left), minOf(top, o.top), maxOf(right, o.right), maxOf(bottom, o.bottom))

    companion object {
        fun at(topLeft: Pt, size: Sz) = Rect(topLeft.x, topLeft.y, topLeft.x + size.w, topLeft.y + size.h)
    }
}

/** How a node card is drawn (DS-50; no circles anywhere). */
enum class NodeLook {
    /** `raise` fill, 1 dp `line` inset. */
    CARD,

    /** Memo: transparent, dashed `line` border. */
    MEMO,

    /** A reference to a deleted item: dotted, with `◇` (rule 20 §2). */
    DOTTED,

    /** A frame that holds other nodes (묶음); drawn behind the cards. */
    FRAME,
}

/**
 * A node: a card at [topLeft] with a title and a kind or descriptor line. A [NodeLook.FRAME] node has a
 * fixed [frameSize]; cards size themselves to their text.
 */
data class GraphNode(
    val key: String,
    val title: String,
    val subtitle: String?,
    val topLeft: Pt,
    val look: NodeLook = NodeLook.CARD,
    val frameSize: Sz? = null,
    /** Lines may end on it (frames take none). */
    val connectable: Boolean = look != NodeLook.FRAME,
)

/** Solid or dashed stroke; never coloured by kind (rule 13 §4). */
enum class LineStroke { SOLID, DASHED }

/**
 * The middle label of a line. A symmetric 사이 or a plain label has only [fromSide]; a pair kind has
 * both roles, each standing for the end it belongs to (`선배 ↔ 후배`, ordered left to right on screen).
 */
data class MiddleWord(val fromSide: String, val toSide: String? = null)

/** A label near one end with an arrow toward the other end (`경계 →`, `← 보호`). */
data class EndWord(val word: String, val nearFrom: Boolean)

/**
 * A line from node [from] to node [to] (drawing direction kept), straight, centre to centre (CNV-01).
 */
data class GraphLine(
    val key: String,
    val from: String,
    val to: String,
    val stroke: LineStroke = LineStroke.SOLID,
    val middle: MiddleWord? = null,
    val ends: List<EndWord> = emptyList(),
)

/** What is selected; at most one node or one line. */
data class GraphSelection(val nodeKey: String? = null, val lineKey: String? = null) {
    companion object {
        val NONE = GraphSelection()
    }
}

/** Everything the surface draws. */
data class GraphScene(
    val nodes: List<GraphNode>,
    val lines: List<GraphLine>,
) {
    val nodesByKey: Map<String, GraphNode> by lazy { nodes.associateBy { it.key } }

    companion object {
        val EMPTY = GraphScene(emptyList(), emptyList())
    }
}
