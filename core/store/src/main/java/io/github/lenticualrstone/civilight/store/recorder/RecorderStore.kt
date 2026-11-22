package io.github.lenticualrstone.civilight.store.recorder

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.file.RecorderFile
import io.github.lenticualrstone.civilight.model.file.TrashIndexFile
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.recorder.RecordingSession
import io.github.lenticualrstone.civilight.model.recorder.TextMemoDraft
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.store.StoreEnvironment
import io.github.lenticualrstone.civilight.store.blob.BlobStore
import io.github.lenticualrstone.civilight.store.io.AtomicFiles
import io.github.lenticualrstone.civilight.store.io.DebouncedSaver
import io.github.lenticualrstone.civilight.store.io.StoreDirectory
import io.github.lenticualrstone.civilight.store.version.StoreUpgradeChain
import io.github.lenticualrstone.civilight.store.version.StoreVersionFile
import io.github.lenticualrstone.civilight.store.version.StoreVersions
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import java.io.File
import java.io.InputStream
import java.time.LocalDate

/** The recorder library plus the trash rows of recorder records (kind `recorder`, no project). */
data class RecorderState(
    val library: RecorderLibrary = RecorderLibrary(),
    val trash: TrashIndexFile = TrashIndexFile(),
    val loaded: Boolean = false,
)

/**
 * The global Life Recorder library (class R, §2.12, §8.2): `files/recorder/records.json` holds
 * every record (trashed ones too) with all its links and placements (each with its project key),
 * `trash.json` the recorder trash rows. Kept audio lives in the [BlobStore] by hash.
 * `session.json` (a recording in progress) and `draft.json` (the text memo draft) are crash
 * recovery files written at once.
 */
class RecorderStore(
    val dir: File,
    private val env: StoreEnvironment,
    private val blobs: BlobStore,
    private val chain: StoreUpgradeChain = StoreVersions.recorder,
) {
    private val directory = StoreDirectory(dir)
    private val _state = MutableStateFlow(RecorderState())
    private val loadLock = Mutex()
    private val editing = Mutex()
    private var version = 0L
    private var savedVersion = 0L
    private var writtenLibrary: RecorderLibrary? = null
    private var writtenTrash: TrashIndexFile? = null
    private val saver = DebouncedSaver(env.scope, env.timing.saveDebounceMs, env.timing.saveMaxDelayMs, ::save)

    /** Records, links, placements and recorder trash rows. Empty until [ensureLoaded] finished. */
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    /** Files that could not be read and were set aside (name → reason). */
    var damaged: Map<String, String> = emptyMap()
        private set

    /** True while a damaged recorder file that was set aside exists: its records may still name audio blobs. */
    fun hasSetAsideFiles(): Boolean = dir.list()?.any { DAMAGED_MARK in it } == true

    suspend fun ensureLoaded() {
        if (_state.value.loaded) return
        loadLock.withLock {
            if (_state.value.loaded) return
            val loaded = withContext(env.io) { readFromDisk() }
            editing.withLock { _state.value = loaded }
        }
    }

    /** Applies [transform] to the library and its trash rows; saved debounced. */
    suspend fun update(transform: (RecorderState) -> RecorderState): RecorderState {
        ensureLoaded()
        return editing.withLock {
            val before = _state.value
            val after = transform(before).copy(loaded = true)
            if (after != before) {
                _state.value = after
                version++
                saver.markDirty()
            }
            after
        }
    }

    /** Applies [transform] to the library only. */
    suspend fun updateLibrary(transform: (RecorderLibrary) -> RecorderLibrary): RecorderLibrary =
        update { it.copy(library = transform(it.library)) }.library

    /** Stores recorded audio in the blob store (source `recorder`); the caller puts its hash on the record. */
    suspend fun importAudio(input: InputStream, mime: String, durationMs: Long?): Blob =
        blobs.import(input, mime, BlobSource.RECORDER, durationMs = durationMs)

    /** The audio file of [hash], or null when it is not kept. */
    fun audioFile(hash: String): File? = blobs.fileOf(hash)

    // ---- trash ----

    /** Moves a record to the trash (filter `레코더`); its links and placements stay with it. */
    suspend fun moveToTrash(recordKey: Key): TrashEntry {
        var row: TrashEntry? = null
        update { s ->
            val record = requireNotNull(s.library.record(recordKey)) { "no record $recordKey" }
            check(record.deletedAt == null) { "record $recordKey is in the trash" }
            val now = env.clock.now()
            val entry = TrashEntry(
                key = Keys.newKey(), kind = TrashKind.RECORDER.open(), projectKey = null, targetKeys = listOf(recordKey),
                hiddenAtDelete = record.hidden, deletedAt = now, purgeOn = TrashEntry.purgeDateFor(env.clock.localDateOf(now)),
            )
            row = entry
            s.copy(
                library = s.library.copy(records = s.library.records.map {
                    if (it.key == recordKey) it.copy(deletedAt = now, trashEntryKey = entry.key) else it
                }),
                trash = s.trash.copy(entries = s.trash.entries + entry),
            )
        }
        return row!!
    }

    /** Brings a trashed record back. */
    suspend fun restore(recordKey: Key) {
        update { s ->
            s.copy(
                library = s.library.copy(records = s.library.records.map {
                    if (it.key == recordKey) it.copy(deletedAt = null, trashEntryKey = null) else it
                }),
                trash = s.trash.copy(entries = s.trash.entries.filter { recordKey !in it.targetKeys }),
            )
        }
    }

    /** Deletes a record for good with its links and placements (copies in projects keep their text). */
    suspend fun purge(recordKey: Key) {
        update { s ->
            s.copy(
                library = RecorderLibrary(
                    records = s.library.records.filter { it.key != recordKey },
                    links = s.library.links.filter { it.recordKey != recordKey },
                    placements = s.library.placements.filter { it.recordKey != recordKey },
                ),
                trash = s.trash.copy(entries = s.trash.entries.filter { recordKey !in it.targetKeys }),
            )
        }
    }

    /** Purges trashed records whose purge date has come; returns their keys. */
    suspend fun purgeExpired(today: LocalDate = env.clock.today()): List<Key> {
        ensureLoaded()
        val due = _state.value.trash.entries.filter { !today.isBefore(it.purgeOn) }.flatMap { it.targetKeys }
        for (key in due) purge(key)
        return due
    }

    // ---- links into projects ----

    /**
     * A project was purged: its links and placements keep their last label and get `purgedAt`
     * (§5.3). Project links get the project's name as label (masked when [hidden]).
     */
    suspend fun markProjectPurged(projectKey: Key, projectName: String, hidden: Boolean) {
        val now = env.clock.now()
        update { s ->
            s.copy(library = s.library.copy(
                links = s.library.links.map { link ->
                    if (link.projectKey != projectKey || link.purgedAt != null) link
                    else link.copy(
                        purgedAt = now,
                        lastLabel = link.lastLabel ?: projectName.takeIf { link.targetKind isOf RecorderTargetKind.PROJECT },
                        lastLabelHidden = link.lastLabelHidden || hidden,
                    )
                },
                placements = s.library.placements.map { p ->
                    if (p.projectKey != projectKey || p.purgedAt != null) p else p.copy(purgedAt = now)
                },
            ))
        }
    }

    /** Removes every link and placement into [projectKey] (the example project, LRC-23). */
    suspend fun removeProjectLinks(projectKey: Key) {
        update { s ->
            s.copy(library = s.library.copy(
                links = s.library.links.filter { it.projectKey != projectKey },
                placements = s.library.placements.filter { it.projectKey != projectKey },
            ))
        }
    }

    // ---- crash recovery files ----

    /** Writes (or with null removes) the recording in progress, at once. */
    suspend fun saveSession(session: RecordingSession?) = writeSmall(SESSION, session, RecordingSession.serializer())

    suspend fun readSession(): RecordingSession? = readSmall(SESSION, RecordingSession.serializer())

    /** Writes (or with null removes) the text memo draft, at once. */
    suspend fun saveDraft(draft: TextMemoDraft?) = writeSmall(DRAFT, draft, TextMemoDraft.serializer())

    suspend fun readDraft(): TextMemoDraft? = readSmall(DRAFT, TextMemoDraft.serializer())

    private suspend fun <T> writeSmall(path: String, value: T?, serializer: KSerializer<T>) = withContext(env.io + NonCancellable) {
        if (value == null) directory.delete(path)
        else directory.write(path, CivilightJson.encode(serializer, value).toByteArray())
    }

    private suspend fun <T> readSmall(path: String, serializer: KSerializer<T>): T? = withContext(env.io) {
        directory.readText(path)?.let { text -> runCatching { CivilightJson.decode(serializer, text) }.getOrNull() }
    }

    // ---- saving ----

    suspend fun flush() = saver.flush()

    private suspend fun save() {
        val (snapshot, snapshotVersion) = editing.withLock { _state.value to version }
        if (!snapshot.loaded || snapshotVersion == savedVersion) return
        withContext(env.io + NonCancellable) {
            val writes = LinkedHashMap<String, ByteArray>()
            if (snapshot.library != writtenLibrary) {
                writes[RECORDS] = CivilightJson.encode(RecorderFile.serializer(), RecorderFile.library(snapshot.library, includeTrashed = true)).toByteArray()
            }
            if (snapshot.trash != writtenTrash) {
                writes[TRASH] = CivilightJson.encode(TrashIndexFile.serializer(), snapshot.trash).toByteArray()
            }
            if (!directory.exists(STORE_META)) {
                writes[STORE_META] = CivilightJson.encode(StoreVersionFile.serializer(), StoreVersionFile(chain.current)).toByteArray()
            }
            directory.commit(writes)
            writtenLibrary = snapshot.library
            writtenTrash = snapshot.trash
        }
        editing.withLock { savedVersion = snapshotVersion }
    }

    private fun readFromDisk(): RecorderState {
        dir.mkdirs()
        directory.recover(recursive = false)
        val version = directory.readText(STORE_META)
            ?.let { runCatching { CivilightJson.decode(StoreVersionFile.serializer(), it).storeVersion }.getOrNull() }
            ?: 1
        val upgrade = chain.needsUpgrade(version)
        val problems = LinkedHashMap<String, String>()
        val json = LinkedHashMap<String, JsonElement>()
        for (path in listOf(RECORDS, TRASH)) {
            val text = directory.readText(path) ?: continue
            val element = runCatching { CivilightJson.json.parseToJsonElement(text) }.getOrNull()
            if (element == null) setAside(path, "not JSON", problems) else json[path] = element
        }
        val files = if (upgrade) chain.upgrade(version, json) else json
        if (upgrade) {
            val writes = files.mapValues { CivilightJson.json.encodeToString(JsonElement.serializer(), it.value).toByteArray() } +
                (STORE_META to CivilightJson.encode(StoreVersionFile.serializer(), StoreVersionFile(chain.current)).toByteArray())
            directory.commit(writes, json.keys.filter { it !in files })
        }
        val library = files[RECORDS]?.let { element ->
            try {
                CivilightJson.decodeElement(RecorderFile.serializer(), element).toLibrary()
            } catch (e: IllegalArgumentException) {
                setAside(RECORDS, e.message ?: "unreadable", problems)
                null
            }
        }
        val trash = files[TRASH]?.let { element ->
            try {
                CivilightJson.decodeElement(TrashIndexFile.serializer(), element)
            } catch (e: IllegalArgumentException) {
                setAside(TRASH, e.message ?: "unreadable", problems)
                null
            }
        }
        damaged = problems
        writtenLibrary = library ?: RecorderLibrary().takeIf { RECORDS !in files }
        writtenTrash = trash ?: TrashIndexFile().takeIf { TRASH !in files }
        return RecorderState(library ?: RecorderLibrary(), trash ?: TrashIndexFile(), loaded = true)
    }

    private fun setAside(path: String, reason: String, problems: MutableMap<String, String>) {
        val from = directory.file(path)
        if (from.isFile) AtomicFiles.moveAtomically(from, File(from.parentFile, "${from.name}$DAMAGED_MARK${env.clock.now()}"))
        problems[path] = reason
    }

    companion object {
        const val RECORDS = "records.json"
        const val TRASH = "trash.json"
        const val STORE_META = "store.json"
        const val SESSION = "session.json"
        const val DRAFT = "draft.json"
        const val DAMAGED_MARK = ".damaged-"
    }
}
