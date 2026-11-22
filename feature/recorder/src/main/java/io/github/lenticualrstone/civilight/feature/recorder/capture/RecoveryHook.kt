package io.github.lenticualrstone.civilight.feature.recorder.capture

import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.StartupHook

/**
 * LRC-09 recovery at start-up (`[PROPOSED]`): the process ended while a take was recording, paused or
 * in `정리 중`. The shell runs this once per process after the app lock; all I/O happens here. The take
 * is saved with what was captured and `정리 중` opens over the start screen. When the restored back
 * stack still holds a `Recording` entry, that entry shows the recovered capture and no second one
 * is pushed.
 */
object RecoveryHook : StartupHook {
    const val ID = "recorder.resume"

    override suspend fun run(deps: FeatureDeps): Route? {
        val services = CaptureHubs.services(deps)
        val saved = services.recorder.readSession()
        // Takes nobody brought back (a `취소` never undone) go; the cut-off one is kept for recovery.
        services.hub.pruneTakes(saved?.recordKey)
        if (saved == null) return null
        val entry = services.hub.recover(saved) ?: return null
        if (deps.navigator.backStack.any { it is Recording }) return null
        return EntryRoutes.routeOf(entry)
    }
}
