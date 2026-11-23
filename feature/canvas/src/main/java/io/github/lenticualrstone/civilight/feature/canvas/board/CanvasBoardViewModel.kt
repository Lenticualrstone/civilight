package io.github.lenticualrstone.civilight.feature.canvas.board

import io.github.lenticualrstone.civilight.domain.actions.CanvasActions
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.feature.canvas.CanvasCopy
import io.github.lenticualrstone.civilight.feature.canvas.common.RefFixes
import io.github.lenticualrstone.civilight.feature.canvas.common.RestoreResult
import kotlinx.coroutines.launch
import io.github.lenticualrstone.civilight.feature.canvas.common.CanvasViewModel
import io.github.lenticualrstone.civilight.feature.canvas.edit.CanvasEdits
import io.github.lenticualrstone.civilight.feature.canvas.edit.LineResult
import io.github.lenticualrstone.civilight.ui.graph.GraphViewport
import io.github.lenticualrstone.civilight.ui.graph.Pt
import io.github.lenticualrstone.civilight.ui.graph.Sz
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.Viewport
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions

/**
 * The canvas editor (CNV-02 … CNV-10). Each user action is one undo step named by its tool or button;
 * drags and pan/zoom stay on screen and are written once, at their end. In a read-only project every
 * write answers [EditResult.READ_ONLY] and the screen says `읽기 전용`.
 */
class CanvasBoardViewModel(
    projectKey: Key,
    sessions: ProjectSessions,
    contextOf: () -> EditContext,
) : CanvasViewModel(projectKey, sessions, contextOf) {
    private var creatingCanvas = false

    /**
     * CNV-02 [PROPOSED] / §13.6 `ED`: a project without a canvas gets an empty one when the canvas is opened.
     * The latch is released once the write is done, so a later empty state can make one again.
     */
    fun createFirstCanvas() {
        if (creatingCanvas) return
        creatingCanvas = true
        act(CanvasCopy.UNDO_NEW_CANVAS, then = { creatingCanvas = false }) { doc, ctx ->
            if (CanvasActions.live(doc).isNotEmpty()) doc else CanvasListEdits.create(doc, CanvasCopy.NEW_CANVAS_NAME, ctx)?.first ?: doc
        }
    }

    /** N-58 / U-15: `만들기` of the create card (`CanvasActions.create`); [then] gets the new canvas key. */
    fun createCanvas(name: String, then: (EditResult, Key?) -> Unit) {
        var made: Key? = null
        act(CanvasCopy.UNDO_NEW_CANVAS, then = { then(it, made) }) { doc, ctx ->
            CanvasListEdits.create(doc, name, ctx)?.also { made = it.second }?.first ?: doc
        }
    }

    /** N-58 `이름 바꾸기` (`CanvasActions.rename`). */
    fun renameCanvas(canvasKey: Key, name: String, then: (EditResult) -> Unit) {
        act(CanvasCopy.UNDO_RENAME, then = then) { doc, ctx -> CanvasListEdits.rename(doc, canvasKey, name, ctx) }
    }

    /** N-58 `지우기` (`CanvasActions.delete`, trash kind `canvas`); [then] gets the trash entry and toast. */
    fun deleteCanvas(canvasKey: Key, then: (EditResult, CanvasDeleted?) -> Unit) {
        var deleted: CanvasDeleted? = null
        act(CanvasCopy.UNDO_DELETE, then = { then(it, deleted) }) { doc, ctx ->
            CanvasListEdits.delete(doc, canvasKey, ctx)?.also { deleted = it }?.doc ?: doc
        }
    }

    fun addItem(canvasKey: Key, kind: CanvasNodeKind, refKey: Key, topLeft: Pt, then: (EditResult, Key?) -> Unit) {
        var made: Key? = null
        act(CanvasCopy.UNDO_ADD_NODE, then = { then(it, made) }) { doc, ctx ->
            CanvasEdits.addItemNode(doc, canvasKey, kind, refKey, topLeft, ctx).also { made = it.second }.first
        }
    }

    fun addMemo(canvasKey: Key, topLeft: Pt, then: (EditResult, Key?) -> Unit) {
        var made: Key? = null
        act(CanvasCopy.UNDO_MEMO, then = { then(it, made) }) { doc, ctx ->
            CanvasEdits.addMemo(doc, canvasKey, topLeft, ctx).also { made = it.second }.first
        }
    }

    fun addGroup(canvasKey: Key, topLeft: Pt, then: (EditResult, Key?) -> Unit) {
        var made: Key? = null
        act(CanvasCopy.UNDO_GROUP, then = { then(it, made) }) { doc, ctx ->
            CanvasEdits.addGroup(doc, canvasKey, topLeft, ctx).also { made = it.second }.first
        }
    }

    fun move(canvasKey: Key, nodeKey: Key, topLeft: Pt, size: Sz, then: (EditResult) -> Unit) {
        act(CanvasCopy.UNDO_MOVE, then = then) { doc, ctx -> CanvasEdits.moveNode(doc, canvasKey, nodeKey, topLeft, ctx, size) }
    }

    fun drawLine(canvasKey: Key, from: Key, to: Key, then: (EditResult, LineResult) -> Unit) {
        var result: LineResult = LineResult.Refused
        act(CanvasCopy.UNDO_LINE, then = { then(it, result) }) { doc, ctx ->
            CanvasEdits.addLine(doc, canvasKey, from, to, ctx).also { result = it.second }.first
        }
    }

    /** Memo text or 묶음 title; quick typing merges into one undo step per node. */
    fun setText(canvasKey: Key, nodeKey: Key, text: String) {
        act(CanvasCopy.UNDO_MEMO, mergeKey = "canvas-text-$nodeKey") { doc, ctx -> CanvasEdits.setText(doc, canvasKey, nodeKey, text, ctx) }
    }

    fun removeNode(canvasKey: Key, nodeKey: Key, then: (EditResult) -> Unit) {
        act(CanvasCopy.UNDO_REMOVE, then = then) { doc, ctx -> CanvasEdits.removeNode(doc, canvasKey, nodeKey, ctx) }
    }

    /** `되살리기` of a deleted item behind a node (CNV-11); [then] gets the toast or `needsAuth`. */
    fun restoreItem(item: ItemId, authenticated: Boolean, then: (EditResult, RestoreResult) -> Unit) {
        var result = RestoreResult(null, null)
        act(CanvasCopy.BTN_RESTORE, then = { then(it, result) }) { doc, ctx ->
            result = RefFixes.restore(doc, item, ctx, authenticated)
            result.doc ?: doc
        }
    }

    /** `이대로 두기` on a deleted item behind a node (CNV-11). */
    fun keepItem(item: ItemId, then: (EditResult) -> Unit) {
        act(CanvasCopy.BTN_KEEP, then = then) { doc, ctx -> RefFixes.keep(doc, item, ctx) }
    }

    /** `되돌리기` of the last step (the restore toast). */
    fun undoLast() {
        viewModelScope.launch { undo() }
    }

    /** CNV-02 / N-58: the canvas home `캔버스` opens next (`CanvasActions.opened`, `_app/view.json` `lastCanvasKey`). */
    fun markOpened(canvasKey: Key) {
        actApp { app -> CanvasActions.opened(app, canvasKey) }
    }

    /** The canvas viewport is app state (`_app/view.json`), never exported (CNV-12 [PROPOSED]). */
    fun saveViewport(canvasKey: Key, viewport: GraphViewport) {
        actApp { app ->
            val view = app.view
            app.copy(view = view.copy(canvasViewports = view.canvasViewports + (canvasKey to viewport.toModel())))
        }
    }
}

fun GraphViewport.toModel(): Viewport = Viewport(x = offsetX, y = offsetY, zoom = zoom)

fun Viewport.toGraph(): GraphViewport = GraphViewport(offsetX = x, offsetY = y, zoom = GraphViewport.clampZoom(zoom))
