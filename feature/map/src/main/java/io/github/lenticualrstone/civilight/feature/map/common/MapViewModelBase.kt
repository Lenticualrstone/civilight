package io.github.lenticualrstone.civilight.feature.map.common

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.map.TravelModeEdit
import io.github.lenticualrstone.civilight.domain.world.map.TravelModeEdits
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.sketch.SketchResult
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.map.MapParent
import io.github.lenticualrstone.civilight.store.blob.import
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.lenticualrstone.civilight.domain.world.gen.RerollMode
import io.github.lenticualrstone.civilight.domain.world.gen.RerollResult
import io.github.lenticualrstone.civilight.domain.world.gen.StampCheck
import io.github.lenticualrstone.civilight.domain.world.gen.StepChoices
import io.github.lenticualrstone.civilight.domain.world.gen.StepReroll
import io.github.lenticualrstone.civilight.domain.world.map.DetailEdits
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.model.map.GenerationStepKind
import io.github.lenticualrstone.civilight.model.map.MapStyle
import io.github.lenticualrstone.civilight.model.open

/** Applying a re-roll computed off the main thread (WLD-56). */
object RerollMerge {
    /**
     * [computed]'s row of [mapKey] put into [now], only while that map is still as it was in
     * [before] (the re-roll ran on it); null when the map changed meanwhile or is gone.
     */
    fun splice(now: ProjectDoc, before: ProjectDoc, computed: ProjectDoc, mapKey: Key): ProjectDoc? {
        val was = before.maps[mapKey] ?: return null
        if (now.maps[mapKey] != was) return null
        val next = computed.maps[mapKey] ?: return null
        return now.copy(maps = now.maps + (mapKey to next))
    }
}

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

    // --- New maps (WLD-36 … 39, 48, 50), from any map screen (a pin's `하위 지도 · 만들기`) ---

    /** Applies a map creation as one undo step `새 지도`; the new map's key, or null (read-only, refused). */
    suspend fun createMap(build: (ProjectDoc) -> Created?): Key? {
        var key: Key? = null
        val result = update(MapCopy.STEP_NEW_MAP) { d -> build(d)?.also { key = it.mapKey }?.doc ?: d }
        if (result == EditResult.READ_ONLY) {
            env.toasts.show(MapCopy.READ_ONLY, actionLabel = null)
            return null
        }
        if (key != null) env.toasts.show(MapCopy.STEP_NEW_MAP, onAction = { undoLast() })
        return key
    }

    suspend fun createNodeMap(parent: MapParent?): Key? {
        val ctx = editContext()
        return createMap { MapCreation.nodeMap(it, ctx, parent) }
    }

    /** `이미지 가져오기` from the gallery or the file picker: stored once by content hash, uncropped. */
    suspend fun importImage(resolver: ContentResolver, uri: Uri, source: BlobSource, parent: MapParent?): Key? {
        if (readOnly.value) {
            env.toasts.show(MapCopy.READ_ONLY, actionLabel = null)
            return null
        }
        val blob = try {
            env.blobs.import(resolver, uri, source)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (blob == null || !blob.mime.startsWith("image/")) {
            env.toasts.show(MapCopy.IMPORT_FAILED, actionLabel = null)
            return null
        }
        val ctx = editContext()
        return createMap { MapCreation.imageMap(it, blob, ctx, parent) }
    }

    /** `스케치 촬영 → 펴기`: the photo and the straightened image become a SKETCH map. */
    suspend fun createSketchMap(result: SketchResult, parent: MapParent?): Key? {
        val ctx = editContext()
        return createMap { MapCreation.sketchMap(it, result.photo, result.straightened, result.corners, ctx, parent) }
    }

    // --- Travel modes (WLD-47) ---

    /**
     * `직접` in the mode editor: a new mode, or new values for [modeKey]. One undo step `이동 수단 ·
     * <name>`; [onApplied] gets the mode's key. Refused values change nothing.
     */
    fun saveTravelMode(modeKey: Key?, name: String, kmPerDay: Double, hours: Double, onApplied: (Key) -> Unit = {}): Job {
        val ctx = editContext()
        return commitValue(MapCopy.step(MapCopy.TRAVEL_MODES, name.trim()), onApplied) { d ->
            val edit = if (modeKey == null) {
                TravelModeEdits.add(d, name, kmPerDay, hours, ctx)
            } else {
                TravelModeEdits.update(TravelModeEdits.ensureStored(d, ctx), modeKey, name, kmPerDay, hours, ctx)
            }
            (edit as? TravelModeEdit.Applied)?.let { it.doc to it.mode.key }
        }
    }

    // --- Map menu (WLD-36 rename, WLD-56 re-roll, WLD-57 style) ---

    /** `이름` of a map: one step `이름 · <name>`; a blank name is refused. */
    fun renameMap(mapKey: Key, name: String): Job {
        val ctx = editContext()
        val clean = name.trim()
        return commit(MapCopy.step(MapCopy.MAP_NAME, clean)) { d ->
            if (clean.isEmpty()) null else MapEdits.updateMap(d, mapKey, ctx) { it.copy(name = clean) }
        }
    }

    /** `그림 · 색면` and `등고선` (WLD-57): one step `그림 · <style>`. */
    fun setStyle(mapKey: Key, style: MapStyle, contours: Boolean): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.STYLE, MapCopy.STYLE_NAMES.getOrNull(style.ordinal))) { d ->
            val map = d.maps[mapKey] ?: return@commit null
            if (map.style?.known == style && map.contours == contours) null
            else MapEdits.updateMap(d, mapKey, ctx) { it.copy(style = style.open(), contours = contours) }
        }
    }

    /**
     * `<단계> 다시 만들기` (WLD-56): the generator runs once off the main thread on the current
     * document, then the result is applied as one step (only if nothing changed meanwhile). A
     * refusal (another generator version) shows its toast; [onChecks] gets the `위치 확인` rows of
     * `모두 새로`.
     */
    fun reroll(mapKey: Key, step: GenerationStepKind, mode: RerollMode, seed: Int, onChecks: (List<StampCheck>) -> Unit = {}): Job = viewModelScope.launch {
        val current = doc.value ?: return@launch
        if (readOnly.value) {
            env.toasts.show(MapCopy.READ_ONLY, actionLabel = null)
            return@launch
        }
        val ctx = editContext()
        val result = withContext(Dispatchers.Default) { StepReroll.apply(current, mapKey, step, seed, mode, ctx) }
        if (result !is RerollResult.Applied) {
            env.toasts.show(MapCopy.REROLL_OTHER_VERSION, actionLabel = null)
            return@launch
        }
        val label = MapCopy.rerollStep(StepChoices.stepName(step))
        var applied = false
        val outcome = update(label) { d -> RerollMerge.splice(d, current, result.doc, mapKey)?.also { applied = true } ?: d }
        if (outcome == EditResult.READ_ONLY) env.toasts.show(MapCopy.READ_ONLY, actionLabel = null)
        if (outcome == EditResult.APPLIED && applied) {
            env.toasts.show(label, onAction = { undoLast() })
            onChecks(result.checks)
        }
    }

    /**
     * `지우기` of a map (WLD-36): into the trash through `domain:trash` `TrashOps` (DECISIONS §1),
     * one undo step with the trash toast `지도 <name> 지움 · …`; [onDone] runs when it was applied.
     */
    fun deleteMap(mapKey: Key, onDone: () -> Unit): Job = viewModelScope.launch {
        val current = doc.value ?: return@launch
        val item = io.github.lenticualrstone.civilight.domain.trash.ItemId.map(mapKey)
        val toast = io.github.lenticualrstone.civilight.domain.trash.Messages.deleteToast(io.github.lenticualrstone.civilight.domain.trash.Impact.of(current, item))
        val ctx = io.github.lenticualrstone.civilight.domain.trash.TrashContext(env.clock.now(), env.clock.zone, env.deviceId(), Keys::newKey)
        var applied = false
        val outcome = update(toast.message) { d ->
            val result = io.github.lenticualrstone.civilight.domain.trash.TrashOps.delete(d, item, ctx)
            if (result.trashed.isEmpty()) d else result.doc.also { applied = true }
        }
        if (outcome == EditResult.READ_ONLY) env.toasts.show(MapCopy.READ_ONLY, actionLabel = null)
        if (outcome == EditResult.APPLIED && applied) {
            env.toasts.show(toast.message, actionLabel = toast.action ?: MapCopy.BTN_UNDO, onAction = { undoLast() })
            onDone()
        }
    }

    /** `위치 확인 · 지금 자리`: the stamp goes to the nearest point of the new layer. */
    fun moveStampTo(mapKey: Key, stampKey: Key, to: io.github.lenticualrstone.civilight.model.Point): Job {
        val ctx = editContext()
        return commit(MapCopy.step(MapCopy.STAMP, MapCopy.BTN_NEW_SPOT)) { d -> DetailEdits.moveStamp(d, mapKey, stampKey, to, ctx) }
    }

    companion object {
        /** The lens point the map draws at (a stale point reads as 최신). */
        fun lensOf(doc: ProjectDoc): LensPoint = LensRules.effective(doc, doc.app.view.lens.point)
    }
}
