package io.github.lenticualrstone.civilight.feature.recorder.env

import io.github.lenticualrstone.civilight.domain.recorder.ProjectSnapshot
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.app.UnlockSession
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.store.library.LibraryStore
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import io.github.lenticualrstone.civilight.ui.security.UnlockState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

/**
 * Where the recorder's [RecorderEnv] comes from. `RecorderEnv` treats a project it does not list
 * as permanently deleted, so [load] lists every 서재 row (live and trashed) with its document.
 */
interface EnvSource {
    /** Every project with its document (open documents as they are now, others read from disk). */
    suspend fun load(): RecorderEnv

    /** Without I/O: only the projects open right now (enough for the entry project at start). */
    fun quick(): RecorderEnv

    /** [projectKey] was written by the recorder: its cached document is dropped, so the next [load] reads it again. */
    fun forget(projectKey: Key) = Unit
}

/** Pure assembly of a [RecorderEnv] from the 서재 rows, documents and unlock state. */
object RecorderEnvs {
    /**
     * Live rows in 서재 order (last modified first), then trashed rows; a row without a document
     * is left out. The most recently opened live project leads the picker's search order.
     */
    fun build(entries: List<LibraryEntry>, docs: Map<Key, ProjectDoc>, unlock: UnlockState, zone: ZoneId): RecorderEnv {
        val live = entries.filter { it.deletedAt == null }.sortedByDescending { it.updatedAt }
        val trashed = entries.filter { it.deletedAt != null }.sortedByDescending { it.deletedAt }
        val projects = (live + trashed).mapNotNull { entry -> docs[entry.key]?.let { ProjectSnapshot(entry, it) } }
        val recent = live.filter { it.lastOpenedAt != null }.maxByOrNull { it.lastOpenedAt!! }?.key
        return RecorderEnv(projects = projects, unlock = session(unlock), zone = zone, recentProjectKey = recent)
    }

    /** The shell's unlock state as the model's `UnlockSession`. */
    fun session(unlock: UnlockState): UnlockSession = UnlockSession(
        appUnlocked = !unlock.appLocked,
        hiddenGroupUnlockedAt = unlock.hiddenGroupUnlockedAt,
        itemsUnlocked = unlock.itemsUnlockedAt,
        recordsUnlockedAt = unlock.recordsUnlockedAt,
    )
}

/** [EnvSource] over the library store and the open project sessions. Closed documents are cached by their row's time. */
class StoreEnvSource(
    private val library: LibraryStore,
    private val sessions: ProjectSessions,
    private val unlock: () -> UnlockState,
    private val zone: () -> ZoneId,
) : EnvSource {
    private val lock = Mutex()
    private val cache = ConcurrentHashMap<Key, Pair<Long, ProjectDoc>>()

    override suspend fun load(): RecorderEnv {
        library.ensureLoaded()
        val entries = library.state.value.entries
        val docs = HashMap<Key, ProjectDoc>()
        lock.withLock {
            for (entry in entries) {
                val stamp = entry.deletedAt ?: entry.updatedAt
                val doc = sessions.peekDoc(entry.key)
                    ?: cache[entry.key]?.takeIf { it.first == stamp }?.second
                    ?: read(entry.key)?.also { cache[entry.key] = stamp to it }
                if (doc != null) docs[entry.key] = doc
            }
            cache.keys.retainAll(entries.map { it.key }.toSet())
        }
        return RecorderEnvs.build(entries, docs, unlock(), zone())
    }

    override fun forget(projectKey: Key) {
        cache.remove(projectKey)
    }

    override fun quick(): RecorderEnv {
        val entries = library.state.value.entries
        val docs = entries.mapNotNull { e -> (sessions.peekDoc(e.key) ?: cache[e.key]?.second)?.let { e.key to it } }.toMap()
        return RecorderEnvs.build(entries, docs, unlock(), zone())
    }

    private suspend fun read(key: Key): ProjectDoc? = try {
        library.readDoc(key)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
