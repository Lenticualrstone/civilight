package io.github.lenticualrstone.civilight.feature.library.shelf.flat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
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

/**
 * 원고지 격자 (r3 `LvLibPaperC` / `LvLibPaperI`): each project a sheet of 원고지, one glyph a cell: the name (two
 * lines at most), the character count with `자`, a progress row of cells (done filled, the next one ringed), the
 * form and the last edit along the bottom line. Inner: the total over all drawn projects above three columns,
 * the last place and recent places in the sheet, the `새 원고지` sheet, and the picked project's pane. The lock
 * sheet is blank cells and the lock. Rows fill the height down to a smallest sheet, then scroll.
 */
@Composable
internal fun PaperView(args: FlatArgs) {
    if (!args.wide) {
        PaperGridOf(args, cols = 2, k = 9, r = 6, newTile = false, minH = 150f)
    } else {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CivilightText(FlatViewCopy.number(FlatViews.totalChars(args.items)), TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold), color = CivilightTheme.colors.ink, maxLines = 1)
                    CivilightText("${FlatViewCopy.CHAR_UNIT} · ${FlatViewCopy.ALL_PROJECTS}", CivilightTheme.type.small, Modifier.padding(bottom = 4.dp), color = CivilightTheme.colors.ink3, maxLines = 1)
                }
                Box(Modifier.weight(1f)) { PaperGridOf(args, cols = 3, k = 7, r = 13, newTile = true, minH = 260f) }
            }
            PickedPane(args, Modifier.width(228.dp).fillMaxHeight())
        }
    }
}

@Composable
private fun PaperGridOf(args: FlatArgs, cols: Int, k: Int, r: Int, newTile: Boolean, minH: Float) {
    val total = args.count + if (newTile) 1 else 0
    val rows = ceil(total / cols.toFloat()).toInt().coerceAtLeast(1)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = FillRows.height(rows, maxHeight.value, 8f, minH)
        val scroll = h * rows + 8f * (rows - 1) > maxHeight.value + 1f
        Column(
            Modifier.fillMaxSize().then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth().height(h.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in 0 until cols) {
                        val i = row * cols + c
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            when {
                                i < args.count -> PaperTile(args, i, k, r)
                                i == args.count && newTile -> NewPaperTile(args)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaperTile(args: FlatArgs, i: Int, k: Int, r: Int) {
    val colors = CivilightTheme.colors
    val look = FlatLook.current()
    val item = args.items[i]
    val on = args.isOn(i)
    val shape = RoundedCornerShape(12.dp)
    val measurer = rememberTextMeasurer()
    val row = (item as? ShelfCardItem.Project)?.card?.row
    val label = row?.let { listOfNotNull(it.title, FlatViewCopy.chars(it.facts?.chars ?: 0), progressLine(it).ifEmpty { null }).joinToString(" · ") } ?: FlatViewCopy.LOCKED_PROJECT
    val lines = row?.let { paperLines(args, it, k, r) }
    val glyph = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.ink)
    val digit = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.ink)
    val unit = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.ink3)
    val lineStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.ink2)
    val acc = colors.acc
    Box(
        Modifier
            .fillMaxSize()
            .lockTour(args, item)
            .unhideOf(item, args)
            .clip(shape)
            .background(look.paper)
            .insetOutline(colors.line, 1.dp, shape)
            .ring(on, acc, 12.dp)
            .pressable(onClick = { args.tap(i) }, role = Role.Button, shape = shape)
            .semantics { contentDescription = label; selected = on },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(look.pgut, size = Size(size.width, 4.dp.toPx()))
            val left = 7.dp.toPx()
            val top = 10.dp.toPx()
            val gw = size.width - 14.dp.toPx()
            val gh = size.height - top - 7.dp.toPx()
            val cw = gw / k
            val ch = gh / r
            val stroke = 1.dp.toPx()
            // Rules: the outer top and left, then each cell's right and bottom.
            drawRect(look.pline, Offset(left, top), Size(gw, stroke))
            drawRect(look.pline, Offset(left, top), Size(stroke, gh))
            for (y in 0 until r) {
                val line = lines?.getOrNull(y)
                if (line is PaperLine.Text) {
                    drawRect(look.pline, Offset(left, top + (y + 1) * ch - stroke), Size(gw, stroke))
                    drawRect(look.pline, Offset(left + gw - stroke, top + y * ch), Size(stroke, ch))
                } else {
                    for (x in 0 until k) {
                        drawRect(look.pline, Offset(left + (x + 1) * cw - stroke, top + y * ch), Size(stroke, ch))
                        drawRect(look.pline, Offset(left + x * cw, top + (y + 1) * ch - stroke), Size(cw, stroke))
                    }
                }
                when (line) {
                    is PaperLine.Glyphs -> line.glyphs.forEachIndexed { x, g ->
                        val st = when (line.kind) { GlyphKind.DIGITS -> if (x == line.glyphs.size - 1) unit else digit; else -> glyph }
                        cellText(measurer, g, st, left + x * cw, top + y * ch, cw, ch)
                    }
                    is PaperLine.Cells -> line.cells.forEachIndexed { x, s ->
                        val o = Offset(left + x * cw, top + y * ch)
                        when (s) {
                            CellState.DONE -> drawRect(acc, o, Size(cw - stroke, ch - stroke))
                            CellState.CURRENT -> drawRect(acc, o + Offset(stroke, stroke), Size(cw - 3 * stroke, ch - 3 * stroke), style = Stroke(2.dp.toPx()))
                            CellState.PLANNED -> Unit
                        }
                    }
                    is PaperLine.Text -> {
                        val st = when (line.tone) { 1 -> lineStyle.copy(color = colors.ink3, fontWeight = FontWeight.SemiBold); 2 -> lineStyle.copy(color = acc, fontWeight = FontWeight.SemiBold); else -> lineStyle }
                        val reserve = if (line.last && !args.wide) 36.dp.toPx() else 0f
                        lineText(measurer, line.text, st, left + 4.dp.toPx(), top + y * ch, gw - 8.dp.toPx() - reserve, ch)
                    }
                    null -> Unit
                }
            }
        }
        if (item is ShelfCardItem.Lock) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CivilightIcon(IconId.Lock, contentDescription = null, size = 24.dp, tint = colors.ink2) }
        } else if (!args.wide) {
            Box(Modifier.align(Alignment.BottomEnd)) { MoreButton(args, item, tint = colors.ink3) }
        }
    }
}

private enum class GlyphKind { NAME, DIGITS }

private sealed interface PaperLine {
    data class Glyphs(val glyphs: List<String>, val kind: GlyphKind) : PaperLine
    data class Cells(val cells: List<CellState>) : PaperLine
    /** A line along a whole row; [tone] 0 = ink2, 1 = ink3 (진행 없음), 2 = accent (the last place). */
    data class Text(val text: String, val tone: Int = 0, val last: Boolean = false) : PaperLine
}

/** The rows of a sheet (board `paper_tile`), [r] in all; null rows are blank cells. */
private fun paperLines(args: FlatArgs, row: ProjectRowUi, k: Int, r: Int): List<PaperLine?> {
    val out = mutableListOf<PaperLine?>()
    val name = PaperGrid.nameRows(row.title, k)
    name.forEach { out += PaperLine.Glyphs(it.map { c -> if (c == ' ') "" else c.toString() }, GlyphKind.NAME) }
    while (out.size < 2) out += null
    out += null
    val digits = PaperGrid.countGlyphs(row.facts?.chars ?: 0, k)
    out += if (digits != null) PaperLine.Glyphs(digits, GlyphKind.DIGITS) else PaperLine.Text(FlatViewCopy.chars(row.facts?.chars ?: 0))
    if (args.wide) out += null
    val cells = row.facts?.progress
    if (cells != null) {
        out += PaperLine.Cells(PaperGrid.progressRow(cells, k))
    } else {
        out += PaperLine.Text(if (args.wide) FlatViewCopy.noProgress else "${FlatViewCopy.NO_PROGRESS} · ${progressLine(row)}", tone = 1)
    }
    if (args.wide) {
        out += PaperLine.Text(progressLine(row))
        out += null
        row.facts?.place?.let { out += PaperLine.Text(it, tone = 2) }
    }
    val last = PaperLine.Text(formLine(row), last = true)
    while (out.size < r - 1) out += null
    return out.take(r - 1) + last
}

private fun DrawScope.cellText(m: TextMeasurer, s: String, style: TextStyle, x: Float, y: Float, w: Float, h: Float) {
    if (s.isEmpty()) return
    val layout = m.measure(s, style, maxLines = 1, constraints = Constraints(maxWidth = (w.toInt() * 2).coerceAtLeast(1)))
    drawText(layout, topLeft = Offset(x + (w - layout.size.width) / 2f, y + (h - layout.size.height) / 2f))
}

private fun DrawScope.lineText(m: TextMeasurer, s: String, style: TextStyle, x: Float, y: Float, w: Float, h: Float) {
    if (w <= 1f) return
    val layout = m.measure(s, style, overflow = TextOverflow.Ellipsis, maxLines = 1, softWrap = false, constraints = Constraints(maxWidth = w.toInt()))
    drawText(layout, topLeft = Offset(x, y + (h - layout.size.height) / 2f))
}

@Composable
private fun NewPaperTile(args: FlatArgs) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxSize().clip(shape).background(colors.tint).insetOutline(colors.line, 1.dp, shape)
            .pressable(onClick = args.input.onNewProject, role = Role.Button, shape = shape),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CivilightIcon(IconId.Plus, contentDescription = null, size = 22.dp, tint = colors.ink2)
        CivilightText(FlatViewCopy.NEW_PAPER, CivilightTheme.type.small.merge(TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)), color = colors.ink2, maxLines = 1)
    }
}
