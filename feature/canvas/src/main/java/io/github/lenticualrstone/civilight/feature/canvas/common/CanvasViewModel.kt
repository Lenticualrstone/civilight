package io.github.lenticualrstone.civilight.feature.canvas.common

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.app.ProjectAppState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.project.ProjectViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** The edit context of a canvas edit made now: the shell's clock and zone, this installation's id. */
fun FeatureDeps.canvasEditContext(): EditContext =
    EditContext(newKey = Keys::newKey, now = clock.now(), device = stores.deviceId.value, zone = clock.zone)

/**
 * Base of every canvas view model: a [ProjectViewModel] whose edits receive a fresh [EditContext].
 * Every document write goes through [ProjectViewModel.update], so the read-only example project
 * answers [EditResult.READ_ONLY] instead of throwing; [then] lets the screen say `읽기 전용`.
 */
open class CanvasViewModel(
    projectKey: Key,
    sessions: ProjectSessions,
    private val contextOf: () -> EditContext,
) : ProjectViewModel(projectKey, sessions) {
    /** One undo step named [label] (UI copy) built from the document and a new context. */
    suspend fun apply(label: String, mergeKey: String? = null, transform: (ProjectDoc, EditContext) -> ProjectDoc): EditResult {
        val ctx = contextOf()
        return update(label, mergeKey) { doc -> transform(doc, ctx) }
    }

    /** [apply] from a UI callback. */
    fun act(
        label: String,
        mergeKey: String? = null,
        then: (EditResult) -> Unit = {},
        transform: (ProjectDoc, EditContext) -> ProjectDoc,
    ): Job = viewModelScope.launch { then(apply(label, mergeKey, transform)) }

    /** App-only state (viewports); works in read-only projects too. */
    fun actApp(transform: (ProjectAppState) -> ProjectAppState): Job = viewModelScope.launch { updateApp(transform) }
}
