package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.feature.story.Fx.unit
import io.github.lenticualrstone.civilight.feature.story.newunit.NewSceneForm
import io.github.lenticualrstone.civilight.feature.story.newunit.NewSceneForms
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.navigation.NewUnitSheet
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** STO-05 / STO-21: editing 때 in the new-scene sheet (`같은 날 · +1일 · +2일` and the band chips). */
class NewSceneTimeTest {
    private val doc = Fx.doc()
    private val p = K.PROJECT
    private val s8 = K.SCENE_1_7_2
    private val sheet = NewUnitSheet(p, parentKey = K.EPISODE_1_7, afterKey = s8)
    private val setup = NewSceneForms.setup(doc, sheet)!!

    @Test
    fun thePrefillReadsAsTheSameDayChip() {
        assertEquals(s8, NewSceneForms.baseKey(doc, setup))
        assertEquals(0, NewSceneForms.dayChipOf(doc, setup, NewSceneForm()))
        assertEquals(Band.EVENING, NewSceneForms.bandOf(doc, setup, NewSceneForm()))
    }

    @Test
    fun aDayChipAndABandMakeTheBaseRelativeTimeWithoutTheOldClock() {
        val form = NewSceneForm(dayChip = 1, band = Band.DAWN.name)
        assertEquals("씬 2 기준 · +1일 · 새벽", NewSceneForms.lines(doc, setup, form).time)
        val (after, n) = NewSceneForms.create(doc, sheet, form, Fx.ctx())!!
        val t = after.unit(n).time as SceneTime.Relative
        assertEquals(s8, t.baseKey)
        assertEquals(1 to 1, t.dayMin to t.dayMax)
        assertEquals(Band.DAWN, t.band?.known)
        assertNull(t.clock)
        assertEquals("1-7 · 2일째 · 새벽", TimeLabels(SceneTimes(after)).full(n))
    }

    @Test
    fun clearingWinsAndAnEmptyEpisodeOffersNoChips() {
        assertEquals(SceneTime.None, NewSceneForms.time(doc, setup, NewSceneForm(timeCleared = true, dayChip = 2)))
        val empty = Fx.mini(listOf("에피소드", "씬"), Fx.node("E1", "하나"))
        val emptySetup = NewSceneForms.setup(empty, NewUnitSheet(empty.project.key, parentKey = "E1"))!!
        assertNull(NewSceneForms.baseKey(empty, emptySetup))
        assertEquals(SceneTime.None, NewSceneForms.time(empty, emptySetup, NewSceneForm(dayChip = 1)))
    }
}
