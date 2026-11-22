package io.github.lenticualrstone.civilight.feature.recorder.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureHubs
import io.github.lenticualrstone.civilight.feature.recorder.capture.RecorderServices
import io.github.lenticualrstone.civilight.navigation.RecorderInbox
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps

/** One 기록함 entry: its [controller] lives as long as the entry (its filter is this entry's own). */
class InboxViewModel(deps: FeatureDeps, route: RecorderInbox) : ViewModel() {
    val services: RecorderServices = CaptureHubs.services(deps)

    val controller = InboxController(
        scope = viewModelScope,
        recorder = services.recorder,
        envSource = services.envSource,
        writer = services.writer,
        now = deps.clock::now,
        relockPolicy = { deps.settings.value.relockPolicy },
        unlockChanges = deps.unlock.state,
        preset = InboxRoutes.preset(route),
    )
}
