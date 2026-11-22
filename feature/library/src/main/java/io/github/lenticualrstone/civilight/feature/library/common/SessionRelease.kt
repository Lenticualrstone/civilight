package io.github.lenticualrstone.civilight.feature.library.common

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import kotlinx.coroutines.delay

/**
 * Waits for a project's shared store to be let go: screens release their sessions when their
 * entries leave the stack (a frame later), and the session cache keeps a closed-over store for 15 s
 * unless [ProjectSessions.closeIdle] drops it (hazard of batch 2). Gives up after about 2 s.
 */
object SessionRelease {
    private const val TRIES = 20
    private const val WAIT_MS = 100L

    suspend fun await(projects: ProjectSessions, projectKey: Key) {
        repeat(TRIES) {
            projects.closeIdle()
            if (projects.peek(projectKey) == null) return
            delay(WAIT_MS)
        }
    }
}
