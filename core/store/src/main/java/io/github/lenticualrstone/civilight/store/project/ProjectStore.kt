package io.github.lenticualrstone.civilight.store.project

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ProjectAppState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.store.EditStamp
import io.github.lenticualrstone.civilight.store.ReadOnlyProjectException
import io.github.lenticualrstone.civilight.store.StoreEnvironment
import io.github.lenticualrstone.civilight.store.io.DebouncedSaver
import io.github.lenticualrstone.civilight.store.io.StoreDirectory
import io.github.lenticualrstone.civilight.store.version.StoreUpgradeChain
import io.github.lenticualrstone.civilight.store.version.StoreVersions
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Save progress of a store. */
sealed interface SaveState {
    /** Everything is on disk. */
    data object Saved : SaveState

    /** Changes wait for the debounce timer. */
    data object Pending : SaveState

    /** The last save failed; the changes stay in memory and the next save retries. */
    data class Failed(val message: String) : SaveState
}

/** Told about every change of a project's content (the library keeps its index in step). */
fun interface ProjectStoreListener {
    fun onContentChanged(key: Key, project: Project, at: Long)

    companion object {
        val None = ProjectStoreListener { _, _, _ -> }
    }
}

/**
 * The working store of one open project (§8.2): the whole project as one immutable [ProjectDoc]
 * in [doc]; edits are pure functions applied by [update]; saving writes only the file units that
 * changed, debounced (about 600 ms, at least every 3 s while editing), each save one crash-safe
 * transaction; [flush] saves at once (call it from `onPause`/`onStop`).
 *
 * Every content edit is one labelled undo step ([undo], [redo]); app-only state ([updateApp]) is
 * not. A read-only project (the example, LIB-17) rejects content edits.
 *
 * Get instances from `LibraryStore.open` (one per project); [open] is for tools and tests.
 */
class ProjectStore internal constructor(
    val key: Key,
    private val directory: StoreDirectory,
    loaded: LoadedProject,
    val readOnly: Boolean,
    private val env: StoreEnvironment,
    private val listener: ProjectStoreListener,
    private val storeVersion: Int,
) {
    private val _doc = MutableStateFlow(loaded.doc)
    private val _undo = MutableStateFlow(UndoStack(limit = env.timing.undoLimit, mergeWindowMs = env.timing.undoMergeWindowMs))
    private val _undoState = MutableStateFlow(UndoState())
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Saved)

    private val editing = Mutex()
    private var version = 0L
    private var savedVersion = 0L
    private var written: WrittenState = loaded.written
    @Volatile private var closed = false

    private val saver = DebouncedSaver(env.scope, env.timing.saveDebounceMs, env.timing.saveMaxDelayMs, ::save)

    /** The current project. */
    val doc: StateFlow<ProjectDoc> = _doc.asStateFlow()

    /** Labels and counts for undo and redo. */
    val undoState: StateFlow<UndoState> = _undoState.asStateFlow()

    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    /** What the load found (damaged files, upgrade, recovered save). */
    val loadReport: LoadReport = loaded.report

    /** The project directory. */
    val dir: File get() = directory.root

    internal val storeDirectory: StoreDirectory get() = directory

    /** `updatedAt`/`updatedBy` for an edit made now (§1.3). */
    fun stamp(): EditStamp = env.stamp()

    /**
     * Applies [transform] to the project as one undo step named [label] (the toast text of the
     * owning area). A transform that returns an equal document changes nothing and records no
     * step. Returns the new document.
     *
     * @throws ReadOnlyProjectException for a read-only project.
     */
    suspend fun update(label: String, transform: (ProjectDoc) -> ProjectDoc): ProjectDoc = update(label, null, transform)

    /**
     * [update] where edits with the same non-null [mergeKey] in quick succession (2 s) form one
     * undo step (typing in one block).
     */
    suspend fun update(label: String, mergeKey: String?, transform: (ProjectDoc) -> ProjectDoc): ProjectDoc {
        if (readOnly) throw ReadOnlyProjectException(key)
        return editing.withLock {
            checkOpen()
            val before = _doc.value
            val after = transform(before)
            require(after.project.key == key) { "an edit cannot change the project key" }
            if (after == before) return@withLock before
            val now = env.clock.now()
            _undo.value = _undo.value.record(label, before, mergeKey, now)
            commitInMemory(after, contentChanged = true)
            after
        }
    }

    /**
     * (0.2.0) A content write that is no undo step and that a later undo or redo does not revert: [transform]
     * also runs on every kept step ([UndoStack.patch]). For settings the spec keeps out of undo (`게임 제작 도구`,
     * `용어 후보`, `이 탭 잠그기`) and for what an export writes after its save (`내보낸 판`, fixed voice IDs).
     * Without [carry], [transform] itself runs on each kept step, so it must be a plain field write that makes
     * sense on an older document. A write derived from the current text (an export's `내보낸 판`) passes [carry]
     * instead: it gets each kept step with the live document before and after [transform], and carries only
     * that difference over. Returns the new document.
     *
     * @throws ReadOnlyProjectException for a read-only project.
     */
    suspend fun updateWithoutUndo(
        carry: ((step: ProjectDoc, before: ProjectDoc, after: ProjectDoc) -> ProjectDoc)? = null,
        transform: (ProjectDoc) -> ProjectDoc,
    ): ProjectDoc {
        if (readOnly) throw ReadOnlyProjectException(key)
        return editing.withLock {
            checkOpen()
            val before = _doc.value
            val after = transform(before)
            require(after.project.key == key) { "an edit cannot change the project key" }
            if (after == before) return@withLock before
            _undo.value = _undo.value.patch(if (carry == null) transform else { step -> carry(step, before, after) })
            commitInMemory(after, contentChanged = true)
            after
        }
    }

    /** Changes app-only state (`_app/…`: last position, lens, view state). No undo step; allowed when read-only. */
    suspend fun updateApp(transform: (ProjectAppState) -> ProjectAppState) {
        editing.withLock {
            checkOpen()
            val before = _doc.value
            val app = transform(before.app)
            if (app == before.app) return@withLock
            commitInMemory(before.copy(app = app), contentChanged = false)
        }
    }

    /** Undoes the last step; returns its label, or null when there was none. */
    suspend fun undo(): String? = step { stack, current -> stack.undo(current) }

    /** Redoes the last undone step; returns its label, or null when there was none. */
    suspend fun redo(): String? = step { stack, current -> stack.redo(current) }

    /** Forgets the undo history (e.g. after an operation that cannot be undone). */
    suspend fun clearUndo() {
        editing.withLock {
            _undo.value = _undo.value.clear()
            _undoState.value = _undo.value.state
        }
    }

    /** Writes pending changes now and waits until they are on disk. */
    suspend fun flush() = saver.flush()

    /** [flush] for callers that cannot suspend (`onStop`); blocks the calling thread. */
    fun flushBlocking() = runBlocking { flush() }

    /** Stops edits, saves what is pending and stops the timers; later edits fail. */
    suspend fun close() {
        editing.withLock { closed = true }
        flush()
        saver.cancel()
    }

    val isClosed: Boolean get() = closed

    private suspend fun step(move: (UndoStack, ProjectDoc) -> Pair<UndoStack, UndoEntry>?): String? {
        if (readOnly) throw ReadOnlyProjectException(key)
        return editing.withLock {
            checkOpen()
            val (stack, entry) = move(_undo.value, _doc.value) ?: return@withLock null
            _undo.value = stack
            commitInMemory(entry.doc, contentChanged = true)
            entry.label
        }
    }

    /** Call with [editing] held. */
    private fun commitInMemory(next: ProjectDoc, contentChanged: Boolean) {
        _doc.value = next
        _undoState.value = _undo.value.state
        version++
        _saveState.value = SaveState.Pending
        saver.markDirty()
        if (contentChanged) listener.onContentChanged(key, next.project, env.clock.now())
    }

    private suspend fun save() {
        val (snapshot, snapshotVersion) = editing.withLock { _doc.value to version }
        if (snapshotVersion == savedVersion) return
        try {
            withContext(env.io + NonCancellable) {
                val commit = ProjectWriter.plan(snapshot, written, storeVersion)
                if (!commit.isEmpty) directory.commit(commit.writes, commit.deletes)
                written = commit.next
            }
            editing.withLock {
                savedVersion = snapshotVersion
                if (version == savedVersion) _saveState.value = SaveState.Saved
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _saveState.value = SaveState.Failed(e.message ?: e.toString())
        }
    }

    private fun checkOpen() = check(!closed) { "project store $key is closed" }

    companion object {
        /**
         * Opens the project in [dir] (whose name is [key]). Interrupted saves are completed, old
         * store versions upgraded and damaged files set aside before the document is built.
         */
        suspend fun open(
            key: Key,
            dir: File,
            env: StoreEnvironment,
            readOnly: Boolean = false,
            listener: ProjectStoreListener = ProjectStoreListener.None,
            chain: StoreUpgradeChain = StoreVersions.project,
        ): ProjectStore {
            val directory = StoreDirectory(dir)
            val loaded = withContext(env.io) { ProjectLoader.load(key, directory, chain, env.clock, mutate = true) }
            return ProjectStore(key, directory, loaded, readOnly, env, listener, chain.current)
        }

        /** Reads the project in [dir] without changing anything on disk except finishing an interrupted save. */
        fun read(key: Key, dir: File, env: StoreEnvironment, chain: StoreUpgradeChain = StoreVersions.project): ProjectDoc =
            readWithReport(key, dir, env, chain).first

        /** [read] plus what the load found (damaged files are reported, not set aside). */
        fun readWithReport(key: Key, dir: File, env: StoreEnvironment, chain: StoreUpgradeChain = StoreVersions.project): Pair<ProjectDoc, LoadReport> =
            ProjectLoader.load(key, StoreDirectory(dir), chain, env.clock, mutate = false).let { it.doc to it.report }

        /** Writes [doc] as a complete project directory [dir] (which must not hold another project). */
        fun writeNew(doc: ProjectDoc, dir: File, chain: StoreUpgradeChain = StoreVersions.project) {
            val commit = ProjectWriter.all(doc, chain.current)
            StoreDirectory(dir).commit(commit.writes, commit.deletes)
        }
    }
}
