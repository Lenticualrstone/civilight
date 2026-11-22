package io.github.lenticualrstone.civilight.feature.script.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.readingMeasure
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.project.TransitionTemplate
import io.github.lenticualrstone.civilight.navigation.TransitionTextSettings
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.project.ProjectViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.BACK_LABEL
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** `전환 문구` settings (SCR-48): one undo step per choice; read-only projects show the values only. */
class TransitionSettingsViewModel(projectKey: Key, deps: FeatureDeps) : ProjectViewModel(projectKey, deps.projects) {
    val ui: StateFlow<TransitionSettingsUi?> = state
        .map { (it as? ProjectLoad.Ready)?.let { r -> TransitionSettingsModel.of(r.doc, r.readOnly) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setDefault(mode: TransitionMode) = edit(TransitionCopy.TITLE) { TransitionSettingsModel.withDefault(it, mode) }

    fun setTemplate(template: TransitionTemplate) = edit(TransitionCopy.TITLE) { TransitionSettingsModel.withTemplate(it, template) }
}

/**
 * The route `TransitionTextSettings` (SCR-48): `새 씬 기본` (`없음 · 자동`) and `틀` (`장소 · 시각` ·
 * `장소만`, each with the project's own sample such as `결사의 문 · 06:10`). A change of the template
 * rewrites every `자동` text at once (they are computed); `직접` texts never change.
 */
@Composable
fun TransitionSettingsScreen(route: TransitionTextSettings, deps: FeatureDeps) {
    val vm = entryViewModel { TransitionSettingsViewModel(route.projectKey, deps) }
    val ui by vm.ui.collectAsState()
    val current = ui
    CivilightScreen(
        title = TransitionCopy.TITLE,
        smallLine = current?.projectName,
        back = IconAction(BACK_LABEL) { deps.navigator.pop() },
    ) { padding ->
        if (current == null) return@CivilightScreen
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .readingMeasure()
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                SectionLabel(TransitionCopy.DEFAULT_SECTION)
                current.modes.forEach { choice ->
                    RadioRow(title = choice.title, subtitle = choice.sample, selected = choice.selected, enabled = !current.readOnly, onClick = { vm.setDefault(choice.value) })
                }
                SectionLabel(TransitionCopy.TEMPLATE_SECTION, Modifier.padding(top = 20.dp))
                current.templates.forEach { choice ->
                    RadioRow(title = choice.title, subtitle = choice.sample, selected = choice.selected, enabled = !current.readOnly, onClick = { vm.setTemplate(choice.value) })
                }
            }
        }
    }
}
