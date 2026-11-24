package io.github.lenticualrstone.civilight.feature.library.shelf.flat

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCopy

/**
 * 표지 벽 (r3 `LvLibWallC` / `LvLibWallI`): covers in columns of weighted heights (masonry) that fill the area
 * exactly; each tile the cover with its name and `형식 · 고친 때` on a dark foot. The lock tile is neutral with
 * `잠금 풀기`; inner adds the `새 프로젝트` tile and the picked project's pane. Columns that would shrink a tile
 * under [WallLayout.MIN_TILE] scroll instead.
 */
@Composable
internal fun WallView(args: FlatArgs) {
    if (!args.wide) {
        WallColumns(args, cols = 2, newTile = false)
    } else {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight()) { WallColumns(args, cols = 3, newTile = true) }
            PickedPane(args, Modifier.width(228.dp).fillMaxHeight())
        }
    }
}

@Composable
private fun WallColumns(args: FlatArgs, cols: Int, newTile: Boolean) {
    val count = args.count + if (newTile) 1 else 0
    val columns = WallLayout.columns(count, cols)
    fun weightOf(i: Int): Float = if (i >= args.count) WallLayout.SMALL else WallLayout.weight(i, args.items[i])
    val weights = columns.map { c -> c.map(::weightOf) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val unit = WallLayout.unit(weights, maxHeight.value, 8f)
        val tallest = weights.maxOf { col -> col.sum() * unit + 8f * (col.size - 1).coerceAtLeast(0) }
        val scroll = tallest > maxHeight.value + 1f
        Row(Modifier.fillMaxSize().then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            columns.forEach { col ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    col.forEach { i ->
                        val h = (weightOf(i) * unit).dp
                        if (i >= args.count) NewTile(args, Modifier.fillMaxWidth().height(h)) else WallTile(args, i, Modifier.fillMaxWidth().height(h))
                    }
                }
            }
        }
    }
}

@Composable
private fun WallTile(args: FlatArgs, i: Int, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val item = args.items[i]
    val on = args.isOn(i)
    val shape = RoundedCornerShape(14.dp)
    val row = (item as? ShelfCardItem.Project)?.card?.row
    val label = row?.title ?: if (!args.wide) "${FlatViewCopy.LOCKED_PROJECT} · ${FlatViewCopy.UNLOCK}" else FlatViewCopy.LOCKED_PROJECT
    Box(
        modifier
            .lockTour(args, item)
            .unhideOf(item, args)
            .clip(shape)
            .ring(on, colors.acc, 14.dp)
            .pressable(onClick = { args.tap(i) }, role = Role.Button, shape = shape, scaleOnPress = false)
            .semantics { contentDescription = label; selected = on },
    ) {
        if (row == null) {
            Column(
                Modifier.fillMaxSize().background(colors.tint).insetOutline(colors.line, 1.dp, shape),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CivilightIcon(IconId.Lock, contentDescription = null, size = 24.dp, tint = colors.ink2)
                // Board `.ul`: the unlock word where a tap unlocks (cover); inner picks first, so only once picked.
                if (!args.wide || on) Row(
                    Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(colors.pri).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CivilightIcon(IconId.Fingerprint, contentDescription = null, size = 16.dp, tint = colors.onpri)
                    CivilightText(FlatViewCopy.UNLOCK, CivilightTheme.type.small.merge(TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)), color = colors.onpri, maxLines = 1)
                }
            }
            return@Box
        }
        Art(item, Modifier.fillMaxSize(), targetWidth = 520)
        // Board `.cap`: the foot gradient and the light caption (fixed colours over any cover).
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(0f to CAP_TOP, .4f to CAP_MID, 1f to CAP_END))
                .padding(start = 12.dp, end = 0.dp, top = 34.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f).padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                CivilightText(row.title, TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, shadow = CAP_SHADOW), color = CAP_INK, maxLines = 1)
                CivilightText(formLine(row), TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, shadow = CAP_SHADOW), color = CAP_INK.copy(alpha = .9f), maxLines = 1)
            }
            if (!args.wide) MoreButton(args, item, tint = CAP_INK)
        }
    }
}

@Composable
private fun NewTile(args: FlatArgs, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.clip(shape).insetOutline(colors.edge, 1.5.dp, shape).pressable(onClick = args.input.onNewProject, role = Role.Button, shape = shape),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CivilightIcon(IconId.Plus, contentDescription = null, size = 22.dp, tint = colors.ink2)
        CivilightText(ShelfCopy.NEW_PROJECT, CivilightTheme.type.small.merge(TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)), color = colors.ink2, maxLines = 1)
    }
}

private val CAP_TOP = Color(8, 9, 11).copy(alpha = 0f)
private val CAP_MID = Color(8, 9, 11).copy(alpha = .62f)
private val CAP_END = Color(8, 9, 11).copy(alpha = .88f)
private val CAP_INK = Color(0xFFF4F2EE)
private val CAP_SHADOW = Shadow(Color.Black.copy(alpha = .5f), androidx.compose.ui.geometry.Offset(0f, 1f), 2f)
