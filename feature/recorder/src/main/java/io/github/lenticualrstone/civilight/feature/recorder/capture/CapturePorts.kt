package io.github.lenticualrstone.civilight.feature.recorder.capture

import io.github.lenticualrstone.civilight.domain.trash.RecorderTrashState
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecordingSession
import io.github.lenticualrstone.civilight.model.recorder.TextMemoDraft
import io.github.lenticualrstone.civilight.store.ReadOnlyProjectException
import io.github.lenticualrstone.civilight.store.recorder.RecorderStore
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.model.asset.Blob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The recorder library as the capture writes it (core:store `RecorderStore` in the app, a fake in tests). */
interface RecorderPort {
    /** The library as it is now (empty until loaded). */
    fun current(): RecorderLibrary

    suspend fun ensureLoaded()

    /** Applies [transform] to the library; saved debounced. */
    suspend fun update(transform: (RecorderLibrary) -> RecorderLibrary): RecorderLibrary

    /** The recording in progress, written at once for crash recovery (null removes it). */
    suspend fun saveSession(session: RecordingSession?)

    /** The one text memo draft (null removes it). */
    suspend fun saveDraft(draft: TextMemoDraft?)

    suspend fun readDraft(): TextMemoDraft?

    /** The library as it changes (the inbox redraws from it). */
    fun observe(): Flow<RecorderLibrary>

    /** The library and the recorder trash rows in one write (`지우기` / `되살리기`, DECISIONS §1 `RecorderTrash`). */
    suspend fun updateTrash(transform: (RecorderTrashState) -> RecorderTrashState)

    /** The recording that was running when the process ended (LRC-09 recovery), or null. */
    suspend fun readSession(): RecordingSession?

    /** Keeps a take's audio in the blob store (LRC-14, content-addressed); null when it could not be stored. */
    suspend fun importAudio(path: String, mime: String, durationMs: Long): Blob?

    /** The kept audio file of [hash], or null when it is not stored. */
    fun audioFile(hash: String): File?
}

/** Project documents a capture writes into (`여기 넣기`). */
interface ProjectPort {
    /**
     * Applies [transform] to project [projectKey] as one undo step named [label] and returns its
     * second value; null when the project is read-only or cannot be opened.
     */
    suspend fun <T> edit(projectKey: Key, label: String, transform: (ProjectDoc) -> Pair<ProjectDoc, T>): T?
}

class StoreRecorderPort(private val store: RecorderStore) : RecorderPort {
    override fun current(): RecorderLibrary = store.state.value.library

    override suspend fun ensureLoaded() = store.ensureLoaded()

    override suspend fun update(transform: (RecorderLibrary) -> RecorderLibrary): RecorderLibrary = store.updateLibrary(transform)

    override suspend fun saveSession(session: RecordingSession?) = store.saveSession(session)

    override suspend fun saveDraft(draft: TextMemoDraft?) = store.saveDraft(draft)

    override suspend fun readDraft(): TextMemoDraft? = store.readDraft()

    override fun observe(): Flow<RecorderLibrary> = store.state.map { it.library }.distinctUntilChanged()

    override suspend fun updateTrash(transform: (RecorderTrashState) -> RecorderTrashState) {
        store.ensureLoaded()
        store.update { s ->
            val next = transform(RecorderTrashState(s.library, s.trash.entries))
            s.copy(library = next.library, trash = s.trash.copy(entries = next.entries))
        }
    }

    override suspend fun readSession(): RecordingSession? = store.readSession()

    override suspend fun importAudio(path: String, mime: String, durationMs: Long): Blob? = try {
        withContext(Dispatchers.IO) { File(path).inputStream().use { store.importAudio(it, mime, durationMs) } }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    override fun audioFile(hash: String): File? = store.audioFile(hash)
}

/** [ProjectPort] over the shared project sessions (the store is shared with the screens of that project). */
class SessionsProjectPort(private val sessions: ProjectSessions) : ProjectPort {
    override suspend fun <T> edit(projectKey: Key, label: String, transform: (ProjectDoc) -> Pair<ProjectDoc, T>): T? {
        val store = try {
            sessions.acquire(projectKey)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        }
        try {
            if (store.readOnly) return null
            var result: T? = null
            store.update(label) { doc ->
                val (next, value) = transform(doc)
                result = value
                next
            }
            return result
        } catch (e: ReadOnlyProjectException) {
            return null
        } finally {
            sessions.release(projectKey)
        }
    }
}
