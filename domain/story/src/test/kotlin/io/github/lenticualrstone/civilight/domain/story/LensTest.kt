package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.Fx.withBlock
import io.github.lenticualrstone.civilight.domain.story.lens.EditChoice
import io.github.lenticualrstone.civilight.domain.story.lens.LensEdit
import io.github.lenticualrstone.civilight.domain.story.lens.LensView
import io.github.lenticualrstone.civilight.domain.story.lens.RecordStatus
import io.github.lenticualrstone.civilight.domain.story.lens.ScriptLens
import io.github.lenticualrstone.civilight.domain.story.lens.ValueText
import io.github.lenticualrstone.civilight.domain.story.scene.SceneTrash
import io.github.lenticualrstone.civilight.model.app.LensState
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rule 11 lens, 집필 선, 예정 and the two edit buttons (20-story-time §4.2–§4.3). */
class LensTest {
    private val doc = Fx.fixtureR()

    private val mindRien = ChangeTarget.RelationSlot(K.REL_RIEN_HART, RelationSlotName.MIND_A)
    private val mindHart = ChangeTarget.RelationSlot(K.REL_RIEN_HART, RelationSlotName.MIND_B)
    private val between = ChangeTarget.RelationSlot(K.REL_RIEN_HART, RelationSlotName.BETWEEN)
    private val addressRien = ChangeTarget.RelationSlot(K.REL_RIEN_HART, RelationSlotName.ADDRESS_A)
    private val addressHart = ChangeTarget.RelationSlot(K.REL_RIEN_HART, RelationSlotName.ADDRESS_B)
    private val hartName = ChangeTarget.DisplayName(K.HART)
    private val affiliation = ChangeTarget.Field(EntityKind.CHARACTER, K.RIEN, K.FIELD_AFFILIATION)

    private fun ep(key: String) = LensPoint.UnitEnd(key)

    /** Shown text of a target at a lens point (record value or base value). */
    private fun ProjectDoc.textAt(target: ChangeTarget, point: LensPoint): String {
        val values = ValueText(this)
        return LensView(this).recordAt(target, point)?.let { values.of(target, it.value) } ?: values.base(target)
    }

    private fun texts(value: String) = ChangeRecord.encodeValue(FieldValue.serializer(), FieldValue.of(FieldPayload.Texts(listOf(value))))

    private fun ProjectDoc.record(key: String) = changes.records.single { it.key == key }

    // --- §4.2 Lens and 집필 선 -------------------------------------------------------------------

    @Test
    fun lens01_writingLineAndChip() {
        assertEquals(K.SCENE_1_7_3, doc.manuscript.writingLineSceneKey)
        assertEquals(K.BLOCK_048, doc.manuscript.liveBlocksOf(K.SCENE_1_7_3).last().key)
        assertEquals("최신 · 1-7", LensView(doc).chip(LensPoint.Latest).label)
        assertFalse(LensView(doc).chip(LensPoint.Latest).accented)
    }

    @Test
    fun lens02_latestShowsAppliedAndPlanned() {
        val view = LensView(doc)
        assertEquals("경계", doc.textAt(mindRien, LensPoint.Latest))
        assertEquals("보호", doc.textAt(mindHart, LensPoint.Latest))
        assertEquals("1-7 #043", view.anchorLabel(view.recordAt(mindRien, LensPoint.Latest)!!.anchor))
        assertEquals("???", doc.textAt(hartName, LensPoint.Latest))

        val r3 = doc.record(K.R3)
        assertEquals(RecordStatus.PLANNED, view.status(r3))
        assertEquals("예정 · 2-5", view.plannedLabel(r3))
        assertEquals("등불지기장", ValueText(doc).of(affiliation, r3.value))
        assertEquals(listOf(K.R3), view.planned(affiliation).map { it.key })

        assertEquals("예정 · 1-9", view.plannedLabel(doc.record(K.R7)))
        assertEquals("신뢰", ValueText(doc).of(mindRien, doc.record(K.R7).value))
        assertEquals("예정 · 1-9", view.plannedLabel(doc.record(K.R6)))
        assertEquals("동료", ValueText(doc).of(between, doc.record(K.R6).value))
        assertEquals(listOf(K.R7), view.planned(mindRien).map { it.key })
        assertNull(view.plannedLabel(doc.record(K.R4)))
        assertEquals("예정 · 집필 선(1-7 씬 3) 뒤 기록", view.plannedFootnote())
    }

    @Test
    fun lens03_beforeTheyMeet() {
        val at = ep(K.EPISODE_1_6)
        assertEquals("만나기 전", doc.textAt(mindRien, at))
        assertEquals("만나기 전", doc.textAt(mindHart, at))
        assertEquals("???", doc.textAt(hartName, at))
    }

    @Test
    fun lens04_episode17() {
        val at = ep(K.EPISODE_1_7)
        assertEquals("경계", doc.textAt(mindRien, at))
        assertEquals("보호", doc.textAt(mindHart, at))
        assertEquals("꼬마", doc.textAt(addressHart, at))
        assertEquals("???", doc.textAt(hartName, at))
    }

    @Test
    fun lens05_episode18SameAs17() {
        for (t in listOf(mindRien, mindHart, addressHart, hartName, between)) {
            assertEquals(doc.textAt(t, ep(K.EPISODE_1_7)), doc.textAt(t, ep(K.EPISODE_1_8)))
        }
    }

    @Test
    fun lens06_unwrittenEpisode19() {
        val at = ep(K.EPISODE_1_9)
        assertEquals("동료", doc.textAt(between, at))
        assertEquals("신뢰", doc.textAt(mindRien, at))
        assertEquals("하르트", doc.textAt(addressRien, at))
        assertEquals("하르트", doc.textAt(hartName, at))
        val view = LensView(doc)
        assertEquals("??? (1-8까지)", view.previousValueLabel(hartName, at))
        assertNull(view.previousValueLabel(hartName, LensPoint.Latest))
        val row = view.sheetRows(at, listOf(hartName)).single { it.point == at }
        assertTrue(row.unwritten)
        assertTrue(row.viewing)
        assertEquals("아직 쓰지 않음", row.sub)
        assertEquals("1-9", row.code)
    }

    @Test
    fun lens07_scriptPanelIsBlockLevel() {
        val view = LensView(doc)
        assertEquals("만나기 전", view.panelRow(mindRien, K.BLOCK_042))
        assertEquals("만나기 전 → 경계 · ◆ 여기서", view.panelRow(mindRien, K.BLOCK_043))
        assertEquals("경계", view.panelRow(mindRien, K.BLOCK_044))
        assertEquals(setOf(K.R4, K.R5, K.R5A), view.changesAt(K.BLOCK_043).map { it.key }.toSet())
    }

    @Test
    fun lens08_preRevealUseByBlockOrder() {
        val view = LensView(doc)
        assertTrue(view.isLinkBeforeReveal(K.TERM_ORIGINIUM_LANTERN, K.BLOCK_042))
        assertFalse(view.isLinkBeforeReveal(K.TERM_ORIGINIUM_LANTERN, K.BLOCK_044))
        assertFalse(view.isLinkBeforeReveal(K.TERM_ORIGINIUM_LANTERN, K.BLOCK_045))
    }

    private val in19 = "01JZ000000TEST0000000000A1"

    private fun lens09State() = doc.withBlock(K.SCENE_1_9_1, in19, "관문 앞은 조용했다.")

    @Test
    fun lens09_gapsAreIgnored() {
        val d = lens09State()
        assertEquals(K.SCENE_1_9_1, d.manuscript.writingLineSceneKey)
        val view = LensView(d)
        assertEquals("최신 · 1-9", view.chip(LensPoint.Latest).label)
        for (r in listOf(K.R6, K.R7, K.R7A, K.R8)) assertEquals(RecordStatus.APPLIED, view.status(d.record(r)))
        assertEquals("하르트", d.textAt(hartName, LensPoint.Latest))
    }

    @Test
    fun lens10_nothingWritten() {
        val d = doc.copy(episodes = doc.episodes.mapValues { (_, f) -> f.copy(blocks = emptyList()) })
        assertNull(d.manuscript.writingLineSceneKey)
        val view = LensView(d)
        assertEquals("최신 · 전체", view.chip(LensPoint.Latest).label)
        assertEquals("등불지기장", d.textAt(affiliation, LensPoint.Latest))
        assertTrue(d.changes.records.none { view.status(it) == RecordStatus.PLANNED })
        assertNull(view.plannedFootnote())
    }

    @Test
    fun lens11_firstBlockIn25AppliesR3() {
        val d = doc.withBlock(K.SCENE_2_5_1, "01JZ000000TEST0000000000A2", "등불지기장의 첫날.")
        val view = LensView(d)
        assertEquals("최신 · 2-5", view.chip(LensPoint.Latest).label)
        assertEquals(RecordStatus.APPLIED, view.status(d.record(K.R3)))
        assertEquals("등불지기장", d.textAt(affiliation, LensPoint.Latest))
    }

    @Test
    fun lens12_trashingThe19SceneMakesRecordsPlannedAgain() {
        val d = SceneTrash.delete(lens09State(), K.SCENE_1_9_1, Fx.ctx())
        assertEquals(K.SCENE_1_7_3, d.manuscript.writingLineSceneKey)
        val view = LensView(d)
        for (r in listOf(K.R6, K.R7, K.R7A, K.R8)) assertEquals(RecordStatus.PLANNED, view.status(d.record(r)))
        assertEquals(doc.changes.records.map { it.key }, d.changes.records.map { it.key })
    }

    @Test
    fun lens13_pastLensChipAndSheet() {
        val at = ep(K.EPISODE_1_5)
        val view = LensView(doc)
        val chip = view.chip(at)
        assertEquals("시점 · 1-5", chip.label)
        assertTrue(chip.accented)
        val rows = view.sheetRows(at, listOf(mindRien))
        assertTrue(rows.single { it.code == "1-5" }.viewing)
        assertFalse(rows.first().viewing)
        assertEquals("최신", rows.first().code)
        assertEquals("기본값 · 지금까지 쓴 모든 내용", rows.first().sub)
        assertEquals("갈림길", rows.single { it.code == "1-5" }.sub)
    }

    @Test
    fun lens14_scriptStoresAndRestoresTheLens() {
        val view = LensView(doc)
        val opened = ScriptLens.enter(LensState(point = ep(K.EPISODE_1_5)))
        val tapped = ScriptLens.leave(ScriptLens.setLensToScene(opened, K.SCENE_1_7_3))
        assertEquals("시점 · 1-7 씬 3", view.chip(tapped.point).label)
        assertNull(tapped.savedForScript)
        val untouched = ScriptLens.leave(opened)
        assertEquals("시점 · 1-5", view.chip(untouched.point).label)
    }

    // --- §4.3 Editing under the lens -------------------------------------------------------------

    @Test
    fun edit01_overwriteTheRecordInForce() {
        val choice = LensEdit.choice(doc, affiliation, LensPoint.Latest) as EditChoice.TwoButtons
        assertEquals("이 값 고치기", choice.overwrite.label)
        assertEquals("1-3 기록을 고침 · 1-3부터 새 값", choice.overwrite.sub)
        assertEquals(K.R2, choice.overwriteRecordKey)
        assertNull(choice.banner)
        val after = LensEdit.overwrite(doc, affiliation, LensPoint.Latest, texts("등불 결사 · 견습"), Fx.ctx())
        assertEquals(doc.changes.records.size, after.changes.records.size)
        assertEquals("등불 결사 · 견습", ValueText(after).of(affiliation, after.record(K.R2).value))
        assertEquals(doc.record(K.R3), after.record(K.R3))
        assertEquals("등불 결사 · 견습", after.textAt(affiliation, LensPoint.Latest))
    }

    @Test
    fun edit02_changeFromHereAtLatest() {
        val choice = LensEdit.choice(doc, affiliation, LensPoint.Latest) as EditChoice.TwoButtons
        assertEquals("여기서부터 바뀜 · 1-7", choice.fromHere.label)
        assertEquals("새 ◆ · 집필 선 1-7 씬 3 뒤", choice.fromHere.sub)
        assertEquals(Anchor.UnitEnd(K.SCENE_1_7_3), choice.newAnchor)

        val after = LensEdit.changeFromHere(doc, affiliation, LensPoint.Latest, texts("등불 결사 · 견습"), Fx.ctx())
        val added = after.changes.records.single { r -> doc.changes.records.none { it.key == r.key } }
        assertEquals(Anchor.UnitEnd(K.SCENE_1_7_3), added.anchor)
        assertEquals(doc.record(K.R2), after.record(K.R2))
        assertEquals("등불 결사 · 견습", after.textAt(affiliation, LensPoint.Latest))
        val view = LensView(after)
        assertEquals(K.R2, view.stateBefore(affiliation, K.BLOCK_045)?.key)
        assertEquals(K.R2, view.stateBefore(affiliation, K.BLOCK_048)?.key)
        val later = after.withBlock(K.SCENE_1_8_1, "01JZ000000TEST0000000000A3", "송전탑 아래.")
        assertEquals(added.key, LensView(later).stateBefore(affiliation, "01JZ000000TEST0000000000A3")?.key)
    }

    @Test
    fun edit03_pastLensBannerAndUnitStartRecord() {
        val at = ep(K.EPISODE_1_5)
        val choice = LensEdit.choice(doc, affiliation, at) as EditChoice.TwoButtons
        assertEquals("1-5 시점 값 고치는 중 | 고치면 1-5부터 적용", choice.banner?.text)
        assertEquals("최신으로", choice.banner?.action)
        assertEquals(Anchor.UnitStart(K.EPISODE_1_5), choice.newAnchor)
        assertEquals("저장 시 ◆ 변화 기록 · 1-5 #— · 이전 값 ‘등불 결사’", LensEdit.footnote(doc, affiliation, at))
        val after = LensEdit.changeFromHere(doc, affiliation, at, texts("등불 결사 · 견습"), Fx.ctx())
        assertEquals("등불 결사 · 견습", after.textAt(affiliation, at))
        assertEquals("등불 결사", after.textAt(affiliation, ep(K.EPISODE_1_4)))
        // A past lens shows the two buttons even for a field without history.
        val height = ChangeTarget.Field(EntityKind.CHARACTER, K.RIEN, K.FIELD_HEIGHT)
        assertTrue(LensEdit.choice(doc, height, at) is EditChoice.TwoButtons)
    }

    @Test
    fun edit04_noHistoryNoChoice() {
        val height = ChangeTarget.Field(EntityKind.CHARACTER, K.RIEN, K.FIELD_HEIGHT)
        assertEquals(EditChoice.Plain, LensEdit.choice(doc, height, LensPoint.Latest))
    }

    @Test
    fun editSheetLines() {
        assertEquals("지금 값 | 등불 결사 · 1-3부터", LensEdit.currentLine(doc, affiliation, LensPoint.Latest))
        assertEquals("1-1 없음 · 1-3 등불 결사 · 예정 2-5 등불지기장", LensEdit.historyLine(doc, affiliation))
    }
}
