package io.github.lenticualrstone.civilight.model.recorder

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.json.LowerCamelEnumSerializer
import io.github.lenticualrstone.civilight.model.text.TextRun
import kotlinx.serialization.Serializable

/** Recorder settings group of `AppSettings` (class S). */
@Serializable
data class RecorderSettings(
    val transcription: RecorderTranscription = RecorderTranscription.ON_DEVICE,
    val lockScreenRecording: Boolean = true,
    val keepAudio: Boolean = true,
    /** `보던 곳 연결`. */
    val presetLink: Boolean = true,
)

/** App-settings copy of [TranscriptionMode] (closed enum: settings are not file content). */
@Serializable(with = RecorderTranscription.Codec::class)
enum class RecorderTranscription {
    ON_DEVICE, CLOUD;

    object Codec : LowerCamelEnumSerializer<RecorderTranscription>("RecorderTranscription", entries, ON_DEVICE)
}

/** Where a capture started (runtime; part of the crash-recovery file). */
@Serializable
data class EntryContext(
    val surface: EntrySurface,
    val projectKey: Key? = null,
    val targetKind: RecorderTargetKind? = null,
    val targetKey: Key? = null,
    /** Script only: block holding the caret (for `여기 넣기`). */
    val caretBlockKey: Key? = null,
    val isExample: Boolean = false,
    val onLockScreen: Boolean = false,
)

@Serializable(with = EntrySurface.Codec::class)
enum class EntrySurface {
    SCRIPT, CHARACTER, PLACE, TERM, FACTION, PAGE, PROJECT_HOME, STORY, TIMELINE, MAP, OTHER_IN_PROJECT,
    LIBRARY, HOME_WIDGET, QS_TILE, LOCK_SCREEN, COVER_WIDGET, INBOX;

    object Codec : LowerCamelEnumSerializer<EntrySurface>("EntrySurface", entries, OTHER_IN_PROJECT)
}

/** A recording in progress, persisted for crash recovery only (RT). */
@Serializable
data class RecordingSession(
    val recordKey: Key,
    val entry: EntryContext,
    val phase: RecordingPhase = RecordingPhase.RECORDING,
    val elapsedMs: Long = 0,
    val confirmedRuns: List<TextRun> = emptyList(),
    val partialText: String = "",
    val pendingLinks: List<RecorderLink> = emptyList(),
    /** LRC-42 row value; null when the row is not shown. */
    val hideSwitch: Boolean? = null,
    val audioTempPath: String,
)

@Serializable(with = RecordingPhase.Codec::class)
enum class RecordingPhase {
    RECORDING, PAUSED, PROCESSING;

    object Codec : LowerCamelEnumSerializer<RecordingPhase>("RecordingPhase", entries, PAUSED)
}

/** The one text memo draft (class A). */
@Serializable
data class TextMemoDraft(
    val runs: List<TextRun> = emptyList(),
    val pendingLinks: List<RecorderLink> = emptyList(),
    val entry: EntryContext,
    val updatedAt: Long,
)

/** Inbox filter state for one app session (RT). Runtime only: never written to a file. */
data class InboxFilterState(
    val status: StatusFilter = StatusFilter.ALL,
    val kind: KindFilter = KindFilter.NONE,
    val project: ProjectFilter = ProjectFilter.All,
    val target: InboxTarget? = null,
)

/**
 * The target of the inbox target filter. Item keys are unique only inside a project (§1.1) and
 * repeat across projects after a `새 프로젝트로` import, so the filter carries the project key. For a
 * project target [key] equals [projectKey].
 */
data class InboxTarget(val projectKey: Key, val kind: RecorderTargetKind, val key: Key)

enum class StatusFilter { ALL, UNSORTED, PUT, DONE }

enum class KindFilter { NONE, VOICE, TEXT }

sealed interface ProjectFilter {
    data object All : ProjectFilter
    data class Project(val projectKey: Key) : ProjectFilter
    data object NoLink : ProjectFilter
}
