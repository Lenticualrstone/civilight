package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.world.item.CreateResult
import io.github.lenticualrstone.civilight.domain.world.item.NewWorldItem
import io.github.lenticualrstone.civilight.domain.world.item.WorldCreate
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * (0.2.0, U-15.3) Create in place from a picker: one write per kind, used by every area's picker sheets
 * (speaker, `@` link, scene `등장`, scene place, relation partner …). The item is made as the list create
 * cards make it (CST-66 `CharacterEdits.create`, WLD-70 `WorldCreate.create`); the caller picks the key.
 */
object CreateActions {
    /** The kinds a picker can create; the names equal `core:designsystem` `CreateKind`'s. */
    enum class Kind { CHARACTER, PLACE, TERM, FACTION }

    /**
     * One write for [kind] ([kindName] = a `CreateKind` name); null for a blank name or a kind pickers do not
     * create. [note] is `한 줄 소개` (character) or `한 줄 설명`; [displayName] and [tagKeys] are character fields
     * (trashed tags are dropped by `CharacterEdits.create`); [alias] is a term's `다른 이름`; [parentKey] a
     * place's `상위 지역` (ignored unless it is a live place).
     */
    fun inPicker(
        doc: ProjectDoc,
        kindName: String,
        name: String,
        ctx: ActionContext,
        displayName: String? = null,
        note: String? = null,
        alias: String? = null,
        tagKeys: List<Key> = emptyList(),
        parentKey: Key? = null,
    ): Pair<ProjectDoc, Key>? {
        val kind = Kind.entries.firstOrNull { it.name == kindName } ?: return null
        if (name.isBlank()) return null
        return when (kind) {
            Kind.CHARACTER -> CharacterEdits.create(doc, name, ctx.edit, oneLiner = note, displayNameBase = displayName, tagKeys = tagKeys)
                .let { (next, c) -> next to c.key }
            Kind.PLACE, Kind.TERM, Kind.FACTION -> {
                val worldKind = WorldKind.valueOf(kind.name)
                val item = NewWorldItem(
                    name = name,
                    description = note.orEmpty(),
                    alias = if (kind == Kind.TERM) alias.orEmpty() else "",
                    parentPlaceKey = if (kind == Kind.PLACE) parentKey else null,
                )
                when (val r = WorldCreate.create(doc, worldKind, item, ctx.world)) {
                    is CreateResult.Created -> r.doc to r.key
                    else -> null
                }
            }
        }
    }
}
