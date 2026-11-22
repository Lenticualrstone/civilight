package io.github.lenticualrstone.civilight.domain.cast.relation

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint

/**
 * The line-label sheet of a line between two characters that has no canvas edge: a line drawn on
 * the full relationship map (CST-33, CST-34; `LineLabelSheet.forCharacters`). Same rules as
 * [CanvasRelations]: the line becomes a relation only by `관계로 만들기`; picking chips records
 * nothing; the drawn direction preselects `<drawn-from>의 마음`; the value starts at the start of the
 * chosen unit (a point anchor).
 */
object PairLines {
    /** The sheet for a line drawn from [fromKey] to [toKey]; null unless both are characters and differ. */
    fun sheet(doc: ProjectDoc, fromKey: Key, toKey: Key, lens: LensPoint): LineLabelSheet? {
        if (fromKey == toKey) return null
        val from = doc.index.character(fromKey)?.takeIf { it.deletedAt == null } ?: return null
        val to = doc.index.character(toKey)?.takeIf { it.deletedAt == null } ?: return null
        val unit = CanvasRelations.startUnit(doc, lens)
        return LineLabelSheet(
            title = CanvasRelations.TITLE,
            sub = "${from.name} · ${to.name}",
            slotChips = listOf("사이", RelationDrafts.mindLabel(from.name), RelationDrafts.mindLabel(to.name)),
            preselected = LineSlot.FROM_MIND,
            kindChips = RelationDrafts.kindChips(doc, sheetSlot(LineSlot.FROM_MIND)).map { it.label },
            startPoint = unit?.let { io.github.lenticualrstone.civilight.domain.cast.StoryLabels.unitCode(doc, it) }
                ?: io.github.lenticualrstone.civilight.domain.cast.StoryLabels.lensPoint(doc, lens),
            startUnitKey = unit,
            canMakeRelation = true,
            keepLabel = CanvasRelations.KEEP,
            makeLabel = CanvasRelations.MAKE,
        )
    }

    /** The relation sheet's slot for a line slot, read from the drawn-from end. */
    fun sheetSlot(slot: LineSlot): SheetSlot = when (slot) {
        LineSlot.BETWEEN -> SheetSlot.BETWEEN
        LineSlot.FROM_MIND -> SheetSlot.VIEWER_MIND
        LineSlot.TO_MIND -> SheetSlot.PARTNER_MIND
    }

    /** Kind chips of a line slot (a pair kind gives both role names, as in the relation sheet). */
    fun kindChips(doc: ProjectDoc, slot: LineSlot): List<KindChip> = RelationDrafts.kindChips(doc, sheetSlot(slot))

    /**
     * `관계로 만들기`: opens (or creates) the pair's relation and records [chip]'s kind in [slot] from the
     * start of [startUnitKey]. For a pair kind, [KindChip.partnerHoldsFirst] says whether the drawn-to
     * end holds the kind's first name (`선배`).
     */
    fun makeRelation(
        doc: ProjectDoc,
        fromKey: Key,
        toKey: Key,
        slot: LineSlot,
        chip: KindChip,
        startUnitKey: Key,
        ctx: EditContext,
        description: String? = null,
    ): ProjectDoc {
        val (withRelation, relation) = RelationEdits.openOrCreate(doc, fromKey, toKey, ctx)
        val kind = requireNotNull(withRelation.index.relationKind(chip.kindKey)) { "no kind ${chip.kindKey}" }
        val (relSlot, value) = when (slot) {
            LineSlot.BETWEEN -> {
                val holder = when (chip.partnerHoldsFirst) {
                    true -> toKey
                    false -> fromKey
                    null -> null
                }
                RelationSlotName.BETWEEN to RelationEdits.betweenValue(relation, kind, holder, description)
            }
            LineSlot.FROM_MIND -> RelationReader.mindSlot(RelationReader.sideOf(relation, fromKey)!!) to RelationEdits.mindValue(kind, description)
            LineSlot.TO_MIND -> RelationReader.mindSlot(RelationReader.sideOf(relation, toKey)!!) to RelationEdits.mindValue(kind, description)
        }
        return RelationEdits.record(withRelation, relation.key, mapOf(relSlot to SlotInput.Kind(value)), Anchor.UnitStart(startUnitKey), ctx).first
    }
}
