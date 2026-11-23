package io.github.lenticualrstone.civilight.feature.canvas.common

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Writes that must outlive the screen that starts them: a toast's `되돌리기` pressed after the user left the
 * canvas screen (toasts stay when a screen is popped, DS-67 `SCREEN_POPPED`). They run on the app scope with
 * their own project session, never on a view model's scope (as feature:cast `CastWrites`).
 */
object CanvasWrites {
    /** One undo step named [label]; [transform] returns the document unchanged to change nothing. Read-only: nothing. */
    fun launch(deps: FeatureDeps, projectKey: Key, label: String, transform: (ProjectDoc, EditContext) -> ProjectDoc): Job =
        deps.appScope.launch {
            val store = deps.projects.acquire(projectKey)
            try {
                if (store.readOnly) return@launch
                val ctx = deps.canvasEditContext()
                store.update(label, null) { doc -> transform(doc, ctx) }
            } finally {
                deps.projects.release(projectKey)
            }
        }
}
