package io.github.lenticualrstone.civilight.feature.library.shelf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.HoloDefaultTilt
import io.github.lenticualrstone.civilight.designsystem.component.HoloStrength
import io.github.lenticualrstone.civilight.designsystem.component.ShardArt
import io.github.lenticualrstone.civilight.designsystem.component.holoFoil
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Decodes a cover or recent-card image off the main thread, sampled to about [targetWidth] px. */
internal object CoverBitmaps {
    fun decode(file: File, crop: Crop?, targetWidth: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val cropWidth = crop?.w ?: bounds.outWidth
        var sample = 1
        while (cropWidth / (sample * 2) >= targetWidth) sample *= 2
        val raw = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        if (crop == null) return raw
        val matrix = Matrix().apply {
            if (crop.rotation % 360 != 0) postRotate(crop.rotation.toFloat())
            if (crop.flipH) postScale(-1f, 1f)
        }
        val turned = if (matrix.isIdentity) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        val x = (crop.x / sample).coerceIn(0, turned.width - 1)
        val y = (crop.y / sample).coerceIn(0, turned.height - 1)
        val w = (crop.w / sample).coerceIn(1, turned.width - x)
        val h = (crop.h / sample).coerceIn(1, turned.height - y)
        return Bitmap.createBitmap(turned, x, y, w, h)
    }
}

/** A picked image from the blob store, or nothing while it decodes or when it is gone. */
@Composable
internal fun BlobImage(hash: String, crop: Crop?, targetWidth: Int, modifier: Modifier = Modifier) {
    val deps = LocalFeatureDeps.current
    val bitmap by produceState<ImageBitmap?>(null, hash, crop) {
        value = withContext(Dispatchers.IO) {
            deps.stores.blobs.fileOf(hash)?.let { runCatching { CoverBitmaps.decode(it, crop, targetWidth) }.getOrNull() }?.asImageBitmap()
        }
    }
    bitmap?.let { Image(it, contentDescription = null, modifier = modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
}

/**
 * A project card's cover (LIB-04): the picked image, a preset (abstract shard art; the 8 preset
 * pictures are the image area's, CNV-26), or the initial-letter card; the holo foil when on.
 */
@Composable
internal fun BoxScope.CoverArt(cover: CoverUi, targetWidth: Int) {
    val colors = CivilightTheme.colors
    val theme = if (colors.isDark) ResolvedTheme.DARK else ResolvedTheme.LIGHT
    val foil = if (cover.holo) Modifier.holoFoil(HoloDefaultTilt, theme, HoloStrength.CARD_FOIL) else Modifier
    Box(Modifier.matchParentSize().then(foil).background(colors.chip)) {
        when {
            cover.hash != null -> BlobImage(cover.hash, cover.crop, targetWidth)
            cover.presetId != null -> ShardArt(listOf(colors.raise, colors.chip, colors.edge, colors.acc))
            else -> CivilightText(
                cover.initial,
                CivilightTheme.type.title.merge(TextStyle(fontSize = 64.sp, lineHeight = 72.sp)),
                Modifier.align(Alignment.TopStart).padding(18.dp),
                color = FixedColors.onImage2,
            )
        }
    }
}
