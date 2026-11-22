package io.github.lenticualrstone.civilight.store.blob

import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary

/**
 * Which blobs rows refer to (§2.10, §5.3). A document holds live and trashed rows together, so
 * both count: a blob goes only when no live or trashed row references it. The `assets.blobs`
 * index rows are metadata, not references.
 */
object BlobReferences {
    /** Hashes referenced by one project: image uses, script assets, map bases and sketch photos. */
    fun of(doc: ProjectDoc): Set<String> = counts(doc).keys

    /** Hashes of kept recorder audio. */
    fun of(library: RecorderLibrary): Set<String> = library.records.mapNotNullTo(HashSet()) { it.audio?.hash }

    /** Reference count per hash in one project. */
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
        return out
    }

    /** Reference count per hash over several projects and the recorder library. */
    fun counts(docs: Iterable<ProjectDoc>, recorder: RecorderLibrary? = null): Map<String, Int> {
        val out = HashMap<String, Int>()
        for (doc in docs) for ((hash, n) in counts(doc)) out[hash] = (out[hash] ?: 0) + n
        recorder?.records?.forEach { r -> r.audio?.hash?.let { out[it] = (out[it] ?: 0) + 1 } }
        return out
    }
}
