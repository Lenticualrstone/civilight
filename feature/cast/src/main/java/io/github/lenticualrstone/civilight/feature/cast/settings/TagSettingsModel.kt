package io.github.lenticualrstone.civilight.feature.cast.settings

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc

/** One project tag with the number of live characters that carry it (`주연 · 3`, as the card rows count). */
data class TagRow(val key: Key, val name: String, val count: Int) {
    val title: String get() = CastCopy.join(name, count.toString())
}

/**
 * Project tags (CST-12): names unique per project, in their order. Tags group the list (CST-01),
 * filter it (CST-02), make the card rows (CST-04) and the speaker picker's chips (CST-37). No source
 * defines deleting or reordering a tag, so the screen offers renaming and adding only.
 */
object TagSettingsModel {
    /** Counts follow the list chips: locked hidden characters are not counted (rule 19 §6). */
    fun rows(doc: ProjectDoc, hiddenUnlocked: Boolean = false): List<TagRow> {
        val live = doc.cast.characters.filter { it.deletedAt == null && (hiddenUnlocked || !it.hidden) }
        return CharacterEdits.tags(doc).map { tag -> TagRow(tag.key, tag.name, live.count { tag.key in it.tagKeys }) }
    }

    /** A new name is usable when it is not blank and no other tag has it (NFC). */
    fun canName(doc: ProjectDoc, name: String, except: Key? = null): Boolean {
        val n = name.trim().nfc()
        return n.isNotEmpty() && doc.project.tags.none { it.key != except && it.name.nfc() == n }
    }

    /** Renames [key]; null when the name is not usable or unchanged. Characters keep the tag (by key). */
    fun rename(doc: ProjectDoc, key: Key, name: String, ctx: EditContext): ProjectDoc? {
        val n = name.trim().nfc()
        val tag = doc.project.tags.firstOrNull { it.key == key } ?: return null
        if (tag.name == n || !canName(doc, n, except = key)) return null
        val tags = doc.project.tags.map { if (it.key == key) it.copy(name = n, updatedAt = ctx.now, updatedBy = ctx.device) else it }
        return doc.copy(project = doc.project.copy(tags = tags))
    }

    /** `넣기`: a new tag at the end; null when the name is not usable. */
    fun add(doc: ProjectDoc, name: String, ctx: EditContext): ProjectDoc? =
        if (!canName(doc, name)) null else CharacterEdits.addTag(doc, name, ctx).first
}
