package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.animatedInsetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ThemeMode
import io.github.lenticualrstone.civilight.designsystem.theme.animatedAlpha
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor

/**
 * DS-01 theme swatch (onboarding `OnbLook`, screen LIB): previews a theme with its own `bg`, `ink` and
 * `acc` (two manuscript lines and a caret); the [ThemeMode.SYSTEM] swatch is split diagonally light | dark.
 * Selected: 1.5 dp `acc` inset (proposed); unselected 1 dp `edge`. [label] below (`기기 설정` · `라이트` · `다크`).
 */
@Composable
fun ThemeSwatch(
    mode: ThemeMode,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(14.dp)
    // The selection fades like the other choice controls (chips, segments): outline colour and width and the
    // label colour move on the standard token (120 ms linear when reduced), read in the draw phase.
    val outlineColor = animatedColor(if (selected) colors.acc else colors.edge, label = "swatchOutline")
    val outlineWidth = animatedAlpha(if (selected) 1.5f else 1f, label = "swatchOutlineWidth")
    val labelColor by animatedColor(if (selected) colors.ink else colors.ink2, label = "swatchLabel")
    Column(
        modifier.pressable(PressAction.Select(selected, onClick), role = Role.RadioButton, shape = shape),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .clip(shape)
                .animatedInsetOutline(outlineColor, { outlineWidth.value.dp }, shape),
        ) {
            when (mode) {
                ThemeMode.LIGHT -> drawSample(CivilightColors.light)
                ThemeMode.DARK -> drawSample(CivilightColors.dark)
                ThemeMode.SYSTEM -> {
                    val light = Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(0f, size.height); close() }
                    val dark = Path().apply { moveTo(size.width, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
                    clipPath(light) { drawSample(CivilightColors.light) }
                    clipPath(dark) { drawSample(CivilightColors.dark) }
                }
            }
        }
        CivilightText(label, CivilightTheme.type.small, color = labelColor, maxLines = 1)
    }
}

private fun DrawScope.drawSample(c: CivilightColors) {
    drawRect(c.bg)
    val x = 14.dp.toPx()
    val h = 4.dp.toPx()
    val r = CornerRadius(h / 2)
    drawRoundRect(c.ink, Offset(x, size.height * 0.34f), Size(size.width * 0.55f, h), r)
    drawRoundRect(c.ink, Offset(x, size.height * 0.56f), Size(size.width * 0.35f, h), r)
    drawRoundRect(c.acc, Offset(x + size.width * 0.37f, size.height * 0.5f), Size(2.dp.toPx(), 12.dp.toPx()), CornerRadius(1.dp.toPx()))
}
