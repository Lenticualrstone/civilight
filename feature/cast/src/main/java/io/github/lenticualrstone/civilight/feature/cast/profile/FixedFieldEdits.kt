package io.github.lenticualrstone.civilight.feature.cast.profile

import io.github.lenticualrstone.civilight.domain.actions.TagActions
import io.github.lenticualrstone.civilight.feature.cast.common.toAction
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import kotlinx.serialization.Serializable

/** A tag picked in the `태그` sheet: a project tag ([key]) or a new name typed there (key null). */
@Serializable
data class TagPick(val key: Key?, val name: String)

/**
 * Editing the fixed fields that have no history (CST-07, CST-12): `이름`, `한 줄 소개` and the
 * character's ordered tags. Tags are project records; a name typed in the sheet becomes a new tag
 * on save (an existing name picks that tag, names are unique per project).
 */
object FixedFieldEdits {
    fun canRename(name: String, current: String): Boolean = name.isNotBlank() && name.trim().nfc() != current

    fun rename(doc: ProjectDoc, characterKey: Key, name: String, ctx: EditContext): ProjectDoc =
        if (name.isBlank()) doc else CharacterEdits.rename(doc, characterKey, name, ctx)

    fun setOneLiner(doc: ProjectDoc, characterKey: Key, text: String, ctx: EditContext): ProjectDoc =
        CharacterEdits.setOneLiner(doc, characterKey, text, ctx)

    /** The character's tags as picks, in its order. */
    fun picks(doc: ProjectDoc, characterKey: Key): List<TagPick> =
        doc.index.character(characterKey)?.tagKeys.orEmpty().mapNotNull { key -> CharacterEdits.liveTag(doc, key)?.let { TagPick(it.key, it.name) } }

    /** Tapping a project tag chip: on when off, off when on (a chip, never a guess). */
    fun toggle(picks: List<TagPick>, key: Key, name: String): List<TagPick> =
        if (picks.any { it.key == key }) picks.filterNot { it.key == key } else picks + TagPick(key, name)

    /** `넣기` under the name line: an existing tag of that name is picked, else a new name is queued. */
    fun addName(doc: ProjectDoc, picks: List<TagPick>, name: String): List<TagPick> {
        val n = name.trim().nfc()
        if (n.isEmpty()) return picks
        val existing = CharacterEdits.tags(doc).firstOrNull { it.name.nfc() == n }
        return when {
            existing != null -> if (picks.any { it.key == existing.key }) picks else picks + TagPick(existing.key, existing.name)
            picks.any { it.key == null && it.name == n } -> picks
            else -> picks + TagPick(null, n)
        }
    }

    /**
     * Saves the picks: new names become project tags (`TagActions.create`; an existing live name picks that
     * tag), then the character's tags are set in pick order. (0.2.0, N-44) Keys of tags in the trash stay on
     * the character, so restoring the tag re-attaches it.
     */
    fun apply(doc: ProjectDoc, characterKey: Key, picks: List<TagPick>, ctx: EditContext): ProjectDoc {
        var next = doc
        val keys = picks.mapNotNull { pick ->
            pick.key ?: CharacterEdits.tags(next).firstOrNull { it.name.nfc() == pick.name.trim().nfc() }?.key
                ?: TagActions.create(next, pick.name, null, ctx.toAction())?.let { (d, tag) ->
                    next = d
                    tag.key
                }
        }
        val trashed = doc.index.character(characterKey)?.tagKeys.orEmpty().filter { CharacterEdits.liveTag(doc, it) == null && doc.index.tag(it) != null }
        return CharacterEdits.setTags(next, characterKey, keys + trashed, ctx)
    }
}
