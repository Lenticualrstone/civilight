package io.github.lenticualrstone.civilight.feature.library.export

import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import io.github.lenticualrstone.civilight.domain.library.hiding.ItemSurfaces
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * The export sheet's gallery row (55 §6 Q13 default, GAL-21, GAL-22): `갤러리 이미지 <n> · <MB>`, so a
 * `.civilight` that carries 100+ originals is no surprise. `.civilight` only: PDF, CSV and engine JSON carry
 * no gallery content (GAL-22, §6 Q7).
 *
 * [images] counts the live items whose original is in the package (presets live in the app, so they add
 * nothing); [bytes] is the sum of those distinct blobs, the same sum as `GalleryActions.galleryBytes` but
 * over the items [locked] lets show. While hidden images are locked they are left out of both numbers, as
 * every other count is (GAL-19), so the row never tells how many are hidden; the file itself still holds
 * them. No stored image: no row.
 */
data class ExportGalleryLine(val images: Int, val bytes: Long) {
    val label: String get() = "$LABEL $images"
    val value: String get() = TimeLabels.size(bytes)

    /** The drawn text, `갤러리 이미지 12 · 148 MB`. */
    val text: String get() = label + ExportCopy.SEP + value

    companion object {
        const val LABEL = "갤러리 이미지"

        fun of(doc: ProjectDoc, locked: Boolean): ExportGalleryLine? {
            val surfaces = ItemSurfaces(doc)
            val hashes = doc.galleryItems.filter { surfaces.isGalleryItemVisible(it, locked) }.mapNotNull { it.blobHash }
            if (hashes.isEmpty()) return null
            return ExportGalleryLine(hashes.size, hashes.toSet().sumOf { doc.index.blob(it)?.bytes ?: 0L })
        }
    }
}
