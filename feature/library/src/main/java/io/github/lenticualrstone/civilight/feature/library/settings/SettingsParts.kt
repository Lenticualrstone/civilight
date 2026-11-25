package io.github.lenticualrstone.civilight.feature.library.settings

import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.ValueSwap
import io.github.lenticualrstone.civilight.ui.window.LocalWindowLayout
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
                BottomGlassBar(inner = LocalWindowLayout.current.isInner) {
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
internal fun LinkRow(row: SettingsLinkRow, leadingIcon: IconId? = null, onClick: () -> Unit) {
    SettingsValueRow(title = row.title, onClick = onClick, value = row.value, subtitle = row.sub, leadingIcon = leadingIcon)
}

/**
 * The DS `SettingsRow` with its value drawn through `ValueSwap` (audit row 15, P4): a changed value
 * leaves up and the new one comes in from below (CNV-42); reduced motion crossfades.
 */
@Composable
internal fun SettingsValueRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    subtitle: String? = null,
    leadingIcon: IconId? = null,
) {
    val colors = CivilightTheme.colors
    ListRow(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        leading = leadingIcon?.let { icon ->
            { CivilightIcon(icon, contentDescription = null, size = 18.dp, tint = colors.ink2) }
        },
        onClick = onClick,
        compact = true,
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (value != null) ValueSwap(value, CivilightTheme.type.small, color = colors.ink2, contentAlignment = Alignment.CenterEnd)
                CivilightIcon(IconId.Next, contentDescription = null, size = 14.dp, tint = colors.ink3)
            }
        },
    )
}

@Composable
internal fun SwitchSettingRow(row: SettingsSwitchRow, onChange: (Boolean) -> Unit) {
    SwitchRow(title = row.title, checked = row.on, onCheckedChange = onChange, subtitle = row.sub, enabled = row.enabled)
}

@Composable
internal fun SwitchSettingRow(row: SettingsSwitchRow, leadingIcon: IconId, onChange: (Boolean) -> Unit) {
    SwitchRow(title = row.title, checked = row.on, onCheckedChange = onChange, subtitle = row.sub, enabled = row.enabled, leadingIcon = leadingIcon)
}

/** A row that opens another screen while showing a switch it does not toggle (the Drive row). */
@Composable
internal fun SwitchLinkRow(row: SettingsSwitchRow, leadingIcon: IconId? = null, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    ListRow(
        title = row.title,
        subtitle = row.sub,
        leading = leadingIcon?.let { icon ->
            { CivilightIcon(icon, contentDescription = null, size = 18.dp, tint = colors.ink2) }
        },
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
