package io.github.lenticualrstone.civilight.domain.world.item

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.term.AliasEdit
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.domain.world.term.TermText
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind

/**
 * (0.2.0, U-15, WLD-70) What the create card of a place, term or faction holds. Only [name] is required.
 * [alias] is the term card's `다른 이름` (one optional alias, never split); [parentPlaceKey] is the place
 * card's `상위 지역`; [description] is the `한 줄 설명`.
 */
data class NewWorldItem(
    val name: String,
    val description: String = "",
    val alias: String = "",
    val parentPlaceKey: Key? = null,
)

/** (0.2.0, U-15) Creating a world item in place: one edit, one undo step (WLD-70). */
object WorldCreate {
    /** `만들기` and `만들고 열기` are on only while the name is non-blank after trim and NFC. */
    fun canCreate(name: String): Boolean = TermText.clean(name).isNotEmpty()

    /**
     * Creates a [kind] item from [item]. A term's [NewWorldItem.alias] is validated as in WLD-16; an alias
     * equal to the name (or blank) adds nothing. A parent that is not a live place of the project is
     * ignored. Same names are allowed (WLD-11).
     */
    fun create(doc: ProjectDoc, kind: WorldKind, item: NewWorldItem, ctx: EditContext): CreateResult {
        val parent = item.parentPlaceKey?.takeIf { key -> doc.index.place(key)?.deletedAt == null && doc.index.place(key) != null }
        val made = when (kind) {
            WorldKind.TERM -> WorldItems.createTerm(doc, item.name, ctx, item.description)
            WorldKind.PLACE -> WorldItems.createPlace(doc, item.name, ctx, item.description, parent)
            WorldKind.FACTION -> WorldItems.createFaction(doc, item.name, ctx, item.description)
        }
        if (made !is CreateResult.Created || kind != WorldKind.TERM || item.alias.isBlank()) return made
        return when (val aliased = TermEdits.setAliases(made.doc, made.key, listOf(item.alias), ctx)) {
            is AliasEdit.Applied -> CreateResult.Created(aliased.doc, made.key)
            is AliasEdit.Invalid -> made
        }
    }

    /**
     * U-15.3, WLD-52: a place made from a picker's create row and returned as the pick. Null when the name
     * is blank.
     */
    fun createPlaceAndPick(doc: ProjectDoc, item: NewWorldItem, ctx: EditContext): Pair<ProjectDoc, PlaceRef>? =
        when (val r = create(doc, WorldKind.PLACE, item, ctx)) {
            is CreateResult.Created -> r.doc to PlaceRef(PlaceRefKind.PLACE, r.key)
            CreateResult.EmptyName -> null
        }

    /**
     * The picker create row's state (U-15.3): [query] fills the name; [matched] false (nothing matches the
     * query) turns the row into `‘<query>’ 새 <kind>`.
     */
    fun placePickerMatched(doc: ProjectDoc, query: String, hiddenUnlocked: Boolean = false): Boolean {
        val q = TermText.clean(query)
        if (q.isEmpty()) return true
        return PlacePicker(doc, hiddenUnlocked).rows(q, regions = false).isNotEmpty()
    }
}
