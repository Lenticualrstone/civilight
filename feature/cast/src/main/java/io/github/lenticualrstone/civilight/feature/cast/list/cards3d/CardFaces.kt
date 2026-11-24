package io.github.lenticualrstone.civilight.feature.cast.list.cards3d

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.HoloStrength
import io.github.lenticualrstone.civilight.designsystem.component.HoloPoseSource
import io.github.lenticualrstone.civilight.designsystem.component.holoImage
import io.github.lenticualrstone.civilight.designsystem.component.holoImageFoil
import io.github.lenticualrstone.civilight.designsystem.component.holoFoil
import io.github.lenticualrstone.civilight.designsystem.component.holoImage
import io.github.lenticualrstone.civilight.designsystem.card.DeviceTilt
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import io.github.lenticualrstone.civilight.domain.cast.list.CastCard
import io.github.lenticualrstone.civilight.domain.cast.list.LockRow
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.HiddenMark
import io.github.lenticualrstone.civilight.feature.cast.image.PresetPainter
import io.github.lenticualrstone.civilight.feature.cast.image.SlotImage
import io.github.lenticualrstone.civilight.feature.cast.image.rememberSlotPainter

/**
 * Board px → dp for a card drawn at a stage scale ([k] dp per board px). Card text is sized with the card
 * (it is part of the card's art, as on the boards), so a large font setting never breaks a card face.
 */
@Stable
internal class Bp(val k: Float) {
    fun dp(v: Float): Dp = (v * k).dp
}

@Composable
@ReadOnlyComposable
private fun Bp.sp(v: Float): TextUnit = with(LocalDensity.current) { dp(v).toSp() }

/** Board `font: <weight> <size>/<line>` at the card scale, letter-spacing in em. */
@Composable
@ReadOnlyComposable
private fun Bp.font(size: Float, line: Float, weight: Int, tracking: Float = 0f): TextStyle =
    CivilightTheme.type.body.merge(
        TextStyle(fontSize = sp(size), lineHeight = sp(line), fontWeight = FontWeight(weight), letterSpacing = tracking.em),
    )

/** The preset a portrait-less card is drawn with: a stable pick from the key (board `PRE`, `SHARDS`). */
internal fun presetFor(key: String): Int = Math.floorMod(key.hashCode(), PRESET_COUNT)

private const val PRESET_COUNT = 8

/** The device tilt of the card stage while it follows the device (null: the holo surface's own source). */
internal val LocalCardTilt = staticCompositionLocalOf<DeviceTilt?> { null }

/**
 * A card's art, full bleed: the portrait (4:5 crop) or, without one, the shard preset of the key with the
 * big faint initial (board `.gl`). A portrait marked holo carries the DS holo surface (`holoImage`: the
 * `홀로` effect, not only its badge). [parallax] (−1..1) shifts the portrait against the card.
 */
@Composable
internal fun CardArt(
    card: CastCard,
    image: SlotImage?,
    bp: Bp,
    modifier: Modifier = Modifier,
    glyph: Boolean = false,
    glyphShift: (() -> Offset)? = null,
    tilt: (() -> Offset)? = null,
    parallax: (() -> Float)? = null,
) {
    val painter = rememberSlotPainter(image)
    val preset = remember(card.key) { PresetPainter(presetFor(card.key)) }
    // A holo portrait carries the DS holo surface (the `홀로` effect itself), posed by the stage's device tilt.
    val holo = card.holo && painter != null
    Box(modifier.fillMaxSize().holoImage(on = holo, tilt = LocalCardTilt.current)) {
        Image(
            painter ?: preset,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                if (parallax != null && painter != null) {
                    val p = parallax()
                    scaleX = CivilightMotion.cardImageScale
                    scaleY = CivilightMotion.cardImageScale
                    translationX = -p * CivilightMotion.cardParallax * size.width
                }
            },
        )
        if (glyph || painter == null) {
            // Board `.gl`: 210 px / 800 at right −10, top −40 of a 270-wide card, white 12 %.
            CivilightText(
                card.initial,
                bp.font(210f, 210f, 800, -0.04f),
                Modifier.align(Alignment.TopEnd).offset(x = bp.dp(10f), y = bp.dp(-40f)).graphicsLayer {
                    val s = glyphShift?.invoke() ?: Offset.Zero
                    translationX = s.x * bp.k * density
                    translationY = s.y * bp.k * density
                },
                color = CardMaterial.glyph,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
            )
        }
    }
}

/** A chip over card art (board `.fbadge i`, `.ctag`, `.kd`, `.nb`). */
@Composable
private fun ArtChip(text: String, bp: Bp, h: Float, padX: Float, radius: Float, size: Float, fill: Color, edge: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(bp.dp(radius))
    CivilightText(
        text,
        bp.font(size, h, 600),
        modifier
            .height(bp.dp(h))
            .clip(shape)
            .background(fill)
            .then(if (edge) Modifier.border(1.dp, CardMaterial.artChipEdge, shape) else Modifier)
            .padding(horizontal = bp.dp(padX))
            .wrapContentSize(Alignment.Center),
        color = CardMaterial.onArt,
        maxLines = 1,
    )
}

/** The face style of a portrait card: 부채꼴 덱 (220 wide) or 회전 목마 (168 wide), board `front()`. */
internal enum class PortraitStyle(val radius: Float, val inset: Float, val title: Float, val titleLine: Float, val sub: Float, val subLine: Float) {
    DECK(18f, 16f, 26f, 32f, 12f, 17f),
    RING(16f, 13f, 21f, 26f, 11f, 15f),
}

/**
 * Board `front()` of cards-a: art, light band, the bottom scrim, the top badges (the first tag, e.g. `주연`,
 * and the first appearance), the name and the subtitle in white, and the inset edge light. [sheen] gives
 * the band's shift (fractions of the band box) per frame; the dim and the shadow belong to the posed card.
 */
@Composable
internal fun PortraitFace(
    card: CastCard,
    image: SlotImage?,
    hidden: Boolean,
    style: PortraitStyle,
    bp: Bp,
    sheen: () -> Offset,
    tilt: () -> Offset,
    parallax: (() -> Float)? = null,
) {
    val mat = CardMaterial.of(CivilightTheme.colors.isDark)
    val spec = remember(mat) { Sheens.deck(mat.sheen) }
    Box(Modifier.fillMaxSize()) {
        CardArt(card, image, bp, Modifier.sheen(spec, sheen), tilt = tilt, parallax = parallax)
        // Board `.scrim`: bottom 56 %, `rgba(6,7,10,.86)` → `.5` at 45 % → clear.
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.56f).background(
                Brush.verticalGradient(0f to Color.Transparent, 0.55f to CardMaterial.scrimInkMid, 1f to CardMaterial.scrimInk),
            ),
        )
        Row(
            Modifier.fillMaxWidth().padding(top = bp.dp(12f), start = bp.dp(12f), end = bp.dp(12f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            card.tags.firstOrNull()?.let { ArtChip(it, bp, 22f, 8f, 11f, 11f, CardMaterial.artChip, edge = true) }
            Spacer(Modifier.weight(1f))
            if (hidden) HiddenMark(Modifier.padding(end = bp.dp(6f)))
            card.firstAppearance?.let { ArtChip(it, bp, 22f, 8f, 11f, 11f, CardMaterial.artChip, edge = true) }
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = bp.dp(style.inset), end = bp.dp(style.inset), bottom = bp.dp(style.inset))) {
            CivilightText(card.title, bp.font(style.title, style.titleLine, 800, -0.01f), color = Color.White, maxLines = 1)
            if (card.subtitle.isNotEmpty()) {
                CivilightText(
                    card.subtitle,
                    bp.font(style.sub, style.subLine, 500),
                    Modifier.padding(top = bp.dp(3f)),
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 2,
                )
            }
        }
        EdgeLight(style.radius, bp)
    }
}

/** Board `.edge`: an inset 1 px rim (16 %) and a top highlight (22 %). */
@Composable
private fun BoxScope.EdgeLight(radius: Float, bp: Bp) {
    Box(
        Modifier.matchParentSize().drawBehind {
            val r = CornerRadius(bp.k * radius * density)
            val w = 1.dp.toPx()
            drawRoundRect(CardMaterial.artEdge, topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w), cornerRadius = r, style = Stroke(w))
            drawRoundRect(CardMaterial.artEdgeTop, topLeft = Offset(r.x, 0f), size = Size(size.width - r.x * 2, w))
        },
    )
}

/**
 * Board `back_info()`: the back of a turned 부채꼴 덱 card. The small avatar, the name and subtitle, the
 * `한 줄 소개` (or `비어 있음`), up to two `항목 · 값` rows and `열기`.
 */
@Composable
internal fun DeckBackFace(card: CastCard, back: CardBack?, bp: Bp, onOpen: () -> Unit) {
    val colors = CivilightTheme.colors
    val mat = CardMaterial.of(colors.isDark)
    Column(
        Modifier.fillMaxSize().background(mat.back).border(1.dp, colors.line, RoundedCornerShape(bp.dp(18f)))
            .padding(start = bp.dp(18f), end = bp.dp(18f), top = bp.dp(18f), bottom = bp.dp(16f)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(bp.dp(10f))) {
            Box(Modifier.size(bp.dp(36f)).clip(RoundedCornerShape(50)).background(colors.tint)) {
                val preset = remember(card.key) { PresetPainter(presetFor(card.key)) }
                Image(preset, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                CivilightText(card.initial, bp.font(15f, 15f, 800), Modifier.align(Alignment.Center), color = Color.White, maxLines = 1)
            }
            Column(Modifier.weight(1f)) {
                CivilightText(back?.title ?: card.title, bp.font(19f, 24f, 800), color = colors.ink, maxLines = 1)
                if (card.subtitle.isNotEmpty()) CivilightText(card.subtitle, bp.font(11f, 15f, 500), color = colors.ink3, maxLines = 2)
            }
        }
        CivilightText(CastUiCopy.ONE_LINER, bp.font(11f, 15f, 600, 0.04f), Modifier.padding(top = bp.dp(16f)), color = colors.ink3, maxLines = 1)
        val line = back?.line
        if (line != null) {
            CivilightText(line, bp.font(15f, 22f, 600), Modifier.padding(top = bp.dp(5f)), color = colors.ink, maxLines = 3)
        } else {
            CivilightText(CastUiCopy.EMPTY_VALUE, bp.font(15f, 22f, 500), Modifier.padding(top = bp.dp(5f)), color = colors.ink3, maxLines = 1)
        }
        Column(Modifier.padding(top = bp.dp(12f))) {
            for (f in back?.fields.orEmpty().take(CardBackModel.DECK_FIELDS)) {
                Row(
                    Modifier.fillMaxWidth().drawBehind { drawRect(colors.line, size = Size(size.width, 1.dp.toPx())) }.padding(vertical = bp.dp(9f)),
                    horizontalArrangement = Arrangement.spacedBy(bp.dp(10f)),
                ) {
                    CivilightText(f.label, bp.font(12f, 17f, 500), color = colors.ink3, maxLines = 1)
                    CivilightText(f.value, bp.font(12f, 17f, 600), Modifier.weight(1f), color = colors.ink, maxLines = 2, textAlign = TextAlign.End)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        CivilightIconButton(IconButtonSpec.CARD_OPEN, onOpen, Modifier.fillMaxWidth())
    }
}

/** Board `back_pat()`: a ring card seen from behind, its patterned back (no name: nothing about who it is). */
@Composable
internal fun PatternBackFace(bp: Bp, radius: Float) {
    val colors = CivilightTheme.colors
    val mat = CardMaterial.of(colors.isDark)
    Box(
        Modifier.fillMaxSize().background(mat.back).border(1.dp, colors.line, RoundedCornerShape(bp.dp(radius))).drawWithCache {
            val inset = bp.k * 10f * density
            val r = CornerRadius(bp.k * 10f * density)
            val inner = Size(size.width - inset * 2, size.height - inset * 2)
            val clip = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(inset, inset, inset + inner.width, inset + inner.height, r)) }
            val step = bp.k * 9f * density
            val stripe = bp.k * 2f * density
            onDrawBehind {
                clipPath(clip) {
                    // `repeating-linear-gradient(135deg, backline 0 2px, transparent 2px 9px)`.
                    rotate(45f) {
                        var x = -size.height
                        while (x < size.width + size.height) {
                            drawRect(mat.backLine, topLeft = Offset(x, -size.height), size = Size(stripe, size.height * 3))
                            x += step
                        }
                    }
                }
                drawRoundRect(colors.line, topLeft = Offset(inset, inset), size = inner, cornerRadius = r, style = Stroke(1.dp.toPx()))
            }
        },
    )
}

/**
 * Board `cface()` of the 겹친 더미: art with the big initial, the bottom shade, the first tag and the card
 * number, the name, the subtitle and `첫 등장 1-1` under a rule; inset rim.
 */
@Composable
internal fun PileFace(
    card: CastCard,
    image: SlotImage?,
    hidden: Boolean,
    number: Int,
    bp: Bp,
    sheen: () -> Offset,
    glyphShift: () -> Offset,
    tilt: () -> Offset,
) {
    Box(Modifier.fillMaxSize().background(Color(0xFF111111))) {
        CardArt(card, image, bp, Modifier.sheen(Sheens.pile, sheen), glyph = true, glyphShift = glyphShift, tilt = tilt)
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.6f)
                .background(Brush.verticalGradient(0f to Color.Transparent, 1f to CardMaterial.shadeInk)),
        )
        Row(Modifier.fillMaxWidth().padding(start = bp.dp(14f), end = bp.dp(16f), top = bp.dp(14f)), verticalAlignment = Alignment.CenterVertically) {
            card.tags.firstOrNull()?.let { ArtChip(it, bp, 24f, 9f, 8f, 11f, CardMaterial.artChip.copy(alpha = 0.5f), edge = false) }
            if (hidden) HiddenMark(Modifier.padding(start = bp.dp(6f)))
            Spacer(Modifier.weight(1f))
            CivilightText(Cards3dCopy.twoDigits(number), bp.font(12f, 16f, 600), color = CardMaterial.onArt.copy(alpha = 0.78f), maxLines = 1)
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = bp.dp(18f), end = bp.dp(18f), bottom = bp.dp(16f))) {
            CivilightText(card.title, bp.font(32f, 38f, 800, -0.02f), color = CardMaterial.onArt, maxLines = 1)
            if (card.subtitle.isNotEmpty()) CivilightText(card.subtitle, bp.font(13f, 19f, 500), color = CardMaterial.onArt.copy(alpha = 0.86f), maxLines = 2)
            card.firstAppearance?.let {
                Box(
                    Modifier.padding(top = bp.dp(10f)).fillMaxWidth()
                        .drawBehind { drawRect(CardMaterial.onArt.copy(alpha = 0.2f), size = Size(size.width, 1.dp.toPx())) }
                        .padding(top = bp.dp(10f)),
                ) {
                    CivilightText("${CastUiCopy.FIRST_APPEARANCE} $it", bp.font(12f, 16f, 500), color = CardMaterial.onArt.copy(alpha = 0.72f), maxLines = 1)
                }
            }
        }
        Box(Modifier.matchParentSize().border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(bp.dp(22f))))
    }
}

/**
 * Board `face()` of the 기울어진 벽: art, the bottom shade, the kind chip (`인물`) and the first appearance,
 * the name and the subtitle. [pulled] is the 264-wide card drawn out of the wall, with bigger type and the
 * holo foil the board lays over it (`.foil`). [labels] (0..1) hides the text of the wall behind a pulled card.
 */
@Composable
internal fun WallFace(
    card: CastCard,
    image: SlotImage?,
    hidden: Boolean,
    pulled: Boolean,
    bp: Bp,
    sheen: () -> Offset,
    tilt: () -> Offset,
    labels: () -> Float = { 1f },
) {
    val colors = CivilightTheme.colors
    val theme = if (colors.isDark) ResolvedTheme.DARK else ResolvedTheme.LIGHT
    val chipH = if (pulled) 24f else 18f
    val chipPad = if (pulled) 9f else 6f
    val chipSize = if (pulled) 12f else 10f
    val chipInset = if (pulled) 14f else 8f
    val textInset = if (pulled) 18f else 9f
    Box(Modifier.fillMaxSize().background(Color(0xFF222222))) {
        CardArt(
            card,
            image,
            bp,
            Modifier.sheen(Sheens.wall, sheen).then(if (pulled && !(card.holo && image != null)) Modifier.holoFoil(tilt, theme, { HoloStrength.CARD_FOIL }) else Modifier),
            tilt = tilt,
        )
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.64f)
                .background(Brush.verticalGradient(0f to Color.Transparent, 1f to CardMaterial.shadeInk.copy(alpha = 0.84f))),
        )
        Box(Modifier.matchParentSize().graphicsLayer { alpha = labels() }) {
            Row(Modifier.fillMaxWidth().padding(start = bp.dp(chipInset), end = bp.dp(chipInset), top = bp.dp(chipInset)), verticalAlignment = Alignment.CenterVertically) {
                ArtChip(Cards3dCopy.KIND, bp, chipH, chipPad, chipH / 2, chipSize, CardMaterial.artChip.copy(alpha = 0.45f), edge = false)
                if (hidden) HiddenMark(Modifier.padding(start = bp.dp(4f)))
                Spacer(Modifier.weight(1f))
                card.firstAppearance?.let { ArtChip(it, bp, chipH, chipPad, chipH / 2, chipSize, CardMaterial.artChip.copy(alpha = 0.6f), edge = false) }
            }
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = bp.dp(textInset), end = bp.dp(textInset), bottom = bp.dp(if (pulled) 18f else 8f))) {
                CivilightText(card.title, if (pulled) bp.font(26f, 32f, 800) else bp.font(14f, 18f, 800), color = CardMaterial.onArt, maxLines = 1)
                if (card.subtitle.isNotEmpty()) {
                    CivilightText(
                        card.subtitle,
                        if (pulled) bp.font(13f, 18f, 500) else bp.font(10f, 13f, 500),
                        Modifier.padding(top = bp.dp(if (pulled) 4f else 0f)),
                        color = CardMaterial.onArt.copy(alpha = 0.8f),
                        maxLines = if (pulled) 2 else 1,
                    )
                }
            }
        }
        if (pulled) Box(Modifier.matchParentSize().border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(bp.dp(20f))))
    }
}

/**
 * Board `tface()` back: the face-down 타로 card, a 160° gradient, a cross-hatched panel, the diamond mark and
 * the kind under it (`인물`). Nothing about who is under it.
 */
@Composable
internal fun TarotBackFace(bp: Bp) {
    val mat = CardMaterial.of(CivilightTheme.colors.isDark)
    Box(
        Modifier.fillMaxSize().drawWithCache {
            val bg = Brush.linearGradient(listOf(mat.tarotBack1, mat.tarotBack2), start = Offset(size.width * 0.33f, 0f), end = Offset(size.width * 0.67f, size.height))
            val inset = bp.k * 6f * density
            val r = CornerRadius(bp.k * 6f * density)
            val inner = Size(size.width - inset * 2, size.height - inset * 2)
            val clip = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(inset, inset, inset + inner.width, inset + inner.height, r)) }
            val step = bp.k * 8f * density
            val line = 1.dp.toPx()
            val hatch = mat.tarotLine.copy(alpha = mat.tarotLine.alpha * 0.6f)
            val d = bp.k * 30f * density
            val dc = Offset(size.width / 2, size.height * 0.44f)
            onDrawBehind {
                drawRect(bg)
                clipPath(clip) {
                    for (angle in floatArrayOf(45f, -45f)) {
                        rotate(angle) {
                            var x = -size.height
                            while (x < size.width + size.height) {
                                drawRect(hatch, topLeft = Offset(x, -size.height), size = Size(line, size.height * 3))
                                x += step
                            }
                        }
                    }
                }
                drawRoundRect(hatch, topLeft = Offset(inset, inset), size = inner, cornerRadius = r, style = Stroke(line))
                rotate(45f, dc) {
                    drawRect(mat.tarotBack2, topLeft = dc - Offset(d / 2, d / 2), size = Size(d, d))
                    drawRect(mat.tarotMark, topLeft = dc - Offset(d / 2, d / 2), size = Size(d, d), style = Stroke(line))
                    val i = d - bp.k * 18f * density
                    drawRect(mat.tarotMark, topLeft = dc - Offset(i / 2, i / 2), size = Size(i, i))
                }
            }
        },
    ) {
        CivilightText(
            Cards3dCopy.KIND,
            bp.font(10f, 12f, 600, 0.2f),
            Modifier.align(Alignment.BottomCenter).padding(bottom = bp.dp(12f)),
            color = mat.tarotMark,
            maxLines = 1,
        )
    }
}

/** Board `tface()` front: art over the top 62 %, then the kind and the name on the card colour. */
@Composable
internal fun TarotFrontFace(card: CastCard, image: SlotImage?, bp: Bp, sheen: () -> Offset, tilt: () -> Offset) {
    val colors = CivilightTheme.colors
    val mat = CardMaterial.of(colors.isDark)
    Box(Modifier.fillMaxSize().background(mat.tarotCard).sheen(Sheens.pile, sheen)) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.62f)) { CardArt(card, image, Bp(bp.k * 84f / 270f), tilt = tilt) }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = bp.dp(8f), end = bp.dp(8f), bottom = bp.dp(9f))) {
            CivilightText(Cards3dCopy.KIND, bp.font(9f, 12f, 600, 0.12f), color = colors.ink3, maxLines = 1)
            CivilightText(card.title, bp.font(14f, 18f, 800, -0.02f), color = colors.ink, maxLines = 1)
        }
    }
}

/**
 * Board `rcard()` of the 롤로덱스: an index card on paper with ruled lines, the portrait (120 × 160), the name,
 * the subtitle and up to three `항목 · 값` rows. [content] (0..1) fades all but the paper (the cards behind).
 */
@Composable
internal fun IndexCardFace(
    card: CastCard,
    image: SlotImage?,
    hidden: Boolean,
    back: CardBack?,
    bp: Bp,
    content: () -> Float,
    sheen: () -> Offset,
    tilt: () -> Offset,
) {
    val colors = CivilightTheme.colors
    val mat = CardMaterial.of(colors.isDark)
    Box(
        Modifier.fillMaxSize().background(mat.paper).border(1.dp, colors.line, RoundedCornerShape(bp.dp(16f))).drawBehind {
            // Board `.ru`: left 160, right 16, top 58, bottom 14, a rule every 32 px.
            val k = bp.k * density
            var y = 58f * k + 31f * k
            while (y < size.height - 14f * k) {
                drawRect(mat.rule, topLeft = Offset(160f * k, y), size = Size(size.width - 176f * k, 1.dp.toPx()))
                y += 32f * k
            }
        },
    ) {
        Box(Modifier.matchParentSize().graphicsLayer { alpha = content() }.sheen(Sheens.pile, sheen)) {
            Box(Modifier.offset(bp.dp(16f), bp.dp(20f)).size(bp.dp(120f), bp.dp(160f)).clip(RoundedCornerShape(bp.dp(9f)))) {
                CardArt(card, image, Bp(bp.k * 120f / 270f), tilt = tilt)
            }
            CivilightText(card.title, bp.font(24f, 30f, 800), Modifier.offset(bp.dp(152f), bp.dp(18f)).width(bp.dp(174f)), color = colors.ink, maxLines = 1)
            if (card.subtitle.isNotEmpty()) {
                CivilightText(card.subtitle, bp.font(12f, 16f, 500), Modifier.offset(bp.dp(160f), bp.dp(52f)).width(bp.dp(166f)), color = colors.ink2, maxLines = 2)
            }
            Column(Modifier.offset(bp.dp(160f), bp.dp(90f)).width(bp.dp(166f))) {
                if (hidden) HiddenMark()
                for (f in back?.fields.orEmpty().take(ROLO_ROWS)) {
                    Row(Modifier.height(bp.dp(32f)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(bp.dp(8f))) {
                        CivilightText(f.label, bp.font(12f, 16f, 500), Modifier.width(bp.dp(52f)), color = colors.ink3, maxLines = 1)
                        CivilightText(f.value, bp.font(12f, 16f, 500), color = colors.ink, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Board `.rc .rows`: three `항목 · 값` rows on an index card. */
internal const val ROLO_ROWS = 3

/** Board `.tab`: the index tab above a 롤로덱스 card, its 초성 in the accent then the name (none on the lock card). */
@Composable
internal fun IndexTab(letter: String, title: String, bp: Bp, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val mat = CardMaterial.of(colors.isDark)
    Row(
        modifier.size(bp.dp(100f), bp.dp(28f))
            .clip(RoundedCornerShape(topStart = bp.dp(10f), topEnd = bp.dp(10f)))
            .background(mat.tab)
            .border(1.dp, colors.line, RoundedCornerShape(topStart = bp.dp(10f), topEnd = bp.dp(10f))),
        horizontalArrangement = Arrangement.spacedBy(bp.dp(6f), Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CivilightText(letter, bp.font(12f, 14f, 600), color = colors.acc, maxLines = 1)
        if (title.isNotEmpty()) CivilightText(title, bp.font(12f, 14f, 700), Modifier.weight(1f, fill = false), color = colors.ink2, maxLines = 1)
    }
}

/**
 * The lock card at the end of a view (CST-67, rule 19 §6): `숨긴 인물 <n>`, `지문 · PIN으로 열림` and
 * `지문으로 보기`, sized to the view's card. It names no hidden character and counts only what the list's lock
 * row counts.
 */
@Composable
internal fun LockCardFace(lock: LockRow, bp: Bp, compact: Boolean, onUnlock: () -> Unit) {
    val colors = CivilightTheme.colors
    val mat = CardMaterial.of(colors.isDark)
    Column(
        Modifier.fillMaxSize().background(mat.back).border(1.dp, colors.line, RoundedCornerShape(bp.dp(16f))).padding(bp.dp(if (compact) 8f else 16f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(bp.dp(8f), Alignment.CenterVertically),
    ) {
        CivilightIcon(IconId.Lock, contentDescription = null, tint = colors.ink2)
        CivilightText(lock.title, if (compact) bp.font(12f, 16f, 700) else bp.font(17f, 22f, 800), color = colors.ink, maxLines = 1, textAlign = TextAlign.Center)
        if (!compact) CivilightText(lock.sub, bp.font(12f, 17f, 500), color = colors.ink3, maxLines = 2, textAlign = TextAlign.Center)
        if (!compact) CivilightIconButton(IconButtonSpec.SHOW_WITH_FINGERPRINT, onUnlock)
    }
}
