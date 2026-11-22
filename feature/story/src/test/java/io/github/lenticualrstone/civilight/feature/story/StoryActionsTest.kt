package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.feature.story.Fx.unit
import io.github.lenticualrstone.civilight.feature.story.Fx.withBlock
import io.github.lenticualrstone.civilight.feature.story.common.StoryActions
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.SceneNotation
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** The story screens' edits (STO-05, STO-06, STO-10, STO-17) through the one write path. */
class StoryActionsTest {
    private val doc = Fx.doc()

    private fun ProjectDoc.full(sceneKey: String): String? = TimeLabels(SceneTimes(this)).full(sceneKey)

    @Test
    fun del01_deletingS8RebasesS9WithItsAbsoluteKept() {
        val before = doc.full(K.SCENE_1_7_3)
        val result = StoryActions.deleteScene(doc, K.SCENE_1_7_2, Fx.ctx())!!
        val d = result.doc
        assertFalse(d.liveness.isUnitLive(K.SCENE_1_7_2))
        val s9 = d.unit(K.SCENE_1_7_3).time as SceneTime.Relative
        assertEquals(K.SCENE_1_7_1, s9.baseKey)
        assertEquals(K.SCENE_1_7_2, s9.rebasedFromKey)
        assertEquals(before, d.full(K.SCENE_1_7_3))
        assertTrue(result.toast.message, result.toast.message.startsWith("씬 2 지움"))
    }

    @Test
    fun del02_undoBringsS8BackAsS9sBase() {
        val result = StoryActions.deleteScene(doc, K.SCENE_1_7_2, Fx.ctx())!!
        val back = StoryActions.undoDelete(result.doc, result)
        assertTrue(back.liveness.isUnitLive(K.SCENE_1_7_2))
        assertEquals(K.SCENE_1_7_2, (back.unit(K.SCENE_1_7_3).time as SceneTime.Relative).baseKey)
        assertEquals(doc.full(K.SCENE_1_7_2), back.full(K.SCENE_1_7_2))
        assertEquals(doc.full(K.SCENE_1_7_3), back.full(K.SCENE_1_7_3))
    }

    @Test
    fun lens12_deletingThe19SceneMovesTheWritingLineBack() {
        val lens09 = doc.withBlock(K.SCENE_1_9_1, "01JZ000000TEST0000000000A1", "관문 앞은 조용했다.")
        assertEquals(K.SCENE_1_9_1, lens09.manuscript.writingLineSceneKey)
        val d = StoryActions.deleteScene(lens09, K.SCENE_1_9_1, Fx.ctx())!!.doc
        assertEquals(K.SCENE_1_7_3, d.manuscript.writingLineSceneKey)
        assertEquals("no record is deleted", lens09.changes.records, d.changes.records)
    }

    @Test
    fun deleteIsOnlyForLiveScenes() {
        assertNull(StoryActions.deleteScene(doc, K.EPISODE_1_7, Fx.ctx()))
        assertNull(StoryActions.deleteScene(doc, "unknown", Fx.ctx()))
        val gone = StoryActions.deleteScene(doc, K.SCENE_1_7_4, Fx.ctx())!!.doc
        assertNull("a second tap on a stale menu does nothing", StoryActions.deleteScene(gone, K.SCENE_1_7_4, Fx.ctx()))
        assertEquals("씬 4 지움", StoryActions.deleteLabel(doc, K.SCENE_1_7_4))
    }

    @Test
    fun newEpisodeGoesRightAfterTheOneWhoseMenuWasUsed() {
        val made = StoryActions.newUnitAfter(doc, K.EPISODE_1_7, Fx.ctx())!!
        val codes = UnitCodes(made.doc)
        assertEquals("1-8", codes.episodeCode(made.unitKey))
        assertEquals("1-9", codes.episodeCode(K.EPISODE_1_8))
        assertEquals(made.doc.unit(K.EPISODE_1_7).parentKey, made.doc.unit(made.unitKey).parentKey)
        assertNull(StoryActions.newUnitAfter(doc, "unknown", Fx.ctx()))
    }

    @Test
    fun newChapterGoesLastAndNotInAOneLevelStructure() {
        val made = StoryActions.newTopUnit(doc, Fx.ctx())!!
        val tops = made.doc.index.children(null).filter { made.doc.liveness.isUnitLive(it.key) }
        assertEquals(made.unitKey, tops.last().key)
        assertNull(StoryActions.newTopUnit(Fx.mini(listOf("씬"), Fx.node("S1")), Fx.ctx()))
    }

    @Test
    fun doneFlagIsTheAuthorsAndRepeatsAreNoOps_STO06() {
        val d = StoryActions.setDone(doc, K.EPISODE_1_7, true, Fx.ctx())
        assertTrue(d.unit(K.EPISODE_1_7).doneMarked)
        assertSame(d, StoryActions.setDone(d, K.EPISODE_1_7, true, Fx.ctx()))
        assertFalse(StoryActions.setDone(d, K.EPISODE_1_7, false, Fx.ctx()).unit(K.EPISODE_1_7).doneMarked)
    }

    @Test
    fun notationChangesLabelsOnly_STO17() {
        val d = StoryActions.setNotation(doc, SceneNotation.STORY_START, Fx.ctx())
        assertEquals(SceneNotation.STORY_START, d.project.sceneTime.notation.known)
        assertEquals(doc.story.units.map { it.time }, d.story.units.map { it.time })
        assertNotNull(d.full(K.SCENE_1_7_3))
        assertSame(d, StoryActions.setNotation(d, SceneNotation.STORY_START, Fx.ctx()))
    }
}
