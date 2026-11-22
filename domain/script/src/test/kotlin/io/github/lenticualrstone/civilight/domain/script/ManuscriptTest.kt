package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.script.ScriptPreview
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Display layers, preview modes, speaker labels, marks and address hints (SCR-20, 26–29, 42). */
class ManuscriptTest {
    private val ctx = testCtx()
    private fun b(n: Int) = F1.b(n)
    private val doc = F1.build()
    private val times = StoryTimes.source(doc)

    private fun blockRows(layers: ScriptLayers = ScriptLayers(), preview: ScriptPreview = ScriptPreview.WRITE) =
        Manuscript.rows(doc, F1.E17, times, layers, preview).filterIsInstance<Manuscript.Row.BlockRow>()

    private fun row(n: Int, layers: ScriptLayers = ScriptLayers()) = blockRows(layers).single { it.block.key == b(n) }

    @Test
    fun `layers hide kinds but never remove content`() {
        val all = blockRows().map { it.block.key }
        assertTrue(b(41) in all && b(46) in all)
        assertFalse(b(41) in blockRows(ScriptLayers(direction = false)).map { it.block.key })
        assertFalse(b(46) in blockRows(ScriptLayers(memo = false)).map { it.block.key })
        val withSystem = BlockOps.insert(doc, BlockPosition.After(b(44)), BlockKind.SYSTEM, ctx, runs = listOf(TextRun.Plain("30분 뒤")))
        val hidden = Manuscript.rows(withSystem.doc, F1.E17, times, ScriptLayers(system = false))
        assertTrue(hidden.none { it is Manuscript.Row.BlockRow && it.block.key == withSystem.blockKey })
        assertEquals(doc, F1.build())
        assertEquals(ScriptLayers(system = true), Manuscript.layersAfterInsert(ScriptLayers(system = false), BlockKind.SYSTEM))
    }

    @Test
    fun `paren layer and drawn text`() {
        assertEquals("(작게) 저기… 불빛이 보여요. 누가 아직 남아 있는 걸까요?", row(42).text)
        assertEquals("저기… 불빛이 보여요. 누가 아직 남아 있는 걸까요?", row(42, ScriptLayers(paren = false)).text)
        assertEquals("▸ 배경 하층 거리(새벽) ♪ 잔불", row(41).text)
        assertEquals("▸ 배경 하층 거리(새벽) · ♪ 잔불", Manuscript.cueText(doc, doc.block(b(41)), " · "))
        assertEquals("화면 효과 fade 효과음 바람", Manuscript.cueText(doc, doc.block(K.block(1, 7, 1))))
    }

    @Test
    fun `dialogue-only preview keeps heads and dialogue`() {
        val rows = Manuscript.rows(doc, F1.E17, times, preview = ScriptPreview.DIALOGUE_ONLY)
        assertTrue(rows.filterIsInstance<Manuscript.Row.BlockRow>().all { it.block.kind.known == BlockKind.DIALOGUE })
        assertTrue(rows.filterIsInstance<Manuscript.Row.Head>().all { it.transitionLine == null })
        assertEquals(5, rows.filterIsInstance<Manuscript.Row.Head>().size)
    }

    @Test
    fun `marks follow the marks layer`() {
        assertEquals(listOf("◆"), row(43).marks.map { it.glyph })
        assertEquals(listOf("◇"), row(44).marks.map { it.glyph })
        assertTrue(row(43, ScriptLayers(marks = false)).marks.isEmpty())
        assertFalse(row(43).marks.single().dotted)
    }

    @Test
    fun `speaker labels with display names and missing speakers`() {
        assertEquals(Manuscript.SpeakerLabel.Named("하르트", "???"), row(43).speaker)
        assertEquals("??? 하르트", (row(43).speaker as Manuscript.SpeakerLabel.Named).text)
        assertEquals("??? 하르트 · #043", Manuscript.speakerLineWithNumber(doc, doc.block(b(43))))
        assertEquals(Manuscript.SpeakerLabel.Missing, Manuscript.speakerLabel(doc, doc.block(K.BLOCK_1_7_027)))
        assertNull(Manuscript.speakerLabel(doc, doc.block(b(44))))
        val empty = BlockOps.insert(doc, BlockPosition.After(b(48)), BlockKind.DIALOGUE, ctx, inheritSpeaker = false)
        assertNull(Manuscript.speakerLabel(empty.doc, empty.doc.block(empty.blockKey)))
    }

    @Test
    fun `speaker missing check covers non-empty dialogue only`() {
        val item = ScriptChecks.speakerMissing(doc).single()
        assertEquals("▲ 화자 없음", item.title)
        assertEquals(listOf(K.BLOCK_1_7_027), item.subjectKeys)
        assertEquals(listOf("화자 고르기", "나레이션으로 바꾸기"), item.buttons)
        assertEquals("화자", item.filter)
        assertEquals("1-7 씬 2", item.details.last())
        val fixed = BlockOps.changeKind(doc, K.BLOCK_1_7_027, BlockKind.NARRATION, ctx)
        assertTrue(ScriptChecks.speakerMissing(fixed).isEmpty())
    }

    @Test
    fun `address hints from the speaker's side at the block`() {
        val raw = ExampleProject.build()
        assertEquals(listOf("리엔을 ‘꼬마’"), Manuscript.addressHints(raw, raw.block(K.BLOCK_043)))
        assertEquals(listOf("세라를 ‘세라 씨’"), Manuscript.addressHints(raw, raw.block(K.BLOCK_042)))
        assertTrue(Manuscript.addressHints(doc, doc.block(b(43))).isEmpty())
        assertEquals(listOf("리엔을 ‘꼬마’"), Manuscript.addressHints(doc, doc.block(b(47))))
        assertTrue(row(47).addressHints.isEmpty())
        assertEquals(listOf("리엔을 ‘꼬마’"), row(47, ScriptLayers(addressHint = true)).addressHints)
    }

    @Test
    fun `point lines, orphan card and selection bar`() {
        val deleted = BlockTrash.delete(doc, listOf(b(43)), ctx).doc
        val card = Panels.orphanCard(deleted, deleted.record(F1.R1))!!
        assertEquals("근거 #043 지워짐 · #044로 옮겨 옴", card.first)
        assertEquals("#043 · 휴지통 · 30일 · 되살리면 다시 이어짐", card.second)
        assertEquals(listOf("다른 블록에 잇기", "이대로 두기", "기록 지우기"), Panels.ORPHAN_BUTTONS)
        val kept = Anchors.keepHere(deleted, F1.R1, ctx)
        assertTrue(Records.marksOn(kept, b(44)).single { it.recordKey == F1.R1 }.dotted)
        assertTrue(ScriptChecks.anchorDeleted(kept).isEmpty())
        val restored = (BlockTrash.restore(kept, b(43), ctx) as BlockTrash.RestoreResult.Restored).doc
        assertEquals(io.github.lenticualrstone.civilight.model.change.Anchor.AtBlock(b(44)), restored.record(F1.R1).anchor)

        val bar = SpeakerPicker.selectionBar(doc, listOf(b(42), b(43), b(44)))
        assertEquals("고른 블록 3개", bar.count)
        assertEquals("#042 ~ #044 · ◆ 2 · ◇ 1 같이 감", bar.range)
        assertEquals(listOf("옮기기", "지우기", "잘라내기", "복사"), bar.buttons)
        assertEquals("연출 #041", SpeakerPicker.selectLabel(doc, doc.block(b(41))))
        assertEquals("고른 블록 · #043", Panels.selectedBlockTitle(doc.block(b(43))))
    }

    @Test
    fun `record sheet rows and reveal re-seating`() {
        assertEquals("#043 · 1-7 씬 3", Records.sheetSubline(doc, b(43)))
        assertEquals("시작 시점 · 이 블록 · 1-7 #043", Records.startRow(doc, b(43)))
        assertEquals("첫 공개 1-7 #044 → 1-7 #047", Records.revealMovePreview(doc, K.TERM_ORIGINIUM_LANTERN, b(47)))
        val created = Records.createAtBlock(
            doc, b(47), io.github.lenticualrstone.civilight.model.change.ChangeTarget.TermReveal(K.TERM_ORIGINIUM_LANTERN), null, ctx,
        )!!
        assertEquals(F1.REVEAL, created.recordKey)
        assertEquals(1, created.doc.changes.records.count { (it.target as? io.github.lenticualrstone.civilight.model.change.ChangeTarget.TermReveal)?.termKey == K.TERM_ORIGINIUM_LANTERN })
        assertEquals("◆ 관계 · 리엔 → 하르트", Records.shortTitle(doc, doc.record(F1.R1)))
        assertEquals("리엔의 마음 → 하르트", Records.subject(doc, doc.record(F1.R1), possessive = true))
    }
}
