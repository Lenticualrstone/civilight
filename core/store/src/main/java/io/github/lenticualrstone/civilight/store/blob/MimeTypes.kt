package io.github.lenticualrstone.civilight.store.blob

/** File extensions of the media types the app stores. */
object MimeTypes {
    private val byMime = mapOf(
        "image/jpeg" to "jpg",
        "image/jpg" to "jpg",
        "image/png" to "png",
        "image/webp" to "webp",
        "image/gif" to "gif",
        "image/heic" to "heic",
        "image/heif" to "heif",
        "image/avif" to "avif",
        "image/bmp" to "bmp",
        "image/svg+xml" to "svg",
        "audio/mp4" to "m4a",
        "audio/m4a" to "m4a",
        "audio/x-m4a" to "m4a",
        "audio/aac" to "aac",
        "audio/mpeg" to "mp3",
        "audio/ogg" to "ogg",
        "audio/opus" to "opus",
        "audio/wav" to "wav",
        "audio/x-wav" to "wav",
        "audio/flac" to "flac",
        "audio/webm" to "weba",
        "application/pdf" to "pdf",
        "application/json" to "json",
        "text/plain" to "txt",
    )

    /** The extension for [mime], else the one of [fileName], else `bin`. Lower case, letters and digits only. */
    fun extensionFor(mime: String, fileName: String? = null): String {
        byMime[mime.substringBefore(';').trim().lowercase()]?.let { return it }
        val fromName = fileName?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return if (fromName.isNotEmpty() && fromName.length <= 8 && fromName.all { it.isLetterOrDigit() && it.code < 128 }) fromName else "bin"
    }

    /** The media type of an extension, or `application/octet-stream`. */
    fun mimeFor(ext: String): String =
        byMime.entries.firstOrNull { it.value == ext.lowercase() }?.key ?: "application/octet-stream"
}
