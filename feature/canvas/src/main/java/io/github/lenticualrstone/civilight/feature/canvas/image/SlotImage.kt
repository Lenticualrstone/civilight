package io.github.lenticualrstone.civilight.feature.canvas.image

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import io.github.lenticualrstone.civilight.feature.canvas.card.ImagePlaceholder
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * What an image slot shows: the placed image (a picked original cut by the slot's crop for [ratio], or a
 * preset, CNV-26), with the holo foil when the use has it and its slot allows it (CNV-30), or the
 * initial-letter placeholder (CNV-24) when nothing is placed; [onPick] draws its `이미지 고르기` tag.
 * [imageModifier] goes on the image layer inside the frame's clip: a card row's parallax and constant 1.1
 * scale (`SnapCardRow`, CNV-22; read in the draw phase, none when reduced).
 */
@Composable
fun BoxScope.SlotImage(
    use: ImageUse?,
    ratio: Ratio,
    ownerName: String,
    onPick: (() -> Unit)?,
    imageModifier: Modifier = Modifier,
    compactPlaceholder: Boolean = false,
    contentDescription: String? = null,
) {
    val holo = Modifier.holoSurface(HoloRules.shows(use))
    when {
        use == null -> ImagePlaceholder(ownerName, onPick, compactPlaceholder)
        use.isPreset -> {
            val preset = PresetImages.byId(use.presetId)
            if (preset == null) ImagePlaceholder(ownerName, onPick, compactPlaceholder)
            else Box(Modifier.matchParentSize().then(holo)) { PresetArt(preset, imageModifier) }
        }
        else -> BlobImage(use.blobHash, io.github.lenticualrstone.civilight.ui.image.ImageCrops.cropFor(use, ratio), SLOT_MAX_SIDE, contentDescription, imageModifier, Modifier.then(holo)) {
            ImagePlaceholder(ownerName, onPick = null, compact = compactPlaceholder)
        }
    }
}

/** Decode size of slot images (cards and headers). */
private const val SLOT_MAX_SIDE = 720

/**
 * A stored original drawn with its [crop], decoded off the main thread at most [maxSide] px on its long side
 * (WLD-38: never a full decode of a large image); [placeholder] while decoding or when the blob is missing.
 * [layerModifier] wraps the image layer (the holo surface); [imageModifier] goes on the image itself (parallax,
 * shared element). Shared by slot images and the gallery.
 */
@Composable
fun BoxScope.BlobImage(
    hash: String?,
    crop: Crop?,
    maxSide: Int,
    contentDescription: String?,
    imageModifier: Modifier = Modifier,
    layerModifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    /** A fitted image (`보기`) gets these corners and its layer (the holo foil) on the image's own box. */
    fitCorner: androidx.compose.ui.unit.Dp? = null,
    placeholder: @Composable BoxScope.() -> Unit,
) {
    val deps = LocalFeatureDeps.current
    val cached = androidx.compose.runtime.remember(hash, crop, maxSide) {
        hash?.let { io.github.lenticualrstone.civilight.ui.image.ImageCache.get(io.github.lenticualrstone.civilight.ui.image.ImageCache.key(it, crop, maxSide)) }
    }
    val bitmap by produceState<ImageBitmap?>(cached, hash, crop, maxSide) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            val h = hash ?: return@withContext null
            val file = deps.stores.blobs.fileOf(h) ?: return@withContext null
            runCatching { io.github.lenticualrstone.civilight.ui.image.ImageBitmaps.getOrDecode(h, file, crop, maxSide) }.getOrNull()
        }
    }
    val shown = bitmap
    if (shown == null) placeholder()
    else if (fitCorner != null && shown.width > 0 && shown.height > 0) {
        Box(Modifier.matchParentSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Box(
                Modifier
                    .aspectRatio(shown.width.toFloat() / shown.height)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(fitCorner))
                    .then(layerModifier),
            ) {
                Image(shown, contentDescription = contentDescription, modifier = imageModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            }
        }
    } else Box(Modifier.matchParentSize().then(layerModifier)) {
        Image(shown, contentDescription = contentDescription, modifier = imageModifier.fillMaxSize(), contentScale = contentScale)
    }
}

/**
 * Decoding of a stored original with its crop (rotation, mirror, rectangle in rotated source pixels). The same
 * decoder as every display ([io.github.lenticualrstone.civilight.ui.image.ImageBitmaps.decodeUprightSampled]):
 * EXIF orientation is applied first, so the crop editor and the slots share one pixel space.
 */
object SlotImages {
    fun decode(file: File, crop: Crop?, maxSide: Int): Bitmap? =
        io.github.lenticualrstone.civilight.ui.image.ImageBitmaps.decodeUprightSampled(file, crop, maxSide)
}
