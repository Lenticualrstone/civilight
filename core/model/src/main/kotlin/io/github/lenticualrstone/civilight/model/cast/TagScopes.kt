package io.github.lenticualrstone.civilight.model.cast

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc

/**
 * (2026-11-26) True when an item of [kind] may carry this tag: the tag's own kind or [TagScope.ALL].
 * A scope from a newer file (unknown) fits no kind: it is kept and written back, but offered nowhere.
 * [kind] is the kind of the item (never [TagScope.ALL]).
 */
fun Tag.fits(kind: TagScope): Boolean {
    val own = scope.known ?: return false
    return own == TagScope.ALL || own == kind
}

/**
 * (2026-11-26) Scope reads shared by every surface (pickers, chips, sections, cards). Pure; the writes
 * live in `domain:actions` `TagActions` and `domain:world` `TermTags`.
 */
object TagScopes {
    /** Every live tag in tag order, whatever its scope (name uniqueness, tag settings). */
    fun live(doc: ProjectDoc): List<Tag> =
        doc.project.tags.filter { it.isLive }.sortedWith(compareBy<Tag>({ it.order }, { it.key }))

    /** `tagsFor(kind)`: live tags an item of [kind] may carry (scope == [kind] or ALL), in tag order. */
    fun tagsFor(doc: ProjectDoc, kind: TagScope): List<Tag> = live(doc).filter { it.fits(kind) }

    /** The live tag of [tagKey] when an item of [kind] may carry it, else null (trashed, gone or out of scope). */
    fun liveFor(doc: ProjectDoc, tagKey: Key, kind: TagScope): Tag? =
        doc.index.tag(tagKey)?.takeIf { it.isLive && it.fits(kind) }

    /** The live tag already named [name] (NFC, trimmed), any scope; null when the name is free or blank. */
    fun liveNamed(doc: ProjectDoc, name: String, except: Key? = null): Tag? {
        val n = name.trim().nfc()
        if (n.isEmpty()) return null
        return doc.project.tags.firstOrNull { it.isLive && it.key != except && it.name.nfc() == n }
    }

    /**
     * A key an item already holds that no edit may take off: a trashed tag's (N-44: restoring the tag shows
     * it again) or a live tag's whose scope comes from a newer file (unknown here), so an older app never
     * silently detaches it.
     */
    fun keepsHeld(tag: Tag): Boolean = !tag.isLive || tag.scope.known == null

    /**
     * Holder keys after an edit: the picked keys that are live tags an item of [kind] may carry, plus the
     * held keys no edit takes off ([keepsHeld]), all in tag order.
     */
    fun settle(doc: ProjectDoc, kind: TagScope, picked: List<Key>, held: List<Key>): List<Key> {
        val keep = LinkedHashSet<Key>()
        picked.forEach { k -> if (liveFor(doc, k, kind) != null) keep += k }
        held.forEach { k -> doc.index.tag(k)?.takeIf(::keepsHeld)?.let { keep += k } }
        return inTagOrder(doc, keep)
    }

    /**
     * Holder keys after a pick-ordered edit (characters, gallery images): [picked] in its order, minus live tags
     * of another scope and unknown-scope keys not already [held]; held unknown-scope keys stay (appended when the
     * pick left them out). [keepMissing] = keep keys with no tag row.
     */
    fun settleInPickOrder(doc: ProjectDoc, kind: TagScope, picked: List<Key>, held: List<Key>, keepMissing: Boolean): List<Key> {
        val heldSet = held.toHashSet()
        val out = picked.distinct().filter { k ->
            val tag = doc.index.tag(k) ?: return@filter keepMissing
            when {
                !tag.isLive -> true
                tag.scope.known == null -> k in heldSet
                else -> tag.fits(kind)
            }
        }
        val kept = out.toHashSet()
        return out + held.distinct().filter { k -> k !in kept && doc.index.tag(k)?.let { it.isLive && it.scope.known == null } == true }
    }

    /** [keys] sorted by the tags' order (trashed ones too); keys with no tag row keep their place at the end. */
    fun inTagOrder(doc: ProjectDoc, keys: Collection<Key>): List<Key> {
        val rank = doc.project.tags.sortedWith(compareBy<Tag>({ it.order }, { it.key })).withIndex().associate { it.value.key to it.index }
        return keys.distinct().sortedBy { rank[it] ?: Int.MAX_VALUE }
    }
}
