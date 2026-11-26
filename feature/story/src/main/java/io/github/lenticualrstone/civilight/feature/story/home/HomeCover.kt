package io.github.lenticualrstone.civilight.feature.story.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.image.CoverPreset
import io.github.lenticualrstone.civilight.ui.image.CoverUi
import io.github.lenticualrstone.civilight.ui.image.ImageBitmaps
import io.github.lenticualrstone.civilight.ui.image.ImageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The hero of the immersive 프로젝트 홈 (`Immersive.dc.html` kind `project`, LIB-83): the project's 3:4 cover
 * filling the screen, cropped with top alignment. A preset draws its real art; with no cover (or a missing
 * original) a plain `tint` field, never a stand-in picture. No holo foil here: the live foil would redraw the
 * blurred full-screen hero every frame (the card and the 서재 keep it).
 * Decorative (the name is the title); the cover's edit control is the head button ([HomeCopy] cover labels).
 */
@Composable
internal fun HomeHero(cover: CoverUi, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    BoxWithConstraints(modifier.fillMaxSize().clearAndSetSemantics { }.background(colors.tint), contentAlignment = Alignment.TopCenter) {
        // A 3:4 picture fills the box: its width is the larger of the box width and ¾ of its height.
        val targetWidth = maxOf(constraints.maxWidth, constraints.maxHeight * 3 / 4)
        val artW = maxOf(maxWidth, maxHeight * 3f / 4f)
        val hash = cover.hash
        val presetId = cover.presetId
        when {
            hash != null -> HeroBlob(hash, cover.crop, targetWidth)
            // The preset art draws to its box: give it a 3:4 box, top-aligned; the frame clips the overflow.
            presetId != null -> CoverPreset(presetId, Modifier.wrapContentSize(Alignment.TopCenter, unbounded = true).size(artW, artW * 4f / 3f))
        }
    }
}

/** A picked original at hero size: decoded off the main thread through the shared [ImageCache]; fades in. */
@Composable
private fun HeroBlob(hash: String, crop: Crop?, targetWidth: Int) {
    val deps = LocalFeatureDeps.current
    val width = targetWidth.coerceAtLeast(1)
    val cached = remember(hash, crop, width) { ImageCache.get(ImageCache.key(hash, crop, width)) }
    val bitmap by produceState(cached, hash, crop, width) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            deps.stores.blobs.fileOf(hash)?.let { runCatching { ImageBitmaps.getOrDecode(hash, it, crop, width) }.getOrNull() }
        }
    }
    ContentSwap(bitmap, Modifier.fillMaxSize(), label = "homeHero") { image ->
        if (image != null) {
            Image(image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
        } else {
            Box(Modifier.fillMaxSize())
        }
    }
}
