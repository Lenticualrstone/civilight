package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.script.BlockOps
import io.github.lenticualrstone.civilight.domain.script.BlockPosition
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.domain.story.transition.Transitions
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.feature.story.Fx.node
import io.github.lenticualrstone.civilight.feature.story.Fx.unit
import io.github.lenticualrstone.civilight.feature.story.newunit.NewSceneForm
import io.github.lenticualrstone.civilight.feature.story.newunit.NewSceneForms
import io.github.lenticualrstone.civilight.feature.story.newunit.PlaceChoice
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.navigation.NewUnitSheet
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** `다음 씬 만들기` · `앞에 새 씬` (STO-05; 20-story-time INS-01, TIME-12; R22 §4). */
class NewSceneFormTest {
    private val doc = Fx.doc()
    private val p = K.PROJECT
    private val s8 = K.SCENE_1_7_2
    private val s9 = K.SCENE_1_7_3

    private fun assertOtherAbsolutesUnchanged(before: ProjectDoc, after: ProjectDoc, except: String) {
        val a = SceneTimes(before)
        val b = SceneTimes(after)
        for (scene in a.liveScenes) {
            if (scene.key == except) continue
            assertEquals("absolute of ${scene.key}", a.timed(scene.key)?.absolute, b.timed(scene.key)?.absolute)
        }
    }

    @Test
    fun ins01_keptPrefillIsSameTimeAsTheSceneBefore() {
        val sheet = NewUnitSheet(p, parentKey = K.EPISODE_1_7, afterKey = s8)
        val setup = NewSceneForms.setup(doc, sheet)!!
        assertEquals("다음 씬 만들기", setup.title)
        assertEquals("1-7 · 씬 2 뒤", setup.subtitle)
        val lines = NewSceneForms.lines(doc, setup, NewSceneForm())
        assertEquals("미정", lines.title)
        assertEquals("상층 관문 · 씬 2와 같음", lines.place)
        assertEquals("씬 2 기준 · 같은 때", lines.time)
        assertTrue(lines.placeClearable && lines.timeClearable)

        val (after, n) = NewSceneForms.create(doc, sheet, NewSceneForm(), Fx.ctx())!!
        val time = after.unit(n).time as SceneTime.Relative
        assertEquals(s8, time.baseKey)
        assertEquals(0 to 0, time.dayMin to time.dayMax)
        assertEquals(s8, (after.unit(s9).time as SceneTime.Relative).baseKey)
        assertOtherAbsolutesUnchanged(doc, after, n)
        assertEquals("1-7 · 1일째 · 저녁", TimeLabels(SceneTimes(after)).full(n))
        assertEquals("씬 3", UnitCodes(after).sceneLabel(after.unit(n)))
        assertEquals(after.unit(s8).place, after.unit(n).place)
    }

    @Test
    fun ins01_clearedTimeLeavesTheNewSceneWithout때() {
        val sheet = NewUnitSheet(p, parentKey = K.EPISODE_1_7, afterKey = s8)
        val form = NewSceneForm(timeCleared = true, place = PlaceChoice.Cleared)
        val setup = NewSceneForms.setup(doc, sheet)!!
        val lines = NewSceneForms.lines(doc, setup, form)
        assertEquals("때 없음" to "장소 없음", lines.time to lines.place)
        assertFalse(lines.timeClearable || lines.placeClearable)
        val (after, n) = NewSceneForms.create(doc, sheet, form, Fx.ctx())!!
        assertEquals(SceneTime.None, after.unit(n).time)
        assertNull(after.unit(n).place)
        assertEquals(doc.unit(s9).time, after.unit(s9).time)
        assertOtherAbsolutesUnchanged(doc, after, n)
    }

    @Test
    fun time12_afterAFlashbackTheBaseSkipsIt() {
        val sheet = NewUnitSheet(p, parentKey = K.EPISODE_1_4, afterKey = K.SCENE_1_4_2)
        val setup = NewSceneForms.setup(doc, sheet)!!
        assertEquals("1-3 씬 2 기준 · 같은 때", NewSceneForms.lines(doc, setup, NewSceneForm()).time)
        val (after, n) = NewSceneForms.create(doc, sheet, NewSceneForm(), Fx.ctx())!!
        assertFalse(after.unit(n).time is SceneTime.Flashback)
        assertEquals("1-4 · 1일째 · 아침", TimeLabels(SceneTimes(after)).full(n))
    }

    @Test
    fun beforeAScenePrefillsFromTheSceneBeforeIt() {
        val sheet = NewUnitSheet(p, parentKey = K.EPISODE_1_7, beforeKey = K.SCENE_1_7_4)
        val setup = NewSceneForms.setup(doc, sheet)!!
        assertEquals("앞에 새 씬", setup.title)
        assertEquals("1-7 · 씬 4 앞", setup.subtitle)
        assertEquals(s9, setup.afterSceneKey)
        val (after, n) = NewSceneForms.create(doc, sheet, NewSceneForm(title = "  문 앞  "), Fx.ctx())!!
        assertEquals("문 앞", after.unit(n).title)
        assertEquals("씬 4", UnitCodes(after).sceneLabel(after.unit(n)))
        assertEquals("씬 5", UnitCodes(after).sceneLabel(after.unit(K.SCENE_1_7_4)))
        assertEquals(s9, (after.unit(n).time as SceneTime.Relative).baseKey)
        assertOtherAbsolutesUnchanged(doc, after, n)
    }

    @Test
    fun firstSceneOfAnEmptyEpisodeStartsWithNothingPrefilled() {
        val d = Fx.mini(listOf("장", "에피소드", "씬"), node("C", null, null, node("E1", null, null, node("S1", null, "글")), node("E2")))
        val sheet = NewUnitSheet(d.project.key, parentKey = "E2")
        val setup = NewSceneForms.setup(d, sheet)!!
        assertEquals("1-2", setup.subtitle)
        assertNull(setup.afterSceneKey)
        val (after, n) = NewSceneForms.create(d, sheet, NewSceneForm(), Fx.ctx())!!
        assertEquals("E2", after.unit(n).parentKey)
        assertEquals(SceneTime.None, after.unit(n).time)
        // Its first block gets #001 from the episode's counter (domain:script).
        val inserted = BlockOps.insert(after, BlockPosition.SceneEnd(n), BlockKind.NARRATION, Fx.ctx().let { c ->
            io.github.lenticualrstone.civilight.domain.script.EditContext(now = c.now, zone = c.zone, device = c.device, newKey = { "01JZ000000TESTBLOCK0000001" })
        }, runs = listOf(TextRun.Plain("처음.")))
        assertEquals(1, inserted.number)
    }

    @Test
    fun pickedPlaceAndDirectTransition() {
        val sheet = NewUnitSheet(p, parentKey = K.EPISODE_1_7, afterKey = s8)
        val gate = doc.unit(s9).place!!
        val picked = NewSceneForm(place = PlaceChoice.Picked(gate), transition = TransitionMode.DIRECT, directText = "결사의 문 앞")
        val setup = NewSceneForms.setup(doc, sheet)!!
        assertEquals("결사의 문", NewSceneForms.lines(doc, setup, picked).place)
        val (after, n) = NewSceneForms.create(doc, sheet, picked, Fx.ctx())!!
        assertTrue(after.unit(n).place!!.sameAs(gate))
        assertEquals(TransitionMode.DIRECT, after.unit(n).transitionModeOrNone)
        assertEquals("결사의 문 앞", Transitions(after).directText(n))
        assertNotNull(after.unit(n).transitionSnapshot)
        // `직접` without text is created as `없음` (ED).
        val (blank, m) = NewSceneForms.create(doc, sheet, picked.copy(directText = "  "), Fx.ctx())!!
        assertEquals(TransitionMode.NONE, blank.unit(m).transitionModeOrNone)
    }

    @Test
    fun autoNeedsAPlace() {
        val sheet = NewUnitSheet(p, parentKey = K.EPISODE_1_7, afterKey = s8)
        val setup = NewSceneForms.setup(doc, sheet)!!
        val auto = NewSceneForm(transition = TransitionMode.AUTO)
        assertEquals(TransitionMode.AUTO, NewSceneForms.lines(doc, setup, auto).transition)
        val noPlace = auto.copy(place = PlaceChoice.Cleared)
        assertFalse(NewSceneForms.lines(doc, setup, noPlace).autoEnabled)
        assertEquals(TransitionMode.NONE, NewSceneForms.lines(doc, setup, noPlace).transition)
        val region = PlaceRef(PlaceRefKind.REGION, "unknown-region")
        assertFalse(NewSceneForms.autoEnabled(doc, setup, auto.copy(place = PlaceChoice.Picked(region))))
    }

    @Test
    fun staleOrInvalidSheetsCreateNothing() {
        assertNull(NewSceneForms.setup(doc, NewUnitSheet(p, parentKey = K.EPISODE_1_7, afterKey = K.SCENE_1_5_1)))
        assertNull(NewSceneForms.setup(doc, NewUnitSheet(p, parentKey = K.CHAPTER_1)))
        assertNull(NewSceneForms.setup(doc, NewUnitSheet(p)))
        val sheet = NewUnitSheet(p, parentKey = K.EPISODE_1_7, afterKey = s9)
        val deleted = io.github.lenticualrstone.civilight.domain.actions.SceneActions.delete(doc, s9, Fx.ctx()).doc
        assertNull(NewSceneForms.create(deleted, sheet, NewSceneForm(), Fx.ctx()))
        val oneLevel = Fx.mini(listOf("씬"), node("S1"))
        assertNull(NewSceneForms.setup(oneLevel, NewUnitSheet(oneLevel.project.key, parentKey = null)))
    }
}
