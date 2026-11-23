package io.github.lenticualrstone.civilight.feature.cast.list.cards3d

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ImageStatusChip
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.list.CastCard
import io.github.lenticualrstone.civilight.domain.cast.list.LockRow
import io.github.lenticualrstone.civilight.feature.cast.common.HiddenMark
import io.github.lenticualrstone.civilight.feature.cast.image.CharacterImages
import io.github.lenticualrstone.civilight.feature.cast.image.SlotImage
import io.github.lenticualrstone.civilight.feature.cast.image.rememberSlotPainter

/** Strength of the light band over the highlight colour. */
private const val SHEEN_ALPHA = 0.55f

/** Card corner radius of the 3D views. */
internal val CARD_RADIUS = 18.dp

/** Below this width a card shows only its title under the portrait (타로, small walls). */
private val COMPACT_W = 140.dp

/**
 * Shading drawn over a card ([dim] 0..1, read in the draw phase) and, while [sheen] is set, a soft light band
 * that slides across with the tilt (U-16 light, 기울어진 벽 holo sheen). The band's gradient is built once per
 * card size and only moved per frame.
 */
internal fun Modifier.cardLight(shade: Color, band: Color, dim: () -> Float, sheen: (() -> Float)?): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    // The band centred on x = 0; each frame shifts it to the light's place.
    val brush = if (sheen == null) {
        null
    } else {
        Brush.linearGradient(
            0f to Color.Transparent,
            0.5f to band.copy(alpha = band.alpha * SHEEN_ALPHA),
            1f to Color.Transparent,
            start = Offset(-w * 0.35f, 0f),
            end = Offset(w * 0.35f, h),
        )
    }
    onDrawWithContent {
        drawContent()
        val d = dim()
        if (d > 0.01f) drawRect(shade, alpha = d.coerceIn(0f, 0.85f))
        if (brush == null || sheen == null) return@onDrawWithContent
        val centre = sheen() * (w + h) - h * 0.5f
        translate(left = centre) { drawRect(brush, topLeft = Offset(-centre, 0f), size = size) }
    }
}

/**
 * The front of a character card (CST-04 content rules carried to CST-67): the portrait 4:5 crop or the first
 * letter, `홀로` badge, the `숨김` mark of an unlocked hidden character, title, first appearance and subtitle.
 * [parallax] (−1..1, read in the layer) shifts the portrait slightly against the card.
 */
@Composable
internal fun CardFront(
    card: CastCard,
    image: SlotImage?,
    hidden: Boolean,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    parallax: (() -> Float)? = null,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(CARD_RADIUS)
    val painter = rememberSlotPainter(image)
    val compact = width < COMPACT_W
    Column(
        modifier.size(width, height).clip(shape).background(colors.raise).border(1.dp, colors.line, shape),
    ) {
        Box(Modifier.fillMaxWidth().height(width * 5 / 4).clip(RoundedCornerShape(topStart = CARD_RADIUS, topEnd = CARD_RADIUS)).background(colors.tint)) {
            if (painter != null) {
                Image(
                    painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        val p = parallax?.invoke() ?: 0f
                        val s = if (parallax != null) CivilightMotion.cardImageScale else 1f
                        scaleX = s
                        scaleY = s
                        translationX = -p * CivilightMotion.cardParallax * size.width
                    },
                )
            } else {
                CivilightText(card.initial, if (compact) type.title else type.display, Modifier.align(Alignment.Center), color = colors.ink3)
            }
            if (card.holo && painter != null) ImageStatusChip(CharacterImages.HOLO, Modifier.align(Alignment.TopStart).padding(8.dp))
            if (hidden) HiddenMark(Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = if (compact) 8.dp else 12.dp, vertical = if (compact) 6.dp else 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CivilightText(card.title, if (compact) type.small else type.itemTitle, Modifier.weight(1f), color = colors.ink, maxLines = 1)
                if (!compact) card.firstAppearance?.let { CivilightText(it, type.number.merge(TextStyle(fontSize = 12.sp)), color = colors.ink3, maxLines = 1) }
            }
            if (!compact && card.subtitle.isNotEmpty()) CivilightText(card.subtitle, type.small, color = colors.ink3, maxLines = 2)
        }
    }
}

/** A landscape index card for the 롤로덱스: small portrait at the start, title, subtitle and first appearance. */
@Composable
internal fun IndexCardFront(card: CastCard, image: SlotImage?, hidden: Boolean, width: Dp, height: Dp, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(CARD_RADIUS)
    val painter = rememberSlotPainter(image)
    val portraitW = height * 4 / 5 - 8.dp
    Row(modifier.size(width, height).clip(shape).background(colors.raise).border(1.dp, colors.line, shape).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(portraitW, height - 24.dp).clip(RoundedCornerShape(12.dp)).background(colors.tint)) {
            if (painter != null) {
                Image(painter, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                CivilightText(card.initial, type.title, Modifier.align(Alignment.Center), color = colors.ink3)
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CivilightText(card.title, type.itemTitle, Modifier.weight(1f), color = colors.ink, maxLines = 1)
                card.firstAppearance?.let { CivilightText(it, type.number.merge(TextStyle(fontSize = 12.sp)), color = colors.ink3, maxLines = 1) }
            }
            if (hidden) HiddenMark(Modifier.padding(top = 4.dp))
            if (card.subtitle.isNotEmpty()) CivilightText(card.subtitle, type.small, Modifier.padding(top = 4.dp), color = colors.ink3, maxLines = 3)
            if (card.holo && painter != null) ImageStatusChip(CharacterImages.HOLO, Modifier.padding(top = 6.dp))
        }
    }
}

/** The back of a flipped 부채꼴 덱 card: title, `한 줄 소개`, up to two `항목 · 값` lines and `열기`. */
@Composable
internal fun CardBackFace(back: CardBack?, width: Dp, height: Dp, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(CARD_RADIUS)
    Column(
        modifier.size(width, height).clip(shape).background(colors.raise).border(1.dp, colors.line, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (back != null) {
            CivilightText(back.title, type.title, color = colors.ink, maxLines = 1)
            back.line?.let { CivilightText(it, type.body, color = colors.ink2, maxLines = 4) }
            for (f in back.fields) {
                Column {
                    CivilightText(f.label, type.small, color = colors.ink3, maxLines = 1)
                    CivilightText(f.value, type.body, color = colors.ink, maxLines = 2)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        CivilightIconButton(IconButtonSpec.CARD_OPEN, onOpen)
    }
}

/** The face-down back of a 타로 card: a pattern only, nothing about who is under it. */
@Composable
internal fun TarotBackFace(width: Dp, height: Dp, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(CARD_RADIUS)
    Box(
        modifier.size(width, height).clip(shape).background(colors.tint).border(1.dp, colors.edge, shape)
            .drawWithContent {
                drawContent()
                val inset = size.minDimension * 0.12f
                val c = colors.edge
                drawRect(c, topLeft = Offset(inset, inset), size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
            },
        contentAlignment = Alignment.Center,
    ) {
        CivilightIcon(IconId.Fan, contentDescription = null, tint = colors.ink3)
    }
}

/**
 * The lock card at the end of a view (CST-67, rule 19 §6): `숨긴 인물 <n>`, `지문 · PIN으로 열림` and
 * `지문으로 보기`. It names no hidden character and counts only what the list's lock row counts.
 */
@Composable
internal fun LockCardFace(lock: LockRow, width: Dp, height: Dp, onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val shape = RoundedCornerShape(CARD_RADIUS)
    val compact = width < COMPACT_W
    Column(
        modifier.size(width, height).clip(shape).background(colors.tint).border(1.dp, colors.line, shape).padding(if (compact) 8.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        CivilightIcon(IconId.Lock, contentDescription = null, tint = colors.ink2)
        CivilightText(lock.title, if (compact) type.small else type.itemTitle, color = colors.ink, maxLines = 1)
        if (!compact) CivilightText(lock.sub, type.small, color = colors.ink3, maxLines = 2)
        CivilightIconButton(IconButtonSpec.SHOW_WITH_FINGERPRINT, onUnlock)
    }
}
