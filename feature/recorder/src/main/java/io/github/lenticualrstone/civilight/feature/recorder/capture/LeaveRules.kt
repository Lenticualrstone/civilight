package io.github.lenticualrstone.civilight.feature.recorder.capture

import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.navigation.Recording
import kotlinx.coroutines.Job

/**
 * When a running or paused take is saved and the microphone released (U-13, LRC-49, D8, D32), as plain
 * Kotlin so every case has a JVM test (`Recorder020Test`).
 *
 * - Leaving the recorder inside the app (system back, another screen on top, the route popped): save as
 *   `정리 안 됨` and release.
 * - A configuration change (fold, rotation): nothing; the same capture is shown again.
 * - The app going to the background (home, another app, screen off), even when the system then tears the
 *   screen down: nothing; the take keeps recording with its notification (LRC-09, D32).
 * - The activity finishing, or the task removed from recents: save and release.
 */
object LeaveRules {
    /**
     * The `Recording` route left composition. [changingConfigurations] and [finishing] come from the
     * activity; [activityStarted] is the activity's own lifecycle (at least STARTED), not the entry's;
     * [backStack] is the stack after the change.
     */
    fun leaveOnDispose(
        changingConfigurations: Boolean,
        finishing: Boolean,
        activityStarted: Boolean,
        backStack: List<NavKey>,
    ): Boolean = when {
        changingConfigurations -> false
        finishing -> true
        !activityStarted -> false
        // Another `Recording` on top (the notification reopening it) shows the same capture again.
        else -> backStack.lastOrNull() !is Recording
    }

    /** The task was removed from recents while the service keeps the process (LRC-49): save and release, once. */
    fun onTaskRemoved(hub: CaptureHub): Job? = hub.leave()
}
