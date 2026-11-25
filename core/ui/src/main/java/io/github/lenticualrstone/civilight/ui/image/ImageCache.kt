package io.github.lenticualrstone.civilight.ui.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.lenticualrstone.civilight.model.asset.Crop
import java.io.File

/**
 * Shared in-memory LRU cache for decoded UI images (covers, character slots, thumbnails).
 * Sized to 1/8 of the app's max heap so scrolling lists never re-decode bitmaps from disk
 * on every re-entry while staying safely below OOM limits.
 */
object ImageCache {
    private val maxKb: Int = ((Runtime.getRuntime().maxMemory() / 1024L) / 8L)
        .toInt()
        .coerceIn(8 * 1024, 64 * 1024)

    private val cache = object : LruCache<String, ImageBitmap>(maxKb) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            // 4 bytes per ARGB_8888 pixel -> KB.
            val kb = (value.width.toLong() * value.height.toLong() * 4L / 1024L).toInt()
            return kb.coerceAtLeast(1)
        }
    }

    fun key(hash: String, crop: Crop?, maxSidePx: Int): String =
        if (crop == null) "$hash@$maxSidePx" else "$hash@${crop.x},${crop.y},${crop.w},${crop.h},${crop.rotation},${crop.flipH}@$maxSidePx"

    fun get(key: String): ImageBitmap? = cache.get(key)

    fun put(key: String, bitmap: ImageBitmap) {
        cache.put(key, bitmap)
    }

    fun clear() {
        cache.evictAll()
    }
}

/**
 * Shared EXIF-aware bitmap decoder with safe sampling (guards against zero target size)
 * and deterministic recycling of intermediate bitmaps.
 */
object ImageBitmaps {
    fun decodeUprightSampled(file: File, crop: Crop?, targetSidePx: Int): Bitmap? {
        val safeTarget = targetSidePx.coerceAtLeast(1)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val refWidth = if (crop != null && crop.w > 0) crop.w else maxOf(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (refWidth / (sample * 2) >= safeTarget && sample < (1 shl 20)) sample *= 2

        val raw = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val upright = applyExifOrientation(file, raw)
        if (upright !== raw) raw.recycle()

        if (crop == null) return upright

        val matrix = Matrix().apply {
            if (crop.rotation % 360 != 0) postRotate(crop.rotation.toFloat())
            if (crop.flipH) postScale(-1f, 1f)
        }
        val turned = if (matrix.isIdentity) {
            upright
        } else {
            Bitmap.createBitmap(upright, 0, 0, upright.width, upright.height, matrix, true).also {
                if (it !== upright) upright.recycle()
            }
        }
        val x = (crop.x / sample).coerceIn(0, (turned.width - 1).coerceAtLeast(0))
        val y = (crop.y / sample).coerceIn(0, (turned.height - 1).coerceAtLeast(0))
        val w = (crop.w / sample).coerceIn(1, (turned.width - x).coerceAtLeast(1))
        val h = (crop.h / sample).coerceIn(1, (turned.height - y).coerceAtLeast(1))
        val cropped = Bitmap.createBitmap(turned, x, y, w, h)
        if (cropped !== turned) turned.recycle()
        return cropped
    }

    fun getOrDecode(hash: String, file: File, crop: Crop?, targetSidePx: Int): ImageBitmap? {
        val cacheKey = ImageCache.key(hash, crop, targetSidePx)
        ImageCache.get(cacheKey)?.let { return it }
        val decoded = decodeUprightSampled(file, crop, targetSidePx)?.asImageBitmap() ?: return null
        ImageCache.put(cacheKey, decoded)
        return decoded
    }

    private fun applyExifOrientation(file: File, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return runCatching { Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true) }
            .getOrDefault(bitmap)
    }
}
