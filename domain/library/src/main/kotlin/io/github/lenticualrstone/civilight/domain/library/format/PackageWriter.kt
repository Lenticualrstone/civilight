package io.github.lenticualrstone.civilight.domain.library.format

import io.github.lenticualrstone.civilight.model.file.FileManifest
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import kotlinx.serialization.json.JsonElement

/**
 * Second writer layer (LIB-49/50): files by path → manifest with `sha256-` hashes → ZIP with
 * `mimetype` first (stored) and `manifest.json` second. Kept separate from the project mapping so a
 * package can be built from any file map (tests build tampered packages this way).
 */
object PackageWriter {
    /** [manifest] with `files` set to the hash of every entry of [files]. */
    fun withHashes(manifest: FileManifest, files: Map<String, ByteArray>): FileManifest =
        manifest.copy(files = files.mapValues { (_, bytes) -> Sha256.label(bytes) })

    /** The ZIP bytes. [files] must not contain `mimetype` or `manifest.json`. */
    fun zip(mimetype: String, manifest: FileManifest, files: Map<String, ByteArray>): ByteArray {
        require(PackagePaths.MIMETYPE !in files && PackagePaths.MANIFEST !in files) { "reserved path in files" }
        val hashed = withHashes(manifest, files)
        val entries = LinkedHashMap<String, ByteArray>()
        entries[PackagePaths.MANIFEST] = jsonBytes(CivilightJson.encodeElement(FileManifest.serializer(), hashed))
        entries.putAll(files)
        return ZipCodec.write(mimetype, entries, manifest.writtenAt)
    }

    /** UTF-8 bytes of a JSON tree (compact; the tree is already NFC from `CivilightJson`). */
    fun jsonBytes(element: JsonElement): ByteArray =
        CivilightJson.json.encodeToString(JsonElement.serializer(), element).toByteArray(Charsets.UTF_8)
}
