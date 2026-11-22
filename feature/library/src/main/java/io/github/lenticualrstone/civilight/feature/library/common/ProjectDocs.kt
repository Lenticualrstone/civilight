package io.github.lenticualrstone.civilight.feature.library.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import kotlinx.coroutines.CancellationException

/**
 * The live document of [projectKey] while the calling composable (a sheet opened over any screen)
 * is shown: the shared store is acquired on entry and released when it leaves. Null while loading
 * or when the project cannot be opened.
 */
@Composable
internal fun rememberProjectDoc(sessions: ProjectSessions, projectKey: Key): State<ProjectDoc?> =
    produceState<ProjectDoc?>(initialValue = sessions.peekDoc(projectKey), sessions, projectKey) {
        val store = try {
            sessions.acquire(projectKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            value = null
            return@produceState
        }
        try {
            store.doc.collect { value = it }
        } finally {
            sessions.release(projectKey)
        }
    }
