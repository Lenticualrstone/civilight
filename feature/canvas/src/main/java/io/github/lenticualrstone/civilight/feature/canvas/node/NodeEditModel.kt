package io.github.lenticualrstone.civilight.feature.canvas.node

import io.github.lenticualrstone.civilight.feature.canvas.CanvasCopy
import io.github.lenticualrstone.civilight.feature.canvas.board.NodeLabels
import io.github.lenticualrstone.civilight.feature.canvas.edit.CanvasEdits
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.EventDetail
import io.github.lenticualrstone.civilight.navigation.FactionCard
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.TermCard

/**
 * The node edit surface (CNV-10; contents not drawn, Q-CV-03). Minimum functions [PROPOSED]: edit a
 * memo's text (or a 묶음's title), open the referenced item's record or card, take the node off this
 * canvas (the project item stays).
 */
data class NodeEditView(
    val nodeKey: Key,
    val title: String,
    /** Kind line; null for a locked hidden item. */
    val kindWord: String?,
    /** Memo text or 묶음 title being edited; null for item nodes. */
    val text: String?,
    /** `인물 기록` · `용어 카드` · `세력 카드` · `사건 상세`; null when there is nothing to open. */
    val openLabel: String?,
    val openRoute: Route?,
    val removeLabel: String,
    val readOnly: Boolean,
)

object NodeEditModel {
    fun build(doc: ProjectDoc, canvasKey: Key, nodeKey: Key, hiddenOpen: Boolean, readOnly: Boolean): NodeEditView? {
        val node = CanvasEdits.canvas(doc, canvasKey)?.nodes?.firstOrNull { it.key == nodeKey } ?: return null
        val label = NodeLabels.of(doc, node, hiddenOpen)
        val kind = node.kind.known
        val ref = node.refKey
        val (openLabel, route) = when {
            !label.openable || ref == null -> null to null
            kind == CanvasNodeKind.CHARACTER -> CanvasCopy.BTN_OPEN_CHARACTER to CharacterProfile(doc.key, ref)
            kind == CanvasNodeKind.TERM -> CanvasCopy.BTN_OPEN_TERM to TermCard(doc.key, ref)
            kind == CanvasNodeKind.FACTION -> CanvasCopy.BTN_OPEN_FACTION to FactionCard(doc.key, ref)
            kind == CanvasNodeKind.EVENT -> CanvasCopy.BTN_OPEN_EVENT to EventDetail(doc.key, ref)
            else -> null to null
        }
        val text = when (kind) {
            CanvasNodeKind.MEMO, CanvasNodeKind.GROUP, null -> node.text.orEmpty()
            else -> null
        }
        return NodeEditView(
            nodeKey = nodeKey,
            title = label.title,
            kindWord = label.kindWord,
            text = text,
            openLabel = openLabel,
            openRoute = route,
            removeLabel = CanvasCopy.TODO_COPY_CANVAS_REMOVE_NODE,
            readOnly = readOnly,
        )
    }
}
