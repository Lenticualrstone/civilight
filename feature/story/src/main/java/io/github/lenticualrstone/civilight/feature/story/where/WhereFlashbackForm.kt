package io.github.lenticualrstone.civilight.feature.story.where

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.designsystem.theme.ValueSwap
import io.github.lenticualrstone.civilight.model.project.CalendarMonth
import io.github.lenticualrstone.civilight.model.story.FlashbackMode
import io.github.lenticualrstone.civilight.model.story.FlashbackRelation

/**
 * The 회상 form under the switch (STO-24, board `RTFlashback`): `때 · 기준 | 회상은 직전 씬 기준 없음`,
 * segmented `기준 사건 | 직접`; `기준 사건 | 대정전 1195 바꾸기 ›` + `전 · 그날 · 후`, or the year field;
 * the result box `연도 1195 · 대정전 그날 | 이야기 시작 5년 전 · 연표에서`; `씬 3 때 기준 | 씬 1 · 회상 건너뜀`.
 * No 전 · 후 amount is drawn (Q20 open). Every control applies at once (one undo step each).
 */
@Composable
fun WhereFlashbackForm(ui: WhereUi, onAction: (FlashbackAction) -> Unit, onPickEvent: () -> Unit, onTimeline: () -> Unit) {
    val f = ui.flashback ?: return
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val enabled = !ui.readOnly
    // No `spacedBy`: a folded-away block would still add its gap. Each block carries its own 10 dp.
    Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CivilightText(WhereCopy.FLASHBACK_BASE, type.small, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
            CivilightText(WhereCopy.FLASHBACK_NO_BASE, type.small, color = colors.ink3, maxLines = 1)
        }
        SegmentedControl(
            options = listOf(WhereCopy.ANCHOR_EVENT, WhereCopy.DIRECT),
            selectedIndex = if (f.mode == FlashbackMode.ANCHOR) 0 else 1,
            onSelect = { i -> if (enabled) onAction(FlashbackAction.Mode(if (i == 0) FlashbackMode.ANCHOR else FlashbackMode.DIRECT)) },
            modifier = Modifier.padding(top = 10.dp),
        )
        // The two modes swap on the mode, not on the data (P3).
        ContentSwap(f.mode, Modifier.fillMaxWidth().padding(top = 10.dp)) { mode ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (mode) {
                    FlashbackMode.ANCHOR -> {
                        EventRow(f, enabled, onPickEvent)
                        ChipFlow {
                            listOf(
                                FlashbackRelation.BEFORE to WhereCopy.BEFORE,
                                FlashbackRelation.ON to WhereCopy.ON_THE_DAY,
                                FlashbackRelation.AFTER to WhereCopy.AFTER,
                            ).forEach { (relation, label) ->
                                FilterChip(label, f.relation == relation, { onAction(FlashbackAction.Relation(relation)) }, enabled = enabled && f.eventKey != null)
                            }
                        }
                    }
                    FlashbackMode.DIRECT -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CivilightText(WhereCopy.YEAR, type.small, color = colors.ink2)
                            CommitField(
                                seed = f.year?.toString().orEmpty(),
                                onCommit = { text -> FlashbackModel.parseYear(text)?.takeIf { it != f.year }?.let { onAction(FlashbackAction.Year(it)) } },
                                modifier = Modifier.width(120.dp),
                                enabled = enabled,
                            )
                        }
                        f.months?.let { months -> CalendarDateRow(months, f, enabled, onAction) }
                    }
                }
            }
        }
        FoldRevealValue(ui.resultLine) { line -> Box(Modifier.padding(top = 10.dp)) { ResultBox(line, onTimeline) } }
        FoldRevealValue(f.skipLine) { (label, value) ->
            Row(Modifier.fillMaxWidth().padding(top = 10.dp).topHairline(colors.line).padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CivilightText(label, type.small, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
                ValueSwap(value, type.small, color = colors.ink2)
            }
        }
    }
}

/** `기준 사건 | 대정전 1195 바꾸기 ›`; a deleted event is dotted with `◇` (stored time kept). Tap = the event picker. */
@Composable
private fun EventRow(f: FlashbackUi, enabled: Boolean, onPickEvent: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .pressable(onClick = onPickEvent, enabled = enabled, scaleOnPress = false)
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CivilightText(WhereCopy.ANCHOR_EVENT, type.command, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
        f.eventName?.let { name ->
            if (f.eventDeleted) {
                CivilightText(WhereCopy.DELETED_MARK, type.small, color = colors.ink3)
                DottedText(name, type.command, Modifier, colors.ink3)
            } else {
                CivilightText(name, type.command, color = colors.ink, maxLines = 1)
            }
        }
        f.eventYear?.let { CivilightText(it.toString(), type.number, color = colors.ink3, maxLines = 1) }
        if (enabled) TextAction(WhereCopy.CHANGE_LINK, onPickEvent)
    }
}

/**
 * `직접` with the world calendar on (STO-24 "year and date", STO-18): the calendar's month names as
 * chips, then the day field `[d]일` (Q14 date format). A month pick keeps the day when it fits.
 */
@Composable
private fun CalendarDateRow(months: List<CalendarMonth>, f: FlashbackUi, enabled: Boolean, onAction: (FlashbackAction) -> Unit) {
    val month = f.date?.month
    ChipFlow {
        months.forEachIndexed { i, m ->
            val day = (f.date?.day ?: 1).coerceAtMost(m.days)
            FilterChip(m.name, month == i + 1, { if (month != i + 1) onAction(FlashbackAction.Date(i + 1, day)) }, enabled = enabled)
        }
    }
    if (month != null) {
        val maxDay = months.getOrNull(month - 1)?.days ?: return
        CommitField(
            seed = f.date.day.toString(),
            onCommit = { text -> FlashbackModel.parseDay(text, maxDay)?.takeIf { it != f.date.day }?.let { onAction(FlashbackAction.Date(month, it)) } },
            modifier = Modifier.width(120.dp),
            unit = WhereCopy.DATE_DAY_UNIT,
            enabled = enabled,
        )
    }
}
