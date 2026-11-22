package io.github.lenticualrstone.civilight.feature.cast.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import kotlinx.serialization.Serializable

/** `항목 필터 · 정렬` over the list (CST-03; §6 Q13 default placement). Feature-local. */
@Serializable
data class CastFilterSheet(val projectKey: Key) : SheetKey

/**
 * Filter chips per `필터` field (options or linked records; several options of one field widen,
 * several fields narrow), a `숫자` range per number field, and the `<항목>순` orders (tap the chosen
 * one again for the list order). Changes apply at once.
 */
@Composable
fun SheetScope.CastFilterContent(view: FilterSheetView, state: FieldFilterState, onChange: (FieldFilterState) -> Unit) {
    SheetHeader(view.title, onClose = ::close, closeLabel = CastUiCopy.BTN_CLOSE)
    SheetScrollBody {
        for (chip in view.chips) {
            SectionLabel(chip.label, Modifier.padding(top = 14.dp, bottom = 6.dp))
            ChipFlow {
                val picked = state.selected[chip.defKey].orEmpty()
                for (o in chip.options) FilterChip(o.label, o.key in picked, { onChange(state.toggle(chip.defKey, o.key)) })
            }
        }
        for (range in view.ranges) {
            SectionLabel(range.label, Modifier.padding(top = 14.dp, bottom = 6.dp))
            val input = state.ranges[range.defKey] ?: RangeInput()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val number = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                CivilightTextField(input.min, { onChange(state.range(range.defKey, input.copy(min = it))) }, Modifier.weight(1f), unit = range.unit, keyboardOptions = number)
                CivilightTextField(input.max, { onChange(state.range(range.defKey, input.copy(max = it))) }, Modifier.weight(1f), unit = range.unit, keyboardOptions = number)
            }
        }
        if (view.sorts.isNotEmpty()) {
            SectionLabel(CastFilterModel.SORT_TITLE, Modifier.padding(top = 14.dp, bottom = 6.dp))
            ChipFlow {
                for (s in view.sorts) FilterChip(s.label, state.sortDefKey == s.defKey, { onChange(state.sort(s.defKey)) })
            }
        }
    }
}
