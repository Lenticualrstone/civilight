package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.script.BlockPosition
import io.github.lenticualrstone.civilight.domain.script.Replace
import io.github.lenticualrstone.civilight.domain.script.ScriptWrite
import io.github.lenticualrstone.civilight.feature.script.input.BottomAnchor
import io.github.lenticualrstone.civilight.feature.script.input.Draft
import io.github.lenticualrstone.civilight.feature.script.input.DraftEffect
import io.github.lenticualrstone.civilight.feature.script.input.DraftMark
import io.github.lenticualrstone.civilight.feature.script.input.Drafts
import io.github.lenticualrstone.civilight.feature.script.input.KindMenu
import io.github.lenticualrstone.civilight.feature.script.input.ReadAloudNav
import io.github.lenticualrstone.civilight.feature.script.input.SceneInView
import io.github.lenticualrstone.civilight.feature.script.input.WriteCopy
import io.github.lenticualrstone.civilight.feature.script.input.cueChipText
import io.github.lenticualrstone.civilight.feature.script.input.hitLabel
import io.github.lenticualrstone.civilight.feature.script.input.rawText
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.script.DirectionCue
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** (0.2.0) The input bar's draft and the screen-side rules of U-14, N-27, N-28 (SCR §4.7, feature side). */
class Write020FeatureTest {
    private val doc: ProjectDoc get() = Fx.example

    private fun typed(d: Draft, text: String): Draft = Drafts.textChanged(d, text, text.length, text.length).first

    // --- T-U14-01: chips keep their values after sends ---

    @Test
    fun `T-U14-01 a send clears the text and keeps both chips, three times over`() {
        var d = Draft(speakerKey = K.RIEN, kind = BlockKind.DIALOGUE)
        repeat(3) { i ->
            d = typed(d, "줄 $i")
            assertEquals("줄 $i", d.text)
            d = Drafts.sent(d)
            assertEquals("", d.text)
            assertEquals(K.RIEN, d.speakerKey)
            assertEquals(BlockKind.DIALOGUE, d.kind)
            assertEquals(DraftMark.None, d.mark)
        }
    }

    // --- T-U14-02: Enter is a line break in the draft ---

    @Test
    fun `T-U14-02 a typed newline becomes a Break run and the draft sends as one block`() {
        val d = typed(Draft(), "하나\n둘")
        assertEquals(listOf(TextRun.Plain("하나"), TextRun.Break, TextRun.Plain("둘")), d.runs)
        val sent = ScriptWrite.send(doc, BlockPosition.SceneEnd(K.SCENE_1_7_3), Drafts.toWrite(d.copy(speakerKey = K.RIEN), false), false, Fx.ctx())!!
        assertEquals(listOf(TextRun.Plain("하나"), TextRun.Break, TextRun.Plain("둘")), sent.doc.index.block(sent.blockKey)!!.runs)
        assertEquals(K.SCENE_1_7_3, sent.doc.index.block(sent.blockKey)!!.sceneKey)
    }

    @Test
    fun `a typed at sign opens the link picker and is not kept`() {
        val (d, effect) = Drafts.textChanged(typed(Draft(), "가"), "가@", 2, 2)
        assertEquals(DraftEffect.OPEN_LINK_PICKER, effect)
        assertEquals("가", d.text)
    }

    // --- T-U14-04 / 05: 이 뒤에 ---

    @Test
    fun `T-U14-04 이 뒤에 is used once, and T-U14-05 it clears when its block goes`() {
        var d = Drafts.insertAfter(typed(Draft(speakerKey = K.RIEN), "저기."), K.BLOCK_044)
        assertEquals(DraftMark.After(K.BLOCK_044), d.mark)
        assertEquals("저기.", d.text)
        d = Drafts.sent(d)
        assertEquals(DraftMark.None, d.mark)

        val marked = Drafts.insertAfter(Draft(), K.BLOCK_044)
        val deleted = io.github.lenticualrstone.civilight.domain.script.BlockTrash.delete(doc, listOf(K.BLOCK_044), Fx.ctx()).doc
        assertEquals(DraftMark.None, Drafts.reconcile(marked, deleted, K.EPISODE_1_7, unlocked = false).mark)
        // While 고치기 runs, 이 뒤에 cannot be set (no right swipe).
        val editing = Drafts.startEdit(Draft(), doc.index.block(K.BLOCK_047)!!)
        assertEquals(editing, Drafts.insertAfter(editing, K.BLOCK_044))
    }

    // --- T-U14-08: 고치기 sets the draft aside and gives it back ---

    @Test
    fun `T-U14-08 고치기 takes the block, ✕ gives the set-aside draft and chips back`() {
        val before = typed(Draft(speakerKey = K.RIEN, kind = BlockKind.DIALOGUE), "준비")
        val b47 = doc.index.block(K.BLOCK_047)!!
        val editing = Drafts.startEdit(before, b47)
        assertEquals(b47.runs.plainText, editing.runs.plainText)
        assertEquals(b47.speakerKey, editing.speakerKey)
        assertEquals(BlockKind.DIALOGUE, editing.kind)
        assertEquals(DraftMark.Edit(K.BLOCK_047), editing.mark)
        assertEquals(rawText(editing.runs).length, editing.caret)
        val changed = typed(editing, editing.text + " 더")
        val closed = Drafts.close(changed)
        assertEquals("준비", closed.text)
        assertEquals(K.RIEN, closed.speakerKey)
        assertEquals(DraftMark.None, closed.mark)
        assertNull(closed.aside)
        // A finished 고치기 send gives the set-aside draft back too.
        assertEquals("준비", Drafts.sent(changed).text)
    }

    // --- T-U14-09 / T-U14-25: the 고치기 block goes away ---

    @Test
    fun `T-U14-09 a removed 고치기 block keeps both the set-aside text and the edited text`() {
        val aside = typed(Draft(speakerKey = K.RIEN, kind = BlockKind.DIALOGUE), "준비")
        val editing = typed(Drafts.startEdit(aside, doc.index.block(K.BLOCK_047)!!), "새 글")
        val removed = io.github.lenticualrstone.civilight.domain.script.BlockTrash.delete(doc, listOf(K.BLOCK_047), Fx.ctx()).doc
        val after = Drafts.reconcile(editing, removed, K.EPISODE_1_7, unlocked = false)
        // Nothing typed is lost (SCR-72): the set-aside text, a line break, then the edited text; the aside's chips.
        assertEquals("준비\n새 글", after.text)
        assertEquals(listOf(TextRun.Plain("준비"), TextRun.Break, TextRun.Plain("새 글")), after.runs)
        assertEquals(K.RIEN, after.speakerKey)
        assertEquals(DraftMark.None, after.mark)
        assertNull(after.aside)
        assertEquals(after.text.length, after.selEnd)
    }

    @Test
    fun `T-U14-09 with a blank set-aside draft the edited text stays alone`() {
        val editing = typed(Drafts.startEdit(Draft(), doc.index.block(K.BLOCK_047)!!), "새 글")
        val removed = io.github.lenticualrstone.civilight.domain.script.BlockTrash.delete(doc, listOf(K.BLOCK_047), Fx.ctx()).doc
        val after = Drafts.reconcile(editing, removed, K.EPISODE_1_7, unlocked = false)
        assertEquals("새 글", after.text)
        assertEquals(DraftMark.None, after.mark)
        assertNull(after.aside)
        // A blank edit keeps the set-aside text as it was.
        val blank = typed(Drafts.startEdit(typed(Draft(), "준비"), doc.index.block(K.BLOCK_047)!!), "")
        assertEquals("준비", Drafts.reconcile(blank, removed, K.EPISODE_1_7, unlocked = false).text)
    }

    @Test
    fun `T-U14-25 a 고치기 block that becomes hidden and locked leaves no text of it in the field`() {
        val editing = typed(Drafts.startEdit(typed(Draft(), "준비"), doc.index.block(K.BLOCK_047)!!), "비밀 줄")
        val hidden = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == K.SCENE_1_7_3) it.copy(hidden = true) else it }))
        val locked = Drafts.reconcile(editing, hidden, K.EPISODE_1_7, unlocked = false)
        assertEquals("준비", locked.text)
        assertFalse(locked.text.contains("비밀"))
        assertEquals(DraftMark.None, locked.mark)
        // Unlocked, nothing changes.
        assertEquals(editing, Drafts.reconcile(editing, hidden, K.EPISODE_1_7, unlocked = true))
        // A chip speaker that becomes hidden and locked resets to 화자 없음 (SCR-18).
        val hart = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.HART) it.copy(hidden = true) else it }))
        assertNull(Drafts.reconcile(Draft(speakerKey = K.HART), hart, K.EPISODE_1_7, unlocked = false).speakerKey)
    }

    // --- T-U14-10: 나누기 availability ---

    @Test
    fun `T-U14-10 나누기 needs text on both sides of the caret, never on a choice`() {
        val editing = Drafts.startEdit(Draft(), doc.index.block(K.BLOCK_047)!!)
        assertFalse(Drafts.canSplit(editing.copy(selStart = 0, selEnd = 0)))
        assertFalse(Drafts.canSplit(editing))
        val mid = editing.text.indexOf(' ').coerceAtLeast(1)
        assertTrue(Drafts.canSplit(editing.copy(selStart = mid, selEnd = mid)))
        val (front, back) = Drafts.splitParts(editing.copy(selStart = mid, selEnd = mid))
        assertEquals(editing.text, rawText(front) + rawText(back))
        assertFalse(Drafts.canSplit(editing.copy(kind = BlockKind.CHOICE, selStart = mid, selEnd = mid)))
        assertFalse(Drafts.canSplit(typed(Draft(), "가 나").copy(selStart = 1, selEnd = 1)))
    }

    // --- T-U14-23: the draft survives encode and decode ---

    @Test
    fun `T-U14-23 a draft with a 고치기 mark, its aside and the caret round-trips`() {
        val aside = Draft(runs = listOf(TextRun.Plain("준비")), selStart = 2, selEnd = 2, speakerKey = K.RIEN, kind = BlockKind.DIALOGUE)
        val d = Draft(runs = listOf(TextRun.Plain("멈춰.")), selStart = 4, selEnd = 4, mark = DraftMark.Edit(K.BLOCK_043), aside = aside, speakerKey = K.HART)
        assertEquals(d, Drafts.decode(Drafts.encode(d)))
        assertNull(Drafts.decode("not json"))
        assertNull(Drafts.decode(null))
    }

    // --- Switch off: the draft hands no cues, so stored cues stay (scope §8) ---

    @Test
    fun `cue chips are sent only with the switch on`() {
        val cue = DirectionCue(key = "c1", kind = OpenEnum.of(CueKind.SCREEN_EFFECT), effect = "blackout")
        val d = Drafts.addCue(Draft(kind = BlockKind.DIRECTION), cue)
        assertEquals(listOf(cue), Drafts.toWrite(d, gameTools = true).cues)
        assertNull(Drafts.toWrite(d, gameTools = false).cues)
        assertTrue(Drafts.removeCue(d, "c1").cues.isEmpty())
        assertEquals("화면 효과 암전", cueChipText(doc, cue))
    }

    // --- T-U14-15: the scene in view ---

    @Test
    fun `T-U14-15 SceneInView follows the item above the input bar and never a locked scene`() {
        val s3 = "S3"
        val s4 = "S4"
        val items = listOf(
            SceneInView.Item(s3, SceneInView.Kind.HEAD, 100f, 140f),
            SceneInView.Item(s3, SceneInView.Kind.BLOCK, 150f, 200f),
            SceneInView.Item(s3, SceneInView.Kind.BLOCK, 210f, 260f),
            SceneInView.Item(s4, SceneInView.Kind.HEAD, 270f, 310f),
            SceneInView.Item(s4, SceneInView.Kind.BLOCK, 320f, 360f),
        )
        assertEquals(s3, SceneInView.target(items, 265f))
        assertEquals(s4, SceneInView.target(items, 300f))
        assertEquals(s3, SceneInView.target(items, 120f))
        assertEquals(s3, SceneInView.target(items, 90f))
        val hidden = items.take(3) + SceneInView.Item(s4, SceneInView.Kind.HIDDEN_ROW, 270f, 320f)
        assertEquals(s3, SceneInView.target(hidden, 330f))
        assertNull(SceneInView.target(listOf(SceneInView.Item(s4, SceneInView.Kind.HIDDEN_ROW, 0f, 10f)), 330f))
        assertNull(SceneInView.target(emptyList(), 330f))
    }

    @Test
    fun `SCR-71 the manuscript moves with the bar's top edge, not while dragging`() {
        assertEquals(0f, BottomAnchor.delta(null, 700, dragging = false))
        assertEquals(48f, BottomAnchor.delta(700, 652, dragging = false))
        assertEquals(-48f, BottomAnchor.delta(652, 700, dragging = false))
        assertEquals(0f, BottomAnchor.delta(700, 400, dragging = true))
    }

    // --- SCR-12 / 13: the kind menu ---

    @Test
    fun `SCR-12 the kind menu reads as the board, dims a cue-holding 연출 with its reason`() {
        val rows = KindMenu.rows(BlockKind.DIALOGUE, null, null)
        assertEquals(listOf("대사", "나레이션", "연출", "선택지", "시스템", "메모"), rows.map { it.name })
        assertEquals(listOf(null, "화자 없음", "글 한 줄", "화자 없음 · 2개 이상", "화면 문구", "게임 출력 제외"), rows.map { it.sub })
        assertTrue(rows.all { it.enabled })
        assertTrue(rows.single { it.kind == BlockKind.DIALOGUE }.current)
        val b41 = doc.index.block(K.BLOCK_041)!!
        val swipe = KindMenu.rows(BlockKind.DIRECTION, b41, null)
        assertFalse(swipe.single { it.kind == BlockKind.NARRATION }.enabled)
        assertEquals(WriteCopy.KIND_HAS_CUES, swipe.single { it.kind == BlockKind.NARRATION }.sub)
        // A one-line block cannot become a choice from the swipe; in 고치기 the send rule checks the lines instead.
        val b44 = doc.index.block(K.BLOCK_044)!!
        assertEquals(WriteCopy.KIND_CHOICE_LINES, KindMenu.rows(BlockKind.NARRATION, b44, null).single { it.kind == BlockKind.CHOICE }.sub)
        assertTrue(KindMenu.rows(BlockKind.NARRATION, null, b44).single { it.kind == BlockKind.CHOICE }.enabled)
    }

    // --- N-27: 낭독 navigation ---

    @Test
    fun `N-27 이전 and 다음 stop at the ends`() {
        assertEquals(1, ReadAloudNav.next(0, 3))
        assertEquals(2, ReadAloudNav.next(2, 3))
        assertEquals(0, ReadAloudNav.prev(0))
        assertFalse(ReadAloudNav.canPrev(0))
        assertFalse(ReadAloudNav.canNext(2, 3))
        assertTrue(ReadAloudNav.canNext(1, 3))
    }

    // --- N-28: replace rows never name a locked hidden speaker ---

    @Test
    fun `T-U14-19 a replace row of a locked hidden speaker reads 숨긴 인물`() {
        val b43 = doc.index.block(K.BLOCK_043)!!
        val word = b43.runs.plainText.take(2)
        val hidden = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.HART) it.copy(hidden = true) else it }))
        val hit = Replace.find(hidden, word).first { it.blockKey == K.BLOCK_043 }
        val label = hitLabel(hidden, hit, unlocked = false)
        assertTrue(label, label.contains("숨긴 인물"))
        assertFalse(label, label.contains("하르트"))
        assertTrue(label.contains("#043"))
    }

    // --- The field's text round-trips through the runs (IME composing ranges depend on it) ---

    @Test
    fun `typing keeps the field text exactly - trailing space, Enter at the end, empty lines, leading space`() {
        for (t in listOf("가 ", "가\n", "가\n\n나", " 가", "\n", "가 나 ")) {
            val d = Drafts.textChanged(Draft(), t, t.length, t.length).first
            assertEquals(t, d.text)
            assertEquals(t.length, d.caret)
        }
        // Step by step, as a keyboard sends it.
        var d = Draft()
        for (t in listOf("가", "가\n", "가\n나", "가\n나 ")) d = Drafts.textChanged(d, t, t.length, t.length).first
        assertEquals("가\n나 ", d.text)
    }

    // --- SCR-20 / SCR-28: a locked hidden speaker never shows through a block display name ---

    @Test
    fun `a locked hidden speaker reads 숨긴 인물 in the row and in 낭독, even with a block display name`() {
        val named = io.github.lenticualrstone.civilight.domain.actions.BlockFieldActions.setDisplayNameOverride(doc, K.BLOCK_047, "경비병", actx())
        assertEquals("경비병", named.index.block(K.BLOCK_047)!!.displayNameOverride)
        val hidden = named.copy(cast = named.cast.copy(characters = named.cast.characters.map { if (it.key == K.HART) it.copy(hidden = true) else it }))
        val time = io.github.lenticualrstone.civilight.domain.script.StoryTimes.source(hidden)
        val rows = io.github.lenticualrstone.civilight.feature.script.state.ScriptRows.build(
            hidden, K.EPISODE_1_7, time, io.github.lenticualrstone.civilight.model.script.ScriptLayers(),
            io.github.lenticualrstone.civilight.model.script.ScriptPreview.WRITE, unlocked = false, readOnly = false, zone = Fx.zone,
        )
        val row = rows.filterIsInstance<io.github.lenticualrstone.civilight.feature.script.state.BlockRow>().single { it.blockKey == K.BLOCK_047 }
        assertFalse(row.speaker.toString(), row.speaker.toString().contains("경비병"))
        assertFalse(row.speaker.toString().contains("하르트"))
        val step = io.github.lenticualrstone.civilight.domain.script.ReadAloud.steps(hidden, K.EPISODE_1_7, gameTools = false, unlocked = false)
            .first { it.blockKey == K.BLOCK_047 } as io.github.lenticualrstone.civilight.domain.script.ReadAloud.Step.Line
        assertEquals("숨긴 인물", step.speaker)
        // Unlocked, the block display name shows: `경비병 (하르트)` in the row, `경비병` in 낭독.
        val open = io.github.lenticualrstone.civilight.domain.script.ReadAloud.steps(named, K.EPISODE_1_7, gameTools = false, unlocked = false)
            .first { it.blockKey == K.BLOCK_047 } as io.github.lenticualrstone.civilight.domain.script.ReadAloud.Step.Line
        assertEquals("경비병", open.speaker)
    }

    // --- T-U14-17: 다음 씬 ---

    private fun actx(): io.github.lenticualrstone.civilight.domain.actions.ActionContext {
        var n = 0
        return io.github.lenticualrstone.civilight.domain.actions.ActionContext(
            now = io.github.lenticualrstone.civilight.model.sample.ExampleClock.NOW, zone = Fx.zone, device = "test",
            newKey = { "01JZ000000NXSC" + (++n).toString().padStart(12, '0') },
        )
    }

    @Test
    fun `T-U14-17 다음 씬 follows the target scene with its place and shifts the later ordinals`() {
        val (out, key) = NextScene.create(doc, K.EPISODE_1_7, K.PROJECT, K.SCENE_1_7_3, actx())!!
        val labels = io.github.lenticualrstone.civilight.domain.script.Labels
        assertEquals(4, labels.sceneOrdinal(out, key))
        assertEquals(5, labels.sceneOrdinal(out, K.SCENE_1_7_4))
        assertEquals(3, labels.sceneOrdinal(out, K.SCENE_1_7_3))
        assertEquals(doc.index.unit(K.SCENE_1_7_3)!!.place, out.index.unit(key)!!.place)
        assertTrue(out.manuscript.liveBlocksOf(key).isEmpty())
        assertNull(out.index.unit(key)!!.title?.takeIf { it.isNotBlank() })
        // The rule SCR-74 would dim by (not applied until the shared bar can dim it).
        assertFalse(nextSceneEnabled(out, key))
        assertTrue(nextSceneEnabled(out, K.SCENE_1_7_3))
        assertTrue(nextSceneEnabled(out, null))
    }

    @Test
    fun `T-U14-17 in an empty episode 다음 씬 makes 씬 1 with no place`() {
        val scenes = doc.story.units.filter { it.parentKey == K.EPISODE_1_7 }.map { it.key }.toSet()
        val empty = doc.copy(story = doc.story.copy(units = doc.story.units.filterNot { it.key in scenes }))
        val (out, key) = NextScene.create(empty, K.EPISODE_1_7, K.PROJECT, null, actx())!!
        assertEquals(1, io.github.lenticualrstone.civilight.domain.script.Labels.sceneOrdinal(out, key))
        assertNull(out.index.unit(key)!!.place)
        assertEquals(K.EPISODE_1_7, out.index.unit(key)!!.parentKey)
    }
}

