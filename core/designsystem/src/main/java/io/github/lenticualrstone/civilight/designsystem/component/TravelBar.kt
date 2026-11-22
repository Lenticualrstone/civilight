package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.text.DecoratedText
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * DS-60 travel bar: rows of label (44) · bar (flexible) · value (≥ 64), gap 8. `필요` row: a full-width `warn`
 * bar; `있음` row: an `acc` bar of [travelBarFraction] (longest gap ÷ required, rule 18). Values in `small`
 * `ink` (strings STO; never decimal days). Below: [warning] (`▲ 이동 시간 부족 · 경로 38 km · 도보 15 km/일`,
 * the `▲` part in `warn`) and optional [chips].
 */
@Composable
fun TravelBar(
    requiredValue: String,
    availableValue: String,
    requiredMinutes: Long,
    availableMinutes: Long,
    modifier: Modifier = Modifier,
    requiredLabel: String = "필요",
    availableLabel: String = "있음",
    warning: String? = null,
    chips: (@Composable () -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val fraction = travelBarFraction(availableMinutes, requiredMinutes)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TravelRow(requiredLabel, requiredValue, 1f, colors.warn)
        TravelRow(availableLabel, availableValue, fraction, colors.acc)
        if (warning != null) DecoratedText(warningSpan(warning, colors.warn), CivilightTheme.type.small, color = colors.ink3)
        chips?.invoke()
    }
}

@Composable
private fun TravelRow(label: String, value: String, fraction: Float, barColor: Color) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CivilightText(label, type.small, Modifier.width(44.dp), color = colors.ink3, maxLines = 1)
        Box(Modifier.weight(1f).height(8.dp)) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor),
            )
        }
        CivilightText(value, type.small, Modifier.widthIn(min = 64.dp), color = colors.ink, maxLines = 1)
    }
}
