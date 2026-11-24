package io.github.lenticualrstone.civilight.feature.library.shelf.flat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem

/**
 * 타임라인 띠 (r3 `LvLibRibbonC` / `LvLibRibbonI`): the projects on a time axis of six equal segments (`오늘` …
 * `이전`, recent on the left), covers alternating above and below, each on a stem to its diamond on the axis. The
 * lock sits after a dashed divider, off the time scale (a locked project has no date on the axis). Under the
 * band, the picked project's detail with ◀ ▶ and `열기`. Too many projects for the width: the band scrolls sideways.
 */
@Composable
internal fun TimelineView(args: FlatArgs) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().weight(1f)) { Ribbon(args) }
        if (args.wide) WideDetail(args, nav = true) else RibbonDetail(args)
    }
}

@Composable
private fun Ribbon(args: FlatArgs) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(20.dp)
    val projects = args.items.withIndex().filter { it.value is ShelfCardItem.Project }
    val lockIndex = args.items.indexOfFirst { it is ShelfCardItem.Lock }
    BoxWithConstraints(Modifier.fillMaxSize().clip(shape).background(colors.tint).insetOutline(colors.line, 1.dp, shape)) {
        val baseCw = if (args.wide) 92f else 54f
        val baseCh = if (args.wide) 128f else 74f
        val lockW = if (lockIndex >= 0) (if (args.wide) 96f else 56f) else 0f
        // A short band shrinks the covers (label 28, stem ≥ 8, stagger ≤ 36, gaps 18).
        val half = (maxHeight.value - AXIS) / 2f
        val k = ((half - 36f - 28f - 8f - 18f) / baseCh).coerceIn(.45f, 1f)
        val cw = baseCw * k
        val ch = baseCh * k
        val ages = projects.map { (it.value as ShelfCardItem.Project).card.row.ageDays }
        val placed = remember(ages, maxWidth, cw, lockW) { TimelineLayout.place(ages, maxWidth.value, cw, lockW) }
        val xs = placed.first
        val zoneW = placed.second
        val x1 = zoneW - lockW
        Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState(), enabled = zoneW > maxWidth.value + 1f)) {
            Box(Modifier.width(zoneW.dp).fillMaxHeight()) {
                // The axis: accent → line, six segments, the dashed lock divider, then every diamond.
                Box(Modifier.fillMaxWidth().height(AXIS.dp).align(Alignment.Center)) {
                    val acc = colors.acc
                    val line = colors.line
                    val edge = colors.edge
                    Canvas(Modifier.fillMaxSize()) {
                        val y = size.height / 2f
                        drawRect(Brush.horizontalGradient(listOf(acc, line), startX = 12.dp.toPx(), endX = size.width - 12.dp.toPx()), Offset(12.dp.toPx(), y - 1.dp.toPx()), Size(size.width - 24.dp.toPx(), 2.dp.toPx()))
                        val segW = (x1 - 12f) / TimelineLayout.SEGMENTS.size
                        for (s in TimelineLayout.SEGMENTS.indices) drawRect(line, Offset((12f + segW * s).dp.toPx(), 0f), Size(1.dp.toPx(), size.height))
                        if (lockW > 0f) {
                            drawLine(edge, Offset((x1 + 2f).dp.toPx(), 8.dp.toPx()), Offset((x1 + 2f).dp.toPx(), size.height - 8.dp.toPx()), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
                        }
                    }
                    val segW = (x1 - 12f) / TimelineLayout.SEGMENTS.size
                    FlatViewCopy.SEGMENTS.forEachIndexed { s, label ->
                        CivilightText(
                            label,
                            type.small.merge(TextStyle(fontWeight = FontWeight.SemiBold)),
                            Modifier.offset(x = (12f + segW * s + 6f).dp).align(Alignment.BottomStart).padding(bottom = 1.dp),
                            color = if (s == 0) colors.acc else colors.ink3,
                            maxLines = 1,
                        )
                    }
                    projects.forEachIndexed { n, (i, _) ->
                        Diamond(on = args.isOn(i), modifier = Modifier.offset(x = (xs[n] - 5f).dp).align(Alignment.CenterStart))
                    }
                }
                // Cards above (even) and below (odd) the axis.
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                        projects.forEachIndexed { n, (i, item) ->
                            if (n % 2 == 0) RibbonCard(args, i, item, xs[n], cw, ch, up = true, top = TimelineLayout.stagger(n))
                        }
                        if (lockIndex >= 0) RibbonCard(args, lockIndex, args.items[lockIndex], zoneW - lockW / 2f - 2f, lockW - 16f, lockW - 16f, up = true, top = 10f)
                    }
                    Spacer(Modifier.height(AXIS.dp))
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                        projects.forEachIndexed { n, (i, item) ->
                            if (n % 2 == 1) RibbonCard(args, i, item, xs[n], cw, ch, up = false, top = TimelineLayout.stagger(n))
                        }
                    }
                }
            }
        }
    }
}

private const val AXIS = 40f

@Composable
private fun Diamond(on: Boolean, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val bg = colors.bg
    val acc = colors.acc
    val ink3 = colors.ink3
    Canvas(modifier.size(10.dp)) {
        rotate(45f) {
            if (on) {
                drawRect(acc)
            } else {
                drawRect(bg)
                drawRect(ink3, style = Stroke(2.dp.toPx()))
            }
        }
    }
}

/** One card: label, cover, stem (above the axis; mirrored below). */
@Composable
private fun RibbonCard(args: FlatArgs, i: Int, item: ShelfCardItem, x: Float, cw: Float, ch: Float, up: Boolean, top: Float) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val on = args.isOn(i)
    val lift by animateFloatAsState(if (on) (if (up) -4f else 4f) else 0f, flatMove(), label = "ribbonLift")
    val name = (item as? ShelfCardItem.Project)?.card?.row?.let { "${it.title} · ${it.edited}" } ?: FlatViewCopy.LOCKED_PROJECT
    val w = maxOf(cw + 14f, 44f)
    Column(
        Modifier
            .offset(x = (x - w / 2f).dp)
            .width(w.dp)
            .fillMaxHeight()
            .padding(top = if (up) top.dp else 0.dp, bottom = if (up) 0.dp else top.dp)
            .lockTour(args, item)
            .unhideOf(item, args)
            .pressable(onClick = { args.tap(i) }, role = Role.Button, scaleOnPress = false)
            .semantics { contentDescription = name; selected = on },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val label: @Composable () -> Unit = {
            if (item is ShelfCardItem.Project) {
                CivilightText(
                    item.card.row.title,
                    type.micro.merge(TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold)),
                    Modifier.widthIn(max = (cw + 14f).dp),
                    color = if (on) colors.ink else colors.ink2,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                )
            }
        }
        val cover: @Composable () -> Unit = {
            Art(
                item,
                Modifier.size(cw.dp, ch.dp).graphicsLayer { translationY = lift * density }.ring(on, colors.acc, 6.dp),
                radius = 6.dp,
                targetWidth = 240,
                lockSize = 20.dp,
            )
        }
        val stem: @Composable () -> Unit = { Box(Modifier.width(2.dp).weight(1f).background(if (on) colors.acc else colors.line)) }
        if (up) { label(); cover(); stem() } else { stem(); cover(); label() }
    }
}

/** Cover detail (board `.rd`): art 56 × 76 and the lines, then ◀ `n / N` ▶ and `열기`. */
@Composable
private fun RibbonDetail(args: FlatArgs) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val item = args.selected
    Column(
        Modifier.fillMaxWidth().clip(shape).background(FlatLook.current().raise).insetOutline(colors.line, 1.dp, shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (item) {
            is ShelfCardItem.Project -> Row(Modifier.fillMaxWidth().unhideOf(item, args), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Art(item, Modifier.size(56.dp, 76.dp), radius = 6.dp, targetWidth = 160)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { DetailLines(item.card.row) }
            }
            is ShelfCardItem.Lock -> Row(Modifier.fillMaxWidth().height(76.dp), verticalAlignment = Alignment.CenterVertically) { LockMark(28.dp) }
            else -> Unit
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NavCtl(args, countWeight = true)
            ActButton(args)
            MoreButton(args, item)
        }
    }
}

/**
 * The wide detail of the inner boards (board `wide_detail`): art | lines, 글자, 인물, controls | 최근 위치.
 * Locked: the lock mark and `잠금 풀기` (with ◀ ▶ when [nav]).
 */
@Composable
internal fun WideDetail(args: FlatArgs, nav: Boolean) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val item = args.selected
    // Sized by its content (large text grows it; the controls are never squeezed).
    Box(Modifier.fillMaxWidth().clip(shape).background(FlatLook.current().raise).insetOutline(colors.line, 1.dp, shape).padding(16.dp)) {
        when (item) {
            is ShelfCardItem.Project -> Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).unhideOf(item, args), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Art(item, Modifier.width(132.dp).fillMaxHeight(), radius = 10.dp, targetWidth = 320)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailLines(item.card.row)
                    FactLine(FlatViewCopy.CHARS, FlatViewCopy.number(item.card.row.facts?.chars ?: 0))
                    FactLine(FlatViewCopy.CAST, (item.card.row.facts?.cast ?: 0).toString())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (nav) NavCtl(args)
                        Spacer(Modifier.weight(1f))
                        ActButton(args)
                        MoreButton(args, item)
                    }
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
                RecentPlaces(args, item.card.row, 3, Modifier.width(174.dp))
            }
            is ShelfCardItem.Lock -> Row(Modifier.fillMaxWidth().height(120.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                LockMark(32.dp)
                ActButton(args)
                if (nav) {
                    Spacer(Modifier.width(16.dp))
                    NavCtl(args)
                }
            }
            else -> Unit
        }
    }
}
