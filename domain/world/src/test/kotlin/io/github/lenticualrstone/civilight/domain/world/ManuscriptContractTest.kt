package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.K
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.T1
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.ctx
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.queries
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.stateA
import io.github.lenticualrstone.civilight.domain.world.term.ManuscriptMarks
import io.github.lenticualrstone.civilight.domain.world.term.ScriptSpot
import io.github.lenticualrstone.civilight.domain.world.term.StaleRejections
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.domain.world.time.WorldClock
import io.github.lenticualrstone.civilight.domain.world.time.WorldTimeEdits
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.CalendarMonth
import io.github.lenticualrstone.civilight.model.project.MonthDay
import io.github.lenticualrstone.civilight.model.project.WorldCalendar
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-27 manuscript display contract, WLD-22 stale `아님` release, WLD-30 calendar save. */
class ManuscriptContractTest {
    private val b12 = ScriptSpot.BlockText(K.BLOCK_B12)
    private val b42 = ScriptSpot.BlockText(K.BLOCK_B42)
    private val b44 = ScriptSpot.BlockText(K.BLOCK_044)

    @Test
    fun `WLD-27 a pre-reveal link carries the margin warning, the reveal block does not`() {
        val marks = ManuscriptMarks(queries(stateA()))
        val at42 = marks.at(b42)
        val link = at42.links.single { it.termKey == T1 }
        assertTrue(link.preReveal)
        assertFalse(link.intended)
        assertTrue(at42.marginWarning)
        assertFalse(marks.at(b44).links.single { it.termKey == T1 }.preReveal)
        assertFalse(marks.at(b44).marginWarning)
    }

    @Test
    fun `WLD-27 and SCR-56 the candidate span and its popover`() {
        val marks = ManuscriptMarks(queries(stateA()))
        val mark = marks.at(b12).candidates.single()
        assertEquals("오리지늄 등불", mark.termName)
        assertEquals("후보 · 끊김", mark.tag)
        assertEquals(10 to 13, mark.start to mark.end)
        val popover = marks.popover(mark.candidate)!!
        assertEquals("오리지늄 등불", popover.title)
        assertEquals("후보 · 끊김", popover.tag)
        assertEquals("첫 공개 1-7 · 잇기 시", popover.revealLine)
        assertEquals("▲ 공개 전 사용", popover.warning)
        assertEquals("잇기" to "아님", popover.linkLabel to popover.rejectLabel)
    }

    @Test
    fun `WLD-27 a 맞음 link keeps the link look and a rejected candidate is gone`() {
        val a = stateA()
        val q = queries(a)
        val link = q.usage(T1).first { it.spot == b42 }
        val marked = TermEdits.markIntended(a, link, ctx())
        val at42 = ManuscriptMarks(queries(marked)).at(b42)
        assertTrue(at42.links.single { it.termKey == T1 }.intended)
        assertFalse(at42.marginWarning)
        val candidate = q.candidates(T1).single()
        val rejected = TermEdits.reject(a, candidate, ctx())
        val after = ManuscriptMarks(queries(rejected))
        assertTrue(after.at(b12).candidates.isEmpty())
        assertNull(after.popover(candidate))
    }

    @Test
    fun `WLD-27 a hidden term's link is masked while locked`() {
        val hidden = WorldFixture.withTerm(stateA(), T1) { it.copy(hidden = true) }
        val locked = ManuscriptMarks(queries(hidden)).at(b42).links.single { it.termKey == T1 }
        assertTrue(locked.masked)
        assertFalse(locked.preReveal)
        val open = ManuscriptMarks(queries(hidden, TermAccess(hiddenUnlocked = true))).at(b42).links.single { it.termKey == T1 }
        assertFalse(open.masked)
        assertTrue(open.preReveal)
    }

    @Test
    fun `WLD-T07 stale release keeps a rejection whose text is still there`() {
        val rejected = rejectedB12()
        val edited = WorldFixture.withBlockText(rejected, K.BLOCK_B12, "등불 속 그 돌, 오리지… 뭐였죠?")
        assertSame(edited, StaleRejections.release(edited, spots = setOf(b12)))
        assertTrue(StaleRejections.stale(edited).isEmpty())
    }

    @Test
    fun `WLD-T08 stale release drops a rejection whose matched text changed, on a fresh scan`() {
        val rejected = rejectedB12()
        val edited = WorldFixture.withBlockText(rejected, K.BLOCK_B12, "등불 속 그 돌, 오리… 뭐라고 했죠?")
        val key = rejected.index.term(T1)!!.rejections.single().key
        assertEquals(setOf(key), StaleRejections.stale(edited))
        val released = StaleRejections.release(edited, spots = setOf(b12))
        assertTrue(released.index.term(T1)!!.rejections.isEmpty())
        assertEquals("오리", queries(released).candidates(T1).single().matchedText)
        // Spots outside the edit are left alone.
        assertSame(edited, StaleRejections.release(edited, spots = setOf(b42)))
    }

    @Test
    fun `WLD-T52 the saved calendar drives the clock`() {
        val calendar = WorldCalendar(
            months = listOf(CalendarMonth("가달", 30), CalendarMonth("나달", 31), CalendarMonth("다달", 30), CalendarMonth("라달", 29)),
            weekdays = listOf("해", "달", "별"),
            storyStart = MonthDay(1, 1),
            storyStartWeekday = 1,
        )
        val doc = WorldTimeEdits.setMode(WorldTimeEdits.setCalendar(stateA(), calendar), WorldTimeMode.CALENDAR)
        assertEquals(WorldTimeMode.CALENDAR.open(), doc.project.worldTime.mode)
        val clock = WorldClock(doc.project.worldTime)
        assertNotNull(clock.calendar)
        assertEquals("나달 16일", clock.dateLabel(clock.dateOfOffset(45)!!))
        assertEquals("해", clock.weekdayOf(45))
        assertSame(doc, WorldTimeEdits.setCalendar(doc, calendar))
    }

    @Test
    fun `WLD-63 the locked 이곳의 시간 leaves hidden characters out of scene sub lines`() {
        val base = WorldFixture.stateB()
        val hidden = base.copy(cast = base.cast.copy(characters = base.cast.characters.map { if (it.key == K.RIEN) it.copy(hidden = true) else it }))
        val engine = io.github.lenticualrstone.civilight.domain.world.time.StoryEngine(hidden)
        val times = io.github.lenticualrstone.civilight.domain.world.track.PlaceTimes(hidden, engine, engine)
        fun subs(rows: List<io.github.lenticualrstone.civilight.domain.world.track.PlaceTimeRow>) =
            rows.filterIsInstance<io.github.lenticualrstone.civilight.domain.world.track.PlaceTimeRow.SceneRow>().map { it.sub }
        val lens = io.github.lenticualrstone.civilight.model.position.LensPoint.Latest
        assertTrue(subs(times.rows(K.PLACE_LOWER_STREET, lens, hiddenUnlocked = false)).none { it.contains("리엔") })
        assertTrue(subs(times.rows(K.PLACE_LOWER_STREET, lens, hiddenUnlocked = true)).any { it.contains("리엔") })
        assertEquals(times.rows(K.PLACE_LOWER_STREET, lens), times.rows(K.PLACE_LOWER_STREET, lens, hiddenUnlocked = true))
    }

    private fun rejectedB12() = stateA().let { a ->
        TermEdits.reject(a, queries(a).candidates(T1).single { it.spot == b12 }, ctx())
    }
}
