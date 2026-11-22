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
import io.github.lenticualrstone.civilight.store.recorder.RecorderStore
import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import java.time.ZoneId
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.project.ProjectViewModel
import io.github.lenticualrstone.civilight.ui.security.UnlockController
import kotlinx.coroutines.CoroutineScope
import io.github.lenticualrstone.civilight.domain.actions.MergeActionResult
import io.github.lenticualrstone.civilight.domain.actions.MergeActions
import io.github.lenticualrstone.civilight.domain.trash.DeleteImpact
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.MergeOps
import io.github.lenticualrstone.civilight.domain.trash.Toast
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.domain.trash.MergeChoices as TrashMergeChoices
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** After `합치기`: the toast `시장 골목 → 잿더미 시장 합침 · 참조 2` and its `되돌리기`. */
class Merged(val toast: Toast, val undo: () -> Unit)

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
    /** The global Life Recorder library: its links follow a merged item (WLD-15). Null in tests. */
    val recorder: RecorderStore? = null,
    val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    companion object {
        fun of(deps: FeatureDeps) = WorldEnv(
            sessions = deps.projects,
            unlock = deps.unlock,
            clock = deps.clock,
            deviceId = { deps.stores.deviceId.value },
            appScope = deps.appScope,
            recorder = deps.stores.recorder,
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

    /** The same for `domain:actions` and `domain:trash` (one key source and clock, DECISIONS §1). */
    fun actionContext(): ActionContext = ActionContext(now = env.clock.now(), zone = env.zone(), device = env.deviceId(), newKey = Keys::newKey)

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

    /** `숨기기` / `숨김 풀기` (WLD-13, rule 19): one undo step. */
    fun setHidden(kind: WorldKind, key: Key, hidden: Boolean): Job {
        val ctx = editContext()
        return viewModelScope.launch {
            apply(if (hidden) WorldCopy.MENU_HIDE else WorldCopy.MENU_UNHIDE) { d -> ItemLifecycle.setHidden(d, kind, key, hidden, ctx) }
        }
    }

    /** The Life Recorder library, loaded (its chips count as references, rule 20). */
    protected suspend fun recorderLibrary(): RecorderLibrary? = env.recorder?.let { store ->
        runCatching { store.ensureLoaded() }
        store.state.value.library
    }

    /** The `지우기` impact of [item] (`같이 휴지통으로` · `제자리에 남음`); null before the project is open. */
    suspend fun impact(item: ItemId): DeleteImpact? = doc.value?.let { ItemLifecycle.impact(it, item, recorderLibrary()) }

    /** `휴지통으로` (WLD-14): one undo step; null when nothing was deleted (read-only, not live). */
    suspend fun deleteItem(item: ItemId): Deleted? {
        var deleted: Deleted? = null
        val ctx = actionContext()
        val recorder = recorderLibrary()
        val result = apply(WorldCopy.MENU_DELETE) { d -> ItemLifecycle.delete(d, item, ctx, recorder)?.also { deleted = it }?.doc ?: d }
        return if (result == EditResult.APPLIED) deleted else null
    }

    /** `되살리기` of a trashed item from its card; hidden items need [authenticated]. */
    suspend fun restoreItem(item: ItemId, authenticated: Boolean): Restored {
        var outcome: Restored = Restored.NotFound
        val ctx = actionContext()
        apply(WorldCopy.BTN_RESTORE) { d ->
            when (val r = ItemLifecycle.restore(d, item, ctx, authenticated)) {
                is Restored.Done -> r.doc.also { outcome = r }
                else -> d.also { outcome = r }
            }
        }
        return outcome
    }

    /**
     * `합치기` (WLD-15) through `domain:actions` [MergeActions.mergeItems]: [mergedKey] goes into
     * [targetKey] with [choices] as one undo step, and the Life Recorder links of the merged item
     * move to the target. Returns the toast, or null when nothing happened.
     */
    suspend fun merge(kind: WorldKind, mergedKey: Key, targetKey: Key, choices: TrashMergeChoices): Merged? {
        var done: MergeActionResult? = null
        val ctx = actionContext()
        val recorder = recorderLibrary()
        val itemKind = ItemLifecycle.itemKind(kind)
        val result = apply(WorldCopy.MENU_MERGE) { d ->
            if (!MergePlan.canMerge(d, kind, mergedKey, targetKey)) d
            else MergeActions.mergeItems(d, itemKind, mergedKey, targetKey, ctx, choices, recorder).also { done = it }.doc
        }
        val merged = done
        if (result != EditResult.APPLIED || merged == null) return null
        val projectKey = merged.doc.key
        env.recorder?.updateLibrary { lib -> MergeOps.applyToRecorder(lib, projectKey, itemKind, mergedKey, targetKey) }
        // The target card opens right after the merge, which may clear this view model: the undo runs
        // in the app scope on the project's open store.
        val key = projectKey
        return Merged(merged.toast) {
            env.appScope.launch {
                val store = env.sessions.peek(key) ?: return@launch
                if (store.readOnly) return@launch
                store.undo()
                env.recorder?.updateLibrary { lib -> MergeActions.undo(merged.doc, lib, merged).second ?: lib }
            }
        }
    }

    /** `최신으로` on the past-lens banner (WLD-12): `_app/` only, no undo step, works read-only. */
    fun backToLatest(): Job = viewModelScope.launch {
        updateApp { app -> app.copy(view = app.view.copy(lens = app.view.lens.copy(point = LensPoint.Latest))) }
    }
}
