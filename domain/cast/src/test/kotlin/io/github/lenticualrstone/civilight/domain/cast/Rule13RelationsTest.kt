package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.cast.export.CharacterExport
import io.github.lenticualrstone.civilight.domain.cast.export.CharacterExportOptions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTypes
import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.domain.cast.label
import io.github.lenticualrstone.civilight.domain.cast.list.CastList
import io.github.lenticualrstone.civilight.domain.cast.map.LineStyle
import io.github.lenticualrstone.civilight.domain.cast.map.MapFilter
import io.github.lenticualrstone.civilight.domain.cast.map.MapSlotFilter
import io.github.lenticualrstone.civilight.domain.cast.map.RelationMap
import io.github.lenticualrstone.civilight.domain.cast.relation.CanvasRelations
import io.github.lenticualrstone.civilight.domain.cast.relation.LineSlot
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationEdits
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationKinds
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationPanel
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.domain.cast.relation.SlotInput
import io.github.lenticualrstone.civilight.domain.cast.relation.StepStatus
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.model.canvas.CanvasEdge
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.RelationCategory
import io.github.lenticualrstone.civilight.model.cast.RelationSymmetry
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Rule 13 · 관계 (30-cast.md §4.1). */
class Rule13RelationsTest {
    private val doc = Fx.doc()

    @Test
    fun `TC-13-01 relation shape`() {
        val v = requireNotNull(RelationViews.view(doc, K.RIEN, K.HART, Fx.LATEST))
        assertNull(v.partnerRole)
        assertNull(v.state.between)
        assertEquals("경계", v.ownMind?.word)
        assertEquals("불빛을 아는 낯선 사람", v.ownMind?.description)
        assertEquals("보호", v.otherMind?.word)
        assertEquals("꼬마", v.otherAddress)
        assertEquals("→ 경계 · ← 보호", v.summary)
    }

    @Test
    fun `TC-13-02 pair kind fills the other side`() {
        val fromRien = requireNotNull(RelationViews.view(doc, K.RIEN, K.SERA, Fx.LATEST))
        val fromSera = requireNotNull(RelationViews.view(doc, K.SERA, K.RIEN, Fx.LATEST))
        assertEquals("세라 · 선배", fromRien.title("세라"))
        assertEquals("리엔 · 후배", fromSera.title("리엔"))
        assertNull(fromSera.ownMind)
    }

    @Test
    fun `TC-13-02 choosing 선배 for 세라 stores the pair with 세라 holding 선배`() {
        val ctx = Fx.ctx()
        val (d, relation) = RelationEdits.openOrCreate(Fx.withoutRelation(doc, K.REL_RIEN_SERA), K.RIEN, K.SERA, ctx)
        val value = RelationEdits.betweenValue(relation, Fx.kind(d, "선배"), holderOfName = K.SERA)
        val edited = RelationEdits.setBase(d, relation.key, RelationSlotName.BETWEEN, SlotInput.Kind(value), ctx)
        assertEquals("세라 · 선배", RelationViews.view(edited, K.RIEN, K.SERA, Fx.LATEST)!!.title("세라"))
        assertEquals("리엔 · 후배", RelationViews.view(edited, K.SERA, K.RIEN, Fx.LATEST)!!.title("리엔"))
        assertNull(edited.index.relation(relation.key)!!.mindA)
        assertNull(edited.index.relation(relation.key)!!.mindB)
    }

    @Test
    fun `TC-13-03 symmetric kind, no minds`() {
        val fromRien = requireNotNull(RelationViews.view(doc, K.RIEN, K.OWEN, Fx.LATEST))
        assertEquals("오웬 · 거래 상대", fromRien.title("오웬"))
        assertEquals("마음 적지 않음", fromRien.summary)
        assertEquals("리엔 · 거래 상대", RelationViews.view(doc, K.OWEN, K.RIEN, Fx.LATEST)!!.title("리엔"))
    }

    @Test
    fun `TC-13-04 minds are never filled for the other side`() {
        val fresh = Fx.withoutRelationRecords(doc, K.REL_RIEN_HART)
        val before = fresh.changes.records.size
        val edited = RelationEdits.setMind(fresh, K.RIEN, K.HART, SlotValue(kindKey = K.KIND_WARINESS), Fx.ctx(), Anchor.AtBlock(K.BLOCK_043))
        val made = edited.changes.records.drop(before)
        assertEquals(1, made.size)
        assertEquals(RelationSlotName.MIND_A, (made.single().target as ChangeTarget.RelationSlot).slot.known)
        val state = RelationReader.state(edited, edited.index.relation(K.REL_RIEN_HART)!!, ReadPoint.AtLens(Fx.LATEST))
        assertEquals(K.KIND_WARINESS, state.mindA?.kindKey)
        assertNull(state.mindB)
    }

    @Test
    fun `TC-13-05 viewer side on both records`() {
        val rienTab = RelationViews.tab(doc, K.RIEN, Fx.LATEST)
        val hart = rienTab.blocks.single { it.partnerKey == K.HART }
        assertEquals("하르트", hart.title)
        assertEquals("→ 경계 · ← 보호", hart.sub)
        val hartTab = RelationViews.tab(doc, K.HART, Fx.LATEST)
        assertEquals("→ 보호 · ← 경계", hartTab.blocks.single { it.partnerKey == K.RIEN }.sub)
    }

    @Test
    fun `TC-13-06 change over time`() {
        // Lens 1-6: 만나기 전, no line.
        assertEquals("만나기 전", RelationViews.summary(doc, K.RIEN, K.HART, Fx.L1_6))
        assertTrue(RelationMap.build(doc, Fx.L1_6).lines.none { it.relationKey == K.REL_RIEN_HART })

        // Lens 1-7: minds only, dashed.
        val at17 = RelationViews.view(doc, K.RIEN, K.HART, Fx.L1_7)!!
        assertEquals("→ 경계 · ← 보호", at17.summary)
        assertNull(at17.state.between)
        assertEquals(LineStyle.DASHED, RelationMap.build(doc, Fx.L1_7).lines.single { it.relationKey == K.REL_RIEN_HART }.style)

        // 최신 · 1-7: same values; 1-9 only as 예정 rows.
        assertEquals("→ 경계 · ← 보호", RelationViews.summary(doc, K.RIEN, K.HART, Fx.LATEST))
        val relation = doc.index.relation(K.REL_RIEN_HART)!!
        assertEquals(listOf("예정 · 1-9 · 사이 동료 · → 신뢰"), RelationViews.plannedRows(doc, relation, K.RIEN, Fx.LATEST))

        // Lens 1-9: 사이 동료 (solid, middle 동료), → 신뢰 · ← 보호, 호칭 하르트.
        val at19 = RelationViews.view(doc, K.RIEN, K.HART, Fx.L1_9)!!
        assertEquals("동료", at19.partnerRole)
        assertEquals("→ 신뢰 · ← 보호", at19.summary)
        assertEquals("하르트", at19.ownAddress)
        val line = RelationMap.build(doc, Fx.L1_9).lines.single { it.relationKey == K.REL_RIEN_HART }
        assertEquals(LineStyle.SOLID, line.style)
        assertEquals("동료", line.middle?.text)
    }

    @Test
    fun `TC-13-06 steps on the 관계 tab`() {
        val steps = RelationViews.tab(doc, K.RIEN, Fx.LATEST).blocks.single { it.partnerKey == K.HART }.steps
        assertEquals(listOf("1-7 · 처음 만남 · → 경계 · ← 보호", "1-9 · 사이 동료 · → 신뢰"), steps.map { it.text })
        assertEquals(listOf(StepStatus.IN_EFFECT, StepStatus.PLANNED), steps.map { it.status })
        // At a past lens a written later step is `지금 시점보다 뒤`.
        val at16 = RelationViews.tab(doc, K.RIEN, Fx.L1_6).blocks.single { it.partnerKey == K.HART }.steps
        assertEquals(StepStatus.AFTER_LENS, at16.first().status)
        assertEquals("지금 시점보다 뒤", at16.first().statusLabel)
    }

    @Test
    fun `TC-13-07 per-block panel`() {
        assertEquals("만나기 전", RelationPanel.row(doc, K.RIEN, K.HART, K.BLOCK_042)!!.own.text)
        assertEquals("하르트 · 만나기 전", RelationPanel.row(doc, K.RIEN, K.HART, K.BLOCK_042)!!.line)

        val rienAt43 = RelationPanel.row(doc, K.RIEN, K.HART, K.BLOCK_043)!!
        assertEquals("만나기 전 → 경계 ◆ 여기서", rienAt43.own.text)
        val hartAt43 = RelationPanel.row(doc, K.HART, K.RIEN, K.BLOCK_043)!!
        assertEquals("만나기 전 → 보호 ◆ 여기서", hartAt43.own.text)
        assertEquals("만나기 전 → 경계 ◆ 여기서", hartAt43.otherLine)

        val hartAt44 = RelationPanel.row(doc, K.HART, K.RIEN, K.BLOCK_044)!!
        assertEquals("리엔 · → 보호 · 호칭 ‘꼬마’", hartAt44.line)
        assertEquals("← 경계", hartAt44.otherLine)
        assertEquals(listOf("리엔을" to "‘꼬마’"), RelationPanel.addressRows(doc, K.HART, K.BLOCK_044))
        assertEquals(emptyList<Pair<String, String>>(), RelationPanel.addressRows(doc, K.HART, K.BLOCK_043))
        assertEquals(listOf("세라를" to "‘세라 씨’"), RelationPanel.addressRows(doc, K.RIEN, K.BLOCK_044))
    }

    @Test
    fun `TC-13-08 two minds in one block make two records`() {
        val fresh = Fx.withoutRelationRecords(doc, K.REL_RIEN_HART)
        val (_, made) = RelationEdits.record(
            fresh, K.REL_RIEN_HART,
            mapOf(
                RelationSlotName.MIND_A to SlotInput.Kind(SlotValue(kindKey = K.KIND_WARINESS)),
                RelationSlotName.MIND_B to SlotInput.Kind(SlotValue(kindKey = K.KIND_PROTECTION)),
            ),
            Anchor.AtBlock(K.BLOCK_043), Fx.ctx(),
        )
        assertEquals(2, made.size)
        assertTrue(made.all { it.anchor.sameAs(Anchor.AtBlock(K.BLOCK_043)) })
        assertEquals(
            setOf(RelationSlotName.MIND_A, RelationSlotName.MIND_B),
            made.map { (it.target as ChangeTarget.RelationSlot).slot.known }.toSet(),
        )
    }

    @Test
    fun `TC-13-09 호칭 only creates an empty relation`() {
        assertNull(RelationReader.between(doc, K.SERA, K.MARTHA))
        val edited = RelationEdits.setAddress(doc, K.SERA, K.MARTHA, "결사장님", Fx.ctx())
        val relation = requireNotNull(RelationReader.between(edited, K.SERA, K.MARTHA))
        assertNull(relation.between)
        assertNull(relation.mindA)
        assertNull(relation.mindB)
        assertTrue(RelationMap.build(edited, Fx.LATEST).lines.none { it.relationKey == relation.key })
        assertTrue("마르타 → 결사장님" in RelationViews.addressChips(edited, K.SERA, Fx.LATEST).map { it.label })
    }

    @Test
    fun `TC-13-10 one relation per pair`() {
        val (d, relation) = RelationEdits.openOrCreate(doc, K.RIEN, K.HART, Fx.ctx())
        assertEquals(K.REL_RIEN_HART, relation.key)
        assertEquals(doc.cast.relations.size, d.cast.relations.size)
        // The other order finds the same relation.
        assertEquals(K.REL_RIEN_HART, RelationEdits.openOrCreate(doc, K.HART, K.RIEN, Fx.ctx()).second.key)
    }

    @Test
    fun `TC-13-11 kind rename`() {
        val renamed = RelationKinds.rename(doc, K.KIND_WARINESS, "경계심", Fx.ctx())
        assertEquals("→ 경계심 · ← 보호", RelationViews.summary(renamed, K.RIEN, K.HART, Fx.LATEST))
        assertEquals(K.KIND_WARINESS, renamed.index.record(K.R4)!!.valueAs(SlotValue.serializer())!!.kindKey)
    }

    @Test
    fun `TC-13-12 kind delete keeps text`() {
        val deleted = RelationKinds.delete(doc, K.KIND_PROTECTION, Fx.ctx())
        val value = deleted.index.record(K.R5)!!.valueAs(SlotValue.serializer())!!
        assertNull(value.kindKey)
        assertEquals("보호", value.kindFallbackText)
        val mind = RelationViews.view(deleted, K.HART, K.RIEN, Fx.LATEST)!!.ownMind!!
        assertEquals("보호", mind.word)
        assertTrue(mind.fallback)
        assertEquals("보호 ◇", mind.full)
        assertNull(deleted.index.relationKind(K.KIND_PROTECTION))
    }

    @Test
    fun `TC-13-13 pair rename updates the written other side`() {
        val renamed = RelationKinds.renamePair(doc, K.KIND_SENIOR_JUNIOR, "막내", Fx.ctx())
        assertEquals("리엔 · 막내", RelationViews.view(renamed, K.SERA, K.RIEN, Fx.LATEST)!!.title("리엔"))
        assertEquals("세라 · 선배", RelationViews.view(renamed, K.RIEN, K.SERA, Fx.LATEST)!!.title("세라"))
    }

    @Test
    fun `TC-13-14 map line style and labels`() {
        val both = RelationMap.build(doc, Fx.L1_7, selectedRelationKey = K.REL_RIEN_HART)
        assertEquals(setOf(K.REL_RIEN_HART, K.REL_RIEN_SERA, K.REL_RIEN_OWEN, K.REL_SERA_HART), both.lines.map { it.relationKey }.toSet())
        val rh = both.lines.single { it.relationKey == K.REL_RIEN_HART }
        assertEquals(LineStyle.DASHED, rh.style)
        assertEquals(K.RIEN, rh.mindA!!.holderKey)
        assertEquals("경계 →", rh.mindTextA)
        assertEquals(K.HART, rh.mindB!!.holderKey)
        assertEquals("← 보호", rh.mindTextB)
        val rs = both.lines.single { it.relationKey == K.REL_RIEN_SERA }
        assertEquals(LineStyle.SOLID, rs.style)
        assertNotNull(rs.middle)
        assertEquals("의지 →", rs.mindTextA)
        val ro = both.lines.single { it.relationKey == K.REL_RIEN_OWEN }
        assertEquals(LineStyle.SOLID, ro.style)
        assertEquals("거래 상대", ro.middle!!.text)
        val sh = both.lines.single { it.relationKey == K.REL_SERA_HART }
        assertEquals("동료", sh.middle!!.text)
        // One colour for all lines except the selected one.
        assertEquals(listOf(K.REL_RIEN_HART), both.lines.filter { it.selected }.map { it.relationKey })

        val between = RelationMap.build(doc, Fx.L1_7, MapFilter(slots = MapSlotFilter.BETWEEN))
        assertEquals(setOf(K.REL_RIEN_SERA, K.REL_RIEN_OWEN, K.REL_SERA_HART), between.lines.map { it.relationKey }.toSet())
        val minds = RelationMap.build(doc, Fx.L1_7, MapFilter(slots = MapSlotFilter.MIND))
        assertEquals(setOf(K.REL_RIEN_HART, K.REL_RIEN_SERA), minds.lines.map { it.relationKey }.toSet())
    }

    @Test
    fun `TC-13-15 canvas line needs the button`() {
        // Given: a plain line from 리엔 to 하르트 (no relation yet), and a line from 리엔 to the faction 등불 결사.
        val factionEdge = Fx.key("CNED", 9)
        var d = Fx.withoutRelation(doc, K.REL_RIEN_HART)
        val canvas = d.canvases.getValue(K.CANVAS_CHAPTER_1)
        d = d.copy(
            canvases = d.canvases + (
                K.CANVAS_CHAPTER_1 to canvas.copy(
                    edges = canvas.edges.map { if (it.key == K.CANVAS_EDGE_RIEN_HART) it.copy(relationKey = null) else it } +
                        CanvasEdge(key = factionEdge, fromNodeKey = K.canvasNode(2), toNodeKey = K.canvasNode(1)),
                )
                ),
        )
        val sheet = requireNotNull(CanvasRelations.sheet(d, K.CANVAS_CHAPTER_1, K.CANVAS_EDGE_RIEN_HART, Fx.L1_7))
        assertEquals("선 라벨", sheet.title)
        assertEquals(listOf("사이", "리엔의 마음", "하르트의 마음"), sheet.slotChips)
        assertEquals(LineSlot.FROM_MIND, sheet.preselected)
        assertEquals("1-7", sheet.startPoint)

        // 그냥 선으로 두기: nothing recorded.
        val kept = CanvasRelations.keepAsLine(d, K.CANVAS_CHAPTER_1, K.CANVAS_EDGE_RIEN_HART, null, Fx.ctx())
        assertNull(RelationReader.between(kept, K.RIEN, K.HART))
        assertEquals(d.changes.records, kept.changes.records)
        assertNull(kept.canvases.getValue(K.CANVAS_CHAPTER_1).edges.single { it.key == K.CANVAS_EDGE_RIEN_HART }.relationKey)

        // 관계로 만들기: 리엔's mind 경계 from the start of 1-7, line drawn dashed with end labels.
        val made = CanvasRelations.makeRelation(
            d, K.CANVAS_CHAPTER_1, K.CANVAS_EDGE_RIEN_HART, LineSlot.FROM_MIND, K.KIND_WARINESS, K.EPISODE_1_7, Fx.ctx(),
        )
        val relation = requireNotNull(RelationReader.between(made, K.RIEN, K.HART))
        val record = made.changes.records.single { (it.target as? ChangeTarget.RelationSlot)?.relationKey == relation.key }
        assertTrue(record.anchor.sameAs(Anchor.UnitStart(K.EPISODE_1_7)))
        assertEquals(relation.key, made.canvases.getValue(K.CANVAS_CHAPTER_1).edges.single { it.key == K.CANVAS_EDGE_RIEN_HART }.relationKey)
        val line = RelationMap.build(made, Fx.L1_7).lines.single { it.relationKey == relation.key }
        assertEquals(LineStyle.DASHED, line.style)
        assertEquals("경계 →", line.mindTextA)

        // A line to a faction cannot become a relation.
        val factionSheet = requireNotNull(CanvasRelations.sheet(d, K.CANVAS_CHAPTER_1, factionEdge, Fx.L1_7))
        assertFalse(factionSheet.canMakeRelation)
        assertNull(factionSheet.makeLabel)
    }

    @Test
    fun `TC-13-16 연결 field cannot target characters`() {
        assertEquals(listOf(FieldLinkTargetKind.FACTION, FieldLinkTargetKind.PLACE, FieldLinkTargetKind.TERM), FieldTypes.LINK_TARGETS)
        assertEquals(listOf("세력", "장소", "용어"), FieldTypes.LINK_TARGETS.map { it.label })
    }

    @Test
    fun `TC-13-17 no relations on rows and cards`() {
        val row = CastList.build(doc, Fx.L1_7).rows.single { it.key == K.RIEN }
        val card = CastList.card(doc, Fx.character(doc, K.RIEN), Fx.L1_7)
        for (word in listOf("경계", "선배", "의지", "거래 상대", "보호")) {
            assertFalse(word in row.subtitle)
            assertFalse(word in card.subtitle)
            assertFalse(word in card.title)
        }
        assertEquals("17세 · 등불 결사 · 막내 등불지기", Profile.listSubtitle(doc, K.RIEN, Fx.L1_7))
    }

    @Test
    fun `TC-13-18 호칭 is one value`() {
        val given = RelationEdits.setAddress(doc, K.RIEN, K.HART, "하르트 씨", Fx.ctx())
        assertEquals(listOf("하르트 → 하르트 씨", "세라 → 세라 씨"), RelationViews.addressChips(given, K.RIEN, Fx.LATEST).map { it.label })
        val edited = RelationEdits.editAddressChip(given, K.RIEN, K.HART, "하르트 님", Fx.LATEST, Fx.ctx())
        // The relation sheet (state) and the script panel read the same slot.
        assertEquals("하르트 님", RelationViews.view(edited, K.RIEN, K.HART, Fx.LATEST)!!.ownAddress)
        assertEquals("하르트 님", RelationPanel.row(edited, K.RIEN, K.HART, K.BLOCK_045)!!.address)
        // No separate 말투 value: the character row is unchanged.
        assertEquals(Fx.character(given, K.RIEN), Fx.character(edited, K.RIEN))
    }

    @Test
    fun `TC-13-19 무관심 vs 만나기 전`() {
        assertEquals("만나기 전", RelationViews.summary(doc, K.RIEN, K.MARTHA, Fx.L1_7))
        val edited = RelationEdits.setMind(
            doc, K.RIEN, K.MARTHA, SlotValue(kindKey = K.KIND_INDIFFERENCE), Fx.ctx(), Anchor.UnitStart(K.EPISODE_1_7),
        )
        val relation = requireNotNull(RelationReader.between(edited, K.RIEN, K.MARTHA))
        val line = RelationMap.build(edited, Fx.L1_7).lines.single { it.relationKey == relation.key }
        assertEquals(LineStyle.DASHED, line.style)
        assertEquals("무관심 →", line.mindTextA)
        assertEquals(K.RIEN, line.mindA!!.holderKey)
        assertEquals("만나기 전", RelationViews.summary(edited, K.RIEN, K.MARTHA, Fx.L1_6))
        assertTrue(RelationMap.build(edited, Fx.L1_6).lines.none { it.relationKey == relation.key })
    }

    @Test
    fun `TC-13-20 switching a kind's category shows the effect first`() {
        val preview = RelationKinds.switchPreview(doc, K.KIND_TRADE_PARTNER)
        assertEquals(listOf(K.REL_RIEN_OWEN), preview.relationKeys)
        assertEquals(listOf("리엔 · 오웬"), preview.pairs)
        assertEquals("바뀌는 관계 1", preview.countLabel)
        // Cancelled: nothing changed.
        val kind = doc.index.relationKind(K.KIND_TRADE_PARTNER)!!
        assertEquals(RelationCategory.BETWEEN, kind.category.known)
        assertEquals(RelationSymmetry.SYMMETRIC, kind.symmetry?.known)
        assertEquals("거래 상대", RelationViews.view(doc, K.RIEN, K.OWEN, Fx.LATEST)!!.partnerRole)
        // The same preview for a 대칭 ↔ 짝 switch.
        assertEquals(listOf(K.REL_RIEN_SERA), RelationKinds.switchPreview(doc, K.KIND_SENIOR_JUNIOR).relationKeys)
    }

    @Test
    fun `TC-13-20 applying a confirmed switch changes only the kind`() {
        val applied = RelationKinds.applySwitch(doc, K.KIND_TRADE_PARTNER, RelationCategory.MIND, Fx.ctx())
        val kind = applied.index.relationKind(K.KIND_TRADE_PARTNER)!!
        assertEquals(RelationCategory.MIND, kind.category.known)
        assertNull(kind.symmetry)
        assertEquals(doc.cast.relations, applied.cast.relations)
        val toPair = RelationKinds.applySwitch(doc, K.KIND_COLLEAGUE, RelationCategory.BETWEEN, Fx.ctx(), RelationSymmetry.PAIR, "동료 후배")
        assertEquals("동료 후배", toPair.index.relationKind(K.KIND_COLLEAGUE)!!.pairName)
        assertEquals(RelationSymmetry.PAIR, toPair.index.relationKind(K.KIND_COLLEAGUE)!!.symmetry?.known)
    }

    @Test
    fun `two edit buttons on a relation slot with history`() {
        // 이 값 고치기 on 하르트's 호칭 (R5A in effect at 최신): the record changes, no new record.
        val corrected = RelationEdits.correctInEffect(doc, K.REL_RIEN_HART, RelationSlotName.ADDRESS_B, SlotInput.Address("꼬맹이"), Fx.LATEST, Fx.ctx())
        assertEquals(doc.changes.records.size, corrected.changes.records.size)
        assertEquals("꼬맹이", RelationReader.addressOf(corrected.index.record(K.R5A)!!))
        assertEquals("꼬맹이", RelationViews.view(corrected, K.RIEN, K.HART, Fx.LATEST)!!.otherAddress)
        // 여기서부터 바뀜 on 리엔's mind at 최신: a new ◆ after the 집필 선 scene; later blocks of that scene still read 경계.
        val changed = RelationEdits.changeFromHere(
            doc, K.REL_RIEN_HART, RelationSlotName.MIND_A, SlotInput.Kind(SlotValue(kindKey = K.KIND_RELIANCE)), Fx.LATEST, Fx.ctx(),
        )
        val added = changed.changes.records.single { r -> doc.changes.records.none { it.key == r.key } }
        assertTrue(added.anchor.sameAs(Anchor.UnitEnd(K.SCENE_1_7_3)))
        assertEquals("→ 의지 · ← 보호", RelationViews.summary(changed, K.RIEN, K.HART, Fx.LATEST))
        assertEquals("경계", RelationPanel.row(changed, K.RIEN, K.HART, K.BLOCK_048)!!.own.text)
        // A 호칭 slot refuses a kind value.
        assertTrue(runCatching { RelationEdits.setBase(doc, K.REL_RIEN_HART, RelationSlotName.ADDRESS_A, SlotInput.Kind(null), Fx.ctx()) }.isFailure)
    }

    @Test
    fun `TC-13-21 relation export`() {
        // .civilight round trip: one entry for the pair, every record with the same anchor.
        val files = ProjectFileSet.split(doc).encode()
        val back: ProjectDoc = ProjectFileSet.decode(files).files.toDoc(app = doc.app)
        assertEquals(doc.index.relation(K.REL_RIEN_HART), back.index.relation(K.REL_RIEN_HART))
        assertEquals(1, back.cast.relations.count { it.pairId == doc.index.relation(K.REL_RIEN_HART)!!.pairId })
        for (key in listOf(K.R4, K.R5, K.R5A, K.R6, K.R7, K.R7A)) {
            val original = doc.index.record(key)!!
            val read = back.index.record(key)!!
            assertTrue(original.anchor.sameAs(read.anchor))
            assertEquals(original.value, read.value)
            assertEquals(original.target, read.target)
        }

        // Game-engine JSON with default options: no relation data.
        val plain = CharacterExport.characters(doc, listOf(K.RIEN, K.HART))
        for (entry in plain) {
            assertFalse(CharacterExport.ADDRESSES in entry.jsonObject)
            assertFalse(entry.toString().contains("경계") || entry.toString().contains("보호") || entry.toString().contains("꼬마"))
        }

        // 호칭 only: 호칭 values and nothing of 사이 or minds.
        val withAddresses = CharacterExport.characters(doc, listOf(K.RIEN, K.HART), CharacterExportOptions(addresses = true))
        val rien = withAddresses.first().jsonObject.getValue(CharacterExport.ADDRESSES).jsonObject
        assertEquals("세라 씨", rien.getValue(K.SERA).jsonPrimitive.content)
        val toHart = rien.getValue(K.HART) as JsonArray
        assertEquals(JsonPrimitive("하르트"), toHart.single().jsonObject["value"])
        assertEquals(JsonPrimitive("1-9"), toHart.single().jsonObject["from"])
        val hart = withAddresses[1].jsonObject.getValue(CharacterExport.ADDRESSES) as JsonObject
        assertEquals(JsonPrimitive("꼬마"), (hart.getValue(K.RIEN) as JsonArray).single().jsonObject["value"])
        for (word in listOf("경계", "보호", "동료", "신뢰", "선배", "의지")) assertFalse(withAddresses.toString().contains(word))
    }
}
