package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.script.Numbering
import io.github.lenticualrstone.civilight.feature.story.menu.RenumberModel
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** `번호 다시 매기기 · 1-7` from the episode ⋯ (STO-07 → SCR-22). */
class RenumberModelTest {
    /** The example's 1-7 with its first two live blocks' numbers swapped, so two numbers change. */
    private fun shuffled(): ProjectDoc {
        val doc = Fx.doc()
        val blocks = Numbering.liveBlocksOfUnit(doc, K.EPISODE_1_7)
        val (a, b) = blocks[0] to blocks[1]
        return doc.copy(
            episodes = doc.episodes.mapValues { (_, file) ->
                file.copy(blocks = file.blocks.map { when (it.key) { a.key -> it.copy(number = b.number); b.key -> it.copy(number = a.number); else -> it } })
            },
        )
    }

    @Test
    fun previewOfAnEpisode() {
        val doc = shuffled()
        val preview = RenumberModel.preview(doc, K.EPISODE_1_7)!!
        assertEquals("번호 다시 매기기 · 1-7", preview.title)
        assertEquals("원고 순서대로 #001부터", preview.subtitle)
        assertTrue(preview.changed >= 2)
        assertEquals(Numbering.liveBlocksOfUnit(doc, K.EPISODE_1_7).size, preview.total)
        assertTrue(RenumberModel.canApply(preview, readOnly = false))
        assertFalse(RenumberModel.canApply(preview, readOnly = true))
    }

    @Test
    fun applyNumbersTheEpisodeInManuscriptOrderAndKeepsKeys() {
        val doc = shuffled()
        val after = RenumberModel.apply(doc, K.EPISODE_1_7, Fx.ctx())
        val blocks = Numbering.liveBlocksOfUnit(after, K.EPISODE_1_7)
        assertEquals((1..blocks.size).toList(), blocks.map { it.number })
        assertEquals(Numbering.liveBlocksOfUnit(doc, K.EPISODE_1_7).map { it.key }, blocks.map { it.key })
        assertEquals(doc.changes, after.changes)
        val again = RenumberModel.preview(after, K.EPISODE_1_7)!!
        assertEquals(0, again.changed)
        assertFalse("nothing left to change", RenumberModel.canApply(again, readOnly = false))
    }

    @Test
    fun onlyLiveEpisodesHaveOne() {
        val doc = Fx.doc()
        assertNull(RenumberModel.preview(doc, K.SCENE_1_7_3))
        assertNull(RenumberModel.preview(doc, K.CHAPTER_1))
        assertNull(RenumberModel.preview(doc, "unknown"))
        assertFalse(RenumberModel.canApply(null, readOnly = false))
        assertSame(doc, RenumberModel.apply(doc, K.SCENE_1_7_3, Fx.ctx()))
    }
}
