package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import org.junit.Assert.assertEquals
import org.junit.Test
import io.github.lenticualrstone.civilight.domain.story.scene.EditContext as StoryEditContext
import io.github.lenticualrstone.civilight.domain.story.scene.MergeOptions as StoryMergeOptions
import io.github.lenticualrstone.civilight.domain.story.scene.MergePlan as StoryMergePlan
import io.github.lenticualrstone.civilight.domain.story.scene.MergeSide as StoryMergeSide
import io.github.lenticualrstone.civilight.domain.story.scene.SceneMerge as StorySceneMerge
import io.github.lenticualrstone.civilight.domain.story.scene.SceneSplit as StorySceneSplit
import io.github.lenticualrstone.civilight.domain.story.scene.SceneTrash as StorySceneTrash

/**
 * `domain:story` also implements split, merge and scene delete (STO-09/10). The script's versions must
 * leave the same 때 and the same block placement; this pins the two together until one is dropped.
 */
class StoryCrossCheckTest {
    private val ctx = testCtx()
    private var n = 0
    private val storyCtx = StoryEditContext(newKey = { "01JZ000000STRY" + (++n).toString().padStart(12, '0') }, now = ExampleClock.NOW, device = "test")

    @Test
    fun `split below #044 agrees with the story engine`() {
        val doc = F1.build()
        val mine = SceneSplit.apply(doc, F1.b(44), ctx)!!
        val theirs = StorySceneSplit.plan(doc, F1.S3, F1.b(44), storyCtx)
        val a = StoryTimes.source(mine.doc)
        val b = StoryTimes.source(theirs.result)
        for (scene in listOf(F1.S1, F1.S2, F1.S3, F1.S4, F1.S5, F1.S18_1)) assertEquals("scene $scene", b.timeOf(scene), a.timeOf(scene))
        assertEquals(b.timeOf(theirs.newSceneKey), a.timeOf(mine.newSceneKey))
        assertEquals(theirs.result.liveNumbers(theirs.newSceneKey), mine.doc.liveNumbers(mine.newSceneKey))
        assertEquals(theirs.result.liveNumbers(F1.S3), mine.doc.liveNumbers(F1.S3))
    }

    @Test
    fun `merge of 씬 4 into 씬 3 agrees with the story engine for both time choices`() {
        val doc = F1.build()
        for (fromB in listOf(false, true)) {
            val mine = SceneMerge.apply(doc, F1.S4, SceneMerge.Choices(timeFromB = fromB), StoryTimes.source(doc), ctx)!!.doc
            val plan = StorySceneMerge.plan(
                doc, F1.S4, storyCtx,
                StoryMergeOptions(time = if (fromB) StoryMergeSide.SECOND else StoryMergeSide.FIRST),
            ) as StoryMergePlan.Ready
            val theirs = plan.preview.result
            val a = StoryTimes.source(mine)
            val b = StoryTimes.source(theirs)
            for (scene in listOf(F1.S1, F1.S2, F1.S3, F1.S5, F1.S18_1)) assertEquals("fromB=$fromB scene $scene", b.timeOf(scene), a.timeOf(scene))
            assertEquals("fromB=$fromB", theirs.liveNumbers(F1.S3), mine.liveNumbers(F1.S3))
        }
    }

    @Test
    fun `scene delete agrees with the story engine on the other scenes' times`() {
        val doc = F1.build()
        val mine = SceneLifecycle.delete(doc, F1.S4, ctx)!!.doc
        val theirs = StorySceneTrash.delete(doc, F1.S4, storyCtx)
        val a = StoryTimes.source(mine)
        val b = StoryTimes.source(theirs)
        for (scene in listOf(F1.S1, F1.S2, F1.S3, F1.S5, F1.S18_1)) assertEquals("scene $scene", b.timeOf(scene), a.timeOf(scene))
    }
}
