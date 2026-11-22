package io.github.lenticualrstone.civilight.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ProjectAppState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.store.ReadOnlyProjectException
import io.github.lenticualrstone.civilight.store.project.ProjectStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Loading state of a project screen. */
sealed interface ProjectLoad {
    data object Loading : ProjectLoad

    data class Ready(val doc: ProjectDoc, val readOnly: Boolean) : ProjectLoad

    /** The project could not be opened (gone, unreadable, newer store version). */
    data class Failed(val error: Throwable) : ProjectLoad
}

/** Outcome of an edit. */
enum class EditResult {
    APPLIED,

    /** The project is read-only (the example, LIB-17); nothing changed. */
    READ_ONLY,
}

/**
 * Base for every project-scoped view model: opens the project through [ProjectSessions] on
 * creation, exposes the document as [state] / [doc], and releases it in [onCleared].
 *
 * Edits are pure `ProjectDoc -> ProjectDoc` functions passed to [update] with an undo label (the
 * label is user-visible in undo toasts, so it is UI copy). App-only state (lens, viewports, last
 * position) goes through [updateApp], which also works in read-only projects.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ProjectViewModel(
    val projectKey: Key,
    private val sessions: ProjectSessions,
) : ViewModel() {
    private val store = MutableStateFlow<ProjectStore?>(null)
    private val failure = MutableStateFlow<Throwable?>(null)
    private var acquired = false

    val state: StateFlow<ProjectLoad> = combine(
        store.flatMapLatest { s -> s?.doc?.map { doc -> ProjectLoad.Ready(doc, s.readOnly) } ?: flowOf(null) },
        failure,
    ) { ready, error ->
        when {
            ready != null -> ready
            error != null -> ProjectLoad.Failed(error)
            else -> ProjectLoad.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProjectLoad.Loading)

    /** The document, or null while loading or after a failure. */
    val doc: StateFlow<ProjectDoc?> = state
        .map { (it as? ProjectLoad.Ready)?.doc }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            try {
                val opened = sessions.acquire(projectKey)
                acquired = true
                store.value = opened
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                failure.value = e
            }
        }
    }

    /** The open store, or null while loading. */
    protected val projectStore: ProjectStore? get() = store.value

    val isReadOnly: Boolean get() = store.value?.readOnly ?: false

    /** Applies [transform] as one undo step named [label]; [mergeKey] merges quick typing into one step. */
    suspend fun update(label: String, mergeKey: String? = null, transform: (ProjectDoc) -> ProjectDoc): EditResult {
        val s = awaitStore()
        if (s.readOnly) return EditResult.READ_ONLY
        return try {
            s.update(label, mergeKey, transform)
            EditResult.APPLIED
        } catch (e: ReadOnlyProjectException) {
            EditResult.READ_ONLY
        }
    }

    /** [update] from UI callbacks. */
    fun edit(label: String, mergeKey: String? = null, transform: (ProjectDoc) -> ProjectDoc): Job =
        viewModelScope.launch { update(label, mergeKey, transform) }

    /** Changes `_app/` state (no undo step; allowed in read-only projects). */
    suspend fun updateApp(transform: (ProjectAppState) -> ProjectAppState) = awaitStore().updateApp(transform)

    /** Undo / redo of the project's history; returns the step's label for the toast. */
    suspend fun undo(): String? = awaitStore().undo()

    suspend fun redo(): String? = awaitStore().redo()

    /** Waits for the open in [init]; throws its error when the project could not be opened. */
    private suspend fun awaitStore(): ProjectStore {
        store.value?.let { return it }
        val settled = state.first { it !is ProjectLoad.Loading }
        if (settled is ProjectLoad.Failed) throw settled.error
        return checkNotNull(store.value)
    }

    override fun onCleared() {
        if (acquired) sessions.release(projectKey)
        super.onCleared()
    }
}
