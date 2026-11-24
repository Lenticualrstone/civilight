package io.github.lenticualrstone.civilight.feature.cast.profile

import androidx.compose.foundation.Image
import io.github.lenticualrstone.civilight.feature.cast.image.CharacterImages
import io.github.lenticualrstone.civilight.designsystem.component.HoloCard
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.FadingHeaderArt
import io.github.lenticualrstone.civilight.feature.cast.image.SlotImage
import io.github.lenticualrstone.civilight.feature.cast.image.rememberSlotPainter

/** Cover art (`Profile`): 430 tall, the record text starts 300 down, over its fading foot. */
private val COVER_ART = 430.dp
private val COVER_TEXT_TOP = 300.dp

/**
 * The record's background art (CST-13; `Profile` 9:16 on the cover screen, 4:3 inside, CST-06):
 * full-bleed across the list's side padding, fading into the page. The item is only as tall as
 * the part above the name; the faded foot reaches under the head that follows.
 */
@Composable
fun RecordArt(image: SlotImage, inner: Boolean, sidePadding: Dp) {
    val painter = rememberSlotPainter(image) ?: return
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val full = maxWidth + sidePadding * 2
        val artHeight = if (inner) full * 3 / 4 else COVER_ART
        val visible = artHeight * (COVER_TEXT_TOP / COVER_ART)
        Box(
            Modifier
                .fillMaxWidth()
                .height(visible)
                .layout { measurable, constraints ->
                    val extra = (sidePadding * 2).roundToPx()
                    val width = constraints.maxWidth + extra
                    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width, minHeight = 0, maxHeight = artHeight.roundToPx()))
                    layout(constraints.maxWidth, visible.roundToPx()) { placeable.place(-extra / 2, 0) }
                },
        ) {
            FadingHeaderArt(height = artHeight, fadeStart = artHeight * (230f / 430f)) {
                Image(painter, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
    }
}


/** The holo board's card: 300 wide (narrower screens: the list's width), the portrait's 4:5, radius 22. */
private val HERO_W = 300.dp
private val HERO_RADIUS = 22.dp

/**
 * The record's hero when it has no background but a portrait (QA 0.2.1): the portrait in the holo board's card
 * (`gen_holo.py`), centred. With holo on it turns with the device (else a drag, else the 7 s idle sway) and
 * carries the foil, sheen and grain; holo off or reduced motion: a flat card.
 */
@Composable
fun PortraitHero(image: SlotImage) {
    val painter = rememberSlotPainter(image) ?: return
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp), contentAlignment = Alignment.Center) {
        val w = if (maxWidth < HERO_W) maxWidth else HERO_W
        HoloCard(on = image.holo, shape = RoundedCornerShape(HERO_RADIUS), modifier = Modifier.width(w).aspectRatio(CharacterImages.aspect(image.ratio))) {
            Image(painter, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}
