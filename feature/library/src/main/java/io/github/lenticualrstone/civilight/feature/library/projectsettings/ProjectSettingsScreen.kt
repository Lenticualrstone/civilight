package io.github.lenticualrstone.civilight.feature.library.projectsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightSwitch
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.DISABLED_ALPHA
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SettingsRow
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.readingMeasure
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.library.rating.Protection
import io.github.lenticualrstone.civilight.domain.library.rating.ProtectionSettingRow
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.feature.library.common.sheetBody
import io.github.lenticualrstone.civilight.feature.library.rating.RatingPickContent
import io.github.lenticualrstone.civilight.feature.library.rating.RatingPickSheet
import io.github.lenticualrstone.civilight.navigation.ProjectSettings
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.scaffold.BACK_LABEL
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost

/**
 * 프로젝트 설정 (head `<project name>` / `프로젝트 설정`, board `ProjSettings`). Batch 1: section
 * `등급 · 숨기기` (LIB-25): `수위 등급 · ALL · R-15 · R-18 · <rating> ›` → picker → `바뀌는 것`, then
 * the four protection switches with `<rating> 기본과 다름` in `acc` where a value differs.
 */
@Composable
internal fun ProjectSettingsScreen(route: ProjectSettings, services: LibraryServices) {
    val deps = services.deps
    val vm = entryViewModel { ProjectSettingsViewModel(route.projectKey, services) }
    val ui by vm.ui.collectAsState()
    val sheets = rememberSheetHost()
    val current = ui
    CivilightScreen(
        title = ProjectSettingsCopy.TITLE,
        smallLine = current?.projectName,
        back = IconAction(BACK_LABEL) { deps.navigator.pop() },
        sheets = sheets,
        localSheets = { key ->
            when (key) {
                is RatingPickSheet -> sheetBody { ui?.let { RatingPickContent(key.projectKey, it.protection.rating) } }
                else -> null
            }
        },
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
                ProtectionSection(
                    ui = current,
                    onRating = { sheets.open(RatingPickSheet(route.projectKey)) },
                    onSwitch = vm::switch,
                )
            }
        }
    }
}

@Composable
private fun ProtectionSection(ui: ProjectSettingsUi, onRating: () -> Unit, onSwitch: (Protection, Boolean) -> Unit) {
    val p = ui.protection
    val enabled = !ui.readOnly
    SectionLabel(ProjectSettingsCopy.SECTION_PROTECTION, Modifier.padding(top = 8.dp, bottom = 6.dp))
    Box(Modifier.alpha(if (enabled) 1f else DISABLED_ALPHA)) {
        SettingsRow(title = p.ratingRowTitle, onClick = { if (enabled) onRating() }, value = p.ratingRowValue, subtitle = p.ratingRowSub)
    }
    p.rows.forEach { row -> ProtectionSwitchRow(row, enabled) { on -> onSwitch(row.protection, on) } }
}

/** A switch row whose sub ends with `<rating> 기본과 다름` in `acc` when the value is not the default. */
@Composable
private fun ProtectionSwitchRow(row: ProtectionSettingRow, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .topHairline(colors.line)
            .pressable(PressAction.Toggle(row.on, onChange), enabled = enabled, role = Role.Switch, scaleOnPress = false)
            .heightIn(min = 44.dp)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            CivilightText(row.title, type.itemTitleCompact, color = colors.ink)
            val sub = buildAnnotatedString {
                append(row.sub)
                row.differsMarker?.let { marker ->
                    append(" · ")
                    withStyle(SpanStyle(color = colors.acc)) { append(marker) }
                }
            }
            DecoratedText(sub, type.small, color = colors.ink3)
        }
        CivilightSwitch(row.on, onCheckedChange = null)
    }
}
