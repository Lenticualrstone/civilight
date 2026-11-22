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

    /** A title worth drawing; a blank title is an untitled card (no shade, no text). */
    fun shownTitle(title: String?): String? = title?.takeIf { it.isNotBlank() }
}

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
                val g = 2.dp.toPx()
                val r = radius.dp.toPx()
                drawRoundRect(colors.bg, Offset(-g / 2, -g / 2), Size(size.width.dp.toPx() + g, size.height.dp.toPx() + g), CornerRadius(r + g / 2), style = Stroke(g), alpha = a)
                drawRoundRect(colors.acc, Offset(-g * 1.5f, -g * 1.5f), Size(size.width.dp.toPx() + g * 3, size.height.dp.toPx() + g * 3), CornerRadius(r + g * 1.5f), style = Stroke(g), alpha = a)
            }
        }
    val more = onMenu?.let { IconAction(GalleryCardCopy.MENU, it) }
    if (shown != null) {
        ImageCard(title = shown, modifier = frame, meta = meta, statusChip = chip, more = more, onClick = onClick, image = image)
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
