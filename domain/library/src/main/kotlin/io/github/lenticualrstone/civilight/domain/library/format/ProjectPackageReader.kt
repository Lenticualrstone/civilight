package io.github.lenticualrstone.civilight.domain.library.format

import io.github.lenticualrstone.civilight.domain.library.common.UnitCodes
import io.github.lenticualrstone.civilight.model.app.UnknownFile
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import io.github.lenticualrstone.civilight.model.file.EncryptionHeader
import io.github.lenticualrstone.civilight.model.file.Features
import io.github.lenticualrstone.civilight.model.file.FileManifest
import io.github.lenticualrstone.civilight.model.file.ManifestProject
import io.github.lenticualrstone.civilight.model.file.PackageLockType
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.file.RecorderFile
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import java.util.Base64

/** A package file that could not be read; everything else was imported (LIB-55). */
data class FailedPart(val path: String, val label: String)

/** A readable `.civilight` package, ready for the import decision (LIB-52). */
class ImportedPackage(
    val manifest: FileManifest,
    /** The project as read (after upgrades); app-only state empty; unknown files in `unknownFiles`. */
    val doc: ProjectDoc,
    /** `recorder.json`, or null when absent or unreadable. */
    val recorder: RecorderFile?,
    /** Blob bytes by hash (`assets/<hash>.<ext>` and gallery originals `gallery/files/<hash>.<ext>`). */
    val blobs: Map<String, ByteArray>,
    val preview: ByteArray?,
    val failed: List<FailedPart>,
    /** Optional feature ids this app does not know; their parts were skipped (kept as unknown files). */
    val skippedOptionalFeatures: List<String>,
    /** Upgrade steps applied, in order (`1.0→1.1`). */
    val appliedUpgrades: List<String>,
    /** Size of the package file. */
    val sizeBytes: Long,
    /** Lock of the encrypted container this package came out of; null for a plain file. */
    val lockType: OpenEnum<PackageLockType>? = null,
) {
    /** True when this package came out of an encrypted container. */
    val decrypted: Boolean get() = lockType != null

    /** `열지 못한 부분 · 1-7` (one line, parts joined by ` · `), or null when nothing failed. */
    val failedMessage: String?
        get() = if (failed.isEmpty()) null else (listOf(FAILED_PREFIX) + failed.map { it.label }).joinToString(" · ")

    /** This package as read out of a container locked with [lock]. */
    fun copyDecrypted(lock: OpenEnum<PackageLockType>): ImportedPackage = ImportedPackage(
        manifest, doc, recorder, blobs, preview, failed, skippedOptionalFeatures, appliedUpgrades, sizeBytes, lockType = lock,
    )

    companion object {
        const val FAILED_PREFIX = "열지 못한 부분"
    }
}

/** Outcome of opening a file (LIB-52…55). */
sealed interface PackageOpen {
    /** Not a `.civilight` project package. */
    data class NotAPackage(val reason: String) : PackageOpen

    /** An encrypted container; nothing but the header is readable before the password (LIB-54). */
    class Encrypted(val header: EncryptionHeader, val sizeBytes: Long) : PackageOpen

    /** Made by a newer app; refused, nothing imported (LIB-53). */
    data class Newer(val manifest: FileManifest, val check: NewerFileCheck, val sheet: NewerFileSheet) : PackageOpen

    /** `manifest.json`, `project.json` or `story/index.json` unreadable: nothing imported (LIB-55). */
    data class Damaged(val reason: String, val path: String?) : PackageOpen

    class Ready(val pkg: ImportedPackage) : PackageOpen
}

/**
 * Reads a `.civilight` package (LIB-49…55, §7, §8). Gate order: ZIP → `mimetype` → encryption header
 * → manifest → newer-file check → per-file hashes → upgrade chain → decode. The input bytes are
 * never modified (promise 1).
 */
object ProjectPackageReader {
    private val FATAL = setOf(PackagePaths.PROJECT, PackagePaths.STORY_INDEX)

    fun open(
        bytes: ByteArray,
        chain: UpgradeChain = UpgradeChain.CURRENT,
        knownFeatures: Set<String> = Features.KNOWN,
    ): PackageOpen {
        val zip = try {
            ZipCodec.read(bytes)
        } catch (e: PackageZipException) {
            return PackageOpen.NotAPackage(e.message ?: "not a zip")
        }
        val first = zip.items.first()
        if (first.path != PackagePaths.MIMETYPE) return PackageOpen.NotAPackage("first entry is not mimetype")
        val mime = first.bytes.toString(Charsets.US_ASCII).trim()
        if (mime != PackagePaths.MIME_PROJECT) return PackageOpen.NotAPackage("mimetype $mime")

        zip[PackagePaths.ENCRYPTION]?.let { headerBytes ->
            val header = try {
                CivilightJson.decode(EncryptionHeader.serializer(), headerBytes.toString(Charsets.UTF_8))
            } catch (e: SerializationException) {
                return PackageOpen.Damaged("encryption header: ${e.message}", PackagePaths.ENCRYPTION)
            } catch (e: IllegalArgumentException) {
                return PackageOpen.Damaged("encryption header: ${e.message}", PackagePaths.ENCRYPTION)
            }
            return PackageOpen.Encrypted(header, bytes.size.toLong())
        }

        val manifestBytes = zip[PackagePaths.MANIFEST] ?: return PackageOpen.Damaged("manifest.json missing", PackagePaths.MANIFEST)
        val manifest = try {
            CivilightJson.decode(FileManifest.serializer(), manifestBytes.toString(Charsets.UTF_8))
        } catch (e: IllegalArgumentException) {
            return PackageOpen.Damaged("manifest.json: ${e.message}", PackagePaths.MANIFEST)
        }
        if (manifest.format != FileManifest.FORMAT_PROJECT) return PackageOpen.NotAPackage("format ${manifest.format}")

        val appMax = chain.latest
        val check = NewerFileCheck.of(manifest, appMax, knownFeatures)
        if (check.refused) return PackageOpen.Newer(manifest, check, NewerFileSheet.of(manifest, check, appMax))
        val version = FormatVersion.parseOrNull(manifest.formatVersion)
            ?: return PackageOpen.Damaged("formatVersion ${manifest.formatVersion}", PackagePaths.MANIFEST)

        // Per-file hashes (LIB-55). A file listed but missing or different is dropped.
        val failedPaths = LinkedHashSet<String>()
        for ((path, expected) in manifest.files) {
            val data = zip[path]
            if (data == null || Sha256.label(data) != expected) failedPaths += path
        }
        failedPaths.firstOrNull { it in FATAL }?.let { return PackageOpen.Damaged("$it damaged", it) }

        // Parse JSON files (all except the manifest and recorder.json go to the project decoder).
        val json = LinkedHashMap<String, JsonElement>()
        var recorderElement: JsonElement? = null
        for (item in zip.items) {
            val path = item.path
            if (path == PackagePaths.MIMETYPE || path == PackagePaths.MANIFEST || path in failedPaths) continue
            if (!path.endsWith(".json") || isBlobPath(path) || isGalleryOriginalPath(path)) continue
            val element = try {
                CivilightJson.json.parseToJsonElement(item.bytes.toString(Charsets.UTF_8))
            } catch (e: SerializationException) {
                if (path in FATAL) return PackageOpen.Damaged("$path: ${e.message}", path)
                failedPaths += path
                continue
            }
            if (path == PackagePaths.RECORDER) recorderElement = element else json[path] = element
        }

        val upgraded = try {
            chain.upgrade(version, json)
        } catch (e: UnsupportedFormatException) {
            return PackageOpen.Damaged(e.message ?: "unsupported version", PackagePaths.MANIFEST)
        }

        val decoded = try {
            ProjectFileSet.decode(upgraded.files)
        } catch (e: IllegalArgumentException) {
            return PackageOpen.Damaged(e.message ?: "project.json or story/index.json unreadable", PackagePaths.PROJECT)
        }
        failedPaths += decoded.failedPaths.keys

        val recorder = recorderElement?.let {
            try {
                CivilightJson.decodeElement(RecorderFile.serializer(), it)
            } catch (e: IllegalArgumentException) {
                failedPaths += PackagePaths.RECORDER
                null
            }
        }

        // Blobs, preview and unknown files.
        val blobs = LinkedHashMap<String, ByteArray>()
        val unknown = LinkedHashMap<String, UnknownFile>()
        val decodedUnknown = decoded.unknownPaths.toSet()
        for (item in zip.items) {
            val path = item.path
            if (path == PackagePaths.MIMETYPE || path == PackagePaths.MANIFEST || path in failedPaths) continue
            when {
                isBlobPath(path) -> {
                    val hash = path.removePrefix("assets/").substringBefore('.')
                    if (Sha256.hex(item.bytes) == hash) blobs[hash] = item.bytes else failedPaths += path
                }
                // Gallery originals join the one blob store, read by path whether or not the manifest
                // lists `gallery` (55 §3.3); the same bytes under `assets/` are stored once.
                isGalleryOriginalPath(path) -> {
                    val hash = path.removePrefix(PackagePaths.GALLERY_FILES_DIR).substringBefore('.')
                    if (Sha256.hex(item.bytes) == hash) blobs.getOrPut(hash) { item.bytes } else failedPaths += path
                }
                path == PackagePaths.PREVIEW || path == PackagePaths.RECORDER -> Unit
                path in decodedUnknown || !path.endsWith(".json") -> unknown[path] = UnknownFile(
                    path = path,
                    bytesBase64 = Base64.getEncoder().encodeToString(item.bytes),
                    sha256 = Sha256.label(item.bytes),
                )
            }
        }

        val doc = withoutOrphanGallery(decoded.files.toDoc(unknownFiles = unknown))
        val codes = UnitCodes(doc)
        val failed = failedPaths.map { path -> FailedPart(path, labelOf(path, codes)) }
        return PackageOpen.Ready(
            ImportedPackage(
                manifest = describingContent(manifest, doc),
                doc = doc,
                recorder = recorder,
                blobs = blobs,
                preview = zip[PackagePaths.PREVIEW]?.takeIf { PackagePaths.PREVIEW !in failedPaths },
                failed = failed,
                skippedOptionalFeatures = check.unknownOptional,
                appliedUpgrades = upgraded.applied.map { it.toString() },
                sizeBytes = bytes.size.toLong(),
            ),
        )
    }

    /**
     * The manifest's `project` block is a description of `project.json`; when the two disagree (an
     * edited or forged manifest) the content wins (`ED`), so the same-project check (LIB-52) and the
     * import always use the key that is actually imported.
     */
    private fun describingContent(manifest: FileManifest, doc: ProjectDoc): FileManifest {
        val p = doc.project
        val described = manifest.project
        if (described != null && described.key == p.key && described.title == p.name && described.rating == p.rating) return manifest
        return manifest.copy(project = ManifestProject(p.key, p.name, p.rating, described?.ext))
    }

    /**
     * [doc] without gallery files whose owner is not in the package (a character purged elsewhere):
     * a character owner must be a character row, a project owner the project itself (55 §3.3).
     */
    internal fun withoutOrphanGallery(doc: ProjectDoc): ProjectDoc {
        val characters = doc.cast.characters.mapTo(HashSet()) { it.key }
        fun ownerExists(kind: GalleryOwnerKind?, key: String): Boolean = when (kind) {
            GalleryOwnerKind.PROJECT -> key == doc.key
            GalleryOwnerKind.CHARACTER -> key in characters
            null -> true
        }
        val kept = doc.gallery.filterValues { ownerExists(it.ownerKind.known, it.ownerKey) }
        return if (kept.size == doc.gallery.size) doc else doc.copy(gallery = kept)
    }

    /** `gallery/files/<64 hex>.<ext>`. */
    internal fun isGalleryOriginalPath(path: String): Boolean {
        if (!path.startsWith(PackagePaths.GALLERY_FILES_DIR) || path.count { it == '/' } != 2) return false
        val name = path.removePrefix(PackagePaths.GALLERY_FILES_DIR)
        val hash = name.substringBefore('.')
        return name.contains('.') && hash.length == 64 && hash.all { it in '0'..'9' || it in 'a'..'f' }
    }

    /** `assets/<64 hex>.<ext>`. */
    internal fun isBlobPath(path: String): Boolean {
        if (!path.startsWith("assets/") || path.count { it == '/' } != 1) return false
        val name = path.removePrefix("assets/")
        val hash = name.substringBefore('.')
        return name.contains('.') && hash.length == 64 && hash.all { it in '0'..'9' || it in 'a'..'f' }
    }

    /** The episode code of an episode file (`1-7`); other files are named by their path (`ED`). */
    private fun labelOf(path: String, codes: UnitCodes): String {
        if (path.startsWith("story/") && path != PackagePaths.STORY_INDEX && path.count { it == '/' } == 1) {
            val key = path.removePrefix("story/").removeSuffix(".json")
            return codes.code(key) ?: key
        }
        return path
    }
}
