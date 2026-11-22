package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.library.hiding.ItemSurfaces
import io.github.lenticualrstone.civilight.domain.trash.DeleteResult
import io.github.lenticualrstone.civilight.domain.trash.DocChange
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.Messages
import io.github.lenticualrstone.civilight.domain.trash.PurgeOps
import io.github.lenticualrstone.civilight.domain.trash.PurgeResult
import io.github.lenticualrstone.civilight.domain.trash.RestoreOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.Toast
import io.github.lenticualrstone.civilight.domain.trash.TrashCopy
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryFile
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.text.RichDoc

/** The owner of one gallery (55 §3.1): the project itself or a character. */
data class GalleryOwner(val kind: GalleryOwnerKind, val key: Key) {
    companion object {
        fun project(doc: ProjectDoc) = GalleryOwner(GalleryOwnerKind.PROJECT, doc.key)
        fun character(key: Key) = GalleryOwner(GalleryOwnerKind.CHARACTER, key)
        fun of(item: GalleryItem) = GalleryOwner(item.ownerKind.known ?: GalleryOwnerKind.PROJECT, item.ownerKey)
    }
}

/** A picker choice to add (GAL-11): `preset:<id>` or a blob hash, with the blob row for a hash. */
data class GalleryAddSource(val source: String, val blob: Blob? = null)

/** What a picker tile or an owner row shows for a source already in a gallery (GAL-11, GAL-16, GAL-19). */
enum class GallerySourceMark(val label: String?) {
    /** Free to add. Also the answer for a source only a hidden item holds while locked (never named). */
    NONE(null),

    /** A visible live item holds it (`이미 있음`). */
    ALREADY_THERE("이미 있음"),

    /** A visible trashed item holds it (`휴지통에 있음`); restoring it never duplicates (§6 Q15). */
    IN_TRASH("휴지통에 있음"),
}

/** One gallery edit for `되돌리기` (one undo step, T-GAL-11, T-GAL-25). */
data class GalleryEdit(val doc: ProjectDoc, val change: DocChange)

/**
 * `넣기` and `다른 곳에도 넣기` (GAL-11, GAL-16). [added] in the chosen order (the first is the item to
 * pick); [alreadyThere] counts only visible duplicates, so a hidden one is never hinted at (GAL-19).
 */
data class GalleryAddResult(
    val doc: ProjectDoc,
    val change: DocChange,
    val added: List<Key>,
    val alreadyThere: Int,
    /** `3장 넣음` · `0장 넣음 · 이미 있음 1`; `되돌리기` when anything was added. */
    val toast: Toast,
)

/** `지우기` of one or several images (GAL-20, GAL-18): the trash result and `이미지 지움 · <title>`. */
data class GalleryDeleteResult(val result: DeleteResult, val toast: Toast) {
    val doc: ProjectDoc get() = result.doc
    val change: DocChange get() = result.change
}

/** The order buttons (GAL-15): `앞으로` · `뒤로` · `맨 앞` · `맨 뒤`. */
enum class GalleryMove { FORWARD, BACKWARD, FIRST, LAST }

/** UI copy of the gallery operations (55, terse fragments). */
object GalleryCopy {
    const val ADDED_SUFFIX = "장 넣음"
    const val ALREADY_THERE = "이미 있음"

    /** `3장 넣음` · `0장 넣음 · 이미 있음 1` (GAL-11). */
    fun addedToast(added: Int, alreadyThere: Int): Toast = Toast(
        TrashCopy.join("$added$ADDED_SUFFIX", if (alreadyThere > 0) "$ALREADY_THERE $alreadyThere" else null),
        if (added > 0) TrashCopy.UNDO else null,
    )
}

/**
 * The one write path of gallery items (DECISIONS §1, 55 §3.4): add, copy to another owner, edit,
 * order (buttons and `이 뒤로`), delete, restore and purge. `feature:canvas` calls these; trash work
 * goes through `domain:trash` (`TrashOps`, `RestoreOps`, `PurgeOps`). Every call is one undo step
 * (`DocChange.revert`).
 *
 * [locked] everywhere means "the project's hidden items are locked" (rule 19): hidden images are then
 * invisible, skipped without being counted and never moved past (GAL-19).
 */
object GalleryActions {
    // --- Reading ---

    /** Live items of [owner] in `직접 순서` (hidden ones included; mask with `ItemSurfaces.gallery`). */
    fun items(doc: ProjectDoc, owner: GalleryOwner): List<GalleryItem> =
        doc.index.galleryItemsOf(owner.key).filter { it.ownerKind.known == owner.kind && doc.liveness.isLive(it) }

    /** Visible live items of [owner] in `직접 순서` (what positions `<i>/<N>` count). */
    fun visibleItems(doc: ProjectDoc, owner: GalleryOwner, locked: Boolean): List<GalleryItem> {
        val surfaces = ItemSurfaces(doc)
        return items(doc, owner).filter { surfaces.isGalleryItemVisible(it, locked) }
    }

    /** The mark of [source] for [owner] (`이미 있음` · `휴지통에 있음`); see [GallerySourceMark]. */
    fun sourceMark(doc: ProjectDoc, owner: GalleryOwner, source: String, locked: Boolean): GallerySourceMark {
        val holder = holderOf(doc, owner, source) ?: return GallerySourceMark.NONE
        if (locked && holder.hidden) return GallerySourceMark.NONE
        return if (holder.deletedAt != null) GallerySourceMark.IN_TRASH else GallerySourceMark.ALREADY_THERE
    }

    /**
     * Bytes of the distinct blobs live gallery items reference (55 §6 Q13, T-GAL-38): each blob
     * once, presets 0; sizes from the `assets.blobs` rows.
     */
    fun galleryBytes(doc: ProjectDoc): Long {
        val hashes = doc.galleryItems.filter { doc.liveness.isLive(it) }.mapNotNullTo(LinkedHashSet()) { it.blobHash }
        return hashes.sumOf { doc.index.blob(it)?.bytes ?: 0L }
    }

    // --- Adding ---

    /**
     * `넣기 · <k>` (GAL-11): [sources] in the chosen order become new items at the end of [owner]'s
     * gallery. A source the owner already holds (live or trashed, §3.1) is skipped; one held only by
     * a hidden item while [locked] is skipped without being counted. Blob rows are added to the
     * project's catalogue once.
     */
    fun add(
        doc: ProjectDoc,
        owner: GalleryOwner,
        sources: List<GalleryAddSource>,
        ctx: ActionContext,
        locked: Boolean,
        holo: Boolean = false,
    ): GalleryAddResult {
        var d = doc
        val added = mutableListOf<Key>()
        var visibleDuplicates = 0
        val seen = HashSet<String>()
        for (choice in sources) {
            if (choice.source.isBlank() || !seen.add(choice.source)) continue
            val holder = holderOf(d, owner, choice.source)
            if (holder != null) {
                if (!(locked && holder.hidden)) visibleDuplicates++
                continue
            }
            val item = GalleryItem(
                key = ctx.newKey(), ownerKind = owner.kind.open(), ownerKey = owner.key, source = choice.source,
                order = OrderKeys.after(lastOrder(d, owner)), holo = holo,
                createdAt = ctx.now, updatedAt = ctx.now, updatedBy = ctx.device,
            )
            d = d.withBlob(choice.blob).withItem(owner, item)
            added += item.key
        }
        return GalleryAddResult(d, DocChange(doc, d), added, visibleDuplicates, GalleryCopy.addedToast(added.size, visibleDuplicates))
    }

    /**
     * `다른 곳에도 넣기` (GAL-16, T-GAL-22): a new item for [target] per item of [itemKeys], with every
     * field except key, owner, order and bookkeeping (`hidden` included). Same uniqueness as [add].
     */
    fun copyTo(doc: ProjectDoc, itemKeys: List<Key>, target: GalleryOwner, ctx: ActionContext, locked: Boolean): GalleryAddResult {
        var d = doc
        val added = mutableListOf<Key>()
        var visibleDuplicates = 0
        for (key in itemKeys.distinct()) {
            val from = d.index.galleryItem(key) ?: continue
            val holder = holderOf(d, target, from.source)
            if (holder != null) {
                if (!(locked && holder.hidden)) visibleDuplicates++
                continue
            }
            val item = from.copy(
                key = ctx.newKey(), ownerKind = target.kind.open(), ownerKey = target.key,
                order = OrderKeys.after(lastOrder(d, target)),
                createdAt = ctx.now, deletedAt = null, trashEntryKey = null, updatedAt = ctx.now, updatedBy = ctx.device,
            )
            d = d.withItem(target, item)
            added += item.key
        }
        return GalleryAddResult(d, DocChange(doc, d), added, visibleDuplicates, GalleryCopy.addedToast(added.size, visibleDuplicates))
    }

    // --- Editing (the text sheet, crop, menu switches) ---

    /** Any change to one item's own fields; key, owner, order, source and trash marks are kept. Null when unknown. */
    fun edit(doc: ProjectDoc, itemKey: Key, ctx: ActionContext, change: (GalleryItem) -> GalleryItem): GalleryEdit? {
        val item = doc.index.galleryItem(itemKey) ?: return null
        val next = change(item).copy(
            key = item.key, ownerKind = item.ownerKind, ownerKey = item.ownerKey, order = item.order, source = item.source,
            deletedAt = item.deletedAt, trashEntryKey = item.trashEntryKey, createdAt = item.createdAt,
        )
        if (next == item) return GalleryEdit(doc, DocChange(doc, doc))
        val d = doc.replaceItem(next.copy(updatedAt = ctx.now, updatedBy = ctx.device))
        return GalleryEdit(d, DocChange(doc, d))
    }

    fun setTitle(doc: ProjectDoc, itemKey: Key, title: String, ctx: ActionContext) = edit(doc, itemKey, ctx) { it.copy(title = title.trim()) }

    /** The description; an empty document clears it (`설명 없음`). */
    fun setCaption(doc: ProjectDoc, itemKey: Key, caption: RichDoc?, ctx: ActionContext) =
        edit(doc, itemKey, ctx) { it.copy(caption = caption?.takeIf { c -> c.any { b -> b.runs.isNotEmpty() } }) }

    fun setTags(doc: ProjectDoc, itemKey: Key, tagKeys: List<Key>, ctx: ActionContext) =
        edit(doc, itemKey, ctx) { it.copy(tagKeys = tagKeys.distinct().filter { k -> doc.index.tag(k) != null }) }

    fun setScene(doc: ProjectDoc, itemKey: Key, sceneKey: Key?, ctx: ActionContext) = edit(doc, itemKey, ctx) { it.copy(sceneKey = sceneKey) }

    /** `자르기` (GAL-12): only the gallery ratios `r3x4` and `r1x1`. */
    fun setCrop(doc: ProjectDoc, itemKey: Key, ratio: Ratio, crop: Crop, ctx: ActionContext): GalleryEdit? {
        require(ratio in GalleryItem.RATIOS) { "not a gallery ratio: $ratio" }
        return edit(doc, itemKey, ctx) { it.withCrop(ratio, crop) }
    }

    fun setHolo(doc: ProjectDoc, itemKey: Key, holo: Boolean, ctx: ActionContext) = edit(doc, itemKey, ctx) { it.copy(holo = holo) }

    /** `숨기기` (GAL-19); turning it off needs authentication first (the caller asks). */
    fun setHidden(doc: ProjectDoc, itemKey: Key, hidden: Boolean, ctx: ActionContext) = edit(doc, itemKey, ctx) { it.copy(hidden = hidden) }

    // --- Order (GAL-15) ---

    /**
     * `앞으로` · `뒤로` · `맨 앞` · `맨 뒤` among the visible items (T-GAL-11, T-GAL-24): a hidden item
     * while [locked] is never a neighbour, so the visible order changes by exactly one place. Null
     * when the move is not possible (at the end, unknown, not visible).
     */
    fun move(doc: ProjectDoc, itemKey: Key, move: GalleryMove, ctx: ActionContext, locked: Boolean): GalleryEdit? {
        val item = doc.index.galleryItem(itemKey) ?: return null
        val visible = visibleItems(doc, GalleryOwner.of(item), locked)
        val i = visible.indexOfFirst { it.key == itemKey }
        if (i < 0) return null
        return when (move) {
            GalleryMove.FORWARD -> if (i == 0) null else placeBefore(doc, item, visible[i - 1].key, ctx)
            GalleryMove.BACKWARD -> if (i == visible.lastIndex) null else placeAfter(doc, item, visible[i + 1].key, ctx)
            GalleryMove.FIRST -> if (i == 0) null else placeBefore(doc, item, visible.first().key, ctx)
            GalleryMove.LAST -> if (i == visible.lastIndex) null else placeAfter(doc, item, visible.last().key, ctx)
        }
    }

    /** `옮기기` → `이 뒤로` on [afterKey] (T-GAL-25); null [afterKey] = `맨 앞으로`. One undo step. */
    fun moveAfter(doc: ProjectDoc, itemKey: Key, afterKey: Key?, ctx: ActionContext, locked: Boolean): GalleryEdit? {
        val item = doc.index.galleryItem(itemKey) ?: return null
        if (afterKey == itemKey) return null
        if (afterKey == null) {
            val first = visibleItems(doc, GalleryOwner.of(item), locked).firstOrNull { it.key != itemKey } ?: return null
            return placeBefore(doc, item, first.key, ctx)
        }
        val after = doc.index.galleryItem(afterKey) ?: return null
        if (after.ownerKey != item.ownerKey) return null
        return placeAfter(doc, item, afterKey, ctx)
    }

    // --- Trash ---

    /** `지우기` (GAL-20, GAL-18): live items to the trash, one entry each, one toast and one undo step. */
    fun delete(doc: ProjectDoc, itemKeys: List<Key>, ctx: ActionContext): GalleryDeleteResult? {
        val live = itemKeys.distinct().mapNotNull(doc.index::galleryItem).filter { doc.liveness.isLive(it) }
        if (live.isEmpty()) return null
        val result = TrashOps.delete(doc, live.map { ItemId.galleryItem(it.key) }, ctx.trash)
        return GalleryDeleteResult(result, Messages.galleryDeleteToast(live.map { it.title }))
    }

    /** `되돌리기` of any result above. */
    fun undo(current: ProjectDoc, change: DocChange): ProjectDoc = change.revert(current)

    /** `되살리기` of an image's trash entry; it comes back at its kept order. Hidden ones need [authenticated]. */
    fun restore(doc: ProjectDoc, entryKey: Key, ctx: ActionContext, authenticated: Boolean = false): RestoreOutcome =
        RestoreOps.restore(doc, entryKey, ctx.trash, authenticated)

    /** `영구 삭제` of an image's trash entry; its source is free again (T-GAL-21). */
    fun purge(doc: ProjectDoc, entryKey: Key, ctx: ActionContext): PurgeResult = PurgeOps.purge(doc, entryKey, ctx.trash)

    // --- Helpers ---

    /** The item of [owner] (live or trashed, any hiding) that holds [source], if any. */
    private fun holderOf(doc: ProjectDoc, owner: GalleryOwner, source: String): GalleryItem? =
        doc.index.galleryItemsWithSource(source).firstOrNull { it.ownerKey == owner.key && it.ownerKind.known == owner.kind }

    private fun lastOrder(doc: ProjectDoc, owner: GalleryOwner): OrderKey? = doc.gallery[owner.key]?.items?.maxOfOrNull { it.order }

    private fun ProjectDoc.withBlob(blob: Blob?): ProjectDoc {
        if (blob == null || index.blob(blob.hash) != null) return this
        return copy(assets = assets.copy(blobs = assets.blobs + blob))
    }

    private fun ProjectDoc.withItem(owner: GalleryOwner, item: GalleryItem): ProjectDoc {
        val file = gallery[owner.key] ?: GalleryFile(owner.kind.open(), owner.key)
        return copy(gallery = gallery + (owner.key to file.copy(items = file.items + item)))
    }

    private fun ProjectDoc.replaceItem(item: GalleryItem): ProjectDoc {
        val file = gallery[item.ownerKey] ?: return this
        return copy(gallery = gallery + (item.ownerKey to file.copy(items = file.items.map { if (it.key == item.key) item else it })))
    }

    /** [item] right after [anchorKey] in the owner's full stored order (hidden and trashed items included). */
    private fun placeAfter(doc: ProjectDoc, item: GalleryItem, anchorKey: Key, ctx: ActionContext): GalleryEdit? =
        place(doc, item, ctx) { others -> others.indexOfFirst { it.key == anchorKey }.takeIf { it >= 0 }?.let { it + 1 } }

    /** [item] right before [anchorKey]. */
    private fun placeBefore(doc: ProjectDoc, item: GalleryItem, anchorKey: Key, ctx: ActionContext): GalleryEdit? =
        place(doc, item, ctx) { others -> others.indexOfFirst { it.key == anchorKey }.takeIf { it >= 0 } }

    /**
     * Gives [item] an order key at slot [slotOf] of the other items (0 = first). When no key fits
     * between the neighbours (equal or too long keys), the owner's list is rebalanced in the same step.
     */
    private fun place(doc: ProjectDoc, item: GalleryItem, ctx: ActionContext, slotOf: (List<GalleryItem>) -> Int?): GalleryEdit? {
        val all = doc.index.galleryItemsOf(item.ownerKey)
        val others = all.filter { it.key != item.key }
        val slot = slotOf(others) ?: return null
        val lower = others.getOrNull(slot - 1)?.order
        val upper = others.getOrNull(slot)?.order
        val fits = lower == null || upper == null || lower < upper
        val key = if (fits) OrderKeys.between(lower, upper) else null
        val d = if (key != null && !OrderKeys.needsRebalance(key)) {
            doc.replaceItem(item.copy(order = key, updatedAt = ctx.now, updatedBy = ctx.device))
        } else {
            val wanted = others.toMutableList().apply { add(slot, item) }
            val keys = OrderKeys.rebalance(wanted).associate { (row, k) -> row.key to k }
            val file = doc.gallery.getValue(item.ownerKey)
            doc.copy(
                gallery = doc.gallery + (
                    item.ownerKey to file.copy(
                        items = file.items.map { row ->
                            val k = keys.getValue(row.key)
                            if (row.key == item.key || row.order != k) row.copy(order = k, updatedAt = ctx.now, updatedBy = ctx.device) else row
                        },
                    )
                    ),
            )
        }
        return GalleryEdit(d, DocChange(doc, d))
    }
}
