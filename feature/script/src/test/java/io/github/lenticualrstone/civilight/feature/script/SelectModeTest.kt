package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.script.BlockMove
import io.github.lenticualrstone.civilight.domain.script.BlockPosition
import io.github.lenticualrstone.civilight.domain.script.Records
import io.github.lenticualrstone.civilight.domain.script.StoryTimes
import io.github.lenticualrstone.civilight.feature.script.layers.LayerRow
import io.github.lenticualrstone.civilight.feature.script.layers.LayersModel
import io.github.lenticualrstone.civilight.feature.script.select.DropTarget
import io.github.lenticualrstone.civilight.feature.script.select.MoveTargets
import io.github.lenticualrstone.civilight.feature.script.select.ScriptClipboard
import io.github.lenticualrstone.civilight.feature.script.select.SelectOps
import io.github.lenticualrstone.civilight.feature.script.select.SelectRowModel
import io.github.lenticualrstone.civilight.feature.script.select.SelectRows
import io.github.lenticualrstone.civilight.feature.script.select.SelectRules
import io.github.lenticualrstone.civilight.feature.script.select.SelectState
import io.github.lenticualrstone.civilight.feature.script.state.CommandAction
import io.github.lenticualrstone.civilight.feature.script.state.CommandRouting
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRow
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRows
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.script.ScriptPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** `블록 고르기` and its bar (SCR-36 … SCR-40), consequences per SCR-44, on the example project. */
class SelectModeTest {
    private val doc = Fx.example

    private fun rows(d: ProjectDoc = doc, layers: ScriptLayers = ScriptLayers()): List<ScriptRow> =
        ScriptRows.build(d, K.EPISODE_1_7, StoryTimes.source(d), layers, ScriptPreview.WRITE, true, false, Fx.zone)

    @Test
    fun `SCR-36 entering checks the selected block, rows toggle, 모두 takes the drawn blocks of the scene`() {
        val r = rows()
        val drawn = SelectRules.drawnKeys(r)
        val start = SelectRules.enter(K.BLOCK_043, drawn)
        assertEquals(setOf(K.BLOCK_043), start.checked)
        val two = SelectRules.toggle(start, K.BLOCK_044, drawn)
        assertEquals(setOf(K.BLOCK_043, K.BLOCK_044), two.checked)
        assertEquals(setOf(K.BLOCK_043), SelectRules.toggle(two, K.BLOCK_044, drawn).checked)
        val all = SelectRules.all(start, r, K.SCENE_1_7_3)
        assertEquals(doc.manuscript.liveBlocksOf(K.SCENE_1_7_3).map { it.key }.toSet(), all.checked)
        // 모두 again: none of the scene.
        assertTrue(SelectRules.all(all, r, K.SCENE_1_7_3).checked.isEmpty())
        assertEquals(listOf(K.BLOCK_043, K.BLOCK_044), SelectRules.ordered(SelectState(true, setOf(K.BLOCK_044, K.BLOCK_043)), r))
    }

    @Test
    fun `SCR-36 a block a layer hides cannot be checked, and checks on it drop out`() {
        val hidden = LayersModel.set(ScriptLayers(), LayerRow.DIRECTION, false)
        val r = rows(layers = hidden)
        val drawn = SelectRules.drawnKeys(r)
        assertFalse(K.BLOCK_041 in drawn)
        assertFalse(SelectRules.toggle(SelectState(true), K.BLOCK_041, drawn).isChecked(K.BLOCK_041))
        assertFalse(K.BLOCK_041 in SelectRules.all(SelectState(true), r, K.SCENE_1_7_3).checked)
        assertEquals(setOf(K.BLOCK_043), SelectRules.reconcile(SelectState(true, setOf(K.BLOCK_041, K.BLOCK_043)), drawn).checked)
    }

    @Test
    fun `SCR-36 rows read 하르트 #043 with marks, heads stay as dividers, the bar counts what rides along`() {
        val r = rows()
        val state = SelectState(true, setOf(K.BLOCK_042, K.BLOCK_043, K.BLOCK_044))
        val select = SelectRows.of(doc, r, state, ScriptLayers(), unlocked = true)
        val row043 = select.filterIsInstance<SelectRowModel.Block>().single { it.blockKey == K.BLOCK_043 }
        assertEquals("하르트 #043", row043.label)
        assertTrue(row043.checked)
        assertEquals(3, row043.marks.count { it.glyph == "◆" })
        assertEquals("연출 #041", select.filterIsInstance<SelectRowModel.Block>().single { it.blockKey == K.BLOCK_041 }.label)
        assertTrue(select.any { it is SelectRowModel.Head && it.sceneKey == K.SCENE_1_7_3 })
        val bar = SelectRows.bar(doc, state)
        assertEquals("고른 블록 3개", bar.count)
        val records = listOf(K.BLOCK_042, K.BLOCK_043, K.BLOCK_044).flatMap { b -> Records.marksOn(doc, b) }
        assertEquals("#042 ~ #044 · ◆ ${records.count { it.glyph == "◆" }} · ◇ ${records.count { it.glyph == "◇" }} 같이 감", bar.range)
        assertEquals(listOf("옮기기", "지우기", "잘라내기", "복사"), SelectRows.BUTTONS)
        assertFalse(SelectRows.bar(doc, SelectState(true)).enabled)
    }

    @Test
    fun `rule 19 select rows mask a locked hidden speaker as the manuscript does`() {
        val d = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.HART) it.copy(hidden = true) else it }))
        val r = rows(d)
        val locked = SelectRows.of(d, r, SelectState(true), ScriptLayers(), unlocked = false)
        assertEquals("숨긴 인물 #043", locked.filterIsInstance<SelectRowModel.Block>().single { it.blockKey == K.BLOCK_043 }.label)
        val open = SelectRows.of(d, r, SelectState(true), ScriptLayers(), unlocked = true)
        assertEquals("하르트 #043", open.filterIsInstance<SelectRowModel.Block>().single { it.blockKey == K.BLOCK_043 }.label)
    }

    @Test
    fun `SCR-68 the command 블록 고르기 enters select mode`() {
        assertEquals(CommandAction.SelectMode, CommandRouting.route(doc, K.EPISODE_1_7, "블록 고르기"))
    }

    @Test
    fun `SCR-37 the picker lists scenes, then 맨 앞 and the blocks below which the moved ones can go`() {
        val scenes = MoveTargets.scenes(doc, listOf(K.BLOCK_043), unlocked = true)
        val s3 = scenes.single { it.sceneKey == K.SCENE_1_7_3 }
        assertEquals("1-7 씬 3", s3.location)
        assertEquals("꺼진 등불", s3.title)
        assertTrue(s3.current)
        val positions = MoveTargets.positions(doc, K.SCENE_1_7_3, listOf(K.BLOCK_043), unlocked = true)
        assertEquals("맨 앞", positions.first().label)
        assertEquals(BlockPosition.SceneStart(K.SCENE_1_7_3), positions.first().position)
        assertEquals("#036 아래", positions[1].label)
        assertTrue(positions.none { (it.position as? BlockPosition.After)?.blockKey == K.BLOCK_043 })
    }

    @Test
    fun `T-12-11 moving to another scene of the episode keeps number and records, with the toast`() {
        val out = SelectOps.move(doc, listOf(K.BLOCK_043), BlockPosition.SceneStart(K.SCENE_1_7_4), Fx.ctx())!!
        val moved = Fx.block(out.doc, K.BLOCK_043)
        assertEquals(K.SCENE_1_7_4, moved.sceneKey)
        assertEquals(43, moved.number)
        assertEquals("◆ 3개 씬 3 → 씬 4", out.toast)
        assertEquals(3, Records.marksOn(out.doc, K.BLOCK_043).count { it.glyph == "◆" })
        // Within the scene: no toast.
        assertNull(SelectOps.move(doc, listOf(K.BLOCK_043), BlockPosition.After(K.BLOCK_048), Fx.ctx())!!.toast)
    }

    @Test
    fun `T-12-12 moving to another episode takes that episode's next number`() {
        val next = doc.index.unit(K.EPISODE_1_8)!!.nextBlockNumber!!
        val out = SelectOps.move(doc, listOf(K.BLOCK_043), BlockPosition.SceneStart(K.SCENE_1_8_1), Fx.ctx())!!
        assertEquals(next, Fx.block(out.doc, K.BLOCK_043).number)
        assertEquals("◆ 3개 1-7 → 1-8", out.toast)
    }

    @Test
    fun `T-12-10 copy pastes new blocks without records, T-12-22 cut pastes as a move`() {
        val copy = BlockMove.copy(doc, listOf(K.BLOCK_043))
        val pasted = SelectOps.paste(doc, copy, K.BLOCK_048, Fx.ctx())!!
        val newKey = pasted.select!!
        assertNotEquals(K.BLOCK_043, newKey)
        assertEquals(Fx.block(doc, K.BLOCK_043).runs.size, Fx.block(pasted.doc, newKey).runs.size)
        assertEquals(58, Fx.block(pasted.doc, newKey).number)
        assertTrue(Records.marksOn(pasted.doc, newKey).isEmpty())
        assertEquals("◆는 복사되지 않음", pasted.toast)

        val cut = BlockMove.cut(doc, listOf(K.BLOCK_043))
        val moved = SelectOps.paste(doc, cut, K.BLOCK_1_8_001, Fx.ctx())!!
        assertEquals(K.BLOCK_043, moved.select)
        assertEquals(K.SCENE_1_8_1, Fx.block(moved.doc, K.BLOCK_043).sceneKey)
        assertEquals("◆ 3개 1-7 → 1-8", moved.toast)
        assertTrue(moved.doc.changes.records.filter { (it.anchor as? Anchor.AtBlock)?.blockKey == K.BLOCK_043 }.size >= 3)
    }

    @Test
    fun `SCR-40 the clipboard belongs to one project and a cut is pasted once`() {
        val cut = BlockMove.cut(doc, listOf(K.BLOCK_043))
        ScriptClipboard.put(doc.key, cut)
        val entry = ScriptClipboard.of(doc.key)!!
        assertNull(ScriptClipboard.of("another"))
        ScriptClipboard.afterPaste(entry)
        assertNull(ScriptClipboard.of(doc.key))
        val copy = BlockMove.copy(doc, listOf(K.BLOCK_043))
        ScriptClipboard.put(doc.key, copy)
        ScriptClipboard.afterPaste(ScriptClipboard.of(doc.key)!!)
        assertEquals(1, ScriptClipboard.of(doc.key)!!.count)
    }

    @Test
    fun `SCR-37 a drop maps to the gap, down below the row, up above it, heads are gaps`() {
        val h3 = DropTarget.Row.Head("S3")
        val a = DropTarget.Row.Block("S3", "a")
        val b = DropTarget.Row.Block("S3", "b")
        val c = DropTarget.Row.Block("S3", "c")
        val h4 = DropTarget.Row.Head("S4")
        val d = DropTarget.Row.Block("S4", "d")
        val rows = listOf(h3, a, b, c, h4, d)
        // a dragged down onto c: below c.
        assertEquals(BlockPosition.After("c"), DropTarget.of(rows, 1, 3, setOf("a")))
        // c dragged up onto a: above a = first in S3.
        assertEquals(BlockPosition.SceneStart("S3"), DropTarget.of(rows, 3, 1, setOf("c")))
        // a dragged down onto the S4 head: first in S4.
        assertEquals(BlockPosition.SceneStart("S4"), DropTarget.of(rows, 1, 4, setOf("a")))
        // d dragged up onto the S4 head: last in S3 (below c).
        assertEquals(BlockPosition.After("c"), DropTarget.of(rows, 5, 4, setOf("d")))
        // b and c moving, b dragged up onto c's neighbour a: above a.
        assertEquals(BlockPosition.SceneStart("S3"), DropTarget.of(rows, 2, 1, setOf("b", "c")))
        // Dropped onto a moving row below: the gap above it, below the nearest row that stays.
        assertEquals(BlockPosition.After("a"), DropTarget.of(rows, 2, 3, setOf("b", "c")))
        assertNull(DropTarget.of(rows, 2, 2, setOf("b")))
    }
}
