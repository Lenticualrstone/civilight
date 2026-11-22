package io.github.lenticualrstone.civilight.feature.world.time

import io.github.lenticualrstone.civilight.domain.world.time.WorldClock
import io.github.lenticualrstone.civilight.domain.world.time.WorldTimeEdits
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-30 세계 달력 editor. */
class CalendarDraftTest {
    private fun t52(): CalendarDraft = CalendarDraft()
        .setMonth(0, MonthDraft("가달", "30"))
        .addMonth().setMonth(1, MonthDraft("나달", "31"))
        .addMonth().setMonth(2, MonthDraft("다달", "30"))
        .addMonth().setMonth(3, MonthDraft("라달", "29"))
        .addWeekday().setWeekday(0, "해")
        .addWeekday().setWeekday(1, "달")
        .addWeekday().setWeekday(2, "별")

    @Test
    fun `WLD-T52 the edited calendar gives 나달 16일 on 해 at offset 45`() {
        val draft = t52()
        assertNull(draft.problem())
        val doc = WorldTimeEdits.setMode(
            WorldTimeEdits.setCalendar(WorldTestFixture.stateB(), draft.toCalendar()!!),
            WorldTimeMode.CALENDAR,
        )
        val clock = WorldClock(doc.project.worldTime)
        assertEquals(120, clock.yearLength)
        assertEquals("나달 16일", clock.dateLabel(clock.dateOfOffset(45)!!))
        assertEquals("해", clock.weekdayOf(45))
        assertEquals(draft, CalendarDraft.of(doc.project.worldTime.calendar))
    }

    @Test
    fun `problems stop 저장`() {
        assertEquals(WorldCopy.TODO_COPY_WORLD_CALENDAR_MONTH_NAME, CalendarDraft().problem())
        assertEquals(WorldCopy.TODO_COPY_WORLD_CALENDAR_MONTH_DAYS, CalendarDraft(listOf(MonthDraft("가달", "0"))).problem())
        assertEquals(WorldCopy.TODO_COPY_WORLD_CALENDAR_WEEKDAY_NAME, t52().addWeekday().problem())
        assertEquals(WorldCopy.TODO_COPY_WORLD_CALENDAR_START, t52().copy(startDay = "31").problem())
        assertEquals(WorldCopy.TODO_COPY_WORLD_CALENDAR_NO_MONTH, CalendarDraft(emptyList()).problem())
        assertNull(CalendarDraft().toCalendar())
    }

    @Test
    fun `removing a month or weekday keeps the start on the same row`() {
        val draft = t52().copy(startMonth = 3, startWeekday = 3)
        val fewer = draft.removeMonth(0).removeWeekday(0)
        assertEquals(2, fewer.startMonth)
        assertEquals(2, fewer.startWeekday)
        assertEquals("다달", fewer.months[fewer.startMonth - 1].name)
        assertEquals(1, draft.removeMonth(2).startMonth)
    }

    @Test
    fun `calendar mode shows the editor even before any month exists (new project step 3)`() {
        val doc = WorldTimeEdits.setMode(WorldTestFixture.stateB(), WorldTimeMode.CALENDAR)
        val ui = WorldTimeState.build(doc, readOnly = false)
        assertTrue(ui.showCalendarEditor)
        assertEquals(CalendarDraft(), CalendarDraft.of(ui.calendar))
    }
}
