package io.github.lenticualrstone.civilight.feature.cast.profile

import io.github.lenticualrstone.civilight.model.cast.TagScope
import io.github.lenticualrstone.civilight.model.cast.TagScopes
import io.github.lenticualrstone.civilight.model.cast.fits
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

    /** (2026-11-26) `이 태그 쓰기` of the same-name line: the existing tag is checked (kept in place when already on). */
    fun useExisting(picks: List<TagPick>, key: Key, name: String): List<TagPick> =
        if (picks.any { it.key == key }) picks else picks + TagPick(key, name)

    /**
     * A tag created from the sheet's `＋ 새 태그` card: that write already checked it on the character, so it
     * joins both the picks and the baseline `저장` compares against (untapping it then enables `저장`).
     */
    fun created(picks: List<TagPick>, key: Key, name: String): List<TagPick> =
        if (picks.any { it.key == key }) picks else picks + TagPick(key, name)

    /**
     * `넣기` under the name line: an existing tag of that name is picked, else a new name is queued.
     * (2026-11-26) Names are unique across scopes: a name held by a tag a character may not carry (`용어 전용`)
     * adds nothing ([takenElsewhere] says so for the line).
     */
    fun addName(doc: ProjectDoc, picks: List<TagPick>, name: String): List<TagPick> {
        val n = name.trim().nfc()
        if (n.isEmpty()) return picks
        val existing = TagScopes.liveNamed(doc, n)
        return when {
            existing != null && !existing.fits(TagScope.CHARACTER) -> picks
            existing != null -> if (picks.any { it.key == existing.key }) picks else picks + TagPick(existing.key, existing.name)
            picks.any { it.key == null && it.name == n } -> picks
            else -> picks + TagPick(null, n)
        }
    }

    /** (2026-11-26) The live tag of [name] when a character may not carry it (`같은 이름 태그 · 광물 · 용어`), else null. */
    fun takenElsewhere(doc: ProjectDoc, name: String): io.github.lenticualrstone.civilight.model.cast.Tag? =
        TagScopes.liveNamed(doc, name)?.takeUnless { it.fits(TagScope.CHARACTER) }

    /**
     * Saves the picks: new names become project tags (`TagActions.create`; an existing live name picks that
     * tag), then the character's tags are set in pick order. (0.2.0, N-44) Keys of tags in the trash stay on
     * the character, so restoring the tag re-attaches it.
     */
    fun apply(doc: ProjectDoc, characterKey: Key, picks: List<TagPick>, ctx: EditContext): ProjectDoc {
        var next = doc
        val keys = picks.mapNotNull { pick ->
            pick.key ?: TagScopes.liveNamed(next, pick.name)?.let { if (it.fits(TagScope.CHARACTER)) it.key else return@mapNotNull null }
                ?: TagActions.create(next, pick.name, null, ctx.toAction())?.let { (d, tag) ->
                    next = d
                    tag.key
                }
        }
        // Held keys no edit takes off: trashed tags' and (2026-11-26) unknown-scope tags' (newer file).
        val trashed = doc.index.character(characterKey)?.tagKeys.orEmpty().filter { k -> doc.index.tag(k)?.let(TagScopes::keepsHeld) == true }
        return CharacterEdits.setTags(next, characterKey, keys + trashed, ctx)
    }
}
