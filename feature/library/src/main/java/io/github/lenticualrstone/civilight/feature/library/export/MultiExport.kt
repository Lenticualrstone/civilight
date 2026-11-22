package io.github.lenticualrstone.civilight.feature.library.export

import io.github.lenticualrstone.civilight.domain.library.export.ExportDefaults
import io.github.lenticualrstone.civilight.domain.library.export.ProjectFileExport
import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import java.time.ZoneId

/** One project of the 여러 프로젝트 checklist (LIB-72). */
data class MultiExportRow(val key: Key, val title: String, val badge: String, val checked: Boolean, val locked: Boolean)

/** One file to write: its name (unique in the batch) and whether it is locked with an `암호`. */
data class MultiExportFile(val key: Key, val fileName: String, val encrypt: Boolean)

/**
 * 여러 프로젝트 한꺼번에 내보내기 (LIB-72, P2; §6 Q23 `ED`): only `프로젝트 파일 .civilight`, one file per
 * project (never a combined file), each with its own LIB-47 defaults (file name, `암호` on for a hidden,
 * R-18 or hidden-item project). The caller lists only what the 서재 shows: visible projects, and the
 * hidden ones only while the group is open; never the example or a trashed project.
 */
object MultiExport {
    fun rows(docs: List<ProjectDoc>, picked: Set<Key>): List<MultiExportRow> = docs.map { doc ->
        MultiExportRow(doc.key, doc.project.name, RatingLabels.badge(doc.project.rating), doc.key in picked, ExportDefaults.encryptDefault(doc))
    }

    fun toggle(picked: Set<Key>, key: Key): Set<Key> = if (key in picked) picked - key else picked + key

    /** Files in checklist order; equal names get ` (2)`, ` (3)` … so none overwrites another. */
    fun files(docs: List<ProjectDoc>, picked: Set<Key>, now: Timestamp, zone: ZoneId): List<MultiExportFile> {
        val used = HashSet<String>()
        return docs.filter { it.key in picked }.map { doc ->
            val base = ExportDefaults.defaultFileName(doc, now, zone)
            var name = base
            var n = 2
            while (!used.add(name)) name = "$base (${n++})"
            MultiExportFile(doc.key, ProjectFileExport.fileName(name), ExportDefaults.encryptDefault(doc))
        }
    }

    /** The bytes of one file (LIB-47 defaults: no audio, no trash), locked with [password] when given. */
    fun build(
        doc: ProjectDoc,
        file: MultiExportFile,
        password: CharArray?,
        recorder: RecorderLibrary,
        blobs: BlobReader,
        writtenBy: String,
        now: Timestamp,
        zone: ZoneId,
        preview: PreviewRenderer? = null,
    ): ByteArray {
        val options = ExportDefaults.initial(doc, ExportFormat.PROJECT_FILE, now, zone).copy(fileName = file.fileName, encrypt = file.encrypt)
        val cover = if (doc.project.protections.hidden) null else preview?.let { runCatching { it.render(doc) }.getOrNull() }
        return ProjectFileExport.export(doc, options, password, recorder, blobs, writtenBy, now, preview = cover).bytes
    }
}
