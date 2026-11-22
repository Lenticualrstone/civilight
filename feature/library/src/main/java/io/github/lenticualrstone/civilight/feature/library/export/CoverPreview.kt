package io.github.lenticualrstone.civilight.feature.library.export

import android.graphics.Bitmap
import android.os.Build
import io.github.lenticualrstone.civilight.feature.library.shelf.CoverBitmaps
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.store.blob.BlobStore
import java.io.ByteArrayOutputStream

/** The picture a `.civilight`'s `preview.webp` is made from: the cover blob and its 3:4 crop. */
data class CoverSource(val blobHash: String, val crop: Crop?)

/**
 * `preview.webp` (LIB-47, LIB-49: the library card cover, absent for hidden projects). The rule is pure;
 * the pixels come from a [PreviewRenderer].
 */
object CoverPreview {
    /** Card width of the preview in pixels (3:4, so 360 × 480). */
    const val WIDTH = 360

    /**
     * The project's own cover picture (`PROJECT_COVER`, 3:4 crop), or null: a hidden project, no
     * cover, or a built-in preset (the reader draws presets itself, so no file is needed).
     */
    fun source(doc: ProjectDoc): CoverSource? {
        if (doc.project.protections.hidden) return null
        val use = doc.index.imageUse(ImageOwnerKind.PROJECT, doc.key, ImageSlot.PROJECT_COVER) ?: return null
        val hash = use.blobHash ?: return null
        return CoverSource(hash, use.cropFor(Ratio.R3X4))
    }
}

/** Makes the `preview.webp` bytes of a project, or null when it has none. */
fun interface PreviewRenderer {
    fun render(doc: ProjectDoc): ByteArray?
}

/** [PreviewRenderer] over the blob store: decode, crop, scale to [CoverPreview.WIDTH], WebP. */
internal class AndroidCoverPreview(private val blobs: BlobStore) : PreviewRenderer {
    override fun render(doc: ProjectDoc): ByteArray? {
        val source = CoverPreview.source(doc) ?: return null
        val file = blobs.fileOf(source.blobHash) ?: return null
        val bitmap = runCatching { CoverBitmaps.decode(file, source.crop, CoverPreview.WIDTH) }.getOrNull() ?: return null
        val height = (bitmap.height.toLong() * CoverPreview.WIDTH / bitmap.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
        val scaled = if (bitmap.width > CoverPreview.WIDTH) Bitmap.createScaledBitmap(bitmap, CoverPreview.WIDTH, height, true) else bitmap
        val out = ByteArrayOutputStream()
        @Suppress("DEPRECATION")
        val format = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        return if (scaled.compress(format, QUALITY, out)) out.toByteArray() else null
    }

    private companion object {
        const val QUALITY = 80
    }
}
