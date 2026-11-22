package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.actions.SceneActions
import io.github.lenticualrstone.civilight.domain.story.structure.UnitSettingsEdit
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.feature.story.Fx.node
import io.github.lenticualrstone.civilight.feature.story.Fx.unit
import io.github.lenticualrstone.civilight.feature.story.move.MoveModel
import io.github.lenticualrstone.civilight.feature.story.settings.DayBandModel
import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.DayBoundary
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** STO-08 `옮기기` and STO-19 `하루 · 시간대`. */
class MoveAndDayBandTest {
    private val doc = Fx.doc()
    private val s9 = K.SCENE_1_7_3

    @Test
    fun slotsAreFirstThenAfterEachSibling() {
        val other = MoveModel.slots(doc, s9, K.EPISODE_1_8)
        assertEquals(listOf("맨 앞", "씬 1 뒤", "씬 2 뒤"), other.map { it.label })
        assertTrue(other.none { it.current })
        val own = MoveModel.slots(doc, s9, K.EPISODE_1_7)
        assertEquals(listOf("맨 앞", "씬 1 뒤", "씬 2 뒤", "씬 4 뒤"), own.map { it.label })
        assertEquals("씬 2 뒤", own.single { it.current }.label)
    }

    @Test
    fun move01_previewThenMoveKeepsTheScenesTime() {
        val preview = MoveModel.preview(doc, s9, K.EPISODE_1_8, null, Fx.ctx())!!
        assertEquals("씬 옮기기", preview.title)
        assertEquals("미리 보기 · 옮기기 전에는 그대로", preview.sub)
        assertEquals("1-8 | 맨 앞 · 씬 1", preview.toLine)
        assertTrue(preview.timeRow.startsWith("그대로"))
        val before = SceneTimes(doc).timed(s9)!!.absolute
        val moved = MoveModel.moveScene(doc, s9, K.EPISODE_1_8, null, Fx.ctx())
        assertEquals(K.EPISODE_1_8, moved.unit(s9).parentKey)
        assertEquals(before, SceneTimes(moved).timed(s9)!!.absolute)
    }

    @Test
    fun episodesMoveWithoutPreviewAndCodesRenumber() {
        val moved = MoveModel.moveUnit(doc, K.EPISODE_1_8, K.CHAPTER_1, null, Fx.ctx())
        assertEquals("1-1", UnitCodes(moved).episodeCode(moved.unit(K.EPISODE_1_8)))
        assertEquals(doc.story.units.map { it.time }.toSet(), moved.story.units.map { it.time }.toSet())
        assertEquals("a scene is not moved this way", doc, MoveModel.moveUnit(doc, s9, K.EPISODE_1_8, null, Fx.ctx()))
    }

    @Test
    fun staleOrImpossibleMovesChangeNothing() {
        val deleted = SceneActions.delete(doc, s9, Fx.ctx()).doc
        assertNull(MoveModel.preview(deleted, s9, K.EPISODE_1_8, null, Fx.ctx()))
        assertEquals(deleted, MoveModel.moveScene(deleted, s9, K.EPISODE_1_8, null, Fx.ctx()))
        assertNull("after a scene of another episode", MoveModel.preview(doc, s9, K.EPISODE_1_8, K.SCENE_1_7_1, Fx.ctx()))
        val oneLevel = Fx.mini(listOf("씬"), node("S1"), node("S2"))
        assertFalse(MoveModel.movable(oneLevel, "S1"))
        assertTrue(MoveModel.movable(doc, s9))
    }

    @Test
    fun band01_settingsRowAndEditableBoundaries_STO19() {
        val ui = DayBandModel.build(doc, readOnly = false)
        assertEquals("하루는 새벽 4시부터 · 저녁 17–20", ui.summary)
        assertEquals(listOf("새벽", "아침", "낮", "저녁", "밤"), ui.bands.map { it.word })
        assertEquals(listOf(false, true, true, true, true), ui.bands.map { it.editable })
        assertEquals("17–20", ui.bands.single { it.band == Band.EVENING }.hours)
        assertTrue(DayBandModel.build(doc, readOnly = true).bands.none { it.editable })
        val midnight = UnitSettingsEdit.setDayBoundary(doc, DayBoundary.MIDNIGHT, Fx.ctx().edit)
        assertEquals("자정 · 저녁 17–20", DayBandModel.summary(midnight))
        assertTrue(DayBandModel.accepts(doc, Band.EVENING, ClockTime.of(18, 0)))
        assertFalse(DayBandModel.accepts(doc, Band.EVENING, ClockTime.of(21, 0)))
    }
}
