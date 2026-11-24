package io.github.lenticualrstone.civilight.ui.gallery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.PaddingValues
import io.github.lenticualrstone.civilight.designsystem.card.CardSize
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.ImageCard
import io.github.lenticualrstone.civilight.designsystem.component.ImageStatusChip
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction

/** Copy of the gallery card (GAL-07, GAL-08). */
object GalleryCardCopy {
    /** Accessibility label of the `⋯` button. */
    const val MENU = "메뉴"

    /** Accessibility name of an untitled card (the panel's `제목 없음`). */
    const val UNTITLED = "제목 없음"
    const val PORTRAIT = "초상"
    const val BACKGROUND = "배경"
    const val COVER = "표지"
    const val HOLO = "홀로"
    const val SEP = " · "
}

/** Sizes and the chip of the gallery card (GAL-07); plain values, JVM-tested. */
object GalleryCardRules {
    /** 3:4 cards: cover 222 × 296, inner 236 × 315. */
    fun size(inner: Boolean): CardSize = if (inner) CardSize(236f, 315f) else CardSize(222f, 296f)

    /** Card start to card start: 236 cover, 252 inner. */
    fun pitch(inner: Boolean): Float = if (inner) 252f else 236f

    const val RADIUS_DP: Float = 20f

    /** `gcard` `big = w >= 190`: below it the small text (15 / 20 title, 11 / 15 meta, 12 · 10 dp insets). */
    const val SMALL_BELOW_DP: Float = 190f

    /** Whether a card [widthDp] wide uses the small text (group rows 162, the profile row 132). */
    fun small(widthDp: Float): Boolean = widthDp < SMALL_BELOW_DP

    /** How far the picked card lifts (GAL-07). */
    const val LIFT_DP: Float = 10f

    /**
     * The top-left chip: `초상` · `배경` · `표지` · `홀로` as they apply, joined with ` · ` (`초상 · 홀로`);
     * null when none applies.
     */
    fun chip(portrait: Boolean, background: Boolean, cover: Boolean, holo: Boolean): String? = listOfNotNull(
        GalleryCardCopy.PORTRAIT.takeIf { portrait },
        GalleryCardCopy.BACKGROUND.takeIf { background },
        GalleryCardCopy.COVER.takeIf { cover },
        GalleryCardCopy.HOLO.takeIf { holo },
    ).joinToString(GalleryCardCopy.SEP).ifEmpty { null }

    /**
     * The selection ring's `bg` gap stroke (centre line) around a card drawn [width] × [height] px with a
     * [stroke] px line and [radius] px corners: 2 dp `bg` gap, then 2 dp `acc`. Uses the drawn size, never
     * [size], so a card shrunk by its caller (group rows, 162 × 216) gets a ring that hugs it.
     */
    fun ringGap(width: Float, height: Float, stroke: Float, radius: Float): RingRect =
        RingRect(-stroke / 2, -stroke / 2, width + stroke, height + stroke, radius + stroke / 2)

    /** The selection ring's `acc` stroke (centre line), just outside [ringGap]. */
    fun ringAcc(width: Float, height: Float, stroke: Float, radius: Float): RingRect =
        RingRect(-stroke * 1.5f, -stroke * 1.5f, width + stroke * 3, height + stroke * 3, radius + stroke * 1.5f)

    /** A title worth drawing; a blank title is an untitled card (no shade, no text). */
    fun shownTitle(title: String?): String? = title?.takeIf { it.isNotBlank() }
}

/** One ring stroke's rounded rect in px, relative to the card's top-left. */
data class RingRect(val left: Float, val top: Float, val width: Float, val height: Float, val corner: Float)

/**
 * The shared gallery card (GAL-07; `ProjectCard` anatomy, 3:4): the item's `카드 3:4` crop in [image], the
 * title and [meta] (scene code, else tags) on the bottom shade, the role / holo [chip] top-left and `⋯`
 * top-right (accessibility `메뉴`). An untitled item has no shade and no text. A [picked] card lifts 10 dp
 * with the selection ring (2 dp `bg` gap + 2 dp `acc`). Tapping the card is the only way to pick it (GAL-08).
 *
 * Shared by `feature:canvas` (gallery screen), `feature:cast` (profile section) and `feature:story` (home), so
 * none of them copies it; rows of cards use the DS `SnapCardRow` (`designsystem.card`).
 */
@Composable
fun GalleryCard(
    title: String?,
    meta: String?,
    chip: String?,
    inner: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMenu: (() -> Unit)? = null,
    picked: Boolean = false,
    small: Boolean = false,
    image: @Composable BoxScope.() -> Unit,
) {
    val colors = CivilightTheme.colors
    val motion = LocalMotion.current
    val size = GalleryCardRules.size(inner)
    val shown = GalleryCardRules.shownTitle(title)
    val ring = animatedFraction(picked, label = "galleryRing")
    val lift by animateFloatAsState(if (picked) GalleryCardRules.LIFT_DP else 0f, motion.move(CivilightMotion.standard), label = "galleryLift")
    val radius = GalleryCardRules.RADIUS_DP
    val frame = modifier
        .size(size.width.dp, size.height.dp)
        .graphicsLayer { translationY = -lift.dp.toPx() }
        .semantics { selected = picked }
        .drawWithContent {
            drawContent()
            val a = ring.value
            if (a > 0f) {
                // The drawn card's own size: a caller may shrink the card (group rows use 162 × 216).
                val drawn = this.size
                val g = 2.dp.toPx()
                val r = radius.dp.toPx()
                val gap = GalleryCardRules.ringGap(drawn.width, drawn.height, g, r)
                val acc = GalleryCardRules.ringAcc(drawn.width, drawn.height, g, r)
                drawRoundRect(colors.bg, Offset(gap.left, gap.top), Size(gap.width, gap.height), CornerRadius(gap.corner), style = Stroke(g), alpha = a)
                drawRoundRect(colors.acc, Offset(acc.left, acc.top), Size(acc.width, acc.height), CornerRadius(acc.corner), style = Stroke(g), alpha = a)
            }
        }
    val more = onMenu?.let { IconAction(GalleryCardCopy.MENU, it) }
    if (shown != null) {
        // scr_gallery `gcard`: title 800 20 / 26 on a big card, 15 / 20 with meta 11 / 15 and 12 · 10 dp insets on a small one.
        val type = CivilightTheme.type
        ImageCard(
            title = shown, modifier = frame, meta = meta, statusChip = chip, more = more, onClick = onClick,
            titleStyle = type.title.merge(if (small) TextStyle(fontSize = 15.sp, lineHeight = 20.sp) else TextStyle(fontSize = 20.sp, lineHeight = 26.sp)),
            metaStyle = if (small) type.small.merge(TextStyle(fontSize = 11.sp, lineHeight = 15.sp)) else null,
            textPadding = if (small) PaddingValues(start = 12.dp, end = 12.dp, bottom = 10.dp) else null,
            image = image,
        )
        return
    }
    val shape = RoundedCornerShape(radius.dp)
    Box(
        frame
            .pressable(PressAction.Click(onClick), role = Role.Button, shape = shape)
            .semantics { contentDescription = GalleryCardCopy.UNTITLED }
            .clip(shape),
    ) {
        Box(Modifier.matchParentSize().clearAndSetSemantics { }, content = image)
        if (chip != null) ImageStatusChip(chip, Modifier.align(Alignment.TopStart).padding(12.dp))
        if (more != null) {
            CivilightIconButton(IconId.More, more.label, more.onClick, Modifier.align(Alignment.TopEnd), tint = FixedColors.onImage, iconSize = 18.dp)
        }
    }
}
