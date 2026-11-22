package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.actions.SceneActions
import io.github.lenticualrstone.civilight.domain.story.checks.Acks
import io.github.lenticualrstone.civilight.domain.story.time.CascadeMode
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.feature.story.Fx.unit
import io.github.lenticualrstone.civilight.feature.story.where.TimeStep
import io.github.lenticualrstone.civilight.feature.story.where.WhenAction
import io.github.lenticualrstone.civilight.feature.story.where.WhenDay
import io.github.lenticualrstone.civilight.feature.story.where.WhenEdit
import io.github.lenticualrstone.civilight.feature.story.where.WhereInputs
import io.github.lenticualrstone.civilight.feature.story.where.WhereModel
import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.DayBoundary
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.TimePrecision
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** `씬 · 장소와 때` (STO-20 … STO-23): the 때 reducer, the cascade step and the pure edits. */
class WhereModelTest {
    private val doc = Fx.doc()
    private val s9 = K.SCENE_1_7_3

    private fun relative(d: ProjectDoc, key: String) = d.unit(key).time as SceneTime.Relative

    @Test
    fun controlsOfScene9_SceneWhereBoard() {
        val c = WhenEdit.controls(doc, s9)
        assertTrue(c.editable)
        assertFalse(c.onlyDirect)
        assertEquals(WhenDay.PLUS_ONE, c.day)
        assertEquals(Band.DAWN, c.band)
        assertEquals(ClockTime.of(5, 40), c.clock)
        assertEquals(TimePrecision.EXACT, c.precision)
        val ui = WhereModel.build(doc, s9, readOnly = false)!!
        assertEquals("씬 3 · 장소와 때", ui.sheet.title)
        assertEquals("결사의 문", ui.sheet.place)
    }

    @Test
    fun dayChipKeepsTheBaseAndTimeOfDay_STO21() {
        val next = WhenEdit.next(doc, s9, WhenAction.Day(WhenDay.PLUS_TWO)) as SceneTime.Relative
        val before = relative(doc, s9)
        assertEquals(before.baseKey, next.baseKey)
        assertEquals(2 to 2, next.dayMin to next.dayMax)
        assertEquals(ClockTime.of(5, 40), next.clock)
        assertNull("same chip again: no change", WhenEdit.next(doc, s9, WhenAction.Day(WhenDay.PLUS_ONE))?.takeIf { it != before })
    }

    @Test
    fun approxTurnsTheChipIntoItsRange_Q17() {
        val approx = WhenEdit.next(doc, s9, WhenAction.Precision(TimePrecision.APPROX)) as SceneTime.Relative
        assertEquals(0 to 2, approx.dayMin to approx.dayMax)
        assertEquals(TimePrecision.APPROX, approx.precision.known)
        val stored = doc.withUnitTime(s9, approx)
        assertEquals(WhenDay.PLUS_ONE, WhenEdit.controls(stored, s9).day)
        val exact = WhenEdit.next(stored, s9, WhenAction.Precision(TimePrecision.EXACT)) as SceneTime.Relative
        assertEquals(1 to 1, exact.dayMin to exact.dayMax)
    }

    @Test
    fun aClockPicksItsBandAndABandKeepsOnlyAClockInsideIt_STO19() {
        val clock = WhenEdit.next(doc, s9, WhenAction.SetClock(ClockTime.of(7, 0))) as SceneTime.Relative
        assertEquals(Band.MORNING, clock.band?.known)
        assertEquals(ClockTime.of(7, 0), clock.clock)
        val evening = WhenEdit.next(doc, s9, WhenAction.PickBand(Band.EVENING)) as SceneTime.Relative
        assertEquals(Band.EVENING, evening.band?.known)
        assertNull("05:40 is not 저녁", evening.clock)
        assertNull("tapping the selected band changes nothing (no hidden clearing)", WhenEdit.next(doc, s9, WhenAction.PickBand(Band.DAWN)))
    }

    @Test
    fun underMidnightA0130ClockKeepsTheLabelDay_STO19_STO21() {
        val midnight = doc.copy(project = doc.project.copy(sceneTime = doc.project.sceneTime.copy(dayBoundary = DayBoundary.MIDNIGHT.open())))
        val labelBefore = SceneTimes(midnight).storyDay(s9)
        val next = WhenEdit.next(midnight, s9, WhenAction.SetClock(ClockTime.of(1, 30))) as SceneTime.Relative
        assertEquals("stored in the 04:00-day form: one day shorter", 0 to 0, next.dayMin to next.dayMax)
        assertEquals(ClockTime.of(1, 30), next.clock)
        val stored = midnight.withUnitTime(s9, next)
        assertEquals(labelBefore, SceneTimes(stored).storyDay(s9))
        assertEquals(WhenDay.PLUS_ONE, WhenEdit.controls(stored, s9).day)
        // `대략` under `자정`: the stored range [-1, 0] reads as the `같은 날` chip once the shift is removed.
        val approx = WhenEdit.next(stored, s9, WhenAction.Day(WhenDay.SAME_DAY)) as SceneTime.Relative
        val approxStored = stored.withUnitTime(s9, approx.copy(dayMin = -1, dayMax = 0, precision = TimePrecision.APPROX.open()))
        assertEquals(WhenDay.SAME_DAY, WhenEdit.controls(approxStored, s9).day)
        // Under 04:00 the same entry is the night of the stored day (밤 belongs to the day it started).
        val dawn = WhenEdit.next(doc, s9, WhenAction.SetClock(ClockTime.of(1, 30))) as SceneTime.Relative
        assertEquals(1 to 1, dawn.dayMin to dawn.dayMax)
        assertEquals(Band.NIGHT, dawn.band?.known)
    }

    @Test
    fun directKeepsTheLabelDayAndIsTheOnlyChoiceWithoutAnEarlierTimedScene_BASE03() {
        val direct = WhenEdit.next(doc, s9, WhenAction.Day(WhenDay.DIRECT)) as SceneTime.Direct
        assertEquals(SceneTimes(doc).storyDay(s9)!!.min, direct.storyDay)
        assertEquals(ClockTime.of(5, 40), direct.clock)
        val moved = WhenEdit.next(doc.withUnitTime(s9, direct), s9, WhenAction.DirectDay(20)) as SceneTime.Direct
        assertEquals(20, moved.storyDay)
        val first = WhenEdit.controls(doc, K.SCENE_1_1_1)
        assertTrue(first.onlyDirect)
        assertEquals(WhenDay.DIRECT, first.day)
        assertNull(WhenEdit.next(doc, K.SCENE_1_1_1, WhenAction.Day(WhenDay.PLUS_ONE)))
    }

    @Test
    fun aDayChangeWithDependentsShowsTheCascadeAndATimeOfDayChangeDoesNot_STO23() {
        val later = WhenEdit.next(doc, s9, WhenAction.Day(WhenDay.PLUS_TWO))
        val step = WhereModel.step(doc, s9, later)
        assertTrue(step is TimeStep.Preview)
        val preview = (step as TimeStep.Preview).preview
        assertTrue(K.SCENE_1_7_4 in preview.dependents)
        val clockOnly = WhereModel.step(doc, s9, WhenEdit.next(doc, s9, WhenAction.SetClock(ClockTime.of(5, 50))))
        assertTrue(clockOnly is TimeStep.Direct)
        assertEquals(TimeStep.NoChange, WhereModel.step(doc, s9, relative(doc, s9)))
    }

    @Test
    fun thisOnlyKeepsTheDependentsWhereTheyAre_CASC() {
        val later = WhenEdit.next(doc, s9, WhenAction.Day(WhenDay.PLUS_TWO))!!
        val before = SceneTimes(doc).timed(K.SCENE_1_7_4)!!.absolute
        val thisOnly = WhereModel.setTime(doc, s9, later, CascadeMode.THIS_ONLY, Fx.ctx())
        assertEquals(before, SceneTimes(thisOnly).timed(K.SCENE_1_7_4)!!.absolute)
        val withFollowing = WhereModel.setTime(doc, s9, later, CascadeMode.WITH_FOLLOWING, Fx.ctx())
        assertEquals(before + 1440, SceneTimes(withFollowing).timed(K.SCENE_1_7_4)!!.absolute)
    }

    @Test
    fun flashbackOnLeavesTheChainAndDependentsKeepTheirTime() {
        val before = SceneTimes(doc).timed(K.SCENE_1_7_4)!!.absolute
        val on = WhereModel.setTime(doc, s9, WhereModel.flashbackTime(true), CascadeMode.WITH_FOLLOWING, Fx.ctx())
        assertTrue(on.unit(s9).time is SceneTime.Flashback)
        assertEquals(before, SceneTimes(on).timed(K.SCENE_1_7_4)!!.absolute)
        assertFalse(WhenEdit.controls(on, s9).editable)
        assertTrue(WhereModel.build(on, s9, readOnly = false)!!.sheet.flashback)
    }

    @Test
    fun placeAndTransitionEdits() {
        val place = doc.unit(K.SCENE_1_7_2).place!!
        val moved = WhereModel.setPlace(doc, s9, place, Fx.ctx())
        assertEquals(place, moved.unit(s9).place)
        val none = WhereModel.setTransitionMode(doc, s9, TransitionMode.DIRECT, Fx.ctx())
        val written = WhereModel.writeDirect(none, s9, "결사의 문 앞", Fx.ctx())
        assertEquals("결사의 문 앞", WhereModel.build(written, s9, false)!!.sheet.transition.directText)
        val noPlace = Fx.mini(listOf("씬"), Fx.node("S1"))
        assertEquals("자동 needs a place", noPlace, WhereModel.setTransitionMode(noPlace, "S1", TransitionMode.AUTO, Fx.ctx()))
    }

    @Test
    fun travelBlockAcceptsWithMatch_STO30() {
        val open = Acks.revert(doc, K.ACK_TRAVEL_HART)
        val block = WhereModel.build(open, s9, false)!!.sheet.travel.single()
        assertEquals("맞음", block.chips.last())
        val accepted = WhereModel.acknowledgeTravel(open, block.warning, Fx.ctx())
        assertTrue(WhereModel.build(accepted, s9, false)!!.sheet.travel.isEmpty())
        val delayed = WhereModel.delayedTime(open, block.warning.timeChip.sceneKey, block.warning.timeChip.days)
        assertNotNull(delayed)
    }

    @Test
    fun staleSheetsChangeNothing() {
        val deleted = SceneActions.delete(doc, s9, Fx.ctx()).doc
        assertNull(WhereModel.build(deleted, s9, false))
        assertEquals(TimeStep.NoChange, WhereModel.step(deleted, s9, SceneTime.None))
        assertEquals(deleted, WhereModel.setTime(deleted, s9, SceneTime.None, CascadeMode.WITH_FOLLOWING, Fx.ctx()))
        assertEquals(deleted, WhereModel.setPlace(deleted, s9, PlaceRef(PlaceRefKind.PLACE, "x"), Fx.ctx()))
        assertNull(WhereModel.build(doc, K.EPISODE_1_7, false))
        assertNull(WhenEdit.next(deleted, s9, WhenAction.Day(WhenDay.SAME_DAY)))
    }

    @Test
    fun inputsAndPendingTimeRoundTrip() {
        assertEquals(WhereInputs.ClockDraft.Value(ClockTime.of(5, 40)), WhereInputs.clock("05:40"))
        assertEquals(WhereInputs.ClockDraft.Value(ClockTime.of(5, 40)), WhereInputs.clock("0540"))
        assertEquals("one-digit hours are an intermediate draft", WhereInputs.ClockDraft.Incomplete, WhereInputs.clock("7:05"))
        assertEquals("a half-deleted 20:00 is not a clock", WhereInputs.ClockDraft.Incomplete, WhereInputs.clock("0:00"))
        assertEquals(WhereInputs.ClockDraft.Incomplete, WhereInputs.clock("05:4"))
        assertEquals(WhereInputs.ClockDraft.Incomplete, WhereInputs.clock("2460"))
        assertEquals(WhereInputs.ClockDraft.Clear, WhereInputs.clock(" "))
        assertEquals(12, WhereInputs.storyDay("12"))
        assertNull(WhereInputs.storyDay("0"))
        val t = relative(doc, s9)
        assertEquals(t, WhereModel.decode(WhereModel.encode(t)))
        assertNull(WhereModel.decode("not json"))
    }

    private fun ProjectDoc.withUnitTime(key: String, time: SceneTime): ProjectDoc =
        copy(story = story.copy(units = story.units.map { if (it.key == key) it.copy(time = time) else it }))
}
