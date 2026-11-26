package io.github.lenticualrstone.civilight.feature.canvas.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.horizontalSwipe
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalBackHandler
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import io.github.lenticualrstone.civilight.model.asset.Ratio
import kotlin.math.abs
import kotlin.math.floor

/** Picker state rules (pure): the list wraps; `이 보기로` only for a view that is not the current one. */
object StylePickRules {
    fun wrap(i: Int, n: Int): Int = if (n <= 0) 0 else ((i % n) + n) % n

    fun canApply(picked: GalleryStyle, current: GalleryStyle): Boolean = picked != current

    /** The swipe step of a drag of [dx] px with a [threshold]: +1 = next (drag left). */
    fun swipeStep(dx: Float, threshold: Float): Int = when {
        dx <= -threshold -> 1
        dx >= threshold -> -1
        else -> 0
    }
}

/** The header's `보기 ▾` (board `.vb`): icon, the current view's name, chevron; opens the picker. */
@Composable
fun StyleViewButton(style: GalleryStyle, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .height(44.dp)
            .clip(shape)
            .background(colors.tint)
            .insetOutline(colors.line, 1.dp, shape)
            .semantics { contentDescription = GalleryStyleCopy.viewButton(style.label) }
            .then(if (enabled) Modifier.pressable(onClick = onClick, shape = shape) else Modifier)
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CivilightIcon(style.icon, null, size = 18.dp, tint = colors.ink)
        CivilightText(style.label, CivilightTheme.type.label, color = colors.ink, maxLines = 1)
        CivilightIcon(IconId.Down, null, size = 14.dp, tint = colors.ink3)
    }
}

/** The data a preview draws: the masked gallery and the screen's current pick. */
class StylePreviewData(val model: StyleModel, val sel: Int)

/**
 * `보기 고르기` (boards `VpGalC` cover sheet, `VpGalI` inner dialog): every view with a live preview of the real,
 * masked gallery (nothing inside a preview reacts); tiles, ◀ ▶ and a swipe on the stage move the choice;
 * `이 보기로` applies it, `취소`, the scrim and back close it. Reduced motion: no slides, fades only.
 */
@Composable
fun GalleryStylePicker(
    open: Boolean,
    current: GalleryStyle,
    data: StylePreviewData?,
    inner: Boolean,
    onApply: (GalleryStyle) -> Unit,
    onClose: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val motion = CivilightTheme.motion
    LocalBackHandler.current(open) { onClose() }
    val order = GalleryStyle.ORDER
    // Unbounded position so the stage can glide past the ends; the choice is it wrapped.
    var raw by rememberSaveable(open) { mutableIntStateOf(order.indexOf(current).coerceAtLeast(0)) }
    val picked = order[StylePickRules.wrap(raw, order.size)]
    BoxWithConstraints(Modifier.fillMaxSize().zIndex(30f)) {
        val screenW = maxWidth
        val screenH = maxHeight
        AnimatedVisibility(open, enter = motion.enter(CivilightMotion.standard), exit = motion.exit()) {
            Box(Modifier.fillMaxSize().background(colors.shade).pressable(onClick = onClose, scaleOnPress = false).semantics { contentDescription = GalleryStyleCopy.BTN_CANCEL })
        }
        val pick = { i: Int -> raw = i }
        if (!inner) {
            AnimatedVisibility(
                open,
                modifier = Modifier.fillMaxSize(),
                enter = motion.enter(CivilightMotion.enter, rise = screenH),
                exit = motion.exit(CivilightMotion.exit, rise = screenH),
            ) {
                CoverSheet(current, picked, raw, data, screenW, screenH, pick, { onApply(picked) }, onClose)
            }
        } else {
            AnimatedVisibility(open, modifier = Modifier.fillMaxSize(), enter = motion.enter(CivilightMotion.enter), exit = motion.exit()) {
                InnerDialog(current, picked, raw, data, screenW, screenH, pick, { onApply(picked) }, onClose)
            }
        }
    }
}

// ---------------------------------------------------------------- cover sheet

@Composable
private fun CoverSheet(
    current: GalleryStyle,
    picked: GalleryStyle,
    raw: Int,
    data: StylePreviewData?,
    screenW: Dp,
    screenH: Dp,
    onPick: (Int) -> Unit,
    onApply: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val order = GalleryStyle.ORDER
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 16.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .shadow(16.dp, shape)
                .clip(shape)
                .background(colors.raise)
                .semantics { contentDescription = GalleryStyleCopy.join(listOf(GalleryStyleCopy.PICK_TITLE, GalleryUiCopy.TITLE)) }
                .pointerInput(Unit) { detectTapGestures { } }
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        ) {
            PickerHead(current, Modifier.height(28.dp))
            Spacer(Modifier.height(12.dp))
            Stage(raw, data, screenW, screenH, inner = false, onPick = onPick, modifier = Modifier.weight(1f).fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            NameRow(picked, raw, onPick, Modifier.height(44.dp))
            Spacer(Modifier.height(10.dp))
            Column(Modifier.semantics { contentDescription = GalleryStyleCopy.join(GalleryStyleGroup.entries.map { GalleryStyleCopy.group(it.label, order.count { s -> s.group == it }) }) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GalleryStyleGroup.entries.forEach { g ->
                    Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val glShape = RoundedCornerShape(8.dp)
                        Box(Modifier.width(22.dp).fillMaxHeight().clip(glShape).insetOutline(colors.line, 1.dp, glShape), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                g.short.forEach { ch -> CivilightText(ch.toString(), CivilightTheme.type.micro, color = colors.ink3, maxLines = 1) }
                            }
                        }
                        order.filter { it.group == g }.forEach { st ->
                            StyleTile(st, st == picked, st == current, { onPick(nearest(raw, order.indexOf(st), order.size)) }, Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Actions(picked, current, onApply, onClose, Modifier.fillMaxWidth())
        }
    }
}

/** The raw position nearest to [raw] whose wrapped index is [index] (so a tile tap glides the short way). */
private fun nearest(raw: Int, index: Int, n: Int): Int {
    val base = raw - StylePickRules.wrap(raw, n)
    return listOf(base + index - n, base + index, base + index + n).minBy { abs(it - raw) }
}

@Composable
private fun PickerHead(current: GalleryStyle, modifier: Modifier) {
    val colors = CivilightTheme.colors
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CivilightText(GalleryStyleCopy.PICK_TITLE, CivilightTheme.type.title, color = colors.ink, maxLines = 1)
        CivilightText(GalleryUiCopy.TITLE, CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
        Spacer(Modifier.weight(1f))
        CivilightText(GalleryStyleCopy.now(current.label), CivilightTheme.type.small, color = colors.ink3, maxLines = 1)
    }
}

@Composable
private fun NameRow(picked: GalleryStyle, raw: Int, onPick: (Int) -> Unit, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val n = GalleryStyle.ORDER.size
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StyleRoundButton(IconId.Prev, GalleryStyleCopy.BTN_PREV_STYLE, { onPick(raw - 1) })
        Column(Modifier.weight(1f).semantics { contentDescription = GalleryStyleCopy.preview(picked.label) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CivilightText(picked.label, CivilightTheme.type.itemTitle, color = colors.ink, maxLines = 1)
            CivilightText(
                GalleryStyleCopy.join(listOf(picked.fragment, GalleryStyleCopy.count(StylePickRules.wrap(raw, n) + 1, n))),
                CivilightTheme.type.small,
                color = colors.ink3,
                maxLines = 1,
            )
        }
        StyleRoundButton(IconId.Next, GalleryStyleCopy.BTN_NEXT_STYLE, { onPick(raw + 1) })
    }
}

@Composable
private fun StyleTile(style: GalleryStyle, on: Boolean, now: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val bg by animatedColor(if (on) colors.sel else colors.tint, label = "styleTile")
    Box(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(bg)
                .insetOutline(if (on) colors.acc else colors.line, if (on) 2.dp else 1.dp, shape)
                .semantics { selected = on; contentDescription = GalleryStyleCopy.tile(style) }
                .pressable(PressAction.Select(on, onClick), role = Role.Button, shape = shape),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CivilightIcon(style.icon, null, size = 18.dp, tint = if (on) colors.ink else colors.ink2)
            CivilightText(style.label, CivilightTheme.type.micro, color = if (on) colors.ink else colors.ink2, maxLines = 1, textAlign = TextAlign.Center)
        }
        if (on) CheckBadge(Modifier.align(Alignment.TopStart).padding(3.dp))
        if (now) NowBadge(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-8).dp))
    }
}

@Composable
private fun CheckBadge(modifier: Modifier) {
    val colors = CivilightTheme.colors
    Box(modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(colors.acc), contentAlignment = Alignment.Center) {
        CivilightIcon(IconId.Check, null, size = 10.dp, tint = colors.onpri)
    }
}

@Composable
private fun NowBadge(modifier: Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Box(modifier.height(16.dp).clip(shape).background(colors.raise).insetOutline(colors.ink2, 1.5.dp, shape).padding(horizontal = 5.dp), contentAlignment = Alignment.Center) {
        CivilightText(GalleryStyleCopy.NOW, CivilightTheme.type.micro, color = colors.ink, maxLines = 1)
    }
}

@Composable
private fun Actions(picked: GalleryStyle, current: GalleryStyle, onApply: () -> Unit, onClose: () -> Unit, modifier: Modifier, fixed: Boolean = false) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(24.dp)
    val can = StylePickRules.canApply(picked, current)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .then(if (fixed) Modifier.width(120.dp) else Modifier.weight(1f))
                .height(48.dp)
                .clip(shape)
                .background(colors.tint)
                .insetOutline(colors.line, 1.dp, shape)
                .pressable(onClick = onClose, shape = shape),
            contentAlignment = Alignment.Center,
        ) { CivilightText(GalleryStyleCopy.BTN_CANCEL, CivilightTheme.type.buttonLarge, color = colors.ink, maxLines = 1) }
        Row(
            Modifier
                .then(if (fixed) Modifier.width(200.dp) else Modifier.weight(2f))
                .height(48.dp)
                .clip(shape)
                .background(colors.pri)
                .graphicsLayer { alpha = if (can) 1f else 0.36f }
                .semantics { if (!can) disabled() }
                .pressable(onClick = onApply, enabled = can, shape = shape),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            CivilightIcon(IconId.Check, null, size = 18.dp, tint = colors.onpri)
            CivilightText(GalleryStyleCopy.BTN_APPLY, CivilightTheme.type.buttonLarge, color = colors.onpri, maxLines = 1)
        }
    }
}

/**
 * The preview stage: the picked view big in the middle, its neighbours smaller and dimmed beside it (cover), or
 * alone (inner). The scenes are the real views at the screen's size, shrunk (transform only). Swipe = extra.
 */
@Composable
private fun Stage(raw: Int, data: StylePreviewData?, screenW: Dp, screenH: Dp, inner: Boolean, onPick: (Int) -> Unit, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val motion = CivilightTheme.motion
    val order = GalleryStyle.ORDER
    val shape = RoundedCornerShape(if (inner) 16.dp else 20.dp)
    val pos by animateFloatAsState(raw.toFloat(), motion.move(CivilightMotion.enter), label = "pickerStage")
    var drag by remember { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 56.dp.toPx() }
    val density = LocalDensity.current
    val picked = order[StylePickRules.wrap(raw, order.size)]
    BoxWithConstraints(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(colors.tint, Color.Transparent)))
            .insetOutline(colors.line, 1.dp, shape)
            .semantics { role = Role.Image; contentDescription = GalleryStyleCopy.preview(picked.label) }
            .horizontalSwipe(
                onDelta = { amount -> drag += amount },
                onStop = {
                    val step = StylePickRules.swipeStep(drag, threshold)
                    drag = 0f
                    if (step != 0) onPick(raw + step)
                },
            ),
    ) {
        val stageW = maxWidth.value
        val stageH = maxHeight.value
        val sw = screenW.value.coerceAtLeast(1f)
        val sh = screenH.value.coerceAtLeast(1f)
        val s0 = (if (inner) minOf(stageW / sw, stageH / sh) else ((stageH - 52f) / sh).coerceAtMost(stageW * 0.62f / sw)).coerceAtLeast(0.05f)
        val s2 = s0 * 0.78f
        val dx = sw * (s0 + s2) / 2f + 42f
        val dragDp = with(density) { drag.toDp().value }
        val centre = floor(pos + 0.5f).toInt()
        val span = if (inner) 0 else 1
        for (r in (centre - span - 1)..(centre + span + 1)) {
            val d = r - pos
            val a = abs(d)
            if (a > span + 0.999f) continue
            val style = order[StylePickRules.wrap(r, order.size)]
            val scale = s0 + (s2 - s0) * a.coerceAtMost(1f)
            val x = if (inner) dragDp * 0.3f else d * dx + dragDp
            val y = if (inner) 0f else -16f
            Box(
                Modifier
                    .align(Alignment.Center)
                    .offset(x.dp, y.dp)
                    .zIndex(10f - a)
                    .size((sw * scale).dp, (sh * scale).dp)
                    .clip(RoundedCornerShape((28f * scale / s0).coerceAtMost(28f).dp))
                    .clearAndSetSemantics { },
            ) {
                ScaledScene(style, data, sw, sh, scale, inner)
                val dim = (a.coerceAtMost(1f)) * 0.18f
                if (dim > 0f) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = dim)))
            }
        }
        if (!inner) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.glass)
                    .insetOutline(colors.line, 1.dp, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CivilightIcon(SWIPE_ICON, null, size = 14.dp, tint = colors.ink2)
                CivilightText(GalleryStyleCopy.SWIPE_HINT, CivilightTheme.type.micro, color = colors.ink2, maxLines = 1)
            }
        }
    }
}

private val SWIPE_ICON = styleIcon("gallery-swipe", "M4 12h16M8 8l-4 4 4 4M16 8l4 4-4 4")

/** A view's scene at the screen's own size, shrunk by [scale] (layout inside stays the real one). */
@Composable
private fun ScaledScene(style: GalleryStyle, data: StylePreviewData?, w: Float, h: Float, scale: Float, inner: Boolean) {
    Box(Modifier.size((w * scale).dp, (h * scale).dp), contentAlignment = Alignment.Center) {
        Box(Modifier.requiredSize(w.dp, h.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
            StyleScene(style, data, inner)
        }
    }
}

/**
 * One view as a whole screen (board scenes): floating glass header with `보기 ▾`, the view between header and dock,
 * the search dock. Not interactive.
 */
@Composable
fun StyleScene(style: GalleryStyle, data: StylePreviewData?, inner: Boolean) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Box(Modifier.fillMaxSize().background(colors.bg)) {
        Box(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 72.dp, bottom = 72.dp)) {
            if (data != null && data.model.slots.isNotEmpty()) {
                if (style.isNew) {
                    GalleryStyleBody(
                        StyleBodyState(style, data.model, inner, data.sel.coerceIn(0, data.model.slots.lastIndex), interactive = false, onPick = {}, onOpen = {}),
                        Modifier.fillMaxSize(),
                    )
                } else {
                    BasicStill(style, data, inner)
                }
            }
        }
        val glass = RoundedCornerShape(20.dp)
        Row(
            Modifier.align(Alignment.TopCenter).padding(8.dp).fillMaxWidth().height(56.dp).clip(glass).background(colors.glass).insetOutline(colors.line, 1.dp, glass)
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CivilightText(GalleryUiCopy.TITLE, type.headerTitle, color = colors.ink, maxLines = 1)
                data?.let { d ->
                    CivilightText(
                        GalleryStyleCopy.join(listOfNotNull(d.model.ownerName.ifBlank { null }, GalleryUiCopy.imageCount(d.model.itemCount, d.model.hiddenLocked))),
                        type.small,
                        color = colors.ink3,
                        maxLines = 1,
                    )
                }
            }
            StyleViewButton(style, {}, enabled = false)
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { CivilightIcon(IconId.Plus, null, size = 20.dp, tint = colors.ink) }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(8.dp).fillMaxWidth().height(56.dp).clip(glass).background(colors.glass).insetOutline(colors.line, 1.dp, glass)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CivilightIcon(IconId.Search, null, size = 18.dp, tint = colors.ink3)
            CivilightText(GalleryUiCopy.FIND.substringBefore(GalleryUiCopy.SEP), type.command, color = colors.ink3, maxLines = 1)
        }
    }
}

/** Still scenes of `목록` · `격자` · `카드 진열` with the real images (board `scr_v_fold3d` basic group). */
@Composable
private fun BasicStill(style: GalleryStyle, data: StylePreviewData, inner: Boolean) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val items = data.model.slots.filterIsInstance<StyleSlot.Item>()
    val lock = data.model.slots.firstOrNull { it is StyleSlot.Locked }
    when (style) {
        GalleryStyle.LIST -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            (items + listOfNotNull(lock)).take(12).forEach { slot ->
                Row(Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))) {
                        if (slot is StyleSlot.Item) GalleryImage(slot.entry.item, Ratio.R1X1, GalleryImageSizes.THUMB) else StylePicture(slot)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        when (slot) {
                            is StyleSlot.Item -> {
                                CivilightText(slot.info.title, type.itemTitle, color = if (slot.info.untitled) colors.ink3 else colors.ink, maxLines = 1)
                                CivilightText(slot.info.meta, type.small, color = colors.ink3, maxLines = 1)
                            }
                            is StyleSlot.Locked -> CivilightText(slot.label, type.itemTitle, color = colors.ink2, maxLines = 1)
                        }
                    }
                }
            }
        }
        GalleryStyle.GRID -> {
            val cols = if (inner) 4 else 3
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (items + listOfNotNull(lock)).take(cols * 5).chunked(cols).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { slot ->
                            Box(Modifier.weight(1f).aspectRatio(3f / 4f).clip(RoundedCornerShape(14.dp))) {
                                if (slot is StyleSlot.Item) GalleryImage(slot.entry.item, Ratio.R3X4, GalleryImageSizes.TILE) else StylePicture(slot)
                            }
                        }
                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        else -> Row(Modifier.fillMaxSize().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val from = data.sel.coerceIn(0, maxOf(0, items.lastIndex))
            items.drop(from).take(4).forEach { slot ->
                Column(Modifier.width(if (inner) 236.dp else 222.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(20.dp))) {
                        GalleryImage(slot.entry.item, Ratio.R3X4, GalleryImageSizes.CARD)
                    }
                    CivilightText(slot.info.title, type.itemTitle, color = if (slot.info.untitled) colors.ink3 else colors.ink, maxLines = 1)
                    CivilightText(slot.info.meta, type.small, color = colors.ink3, maxLines = 1)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- inner dialog

@Composable
private fun InnerDialog(
    current: GalleryStyle,
    picked: GalleryStyle,
    raw: Int,
    data: StylePreviewData?,
    screenW: Dp,
    screenH: Dp,
    onPick: (Int) -> Unit,
    onApply: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(28.dp)
    val order = GalleryStyle.ORDER
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(12.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .shadow(24.dp, shape)
                .clip(shape)
                .background(colors.raise)
                .insetOutline(colors.line, 1.dp, shape)
                .semantics { contentDescription = GalleryStyleCopy.join(listOf(GalleryStyleCopy.PICK_TITLE, GalleryUiCopy.TITLE)) }
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PickerHead(current, Modifier.height(40.dp))
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.width(212.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GalleryStyleGroup.entries.forEach { g ->
                        val styles = order.filter { it.group == g }
                        CivilightText(GalleryStyleCopy.group(g.label, styles.size), type.small, Modifier.height(20.dp).padding(horizontal = 4.dp), color = colors.ink3, maxLines = 1)
                        styles.chunked(2).forEach { pair ->
                            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                pair.forEach { st -> StyleRow(st, st == picked, st == current, { onPick(nearest(raw, order.indexOf(st), order.size)) }, Modifier.weight(1f).fillMaxHeight()) }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                    val lowH = minOf((maxHeight.value * 0.28f).coerceAtLeast(150f), maxHeight.value * 0.5f).coerceAtLeast(0f)
                    val bigH = (maxHeight.value - lowH - 12f).coerceAtLeast(0f)
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Stage(raw, data, screenW, screenH, inner = true, onPick = onPick, modifier = Modifier.fillMaxWidth().height(bigH.dp))
                        Row(Modifier.fillMaxWidth().height(lowH.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val cs = (lowH / COVER_H).coerceAtLeast(0.01f)
                            val cvShape = RoundedCornerShape(16.dp)
                            Box(Modifier.size((COVER_W * cs).dp, lowH.dp).clip(cvShape).background(colors.tint).insetOutline(colors.line, 1.dp, cvShape).clearAndSetSemantics { }) {
                                ScaledScene(picked, data, COVER_W, COVER_H, cs, inner = false)
                            }
                            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                CivilightText(picked.label, type.title, color = colors.ink, maxLines = 1)
                                CivilightText(picked.fragment, type.small, color = colors.ink2, maxLines = 2)
                                CivilightText(GalleryStyleCopy.BOTH_FORMS, type.small, color = colors.ink3, maxLines = 2)
                                Spacer(Modifier.weight(1f))
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    StyleRoundButton(IconId.Prev, GalleryStyleCopy.BTN_PREV_STYLE, { onPick(raw - 1) })
                                    CivilightText(
                                        GalleryStyleCopy.count(StylePickRules.wrap(raw, order.size) + 1, order.size),
                                        type.number,
                                        Modifier.weight(1f),
                                        color = colors.ink2,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center,
                                    )
                                    StyleRoundButton(IconId.Next, GalleryStyleCopy.BTN_NEXT_STYLE, { onPick(raw + 1) })
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CivilightIcon(SWIPE_ICON, null, size = 14.dp, tint = colors.ink3)
                    CivilightText(GalleryStyleCopy.join(listOf(GalleryStyleCopy.PREVIEW, GalleryStyleCopy.SWIPE_HINT)), type.small, color = colors.ink3, maxLines = 1)
                }
                Actions(picked, current, onApply, onClose, Modifier, fixed = true)
            }
        }
    }
}

/** The cover form a preview is drawn at inside the inner dialog (Galaxy Z Fold 8 cover, board 475 × 751). */
private const val COVER_W = 475f
private const val COVER_H = 751f

@Composable
private fun StyleRow(style: GalleryStyle, on: Boolean, now: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(14.dp)
    val bg by animatedColor(if (on) colors.sel else colors.raise.copy(alpha = 0f), label = "styleRow")
    Box(modifier.heightIn(min = 44.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(bg)
                .insetOutline(if (on) colors.acc else colors.line, if (on) 2.dp else 1.dp, shape)
                .semantics { selected = on; contentDescription = GalleryStyleCopy.tile(style) }
                .pressable(PressAction.Select(on, onClick), role = Role.Button, shape = shape)
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CivilightIcon(style.icon, null, size = 16.dp, tint = colors.ink2)
                CivilightText(style.label, type.label, color = colors.ink, maxLines = 1)
            }
            CivilightText(style.fragment, type.micro, color = colors.ink3, maxLines = 1)
        }
        if (on) CheckBadge(Modifier.align(Alignment.TopStart).padding(5.dp))
        if (now) NowBadge(Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = (-6).dp))
    }
}
