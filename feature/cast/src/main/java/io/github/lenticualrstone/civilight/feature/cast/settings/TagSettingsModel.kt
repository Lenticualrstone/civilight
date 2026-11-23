package io.github.lenticualrstone.civilight.feature.cast.settings

import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.TagUsage
import io.github.lenticualrstone.civilight.domain.library.hiding.ItemSurfaces
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc

/**
 * One project tag with its use: live characters and live gallery images that carry it, as the line
 * `인물 3 · 이미지 1` (55 §3.4; the character count is also what the card rows count, `주연 · 3`).
 */
data class TagRow(val key: Key, val name: String, val usage: TagUsage) {
    val count: Int get() = usage.characters

    /** `인물 3 · 이미지 1`, under the tag's name. */
    val line: String get() = usage.line
}

/**
 * Project tags (CST-12): names unique per project, in their order. Tags group the list (CST-01),
 * filter it (CST-02), make the card rows (CST-04) and the speaker picker's chips (CST-37). No source
 * defines deleting or reordering a tag, so the screen offers renaming and adding only.
 */
object TagSettingsModel {
    /**
     * Counts follow the list chips and the gallery: locked hidden characters and hidden images are not
     * counted (rule 19 §6). Unlocked, a row equals `CharacterEdits.tagUsage`.
     */
    fun rows(doc: ProjectDoc, hiddenUnlocked: Boolean = false): List<TagRow> {
        val live = doc.cast.characters.filter { it.deletedAt == null && (hiddenUnlocked || !it.hidden) }
        val surfaces = ItemSurfaces(doc)
        val images = doc.galleryItems.filter { surfaces.isGalleryItemVisible(it, locked = !hiddenUnlocked) }
        return CharacterEdits.tags(doc).map { tag ->
            TagRow(tag.key, tag.name, TagUsage(live.count { tag.key in it.tagKeys }, images.count { tag.key in it.tagKeys }))
        }
    }

    /** A new name is usable when it is not blank and no other tag has it (NFC). */
    fun canName(doc: ProjectDoc, name: String, except: Key? = null): Boolean {
        val n = name.trim().nfc()
        return n.isNotEmpty() && CharacterEdits.tags(doc).none { it.key != except && it.name.nfc() == n }
    }

    /**
     * Renames [key] through the one write path `CharacterEdits.renameTag`; null when the name is blank,
     * taken or unchanged. Characters and gallery images keep the tag (by key), so nothing else changes.
     */
    fun rename(doc: ProjectDoc, key: Key, name: String, ctx: EditContext): ProjectDoc? {
        val n = name.trim().nfc()
        val tag = CharacterEdits.liveTag(doc, key) ?: return null
        if (n.isEmpty() || tag.name == n || !canName(doc, n, except = key)) return null
        return CharacterEdits.renameTag(doc, key, n, ctx)
    }
}
