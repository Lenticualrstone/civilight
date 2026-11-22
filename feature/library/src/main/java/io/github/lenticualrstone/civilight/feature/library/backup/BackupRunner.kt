package io.github.lenticualrstone.civilight.feature.library.backup

import io.github.lenticualrstone.civilight.domain.library.backup.BackupAction
import io.github.lenticualrstone.civilight.domain.library.backup.BackupCandidate
import io.github.lenticualrstone.civilight.domain.library.backup.BackupFailure
import io.github.lenticualrstone.civilight.domain.library.backup.BackupPlanner
import io.github.lenticualrstone.civilight.domain.library.crypto.PackageCrypto
import io.github.lenticualrstone.civilight.domain.library.crypto.RecoveryKey
import io.github.lenticualrstone.civilight.domain.library.export.ProjectFileExport
import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.domain.library.format.RecorderPackage
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.BackupEntry
import io.github.lenticualrstone.civilight.model.app.BackupKind
import io.github.lenticualrstone.civilight.model.app.BackupRun
import io.github.lenticualrstone.civilight.model.app.BackupState
import io.github.lenticualrstone.civilight.model.app.LockKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.feature.library.export.PreviewRenderer
import java.security.SecureRandom

/**
 * A live project and when its content last changed: the 서재 index time, which every content edit
 * moves (`Project.updatedAt` is not stamped by the store, so it cannot tell an edited project).
 */
data class BackupProject(val doc: ProjectDoc, val updatedAt: Timestamp)

/** What a backup run reads from the device (the app: the stores; tests: memory). */
interface BackupSources {
    /** Live projects of the 서재 that are not the example, with their documents. */
    suspend fun projects(): List<BackupProject>

    fun blobs(): BlobReader

    suspend fun recorder(): RecorderLibrary

    /** Recorder audio by hash (original audio goes only into the library file). */
    fun audio(hash: String): ByteArray?

    suspend fun settings(): AppSettings
}

/** How a run ended. */
sealed interface BackupOutcome {
    /** `Google Drive 자동 백업` is off, or no account is connected: nothing ran. */
    data object NotRunning : BackupOutcome

    data class Done(val run: BackupRun) : BackupOutcome

    /** Drive failed; the book's failure streak moved (LIB-61 banner). */
    data class Failed(val run: BackupRun, val reason: BackupFailure) : BackupOutcome
}

/**
 * One daily Drive backup (LIB-57): `domain:library` [BackupPlanner] decides (hidden projects locked
 * with the 복구 암호 or left out, projects with hidden items locked when a 복구 암호 exists, unchanged
 * projects reused, example and trashed ones skipped); each upload is [ProjectFileExport.backup]; the
 * Life Recorder library goes up as one `.civilight-recorder` ([RecorderPackage], LIB-48) and the app
 * settings as one JSON file. Versions no run of the last 30 days references are deleted (B-07). The
 * run, the content times and the failure streak go into the [BackupBooks]; the run index goes to
 * Drive so a new device can list runs (LIB-59).
 */
class BackupRunner(
    private val sources: BackupSources,
    private val drive: DriveGateway,
    private val book: BackupBooks,
    private val recoveryKey: suspend () -> RecoveryKey?,
    private val writtenBy: String,
    private val random: SecureRandom = SecureRandom(),
    /** `preview.webp` of each uploaded visible project (LIB-49); none in tests. */
    private val preview: PreviewRenderer? = null,
) {
    suspend fun run(now: Timestamp, runId: String): BackupOutcome {
        val settings = sources.settings()
        if (!settings.driveBackup || drive.account() == null) return BackupOutcome.NotRunning
        book.ensureLoaded()
        val recovery = recoveryKey()
        return try {
            val done = upload(settings, recovery, now, runId)
            finish(done, now)
            BackupOutcome.Done(done.run)
        } catch (e: DriveException) {
            val run = BackupRun(runId, startedAt = now, finishedAt = now, ok = false, failureReason = e.reason.code)
            book.update { b ->
                b.copy(
                    runs = b.runs + run,
                    state = b.state.copy(
                        failureStreakStartedAt = b.state.failureStreakStartedAt ?: now,
                        lastFailureReason = e.reason.code,
                    ),
                )
            }
            BackupOutcome.Failed(run, e.reason)
        }
    }

    private suspend fun upload(settings: AppSettings, recovery: RecoveryKey?, now: Timestamp, runId: String): Uploaded {
        val projects = sources.projects()
        val byKey = projects.associateBy { it.doc.key }
        val plan = BackupPlanner.plan(projects.map { BackupCandidate.of(it.doc, updatedAt = it.updatedAt) }, settings, recovery != null, book.state.value.lastUploads())
        val recorder = sources.recorder()
        val blobs = sources.blobs()
        val entries = ArrayList<BackupEntry>()
        val uploaded = HashMap<Key, Timestamp>()
        for (item in plan.items) {
            val project = byKey[item.projectKey] ?: continue
            val doc = project.doc
            when (item.action) {
                BackupAction.SKIPPED, BackupAction.EXCLUDED_HIDDEN -> Unit
                BackupAction.REUSE -> item.previous?.let { entries += it }
                BackupAction.UPLOAD -> {
                    val lock = if (item.lock == LockKind.RECOVERY) recovery else null
                    val cover = if (doc.project.protections.hidden) null else preview?.let { runCatching { it.render(doc) }.getOrNull() }
                    val file = ProjectFileExport.backup(doc, recorder, blobs, lock, writtenBy, now, random, cover)
                    val version = drive.upload(file.fileName, PackagePaths.MIME_PROJECT, file.bytes)
                    entries += BackupEntry(
                        kind = BackupKind.PROJECT,
                        projectKey = doc.key,
                        driveFileId = version.fileId,
                        versionId = version.versionId,
                        sizeBytes = version.sizeBytes,
                        encrypted = file.encrypted,
                        lockType = item.lock,
                        hidden = doc.project.protections.hidden,
                        hasHiddenItems = BackupCandidate.of(doc).hiddenItemCount > 0,
                    )
                    uploaded[doc.key] = project.updatedAt
                }
            }
        }
        entries += uploadRecorder(recorder, settings, recovery, now)
        entries += uploadSettings(settings)
        val run = BackupRun(
            runId = runId,
            startedAt = now,
            finishedAt = now,
            ok = true,
            entries = entries,
            totalBytes = entries.sumOf { it.sizeBytes },
            hiddenIncluded = entries.count { it.kind == BackupKind.PROJECT && it.hidden },
            hiddenExcluded = plan.hiddenExcluded,
        )
        return Uploaded(run, uploaded)
    }

    /** A finished upload and the content time of each project version it uploaded. */
    private class Uploaded(val run: BackupRun, val content: Map<Key, Timestamp>)

    /**
     * The library file (LIB-48): hidden records only when a 복구 암호 locks the file and `숨긴 것도
     * 백업` is on; original audio per `원본 음성 보관`.
     */
    private suspend fun uploadRecorder(library: RecorderLibrary, settings: AppSettings, recovery: RecoveryKey?, now: Timestamp): BackupEntry {
        val withHidden = recovery != null && settings.backupHidden && library.records.any { it.hidden && it.deletedAt == null }
        val inner = RecorderPackage.write(library, withHidden, settings.recorder.keepAudio, sources::audio, writtenBy, now)
        val bytes = if (withHidden) PackageCrypto.encryptWithRecoveryKey(inner, recovery!!, random, now, PackagePaths.MIME_RECORDER) else inner
        val version = drive.upload(RECORDER_FILE, PackagePaths.MIME_RECORDER, bytes)
        return BackupEntry(
            kind = BackupKind.RECORDER, driveFileId = version.fileId, versionId = version.versionId, sizeBytes = version.sizeBytes,
            encrypted = withHidden, lockType = if (withHidden) LockKind.RECOVERY else null, hidden = withHidden,
        )
    }

    /** App settings travel as plain JSON (no project content; the Drive account itself is left out). */
    private suspend fun uploadSettings(settings: AppSettings): BackupEntry {
        val bytes = CivilightJson.encode(AppSettings.serializer(), settings.copy(driveAccountId = null)).toByteArray()
        val version = drive.upload(SETTINGS_FILE, JSON_MIME, bytes)
        return BackupEntry(BackupKind.SETTINGS, driveFileId = version.fileId, versionId = version.versionId, sizeBytes = version.sizeBytes, encrypted = false)
    }

    /** Book, prune, then the run index on Drive. */
    private suspend fun finish(done: Uploaded, now: Timestamp) {
        val all = book.state.value.runs + done.run
        val stale = BackupPlanner.prune(all, now)
        for (entry in stale) runCatching { drive.delete(entry.driveFileId, entry.versionId) }
        val kept = all.filter { it.startedAt >= now - BackupPlanner.RETENTION_MS }
        val next = book.update { b ->
            b.copy(
                runs = kept,
                state = BackupState(lastSuccessAt = now),
                uploadedContent = b.uploadedContent + done.content,
            )
        }
        runCatching { drive.writeIndex(DriveRunIndex(next.runs.filter { it.ok }, namesOf(next.runs))) }
    }

    private suspend fun namesOf(runs: List<BackupRun>): Map<Key, String> {
        val keys = runs.flatMap { it.entries }.filter { it.kind == BackupKind.PROJECT && !it.hidden }.mapNotNull { it.projectKey }.toSet()
        val previous = runCatching { drive.readIndex() }.getOrNull()?.projectNames.orEmpty()
        val current = sources.projects().map { it.doc }.filter { !it.project.protections.hidden }.associate { it.key to it.project.name }
        return keys.mapNotNull { key -> (current[key] ?: previous[key])?.let { key to it } }.toMap()
    }

    companion object {
        const val RECORDER_FILE = "library.civilight-recorder"
        const val SETTINGS_FILE = "settings.json"
        const val JSON_MIME = "application/json"
    }
}
