package io.github.lenticualrstone.civilight.model.app

import io.github.lenticualrstone.civilight.model.IsoDate
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.json.LowerCamelEnumSerializer
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.gallery.GalleryViewState
import io.github.lenticualrstone.civilight.model.script.ScriptViewState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * App-only state of one project (class A, `_app/…` in the working store, §9); never exported.
 * `_app/activity.json` = [ProjectActivityFile], `_app/view.json` = [ProjectViewFile].
 */
data class ProjectAppState(
    val activity: ProjectActivityFile = ProjectActivityFile(),
    val view: ProjectViewFile = ProjectViewFile(),
)

/** `_app/activity.json`. */
@Serializable
data class ProjectActivityFile(
    val activity: ProjectActivity = ProjectActivity(),
    val dayBaselines: List<DayBaseline> = emptyList(),
    val startChecklist: StartChecklist = StartChecklist(),
    val exportRecords: List<ExportRecord> = emptyList(),
)

/** `_app/view.json`. */
@Serializable
data class ProjectViewFile(
    val lens: LensState = LensState(),
    val script: ScriptViewState = ScriptViewState(),
    /** Canvas key → viewport. */
    val canvasViewports: Map<Key, Viewport> = emptyMap(),
    /** Map key → viewport. */
    val mapViewports: Map<Key, Viewport> = emptyMap(),
    /** Remembered world tab. */
    val worldTab: String? = null,
    /** The canvas opened most recently (CNV-02 opens it); null = none yet or it is gone. Not written when null. */
    val lastCanvasKey: Key? = null,
    /**
     * Gallery screen state by owner key (the project key for the project gallery): pick, panel
     * expanded, row scroll (55 GAL-08, CNV-35). Not written while empty.
     */
    val galleries: Map<Key, GalleryViewState> = emptyMap(),
) {
    /** The saved state of the gallery of [ownerKey] (empty state when none). */
    fun galleryOf(ownerKey: Key): GalleryViewState = galleries[ownerKey] ?: GalleryViewState()

    /** [ownerKey]'s gallery state set to [state]; the empty state removes the entry. */
    fun withGallery(ownerKey: Key, state: GalleryViewState): ProjectViewFile =
        copy(galleries = if (state == GalleryViewState()) galleries - ownerKey else galleries + (ownerKey to state))
}

@Serializable
data class Viewport(val x: Float = 0f, val y: Float = 0f, val zoom: Float = 1f)

/** `이어 쓰기`, `마지막으로 쓰던 곳`, `열기 · 마지막 위치` (STO-01, SCR-59, LRC-33, LIB-02). */
@Serializable
data class ProjectActivity(
    val lastEditedBlockKey: Key? = null,
    val lastEditedOffset: Int? = null,
    val lastEditedAt: Timestamp? = null,
    val lastOpenedAt: Timestamp? = null,
    val lastRoute: String? = null,
)

/** Length of a block at the start of a local day, for `오늘 쓴 글자` (STO-06). */
@Serializable
data class DayBaseline(
    val localDate: IsoDate,
    val blockKey: Key,
    val lengthAtDayStart: Int,
)

/** Lens value; [savedForScript] is set by `렌즈를 이 씬으로 맞추기` (STO-13/16). */
@Serializable
data class LensState(
    val point: LensPoint = LensPoint.Latest,
    val savedForScript: LensPoint? = null,
)

/** `시작해 볼 것` card (LIB-13); items complete by fact. */
@Serializable
data class StartChecklist(val dismissed: Boolean = false)

/** UI history of exports (CSV inputs live in `VoiceState.lastExport`). */
@Serializable
data class ExportRecord(
    val exportedAt: Timestamp,
    val format: ExportFormat,
    val rangeKeys: List<Key> = emptyList(),
)

@Serializable(with = ExportFormat.Codec::class)
enum class ExportFormat {
    PDF, CSV, ENGINE_JSON, PROJECT_FILE;

    object Codec : LowerCamelEnumSerializer<ExportFormat>("ExportFormat", entries, PROJECT_FILE)
}

/** One undo step; the content is opaque and owned by each area. */
@Serializable
data class UndoStep(
    val key: Key,
    val area: String,
    val createdAt: Timestamp,
    val payload: JsonElement,
)
