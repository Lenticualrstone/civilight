package io.github.lenticualrstone.civilight.feature.script.settings

import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.script.EditContext
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.project.VoiceIdFormatMode
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.project.ProjectViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** `스크립트 추가 칸` (SCR-24, SCR-25): project settings written through the store, one undo step each. */
class ScriptFieldsViewModel(projectKey: Key, private val deps: FeatureDeps) : ProjectViewModel(projectKey, deps.projects) {
    val ui: StateFlow<ScriptFieldsUi?> = state
        .map { (it as? ProjectLoad.Ready)?.let { r -> ScriptFieldsModel.of(r.doc, r.readOnly) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setStage(on: Boolean) = edit(ScriptCopy.STAGE_FIELD) { ScriptFieldsModel.withStage(it, on) }

    fun setVoice(on: Boolean) = edit(ScriptCopy.VOICE_FIELD) { ScriptFieldsModel.withVoice(it, on) }

    fun setMode(mode: VoiceIdFormatMode) = edit(ScriptCopy.ID_FORMAT) { ScriptFieldsModel.withMode(it, mode) }

    /** `고정 ID도 새 형식으로`. */
    fun refix() = edit(ScriptCopy.ID_FORMAT) { ScriptFieldsModel.refix(it, ctx()) }

    private fun ctx(): EditContext = EditContext(
        now = deps.clock.now(),
        zone = deps.clock.zone,
        device = deps.stores.deviceId.value,
        newKey = Keys::newKey,
    )
}
