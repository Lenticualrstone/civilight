package io.github.lenticualrstone.civilight.feature.cast.image

import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Longest side a decoded slot image keeps (the record art is the largest place). */
private const val MAX_SIDE_PX = 1440

/**
 * The painter of a slot image: a preset's shards, or the blob decoded off the main thread with the
 * place's crop applied (none = the whole image, filled by the caller's `ContentScale.Crop`). Null
 * while loading, without an image, or when the blob file is missing.
 */
@Composable
fun rememberSlotPainter(image: SlotImage?): Painter? {
    if (image == null) return null
    image.presetId?.let { id -> return remember(id) { PresetPainter(CharacterImages.presetIndex(id) ?: 0) } }
    val hash = image.blobHash ?: return null
    val blobs = LocalFeatureDeps.current.stores.blobs
    val painter by produceState<Painter?>(null, hash, image.crop) {
        value = withContext(Dispatchers.IO) {
            val file = blobs.fileOf(hash) ?: return@withContext null
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE_PX) sample *= 2
                val full = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return@runCatching null
                val crop = image.crop
                val shaped = if (crop == null) {
                    full
                } else {
                    val rotated = if (crop.rotation % 360 == 0 && !crop.flipH) {
                        full
                    } else {
                        val m = Matrix().apply {
                            postRotate(crop.rotation.toFloat())
                            if (crop.flipH) postScale(-1f, 1f)
                        }
                        android.graphics.Bitmap.createBitmap(full, 0, 0, full.width, full.height, m, true)
                    }
                    val x = (crop.x / sample).coerceIn(0, rotated.width - 1)
                    val y = (crop.y / sample).coerceIn(0, rotated.height - 1)
                    val w = (crop.w / sample).coerceIn(1, rotated.width - x)
                    val h = (crop.h / sample).coerceIn(1, rotated.height - y)
                    android.graphics.Bitmap.createBitmap(rotated, x, y, w, h)
                }
                BitmapPainter(shaped.asImageBitmap())
            }.getOrNull()
        }
    }
    return painter
}
