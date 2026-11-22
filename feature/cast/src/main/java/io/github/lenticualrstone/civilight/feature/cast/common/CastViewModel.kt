package io.github.lenticualrstone.civilight.feature.cast.common

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.project.ProjectViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** The edit context of a cast edit made now: the shell's clock and zone, this installation's id. */
fun FeatureDeps.editContext(): EditContext =
    EditContext(now = clock.now(), device = stores.deviceId.value, zone = clock.zone)

/**
 * Base of every cast view model: a [ProjectViewModel] whose edits receive a fresh [EditContext].
 * Every write goes through [ProjectViewModel.update], so the read-only example project answers
 * [EditResult.READ_ONLY] instead of throwing.
 */
open class CastViewModel(
    projectKey: Key,
    sessions: ProjectSessions,
    private val contextOf: () -> EditContext,
) : ProjectViewModel(projectKey, sessions) {
    /** One undo step named [label] (UI copy) built from the document and a new context. */
    suspend fun apply(label: String, mergeKey: String? = null, transform: (ProjectDoc, EditContext) -> ProjectDoc): EditResult {
        val ctx = contextOf()
        return update(label, mergeKey) { doc -> transform(doc, ctx) }
    }

    /** [apply] from a UI callback; [then] runs after the edit with its result. */
    fun act(
        label: String,
        mergeKey: String? = null,
        then: (EditResult) -> Unit = {},
        transform: (ProjectDoc, EditContext) -> ProjectDoc,
    ): Job = viewModelScope.launch { then(apply(label, mergeKey, transform)) }
}

/**
 * The editor behind the shared cast sheets (field value, history, `항목 넣기`). Sheets are drawn
 * inside whichever entry opened them, so each sheet gets this view model from that entry by key.
 */
class CastEditorViewModel(
    projectKey: Key,
    sessions: ProjectSessions,
    contextOf: () -> EditContext,
) : CastViewModel(projectKey, sessions, contextOf)
