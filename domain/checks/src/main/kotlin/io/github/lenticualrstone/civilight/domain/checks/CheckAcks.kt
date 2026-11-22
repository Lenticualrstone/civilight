package io.github.lenticualrstone.civilight.domain.checks

import io.github.lenticualrstone.civilight.domain.script.Anchors
import io.github.lenticualrstone.civilight.domain.story.checks.Acks
import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.domain.trash.Fixes
import io.github.lenticualrstone.civilight.domain.trash.TrashContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.AnchorSerializer
import io.github.lenticualrstone.civilight.model.change.AnchorState
import io.github.lenticualrstone.civilight.model.checks.Acknowledgement
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.open
import kotlinx.serialization.json.buildJsonObject
import io.github.lenticualrstone.civilight.domain.script.EditContext as ScriptEditContext

/**
 * `맞음` · `그대로` · `이대로 두기` and `되돌리기` from 점검 센터 (STO-41). Each returns the new doc
 * (one undo step); stored [Acknowledgement]s go to `checks.json` and travel with the `.civilight`.
 */
object CheckAcks {
    /** The stored acknowledgement of an item (any fingerprint), or null. */
    fun find(doc: ProjectDoc, result: CheckResult): Acknowledgement? = Acks.find(doc, result)

    /**
     * Acknowledges [result] with its registry kind. Types that cannot be acknowledged (`▲ 화자 없음`,
     * `▲ 보이스 비어 있음`, …) leave the doc unchanged.
     * - `◇ 근거 지워진 변화`: `이대로 두기` also sets the record `kept` (`domain:script` `Anchors.keepHere`).
     * - `◇ 지운 …`: `이대로 두기` on the item (`domain:trash` `Fixes.keep`); dotted marks stay.
     */
    fun acknowledge(doc: ProjectDoc, result: CheckResult, ctx: EditContext): ProjectDoc {
        if (!result.ackable) return doc
        return when (result.checkType) {
            CheckType.CHANGE_ANCHOR_DELETED -> keepRecord(doc, result.subjectKeys.single(), ctx)
            CheckType.DELETED_ITEM -> Fixes.keep(doc, result.subjectKeys.first(), trashContext(ctx), result.subjectKeys.getOrNull(1)).doc
            else -> Acks.acknowledge(doc, result, ctx)
        }
    }

    /**
     * `이대로 두기` on a `근거 지워짐` record: `Anchors.keepHere` marks it `kept` and forgets the
     * pre-relocation anchor; the acknowledgement keeps that anchor in its `ext` so `되돌리기` can
     * put the record back exactly.
     */
    private fun keepRecord(doc: ProjectDoc, recordKey: Key, ctx: EditContext): ProjectDoc {
        val original = doc.index.record(recordKey)?.originalAnchor
        val kept = Anchors.keepHere(doc, recordKey, scriptContext(ctx))
        if (original == null) return kept
        val ext = buildJsonObject { put(EXT_ORIGINAL_ANCHOR, CivilightJson.encodeElement(AnchorSerializer, original)) }
        return kept.copy(
            checks = kept.checks.copy(
                acknowledgements = kept.checks.acknowledgements.map { a ->
                    if (a.checkType == CheckType.CHANGE_ANCHOR_DELETED && a.subjectKeys == listOf(recordKey)) a.copy(ext = ext) else a
                },
            ),
        )
    }

    /**
     * `되돌리기` in `맞다고 표시한 것`: deletes the acknowledgement so the item is open again. A kept
     * record goes back to `근거 지워짐` with the anchor it had before relocation (from the
     * acknowledgement; its current anchor when the acknowledgement was written elsewhere without it),
     * so `originalAnchor` stays non-null while orphaned.
     */
    fun revert(doc: ProjectDoc, ackKey: Key, ctx: EditContext): ProjectDoc {
        val ack = doc.index.acknowledgement(ackKey) ?: return doc
        val reverted = Acks.revert(doc, ackKey)
        if (ack.checkType != CheckType.CHANGE_ANCHOR_DELETED) return reverted
        val stored = ack.ext?.get(EXT_ORIGINAL_ANCHOR)?.let { CivilightJson.decodeElement(AnchorSerializer, it) }
        val recordKeys = ack.subjectKeys.toSet()
        return reverted.copy(
            changes = reverted.changes.copy(
                records = reverted.changes.records.map { r ->
                    if (r.key in recordKeys && r.anchorState.known == AnchorState.KEPT) {
                        r.copy(
                            anchorState = AnchorState.ORPHANED.open(), originalAnchor = stored ?: r.anchor,
                            updatedAt = ctx.now, updatedBy = ctx.device,
                        )
                    } else {
                        r
                    }
                },
            ),
        )
    }

    /** `ext` key of a keep acknowledgement holding the record's pre-relocation anchor. */
    const val EXT_ORIGINAL_ANCHOR = "originalAnchor"

    /**
     * Stored acknowledgements that are void now: an item with the same type and subject exists but
     * its fingerprint differs (STO-41). Acknowledgements whose item does not fire in [report] are not
     * judged (a switched-off or partial evaluation never voids anything), and `deleted.item` ones are
     * exempt (they are about the deleted item and stay while it is referenced, §2.9).
     */
    fun voided(report: CheckReport): List<Acknowledgement> {
        val current = report.results.groupBy { it.checkType to it.subjectKeys }
        return report.doc.checks.acknowledgements.filter { ack ->
            if (ack.checkType == CheckType.DELETED_ITEM) return@filter false
            val items = current[ack.checkType to ack.subjectKeys] ?: return@filter false
            items.none { it.fingerprint == ack.fingerprint }
        }
    }

    /** Deletes the [voided] acknowledgements ("deleted on next evaluation", §2.9). */
    fun prune(report: CheckReport): ProjectDoc {
        val gone = voided(report).map { it.key }.toSet()
        if (gone.isEmpty()) return report.doc
        val doc = report.doc
        return doc.copy(checks = doc.checks.copy(acknowledgements = doc.checks.acknowledgements.filterNot { it.key in gone }))
    }

    /** Acknowledgements go with a deleted subject; undo restores the previous doc and so the ack (CHK-05). */
    fun dropForSubjects(doc: ProjectDoc, deletedKeys: Collection<Key>): ProjectDoc = Acks.dropForSubjects(doc, deletedKeys)

    private fun scriptContext(ctx: EditContext) = ScriptEditContext(now = ctx.now, zone = ctx.zone, device = ctx.device, newKey = ctx.newKey)

    private fun trashContext(ctx: EditContext) = TrashContext(now = ctx.now, zone = ctx.zone, deviceId = ctx.device, newKey = ctx.newKey)
}
