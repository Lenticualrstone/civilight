package io.github.lenticualrstone.civilight.feature.script.assets

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** One asset row (board `Assets` `.li`): 56 × 40 thumbnail, name and subline, the first episode or `비어 있음`. */
@Composable
fun AssetRowView(row: AssetRow, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    ListRow(
        title = row.name,
        subtitle = row.subline,
        leading = { AssetThumbnail(row.thumb) },
        trailing = row.trailing?.let { t ->
            { CivilightText(t, if (row.trailingWarn) type.small else type.number, color = if (row.trailingWarn) colors.warn else colors.ink3, maxLines = 1) }
        },
        onClick = onClick,
    )
}

/** The 56 × 40 thumbnail: the image (cropped to fill), a tinted box with the image mark, or the waveform. */
@Composable
fun AssetThumbnail(thumb: AssetThumb) {
    val colors = CivilightTheme.colors
    Box(
        Modifier.size(56.dp, 40.dp).clip(RoundedCornerShape(8.dp)).background(colors.tint),
        contentAlignment = Alignment.Center,
    ) {
        when (thumb) {
            AssetThumb.Audio -> CivilightIcon(WaveBars, null, size = 18.dp, tint = colors.ink2)
            is AssetThumb.Image -> {
                val bitmap = thumb.hash?.let { rememberThumbnail(it) }
                if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else CivilightIcon(IconId.Image, null, size = 18.dp, tint = colors.ink3)
            }
        }
    }
}

/** Decodes a blob image at thumbnail size off the main thread; null while decoding or when gone. */
@Composable
private fun rememberThumbnail(hash: String): ImageBitmap? {
    val deps = LocalFeatureDeps.current
    val bitmap by produceState<ImageBitmap?>(null, hash) {
        value = withContext(Dispatchers.IO) { deps.stores.blobs.fileOf(hash)?.let(::decodeSmall) }
    }
    return bitmap
}

private fun decodeSmall(file: File): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= THUMB_PX && bounds.outHeight / (sample * 2) >= THUMB_PX) sample *= 2
    BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}.getOrNull()

private const val THUMB_PX = 168

/**
 * The audio mark of board `Assets` (five bars, `M4 12h2M8 8v8M12 5v14M16 9v6M20 11v2`), stroke 1.6
 * with round caps like DS-12. Local until the DS has it (Requests).
 */
private val WaveBars: ImageVector = ImageVector.Builder("assetWaveBars", 24.dp, 24.dp, 24f, 24f).apply {
    addPath(
        pathData = addPathNodes("M4 12h2M8 8v8M12 5v14M16 9v6M20 11v2"),
        stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
        strokeLineWidth = 1.6f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
}.build()
