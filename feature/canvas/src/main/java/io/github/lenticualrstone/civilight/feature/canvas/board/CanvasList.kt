package io.github.lenticualrstone.civilight.feature.canvas.board

import io.github.lenticualrstone.civilight.domain.actions.CanvasActions
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.trash.Impact
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.Toast
import io.github.lenticualrstone.civilight.feature.canvas.common.actions
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.SheetKey
import kotlinx.serialization.Serializable

/** N-58: the canvas list sheet, opened from the canvas name `▾` in the head. */
@Serializable
data class CanvasListSheet(val projectKey: Key, val canvasKey: Key) : SheetKey

/** N-58: a canvas row's `⋯` (`이름 바꾸기 · 지우기`, same shape as the page menu, S-6 / D14). */
@Serializable
data class CanvasRowMenuSheet(val projectKey: Key, val canvasKey: Key, val currentKey: Key) : SheetKey

/**
 * One row of the canvas list. [current] marks the open canvas only (drawn selected); [highlighted] is the row
 * just made with `만들기`, flashed for a moment (U-15.6) and never drawn or read as selected.
 */
data class CanvasListRow(
    val key: Key,
    val name: String,
    val current: Boolean,
    val nodes: Int,
    val canDelete: Boolean,
    val highlighted: Boolean = false,
)

/**
 * The canvas list (N-58): live canvases in list order with the open one marked. [canCreate] and [canEdit]
 * (`＋ 새 캔버스` row, row `⋯`) are false in a read-only project (U-15.10).
 */
data class CanvasListView(val rows: List<CanvasListRow>, val canCreate: Boolean, val canEdit: Boolean)

/** The result of `지우기` on a canvas: the new document, the trash entry (for `되돌리기`) and the toast. */
data class CanvasDeleted(val doc: ProjectDoc, val entryKey: Key, val toast: Toast)

/**
 * N-58 pure rules for several canvases. Every write goes through `domain:actions` `CanvasActions`
 * (DECISIONS §1); node and edge rules are unchanged (D25).
 */
object CanvasListModel {
    fun build(doc: ProjectDoc, currentKey: Key?, readOnly: Boolean, highlightedKey: Key? = null): CanvasListView {
        val live = CanvasActions.live(doc)
        return CanvasListView(
            rows = live.map { c ->
                CanvasListRow(c.key, c.name, c.key == currentKey, c.nodes.size, canDelete(doc, c.key, readOnly), c.key == highlightedKey)
            },
            canCreate = !readOnly,
            canEdit = !readOnly,
        )
    }

    /**
     * `지우기` shows only while another live canvas remains: the canvas screen always has a canvas to show,
     * and the home `캔버스` never has to make a new one behind the user's back (§13.6 `ED`).
     */
    fun canDelete(doc: ProjectDoc, key: Key, readOnly: Boolean): Boolean {
        if (readOnly) return false
        val live = CanvasActions.live(doc)
        return live.size > 1 && live.any { it.key == key }
    }

    /** The canvas to show after [deleted] went to the trash: the last opened one when live, else the first. */
    fun afterDelete(doc: ProjectDoc): Key? = CanvasActions.openTarget(doc)
}

/** N-58 writes, as one undo step each through `CanvasActions`. */
object CanvasListEdits {
    /** `만들기` of the create card: a new empty canvas at the end; null when the name is blank. */
    fun create(doc: ProjectDoc, name: String, ctx: EditContext): Pair<ProjectDoc, Key>? =
        CanvasActions.create(doc, name, ctx.actions())?.let { (d, c) -> d to c.key }

    fun rename(doc: ProjectDoc, key: Key, name: String, ctx: EditContext): ProjectDoc =
        CanvasActions.rename(doc, key, name, ctx.actions())

    /** `지우기`: to the trash with its nodes and edges; null when [CanvasListModel.canDelete] is false. */
    fun delete(doc: ProjectDoc, key: Key, ctx: EditContext): CanvasDeleted? {
        if (!CanvasListModel.canDelete(doc, key, readOnly = false)) return null
        val item = ItemId.canvas(key)
        val toast = Messages.deleteToast(Impact.of(doc, item))
        val result = CanvasActions.delete(doc, key, ctx.actions())
        if (item !in result.trashed) return null
        val entry = result.entries.firstOrNull() ?: return null
        return CanvasDeleted(result.doc, entry.key, toast)
    }

    /** `되돌리기` of the delete toast: restores the trash entry (not a generic undo of the last step). */
    fun restore(doc: ProjectDoc, entryKey: Key, ctx: EditContext): ProjectDoc? =
        (CanvasActions.restore(doc, entryKey, ctx.actions()) as? RestoreOutcome.Restored)?.doc
}
