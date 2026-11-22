package io.github.lenticualrstone.civilight.model.doc

/**
 * The one blob reference function of a project (§2.10, §5.3, 55 §3.4, T-GAL-27). Every row that
 * points at a blob counts, live and trashed alike, because a blob goes only when no live or trashed
 * row references it: image uses, script assets, map bases and sketch photos, and gallery items.
 * The `assets.blobs` index rows are metadata, not references.
 *
 * `core:store` `BlobReferences.counts`, `domain:trash` `PurgeOps.referencedBlobs` and
 * `domain:actions` `PurgeActions.heldBlobs` all delegate here; a new row kind that holds a blob is
 * added here and nowhere else.
 */
object BlobRefs {
    /** Reference count per blob hash in [doc]. */
    fun counts(doc: ProjectDoc): Map<String, Int> {
        val out = HashMap<String, Int>()
        fun add(hash: String?) {
            if (!hash.isNullOrEmpty()) out[hash] = (out[hash] ?: 0) + 1
        }
        doc.assets.imageUses.forEach { add(it.blobHash) }
        doc.assets.scriptAssets.forEach { add(it.fileHash) }
        doc.maps.values.forEach { map ->
            add(map.baseImageHash)
            add(map.sketch?.photoHash)
        }
        doc.galleryItems.forEach { add(it.blobHash) }
        return out
    }

    /** Blob hashes [doc] references. */
    fun of(doc: ProjectDoc): Set<String> = counts(doc).keys
}
