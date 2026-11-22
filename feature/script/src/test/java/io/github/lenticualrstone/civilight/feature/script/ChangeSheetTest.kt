package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.cast.relation.SheetSlot
import io.github.lenticualrstone.civilight.domain.cast.relation.SlotDraft
import io.github.lenticualrstone.civilight.domain.script.Records
import io.github.lenticualrstone.civilight.feature.script.change.ChangeCommit
import io.github.lenticualrstone.civilight.feature.script.change.ChangeDraft
import io.github.lenticualrstone.civilight.feature.script.change.ChangeModel
import io.github.lenticualrstone.civilight.feature.script.change.ChangeSegment
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.domain.cast.EditContext as CastContext
import io.github.lenticualrstone.civilight.domain.world.common.EditContext as WorldContext
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** `◆ 변화 기록` from a block (SCR-41) and `◇ 첫 공개` set there (SCR-45), on the example project. */
class ChangeSheetTest {
    private val doc = Fx.example

    private fun cast(): CastContext {
        var n = 0
        return CastContext(newKey = { "01JZ000000CAST" + (++n).toString().padStart(12, '0') }, now = ExampleClock.NOW, device = "test", zone = Fx.zone)
    }

    private fun world(): WorldContext {
        var n = 0
        return WorldContext(newKey = { "01JZ000000WRLD" + (++n).toString().padStart(12, '0') }, now = { ExampleClock.NOW }, device = "test")
    }

    private fun record(d: ProjectDoc, draft: ChangeDraft) = ChangeCommit.record(d, draft, cast(), world())

    @Test
    fun `SCR-41 the sheet opens on 관계 with the speaker as one side and 인물 고르기 as the other`() {
        val draft = ChangeDraft.start(doc, K.BLOCK_043)
        assertEquals(K.HART, draft.partnerKey)
        assertNull(draft.viewerKey)
        val model = ChangeModel.of(doc, draft)!!
        assertEquals("변화 기록", model.title)
        assertEquals("취소", model.cancel)
        assertEquals("#043 · 1-7 씬 3", model.sub)
        assertEquals(ChangeSegment.RELATION, model.segment)
        assertEquals("인물 고르기", model.relation!!.viewer)
        assertEquals("하르트", model.relation!!.partner)
        assertEquals("시작 시점" to "이 블록 · 1-7 #043", model.startRow)
        assertEquals(listOf("관계", "필드", "용어 공개"), ChangeSegment.entries.map { it.label })
        assertFalse(model.canRecord)
    }

    @Test
    fun `SCR-41 관계 reads the value just before the block and 기록 adds a block-seat record there`() {
        val draft = ChangeDraft.start(doc, K.BLOCK_043).copy(viewerKey = K.RIEN)
        val relation = ChangeModel.of(doc, draft)!!.relation!!
        assertEquals(listOf("리엔의 마음", "하르트의 마음", "사이", "호칭"), relation.slotChips.map { it.label })
        assertEquals(listOf(true, false, false, false), relation.slotChips.map { it.selected })
        val section = relation.sections.single()
        assertEquals("값 · 만나기 전 →", section.valueLabel)
        val trust = section.chips.first { it.kindKey == K.KIND_TRUST }
        assertEquals("반영" to "인물 · 관계도 · 시점 렌즈", relation.reflectRow)

        val ready = draft.copy(slotDrafts = mapOf(SheetSlot.VIEWER_MIND to SlotDraft(kindKey = trust.kindKey)))
        assertTrue(ChangeModel.canRecord(doc, ready))
        val before = Records.marksOn(doc, K.BLOCK_043).size
        val out = record(doc, ready)!!
        val added = out.changes.records.filter { it.key !in doc.changes.records.map { r -> r.key }.toSet() }
        assertEquals(1, added.size)
        assertEquals(Anchor.AtBlock(K.BLOCK_043), added.single().anchor)
        assertTrue(added.single().target is ChangeTarget.RelationSlot)
        assertEquals(before + 1, Records.marksOn(out, K.BLOCK_043).size)
    }

    @Test
    fun `SCR-41 호칭 takes typed text, and a choice block anchors the whole block (T-12-16)`() {
        val address = ChangeDraft.start(doc, K.BLOCK_043).copy(
            viewerKey = K.RIEN, slots = listOf(SheetSlot.ADDRESS), slotDrafts = mapOf(SheetSlot.ADDRESS to SlotDraft(address = "하르트 씨")),
        )
        val out = record(doc, address)!!
        assertTrue(out.changes.records.size == doc.changes.records.size + 1)

        val choice = ChangeDraft.start(doc, K.BLOCK_1_5_CHOICE).copy(viewerKey = K.RIEN, partnerKey = K.HART)
        val chip = ChangeModel.of(doc, choice)!!.relation!!.sections.single().chips.first()
        val onChoice = record(doc, choice.copy(slotDrafts = mapOf(SheetSlot.VIEWER_MIND to SlotDraft(kindKey = chip.kindKey))))!!
        assertEquals(Anchor.AtBlock(K.BLOCK_1_5_CHOICE), onChoice.changes.records.last().anchor)
    }

    @Test
    fun `SCR-45 용어 공개 moves the term's one reveal to this block and says where from`() {
        val draft = ChangeDraft.start(doc, K.BLOCK_043).copy(segment = ChangeSegment.TERM, termKey = K.TERM_BLACKOUT)
        val term = ChangeModel.of(doc, draft)!!.term!!
        assertEquals(doc.index.term(K.TERM_BLACKOUT)!!.name, term.term)
        val preview = term.movePreview
        assertNotNull(preview)
        assertTrue(preview!!.startsWith("첫 공개 ") && preview.endsWith(" → 1-7 #043"))
        val out = record(doc, draft)!!
        val reveal = Records.existingReveal(out, K.TERM_BLACKOUT)!!
        assertEquals(K.REVEAL_BLACKOUT, reveal.key)
        assertEquals(Anchor.AtBlock(K.BLOCK_043), reveal.anchor)
        // The term already revealed on this block: nothing to record.
        val same = ChangeDraft.start(doc, K.BLOCK_044).copy(segment = ChangeSegment.TERM, termKey = K.TERM_ORIGINIUM_LANTERN)
        assertNull(ChangeModel.of(doc, same)!!.term!!.movePreview)
        assertFalse(ChangeModel.canRecord(doc, same))
        assertEquals("용어 고르기", ChangeModel.of(doc, ChangeDraft(K.BLOCK_043, ChangeSegment.TERM))!!.term!!.term)
    }

    @Test
    fun `SCR-41 필드 lists 표시 이름 and the editable fields, and records a value from this block`() {
        val draft = ChangeDraft.start(doc, K.BLOCK_043).copy(segment = ChangeSegment.FIELD)
        val field = ChangeModel.of(doc, draft)!!.field!!
        assertEquals("하르트", field.owner)
        assertEquals("표시 이름", field.fields.first().label)
        val defs = doc.index.fieldDefs(io.github.lenticualrstone.civilight.model.cast.EntityKind.CHARACTER)
        assertTrue(field.fields.drop(1).all { f -> defs.first { it.key == f.key }.type.known in ChangeModel.EDITABLE_TYPES })

        val name = draft.copy(fieldKey = ChangeDraft.DISPLAY_NAME)
        assertEquals("값 · ??? →", ChangeModel.of(doc, name)!!.field!!.valueLabel)
        assertTrue(ChangeModel.canRecord(doc, name))
        val revealed = record(doc, name)!!
        val added = revealed.changes.records.last()
        assertEquals(ChangeTarget.DisplayName(K.HART), added.target)
        assertEquals(Anchor.AtBlock(K.BLOCK_043), added.anchor)
        assertNull(added.value)

        val choiceDef = defs.first { it.type.known == FieldType.CHOICE && it.scopeCharacterKey == null && !it.definitionHidden && it.deletedAt == null }
        val pick = draft.copy(fieldKey = choiceDef.key)
        val model = ChangeModel.of(doc, pick)!!.field!!
        assertEquals(choiceDef.options!!.size, model.options!!.size)
        assertFalse(ChangeModel.canRecord(doc, pick))
        val out = record(doc, pick.copy(fieldOption = model.options!!.first().first))!!
        assertTrue(out.changes.records.last().target is ChangeTarget.Field)
    }
}
