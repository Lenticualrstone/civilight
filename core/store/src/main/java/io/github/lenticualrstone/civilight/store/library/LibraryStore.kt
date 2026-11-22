package io.github.lenticualrstone.civilight.store.library

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.app.LibraryIndexFile
import io.github.lenticualrstone.civilight.model.app.ProjectAppState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.file.TrashIndexFile
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.project.TravelMode
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import io.github.lenticualrstone.civilight.store.NoSuchProjectException
import io.github.lenticualrstone.civilight.store.ProjectKeyTakenException
import io.github.lenticualrstone.civilight.store.ReadOnlyProjectException
import io.github.lenticualrstone.civilight.store.StoreEnvironment
import io.github.lenticualrstone.civilight.store.io.AtomicFiles
import io.github.lenticualrstone.civilight.store.io.DebouncedSaver
import io.github.lenticualrstone.civilight.store.io.Dirs
import io.github.lenticualrstone.civilight.store.io.StoreDirectory
import io.github.lenticualrstone.civilight.store.project.ProjectLayout
import io.github.lenticualrstone.civilight.store.project.ProjectRekey
import io.github.lenticualrstone.civilight.store.project.ProjectStore
import io.github.lenticualrstone.civilight.store.project.ProjectStoreListener
import io.github.lenticualrstone.civilight.store.version.StoreUpgradeChain
import io.github.lenticualrstone.civilight.store.version.StoreVersions
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * The 서재 and the project directories (§8.2, §9): the project index `files/library.json`, live
 * projects in `files/projects/<key>/`, trashed ones in `files/trash/projects/<key>/` with their
 * trash rows in `files/trash/index.json` (30-day purge date, LIB-38/39).
 *
 * It also hands out the one [ProjectStore] per open project ([open]); name, rating and hidden flag
 * of the index follow every edit of `project.json` made through those stores.
 *
 * Crash safety: an operation writes the index first and then moves the directory (trash,
 * restore); new directories are written under a staging name and renamed into place; deletion
 * renames the directory aside first. On start the index and the directories are reconciled
 * ([reconcileReport]); a directory is never deleted because the index does not know it.
 */
class LibraryStore(
    private val filesDir: File,
    private val env: StoreEnvironment,
    private val projectChain: StoreUpgradeChain = StoreVersions.project,
    private val libraryChain: StoreUpgradeChain = StoreVersions.library,
) {
    val projectsDir = File(filesDir, PROJECTS_DIR)
    val trashedProjectsDir = File(filesDir, TRASHED_PROJECTS_DIR)

    private val root = StoreDirectory(filesDir)
    private val _state = MutableStateFlow(LibraryState())
    private val loadLock = Mutex()
    private val ops = Mutex()
    private val open = ConcurrentHashMap<Key, ProjectStore>()
    private val pinned = HashSet<Key>()
    private var writtenIndex: LibraryIndexFile? = null
    private var writtenTrash: TrashIndexFile? = null
    private val saver = DebouncedSaver(env.scope, env.timing.saveDebounceMs, env.timing.saveMaxDelayMs, ::save)

    /** Index and project trash rows. Empty until [ensureLoaded] finished. */
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    /** What the last start-up check fixed. */
    var reconcileReport: ReconcileReport = ReconcileReport()
        private set

    private val listener = ProjectStoreListener { key, project, at ->
        _state.update { s ->
            s.withEntry(key) {
                it.copy(name = project.name, rating = project.rating, hidden = project.protections.hidden, updatedAt = at)
            }
        }
        saver.markDirty()
    }

    fun projectDir(key: Key) = File(projectsDir, key)

    fun trashedDir(key: Key) = File(trashedProjectsDir, key)

    /** Reads the index once (and reconciles it with the directories). Every operation calls it. */
    suspend fun ensureLoaded() {
        if (_state.value.loaded) return
        loadLock.withLock {
            if (_state.value.loaded) return
            ops.withLock {
                val (loaded, report) = withContext(env.io) { reconcile(readFromDisk()) }
                reconcileReport = report
                _state.value = loaded
                if (!report.isClean) save() else {
                    writtenIndex = loaded.index
                    writtenTrash = loaded.trash
                }
            }
        }
    }

    // ---- open projects ----

    /** The store of a live project, opened (and loaded) on first use; one instance per project. */
    suspend fun open(key: Key): ProjectStore {
        ensureLoaded()
        val store = ops.withLock {
            pinned += key
            openLocked(key)
        }
        val now = env.clock.now()
        _state.update { s -> s.withEntry(key) { it.copy(lastOpenedAt = now) } }
        saver.markDirty()
        return store
    }

    /** The store of [key] if it is open. */
    fun openStore(key: Key): ProjectStore? = open[key]

    /** Saves and closes the store of [key] (and writes the index rows it changed). */
    suspend fun close(key: Key) {
        ops.withLock {
            pinned -= key
            open.remove(key)?.close()
        }
        saveNow()
    }

    /** Saves and closes every open project. */
    suspend fun closeAll() {
        ops.withLock {
            pinned.clear()
            val stores = open.values.toList()
            open.clear()
            stores.forEach { it.close() }
        }
        saveNow()
    }

    /** The current document of [key]: the open store's, else read from its directory (live or trashed). */
    suspend fun readDoc(key: Key): ProjectDoc {
        ensureLoaded()
        open[key]?.let { return it.doc.value }
        val entry = _state.value.entry(key) ?: throw NoSuchProjectException(key)
        val dir = if (entry.deletedAt == null) projectDir(key) else trashedDir(key)
        return withContext(env.io) { ProjectStore.read(key, dir, env, projectChain) }
    }

    /** A project's document for reference scans, and whether part of it could not be read. */
    class ProjectScan(val doc: ProjectDoc, val incomplete: Boolean)

    /**
     * [readDoc] for scans that delete things (blob garbage collection): [ProjectScan.incomplete] is
     * true when a file of the project is damaged or was set aside, so the document may miss
     * references.
     */
    suspend fun scan(key: Key): ProjectScan {
        ensureLoaded()
        val entry = _state.value.entry(key) ?: throw NoSuchProjectException(key)
        val dir = if (entry.deletedAt == null) projectDir(key) else trashedDir(key)
        val setAside = File(dir, ProjectLayout.DAMAGED_DIR).list()?.isNotEmpty() == true
        open[key]?.let { return ProjectScan(it.doc.value, setAside || it.loadReport.damaged.isNotEmpty()) }
        val (doc, report) = withContext(env.io) { ProjectStore.readWithReport(key, dir, env, projectChain) }
        return ProjectScan(doc, setAside || report.damaged.isNotEmpty())
    }

    // ---- create, rename, duplicate ----

    /**
     * Creates a project named [name] (NFC, trimmed, not blank). The new document has a fresh key,
     * the default travel modes (`TravelMode.defaults()`) and the bookkeeping fields; [configure]
     * adds the rest (structure, first units, field definitions …) before anything is written.
     */
    suspend fun create(
        name: String,
        isExample: Boolean = false,
        readOnly: Boolean = false,
        configure: (ProjectDoc) -> ProjectDoc = { it },
    ): LibraryEntry {
        val clean = ProjectNames.clean(name)
        require(clean.isNotEmpty()) { "project name is blank" }
        val stamp = env.stamp()
        val key = Keys.newKey()
        val base = ProjectDoc(
            project = Project(
                key = key, name = clean,
                travelModes = TravelMode.defaults(at = stamp.at, by = stamp.by),
                createdAt = stamp.at, updatedAt = stamp.at, updatedBy = stamp.by,
            ),
        )
        val doc = configure(base)
        require(doc.project.key == key) { "configure must keep the project key" }
        return add(doc, isExample, readOnly)
    }

    /**
     * Adds a complete project (an import, a copy). Its key must not be used by any live or trashed
     * project (§6); re-key it first ([ProjectRekey]) when it is.
     */
    suspend fun add(doc: ProjectDoc, isExample: Boolean = false, readOnly: Boolean = false): LibraryEntry {
        ensureLoaded()
        return ops.withLock {
            val key = doc.project.key
            if (isTaken(key)) throw ProjectKeyTakenException(key)
            withContext(env.io + NonCancellable) { writeDir(doc, projectDir(key)) }
            val now = env.clock.now()
            val entry = LibraryEntry(
                key = key, name = doc.project.name, rating = doc.project.rating,
                hidden = doc.project.protections.hidden, isExample = isExample, readOnly = readOnly,
                createdAt = doc.project.createdAt.takeIf { it > 0 } ?: now, updatedAt = now,
            )
            _state.update { it.plus(entry) }
            saveNow()
            entry
        }
    }

    /** True when [key] is used by a project or a project directory. */
    fun isTaken(key: Key): Boolean = _state.value.entry(key) != null || projectDir(key).exists() || trashedDir(key).exists()

    /**
     * Renames a live project (LIB-02): `project.json` and the index. One undo step `이름 바꾸기` in
     * the project's history.
     */
    suspend fun rename(key: Key, name: String): LibraryEntry {
        val clean = ProjectNames.clean(name)
        require(clean.isNotEmpty()) { "project name is blank" }
        val entry = requireLive(key)
        if (entry.readOnly) throw ReadOnlyProjectException(key)
        withStore(key) { store ->
            store.update(RENAME_LABEL) { d ->
                val stamp = store.stamp()
                d.copy(project = d.project.copy(name = clean, updatedAt = stamp.at, updatedBy = stamp.by))
            }
            store.flush()
        }
        _state.update { s -> s.withEntry(key) { it.copy(name = clean) } }
        saveNow()
        return _state.value.entry(key)!!
    }

    /**
     * Copies a live project as `<name> (n)` under a new key (item keys kept, §6). The copy starts
     * with fresh app state and is never an example or read-only. The example project is not copied.
     */
    suspend fun duplicate(key: Key): LibraryEntry {
        val entry = requireLive(key)
        require(!entry.isExample) { "the example project is not copied" }
        val source = readDoc(key)
        val stamp = env.stamp()
        val name = ProjectNames.nextFree(source.project.name, _state.value.entries.map { it.name })
        val copy = ProjectRekey.rekey(source.copy(app = ProjectAppState()), Keys.newKey())
        return add(copy.copy(project = copy.project.copy(name = name, createdAt = stamp.at, updatedAt = stamp.at, updatedBy = stamp.by)))
    }

    // ---- trash ----

    /** Moves a live project to the trash (LIB-03): one `project` trash row, purge date today + 30 days. */
    suspend fun moveToTrash(key: Key, reason: TrashReason = TrashReason.DELETED): TrashEntry {
        ensureLoaded()
        return ops.withLock {
            val entry = liveEntry(key)
            if (entry.isExample) throw ReadOnlyProjectException(key)
            closeLocked(key)
            val now = env.clock.now()
            val row = TrashEntry(
                key = Keys.newKey(), kind = TrashKind.PROJECT.open(), projectKey = key, targetKeys = listOf(key),
                reason = reason.open(), hiddenAtDelete = entry.hidden, deletedAt = now,
                purgeOn = TrashEntry.purgeDateFor(env.clock.localDateOf(now)),
            )
            _state.update { s ->
                s.withEntry(key) { it.copy(deletedAt = now, trashEntryKey = row.key) }
                    .let { it.copy(trash = it.trash.copy(entries = it.trash.entries + row)) }
            }
            saveNow()
            withContext(env.io + NonCancellable) { Dirs.move(projectDir(key), trashedDir(key)) }
            row
        }
    }

    /** Brings a trashed project back (`되살리기`). Authentication for hidden projects is the caller's. */
    suspend fun restore(key: Key): LibraryEntry {
        ensureLoaded()
        return ops.withLock {
            trashedEntry(key)
            _state.update { s ->
                s.withEntry(key) { it.copy(deletedAt = null, trashEntryKey = null) }
                    .let { it.copy(trash = it.trash.copy(entries = it.trash.entries.filter { e -> e.projectKey != key })) }
            }
            saveNow()
            withContext(env.io + NonCancellable) { Dirs.move(trashedDir(key), projectDir(key)) }
            _state.value.entry(key)!!
        }
    }

    /** Deletes a trashed project for good (`영구 삭제`, LIB-39). Returns its last index row. */
    suspend fun purge(key: Key): LibraryEntry {
        ensureLoaded()
        return ops.withLock {
            val entry = trashedEntry(key)
            withContext(env.io + NonCancellable) { Dirs.delete(trashedDir(key)) }
            _state.update { it.without(key) }
            saveNow()
            entry
        }
    }

    /**
     * Deletes a live project at once without the trash (the example project on `온보딩 끝내기`,
     * LIB-18, test P-07). Returns its last index row.
     */
    suspend fun deleteWithoutTrash(key: Key): LibraryEntry {
        ensureLoaded()
        return ops.withLock {
            val entry = liveEntry(key)
            closeLocked(key)
            withContext(env.io + NonCancellable) { Dirs.delete(projectDir(key)) }
            _state.update { it.without(key) }
            saveNow()
            entry
        }
    }

    /** Purges every trashed project whose purge date has come (`daysLeft ≤ 0`, T-08). */
    suspend fun purgeExpired(today: LocalDate = env.clock.today()): List<LibraryEntry> {
        ensureLoaded()
        val due = _state.value.trashed.filter { e -> _state.value.trashEntryOf(e.key)?.let { !today.isBefore(it.purgeOn) } ?: false }
        return due.map { purge(it.key) }
    }

    /**
     * Moves the project [key] out of the way of an incoming file with the same key (§6 `ED`): its
     * content is copied under a fresh key K′, the copy is put in the trash (a live project with
     * reason `replacedByOverwrite`; a trashed one keeping its trash row), and [key] is removed.
     * Returns K′. A crash part-way leaves an extra copy, never a lost one.
     */
    suspend fun moveAsideForImport(key: Key): Key {
        ensureLoaded()
        val entry = _state.value.entry(key) ?: throw NoSuchProjectException(key)
        if (entry.isExample) throw ReadOnlyProjectException(key)
        val doc = readDoc(key)
        val newKey = Keys.newKey()
        add(ProjectRekey.rekey(doc, newKey))
        if (entry.deletedAt == null) {
            moveToTrash(newKey, TrashReason.REPLACED_BY_OVERWRITE)
            deleteWithoutTrash(key)
        } else {
            val old = _state.value.trashEntryOf(key)
            moveToTrash(newKey, old?.reason?.known ?: TrashReason.DELETED)
            if (old != null) {
                _state.update { s ->
                    val row = s.trashEntryOf(newKey) ?: return@update s
                    val kept = row.copy(deletedAt = old.deletedAt, purgeOn = old.purgeOn, hiddenAtDelete = old.hiddenAtDelete)
                    s.withEntry(newKey) { it.copy(deletedAt = old.deletedAt) }
                        .let { it.copy(trash = it.trash.copy(entries = it.trash.entries.map { e -> if (e.key == row.key) kept else e })) }
                }
                saveNow()
            }
            purge(key)
        }
        return newKey
    }

    // ---- saving ----

    /** Writes the index now. */
    suspend fun flush() = saver.flush()

    private suspend fun saveNow() = saver.flush()

    private suspend fun save() {
        val s = _state.value
        if (!s.loaded) return
        withContext(env.io + NonCancellable) {
            if (s.index != writtenIndex) {
                root.write(LIBRARY_FILE, CivilightJson.encode(LibraryIndexFile.serializer(), s.index).toByteArray())
                writtenIndex = s.index
            }
            if (s.trash != writtenTrash) {
                root.write(TRASH_INDEX_FILE, CivilightJson.encode(TrashIndexFile.serializer(), s.trash).toByteArray())
                writtenTrash = s.trash
            }
        }
    }

    // ---- internals ----

    private suspend fun <T> withStore(key: Key, block: suspend (ProjectStore) -> T): T {
        val store = ops.withLock { openLocked(key) }
        try {
            return block(store)
        } finally {
            ops.withLock {
                if (key !in pinned && open[key] === store) {
                    open.remove(key)
                    store.close()
                }
            }
        }
    }

    private suspend fun openLocked(key: Key): ProjectStore {
        open[key]?.let { return it }
        val entry = liveEntry(key)
        val store = ProjectStore.open(key, projectDir(key), env, entry.readOnly, listener, projectChain)
        open[key] = store
        return store
    }

    private suspend fun closeLocked(key: Key) {
        pinned -= key
        open.remove(key)?.close()
    }

    private suspend fun requireLive(key: Key): LibraryEntry {
        ensureLoaded()
        return liveEntry(key)
    }

    private fun liveEntry(key: Key): LibraryEntry {
        val entry = _state.value.entry(key) ?: throw NoSuchProjectException(key)
        if (entry.deletedAt != null) throw NoSuchProjectException(key, "in the trash")
        return entry
    }

    private fun trashedEntry(key: Key): LibraryEntry {
        val entry = _state.value.entry(key) ?: throw NoSuchProjectException(key)
        if (entry.deletedAt == null) throw NoSuchProjectException(key, "not in the trash")
        return entry
    }

    private fun writeDir(doc: ProjectDoc, target: File) {
        val staging = File(target.parentFile, Dirs.STAGING_PREFIX + target.name)
        staging.deleteRecursively()
        ProjectStore.writeNew(doc, staging, projectChain)
        Dirs.move(staging, target)
    }

    private fun readFromDisk(): LibraryState {
        AtomicFiles.deleteTemps(filesDir, recursive = false)
        AtomicFiles.deleteTemps(File(filesDir, TRASH_DIR), recursive = false)
        val index = readIndex()
        val trash = readJson(TRASH_INDEX_FILE) { CivilightJson.decodeElement(TrashIndexFile.serializer(), it) } ?: TrashIndexFile()
        return LibraryState(index, trash, loaded = true)
    }

    private fun readIndex(): LibraryIndexFile = readJson(LIBRARY_FILE) { element ->
        val version = (element as? JsonObject)?.get("storeVersion")?.jsonPrimitive?.intOrNull ?: 1
        val upgraded = libraryChain.upgrade(version, mapOf(LIBRARY_FILE to element)).getValue(LIBRARY_FILE)
        CivilightJson.decodeElement(LibraryIndexFile.serializer(), upgraded).copy(storeVersion = libraryChain.current)
    } ?: LibraryIndexFile(storeVersion = libraryChain.current)

    /** Reads a JSON file of the root; an unreadable one is set aside (never overwritten) and null returned. */
    private fun <T> readJson(path: String, decode: (JsonElement) -> T): T? {
        val text = root.readText(path) ?: return null
        return try {
            decode(CivilightJson.json.parseToJsonElement(text))
        } catch (e: IllegalArgumentException) {
            val from = root.file(path)
            AtomicFiles.moveAtomically(from, File(from.parentFile, "${from.name}.damaged-${env.clock.now()}"))
            null
        }
    }

    /** Makes the index and the directories agree (see the class comment). Runs on [StoreEnvironment.io]. */
    private fun reconcile(state: LibraryState): Pair<LibraryState, ReconcileReport> {
        projectsDir.mkdirs()
        trashedProjectsDir.mkdirs()
        Dirs.cleanUp(projectsDir)
        Dirs.cleanUp(trashedProjectsDir)

        val moved = mutableListOf<Key>()
        val dropped = mutableListOf<Key>()
        val adopted = mutableListOf<Key>()
        val unreadable = mutableListOf<String>()
        var s = state

        for (entry in state.entries) {
            val live = projectDir(entry.key)
            val trashed = trashedDir(entry.key)
            val (expected, other) = if (entry.deletedAt == null) live to trashed else trashed to live
            when {
                expected.isDirectory -> Unit
                other.isDirectory -> {
                    Dirs.move(other, expected)
                    moved += entry.key
                }
                else -> {
                    s = s.without(entry.key)
                    dropped += entry.key
                }
            }
        }

        val now = env.clock.now()
        for ((parent, trashed) in listOf(projectsDir to false, trashedProjectsDir to true)) {
            for (dir in Dirs.contentDirs(parent)) {
                if (s.entry(dir.name) != null) continue
                val project = readProjectJson(dir)
                if (project == null || project.key != dir.name) {
                    unreadable += dir.path
                    continue
                }
                s = s.plus(
                    LibraryEntry(
                        key = project.key, name = project.name, rating = project.rating, hidden = project.protections.hidden,
                        deletedAt = if (trashed) now else null, createdAt = project.createdAt,
                        updatedAt = maxOf(project.updatedAt, dir.lastModified()),
                    ),
                )
                adopted += project.key
            }
        }

        // Trash rows: exactly one per trashed project.
        var fixed = 0
        val trashedKeys = s.entries.filter { it.deletedAt != null }.map { it.key }.toSet()
        val keptRows = s.trash.entries.filter { row -> row.projectKey in trashedKeys }
            .distinctBy { it.projectKey }
        fixed += s.trash.entries.size - keptRows.size
        val rows = keptRows.toMutableList()
        var entries = s.entries
        for (entry in s.entries.filter { it.deletedAt != null }) {
            val row = rows.firstOrNull { it.projectKey == entry.key }
            if (row == null) {
                val deletedAt: Timestamp = entry.deletedAt!!
                val newRow = TrashEntry(
                    key = Keys.newKey(), kind = TrashKind.PROJECT.open(), projectKey = entry.key, targetKeys = listOf(entry.key),
                    hiddenAtDelete = entry.hidden, deletedAt = deletedAt,
                    purgeOn = TrashEntry.purgeDateFor(env.clock.localDateOf(deletedAt)),
                )
                rows += newRow
                entries = entries.map { if (it.key == entry.key) it.copy(trashEntryKey = newRow.key) else it }
                fixed++
            } else if (entry.trashEntryKey != row.key) {
                entries = entries.map { if (it.key == entry.key) it.copy(trashEntryKey = row.key) else it }
                fixed++
            }
        }
        s = s.copy(index = s.index.copy(projects = entries), trash = s.trash.copy(entries = rows))
        return s to ReconcileReport(moved, dropped, adopted, unreadable, fixed)
    }

    private fun readProjectJson(dir: File): Project? {
        val file = File(dir, PackagePaths.PROJECT)
        if (!file.isFile) return null
        return runCatching { CivilightJson.decode(Project.serializer(), file.readText()) }.getOrNull()
    }

    companion object {
        const val LIBRARY_FILE = "library.json"
        const val PROJECTS_DIR = "projects"
        const val TRASH_DIR = "trash"
        const val TRASH_INDEX_FILE = "trash/index.json"
        const val TRASHED_PROJECTS_DIR = "trash/projects"

        /** Undo label of a rename (LIB-02 row `이름 바꾸기`). */
        const val RENAME_LABEL = "이름 바꾸기"
    }
}
