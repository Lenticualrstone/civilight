package io.github.lenticualrstone.civilight.feature.cast.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.designsystem.component.HoloCard
import io.github.lenticualrstone.civilight.designsystem.component.ImmersiveMath
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.cast.image.SlotImage
import io.github.lenticualrstone.civilight.feature.cast.image.rememberSlotPainter

/**
 * What the record's hero draws (CST-13): a full-bleed back layer and, over it, the portrait card. The two are
 * independent, so a hologram portrait keeps its back layer and a background image keeps the holo card.
 *
 * - [back]: the background image, else the portrait when [RecordHeroLayout.layers]' `portraitBack` (the
 *   `초상 앰비언트 블러` setting) is on.
 * - [backIsPortrait]: the back layer is the portrait.
 * - [ambient]: the portrait sits behind its own card, so the whole back layer is blurred (glass blur).
 * - [card]: the portrait card (tilting holo when the portrait's holo is on, still otherwise).
 */
data class HeroLayers(
    val back: SlotImage?,
    val backIsPortrait: Boolean,
    val ambient: Boolean,
    val card: SlotImage?,
) {
    val isEmpty: Boolean get() = back == null && card == null
}

/** The card's place in the hero, in dp from the hero's top-left (0 × 0 without room). */
data class HeroCardBox(val left: Float, val top: Float, val width: Float, val height: Float)

/**
 * Which picker a tap over the record's art opens (CST-13), and when the title opens its sheet. Pure, so the
 * choice is JVM-tested.
 *
 * - The art: the background picker while a background image is set, else the portrait picker (as before the
 *   immersive frame: portrait tap → portrait, background tap → background).
 * - The portrait card (holo or still): the portrait picker.
 * - The title: `이름`, or `표시 이름` while the title shows the display name (`???`), so the title edits what it
 *   shows. The subtitle: `한 줄 소개`. Both only while the record is editable and nothing is masked.
 */
object RecordHeroTaps {
    fun artField(hasBackground: Boolean): FixedField = if (hasBackground) FixedField.BACKGROUND else FixedField.PORTRAIT

    val cardField: FixedField = FixedField.PORTRAIT

    fun label(field: FixedField): String = when (field) {
        FixedField.BACKGROUND -> CastUiCopy.BACKGROUND
        FixedField.PORTRAIT -> CastUiCopy.PORTRAIT
        FixedField.NAME -> CastUiCopy.NAME
        FixedField.DISPLAY_NAME -> CastUiCopy.DISPLAY_NAME
        FixedField.ONE_LINER -> CastUiCopy.ONE_LINER
        FixedField.TAGS -> CastUiCopy.TAGS
    }

    /** The art and card pickers: editable records only. */
    fun artTaps(editable: Boolean): Boolean = editable

    /**
     * The title's sheet, or null: never read-only (or leaving), never while the record is hidden and the hidden
     * items are locked. [overridden]: the title shows the display name instead of the name.
     */
    fun titleField(editable: Boolean, hidden: Boolean, hiddenUnlocked: Boolean, overridden: Boolean): FixedField? = when {
        !editable -> null
        hidden && !hiddenUnlocked -> null
        overridden -> FixedField.DISPLAY_NAME
        else -> FixedField.NAME
    }

    /** The subtitle's sheet (`한 줄 소개`), or null on the same terms as [titleField]. */
    fun subtitleField(editable: Boolean, hidden: Boolean, hiddenUnlocked: Boolean): FixedField? =
        if (!editable || (hidden && !hiddenUnlocked)) null else FixedField.ONE_LINER
}

/** The pure rules behind [RecordImmersiveHero]: which layers draw, and where the card sits. */
object RecordHeroLayout {
    /** The holo board's card (`gen_holo.py` `.card`): 300 × 430, radius 22. */
    const val CARD_W = 300f
    const val CARD_H = 430f
    const val CARD_RADIUS = 22f

    /** Gap between the header row and the card. */
    const val CARD_GAP_TOP = 8f

    /** Gap between the card's foot and the resting title. */
    const val CARD_GAP_BOTTOM = 20f

    /**
     * Which layers draw. [portraitBack] is the `초상 앰비언트 블러` setting: the portrait may be the back layer when
     * there is no background image. With it off and no background, the portrait still shows as the (still) card.
     */
    fun layers(background: SlotImage?, portrait: SlotImage?, portraitBack: Boolean): HeroLayers {
        val holo = portrait?.holo == true
        val back = background ?: portrait?.takeIf { portraitBack }
        val backIsPortrait = background == null && back != null
        val card = when {
            portrait == null -> null
            holo -> portrait
            // The portrait would otherwise be on nowhere: show it as the still card.
            back == null -> portrait
            else -> null
        }
        return HeroLayers(back = back, backIsPortrait = backIsPortrait, ambient = backIsPortrait && card != null, card = card)
    }

    /** The card's width in a space [space] wide (narrower than 300: the space). */
    fun cardWidth(space: Float): Float = minOf(space, CARD_W).coerceAtLeast(0f)

    fun cardHeight(cardWidth: Float): Float = cardWidth * CARD_H / CARD_W

    /**
     * Where the card sits in a hero [width] × [height] (the pane, not the window): inside the gutters, between
     * the header row ([statusInset] + 56 + [CARD_GAP_TOP]) and the resting title block ([CARD_GAP_BOTTOM] above
     * it), centred in that space; 300 × 430 at most, smaller (same ratio) when the space is shorter.
     */
    fun cardBox(width: Float, height: Float, statusInset: Float, navInset: Float, hasSubtitle: Boolean): HeroCardBox {
        val wide = ImmersiveMath.isWide(width)
        val gutter = ImmersiveMath.gutterDp(wide)
        val lineHeight = ImmersiveMath.titleLineHeight(ImmersiveMath.titleStartSp(wide))
        val top = ImmersiveMath.headerHeight(statusInset) + CARD_GAP_TOP
        val bottom = ImmersiveMath.titleStartY(height, lineHeight, navInset, hasBar = true, hasSubtitle = hasSubtitle) - CARD_GAP_BOTTOM
        val room = (bottom - top).coerceAtLeast(0f)
        var w = cardWidth(width - gutter * 2)
        var h = cardHeight(w)
        if (h > room) {
            h = room
            w = h * CARD_W / CARD_H
        }
        return HeroCardBox(left = (width - w) / 2f, top = top + (room - h) / 2f, width = w, height = h)
    }

    /** Board: art fills the screen, cropped with top alignment (the face stays in), background and portrait alike. */
    val alignment: Alignment = Alignment.TopCenter
}

/**
 * The record's immersive hero (`Immersive.dc.html` kind=char): the back layer full-bleed (crop, top-aligned; the
 * portrait blurred when it sits behind its own card), else a colour field; the portrait card (holo: the tilting
 * 300 × 430 card) over it. Sized from the space the scaffold gives it, so it works inside a pane. Draw only: the
 * scaffold's scroller lies over the hero, so taps go to its `onHeroClick` and [RecordHeroTargets]. The holo card
 * tilts by the device sensor (else the idle sway); a finger drag does not reach it under the scroller.
 * [navInset]: the bottom inset the scaffold uses (its `bottomInset`, else the navigation bar).
 */
@Composable
fun RecordImmersiveHero(
    layers: HeroLayers,
    hasSubtitle: Boolean,
    navInset: Dp? = null,
) {
    val backPainter = rememberSlotPainter(layers.back)
    val cardPainter = rememberSlotPainter(layers.card)
    val colors = CivilightTheme.colors
    val glassBlur = CivilightTheme.blur.glass
    val density = LocalDensity.current
    val statusDp = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navDp = navInset ?: with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    val field = remember(colors) { Brush.verticalGradient(listOf(colors.tint, colors.bg)) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (backPainter != null) {
            Image(
                backPainter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (layers.ambient) Modifier.blur(glassBlur) else Modifier),
                contentScale = ContentScale.Crop,
                alignment = RecordHeroLayout.alignment,
            )
        } else {
            Box(Modifier.fillMaxSize().background(field))
        }
        val card = layers.card
        if (cardPainter != null && card != null) {
            val box = RecordHeroLayout.cardBox(maxWidth.value, maxHeight.value, statusDp.value, navDp.value, hasSubtitle)
            if (box.width > 0f && box.height > 0f) {
                HoloCard(
                    on = card.holo,
                    shape = RoundedCornerShape(RecordHeroLayout.CARD_RADIUS.dp),
                    modifier = Modifier
                        .offset(x = box.left.dp, y = box.top.dp)
                        .size(box.width.dp, box.height.dp),
                ) {
                    Image(cardPainter, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
                }
            }
        }
    }
}

/**
 * The portrait card's tap target for the scaffold's `heroTargets`: a transparent button at the card's place
 * ([RecordHeroLayout.cardBox], same inputs as [RecordImmersiveHero]); nothing when no card draws.
 */
@Composable
fun BoxScope.RecordHeroTargets(
    layers: HeroLayers,
    hasSubtitle: Boolean,
    navInset: Dp?,
    onCard: () -> Unit,
) {
    if (layers.card == null) return
    val density = LocalDensity.current
    val statusDp = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navDp = navInset ?: with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    BoxWithConstraints(Modifier.matchParentSize()) {
        val box = RecordHeroLayout.cardBox(maxWidth.value, maxHeight.value, statusDp.value, navDp.value, hasSubtitle)
        if (box.width > 0f && box.height > 0f) {
            val label = RecordHeroTaps.label(RecordHeroTaps.cardField)
            Box(
                Modifier
                    .offset(x = box.left.dp, y = box.top.dp)
                    .size(box.width.dp, box.height.dp)
                    .pressable(onClick = onCard, scaleOnPress = false, shape = RoundedCornerShape(RecordHeroLayout.CARD_RADIUS.dp), pressOverlay = false)
                    .semantics { contentDescription = label },
            )
        }
    }
}
