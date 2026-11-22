package io.github.lenticualrstone.civilight.feature.map.sketch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import io.github.lenticualrstone.civilight.feature.map.canvas.TilePlan
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.store.blob.BlobStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/** The module's own FileProvider (a subclass, so its manifest entry cannot clash with another module's). */
class MapSketchFileProvider : FileProvider()

/**
 * Photos for `스케치 촬영` (WLD-39): a private cache file the system camera writes into (shared only
 * through [MapSketchFileProvider], never exported), its import into the blob store, and the
 * perspective straightening of `펴기`.
 */
object SketchImages {
    private const val DIR = "map_sketch"

    fun authority(context: Context): String = "${context.packageName}.map.sketch"

    /** A new empty cache file and the content URI the camera app may write to. */
    fun newCaptureTarget(context: Context): Pair<File, Uri> {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        val file = File(dir, "sketch-${System.currentTimeMillis()}.jpg")
        return file to FileProvider.getUriForFile(context, authority(context), file)
    }

    /** Imports the captured photo and deletes the cache file. */
    suspend fun importCapture(blobs: BlobStore, file: File): Blob? = try {
        if (file.length() > 0) blobs.import(file, "image/jpeg", BlobSource.SKETCH) else null
    } finally {
        withContext(Dispatchers.IO) { file.delete() }
    }

    /** Decodes [file] upright (EXIF rotation applied), subsampled so the long edge fits [maxEdge]. */
    fun decodeUpright(file: File, maxEdge: Int = TilePlan.FULL_DECODE_MAX): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val sample = TilePlan.sampleToFit(maxOf(bounds.outWidth, bounds.outHeight), maxEdge)
        val bmp = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val degrees = runCatching {
            when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        if (degrees == 0f) return bmp
        val m = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    /** Maps the quadrilateral [corners] (0–1, TL TR BR BL) of [photo] onto a rectangle. */
    fun straighten(photo: Bitmap, corners: List<Point>): Bitmap {
        val (w, h) = Straighten.outputSize(corners, photo.width, photo.height)
        val src = corners.flatMap { listOf(it.x * photo.width, it.y * photo.height) }.toFloatArray()
        val dst = floatArrayOf(0f, 0f, w.toFloat(), 0f, w.toFloat(), h.toFloat(), 0f, h.toFloat())
        val matrix = Matrix().apply { setPolyToPoly(src, 0, dst, 0, 4) }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(photo, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        return out
    }

    /** `펴기`: straightens [photo] and stores the result as a new blob. */
    suspend fun straightenToBlob(blobs: BlobStore, photo: Blob, corners: List<Point>): SketchResult? = withContext(Dispatchers.Default) {
        val file = blobs.fileOf(photo) ?: return@withContext null
        val bitmap = decodeUpright(file) ?: return@withContext null
        val out = straighten(bitmap, corners)
        val bytes = ByteArrayOutputStream().use { stream ->
            out.compress(Bitmap.CompressFormat.JPEG, 92, stream)
            stream.toByteArray()
        }
        val blob = blobs.import(ByteArrayInputStream(bytes), "image/jpeg", BlobSource.SKETCH, fileName = "sketch.jpg")
        SketchResult(photo, blob.copy(width = blob.width ?: out.width, height = blob.height ?: out.height), corners)
    }
}
