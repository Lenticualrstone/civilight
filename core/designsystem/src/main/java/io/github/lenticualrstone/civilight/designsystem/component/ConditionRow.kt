package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/** Result of a condition after map generation (DS-59). */
enum class ConditionResult { APPLIED, NEEDS_CHECK }

/**
 * DS-59 condition row (`.cond`): gap 10, padding 10 · 0, 1 dp `line` top. Leading 3 × 3 grid (8 dp cells,
 * gap 2, radius 2; cells in `line`, [coveredCells] (0–8, row-major) in `acc`), or after generation an
 * icon 18: `Check` in `acc` for [ConditionResult.APPLIED], an exclamation in `warn` for
 * [ConditionResult.NEEDS_CHECK]. Name 700 15 / 20, `small` [detail], trailing `⋯` 18 `ink3`.
 */
@Composable
fun ConditionRow(
    name: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    coveredCells: Set<Int> = emptySet(),
    result: ConditionResult? = null,
    more: IconAction? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (result) {
            ConditionResult.APPLIED -> CivilightIcon(IconId.Check, null, size = 18.dp, tint = colors.acc)
            ConditionResult.NEEDS_CHECK -> CivilightIcon(IconId.Exclaim, null, size = 18.dp, tint = colors.warn)
            null -> Column(Modifier.clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) { r ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(3) { c ->
                            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(if (r * 3 + c in coveredCells) colors.acc else colors.line))
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1f)) {
            CivilightText(name, type.itemTitleCompact.merge(TextStyle(lineHeight = 20.sp, fontWeight = FontWeight.Bold)), color = colors.ink)
            if (detail != null) CivilightText(detail, type.small, color = colors.ink3)
        }
        if (more != null) CivilightIconButton(IconId.More, more.label, more.onClick, tint = colors.ink3, iconSize = 18.dp)
    }
}
