package io.github.lenticualrstone.civilight.feature.cast.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.theme.ValueSwap
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy

/**
 * A row of the record's field table or `항목` list (`.row`): label `ink3` | value, then the start of
 * the record in effect (`1-3부터`) and `◆` in `acc`. Tapping the row opens the value sheet; `◆` is
 * its own 44 dp target and opens the history sheet (CST-10). An empty value reads `비어 있음` in
 * `ink3` (shown only while `보기` is on). [mark] is the small `이 인물만` mark.
 */
@Composable
fun RecordFieldRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    since: String? = null,
    hasHistory: Boolean = false,
    numeric: Boolean = false,
    mark: Boolean = false,
    onClick: (() -> Unit)? = null,
    onHistory: (() -> Unit)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val valueStyle = if (numeric) type.number.merge(TextStyle(fontSize = 14.sp, lineHeight = 20.sp)) else type.command
    Row(
        modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .then(if (onClick != null) Modifier.pressable(onClick = onClick, scaleOnPress = false, onClickLabel = label) else Modifier)
            .heightIn(min = 44.dp)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText(if (mark) "$label ${CastCopy.SOFT_MARK}" else label, type.command, color = colors.ink3, maxLines = 1)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            if (value.isEmpty()) {
                CivilightText(CastUiCopy.EMPTY_VALUE, type.command, color = colors.ink3, maxLines = 1)
            } else {
                // A value that changes with the lens or an edit swaps with the shared value motion (P4, CNV-42).
                ValueSwap(value, valueStyle, Modifier.weight(1f, fill = false), color = colors.ink, maxLines = 2, contentAlignment = Alignment.CenterEnd)
            }
            if (since != null) ValueSwap(since, type.small, color = colors.ink3, maxLines = 1, contentAlignment = Alignment.CenterEnd)
            if (hasHistory) {
                Box(
                    Modifier
                        .then(
                            if (onHistory != null) {
                                Modifier.pressable(onClick = onHistory, role = Role.Button, hitTarget = HitTarget.CHIP, scaleOnPress = false, onClickLabel = CastUiCopy.HISTORY_OPEN)
                            } else {
                                Modifier
                            },
                        )
                        .widthIn(min = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CivilightText(CastCopy.CHANGE_MARK, type.command, color = colors.acc, maxLines = 1)
                }
            }
        }
    }
}

/** The dimmed `예정 · 2-5 | 등불지기장` row under a field whose next record is past the 집필 선 (`RTPlanned`). */
@Composable
fun PlannedRow(point: String, value: String, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        modifier
            .fillMaxWidth()
            .alpha(0.75f)
            .padding(bottom = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ValueSwap(point, type.number.merge(TextStyle(fontSize = 11.sp)), color = colors.ink3, maxLines = 1, contentAlignment = Alignment.CenterEnd)
        ValueSwap(value, type.small.merge(TextStyle(fontSize = 13.sp, lineHeight = 18.sp)), color = colors.ink3, maxLines = 1, contentAlignment = Alignment.CenterEnd)
    }
}
