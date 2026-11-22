package io.github.lenticualrstone.civilight.feature.map.list

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.feature.map.common.MapEnv
import io.github.lenticualrstone.civilight.feature.map.common.MapScreenViewModel
import io.github.lenticualrstone.civilight.model.Key
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * 지도 목록 (WLD-36). New maps (WLD-38, WLD-39, WLD-50) are made through the base class
 * ([MapScreenViewModel.createMap] and friends), shared with a pin's `하위 지도 · 만들기`.
 */
class MapListViewModel(projectKey: Key, env: MapEnv) : MapScreenViewModel(projectKey, env) {
    val ui: StateFlow<MapListUi?> = combine(doc, hiddenOpen) { d, open ->
        d?.let { MapListState.build(it, lensOf(it), open) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
