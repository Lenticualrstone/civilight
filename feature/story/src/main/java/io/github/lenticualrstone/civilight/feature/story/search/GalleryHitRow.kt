package io.github.lenticualrstone.civilight.feature.story.search

import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.SearchHit
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.story.search.GalleryHit
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.gallery.PresetPicture
import io.github.lenticualrstone.civilight.ui.gallery.ThumbSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A gallery result (GAL-04, board `GalSearch`): the 44 × 59 thumbnail, then the label `<owner> · 갤러리 ·
 * <title> · 설명` and the whole sentence (or title, or tag) with the match boxed (DS `SearchHit`).
 */
@Composable
internal fun GalleryHitRow(hit: GalleryHit, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    Row(
        modifier.fillMaxWidth().topHairline(colors.line).pressable(onClick = onClick, scaleOnPress = false),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        GalleryThumb(hit.source, Modifier.padding(vertical = 12.dp))
        SearchHit(
            location = hit.label,
            sentence = hit.match.text,
            matches = hit.match.hits,
            modifier = Modifier.weight(1f),
            divider = false,
        )
    }
}

/**
 * The thumbnail: a bundled preset is drawn at once (same art as the gallery, CNV-26); a blob is decoded small
 * off the main thread and faded in on `standard`; a missing blob or an unknown preset keeps the `chip` fill.
 */
@Composable
private fun GalleryThumb(source: String, modifier: Modifier = Modifier) {
    val deps = LocalFeatureDeps.current
    val motion = CivilightTheme.motion
    val target = with(LocalDensity.current) { THUMB_HEIGHT.dp.roundToPx() }
    val thumb = remember(source) { ThumbSource.of(source) }
    val bitmap by produceState<ImageBitmap?>(null, thumb, target) {
        val blob = thumb as? ThumbSource.Blob
        value = if (blob == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching { deps.stores.blobs.fileOf(blob.hash)?.let { decode(it.path, target) } }.getOrNull()
            }
        }
    }
    Box(modifier.size(THUMB_WIDTH.dp, THUMB_HEIGHT.dp).clip(RoundedCornerShape(10.dp)).background(CivilightTheme.colors.chip)) {
        if (thumb is ThumbSource.Preset) {
            PresetPicture(thumb.index)
        } else {
            Crossfade(bitmap, Modifier.fillMaxSize(), animationSpec = motion.spec(CivilightMotion.standard), label = "galleryThumb") { image ->
                if (image != null) Image(image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
    }
}

/** Decodes [path] with the largest power-of-two sample size that keeps the short side at least [target] px. */
private fun decode(path: String, target: Int): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    val short = minOf(bounds.outWidth, bounds.outHeight)
    if (short <= 0) return null
    var sample = 1
    while (short / (sample * 2) >= target) sample *= 2
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}

private const val THUMB_WIDTH = 44
private const val THUMB_HEIGHT = 59
