package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.renumber.RenumberModel
import io.github.lenticualrstone.civilight.feature.script.state.CommandAction
import io.github.lenticualrstone.civilight.feature.script.state.CommandRouting
import io.github.lenticualrstone.civilight.feature.script.state.InsertAt
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.model.script.BlockKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** SCR-22 `번호 다시 매기기` through the shared `RenumberSheet` and the command field. */
class RenumberModelTest {
    private val doc = Fx.example

    @Test
    fun `SCR-22 the sheet names an episode, never a scene or the project of a multi-level structure`() {
        assertEquals(K.EPISODE_1_7, RenumberModel.unitOf(doc, K.EPISODE_1_7))
        assertNull(RenumberModel.unitOf(doc, K.SCENE_1_7_3))
        assertNull(RenumberModel.unitOf(doc, doc.project.key))
    }

    @Test
    fun `T-12-17 the preview changes nothing and apply numbers live blocks 1 to N in manuscript order`() {
        // A block inserted mid-episode reads out of order (#058 after #043).
        val doc = ScriptOps.insert(this.doc, InsertAt.After(K.BLOCK_043), BlockKind.NARRATION, null, Fx.ctx()).doc
        val preview = RenumberModel.preview(doc, K.EPISODE_1_7)
        assertNotNull(preview)
        assertEquals("번호 다시 매기기 · 1-7", preview!!.title)
        assertTrue(RenumberModel.canApply(preview, readOnly = false))
        assertFalse(RenumberModel.canApply(preview, readOnly = true))
        val after = RenumberModel.apply(doc, K.EPISODE_1_7, Fx.ctx())
        val blocks = io.github.lenticualrstone.civilight.domain.script.Numbering.liveBlocksOfUnit(doc, K.EPISODE_1_7)
        val numbers = blocks.map { Fx.block(after, it.key).number }
        assertEquals((1..blocks.size).toList(), numbers)
        // Nothing left to change: the button is off.
        assertFalse(RenumberModel.canApply(RenumberModel.preview(after, K.EPISODE_1_7), readOnly = false))
    }

    @Test
    fun `a stale sheet leaves the document as it is`() {
        assertSame(doc, RenumberModel.apply(doc, K.SCENE_1_7_3, Fx.ctx()))
    }

    @Test
    fun `SCR-68 the command opens this episode's preview or the episode it names`() {
        assertEquals(CommandAction.Renumber(K.EPISODE_1_7), CommandRouting.route(doc, K.EPISODE_1_7, "번호 다시 매기기"))
        assertEquals(CommandAction.Renumber(K.EPISODE_1_7), CommandRouting.route(doc, K.EPISODE_1_7, "번호 다시 매기기 · 1-7"))
        assertEquals(CommandAction.Renumber(K.EPISODE_1_8), CommandRouting.route(doc, K.EPISODE_1_7, "번호 다시 매기기 · 1-8"))
        assertEquals(CommandAction.Search("번호 다시 매기기 · 9-9"), CommandRouting.route(doc, K.EPISODE_1_7, "번호 다시 매기기 · 9-9"))
        assertEquals(CommandAction.OpenCheckCenter, CommandRouting.route(doc, K.EPISODE_1_7, "점검 센터 열기"))
    }
}
