package io.github.lenticualrstone.civilight.feature.recorder.capture

import android.os.SystemClock
import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.feature.recorder.env.StoreEnvSource
import io.github.lenticualrstone.civilight.feature.recorder.speech.SpeechEngines
import io.github.lenticualrstone.civilight.store.StoreContainer
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import java.io.File

/**
 * The process's [CaptureHub], made on first use from a screen or sheet (never from the entries
 * function, which must start no work). Keyed by the store container, which lives as long as the
 * process; the deps' clock, settings, sessions and unlock controller are process-wide too.
 */
object CaptureHubs {
    private var hub: CaptureHub? = null
    private var owner: StoreContainer? = null

    fun of(deps: FeatureDeps): CaptureHub = synchronized(this) {
        hub?.takeIf { owner === deps.stores } ?: create(deps).also {
            hub = it
            owner = deps.stores
        }
    }

    private fun create(deps: FeatureDeps): CaptureHub {
        val stores = deps.stores
        val takes = File(stores.filesDir, "${StoreContainer.RECORDER_DIR}/takes")
        return CaptureHub(
            scope = deps.appScope,
            recorder = StoreRecorderPort(stores.recorder),
            envSource = StoreEnvSource(stores.library, deps.projects, { deps.unlock.state.value }, { deps.clock.zone }),
            projects = SessionsProjectPort(deps.projects),
            settings = { deps.settings.value },
            context = { ActionContext(now = deps.clock.now(), zone = deps.clock.zone, device = stores.deviceId.value) },
            monotonicMs = SystemClock::elapsedRealtime,
            // The growing audio file of a take (LRC-08, LRC-14); written once audio capture lands.
            audioTempPath = { key -> File(takes, "$key.m4a").path },
            engineFactory = SpeechEngines.factory,
        )
    }
}
