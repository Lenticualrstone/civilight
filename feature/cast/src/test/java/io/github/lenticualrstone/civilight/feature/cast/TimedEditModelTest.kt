package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.DisplayNames
import io.github.lenticualrstone.civilight.domain.cast.field.FieldReader
import io.github.lenticualrstone.civilight.domain.cast.field.FieldText
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.feature.cast.field.EditMode
import io.github.lenticualrstone.civilight.feature.cast.field.SaveChoice
import io.github.lenticualrstone.civilight.feature.cast.field.TimedEditModel
import io.github.lenticualrstone.civilight.feature.cast.field.TimedSubject
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** The value and history sheets (CST-09, CST-10, CST-11; S9, S10, S11) on the example project. */
class TimedEditModelTest {
    private val doc = CastFx.doc()
    private val latest: LensPoint = LensPoint.Latest
    private val l15 = LensPoint.UnitEnd(K.EPISODE_1_5)
    private val l14 = LensPoint.UnitEnd(K.EPISODE_1_4)
    private val affiliation = TimedSubject.Field(EntityKind.CHARACTER, K.RIEN, K.FIELD_AFFILIATION)
    private val height = TimedSubject.Field(EntityKind.CHARACTER, K.RIEN, K.FIELD_HEIGHT)
    private val hartName = TimedSubject.DisplayName(K.HART)

    private fun text(value: String) = FieldValue.of(FieldPayload.Texts(listOf(value)))

    private fun valueAt(d: ProjectDoc, s: TimedSubject.Field, lens: LensPoint): String {
        val def = d.index.fieldDef(s.defKey)!!
        return FieldText.format(d, def, FieldReader.read(d, s.kind, s.entityKey, def, ReadPoint.AtLens(lens)).value).text
    }

    private fun added(after: ProjectDoc) = after.changes.records.single { r -> doc.changes.records.none { it.key == r.key } }

    @Test
    fun `TC-11-01 history sheet at lens 1-5`() {
        val view = TimedEditModel.history(doc, affiliation, l15)!!
        assertEquals("소속 · 변화 기록", view.title)
        assertEquals("시점별 값 · 프로필에는 1-5 값", view.sub)
        assertEquals(listOf("1-1", "1-3", "2-5"), view.rows.map { it.point })
        assertEquals(listOf(false, true, false), view.rows.map { it.inEffect })
        assertEquals(listOf(false, false, true), view.rows.map { it.dim })
        assertEquals("등불 결사", view.rows[1].value)
        assertEquals("세라의 안내로 합류", view.rows[1].note)
        assertEquals("지금 시점보다 뒤 · 예정", view.rows[2].note)
        assertEquals("시점 추가", view.addLabel)
        assertEquals("1-3 값 고치기", view.editLabel)
    }

    @Test
    fun `TC-11-03 a field with history offers the two buttons at 최신`() {
        val view = TimedEditModel.fieldView(doc, affiliation, latest)!!
        assertEquals("소속", view.title)
        assertEquals("등불 결사", view.currentText)
        assertNull(view.derived)
        val sheet = (view.mode as EditMode.TwoButtons).sheet
        assertEquals("등불 결사 · 1-3부터", sheet.currentLine)
        assertEquals("이 값 고치기", sheet.correctLabel)
        assertEquals("1-3 기록을 고침 · 1-3부터 새 값", sheet.correctSub)
        assertEquals("여기서부터 바뀜 · 1-7", sheet.changeLabel)
        assertEquals("새 ◆ · 집필 선 1-7 씬 3 뒤", sheet.changeSub)
        assertNull(sheet.banner)
    }

    @Test
    fun `CST-09 a field without history at 최신 has one 이 값 고치기 that writes the base`() {
        val view = TimedEditModel.fieldView(doc, height, latest)!!
        assertEquals(EditMode.Plain, view.mode)
        val edited = TimedEditModel.saveField(doc, height, FieldValue.of(FieldPayload.Number("155")), SaveChoice.PLAIN, latest, CastFx.ctx())
        assertEquals(doc.changes.records.size, edited.changes.records.size)
        assertEquals("155 cm", valueAt(edited, height, latest))
    }

    @Test
    fun `TC-F-02 an unknown value is stored as ??`() {
        val edited = TimedEditModel.saveField(doc, height, FieldValue.UNKNOWN, SaveChoice.PLAIN, latest, CastFx.ctx())
        assertEquals("?? cm", valueAt(edited, height, latest))
    }

    @Test
    fun `TC-11-02 이 값 고치기 overwrites the record in effect`() {
        val edited = TimedEditModel.saveField(doc, affiliation, text("등불 결사 · 견습"), SaveChoice.CORRECT, latest, CastFx.ctx())
        assertEquals(doc.changes.records.size, edited.changes.records.size)
        assertEquals(text("등불 결사 · 견습"), edited.index.record(K.R2)!!.valueAs(FieldValue.serializer()))
        assertEquals(doc.index.record(K.R1), edited.index.record(K.R1))
        assertEquals(doc.index.record(K.R3), edited.index.record(K.R3))
    }

    @Test
    fun `TC-11-03 여기서부터 바뀜 adds a record after the 집필 선`() {
        val edited = TimedEditModel.saveField(doc, affiliation, text("등불 결사 · 견습"), SaveChoice.FROM_HERE, latest, CastFx.ctx())
        assertTrue(added(edited).anchor.sameAs(Anchor.UnitEnd(K.SCENE_1_7_3)))
        assertEquals("등불 결사", valueAt(edited, affiliation, l15))
        assertEquals("등불 결사 · 견습", valueAt(edited, affiliation, latest))
        assertEquals(doc.index.record(K.R2), edited.index.record(K.R2))
    }

    @Test
    fun `TC-11-04 a past-lens edit shows the banner and starts at that unit`() {
        val sheet = (TimedEditModel.fieldView(doc, affiliation, l15)!!.mode as EditMode.TwoButtons).sheet
        // The board's `1-5부터 · 다음 변화 2-5` preview line is not built (feature-cast.md, CST-10 left).
        assertEquals("등불 결사 · 1-3부터", sheet.currentLine)
        assertEquals("1-5 시점 값 고치는 중", sheet.banner?.title)
        assertEquals("고치면 1-5부터 적용", sheet.banner?.sub)
        assertEquals("최신으로", sheet.banner?.action)
        assertEquals("여기서부터 바뀜 · 1-5", sheet.changeLabel)
        val edited = TimedEditModel.saveField(doc, affiliation, text("등불 결사 · 견습"), SaveChoice.FROM_HERE, l15, CastFx.ctx())
        assertTrue(added(edited).anchor.sameAs(Anchor.UnitStart(K.EPISODE_1_5)))
        assertEquals("등불 결사", valueAt(edited, affiliation, l14))
        assertEquals("등불 결사 · 견습", valueAt(edited, affiliation, l15))
    }

    @Test
    fun `시점 추가 writes a point-anchored record from the start of the picked unit`() {
        val edited = TimedEditModel.addPoint(doc, height, K.EPISODE_1_5, FieldValue.of(FieldPayload.Number("158")), null, CastFx.ctx())
        assertTrue(added(edited).anchor.sameAs(Anchor.UnitStart(K.EPISODE_1_5)))
        assertEquals("154 cm", valueAt(edited, height, l14))
        assertEquals("158 cm", valueAt(edited, height, l15))
    }

    @Test
    fun `TC-11-05 the 표시 이름 sheet of 하르트 reads ??? with history`() {
        val view = TimedEditModel.displayNameView(doc, hartName, latest)!!
        assertEquals("표시 이름", view.title)
        assertEquals("???", view.currentText)
        assertTrue(view.mode is EditMode.TwoButtons)
        assertEquals("???", TimedEditModel.displayNameDraft(doc, hartName, latest))
        val history = TimedEditModel.history(doc, hartName, latest)!!
        assertEquals("표시 이름 · 변화 기록", history.title)
        assertEquals("하르트", history.rows.last().value)
    }

    @Test
    fun `CST-11 a blank display name shows the name`() {
        val edited = TimedEditModel.saveDisplayName(doc, hartName, "  ", SaveChoice.PLAIN, latest, CastFx.ctx())
        assertNull(edited.index.character(K.HART)!!.displayNameBase)
        assertEquals("하르트", DisplayNames.read(edited, K.HART, latest).shown)
        val titled = TimedEditModel.saveDisplayName(doc, TimedSubject.DisplayName(K.RIEN), "등불지기", SaveChoice.PLAIN, latest, CastFx.ctx())
        assertEquals("등불지기", DisplayNames.read(titled, K.RIEN, latest).shown)
    }

    @Test
    fun `a 계산 age is shown read-only with its source`() {
        val age = TimedSubject.Field(EntityKind.CHARACTER, K.RIEN, K.FIELD_AGE)
        val view = TimedEditModel.fieldView(doc, age, latest)!!
        assertEquals("17세", view.currentText)
        assertNotNull(view.derived)
        assertTrue(view.derived!!, view.derived!!.startsWith("계산"))
    }

    @Test
    fun `sheets of a gone field or character close`() {
        assertNull(TimedEditModel.fieldView(doc, affiliation.copy(defKey = "gone"), latest))
        assertNull(TimedEditModel.history(doc, TimedSubject.DisplayName("gone"), latest))
    }
}
