package io.github.lenticualrstone.civilight.domain.library.export

import io.github.lenticualrstone.civilight.domain.library.crypto.PackageCrypto
import io.github.lenticualrstone.civilight.domain.library.crypto.RecoveryKey
import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.domain.library.format.ProjectFileOptions
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageWriter
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.ExportOptions
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import java.security.SecureRandom

/** A finished `.civilight` file. */
class ProjectFile(val fileName: String, val bytes: ByteArray, val encrypted: Boolean, val recorderRecords: Int, val missingBlobs: List<String>)

/**
 * `프로젝트 파일 .civilight` export (LIB-47) and the per-project part of the Drive backup (LIB-57):
 * latest format, encrypted with the `암호` when the switch is on (export lock), or with the 복구 암호
 * key (recovery lock). Voice IDs are never fixed.
 */
object ProjectFileExport {
    /** Sheet export. [password] is required when `options.encrypt` is on. */
    fun export(
        doc: ProjectDoc,
        options: ExportOptions,
        password: CharArray?,
        recorder: RecorderLibrary,
        blobs: BlobReader,
        writtenBy: String,
        now: Timestamp,
        iterations: Int = PackageCrypto.DEFAULT_ITERATIONS,
        random: SecureRandom = SecureRandom(),
        preview: ByteArray? = null,
    ): ProjectFile {
        require(options.format == ExportFormat.PROJECT_FILE) { "not a project file export" }
        require(!options.encrypt || (password != null && password.isNotEmpty())) { "password required" }
        val content = ProjectPackageWriter.content(
            doc, recorder, blobs,
            ProjectFileOptions(
                includeTrash = options.includeTrash,
                includeRecorderAudio = options.includeRecorderAudio,
                encrypted = options.encrypt,
                preview = preview,
            ),
            writtenBy, now,
        )
        val plain = content.toZip()
        val bytes = if (options.encrypt) PackageCrypto.encryptWithPassword(plain, password!!, iterations, random, now) else plain
        return ProjectFile(fileName(options.fileName), bytes, options.encrypt, content.recorderRecordKeys.size, content.missingBlobs)
    }

    /** Backup file of one project; [recovery] non-null locks it with the 복구 암호 key (LIB-57). */
    fun backup(
        doc: ProjectDoc,
        recorder: RecorderLibrary,
        blobs: BlobReader,
        recovery: RecoveryKey?,
        writtenBy: String,
        now: Timestamp,
        random: SecureRandom = SecureRandom(),
        preview: ByteArray? = null,
    ): ProjectFile {
        val content = ProjectPackageWriter.content(
            doc, recorder, blobs,
            // Recorder audio travels once, in the library backup (`.civilight-recorder`).
            ProjectFileOptions(encrypted = recovery != null, preview = preview, includeRecorderAudio = false),
            writtenBy, now,
        )
        val plain = content.toZip()
        val bytes = if (recovery != null) PackageCrypto.encryptWithRecoveryKey(plain, recovery, random, now) else plain
        return ProjectFile(fileName(doc.key), bytes, recovery != null, content.recorderRecordKeys.size, content.missingBlobs)
    }

    /** `<name>.civilight`. */
    fun fileName(base: String): String = if (base.endsWith(".civilight")) base else "$base.civilight"
}
