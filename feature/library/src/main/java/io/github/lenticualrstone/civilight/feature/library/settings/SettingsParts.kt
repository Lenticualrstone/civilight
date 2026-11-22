package io.github.lenticualrstone.civilight.feature.library.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.BottomGlassBar
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightSwitch
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.SettingsRow
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.foundation.readingMeasure
import io.github.lenticualrstone.civilight.ui.scaffold.BACK_LABEL
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.LocalSheets
import io.github.lenticualrstone.civilight.ui.scaffold.SheetHost

/**
 * The frame every settings screen of this lane shares: glass head with `‹`, small line and title,
 * then one scrolling column at reading width (560, centred on inner screens, `ED`: the boards draw
 * only the cover layout).
 */
@Composable
internal fun SettingsFrame(
    title: String,
    smallLine: String?,
    onBack: () -> Unit,
    bottomButton: BottomButton? = null,
    sheets: SheetHost? = null,
    localSheets: LocalSheets = { null },
    content: @Composable ColumnScope.() -> Unit,
) {
    CivilightScreen(
        title = title,
        smallLine = smallLine,
        back = IconAction(BACK_LABEL, onBack),
        sheets = sheets,
        localSheets = localSheets,
        bottomBar = bottomButton?.let { button ->
            {
                BottomGlassBar {
                    CivilightButton(
                        button.text,
                        onClick = button.onClick,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        variant = ButtonVariant.PRIMARY_LARGE,
                        enabled = button.enabled,
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .readingMeasure()
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
                content = content,
            )
        }
    }
}

/** The full-width bottom primary of a settings page (boards `RHRecoveryPw`, `Restore`: 52 tall, 16 side). */
internal class BottomButton(val text: String, val enabled: Boolean, val onClick: () -> Unit)

/** A section label; [first] sits right under the head, the others 22 dp below the last row. */
@Composable
internal fun SettingsSection(text: String, first: Boolean = false) {
    SectionLabel(text, Modifier.padding(top = if (first) 0.dp else 22.dp, bottom = 6.dp))
}

@Composable
internal fun LinkRow(row: SettingsLinkRow, onClick: () -> Unit) {
    SettingsRow(title = row.title, onClick = onClick, value = row.value, subtitle = row.sub)
}

@Composable
internal fun SwitchSettingRow(row: SettingsSwitchRow, onChange: (Boolean) -> Unit) {
    SwitchRow(title = row.title, checked = row.on, onCheckedChange = onChange, subtitle = row.sub, enabled = row.enabled)
}

/** A row that opens another screen while showing a switch it does not toggle (the Drive row). */
@Composable
internal fun SwitchLinkRow(row: SettingsSwitchRow, onClick: () -> Unit) {
    ListRow(
        title = row.title,
        subtitle = row.sub,
        compact = true,
        onClick = onClick,
        trailing = { CivilightSwitch(row.on, onCheckedChange = null) },
    )
}

/** `기기 설정 · 라이트 · 다크` (LIB-63, LIB-65; also usable by onboarding `OnbLook`). */
@Composable
internal fun ThemeSegmented(selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    SegmentedControl(options = SettingsState.THEME_LABELS, selectedIndex = selectedIndex, onSelect = onSelect, modifier = modifier)
}
