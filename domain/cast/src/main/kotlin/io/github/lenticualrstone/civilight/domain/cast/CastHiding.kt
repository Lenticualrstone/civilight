package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.EntityRef
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** A list of change records with the locked hidden ones folded into one row (rule 19 §6). */
data class ChangeRows(val visible: List<ChangeRecord>, val hiddenCount: Int) {
    /** `숨긴 항목 변화 2`, or null. */
    val hiddenRow: String? get() = if (hiddenCount > 0) "숨긴 항목 변화 $hiddenCount" else null
}

/**
 * Item hiding as cast data (CST-39): which records and ends belong to a hidden item, so lists of
 * change records (lens views, the scene time panel) can fold them into `숨긴 항목 변화 N`.
 */
object CastHiding {
    private fun hiddenItem(doc: ProjectDoc, kind: EntityKind, key: String): Boolean = when (kind) {
        EntityKind.CHARACTER -> doc.index.character(key)?.hidden
        EntityKind.PLACE -> doc.index.place(key)?.hidden
        EntityKind.TERM -> doc.index.term(key)?.hidden
        EntityKind.FACTION -> doc.index.faction(key)?.hidden
    } == true

    private fun hiddenEnd(doc: ProjectDoc, end: EntityRef): Boolean = when (end.kind.known) {
        RelationEndKind.CHARACTER -> hiddenItem(doc, EntityKind.CHARACTER, end.key)
        RelationEndKind.FACTION -> hiddenItem(doc, EntityKind.FACTION, end.key)
        null -> false
    }

    /** The record is about a hidden item: its field or display name, a relation with a hidden end, a hidden term's reveal. */
    fun isAboutHidden(doc: ProjectDoc, record: ChangeRecord): Boolean = when (val t = record.target) {
        is ChangeTarget.Field -> t.entityKind.known?.let { hiddenItem(doc, it, t.entityKey) } == true
        is ChangeTarget.DisplayName -> hiddenItem(doc, EntityKind.CHARACTER, t.characterKey)
        is ChangeTarget.RelationSlot -> doc.index.relation(t.relationKey)?.let { hiddenEnd(doc, it.endA) || hiddenEnd(doc, it.endB) } == true
        is ChangeTarget.TermReveal -> hiddenItem(doc, EntityKind.TERM, t.termKey)
        else -> false
    }

    /** While locked, records about hidden items are replaced by one count row. */
    fun changeRows(doc: ProjectDoc, records: List<ChangeRecord>, hiddenUnlocked: Boolean): ChangeRows {
        if (hiddenUnlocked) return ChangeRows(records, 0)
        val (hidden, visible) = records.partition { isAboutHidden(doc, it) }
        return ChangeRows(visible, hidden.size)
    }
}
