@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.file

import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.project.Rating
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** `manifest.json` (§7.3, LIB-50). */
@Serializable
data class FileManifest(
    @EncodeDefault val format: String = FORMAT_PROJECT,
    @EncodeDefault val formatVersion: String = FORMAT_VERSION,
    @EncodeDefault val minReaderVersion: String = FORMAT_VERSION,
    @EncodeDefault val features: ManifestFeatures = ManifestFeatures(),
    val writtenBy: String,
    val writtenAt: Timestamp,
    /** Absent in a `.civilight-recorder` manifest. */
    val project: ManifestProject? = null,
    /** Describes the package it sits in; an encrypted file's inner manifest says false. */
    @EncodeDefault val encrypted: Boolean = false,
    /** Path → `sha256-<hex>` for every entry except `mimetype` and `manifest.json`. */
    @EncodeDefault val files: Map<String, String> = emptyMap(),
    override val ext: JsonObject? = null,
) : HasExt {
    companion object {
        const val FORMAT_PROJECT = "civilight-project"
        const val FORMAT_RECORDER = "civilight-recorder"
        const val FORMAT_VERSION = "1.0"
    }
}

@Serializable
data class ManifestFeatures(
    @EncodeDefault val required: List<String> = Features.ALWAYS_REQUIRED,
    @EncodeDefault val optional: List<String> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt

@Serializable
data class ManifestProject(
    val key: Key,
    val title: String,
    val rating: OpenEnum<Rating>,
    override val ext: JsonObject? = null,
) : HasExt

/** Feature ids written by format 1.0 (§7.3). */
object Features {
    const val STORY_BLOCKS = "story.blocks"
    const val CHANGES = "changes"
    const val TIME_BASE_INTERVAL = "time.base-interval"
    const val FIELDS_CUSTOM = "fields.custom"
    const val MAPS_GEN = "maps.gen"
    const val CHECKS_ACK = "checks.ack"
    const val TOMBSTONES = "tombstones"
    const val DELETIONS = "deletions"
    const val TRASH = "trash"
    const val RECORDER = "recorder"
    const val RECORDER_AUDIO = "recorder.audio"

    /** `gallery/` (55 §3.3): present when any gallery file is written. Part of format 1.0. */
    const val GALLERY = "gallery"

    val ALWAYS_REQUIRED: List<String> = listOf(STORY_BLOCKS, CHANGES, TIME_BASE_INTERVAL)

    val OPTIONAL: List<String> = listOf(FIELDS_CUSTOM, MAPS_GEN, CHECKS_ACK, TOMBSTONES, DELETIONS, TRASH, RECORDER, RECORDER_AUDIO, GALLERY)

    /** Every id this reader understands; a required id outside this set refuses the file. */
    val KNOWN: Set<String> = (ALWAYS_REQUIRED + OPTIONAL).toSet()
}

/** `encryption.json` of an encrypted package (LIB-51 `ED`). Byte values are base64. */
@Serializable
data class EncryptionHeader(
    @EncodeDefault val format: String = FORMAT,
    val lockType: OpenEnum<PackageLockType>,
    val kdf: String,
    val iterations: Int,
    val salt: String,
    val cipher: String,
    val nonce: String,
    /** Recovery lock only. */
    val wrappedDataKey: String? = null,
    override val ext: JsonObject? = null,
) : HasExt {
    companion object {
        const val FORMAT = "civilight-encrypted"
    }
}

enum class PackageLockType { EXPORT, RECOVERY }
