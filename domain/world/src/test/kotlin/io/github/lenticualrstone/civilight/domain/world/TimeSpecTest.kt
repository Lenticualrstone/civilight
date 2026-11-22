package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.K
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.ctx
import io.github.lenticualrstone.civilight.domain.world.time.Ages
import io.github.lenticualrstone.civilight.domain.world.time.CalendarDay
import io.github.lenticualrstone.civilight.domain.world.time.EventEdit
import io.github.lenticualrstone.civilight.domain.world.time.EventEdits
import io.github.lenticualrstone.civilight.domain.world.time.Moments
import io.github.lenticualrstone.civilight.domain.world.time.StoryEngine
import io.github.lenticualrstone.civilight.domain.world.time.TimeLabel
import io.github.lenticualrstone.civilight.domain.world.time.TimeSubject
import io.github.lenticualrstone.civilight.domain.world.time.TimelineBuilder
import io.github.lenticualrstone.civilight.domain.world.time.TimelineRow
import io.github.lenticualrstone.civilight.domain.world.time.TimelineView
import io.github.lenticualrstone.civilight.domain.world.time.WorldClock
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.CalendarMonth
import io.github.lenticualrstone.civilight.model.project.MonthDay
import io.github.lenticualrstone.civilight.model.project.WorldCalendar
import io.github.lenticualrstone.civilight.model.project.WorldTime
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.world.WorldDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD §4.6 time, timeline and ages (WLD-T48 … WLD-T52). */
class TimeSpecTest {
    private val doc = ExampleProject.build()
    private val engine = StoryEngine(doc)
    private val moments = Moments(doc, engine)

    private fun scene(key: String) = TimeSubject.Scene(key)

    @Test
    fun `WLD-T48 ages`() {
        val ages = Ages(doc)
        val rien = doc.index.character(K.RIEN)!!
        val sera = doc.index.character(K.SERA)!!
        val hart = doc.index.character(K.HART)!!
        assertEquals("리엔 12세 · 세라 16세", ages.line(listOf(rien, sera), 1195))
        assertEquals("하르트 ??세", ages.label(hart, 1195))
        assertEquals("리엔 17세", ages.label(rien, 1200))
    }

    @Test
    fun `WLD-T49 story-start view labels from the time engine`() {
        val labels = listOf(K.SCENE_1_2_1, K.SCENE_1_3_2, K.SCENE_1_6_1, K.SCENE_1_7_1, K.SCENE_1_7_3)
            .map { moments.storyStartLabel(scene(it)) }
        assertEquals(
            listOf(
                TimeLabel("2일째"),
                TimeLabel("3일째"),
                TimeLabel("약 10일째 (8~12)"),
                TimeLabel("약 12일째 (10~14)"),
                TimeLabel("약 13일째 (11~15)"),
            ),
            labels,
        )
    }

    @Test
    fun `WLD-T50 anchor view`() {
        val anchor = TimeSubject.Event(K.EVENT_JOINS_SOCIETY)
        assertEquals(TimeLabel("−5년"), moments.anchorLabel(TimeSubject.Event(K.EVENT_BLACKOUT), anchor))
        assertEquals(TimeLabel("−1일"), moments.anchorLabel(scene(K.SCENE_1_2_1), anchor))
        assertEquals(TimeLabel("기준"), moments.anchorLabel(scene(K.SCENE_1_3_2), anchor))
        assertEquals(TimeLabel("+약 7일", "5~9일"), moments.anchorLabel(scene(K.SCENE_1_6_1), anchor))
        assertEquals("+약 9일", moments.anchorLabel(scene(K.SCENE_1_7_1), anchor)!!.text)
        assertEquals("+약 10일", moments.anchorLabel(scene(K.SCENE_1_7_3), anchor)!!.text)
        // The timeline in that view labels the world event the same way.
        val timeline = TimelineBuilder(doc, engine).build(view = TimelineView.ANCHOR, anchor = anchor)
        val blackout = timeline.rows.filterIsInstance<TimelineRow.EventRow>().single { it.event.key == K.EVENT_BLACKOUT }
        assertEquals("−5년", blackout.time.text)
    }

    @Test
    fun `anchor on an approximate scene cancels the shared unknown`() {
        // 1-7 씬 1 is 1-6 씬 1 + 2 days; both hang on the same `대략 +5~9` link.
        assertEquals(TimeLabel("+2일"), moments.anchorLabel(scene(K.SCENE_1_7_1), scene(K.SCENE_1_6_1)))
        assertEquals(TimeLabel("−2일"), moments.anchorLabel(scene(K.SCENE_1_6_1), scene(K.SCENE_1_7_1)))
    }

    @Test
    fun `WLD-T51 mode without time`() {
        val none = doc.copy(project = doc.project.copy(worldTime = WorldTime(mode = WorldTimeMode.NONE.open())))
        assertFalse(TimelineBuilder(none, StoryEngine(none)).build().available)
        assertFalse(WorldClock(none.project.worldTime).hasTimeline)
        assertEquals(EventEdit.NoWorldTime, EventEdits.createWorldEvent(none, "새 사건", WorldDate(1190), ctx()))
        assertTrue(TimelineBuilder(doc, engine).build().available)
    }

    private fun calendarDoc(): ProjectDoc = doc.copy(
        project = doc.project.copy(
            worldTime = WorldTime(
                mode = WorldTimeMode.CALENDAR.open(),
                storyStartYear = 1200,
                calendar = WorldCalendar(
                    months = listOf(CalendarMonth("가달", 30), CalendarMonth("나달", 31), CalendarMonth("다달", 30), CalendarMonth("라달", 29)),
                    weekdays = listOf("해", "달", "별"),
                    storyStart = MonthDay(1, 1),
                    storyStartWeekday = 1,
                ),
            ),
        ),
    )

    @Test
    fun `WLD-T52 calendar arithmetic`() {
        val clock = WorldClock(calendarDoc().project.worldTime)
        assertEquals(120, clock.yearLength)
        val day45 = clock.dateOfOffset(45)!!
        assertEquals(CalendarDay(1200, 2, 16), day45)
        assertEquals("나달 16일", clock.dateLabel(day45))
        assertEquals("해", clock.weekdayOf(45))
        assertEquals(CalendarDay(1201, 1, 1), clock.dateOfOffset(120))
        assertEquals(45, clock.offsetOf(CalendarDay(1200, 2, 16)))
        assertEquals("달", clock.weekdayOf(46))
    }

    @Test
    fun `calendar mode keeps month and day of world events and ages use full years`() {
        val cal = calendarDoc()
        val created = EventEdits.createWorldEvent(cal, "등불 축제", WorldDate(1199, 3, 10), ctx()) as EventEdit.Applied
        assertEquals(WorldDate(1199, 3, 10), created.event.worldDate)
        val yearOnly = EventEdits.createWorldEvent(doc, "등불 축제", WorldDate(1199, 3, 10), ctx()) as EventEdit.Applied
        assertEquals(WorldDate(1199), yearOnly.event.worldDate)
        // 리엔: born 1183 (birthday 3/14). In calendar mode, on 3/10 of 1200 she is still 16.
        val ages = Ages(cal)
        val rien = cal.index.character(K.RIEN)!!
        assertEquals("리엔 16세", ages.label(rien, 1200, 3, 10))
        assertEquals("리엔 17세", ages.label(rien, 1200, 3, 14))
        assertEquals("리엔 17세", Ages(doc).label(rien, 1200, 3, 10))
    }

    @Test
    fun `timeline puts world events before the story then chapter and story rows in time order`() {
        val rows = TimelineBuilder(doc, engine).build().rows
        val firstEvent = rows.indexOfFirst { it is TimelineRow.EventRow && it.event.key == K.EVENT_BLACKOUT }
        val divider = rows.indexOfFirst { it is TimelineRow.Divider }
        val joins = rows.indexOfFirst { it is TimelineRow.EventRow && it.event.key == K.EVENT_JOINS_SOCIETY }
        val accident = rows.indexOfFirst { it is TimelineRow.EventRow && it.event.key == K.EVENT_GRID_ACCIDENT }
        assertTrue(firstEvent in 0 until divider)
        assertEquals("이야기 시작 · 1장", (rows[divider] as TimelineRow.Divider).text)
        assertTrue(joins > divider && accident > joins)
        val accidentRow = rows[accident] as TimelineRow.EventRow
        assertTrue(accidentRow.sub, accidentRow.sub.endsWith("2장에서 공개 예정"))
        assertTrue(accidentRow.dimmed)
        val blackoutRow = rows[firstEvent] as TimelineRow.EventRow
        assertTrue(blackoutRow.sub, blackoutRow.sub.contains("1-4에서 회상"))
        val chapter1 = rows.filterIsInstance<TimelineRow.ChapterRow>().first()
        assertEquals("1200", chapter1.time.text)
        assertTrue(chapter1.sub, chapter1.sub.startsWith("1-1 ~ 1-10"))
    }
}
