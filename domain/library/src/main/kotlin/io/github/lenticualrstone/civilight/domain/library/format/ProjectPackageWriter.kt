package io.github.lenticualrstone.civilight.domain.library.format

import io.github.lenticualrstone.civilight.domain.library.common.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import io.github.lenticualrstone.civilight.model.file.AssetsIndexFile
import io.github.lenticualrstone.civilight.model.file.ChecksFile
import io.github.lenticualrstone.civilight.model.file.DeletionsFile
import io.github.lenticualrstone.civilight.model.file.FileManifest
import io.github.lenticualrstone.civilight.model.file.Features
import io.github.lenticualrstone.civilight.model.file.ManifestFeatures
import io.github.lenticualrstone.civilight.model.file.ManifestProject
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.file.RecorderFile
import io.github.lenticualrstone.civilight.model.file.TombstonesFile
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.marks.Marks
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.trash.NameTag
import io.github.lenticualrstone.civilight.model.trash.NameTagKind
import io.github.lenticualrstone.civilight.model.trash.NameTagState
import kotlinx.serialization.json.JsonElement
import java.util.Base64

/** Reads the bytes of a content-addressed blob from the device store; null when missing. */
fun interface BlobReader {
    fun read(blob: Blob): ByteArray?

    companion object {
        val NONE = BlobReader { null }

        fun of(bytesByHash: Map<String, ByteArray>) = BlobReader { bytesByHash[it.hash] }
    }
}

/** Choices of a `.civilight` export that change the content (LIB-41, LIB-47). */
data class ProjectFileOptions(
    /** `휴지통도 포함` (default off). */
    val includeTrash: Boolean = false,
    /** `원본 음성 포함` (default off). */
    val includeRecorderAudio: Boolean = false,
    /** The package will be encrypted; with a hidden project this lets hidden linked records in. */
    val encrypted: Boolean = false,
    /** Library card cover (`preview.webp`); never written for a hidden project. */
    val preview: ByteArray? = null,
    /**
     * (0.2.0, LIB-97, 00 §13.5) Write `versions/` (episode versions). Default on; the manual export
     * sheet row may turn it off; backups always write them.
     */
    val includeVersions: Boolean = true,
)

/** First writer layer's result: the package files (without `mimetype` and the manifest) and the manifest. */
class ProjectPackageContent(
    /** Path → bytes in package order. */
    val files: Map<String, ByteArray>,
    /** Manifest without hashes; [PackageWriter] fills `files`. */
    val manifest: FileManifest,
    /** Recorder records written into `recorder.json`. */
    val recorderRecordKeys: List<Key>,
    /** Linked hidden records left out. */
    val excludedHiddenRecords: Int,
    /** Hashes of referenced blobs whose bytes the [BlobReader] could not supply. */
    val missingBlobs: List<String>,
) {
    /** The plain (unencrypted) package ZIP. */
    fun toZip(): ByteArray = PackageWriter.zip(PackagePaths.MIME_PROJECT, manifest, files)
}

/**
 * Maps one project to the `.civilight` file units (LIB-47, LIB-49, §5.6, §7): always the latest
 * format; never app-only state, derived values or the example flag.
 *
 * - Without `휴지통도 포함` non-live rows are left out and every trashed character · place · term ·
 *   faction · page · unit still referenced by exported data gets a `NameTag(notExported)` (§5.6).
 * - Only blobs referenced by an exported row are written (§7.8); recorder audio only with
 *   `원본 음성 포함`. Originals of written gallery items go to `gallery/files/<hash>.<ext>`, and also to
 *   `assets/` only when a non-gallery row uses them (55 §3.3); every written blob keeps its metadata
 *   row in `assets/index.json`.
 * - Unknown files kept from an earlier import are written back unchanged (§8.3).
 * - `features` are computed from the content (§7.3).
 */
object ProjectPackageWriter {
    const val MIN_READER_VERSION = "1.0"

    fun content(
        doc: ProjectDoc,
        recorder: RecorderLibrary = RecorderLibrary(),
        blobs: BlobReader = BlobReader.NONE,
        options: ProjectFileOptions = ProjectFileOptions(),
        writtenBy: String,
        writtenAt: Timestamp,
    ): ProjectPackageContent {
        val projectKey = doc.key
        val hidden = doc.project.protections.hidden
        val slice = RecorderSlice.select(recorder, projectKey, hidden, options.encrypted)
        val recorderFile = slice.toFile(recorder, projectKey, options.includeRecorderAudio)
        val recorderJson = if (recorderFile.records.isEmpty()) null else CivilightJson.encodeElement(RecorderFile.serializer(), recorderFile)

        val exportedDoc = if (options.includeTrash) doc else withoutTrashed020(withNotExportedNameTags(doc, recorderJson))
        val fileSet = ProjectFileSet.split(exportedDoc).let { if (options.includeTrash) it else it.copy(trash = null) }
        val json = LinkedHashMap(fileSet.encode())
        if (!options.includeVersions) json.keys.removeAll { PackagePaths.isVersionsUnit(it) }
        dropEmpty(json, PackagePaths.CHECKS, exportedDoc.checks == ChecksFile())
        dropEmpty(json, PackagePaths.TOMBSTONES, exportedDoc.tombstones == TombstonesFile())
        dropEmpty(json, PackagePaths.DELETIONS, exportedDoc.deletions == DeletionsFile())

        // Blobs: referenced by any written row (the blob catalogue itself does not count). Gallery
        // files are scanned apart: their originals go under `gallery/files/` (55 §3.3).
        val refs = HashSet<String>()
        val galleryRefs = HashSet<String>()
        for ((path, element) in json) {
            when {
                path == PackagePaths.ASSETS_INDEX || path == PackagePaths.trash(PackagePaths.ASSETS_INDEX) -> {
                    val assets = CivilightJson.decodeElement(AssetsIndexFile.serializer(), element)
                    JsonScan.strings(CivilightJson.encodeElement(AssetsIndexFile.serializer(), assets.copy(blobs = emptyList())), refs)
                }
                isGalleryJson(path) -> JsonScan.strings(element, galleryRefs)
                else -> JsonScan.strings(element, refs)
            }
        }
        val audioBlobs = if (options.includeRecorderAudio) {
            recorderFile.records.mapNotNull { r ->
                val audio = r.audio ?: return@mapNotNull null
                exportedDoc.assets.blobs.firstOrNull { it.hash == audio.hash } ?: Blob(
                    hash = audio.hash, fileExt = audioExt(audio.mime), mime = audio.mime, bytes = audio.bytes,
                    durationMs = audio.durationMs, source = BlobSource.RECORDER.open(), importedAt = r.createdAt,
                )
            }
        } else {
            emptyList()
        }
        val galleryBlobs = exportedDoc.assets.blobs.filter { it.hash in galleryRefs }
        val writtenBlobs = (exportedDoc.assets.blobs.filter { it.hash in refs } + audioBlobs).distinctBy { it.hash }
        val catalogue = (writtenBlobs + galleryBlobs).distinctBy { it.hash }
        json[PackagePaths.ASSETS_INDEX] = CivilightJson.encodeElement(
            AssetsIndexFile.serializer(),
            fileSet.live.assets.copy(blobs = catalogue),
        )

        val files = LinkedHashMap<String, ByteArray>()
        for ((path, element) in json) files[path] = PackageWriter.jsonBytes(element)
        if (recorderJson != null) files[PackagePaths.RECORDER] = PackageWriter.jsonBytes(recorderJson)
        val missing = ArrayList<String>()
        for (blob in writtenBlobs) {
            val bytes = blobs.read(blob)
            if (bytes == null) missing += blob.hash else files[PackagePaths.asset(blob.hash, blob.fileExt)] = bytes
        }
        for (blob in galleryBlobs) {
            val bytes = blobs.read(blob)
            if (bytes == null) {
                if (blob.hash !in missing) missing += blob.hash
            } else {
                files[PackagePaths.galleryOriginal(blob.hash, blob.fileExt)] = bytes
            }
        }
        if (options.preview != null && !hidden) files[PackagePaths.PREVIEW] = options.preview
        for ((path, unknown) in exportedDoc.unknownFiles) {
            if (path in files || path == PackagePaths.MIMETYPE || path == PackagePaths.MANIFEST) continue
            files[path] = Base64.getDecoder().decode(unknown.bytesBase64)
        }

        val optional = buildList {
            if (fileSet.live.project.fieldDefs.isNotEmpty()) add(Features.FIELDS_CUSTOM)
            if (exportedDoc.maps.values.any { it.generation != null }) add(Features.MAPS_GEN)
            if (PackagePaths.CHECKS in files) add(Features.CHECKS_ACK)
            if (PackagePaths.TOMBSTONES in files) add(Features.TOMBSTONES)
            if (PackagePaths.DELETIONS in files) add(Features.DELETIONS)
            if (options.includeTrash) add(Features.TRASH)
            if (recorderJson != null) add(Features.RECORDER)
            if (audioBlobs.isNotEmpty()) add(Features.RECORDER_AUDIO)
            if (json.keys.any(::isGalleryJson)) add(Features.GALLERY)
            if (json.keys.any(PackagePaths::isVersionsUnit)) add(Features.VERSIONS)
        }
        val manifest = FileManifest(
            format = FileManifest.FORMAT_PROJECT,
            formatVersion = FormatVersion.CURRENT.toString(),
            minReaderVersion = MIN_READER_VERSION,
            features = ManifestFeatures(required = Features.ALWAYS_REQUIRED, optional = optional),
            writtenBy = writtenBy,
            writtenAt = writtenAt,
            project = ManifestProject(projectKey, doc.project.name, doc.project.rating),
            encrypted = false,
        )
        return ProjectPackageContent(files, manifest, recorderFile.records.map { it.key }, slice.excludedHidden, missing)
    }

    /** The plain package ZIP of [doc]. */
    fun write(
        doc: ProjectDoc,
        recorder: RecorderLibrary = RecorderLibrary(),
        blobs: BlobReader = BlobReader.NONE,
        options: ProjectFileOptions = ProjectFileOptions(),
        writtenBy: String,
        writtenAt: Timestamp,
    ): ByteArray = content(doc, recorder, blobs, options, writtenBy, writtenAt).toZip()

    /**
     * [doc] with a `NameTag(state = notExported)` for each trashed character · place · term ·
     * faction · page · unit that the exported live files or [recorderJson] reference (§5.6).
     * Existing name tags are kept; history files (checks, tombstones, deletions) do not count as
     * references.
     */
    fun withNotExportedNameTags(doc: ProjectDoc, recorderJson: JsonElement? = null): ProjectDoc {
        val live = ProjectFileSet.split(doc).live.encode("")
        val refs = HashSet<String>()
        for ((path, element) in live) {
            if (path == PackagePaths.CHECKS || path == PackagePaths.TOMBSTONES || path == PackagePaths.DELETIONS) continue
            JsonScan.strings(element, refs)
        }
        if (recorderJson != null) JsonScan.strings(recorderJson, refs)

        val existing = doc.tombstones.nameTags.map { it.key }.toSet()
        val codes = UnitCodes(doc)
        val tags = ArrayList<NameTag>()
        fun tag(key: Key, kind: NameTagKind, name: String, hidden: Boolean, voiceCode: String? = null) {
            if (key in refs && key !in existing) {
                tags += NameTag(key, kind.open(), name, voiceCode, hidden, NameTagState.NOT_EXPORTED.open())
            }
        }
        doc.cast.characters.filter { it.deletedAt != null }.forEach { tag(it.key, NameTagKind.CHARACTER, it.name, it.hidden, it.voiceCode) }
        doc.world.places.filter { it.deletedAt != null }.forEach { tag(it.key, NameTagKind.PLACE, it.name, it.hidden) }
        doc.world.terms.filter { it.deletedAt != null }.forEach { tag(it.key, NameTagKind.TERM, it.name, it.hidden) }
        doc.world.factions.filter { it.deletedAt != null }.forEach { tag(it.key, NameTagKind.FACTION, it.name, it.hidden) }
        doc.pages.values.filter { it.deletedAt != null }.forEach { tag(it.key, NameTagKind.PAGE, it.name, it.hidden) }
        doc.story.units.filterNot { doc.liveness.isLive(it) }.forEach { unit ->
            val name = unit.title ?: codes.sceneLabel(unit.key) ?: codes.code(unit.key) ?: ""
            tag(unit.key, NameTagKind.UNIT, name, unit.hidden)
        }
        if (tags.isEmpty()) return doc
        return doc.copy(tombstones = doc.tombstones.copy(nameTags = doc.tombstones.nameTags + tags))
    }

    /**
     * (0.2.0) Without `휴지통도 포함`, rows that live in the normal files while trashed are left out
     * like every other trashed row: trashed tags (N-44; their entry is not written either), versions
     * of numbering units that are not live (N-29), and mark anchors on blocks that are not written
     * are pinned to the block they are drawn on now (N-41, as a purge does).
     */
    fun withoutTrashed020(doc: ProjectDoc): ProjectDoc {
        val l = doc.liveness
        val deadBlocks = doc.allBlocks.filterNot { l.isLive(it) }.mapTo(HashSet()) { it.key }
        val marked = Marks.settleBeforePurge(doc, deadBlocks)
        val tags = doc.project.tags.filter { it.isLive }
        val versions = doc.versions.filterKeys { it == doc.key || l.isUnitLive(it) }
        if (marked === doc && tags.size == doc.project.tags.size && versions.size == doc.versions.size) return doc
        return marked.copy(project = marked.project.copy(tags = tags), versions = versions)
    }

    /** `gallery/<owner>.json` and its trash mirror `trash/gallery/<owner>.json`. */
    internal fun isGalleryJson(path: String): Boolean = PackagePaths.isGalleryUnit(path.removePrefix(PackagePaths.TRASH_DIR))

    private fun dropEmpty(json: MutableMap<String, JsonElement>, path: String, empty: Boolean) {
        if (empty) json.remove(path)
    }

    /** File extension of a recorder audio MIME type (`ED`). */
    fun audioExt(mime: String): String = when (mime.substringBefore(';').trim().lowercase()) {
        "audio/mp4", "audio/m4a", "audio/x-m4a" -> "m4a"
        "audio/aac" -> "aac"
        "audio/ogg", "audio/opus" -> "ogg"
        "audio/mpeg" -> "mp3"
        "audio/wav", "audio/x-wav" -> "wav"
        "audio/webm" -> "webm"
        "audio/3gpp" -> "3gp"
        else -> "bin"
    }
}
