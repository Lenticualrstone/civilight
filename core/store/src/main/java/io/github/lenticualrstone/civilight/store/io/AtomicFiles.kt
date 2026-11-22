package io.github.lenticualrstone.civilight.store.io

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.SecureRandom

/**
 * Crash-safe single-file writes: the bytes go to a temp file in the same directory, are synced to
 * the disk, and only then replace the target with one atomic rename. A crash at any point leaves
 * either the old file or the new one, never a half-written target; a left-over temp is removed by
 * [deleteTemps] on the next start.
 */
object AtomicFiles {
    /** Suffix of every temp file the stores write. Files with it are never read as content. */
    const val TEMP_SUFFIX = ".civilight-tmp"

    private val random = SecureRandom()

    fun isTemp(file: File): Boolean = file.name.endsWith(TEMP_SUFFIX)

    /** A fresh temp file next to [target] (not created). */
    fun tempFor(target: File): File {
        val tag = java.lang.Long.toHexString(random.nextLong())
        return File(target.parentFile, ".${target.name}.$tag$TEMP_SUFFIX")
    }

    /** Writes [bytes] to [target] atomically. */
    fun write(target: File, bytes: ByteArray, faults: WriteFaults = WriteFaults.None) {
        val temp = writeTemp(target, bytes, faults)
        try {
            faults.beforeRename(target)
            moveAtomically(temp, target)
        } catch (e: Throwable) {
            if (e !is SimulatedCrash) temp.delete()
            throw e
        }
    }

    /** Copies [input] into [target] atomically; returns the number of bytes. */
    fun write(target: File, input: InputStream): Long {
        target.parentFile?.mkdirs()
        val temp = tempFor(target)
        try {
            val count = FileOutputStream(temp).use { out ->
                val n = input.copyTo(out)
                out.flush()
                out.fd.sync()
                n
            }
            moveAtomically(temp, target)
            return count
        } catch (e: Throwable) {
            temp.delete()
            throw e
        }
    }

    /** Writes [bytes] to a synced temp file next to [target] and returns it. */
    fun writeTemp(target: File, bytes: ByteArray, faults: WriteFaults = WriteFaults.None): File {
        target.parentFile?.let { if (!it.isDirectory && !it.mkdirs() && !it.isDirectory) throw IOException("cannot create $it") }
        val temp = tempFor(target)
        try {
            FileOutputStream(temp).use { out ->
                faults.write(target, out, bytes)
                out.flush()
                out.fd.sync()
            }
        } catch (e: Throwable) {
            if (e !is SimulatedCrash) temp.delete()
            throw e
        }
        return temp
    }

    /** Renames [from] onto [to], replacing it; atomic where the file system allows it. */
    fun moveAtomically(from: File, to: File) {
        to.parentFile?.mkdirs()
        try {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    /** Deletes temp files under [dir] ([recursive] into sub-directories); returns how many. */
    fun deleteTemps(dir: File, recursive: Boolean): Int {
        if (!dir.isDirectory) return 0
        var count = 0
        for (child in dir.listFiles().orEmpty()) {
            when {
                child.isFile && isTemp(child) -> if (child.delete()) count++
                child.isDirectory && recursive -> count += deleteTemps(child, true)
            }
        }
        return count
    }

    /** Reads [file], or null when it does not exist. */
    fun readOrNull(file: File): ByteArray? = if (file.isFile) file.readBytes() else null
}
