package io.github.lenticualrstone.civilight.feature.library.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel

/**
 * 설정 · 숨기기 (LIB-64, board `HideSettings`; head `설정` / `숨기기`): intro, `다시 잠그는 때`
 * radios, `가리는 곳` switches (the second on and disabled, §6 Q28), `인증` (지문 status, `숨긴 것도
 * 백업`, `수위 등급별 동작 ›` → 설정 · 수위 등급).
 */
@Composable
internal fun HidingSettingsScreen(services: LibraryServices) {
    val navigator = services.deps.navigator
    val vm = entryViewModel { SettingsViewModel(services) }
    val ui by vm.hiding.collectAsState()
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    SettingsFrame(title = SettingsCopy.HIDING, smallLine = SettingsCopy.TITLE, onBack = { navigator.pop() }) {
        CivilightText(SettingsCopy.HIDING_INTRO, type.small, color = colors.ink3)

        io.github.lenticualrstone.civilight.designsystem.component.SettingsRow(
            title = SettingsCopy.HIDDEN_VIEW_TITLE,
            subtitle = SettingsCopy.HIDDEN_VIEW_SUB,
            leadingIcon = io.github.lenticualrstone.civilight.designsystem.icon.IconId.Lock,
            onClick = { navigator.push(io.github.lenticualrstone.civilight.navigation.SettingsHiddenProjects) },
        )

        SettingsSection(SettingsCopy.RELOCK_WHEN)
        ui.relock.forEach { option ->
            RadioRow(title = option.title, selected = option.selected, onClick = { vm.setRelock(option.policy) }, subtitle = option.sub)
        }

        SettingsSection(SettingsCopy.SECTION_MASKED)
        SwitchSettingRow(ui.searchResults, vm::setSearchResults)
        SwitchSettingRow(ui.widgets) {}

        SettingsSection(SettingsCopy.SECTION_AUTH)
        ListRow(
            title = SettingsCopy.AUTH_BIOMETRIC,
            subtitle = SettingsCopy.AUTH_BIOMETRIC_SUB,
            compact = true,
            trailing = ui.biometricStatus?.let { status -> { CivilightText(status, type.small, color = colors.ink2, maxLines = 1) } },
        )
        SwitchSettingRow(ui.backupHidden, vm::setBackupHidden)
        // (0.2.0, LIB-92) `수위 등급별 동작` left: the 설정 · 수위 등급 screen has no entry point.
    }
}
