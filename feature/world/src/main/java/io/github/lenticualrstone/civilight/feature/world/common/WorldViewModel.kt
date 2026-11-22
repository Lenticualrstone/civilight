package io.github.lenticualrstone.civilight.feature.world.common

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.store.AppClock
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.project.ProjectViewModel
import io.github.lenticualrstone.civilight.ui.security.UnlockController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The app-scoped parts of [FeatureDeps] a world view model may keep (never the navigator or the
 * deps object itself: those belong to the activity, view models outlive it).
 */
class WorldEnv(
    val sessions: ProjectSessions,
    val unlock: UnlockController,
    val clock: AppClock,
    val deviceId: () -> String,
    val appScope: CoroutineScope,
) {
    companion object {
        fun of(deps: FeatureDeps) = WorldEnv(
            sessions = deps.projects,
            unlock = deps.unlock,
            clock = deps.clock,
            deviceId = { deps.stores.deviceId.value },
            appScope = deps.appScope,
        )
    }
}

/**
 * Base of every world view model: the project document (from [ProjectViewModel]), whether the
 * project is read-only (the example, LIB-17) and whether its hidden items are open (rule 19, one
 * unlock per project). Edits go through [apply] with an undo label from `WorldCopy`.
 */
abstract class WorldViewModel(projectKey: Key, protected val env: WorldEnv) : ProjectViewModel(projectKey, env.sessions) {
    /** True while loading and for read-only projects: mutating controls are disabled with `읽기 전용`. */
    val readOnly: StateFlow<Boolean> = state
        .map { (it as? ProjectLoad.Ready)?.readOnly ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Hidden terms, places and factions of this project are open (`지문으로 보기` passed). */
    val hiddenOpen: StateFlow<Boolean> = env.unlock.state
        .map { it.itemsOpen(projectKey) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, env.unlock.state.value.itemsOpen(projectKey))

    /** Keys, clock and device id for domain edits (never the domain defaults). */
    fun editContext(): EditContext = EditContext(newKey = Keys::newKey, now = env.clock::now, device = env.deviceId())

    /** Applies [transform] as one undo step named [label]. */
    suspend fun apply(label: String, transform: (ProjectDoc) -> ProjectDoc): EditResult = update(label, transform = transform)

    /** `되돌리기` of the project's last step (the toast action). */
    fun undoLast(): Job = viewModelScope.launch { undo() }

    /** `이름 바꾸기` (WLD-11). An empty name changes nothing. */
    suspend fun rename(kind: WorldKind, key: Key, name: String): EditResult {
        val ctx = editContext()
        return apply(WorldCopy.RENAME) { d -> ItemEdits.rename(d, kind, key, name, ctx) ?: d }
    }

    /** `설명 고치기`: keeps links and paragraph keys. */
    suspend fun setDescription(kind: WorldKind, key: Key, text: String): EditResult {
        val ctx = editContext()
        return apply(WorldCopy.EDIT_DESCRIPTION) { d -> ItemEdits.setDescription(d, kind, key, text, ctx) }
    }

    /** `설명 고치기` with the editor's draft (text plus the `@` links inserted in the sheet, WLD-17). */
    suspend fun setDescriptionDoc(kind: WorldKind, key: Key, description: RichDoc): EditResult {
        val ctx = editContext()
        return apply(WorldCopy.EDIT_DESCRIPTION) { d -> ItemEdits.setDescriptionDoc(d, kind, key, description, ctx) }
    }

    /** `최신으로` on the past-lens banner (WLD-12): `_app/` only, no undo step, works read-only. */
    fun backToLatest(): Job = viewModelScope.launch {
        updateApp { app -> app.copy(view = app.view.copy(lens = app.view.lens.copy(point = LensPoint.Latest))) }
    }
}
