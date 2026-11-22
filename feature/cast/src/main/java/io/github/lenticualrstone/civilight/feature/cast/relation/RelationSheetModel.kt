package io.github.lenticualrstone.civilight.feature.cast.relation

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.relation.CanvasRelations
import io.github.lenticualrstone.civilight.domain.cast.relation.KindChip
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationDrafts
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationEdits
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader
import io.github.lenticualrstone.civilight.domain.cast.relation.SheetSlot
import io.github.lenticualrstone.civilight.domain.cast.relation.SlotDraft
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.domain.cast.timed.TimedValues
import io.github.lenticualrstone.civilight.domain.story.lens.EditChoice
import io.github.lenticualrstone.civilight.domain.story.lens.LensEdit
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.field.EditMode
import io.github.lenticualrstone.civilight.feature.cast.field.SaveChoice
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.ui.labels.UnitLabels

/** How the relation sheet saves the chosen slot (CST-29). */
sealed interface RelationSave {
    /**
     * The slot holds nothing yet: one primary that writes a `◆` from the `시작 시점` (rule 12 §1
     * point anchor). `관계로 만들기` for a new pair, `기록` for an empty slot of an existing one.
     */
    data class FromStart(val label: String) : RelationSave

    /** The slot has a value: `이 값 고치기`, or the two buttons of CST-10 when it has history. */
    data class Edit(val mode: EditMode) : RelationSave
}

/** The relation sheet for one pair and one slot (S16). */
data class RelationSheetView(
    /** `리엔 · 세라`. */
    val title: String,
    val viewerName: String,
    val partnerName: String,
    /** `사이 · 리엔의 마음 · 세라의 마음 · 호칭`. */
    val slots: List<Pair<SheetSlot, String>>,
    /** The value in effect at the lens: `만나기 전` before the pair's first value, `비어 있음` for an empty slot. */
    val current: String,
    val chips: List<KindChip>,
    val save: RelationSave,
    /** `1-7`: the unit the new `◆` starts at (only for [RelationSave.FromStart]). */
    val startLabel: String?,
    val relationKey: Key?,
)

/**
 * Pure model of `관계 만들기 · 고치기` (CST-29). One relation per pair (TC-13-10): an existing pair
 * opens its relation. Pair kinds fill the other side as a fact; minds are written to one side only.
 * The one-or-two buttons follow `domain:story` [LensEdit.choice] like every timed value.
 */
object RelationSheetModel {
    /** The default `시작 시점`: the lens episode (`최신` = the 집필 선 episode). */
    fun defaultStart(doc: ProjectDoc, lens: LensPoint): Key? = CanvasRelations.startUnit(doc, lens)

    fun relation(doc: ProjectDoc, viewerKey: Key, partnerKey: Key): Relation? = RelationReader.between(doc, viewerKey, partnerKey)

    /** The draft the sheet opens with for [slot]: the value in effect at the lens. */
    fun initialDraft(doc: ProjectDoc, viewerKey: Key, partnerKey: Key, slot: SheetSlot, lens: LensPoint): SlotDraft =
        RelationDrafts.draftOf(doc, relation(doc, viewerKey, partnerKey), viewerKey, slot, ReadPoint.AtLens(lens))

    fun view(doc: ProjectDoc, viewerKey: Key, partnerKey: Key, slot: SheetSlot, startUnitKey: Key?, lens: LensPoint): RelationSheetView? {
        if (viewerKey == partnerKey) return null
        val viewer = doc.index.character(viewerKey) ?: return null
        val partner = doc.index.character(partnerKey) ?: return null
        val relation = relation(doc, viewerKey, partnerKey)
        val point = ReadPoint.AtLens(lens)
        val draft = RelationDrafts.draftOf(doc, relation, viewerKey, slot, point)
        val text = RelationDrafts.valueText(doc, relation, viewerKey, slot, draft)
        val current = when {
            relation == null || RelationReader.state(doc, relation, point).notMet -> CastCopy.NOT_MET
            text.isEmpty() -> CastUiCopy.EMPTY_VALUE
            else -> text
        }
        val save = save(doc, relation, viewerKey, slot, lens, current)
        return RelationSheetView(
            title = CastCopy.join(viewer.name, partner.name),
            viewerName = viewer.name,
            partnerName = partner.name,
            slots = RelationDrafts.sheetSlots(viewer.name, partner.name),
            current = current,
            chips = RelationDrafts.kindChips(doc, slot),
            save = save,
            startLabel = if (save is RelationSave.FromStart) startUnitKey?.let { UnitLabels(doc).code(it) } else null,
            relationKey = relation?.key,
        )
    }

    private fun save(doc: ProjectDoc, relation: Relation?, viewerKey: Key, slot: SheetSlot, lens: LensPoint, current: String): RelationSave {
        relation ?: return RelationSave.FromStart(CastUiCopy.BTN_MAKE_RELATION)
        val target = RelationReader.target(relation.key, RelationDrafts.slotName(relation, viewerKey, slot))
        // An empty slot with no history starts now; anything else follows LensEdit like every timed value.
        if (!TimedValues.hasHistory(doc, target) && RelationDrafts.draftOf(doc, relation, viewerKey, slot, ReadPoint.LATEST) == SlotDraft()) {
            return RelationSave.FromStart(RelationDrafts.RECORD)
        }
        return when (LensEdit.choice(doc, target, lens)) {
            EditChoice.Plain -> RelationSave.Edit(EditMode.Plain)
            is EditChoice.TwoButtons -> RelationSave.Edit(
                EditMode.TwoButtons(TimedValues.editSheet(doc, target, lens, current) { RelationDrafts.recordText(doc, relation, viewerKey, it) }),
            )
        }
    }

    /** A save writes something: the draft differs from the value in effect, and a new pair needs a value. */
    fun canSave(doc: ProjectDoc, viewerKey: Key, partnerKey: Key, slot: SheetSlot, draft: SlotDraft, lens: LensPoint): Boolean {
        if (viewerKey == partnerKey) return false
        val relation = relation(doc, viewerKey, partnerKey)
        val current = initialDraft(doc, viewerKey, partnerKey, slot, lens)
        return RelationDrafts.changes(doc, relation, viewerKey, partnerKey, slot, draft, current)
    }

    /**
     * Saves [draft] into [slot] with the pressed button. The pair's relation is created here, inside
     * the save, so cancelling the sheet leaves no empty relation behind.
     */
    fun save(
        doc: ProjectDoc,
        viewerKey: Key,
        partnerKey: Key,
        slot: SheetSlot,
        draft: SlotDraft,
        choice: SaveChoice?,
        startUnitKey: Key?,
        lens: LensPoint,
        ctx: EditContext,
    ): ProjectDoc {
        if (viewerKey == partnerKey) return doc
        val existing = relation(doc, viewerKey, partnerKey)
        val (withRelation, relation) = RelationEdits.openOrCreate(doc, viewerKey, partnerKey, ctx)
        val slotName = RelationDrafts.slotName(relation, viewerKey, slot)
        val input = RelationDrafts.input(withRelation, existing ?: relation, viewerKey, partnerKey, slot, draft)
        return when (choice) {
            null -> if (startUnitKey == null) {
                RelationEdits.setBase(withRelation, relation.key, slotName, input, ctx)
            } else {
                RelationEdits.record(withRelation, relation.key, mapOf(slotName to input), Anchor.UnitStart(startUnitKey), ctx).first
            }
            SaveChoice.PLAIN -> RelationEdits.setBase(withRelation, relation.key, slotName, input, ctx)
            SaveChoice.CORRECT -> RelationEdits.correctInEffect(withRelation, relation.key, slotName, input, lens, ctx)
            SaveChoice.FROM_HERE -> RelationEdits.changeFromHere(withRelation, relation.key, slotName, input, lens, ctx)
        }
    }
}
