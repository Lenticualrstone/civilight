package io.github.lenticualrstone.civilight.feature.canvas.image

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.feature.canvas.page.PageCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.serialName

/** The place an image goes: an owner's slot (unique per owner kind, owner and slot). */
data class ImageOwner(val kind: ImageOwnerKind, val key: Key, val slot: ImageSlot) {
    fun matches(use: ImageUse): Boolean =
        use.ownerKind.raw == kind.serialName && use.ownerKey == key && use.slot.raw == slot.serialName
}

/**
 * Image writes (CNV-26 … CNV-30): originals are added once by content hash, each owner × slot holds one
 * [ImageUse] (`preset:<id>` or a hash, one crop per ratio, holo). Pure; one call = one undo step.
 */
object ImageEdits {
    /** The use in [owner]'s slot, or null. */
    fun use(doc: ProjectDoc, owner: ImageOwner): ImageUse? = doc.assets.imageUses.firstOrNull(owner::matches)

    /** Adds the original's row unless one with its hash is already there (stored once, T-CNV-35). */
    fun addBlob(doc: ProjectDoc, blob: Blob): ProjectDoc =
        if (doc.assets.blobs.any { it.hash == blob.hash }) doc
        else doc.copy(assets = doc.assets.copy(blobs = doc.assets.blobs + blob))

    /** Sets [owner]'s slot to the preset [presetId] (stored by name, never as a file, T-CNV-36). */
    fun placePreset(doc: ProjectDoc, owner: ImageOwner, presetId: String, holo: Boolean, ctx: EditContext): ProjectDoc =
        put(doc, owner, ImageUse.PRESET_PREFIX + presetId, emptyMap(), holo, ctx)

    /**
     * Sets [owner]'s slot to the original [hash] with [crops]. Re-cropping the same original keeps the
     * crops of the other ratios (T-CNV-33); a different original starts without them.
     */
    fun placeBlob(doc: ProjectDoc, owner: ImageOwner, hash: String, crops: Map<Ratio, Crop>, holo: Boolean, ctx: EditContext): ProjectDoc {
        val old = use(doc, owner)
        val kept = if (old?.blobHash == hash) old.crops else emptyMap()
        return put(doc, owner, hash, kept + crops.mapKeys { it.key.serialName }, holo, ctx)
    }

    /**
     * Empties [owner]'s slot: its [ImageUse] row goes, the original stays in `assets.blobs` until the blob
     * sweep finds nothing referencing it (U-08 LIB-87 `빼기`; 00 §5.3). One undo step; [ctx] is unused but
     * kept so every slot write has the same shape. No row: [doc] unchanged.
     */
    @Suppress("UNUSED_PARAMETER")
    fun clear(doc: ProjectDoc, owner: ImageOwner, ctx: EditContext): ProjectDoc {
        if (doc.assets.imageUses.none(owner::matches)) return doc
        return doc.copy(assets = doc.assets.copy(imageUses = doc.assets.imageUses.filterNot(owner::matches)))
    }

    /** The holo flag of the slot's image; always false where holo is not allowed (T-CNV-34). */
    fun setHolo(doc: ProjectDoc, owner: ImageOwner, holo: Boolean, ctx: EditContext): ProjectDoc {
        val old = use(doc, owner) ?: return doc
        return put(doc, owner, old.source, old.crops, holo, ctx)
    }

    private fun put(doc: ProjectDoc, owner: ImageOwner, source: String, crops: Map<String, Crop>, holo: Boolean, ctx: EditContext): ProjectDoc {
        val old = use(doc, owner)
        val use = ImageUse(
            key = old?.key ?: ctx.newKey(),
            ownerKind = owner.kind.open(),
            ownerKey = owner.key,
            slot = owner.slot.open(),
            source = source,
            crops = crops,
            holo = holo && ImageSlots.holoAllowed(owner.slot),
            updatedAt = ctx.now,
            updatedBy = ctx.device,
            ext = old?.ext,
        )
        val others = doc.assets.imageUses.filterNot(owner::matches)
        val at = doc.assets.imageUses.indexOfFirst(owner::matches)
        val uses = if (at < 0) others + use else others.toMutableList().apply { add(at, use) }
        return doc.copy(assets = doc.assets.copy(imageUses = uses))
    }
}

/** Readings for the picker: owner names and `내 이미지`. */
object ImageOwners {
    /** The owner's name for the sheet subtitle and the crop header. */
    fun name(doc: ProjectDoc, kind: ImageOwnerKind, key: Key): String = when (kind) {
        ImageOwnerKind.PROJECT -> doc.project.name
        ImageOwnerKind.CHARACTER -> doc.index.character(key)?.name
        ImageOwnerKind.PLACE -> doc.index.place(key)?.name
        ImageOwnerKind.TERM -> doc.index.term(key)?.name
        ImageOwnerKind.FACTION -> doc.index.faction(key)?.name
        ImageOwnerKind.PAGE -> doc.pages[key]?.name?.takeIf { it.isNotBlank() }
    } ?: PageCopy.UNNAMED

    /**
     * `내 이미지` (Q-IM-03 default: this project's imported images): image originals picked from the
     * gallery or files, newest first. Map bases, sketches and recorder audio are not listed.
     */
    fun myImages(doc: ProjectDoc): List<Blob> = doc.assets.blobs
        .filter { it.mime.startsWith("image/") && it.source.known.let { s -> s == BlobSource.GALLERY || s == BlobSource.FILE } }
        .sortedWith(compareByDescending<Blob> { it.importedAt }.thenBy { it.hash })
}
