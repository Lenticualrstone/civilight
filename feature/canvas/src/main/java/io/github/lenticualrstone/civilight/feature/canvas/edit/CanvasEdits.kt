package io.github.lenticualrstone.civilight.feature.canvas.edit

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.ui.graph.Pt
import io.github.lenticualrstone.civilight.ui.graph.Rect
import io.github.lenticualrstone.civilight.ui.graph.Sz
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.canvas.Canvas
import io.github.lenticualrstone.civilight.model.canvas.CanvasEdge
import io.github.lenticualrstone.civilight.model.canvas.CanvasNode
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.open

/** Outcome of drawing a line from one node to another (CNV-07). */
sealed interface LineResult {
    /** A new plain line; its label surface opens next. */
    data class Created(val edgeKey: Key) : LineResult

    /** The pair already has a line (at most one per unordered pair); that one opens instead. */
    data class Existing(val edgeKey: Key) : LineResult

    /** A line from a node to itself, or to a 묶음 frame. Nothing changes. */
    data object Refused : LineResult
}

/**
 * Canvas document edits (CNV-02 … CNV-07, CNV-10): pure `ProjectDoc -> ProjectDoc` functions. The
 * canvas file holds only keys, positions, memo text, group titles and plain line labels; item and
 * relation data stay in their own files (CNV-12). Relation writes are `domain:cast` `CanvasRelations`.
 */
object CanvasEdits {
    /** Default 묶음 frame (CNV-06 [PROPOSED] shape: a frame with a title holding member nodes). */
    val GROUP_SIZE = Sz(260f, 180f)

    fun canvas(doc: ProjectDoc, canvasKey: Key): Canvas? = doc.canvases[canvasKey]?.takeIf { it.deletedAt == null }

    /** Live canvases in their order. */
    fun canvases(doc: ProjectDoc): List<Canvas> =
        doc.canvases.values.filter { it.deletedAt == null }.sortedWith(compareBy<Canvas>({ it.order }, { it.key }))

    /**
     * The canvas a `CanvasBoard` route shows: the requested one when it is live, else the project's
     * first canvas (CNV-02 [PROPOSED]); null when the project has none.
     */
    fun resolve(doc: ProjectDoc, requested: Key?): Canvas? =
        requested?.let { canvas(doc, it) } ?: canvases(doc).firstOrNull()

    /**
     * The canvas a `CanvasBoard(canvasKey)` route opens (CNV-02): the requested one; without one (the
     * home shortcut) the most recently opened canvas (`_app/view.json` `lastCanvasKey`), else the first.
     */
    fun resolveForRoute(doc: ProjectDoc, requested: Key?): Canvas? = resolve(doc, requested ?: doc.app.view.lastCanvasKey)

    /** A new empty canvas after the others (CNV-02 [PROPOSED]: made when a project has none). */
    fun createCanvas(doc: ProjectDoc, name: String, ctx: EditContext): Pair<ProjectDoc, Key> {
        val key = ctx.newKey()
        val canvas = Canvas(
            key = key,
            order = OrderKeys.after(canvases(doc).lastOrNull()?.order),
            name = name.trim().nfc(),
            createdAt = ctx.now,
            updatedAt = ctx.now,
            updatedBy = ctx.device,
        )
        return doc.copy(canvases = doc.canvases + (key to canvas)) to key
    }

    /** An item node (`인물 · 세력 · 사건 · 용어`) referencing [refKey] at [topLeft] (CNV-04). */
    fun addItemNode(doc: ProjectDoc, canvasKey: Key, kind: CanvasNodeKind, refKey: Key, topLeft: Pt, ctx: EditContext): Pair<ProjectDoc, Key> {
        require(kind.isItem) { "$kind is not an item kind" }
        return addNode(doc, canvasKey, ctx) { key, z -> CanvasNode(key = key, kind = kind.open(), refKey = refKey, x = topLeft.x, y = topLeft.y, z = z) }
    }

    /** A memo node owned by the canvas (CNV-05); text is edited afterwards through node edit. */
    fun addMemo(doc: ProjectDoc, canvasKey: Key, topLeft: Pt, ctx: EditContext, text: String = ""): Pair<ProjectDoc, Key> =
        addNode(doc, canvasKey, ctx) { key, z ->
            CanvasNode(key = key, kind = CanvasNodeKind.MEMO.open(), text = text.nfc(), x = topLeft.x, y = topLeft.y, z = z)
        }

    /** A 묶음 frame (CNV-06 [PROPOSED]) with no title yet. */
    fun addGroup(doc: ProjectDoc, canvasKey: Key, topLeft: Pt, ctx: EditContext): Pair<ProjectDoc, Key> =
        addNode(doc, canvasKey, ctx) { key, z ->
            CanvasNode(key = key, kind = CanvasNodeKind.GROUP.open(), text = "", x = topLeft.x, y = topLeft.y, w = GROUP_SIZE.w, h = GROUP_SIZE.h, z = z)
        }

    private fun addNode(doc: ProjectDoc, canvasKey: Key, ctx: EditContext, make: (Key, String) -> CanvasNode): Pair<ProjectDoc, Key> {
        val canvas = requireNotNull(canvas(doc, canvasKey)) { "no canvas $canvasKey" }
        val key = ctx.newKey()
        val z = OrderKeys.after(canvas.nodes.maxOfOrNull { it.z })
        val node = make(key, z).copy(updatedAt = ctx.now, updatedBy = ctx.device)
        return put(doc, canvas.copy(nodes = canvas.nodes + node), ctx) to key
    }

    /**
     * Moves a node to [topLeft] (one undo step at the end of a drag). A 묶음 frame carries its members
     * by the same distance. A card whose centre ([size] = its measured size) lands inside a frame joins
     * that frame; dropped outside every frame it leaves its frame.
     */
    fun moveNode(doc: ProjectDoc, canvasKey: Key, nodeKey: Key, topLeft: Pt, ctx: EditContext, size: Sz = Sz.DEFAULT): ProjectDoc {
        val canvas = canvas(doc, canvasKey) ?: return doc
        val node = canvas.nodes.firstOrNull { it.key == nodeKey } ?: return doc
        val dx = topLeft.x - node.x
        val dy = topLeft.y - node.y
        val moved = if (node.kind.known == CanvasNodeKind.GROUP) {
            canvas.nodes.map { n ->
                when {
                    n.key == nodeKey -> n.copy(x = topLeft.x, y = topLeft.y, updatedAt = ctx.now, updatedBy = ctx.device)
                    n.groupKey == nodeKey -> n.copy(x = n.x + dx, y = n.y + dy, updatedAt = ctx.now, updatedBy = ctx.device)
                    else -> n
                }
            }
        } else {
            val center = Pt(topLeft.x + size.w / 2f, topLeft.y + size.h / 2f)
            val frame = canvas.nodes.lastOrNull { it.kind.known == CanvasNodeKind.GROUP && frameRect(it).contains(center) }
            canvas.nodes.map { n ->
                if (n.key != nodeKey) n
                else n.copy(x = topLeft.x, y = topLeft.y, groupKey = frame?.key, updatedAt = ctx.now, updatedBy = ctx.device)
            }
        }
        return put(doc, canvas.copy(nodes = moved), ctx)
    }

    fun frameRect(node: CanvasNode): Rect = Rect.at(Pt(node.x, node.y), Sz(node.w ?: GROUP_SIZE.w, node.h ?: GROUP_SIZE.h))

    /** Memo text (CNV-05) or 묶음 title (CNV-06); item nodes have none. */
    fun setText(doc: ProjectDoc, canvasKey: Key, nodeKey: Key, text: String, ctx: EditContext): ProjectDoc {
        val canvas = canvas(doc, canvasKey) ?: return doc
        val nodes = canvas.nodes.map { n ->
            if (n.key != nodeKey || n.kind.known?.isItem != false) n
            else n.copy(text = text.nfc(), updatedAt = ctx.now, updatedBy = ctx.device)
        }
        return put(doc, canvas.copy(nodes = nodes), ctx)
    }

    /**
     * Takes a node off this canvas (CNV-10): its lines go with it, the project item stays, and a
     * removed 묶음 lets its members go free. Relations behind removed lines stay (CNV-09 [PROPOSED]).
     */
    fun removeNode(doc: ProjectDoc, canvasKey: Key, nodeKey: Key, ctx: EditContext): ProjectDoc {
        val canvas = canvas(doc, canvasKey) ?: return doc
        if (canvas.nodes.none { it.key == nodeKey }) return doc
        val nodes = canvas.nodes.filter { it.key != nodeKey }.map { n ->
            if (n.groupKey == nodeKey) n.copy(groupKey = null, updatedAt = ctx.now, updatedBy = ctx.device) else n
        }
        val edges = canvas.edges.filter { it.fromNodeKey != nodeKey && it.toNodeKey != nodeKey }
        return put(doc, canvas.copy(nodes = nodes, edges = edges), ctx)
    }

    /**
     * Draws a line [fromKey] → [toKey] (CNV-07 [PROPOSED]: tap A then tap B, no self-line, one line per
     * unordered pair). The drawing direction is stored; it only preselects `<A>의 마음` (rule 13 §7).
     */
    fun addLine(doc: ProjectDoc, canvasKey: Key, fromKey: Key, toKey: Key, ctx: EditContext): Pair<ProjectDoc, LineResult> {
        val canvas = canvas(doc, canvasKey) ?: return doc to LineResult.Refused
        if (fromKey == toKey) return doc to LineResult.Refused
        val ends = canvas.nodes.filter { it.key == fromKey || it.key == toKey }
        if (ends.size != 2 || ends.any { it.kind.known == CanvasNodeKind.GROUP }) return doc to LineResult.Refused
        lineBetween(canvas, fromKey, toKey)?.let { return doc to LineResult.Existing(it.key) }
        val key = ctx.newKey()
        val edge = CanvasEdge(key = key, fromNodeKey = fromKey, toNodeKey = toKey, updatedAt = ctx.now, updatedBy = ctx.device)
        return put(doc, canvas.copy(edges = canvas.edges + edge), ctx) to LineResult.Created(key)
    }

    fun lineBetween(canvas: Canvas, a: Key, b: Key): CanvasEdge? =
        canvas.edges.firstOrNull { (it.fromNodeKey == a && it.toNodeKey == b) || (it.fromNodeKey == b && it.toNodeKey == a) }

    /** Takes a line off the canvas; a relation it was made into stays with the characters (CNV-09 [PROPOSED]). */
    fun removeLine(doc: ProjectDoc, canvasKey: Key, edgeKey: Key, ctx: EditContext): ProjectDoc {
        val canvas = canvas(doc, canvasKey) ?: return doc
        if (canvas.edges.none { it.key == edgeKey }) return doc
        return put(doc, canvas.copy(edges = canvas.edges.filter { it.key != edgeKey }), ctx)
    }

    private fun put(doc: ProjectDoc, canvas: Canvas, ctx: EditContext): ProjectDoc =
        doc.copy(canvases = doc.canvases + (canvas.key to canvas.copy(updatedAt = ctx.now, updatedBy = ctx.device)))
}
