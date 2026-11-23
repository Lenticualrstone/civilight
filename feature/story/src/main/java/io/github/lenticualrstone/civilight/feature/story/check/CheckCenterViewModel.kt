package io.github.lenticualrstone.civilight.feature.story.check

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.checks.CheckInputs
import io.github.lenticualrstone.civilight.domain.checks.CheckReport
import io.github.lenticualrstone.civilight.domain.checks.WritingChecks
import io.github.lenticualrstone.civilight.feature.story.common.StoryServices
import io.github.lenticualrstone.civilight.feature.story.common.StoryViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 점검 센터 (STO-39 … STO-41): runs every registry check (`domain:checks` [ProjectChecks], a full term
 * candidate scan) off the main thread whenever the project, the hidden-item lock, the STO-42 switches
 * or the recorder library change. Results are never stored; only acknowledgements are (STO-41).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckCenterViewModel(
    projectKey: Key,
    services: StoryServices,
    settings: StateFlow<AppSettings>,
) : StoryViewModel(projectKey, services) {
    init {
        viewModelScope.launch { services.recorder.ensureLoaded() }
    }

    /** The inputs of the latest run (the row buttons re-check an item with the same inputs). */
    @Volatile
    var inputs: CheckInputs = CheckInputs(now = services.clock.now(), zone = services.clock.zone)
        private set

    val report: StateFlow<CheckReport?> =
        combine(state, services.unlock, settings, services.recorder.state) { load, unlock, s, recorder ->
            (load as? ProjectLoad.Ready)?.let { ready ->
                ready.doc to CheckInputs.of(
                    settings = s,
                    now = services.clock.now(),
                    zone = services.clock.zone,
                    hiddenUnlocked = !itemsLocked(unlock),
                    recorder = recorder.library,
                )
            }
        }
            .filterNotNull()
            .mapLatest { (doc, inputs) ->
                this.inputs = inputs
                // S-2 / D33: writing items only; voice, travel and candidate rows follow their gates.
                WritingChecks.run(doc, inputs)
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
