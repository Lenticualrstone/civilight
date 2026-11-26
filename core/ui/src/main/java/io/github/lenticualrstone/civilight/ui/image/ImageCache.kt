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

    /** The file's EXIF orientation (1–8); [ExifOrientation.NORMAL] when unreadable or absent. */
    fun exifOrientation(file: File): Int = runCatching {
        ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifOrientation.NORMAL)

    /**
     * Size of the original as every decoder here draws it (EXIF orientation applied): the pixel space crop
     * rectangles refer to. Null when the header cannot be read.
     */
    fun uprightSize(file: File): Pair<Int, Int>? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        return ExifOrientation.uprightSize(bounds.outWidth, bounds.outHeight, exifOrientation(file))
    }

    private fun applyExifOrientation(file: File, bitmap: Bitmap): Bitmap {
        val turn = ExifOrientation.transform(exifOrientation(file))
        if (turn.isIdentity) return bitmap
        val matrix = Matrix().apply {
            if (turn.rotation != 0) postRotate(turn.rotation.toFloat())
            if (turn.mirror) postScale(-1f, 1f)
        }
        return runCatching { Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true) }
            .getOrDefault(bitmap)
    }
}

/** Turn that makes stored pixels upright: rotate clockwise by [rotation], then mirror left–right when [mirror]. */
data class UprightTurn(val rotation: Int, val mirror: Boolean) {
    val isIdentity: Boolean get() = rotation == 0 && !mirror
    val swapsSides: Boolean get() = rotation % 180 == 90
}

/** EXIF orientation tag (TIFF 274, values 1–8) as an [UprightTurn]; pure, so it is unit-tested on the JVM. */
object ExifOrientation {
    const val NORMAL = 1
    const val FLIP_HORIZONTAL = 2
    const val ROTATE_180 = 3
    const val FLIP_VERTICAL = 4
    const val TRANSPOSE = 5
    const val ROTATE_90 = 6
    const val TRANSVERSE = 7
    const val ROTATE_270 = 8

    fun transform(orientation: Int): UprightTurn = when (orientation) {
        FLIP_HORIZONTAL -> UprightTurn(0, mirror = true)
        ROTATE_180 -> UprightTurn(180, mirror = false)
        // A vertical flip is a half turn followed by a left–right mirror.
        FLIP_VERTICAL -> UprightTurn(180, mirror = true)
        TRANSPOSE -> UprightTurn(90, mirror = true)
        ROTATE_90 -> UprightTurn(90, mirror = false)
        TRANSVERSE -> UprightTurn(270, mirror = true)
        ROTATE_270 -> UprightTurn(270, mirror = false)
        else -> UprightTurn(0, mirror = false)
    }

    /** Stored [width]×[height] as drawn upright. */
    fun uprightSize(width: Int, height: Int, orientation: Int): Pair<Int, Int> =
        if (transform(orientation).swapsSides) height to width else width to height
}
