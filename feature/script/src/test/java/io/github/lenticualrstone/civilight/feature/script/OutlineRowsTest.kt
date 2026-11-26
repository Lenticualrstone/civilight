package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.feature.script.scenes.SceneCopy
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRowKeys
import io.github.lenticualrstone.civilight.feature.script.synergy.ScriptOutline
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** `씬 목차`: real scene codes, rule 19 hiding (scene and ancestors), trash and blank scenes. */
class OutlineRowsTest {
    private val doc = Fx.example

    private fun ProjectDoc.updateUnit(key: Key, f: (io.github.lenticualrstone.civilight.model.story.StoryUnit) -> io.github.lenticualrstone.civilight.model.story.StoryUnit) =
        copy(story = story.copy(units = story.units.map { if (it.key == key) f(it) else it }))

    private fun keys(d: ProjectDoc, unlocked: Boolean = false) = ScriptOutline.rows(d, K.EPISODE_1_7, unlocked).map { it.sceneKey }
    private fun labels(d: ProjectDoc, unlocked: Boolean = false) = ScriptOutline.rows(d, K.EPISODE_1_7, unlocked).map { it.label }

    @Test
    fun `rows list the episode's scenes with the scene head's ordinal`() {
        val rows = ScriptOutline.rows(doc, K.EPISODE_1_7, hiddenUnlocked = false)
        assertEquals(listOf(K.SCENE_1_7_1, K.SCENE_1_7_2, K.SCENE_1_7_3, K.SCENE_1_7_4), rows.map { it.sceneKey })
        assertEquals(listOf("씬 1", "씬 2", "씬 3", "씬 4"), rows.map { it.label })
        rows.forEach { assertEquals(Labels.sceneOrdinal(doc, it.sceneKey), it.ordinal) }
        assertEquals("상층 관문", rows[1].title)
        rows.forEach { assertEquals(ScriptRowKeys.head(it.sceneKey), it.jumpRowKey) }
    }

    @Test
    fun `a locked hidden scene leaves a gap instead of renumbering, and comes back unlocked`() {
        val hidden = doc.updateUnit(K.SCENE_1_7_2) { it.copy(hidden = true) }
        assertEquals(listOf("씬 1", "씬 3", "씬 4"), labels(hidden))
        assertEquals(listOf(K.SCENE_1_7_1, K.SCENE_1_7_2, K.SCENE_1_7_3, K.SCENE_1_7_4), keys(hidden, unlocked = true))
        assertEquals(listOf("씬 1", "씬 2", "씬 3", "씬 4"), labels(hidden, unlocked = true))
    }

    @Test
    fun `a hidden episode or chapter hides every scene under it while locked`() {
        val episodeHidden = doc.updateUnit(K.EPISODE_1_7) { it.copy(hidden = true) }
        assertTrue(keys(episodeHidden).isEmpty())
        assertEquals(4, keys(episodeHidden, unlocked = true).size)
        val chapterHidden = doc.updateUnit(K.CHAPTER_1) { it.copy(hidden = true) }
        assertTrue(keys(chapterHidden).isEmpty())
        assertEquals(4, keys(chapterHidden, unlocked = true).size)
    }

    @Test
    fun `a trashed scene is neither listed nor counted`() {
        val trashed = doc.updateUnit(K.SCENE_1_7_2) { it.copy(deletedAt = ExampleClock.NOW, trashEntryKey = "entry") }
        assertEquals(listOf(K.SCENE_1_7_1, K.SCENE_1_7_3, K.SCENE_1_7_4), keys(trashed))
        assertEquals(listOf("씬 1", "씬 2", "씬 3"), labels(trashed))
    }

    @Test
    fun `a blank untitled scene still gets a row that jumps to its head`() {
        val last = doc.index.unit(K.SCENE_1_7_4)!!
        val blankKey = "01JZ000000TESTBLANKSCENE01"
        val withBlank = doc.copy(
            story = doc.story.copy(
                units = doc.story.units + last.copy(key = blankKey, title = null, order = OrderKeys.after(last.order), transitionMode = null),
            ),
        )
        val row = ScriptOutline.rows(withBlank, K.EPISODE_1_7, hiddenUnlocked = false).last()
        assertEquals(blankKey, row.sceneKey)
        assertEquals("씬 5", row.label)
        assertEquals(SceneCopy.UNTITLED, row.title)
        assertEquals(0, row.blocks)
        assertEquals(ScriptRowKeys.head(blankKey), row.jumpRowKey)
    }

    @Test
    fun `outline copy is terse`() {
        assertEquals("씬 없음", ScriptCopy.NO_SCENES)
        assertEquals("미정", SceneCopy.UNTITLED)
    }
}
