package io.github.lenticualrstone.civilight.domain.library.format

import io.github.lenticualrstone.civilight.model.file.PackagePaths
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** One entry of a package ZIP, in file order. */
class ZipItem(val path: String, val bytes: ByteArray, val stored: Boolean)

/** The entries of a package ZIP in file order. */
class ZipContents(val items: List<ZipItem>) {
    private val byPath = items.associateBy { it.path }

    val paths: List<String> get() = items.map { it.path }

    operator fun get(path: String): ByteArray? = byPath[path]?.bytes

    operator fun contains(path: String): Boolean = path in byPath

    /** Total uncompressed size. */
    val totalBytes: Long get() = items.sumOf { it.bytes.size.toLong() }
}

/** Thrown for anything that is not a readable package ZIP. */
class PackageZipException(message: String) : Exception(message)

/**
 * The ZIP container of `.civilight` / `.civilight-recorder` (LIB-49, §7.1): entry 0 is `mimetype`,
 * stored without compression and without a trailing newline; every other entry is deflated.
 */
object ZipCodec {
    /** Default cap on the total uncompressed size read from one package (zip-bomb guard). */
    const val DEFAULT_MAX_BYTES: Long = 4L * 1024 * 1024 * 1024

    /**
     * Writes [mimetype] as the stored first entry, then [files] in their iteration order,
     * deflated. [time] is the modification time written on every entry.
     */
    fun write(mimetype: String, files: Map<String, ByteArray>, time: Long): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            val mime = mimetype.toByteArray(Charsets.US_ASCII)
            val crc = CRC32().apply { update(mime) }
            zip.putNextEntry(
                ZipEntry(PackagePaths.MIMETYPE).apply {
                    method = ZipEntry.STORED
                    size = mime.size.toLong()
                    compressedSize = mime.size.toLong()
                    this.crc = crc.value
                    this.time = time
                },
            )
            zip.write(mime)
            zip.closeEntry()
            for ((path, bytes) in files) {
                require(path != PackagePaths.MIMETYPE) { "mimetype is written first by the codec" }
                checkPath(path)
                zip.putNextEntry(ZipEntry(path).apply { method = ZipEntry.DEFLATED; this.time = time })
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    /**
     * Reads every entry. Rejects unsafe or duplicate entry names and packages whose uncompressed
     * size exceeds [maxBytes]. Directory entries are skipped.
     */
    fun read(bytes: ByteArray, maxBytes: Long = DEFAULT_MAX_BYTES): ZipContents {
        val items = ArrayList<ZipItem>()
        val seen = HashSet<String>()
        var total = 0L
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val path = entry.name
                    checkPath(path)
                    if (!seen.add(path)) throw PackageZipException("duplicate entry: $path")
                    val buffer = ByteArrayOutputStream()
                    val chunk = ByteArray(64 * 1024)
                    while (true) {
                        val n = zip.read(chunk)
                        if (n < 0) break
                        total += n
                        if (total > maxBytes) throw PackageZipException("package larger than $maxBytes bytes")
                        buffer.write(chunk, 0, n)
                    }
                    items += ZipItem(path, buffer.toByteArray(), entry.method == ZipEntry.STORED)
                }
            }
        } catch (e: ZipException) {
            throw PackageZipException(e.message ?: "not a zip")
        } catch (e: java.io.IOException) {
            throw PackageZipException(e.message ?: "unreadable zip")
        } catch (e: IllegalArgumentException) {
            throw PackageZipException(e.message ?: "bad entry name")
        }
        if (items.isEmpty()) throw PackageZipException("empty or not a zip")
        return ZipContents(items)
    }

    private fun checkPath(path: String) {
        val bad = path.isEmpty() || path.startsWith("/") || path.contains('\\') ||
            path.split('/').any { it == ".." || it == "." || it.isEmpty() }
        if (bad) throw PackageZipException("unsafe entry name: $path")
    }
}
