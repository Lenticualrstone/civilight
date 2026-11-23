package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.script.BlockPosition
import io.github.lenticualrstone.civilight.domain.script.ScriptWrite
import io.github.lenticualrstone.civilight.domain.script.StoryTimes
import io.github.lenticualrstone.civilight.feature.script.input.Draft
import io.github.lenticualrstone.civilight.feature.script.input.DraftMark
import io.github.lenticualrstone.civilight.feature.script.input.Drafts
import io.github.lenticualrstone.civilight.feature.script.input.KindMenu
import io.github.lenticualrstone.civilight.feature.script.input.WriteCopy
import io.github.lenticualrstone.civilight.feature.script.layers.LayersModel
import io.github.lenticualrstone.civilight.feature.script.state.BlockRow
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRows
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.script.ScriptPreview
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** (0.2.0) Review fixes of the script lane: hidden links, double taps, 낭독 chip, kind menu, 다음 씬. */
class Review020FeatureTest {
    private val doc: ProjectDoc get() = Fx.example

    private fun typed(d: Draft, text: String): Draft = Drafts.textChanged(d, text, text.length, text.length).first

    private fun hideTerm(d: ProjectDoc): ProjectDoc =
        d.copy(world = d.world.copy(terms = d.world.terms.map { if (it.key == K.TERM_ORIGINIUM_LANTERN) it.copy(hidden = true) else it }))

    private val secret = "비밀등불"
    private fun link() = TextRun.Link("01JZ000000LINK000000000088", io.github.lenticualrstone.civilight.model.text.LinkTarget(
        io.github.lenticualrstone.civilight.model.text.LinkTargetKind.TERM, K.TERM_ORIGINIUM_LANTERN,
    ), secret)

    /** A 연출 line and a dialogue line linking the lantern term. */
    private fun linked(): ProjectDoc = doc.copy(
        episodes = doc.episodes.mapValues { (_, f) ->
            f.copy(blocks = f.blocks.map {
                when (it.key) {
                    K.BLOCK_041 -> it.copy(runs = listOf(TextRun.Plain("▸ "), link(), TextRun.Plain(" 등장")))
                    K.BLOCK_047 -> it.copy(runs = listOf(TextRun.Plain("저 "), link(), TextRun.Plain(" 봐")))
                    else -> it
                }
            })
        },
    )

    private fun rows(d: ProjectDoc, unlocked: Boolean) = ScriptRows.build(
        d, K.EPISODE_1_7, StoryTimes.source(d), ScriptLayers(), ScriptPreview.WRITE, unlocked = unlocked, readOnly = false, zone = Fx.zone,
    ).filterIsInstance<BlockRow>()

    // --- SCR-55: hidden links on the 0.2.0 surfaces ---

    @Test
    fun `SCR-55 a 연출 row draws a locked hidden link as 숨긴 항목 and drops 고치기`() {
        val hidden = hideTerm(linked())
        val locked = rows(hidden, unlocked = false)
        val direction = locked.single { it.blockKey == K.BLOCK_041 }
        assertFalse(direction.text, direction.text.contains(secret))
        assertTrue(direction.text, direction.text.contains("숨긴 항목"))
        assertTrue(direction.editLocked)
        assertTrue(locked.single { it.blockKey == K.BLOCK_047 }.editLocked)
        assertFalse(locked.single { it.blockKey == K.BLOCK_043 }.editLocked)
        val open = rows(hidden, unlocked = true)
        assertTrue(open.single { it.blockKey == K.BLOCK_041 }.text.contains(secret))
        assertFalse(open.single { it.blockKey == K.BLOCK_041 }.editLocked)
    }

    @Test
    fun `SCR-55 a 고치기 block that comes to link a locked hidden item ends with its text out of the field`() {
        val d = linked()
        val editing = typed(Drafts.startEdit(typed(Draft(), "준비"), d.index.block(K.BLOCK_047)!!), "저 비밀등불 봐!")
        val hidden = hideTerm(d)
        val after = Drafts.reconcile(editing, hidden, K.EPISODE_1_7, unlocked = false)
        assertEquals("준비", after.text)
        assertFalse(after.text.contains(secret))
        assertEquals(DraftMark.None, after.mark)
        // Unlocked, 고치기 goes on.
        assertEquals(editing, Drafts.reconcile(editing, hidden, K.EPISODE_1_7, unlocked = true))
        // A link to the item added in the field while unlocked leaves with a re-lock too.
        val plain = d.copy(episodes = d.episodes.mapValues { (_, f) -> f.copy(blocks = f.blocks.map { if (it.key == K.BLOCK_047) it.copy(runs = listOf(TextRun.Plain("그냥 봐"))) else it }) })
        val withLink = Drafts.insertLink(Drafts.startEdit(typed(Draft(), "준비"), plain.index.block(K.BLOCK_047)!!), link())
        assertEquals("준비", Drafts.reconcile(withLink, hideTerm(plain), K.EPISODE_1_7, unlocked = false).text)
        // A removed block whose field holds a hidden link: the field never keeps the link (no join).
        val removed = io.github.lenticualrstone.civilight.domain.script.BlockTrash.delete(hideTerm(plain), listOf(K.BLOCK_047), Fx.ctx()).doc
        val gone = Drafts.reconcile(withLink, removed, K.EPISODE_1_7, unlocked = false)
        assertEquals("준비", gone.text)
        assertTrue(gone.runs.none { it is TextRun.Link })
        // An 이 뒤에 mark on such a block stays (only 고치기 puts its text into the field).
        val after2 = Drafts.reconcile(Drafts.insertAfter(Draft(), K.BLOCK_047), hidden, K.EPISODE_1_7, unlocked = false)
        assertEquals(DraftMark.After(K.BLOCK_047), after2.mark)
    }

    // --- Double taps: one send, one split ---

    @Test
    fun `a second send queued before the first ran sends nothing`() {
        var live = typed(Draft(speakerKey = K.RIEN), "한 번만")
        var d = doc
        val before = d.manuscript.liveBlocksOf(K.SCENE_1_7_3).size
        val firstTap = live
        val secondTap = live
        for (captured in listOf(firstTap, secondTap)) {
            if (!Drafts.stillPending(captured, live)) continue
            d = ScriptWrite.send(d, BlockPosition.SceneEnd(K.SCENE_1_7_3), Drafts.toWrite(captured, false), false, Fx.ctx())!!.doc
            live = Drafts.sent(live)
        }
        assertEquals(before + 1, d.manuscript.liveBlocksOf(K.SCENE_1_7_3).size)
        assertEquals("", live.text)
    }

    @Test
    fun `a second 고치기 send, 나누기 or 합치기 after the first sees the set-aside draft and does nothing`() {
        val editing = typed(Drafts.startEdit(typed(Draft(), "따로"), doc.index.block(K.BLOCK_047)!!), "앞 뒤")
        val afterFirst = Drafts.sent(editing)
        assertEquals("따로", afterFirst.text)
        assertFalse(Drafts.stillPending(editing, afterFirst))
        assertTrue(Drafts.stillPending(editing, editing.copy(selStart = 1, selEnd = 1)))
        // A changed chip or cue is a different draft too.
        assertFalse(Drafts.stillPending(editing, editing.copy(kind = BlockKind.NARRATION)))
    }

    // --- SCR-28: the 낭독 chip ---

    @Test
    fun `SCR-28 the 낭독 chip and command are off when the episode has no step`() {
        assertTrue(LayersModel.readAloudEnabled(doc, K.EPISODE_1_7, unlocked = false))
        assertTrue(LayersModel.chips(true).single { it.preview == ScriptPreview.READ_ALOUD }.enabled)
        // Every scene hidden and locked: nothing to play.
        val scenes = doc.story.units.filter { it.parentKey == K.EPISODE_1_7 }.map { it.key }.toSet()
        val hidden = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key in scenes) it.copy(hidden = true) else it }))
        assertFalse(LayersModel.readAloudEnabled(hidden, K.EPISODE_1_7, unlocked = false))
        assertTrue(LayersModel.readAloudEnabled(hidden, K.EPISODE_1_7, unlocked = true))
        // An empty episode (a new project's first one).
        val empty = doc.copy(story = doc.story.copy(units = doc.story.units.filterNot { it.key in scenes }))
        assertFalse(LayersModel.readAloudEnabled(empty, K.EPISODE_1_7, unlocked = false))
        val chips = LayersModel.chips(false)
        assertFalse(chips.single { it.preview == ScriptPreview.READ_ALOUD }.enabled)
        assertTrue(chips.filter { it.preview != ScriptPreview.READ_ALOUD }.all { it.enabled })
    }

    // --- G-2: the kind menu names no cue with the switch off ---

    @Test
    fun `G-2 with 게임 제작 도구 off a cue-holding 연출 keeps its kind with a neutral reason`() {
        val b41 = doc.index.block(K.BLOCK_041)!!
        val off = KindMenu.rows(BlockKind.DIRECTION, b41, null, gameTools = false)
        val narration = off.single { it.kind == BlockKind.NARRATION }
        assertFalse(narration.enabled)
        assertEquals(WriteCopy.KIND_KEEPS_DATA, narration.sub)
        assertTrue(off.none { it.sub == WriteCopy.KIND_HAS_CUES })
        assertEquals(WriteCopy.KIND_HAS_CUES, KindMenu.rows(BlockKind.DIRECTION, b41, null, gameTools = true).single { it.kind == BlockKind.NARRATION }.sub)
    }

    // --- SCR-74: no empty scenes piling up ---

    private fun actx(): io.github.lenticualrstone.civilight.domain.actions.ActionContext {
        var n = 0
        return io.github.lenticualrstone.civilight.domain.actions.ActionContext(
            now = io.github.lenticualrstone.civilight.model.sample.ExampleClock.NOW, zone = Fx.zone, device = "test",
            newKey = { "01JZ000000RVSC" + (++n).toString().padStart(12, '0') },
        )
    }

    @Test
    fun `SCR-74 a double tap of 다음 씬 with the same target makes one scene`() {
        assertNull(NextScene.emptyAfter(doc, K.EPISODE_1_7, K.PROJECT, K.SCENE_1_7_3, null))
        val (out, made) = NextScene.create(doc, K.EPISODE_1_7, K.PROJECT, K.SCENE_1_7_3, actx())!!
        // The second tap still names 씬 3 (it has blocks), but the scene just made after it is empty.
        assertEquals(made, NextScene.emptyAfter(out, K.EPISODE_1_7, K.PROJECT, K.SCENE_1_7_3, made))
        // Once the view moved to the new scene, the target itself is empty.
        assertEquals(made, NextScene.emptyAfter(out, K.EPISODE_1_7, K.PROJECT, made, made))
        // A send into it lets the next tap create again.
        val sent = ScriptWrite.send(out, BlockPosition.SceneEnd(made), Drafts.toWrite(typed(Draft(speakerKey = K.RIEN), "가"), false), false, Fx.ctx())!!.doc
        assertNull(NextScene.emptyAfter(sent, K.EPISODE_1_7, K.PROJECT, made, made))
        // An empty episode may always make its first scene.
        val scenes = doc.story.units.filter { it.parentKey == K.EPISODE_1_7 }.map { it.key }.toSet()
        val empty = doc.copy(story = doc.story.copy(units = doc.story.units.filterNot { it.key in scenes }))
        assertNull(NextScene.emptyAfter(empty, K.EPISODE_1_7, K.PROJECT, null, null))
        assertNotNull(NextScene.create(empty, K.EPISODE_1_7, K.PROJECT, null, actx()))
    }

    // --- Copy: new words live in WriteCopy ---

    @Test
    fun `new labels are in WriteCopy`() {
        assertEquals("씬 3", WriteCopy.sceneLabel(3))
        assertEquals("블록 12", WriteCopy.versionBlocks(12))
        assertEquals("› 낭독", WriteCopy.commandRow(WriteCopy.READ_ALOUD_CMD))
        assertTrue(WriteCopy.all.contains(WriteCopy.projectBlocksSection(12)))
        assertTrue(WriteCopy.all.contains(WriteCopy.KIND_KEEPS_DATA))
    }
}
