package io.github.lenticualrstone.civilight.feature.canvas.board

import io.github.lenticualrstone.civilight.domain.cast.ItemRefs
import io.github.lenticualrstone.civilight.domain.cast.ResolvedItem
import io.github.lenticualrstone.civilight.feature.canvas.CanvasCopy
import io.github.lenticualrstone.civilight.ui.graph.NodeLook
import io.github.lenticualrstone.civilight.model.canvas.CanvasNode
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** How a canvas node reads: title, kind line and look. */
data class NodeLabel(
    val title: String,
    /** The kind line (`인물 · 세력 · 사건 · 용어 · 메모 · 묶음`); null for a locked hidden item. */
    val kindWord: String?,
    val look: NodeLook,
    /** A hidden item while the project's hidden items are locked (rule 19 §6). */
    val hiddenLocked: Boolean = false,
    /** The referenced item is live and may be opened. */
    val openable: Boolean = false,
)

/**
 * Node titles (CNV-04, CNV-05, CNV-06). Item nodes show the item's **current** name, resolved by key
 * at read time, so a rename shows everywhere and the canvas never copies item data. A locked hidden
 * item never shows its name (`숨긴 인물` / `숨긴 항목`); a deleted one stays as a dotted reference
 * (`지운 인물 · 노아`, CNV-11 [PROPOSED]). Item nodes follow the plain name, not the lens (Q-CV-05: the
 * board shows `하르트` at `최신 · 1-7`).
 */
object NodeLabels {
    fun of(doc: ProjectDoc, node: CanvasNode, hiddenOpen: Boolean): NodeLabel {
        val kind = node.kind.known
        val word = CanvasCopy.kindWord(kind)
        return when (kind) {
            CanvasNodeKind.CHARACTER -> item(doc, EntityKind.CHARACTER, node, word, hiddenOpen)
            CanvasNodeKind.FACTION -> item(doc, EntityKind.FACTION, node, word, hiddenOpen)
            CanvasNodeKind.TERM -> item(doc, EntityKind.TERM, node, word, hiddenOpen)
            CanvasNodeKind.EVENT -> event(doc, node, word)
            CanvasNodeKind.MEMO -> NodeLabel(node.text?.takeIf { it.isNotBlank() } ?: word, word, NodeLook.MEMO)
            CanvasNodeKind.GROUP -> NodeLabel(node.text?.takeIf { it.isNotBlank() } ?: word, word, NodeLook.FRAME)
            null -> NodeLabel(node.text.orEmpty(), null, NodeLook.MEMO)
        }
    }

    private fun item(doc: ProjectDoc, kind: EntityKind, node: CanvasNode, word: String, hiddenOpen: Boolean): NodeLabel {
        val key = node.refKey ?: return NodeLabel(CanvasCopy.DELETED_PREFIX + word, word, NodeLook.DOTTED)
        return when (val r = ItemRefs.resolve(doc, kind, key, hiddenOpen)) {
            is ResolvedItem.HiddenLocked -> NodeLabel(r.wideLabel, null, NodeLook.CARD, hiddenLocked = true)
            is ResolvedItem.Live -> NodeLabel(r.name, word, NodeLook.CARD, openable = true)
            else -> NodeLabel(r.wideLabel, word, NodeLook.DOTTED)
        }
    }

    private fun event(doc: ProjectDoc, node: CanvasNode, word: String): NodeLabel {
        val event = node.refKey?.let { doc.index.event(it) } ?: return NodeLabel(CanvasCopy.DELETED_PREFIX + word, word, NodeLook.DOTTED)
        return if (event.deletedAt == null) NodeLabel(event.title, word, NodeLook.CARD, openable = true)
        else NodeLabel(CanvasCopy.deleted(word, event.title), word, NodeLook.DOTTED)
    }
}
