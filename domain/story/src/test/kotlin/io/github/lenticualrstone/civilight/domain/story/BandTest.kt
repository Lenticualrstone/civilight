package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.Fx.S8
import io.github.lenticualrstone.civilight.domain.story.Fx.S9
import io.github.lenticualrstone.civilight.domain.story.Fx.clock
import io.github.lenticualrstone.civilight.domain.story.Fx.rel
import io.github.lenticualrstone.civilight.domain.story.Fx.unit
import io.github.lenticualrstone.civilight.domain.story.Fx.withTime
import io.github.lenticualrstone.civilight.domain.story.time.BandTable
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.Span
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.DayBoundary
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 20-story-time §4.5 BAND-01 … BAND-05. */
class BandTest {
    private val doc = Fx.doc()
    private val bands = BandTable()

    @Test
    fun band01_rangesAndClocks() {
        assertEquals(Span(0, 120), bands.range(Band.DAWN))
        assertEquals(Span(120, 360), bands.range(Band.MORNING))
        assertEquals(Span(360, 780), bands.range(Band.DAY))
        assertEquals(Span(780, 960), bands.range(Band.EVENING))
        assertEquals(Span(960, 1440), bands.range(Band.NIGHT))
        assertEquals(Band.DAWN, bands.bandOf(clock(5, 40)))
        assertEquals(Band.MORNING, bands.bandOf(clock(6, 10)))
        assertEquals(Band.EVENING, bands.bandOf(clock(17, 0)))
        assertEquals(Band.NIGHT, bands.bandOf(clock(3, 59)))
        assertEquals(Band.DAWN, bands.bandOf(clock(4, 0)))
        assertEquals(Band.NIGHT, bands.bandOf(clock(23, 0)))
    }

    private val nightScene = doc.withTime(S9, rel(S8, 0, clock = clock(1, 30)))

    @Test
    fun band02_after4amDayBoundary() {
        assertEquals("1일째 밤 01:30", Fx.labels(nightScene).head(S9))
    }

    @Test
    fun band03_midnightBoundaryChangesOnlyLabels() {
        val midnight = nightScene.copy(
            project = nightScene.project.copy(sceneTime = nightScene.project.sceneTime.copy(dayBoundary = DayBoundary.MIDNIGHT.open())),
        )
        assertEquals("2일째 밤 01:30", Fx.labels(midnight).head(S9))
        val before = CivilightJson.encode(StoryUnit.serializer(), nightScene.unit(S9))
        val after = CivilightJson.encode(StoryUnit.serializer(), midnight.unit(S9))
        assertEquals(before, after)
        assertEquals(SceneTimes(nightScene).timed(S9)!!.absolute, SceneTimes(midnight).timed(S9)!!.absolute)
    }

    @Test
    fun band04_clockWinsOverBand() {
        val v = doc.withTime(S9, rel(S8, 1, Band.DAWN, clock(7, 0)))
        val times = SceneTimes(v)
        assertEquals(Span.point(180), times.timed(S9)!!.t)
        assertEquals("2일째 새벽 07:00", Fx.labels(v).head(S9))
        assertTrue(times.clockBandMismatch(S9))
        assertFalse(SceneTimes(doc).clockBandMismatch(S9))
    }

    @Test
    fun band05_bandDerivedFromClock() {
        assertEquals(Band.DAWN, SceneTimes(doc).timed(S9)!!.band)
        assertEquals("2일째 새벽 05:40", Fx.labels(doc).head(S9))
    }
}
