package io.github.lenticualrstone.civilight.feature.library.projectsettings

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.library.rating.Protection
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.project.ProjectViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 프로젝트 설정 of one project (LIB-25, LIB-69). Protection changes go through `ProtectionActions`. */
internal class ProjectSettingsViewModel(projectKey: Key, private val services: LibraryServices) :
    ProjectViewModel(projectKey, services.deps.projects) {

    val ui: StateFlow<ProjectSettingsUi?> = combine(state, services.deps.settings) { load, settings ->
        (load as? ProjectLoad.Ready)?.let { ProjectSettingsState.build(it.doc, it.readOnly, settings.ratingDefaults) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** One switch (LIB-25): hiding off asks first; the rating never changes. */
    fun switch(protection: Protection, on: Boolean) {
        val before = doc.value?.project?.protections ?: return
        viewModelScope.launch { services.protection.switch(projectKey, before, protection, on) }
    }
}
