package io.github.lenticualrstone.civilight.feature.canvas.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.github.lenticualrstone.civilight.feature.canvas.card.ImagePlaceholder
import io.github.lenticualrstone.civilight.feature.canvas.image.BlobImage
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
 * A preset draws its own art; holo foil when the item has it on.
 */
@Composable
fun BoxScope.GalleryImage(
    item: GalleryItem,
    ratio: Ratio?,
    maxSide: Int,
    imageModifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
) {
    val holo = Modifier.holoSurface(item.holo)
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
