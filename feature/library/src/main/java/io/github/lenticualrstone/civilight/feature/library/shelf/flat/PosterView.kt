package io.github.lenticualrstone.civilight.feature.library.shelf.flat

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 포스터 넘기기 (r3 `LvLibPosterC` / `LvLibPosterI`): one project a page, the cover large with a slow parallax (the
 * art moves 18 % of a page, the title 40 dp) and the title over a dark foot. ◀ ▶ and the dots under it; a drag
 * pages too (an extra, never the only way). Cover: the poster in front opens on a tap or `열기`. Inner: the
 * detail and 최근 위치 under the poster, and every poster as a thumbnail row. Reduced motion: no parallax, the
 * page cuts.
 */
@Composable
internal fun PosterView(args: FlatArgs) {
    if (!args.wide) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Posters(args, Modifier.fillMaxWidth().weight(1f), meta = true)
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundNav(io.github.lenticualrstone.civilight.designsystem.icon.IconId.Prev, FlatViewCopy.PREV, args.index > 0) { args.select(FlatViews.step(args.index, -1, args.count)) }
                Dots(args, Modifier.weight(1f))
                CivilightText(FlatViewCopy.count(args.index, args.count), CivilightTheme.type.small, color = CivilightTheme.colors.ink3, maxLines = 1)
                RoundNav(io.github.lenticualrstone.civilight.designsystem.icon.IconId.Next, FlatViewCopy.NEXT, args.index < args.count - 1) { args.select(FlatViews.step(args.index, 1, args.count)) }
                ActButton(args)
                MoreButton(args, args.selected)
            }
        }
    } else {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Posters(args, Modifier.fillMaxWidth().weight(1f), meta = false)
            PosterInfo(args)
            Thumbs(args)
        }
    }
}

@Composable
private fun Posters(args: FlatArgs, modifier: Modifier, meta: Boolean) {
    val motion = CivilightTheme.motion
    val look = FlatLook.current()
    val shape = RoundedCornerShape(22.dp)
    val count = args.count
    val pager = rememberPagerState(initialPage = args.index.coerceIn(0, (count - 1).coerceAtLeast(0))) { count }
    val select by rememberUpdatedState(args.select)
    val index by rememberUpdatedState(args.index)
    val pages by rememberUpdatedState(count)
    // The pick drives the pager (◀ ▶, thumbnails); a settled drag picks.
    LaunchedEffect(args.index, count) {
        if (count == 0 || pager.currentPage == args.index) return@LaunchedEffect
        if (motion.reduced) pager.scrollToPage(args.index) else pager.animateScrollToPage(args.index, animationSpec = motion.move(io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion.swipe))
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.distinctUntilChanged().collect { p -> if (p != index && p in 0 until pages) select(p) }
    }
    Box(
        modifier
            .shadow(14.dp, shape, ambientColor = look.shadow, spotColor = look.shadow)
            .clip(shape)
            .semantics { contentDescription = FlatViewCopy.POSTERS },
    ) {
        HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = !args.preview, key = { args.items.getOrNull(it)?.key ?: it }) { page ->
            val item = args.items.getOrNull(page) ?: return@HorizontalPager
            val d = (page - pager.currentPage) - pager.currentPageOffsetFraction
            Poster(args, page, item, if (motion.reduced) 0f else d, meta)
        }
    }
}

@Composable
private fun Poster(args: FlatArgs, i: Int, item: ShelfCardItem, d: Float, meta: Boolean) {
    val look = FlatLook.current()
    val row = (item as? ShelfCardItem.Project)?.card?.row
    if (row == null) {
        Box(
            Modifier.fillMaxSize().background(look.raise).lockTour(args, item)
                .pressable(onClick = args.input.onUnlock, role = Role.Button, scaleOnPress = false)
                .semantics { contentDescription = FlatViewCopy.LOCKED_PROJECT },
            contentAlignment = Alignment.Center,
        ) { LockMark(40.dp) }
        return
    }
    BoxWithConstraints(
        Modifier.fillMaxSize().unhideOf(item, args)
            .pressable(onClick = { args.tap(i) }, role = Role.Button, scaleOnPress = false, onClickLabel = FlatViewCopy.OPEN)
            .semantics { contentDescription = row.title; selected = i == args.index },
    ) {
        val w = maxWidth
        // Board `.pp .art`: 140 % wide (left/right −20 %), moved 18 % of a page per page of distance.
        Box(Modifier.fillMaxHeight().width(w * 1.4f).graphicsLayer { translationX = (-.2f * w.toPx()) + d * .18f * w.toPx() }) {
            Art(item, Modifier.fillMaxSize(), targetWidth = 900)
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.56f).background(Brush.verticalGradient(listOf(Color.Transparent, look.scrim))))
        Column(
            Modifier.align(Alignment.BottomStart).padding(20.dp).graphicsLayer { translationX = d * 40.dp.toPx() },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CivilightText(row.title, TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold), color = POSTER_INK, maxLines = 2)
            if (meta) {
                val small = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
                CivilightText(listOfNotNull(formWord(row.format), row.facts?.place).joinToString(" · "), small, color = POSTER_INK.copy(alpha = .85f), maxLines = 1)
                CivilightText("${row.edited} · ${FlatViewCopy.CHARS} ${FlatViewCopy.number(row.facts?.chars ?: 0)}", small, color = POSTER_INK.copy(alpha = .85f), maxLines = 1)
            }
        }
    }
}

/** Board `.pdots`: one bar a page, the current one long and accent. */
@Composable
private fun Dots(args: FlatArgs, modifier: Modifier) {
    val colors = CivilightTheme.colors
    Row(modifier.clip(RoundedCornerShape(2.dp)), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        // Many projects: the dots would not fit; the count beside them says where we are.
        if (args.count <= 16) {
            for (i in 0 until args.count) {
                val on = i == args.index
                val w by animateDpAsState(if (on) 22.dp else 10.dp, CivilightTheme.motion.move(), label = "dot")
                Box(Modifier.width(w).height(3.dp).clip(RoundedCornerShape(1.5.dp)).background(if (on) colors.acc else colors.edge.copy(alpha = colors.edge.alpha * .5f)))
            }
        }
    }
}

/** Inner info (board `.pin2`): lines, 글자, 인물, ◀ ▶ and `열기` | 최근 위치 five. */
@Composable
private fun PosterInfo(args: FlatArgs) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val item = args.selected
    Box(Modifier.fillMaxWidth().clip(shape).background(FlatLook.current().raise).insetOutline(colors.line, 1.dp, shape).padding(horizontal = 16.dp, vertical = 14.dp)) {
        when (item) {
            is ShelfCardItem.Project -> Row(Modifier.fillMaxWidth().unhideOf(item, args), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailLines(item.card.row)
                    FactLine(FlatViewCopy.CHARS, FlatViewCopy.number(item.card.row.facts?.chars ?: 0))
                    FactLine(FlatViewCopy.CAST, (item.card.row.facts?.cast ?: 0).toString())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NavCtl(args)
                        Spacer(Modifier.weight(1f))
                        ActButton(args)
                        MoreButton(args, item)
                    }
                }
                Box(Modifier.width(1.dp).height(200.dp).background(colors.line))
                RecentPlaces(args, item.card.row, 5, Modifier.weight(1f))
            }
            is ShelfCardItem.Lock -> Row(Modifier.fillMaxWidth().height(120.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                LockMark(36.dp)
                ActButton(args)
                Spacer(Modifier.width(16.dp))
                NavCtl(args)
            }
            else -> Unit
        }
    }
}

/** Board `.pth`: every poster as a thumbnail, the picked one ringed; a long row scrolls. */
@Composable
private fun Thumbs(args: FlatArgs) {
    val colors = CivilightTheme.colors
    BoxWithConstraints(Modifier.fillMaxWidth().height(84.dp).semantics { contentDescription = FlatViewCopy.ALL_POSTERS }) {
        val gap = 8f
        val fit = (maxWidth.value - gap * (args.count - 1)) / args.count.coerceAtLeast(1)
        val w = fit.coerceAtLeast(56f)
        Row(Modifier.fillMaxSize().horizontalScroll(rememberScrollState(), enabled = fit < 56f), horizontalArrangement = Arrangement.spacedBy(gap.dp)) {
            args.items.forEachIndexed { i, item ->
                val on = i == args.index
                val name = (item as? ShelfCardItem.Project)?.card?.row?.title ?: FlatViewCopy.LOCKED_PROJECT
                Box(
                    Modifier.width(w.dp).fillMaxHeight().unhideOf(item, args).ring(on, colors.acc, 10.dp)
                        .pressable(onClick = { args.select(i) }, role = Role.Button, shape = RoundedCornerShape(10.dp), scaleOnPress = false)
                        .semantics { contentDescription = name; selected = on },
                ) { Art(item, Modifier.fillMaxSize(), radius = 10.dp, targetWidth = 160, lockSize = 18.dp) }
            }
        }
    }
}

private val POSTER_INK = Color(0xFFF4F2EE)
