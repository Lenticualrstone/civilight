package io.github.lenticualrstone.civilight.feature.library.backup

import io.github.lenticualrstone.civilight.domain.library.backup.RestoreProgress
import io.github.lenticualrstone.civilight.domain.library.crypto.DecryptResult
import io.github.lenticualrstone.civilight.domain.library.crypto.EncryptedFiles
import io.github.lenticualrstone.civilight.domain.library.crypto.EncryptedOpen
import io.github.lenticualrstone.civilight.domain.library.crypto.PackageCrypto
import io.github.lenticualrstone.civilight.domain.library.format.PackageOpen
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageReader
import io.github.lenticualrstone.civilight.domain.library.format.RecorderPackage
import io.github.lenticualrstone.civilight.domain.library.format.RecorderPackageContent
import io.github.lenticualrstone.civilight.domain.library.format.RecorderPackageOpen
import io.github.lenticualrstone.civilight.domain.library.importing.OpenDecision
import io.github.lenticualrstone.civilight.domain.library.importing.OpenDecisions
import io.github.lenticualrstone.civilight.domain.library.importing.RecorderMerge
import io.github.lenticualrstone.civilight.feature.library.openfile.ImportOutcome
import io.github.lenticualrstone.civilight.feature.library.openfile.ImportPorts
import io.github.lenticualrstone.civilight.feature.library.openfile.ProjectImporter
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.BackupEntry
import java.time.ZoneId

/** Where a restore stands (LIB-60): the board's bytes, counts and current step. */
data class RestoreStatus(
    val progress: RestoreProgress,
    /** 0 `원고 · 인물 · 세계`, 1 `에셋 · 음성 기록`, 2 `숨긴 프로젝트 <h>`; [stepCount] when done. */
    val step: Int,
    val dateLabel: String,
    /** Seconds left at the current rate (`약 <n>초`), null before the first bytes arrive. */
    val secondsLeft: Long?,
    val finished: Boolean = false,
) {
    val stepCount: Int get() = progress.steps.size
}

/** The end of a restore. */
data class RestoreResult(
    val imported: List<ImportOutcome>,
    /** Files that could not be opened (gone from Drive, damaged, newer, wrong key): left out. */
    val failed: Int,
    val recorderMerged: Boolean,
)

/** A wrong 복구 암호: nothing was imported. */
class WrongRecoveryPassword : Exception("wrong recovery password")

/**
 * 되살리기 of one Drive run (LIB-59, LIB-60): downloads each chosen project, opens it with the
 * 복구 암호 when locked (`lockType: recovery`, so it keeps its own hidden flag), imports it through
 * the same [ProjectImporter] as 파일에서 열기 (`새 프로젝트로` or `덮어쓰기`, match by project key,
 * §6 Q26), then merges the Life Recorder library by record key (R23 §6). Progress goes to [onStatus].
 */
class RestoreRunner(
    private val drive: DriveGateway,
    private val ports: ImportPorts,
    private val newKey: () -> Key,
) {
    /**
     * Checks [password] against the first locked file before anything is imported, so a wrong 복구
     * 암호 keeps the sheet with its error (LIB-54 `ED`). Returns the downloaded bytes for reuse.
     */
    suspend fun checkPassword(request: RestoreRequest, password: CharArray): Map<BackupEntry, ByteArray> {
        val locked = (request.projects + listOfNotNull(request.recorder) + request.templates).firstOrNull { it.encrypted } ?: return emptyMap()
        val bytes = drive.download(locked.driveFileId, locked.versionId) ?: return emptyMap()
        return when (PackageCrypto.decrypt(bytes, password)) {
            DecryptResult.WrongPassword -> throw WrongRecoveryPassword()
            else -> mapOf(locked to bytes)
        }
    }

    suspend fun run(
        request: RestoreRequest,
        password: CharArray?,
        now: Timestamp,
        zone: ZoneId,
        dateLabel: String,
        prefetched: Map<BackupEntry, ByteArray> = emptyMap(),
        clockMs: () -> Long = System::currentTimeMillis,
        onStatus: (RestoreStatus) -> Unit,
    ): RestoreResult {
        val total = request.totalBytes
        var done = prefetched.keys.sumOf { it.sizeBytes }
        var images = 0
        var audio = 0
        val started = clockMs()
        fun status(step: Int, inFlight: Long = 0, finished: Boolean = false): RestoreStatus {
            val bytes = (done + inFlight).coerceAtMost(total)
            val elapsed = clockMs() - started
            val left = if (bytes > 0 && elapsed > 0) (total - bytes) * elapsed / bytes / 1000 else null
            return RestoreStatus(RestoreProgress(bytes, total, request.projects.size, images, audio, request.hiddenCount), step, dateLabel, left, finished)
        }
        onStatus(status(0))

        val imported = ArrayList<ImportOutcome>()
        var failed = 0
        for (entry in request.projects.sortedBy { it.hidden }) {
            val bytes = prefetched[entry] ?: drive.download(entry.driveFileId, entry.versionId) { onStatus(status(0, it)) }
            done += entry.sizeBytes
            val opened = bytes?.let { open(it, entry, password) }
            if (opened !is PackageOpen.Ready) {
                failed++
            } else {
                val pkg = opened.pkg
                val decision = OpenDecisions.decide(pkg, ports.libraryEntries())
                if (decision is OpenDecision.RefuseNewer) {
                    failed++
                } else {
                    imported += ProjectImporter(ports).import(pkg, decision, request.choice, now, zone, newKey)
                    images += pkg.doc.assets.blobs.count { it.mime.startsWith(IMAGE) }
                }
            }
            onStatus(status(0))
        }

        var merged = false
        request.recorder?.let { entry ->
            onStatus(status(1))
            val bytes = prefetched[entry] ?: drive.download(entry.driveFileId, entry.versionId) { onStatus(status(1, it)) }
            done += entry.sizeBytes
            val content = bytes?.let { openRecorder(it, password) }
            if (content == null) {
                failed++
            } else {
                for ((hash, data) in content.audio) {
                    val record = content.library.records.firstOrNull { it.audio?.hash == hash } ?: continue
                    ports.importBlob(hash, data, record.audio!!.mime, hash)
                    audio++
                }
                ports.updateRecorder { RecorderMerge.merge(it, content.library, newKey) }
                merged = true
            }
        }
        // 인물 템플릿 (CST-63): merged by key; a newer `updatedAt` wins and nothing local is deleted.
        for (entry in request.templates) {
            val bytes = prefetched[entry] ?: drive.download(entry.driveFileId, entry.versionId)
            done += entry.sizeBytes
            val incoming = bytes?.let { TemplateBackup.decode(it, entry.encrypted, password) }
            if (incoming == null) failed++ else ports.updateTemplates { TemplateBackup.merge(it, incoming) }
        }
        onStatus(status(if (request.hiddenCount > 0) 2 else 1))
        val last = status(0)
        onStatus(status(last.stepCount, finished = true))
        return RestoreResult(imported, failed, merged)
    }

    private fun open(bytes: ByteArray, entry: BackupEntry, password: CharArray?): PackageOpen? {
        if (!entry.encrypted) return ProjectPackageReader.open(bytes)
        val pw = password ?: return null
        return when (val r = EncryptedFiles.open(bytes, pw)) {
            is EncryptedOpen.Opened -> r.result
            else -> null
        }
    }

    private fun openRecorder(bytes: ByteArray, password: CharArray?): RecorderPackageContent? {
        val first = RecorderPackage.read(bytes)
        val opened = if (first is RecorderPackageOpen.Encrypted) {
            val pw = password ?: return null
            val inner = (PackageCrypto.decrypt(bytes, pw) as? DecryptResult.Ok)?.inner ?: return null
            RecorderPackage.read(inner)
        } else {
            first
        }
        return (opened as? RecorderPackageOpen.Ready)?.content
    }

    private companion object {
        const val IMAGE = "image/"
    }
}
