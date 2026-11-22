package io.github.lenticualrstone.civilight.feature.canvas.board

import io.github.lenticualrstone.civilight.domain.cast.map.LineStyle
import io.github.lenticualrstone.civilight.domain.cast.map.MapLine
import io.github.lenticualrstone.civilight.domain.cast.map.RelationMap
import io.github.lenticualrstone.civilight.feature.canvas.edit.CanvasEdits
import io.github.lenticualrstone.civilight.ui.graph.EndWord
import io.github.lenticualrstone.civilight.ui.graph.GraphLine
import io.github.lenticualrstone.civilight.ui.graph.GraphNode
import io.github.lenticualrstone.civilight.ui.graph.GraphScene
import io.github.lenticualrstone.civilight.ui.graph.LineStroke
import io.github.lenticualrstone.civilight.ui.graph.MiddleWord
import io.github.lenticualrstone.civilight.ui.graph.NodeLook
import io.github.lenticualrstone.civilight.ui.graph.Pt
import io.github.lenticualrstone.civilight.ui.graph.Sz
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.canvas.Canvas
import io.github.lenticualrstone.civilight.model.canvas.CanvasEdge
import io.github.lenticualrstone.civilight.model.canvas.CanvasNode
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint

/**
 * A canvas as the graph engine draws it at a lens (CNV-04 … CNV-09).
 *
 * Lines with a relation key take the relationship map's look at the lens (rule 13 §7: "관계로 만든 선은
 * 캔버스에서도 관계도와 같은 모양"), read from `domain:cast` `RelationMap` so both views agree. A relation
 * line that has nothing to draw at the lens (`만나기 전`, only 호칭, a locked hidden end, a relation in
 * the trash) is drawn as a plain line with its label text (CNV-09 [PROPOSED], CNV-11 [PROPOSED]).
 * Plain lines, memo and item nodes do not change with the lens.
 */
object BoardScene {
    fun build(
        doc: ProjectDoc,
        canvas: Canvas,
        lens: LensPoint,
        hiddenOpen: Boolean,
        moved: Map<Key, Pt> = emptyMap(),
        relationLines: Map<Key, MapLine>? = null,
    ): GraphScene {
        val ordered = canvas.nodes.sortedWith(compareBy<CanvasNode>({ it.z }, { it.key }))
        val frames = ordered.filter { it.kind.known == CanvasNodeKind.GROUP }
        val cards = ordered.filter { it.kind.known != CanvasNodeKind.GROUP }
        val nodes = (frames + cards).map { n -> node(doc, n, hiddenOpen, positionOf(canvas, n, moved)) }
        val relations = relationLines ?: relationLines(doc, canvas, lens, hiddenOpen)
        val byKey = canvas.nodes.associateBy { it.key }
        val lines = canvas.edges.mapNotNull { e ->
            val from = byKey[e.fromNodeKey] ?: return@mapNotNull null
            val to = byKey[e.toNodeKey] ?: return@mapNotNull null
            line(e, from, to, relations)
        }
        return GraphScene(nodes, lines)
    }

    /** Relation lines at the lens by relation key (computed once per document and lens, not per drag frame). */
    fun relationLines(doc: ProjectDoc, canvas: Canvas, lens: LensPoint, hiddenOpen: Boolean): Map<Key, MapLine> =
        if (canvas.edges.none { it.relationKey != null }) emptyMap()
        else RelationMap.build(doc, lens, hiddenUnlocked = hiddenOpen).lines.associateBy { it.relationKey }

    /**
     * Where a node is drawn: [moved] holds the live position of the node being dragged. A dragged
     * 묶음 carries its members along before the drop is written.
     */
    fun positionOf(canvas: Canvas, node: CanvasNode, moved: Map<Key, Pt>): Pt {
        moved[node.key]?.let { return it }
        val group = node.groupKey?.let { g -> canvas.nodes.firstOrNull { it.key == g } }
        val groupMoved = group?.let { moved[it.key] }
        if (group != null && groupMoved != null) {
            return Pt(node.x + groupMoved.x - group.x, node.y + groupMoved.y - group.y)
        }
        return Pt(node.x, node.y)
    }

    private fun node(doc: ProjectDoc, n: CanvasNode, hiddenOpen: Boolean, at: Pt): GraphNode {
        val label = NodeLabels.of(doc, n, hiddenOpen)
        val frame = if (label.look == NodeLook.FRAME) Sz(n.w ?: CanvasEdits.GROUP_SIZE.w, n.h ?: CanvasEdits.GROUP_SIZE.h) else null
        return GraphNode(key = n.key, title = label.title, subtitle = if (label.look == NodeLook.FRAME) null else label.kindWord, topLeft = at, look = label.look, frameSize = frame)
    }

    /** One canvas line: relation look when the relation has a value at the lens, else plain grey with its label. */
    fun line(edge: CanvasEdge, from: CanvasNode, to: CanvasNode, relationLines: Map<Key, MapLine>): GraphLine {
        val plain = GraphLine(
            key = edge.key,
            from = edge.fromNodeKey,
            to = edge.toNodeKey,
            stroke = LineStroke.SOLID,
            middle = edge.labelText?.takeIf { it.isNotBlank() }?.let { MiddleWord(it) },
        )
        val relationKey = edge.relationKey ?: return plain
        val map = relationLines[relationKey] ?: return plain
        val fromChar = from.refKey.takeIf { from.kind.known == CanvasNodeKind.CHARACTER } ?: return plain
        val toChar = to.refKey.takeIf { to.kind.known == CanvasNodeKind.CHARACTER } ?: return plain
        val fromIsA = when {
            map.endA == fromChar && map.endB == toChar -> true
            map.endB == fromChar && map.endA == toChar -> false
            else -> return plain
        }
        val middle = map.middle?.let { m ->
            if (m.symmetric) MiddleWord(m.roleOfA)
            else if (fromIsA) MiddleWord(m.roleOfA, m.roleOfB) else MiddleWord(m.roleOfB, m.roleOfA)
        }
        val ends = listOfNotNull(
            map.mindA?.let { EndWord(it.kind, nearFrom = it.holderKey == fromChar) },
            map.mindB?.let { EndWord(it.kind, nearFrom = it.holderKey == fromChar) },
        ).sortedBy { if (it.nearFrom) 0 else 1 }
        return plain.copy(
            stroke = if (map.style == LineStyle.SOLID) LineStroke.SOLID else LineStroke.DASHED,
            middle = middle,
            ends = ends,
        )
    }
}
