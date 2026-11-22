package io.github.lenticualrstone.civilight.feature.canvas.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import io.github.lenticualrstone.civilight.feature.canvas.card.ImagePlaceholder
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * What an image slot shows: the placed image (a picked original cut by the slot's crop for [ratio], or a
 * preset, CNV-26), with the holo foil when the use has it and its slot allows it (CNV-30), or the
 * initial-letter placeholder (CNV-24) when nothing is placed; [onPick] draws its `이미지 고르기` tag.
 * [shiftDp] moves the image inside its frame (card parallax, CNV-22).
 */
@Composable
fun BoxScope.SlotImage(
    use: ImageUse?,
    ratio: Ratio,
    ownerName: String,
    onPick: (() -> Unit)?,
    shiftDp: Float = 0f,
    compactPlaceholder: Boolean = false,
    contentDescription: String? = null,
) {
    val shift = Modifier.graphicsLayer {
        translationX = shiftDp * density
        if (shiftDp != 0f) {
            scaleX = 1.1f
            scaleY = 1.1f
        }
    }
    val holo = Modifier.holoSurface(HoloRules.shows(use))
    when {
        use == null -> ImagePlaceholder(ownerName, onPick, compactPlaceholder)
        use.isPreset -> {
            val preset = PresetImages.byId(use.presetId)
            if (preset == null) ImagePlaceholder(ownerName, onPick, compactPlaceholder)
            else Box(Modifier.matchParentSize().then(holo)) { PresetArt(preset, shift) }
        }
        else -> {
            val deps = LocalFeatureDeps.current
            val hash = use.blobHash
            val crop = use.cropFor(ratio)
            val bitmap by produceState<ImageBitmap?>(null, hash, crop) {
                value = withContext(Dispatchers.IO) {
                    hash?.let { deps.stores.blobs.fileOf(it) }?.let { runCatching { SlotImages.decode(it, crop, 720) }.getOrNull() }?.asImageBitmap()
                }
            }
            val shown = bitmap
            if (shown == null) ImagePlaceholder(ownerName, onPick = null, compact = compactPlaceholder)
            else Box(Modifier.matchParentSize().then(holo)) {
                Image(shown, contentDescription = contentDescription, modifier = shift.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
    }
}

/** Decoding of a stored original with its crop (rotation, mirror, rectangle in rotated source pixels). */
object SlotImages {
    fun decode(file: File, crop: Crop?, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null
        val wanted = if (crop != null) maxSide * longest / max(1, max(crop.w, crop.h)) else maxSide
        var sample = 1
        while (longest / (sample * 2) >= wanted) sample *= 2
        val source = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        if (crop == null) return source
        val matrix = Matrix().apply {
            if (crop.rotation % 360 != 0) postRotate(crop.rotation.toFloat())
            if (crop.flipH) postScale(-1f, 1f)
        }
        val turned = if (matrix.isIdentity) source else Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        val x = (crop.x / sample).coerceIn(0, turned.width - 1)
        val y = (crop.y / sample).coerceIn(0, turned.height - 1)
        val w = (crop.w / sample).coerceIn(1, turned.width - x)
        val h = (crop.h / sample).coerceIn(1, turned.height - y)
        return Bitmap.createBitmap(turned, x, y, w, h)
    }
}
