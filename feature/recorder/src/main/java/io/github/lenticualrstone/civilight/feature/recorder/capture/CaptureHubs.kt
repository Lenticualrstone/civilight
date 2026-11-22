package io.github.lenticualrstone.civilight.feature.recorder.capture

import android.os.SystemClock
import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.feature.recorder.audio.MediaPlayback
import io.github.lenticualrstone.civilight.feature.recorder.audio.MediaRecorderTakes
import io.github.lenticualrstone.civilight.feature.recorder.audio.RecordPlayer
import io.github.lenticualrstone.civilight.feature.recorder.audio.TakeFiles
import io.github.lenticualrstone.civilight.feature.recorder.env.EnvSource
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import io.github.lenticualrstone.civilight.feature.recorder.env.StoreEnvSource
import io.github.lenticualrstone.civilight.feature.recorder.record.RecordWriter
import io.github.lenticualrstone.civilight.feature.recorder.speech.SpeechEngines
import io.github.lenticualrstone.civilight.store.StoreContainer
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import java.io.File

/**
 * The recorder's process-wide parts, made once from the deps (the store container, sessions, clock
 * and unlock controller all live as long as the process): one [EnvSource] (one document cache), the
 * ports, the capture [hub] and the [writer] for saved records.
 */
class RecorderServices(
    val recorder: RecorderPort,
    val projects: ProjectPort,
    val envSource: EnvSource,
    val context: () -> ActionContext,
    val hub: CaptureHub,
    /** The one player of kept audio (LRC-14); stopped when a recording starts and when hidden records lock again. */
    val player: RecordPlayer,
) {
    val writer: RecordWriter = RecordWriter(recorder, projects, envSource, context)
}

/**
 * The process's [CaptureHub] and [RecorderServices], made on first use from a screen, sheet or
 * start-up hook (never from the entries function, which must start no work). Keyed by the store
 * container, which lives as long as the process.
 */
object CaptureHubs {
    private var services: RecorderServices? = null
    private var owner: StoreContainer? = null

    fun of(deps: FeatureDeps): CaptureHub = services(deps).hub

    fun services(deps: FeatureDeps): RecorderServices = synchronized(this) {
        services?.takeIf { owner === deps.stores } ?: create(deps).also {
            services = it
            owner = deps.stores
        }
    }

    private fun create(deps: FeatureDeps): RecorderServices {
        val stores = deps.stores
        val takesDir = File(stores.filesDir, "${StoreContainer.RECORDER_DIR}/takes")
        val recorder = StoreRecorderPort(stores.recorder)
        val envSource = StoreEnvSource(stores.library, deps.projects, { deps.unlock.state.value }, { deps.clock.zone })
        val projects = SessionsProjectPort(deps.projects)
        val context = { ActionContext(now = deps.clock.now(), zone = deps.clock.zone, device = stores.deviceId.value) }
        val hub = CaptureHub(
            scope = deps.appScope,
            recorder = recorder,
            envSource = envSource,
            projects = projects,
            settings = { deps.settings.value },
            context = context,
            monotonicMs = SystemClock::elapsedRealtime,
            // The growing audio file of a take (LRC-08, LRC-14).
            audioTempPath = { key -> TakeFiles.take(takesDir, key).path },
            engineFactory = { mode -> SpeechEngines.factory(mode) },
            takes = MediaRecorderTakes(takesDir),
        )
        val player = RecordPlayer(deps.appScope, MediaPlayback()) { hash -> recorder.audioFile(hash)?.path }
        deps.appScope.launch {
            hub.state.map { it.stage == CaptureStage.RECORDING }.distinctUntilChanged().collect { if (it) player.stop() }
        }
        deps.appScope.launch {
            deps.unlock.state.map { it.recordsUnlockedAt != null }.distinctUntilChanged().collect { open -> if (!open) player.stop() }
        }
        deps.appScope.launch {
            // A record sent to the trash (or purged) while it plays stops (LRC-43).
            recorder.observe().collect { player.follow(it) }
        }
        return RecorderServices(recorder, projects, envSource, context, hub, player)
    }
}
