package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.IntSpan
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.story.SceneTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** The adapter from the place & time engine agrees with the resolved table of SCR §4.1 F1. */
class StoryTimesTest {
    private val doc = F1.build()
    private val times = StoryTimes.source(doc)

    @Test
    fun `engine times equal the F1 table`() {
        for ((key, expected) in F1.times) assertEquals("scene $key", expected, times.timeOf(key))
    }

    @Test
    fun `a wide inherited day range is still exact precision`() {
        val s4 = times.timeOf(F1.S4)!!
        assertEquals(IntSpan(11, 15), s4.storyDay)
        assertEquals(2, s4.episodeDay)
        assertEquals(false, s4.approx)
        assertEquals(false, s4.episodeDayApprox)
    }

    @Test
    fun `own approximate link and untimed and flashback scenes`() {
        val market = times.timeOf(K.SCENE_1_6_1)!!
        assertTrue(market.approx)
        assertEquals(1, market.episodeDay)
        assertEquals(Band.DAY, market.band)
        assertNull(times.timeOf(K.SCENE_1_3_1))
        val flashback = times.timeOf(K.SCENE_1_4_2)!!
        assertTrue(flashback.flashback)
        assertEquals(1195, flashback.flashbackYear)
        assertTrue(doc.unit(K.SCENE_1_4_2).time is SceneTime.Flashback)
    }

    @Test
    fun `the fixture snapshot matches its scene so no stale transition fires`() {
        assertTrue(ScriptChecks.transitionStale(doc, times).isEmpty())
        assertTrue(ScriptChecks.transitionStale(F1.raw(), StoryTimes.source(F1.raw())).isEmpty())
    }

    @Test
    fun `no time-order mismatch in the fixture`() {
        assertEquals(0, StoryTimes.orderMismatches(doc))
    }
}
