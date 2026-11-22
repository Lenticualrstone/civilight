package io.github.lenticualrstone.civilight.feature.canvas.node

import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.feature.canvas.CanvasCopy
import io.github.lenticualrstone.civilight.feature.canvas.common.RefFixes
import io.github.lenticualrstone.civilight.ui.graph.NodeLook
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
 * canvas (the project item stays). A node of a deleted item (CNV-11 [PROPOSED], rule 20 §2) offers the
 * buttons of a deleted reference: `되살리기` while it is in the trash and `이대로 두기`; a hidden item
 * while locked offers `지문으로 보기` (rule 19 §6).
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
    /** The referenced item is in the trash or gone: the card is dotted with `◇` (CNV-11). */
    val deleted: Boolean = false,
    /** The item behind a deleted node, for `되살리기` and `이대로 두기`. */
    val item: ItemId? = null,
    /** `되살리기` while the item is in the trash (not after permanent delete). */
    val restoreLabel: String? = null,
    /** `이대로 두기` until it was chosen (the dotted mark stays either way). */
    val keepLabel: String? = null,
    /** `지문으로 보기` on a hidden item while locked. */
    val unlockLabel: String? = null,
)

object NodeEditModel {
    /** The trash kind of an item node's reference. */
    fun itemOf(kind: CanvasNodeKind?): ItemKind? = when (kind) {
        CanvasNodeKind.CHARACTER -> ItemKind.CHARACTER
        CanvasNodeKind.FACTION -> ItemKind.FACTION
        CanvasNodeKind.TERM -> ItemKind.TERM
        CanvasNodeKind.EVENT -> ItemKind.EVENT
        else -> null
    }

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
        val item = if (ref != null) itemOf(kind)?.let { ItemId(it, ref) } else null
        val deleted = label.look == NodeLook.DOTTED && item != null
        return NodeEditView(
            nodeKey = nodeKey,
            title = label.title,
            kindWord = label.kindWord,
            text = text,
            openLabel = openLabel,
            openRoute = route,
            removeLabel = CanvasCopy.TODO_COPY_CANVAS_REMOVE_NODE,
            readOnly = readOnly,
            deleted = deleted,
            item = item.takeIf { deleted },
            restoreLabel = CanvasCopy.BTN_RESTORE.takeIf { deleted && RefFixes.inTrash(doc, item) },
            keepLabel = CanvasCopy.BTN_KEEP.takeIf { deleted && !RefFixes.isKept(doc, item) },
            unlockLabel = CanvasCopy.BTN_UNLOCK.takeIf { label.hiddenLocked },
        )
    }
}
