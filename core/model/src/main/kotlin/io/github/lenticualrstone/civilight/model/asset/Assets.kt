@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.asset

import io.github.lenticualrstone.civilight.model.Entity
import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.serialName
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A content-addressed file (§2.10, `assets/index.json › blobs` + `assets/<hash>.<ext>`). Its JSON
 * `ext` is the file extension, so a blob has no §1.3 extension slot (unknown properties are dropped).
 */
@Serializable
data class Blob(
    /** Lowercase hex SHA-256 of the bytes. */
    val hash: String,
    @SerialName("ext") val fileExt: String,
    val mime: String,
    val bytes: Long,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    val source: OpenEnum<BlobSource>,
    val importedAt: Timestamp,
) {
    /** Path inside a package or the blob store: `<hash>.<ext>`. */
    val fileName: String get() = "$hash.$fileExt"
}

enum class BlobSource { GALLERY, FILE, SKETCH, GENERATED, RECORDER }

/** An image choice in a slot of an owner (§2.10); unique per (owner kind, owner, slot). */
@Serializable
data class ImageUse(
    override val key: Key,
    val ownerKind: OpenEnum<ImageOwnerKind>,
    val ownerKey: Key,
    val slot: OpenEnum<ImageSlot>,
    /** `preset:<id>` or a blob hash. */
    val source: String,
    /** Crop per ratio (ratio serial name such as `r3x4` → crop). */
    @EncodeDefault val crops: Map<String, Crop> = emptyMap(),
    @EncodeDefault val holo: Boolean = false,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity {
    val isPreset: Boolean get() = source.startsWith(PRESET_PREFIX)

    /** The preset id, or null for a blob. */
    val presetId: String? get() = if (isPreset) source.removePrefix(PRESET_PREFIX) else null

    /** The blob hash, or null for a preset. */
    val blobHash: String? get() = if (isPreset) null else source

    fun cropFor(ratio: Ratio): Crop? = crops[ratio.serialName]

    fun withCrop(ratio: Ratio, crop: Crop): ImageUse = copy(crops = crops + (ratio.serialName to crop))

    companion object {
        const val PRESET_PREFIX = "preset:"
    }
}

enum class ImageOwnerKind { PROJECT, CHARACTER, PLACE, TERM, FACTION, PAGE }

enum class ImageSlot { PROJECT_COVER, CHARACTER_PORTRAIT, CHARACTER_BACKGROUND, CARD_REFERENCE, PAGE_HEADER }

/** Fixed crop ratios (CNV-25). */
enum class Ratio { R3X4, R1X1, R4X5, R9X16, R4X3, R16X9, R21X9 }

/** A crop in source pixels after rotation. */
@Serializable
data class Crop(
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    /** 0, 90, 180 or 270. */
    @EncodeDefault val rotation: Int = 0,
    @EncodeDefault val flipH: Boolean = false,
    /** ≥ 1. */
    @EncodeDefault val zoom: Float = 1f,
    override val ext: JsonObject? = null,
) : HasExt
