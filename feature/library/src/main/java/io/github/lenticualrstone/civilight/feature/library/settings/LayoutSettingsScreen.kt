package io.github.lenticualrstone.civilight.feature.library.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.RadioMark
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.model.app.UnfoldedLayout
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel

/**
 * 설정 · 펼친 화면 배치 (LIB-66, board `LayoutSettings`; head `설정 › 화면` / `펼친 화면`): section
 * `펼친 화면 배치`, two radio cards with a drawn thumbnail (`목록 + 상세` · `가운데 한 칸`), the note.
 * The choice applies live: the shell reads `AppSettings.unfoldedLayout` (CNV-33).
 */
@Composable
internal fun LayoutSettingsScreen(services: LibraryServices) {
    val navigator = services.deps.navigator
    val vm = entryViewModel { SettingsViewModel(services) }
    val ui by vm.layout.collectAsState()
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    SettingsFrame(title = LayoutSettingsCopy.TITLE, smallLine = LayoutSettingsCopy.SMALL, onBack = { navigator.pop() }) {
        SettingsSection(LayoutSettingsCopy.SECTION, first = true)
        Column(Modifier.padding(top = 4.dp)) {
            ui.options.forEach { option -> LayoutCard(option, onSelect = { vm.setLayout(option.layout) }) }
        }
        CivilightText(ui.note, type.small, Modifier.padding(top = 4.dp), color = colors.ink3)
    }
}

/**
 * One `LayoutSettings` card: padding 14, radius 16, 10 below; selected `sel` fill with a 1.5 `acc`
 * outline, else a 1 `edge` outline; 72 thumbnail, title + sub, radio at the end. The whole card
 * selects (DS `ChoiceCard` has no leading slot; Request).
 */
@Composable
private fun LayoutCard(option: LayoutOption, onSelect: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .pressable(PressAction.Select(option.selected, onSelect), role = Role.RadioButton, shape = shape, scaleOnPress = false)
            .clip(shape)
            .then(if (option.selected) Modifier.background(colors.sel) else Modifier)
            .insetOutline(if (option.selected) colors.acc else colors.edge, if (option.selected) 1.5.dp else 1.dp, shape)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LayoutThumbnail(option.layout)
        Column(Modifier.weight(1f)) {
            CivilightText(option.title, type.itemTitle, color = colors.ink)
            CivilightText(option.sub, type.small, color = colors.ink3)
        }
        RadioMark(option.selected)
    }
}

/** The 72 dp sketch: list | detail (detail lines at 60 %), or one centred column with side margins. */
@Composable
private fun LayoutThumbnail(layout: UnfoldedLayout) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .size(72.dp)
            .clearAndSetSemantics { }
            .insetOutline(colors.edge, 1.dp, shape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (layout) {
            UnfoldedLayout.LIST_DETAIL -> {
                Lines(1f, 1f)
                Box(Modifier.width(1.dp).fillMaxHeight().background(colors.edge))
                Lines(1.3f, 0.6f)
            }
            UnfoldedLayout.CENTER_COLUMN -> {
                Spacer(Modifier.weight(0.35f))
                Lines(1f, 1f)
                Spacer(Modifier.weight(0.35f))
            }
        }
    }
}

@Composable
private fun RowScope.Lines(weight: Float, opacity: Float) {
    Column(Modifier.weight(weight), verticalArrangement = Arrangement.spacedBy(4.dp)) { LineBars(opacity) }
}

@Composable
private fun ColumnScope.LineBars(opacity: Float) {
    val colors = CivilightTheme.colors
    repeat(5) {
        Box(Modifier.fillMaxWidth().height(5.dp).alpha(opacity).clip(RoundedCornerShape(2.dp)).background(colors.ink3))
    }
}
