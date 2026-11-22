package io.github.lenticualrstone.civilight.store.blob

import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.store.AppClock
import io.github.lenticualrstone.civilight.store.io.AtomicFiles
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/** Reads pixel size of an image file; [None] knows nothing (JVM tests). */
fun interface ImageSizeReader {
    /** (width, height) or null when [file] is not a readable image. */
    fun sizeOf(file: File): Pair<Int, Int>?

    companion object {
        val None = ImageSizeReader { null }
    }
}

/**
 * The one content-addressed file store of the device (§2.10, §8.2): `files/blobs/<sha256>.<ext>`.
 * Images, map bases, sketch photos and recorder audio are each stored once, whichever projects
 * use them; rows refer to a blob by its hash ([BlobReferences]). [collectGarbage] removes files no
 * live or trashed row references any more (§5.3).
 */
class BlobStore(
    val dir: File,
    private val clock: AppClock,
    private val io: CoroutineDispatcher,
    private val imageSizeReader: ImageSizeReader = ImageSizeReader.None,
) {
    private val extByHash = ConcurrentHashMap<String, String>()
    @Volatile private var scanned = false

    /**
     * Stores the bytes of [input] (closed afterwards) and returns their [Blob] row. The same bytes
     * imported twice give the same file. [fileName] is only used to guess the extension when [mime]
     * does not name one.
     */
    suspend fun import(
        input: InputStream,
        mime: String,
        source: BlobSource,
        fileName: String? = null,
        durationMs: Long? = null,
    ): Blob = importFrom({ input }, mime, source, fileName, durationMs)

    /** Like [import], with the stream opened (on the I/O dispatcher) by [openStream]. */
    suspend fun importFrom(
        openStream: () -> InputStream,
        mime: String,
        source: BlobSource,
        fileName: String? = null,
        durationMs: Long? = null,
    ): Blob = withContext(io) {
        ensureScanned()
        dir.mkdirs()
        val ext = MimeTypes.extensionFor(mime, fileName)
        val temp = AtomicFiles.tempFor(File(dir, "import.$ext"))
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = openStream().use { raw ->
                DigestInputStream(raw, digest).use { inp ->
                    FileOutputStream(temp).use { out ->
                        val n = inp.copyTo(out)
                        out.flush()
                        out.fd.sync()
                        n
                    }
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            val existing = fileOf(hash)
            val target = existing ?: File(dir, "$hash.$ext")
            if (existing == null) {
                AtomicFiles.moveAtomically(temp, target)
                extByHash[hash] = ext
            } else {
                temp.delete()
            }
            // The import time starts the garbage-collection grace period, also for bytes already stored.
            target.setLastModified(clock.now())
            val size = if (mime.startsWith("image/")) runCatching { imageSizeReader.sizeOf(target) }.getOrNull() else null
            Blob(
                hash = hash, fileExt = target.extension, mime = mime, bytes = bytes,
                width = size?.first, height = size?.second, durationMs = durationMs,
                source = source.open(), importedAt = clock.now(),
            )
        } finally {
            temp.delete()
        }
    }

    /** Stores a copy of [file]. */
    suspend fun import(file: File, mime: String, source: BlobSource, durationMs: Long? = null): Blob =
        importFrom({ file.inputStream() }, mime, source, file.name, durationMs)

    /** The file of [hash], or null when it is not stored. */
    fun fileOf(hash: String): File? {
        ensureScanned()
        val ext = extByHash[hash] ?: return null
        val file = File(dir, "$hash.$ext")
        if (file.isFile) return file
        extByHash.remove(hash)
        return null
    }

    /** The file of [blob]. */
    fun fileOf(blob: Blob): File? = fileOf(blob.hash)

    fun contains(hash: String): Boolean = fileOf(hash) != null

    /** Every stored hash. */
    fun hashes(): Set<String> {
        ensureScanned()
        return extByHash.keys.toSet()
    }

    /**
     * Deletes every blob not in [referenced] whose last import is older than [graceMs] (a blob
     * just imported and not yet referenced is kept). Returns the deleted hashes.
     */
    suspend fun collectGarbage(referenced: Set<String>, graceMs: Long): List<String> = withContext(io) {
        ensureScanned()
        val cutoff = clock.now() - graceMs
        val deleted = mutableListOf<String>()
        for ((hash, ext) in extByHash.entries.toList()) {
            if (hash in referenced) continue
            val file = File(dir, "$hash.$ext")
            if (file.isFile && file.lastModified() > cutoff) continue
            if (!file.exists() || file.delete()) {
                extByHash.remove(hash)
                deleted += hash
            }
        }
        deleted
    }

    private fun ensureScanned() {
        if (scanned) return
        synchronized(this) {
            if (scanned) return
            AtomicFiles.deleteTemps(dir, recursive = false)
            for (file in dir.listFiles().orEmpty()) {
                if (!file.isFile) continue
                val hash = file.name.substringBefore('.')
                if (HASH.matches(hash)) extByHash[hash] = file.name.substringAfter('.', "")
            }
            scanned = true
        }
    }

    private companion object {
        val HASH = Regex("[0-9a-f]{64}")
    }
}
