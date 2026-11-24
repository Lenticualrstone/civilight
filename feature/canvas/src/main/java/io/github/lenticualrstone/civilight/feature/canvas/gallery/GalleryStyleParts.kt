package io.github.lenticualrstone.civilight.feature.canvas.gallery

import io.github.lenticualrstone.civilight.designsystem.foundation.horizontalSwipe
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ImageStatusChip
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.icon.iconStrokeDp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.feature.canvas.image.SlotImages
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Everything an r3 view draws from. [interactive] false = a picker preview: nothing reacts, nothing is focusable.
 * [multi] = the `여러 장 고르기` set while that mode is on (a tap toggles it), else null.
 */
@Immutable
class StyleBodyState(
    val style: GalleryStyle,
    val model: StyleModel,
    val inner: Boolean,
    val sel: Int,
    val interactive: Boolean,
    val onPick: (Int) -> Unit,
    val onOpen: (Int) -> Unit,
    val multi: Set<Key>? = null,
    val sharedImage: @Composable (Key) -> Modifier = { Modifier },
    /** Moves the pick only (◀ ▶, page bar, tabs, swipe): never toggles the `여러 장 고르기` set. */
    val onMove: (Int) -> Unit = onPick,
    /** False when [sel] is only the first-slot fallback (nothing picked yet): tapping it picks, never opens. */
    val explicit: Boolean = true,
) {
    val slot: StyleSlot? get() = model.slots.getOrNull(sel)
    val selItem: StyleSlot.Item? get() = slot as? StyleSlot.Item
    val selLocked: Boolean get() = slot is StyleSlot.Locked

    /** Tap on a place: picks it; tapping the picked one opens it (`보기`, or `잠금 풀기` on the lock bundle). */
    fun tap(i: Int) {
        if (!interactive) return
        val s = model.slots.getOrNull(i) ?: return
        when {
            multi != null -> if (s is StyleSlot.Item) onPick(i)
            i == sel && explicit -> onOpen(i)
            else -> onPick(i)
        }
    }

    fun open() {
        if (interactive && sel >= 0) onOpen(sel)
    }

    fun go(i: Int) {
        if (interactive && i >= 0 && i != sel && i < model.slots.size) onMove(i)
    }

    fun checked(i: Int): Boolean? {
        val m = multi ?: return null
        val s = model.slots.getOrNull(i) as? StyleSlot.Item ?: return null
        return s.entry.key in m
    }
}

/** The r3 boards' scene materials (`EXTRA_TOK`), one set per theme. */
@Immutable
data class StyleMaterials(
    val cork: Color, val cork2: Color, val fleck: Color, val tape: Color, val print: Color,
    val note: Color, val noteInk: Color, val noteInk2: Color,
    val film: Color, val film2: Color, val sprocket: Color, val edge2: Color, val filmInk: Color,
    val wall: Color, val wall2: Color, val floor: Color, val floor2: Color, val floorInk: Color,
    val mat: Color, val frame: Color, val plaque: Color, val plaqueInk: Color, val spot: Color, val spot2: Color,
    val rope: Color, val pin: Color, val polaroid: Color, val polaroidInk: Color, val polaroidInk2: Color,
) {
    companion object {
        private fun c(v: Long): Color = Color(0xFF000000 or v)
        private fun a(r: Int, g: Int, b: Int, alpha: Float): Color = Color(r, g, b, (alpha * 255).toInt())

        val DARK = StyleMaterials(
            c(0x2A2620), c(0x221F1A), a(255, 236, 200, .05f), a(232, 220, 190, .38f), c(0xE9E6DF),
            c(0xE9E2CF), c(0x24201A), c(0x4E473B),
            c(0x08090B), c(0x141518), c(0x2A2C31), c(0xD6B57A), c(0xECE8DF),
            c(0x24221F), c(0x1A1917), c(0x2B231C), c(0x211B16), c(0xECE8DF),
            c(0xECE8DF), c(0x0F0F10), c(0xECE8DF), c(0x2A2622), a(255, 240, 210, .16f), a(255, 240, 210, .30f),
            c(0x8A8378), c(0xB08A5A), c(0xF3F1EC), c(0x2A2826), c(0x5E5A52),
        )
        val LIGHT = StyleMaterials(
            c(0xD9CBB2), c(0xCDBC9F), a(90, 60, 20, .07f), a(255, 250, 235, .62f), c(0xFFFFFF),
            c(0xFFF8E2), c(0x1E1A16), c(0x4E473B),
            c(0x121316), c(0x1E2024), c(0xF6F5F1), c(0xD6B57A), c(0xECE8DF),
            c(0xE9E5DD), c(0xDDD8CE), c(0xB9A58C), c(0xA8937A), c(0x1E1A16),
            c(0xFFFFFF), c(0x26221E), c(0xFFFFFF), c(0x26221E), a(255, 252, 240, .55f), a(255, 250, 232, .85f),
            c(0x8C8273), c(0x9A7040), c(0xFFFFFF), c(0x26221E), c(0x5E5A52),
        )

        /** The note's `보기` button (board `.note .bt`). */
        val NOTE_BUTTON = c(0x1E1A16)
        val NOTE_BUTTON_INK = c(0xFFF8E2)

        /** 색 묶음 swatches (board `TONES`). */
        fun swatch(t: GalleryTone): Pair<Color, Color>? = when (t) {
            GalleryTone.WARM -> c(0xC8913F) to c(0xA9483C)
            GalleryTone.COOL -> c(0x2F6F8F) to c(0x7A64A8)
            GalleryTone.GREY -> c(0x6B6F7C) to c(0x8BA593)
            GalleryTone.LOCK -> null
        }
    }
}

@Composable
fun styleMaterials(): StyleMaterials = if (CivilightTheme.colors.isDark) StyleMaterials.DARK else StyleMaterials.LIGHT

private val iconCache = HashMap<String, ImageVector>()

/** A board outline icon (24 × 24 path, stroke 1.6, round caps) as a vector, built once. */
fun styleIcon(name: String, path: String): ImageVector = synchronized(iconCache) {
    iconCache.getOrPut(name) {
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(
                pathData = PathParser().parsePathString(path).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = iconStrokeDp(24f),
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ).build()
    }
}

val GalleryStyle.icon: ImageVector get() = styleIcon("gallery-style-$id", iconPath)

/** The accessibility name of a place (board `alabel`): `등불 앞의 리엔 · 홀로`, `숨긴 이미지 · 잠김`. */
fun StyleSlot.a11y(): String = when (this) {
    is StyleSlot.Item -> GalleryStyleCopy.join(listOfNotNull(info.title, GalleryStyleCopy.HOLO.takeIf { info.holo }))
    is StyleSlot.Locked -> GalleryStyleCopy.join(listOf(label, GalleryStyleCopy.LOCKED))
}

/**
 * A place's picture filling its box: the image (whole original, cropped to the box; holo foil when on), or the
 * lock bundle drawn as a tinted box with the lock mark only.
 */
@Composable
fun BoxScope.StylePicture(
    slot: StyleSlot,
    maxSide: Int = GalleryImageSizes.TILE,
    lockSize: Dp = 22.dp,
    lockFill: Color? = null,
    lockTint: Color? = null,
    chip: Boolean = false,
    imageModifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fitCorner: Dp? = null,
) {
    val colors = CivilightTheme.colors
    when (slot) {
        is StyleSlot.Item -> {
            GalleryImage(slot.entry.item, null, maxSide, imageModifier = imageModifier, contentScale = contentScale, fitCorner = fitCorner)
            if (chip) {
                val text = listOfNotNull(GalleryUiCopy.HIDDEN_TAG.takeIf { slot.entry.hiddenTag }, slot.entry.chip).joinToString(GalleryUiCopy.SEP).ifEmpty { null }
                text?.let { ImageStatusChip(it, Modifier.align(Alignment.TopStart).padding(8.dp)) }
            }
        }
        is StyleSlot.Locked -> Box(
            Modifier.matchParentSize().background(lockFill ?: colors.tint).insetOutline(colors.edge, 1.dp, RoundedCornerShape(0.dp)),
            contentAlignment = Alignment.Center,
        ) { CivilightIcon(IconId.Lock, null, size = lockSize, tint = lockTint ?: colors.ink2) }
    }
}

/** The tile caption over the picture's foot (board `.jc`): white on a dark fade; untitled at .72. */
@Composable
fun BoxScope.StyleCaption(slot: StyleSlot, endPad: Dp = 8.dp) {
    val item = slot as? StyleSlot.Item ?: return
    Box(
        Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, FixedColors.imageShadeBottom.copy(alpha = 0.66f))))
            .padding(start = 8.dp, end = endPad, top = 18.dp, bottom = 6.dp),
    ) {
        CivilightText(
            item.info.title,
            CivilightTheme.type.small,
            color = if (item.info.untitled) FixedColors.onImage.copy(alpha = 0.72f) else FixedColors.onImage,
            maxLines = 1,
        )
    }
}

/** The picked tile's pill (board `.pk`): `보기` on an image, `잠금 풀기` on the lock bundle. Part of the tile. */
@Composable
fun StylePill(locked: Boolean, modifier: Modifier = Modifier, iconOnly: Boolean = false) {
    val colors = CivilightTheme.colors
    Row(
        modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.pri)
            .padding(horizontal = if (iconOnly) 7.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        CivilightIcon(if (locked) IconId.Fingerprint else IconId.Expand, null, size = 14.dp, tint = colors.onpri)
        if (!iconOnly) CivilightText(if (locked) GalleryStyleCopy.BTN_UNLOCK else GalleryStyleCopy.VIEW, CivilightTheme.type.small, color = colors.onpri, maxLines = 1)
    }
}

/** The pick ring (board `.it.on`): 2 dp `bg` gap, then 2 dp `acc`, fading on `standard`. */
@Composable
fun Modifier.styleRing(on: Boolean, radius: Dp, width: Dp = 2.dp, gap: Boolean = true): Modifier {
    val colors = CivilightTheme.colors
    val ring by animatedFraction(on, label = "styleRing")
    return drawWithContent {
        drawContent()
        val a = ring
        if (a <= 0f) return@drawWithContent
        val g = width.toPx()
        val r = radius.toPx()
        if (gap) {
            drawRoundRect(colors.bg, Offset(g / 2, g / 2), Size(size.width - g, size.height - g), CornerRadius(r), style = Stroke(g), alpha = a)
            drawRoundRect(colors.acc, Offset(-g / 2, -g / 2), Size(size.width + g, size.height + g), CornerRadius(r + g), style = Stroke(g), alpha = a)
        } else {
            drawRoundRect(colors.acc, Offset(-g / 2, -g / 2), Size(size.width + g, size.height + g), CornerRadius(r + g / 2), style = Stroke(g), alpha = a)
        }
    }
}

/** A place as a button: picks, then opens; selected state and the place's name for accessibility. */
@Composable
fun Modifier.stylePlace(s: StyleBodyState, i: Int, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(0.dp)): Modifier {
    val slot = s.model.slots.getOrNull(i) ?: return this
    val picked = i == s.sel
    val name = slot.a11y()
    return if (!s.interactive) this
    else this
        .semantics { selected = picked; contentDescription = name }
        .pressable(PressAction.Select(picked) { s.tap(i) }, role = Role.Button, shape = shape)
}

/** The `여러 장 고르기` mark on a place while that mode is on. */
@Composable
fun BoxScope.StyleCheck(s: StyleBodyState, i: Int) {
    val checked = s.checked(i) ?: return
    val colors = CivilightTheme.colors
    Box(
        Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (checked) colors.acc else FixedColors.imageChipFill)
            .insetOutline(FixedColors.onImage, 1.5.dp, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) { if (checked) CivilightIcon(IconId.Check, null, size = 14.dp, tint = colors.onpri) }
}

/** A round control (board `.nb`): 44 dp, `tint` with a `line` ring; disabled at .32. */
@Composable
fun StyleRoundButton(
    icon: IconId,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    fill: Color? = null,
    ring: Color? = null,
    tint: Color? = null,
    modifier: Modifier = Modifier,
) {
    val colors = CivilightTheme.colors
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(fill ?: colors.tint)
            .insetOutline(ring ?: colors.line, 1.dp, CircleShape)
            .semantics {
                contentDescription = label
                if (!enabled) disabled()
            }
            .pressable(onClick = onClick, enabled = enabled, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        CivilightIcon(icon, null, size = 20.dp, tint = (tint ?: colors.ink).copy(alpha = if (enabled) 1f else 0.32f))
    }
}

/** The primary action of a view (board `.bt`): `보기`, or `잠금 풀기` on the lock bundle. */
@Composable
fun StyleAction(
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Color? = null,
    ink: Color? = null,
    enabled: Boolean = true,
) {
    val colors = CivilightTheme.colors
    Row(
        modifier
            .heightIn(min = 44.dp)
            .widthIn(min = 44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(fill ?: colors.pri)
            // Inside a picker preview the button is a picture: no press, no focus.
            .then(if (enabled) Modifier.pressable(onClick = onClick, shape = RoundedCornerShape(22.dp)) else Modifier)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        CivilightIcon(if (locked) IconId.Fingerprint else IconId.Expand, null, size = 18.dp, tint = ink ?: colors.onpri)
        CivilightText(if (locked) GalleryStyleCopy.BTN_UNLOCK else GalleryStyleCopy.VIEW, CivilightTheme.type.button, color = ink ?: colors.onpri, maxLines = 1)
    }
}

/** ◀ `3 / 8` ▶ through the view's own order (board `navctl`). */
@Composable
fun StyleNav(s: StyleBodyState, order: List<Int>, modifier: Modifier = Modifier, prevLabel: String = GalleryUiCopy.BTN_PREV, nextLabel: String = GalleryUiCopy.BTN_NEXT) {
    val colors = CivilightTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        StyleRoundButton(IconId.Prev, prevLabel, { s.go(GalleryStylePlans.step(order, s.sel, -1)) }, enabled = s.interactive && GalleryStylePlans.hasPrev(order, s.sel))
        CivilightText(
            GalleryStylePlans.count(s.model.facts, order, s.sel),
            CivilightTheme.type.number,
            Modifier.widthIn(min = 40.dp),
            color = colors.ink2,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        StyleRoundButton(IconId.Next, nextLabel, { s.go(GalleryStylePlans.step(order, s.sel, 1)) }, enabled = s.interactive && GalleryStylePlans.hasNext(order, s.sel))
    }
}

/** ◀ `1 / 3` ▶ under a paged view (more than one page only): jumps to the first place of the page. */
@Composable
fun StylePageBar(s: StyleBodyState, pages: StylePages, modifier: Modifier = Modifier) {
    if (pages.pages.size <= 1) return
    val page = pages.pageOf(s.sel)
    Row(modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        StyleRoundButton(IconId.Prev, GalleryStyleCopy.BTN_PREV_PAGE, { pages.pages.getOrNull(page - 1)?.firstOrNull()?.let(s::go) }, enabled = s.interactive && page > 0)
        CivilightText(GalleryStyleCopy.count(page + 1, pages.pages.size), CivilightTheme.type.number, color = CivilightTheme.colors.ink2, maxLines = 1)
        StyleRoundButton(IconId.Next, GalleryStyleCopy.BTN_NEXT_PAGE, { pages.pages.getOrNull(page + 1)?.firstOrNull()?.let(s::go) }, enabled = s.interactive && page < pages.pages.lastIndex)
    }
}

/** A sideways swipe on a stage moves one step (an extra to the visible ◀ ▶; taps still reach the places). */
@Composable
fun Modifier.styleSwipe(enabled: Boolean, onStep: (Int) -> Unit): Modifier {
    if (!enabled) return this
    val step by rememberUpdatedState(onStep)
    val threshold = with(LocalDensity.current) { 56.dp.toPx() }
    // 0.2.3: the shared axis rule, so a vertical or diagonal drag still scrolls the screen.
    val dx = remember { floatArrayOf(0f) }
    return horizontalSwipe(
        onStart = { dx[0] = 0f },
        onDelta = { d -> dx[0] += d },
        onStop = {
            if (dx[0] <= -threshold) step(1) else if (dx[0] >= threshold) step(-1)
            dx[0] = 0f
        },
    )
}

/** A process-wide memo of each original's colour group (hash → tone), so 색 묶음 does not decode twice. */
private object ToneCache {
    private val map = object : LinkedHashMap<String, GalleryTone>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, GalleryTone>?): Boolean = size > 512
    }

    fun get(hash: String): GalleryTone? = synchronized(map) { map[hash] }

    fun put(hash: String, tone: GalleryTone) = synchronized(map) { map[hash] = tone }
}

/**
 * The colour group of every place (lock bundles → [GalleryTone.LOCK]): presets from their colours, originals from
 * the mean of a tiny decode (24 px, off the main thread, cached by hash). Null until every group is known.
 */
@Composable
fun rememberTones(model: StyleModel): List<GalleryTone>? {
    val deps = LocalFeatureDeps.current
    val keys = model.slots.map { s -> (s as? StyleSlot.Item)?.entry?.item?.source ?: s.id }
    // The value carries the keys it was made for: produceState keeps the old value across a key change, and a list
    // of the same length from an older slot order must not be read against the new slots.
    val made by produceState<Pair<List<String>, List<GalleryTone>>?>(null, keys) {
        value = keys to withContext(Dispatchers.IO) {
            model.slots.map { s ->
                when (s) {
                    is StyleSlot.Locked -> GalleryTone.LOCK
                    is StyleSlot.Item -> {
                        val item = s.entry.item
                        val hash = item.blobHash
                        when {
                            item.isPreset -> GalleryStylePlans.presetTone(item.presetId)
                            hash == null -> GalleryTone.GREY
                            else -> ToneCache.get(hash) ?: run {
                                val tone = runCatching {
                                    deps.stores.blobs.fileOf(hash)?.let { f -> SlotImages.decode(f, null, 24) }?.let { bmp ->
                                        var r = 0L
                                        var g = 0L
                                        var b = 0L
                                        val w = bmp.width
                                        val h = bmp.height
                                        val px = IntArray(w * h)
                                        bmp.getPixels(px, 0, w, 0, 0, w, h)
                                        px.forEach { c ->
                                            r += (c shr 16) and 0xFF
                                            g += (c shr 8) and 0xFF
                                            b += c and 0xFF
                                        }
                                        val n = px.size.coerceAtLeast(1)
                                        GalleryStylePlans.classify((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
                                    }
                                }.getOrNull() ?: GalleryTone.GREY
                                ToneCache.put(hash, tone)
                                tone
                            }
                        }
                    }
                }
            }
        }
    }
    return made?.takeIf { it.first == keys }?.second
}
