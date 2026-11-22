package io.github.lenticualrstone.civilight.feature.story.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.story.structure.UnitSettingsEdit
import io.github.lenticualrstone.civilight.feature.story.common.StoryCopy
import io.github.lenticualrstone.civilight.feature.story.common.StoryServices
import io.github.lenticualrstone.civilight.feature.story.common.StoryViewModel
import io.github.lenticualrstone.civilight.feature.story.common.storyServices
import io.github.lenticualrstone.civilight.feature.story.where.CommitField
import io.github.lenticualrstone.civilight.feature.story.where.WhereInputs
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.project.DayBoundary
import io.github.lenticualrstone.civilight.navigation.DayBandSettings
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

/** 프로젝트 설정 › 하루 · 시간대 (STO-19). */
class DayBandViewModel(projectKey: Key, services: StoryServices) : StoryViewModel(projectKey, services) {
    val ui: StateFlow<DayBandUi?> = state
        .map { load -> (load as? ProjectLoad.Ready)?.let { DayBandModel.build(it.doc, it.readOnly) } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

/**
 * `하루는 새벽 4시부터` · `자정` as radio rows, then the five bands with their hours (`저녁 17–20`) and
 * a start-time field for each boundary but the 새벽 start. Moving a start moves the previous band's
 * end (bands stay contiguous); a start outside its neighbours is not applied. Labels change, stored
 * 때 never does.
 */
@Composable
fun DayBandScreen(route: DayBandSettings, deps: FeatureDeps) {
    val vm = entryViewModel { DayBandViewModel(route.projectKey, deps.storyServices()) }
    val ui by vm.ui.collectAsState()
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    CenterColumn {
        CivilightScreen(
            title = DayBandCopy.TITLE,
            smallLine = ui?.projectName,
            back = if (LocalPaneRole.current == PaneRole.DETAIL) null else IconAction(StoryCopy.BACK) { deps.navigator.pop() },
        ) { padding ->
            val u = ui
            if (u == null) {
                Box(Modifier.fillMaxSize().background(colors.bg))
                return@CivilightScreen
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .background(colors.bg)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                listOf(DayBoundary.DAWN_0400 to DayBandCopy.DAWN_DAY, DayBoundary.MIDNIGHT to DayBandCopy.MIDNIGHT).forEach { (boundary, label) ->
                    RadioRow(
                        title = label,
                        selected = u.boundary == boundary,
                        onClick = {
                            if (u.boundary != boundary) vm.apply(DayBandCopy.TITLE) { d, ctx -> UnitSettingsEdit.setDayBoundary(d, boundary, ctx.edit) }
                        },
                        enabled = !u.readOnly,
                    )
                }
                SectionLabel(DayBandCopy.BANDS, Modifier.padding(top = 20.dp))
                u.bands.forEach { row ->
                    BandEditRow(row) { start ->
                        vm.apply(DayBandCopy.TITLE) { d, ctx -> UnitSettingsEdit.setBandStart(d, row.band, start, ctx.edit) }
                    }
                }
                if (u.readOnly) CivilightText(StoryCopy.READ_ONLY, type.small, Modifier.padding(top = 12.dp), color = colors.ink3)
            }
        }
    }
}

@Composable
private fun BandEditRow(row: BandRow, onStart: (io.github.lenticualrstone.civilight.model.ClockTime) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier.fillMaxWidth().topHairline(colors.line).heightIn(min = 56.dp).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CivilightText(row.word, type.itemTitleCompact, Modifier.width(48.dp), color = colors.ink, maxLines = 1)
        CivilightText(row.hours, type.number, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
        if (row.editable) {
            // Committed on Done or focus loss only; an invalid start is not applied (the field returns to the stored value).
            CommitField(
                seed = row.start.toString(),
                onCommit = { text ->
                    val clock = (WhereInputs.clock(text) as? WhereInputs.ClockDraft.Value)?.clock
                    if (clock != null && clock != row.start) onStart(clock)
                },
                modifier = Modifier.width(84.dp),
            )
        } else {
            CivilightText(row.start.toString(), type.number, color = colors.ink3, maxLines = 1)
        }
    }
}
