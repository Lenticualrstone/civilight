package io.github.lenticualrstone.civilight.store.library

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.app.LibraryIndexFile
import io.github.lenticualrstone.civilight.model.file.TrashIndexFile
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.trash.TrashEntry

/**
 * The project index (`files/library.json`) and the trash rows of whole projects
 * (`files/trash/index.json`, kind `project`). Rows inside a project stay in that project's own
 * `trash/index.json`; recorder rows in the recorder store.
 */
data class LibraryState(
    val index: LibraryIndexFile = LibraryIndexFile(),
    val trash: TrashIndexFile = TrashIndexFile(),
    /** False until the files were read; nothing is written before. */
    val loaded: Boolean = false,
) {
    val entries: List<LibraryEntry> get() = index.projects

    /** Projects not in the trash, last modified first (LIB-01 `ED`). */
    val live: List<LibraryEntry> get() = index.projects.filter { it.deletedAt == null }.sortedByDescending { it.updatedAt }

    /** Projects in the trash, last deleted first. */
    val trashed: List<LibraryEntry> get() = index.projects.filter { it.deletedAt != null }.sortedByDescending { it.deletedAt }

    fun entry(key: Key): LibraryEntry? = index.projects.firstOrNull { it.key == key }

    /** The trash row of a trashed project. */
    fun trashEntryOf(key: Key): TrashEntry? {
        val entry = entry(key) ?: return null
        return trash.entries.firstOrNull { it.key == entry.trashEntryKey } ?: trash.entries.firstOrNull { it.projectKey == key }
    }

    internal fun withEntry(key: Key, transform: (LibraryEntry) -> LibraryEntry): LibraryState =
        copy(index = index.copy(projects = index.projects.map { if (it.key == key) transform(it) else it }))

    internal fun plus(entry: LibraryEntry): LibraryState = copy(index = index.copy(projects = index.projects + entry))

    internal fun without(key: Key): LibraryState = copy(
        index = index.copy(projects = index.projects.filter { it.key != key }),
        trash = trash.copy(entries = trash.entries.filter { it.projectKey != key }),
    )
}

/** What the start-up check of project directories against the index changed. */
data class ReconcileReport(
    /** Directories moved to match the index (an interrupted trash or restore). */
    val moved: List<Key> = emptyList(),
    /** Index rows dropped because no directory exists anywhere. */
    val dropped: List<Key> = emptyList(),
    /** Directories without an index row that were added back. */
    val adopted: List<Key> = emptyList(),
    /** Directories that could not be read or adopted; left untouched. */
    val unreadable: List<String> = emptyList(),
    /** Trash rows added for trashed projects without one, or removed when not matching a trashed project. */
    val trashRowsFixed: Int = 0,
) {
    val isClean: Boolean get() = moved.isEmpty() && dropped.isEmpty() && adopted.isEmpty() && unreadable.isEmpty() && trashRowsFixed == 0
}

/** Project names (LIB-02 rename, LIB-52 `(2)` suffix). */
object ProjectNames {
    private val SUFFIX = Regex(""" \((\d+)\)$""")

    /** NFC, trimmed (LIB-02 `ED`). */
    fun clean(name: String): String = name.nfc().trim()

    /** `<name> (n)` with the smallest n ≥ 2 not in [taken] (F-10, F-11); an existing ` (n)` suffix is replaced. */
    fun nextFree(name: String, taken: Collection<String>): String {
        val base = clean(name).replace(SUFFIX, "")
        val used = taken.mapTo(HashSet()) { clean(it) }
        var n = 2
        while ("$base ($n)" in used) n++
        return "$base ($n)"
    }
}
