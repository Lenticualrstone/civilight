package io.github.lenticualrstone.civilight.feature.map.common

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.store.AppClock
import io.github.lenticualrstone.civilight.store.blob.BlobStore
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.lens.LensRules
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.project.ProjectViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.ToastController
import io.github.lenticualrstone.civilight.ui.security.UnlockController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The app-scoped parts of [FeatureDeps] a map view model may keep. Never the navigator or the deps
 * object itself: those belong to the activity, and view models outlive it.
 */
class MapEnv(
    val sessions: ProjectSessions,
    val unlock: UnlockController,
    val clock: AppClock,
    val deviceId: () -> String,
    val blobs: BlobStore,
    val toasts: ToastController,
    val appScope: CoroutineScope,
) {
    companion object {
        fun of(deps: FeatureDeps) = MapEnv(
            sessions = deps.projects,
            unlock = deps.unlock,
            clock = deps.clock,
            deviceId = { deps.stores.deviceId.value },
            blobs = deps.stores.blobs,
            toasts = deps.toasts,
            appScope = deps.appScope,
        )
    }
}

/**
 * Base of every map view model: the project document (through [ProjectViewModel]), read-only state
 * (the example, LIB-17), whether hidden places and factions are open (rule 19), the lens point, and
 * [commit], the one write path: one `update` per user action, then the `되돌리기` toast.
 */
abstract class MapScreenViewModel(projectKey: Key, protected val env: MapEnv) : ProjectViewModel(projectKey, env.sessions) {
    /** True while loading and for read-only projects: mutating controls show `읽기 전용`. */
    val readOnly: StateFlow<Boolean> = state
        .map { (it as? ProjectLoad.Ready)?.readOnly ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Hidden places and factions of this project are open (`지문으로 보기` passed). */
    val hiddenOpen: StateFlow<Boolean> = env.unlock.state
        .map { it.itemsOpen(projectKey) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, env.unlock.state.value.itemsOpen(projectKey))

    /** Keys, clock and device id for domain edits. */
    fun editContext(): EditContext = EditContext(newKey = Keys::newKey, now = env.clock::now, device = env.deviceId())

    /**
     * Applies [transform] as one undo step named [label] and shows it as a toast with `되돌리기`.
     * A transform returning null changes nothing (refused edit) and shows no toast.
     */
    fun commit(label: String, onApplied: () -> Unit = {}, transform: (ProjectDoc) -> ProjectDoc?): Job =
        commitValue(label, { onApplied() }) { d -> transform(d)?.let { it to Unit } }

    /** [commit] for edits that make something: [onApplied] gets its value (a new key). */
    fun <T> commitValue(label: String, onApplied: (T) -> Unit = {}, transform: (ProjectDoc) -> Pair<ProjectDoc, T>?): Job = viewModelScope.launch {
        val current = doc.value ?: return@launch
        val preview = transform(current)
        if (preview == null || preview.first == current) return@launch
        var made: Pair<ProjectDoc, T>? = null
        val result = update(label) { d -> transform(d)?.also { made = it }?.first ?: d }
        if (result == EditResult.READ_ONLY) env.toasts.show(MapCopy.READ_ONLY, actionLabel = null)
        val value = made
        if (result == EditResult.APPLIED && value != null) {
            onApplied(value.second)
            env.toasts.show(label, onAction = { undoLast() })
        }
    }

    /** `되돌리기` of the project's last step; on the app scope, because the toast outlives the screen. */
    fun undoLast(): Job = env.appScope.launch { undo() }

    companion object {
        /** The lens point the map draws at (a stale point reads as 최신). */
        fun lensOf(doc: ProjectDoc): LensPoint = LensRules.effective(doc, doc.app.view.lens.point)
    }
}
