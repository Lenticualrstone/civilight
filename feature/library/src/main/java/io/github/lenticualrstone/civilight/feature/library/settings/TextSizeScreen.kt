package io.github.lenticualrstone.civilight.feature.library.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.StepSlider
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel

/**
 * 화면 · 글자 크기 (LIB-65; boards `Settings` row, `OnbLook` controls; head `설정 › 화면` / `글자
 * 크기`): the theme segmented, then the size slider between a small and a large `가` with the live
 * preview line and `본문 <sp> · 행간 1.7`. Both values are stored in AppSettings and apply app-wide at
 * once (the shell maps them into the theme), so the preview line is drawn in the real body style.
 */
@Composable
internal fun TextSizeScreen(services: LibraryServices) {
    val vm = entryViewModel { SettingsViewModel(services) }
    val ui by vm.textSize.collectAsState()
    SettingsFrame(title = SettingsCopy.TEXT_SIZE, smallLine = SettingsCopy.TEXT_SIZE_SMALL, onBack = { services.deps.navigator.pop() }) {
        SettingsSection(SettingsCopy.SECTION_SCREEN, first = true)
        ThemeSegmented(ui.themeIndex, vm::setTheme, Modifier.padding(top = 4.dp))
        SettingsSection(SettingsCopy.TEXT_SIZE)
        TextSizeControl(ui, vm::setTextSlider)
    }
}

/** Slider, preview line and size label (`OnbLook` draws no size label, LIB-15). */
@Composable
internal fun TextSizeControl(ui: TextSizeUi, onChange: (Int) -> Unit, modifier: Modifier = Modifier, showSizeLabel: Boolean = true) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(modifier.fillMaxWidth()) {
        StepSlider(
            value = ui.sliderIndex,
            stepCount = ui.stepCount,
            onValueChange = onChange,
            smallLabel = SettingsCopy.SIZE_GLYPH,
            largeLabel = SettingsCopy.SIZE_GLYPH,
            contentDescription = SettingsCopy.TEXT_SIZE,
        )
        CivilightText(SettingsCopy.PREVIEW_LINE, type.line, Modifier.padding(top = 18.dp), color = colors.ink)
        if (showSizeLabel) CivilightText(ui.label, type.small, Modifier.padding(top = 10.dp), color = colors.ink3)
    }
}
