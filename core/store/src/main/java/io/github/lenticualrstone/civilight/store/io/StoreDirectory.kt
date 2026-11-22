package io.github.lenticualrstone.civilight.store.io

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * One directory of the working store, addressed by relative paths with `/` (`story/index.json`).
 *
 * [commit] writes several files as one transaction: every new file is first written to a synced
 * temp, then a journal naming the temps is written atomically, then the temps are renamed onto
 * their targets and the journal is removed. [recover] (run before reading) finishes a transaction
 * whose journal exists and drops temps of one that never reached its journal, so after a crash the
 * directory holds either all files of a transaction or none of them.
 */
class StoreDirectory(val root: File, var faults: WriteFaults = WriteFaults.None) {

    fun file(path: String): File {
        require(path.isNotEmpty() && !path.startsWith("/") && path.split('/').none { it == ".." || it.isEmpty() }) {
            "bad store path: $path"
        }
        return File(root, path)
    }

    fun exists(path: String): Boolean = file(path).isFile

    fun read(path: String): ByteArray? = AtomicFiles.readOrNull(file(path))

    fun readText(path: String): String? = read(path)?.toString(Charsets.UTF_8)

    /** Writes one file atomically. */
    fun write(path: String, bytes: ByteArray) {
        val target = file(path)
        AtomicFiles.write(target, bytes, faults)
        faults.committed(target, deleted = false)
    }

    /** Deletes one file (no error when missing). */
    fun delete(path: String) {
        val target = file(path)
        if (target.exists() && !target.delete() && target.exists()) throw IOException("cannot delete $target")
        faults.committed(target, deleted = true)
    }

    /**
     * Every content file below [root] as a relative path, skipping temps, the journal and the
     * directories in [skipPrefixes] (each ending in `/`).
     */
    fun listFiles(skipPrefixes: Set<String> = emptySet()): List<String> {
        val out = ArrayList<String>()
        fun walk(dir: File, prefix: String) {
            for (child in dir.listFiles().orEmpty().sortedBy { it.name }) {
                val rel = prefix + child.name
                when {
                    child.isDirectory -> if ("$rel/" !in skipPrefixes) walk(child, "$rel/")
                    child.isFile && !AtomicFiles.isTemp(child) && rel != JOURNAL -> out += rel
                }
            }
        }
        if (root.isDirectory) walk(root, "")
        return out
    }

    /** Writes [writes] and removes [deletes] as one transaction (see the class comment). */
    fun commit(writes: Map<String, ByteArray>, deletes: Collection<String> = emptyList()) {
        val removals = deletes.filter { it !in writes }
        if (writes.isEmpty() && removals.isEmpty()) return
        if (writes.size == 1 && removals.isEmpty()) {
            val (path, bytes) = writes.entries.single()
            write(path, bytes)
            return
        }
        root.mkdirs()
        val temps = LinkedHashMap<String, File>()
        try {
            for ((path, bytes) in writes) temps[path] = AtomicFiles.writeTemp(file(path), bytes, faults)
        } catch (e: Throwable) {
            if (e !is SimulatedCrash) temps.values.forEach { it.delete() }
            throw e
        }
        faults.beforeJournal()
        val journal = Journal(
            writes = temps.map { (path, temp) -> JournalWrite(path, temp.relativeTo(root).invariantSeparatorsPath) },
            deletes = removals,
        )
        AtomicFiles.write(file(JOURNAL), json.encodeToString(Journal.serializer(), journal).toByteArray(), faults)
        apply(journal)
    }

    /**
     * Finishes or drops an interrupted transaction and removes temp files ([recursive] into
     * sub-directories). Returns true when a journal was found and rolled forward.
     */
    fun recover(recursive: Boolean = true): Boolean {
        val journalFile = file(JOURNAL)
        var rolledForward = false
        if (journalFile.isFile) {
            val journal = runCatching { json.decodeFromString(Journal.serializer(), journalFile.readText()) }.getOrNull()
            if (journal != null) {
                apply(journal)
                rolledForward = true
            } else {
                journalFile.delete()
            }
        }
        AtomicFiles.deleteTemps(root, recursive)
        return rolledForward
    }

    private fun apply(journal: Journal) {
        for (write in journal.writes) {
            val temp = File(root, write.temp)
            val target = file(write.path)
            if (temp.isFile) {
                faults.beforeRename(target)
                AtomicFiles.moveAtomically(temp, target)
                faults.committed(target, deleted = false)
            }
        }
        for (path in journal.deletes) {
            val target = file(path)
            if (target.exists()) target.delete()
            faults.committed(target, deleted = true)
        }
        file(JOURNAL).delete()
    }

    @Serializable
    private data class Journal(val writes: List<JournalWrite>, val deletes: List<String> = emptyList())

    @Serializable
    private data class JournalWrite(val path: String, val temp: String)

    companion object {
        /** The transaction journal at the directory root. */
        const val JOURNAL = ".commit-journal.json"

        private val json = Json { ignoreUnknownKeys = true }
    }
}
