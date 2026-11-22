package io.github.lenticualrstone.civilight.ui.project

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.store.library.LibraryStore
import io.github.lenticualrstone.civilight.store.project.ProjectStore
import kotlinx.coroutines.CoroutineScope

/**
 * The open [ProjectStore]s of the app, shared by every screen and view model of a project. Always
 * go through this (never `LibraryStore.open` / `close` directly): the store is opened once, kept
 * while any screen of the project is on the back stack, and closed a little after the last one left.
 */
class ProjectSessions(
    scope: CoroutineScope,
    private val library: LibraryStore,
    closeDelayMs: Long = RefCountedSessions.DEFAULT_CLOSE_DELAY_MS,
) {
    private val sessions = RefCountedSessions<Key, ProjectStore>(
        scope = scope,
        open = { key -> library.open(key) },
        close = { key, _ -> library.close(key) },
        closeDelayMs = closeDelayMs,
    )

    /** Opens or shares the project's store. Throws `NoSuchProjectException` / `ProjectLoadException`. */
    suspend fun acquire(projectKey: Key): ProjectStore = sessions.acquire(projectKey)

    /** Call once per successful [acquire]. */
    fun release(projectKey: Key) = sessions.release(projectKey)

    /** The store if it is open now. */
    fun peek(projectKey: Key): ProjectStore? = sessions.peek(projectKey)

    /** The current document if the project is open now. */
    fun peekDoc(projectKey: Key): ProjectDoc? = peek(projectKey)?.doc?.value

    suspend fun closeIdle() = sessions.closeIdle()
}
