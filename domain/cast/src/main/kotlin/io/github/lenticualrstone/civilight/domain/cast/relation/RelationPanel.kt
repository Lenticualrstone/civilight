package io.github.lenticualrstone.civilight.domain.cast.relation

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.ItemRefs
import io.github.lenticualrstone.civilight.domain.cast.ResolvedItem
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.domain.cast.timed.recordsHere
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** One side's mind in the script panel: the value before the block and, if the block changes it, the new one. */
data class PanelSide(val before: String, val here: String?) {
    /** `만나기 전 → 보호 ◆ 여기서` when changed here; else the value before the block. */
    val text: String get() = if (here != null) "$before → $here ${CastCopy.HERE}" else before
}

/**
 * `관계 · 지금` row of the script `참고` panel for the speaker (CST-38, rule 11 §2): the state just
 * before the block plus that block's own records as `여기서`.
 */
data class PanelRelationRow(
    val relationKey: Key,
    val partnerKey: Key,
    val partnerName: String,
    /** No value at all before the block (`만나기 전`). */
    val notMetBefore: Boolean,
    val own: PanelSide,
    val other: PanelSide,
    /** `사이 동료`, or null. */
    val between: String?,
    /** The speaker's 호칭 for the partner before the block. */
    val address: String?,
) {
    val changedHere: Boolean get() = own.here != null || other.here != null

    /**
     * Main line: `리엔 · → 보호 · 호칭 ‘꼬마’`; `하르트 · 만나기 전`; with a change at this block
     * the speaker's side as `만나기 전 → 보호 ◆ 여기서`.
     */
    val line: String
        get() = when {
            own.here != null -> own.text
            notMetBefore -> CastCopy.join(partnerName, CastCopy.NOT_MET)
            else -> CastCopy.joinAll(
                listOf(partnerName, between, own.before.takeIf { it != CastCopy.NOT_WRITTEN }?.let { "→ $it" }, address?.let { "호칭 ‘$it’" }),
            )
        }

    /** Partner's side on the smaller line below: `← 경계`, or `만나기 전 → 경계 ◆ 여기서`. */
    val otherLine: String?
        get() = when {
            other.here != null -> other.text
            notMetBefore || other.before == CastCopy.NOT_WRITTEN -> null
            else -> "← ${other.before}"
        }
}

/** Relation rows of the script panel. */
object RelationPanel {
    fun rows(doc: ProjectDoc, speakerKey: Key, blockKey: Key, hiddenUnlocked: Boolean = false): List<PanelRelationRow> =
        RelationViews.relationsOf(doc, speakerKey).mapNotNull { relation ->
            if (!doc.liveness.isLive(relation)) return@mapNotNull null
            val side = RelationReader.sideOf(relation, speakerKey) ?: return@mapNotNull null
            val partnerKey = RelationReader.endKey(relation, side.other)
            val partner = ItemRefs.character(doc, partnerKey, hiddenUnlocked) as? ResolvedItem.Live ?: return@mapNotNull null
            val before = RelationReader.state(doc, relation, ReadPoint.BeforeBlock(blockKey))
            val here = RelationReader.SLOTS.associateWith { slot ->
                doc.recordsHere(RelationReader.target(relation.key, slot), blockKey).lastOrNull()
            }
            fun mindWord(value: io.github.lenticualrstone.civilight.model.cast.SlotValue?): String =
                when {
                    before.notMet -> CastCopy.NOT_MET
                    else -> RelationReader.mindText(doc, value)?.word ?: CastCopy.NOT_WRITTEN
                }
            fun hereWord(record: ChangeRecord?): String? =
                record?.let { RelationReader.mindText(doc, RelationReader.slotValueOf(it))?.word ?: CastCopy.NOT_WRITTEN }
            PanelRelationRow(
                relationKey = relation.key,
                partnerKey = partnerKey,
                partnerName = partner.name,
                notMetBefore = before.notMet,
                own = PanelSide(mindWord(before.mind(side)), hereWord(here[RelationReader.mindSlot(side)])),
                other = PanelSide(mindWord(before.mind(side.other)), hereWord(here[RelationReader.mindSlot(side.other)])),
                between = before.between?.let { "사이 ${RelationReader.roleOf(doc, it, side.other)}" },
                address = before.address(side),
            )
        }.sortedBy { row -> doc.index.character(row.partnerKey)?.order }

    /** The row with [partnerKey], or null. */
    fun row(doc: ProjectDoc, speakerKey: Key, partnerKey: Key, blockKey: Key): PanelRelationRow? =
        rows(doc, speakerKey, blockKey, hiddenUnlocked = true).firstOrNull { it.partnerKey == partnerKey }
}
