package io.github.lenticualrstone.civilight.domain.world.item

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.term.TermText
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.world.Faction
import io.github.lenticualrstone.civilight.model.world.Place
import io.github.lenticualrstone.civilight.model.world.Term

/** Kinds of world items (tabs `장소 · 용어 · 세력`). */
enum class WorldKind(val label: String) { PLACE("장소"), TERM("용어"), FACTION("세력") }

/** Result of creating a world item. */
sealed interface CreateResult {
    data class Created(val doc: ProjectDoc, val key: Key) : CreateResult

    /** The name is empty after trimming. */
    data object EmptyName : CreateResult
}

/**
 * Creating, renaming and hiding terms, places and factions (WLD-11, WLD-13). Names are NFC and
 * trimmed; equal names are allowed (the merge rule shows `◇ 같은 이름 2`).
 */
object WorldItems {
    private fun description(ctx: EditContext, text: String): RichDoc =
        if (text.isBlank()) emptyList() else listOf(DocBlock(ctx.newKey(), OrderKeys.FIRST, runs = listOf(TextRun.Plain(TermText.clean(text)))))

    fun createTerm(doc: ProjectDoc, name: String, ctx: EditContext, descriptionText: String = ""): CreateResult {
        val clean = TermText.clean(name)
        if (clean.isEmpty()) return CreateResult.EmptyName
        val term = Term(
            key = ctx.newKey(), order = OrderKeys.after(doc.world.terms.maxOfOrNull { it.order }), name = clean,
            description = description(ctx, descriptionText), updatedAt = ctx.now(), updatedBy = ctx.device,
        )
        return CreateResult.Created(doc.copy(world = doc.world.copy(terms = doc.world.terms + term)), term.key)
    }

    fun createPlace(doc: ProjectDoc, name: String, ctx: EditContext, descriptionText: String = "", parentPlaceKey: Key? = null): CreateResult {
        val clean = TermText.clean(name)
        if (clean.isEmpty()) return CreateResult.EmptyName
        val place = Place(
            key = ctx.newKey(), order = OrderKeys.after(doc.world.places.maxOfOrNull { it.order }), name = clean,
            description = description(ctx, descriptionText), parentPlaceKey = parentPlaceKey,
            updatedAt = ctx.now(), updatedBy = ctx.device,
        )
        return CreateResult.Created(doc.copy(world = doc.world.copy(places = doc.world.places + place)), place.key)
    }

    fun createFaction(doc: ProjectDoc, name: String, ctx: EditContext, descriptionText: String = ""): CreateResult {
        val clean = TermText.clean(name)
        if (clean.isEmpty()) return CreateResult.EmptyName
        val faction = Faction(
            key = ctx.newKey(), order = OrderKeys.after(doc.world.factions.maxOfOrNull { it.order }), name = clean,
            description = description(ctx, descriptionText), updatedAt = ctx.now(), updatedBy = ctx.device,
        )
        return CreateResult.Created(doc.copy(world = doc.world.copy(factions = doc.world.factions + faction)), faction.key)
    }

    fun create(doc: ProjectDoc, kind: WorldKind, name: String, ctx: EditContext): CreateResult = when (kind) {
        WorldKind.PLACE -> createPlace(doc, name, ctx)
        WorldKind.TERM -> createTerm(doc, name, ctx)
        WorldKind.FACTION -> createFaction(doc, name, ctx)
    }

    /** Renames a place or faction (terms: [io.github.lenticualrstone.civilight.domain.world.term.TermEdits.rename]). */
    fun rename(doc: ProjectDoc, kind: WorldKind, key: Key, name: String, ctx: EditContext): ProjectDoc? {
        val clean = TermText.clean(name)
        if (clean.isEmpty()) return null
        val now = ctx.now()
        return when (kind) {
            WorldKind.PLACE -> doc.copy(world = doc.world.copy(places = doc.world.places.map {
                if (it.key == key) it.copy(name = clean, updatedAt = now, updatedBy = ctx.device) else it
            }))
            WorldKind.FACTION -> doc.copy(world = doc.world.copy(factions = doc.world.factions.map {
                if (it.key == key) it.copy(name = clean, updatedAt = now, updatedBy = ctx.device) else it
            }))
            WorldKind.TERM -> doc.copy(world = doc.world.copy(terms = doc.world.terms.map {
                if (it.key == key) it.copy(name = clean, updatedAt = now, updatedBy = ctx.device) else it
            }))
        }
    }

    /** Item hiding (rule 19, WLD-13). */
    fun setHidden(doc: ProjectDoc, kind: WorldKind, key: Key, hidden: Boolean, ctx: EditContext): ProjectDoc {
        val now = ctx.now()
        return when (kind) {
            WorldKind.PLACE -> doc.copy(world = doc.world.copy(places = doc.world.places.map {
                if (it.key == key) it.copy(hidden = hidden, updatedAt = now, updatedBy = ctx.device) else it
            }))
            WorldKind.TERM -> doc.copy(world = doc.world.copy(terms = doc.world.terms.map {
                if (it.key == key) it.copy(hidden = hidden, updatedAt = now, updatedBy = ctx.device) else it
            }))
            WorldKind.FACTION -> doc.copy(world = doc.world.copy(factions = doc.world.factions.map {
                if (it.key == key) it.copy(hidden = hidden, updatedAt = now, updatedBy = ctx.device) else it
            }))
        }
    }

    /** Sets a place's `지역` (parent place); refuses cycles. */
    fun setParentPlace(doc: ProjectDoc, placeKey: Key, parentKey: Key?, ctx: EditContext): ProjectDoc? {
        var current = parentKey
        val seen = HashSet<Key>()
        while (current != null) {
            if (current == placeKey || !seen.add(current)) return null
            current = doc.index.place(current)?.parentPlaceKey
        }
        return doc.copy(world = doc.world.copy(places = doc.world.places.map {
            if (it.key == placeKey) it.copy(parentPlaceKey = parentKey, updatedAt = ctx.now(), updatedBy = ctx.device) else it
        }))
    }
}
