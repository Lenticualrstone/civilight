package io.github.lenticualrstone.civilight.feature.story.where

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.OutlinedSegmented
import io.github.lenticualrstone.civilight.designsystem.component.SegmentItem
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.story.time.word
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.story.TimePrecision

/**
 * The 때 part of `SceneWhere` (DS-58 WhenPicker anatomy, built from its parts because STO-20 adds an
 * exact-time field and the `직접` story day): base line, day chips, band chips + clock field, `정밀도`,
 * the result box, `기준을 직전 씬으로` and the order line.
 */
@Composable
fun WhereTimeSection(ui: WhereUi, onAction: (WhenAction) -> Unit, onRebase: () -> Unit, onTimeline: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val c = ui.controls
    val sheet = ui.sheet
    val enabled = !ui.readOnly && c.editable
    Column(Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth()) {
            CivilightText(sheet.baseLine?.label ?: WhereCopy.FIELD_TIME, type.small, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
            sheet.baseLine?.value?.let { CivilightText(it, type.small, color = colors.ink2, maxLines = 1) }
        }
        if (c.editable) {
            ChipFlow {
                listOf(
                    WhenDay.SAME_DAY to WhereCopy.SAME_DAY,
                    WhenDay.PLUS_ONE to WhereCopy.PLUS_ONE,
                    WhenDay.PLUS_TWO to WhereCopy.PLUS_TWO,
                    WhenDay.DIRECT to WhereCopy.DIRECT,
                ).forEach { (day, label) ->
                    val usable = enabled && (day == WhenDay.DIRECT || !c.onlyDirect)
                    FilterChip(label, c.day == day, { onAction(WhenAction.Day(day)) }, enabled = usable)
                }
            }
            if (c.day == WhenDay.DIRECT) DirectDayField(c.directDay, enabled) { onAction(WhenAction.DirectDay(it)) }
            BandRow(c.band, c.clock?.toString(), enabled && c.bandsEnabled, onAction)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CivilightText(WhereCopy.PRECISION, type.small, Modifier.weight(1f), color = colors.ink3)
                OutlinedSegmented(
                    items = listOf(SegmentItem(WhereCopy.EXACT), SegmentItem(WhereCopy.APPROX)),
                    selectedIndex = if (c.precision == TimePrecision.APPROX) 1 else 0,
                    onSelect = { i -> if (enabled && c.precisionEnabled) onAction(WhenAction.Precision(if (i == 1) TimePrecision.APPROX else TimePrecision.EXACT)) },
                    height = 32.dp,
                )
            }
        }
        ui.resultLine?.let { ResultBox(it, onTimeline) }
        if (sheet.rebaseToPrevious && enabled) TextAction(WhereCopy.REBASE_TO_PREVIOUS, onRebase, color = colors.acc)
        // `직전 씬보다 앞 · 약 9시간 전` (STO-25): a notice in `ink3`, never a ▲.
        sheet.orderLine?.let { CivilightText(it, type.small, color = colors.ink3) }
    }
}

/** Band chips `새벽 · 아침 · 낮 · 저녁 · 밤` and the mono exact-time field (`05:40`), committed on Done or focus loss. */
@Composable
private fun BandRow(band: Band?, clock: String?, enabled: Boolean, onAction: (WhenAction) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChipFlow(Modifier.weight(1f)) {
            Band.entries.forEach { b -> FilterChip(b.word, band == b, { onAction(WhenAction.PickBand(b)) }, enabled = enabled) }
        }
        CommitField(
            seed = clock.orEmpty(),
            onCommit = { text ->
                when (val d = WhereInputs.clock(text)) {
                    is WhereInputs.ClockDraft.Value -> onAction(WhenAction.SetClock(d.clock))
                    WhereInputs.ClockDraft.Clear -> if (clock != null) onAction(WhenAction.SetClock(null))
                    WhereInputs.ClockDraft.Incomplete -> Unit
                }
            },
            modifier = Modifier.width(84.dp),
            placeholder = WhereCopy.CLOCK_HINT,
            enabled = enabled,
        )
    }
}

/** `이야기` [day] `일째` for a `직접` fixed point, committed on Done or focus loss. */
@Composable
private fun DirectDayField(day: Int?, enabled: Boolean, onDay: (Int) -> Unit) {
    val colors = CivilightTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CivilightText(WhereCopy.STORY, CivilightTheme.type.small, color = colors.ink2)
        CommitField(
            seed = day?.toString().orEmpty(),
            onCommit = { text -> WhereInputs.storyDay(text)?.takeIf { it != day }?.let(onDay) },
            modifier = Modifier.width(120.dp),
            unit = WhereCopy.DAY_UNIT,
            enabled = enabled,
            textStyle = CivilightTheme.type.field,
        )
    }
}

/** Result box (`sel`, radius 12): `1-7 · 2일째 · 새벽` and `이야기 약 13일째 · 연표에서` with the 연표 link. */
@Composable
internal fun ResultBox(line: String, onTimeline: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val parts = line.split(" | ")
    val sub = parts.getOrNull(1)
    val subText = sub?.removeSuffix(WhereCopy.ON_TIMELINE)?.removeSuffix(" · ")?.takeIf { it.isNotEmpty() }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.sel).padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            CivilightText(parts[0], type.itemTitle.merge(TextStyle(fontWeight = FontWeight.Bold)), color = colors.ink, maxLines = 1)
            subText?.let { CivilightText(it, type.small, color = colors.ink3, maxLines = 1) }
        }
        if (sub?.endsWith(WhereCopy.ON_TIMELINE) == true) TextAction(WhereCopy.ON_TIMELINE, onTimeline, color = colors.acc)
    }
}
