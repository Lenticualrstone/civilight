package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.trash.DeleteResult
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.domain.trash.RestoreOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.app.ProjectAppState
import io.github.lenticualrstone.civilight.model.canvas.Canvas
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc

/**
 * (0.2.0, N-58, 00 §13.6) Several canvases. Node and edge rules are unchanged (D25). Delete and
 * restore are trash kind `canvas` through `domain:trash` (a canvas's nodes and edges go and come
 * back with it); the last opened canvas is the app-only `_app/view.json › lastCanvasKey` (0.1.0 field).
 */
object CanvasActions {
    /** Live canvases in list order. */
    fun live(doc: ProjectDoc): List<Canvas> =
        doc.canvases.values.filter { it.deletedAt == null }.sortedWith(compareBy<Canvas>({ it.order }, { it.key }))

    /**
     * The canvas home `캔버스` opens: the last opened one while live, else the first live one, else
     * null (the caller creates one, as 0.1.0's first open did).
     */
    fun openTarget(doc: ProjectDoc): Key? {
        val last = doc.app.view.lastCanvasKey
        if (last != null && doc.canvases[last]?.deletedAt == null && last in doc.canvases) return last
        return live(doc).firstOrNull()?.key
    }

    /** App state after opening [canvasKey] (no undo step; `ProjectStore.updateApp`). */
    fun opened(app: ProjectAppState, canvasKey: Key): ProjectAppState =
        if (app.view.lastCanvasKey == canvasKey) app else app.copy(view = app.view.copy(lastCanvasKey = canvasKey))

    /** `+ 캔버스`: a new empty canvas at the end of the list; null when [name] is blank. */
    fun create(doc: ProjectDoc, name: String, ctx: ActionContext): Pair<ProjectDoc, Canvas>? {
        val n = name.nfc().trim().ifEmpty { return null }
        val canvas = Canvas(
            key = ctx.newKey(),
            order = OrderKeys.after(doc.canvases.values.maxOfOrNull { it.order }),
            name = n,
            createdAt = ctx.now,
            updatedAt = ctx.now,
            updatedBy = ctx.device,
        )
        return doc.copy(canvases = doc.canvases + (canvas.key to canvas)) to canvas
    }

    /** Renames a live canvas; a blank name is refused (unchanged document). */
    fun rename(doc: ProjectDoc, canvasKey: Key, name: String, ctx: ActionContext): ProjectDoc {
        val c = doc.canvases[canvasKey]?.takeIf { it.deletedAt == null } ?: return doc
        val n = name.nfc().trim().ifEmpty { return doc }
        if (n == c.name) return doc
        return doc.copy(canvases = doc.canvases + (canvasKey to c.copy(name = n, updatedAt = ctx.now, updatedBy = ctx.device)))
    }

    /** Delete to the trash (kind `canvas`). */
    fun delete(doc: ProjectDoc, canvasKey: Key, ctx: ActionContext): DeleteResult =
        TrashOps.delete(doc, ItemId(ItemKind.CANVAS, canvasKey), ctx.trash)

    /** Restore a canvas trash row. */
    fun restore(doc: ProjectDoc, entryKey: Key, ctx: ActionContext): RestoreOutcome =
        RestoreOps.restore(doc, entryKey, ctx.trash)
}
