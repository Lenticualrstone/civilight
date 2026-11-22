package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * DS-50 canvas / relationship-map node: square-cornered card (radius 14, never a circle), padding 10 × 14,
 * `raise` fill, 1 dp `line` inset; title 700 14 / 20 `ink`, sub-line 400 11 / 15 `ink3` (`세력`, `사건`).
 * [memo] nodes are transparent with a dashed `line` outline; [selected] adds a 1.5 dp `acc` inset.
 */
@Composable
fun NodeCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    memo: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressable(onClick = onClick, shape = shape) else Modifier)
            .clip(shape)
            .then(if (!memo) Modifier.background(colors.raise) else Modifier)
            .insetOutline(
                if (selected) colors.acc else colors.line,
                if (selected) 1.5.dp else 1.dp,
                shape,
                dashed = memo && !selected,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        CivilightText(title, type.command.merge(TextStyle(fontWeight = FontWeight.Bold)), color = colors.ink)
        if (subtitle != null) {
            CivilightText(subtitle, type.small.merge(TextStyle(fontSize = 11.sp, lineHeight = 15.sp)), color = colors.ink3)
        }
    }
}
