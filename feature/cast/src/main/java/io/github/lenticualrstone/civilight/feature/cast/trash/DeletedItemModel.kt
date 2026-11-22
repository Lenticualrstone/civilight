package io.github.lenticualrstone.civilight.feature.cast.trash

import io.github.lenticualrstone.civilight.domain.cast.refs.RefRewriter
import io.github.lenticualrstone.civilight.domain.trash.DeletedItemRow
import io.github.lenticualrstone.civilight.domain.trash.DeletedItems
import io.github.lenticualrstone.civilight.domain.trash.DocChange
import io.github.lenticualrstone.civilight.domain.trash.Fixes
import io.github.lenticualrstone.civilight.domain.trash.HidingLocks
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.RefKind
import io.github.lenticualrstone.civilight.domain.trash.ReferenceScanner
import io.github.lenticualrstone.civilight.domain.trash.TrashContext
import io.github.lenticualrstone.civilight.domain.trash.TrashCopy
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary

/** A bulk change of one reference kind of a deleted item (CST-42): `화자 한꺼번에 바꾸기`, `@ 한꺼번에 바꾸기`. */
enum class BulkKind(val refKind: RefKind) { SPEAKER(RefKind.SPEAKER), LINK(RefKind.LINK) }

/** A bulk button and the kind it changes. */
data class BulkAction(val kind: BulkKind, val label: String)

/**
 * The check row of a deleted item (CST-42, `Check`) as the shared delete sheet shows it for an item
 * already in the trash or left as a name tag: `◇ 지운 인물 · 노아`, `휴지통 · 3일 남음 · 대사 14 · …`,
 * one bulk button per kind, `되살리기` (while in the trash) and `이대로 두기` (until chosen).
 */
data class DeletedItemView(
    val title: String,
    val sub: String,
    val bulk: List<BulkAction>,
    val canRestore: Boolean,
    val kept: Boolean,
)

/** What a bulk change did: the next document, the `되돌리기` pair and the preview line it applied. */
data class BulkChanged(val doc: ProjectDoc, val change: DocChange, val line: String)

/**
 * Rule 20 §3 for one deleted item (CST-42, CST-44). Rows, counts and labels are `domain:trash`
 * [DeletedItems]; speakers change through [Fixes.replaceSpeaker] (one undo step); `@` links are
 * pointed at the new item (text kept) through `domain:cast` [RefRewriter.retargetLinks]. A bulk
 * kind with no write function here (`장소 · 연결`) is not offered.
 */
object DeletedItemModel {
    fun row(doc: ProjectDoc, kind: EntityKind, key: Key, ctx: TrashContext, recorder: RecorderLibrary?, itemsLocked: Boolean): DeletedItemRow? {
        val item = ItemTrash.itemId(kind, key)
        return DeletedItems.rows(doc, ctx, recorder, HidingLocks(itemsLocked = itemsLocked)).firstOrNull { it.item == item }
    }

    /**
     * The row as the sheet draws it. A bulk button shows only while [doc] holds references it can
     * change: the row's `@` count also includes links in recorder text, which `@ 한꺼번에 바꾸기` does
     * not rewrite (they belong to the recorder library).
     */
    fun view(row: DeletedItemRow, doc: ProjectDoc): DeletedItemView = DeletedItemView(
        title = row.label,
        sub = TrashCopy.join(row.location, row.body),
        // A masked (locked hidden) row offers no bulk change: its lines are not named.
        bulk = if (row.masked) emptyList() else row.actions.mapNotNull { label -> BULK_LABELS[label]?.let { BulkAction(it, label) } }
            .filter { count(doc, row.item, it.kind) > 0 },
        canRestore = row.canRestore,
        kept = row.kept,
    )

    /** Live references of [kind] to the deleted item (lines in trashed scenes are not counted). */
    fun count(doc: ProjectDoc, item: ItemId, kind: BulkKind): Int =
        ReferenceScanner(doc).referencesTo(item).count { it.kind == kind.refKind }

    /** `대사 14 · 노아 → 오웬`, `@ 3 · 노아 → 오웬`. */
    fun preview(doc: ProjectDoc, kind: EntityKind, from: Key, to: Key, bulk: BulkKind): String? {
        val item = ItemTrash.itemId(kind, from)
        val fromName = nameOf(doc, kind, from) ?: return null
        val toName = nameOf(doc, kind, to) ?: return null
        val n = count(doc, item, bulk)
        return when (bulk) {
            BulkKind.SPEAKER -> Fixes.replaceSpeakerPreview(n, fromName, toName)
            BulkKind.LINK -> CastUiCopy.linkChange(n, fromName, toName)
        }
    }

    /**
     * `바꾸기`: every live reference of [bulk] to [from] now points at [to] (a live item of the same
     * kind). Null when nothing would change or [to] is not live.
     */
    fun apply(doc: ProjectDoc, kind: EntityKind, from: Key, to: Key, bulk: BulkKind, ctx: TrashContext): BulkChanged? {
        if (from == to || !ItemTrash.isLive(doc, kind, to)) return null
        val line = preview(doc, kind, from, to, bulk) ?: return null
        val next = when (bulk) {
            BulkKind.SPEAKER -> if (kind == EntityKind.CHARACTER) Fixes.replaceSpeaker(doc, from, to, ctx).doc else return null
            BulkKind.LINK -> {
                val links = ReferenceScanner(doc).referencesTo(ItemTrash.itemId(kind, from)).filter { it.kind == RefKind.LINK }.map { it.ownerKey }.toSet()
                RefRewriter.retargetLinks(doc, links, to)
            }
        }
        if (next == doc) return null
        return BulkChanged(next, DocChange(doc, next), line)
    }

    private fun nameOf(doc: ProjectDoc, kind: EntityKind, key: Key): String? = when (kind) {
        EntityKind.CHARACTER -> doc.index.character(key)?.name
        EntityKind.PLACE -> doc.index.place(key)?.name
        EntityKind.TERM -> doc.index.term(key)?.name
        EntityKind.FACTION -> doc.index.faction(key)?.name
        else -> null
    } ?: doc.index.nameTag(key)?.name

    private val BULK_LABELS = mapOf(
        "화자 한꺼번에 바꾸기" to BulkKind.SPEAKER,
        "@ 한꺼번에 바꾸기" to BulkKind.LINK,
    )
}
