package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/** Everything the WhenPicker shows; options, labels and results are STO's (rule 11). */
@Immutable
data class WhenPickerModel(
    val header: String,
    val base: String?,
    val dayOptions: List<String>,
    val selectedDay: Int,
    val partOptions: List<String>,
    val selectedPart: Int,
    val exactTime: String?,
    val exactSelected: Boolean,
    val precisionLabel: String,
    val precisionOptions: List<String>,
    val selectedPrecision: Int,
    val resultTitle: String,
    val resultSubtitle: String?,
)

/**
 * DS-58 WhenPicker anatomy: header `small` (`때 · 직전 씬 기준` + base at the right); day chips
 * (`같은 날 · +1일 · +2일 · 직접`, gap 6); part chips (`새벽 … 밤`) and an exact-time chip (`05:40`);
 * `small` `정밀도` with an outlined segmented (32 tall) `정확 · 대략`; result box (padding 10 × 12,
 * radius 12, `sel`): 700 16 / 22 title + `small` sub.
 */
@Composable
fun WhenPicker(
    model: WhenPickerModel,
    onDay: (Int) -> Unit,
    onPart: (Int) -> Unit,
    onExact: () -> Unit,
    onPrecision: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth()) {
            CivilightText(model.header, type.small, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
            if (model.base != null) CivilightText(model.base, type.small, color = colors.ink2, maxLines = 1)
        }
        ChipFlow {
            model.dayOptions.forEachIndexed { i, o -> FilterChip(o, i == model.selectedDay, { onDay(i) }) }
        }
        ChipFlow {
            model.partOptions.forEachIndexed { i, o -> FilterChip(o, i == model.selectedPart && !model.exactSelected, { onPart(i) }) }
            if (model.exactTime != null) FilterChip(model.exactTime, model.exactSelected, onExact)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CivilightText(model.precisionLabel, type.small, Modifier.weight(1f), color = colors.ink3)
            OutlinedSegmented(
                items = model.precisionOptions.map { SegmentItem(it) },
                selectedIndex = model.selectedPrecision,
                onSelect = onPrecision,
                height = 32.dp,
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.sel)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            CivilightText(model.resultTitle, type.itemTitle.merge(TextStyle(fontWeight = FontWeight.Bold)), color = colors.ink)
            if (model.resultSubtitle != null) CivilightText(model.resultSubtitle, type.small, color = colors.ink3)
        }
    }
}
