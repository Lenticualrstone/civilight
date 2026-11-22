package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.script.StoryTimes
import io.github.lenticualrstone.civilight.domain.script.WritingCounts
import io.github.lenticualrstone.civilight.domain.world.common.EditContext as WorldEditContext
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.feature.script.change.ChangeDraft
import io.github.lenticualrstone.civilight.feature.script.change.ChangeModel
import io.github.lenticualrstone.civilight.feature.script.links.BlockTap
import io.github.lenticualrstone.civilight.feature.script.links.LinkFixes
import io.github.lenticualrstone.civilight.feature.script.panel.PanelRecords
import io.github.lenticualrstone.civilight.feature.script.panel.SubjectDeleted
import io.github.lenticualrstone.civilight.feature.script.readonly.ReadOnlyFrame
import io.github.lenticualrstone.civilight.feature.script.state.BlockRow
import io.github.lenticualrstone.civilight.feature.script.state.HeadRow
import io.github.lenticualrstone.civilight.feature.script.state.ScriptOps
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRows
import io.github.lenticualrstone.civilight.feature.script.terms.TermMarksModel
import io.github.lenticualrstone.civilight.feature.script.ui.HeadLines
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.script.ScriptPreview
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import io.github.lenticualrstone.civilight.navigation.ExampleTour
import io.github.lenticualrstone.civilight.navigation.Library
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.TermCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** SCR-55 … SCR-60 on the example project (WLD fixture: l42 in b42 is a pre-reveal use; b12 holds the cut-off `오리지`). */
class TermsLinksAndModesTest {
    private val doc = Fx.example

    private fun worldCtx(): WorldEditContext {
        var n = 0
        return WorldEditContext(newKey = { "01JZ000000WRLD" + (++n).toString().padStart(12, '0') }, now = { ExampleClock.NOW }, device = "test")
    }

    private fun rows(d: ProjectDoc, unit: Key, candidates: Boolean, unlocked: Boolean = false): List<BlockRow> {
        val layers = ScriptLayers(termCandidates = candidates)
        val index = if (candidates) CandidateIndex.build(d) else CandidateIndex.pending()
        val terms = TermMarksModel.Source(d, index, unlocked, candidates)
        return ScriptRows.build(d, unit, StoryTimes.source(d), layers, ScriptPreview.WRITE, unlocked, readOnly = false, zone = Fx.zone, terms = terms)
            .filterIsInstance<BlockRow>()
    }

    private fun row(d: ProjectDoc, unit: Key, block: Key, candidates: Boolean = false) = rows(d, unit, candidates).first { it.blockKey == block }

    // --- SCR-56 ---

    @Test
    fun `T-11-02 a link before its term's first reveal gets the margin mark, the reveal block's does not`() {
        assertTrue(row(doc, K.EPISODE_1_4, K.BLOCK_B42).preReveal)
        assertFalse(row(doc, K.EPISODE_1_7, K.BLOCK_044).preReveal)
        // The mark does not depend on the 용어 후보 layer.
        assertTrue(row(doc, K.EPISODE_1_4, K.BLOCK_B42, candidates = true).preReveal)
    }

    @Test
    fun `T-21-01 with the layer on the cut-off candidate is underlined and its window reads as RTCandidate`() {
        val off = row(doc, K.EPISODE_1_2, K.BLOCK_B12)
        assertTrue(off.termCandidates.isEmpty())
        val on = row(doc, K.EPISODE_1_2, K.BLOCK_B12, candidates = true)
        val span = on.termCandidates.single()
        val text = on.runs.plainText
        assertEquals(text.indexOf("오리지"), span.start)
        assertEquals(span.start + 3, span.end)
        val source = TermMarksModel.Source(doc, CandidateIndex.build(doc), unlocked = false, candidatesOn = true)
        val window = source.popover(span.candidate)!!
        assertEquals("오리지늄 등불", window.title)
        assertEquals("후보 · 끊김", window.tag)
        assertEquals("첫 공개 1-7 · 잇기 시", window.revealLine)
        assertEquals("▲ 공개 전 사용", window.warning)
        assertEquals(listOf("잇기", "아님"), listOf(window.linkLabel, window.rejectLabel))
        // The tap resolver finds the candidate under the finger.
        assertEquals(BlockTap.Candidate(span.candidate), LinkFixes.tapAt(on, span.start + 1, inPrefix = false))
        assertNull(LinkFixes.tapAt(on, 0, inPrefix = false))
    }

    @Test
    fun `SCR-56 a paren run before the candidate shifts the drawn underline by its parentheses`() {
        val withParen = doc.copy(
            episodes = doc.episodes.mapValues { (_, f) ->
                f.copy(blocks = f.blocks.map { if (it.key == K.BLOCK_B12) it.copy(runs = listOf(TextRun.Paren("작게")) + it.runs) else it })
            },
        )
        val r = row(withParen, K.EPISODE_1_2, K.BLOCK_B12, candidates = true)
        val span = r.termCandidates.single()
        assertEquals(r.runs.plainText.indexOf("오리지"), span.start)
        val layout = io.github.lenticualrstone.civilight.feature.script.editor.RunLayout.of(r.runs, showParens = true)
        assertEquals(layout.text.indexOf("오리지"), layout.toDisplay(span.start))
        assertEquals(layout.text.indexOf("오리지") + 3, layout.toDisplay(span.end))
        // The tap resolver works in raw offsets: the character under the finger maps back.
        assertEquals(BlockTap.Candidate(span.candidate), LinkFixes.tapAt(r, layout.toRaw(layout.text.indexOf("오리지") + 1), inPrefix = false))
    }

    @Test
    fun `T-21-01 잇기 makes exactly that range a link with the margin mark, 아님 stores the dismissal`() {
        val candidate = row(doc, K.EPISODE_1_2, K.BLOCK_B12, candidates = true).termCandidates.single().candidate
        val linked = TermMarksModel.link(doc, candidate, worldCtx())!!
        val runs = linked.index.block(K.BLOCK_B12)!!.runs
        val link = runs.filterIsInstance<TextRun.Link>().single()
        assertEquals("오리지", link.text)
        assertEquals(doc.index.block(K.BLOCK_B12)!!.runs.plainText, runs.plainText)
        assertTrue(row(linked, K.EPISODE_1_2, K.BLOCK_B12).preReveal)

        val rejected = TermMarksModel.reject(doc, candidate, worldCtx())
        val rejection = rejected.index.term(K.TERM_ORIGINIUM_LANTERN)!!.rejections.single()
        assertEquals(K.BLOCK_B12, rejection.containerKey)
        assertTrue(row(rejected, K.EPISODE_1_2, K.BLOCK_B12, candidates = true).termCandidates.isEmpty())
    }

    @Test
    fun `T-21-02 memos are never scanned and WLD-22 releases a dismissal the edit made stale`() {
        val candidate = row(doc, K.EPISODE_1_2, K.BLOCK_B12, candidates = true).termCandidates.single().candidate
        val rejected = TermMarksModel.reject(doc, candidate, worldCtx())
        val edited = ScriptOps.setRuns(rejected, K.BLOCK_B12, listOf(TextRun.Plain("등불 속 그 돌, 뭐라고 했죠?")), Fx.ctx())
        val released = TermMarksModel.releaseStale(edited, K.BLOCK_B12, null, unlocked = false)
        assertTrue(released.index.term(K.TERM_ORIGINIUM_LANTERN)!!.rejections.isEmpty())
        // A memo block of 1-7 (#046) gets no candidate and no mark even with the layer on.
        val memo = row(doc, K.EPISODE_1_7, K.BLOCK_046, candidates = true)
        assertTrue(memo.termCandidates.isEmpty())
        assertFalse(memo.preReveal)
    }

    // --- SCR-55 ---

    @Test
    fun `SCR-55 a tapped live link opens its card and a typed at sign opens the picker`() {
        val r = row(doc, K.EPISODE_1_7, K.BLOCK_044)
        val at = r.runs.plainText.indexOf("오리지늄")
        val tap = LinkFixes.tapAt(r, at + 2, inPrefix = false) as BlockTap.Link
        assertEquals(TermCard(K.PROJECT, K.TERM_ORIGINIUM_LANTERN), LinkFixes.liveRoute(K.PROJECT, tap.link))
        assertNull(LinkFixes.tapAt(r, 0, inPrefix = false))
        assertTrue(LinkFixes.opensPicker("@", 4, 4))
        assertFalse(LinkFixes.opensPicker("@", 4, 6))
        assertFalse(LinkFixes.opensPicker("a", 4, 4))
    }

    @Test
    fun `SCR-55 a deleted target's card offers the four fixes, a purged one has no 되살리기`() {
        val linkKey = doc.index.block(K.BLOCK_044)!!.runs.filterIsInstance<TextRun.Link>().single().linkKey
        assertNull(LinkFixes.card(doc, K.BLOCK_044, linkKey, unlocked = false))
        val deleted = doc.copy(world = doc.world.copy(terms = doc.world.terms.map { if (it.key == K.TERM_ORIGINIUM_LANTERN) it.copy(deletedAt = ExampleClock.NOW) else it }))
        val card = LinkFixes.card(deleted, K.BLOCK_044, linkKey, unlocked = false)!!
        assertEquals("지운 용어 · 오리지늄 등불", card.title)
        assertEquals(listOf("다른 항목에 잇기", "글로 풀기", "되살리기", "이대로 두기"), card.buttons)
        assertTrue(card.canRestore)
        val purged = doc.copy(world = doc.world.copy(terms = doc.world.terms.filterNot { it.key == K.TERM_ORIGINIUM_LANTERN }))
        val chip = LinkFixes.card(purged, K.BLOCK_044, linkKey, unlocked = false)!!
        assertTrue(chip.title, chip.title.startsWith("영구 삭제 · "))
        assertFalse(chip.canRestore)
        assertFalse("되살리기" in chip.buttons)
        // 글로 풀기 keeps the text; 다른 항목에 잇기 keeps key and text.
        val plain = LinkFixes.unlink(deleted, K.BLOCK_044, linkKey, Fx.ctx())
        assertTrue(plain.index.block(K.BLOCK_044)!!.runs.none { it is TextRun.Link })
        assertEquals(doc.index.block(K.BLOCK_044)!!.runs.plainText, plain.index.block(K.BLOCK_044)!!.runs.plainText)
        val other = LinkFixes.relink(deleted, K.BLOCK_044, linkKey, LinkTargetKind.TERM, K.TERM_BLACKOUT, Fx.ctx())
        val moved = other.index.block(K.BLOCK_044)!!.runs.filterIsInstance<TextRun.Link>().single()
        assertEquals(linkKey, moved.linkKey)
        assertEquals(K.TERM_BLACKOUT, moved.target.key)
        // 이대로 두기 is one acknowledgement for this reference.
        val kept = LinkFixes.keep(deleted, K.TERM_ORIGINIUM_LANTERN, K.BLOCK_044, io.github.lenticualrstone.civilight.domain.actions.ActionContext(ExampleClock.NOW).trash)
        assertTrue(LinkFixes.isKept(kept, K.TERM_ORIGINIUM_LANTERN, K.BLOCK_044))
    }

    // --- SCR-57 ---

    @Test
    fun `SCR-57 a record about a deleted item reads 대상 지워짐 with its fixes`() {
        val record = PanelRecords.onBlock(doc, K.BLOCK_044).single(PanelRecords::isReveal)
        assertNull(PanelRecords.card(doc, record, K.BLOCK_044).deletedSubject)
        val deleted = doc.copy(world = doc.world.copy(terms = doc.world.terms.map { if (it.key == K.TERM_ORIGINIUM_LANTERN) it.copy(deletedAt = ExampleClock.NOW, trashEntryKey = "entry") else it }))
        val card = PanelRecords.card(deleted, record, K.BLOCK_044)
        assertEquals("대상 지워짐", card.line)
        val subject = card.deletedSubject!!
        assertEquals(LinkTargetKind.TERM, subject.kind)
        assertEquals(K.TERM_ORIGINIUM_LANTERN, subject.itemKey)
        assertTrue(subject.canRestore)
        assertFalse(subject.kept)
        val kept = SubjectDeleted.keep(deleted, subject.itemKey, record.key, io.github.lenticualrstone.civilight.domain.actions.ActionContext(ExampleClock.NOW).trash)
        assertTrue(PanelRecords.card(kept, record, K.BLOCK_044).deletedSubject!!.kept)
    }

    // --- SCR-58 ---

    @Test
    fun `T-19-01 rule 19 masks hidden speakers in the change sheet and 이 씬 분량, and tags an open hidden head`() {
        val hidden = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.HART) it.copy(hidden = true) else it }))
        val draft = ChangeDraft.start(hidden, K.BLOCK_043).copy(viewerKey = K.RIEN)
        assertEquals("숨긴 인물", ChangeModel.of(hidden, draft, unlocked = false)!!.relation!!.partner)
        assertEquals("하르트", ChangeModel.of(hidden, draft, unlocked = true)!!.relation!!.partner)
        val locked = WritingCounts.sceneSpeakers(hidden, K.SCENE_1_7_3, unlocked = false)
        assertFalse(locked, "하르트" in locked)
        assertTrue(locked, "숨긴 인물" in locked)
        assertEquals(WritingCounts.sceneSpeakers(hidden, K.SCENE_1_7_3), WritingCounts.sceneSpeakers(hidden, K.SCENE_1_7_3, unlocked = true))
        val head = HeadRow(K.SCENE_1_7_4, "씬 4", "전환 문구 · 결사의 문 앞 · 06:10 · 직접", hiddenTag = true, ordinal = 4)
        assertEquals("전환 문구 · 결사의 문 앞 · 06:10 · 직접 · 숨김", HeadLines.subtitle(head))
        assertEquals("숨김", HeadLines.subtitle(head.copy(transitionLine = null)))
        assertNull(HeadLines.subtitle(head.copy(transitionLine = null, hiddenTag = false)))
    }

    // --- SCR-59 ---

    @Test
    fun `T-23-01 a recorder memo's prefix opens its record`() {
        val source = io.github.lenticualrstone.civilight.model.text.RecorderSource(K.RECORDER_R1, ExampleClock.NOW)
        val withMemo = doc.copy(
            episodes = doc.episodes.mapValues { (_, f) -> f.copy(blocks = f.blocks.map { if (it.key == K.BLOCK_046) it.copy(recorderSource = source) else it }) },
        )
        val memo = row(withMemo, K.EPISODE_1_7, K.BLOCK_046)
        assertEquals(K.RECORDER_R1, memo.recorderRecordKey)
        assertTrue(memo.memoPrefix.orEmpty(), memo.memoPrefix.orEmpty().startsWith("⌁ 작가 메모 · 레코더 "))
        assertEquals(BlockTap.Source(K.RECORDER_R1), LinkFixes.tapAt(memo, null, inPrefix = true))
        assertNull(LinkFixes.tapAt(row(doc, K.EPISODE_1_7, K.BLOCK_046), null, inPrefix = true))
    }

    // --- SCR-60 ---

    @Test
    fun `SCR-60 예시 홈으로 returns to the tour under the script, else opens it`() {
        val stack = listOf(Library, ExampleTour(K.PROJECT), Script(K.PROJECT))
        assertEquals(ReadOnlyFrame.Home.PopToTour, ReadOnlyFrame.home(stack, K.PROJECT))
        assertEquals(ReadOnlyFrame.Home.PushTour(ExampleTour(K.PROJECT)), ReadOnlyFrame.home(listOf(Library, Script(K.PROJECT)), K.PROJECT))
        assertEquals("library.onboarding.finish", ReadOnlyFrame.FINISH_REQUEST)
        assertNotNull(ScriptOps.newSceneSheet(doc, K.PROJECT, K.BLOCK_044, K.SCENE_1_7_3, K.EPISODE_1_7))
    }
}
