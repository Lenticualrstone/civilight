package io.github.lenticualrstone.civilight.feature.canvas.label

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.feature.canvas.CanvasCopy
import io.github.lenticualrstone.civilight.feature.canvas.common.CanvasViewModel
import io.github.lenticualrstone.civilight.feature.canvas.edit.CanvasEdits
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import androidx.compose.runtime.mutableStateMapOf

/**
 * Drafts of open `선 라벨` surfaces, by line. Kept in a view model of the entry that shows them, so a
 * draft survives the `시작 시점` picker stacked over the sheet and the popover hiding for it. A close
 * without a button drops the draft: nothing is recorded and typed text is not kept (Q-CV-09 default).
 */
class LineLabelEditor(
    projectKey: Key,
    sessions: ProjectSessions,
    contextOf: () -> EditContext,
) : CanvasViewModel(projectKey, sessions, contextOf) {
    /** Snapshot state, so a keystroke reaches the field in the same frame (no async round trip). */
    private val drafts = mutableStateMapOf<Key, LineLabelDraft>()

    fun draft(edgeKey: Key): LineLabelDraft? = drafts[edgeKey]

    fun put(edgeKey: Key, draft: LineLabelDraft) {
        drafts[edgeKey] = draft
    }

    fun discard(edgeKey: Key) {
        drafts.remove(edgeKey)
    }

    /** `그냥 선으로 두기`. */
    fun keep(canvasKey: Key, edgeKey: Key, draft: LineLabelDraft, then: (EditResult) -> Unit) {
        act(CanvasCopy.BTN_KEEP_LINE, then = { r -> if (r == EditResult.APPLIED) discard(edgeKey); then(r) }) { doc, ctx ->
            LineLabelModel.keep(doc, canvasKey, edgeKey, draft, ctx)
        }
    }

    /** `관계로 만들기`. */
    fun make(canvasKey: Key, edgeKey: Key, draft: LineLabelDraft, then: (EditResult) -> Unit) {
        act(CanvasCopy.BTN_MAKE_RELATION, then = { r -> if (r == EditResult.APPLIED) discard(edgeKey); then(r) }) { doc, ctx ->
            LineLabelModel.make(doc, canvasKey, edgeKey, draft, ctx)
        }
    }

    /**
     * Takes the line off the canvas (Q-CV-13 default: a converted line's relation stays). One undo step
     * named by the button (copy missing: `BTN_REMOVE_LINE`, proposed).
     */
    fun remove(canvasKey: Key, edgeKey: Key, then: (EditResult) -> Unit) {
        act(CanvasCopy.BTN_REMOVE_LINE, then = { r -> if (r == EditResult.APPLIED) discard(edgeKey); then(r) }) { doc, ctx ->
            CanvasEdits.removeLine(doc, canvasKey, edgeKey, ctx)
        }
    }

    companion object {
        /** The view-model key every host in one entry uses, so they share drafts. */
        const val KEY = "canvas-line-label"
    }
}
