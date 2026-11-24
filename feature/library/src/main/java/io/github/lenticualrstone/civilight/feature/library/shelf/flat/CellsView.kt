package io.github.lenticualrstone.civilight.feature.library.shelf.flat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.shelf.ProjectRowUi
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import kotlin.math.ceil
import kotlin.math.min

/**
 * 진행 칸 (r3 `LvLibCellsC` / `LvLibCellsI`): one row a project, one flat cell a unit (episode, or scene with a
 * single level): written filled, being written ringed, planned empty; chapters split by a rule. No rings for
 * progress (user feedback 2026-11-20). Cover: name and count over the strip; a tap opens. Inner: a legend, then
 * name and form | twelve cells a line with the chapter label | the last place, and `열기` on the picked row (a
 * second tap on it opens too). 설정만 reads `진행 없음 · 설정만`. Rows fill the height, then scroll.
 */
@Composable
internal fun CellsView(args: FlatArgs) {
    if (!args.wide) {
        CellRows(args, gap = 6f, minH = 56f) { i, item, h -> CoverCellRow(args, i, item, h) }
    } else {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Legend()
            Box(Modifier.weight(1f)) {
                CellRows(args, gap = 6f, minH = 64f, need = { item -> innerNeed(item) }) { i, item, h -> InnerCellRow(args, i, item, h) }
            }
        }
    }
}

@Composable
private fun CellRows(
    args: FlatArgs,
    gap: Float,
    minH: Float,
    need: (ShelfCardItem) -> Float = { 0f },
    row: @Composable (Int, ShelfCardItem, Float) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fill = FillRows.height(args.count, maxHeight.value, gap, minH)
        val heights = args.items.map { maxOf(fill, need(it)) }
        val scroll = heights.sum() + gap * (args.count - 1) > maxHeight.value + 1f
        Column(Modifier.fillMaxSize().then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier), verticalArrangement = Arrangement.spacedBy(gap.dp)) {
            args.items.forEachIndexed { i, item -> row(i, item, heights[i]) }
        }
    }
}

/** Inner rows: lines of twelve cells 20 tall, 8 apart, 6 + 6 padding. */
private fun innerNeed(item: ShelfCardItem): Float {
    val cells = (item as? ShelfCardItem.Project)?.card?.row?.facts?.progress ?: return 0f
    val lines = cells.groups.sumOf { ceil(it.size / 12f).toInt() }
    return lines * 20f + (lines - 1).coerceAtLeast(0) * 8f + 16f
}

@Composable
private fun CoverCellRow(args: FlatArgs, i: Int, item: ShelfCardItem, h: Float) {
    val colors = CivilightTheme.colors
    val look = FlatLook.current()
    val shape = RoundedCornerShape(14.dp)
    val row = (item as? ShelfCardItem.Project)?.card?.row
    val on = args.isOn(i)
    val label = row?.let { "${it.title} · ${progressLine(it)}" } ?: FlatViewCopy.LOCKED_PROJECT
    Box(
        Modifier
            .fillMaxWidth()
            .height(h.dp)
            .lockTour(args, item)
            .unhideOf(item, args)
            .clip(shape)
            .background(look.raise)
            .insetOutline(colors.line, 1.dp, shape)
            .ring(on, colors.acc, 14.dp)
            .pressable(onClick = { args.tap(i) }, role = Role.Button, shape = shape, scaleOnPress = false)
            .semantics { contentDescription = label; selected = on },
    ) {
        if (row == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CivilightIcon(IconId.Lock, contentDescription = null, size = 20.dp, tint = colors.ink2) }
            return@Box
        }
        Column(Modifier.fillMaxSize().padding(start = 12.dp, end = 44.dp, top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CivilightText(row.title, TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold), Modifier.weight(1f), color = colors.ink, maxLines = 1)
                CivilightText(progressLine(row), CivilightTheme.type.small.merge(TextStyle(fontWeight = FontWeight.SemiBold)), color = colors.ink2, maxLines = 1)
            }
            val cells = row.facts?.progress
            if (cells != null) {
                CellStrip(cells, Modifier.fillMaxWidth().height(12.dp))
            } else {
                CivilightText(FlatViewCopy.noProgress, CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
            }
        }
        Box(Modifier.align(Alignment.CenterEnd)) { MoreButton(args, item, tint = colors.ink3) }
    }
}

/** The cover strip: chapters side by side (weighted by their size), a rule between them. */
@Composable
private fun CellStrip(cells: ProgressCells, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val look = FlatLook.current()
    Canvas(modifier) {
        val total = cells.total
        val g = cells.groups.size
        // Many chapters or units: the rules and gaps shrink so the strip always fits its row (300 episodes too).
        val sep = if (g > 1) min(12.dp.toPx() * (g - 1), size.width * .2f) / (g - 1) else 0f
        val room = (size.width - sep * (g - 1)).coerceAtLeast(0f)
        val pitch = room / total
        val gap = when {
            pitch >= 6.dp.toPx() -> 3.dp.toPx()
            pitch >= 2.dp.toPx() -> 1.dp.toPx()
            else -> 0f
        }
        var x = 0f
        cells.groups.forEachIndexed { gi, group ->
            if (gi > 0) {
                drawRect(colors.edge, Offset(x + sep / 2f, 0f), Size(1.dp.toPx().coerceAtMost(sep), size.height))
                x += sep
            }
            val gw = room * group.size / total
            val cw = ((gw - gap * (group.size - 1)) / group.size).coerceAtLeast(0f)
            group.forEach { s ->
                cell(s, Offset(x, 0f), Size(cw, size.height), look, colors.acc, colors.sel, colors.line)
                x += cw + gap
            }
            x -= gap
        }
    }
}

private fun DrawScope.cell(s: CellState, o: Offset, sz: Size, look: FlatLook, acc: androidx.compose.ui.graphics.Color, sel: androidx.compose.ui.graphics.Color, line: androidx.compose.ui.graphics.Color) {
    val r = CornerRadius(2.dp.toPx())
    when (s) {
        CellState.DONE -> drawRoundRect(acc, o, sz, r)
        CellState.CURRENT -> {
            drawRoundRect(sel, o, sz, r)
            val w = 1.5.dp.toPx()
            drawRoundRect(acc, o + Offset(w / 2, w / 2), Size(sz.width - w, sz.height - w), r, style = Stroke(w))
        }
        CellState.PLANNED -> {
            drawRoundRect(look.cell, o, sz, r)
            drawRoundRect(line, o, sz, r, style = Stroke(1.dp.toPx()))
        }
    }
}

@Composable
private fun Legend() {
    val colors = CivilightTheme.colors
    val look = FlatLook.current()
    val small = CivilightTheme.type.small
    Row(Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(CellState.DONE to FlatViewCopy.LEGEND_DONE, CellState.CURRENT to FlatViewCopy.LEGEND_CURRENT, CellState.PLANNED to FlatViewCopy.LEGEND_PLANNED).forEach { (s, t) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Canvas(Modifier.size(12.dp)) { cell(s, Offset.Zero, size, look, colors.acc, colors.sel, colors.line) }
                CivilightText(t, small, color = colors.ink3, maxLines = 1)
            }
        }
        Spacer(Modifier.weight(1f))
        CivilightText(FlatViewCopy.LEGEND_NOTE, small, color = colors.ink3, maxLines = 1)
    }
}

@Composable
private fun InnerCellRow(args: FlatArgs, i: Int, item: ShelfCardItem, h: Float) {
    val colors = CivilightTheme.colors
    val look = FlatLook.current()
    val shape = RoundedCornerShape(14.dp)
    val row = (item as? ShelfCardItem.Project)?.card?.row
    val on = args.isOn(i)
    val label = row?.let { "${it.title} · ${progressLine(it)}" } ?: FlatViewCopy.LOCKED_PROJECT
    Box(
        Modifier
            .fillMaxWidth()
            .height(h.dp)
            .lockTour(args, item)
            .unhideOf(item, args)
            .clip(shape)
            .background(look.raise)
            .insetOutline(colors.line, 1.dp, shape)
            .ring(on, colors.acc, 14.dp)
            .pressable(onClick = { args.tap(i) }, role = Role.Button, shape = shape, scaleOnPress = false)
            .semantics { contentDescription = label; selected = on },
    ) {
        if (row == null) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                CivilightIcon(IconId.Lock, contentDescription = null, size = 20.dp, tint = colors.ink2)
                if (on) ActButton(args)
            }
            return@Box
        }
        Row(Modifier.fillMaxSize().padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.width(176.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                CivilightText(row.title, TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold), color = colors.ink, maxLines = 1)
                CivilightText(formLine(row), CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
                CivilightText(progressLine(row), CivilightTheme.type.small.merge(TextStyle(fontWeight = FontWeight.SemiBold)), Modifier.padding(top = 4.dp), color = colors.ink2, maxLines = 1)
            }
            Box(Modifier.weight(1f)) { InnerCells(row) }
            Row(Modifier.width(132.dp + 44.dp).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
                Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
                    CivilightText(FlatViewCopy.RECENT_SHORT, CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
                    row.facts?.place?.let { CivilightText(it, CivilightTheme.type.small, color = colors.ink2, maxLines = 1) }
                    if (on) OpenPill(args)
                }
                MoreButton(args, item, tint = colors.ink3)
            }
        }
    }
}

/** Twelve cells a line, the chapter label in front (board `.cg2 .cells .ch`). */
@Composable
private fun InnerCells(row: ProjectRowUi) {
    val colors = CivilightTheme.colors
    val cells = row.facts?.progress
    if (cells == null) {
        CivilightText(FlatViewCopy.noProgress, CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
        return
    }
    val look = FlatLook.current()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.groups.forEachIndexed { g, group ->
            group.chunked(12).forEachIndexed { li, line ->
                Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    val lab = if (li == 0 && cells.groupWord != null) "${g + 1}${cells.groupWord}" else ""
                    CivilightText(lab, CivilightTheme.type.micro.merge(TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)), Modifier.width(28.dp), color = colors.ink3, maxLines = 1)
                    Canvas(Modifier.weight(1f).fillMaxHeight()) {
                        val gap = 3.dp.toPx()
                        val cw = (size.width - gap * 11) / 12f
                        line.forEachIndexed { k, s -> cell(s, Offset(k * (cw + gap), 0f), Size(cw, size.height), look, colors.acc, colors.sel, colors.line) }
                    }
                }
            }
        }
    }
}

/** `열기` on the picked row (board `.og`). */
@Composable
private fun OpenPill(args: FlatArgs) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.height(28.dp).clip(shape).background(colors.pri).pressable(onClick = args.openPicked, role = Role.Button, shape = shape).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CivilightIcon(IconId.Book, contentDescription = null, size = 14.dp, tint = colors.onpri)
        CivilightText(FlatViewCopy.OPEN, CivilightTheme.type.small.merge(TextStyle(fontWeight = FontWeight.SemiBold)), color = colors.onpri, maxLines = 1)
    }
}
