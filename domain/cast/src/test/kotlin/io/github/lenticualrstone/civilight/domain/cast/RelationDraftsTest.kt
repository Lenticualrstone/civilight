package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.cast.map.RelationMap
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationDrafts
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationEdits
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.domain.cast.relation.SheetSlot
import io.github.lenticualrstone.civilight.domain.cast.relation.SlotDraft
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** The shared relation-sheet pieces (CST-29, CST-30). */
class RelationDraftsTest {
    private val doc = Fx.doc()

    @Test
    fun `CST-29 a pair kind gives one chip per role of the partner`() {
        val chips = RelationDrafts.kindChips(doc, SheetSlot.BETWEEN).filter { it.kindKey == K.KIND_SENIOR_JUNIOR }
        assertEquals(listOf("선배" to true, "후배" to false), chips.map { it.label to it.partnerHoldsFirst })
        assertTrue(RelationDrafts.kindChips(doc, SheetSlot.ADDRESS).isEmpty())
        assertEquals("경계", RelationDrafts.kindChips(doc, SheetSlot.VIEWER_MIND).single { it.kindKey == K.KIND_WARINESS }.label)
    }

    @Test
    fun `TC-13-02 picking 선배 for 세라 in 리엔's sheet makes 세라 the 선배 from both sides`() {
        val fresh = Fx.withoutRelationRecords(doc, K.REL_RIEN_SERA).let { d ->
            RelationEdits.setBase(d, K.REL_RIEN_SERA, RelationSlotName.BETWEEN, io.github.lenticualrstone.civilight.domain.cast.relation.SlotInput.Kind(null), Fx.ctx())
        }
        val relation = fresh.index.relation(K.REL_RIEN_SERA)!!
        val draft = SlotDraft(kindKey = K.KIND_SENIOR_JUNIOR, partnerHoldsFirst = true)
        val input = RelationDrafts.input(fresh, relation, K.RIEN, K.SERA, SheetSlot.BETWEEN, draft)
        val saved = RelationEdits.setBase(fresh, relation.key, RelationSlotName.BETWEEN, input, Fx.ctx())
        assertEquals("세라 · 선배", RelationViews.view(saved, K.RIEN, K.SERA, Fx.LATEST)!!.title("세라"))
        assertEquals("리엔 · 후배", RelationViews.view(saved, K.SERA, K.RIEN, Fx.LATEST)!!.title("리엔"))
        // The draft read back from 리엔's side picks the same chip.
        val back = RelationDrafts.draftOf(saved, saved.index.relation(relation.key), K.RIEN, SheetSlot.BETWEEN, ReadPoint.LATEST)
        assertEquals(draft, back)
        assertEquals("선배", RelationDrafts.valueText(saved, relation, K.RIEN, SheetSlot.BETWEEN, back))
    }

    @Test
    fun `TC-13-04 the viewer's mind is written to the viewer's side only`() {
        val relation = doc.index.relation(K.REL_RIEN_HART)!!
        val side = RelationDrafts.slotName(relation, K.HART, SheetSlot.VIEWER_MIND)
        assertEquals(RelationReader.mindSlot(RelationReader.sideOf(relation, K.HART)!!), side)
        // A pair with no relation yet: the viewer becomes end A.
        assertEquals(RelationSlotName.MIND_A, RelationDrafts.slotName(null, K.SERA, SheetSlot.VIEWER_MIND))
        assertEquals(RelationSlotName.ADDRESS_A, RelationDrafts.slotName(null, K.SERA, SheetSlot.ADDRESS))
    }

    @Test
    fun `nothing chosen and nothing typed clears the slot, a changed draft is a change`() {
        val relation = doc.index.relation(K.REL_RIEN_HART)!!
        val current = RelationDrafts.draftOf(doc, relation, K.RIEN, SheetSlot.VIEWER_MIND, ReadPoint.LATEST)
        assertEquals(K.KIND_WARINESS, current.kindKey)
        assertTrue(!RelationDrafts.changes(doc, relation, K.RIEN, K.HART, SheetSlot.VIEWER_MIND, current, current))
        assertTrue(RelationDrafts.changes(doc, relation, K.RIEN, K.HART, SheetSlot.VIEWER_MIND, current.copy(kindKey = K.KIND_TRUST), current))
        assertNull(RelationEdits.encode(RelationDrafts.input(doc, relation, K.RIEN, K.HART, SheetSlot.VIEWER_MIND, SlotDraft())))
    }

    @Test
    fun `CST-30 the change segment reads the value just before the block`() {
        val fresh = Fx.withoutRelationRecords(doc, K.REL_RIEN_HART)
        val view = RelationDrafts.changeView(fresh, K.BLOCK_043, K.RIEN, K.HART, setOf(SheetSlot.VIEWER_MIND))!!
        assertEquals("변화 기록", view.title)
        assertEquals("#043 · 1-7 씬 3", view.sub)
        assertEquals(listOf("리엔의 마음", "하르트의 마음", "사이", "호칭"), view.slotChips.map { it.second })
        assertEquals("값 · 만나기 전 →", view.sections.single().valueLabel)
        assertEquals("시작 시점" to "이 블록 · 1-7 #043", view.startRow)
        assertEquals("반영" to "인물 · 관계도 · 시점 렌즈", view.reflectRow)
        assertEquals("기록", view.primaryLabel)
    }

    @Test
    fun `TC-13-08 two chips at a block make two records anchored there`() {
        val fresh = Fx.withoutRelationRecords(doc, K.REL_RIEN_HART)
        val (next, made) = RelationDrafts.recordAtBlock(
            fresh, K.BLOCK_043, K.RIEN, K.HART,
            mapOf(
                SheetSlot.VIEWER_MIND to SlotDraft(kindKey = K.KIND_WARINESS),
                SheetSlot.PARTNER_MIND to SlotDraft(kindKey = K.KIND_PROTECTION),
            ),
            Fx.ctx(),
        )
        assertEquals(2, made.size)
        assertTrue(made.all { it.anchor.sameAs(Anchor.AtBlock(K.BLOCK_043)) })
        assertEquals("→ 경계 · ← 보호", RelationViews.summary(next, K.RIEN, K.HART, Fx.L1_7))
    }

    @Test
    fun `TC-13-19 무관심 from 1-7 draws a line at 1-7 and none at 1-6`() {
        assertNull(RelationReader.between(doc, K.RIEN, K.MARTHA))
        val (next, made) = RelationDrafts.recordAtBlock(
            doc, K.BLOCK_043, K.RIEN, K.MARTHA, mapOf(SheetSlot.VIEWER_MIND to SlotDraft(kindKey = K.KIND_INDIFFERENCE)), Fx.ctx(),
        )
        val relation = RelationReader.between(next, K.RIEN, K.MARTHA)!!
        assertEquals(relation.key, (made.single().target as ChangeTarget.RelationSlot).relationKey)
        assertTrue(RelationMap.build(next, Fx.L1_7).lines.any { it.relationKey == relation.key })
        assertTrue(RelationMap.build(next, Fx.L1_6).lines.none { it.relationKey == relation.key })
        assertEquals("만나기 전", RelationViews.summary(next, K.RIEN, K.MARTHA, Fx.L1_6))
    }

    @Test
    fun `a new 사이 kind is added as 대칭 and an existing name is reused`() {
        val (next, chip) = RelationDrafts.addKind(doc, SheetSlot.BETWEEN, "소꿉친구", Fx.ctx())!!
        assertEquals("소꿉친구", chip.label)
        assertTrue(RelationDrafts.kindChips(next, SheetSlot.BETWEEN).any { it.kindKey == chip.kindKey })
        val (same, existing) = RelationDrafts.addKind(doc, SheetSlot.VIEWER_MIND, "경계", Fx.ctx())!!
        assertEquals(doc, same)
        assertEquals(K.KIND_WARINESS, existing.kindKey)
    }
}
