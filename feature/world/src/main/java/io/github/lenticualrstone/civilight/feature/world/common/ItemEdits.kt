package io.github.lenticualrstone.civilight.feature.world.common

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.item.WorldItems
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.world.ItemRef
import io.github.lenticualrstone.civilight.model.world.ItemRefKind
import io.github.lenticualrstone.civilight.ui.search.KoreanSearch

/** Edits of a card's fixed fields (WLD-06, WLD-08 … WLD-11): name, description, `관련`. */
object ItemEdits {
    /** The current name and description of a live item, or null. */
    fun nameAndText(doc: ProjectDoc, kind: WorldKind, key: Key): Pair<String, RichDoc>? = when (kind) {
        WorldKind.TERM -> doc.index.term(key)?.let { it.name to it.description }
        WorldKind.PLACE -> doc.index.place(key)?.let { it.name to it.description }
        WorldKind.FACTION -> doc.index.faction(key)?.let { it.name to it.description }
    }

    /** `이름 바꾸기` (WLD-11): never rewrites manuscript text; null when the name is empty. */
    fun rename(doc: ProjectDoc, kind: WorldKind, key: Key, name: String, ctx: EditContext): ProjectDoc? = when (kind) {
        WorldKind.TERM -> TermEdits.rename(doc, key, name, ctx)
        else -> WorldItems.rename(doc, kind, key, name, ctx)
    }

    /** `설명 고치기`: the edited text, keeping links and paragraph keys ([RichTextEdit]). */
    fun setDescription(doc: ProjectDoc, kind: WorldKind, key: Key, text: String, ctx: EditContext): ProjectDoc =
        editDescription(doc, kind, key, ctx) { old -> RichTextEdit.apply(old, text, ctx.newKey) }

    /** `설명 고치기` with a whole new description (the sheet's draft with inserted `@` links). */
    fun setDescriptionDoc(doc: ProjectDoc, kind: WorldKind, key: Key, description: RichDoc, ctx: EditContext): ProjectDoc =
        editDescription(doc, kind, key, ctx) { old -> if (old == description) old else description }

    private fun editDescription(doc: ProjectDoc, kind: WorldKind, key: Key, ctx: EditContext, edit: (RichDoc) -> RichDoc): ProjectDoc {
        val now = ctx.now()
        val world = doc.world
        return when (kind) {
            WorldKind.TERM -> doc.copy(world = world.copy(terms = world.terms.map {
                if (it.key == key) it.copy(description = edit(it.description), updatedAt = now, updatedBy = ctx.device) else it
            }))
            WorldKind.PLACE -> doc.copy(world = world.copy(places = world.places.map {
                if (it.key == key) it.copy(description = edit(it.description), updatedAt = now, updatedBy = ctx.device) else it
            }))
            WorldKind.FACTION -> doc.copy(world = world.copy(factions = world.factions.map {
                if (it.key == key) it.copy(description = edit(it.description), updatedAt = now, updatedBy = ctx.device) else it
            }))
        }
    }

    /** Adds [ref] to the term's `관련` (at the end) or removes it. */
    fun toggleRelated(doc: ProjectDoc, termKey: Key, ref: ItemRef, ctx: EditContext): ProjectDoc {
        val now = ctx.now()
        return doc.copy(world = doc.world.copy(terms = doc.world.terms.map { term ->
            if (term.key != termKey) return@map term
            val has = term.related.any { it.key == ref.key }
            val related = if (has) term.related.filterNot { it.key == ref.key } else term.related + ref
            term.copy(related = related, updatedAt = now, updatedBy = ctx.device)
        }))
    }
}

/** One row of the `관련` sheet. */
data class RelatedOptionUi(val ref: ItemRef, val title: String, val checked: Boolean)

/** A titled group of the `관련` sheet (`용어 · 장소 · 세력`). */
data class RelatedSectionUi(val title: String, val options: List<RelatedOptionUi>)

/** The `관련` sheet (WLD-08 fixed field): terms, places and factions to tick, filtered by search (초성). */
data class RelatedSheetUi(val sections: List<RelatedSectionUi>, val hiddenCount: Int)

object RelatedOptions {
    fun build(doc: ProjectDoc, termKey: Key, query: String, hiddenOpen: Boolean): RelatedSheetUi {
        val related = doc.index.term(termKey)?.related.orEmpty().map { it.key }.toSet()
        var hidden = 0
        fun <T> section(title: String, kind: ItemRefKind, items: List<T>, key: (T) -> Key, name: (T) -> String, isHidden: (T) -> Boolean): RelatedSectionUi {
            val visible = items.filter { item ->
                val locked = isHidden(item) && !hiddenOpen
                if (locked) hidden++
                !locked && (query.isBlank() || KoreanSearch.matches(name(item), query))
            }
            return RelatedSectionUi(title, visible.map { RelatedOptionUi(ItemRef(kind, key(it)), name(it), key(it) in related) })
        }
        val terms = doc.world.terms.filter { it.deletedAt == null && it.key != termKey }.sortedWith(compareBy({ it.order }, { it.key }))
        val places = doc.world.places.filter { it.deletedAt == null }.sortedWith(compareBy({ it.order }, { it.key }))
        val factions = doc.world.factions.filter { it.deletedAt == null }.sortedWith(compareBy({ it.order }, { it.key }))
        val sections = listOf(
            section(WorldCopy.TAB_TERMS, ItemRefKind.TERM, terms, { it.key }, { it.name }, { it.hidden }),
            section(WorldCopy.TAB_PLACES, ItemRefKind.PLACE, places, { it.key }, { it.name }, { it.hidden }),
            section(WorldCopy.TAB_FACTIONS, ItemRefKind.FACTION, factions, { it.key }, { it.name }, { it.hidden }),
        ).filter { it.options.isNotEmpty() }
        return RelatedSheetUi(sections, hidden)
    }
}
