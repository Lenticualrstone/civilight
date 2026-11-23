package io.github.lenticualrstone.civilight.feature.library.projectsettings

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.library.rating.Protection
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.common.actionContext
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
        (load as? ProjectLoad.Ready)?.let { ProjectSettingsState.build(it.doc, it.readOnly, settings.ratingDefaults, services.deps.stores.blobs::contains) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** One switch (LIB-25): hiding off asks first; the rating never changes. */
    fun switch(protection: Protection, on: Boolean) {
        val before = doc.value?.project?.protections ?: return
        viewModelScope.launch { services.report(services.protection.switch(projectKey, before, protection, on)) }
    }

    /**
     * `무대 위치` · `보이스 ID` (SCR-24, SCR-25): one undo step named after the row, like the other switches of
     * this screen. (0.2.0) `게임 제작 도구` (LIB-91) and `용어 후보` (WLD-73) are no undo step: they go through
     * `updateWithoutUndo`, so a later undo or redo never flips them either.
     */
    fun scriptSwitch(switch: ScriptSwitch, on: Boolean) {
        val ctx = services.deps.stores.actionContext()
        if (ScriptSettingsEdit.withoutUndo(switch)) {
            viewModelScope.launch { updateWithoutUndo { ScriptSettingsEdit.set(it, switch, on, ctx) } }
        } else {
            edit(ScriptSettingsEdit.label(switch)) { ScriptSettingsEdit.set(it, switch, on, ctx) }
        }
    }
}
