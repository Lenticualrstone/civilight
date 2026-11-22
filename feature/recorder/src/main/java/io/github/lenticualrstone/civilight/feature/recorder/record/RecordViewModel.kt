package io.github.lenticualrstone.civilight.feature.recorder.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureHubs
import io.github.lenticualrstone.civilight.feature.recorder.capture.RecorderServices
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import kotlinx.coroutines.launch

/**
 * One record screen entry (LRC-28): its [controller] lives as long as the entry. A text edit still
 * waiting for its autosave when the entry goes is written from the app scope, since the entry's
 * own scope ends with it.
 */
class RecordViewModel(deps: FeatureDeps, recordKey: Key) : ViewModel() {
    val services: RecorderServices = CaptureHubs.services(deps)
    private val appScope = deps.appScope

    val controller = RecordController(
        scope = viewModelScope,
        recorder = services.recorder,
        envSource = services.envSource,
        writer = services.writer,
        recordKey = recordKey,
        now = deps.clock::now,
        editContext = { services.context().recorder },
        unlockChanges = deps.unlock.state,
    )

    /** Writes a waiting edit now, in the app scope (leaving the app, opening a 넣기 or the `⋯` menu). */
    fun flushSoon() {
        appScope.launch { controller.flush() }
    }

    override fun onCleared() {
        appScope.launch { controller.flush() }
    }
}
