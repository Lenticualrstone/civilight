package io.github.lenticualrstone.civilight.feature.script.assets

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.project.ProjectViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/**
 * `에셋` (SCR-61): the list follows the project's document, the chip and the typed search. Writes go
 * through the asset form sheet (`AssetFormSheet`, one undo step each); this view model only reads.
 */
class AssetsViewModel(projectKey: Key, @Suppress("unused") private val deps: FeatureDeps) : ProjectViewModel(projectKey, deps.projects) {
    private val filterFlow = MutableStateFlow(AssetFilter.ALL)
    private val queryFlow = MutableStateFlow("")

    val filter: StateFlow<AssetFilter> = filterFlow.asStateFlow()
    val query: StateFlow<String> = queryFlow.asStateFlow()

    val ui: StateFlow<AssetsUi?> = combine(state, filterFlow, queryFlow) { load, filter, query ->
        (load as? ProjectLoad.Ready)?.let { r ->
            AssetsModel.of(r.doc, filter, query, AssetsModel.currentEpisode(r.doc), r.readOnly)
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setFilter(filter: AssetFilter) {
        filterFlow.value = filter
    }

    fun setQuery(text: String) {
        queryFlow.value = text
    }
}
