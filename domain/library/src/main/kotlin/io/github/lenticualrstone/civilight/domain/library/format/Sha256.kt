package io.github.lenticualrstone.civilight.domain.library.format

import java.security.MessageDigest

/** SHA-256 helpers for manifest hashes (`sha256-<hex>`), blob names and voice text hashes. */
object Sha256 {
    const val PREFIX = "sha256-"

    /** Lowercase hex digest. */
    fun hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    /** `sha256-<hex>`, the form of `manifest.files` values and `VoiceExport.textHash`. */
    fun label(bytes: ByteArray): String = PREFIX + hex(bytes)

    /** `sha256-<hex>` of UTF-8 text (CSV `바뀜` input, LIB-44). */
    fun ofText(text: String): String = label(text.toByteArray(Charsets.UTF_8))
}
