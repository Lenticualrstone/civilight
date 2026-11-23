package io.github.lenticualrstone.civilight.model.app

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.json.LowerCamelEnumSerializer
import io.github.lenticualrstone.civilight.model.project.Rating
import kotlinx.serialization.Serializable

/**
 * The project index (`files/library.json`, §8.2/§9): the 서재 list with the app-only project columns.
 * The portable key is unique among live and trashed rows (§6).
 */
@Serializable
data class LibraryIndexFile(
    val projects: List<LibraryEntry> = emptyList(),
    val storeVersion: Int = 1,
)

@Serializable
data class LibraryEntry(
    val key: Key,
    val name: String,
    val rating: OpenEnum<Rating> = Rating.ALL.open(),
    val hidden: Boolean = false,
    val isExample: Boolean = false,
    val readOnly: Boolean = false,
    val deletedAt: Timestamp? = null,
    val trashEntryKey: Key? = null,
    val lastOpenedAt: Timestamp? = null,
    val createdAt: Timestamp = 0L,
    val updatedAt: Timestamp = 0L,
)

/** Onboarding progress (class A, LIB). */
@Serializable
data class OnboardingState(
    val firstRunCompleted: Boolean = false,
    val resumePage: String? = null,
    val walkthroughSeen: Boolean = false,
    val exampleState: ExampleState = ExampleState.NOT_INSTALLED,
    /** Module id → seen. */
    val moduleTourSeen: Map<String, Boolean> = emptyMap(),
    val tipsShown: Set<String> = emptySet(),
    val emptyLibraryNoticeShown: Boolean = false,
)

@Serializable(with = ExampleState.Codec::class)
enum class ExampleState {
    NOT_INSTALLED, INSTALLED, DELETED;

    object Codec : LowerCamelEnumSerializer<ExampleState>("ExampleState", entries, NOT_INSTALLED)
}

/** A recently opened item (class A, LIB-05); at most [MAX] kept. */
@Serializable
data class RecentItem(
    val key: Key,
    val projectKey: Key,
    val kind: RecentKind,
    val targetKey: Key,
    val openedAt: Timestamp,
) {
    companion object {
        const val MAX = 20
    }
}

@Serializable(with = RecentKind.Codec::class)
enum class RecentKind {
    SCENE, CHARACTER, PAGE, TERM, PLACE, FACTION;

    object Codec : LowerCamelEnumSerializer<RecentKind>("RecentKind", entries, SCENE)
}

/** Drive backup run (class A cache, LIB-57…61). */
@Serializable
data class BackupRun(
    val runId: String,
    val startedAt: Timestamp,
    val finishedAt: Timestamp? = null,
    val ok: Boolean,
    val failureReason: String? = null,
    val entries: List<BackupEntry> = emptyList(),
    val totalBytes: Long = 0,
    val hiddenIncluded: Int = 0,
    val hiddenExcluded: Int = 0,
)

@Serializable
data class BackupEntry(
    val kind: BackupKind,
    val projectKey: Key? = null,
    val driveFileId: String,
    val versionId: String,
    val sizeBytes: Long,
    val encrypted: Boolean,
    val lockType: LockKind? = null,
    val hidden: Boolean = false,
    val hasHiddenItems: Boolean = false,
)

@Serializable(with = BackupKind.Codec::class)
enum class BackupKind {
    /** [TEMPLATES]: the app-level 인물 템플릿 file (CST-63); a hidden-templates entry has `hidden`. */
    PROJECT, RECORDER, SETTINGS, TEMPLATES;

    object Codec : LowerCamelEnumSerializer<BackupKind>("BackupKind", entries, PROJECT)
}

/** `암호` (export) or `복구 암호` (recovery). */
@Serializable(with = LockKind.Codec::class)
enum class LockKind {
    EXPORT, RECOVERY;

    object Codec : LowerCamelEnumSerializer<LockKind>("LockKind", entries, EXPORT)
}

@Serializable
data class BackupState(
    val lastSuccessAt: Timestamp? = null,
    val failureStreakStartedAt: Timestamp? = null,
    val lastFailureReason: String? = null,
)

/** Recovery-key material (class A, LIB-58); never the password. Byte values are base64. */
@Serializable
data class RecoveryKeyMaterial(
    val kdf: String,
    val iterations: Int,
    val salt: String,
    val dataKeyWrappedByPassword: String,
    val dataKeyWrappedByKeystore: String,
    val keystoreAlias: String,
    val setAt: Timestamp,
)

/**
 * A file of an imported package this app did not understand (class A, §8.3), written back with the
 * same path on re-export. [bytesBase64] holds the content.
 */
@Serializable
data class UnknownFile(
    val path: String,
    val bytesBase64: String,
    val sha256: String,
)

/** Runtime unlock state (RT, never persisted). Map keys are project keys. */
data class UnlockSession(
    val appUnlocked: Boolean = false,
    val hiddenGroupUnlockedAt: Long? = null,
    val itemsUnlocked: Map<Key, Long> = emptyMap(),
    val recordsUnlockedAt: Long? = null,
    val lastBackgroundAt: Long? = null,
    val lastScreenOffAt: Long? = null,
)

/** The export sheet's transient choices (RT, LIB-41). */
data class ExportOptions(
    val format: ExportFormat = ExportFormat.PDF,
    val rangeKeys: List<Key> = emptyList(),
    val includeHiddenScenes: Boolean = false,
    val layers: Set<String> = setOf("direction", "paren"),
    val includeSecretFields: Boolean = false,
    val fileName: String = "",
    val pdfPassword: String? = null,
    val encrypt: Boolean = false,
    val password: String? = null,
    val includeRecorderAudio: Boolean = false,
    val includeTrash: Boolean = false,
    /** (0.2.0, LIB-97) `.civilight` export row `TODO_COPY_LIB_EXPORT_VERSIONS`: write `versions/`; default on. */
    val includeVersions: Boolean = true,
)
