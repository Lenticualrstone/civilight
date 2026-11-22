package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.Fx.unit
import io.github.lenticualrstone.civilight.domain.story.structure.UnitSettingsEdit
import io.github.lenticualrstone.civilight.domain.story.time.BandTable
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.DayBoundary
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** STO-19 `하루 · 시간대` setters and the `씬도 숨기기` flag (additions for feature:story). */
class DaySettingsEditTest {
    private val doc = Fx.doc()
    private val ctx = Fx.ctx()

    @Test
    fun dayBoundaryChangesLabelsOnly_BAND() {
        val midnight = UnitSettingsEdit.setDayBoundary(doc, DayBoundary.MIDNIGHT, ctx)
        assertEquals(DayBoundary.MIDNIGHT, midnight.project.sceneTime.dayBoundary.known)
        assertEquals(doc.story.units.map { it.time }, midnight.story.units.map { it.time })
        assertSame(midnight, UnitSettingsEdit.setDayBoundary(midnight, DayBoundary.MIDNIGHT, ctx))
    }

    @Test
    fun movingABoundaryMovesTheNeighboursEnd() {
        val moved = UnitSettingsEdit.setBandStart(doc, Band.EVENING, ClockTime.of(18, 0), ctx)
        val table = BandTable(moved.project.sceneTime.bandStarts)
        assertEquals("10–18", table.hoursLabel(Band.DAY))
        assertEquals("18–20", table.hoursLabel(Band.EVENING))
        assertEquals(Band.DAY, table.bandOf(ClockTime.of(17, 30)))
        // Bands stay contiguous and cover the day: every minute has exactly one band.
        val covered = (0 until 1440).map { table.bandOf(ClockTime(it)) }
        assertEquals(Band.entries.toSet(), covered.toSet())
        assertEquals(doc.story.units.map { it.time }, moved.story.units.map { it.time })
    }

    @Test
    fun aBoundaryCannotPassItsNeighboursNorMoveTheDayStart() {
        val starts = doc.project.sceneTime.bandStarts
        assertFalse(UnitSettingsEdit.canSetBandStart(starts, Band.DAWN, ClockTime.of(3, 30)))
        assertFalse(UnitSettingsEdit.canSetBandStart(starts, Band.MORNING, ClockTime.of(4, 0)))
        assertFalse(UnitSettingsEdit.canSetBandStart(starts, Band.MORNING, ClockTime.of(10, 0)))
        assertTrue(UnitSettingsEdit.canSetBandStart(starts, Band.MORNING, ClockTime.of(5, 30)))
        // 밤 may start after midnight (it is still before the next 04:00 day start).
        assertTrue(UnitSettingsEdit.canSetBandStart(starts, Band.NIGHT, ClockTime.of(1, 0)))
        assertFalse(UnitSettingsEdit.canSetBandStart(starts, Band.NIGHT, ClockTime.of(4, 0)))
        assertSame(doc, UnitSettingsEdit.setBandStart(doc, Band.DAWN, ClockTime.of(3, 30), ctx))
        assertSame(doc, UnitSettingsEdit.setBandStart(doc, Band.DAY, ClockTime.of(5, 0), ctx))
    }

    @Test
    fun aNightAfterMidnightKeepsBandRangesInsideTheDay() {
        val late = UnitSettingsEdit.setBandStart(doc, Band.NIGHT, ClockTime.of(1, 0), ctx)
        val table = BandTable(late.project.sceneTime.bandStarts)
        assertEquals(Band.EVENING, table.bandOf(ClockTime.of(23, 0)))
        assertEquals(Band.NIGHT, table.bandOf(ClockTime.of(2, 0)))
        assertEquals(1260 to 1440, table.range(Band.NIGHT).let { it.min to it.max })
        SceneTimes(late).timed(K.SCENE_1_7_3)
    }

    @Test
    fun hidingASceneSetsOnlyItsFlag() {
        val hidden = UnitSettingsEdit.setHidden(doc, K.SCENE_1_7_4, true, ctx)
        assertTrue(hidden.unit(K.SCENE_1_7_4).hidden)
        assertSame(hidden, UnitSettingsEdit.setHidden(hidden, K.SCENE_1_7_4, true, ctx))
        assertSame(doc, UnitSettingsEdit.setHidden(doc, "unknown", true, ctx))
    }
}
