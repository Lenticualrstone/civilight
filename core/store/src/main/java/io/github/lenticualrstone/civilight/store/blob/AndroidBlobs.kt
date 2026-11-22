package io.github.lenticualrstone.civilight.store.blob

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import java.io.File
import java.io.FileNotFoundException

/** Imports the content behind [uri] (gallery, file picker) into the blob store. */
suspend fun BlobStore.import(resolver: ContentResolver, uri: Uri, source: BlobSource, durationMs: Long? = null): Blob {
    val mime = resolver.getType(uri) ?: "application/octet-stream"
    val name = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()
    return importFrom(
        { resolver.openInputStream(uri) ?: throw FileNotFoundException(uri.toString()) },
        mime, source, name, durationMs,
    )
}

/** Image size from the file header only (no pixels decoded). */
object BitmapImageSizeReader : ImageSizeReader {
    override fun sizeOf(file: File): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, options)
        return if (options.outWidth > 0 && options.outHeight > 0) options.outWidth to options.outHeight else null
    }
}
