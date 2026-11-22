package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.cast.field.FieldReader
import io.github.lenticualrstone.civilight.domain.cast.field.FieldText
import io.github.lenticualrstone.civilight.domain.cast.field.FieldValueEdits
import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.domain.cast.timed.TimedValues
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.isPlanned
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Rule 11 applied to cast fields and the display name (30-cast.md §4.2). */
class Rule11FieldHistoryTest {
    private val doc = Fx.doc()
    private val affiliation = Fx.def(doc, "소속")
    private val target = FieldReader.target(EntityKind.CHARACTER, K.RIEN, affiliation)

    private fun affiliationAt(d: ProjectDoc, lens: LensPoint): String =
        FieldText.format(d, affiliation, FieldReader.read(d, EntityKind.CHARACTER, K.RIEN, affiliation, ReadPoint.AtLens(lens)).value).text

    @Test
    fun `TC-11-01 field history at a lens`() {
        val row = Profile.row(doc, K.RIEN, affiliation, Fx.L1_5)
        assertEquals("등불 결사 1-3부터 ◆", Profile.rowText(row))

        val sheet = TimedValues.historySheet(doc, target, Fx.L1_5, "소속")
        assertEquals("소속 · 변화 기록", sheet.title)
        assertEquals("시점별 값 · 프로필에는 1-5 값", sheet.sub)
        assertEquals(listOf("1-1", "1-3", "2-5"), sheet.rows.map { it.point })
        assertEquals(listOf(false, true, false), sheet.rows.map { it.inEffect })
        assertEquals(listOf("지금 시점보다 뒤", "예정"), sheet.rows[2].marks)
        assertEquals(listOf("하층 구역의 고아", "세라의 안내로 합류", null), sheet.rows.map { it.note })
        assertEquals("시점 추가", sheet.addLabel)
        assertEquals("1-3 값 고치기", sheet.editLabel)

        val latest = Profile.row(doc, K.RIEN, affiliation, Fx.LATEST)
        assertEquals("등불 결사 1-3부터 ◆", Profile.rowText(latest))
        assertEquals("예정 · 2-5", latest.planned?.first)
    }

    @Ignore("spec conflict: TC-11-01 Given has R3 = `등불 결사 · 등불지기장` but its Then shows the planned row `예정 · 2-5 | 등불지기장`")
    @Test
    fun `TC-11-01 planned row value as stated`() {
        assertEquals("예정 · 2-5" to "등불지기장", Profile.row(doc, K.RIEN, affiliation, Fx.LATEST).planned)
    }

    @Test
    fun `TC-11-01 planned row shows the planned record's value`() {
        assertEquals("예정 · 2-5" to "등불 결사 · 등불지기장", Profile.row(doc, K.RIEN, affiliation, Fx.LATEST).planned)
        val header = Profile.header(doc, K.RIEN, Fx.LATEST, cover = true)
        assertEquals("예정 · 집필 선(1-7 씬 3) 뒤 기록", header.plannedFootnote)
    }

    @Test
    fun `TC-11-02 이 값 고치기`() {
        val edited = FieldValueEdits.correctInEffect(doc, EntityKind.CHARACTER, K.RIEN, affiliation, Fx.text("등불 결사 · 견습"), Fx.LATEST, Fx.ctx())
        assertEquals(doc.changes.records.size, edited.changes.records.size)
        assertEquals(Fx.text("등불 결사 · 견습"), edited.index.record(K.R2)!!.valueAs(FieldValue.serializer()))
        assertEquals(doc.index.record(K.R1), edited.index.record(K.R1))
        assertEquals(doc.index.record(K.R3), edited.index.record(K.R3))
        assertEquals("등불 결사 · 견습", affiliationAt(edited, Fx.L1_5))
    }

    @Test
    fun `TC-11-03 여기서부터 바뀜`() {
        val sheet = TimedValues.editSheet(doc, target, Fx.LATEST, "등불 결사") { r -> FieldText.format(doc, affiliation, r.valueAs(FieldValue.serializer())).text }
        assertEquals("등불 결사 · 1-3부터", sheet.currentLine)
        assertTrue(sheet.historyLine.startsWith("1-1 없음 · 1-3 등불 결사 · 예정 2-5"))
        assertEquals("이 값 고치기", sheet.correctLabel)
        assertEquals("1-3 기록을 고침 · 1-3부터 새 값", sheet.correctSub)
        assertEquals("여기서부터 바뀜 · 1-7", sheet.changeLabel)
        assertEquals("새 ◆ · 집필 선 1-7 씬 3 뒤", sheet.changeSub)

        val edited = FieldValueEdits.changeFromHere(doc, EntityKind.CHARACTER, K.RIEN, affiliation, Fx.text("등불 결사 · 견습"), Fx.LATEST, Fx.ctx())
        val added = edited.changes.records.single { r -> doc.changes.records.none { it.key == r.key } }
        assertTrue(added.anchor.sameAs(Anchor.UnitEnd(K.SCENE_1_7_3)))
        assertEquals("등불 결사", affiliationAt(edited, Fx.L1_5))
        assertEquals("등불 결사", affiliationAt(edited, Fx.lens(K.SCENE_1_7_2)))
        assertEquals("등불 결사 · 견습", affiliationAt(edited, Fx.LATEST))
        assertEquals(doc.index.record(K.R2), edited.index.record(K.R2))
        assertTrue(edited.isPlanned(edited.index.record(K.R3)!!))
        assertFalse(edited.isPlanned(added))
    }

    @Test
    fun `TC-11-04 past-lens edit`() {
        val sheet = TimedValues.editSheet(doc, target, Fx.L1_5, "등불 결사") { "" }
        assertEquals("1-5 시점 값 고치는 중", sheet.banner?.title)
        assertEquals("고치면 1-5부터 적용", sheet.banner?.sub)
        assertEquals("최신으로", sheet.banner?.action)
        assertEquals("여기서부터 바뀜 · 1-5", sheet.changeLabel)
        assertEquals("저장 시 ◆ 변화 기록 · 1-5 #— · 이전 값 ‘등불 결사’", sheet.saveNote)

        val edited = FieldValueEdits.changeFromHere(doc, EntityKind.CHARACTER, K.RIEN, affiliation, Fx.text("등불 결사 · 견습"), Fx.L1_5, Fx.ctx())
        val added = edited.changes.records.single { r -> doc.changes.records.none { it.key == r.key } }
        assertTrue(added.anchor.sameAs(Anchor.UnitStart(K.EPISODE_1_5)))
        assertEquals("등불 결사", affiliationAt(edited, Fx.L1_4))
        assertEquals("등불 결사 · 견습", affiliationAt(edited, Fx.L1_5))
    }

    @Test
    fun `TC-11-05 display name by lens`() {
        for (lens in listOf(Fx.L1_6, Fx.L1_7, Fx.L1_8, Fx.LATEST)) assertEquals("???", DisplayNames.read(doc, K.HART, lens).shown)
        assertEquals("하르트", DisplayNames.read(doc, K.HART, Fx.L1_9).shown)
        assertEquals("표시 이름 ??? (1-8까지)", DisplayNames.listNote(doc, K.HART, Fx.LATEST))
        assertEquals("???" to "하르트 · 이름 공개 1-9", DisplayNames.card(doc, K.HART, Fx.LATEST))
        assertEquals("??? (1-9 공개)", DisplayNames.referenceValue(doc, K.HART, ReadPoint.LATEST))
        assertEquals("???" to "1-9에서 공개", DisplayNames.propertiesValue(doc, K.HART, ReadPoint.BeforeBlock(K.BLOCK_043)))
        assertEquals("??? 하르트", DisplayNames.speakerLabel(doc, K.HART, ReadPoint.BeforeBlock(K.BLOCK_043)))
        assertEquals("표시 이름 ???", DisplayNames.pickerNote(doc, K.HART, ReadPoint.LATEST))
        assertTrue(Profile.listSubtitle(doc, K.HART, Fx.LATEST).endsWith("표시 이름 ??? (1-8까지)"))
        assertNull(DisplayNames.listNote(doc, K.RIEN, Fx.LATEST))
    }

    @Test
    fun `display name edits`() {
        val ctx = Fx.ctx()
        // 이 값 고치기 at 최신: no record in effect, so the base changes.
        val renamed = DisplayNames.correctInEffect(doc, K.HART, "검은 외투", Fx.LATEST, ctx)
        assertEquals("검은 외투", renamed.index.character(K.HART)!!.displayNameBase)
        assertEquals("검은 외투", DisplayNames.read(renamed, K.HART, Fx.LATEST).shown)
        // 여기서부터 바뀜 at a past lens: from the start of 1-5.
        val fromHere = DisplayNames.changeFromHere(doc, K.HART, "관문지기", Fx.L1_5, ctx)
        assertEquals("???", DisplayNames.read(fromHere, K.HART, Fx.L1_4).shown)
        assertEquals("관문지기", DisplayNames.read(fromHere, K.HART, Fx.L1_5).shown)
        // A reveal at another point: the list note follows it.
        val withoutR8 = doc.copy(changes = doc.changes.copy(records = doc.changes.records.filterNot { it.key == K.R8 }))
        assertEquals("표시 이름 ???", DisplayNames.listNote(withoutR8, K.HART, Fx.LATEST))
        val revealed = DisplayNames.revealAt(withoutR8, K.HART, K.EPISODE_1_8, ctx)
        assertEquals("표시 이름 ??? (1-7까지)", DisplayNames.listNote(revealed, K.HART, Fx.LATEST))
        assertEquals("하르트", DisplayNames.read(revealed, K.HART, Fx.L1_8).shown)
    }

    @Test
    fun `TC-11-06 no manuscript`() {
        val settingOnly = doc.copy(
            project = doc.project.copy(format = ProjectFormat.SETTING_ONLY.open()),
            episodes = emptyMap(),
            changes = doc.changes.copy(records = doc.changes.records.filterNot { it.key == K.R1 || it.key == K.R3 }),
        )
        assertEquals("최신 · 전체", StoryLabels.lensChip(settingOnly, Fx.LATEST))
        assertEquals("등불 결사", affiliationAt(settingOnly, Fx.LATEST))
        assertFalse(settingOnly.isPlanned(settingOnly.index.record(K.R2)!!))
        assertNull(Profile.row(settingOnly, K.RIEN, affiliation, Fx.LATEST).planned)
    }

    @Test
    fun `TC-11-07 planned point becomes applied`() {
        assertTrue(doc.isPlanned(doc.index.record(K.R7)!!))
        val written = Fx.addBlock(doc, Fx.key("BKNM", 1), K.SCENE_1_9_1, number = 1, text = "관문 앞에 다시 섰다.")
        assertEquals(K.SCENE_1_9_1, written.manuscript.writingLineSceneKey)
        assertFalse(written.isPlanned(written.index.record(K.R7)!!))
        assertEquals("→ 신뢰 · ← 보호", RelationViews.summary(written, K.RIEN, K.HART, Fx.LATEST))
        assertEquals("하르트", DisplayNames.read(written, K.HART, Fx.LATEST).shown)
    }
}
