package io.github.lenticualrstone.civilight.feature.story.home

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.feature.story.common.StoryServices
import io.github.lenticualrstone.civilight.feature.story.common.StoryViewModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 프로젝트 홈 (STO-01): the derived [HomeUi], rebuilt when the project, the hidden-item lock or the
 * recorder library (linked records of `시작해 볼 것`) changes.
 */
class HomeViewModel(projectKey: Key, services: StoryServices) : StoryViewModel(projectKey, services) {
    init {
        viewModelScope.launch { services.recorder.ensureLoaded() }
    }

    val ui: StateFlow<HomeUi?> = combine(state, services.unlock, services.recorder.state) { load, unlock, recorder ->
        (load as? ProjectLoad.Ready)?.let {
            HomeModel.build(it.doc, it.readOnly, itemsLocked(unlock), services.clock.today(), services.clock.zone)
                .copy(start = HomeStartModel.build(it.doc, recorder.library, it.readOnly))
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** `목록 닫기` (LIB-13): app state only, so it also works without an undo step. */
    fun dismissStart(): Job = viewModelScope.launch { updateApp(HomeStartModel::dismiss) }
}
