package io.github.lenticualrstone.civilight.feature.canvas.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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

/** An original of `내 이미지` as a picker tile: decoded small, off the main thread, centre-cropped. */
@Composable
fun BoxScope.MineThumb(hash: String) {
    val deps = LocalFeatureDeps.current
    val bitmap by produceState<ImageBitmap?>(null, hash) {
        value = withContext(Dispatchers.IO) {
            deps.stores.blobs.fileOf(hash)?.let { runCatching { SlotImages.decode(it, null, THUMB_SIDE) }.getOrNull() }?.asImageBitmap()
        }
    }
    bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop) }
}
