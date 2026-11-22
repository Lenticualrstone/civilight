package io.github.lenticualrstone.civilight.domain.story.checks

import java.security.MessageDigest

/**
 * Fingerprints of check inputs (STO-41): `sha256-<hex>` of the parts joined with `|`. A different
 * fingerprint voids a stored `맞음`.
 */
object Fingerprints {
    fun of(vararg parts: Any?): String {
        val text = parts.joinToString("|") { it?.toString() ?: "" }
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return "sha256-" + digest.joinToString("") { "%02x".format(it) }
    }
}
