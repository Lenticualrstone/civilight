package io.github.lenticualrstone.civilight.feature.library.backup

import io.github.lenticualrstone.civilight.domain.library.backup.BackupPlanner
import io.github.lenticualrstone.civilight.feature.library.common.ProtectionFacts
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.BackupEntry
import io.github.lenticualrstone.civilight.model.app.BackupKind
import io.github.lenticualrstone.civilight.model.app.BackupRun
import io.github.lenticualrstone.civilight.model.app.BackupState
import io.github.lenticualrstone.civilight.model.app.RecoveryKeyMaterial
import io.github.lenticualrstone.civilight.store.StoreContainer
import io.github.lenticualrstone.civilight.store.io.JsonFile
import io.github.lenticualrstone.civilight.store.io.StoreDirectory
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import java.io.File

/**
 * The device's backup bookkeeping (class A, never exported): the Drive runs of the last 30 days, the
 * failure streak, the 복구 암호 key material (LIB-58, never the password) and, per project, the
 * content time of its last uploaded version (so an unchanged project reuses it, B-04).
 *
 * `ED`: core:store has no slot for `BackupRun` / `BackupState` / `RecoveryKeyMaterial`, so this lane
 * keeps them in `files/app/backup.json` through core:store's own [JsonFile] (Request: move it into
 * `AppStateStore`).
 */
@Serializable
data class BackupBook(
    val runs: List<BackupRun> = emptyList(),
    val state: BackupState = BackupState(),
    val recovery: RecoveryKeyMaterial? = null,
    /** Project key → `updatedAt` of the content its last uploaded version holds. */
    val uploadedContent: Map<Key, Timestamp> = emptyMap(),
) {
    /** The newest run, ok or not. */
    val lastRun: BackupRun? get() = runs.maxByOrNull { it.startedAt }

    /** For [BackupPlanner.plan]: the last uploaded version of each project and its content time. */
    fun lastUploads(): Map<Key, Pair<Timestamp, BackupEntry>> {
        val out = HashMap<Key, Pair<Timestamp, BackupEntry>>()
        for (run in runs.filter { it.ok }.sortedBy { it.startedAt }) {
            for (entry in run.entries) {
                val key = entry.projectKey ?: continue
                if (entry.kind != BackupKind.PROJECT) continue
                val at = uploadedContent[key] ?: continue
                out[key] = at to entry
            }
        }
        return out
    }

    /** Distinct unencrypted versions of [projectKey] still kept on Drive (LIB-24 `이전 백업 n · 암호 없음`). */
    fun unencryptedVersions(projectKey: Key): Int = runs
        .flatMap { it.entries }
        .filter { it.kind == BackupKind.PROJECT && it.projectKey == projectKey && !it.encrypted }
        .distinctBy { it.driveFileId to it.versionId }
        .size

    /** The unencrypted versions of [projectKey] kept on Drive (LIB-24 `지우기 ›`). */
    fun unencryptedEntries(projectKey: Key): List<BackupEntry> = runs
        .flatMap { it.entries }
        .filter { it.kind == BackupKind.PROJECT && it.projectKey == projectKey && !it.encrypted }
        .distinctBy { it.driveFileId to it.versionId }

    /**
     * After those versions were deleted from Drive: they leave every run, and the project uploads a
     * fresh version (now locked, as the new rating asks) on the next run.
     */
    fun withoutUnencrypted(projectKey: Key): BackupBook = copy(
        runs = runs.map { run -> run.copy(entries = run.entries.filterNot { it.kind == BackupKind.PROJECT && it.projectKey == projectKey && !it.encrypted }) },
        uploadedContent = uploadedContent - projectKey,
    )

    /** Runs started within the retention window, newest first (LIB-59 `백업 · 30일`). */
    fun recentRuns(now: Timestamp): List<BackupRun> =
        runs.filter { it.startedAt >= now - BackupPlanner.RETENTION_MS }.sortedByDescending { it.startedAt }
}

/** Where the [BackupBook] lives (the app: [BackupBookStore]; tests: in memory). */
interface BackupBooks {
    val state: StateFlow<BackupBook>

    suspend fun ensureLoaded()

    suspend fun update(transform: (BackupBook) -> BackupBook): BackupBook

    /** Writes now (after the 복구 암호 changed: a lost write would strand future backups). */
    suspend fun flush()
}

/** [BackupBook] on disk: `files/app/backup.json`, debounced atomic saves like every app file. */
class BackupBookStore(stores: StoreContainer) : BackupBooks {
    private val file = JsonFile(StoreDirectory(File(stores.filesDir, APP_DIR)), FILE, BackupBook.serializer(), BackupBook(), stores.env)

    override val state: StateFlow<BackupBook> get() = file.state

    override suspend fun ensureLoaded() = file.ensureLoaded()

    override suspend fun update(transform: (BackupBook) -> BackupBook): BackupBook = file.update(transform)

    override suspend fun flush() = file.flush()

    private companion object {
        const val APP_DIR = "app"
        const val FILE = "backup.json"
    }
}

/**
 * [ProtectionFacts] from the backup book (replaces batch 1's `NoBackupYet`). Read synchronously from
 * the loaded state: callers load it first ([BackupBookStore.ensureLoaded] runs when the 서재, 설정 and
 * 새 프로젝트 open).
 */
class BackupFacts(private val book: StateFlow<BackupBook>) : ProtectionFacts {
    override fun recoverySet(): Boolean = book.value.recovery != null

    override fun unencryptedBackups(projectKey: Key): Int = book.value.unencryptedVersions(projectKey)
}
