package io.github.lenticualrstone.civilight.feature.canvas.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Longest side of a picker tile's decoded thumbnail (px). */
private const val THUMB_SIDE = 360

/**
 * An original of `내 이미지` as a picker tile: decoded small, off the main thread, centre-cropped. [onBitmap] hands the
 * decoded thumbnail to the picker, which passes it to the crop editor.
 */
@Composable
fun BoxScope.MineThumb(hash: String, onBitmap: (ImageBitmap) -> Unit = {}) {
    val deps = LocalFeatureDeps.current
    val cached = androidx.compose.runtime.remember(hash) {
        io.github.lenticualrstone.civilight.ui.image.ImageCache.get(io.github.lenticualrstone.civilight.ui.image.ImageCache.key(hash, null, THUMB_SIDE))
    }
    val bitmap by produceState<ImageBitmap?>(cached, hash) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            deps.stores.blobs.fileOf(hash)?.let { file ->
                runCatching { io.github.lenticualrstone.civilight.ui.image.ImageBitmaps.getOrDecode(hash, file, null, THUMB_SIDE) }.getOrNull()
            }
        }
    }
    LaunchedEffect(bitmap) { bitmap?.let(onBitmap) }
    bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop) }
}
