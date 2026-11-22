package io.github.lenticualrstone.civilight.domain.story.checks

import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.checks.AckKind
import io.github.lenticualrstone.civilight.model.checks.Acknowledgement
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open

/** Anything the check centre lists that can be acknowledged: registry id, subject and fingerprint. */
interface AckSubject {
    val checkType: String
    val subjectKeys: List<Key>
    val fingerprint: String
}

/**
 * `맞음` · `이대로 두기` (STO-41): one stored [Acknowledgement] per (check type, subject). An
 * acknowledgement whose fingerprint differs from the item's current one is void and the item is open
 * again; check results themselves are never stored.
 */
object Acks {
    /** The stored acknowledgement on this subject (any fingerprint), or null. */
    fun find(doc: ProjectDoc, checkType: String, subjectKeys: List<Key>): Acknowledgement? =
        doc.checks.acknowledgements.firstOrNull { it.checkType == checkType && it.subjectKeys == subjectKeys }

    fun find(doc: ProjectDoc, item: AckSubject): Acknowledgement? = find(doc, item.checkType, item.subjectKeys)

    /** True when a valid (same-fingerprint) acknowledgement hides the item from the open list. */
    fun isAcknowledged(doc: ProjectDoc, item: AckSubject): Boolean = find(doc, item)?.fingerprint == item.fingerprint

    /** Items that stay open (no valid acknowledgement). */
    fun <T : AckSubject> open(doc: ProjectDoc, items: List<T>): List<T> = items.filterNot { isAcknowledged(doc, it) }

    /** Items listed under `맞다고 표시한 것`. */
    fun <T : AckSubject> acknowledged(doc: ProjectDoc, items: List<T>): List<T> = items.filter { isAcknowledged(doc, it) }

    /**
     * Stores `맞음` (or `이대로 두기` for the KEEP types) on [item], replacing a void one on the same
     * subject. The kind follows [CheckType.ACKABLE_AS]; types not listed there are acknowledged as
     * `맞음` (`ED`).
     */
    fun acknowledge(doc: ProjectDoc, item: AckSubject, ctx: EditContext, kind: AckKind? = null): ProjectDoc {
        val k = kind ?: CheckType.ACKABLE_AS[item.checkType] ?: AckKind.MATCH
        val existing = find(doc, item)
        val ack = Acknowledgement(
            key = existing?.key ?: ctx.newKey(),
            checkType = item.checkType,
            subjectKeys = item.subjectKeys,
            fingerprint = item.fingerprint,
            kind = k.open(),
            createdAt = ctx.now,
            updatedAt = ctx.now,
            updatedBy = ctx.device,
        )
        val rest = doc.checks.acknowledgements.filterNot { it.key == existing?.key }
        return doc.copy(checks = doc.checks.copy(acknowledgements = rest + ack))
    }

    /** `되돌리기` in `맞다고 표시한 것`: removes the acknowledgement; the item is open again. */
    fun revert(doc: ProjectDoc, ackKey: Key): ProjectDoc =
        doc.copy(checks = doc.checks.copy(acknowledgements = doc.checks.acknowledgements.filterNot { it.key == ackKey }))

    /**
     * Acknowledgements go with a deleted subject (R12 §1, `ED` CHK-05): drops every acknowledgement
     * naming one of [deletedKeys]. Undoing the delete restores the previous doc and so the
     * acknowledgement; restoring the subject later from the trash evaluates the item fresh.
     */
    fun dropForSubjects(doc: ProjectDoc, deletedKeys: Collection<Key>): ProjectDoc {
        val gone = deletedKeys.toSet()
        val kept = doc.checks.acknowledgements.filterNot { a -> a.subjectKeys.any { it in gone } }
        return if (kept.size == doc.checks.acknowledgements.size) doc else doc.copy(checks = doc.checks.copy(acknowledgements = kept))
    }
}
