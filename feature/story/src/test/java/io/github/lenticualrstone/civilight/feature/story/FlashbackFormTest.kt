package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.story.time.CascadeMode
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.feature.story.Fx.unit
import io.github.lenticualrstone.civilight.feature.story.where.FlashbackAction
import io.github.lenticualrstone.civilight.feature.story.where.FlashbackModel
import io.github.lenticualrstone.civilight.feature.story.where.TimeStep
import io.github.lenticualrstone.civilight.feature.story.where.WhereModel
import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.project.CalendarMonth
import io.github.lenticualrstone.civilight.model.project.MonthDay
import io.github.lenticualrstone.civilight.model.project.WorldCalendar
import io.github.lenticualrstone.civilight.model.project.WorldTime
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.story.CalendarDate
import io.github.lenticualrstone.civilight.model.story.FlashbackMode
import io.github.lenticualrstone.civilight.model.story.FlashbackRelation
import io.github.lenticualrstone.civilight.model.story.SceneTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** STO-24 회상 form in the `장소와 때` sheet (board `RTFlashback`; Fixture T S5 = 1-4 씬 2). */
class FlashbackFormTest {
    private val doc = Fx.doc()
    private val s5 = K.SCENE_1_4_2

    /** Stores [action] the way the sheet does: through `WhereModel.step` (no cascade for a flashback). */
    private fun ProjectDoc.after(action: FlashbackAction): ProjectDoc {
        val next = FlashbackModel.next(this, s5, action)!!
        val step = WhereModel.step(this, s5, next)
        assertTrue("a flashback edit never opens the cascade: $step", step is TimeStep.Direct)
        return WhereModel.setTime(this, s5, next, CascadeMode.WITH_FOLLOWING, Fx.ctx())
    }

    @Test
    fun time12_formOfTheFixtureFlashback() {
        val f = FlashbackModel.ui(doc, s5)!!
        assertEquals(FlashbackMode.ANCHOR, f.mode)
        assertEquals("대정전", f.eventName)
        assertEquals(1195, f.eventYear)
        assertEquals(FlashbackRelation.ON, f.relation)
        assertFalse(f.eventDeleted)
        assertEquals("씬 3 때 기준" to "1-3 씬 2 · 회상 건너뜀", f.skipLine)
        val ui = WhereModel.build(doc, s5, readOnly = false)!!
        assertEquals("연도 1195 · 대정전 그날 | 이야기 시작 5년 전 · 연표에서", ui.resultLine)
        assertNull(FlashbackModel.ui(doc, K.SCENE_1_7_3))
    }

    @Test
    fun relationChipsChangeOnlyTheRelation_STO24() {
        val before = doc.after(FlashbackAction.Relation(FlashbackRelation.BEFORE))
        val t = before.unit(s5).time as SceneTime.Flashback
        assertEquals(FlashbackRelation.BEFORE, t.relation?.known)
        assertEquals(K.EVENT_BLACKOUT, t.eventKey)
        assertEquals("연도 1195 · 대정전 전 | 이야기 시작 5년 전 · 연표에서", WhereModel.build(before, s5, false)!!.resultLine)
        // Nothing else moved: every other scene keeps its stored 때.
        assertEquals(doc.story.units.filter { it.key != s5 }.map { it.time }, before.story.units.filter { it.key != s5 }.map { it.time })
    }

    @Test
    fun directModeStartsAtTheShownYearAndKeepsTheRest() {
        val withClock = doc.copy(
            story = doc.story.copy(
                units = doc.story.units.map {
                    if (it.key == s5) it.copy(time = (it.time as SceneTime.Flashback).copy(band = Band.NIGHT.open(), clock = ClockTime.of(22, 0))) else it
                },
            ),
        )
        val direct = withClock.after(FlashbackAction.Mode(FlashbackMode.DIRECT))
        val t = direct.unit(s5).time as SceneTime.Flashback
        assertEquals(FlashbackMode.DIRECT, t.mode.known)
        assertEquals(1195, t.year)
        assertEquals(Band.NIGHT, t.band?.known)
        assertEquals(ClockTime.of(22, 0), t.clock)
        assertEquals("switching alone changes no label", "연도 1195 | 이야기 시작 5년 전 · 연표에서", WhereModel.build(direct, s5, false)!!.resultLine)
        val year = direct.after(FlashbackAction.Year(1190))
        assertEquals("연도 1190 | 이야기 시작 10년 전 · 연표에서", WhereModel.build(year, s5, false)!!.resultLine)
        assertEquals(1190, FlashbackModel.parseYear(" 1190 "))
        assertNull(FlashbackModel.parseYear("11a"))
    }

    @Test
    fun switchingOnStartsOnTheAnchorEventAndAnEventPickResolves() {
        val s9 = K.SCENE_1_7_3
        val on = WhereModel.setTime(doc, s9, WhereModel.flashbackTime(true), CascadeMode.WITH_FOLLOWING, Fx.ctx())
        val f = FlashbackModel.ui(on, s9)!!
        assertEquals(FlashbackMode.ANCHOR, f.mode)
        assertEquals(FlashbackRelation.ON, f.relation)
        assertNull(f.eventName)
        val picked = FlashbackModel.next(on, s9, FlashbackAction.Event(K.EVENT_BLACKOUT))!!
        val stored = WhereModel.setTime(on, s9, picked, CascadeMode.WITH_FOLLOWING, Fx.ctx())
        assertEquals("연도 1195 · 대정전 그날 | 이야기 시작 5년 전 · 연표에서", WhereModel.build(stored, s9, false)!!.resultLine)
        assertNull("no such event", FlashbackModel.next(on, s9, FlashbackAction.Event("unknown")))
    }

    @Test
    fun aDeletedBaseEventIsMarkedAndTheStoredTimeStays_WLD32() {
        val deleted = TrashOps.delete(doc, ItemId.event(K.EVENT_BLACKOUT), Fx.ctx().trash).doc
        val f = FlashbackModel.ui(deleted, s5)!!
        assertTrue(f.eventDeleted)
        assertEquals("대정전", f.eventName)
        assertEquals(doc.unit(s5).time, deleted.unit(s5).time)
    }

    @Test
    fun withTheCalendarOnDirectTakesADateToo_STO24_STO18() {
        val cal = WorldCalendar(months = listOf(CalendarMonth("서리달", 30), CalendarMonth("잿달", 28)), storyStart = MonthDay(1, 25))
        val calDoc = doc.copy(project = doc.project.copy(worldTime = WorldTime(WorldTimeMode.CALENDAR.open(), 1200, cal)))
        assertNull("calendar off: no date row", FlashbackModel.ui(doc, s5)!!.months)
        assertNull("calendar off: no date", FlashbackModel.next(doc, s5, FlashbackAction.Date(1, 3)))
        assertEquals(listOf("서리달", "잿달"), FlashbackModel.ui(calDoc, s5)!!.months!!.map { it.name })
        val dated = calDoc.after(FlashbackAction.Date(2, 5))
        val t = dated.unit(s5).time as SceneTime.Flashback
        assertEquals(FlashbackMode.DIRECT, t.mode.known)
        assertEquals("the shown year stays", 1195, t.year)
        assertEquals(CalendarDate(2, 5, yearOffset = -5), t.calendarDate)
        assertEquals(CalendarDate(2, 5, yearOffset = -5), FlashbackModel.ui(dated, s5)!!.date)
        assertNull("잿달 has 28 days", FlashbackModel.next(dated, s5, FlashbackAction.Date(2, 29)))
        assertNull("no third month", FlashbackModel.next(dated, s5, FlashbackAction.Date(3, 1)))
        val moved = dated.after(FlashbackAction.Year(1190))
        assertEquals(CalendarDate(2, 5, yearOffset = -10), (moved.unit(s5).time as SceneTime.Flashback).calendarDate)
        assertEquals(12, FlashbackModel.parseDay(" 12 ", 28))
        assertNull(FlashbackModel.parseDay("29", 28))
    }
}
