package io.github.lenticualrstone.civilight.feature.cast.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.image.ImageBitmaps
import io.github.lenticualrstone.civilight.ui.image.ImageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Longest side a decoded slot image keeps (1080 px balances crisp record art with memory). */
private const val MAX_SIDE_PX = 1080

/**
 * The painter of a slot image: a preset's shards, or the blob decoded off the main thread with EXIF
 * orientation, crop and shared [ImageCache] applied. Null while loading, without an image, or when missing.
 */
@Composable
fun rememberSlotPainter(image: SlotImage?): Painter? {
    if (image == null) return null
    image.presetId?.let { id -> return remember(id) { PresetPainter(CharacterImages.presetIndex(id) ?: 0) } }
    val hash = image.blobHash ?: return null
    val blobs = LocalFeatureDeps.current.stores.blobs
    val cached = remember(hash, image.crop) {
        ImageCache.get(ImageCache.key(hash, image.crop, MAX_SIDE_PX))?.let(::BitmapPainter)
    }
    val painter by produceState<Painter?>(cached, hash, image.crop) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            val file = blobs.fileOf(hash) ?: return@withContext null
            runCatching { ImageBitmaps.getOrDecode(hash, file, image.crop, MAX_SIDE_PX)?.let(::BitmapPainter) }.getOrNull()
        }
    }
    return painter
}
