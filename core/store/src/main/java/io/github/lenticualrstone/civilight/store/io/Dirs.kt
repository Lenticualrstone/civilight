package io.github.lenticualrstone.civilight.store.io

import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Whole-directory moves and deletes of the store (project directories). */
object Dirs {
    /** Prefix of a directory being written for the first time (never read as a project). */
    const val STAGING_PREFIX = ".staging-"

    /** Prefix of a directory being deleted (finished on the next start). */
    const val PURGING_PREFIX = ".purging-"

    /** Renames [from] to [to] in one step; [to] must not exist. */
    fun move(from: File, to: File) {
        if (to.exists()) throw IOException("target exists: $to")
        to.parentFile?.mkdirs()
        try {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(from.toPath(), to.toPath())
        }
    }

    /** Deletes [dir]: first renamed aside in one step, so a crash never leaves half a project under its name. */
    fun delete(dir: File) {
        if (!dir.exists()) return
        val aside = File(dir.parentFile, PURGING_PREFIX + dir.name + "-" + System.nanoTime())
        move(dir, aside)
        if (!aside.deleteRecursively()) throw IOException("cannot delete $aside")
    }

    /** Removes left-over staging and purging directories directly under [parent]. */
    fun cleanUp(parent: File): Int {
        var count = 0
        for (child in parent.listFiles().orEmpty()) {
            if (child.isDirectory && (child.name.startsWith(STAGING_PREFIX) || child.name.startsWith(PURGING_PREFIX))) {
                if (child.deleteRecursively()) count++
            }
        }
        return count
    }

    /** Sub-directories of [parent] that hold store content (not staging or purging). */
    fun contentDirs(parent: File): List<File> =
        parent.listFiles().orEmpty().filter { it.isDirectory && !it.name.startsWith(".") }.sortedBy { it.name }
}
