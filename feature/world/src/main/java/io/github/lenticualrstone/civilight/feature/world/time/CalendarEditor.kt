package io.github.lenticualrstone.civilight.feature.world.time

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.world.WorldCopy

/**
 * The 세계 달력 editor under `세계 달력 만들기` (WLD-30, not drawn: Q-21). Months as rows of
 * `달 이름` + `한 달의 날 수` with a visible remove button, `+ 달`; weekday names the same way with
 * `+ 요일`; the story start as month chips and a day field, its weekday as chips; `저장` writes the
 * whole calendar as one step. The problem line under the button says what stops `저장`.
 */
@Composable
fun CalendarEditor(draft: CalendarDraft, readOnly: Boolean, onChange: (CalendarDraft) -> Unit, onSave: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val enabled = !readOnly
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        SectionLabel(WorldCopy.CALENDAR_TITLE, Modifier.padding(bottom = 4.dp))
        draft.months.forEachIndexed { i, month ->
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CivilightTextField(
                    value = month.name,
                    onValueChange = { onChange(draft.setMonth(i, month.copy(name = it))) },
                    label = WorldCopy.MONTH_NAME,
                    modifier = Modifier.weight(1.4f),
                    enabled = enabled,
                )
                CivilightTextField(
                    value = month.days,
                    onValueChange = { onChange(draft.setMonth(i, month.copy(days = it))) },
                    label = WorldCopy.MONTH_DAYS,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                CivilightIconButton(
                    icon = IconId.X,
                    contentDescription = WorldCopy.removeLabel(month.name.ifBlank { WorldCopy.MONTH_NAME }),
                    onClick = { onChange(draft.removeMonth(i)) },
                    enabled = enabled && draft.months.size > 1,
                )
            }
        }
        TextAction(WorldCopy.ADD_MONTH, { onChange(draft.addMonth()) }, Modifier.padding(top = 4.dp), color = colors.acc, style = type.small)

        draft.weekdays.forEachIndexed { i, name ->
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CivilightTextField(
                    value = name,
                    onValueChange = { onChange(draft.setWeekday(i, it)) },
                    label = WorldCopy.WEEKDAY,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                )
                CivilightIconButton(
                    icon = IconId.X,
                    contentDescription = WorldCopy.removeLabel(name.ifBlank { WorldCopy.WEEKDAY }),
                    onClick = { onChange(draft.removeWeekday(i)) },
                    enabled = enabled,
                )
            }
        }
        TextAction(WorldCopy.ADD_WEEKDAY, { onChange(draft.addWeekday()) }, Modifier.padding(top = 4.dp), color = colors.acc, style = type.small)

        CivilightText(WorldCopy.TODO_COPY_WORLD_CALENDAR_START_LABEL, type.small, Modifier.padding(top = 14.dp), color = colors.ink3)
        ChipFlow(Modifier.padding(top = 6.dp)) {
            draft.months.forEachIndexed { i, month ->
                FilterChip(
                    label = month.name.ifBlank { (i + 1).toString() },
                    selected = draft.startMonth == i + 1,
                    onClick = { onChange(draft.copy(startMonth = i + 1)) },
                    enabled = enabled,
                )
            }
        }
        CivilightTextField(
            value = draft.startDay,
            onValueChange = { onChange(draft.copy(startDay = it)) },
            label = WorldCopy.EVENT_DAY,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        if (draft.weekdays.isNotEmpty()) {
            CivilightText(WorldCopy.TODO_COPY_WORLD_CALENDAR_START_WEEKDAY_LABEL, type.small, Modifier.padding(top = 14.dp), color = colors.ink3)
            ChipFlow(Modifier.padding(top = 6.dp)) {
                draft.weekdays.forEachIndexed { i, name ->
                    FilterChip(
                        label = name.ifBlank { (i + 1).toString() },
                        selected = draft.startWeekday == i + 1,
                        onClick = { onChange(draft.copy(startWeekday = i + 1)) },
                        enabled = enabled,
                    )
                }
            }
        }
        val problem = draft.problem()
        CivilightButton(
            text = WorldCopy.BTN_SAVE,
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            enabled = enabled && problem == null,
        )
        if (problem != null && enabled) CivilightText(problem, type.small, Modifier.padding(top = 6.dp), color = colors.ink3)
    }
}
