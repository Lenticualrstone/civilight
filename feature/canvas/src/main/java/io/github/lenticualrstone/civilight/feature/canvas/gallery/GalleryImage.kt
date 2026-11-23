package io.github.lenticualrstone.civilight.feature.canvas.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.github.lenticualrstone.civilight.feature.canvas.card.ImagePlaceholder
import io.github.lenticualrstone.civilight.feature.canvas.image.BlobImage
import io.github.lenticualrstone.civilight.feature.canvas.image.HoloRules
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetArt
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetImages
import io.github.lenticualrstone.civilight.feature.canvas.image.holoSurface
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem

/** Decode sizes (px, long side): list thumbnails and tiles stay small; cards as slot images; the viewer at screen size. */
object GalleryImageSizes {
    const val THUMB = 192
    const val TILE = 360
    const val CARD = 720
    const val VIEWER = 2048
}

/**
 * A gallery item's image (GAL-05 … GAL-09, GAL-17): the [ratio] crop (`카드 3:4`, `목록 1:1`; an item without a
 * stored crop shows the centred largest frame), or the whole original when [ratio] is null (`보기`, fitted).
 * A preset draws its own art; holo foil when the item has it on (GAL-17): switching holo on while the item is
 * shown plays the one 480 ms sweep (not on first showing, a restore, or another item taking this place).
 * [holoDragConsumes]: false (cards, tiles, rows: inside a scroller) only watches the finger for the no-gyro
 * tilt, so scrolling and tapping still work; true where the image owns one-finger drags (the viewer).
 */
@Composable
fun BoxScope.GalleryImage(
    item: GalleryItem,
    ratio: Ratio?,
    maxSide: Int,
    imageModifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    holoDragConsumes: Boolean = false,
) {
    // One holder per place (not per item): the trigger only rises, so an item swap here never fires the sweep.
    val edge = remember { HoloToggleEdge(item.key, item.holo) }
    val trigger = HoloRules.toggleTrigger(edge.wasOn, item.holo, edge.trigger, sameItem = edge.key == item.key)
    SideEffect {
        edge.key = item.key
        edge.wasOn = item.holo
        edge.trigger = trigger
    }
    val holo = Modifier.holoSurface(item.holo, sweepTrigger = trigger, consumeDrag = holoDragConsumes)
    val name = item.title
    val preset = item.presetId?.let(PresetImages::byId)
    when {
        item.isPreset && preset != null -> Box(Modifier.matchParentSize().then(holo)) { PresetArt(preset, imageModifier) }
        item.isPreset -> ImagePlaceholder(name, onPick = null, compact = true)
        else -> BlobImage(item.blobHash, ratio?.let(item::cropFor), maxSide, contentDescription, imageModifier, holo, contentScale) {
            ImagePlaceholder(name, onPick = null, compact = true)
        }
    }
}

/** The last shown item, its holo value and the sweep trigger of one image place (see [HoloRules.toggleTrigger]). */
private class HoloToggleEdge(var key: Any?, var wasOn: Boolean) {
    var trigger: Int = 0
}
