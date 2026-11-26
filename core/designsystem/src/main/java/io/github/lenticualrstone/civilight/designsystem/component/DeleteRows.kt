package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * The `⋯` menu's `지우기` row (0.5 interface review; look of `RSHeadMenu`'s `씬 지우기`): always the last row,
 * under its own divider (a 12 dp gap closed by a `line`), trash icon and label in `warn`, min 52, label
 * 15 / 21 medium, optional `small` sub-line. A null [onClick] (read-only) dims the row to 38 %. What the tap
 * does (confirm sheet or undo toast) stays with each caller.
 */
@Composable
fun MenuDeleteRow(
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = onClick != null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Spacer(Modifier.fillMaxWidth().height(MENU_DELETE_GAP).topHairline(colors.line))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .then(if (onClick != null) Modifier.pressable(onClick = onClick, enabled = enabled, scaleOnPress = false) else Modifier)
            .alpha(if (enabled) 1f else MENU_DISABLED_ALPHA)
            .heightIn(min = 52.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CivilightIcon(IconId.Trash, contentDescription = null, tint = colors.warn)
        Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Center) {
            CivilightText(
                label,
                type.itemTitleCompact.merge(TextStyle(fontWeight = FontWeight.Medium)),
                color = colors.warn,
                maxLines = 1,
            )
            subtitle?.let { CivilightText(it, type.small, color = colors.ink3, maxLines = 1) }
        }
    }
}

/**
 * A count row of the shared `지우기 · 영향` sheet (`RCDeleteImpact`): `line` top, min 44, label in `command`
 * with an optional `small` sub-line, the count right-aligned in `number` 15 sp. Cast and world sheets share it.
 */
@Composable
fun ImpactCountRow(label: String, count: String, modifier: Modifier = Modifier, sub: String? = null) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier.fillMaxWidth().topHairline(colors.line).heightIn(min = 44.dp).padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            CivilightText(label, type.command, color = colors.ink)
            sub?.let { CivilightText(it, type.small, color = colors.ink3) }
        }
        CivilightText(count, type.number.merge(TextStyle(fontSize = 15.sp)), color = colors.ink)
    }
}

private const val MENU_DISABLED_ALPHA = 0.38f
private val MENU_DELETE_GAP = 12.dp
