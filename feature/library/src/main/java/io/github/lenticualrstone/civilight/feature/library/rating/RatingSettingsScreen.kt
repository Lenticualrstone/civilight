package io.github.lenticualrstone.civilight.feature.library.rating

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.RatingBadge
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.readingMeasure
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.navigation.RatingDefaultsApply
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.BACK_LABEL
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.theme.ThemeBridge

/**
 * 설정 · 수위 등급 (LIB-23, board `RatingSettings`): intro, R-18 · R-15 · ALL groups with their
 * defaults (fixed ones are not shown as switches; ALL is `라벨만`), the `같이 바꾸기` tag under a
 * changed group, and `모든 등급` › `내보내기 표지에 등급 표시`.
 */
@Composable
internal fun RatingSettingsScreen(services: LibraryServices) {
    val deps = services.deps
    val vm = entryViewModel { RatingSettingsViewModel(services) }
    val ui by vm.state.collectAsState()
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    CivilightScreen(
        title = RatingCopy.RATING,
        smallLine = RatingCopy.SETTINGS_SMALL,
        back = IconAction(BACK_LABEL) { deps.navigator.pop() },
    ) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .readingMeasure()
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                CivilightText(RatingCopy.SETTINGS_INTRO, type.small, color = colors.ink3)
                ui.groups.forEach { group ->
                    Row(
                        Modifier.padding(top = 22.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RatingBadge(ThemeBridge.rating(group.rating))
                        CivilightText(group.gloss, type.small, color = colors.ink3)
                    }
                    if (group.switches.isEmpty()) {
                        CivilightText(RatingCopy.ALL_LABEL_ONLY, type.small, color = colors.ink3)
                    }
                    group.switches.forEach { s ->
                        SwitchRow(
                            title = s.title,
                            checked = s.on,
                            onCheckedChange = { on -> vm.setDefault(s.rating, s.protection, on) },
                            subtitle = s.sub,
                        )
                    }
                    group.tag?.let { tag ->
                        TextAction(tag, { deps.navigator.push(RatingDefaultsApply(group.rating)) }, color = colors.acc, style = type.small)
                    }
                }
                SectionLabel(RatingCopy.ALL_RATINGS, Modifier.padding(top = 26.dp, bottom = 6.dp))
                SwitchRow(
                    title = RatingCopy.EXPORT_COVER,
                    checked = ui.exportCoverRating,
                    onCheckedChange = vm::setExportCover,
                    subtitle = RatingCopy.EXPORT_COVER_SUB,
                )
            }
        }
    }
}
