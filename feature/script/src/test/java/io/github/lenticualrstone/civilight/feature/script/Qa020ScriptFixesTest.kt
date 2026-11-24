package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.editor.RunLayout
import io.github.lenticualrstone.civilight.feature.script.editor.SpanKind
import io.github.lenticualrstone.civilight.feature.script.input.Draft
import io.github.lenticualrstone.civilight.feature.script.input.DraftLooks
import io.github.lenticualrstone.civilight.feature.script.input.DraftMark
import io.github.lenticualrstone.civilight.feature.script.input.Drafts
import io.github.lenticualrstone.civilight.feature.script.panel.PropsModel
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.marks.Foreshadow
import io.github.lenticualrstone.civilight.model.marks.MarkAnchor
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.script.ScriptPreview
import io.github.lenticualrstone.civilight.model.script.ScriptViewState
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** (0.2.0 QA) Script lane fixes: hidden text in the draft, the shared draft, layers on write, 속성 marks. */
class Qa020ScriptFixesTest {
    private val doc: ProjectDoc get() = Fx.example
    private val secret = "비밀등불"

    private fun typed(d: Draft, text: String): Draft = Drafts.textChanged(d, text, text.length, text.length).first

    private fun link() = TextRun.Link("01JZ000000LINK000000000077", LinkTarget(LinkTargetKind.TERM, K.TERM_ORIGINIUM_LANTERN), secret)

    private fun hideTerm(d: ProjectDoc): ProjectDoc =
        d.copy(world = d.world.copy(terms = d.world.terms.map { if (it.key == K.TERM_ORIGINIUM_LANTERN) it.copy(hidden = true) else it }))

    private fun hideHart(d: ProjectDoc): ProjectDoc =
        d.copy(cast = d.cast.copy(characters = d.cast.characters.map { if (it.key == K.HART) it.copy(hidden = true) else it }))

    private fun hideScene(d: ProjectDoc, scene: String): ProjectDoc =
        d.copy(story = d.story.copy(units = d.story.units.map { if (it.key == scene) it.copy(hidden = true) else it }))

    // --- Hidden text in the draft (rule 19, T-U14-25) ---

    @Test
    fun `a plain or 이 뒤에 draft drops a hidden link on lock and keeps it while unlocked`() {
        val plain = Drafts.insertLink(typed(Draft(), "안녕 "), link())
        val hidden = hideTerm(doc)
        assertEquals(plain, Drafts.reconcile(plain, hidden, K.EPISODE_1_7, unlocked = true))
        val locked = Drafts.reconcile(plain, hidden, K.EPISODE_1_7, unlocked = false)
        assertFalse(locked.text.contains(secret))
        assertTrue(locked.runs.none { it is TextRun.Link })
        assertEquals("안녕 ", locked.text)
        assertTrue(locked.selEnd <= locked.text.length)
        // With an 이 뒤에 mark: the mark stays, the link goes.
        val after = Drafts.insertAfter(plain, K.BLOCK_047)
        val lockedAfter = Drafts.reconcile(after, hidden, K.EPISODE_1_7, unlocked = false)
        assertEquals(DraftMark.After(K.BLOCK_047), lockedAfter.mark)
        assertFalse(lockedAfter.text.contains(secret))
    }

    @Test
    fun `the set-aside draft loses a hidden link and a hidden speaker, so ending 고치기 brings neither back`() {
        val aside = Drafts.insertLink(typed(Draft(speakerKey = K.HART), "준비 "), link())
        val editing = Drafts.startEdit(aside, doc.index.block(K.BLOCK_043)!!)
        val hidden = hideHart(hideTerm(doc))
        val locked = Drafts.reconcile(editing, hidden, K.EPISODE_1_7, unlocked = false)
        val back = Drafts.close(locked)
        assertFalse(back.text.contains(secret))
        assertNull(back.speakerKey)
        assertEquals(DraftMark.None, back.mark)
    }

    @Test
    fun `the field draws a locked hidden link as its mask`() {
        val runs = listOf(TextRun.Plain("안녕 "), link())
        val hidden = hideTerm(doc)
        val layout = RunLayout.of(runs, linkLook = { DraftLooks.of(hidden, it, unlocked = false) })
        assertFalse(layout.text.contains(secret))
        assertTrue(layout.text.contains("숨긴 항목"))
        assertTrue(layout.spans.any { it.kind == SpanKind.HIDDEN_LINK })
        assertTrue(RunLayout.of(runs, linkLook = { DraftLooks.of(hidden, it, unlocked = true) }).text.contains(secret))
    }

    @Test
    fun `no hidden text is kept for process death, even when written while unlocked`() {
        val hidden = hideHart(hideTerm(doc))
        val d = Drafts.insertLink(typed(Draft(speakerKey = K.HART), "안녕 "), link())
        val stored = persistedDraft(d, hidden)
        assertNotNull(stored)
        assertFalse(stored!!.contains(secret))
        val back = Drafts.decode(stored)!!
        assertEquals("안녕 ", back.text)
        assertNull(back.speakerKey)
        // A 고치기 of a block in a hidden scene keeps the set-aside draft only.
        val editing = typed(Drafts.startEdit(typed(Draft(), "준비"), doc.index.block(K.BLOCK_047)!!), "비밀 줄")
        val s = persistedDraft(editing, hideScene(doc, K.SCENE_1_7_3))!!
        assertFalse(s.contains("비밀 줄"))
        assertEquals("준비", Drafts.decode(s)!!.text)
        // A visible draft round-trips unchanged.
        val visible = typed(Draft(), "그냥 줄")
        assertEquals(visible, Drafts.decode(persistedDraft(visible, doc)))
    }

    // --- One draft per project, shared by its script screens ---

    @Test
    fun `script screens of one project share one draft flow, and only the one in front checks its episode`() {
        DraftMemory.clear()
        val p = "01JZ000000PROJ00000000QA01"
        assertSame(DraftMemory.flow(p), DraftMemory.flow(p))
        assertNull(DraftMemory.get(p))
        DraftMemory.put(p, typed(Draft(), "준비"))
        assertEquals("준비", DraftMemory.flow(p).value.text)
        val lower = Any()
        val upper = Any()
        assertFalse(DraftMemory.ownedByOther(p, lower))
        DraftMemory.claim(p, upper)
        assertTrue(DraftMemory.ownedByOther(p, lower))
        assertFalse(DraftMemory.ownedByOther(p, upper))
        DraftMemory.claim(p, lower)
        assertTrue(DraftMemory.ownedByOther(p, upper))
        // A screen behind passes no episode: an 이 뒤에 mark set in front on another episode's block stays.
        val marked = Drafts.insertAfter(typed(Draft(), "x"), K.BLOCK_047)
        assertEquals(marked, Drafts.reconcile(marked, doc, null, unlocked = false))
        DraftMemory.clear()
    }

    // --- SCR-26: a written block of a kind whose layer is off turns that layer on ---

    @Test
    fun `writing a kind whose layer is off turns the layer on and the 쓰기 preview`() {
        val off = ScriptViewState(layers = ScriptLayers(direction = false, memo = false, system = false))
        assertTrue(revealedView(off, BlockKind.MEMO)!!.layers.memo)
        assertTrue(revealedView(off, BlockKind.SYSTEM)!!.layers.system)
        assertTrue(revealedView(off, BlockKind.DIRECTION)!!.layers.direction)
        for (k in listOf(BlockKind.DIALOGUE, BlockKind.NARRATION, BlockKind.CHOICE)) assertNull(revealedView(off, k))
        val on = ScriptViewState()
        for (k in BlockKind.entries) assertNull(revealedView(on, k))
        // 대사만: anything but dialogue goes back to 쓰기.
        val dialogueOnly = ScriptViewState(preview = ScriptPreview.DIALOGUE_ONLY)
        assertNull(revealedView(dialogueOnly, BlockKind.DIALOGUE))
        assertEquals(ScriptPreview.WRITE, revealedView(dialogueOnly, BlockKind.NARRATION)!!.preview)
    }

    // --- 속성: foreshadow names behind the gate ---

    @Test
    fun `속성 never names a foreshadowing planted in a locked hidden scene`() {
        val scenes = doc.manuscript.liveScenes.map { it.key }.filter { doc.manuscript.liveBlocksOf(it).isNotEmpty() }
        val plantScene = scenes[1]
        val plantBlock = doc.manuscript.liveBlocksOf(plantScene).first().key
        val shown = doc.manuscript.liveBlocksOf(scenes[0]).first().key
        val f = Foreshadow(key = "01JZ000000FRSH0000000000QA", name = "왕의 반지", plant = MarkAnchor(plantBlock), mentions = listOf(MarkAnchor(shown)))
        val marked = doc.copy(story = doc.story.copy(foreshadows = doc.story.foreshadows + f))
        assertTrue(PropsModel.of(marked, shown, readOnly = false, unlocked = false).marks.any { it.contains("왕의 반지") })
        val hidden = hideScene(marked, plantScene)
        assertTrue(PropsModel.of(hidden, shown, readOnly = false, unlocked = false).marks.none { it.contains("왕의 반지") })
        assertTrue(PropsModel.of(hidden, shown, readOnly = false, unlocked = true).marks.any { it.contains("왕의 반지") })
    }
}
