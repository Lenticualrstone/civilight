package io.github.lenticualrstone.civilight.feature.script.change

import io.github.lenticualrstone.civilight.domain.cast.DisplayNames
import io.github.lenticualrstone.civilight.domain.cast.field.FieldReader
import io.github.lenticualrstone.civilight.domain.cast.field.FieldValueEdits
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationDrafts
import io.github.lenticualrstone.civilight.domain.cast.timed.TimedValues
import io.github.lenticualrstone.civilight.domain.world.term.RevealEdit
import io.github.lenticualrstone.civilight.domain.world.term.RevealTarget
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import kotlinx.serialization.builtins.serializer
import io.github.lenticualrstone.civilight.domain.cast.EditContext as CastContext
import io.github.lenticualrstone.civilight.domain.world.common.EditContext as WorldContext

/**
 * `기록` of the `변화 기록` sheet (SCR-41, SCR-45): one block-seat record per chosen thing, applying
 * from the block inclusive, written through the owning areas' write paths:
 * - 관계: `domain:cast` `RelationDrafts.recordAtBlock` (CST-30; the pair's relation is created when missing);
 * - 필드 and `표시 이름`: `domain:cast` `TimedValues.addRecord` (`change sheet 기록`);
 * - 용어 공개: `domain:world` `TermEdits.setReveal` (one live reveal per term, moved when it exists; the term card uses the same).
 */
object ChangeCommit {
    fun record(doc: ProjectDoc, draft: ChangeDraft, cast: CastContext, world: WorldContext): ProjectDoc? {
        if (!doc.manuscript.isLiveBlock(draft.blockKey) || !ChangeModel.canRecord(doc, draft)) return null
        return when (draft.segment) {
            ChangeSegment.RELATION -> relation(doc, draft, cast)
            ChangeSegment.FIELD -> field(doc, draft, cast)
            ChangeSegment.TERM -> term(doc, draft, world)
        }
    }

    private fun relation(doc: ProjectDoc, draft: ChangeDraft, ctx: CastContext): ProjectDoc? {
        val viewer = draft.viewerKey ?: return null
        val partner = draft.partnerKey ?: return null
        val drafts = draft.slots.associateWith { draft.slotDrafts[it] ?: return null }
        val (out, records) = RelationDrafts.recordAtBlock(doc, draft.blockKey, viewer, partner, drafts, ctx)
        return out.takeIf { records.isNotEmpty() }
    }

    private fun field(doc: ProjectDoc, draft: ChangeDraft, ctx: CastContext): ProjectDoc? {
        val owner = draft.ownerKey ?: return null
        val anchor = Anchor.AtBlock(draft.blockKey)
        if (draft.fieldKey == ChangeDraft.DISPLAY_NAME) {
            // Blank = the name shows from here (the reveal); text = the name shown from here (`???`).
            val text = draft.fieldText.trim().nfc().takeIf { it.isNotEmpty() }
            val value = text?.let { ChangeRecord.encodeValue(String.serializer(), it) }
            return TimedValues.addRecord(doc, DisplayNames.target(owner), value, anchor, ctx).first
        }
        val def = ChangeModel.fieldDef(doc, draft) ?: return null
        val payload = when (def.type.known) {
            FieldType.CHOICE -> FieldPayload.Choice(optionKeys = listOfNotNull(draft.fieldOption))
            FieldType.NUMBER -> FieldPayload.Number(draft.fieldText.trim())
            FieldType.SHORT_TEXT -> FieldPayload.Texts(listOf(draft.fieldText.trim().nfc()))
            else -> return null
        }
        val target = FieldReader.target(EntityKind.CHARACTER, owner, def)
        return TimedValues.addRecord(doc, target, FieldValueEdits.encode(FieldValue.of(payload)), anchor, ctx).first
    }

    private fun term(doc: ProjectDoc, draft: ChangeDraft, ctx: WorldContext): ProjectDoc? {
        val term = draft.termKey ?: return null
        return when (val r = TermEdits.setReveal(doc, term, RevealTarget.Block(draft.blockKey), ctx)) {
            is RevealEdit.Applied -> r.doc
            is RevealEdit.Refused -> null
        }
    }
}
