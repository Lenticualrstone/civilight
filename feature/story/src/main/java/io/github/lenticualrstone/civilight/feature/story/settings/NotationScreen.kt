package io.github.lenticualrstone.civilight.feature.story.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.feature.story.common.StoryCopy
import io.github.lenticualrstone.civilight.feature.story.common.StoryServices
import io.github.lenticualrstone.civilight.feature.story.common.StoryViewModel
import io.github.lenticualrstone.civilight.feature.story.common.storyServices
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SceneNotationSettings
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole
import io.github.lenticualrstone.civilight.ui.nav.PaneRole
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.window.CenterColumn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 프로젝트 설정 › 때 표기 (STO-17). */
class NotationViewModel(projectKey: Key, services: StoryServices) : StoryViewModel(projectKey, services) {
    val ui: StateFlow<NotationUi?> = state
        .map { load -> (load as? ProjectLoad.Ready)?.let { NotationModel.build(it.doc, it.readOnly) } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

/** `에피소드 기준` · `이야기 시작부터` as radio rows with the sample label under each (STO-17). */
@Composable
fun NotationScreen(route: SceneNotationSettings, deps: FeatureDeps) {
    val vm = entryViewModel { NotationViewModel(route.projectKey, deps.storyServices()) }
    val ui by vm.ui.collectAsState()
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    CenterColumn {
        CivilightScreen(
            title = NotationCopy.TITLE,
            smallLine = ui?.projectName,
            back = if (LocalPaneRole.current == PaneRole.DETAIL) null else IconAction(StoryCopy.BACK) { deps.navigator.pop() },
        ) { padding ->
            // Loading -> content swaps on the kind, never on the data (motion audit, feature:story 7).
            ContentSwap(ui != null, Modifier.fillMaxSize()) { loaded ->
                val u = ui
                if (!loaded || u == null) {
                    Box(Modifier.fillMaxSize().background(colors.bg))
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(colors.bg)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
                    ) {
                        u.options.forEach { option ->
                            RadioRow(
                                title = option.label,
                                selected = option.selected,
                                onClick = { if (!option.selected) vm.setNotation(option.notation, NotationCopy.TITLE) },
                                subtitle = option.sample,
                                enabled = !u.readOnly,
                            )
                        }
                        CivilightText(NotationCopy.TIMELINE_NOTE, type.small, Modifier.padding(top = 12.dp), color = colors.ink3)
                        if (u.readOnly) CivilightText(StoryCopy.READ_ONLY, type.small, Modifier.padding(top = 4.dp), color = colors.ink3)
                    }
                }
            }
        }
    }
}
