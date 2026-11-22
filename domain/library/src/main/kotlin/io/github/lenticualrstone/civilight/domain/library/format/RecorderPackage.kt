package io.github.lenticualrstone.civilight.domain.library.format

import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.file.EncryptionHeader
import io.github.lenticualrstone.civilight.model.file.FileManifest
import io.github.lenticualrstone.civilight.model.file.ManifestFeatures
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.file.RecorderFile
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary

/** A read `.civilight-recorder` file. */
class RecorderPackageContent(val manifest: FileManifest, val library: RecorderLibrary, val audio: Map<String, ByteArray>)

sealed interface RecorderPackageOpen {
    data class NotAPackage(val reason: String) : RecorderPackageOpen

    /**
     * Locked with the 복구 암호 (hidden records in a Drive backup, §7.11); decrypt with
     * `PackageCrypto` and read the inner bytes again.
     */
    data class Encrypted(val header: EncryptionHeader) : RecorderPackageOpen

    data class Newer(val check: NewerFileCheck) : RecorderPackageOpen

    data class Damaged(val reason: String) : RecorderPackageOpen

    class Ready(val content: RecorderPackageContent) : RecorderPackageOpen
}

/**
 * The Life Recorder library file `.civilight-recorder` (LIB-48, §7.11): same container and version
 * rules as `.civilight`; `records.json` holds every record not in the trash with all links and
 * placements (each with its project key); audio by hash. Hidden records go in only when the file
 * will be encrypted with the 복구 암호 ([includeHidden], LRC Q16 default: left out otherwise).
 */
object RecorderPackage {
    fun write(
        library: RecorderLibrary,
        includeHidden: Boolean,
        includeAudio: Boolean,
        audio: (hash: String) -> ByteArray?,
        writtenBy: String,
        writtenAt: Timestamp,
    ): ByteArray {
        val kept = library.copy(records = library.records.filter { it.deletedAt == null && (includeHidden || !it.hidden) })
        val keys = kept.records.map { it.key }.toSet()
        val slice = RecorderLibrary(
            kept.records.map { if (includeAudio) it else it.copy(audio = null) },
            kept.links.filter { it.recordKey in keys },
            kept.placements.filter { it.recordKey in keys },
        )
        val files = LinkedHashMap<String, ByteArray>()
        files[PackagePaths.RECORDER_LIBRARY] = PackageWriter.jsonBytes(
            CivilightJson.encodeElement(RecorderFile.serializer(), RecorderFile.library(slice, includeTrashed = false)),
        )
        if (includeAudio) {
            for (record in slice.records) {
                val a = record.audio ?: continue
                val bytes = audio(a.hash) ?: continue
                files[PackagePaths.asset(a.hash, ProjectPackageWriter.audioExt(a.mime))] = bytes
            }
        }
        val manifest = FileManifest(
            format = FileManifest.FORMAT_RECORDER,
            formatVersion = FormatVersion.CURRENT.toString(),
            minReaderVersion = ProjectPackageWriter.MIN_READER_VERSION,
            features = ManifestFeatures(required = emptyList(), optional = emptyList()),
            writtenBy = writtenBy,
            writtenAt = writtenAt,
        )
        return PackageWriter.zip(PackagePaths.MIME_RECORDER, manifest, files)
    }

    fun read(bytes: ByteArray, appMax: FormatVersion = FormatVersion.CURRENT): RecorderPackageOpen {
        val zip = try {
            ZipCodec.read(bytes)
        } catch (e: PackageZipException) {
            return RecorderPackageOpen.NotAPackage(e.message ?: "not a zip")
        }
        val first = zip.items.first()
        if (first.path != PackagePaths.MIMETYPE || first.bytes.toString(Charsets.US_ASCII).trim() != PackagePaths.MIME_RECORDER) {
            return RecorderPackageOpen.NotAPackage("not a recorder library file")
        }
        zip[PackagePaths.ENCRYPTION]?.let { headerBytes ->
            return try {
                RecorderPackageOpen.Encrypted(CivilightJson.decode(EncryptionHeader.serializer(), headerBytes.toString(Charsets.UTF_8)))
            } catch (e: IllegalArgumentException) {
                RecorderPackageOpen.Damaged("encryption header: ${e.message}")
            }
        }
        val manifest = try {
            CivilightJson.decode(FileManifest.serializer(), (zip[PackagePaths.MANIFEST] ?: return RecorderPackageOpen.Damaged("manifest.json missing")).toString(Charsets.UTF_8))
        } catch (e: IllegalArgumentException) {
            return RecorderPackageOpen.Damaged("manifest.json: ${e.message}")
        }
        val check = NewerFileCheck.of(manifest, appMax)
        if (check.refused) return RecorderPackageOpen.Newer(check)
        val recordsBytes = zip[PackagePaths.RECORDER_LIBRARY] ?: return RecorderPackageOpen.Damaged("records.json missing")
        if (manifest.files[PackagePaths.RECORDER_LIBRARY]?.let { it != Sha256.label(recordsBytes) } == true) {
            return RecorderPackageOpen.Damaged("records.json damaged")
        }
        val file = try {
            CivilightJson.decode(RecorderFile.serializer(), recordsBytes.toString(Charsets.UTF_8))
        } catch (e: IllegalArgumentException) {
            return RecorderPackageOpen.Damaged("records.json: ${e.message}")
        }
        val audio = zip.items.filter { ProjectPackageReader.isBlobPath(it.path) }
            .associate { it.path.removePrefix("assets/").substringBefore('.') to it.bytes }
            .filter { (hash, data) -> Sha256.hex(data) == hash }
        return RecorderPackageOpen.Ready(RecorderPackageContent(manifest, file.toLibrary(), audio))
    }

    /** Blob row for a recorder audio file (stored in the device blob store on import). */
    fun audioBlob(hash: String, mime: String, bytes: Long, durationMs: Long?, importedAt: Timestamp) = Blob(
        hash = hash, fileExt = ProjectPackageWriter.audioExt(mime), mime = mime, bytes = bytes, durationMs = durationMs,
        source = BlobSource.RECORDER.open(), importedAt = importedAt,
    )
}
