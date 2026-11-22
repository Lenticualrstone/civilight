package io.github.lenticualrstone.civilight.domain.story.search

import io.github.lenticualrstone.civilight.domain.story.text.Sentences
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.text.docText

/** Which part of a gallery image matched (GAL-04 search label). */
enum class GalleryMatchField(val word: String) { TITLE("제목"), DESCRIPTION("설명"), TAG("태그") }

/**
 * One gallery image found by the command-bar search (55 §3.5, GAL-04): [match] is the title, the whole
 * description sentence or the tag name with the hits boxed; [label] reads `<owner> · 갤러리 · <title> ·
 * 설명` (or `제목`, `태그`). Opening it shows the gallery with the item picked (`Gallery(…, pickKey)`).
 */
data class GalleryHit(
    val itemKey: Key,
    val ownerKind: GalleryOwnerKind,
    val ownerKey: Key,
    /** `preset:<id>` or a blob hash, for the 44 thumbnail. */
    val source: String,
    val field: GalleryMatchField,
    val match: Highlight,
    val label: String,
)

/**
 * The gallery part of the search index: titles, descriptions and tag names of visible gallery images. Left
 * out: trashed images, images of a trashed owner, and while locked hidden images and images of hidden
 * characters (GAL-19). Project gallery first, then characters in cast order; each gallery in its stored
 * order. One hit per image (title, else description, else tag).
 */
class GallerySearch(private val doc: ProjectDoc, private val locked: Boolean) {
    fun search(q: TextQuery): List<GalleryHit> {
        if (q.isEmpty || doc.gallery.isEmpty()) return emptyList()
        val tagNames = doc.project.tags.associate { it.key to it.name }
        val owners = buildList {
            add(GalleryOwnerKind.PROJECT to doc.project.key)
            doc.cast.characters
                .filter { it.deletedAt == null && !(locked && it.hidden) }
                .sortedWith(compareBy({ it.order }, { it.key }))
                .forEach { add(GalleryOwnerKind.CHARACTER to it.key) }
        }
        val out = mutableListOf<GalleryHit>()
        for ((kind, ownerKey) in owners) {
            val ownerName = ownerName(kind, ownerKey) ?: continue
            for (item in doc.index.galleryItemsOf(ownerKey)) {
                if (!item.isOwnedBy(kind, ownerKey) || !doc.liveness.isLive(item) || (locked && item.hidden)) continue
                hit(q, item, kind, ownerKey, ownerName, tagNames)?.let(out::add)
            }
        }
        return out
    }

    private fun ownerName(kind: GalleryOwnerKind, key: Key): String? = when (kind) {
        GalleryOwnerKind.PROJECT -> doc.project.name
        GalleryOwnerKind.CHARACTER -> doc.index.character(key)?.name
    }

    private fun hit(
        q: TextQuery,
        item: GalleryItem,
        kind: GalleryOwnerKind,
        ownerKey: Key,
        ownerName: String,
        tagNames: Map<Key, String>,
    ): GalleryHit? {
        val title = item.title.takeIf { it.isNotBlank() }
        fun of(field: GalleryMatchField, match: Highlight) = GalleryHit(
            itemKey = item.key, ownerKind = kind, ownerKey = ownerKey, source = item.source, field = field, match = match,
            label = listOf(ownerName, GALLERY, title ?: UNTITLED, field.word).joinToString(SEP),
        )
        title?.let { q.highlight(it) }?.let { return of(GalleryMatchField.TITLE, it) }
        val text = item.caption?.docText.orEmpty()
        val hits = q.hits(text)
        if (hits.isNotEmpty()) {
            val sentence = Sentences.around(text, hits.first().first)
            val inside = hits.filter { it.first >= sentence.first && it.last <= sentence.last }
                .map { (it.first - sentence.first)..(it.last - sentence.first) }
            return of(GalleryMatchField.DESCRIPTION, Highlight(text.substring(sentence).trim(), inside.shiftedByTrim(text.substring(sentence))))
        }
        for (tagKey in item.tagKeys) {
            val name = tagNames[tagKey] ?: continue
            q.highlight(name)?.let { return of(GalleryMatchField.TAG, it) }
        }
        return null
    }

    /** Hit ranges of [raw] moved to its trimmed text. */
    private fun List<IntRange>.shiftedByTrim(raw: String): List<IntRange> {
        val lead = raw.length - raw.trimStart().length
        return map { (it.first - lead)..(it.last - lead) }
    }

    companion object {
        const val GALLERY = "갤러리"
        const val UNTITLED = "제목 없음"
        private const val SEP = " · "
    }
}
