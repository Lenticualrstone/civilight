package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.domain.cast.relation.SheetSlot
import io.github.lenticualrstone.civilight.domain.cast.relation.SlotDraft
import io.github.lenticualrstone.civilight.feature.cast.relation.RelationSave
import io.github.lenticualrstone.civilight.feature.cast.field.EditMode
import io.github.lenticualrstone.civilight.domain.story.lens.EditChoice
import io.github.lenticualrstone.civilight.domain.story.lens.LensEdit
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.feature.cast.relation.RelationSheetModel
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** `관계 만들기 · 고치기` (CST-29). */
class RelationSheetModelTest {
    private val doc = CastFx.doc()
    private val latest = LensPoint.Latest

    @Test
    fun `TC-13-10 an existing pair opens its relation, not a second one`() {
        val view = RelationSheetModel.view(doc, K.RIEN, K.HART, SheetSlot.VIEWER_MIND, null, latest)!!
        assertEquals(K.REL_RIEN_HART, view.relationKey)
        assertEquals("리엔 · 하르트", view.title)
        assertEquals(listOf("사이", "리엔의 마음", "하르트의 마음", "호칭"), view.slots.map { it.second })
        assertTrue(view.save is RelationSave.Edit)
        assertEquals("경계", view.current.substringBefore(" · "))
    }

    @Test
    fun `a new pair starts 만나기 전 with 관계로 만들기 from the lens episode`() {
        assertNull(RelationReader.between(doc, K.RIEN, K.MARTHA))
        val start = RelationSheetModel.defaultStart(doc, latest)
        val view = RelationSheetModel.view(doc, K.RIEN, K.MARTHA, SheetSlot.VIEWER_MIND, start, latest)!!
        assertNull(view.relationKey)
        assertEquals("만나기 전", view.current)
        assertEquals(RelationSave.FromStart("관계로 만들기"), view.save)
        assertEquals("1-7", view.startLabel)
        assertFalse(RelationSheetModel.canSave(doc, K.RIEN, K.MARTHA, SheetSlot.VIEWER_MIND, SlotDraft(), latest))
        assertTrue(RelationSheetModel.canSave(doc, K.RIEN, K.MARTHA, SheetSlot.VIEWER_MIND, SlotDraft(kindKey = K.KIND_INDIFFERENCE), latest))
    }

    @Test
    fun `TC-13-19 관계로 만들기 with 무관심 from 1-7 shows from 1-7 only`() {
        val start = RelationSheetModel.defaultStart(doc, latest)
        val saved = RelationSheetModel.save(
            doc, K.RIEN, K.MARTHA, SheetSlot.VIEWER_MIND, SlotDraft(kindKey = K.KIND_INDIFFERENCE), null, start, latest, CastFx.ctx(),
        )
        assertEquals("→ 무관심", RelationViews.summary(saved, K.RIEN, K.MARTHA, LensPoint.UnitEnd(K.EPISODE_1_7)))
        assertEquals("만나기 전", RelationViews.summary(saved, K.RIEN, K.MARTHA, LensPoint.UnitEnd(K.EPISODE_1_6)))
        // TC-13-04: 마르타's mind stays empty.
        val relation = RelationReader.between(saved, K.RIEN, K.MARTHA)!!
        assertEquals(null, RelationViews.view(saved, K.MARTHA, K.RIEN, latest)!!.ownMind)
        assertEquals(relation.key, RelationSheetModel.view(saved, K.RIEN, K.MARTHA, SheetSlot.VIEWER_MIND, start, latest)!!.relationKey)
    }

    @Test
    fun `an empty slot of an existing pair records from the start, a filled one is corrected`() {
        val empty = RelationSheetModel.view(doc, K.RIEN, K.OWEN, SheetSlot.VIEWER_MIND, K.EPISODE_1_7, latest)!!
        assertEquals(RelationSave.FromStart("기록"), empty.save)
        assertEquals("비어 있음", empty.current)
        // 리엔 · 오웬's 사이 holds a base value: the buttons follow LensEdit at every lens.
        for (lens in listOf(latest, LensPoint.UnitEnd(K.EPISODE_1_6))) {
            val filled = RelationSheetModel.view(doc, K.RIEN, K.OWEN, SheetSlot.BETWEEN, K.EPISODE_1_7, lens)!!
            assertEquals("거래 상대", filled.current)
            val target = RelationReader.target(K.REL_RIEN_OWEN, RelationSlotName.BETWEEN)
            val expected = when (LensEdit.choice(doc, target, lens)) {
                EditChoice.Plain -> EditMode.Plain::class
                is EditChoice.TwoButtons -> EditMode.TwoButtons::class
            }
            assertEquals(expected, ((filled.save as RelationSave.Edit).mode)::class)
            assertNull(filled.startLabel)
        }
    }

    @Test
    fun `a character cannot have a relation with itself`() {
        assertNull(RelationSheetModel.view(doc, K.RIEN, K.RIEN, SheetSlot.BETWEEN, null, latest))
        assertEquals(doc, RelationSheetModel.save(doc, K.RIEN, K.RIEN, SheetSlot.BETWEEN, SlotDraft(kindKey = K.KIND_FRIEND), null, null, latest, CastFx.ctx()))
    }

    @Test
    fun `CST-29 pair kind from 리엔's sheet makes 세라 the 선배 and fills 세라's view`() {
        val saved = RelationSheetModel.save(
            doc, K.RIEN, K.SERA, SheetSlot.BETWEEN, SlotDraft(kindKey = K.KIND_SENIOR_JUNIOR, partnerHoldsFirst = true),
            io.github.lenticualrstone.civilight.feature.cast.field.SaveChoice.PLAIN, null, latest, CastFx.ctx(),
        )
        assertEquals("세라 · 선배", RelationViews.view(saved, K.RIEN, K.SERA, latest)!!.title("세라"))
        assertEquals("리엔 · 후배", RelationViews.view(saved, K.SERA, K.RIEN, latest)!!.title("리엔"))
    }
}
