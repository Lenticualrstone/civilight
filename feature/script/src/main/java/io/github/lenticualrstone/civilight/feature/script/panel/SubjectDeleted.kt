package io.github.lenticualrstone.civilight.feature.script.panel

import io.github.lenticualrstone.civilight.domain.trash.Fixes
import io.github.lenticualrstone.civilight.domain.trash.TrashContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind

/**
 * A record whose subject is a deleted item (SCR-57, rule 20 §3, `domain:trash` `DerivedMarks.targetDeleted`):
 * the card reads `대상 지워짐` and offers `되살리기` (not after permanent deletion) · `이대로 두기` ·
 * `기록 지우기`. [itemKey] is the deleted item, [kind] how to restore it.
 */
data class DeletedSubject(val kind: LinkTargetKind?, val itemKey: Key, val canRestore: Boolean, val kept: Boolean)

object SubjectDeleted {
    const val LABEL = "대상 지워짐"

    /** The deleted subject of [record], or null when its subject is live. */
    fun of(doc: ProjectDoc, record: ChangeRecord): DeletedSubject? {
        if (doc.liveness.isTargetLive(record.target)) return null
        val candidates: List<Pair<LinkTargetKind?, Key>> = when (val t = record.target) {
            is ChangeTarget.RelationSlot -> doc.index.relation(t.relationKey)?.let { listOf(it.endA.key, it.endB.key) }.orEmpty().map { kindOf(doc, it) to it }
            is ChangeTarget.Field -> listOf(kindOf(doc, t.entityKey) to t.entityKey)
            is ChangeTarget.DisplayName -> listOf(LinkTargetKind.CHARACTER to t.characterKey)
            is ChangeTarget.TermReveal -> listOf(LinkTargetKind.TERM to t.termKey)
            else -> emptyList()
        }
        val (kind, key) = candidates.firstOrNull { (_, k) -> !isLive(doc, k) } ?: candidates.firstOrNull() ?: return null
        val exists = existsInTrash(doc, key)
        return DeletedSubject(kind, key, canRestore = exists, kept = isKept(doc, key, record.key))
    }

    /** `이대로 두기`: this record keeps its dotted mark and moves to `맞다고 표시한 것` (`[item, record]`). */
    fun keep(doc: ProjectDoc, itemKey: Key, recordKey: Key, ctx: TrashContext): ProjectDoc =
        Fixes.keep(doc, itemKey, ctx, referencingKey = recordKey).doc

    fun isKept(doc: ProjectDoc, itemKey: Key, recordKey: Key): Boolean =
        Fixes.isKept(doc, itemKey) || doc.checks.acknowledgements.any {
            it.checkType == CheckType.DELETED_ITEM && it.subjectKeys == listOf(itemKey, recordKey)
        }

    private fun kindOf(doc: ProjectDoc, key: Key): LinkTargetKind? = when {
        doc.index.character(key) != null -> LinkTargetKind.CHARACTER
        doc.index.term(key) != null -> LinkTargetKind.TERM
        doc.index.place(key) != null -> LinkTargetKind.PLACE
        doc.index.faction(key) != null -> LinkTargetKind.FACTION
        else -> null
    }

    private fun isLive(doc: ProjectDoc, key: Key): Boolean {
        val deletedAt = doc.index.character(key)?.deletedAt ?: doc.index.term(key)?.deletedAt
            ?: doc.index.place(key)?.deletedAt ?: doc.index.faction(key)?.deletedAt
        val exists = doc.index.character(key) != null || doc.index.term(key) != null || doc.index.place(key) != null || doc.index.faction(key) != null
        return exists && deletedAt == null
    }

    /** Still in the trash (not purged): `되살리기` can bring it back. */
    private fun existsInTrash(doc: ProjectDoc, key: Key): Boolean =
        (doc.index.character(key)?.trashEntryKey ?: doc.index.term(key)?.trashEntryKey ?: doc.index.place(key)?.trashEntryKey
            ?: doc.index.faction(key)?.trashEntryKey) != null
}
