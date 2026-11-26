package io.github.lenticualrstone.civilight.domain.world.term

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Tag
import io.github.lenticualrstone.civilight.model.cast.TagScope
import io.github.lenticualrstone.civilight.model.cast.TagScopes
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.world.Term

/**
 * (2026-11-26) Tags on a term (`Term.tagKeys`, board ImmTermTagSheet): only live tags of scope `용어 전용`
 * or `모든 항목` are attached or shown; the stored order follows the tag order. Each function is one edit
 * (one undo step through the card's edit pipeline). Tag create, widen and delete are `domain:actions`
 * `TagActions`; a trashed tag's key stays on the term (shown nowhere), so restoring the tag reattaches it.
 */
object TermTags {
    /** The tags the term card row shows: live, scope-valid, in tag order. Empty for a missing term. */
    fun tags(doc: ProjectDoc, termKey: Key): List<Tag> {
        val term = doc.index.term(termKey) ?: return emptyList()
        val held = term.tagKeys.toSet()
        return TagScopes.tagsFor(doc, TagScope.TERM).filter { it.key in held }
    }

    /** The term tag picker (`용어에서 쓸 수 있는 것`): every live tag a term may carry, in tag order. */
    fun choices(doc: ProjectDoc): List<Tag> = TagScopes.tagsFor(doc, TagScope.TERM)

    /** Whether the card offers `+ 태그`: a live term in an editable project (a new tag can always be made). */
    fun canAdd(doc: ProjectDoc, termKey: Key, editable: Boolean): Boolean =
        editable && doc.index.term(termKey)?.deletedAt == null && doc.index.term(termKey) != null

    /**
     * Sets the term's tags to [tagKeys]: keys of live tags a term may not carry are dropped; trashed tags'
     * keys the term already held stay. Stored in tag order. Unchanged (same instance) when nothing changes.
     */
    fun set(doc: ProjectDoc, termKey: Key, tagKeys: List<Key>, ctx: EditContext): ProjectDoc {
        val term = doc.index.term(termKey) ?: return doc
        val next = TagScopes.settle(doc, TagScope.TERM, tagKeys, term.tagKeys)
        return if (next == term.tagKeys) doc else touch(doc, termKey, ctx) { it.copy(tagKeys = next) }
    }

    /** Attaches one tag (a chip tick). No change when the tag is not live or a term may not carry it. */
    fun attach(doc: ProjectDoc, termKey: Key, tagKey: Key, ctx: EditContext): ProjectDoc {
        val term = doc.index.term(termKey) ?: return doc
        if (TagScopes.liveFor(doc, tagKey, TagScope.TERM) == null || tagKey in term.tagKeys) return doc
        return set(doc, termKey, term.tagKeys + tagKey, ctx)
    }

    /** Detaches one tag (a chip untick). */
    fun detach(doc: ProjectDoc, termKey: Key, tagKey: Key, ctx: EditContext): ProjectDoc {
        val term = doc.index.term(termKey) ?: return doc
        if (tagKey !in term.tagKeys) return doc
        return touch(doc, termKey, ctx) { it.copy(tagKeys = it.tagKeys - tagKey) }
    }

    /** Attaches when off, detaches when on. */
    fun toggle(doc: ProjectDoc, termKey: Key, tagKey: Key, ctx: EditContext): ProjectDoc {
        val term = doc.index.term(termKey) ?: return doc
        return if (tagKey in term.tagKeys) detach(doc, termKey, tagKey, ctx) else attach(doc, termKey, tagKey, ctx)
    }

    private fun touch(doc: ProjectDoc, termKey: Key, ctx: EditContext, f: (Term) -> Term): ProjectDoc =
        doc.copy(world = doc.world.copy(terms = doc.world.terms.map {
            if (it.key == termKey) f(it).copy(updatedAt = ctx.now(), updatedBy = ctx.device) else it
        }))
}
