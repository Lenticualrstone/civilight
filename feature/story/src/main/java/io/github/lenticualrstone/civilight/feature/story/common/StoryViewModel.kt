package io.github.lenticualrstone.civilight.feature.story.common

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.SceneDeleteResult
import io.github.lenticualrstone.civilight.domain.story.structure.NewUnitResult
import io.github.lenticualrstone.civilight.domain.trash.DocChange
import io.github.lenticualrstone.civilight.domain.trash.Toast
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.SceneNotation
import io.github.lenticualrstone.civilight.store.AppClock
import io.github.lenticualrstone.civilight.store.recorder.RecorderStore
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.project.ProjectViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.ToastController
import io.github.lenticualrstone.civilight.ui.security.UnlockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The app-scoped services a story view model may keep. View models outlive configuration changes,
 * so they hold these (all owned by the application) and never the activity's [FeatureDeps] or its
 * navigator; navigation stays in the composables.
 */
class StoryServices(
    val projects: ProjectSessions,
    val clock: AppClock,
    val deviceId: () -> String,
    val toasts: ToastController,
    val appScope: CoroutineScope,
    val unlock: StateFlow<UnlockState>,
    /** The global recorder library (home's `시작해 볼 것` counts the records linked to the project). */
    val recorder: RecorderStore,
    /** Whether a blob's file is on this device (home's cover falls back to the initial-letter card, U-08 LIB-88). */
    val blobExists: (String) -> Boolean = { true },
) {
    /** One context per action: one clock read, one key source (DECISIONS §4). */
    fun actionContext(): ActionContext = ActionContext(now = clock.now(), zone = clock.zone, device = deviceId())
}

fun FeatureDeps.storyServices(): StoryServices =
    StoryServices(projects, clock, { stores.deviceId.value }, toasts, appScope, unlock.state, stores.recorder, stores.blobs::contains)

/**
 * Base of every story view model (the pattern later batches follow): a [ProjectViewModel] plus the
 * story edits. Screen view models add their own derived UI state; sheets that other lanes host use
 * [StoryActionsViewModel]. Every edit is one undo step; a stale precondition leaves the doc as it is.
 */
abstract class StoryViewModel(projectKey: Key, protected val services: StoryServices) :
    ProjectViewModel(projectKey, services.projects) {

    /** True while the hidden items of this project are locked (R19 §6: `숨긴 씬` masks). */
    fun itemsLocked(state: UnlockState): Boolean = !state.itemsOpen(projectKey)

    /**
     * Runs [transform] as one undo step named [label]. The store runs the transform on the caller's
     * thread, so it is launched on [Dispatchers.Default]: some transforms re-run checks or a term scan.
     */
    fun apply(label: String, transform: (ProjectDoc, ActionContext) -> ProjectDoc): Job = viewModelScope.launch(Dispatchers.Default) {
        val ctx = services.actionContext()
        update(label) { doc -> transform(doc, ctx) }
    }

    /** Like [apply], but consecutive edits with the same [mergeKey] form one undo step (typing in a field). */
    fun applyMerged(label: String, mergeKey: String, transform: (ProjectDoc, ActionContext) -> ProjectDoc): Job = viewModelScope.launch(Dispatchers.Default) {
        val ctx = services.actionContext()
        update(label, mergeKey) { doc -> transform(doc, ctx) }
    }

    /**
     * `씬 지우기` (STO-10): through `SceneActions.delete`, without asking, then the toast
     * `씬 3 지움 · ◆ 3 · ◇ 1은 씬 4 처음으로` with `되돌리기`. The undo runs on the app scope with its own
     * session, so it still works after the screen that deleted the scene is gone.
     */
    fun deleteScene(sceneKey: Key): Job = viewModelScope.launch {
        val ctx = services.actionContext()
        var result: SceneDeleteResult? = null
        val label = doc.value?.let { StoryActions.deleteLabel(it, sceneKey) } ?: StoryCopy.deleted(StoryCopy.UNTITLED)
        val applied = update(label) { doc ->
            val r = StoryActions.deleteScene(doc, sceneKey, ctx)
            result = r
            r?.doc ?: doc
        }
        if (applied != EditResult.APPLIED) return@launch
        // (0.3.2) The transform ran but the scene was no longer live: say so instead of closing silently.
        val done = result ?: return@launch services.toasts.show(StoryCopy.ALREADY_DELETED_SCENE, actionLabel = null)
        services.toasts.show(done.toast.message, done.toast.action ?: StoryCopy.UNDO) { undoDelete(label, done) }
    }

    private fun undoDelete(label: String, result: SceneDeleteResult) {
        val sessions = services.projects
        services.appScope.launch {
            val store = sessions.acquire(projectKey)
            try {
                if (!store.readOnly) store.update(label) { StoryActions.undoDelete(it, result) }
            } finally {
                sessions.release(projectKey)
            }
        }
    }

    /**
     * A `되살리기` (STO-11, R20 §2) as one undo step: [restore] returns what it restored, or null when
     * nothing was (stale, or the gate wants authentication). Its toast comes with `되돌리기`, which
     * reverts exactly that restore row by row on the app scope (as the delete toast does).
     */
    fun restoreWithToast(label: String, restore: (ProjectDoc, ActionContext) -> RestoreStep?): Job = viewModelScope.launch {
        val ctx = services.actionContext()
        var step: RestoreStep? = null
        val applied = update(label) { doc ->
            val r = restore(doc, ctx)
            step = r
            r?.doc ?: doc
        }
        val done = step ?: return@launch
        if (applied != EditResult.APPLIED) return@launch
        val toast = done.toast ?: return@launch
        services.toasts.show(toast.message, toast.action ?: StoryCopy.UNDO) { revertOnAppScope(label, done.change) }
    }

    private fun revertOnAppScope(label: String, change: DocChange) {
        val sessions = services.projects
        services.appScope.launch {
            val store = sessions.acquire(projectKey)
            try {
                if (!store.readOnly) store.update(label) { change.revert(it) }
            } finally {
                sessions.release(projectKey)
            }
        }
    }

    /** `새 에피소드` after [unitKey] (STO-05). */
    fun newUnitAfter(unitKey: Key, label: String): Job =
        newUnit(label) { doc, ctx -> StoryActions.newUnitAfter(doc, unitKey, ctx) }

    /** `새 장` at the end (STO-05). */
    fun newTopUnit(label: String): Job = newUnit(label) { doc, ctx -> StoryActions.newTopUnit(doc, ctx) }

    private fun newUnit(label: String, make: (ProjectDoc, ActionContext) -> NewUnitResult?): Job =
        apply(label) { doc, ctx -> make(doc, ctx)?.doc ?: doc }

    /** `완료` on or off (STO-06). */
    fun setDone(unitKey: Key, done: Boolean, label: String): Job =
        apply(label) { doc, ctx -> StoryActions.setDone(doc, unitKey, done, ctx) }

    /** `때 표기` (STO-17). */
    fun setNotation(notation: SceneNotation, label: String): Job =
        apply(label) { doc, ctx -> StoryActions.setNotation(doc, notation, ctx) }
}

/** What a `되살리기` did: the next doc, the row-level change (for `되돌리기`) and the toast. */
data class RestoreStep(val doc: ProjectDoc, val change: DocChange, val toast: Toast?)

/** The view model of story sheets shown over any project screen (`UnitMenuSheet`, `NewUnitSheet`). */
class StoryActionsViewModel(projectKey: Key, services: StoryServices) : StoryViewModel(projectKey, services)
