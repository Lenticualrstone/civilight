package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.Fx.unit
import io.github.lenticualrstone.civilight.domain.story.Fx.withUnit
import io.github.lenticualrstone.civilight.domain.story.structure.NewUnit
import io.github.lenticualrstone.civilight.domain.story.structure.StoryOverview
import io.github.lenticualrstone.civilight.domain.story.structure.UnitSettingsEdit
import io.github.lenticualrstone.civilight.domain.story.structure.resumeTarget
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.project.SceneNotation
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Additions for the story screens: new episode / chapter, `이어 쓰기` block, `완료`, `때 표기` (STO-01, 05, 06, 17). */
class StructureAdditionsTest {
    private val doc = Fx.doc()
    private val ctx = Fx.ctx()

    @Test
    fun newEpisodeAfterAnotherKeepsEveryOtherCode() {
        val result = NewUnit.create(doc, K.CHAPTER_1, 1, K.EPISODE_1_7, ctx, title = "새 이야기")!!
        val codes = UnitCodes(result.doc)
        val episode = result.doc.unit(result.unitKey)
        assertEquals("1-8", codes.episodeCode(episode))
        assertEquals("새 이야기", episode.title)
        assertEquals(1, episode.nextBlockNumber)
        assertEquals(listOf(result.unitKey), result.createdKeys)
        assertEquals("1-9", codes.episodeCode(result.doc.unit(K.EPISODE_1_8)))
        assertEquals("1-7", codes.episodeCode(result.doc.unit(K.EPISODE_1_7)))
        Fx.assertOtherAbsolutesUnchanged(doc, result.doc, emptySet())
    }

    @Test
    fun newChapterStartsWithItsFirstEpisode() {
        val result = NewUnit.create(doc, null, 0, null, ctx)!!
        assertEquals(2, result.createdKeys.size)
        val chapter = result.doc.unit(result.unitKey)
        val episode = result.doc.unit(result.createdKeys[1])
        assertEquals(0, chapter.level)
        assertEquals(chapter.key, episode.parentKey)
        val codes = UnitCodes(result.doc)
        assertEquals("Ⅲ 3장", codes.chapterHeader(chapter))
        assertEquals("3-1", codes.episodeCode(episode))
        assertNull(chapter.nextBlockNumber)
        assertEquals(1, episode.nextBlockNumber)
    }

    @Test
    fun invalidPositionsCreateNothing() {
        assertNull(NewUnit.create(doc, K.CHAPTER_1, 2, null, ctx)) // scene level
        assertNull(NewUnit.create(doc, K.CHAPTER_1, 0, null, ctx)) // wrong level under a chapter
        assertNull(NewUnit.create(doc, K.CHAPTER_1, 1, K.EPISODE_2_3, ctx)) // not a child
        assertNull(NewUnit.create(doc, "unknown", 1, null, ctx))
    }

    @Test
    fun resumeTargetIsTheBlockTheLabelNames() {
        val target = StoryOverview(doc).resumeTarget()!!
        assertEquals(K.BLOCK_044, target.blockKey)
        assertEquals(K.SCENE_1_7_3, target.sceneKey)
        assertEquals(StoryOverview(doc).resume(), target.where to target.quote)
    }

    @Test
    fun resumeTargetSkipsALockedHiddenScene() {
        val hidden = doc.withUnit(K.SCENE_1_7_3) { it.copy(hidden = true) }
        val locked = StoryOverview(hidden, locked = true).resumeTarget()!!
        assertTrue(locked.sceneKey != K.SCENE_1_7_3)
        assertEquals(StoryOverview(hidden, locked = true).resume(), locked.where to locked.quote)
        assertEquals(K.BLOCK_044, StoryOverview(hidden, locked = false).resumeTarget()!!.blockKey)
    }

    @Test
    fun doneFlagChangesTheStatusOnly() {
        val done = UnitSettingsEdit.setDone(doc, K.EPISODE_1_7, true, ctx)
        assertTrue(done.unit(K.EPISODE_1_7).doneMarked)
        assertEquals("완료", StoryOverview(done).episodeStatus(done.unit(K.EPISODE_1_7)))
        assertSame(done, UnitSettingsEdit.setDone(done, K.EPISODE_1_7, true, ctx))
        val undone = UnitSettingsEdit.setDone(done, K.EPISODE_1_7, false, ctx)
        assertFalse(undone.unit(K.EPISODE_1_7).doneMarked)
        assertEquals("쓰는 중 · 씬 3", StoryOverview(undone).episodeStatus(undone.unit(K.EPISODE_1_7)))
    }

    @Test
    fun notationChangesLabelsNotStoredTime() {
        val story = UnitSettingsEdit.setNotation(doc, SceneNotation.STORY_START, ctx)
        assertEquals(SceneNotation.STORY_START, story.project.sceneTime.notation.known)
        assertEquals(doc.story.units, story.story.units)
        assertEquals("1-7 · 2일째 · 새벽", TimeLabels(SceneTimes(doc)).full(K.SCENE_1_7_3))
        assertNotNull(TimeLabels(SceneTimes(story)).full(K.SCENE_1_7_3))
        assertEquals("1-7 · 약 13일째 · 새벽", TimeLabels(SceneTimes(story)).full(K.SCENE_1_7_3))
        assertSame(story, UnitSettingsEdit.setNotation(story, SceneNotation.STORY_START, ctx))
    }
}
