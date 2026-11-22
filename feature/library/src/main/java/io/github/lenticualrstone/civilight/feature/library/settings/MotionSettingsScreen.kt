package io.github.lenticualrstone.civilight.feature.library.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.LibraryServices
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel

/**
 * 설정 · 움직임 · 투명도 (LIB-67, board `ReduceSettings`; head `설정` / `움직임 · 투명도`): `애니메이션`
 * radios, `투명도` with the `투명도 줄이기` switch and the two previews `유리` · `불투명`, then `라이프
 * 레코더 빛`. Values apply live through the shell's theme bridge (CNV-46: reduced motion, opaque bars).
 */
@Composable
internal fun MotionSettingsScreen(services: LibraryServices) {
    val navigator = services.deps.navigator
    val vm = entryViewModel { SettingsViewModel(services) }
    val ui by vm.motion.collectAsState()
    SettingsFrame(title = MotionSettingsCopy.TITLE, smallLine = MotionSettingsCopy.SMALL, onBack = { navigator.pop() }) {
        SettingsSection(MotionSettingsCopy.SECTION_ANIMATION, first = true)
        ui.animation.forEach { option ->
            RadioRow(title = option.title, selected = option.selected, onClick = { vm.setAnimation(option.pref) }, subtitle = option.sub)
        }

        SettingsSection(MotionSettingsCopy.SECTION_TRANSPARENCY)
        SwitchSettingRow(ui.reduceTransparency, vm::setReduceTransparency)
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TransparencyPreview(MotionSettingsCopy.PREVIEW_GLASS, opaque = false, Modifier.weight(1f))
            TransparencyPreview(MotionSettingsCopy.PREVIEW_OPAQUE, opaque = true, Modifier.weight(1f))
        }
        SwitchSettingRow(ui.recorderGlow, vm::setRecorderGlow)
    }
}

/**
 * A 120 dp sample (radius 14, `line` outline): a manuscript line at the top and a 56 dp bar at the
 * bottom: the glass fill over the text, or `raise` with a `line` top edge (board `ReduceTransparency`).
 * Both always draw their own look, whatever the switch says, so the two can be compared.
 */
@Composable
private fun TransparencyPreview(label: String, opaque: Boolean, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .height(120.dp)
            .clip(shape)
            .insetOutline(colors.line, 1.dp, shape),
    ) {
        CivilightText(
            MotionPreviewSample.LINE,
            type.body.merge(TextStyle(fontSize = 13.sp, lineHeight = 22.sp)),
            Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp).clearAndSetSemantics { },
            color = colors.ink,
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(56.dp)
                .then(if (opaque) Modifier.background(colors.raise).topHairline(colors.line) else Modifier.background(colors.glass)),
        )
        CivilightText(label, type.small, Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 8.dp), color = colors.ink3)
    }
}
