package io.github.lenticualrstone.civilight.feature.library.shelf.flat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem

/**
 * 표 (r3 `LvLibTableC` / `LvLibTableI`): a dense table, sortable by its column heads (the sorted head in accent
 * with its arrow), the lock row pinned last. Cover: 이름 (with the form) · 진행 · 고친 때; a tap opens. Inner:
 * 이름 · 형식 · 진행 · 글자 · 고친 때 with the total, and the picked project's wide detail under it. The rows fill the
 * table, then scroll.
 */
@Composable
internal fun TableView(args: FlatArgs) {
    var sortName by rememberSaveable { mutableStateOf(TableSort.EDITED.name) }
    var desc by rememberSaveable { mutableStateOf(false) }
    val sort = TableSort.valueOf(sortName)
    val ordered = remember(args.items, sort, desc) { TableOrder.order(args.items, sort, desc) }
    val onSort: (TableSort) -> Unit = { t -> TableOrder.toggle(sort, desc, t).let { (s, d) -> sortName = s.name; desc = d } }
    if (!args.wide) {
        Table(args, ordered, sort, desc, onSort, columns = COVER_COLS, Modifier.fillMaxSize(), rowMin = 44f, fill = true)
    } else {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Table(args, ordered, sort, desc, onSort, columns = INNER_COLS, Modifier.fillMaxWidth().weight(1f), rowMin = 52f, fill = false)
            WideDetail(args, nav = false)
        }
    }
}

private enum class Col(val sort: TableSort?, val label: String, val width: Dp?, val end: Boolean = false) {
    NAME(TableSort.NAME, FlatViewCopy.NAME, null),
    NAME_FORM(TableSort.NAME, FlatViewCopy.NAME, null),
    FORM(TableSort.FORM, FlatViewCopy.FORM, 72.dp),
    PROGRESS(TableSort.PROGRESS, FlatViewCopy.PROGRESS, 84.dp),
    PROGRESS_WIDE(TableSort.PROGRESS, FlatViewCopy.PROGRESS, 132.dp),
    CHARS(TableSort.CHARS, FlatViewCopy.CHARS, 76.dp, end = true),
    EDITED(TableSort.EDITED, FlatViewCopy.EDITED, 64.dp, end = true),
    EDITED_WIDE(TableSort.EDITED, FlatViewCopy.EDITED, 76.dp, end = true),
}

private val COVER_COLS = listOf(Col.NAME_FORM, Col.PROGRESS, Col.EDITED)
private val INNER_COLS = listOf(Col.NAME, Col.FORM, Col.PROGRESS_WIDE, Col.CHARS, Col.EDITED_WIDE)

/** The `⋯` column at the end of every row (not on the board; see [MoreButton]). */
private val MORE_W = 40.dp

@Composable
private fun Table(
    args: FlatArgs,
    ordered: List<ShelfCardItem>,
    sort: TableSort,
    desc: Boolean,
    onSort: (TableSort) -> Unit,
    columns: List<Col>,
    modifier: Modifier,
    rowMin: Float,
    fill: Boolean,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.clip(shape).background(FlatLook.current().raise).insetOutline(colors.line, 1.dp, shape)) {
        // Head.
        Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            columns.forEach { c -> Cell(c) { HeadButton(c, sort, desc, onSort) } }
            Box(Modifier.width(MORE_W))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.edge))
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val h = if (fill) FillRows.height(ordered.size, maxHeight.value, 0f, rowMin) else rowMin
            val scroll = h * ordered.size > maxHeight.value + 1f
            Column(Modifier.fillMaxSize().then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
                ordered.forEachIndexed { n, item ->
                    val i = args.items.indexOf(item)
                    if (n > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                    TableRow(args, i, item, columns, h)
                }
            }
        }
        // Foot.
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.edge))
        Row(Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            val foot = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
            val line = if (args.wide) "${FlatViewCopy.SUM} · ${FlatViews.countLine(args.items)}" else FlatViews.countLine(args.items)
            CivilightText(line, foot, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
            CivilightText(if (args.wide) FlatViewCopy.number(FlatViews.totalChars(args.items)) else FlatViewCopy.chars(FlatViews.totalChars(args.items)), foot, color = colors.ink3, maxLines = 1)
            if (args.wide) Box(Modifier.width(76.dp + 8.dp + MORE_W))
        }
    }
}

@Composable
private fun RowScope.Cell(c: Col, content: @Composable () -> Unit) {
    val m = if (c.width == null) Modifier.weight(1f) else Modifier.width(c.width)
    Box(m, contentAlignment = if (c.end) Alignment.CenterEnd else Alignment.CenterStart) { content() }
}

@Composable
private fun HeadButton(c: Col, sort: TableSort, desc: Boolean, onSort: (TableSort) -> Unit) {
    val colors = CivilightTheme.colors
    val s = c.sort ?: return
    val on = s == sort
    // 고친 때 sorts by age: ascending (newest first) is drawn ▼, as on the board.
    val up = on && (if (s == TableSort.EDITED) desc else !desc)
    Row(
        Modifier.widthIn(min = 44.dp).height(44.dp)
            .pressable(onClick = { onSort(s) }, role = Role.Button, scaleOnPress = false)
            .semantics { contentDescription = FlatViewCopy.sortBy(c.label); selected = on },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp, if (c.end) Alignment.End else Alignment.Start),
    ) {
        CivilightText(c.label, TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = .4.sp), color = if (on) colors.acc else colors.ink3, maxLines = 1)
        CivilightIcon(IconId.Down, contentDescription = null, size = 14.dp, tint = colors.acc, modifier = Modifier.alpha(if (on) 1f else 0f).rotate(if (up) 180f else 0f))
    }
}

@Composable
private fun TableRow(args: FlatArgs, i: Int, item: ShelfCardItem, columns: List<Col>, h: Float) {
    val colors = CivilightTheme.colors
    val on = args.isOn(i)
    val row = (item as? ShelfCardItem.Project)?.card?.row
    val label = row?.let { "${it.title} · ${formWord(it.format)} · ${it.edited}" } ?: FlatViewCopy.LOCKED_PROJECT
    Row(
        Modifier
            .fillMaxWidth()
            .height(h.dp)
            .lockTour(args, item)
            .unhideOf(item, args)
            .background(if (row == null) colors.tint else if (on) colors.sel else androidx.compose.ui.graphics.Color.Transparent)
            .then(if (on) Modifier.insetStart(colors.acc) else Modifier)
            .pressable(onClick = { args.tap(i) }, role = Role.Button, scaleOnPress = false)
            .semantics { contentDescription = label; selected = on }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (row == null) {
            CivilightIcon(IconId.Lock, contentDescription = null, size = 18.dp, tint = colors.ink2)
            return@Row
        }
        val body = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
        val name = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
        columns.forEach { c ->
            Cell(c) {
                when (c) {
                    Col.NAME -> CivilightText(row.title, name, color = colors.ink, maxLines = 1)
                    Col.NAME_FORM -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        CivilightText(row.title, name, color = colors.ink, maxLines = 1)
                        CivilightText(formWord(row.format), CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
                    }
                    Col.FORM -> CivilightText(formWord(row.format), body, color = colors.ink2, maxLines = 1)
                    Col.PROGRESS -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        ProgressBar(row.facts?.progress?.percent ?: 0, Modifier.fillMaxWidth())
                        CivilightText(progressShort(row.facts), CivilightTheme.type.small.merge(TextStyle(fontWeight = FontWeight.SemiBold)), color = colors.ink2, maxLines = 1)
                    }
                    Col.PROGRESS_WIDE -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProgressBar(row.facts?.progress?.percent ?: 0, Modifier.weight(1f))
                        CivilightText(progressShort(row.facts), CivilightTheme.type.small.merge(TextStyle(fontWeight = FontWeight.SemiBold)), Modifier.widthIn(min = 40.dp), color = colors.ink2, maxLines = 1, textAlign = TextAlign.End)
                    }
                    Col.CHARS -> CivilightText(FlatViewCopy.number(row.facts?.chars ?: 0), body, color = colors.ink2, maxLines = 1, textAlign = TextAlign.End)
                    Col.EDITED, Col.EDITED_WIDE -> CivilightText(row.edited, body, color = colors.ink2, maxLines = 1, textAlign = TextAlign.End)
                }
            }
        }
        Box(Modifier.width(MORE_W), contentAlignment = Alignment.Center) { MoreButton(args, item, tint = colors.ink3) }
    }
}

/** `6 / 20`, or `카드 · 인물 N`'s count for 설정만 (board `prog_txt`). */
private fun progressShort(facts: RowFacts?): String = facts?.progress?.let { "${it.done} / ${it.total}" } ?: facts?.let { FlatViewCopy.cast(it.cast) } ?: ""

/** Board `.tr.on`: a 3 dp accent bar on the start edge. */
private fun Modifier.insetStart(color: androidx.compose.ui.graphics.Color): Modifier =
    this.drawBehind { drawRect(color, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)) }
