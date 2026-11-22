@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.gallery

import io.github.lenticualrstone.civilight.model.Entity
import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.Ordered
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.Trashable
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.serialName
import io.github.lenticualrstone.civilight.model.text.RichDoc
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Who owns a gallery (55 §3.1): the project itself or one character. */
enum class GalleryOwnerKind { PROJECT, CHARACTER }

/**
 * One gallery image (55 §3.1, domain model §12.1): an ordered image of a character or of the
 * project, in the file unit `gallery/<ownerKey>.json`. It references a blob (or a preset) like
 * [ImageUse] does, but many per owner. `초상` · `배경` · `표지` are derived from the owner's
 * [ImageUse] with the same [source]; nothing about them is stored here.
 *
 * Liveness (§12.2): not directly marked and its owner live. Uniqueness: at most one item per
 * (owner kind, owner, source) among live and trashed items, so a restore never duplicates.
 */
@Serializable
data class GalleryItem(
    override val key: Key,
    val ownerKind: OpenEnum<GalleryOwnerKind>,
    /** The project key or a character key. */
    val ownerKey: Key,
    /** `preset:<id>` or a blob hash. */
    val source: String,
    /** `직접 순서` within the owner (§1.2). */
    override val order: OrderKey,
    /** `""` = untitled (`제목 없음`). */
    @EncodeDefault val title: String = "",
    /** The description, with `@` link runs; null = none (`설명 없음`). */
    val caption: RichDoc? = null,
    /** Project [io.github.lenticualrstone.civilight.model.cast.Tag] keys. */
    @EncodeDefault val tagKeys: List<Key> = emptyList(),
    /** A linked scene (label and 때 are computed on read). */
    val sceneKey: Key? = null,
    /** Crop per gallery ratio (`r3x4` card, `r1x1` list); missing = centred largest frame. */
    @EncodeDefault val crops: Map<String, Crop> = emptyMap(),
    @EncodeDefault val holo: Boolean = false,
    /** Item hiding (GAL-19). */
    @EncodeDefault val hidden: Boolean = false,
    @EncodeDefault val createdAt: Timestamp = 0L,
    override val deletedAt: Timestamp? = null,
    override val trashEntryKey: Key? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Ordered, Trashable {
    val isPreset: Boolean get() = source.startsWith(ImageUse.PRESET_PREFIX)

    /** The preset id, or null for a blob. */
    val presetId: String? get() = if (isPreset) source.removePrefix(ImageUse.PRESET_PREFIX) else null

    /** The blob hash, or null for a preset. */
    val blobHash: String? get() = if (isPreset || source.isEmpty()) null else source

    fun cropFor(ratio: Ratio): Crop? = crops[ratio.serialName]

    fun withCrop(ratio: Ratio, crop: Crop): GalleryItem = copy(crops = crops + (ratio.serialName to crop))

    /** True for the owner [kind] and [key]. */
    fun isOwnedBy(kind: GalleryOwnerKind, key: Key): Boolean = ownerKind.known == kind && ownerKey == key

    companion object {
        /** The ratios a gallery item may hold a crop for (`SlotSpec GALLERY`). */
        val RATIOS: List<Ratio> = listOf(Ratio.R3X4, Ratio.R1X1)
    }
}

/**
 * `gallery/<ownerKey>.json` (and `trash/gallery/<ownerKey>.json` for trashed items): the items of one
 * owner. [items] are kept in stored order; read them through [ordered].
 */
@Serializable
data class GalleryFile(
    val ownerKind: OpenEnum<GalleryOwnerKind>,
    val ownerKey: Key,
    @EncodeDefault val items: List<GalleryItem> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt {
    /** Items in `직접 순서` (order key, then item key). */
    val ordered: List<GalleryItem> get() = items.sortedWith(compareBy<GalleryItem> { it.order }.thenBy { it.key })

    companion object {
        /**
         * [gallery] with the project gallery filed under [newProjectKey] (its file key, `ownerKey` and
         * the items' `ownerKey`); for every project re-key (§6). Character galleries are unchanged.
         */
        fun rekeyProject(gallery: Map<Key, GalleryFile>, oldProjectKey: Key, newProjectKey: Key): Map<Key, GalleryFile> {
            if (oldProjectKey == newProjectKey || gallery.isEmpty()) return gallery
            val out = LinkedHashMap<Key, GalleryFile>()
            for ((ownerKey, file) in gallery) {
                if (ownerKey != oldProjectKey && file.ownerKey != oldProjectKey) {
                    out[ownerKey] = file
                    continue
                }
                out[newProjectKey] = file.copy(
                    ownerKey = newProjectKey,
                    items = file.items.map { if (it.ownerKey == oldProjectKey) it.copy(ownerKey = newProjectKey) else it },
                )
            }
            return out
        }
    }
}

/**
 * Per-gallery screen UI state kept across fold, navigation and process death (CNV-35, GAL-08,
 * §12.4); app-only (`_app/view.json`), never exported. Keyed in [io.github.lenticualrstone.civilight.model.app.ProjectViewFile.galleries]
 * by the owner key (the project key for the project gallery).
 */
@Serializable
data class GalleryViewState(
    /** The picked item (`고른 카드`); null = nothing picked. */
    val pickedKey: Key? = null,
    /** `더 보기` expanded on the panel. */
    val panelExpanded: Boolean = false,
    /** First visible card or row index of the card row or list. */
    val scrollIndex: Int = 0,
    /** Pixel offset inside [scrollIndex]. */
    val scrollOffset: Int = 0,
)
