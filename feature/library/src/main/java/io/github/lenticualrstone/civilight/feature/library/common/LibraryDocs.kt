package io.github.lenticualrstone.civilight.feature.library.common

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.store.library.LibraryState
import io.github.lenticualrstone.civilight.store.library.LibraryStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Documents of every live project for screens that list projects (서재 meta, 설정 · 수위 등급,
 * `다른 프로젝트에서`). Read with `LibraryStore.readDoc` (the open store's document when a project is
 * open, else from disk), never opening a store, and read again only when the index row's
 * `updatedAt` moves. A document that cannot be read maps to null.
 */
internal class LibraryDocs(private val library: LibraryStore, scope: CoroutineScope) {
    private data class Cached(val updatedAt: Timestamp, val doc: ProjectDoc?)

    private val cache = MutableStateFlow<Map<Key, Cached>>(emptyMap())

    val docs: StateFlow<Map<Key, ProjectDoc?>> =
        cache.map { c -> c.mapValues { it.value.doc } }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    init {
        scope.launch {
            library.ensureLoaded()
            library.state.collect(::refresh)
        }
    }

    private suspend fun refresh(state: LibraryState) {
        if (!state.loaded) return
        val live = state.live
        val liveKeys = live.mapTo(HashSet()) { it.key }
        cache.update { current -> current.filterKeys { it in liveKeys } }
        for (entry in live) {
            if (cache.value[entry.key]?.updatedAt == entry.updatedAt) continue
            val doc = try {
                library.readDoc(entry.key)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            cache.update { it + (entry.key to Cached(entry.updatedAt, doc)) }
        }
    }
}
