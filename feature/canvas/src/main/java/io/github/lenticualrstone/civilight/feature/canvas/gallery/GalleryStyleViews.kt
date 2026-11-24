package io.github.lenticualrstone.civilight.feature.canvas.gallery

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.animatedAlpha
import androidx.compose.animation.core.animateFloatAsState

/** The pages and ◀ ▶ order of one view (色 묶음 waits for the colour groups). */
class StylePlanSet(
    val pages: StylePages,
    val tagRows: List<TagRow> = emptyList(),
    val scene: ScenePlan? = null,
    val tones: List<GalleryTone>? = null,
    val toneOfPage: List<GalleryTone> = emptyList(),
) {
    val order: List<Int> get() = scene?.order ?: pages.order
}

@Composable
fun rememberStylePlan(s: StyleBodyState): StylePlanSet {
    val facts = s.model.facts
    val tones = if (s.style == GalleryStyle.TONE) rememberTones(s.model) else null
    return remember(s.style, facts, s.inner, tones) {
        when (s.style) {
            GalleryStyle.FILM -> StylePlanSet(GalleryStylePlans.film(facts))
            GalleryStyle.MOOD -> StylePlanSet(GalleryStylePlans.mood(facts))
            GalleryStyle.CLOTHESLINE -> StylePlanSet(GalleryStylePlans.clothesline(facts))
            GalleryStyle.EXHIBIT -> StylePlanSet(GalleryStylePlans.exhibit(facts, s.inner))
            GalleryStyle.TONE -> if (tones == null || tones.size != facts.size) StylePlanSet(StylePages(emptyList()))
            else GalleryStylePlans.tone(tones).let { (p, of) -> StylePlanSet(p, tones = tones, toneOfPage = of) }
            GalleryStyle.TAG_ROWS -> GalleryStylePlans.tagRows(facts).let { rows -> StylePlanSet(GalleryStylePlans.tagStylePages(rows), tagRows = rows) }
            GalleryStyle.SCENE_ORDER -> GalleryStylePlans.scene(facts).let { sp -> StylePlanSet(sp.placed, scene = sp) }
            GalleryStyle.LIST, GalleryStyle.GRID, GalleryStyle.CARD -> StylePlanSet(StylePages(emptyList()))
        }
    }
}

/**
 * An r3 gallery view (0.2.3, boards `NmLvGal*C` · `NmLvGal*I`) filling the area it is given: cover = the view;
 * inner = the view and the information pane (228 dp), except `필름` (info row under the viewer) and `전시실`
 * (detail bar under the wall).
 */
@Composable
fun GalleryStyleBody(s: StyleBodyState, modifier: Modifier = Modifier) {
    if (s.model.slots.isEmpty()) {
        Box(modifier)
        return
    }
    val plan = rememberStylePlan(s)
    val withPane = s.inner && s.style != GalleryStyle.FILM && s.style != GalleryStyle.EXHIBIT
    if (withPane) {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight()) { StyleView(s, plan) }
            StylePane(s, plan.order, Modifier.width(PANE_W).fillMaxHeight())
        }
    } else {
        Box(modifier) { StyleView(s, plan) }
    }
}

private val PANE_W = 228.dp

@Composable
private fun StyleView(s: StyleBodyState, plan: StylePlanSet) {
    when (s.style) {
        GalleryStyle.FILM -> FilmView(s, plan)
        GalleryStyle.MOOD -> MoodView(s, plan)
        GalleryStyle.CLOTHESLINE -> ClotheslineView(s, plan)
        GalleryStyle.EXHIBIT -> ExhibitView(s, plan)
        GalleryStyle.TONE -> ToneView(s, plan)
        GalleryStyle.TAG_ROWS -> TagRowsView(s, plan)
        GalleryStyle.SCENE_ORDER -> SceneOrderView(s, plan)
        // Drawn by the old bodies (GalleryScreen `ItemsBody`), never here. Exhaustive on purpose: a new style must
        // name its renderer here or fail to compile, never draw a blank area.
        GalleryStyle.LIST, GalleryStyle.GRID, GalleryStyle.CARD -> Unit
    }
}

/** A page of a paged view, sliding in the direction of travel (crossfade under reduced motion). */
@Composable
private fun PageSwap(page: Int, modifier: Modifier = Modifier, content: @Composable (Int) -> Unit) {
    val motion = CivilightTheme.motion
    AnimatedContent(
        targetState = page,
        transitionSpec = { motion.push(forward = targetState >= initialState) },
        modifier = modifier,
        label = "galleryStylePage",
    ) { p -> content(p) }
}

/** Swipe to the first place of the neighbouring page. */
@Composable
private fun Modifier.pageSwipe(s: StyleBodyState, pages: StylePages): Modifier = styleSwipe(s.interactive && pages.pages.size > 1) { d ->
    val p = pages.pageOf(s.sel) + d
    pages.pages.getOrNull(p)?.firstOrNull()?.let(s::go)
}

// ================================================================ 필름 (the strip is the pager)

@Composable
private fun FilmView(s: StyleBodyState, plan: StylePlanSet) {
    val m = styleMaterials()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilmViewer(s, plan.order, m, Modifier.weight(1f).fillMaxWidth())
        if (s.inner) FilmInfo(s)
        FilmStrip(s, m, Modifier.fillMaxWidth().height(if (s.inner) 150.dp else 96.dp))
    }
}

@Composable
private fun FilmViewer(s: StyleBodyState, order: List<Int>, m: StyleMaterials, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(18.dp)
    val slot = s.slot
    Box(
        modifier
            .clip(shape)
            .background(Brush.radialGradient(listOf(m.film2, m.film)))
            .insetOutline(colors.line, 1.dp, shape)
            .styleSwipe(s.interactive) { d -> s.go(GalleryStylePlans.step(order, s.sel, d)) },
    ) {
        val pad = if (s.inner) Modifier.padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 20.dp) else Modifier.padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 60.dp)
        PageSwap(s.sel, Modifier.fillMaxSize().then(pad)) { i ->
            val shown = s.model.slots.getOrNull(i)
            if (shown != null) FittedPicture(s, shown, if (shown is StyleSlot.Locked) 1f else (shown as StyleSlot.Item).info.ratio, 4.dp, m.film2, Color(0xFFB4B6BD), 32.dp)
        }
        CivilightText(
            GalleryStyleCopy.filmLabel(GalleryStylePlans.count(s.model.facts, order, s.sel)),
            type.small,
            Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 12.dp),
            color = m.edge2,
            maxLines = 1,
        )
        if (slot != null) {
            StyleRoundButton(
                if (slot is StyleSlot.Locked) IconId.Fingerprint else IconId.Expand,
                if (slot is StyleSlot.Locked) GalleryStyleCopy.BTN_UNLOCK else GalleryStyleCopy.BTN_ENLARGE,
                s::open,
                enabled = s.interactive,
                fill = Color.White.copy(alpha = 0.10f),
                ring = Color.White.copy(alpha = 0.16f),
                tint = m.filmInk,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 6.dp),
            )
        }
        if (!s.inner && slot != null) {
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                when (slot) {
                    is StyleSlot.Item -> {
                        CivilightText(slot.info.title, type.itemTitle, color = if (slot.info.untitled) m.filmInk.copy(alpha = 0.66f) else m.filmInk, maxLines = 1)
                        CivilightText(slot.info.meta, type.small, color = m.edge2, maxLines = 1)
                    }
                    is StyleSlot.Locked -> {
                        CivilightText(slot.label, type.itemTitle, color = m.filmInk, maxLines = 1)
                        CivilightText(GalleryStyleCopy.UNLOCK_SUB, type.small, color = m.edge2, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** A picture fitted at its own ratio in the box (shared element on the picked image). */
@Composable
private fun FittedPicture(s: StyleBodyState, slot: StyleSlot, ratio: Float, corner: Dp, lockFill: Color, lockTint: Color, lockSize: Dp) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val (w, h) = GalleryStylePlans.fit(maxWidth.value, maxHeight.value, ratio)
        val shared = (slot as? StyleSlot.Item)?.let { s.sharedImage(it.entry.key) } ?: Modifier
        Box(
            Modifier
                .size(w.dp, h.dp)
                .shadow(14.dp, RoundedCornerShape(corner), clip = false)
                .clip(RoundedCornerShape(corner))
                .then(shared),
        ) { StylePicture(slot, maxSide = GalleryImageSizes.CARD, lockSize = lockSize, lockFill = lockFill, lockTint = lockTint, chip = true) }
    }
}

@Composable
private fun FilmStrip(s: StyleBodyState, m: StyleMaterials, modifier: Modifier) {
    val fw = if (s.inner) 112.dp else 60.dp
    val band = if (s.inner) 22.dp else 18.dp
    val fg = if (s.inner) 12.dp else 8.dp
    val state = rememberLazyListState()
    val motion = CivilightTheme.motion
    val density = LocalDensity.current
    // Keep the picked frame centred (the strip is the pager).
    LaunchedEffect(s.sel) {
        if (s.sel < 0) return@LaunchedEffect
        var info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == s.sel }
        if (info == null) {
            state.scrollToItem((s.sel - 2).coerceAtLeast(0))
            info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == s.sel } ?: return@LaunchedEffect
        }
        val view = state.layoutInfo.viewportEndOffset - state.layoutInfo.viewportStartOffset
        val delta = (info.offset + info.size / 2 - view / 2).toFloat()
        if (motion.reduced) state.scrollBy(delta) else state.animateScrollBy(delta, motion.move(CivilightMotion.swipe))
    }
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(m.film)
            .semantics { contentDescription = GalleryStyleCopy.FILM_A11Y },
    ) {
        Canvas(Modifier.fillMaxWidth().height(band)) {
            val hole = 8.dp.toPx()
            val step = 22.dp.toPx()
            val hh = 7.dp.toPx()
            var x = 7.dp.toPx()
            while (x < size.width) {
                drawRoundRect(m.sprocket, Offset(x, (size.height - hh) / 2), Size(hole, hh), CornerRadius(1.5f * density.density))
                x += step
            }
        }
        val numbers = remember(s.model.slots) {
            var n = 0
            s.model.slots.map { sl -> if (sl is StyleSlot.Locked) GalleryStyleCopy.HIDDEN else "%02d".format(++n) }
        }
        LazyRow(
            Modifier.fillMaxSize().padding(top = band),
            state = state,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = fg),
            horizontalArrangement = Arrangement.spacedBy(fg),
            userScrollEnabled = s.interactive,
        ) {
            itemsIndexed(s.model.slots, key = { _, sl -> sl.id }) { i, sl ->
                val on = i == s.sel
                val a by animatedAlpha(if (on) 1f else 0.62f, label = "filmFrame")
                Column(
                    Modifier
                        .width(fw)
                        .fillMaxHeight()
                        .graphicsLayer { alpha = a }
                        .stylePlace(s, i, RoundedCornerShape(3.dp)),
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .then(if (on) Modifier.insetOutline(m.edge2, 2.dp, RoundedCornerShape(3.dp)) else Modifier)
                            .clip(RoundedCornerShape(3.dp)),
                    ) {
                        StylePicture(sl, lockSize = 18.dp, lockFill = m.film2, lockTint = Color(0xFFB4B6BD))
                        if (on) Box(Modifier.matchParentSize().insetOutline(m.edge2, 2.dp, RoundedCornerShape(3.dp)))
                        StyleCheck(s, i)
                    }
                    Box(Modifier.fillMaxWidth().height(band), contentAlignment = Alignment.Center) {
                        CivilightText(numbers.getOrElse(i) { "" }, CivilightTheme.type.micro, color = if (on) m.filmInk else m.edge2, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilmInfo(s: StyleBodyState) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(colors.raise).insetOutline(colors.line, 1.dp, shape).padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { contentDescription = GalleryStyleCopy.PICKED_A11Y },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (val slot = s.slot) {
            is StyleSlot.Item -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CivilightText(slot.info.title, type.title, color = if (slot.info.untitled) colors.ink3 else colors.ink, maxLines = 1)
                CivilightText(slot.info.meta, type.small, color = colors.ink2, maxLines = 1)
                CivilightText(slot.info.caption?.joinToString("") { it.text } ?: GalleryUiCopy.NO_DESCRIPTION, type.small, color = colors.ink2, maxLines = 1)
            }
            is StyleSlot.Locked -> Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CivilightIcon(IconId.Lock, null, size = 28.dp, tint = colors.ink2)
                CivilightText(slot.label, type.label, color = colors.ink2, maxLines = 1)
            }
            null -> Spacer(Modifier.weight(1f))
        }
        if (s.slot != null) StyleAction(s.selLocked, s::open, enabled = s.interactive)
    }
}

// ================================================================ 무드보드 (taped prints; a taped note holds 보기)

@Composable
private fun MoodView(s: StyleBodyState, plan: StylePlanSet) {
    val m = styleMaterials()
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val pages = plan.pages
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(shape)
                .background(Brush.linearGradient(listOf(m.cork, m.cork2)))
                .drawBehind {
                    // The cork's flecks (board: two dot grids, 9 and 13 px).
                    val r = 1.dp.toPx()
                    for ((step, off) in listOf(9.dp.toPx() to 0f, 13.dp.toPx() to 4.dp.toPx())) {
                        var y = off
                        while (y < size.height) {
                            var x = off
                            while (x < size.width) {
                                drawCircle(m.fleck, r, Offset(x, y))
                                x += step
                            }
                            y += step
                        }
                    }
                }
                .insetOutline(colors.line, 1.dp, shape)
                .pageSwipe(s, pages)
                .semantics { contentDescription = GalleryStyleCopy.MOOD_A11Y },
        ) {
            val w = maxWidth.value
            val h = maxHeight.value
            val rects = if (s.inner) GalleryStylePlans.MOOD_INNER else GalleryStylePlans.MOOD_COVER
            val noteRect = if (s.inner) GalleryStylePlans.MOOD_INNER_NOTE else GalleryStylePlans.MOOD_COVER_NOTE
            PageSwap(pages.pageOf(s.sel), Modifier.fillMaxSize()) { p ->
                Box(Modifier.fillMaxSize()) {
                    pages.pages.getOrNull(p).orEmpty().forEachIndexed { k, i ->
                        val r = rects[k % rects.size]
                        MoodPrint(s, i, m, Modifier.offset((r.x * w).dp, (r.y * h).dp).size((r.w * w).dp, (r.h * h).dp).zIndex(if (i == s.sel) 40f else r.z.toFloat()), r.rot, (r.w * w) >= 120f)
                    }
                }
            }
            MoodNote(s, m, Modifier.offset((noteRect.x * w).dp, (noteRect.y * h).dp).width((noteRect.w * w).dp).heightIn(min = maxOf(noteRect.h * h, 76f).dp).zIndex(45f))
        }
        StylePageBar(s, pages)
    }
}

@Composable
private fun MoodPrint(s: StyleBodyState, i: Int, m: StyleMaterials, modifier: Modifier, rot: Float, chip: Boolean) {
    val slot = s.model.slots.getOrNull(i) ?: return
    val colors = CivilightTheme.colors
    val on = i == s.sel
    Box(modifier.graphicsLayer { rotationZ = rot }) {
        Box(
            Modifier
                .matchParentSize()
                .shadow(if (on) 14.dp else 6.dp, RoundedCornerShape(0.dp))
                .background(if (slot is StyleSlot.Locked) colors.raise else m.print)
                .styleRing(on, 0.dp, width = 3.dp, gap = false)
                .stylePlace(s, i),
        ) {
            Box(Modifier.matchParentSize().padding(5.dp)) {
                StylePicture(slot, lockSize = 20.dp, lockFill = Color.Transparent, chip = chip)
                StyleCaption(slot)
                StyleCheck(s, i)
            }
        }
        // Tape across the top edge.
        Box(Modifier.align(Alignment.TopCenter).offset(y = (-8).dp).size(46.dp, 16.dp).background(m.tape))
    }
}

@Composable
private fun MoodNote(s: StyleBodyState, m: StyleMaterials, modifier: Modifier) {
    val type = CivilightTheme.type
    val slot = s.slot ?: return
    Box(modifier.graphicsLayer { rotationZ = -1.5f }) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(0.dp))
                .background(m.note)
                .padding(start = 14.dp, end = 12.dp, top = 16.dp, bottom = 12.dp)
                .semantics { contentDescription = GalleryStyleCopy.PICKED_A11Y },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (slot) {
                is StyleSlot.Item -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    CivilightText(slot.info.title, type.label, color = if (slot.info.untitled) m.noteInk2 else m.noteInk, maxLines = 1)
                    CivilightText(slot.info.meta, type.small, color = m.noteInk2, maxLines = 1)
                }
                is StyleSlot.Locked -> Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CivilightIcon(IconId.Lock, null, size = 22.dp, tint = m.noteInk2)
                    CivilightText(slot.label, type.label, color = m.noteInk2, maxLines = 1)
                }
            }
            StyleAction(s.selLocked, s::open, fill = StyleMaterials.NOTE_BUTTON, ink = StyleMaterials.NOTE_BUTTON_INK, enabled = s.interactive)
        }
        Box(Modifier.align(Alignment.TopCenter).offset(y = (-8).dp).size(54.dp, 16.dp).background(m.tape))
    }
}

// ================================================================ 빨랫줄 (three lines, clips, caption on the border)

@Composable
private fun ClotheslineView(s: StyleBodyState, plan: StylePlanSet) {
    val m = styleMaterials()
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val pages = plan.pages
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(shape)
                .background(Brush.verticalGradient(listOf(m.wall2, m.wall)))
                .insetOutline(colors.line, 1.dp, shape)
                .pageSwipe(s, pages)
                .semantics { contentDescription = GalleryStyleCopy.CLOTHESLINE_A11Y },
        ) {
            val w = maxWidth.value
            val lineH = maxHeight.value / GalleryStylePlans.LINES
            val pp = if (s.inner) 8f else 7f
            val cap = if (s.inner) 32f else 28f
            // Never wider than a third of the line; 64 dp at least while the line allows it (no inverted range).
            val maxPw = maxOf(24f, w / 3f - 8f)
            val pw = (w * if (s.inner) 0.277f else 0.23f).coerceAtMost(maxPw).coerceAtLeast(minOf(64f, maxPw))
            val phh = GalleryStylePlans.polaroidPictureH(lineH, pp, cap)
            PageSwap(pages.pageOf(s.sel), Modifier.fillMaxSize()) { p ->
                val page = pages.pages.getOrNull(p).orEmpty()
                Column(Modifier.fillMaxSize()) {
                    for (line in 0 until GalleryStylePlans.LINES) {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            val sag = 12f
                            Canvas(Modifier.fillMaxWidth().height(40.dp)) {
                                val path = Path().apply {
                                    moveTo(-4f, 10.dp.toPx())
                                    quadraticTo(size.width / 2, (10f + 2 * sag).dp.toPx(), size.width + 4f, 10.dp.toPx())
                                }
                                drawPath(path, m.rope, style = Stroke(1.6.dp.toPx()))
                            }
                            for (k in 0 until 3) {
                                val i = page.getOrNull(line * 3 + k) ?: continue
                                val x = w * (k + 0.5f) / 3f - pw / 2f
                                val y = 14f + if (k == 1) sag else sag * 0.55f
                                Polaroid(s, i, m, GalleryStylePlans.LINE_TILT[(line * 3 + k) % 9], pw, pp, cap, phh, Modifier.offset(x.dp, y.dp))
                            }
                        }
                    }
                }
            }
        }
        StylePageBar(s, pages)
    }
}

@Composable
private fun Polaroid(s: StyleBodyState, i: Int, m: StyleMaterials, tilt: Float, pw: Float, pp: Float, cap: Float, phh: Float, modifier: Modifier) {
    val slot = s.model.slots.getOrNull(i) ?: return
    val colors = CivilightTheme.colors
    val motion = CivilightTheme.motion
    val on = i == s.sel
    val rot by animateFloatAsState(if (on) 0f else tilt, motion.move(CivilightMotion.standard), label = "polaroidTilt")
    val lift by animateFloatAsState(if (on) 1f else 0f, motion.move(CivilightMotion.standard), label = "polaroidLift")
    val locked = slot is StyleSlot.Locked
    Box(
        modifier
            .width(pw.dp)
            .zIndex(if (on) 5f else 1f)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 0f)
                rotationZ = rot
                translationY = 6.dp.toPx() * lift
                scaleX = 1f + 0.04f * lift
                scaleY = 1f + 0.04f * lift
            },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(if (on) 14.dp else 8.dp, RoundedCornerShape(0.dp))
                .background(if (locked) colors.raise else m.polaroid)
                .styleRing(on, 0.dp, width = 3.dp, gap = false)
                .stylePlace(s, i)
                .padding(start = pp.dp, end = pp.dp, top = pp.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(phh.dp)) {
                StylePicture(slot, lockSize = 26.dp, lockFill = Color.Transparent, chip = true)
                StyleCheck(s, i)
                if (on && s.multi == null) {
                    if (locked) StylePill(true, Modifier.align(Alignment.Center))
                    else StylePill(false, Modifier.align(Alignment.BottomEnd).padding(4.dp))
                }
            }
            Row(Modifier.fillMaxWidth().height(cap.dp).padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                val text = if (slot is StyleSlot.Item) slot.info.title else GalleryStyleCopy.LOCKED
                val untitled = (slot as? StyleSlot.Item)?.info?.untitled == true
                CivilightText(text, CivilightTheme.type.small, color = if (locked) colors.ink2 else if (untitled) m.polaroidInk2 else m.polaroidInk, maxLines = 1)
            }
        }
        // The clip.
        Box(Modifier.align(Alignment.TopCenter).offset(y = (-12).dp).size(8.dp, 20.dp).clip(RoundedCornerShape(2.dp)).background(m.pin))
    }
}

// ================================================================ 전시실 (◀ ▶ and the wall number on the floor)

@Composable
private fun ExhibitView(s: StyleBodyState, plan: StylePlanSet) {
    val m = styleMaterials()
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(18.dp)
    val pages = plan.pages
    val page = pages.pageOf(s.sel)
    val walls = (s.model.slots.size + GalleryStylePlans.EXHIBIT_WALL - 1) / GalleryStylePlans.EXHIBIT_WALL
    val wallNo = GalleryStyleCopy.wall((if (s.inner) page else s.sel.coerceAtLeast(0) / GalleryStylePlans.EXHIBIT_WALL) + 1, walls.coerceAtLeast(1))
    val wt = if (s.inner) 64.dp else 40.dp
    val fh = if (s.inner) 64.dp else 68.dp
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(shape)
                .background(Brush.verticalGradient(0f to m.wall2, 0.3f to m.wall, 0.8f to m.wall, 1f to m.wall2))
                .insetOutline(colors.line, 1.dp, shape)
                .semantics { contentDescription = GalleryStyleCopy.EXHIBIT_A11Y },
        ) {
            Row(Modifier.fillMaxWidth().height(wt), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                Box(Modifier.size(28.dp, 1.dp).background(colors.edge.copy(alpha = 0.6f)))
                val title = GalleryStyleCopy.exhibitTitle(s.model.ownerName)
                CivilightText(if (s.inner) GalleryStyleCopy.join(listOf(title, wallNo)) else title, type.small, color = colors.ink2, maxLines = 1)
                Box(Modifier.size(28.dp, 1.dp).background(colors.edge.copy(alpha = 0.6f)))
            }
            PageSwap(page, Modifier.fillMaxSize().padding(top = wt, bottom = fh).styleSwipe(s.interactive) { d -> s.go(GalleryStylePlans.step(plan.order, s.sel, d)) }) { p ->
                Row(Modifier.fillMaxSize()) {
                    pages.pages.getOrNull(p).orEmpty().forEach { i -> Bay(s, i, m, Modifier.weight(1f).fillMaxHeight()) }
                    // A short last wall keeps the bay width.
                    if (s.inner) repeat(GalleryStylePlans.EXHIBIT_WALL - pages.pages.getOrNull(p).orEmpty().size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Floor(s, m, plan.order, wallNo, fh, Modifier.align(Alignment.BottomStart))
        }
        if (s.inner) ExhibitDetail(s, plan.order)
    }
}

@Composable
private fun Bay(s: StyleBodyState, i: Int, m: StyleMaterials, modifier: Modifier) {
    val slot = s.model.slots.getOrNull(i) ?: return
    val type = CivilightTheme.type
    val colors = CivilightTheme.colors
    val on = i == s.sel
    val spot by animatedAlpha(if (on) 1f else 0.55f, label = "baySpot")
    val mt = 12f
    Column(
        modifier
            .drawBehind {
                val path = Path().apply {
                    moveTo(size.width * 0.05f + size.width * 0.9f * 0.42f, 0f)
                    lineTo(size.width * 0.05f + size.width * 0.9f * 0.58f, 0f)
                    lineTo(size.width * 0.95f, size.height)
                    lineTo(size.width * 0.05f, size.height)
                    close()
                }
                drawPath(path, Brush.verticalGradient(listOf(if (on) m.spot2 else m.spot, Color.Transparent), endY = size.height * 0.9f), alpha = spot)
            }
            .stylePlace(s, i)
            .padding(bottom = if (s.inner) 22.dp else 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(if (s.inner) androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 30.dp, bottom = 34.dp) else androidx.compose.foundation.layout.PaddingValues(start = 40.dp, end = 40.dp, top = 26.dp, bottom = 30.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val ring = mt + 7f
            val ratio = if (slot is StyleSlot.Item) slot.info.ratio else 1f
            val (w, h) = GalleryStylePlans.fit(maxWidth.value - 2 * ring, maxHeight.value - 2 * ring, ratio)
            val shared = (slot as? StyleSlot.Item)?.takeIf { on }?.let { s.sharedImage(it.entry.key) } ?: Modifier
            Box(
                Modifier
                    .size((w + 2 * ring).dp, (h + 2 * ring).dp)
                    .shadow(12.dp, RoundedCornerShape(1.dp))
                    .background(m.frame)
                    .padding(7.dp)
                    .background(if (slot is StyleSlot.Locked) m.frame else m.mat)
                    .padding(mt.dp),
            ) {
                Box(Modifier.fillMaxSize().then(shared)) {
                    StylePicture(slot, maxSide = GalleryImageSizes.CARD, lockSize = 26.dp, lockFill = m.wall2)
                    StyleCheck(s, i)
                }
            }
        }
        val pq = RoundedCornerShape(2.dp)
        Column(
            Modifier
                .widthIn(min = 44.dp)
                .heightIn(min = 32.dp)
                .shadow(2.dp, pq)
                .background(m.plaque)
                .then(if (on) Modifier.insetOutline(colors.acc, 2.dp, pq) else Modifier)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            when (slot) {
                is StyleSlot.Item -> {
                    CivilightText(slot.info.title, type.micro, color = if (slot.info.untitled) m.plaqueInk.copy(alpha = 0.6f) else m.plaqueInk, maxLines = 1)
                    CivilightText(
                        GalleryStyleCopy.join(listOfNotNull(slot.info.tagGroup ?: GalleryStyleCopy.NO_TAG, GalleryStyleCopy.HOLO.takeIf { slot.info.holo })),
                        type.micro,
                        color = m.plaqueInk.copy(alpha = 0.78f),
                        maxLines = 1,
                    )
                }
                is StyleSlot.Locked -> CivilightIcon(IconId.Lock, null, size = 16.dp, tint = m.plaqueInk)
            }
        }
    }
}

@Composable
private fun Floor(s: StyleBodyState, m: StyleMaterials, order: List<Int>, wallNo: String, fh: Dp, modifier: Modifier) {
    val type = CivilightTheme.type
    Row(
        modifier
            .fillMaxWidth()
            .height(fh)
            .background(m.floor)
            .drawBehind {
                // Floor boards: 58 + 2 px stripes.
                val step = 60.dp.toPx()
                var x = 58.dp.toPx()
                while (x < size.width) {
                    drawRect(m.floor2, Offset(x, 0f), Size(2.dp.toPx(), size.height))
                    x += step
                }
                drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent), endY = 14.dp.toPx()))
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (s.inner) return@Row
        val fill = Color.White.copy(alpha = 0.14f)
        val ring = Color.Black.copy(alpha = 0.18f)
        StyleRoundButton(IconId.Prev, GalleryStyleCopy.BTN_PREV_FRAME, { s.go(GalleryStylePlans.step(order, s.sel, -1)) }, s.interactive && GalleryStylePlans.hasPrev(order, s.sel), fill, ring, m.floorInk)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            CivilightText(GalleryStylePlans.count(s.model.facts, order, s.sel), type.label, color = m.floorInk, maxLines = 1)
            CivilightText(wallNo, type.small, color = m.floorInk.copy(alpha = 0.85f), maxLines = 1)
        }
        StyleRoundButton(IconId.Next, GalleryStyleCopy.BTN_NEXT_FRAME, { s.go(GalleryStylePlans.step(order, s.sel, 1)) }, s.interactive && GalleryStylePlans.hasNext(order, s.sel), fill, ring, m.floorInk)
        if (s.slot != null) StyleAction(s.selLocked, s::open, enabled = s.interactive)
    }
}

@Composable
private fun ExhibitDetail(s: StyleBodyState, order: List<Int>) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().height(168.dp).clip(shape).background(colors.raise).insetOutline(colors.line, 1.dp, shape).padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { contentDescription = GalleryStyleCopy.PICKED_A11Y },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when (val slot = s.slot) {
                is StyleSlot.Item -> {
                    CivilightText(slot.info.title, type.title, color = if (slot.info.untitled) colors.ink3 else colors.ink, maxLines = 1)
                    CivilightText(slot.info.meta, type.small, color = colors.ink2, maxLines = 1)
                    CivilightText(slot.info.caption?.joinToString("") { it.text } ?: GalleryUiCopy.NO_DESCRIPTION, type.small, color = colors.ink2, maxLines = 3)
                }
                is StyleSlot.Locked -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CivilightIcon(IconId.Lock, slot.label, size = 32.dp, tint = colors.ink2) }
                null -> Unit
            }
        }
        Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
            StyleNav(s, order, prevLabel = GalleryStyleCopy.BTN_PREV_FRAME, nextLabel = GalleryStyleCopy.BTN_NEXT_FRAME)
            if (s.slot != null) StyleAction(s.selLocked, s::open, enabled = s.interactive)
        }
    }
}

// ================================================================ 색 묶음 (colour tabs switch the group)

@Composable
private fun ToneView(s: StyleBodyState, plan: StylePlanSet) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val tones = plan.tones
    val pages = plan.pages
    val shownTones = GalleryTone.entries.filter { it != GalleryTone.LOCK || tones?.contains(GalleryTone.LOCK) == true }
    val groups = tones?.let(GalleryStylePlans::toneGroups).orEmpty()
    val current = tones?.getOrNull(s.sel)
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val tabsShape = RoundedCornerShape(16.dp)
        Row(
            Modifier.fillMaxWidth().height(48.dp).clip(tabsShape).background(colors.tint).insetOutline(colors.line, 1.dp, tabsShape).padding(2.dp)
                .semantics { contentDescription = GalleryStyleCopy.TONE_A11Y },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            shownTones.forEach { t ->
                val ids = groups[t].orEmpty()
                val on = t == current
                val bg by io.github.lenticualrstone.civilight.designsystem.theme.animatedColor(if (on) colors.raise else colors.tint.copy(alpha = 0f), label = "toneTab")
                val tabShape = RoundedCornerShape(13.dp)
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(tabShape)
                        .background(bg)
                        .then(if (on) Modifier.insetOutline(colors.line, 1.dp, tabShape) else Modifier)
                        .semantics {
                            role = Role.Tab
                            selected = on
                            contentDescription = if (t == GalleryTone.LOCK) GalleryStyleCopy.LOCKED else GalleryStyleCopy.toneTab(t.label, ids.size)
                        }
                        .then(if (s.interactive) Modifier.pressable(PressAction.Select(on) { ids.firstOrNull()?.let(s::go) }, enabled = ids.isNotEmpty(), shape = tabShape) else Modifier),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                ) {
                    val sw = StyleMaterials.swatch(t)
                    if (sw == null) CivilightIcon(IconId.Lock, null, size = 16.dp, tint = if (on) colors.ink else colors.ink2)
                    else Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(Brush.linearGradient(0f to sw.first, 0.5f to sw.first, 0.5f to sw.second, 1f to sw.second)))
                    CivilightText(t.label, type.label, color = if (on) colors.ink else colors.ink2, maxLines = 1)
                    if (t != GalleryTone.LOCK) CivilightText(ids.size.toString(), type.small, color = colors.ink3, maxLines = 1)
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().pageSwipe(s, pages)) {
            if (tones != null) {
                PageSwap(pages.pageOf(s.sel), Modifier.fillMaxSize()) { p ->
                    val ids = pages.pages.getOrNull(p).orEmpty()
                    val rows = GalleryStylePlans.toneRows(ids.size)
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        var at = 0
                        rows.forEachIndexed { r, n ->
                            val rowIds = ids.subList(at, (at + n).coerceAtMost(ids.size))
                            at += n
                            val wide = n == 1 && rows.size > 1 && r == 0 && ids.size % 2 == 1
                            Row(Modifier.weight(if (wide) 1.3f else 1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowIds.forEach { i -> ToneTile(s, i, Modifier.weight(1f).fillMaxHeight()) }
                            }
                        }
                    }
                }
            }
        }
        StylePageBar(s, pages)
    }
}

@Composable
private fun ToneTile(s: StyleBodyState, i: Int, modifier: Modifier) {
    val slot = s.model.slots.getOrNull(i) ?: return
    val colors = CivilightTheme.colors
    val on = i == s.sel
    val shape = RoundedCornerShape(14.dp)
    Box(modifier.styleRing(on, 14.dp).clip(shape).stylePlace(s, i, shape)) {
        if (slot is StyleSlot.Locked) {
            Column(
                Modifier.matchParentSize().background(colors.tint).insetOutline(colors.line, 1.dp, shape),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CivilightIcon(IconId.Lock, null, size = 32.dp, tint = colors.ink2)
                CivilightText(slot.label, CivilightTheme.type.label, color = colors.ink2, maxLines = 1)
                if (on && s.multi == null) StylePill(true)
            }
        } else {
            StylePicture(slot, maxSide = GalleryImageSizes.CARD, chip = true)
            StyleCaption(slot, endPad = if (on) 78.dp else 8.dp)
            if (on && s.multi == null) StylePill(false, Modifier.align(Alignment.BottomEnd).padding(8.dp))
            StyleCheck(s, i)
        }
    }
}

// ================================================================ 태그 줄 (justified rows grouped by tag)

@Composable
private fun TagRowsView(s: StyleBodyState, plan: StylePlanSet) {
    val pages = plan.pages
    val rowPages = remember(plan.tagRows) { GalleryStylePlans.tagPages(plan.tagRows) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth().pageSwipe(s, pages).semantics { contentDescription = GalleryStyleCopy.TAG_ROWS_A11Y }) {
            PageSwap(pages.pageOf(s.sel), Modifier.fillMaxSize()) { p ->
                val rows = rowPages.getOrNull(p).orEmpty()
                val weights = GalleryStylePlans.rowWeights(rows, s.model.facts)
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rows.forEachIndexed { r, row ->
                        Row(Modifier.weight(weights[r]).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TagLabel(row.label)
                            row.slots.forEach { i ->
                                val ratio = s.model.facts.getOrNull(i)?.ratio ?: 1f
                                TagTile(s, i, Modifier.weight(ratio).fillMaxHeight().widthIn(min = 44.dp))
                            }
                        }
                    }
                }
            }
        }
        StylePageBar(s, pages)
    }
}

/** The row's vertical tag label (board `.jl`, writing-mode vertical: upright letters stacked). */
@Composable
private fun TagLabel(label: String?) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(8.dp)
    if (label == null) {
        Box(Modifier.width(26.dp).fillMaxHeight().drawBehind { drawRect(colors.line, Offset.Zero, Size(1.dp.toPx(), size.height)) })
        return
    }
    Box(
        Modifier.width(26.dp).fillMaxHeight().clip(shape).background(colors.tint).insetOutline(colors.line, 1.dp, shape).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        val letters = label.replace(GalleryStyleCopy.SEP, "·").replace(" ", "")
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            letters.forEach { ch ->
                CivilightText(ch.toString(), CivilightTheme.type.micro, color = colors.ink2, maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun TagTile(s: StyleBodyState, i: Int, modifier: Modifier) {
    val slot = s.model.slots.getOrNull(i) ?: return
    val on = i == s.sel
    val shape = RoundedCornerShape(8.dp)
    Box(modifier.styleRing(on, 8.dp).clip(shape).stylePlace(s, i, shape)) {
        StylePicture(slot, chip = true, lockSize = 22.dp)
        StyleCaption(slot, endPad = if (on) 78.dp else 8.dp)
        if (on && s.multi == null) {
            if (slot is StyleSlot.Locked) StylePill(true, Modifier.align(Alignment.Center).offset(y = 22.dp))
            else StylePill(false, Modifier.align(Alignment.BottomEnd).padding(8.dp))
        }
        StyleCheck(s, i)
    }
}

// ================================================================ 장면 순서 (manuscript order on a spine; 씬 없음 lane)

@Composable
private fun SceneOrderView(s: StyleBodyState, plan: StylePlanSet) {
    val colors = CivilightTheme.colors
    val scene = plan.scene ?: return
    val lw = if (s.inner) 92.dp else 78.dp
    val lane = if (s.inner) 150.dp else 112.dp
    val pages = scene.placed
    val selPage = pages.pages.indexOfFirst { s.sel in it }
    // A pick in the lane keeps the rows where they were.
    val page = rememberLastPage(selPage, pages.pages.lastIndex)
    Column(Modifier.fillMaxSize().semantics { contentDescription = GalleryStyleCopy.SCENE_ORDER_A11Y }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .styleSwipe(s.interactive && pages.pages.size > 1) { d -> pages.pages.getOrNull(page + d)?.firstOrNull()?.let(s::go) }
                .drawBehind {
                    if (pages.pages.isEmpty()) return@drawBehind
                    val x = (lw + 12.dp).toPx()
                    drawRect(Brush.verticalGradient(listOf(colors.acc, colors.line)), Offset(x - 1.dp.toPx(), 10.dp.toPx()), Size(2.dp.toPx(), size.height - 10.dp.toPx()))
                },
        ) {
            if (pages.pages.isEmpty()) {
                CivilightText(GalleryStyleCopy.NO_PLACED, CivilightTheme.type.small, Modifier.align(Alignment.Center), color = colors.ink3)
            } else {
                PageSwap(page, Modifier.fillMaxSize()) { p ->
                    val ids = pages.pages.getOrNull(p).orEmpty()
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(GalleryStylePlans.SCENE_ROWS_PAGE) { r ->
                            Box(Modifier.weight(1f).fillMaxWidth()) { ids.getOrNull(r)?.let { SceneRow(s, it, lw) } }
                        }
                    }
                }
            }
        }
        if (pages.pages.size > 1) {
            val bar = StylePages(pages.pages)
            StylePageBar(StyleBodyState(s.style, s.model, s.inner, pages.pages.getOrNull(page)?.firstOrNull() ?: s.sel, s.interactive, s.onPick, s.onOpen, s.multi, s.sharedImage, s.onMove, s.explicit), bar)
        }
        SceneLane(s, scene.lane, Modifier.fillMaxWidth().height(lane))
    }
}

/** The last page that held the pick (the lane's picks are not on a page). */
@Composable
private fun rememberLastPage(page: Int, lastIndex: Int): Int {
    val holder = remember { intArrayOf(0) }
    if (page >= 0) holder[0] = page
    // The placed list can shrink under a lane pick (filter, delete): never point past its end.
    holder[0] = GalleryStylePlans.clampPage(holder[0], lastIndex)
    return holder[0]
}

@Composable
private fun SceneRow(s: StyleBodyState, i: Int, lw: Dp) {
    val slot = s.model.item(i) ?: return
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val on = i == s.sel
    val (ep, sc) = GalleryStylePlans.splitCode(slot.info.sceneCode.orEmpty())
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(lw).padding(end = 6.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CivilightText(GalleryStyleCopy.join(listOf(ep, sc)), type.label, color = colors.ink, maxLines = 1, textAlign = TextAlign.End)
            slot.info.sceneTime?.let { CivilightText(it, type.small, color = colors.ink3, maxLines = 1, textAlign = TextAlign.End) }
        }
        Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(10.dp)
                    .graphicsLayer { rotationZ = 45f }
                    .background(if (on) colors.acc else colors.bg)
                    .insetOutline(colors.acc, 2.dp, RoundedCornerShape(0.dp)),
            )
        }
        val shape = RoundedCornerShape(12.dp)
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 2.dp)
                .clip(shape)
                .background(if (on) colors.sel else Color.Transparent)
                .then(if (on) Modifier.insetOutline(colors.acc, 2.dp, shape) else Modifier)
                .stylePlace(s, i, shape)
                .padding(4.dp),
        ) {
            val picW = minOf(maxHeight.value * slot.info.ratio, maxWidth.value * 0.52f)
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.width(picW.dp).fillMaxHeight().clip(RoundedCornerShape(8.dp))) {
                    StylePicture(slot)
                    StyleCheck(s, i)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    CivilightText(slot.info.title, type.label, color = if (slot.info.untitled) colors.ink3 else colors.ink, maxLines = 1)
                    CivilightText(
                        GalleryStyleCopy.join(listOfNotNull(slot.info.tagGroup ?: GalleryStyleCopy.NO_TAG, GalleryStyleCopy.HOLO.takeIf { slot.info.holo })),
                        type.small,
                        color = colors.ink3,
                        maxLines = 1,
                    )
                }
            }
            if (on && s.multi == null) StylePill(false, Modifier.align(Alignment.BottomEnd).padding(4.dp))
        }
    }
}

@Composable
private fun SceneLane(s: StyleBodyState, lane: List<Int>, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(16.dp)
    val loose = lane.filter { s.model.facts.getOrNull(it)?.lock == false }
    val locks = lane.filter { s.model.facts.getOrNull(it)?.lock == true }
    val state = rememberLazyListState()
    LaunchedEffect(s.sel) {
        val at = lane.indexOf(s.sel)
        if (at >= 0 && state.layoutInfo.visibleItemsInfo.none { it.index == at }) state.scrollToItem(at)
    }
    Column(
        modifier.clip(shape).background(colors.tint).insetOutline(colors.line, 1.dp, shape).padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            CivilightText(GalleryStyleCopy.lane(loose.size), type.small, Modifier.weight(1f), color = colors.ink3, maxLines = 1)
            if (locks.isNotEmpty()) CivilightText(GalleryStyleCopy.LOCKED, type.small, color = colors.ink3, maxLines = 1)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().semantics { contentDescription = GalleryStyleCopy.NO_SCENE_LANE }) {
            val h = maxHeight.value
            LazyRow(Modifier.fillMaxSize(), state = state, horizontalArrangement = Arrangement.spacedBy(8.dp), userScrollEnabled = s.interactive) {
                itemsIndexed(lane, key = { _, i -> s.model.slots[i].id }) { k, i ->
                    val slot = s.model.slots[i]
                    val on = i == s.sel
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (slot is StyleSlot.Locked && (k == 0 || s.model.slots[lane[k - 1]] !is StyleSlot.Locked) && loose.isNotEmpty()) {
                            Box(
                                Modifier.padding(end = 8.dp).width(1.dp).fillMaxHeight().drawBehind {
                                    val dash = 4.dp.toPx()
                                    var y = 2.dp.toPx()
                                    while (y < size.height) {
                                        drawRect(colors.edge, Offset(0f, y), Size(1.dp.toPx(), dash))
                                        y += dash * 2
                                    }
                                },
                            )
                        }
                        val w = if (slot is StyleSlot.Locked) h * 0.9f else h * slot.facts.ratio
                        val tile = RoundedCornerShape(8.dp)
                        Box(Modifier.width(w.coerceAtLeast(44f).dp).fillMaxHeight().styleRing(on, 8.dp).clip(tile).stylePlace(s, i, tile)) {
                            StylePicture(slot, lockSize = 18.dp)
                            StyleCaption(slot)
                            if (on && s.multi == null) StylePill(slot is StyleSlot.Locked, Modifier.align(if (slot is StyleSlot.Locked) Alignment.Center else Alignment.TopEnd).padding(4.dp), iconOnly = true)
                            StyleCheck(s, i)
                        }
                    }
                }
            }
        }
    }
}

// ================================================================ inner information pane

/** The inner right pane (board `pane()`): information only, no second copy of the picture. */
@Composable
fun StylePane(s: StyleBodyState, order: List<Int>, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.clip(shape).background(colors.raise).insetOutline(colors.line, 1.dp, shape).padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 14.dp)
            .semantics { contentDescription = GalleryStyleCopy.PICKED_A11Y },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (val slot = s.slot) {
            is StyleSlot.Item -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CivilightText(slot.info.title, type.title, color = if (slot.info.untitled) colors.ink3 else colors.ink, maxLines = 2)
                    CivilightText(slot.info.meta, type.small, color = colors.ink2, maxLines = 2)
                    CivilightText(slot.info.caption?.joinToString("") { it.text } ?: GalleryUiCopy.NO_DESCRIPTION, type.small, Modifier.padding(top = 4.dp), color = colors.ink2, maxLines = 3)
                }
                val rows = paneRows(slot.info)
                Column(Modifier.weight(1f).fillMaxWidth().drawBehind { drawRect(colors.line, Offset.Zero, Size(size.width, 1.dp.toPx())) }) {
                    rows.forEach { (k, v, none) ->
                        Row(
                            Modifier.weight(1f).heightIn(min = 36.dp).fillMaxWidth().drawBehind { drawRect(colors.line, Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx())) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CivilightText(k, type.small, color = colors.ink3, maxLines = 1)
                            CivilightText(v, type.label, Modifier.weight(1f), color = if (none) colors.ink3 else colors.ink, maxLines = 1, textAlign = TextAlign.End)
                        }
                    }
                }
            }
            is StyleSlot.Locked -> Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                CivilightIcon(IconId.Lock, null, size = 36.dp, tint = colors.ink2)
                CivilightText(slot.label, type.label, color = colors.ink2, maxLines = 1)
            }
            null -> Spacer(Modifier.weight(1f))
        }
        StyleNav(s, order, Modifier.fillMaxWidth())
        if (s.slot != null) StyleAction(s.selLocked, s::open, Modifier.fillMaxWidth(), enabled = s.interactive)
    }
}

/** The pane's key-value rows (board `KV`): (key, value, value is a 없음 kind). */
fun paneRows(info: SlotInfo): List<Triple<String, String, Boolean>> {
    fun row(k: String, v: String?, none: String = GalleryStyleCopy.NONE) = Triple(k, v ?: none, v == null)
    return listOf(
        row(GalleryStyleCopy.KEY_TAG, info.tags.takeIf { it.isNotEmpty() }?.joinToString(GalleryStyleCopy.SEP), GalleryStyleCopy.NO_TAG),
        row(GalleryStyleCopy.KEY_SCENE, info.sceneText, GalleryStyleCopy.NO_SCENE).let { (k, v, n) -> Triple(k, if (info.sceneCode != null) info.sceneCode else v, n) },
        row(GalleryStyleCopy.KEY_TIME, info.sceneTime),
        Triple(GalleryStyleCopy.KEY_KIND, if (info.holo) GalleryStyleCopy.HOLO else GalleryStyleCopy.PLAIN, false),
        row(GalleryStyleCopy.KEY_RATIO, info.ratioText),
        row(GalleryStyleCopy.KEY_SIZE, info.size),
        row(GalleryStyleCopy.KEY_ADDED, info.added),
        row(GalleryStyleCopy.KEY_USE, info.uses),
    )
}
