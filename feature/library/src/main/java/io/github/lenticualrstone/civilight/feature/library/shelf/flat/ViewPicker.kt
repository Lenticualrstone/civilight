package io.github.lenticualrstone.civilight.feature.library.shelf.flat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.feature.library.shelf.ProjectRow
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCards
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCopy
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfUi
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewInput
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewRenderers
import io.github.lenticualrstone.civilight.feature.library.shelf.views.previewInert
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * (0.2.3, r3 `VpLibC` / `VpLibI` / `VpStory`) 보기 고르기 with a live preview. Picking a style (a tile, ◀ ▶, or a
 * drag on the stage) only previews it: the user's own projects drawn by that very renderer, inert. `이 보기로`
 * applies it ([onApply]); `취소`, the scrim and back leave the stored style as it was ([onCancel]).
 * Cover: a sheet over the current screen (the stage with both neighbours peeking, the name row, the grid of
 * every style in its group, the buttons). Inner: a dialog, the list on the left with each style's words, the
 * big inner preview on the right with the cover preview under it.
 */
@Composable
internal fun LibraryViewPicker(ui: ShelfUi, stored: LibraryStyle, onApply: (LibraryStyle) -> Unit, onCancel: () -> Unit) {
    val entries = remember { ViewPick.entries() }
    val cur = ViewPick.currentIndex(entries, stored)
    var sel by rememberSaveable { mutableIntStateOf(cur) }
    val motion = CivilightTheme.motion
    val scope = rememberCoroutineScope()
    val shown = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown.animateTo(1f, motion.spec(CivilightMotion.enter)) }
    fun close(then: () -> Unit) {
        if (closing) return
        closing = true
        scope.launch {
            shown.animateTo(0f, motion.spec(CivilightMotion.exit))
            then()
        }
    }
    val cancel = { close(onCancel) }
    val apply = { if (ViewPick.canApply(sel, cur)) entries.getOrNull(sel)?.style?.let { s -> close { onApply(s) } } ?: Unit else cancel() }
    LocalBackHandler.current(true, cancel)
    val look = FlatLook.current()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val hostW = maxWidth
        val hostH = maxHeight
        val wide = ShelfCards.isWide(hostW.value.toInt())
        // Scrim (board `.scr`): a tap cancels.
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = shown.value }.background(look.scrim)
                .pointerInput(Unit) { detectTapGestures { cancel() } }
                .clearAndSetSemantics { },
        )
        val state = PickState(entries, sel, cur, { sel = ViewPick.wrap(it, entries.size) }, apply, cancel, ui, hostW, hostH)
        if (wide) {
            InnerDialog(state, Modifier.systemBarsPadding().graphicsLayer {
                alpha = shown.value
                val k = if (motion.reduced) 1f else .97f + .03f * shown.value
                scaleX = k
                scaleY = k
            })
        } else {
            CoverSheet(state, Modifier.systemBarsPadding().graphicsLayer {
                alpha = if (motion.reduced) shown.value else 1f
                translationY = if (motion.reduced) 0f else (1f - shown.value) * size.height * 1.05f
            })
        }
    }
}

private class PickState(
    val entries: List<PickEntry>,
    val sel: Int,
    val cur: Int,
    val select: (Int) -> Unit,
    val apply: () -> Unit,
    val cancel: () -> Unit,
    val ui: ShelfUi,
    val hostW: Dp,
    val hostH: Dp,
) {
    val picked: PickEntry get() = entries[sel]
    val n: Int get() = entries.size
}

// ---------------------------------------------------------------- cover sheet

@Composable
private fun CoverSheet(s: PickState, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    Column(
        modifier
            .fillMaxSize()
            .padding(top = 16.dp)
            .clip(shape)
            .background(colors.raise)
            .insetOutline(colors.line, 1.dp, shape)
            .pointerInput(Unit) { detectTapGestures { } }
            .semantics { paneTitle = "${FlatViewCopy.PICK_TITLE} · ${FlatViewCopy.PICK_KIND}" }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
    ) {
        PickHead(s, Modifier.height(28.dp))
        Spacer(Modifier.height(12.dp))
        Stage(s, Modifier.fillMaxWidth().weight(1f))
        Spacer(Modifier.height(10.dp))
        NameRow(s)
        Spacer(Modifier.height(10.dp))
        TileGrid(s)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CivilightButton(FlatViewCopy.CANCEL, s.cancel, Modifier.weight(1f).height(48.dp), variant = ButtonVariant.GHOST)
            CivilightButton(FlatViewCopy.APPLY, s.apply, Modifier.weight(2f).height(48.dp), variant = ButtonVariant.PRIMARY, icon = IconId.Check, enabled = ViewPick.canApply(s.sel, s.cur))
        }
    }
}

@Composable
private fun PickHead(s: PickState, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CivilightText(FlatViewCopy.PICK_TITLE, TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold), color = colors.ink, maxLines = 1)
        CivilightText(FlatViewCopy.PICK_KIND, type.small, color = colors.ink3, maxLines = 1)
        Spacer(Modifier.weight(1f))
        CivilightText(FlatViewCopy.now(s.entries[s.cur].label), type.small, color = colors.ink3, maxLines = 1)
    }
}

/**
 * The stage (board `.stg`): the picked style big, its neighbours smaller and dimmed at the sides; a drag moves
 * them with the finger and a release past [ViewPick.SWIPE] steps (◀ ▶ do the same). Only these three are composed.
 */
@Composable
private fun Stage(s: PickState, modifier: Modifier) {
    val colors = CivilightTheme.colors
    // The drag handler outlives a pick: it reads the pick as it is when the finger lifts.
    val current = androidx.compose.runtime.rememberUpdatedState(s)
    val shape = RoundedCornerShape(20.dp)
    var drag by remember { mutableFloatStateOf(0f) }
    val motion = CivilightTheme.motion
    BoxWithConstraints(
        modifier
            .clip(shape)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(colors.tint, Color.Transparent)))
            .insetOutline(colors.line, 1.dp, shape)
            .semantics { contentDescription = "${FlatViewCopy.PREVIEW} · ${s.picked.label}" },
    ) {
        val nativeW = s.hostW.value
        val nativeH = s.hostH.value
        val scale = ((maxHeight.value - 52f) / nativeH).coerceIn(.08f, 1f)
        val side = scale * ViewPick.SIDE_SCALE
        val dx = nativeW * (scale + side) / 2f + 42f
        val window = listOf(-1, 0, 1).map { ViewPick.wrap(s.sel + it, s.n) }.distinct()
        for (i in window) {
            key(s.entries[i].style) {
                val d = ViewPick.distance(i, s.sel, s.n)
                val k by animateFloatAsState(if (d == 0) scale else side, motion.move(), label = "pickScale")
                val x by animateFloatAsState(d * dx, motion.move(), label = "pickX")
                val dim by animateFloatAsState(if (d == 0) 0f else 1f, motion.spec(), label = "pickDim")
                Box(
                    Modifier.align(Alignment.Center).offset(y = (-16).dp)
                        .graphicsLayer { translationX = (x + drag) * density },
                ) {
                    Scaled(nativeW, nativeH, k, radius = 28f) { StylePreview(s.entries[i].style, s.ui, nativeW.dp, nativeH.dp) }
                    // Board `.it2.side`: saturate .7 · brightness .82 (a dim veil over the neighbour).
                    if (dim > 0f) Box(Modifier.matchParentSize().clip(RoundedCornerShape((28f * k).dp)).background(Color.Black.copy(alpha = .22f * dim)))
                }
            }
        }
        // The drag layer over the scenes (they are inert): a horizontal drag previews the next or previous style.
        Box(
            Modifier.fillMaxSize().pointerInput(s.n) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val now = current.value
                        now.select(ViewPick.afterSwipe(now.sel, drag, now.n))
                        drag = 0f
                    },
                    onDragCancel = { drag = 0f },
                ) { change, amount ->
                    change.consume()
                    drag += amount / density
                }
            },
        )
        // Board `.hint`: `밀어서 바꾸기`.
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).height(24.dp).clip(RoundedCornerShape(12.dp)).background(colors.glass)
                .insetOutline(colors.line, 1.dp, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp).clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Glyph(SWIPE_D, 14.dp, colors.ink2)
            CivilightText(FlatViewCopy.SWIPE_HINT, TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold), color = colors.ink2, maxLines = 1)
        }
    }
}

/** ◀ name · fragment · `n / N` ▶ (board `.nmr`). */
@Composable
private fun NameRow(s: PickState) {
    val colors = CivilightTheme.colors
    Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RoundNav(IconId.Prev, FlatViewCopy.PREV_VIEW, true) { s.select(s.sel - 1) }
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CivilightText(s.picked.label, TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold), color = colors.ink, maxLines = 1)
            CivilightText("${s.picked.frag} · ${FlatViewCopy.count(s.sel, s.n)}", CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
        }
        RoundNav(IconId.Next, FlatViewCopy.NEXT_VIEW, true) { s.select(s.sel + 1) }
    }
}

/** Every style as a tile, one row a group with its side label (board `.grd`). */
@Composable
private fun TileGrid(s: PickState) {
    val colors = CivilightTheme.colors
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PickGroup.entries.forEach { g ->
            val ids = s.entries.indices.filter { s.entries[it].group == g }
            if (ids.isEmpty()) return@forEach
            Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(
                    Modifier.width(22.dp).fillMaxHeight().clip(RoundedCornerShape(8.dp)).insetOutline(colors.line, 1.dp, RoundedCornerShape(8.dp)).clearAndSetSemantics { },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ViewPick.groupShort(g).forEach { ch -> CivilightText(ch.toString(), TextStyle(fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold), color = colors.ink3, maxLines = 1) }
                }
                ids.forEach { i -> Tile(s, i, Modifier.weight(1f).fillMaxHeight()) }
                // The board's 기본 row holds two tiles; one alone keeps that width.
                if (ids.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Tile(s: PickState, i: Int, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val e = s.entries[i]
    val on = i == s.sel
    val shape = RoundedCornerShape(12.dp)
    Box(modifier) {
        Column(
            Modifier.fillMaxSize().clip(shape).background(if (on) colors.sel else colors.tint)
                .insetOutline(if (on) colors.acc else colors.line, if (on) 2.dp else 1.dp, shape)
                .pressable(onClick = { s.select(i) }, role = Role.Button, shape = shape, scaleOnPress = false)
                .semantics { contentDescription = "${e.label} · ${e.frag} · ${ViewPick.groupLabel(e.group)}"; selected = on },
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Glyph(e.glyph, 18.dp, if (on) colors.ink else colors.ink2)
            CivilightText(e.label, TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.2).sp), color = if (on) colors.ink else colors.ink2, maxLines = 1)
        }
        if (on) CheckBadge(Modifier.align(Alignment.TopStart).padding(3.dp))
        if (i == s.cur) NowBadge(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-8).dp))
    }
}

@Composable
private fun CheckBadge(modifier: Modifier) {
    val colors = CivilightTheme.colors
    Box(modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(colors.acc), contentAlignment = Alignment.Center) {
        Glyph(CHECK_D, 10.dp, colors.onpri, stroke = 2.4f)
    }
}

/** `지금` (board `.now`): the style on now. */
@Composable
private fun NowBadge(modifier: Modifier) {
    val colors = CivilightTheme.colors
    Box(modifier.height(16.dp).clip(RoundedCornerShape(8.dp)).background(colors.raise).insetOutline(colors.ink2, 1.5.dp, RoundedCornerShape(8.dp)).padding(horizontal = 5.dp), contentAlignment = Alignment.Center) {
        CivilightText(FlatViewCopy.NOW, TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold), color = colors.ink, maxLines = 1)
    }
}

// ---------------------------------------------------------------- inner dialog

@Composable
private fun InnerDialog(s: PickState, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .fillMaxSize()
            .padding(12.dp)
            .clip(shape)
            .background(colors.raise)
            .insetOutline(colors.line, 1.dp, shape)
            .pointerInput(Unit) { detectTapGestures { } }
            .semantics { paneTitle = "${FlatViewCopy.PICK_TITLE} · ${FlatViewCopy.PICK_KIND}" }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PickHead(s, Modifier.height(40.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val bodyH = maxHeight.value
            val rw = maxWidth.value - LIST_W - 12f
            val nativeW = s.hostW.value
            val nativeH = s.hostH.value
            // The big inner preview takes the right column's width, leaving at least 150 for the cover row.
            val k = min(rw / nativeW, (bodyH - 150f - 12f) / nativeH).coerceAtLeast(.05f)
            val bigH = nativeH * k
            val lowH = (bodyH - bigH - 12f).coerceAtLeast(0f)
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StyleList(s, Modifier.width(LIST_W.dp).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InnerStage(s, nativeW, nativeH, k, Modifier.fillMaxWidth().height(bigH.dp))
                    Row(Modifier.fillMaxWidth().height(lowH.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val ck = lowH / ViewPick.COVER_H
                        Box(
                            Modifier.width((ViewPick.COVER_W * ck).dp).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(colors.tint)
                                .insetOutline(colors.line, 1.dp, RoundedCornerShape(16.dp)).clearAndSetSemantics { },
                        ) {
                            key(s.picked.style) { Scaled(ViewPick.COVER_W, ViewPick.COVER_H, ck, radius = 0f) { StylePreview(s.picked.style, s.ui, ViewPick.COVER_W.dp, ViewPick.COVER_H.dp) } }
                        }
                        Column(Modifier.weight(1f).fillMaxHeight().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            CivilightText(s.picked.label, TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold), color = colors.ink, maxLines = 1)
                            CivilightText(s.picked.frag, CivilightTheme.type.small.merge(TextStyle(fontSize = 13.sp)), color = colors.ink2, maxLines = 1)
                            CivilightText(FlatViewCopy.BOTH_SCREENS, CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
                            Spacer(Modifier.weight(1f))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                RoundNav(IconId.Prev, FlatViewCopy.PREV_VIEW, true) { s.select(s.sel - 1) }
                                CivilightText(FlatViewCopy.count(s.sel, s.n), CivilightTheme.type.small.merge(TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)), Modifier.weight(1f), color = colors.ink2, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                RoundNav(IconId.Next, FlatViewCopy.NEXT_VIEW, true) { s.select(s.sel + 1) }
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f).clearAndSetSemantics { }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Glyph(SWIPE_D, 14.dp, colors.ink3)
                CivilightText("${FlatViewCopy.PREVIEW} · ${FlatViewCopy.SWIPE_HINT}", CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
            }
            CivilightButton(FlatViewCopy.CANCEL, s.cancel, Modifier.width(120.dp).height(48.dp), variant = ButtonVariant.GHOST)
            CivilightButton(FlatViewCopy.APPLY, s.apply, Modifier.width(200.dp).height(48.dp), variant = ButtonVariant.PRIMARY, icon = IconId.Check, enabled = ViewPick.canApply(s.sel, s.cur))
        }
    }
}

private const val LIST_W = 212f

/** The inner list (board `.lst`): a header a group, two rows a line, each row filling its share of the height. */
@Composable
private fun StyleList(s: PickState, modifier: Modifier) {
    val colors = CivilightTheme.colors
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PickGroup.entries.forEach { g ->
            val ids = s.entries.indices.filter { s.entries[it].group == g }
            if (ids.isEmpty()) return@forEach
            CivilightText("${ViewPick.groupLabel(g)} · ${ids.size}", CivilightTheme.type.small.merge(TextStyle(fontWeight = FontWeight.SemiBold)), Modifier.padding(horizontal = 4.dp).height(20.dp), color = colors.ink3, maxLines = 1)
            ids.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().height(52.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pair.forEach { i -> ListRow(s, i, Modifier.weight(1f).fillMaxHeight()) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ListRow(s: PickState, i: Int, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val e = s.entries[i]
    val on = i == s.sel
    val shape = RoundedCornerShape(14.dp)
    Box(modifier) {
        Column(
            Modifier.fillMaxSize().clip(shape).background(if (on) colors.sel else Color.Transparent)
                .insetOutline(if (on) colors.acc else colors.line, if (on) 2.dp else 1.dp, shape)
                .pressable(onClick = { s.select(i) }, role = Role.Button, shape = shape, scaleOnPress = false)
                .semantics { contentDescription = "${e.label} · ${e.frag} · ${ViewPick.groupLabel(e.group)}"; selected = on }
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Glyph(e.glyph, 16.dp, colors.ink2)
                CivilightText(e.label, TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold), color = colors.ink, maxLines = 1)
            }
            CivilightText(e.frag, CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
        }
        if (on) CheckBadge(Modifier.align(Alignment.TopStart).padding(5.dp))
        if (i == s.cur) NowBadge(Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = (-6).dp))
    }
}

/** The big inner preview: the picked style only (board: neighbours fully faded), moved by a drag. */
@Composable
private fun InnerStage(s: PickState, nativeW: Float, nativeH: Float, k: Float, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val current = androidx.compose.runtime.rememberUpdatedState(s)
    var drag by remember { mutableFloatStateOf(0f) }
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(colors.tint).insetOutline(colors.line, 1.dp, RoundedCornerShape(20.dp)).semantics { contentDescription = "${FlatViewCopy.PREVIEW} · ${s.picked.label}" }) {
        key(s.picked.style) {
            Box(Modifier.align(Alignment.Center).graphicsLayer { translationX = drag * density }) {
                Scaled(nativeW, nativeH, k, radius = 28f) { StylePreview(s.picked.style, s.ui, nativeW.dp, nativeH.dp) }
            }
        }
        Box(
            Modifier.matchParentSize().pointerInput(s.n) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val now = current.value
                        now.select(ViewPick.afterSwipe(now.sel, drag, now.n))
                        drag = 0f
                    },
                    onDragCancel = { drag = 0f },
                ) { change, amount ->
                    change.consume()
                    drag += amount / density
                }
            },
        )
    }
}

// ---------------------------------------------------------------- the previews

/** [content] laid out at its native size and drawn at [scale] (the layout inside is exactly the real one). */
@Composable
private fun Scaled(nativeW: Float, nativeH: Float, scale: Float, radius: Float, content: @Composable () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape((radius * scale).dp))
            .layout { m, _ ->
                val w = nativeW.dp.roundToPx()
                val h = nativeH.dp.roundToPx()
                val p = m.measure(Constraints.fixed(w, h))
                layout((w * scale).roundToInt(), (h * scale).roundToInt()) {
                    p.placeWithLayer(0, 0) {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                }
            },
    ) { content() }
}

/**
 * One style drawn as the 서재 would draw it, with the user's own (masked) projects, at [w] × [h]: the renderer
 * in preview mode under the board's glass head (`서재` · `보기 · <style> ▾` · `+`) and the search bar. Inert.
 */
@Composable
internal fun StylePreview(style: LibraryStyle, ui: ShelfUi, w: Dp, h: Dp) {
    val colors = CivilightTheme.colors
    Box(Modifier.size(w, h).background(colors.bg).previewInert()) {
        val wide = ShelfCards.isWide(w.value.toInt())
        val renderer = ShelfViewRenderers.of(style)
        if (renderer != null && ui.empty == null && (ui.cards.isNotEmpty() || ui.lockedGroup != null)) {
            renderer(
                ShelfViewInput(
                    ui = ui, wide = wide, onOpen = {}, onMenu = {}, onUnlock = {}, onNewProject = {},
                    tilt = false, barTop = PREVIEW_BAR, barBottom = PREVIEW_BAR, covered = true, preview = true,
                ),
            )
        } else {
            ListPreview(ui)
        }
        PreviewChrome(style)
    }
}

private const val PREVIEW_BAR = 72f

/** `목록` in a preview: the rows as the list draws them (no hidden group's names: only visible rows). */
@Composable
private fun ListPreview(ui: ShelfUi) {
    val colors = CivilightTheme.colors
    Column(Modifier.fillMaxSize().padding(top = (PREVIEW_BAR + 8f).dp, start = 24.dp, end = 24.dp)) {
        CivilightText(ui.sectionLabel, CivilightTheme.type.label, Modifier.padding(bottom = 4.dp), color = colors.ink3, maxLines = 1)
        ui.rows.take(12).forEach { row -> ProjectRow(row, onOpen = {}, onMenu = {}) }
        ui.empty?.let { CivilightText(it.title, CivilightTheme.type.body, Modifier.padding(top = 12.dp), color = colors.ink2, maxLines = 1) }
    }
}

/** The board's floating glass head and search bar over a preview. */
@Composable
private fun PreviewChrome(style: LibraryStyle) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Box(Modifier.fillMaxSize()) {
        Row(
            Modifier.align(Alignment.TopCenter).padding(8.dp).fillMaxWidth().height(56.dp).clip(shape).background(colors.glass).insetOutline(colors.line, 1.dp, shape).padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CivilightText(ShelfCopy.TITLE, TextStyle(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold), Modifier.weight(1f), color = colors.ink, maxLines = 1)
            Row(
                Modifier.height(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.tint).insetOutline(colors.line, 1.dp, RoundedCornerShape(14.dp)).padding(start = 10.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Glyph(ViewPick.glyph(style), 18.dp, colors.ink)
                CivilightText(io.github.lenticualrstone.civilight.feature.library.settings.ViewStyles.label(style), TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold), color = colors.ink, maxLines = 1)
                CivilightIcon(IconId.Down, contentDescription = null, size = 14.dp, tint = colors.ink3)
            }
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { CivilightIcon(IconId.Plus, contentDescription = null, size = 20.dp, tint = colors.ink) }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(8.dp).fillMaxWidth().height(56.dp).clip(shape).background(colors.glass).insetOutline(colors.line, 1.dp, shape).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CivilightIcon(IconId.Search, contentDescription = null, size = 18.dp, tint = colors.ink3)
            CivilightText(ShelfCopy.SEARCH_HINT, CivilightTheme.type.small.merge(TextStyle(fontSize = 14.sp)), color = colors.ink3, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------- glyphs

private const val CHECK_D = "M5 12.5l4.5 4.5L19 7.5"
private const val SWIPE_D = "M4 12h16M8 8l-4 4 4 4M16 8l4 4-4 4"

/** A board glyph: one stroke path on a 24 grid, 1.6 wide, round ends. */
@Composable
internal fun Glyph(d: String, size: Dp, tint: Color, stroke: Float = 1.6f) {
    val path = remember(d) { PathParser().parsePathString(d).toPath() }
    Canvas(Modifier.size(size).clearAndSetSemantics { }) {
        val k = this.size.minDimension / 24f
        scale(k, k, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            drawPath(path, tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/**
 * The 서재 head's `보기 · <style> ▾` (r3 `.vb`): the style's glyph, its name and ▾; opens the view picker. The
 * name is left out below the inner width (the head already holds the toggle and three buttons); the button
 * keeps its full name for accessibility.
 */
@Composable
internal fun ViewChip(style: LibraryStyle, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val label = io.github.lenticualrstone.civilight.feature.library.settings.ViewStyles.label(style)
    // The inner screen's single 서재 (not the 390 list pane of `목록 + 상세`).
    val named = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= ShelfCards.WIDE_MIN_DP &&
        io.github.lenticualrstone.civilight.ui.nav.LocalPaneRole.current != io.github.lenticualrstone.civilight.ui.nav.PaneRole.LIST
    run {
        Row(
            Modifier.height(44.dp).clip(shape).background(colors.tint).insetOutline(colors.line, 1.dp, shape)
                .pressable(onClick = onClick, role = Role.Button, shape = shape)
                .clearAndSetSemantics { contentDescription = FlatViewCopy.viewButton(label); this.role = Role.Button; onClick(label = FlatViewCopy.PICK_TITLE) { onClick(); true } }
                .padding(start = 10.dp, end = if (named) 12.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Glyph(ViewPick.glyph(style), 18.dp, colors.ink)
            if (named) CivilightText(label, TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold), color = colors.ink, maxLines = 1)
            CivilightIcon(IconId.Down, contentDescription = null, size = 14.dp, tint = colors.ink3)
        }
    }
}
