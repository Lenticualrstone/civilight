package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.text.MarkerKind
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * DS-50 canvas / relationship-map node: square-cornered card (radius 14, never a circle), padding 10 × 14,
 * `raise` fill, 1 dp `line` inset; title 700 14 / 20 `ink`, sub-line 400 11 / 15 `ink3` (`세력`, `사건`).
 * [memo] nodes are transparent with a dashed `line` outline; [selected] adds a 1.5 dp `acc` inset.
 *
 * [deleted] draws a reference to a trashed or purged item (rule 20 §2, CNV-11): transparent, a dotted
 * `ink3` outline and a `◇` mark (`ink3`) before the title, title in `ink2`. The caller passes the wide
 * label as [title] (`지운 인물 · 노아`). Dotted is its own sign, distinct from the memo's dashes.
 */
@Composable
fun NodeCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    memo: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    deleted: Boolean = false,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(14.dp)
    val look = nodeCardLook(memo = memo, selected = selected, deleted = deleted, colors = colors)
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressable(onClick = onClick, shape = shape) else Modifier)
            .clip(shape)
            .then(if (look.filled) Modifier.background(colors.raise) else Modifier)
            .insetOutline(look.outline, look.outlineWidth, shape, dashed = look.dashed, dotted = look.dotted)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        val titleStyle = type.command.merge(TextStyle(fontWeight = FontWeight.Bold))
        if (deleted) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CivilightText(MarkerKind.NOTICE.symbol.toString(), titleStyle, color = colors.ink3)
                CivilightText(title, titleStyle, color = look.titleColor)
            }
        } else {
            CivilightText(title, titleStyle, color = look.titleColor)
        }
        if (subtitle != null) {
            CivilightText(subtitle, type.small.merge(TextStyle(fontSize = 11.sp, lineHeight = 15.sp)), color = colors.ink3)
        }
    }
}

/** How a [NodeCard] is drawn; pure so the rule 20 variant can be tested. */
data class NodeCardLook(
    val filled: Boolean,
    val outline: androidx.compose.ui.graphics.Color,
    val outlineWidth: androidx.compose.ui.unit.Dp,
    val dashed: Boolean,
    val dotted: Boolean,
    val titleColor: androidx.compose.ui.graphics.Color,
)

/** Selected wins the outline colour and width; a deleted node keeps its dots when selected. */
fun nodeCardLook(memo: Boolean, selected: Boolean, deleted: Boolean, colors: CivilightColors): NodeCardLook = NodeCardLook(
    filled = !memo && !deleted,
    outline = when {
        selected -> colors.acc
        deleted -> colors.ink3
        else -> colors.line
    },
    outlineWidth = if (selected) 1.5.dp else 1.dp,
    dashed = memo && !selected && !deleted,
    dotted = deleted,
    titleColor = if (deleted) colors.ink2 else colors.ink,
)
