package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import io.github.lenticualrstone.civilight.model.version.EpisodeVersion
import io.github.lenticualrstone.civilight.model.version.VersionBlock
import io.github.lenticualrstone.civilight.model.version.VersionOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/**
 * (0.2.0, SCR-55) The text of a link to a locked hidden item reads `숨긴 항목` on every 0.2.0 surface: 연출 text,
 * 낭독 steps, 찾기 / 바꾸기 and the version compare. Plus the counted replace and the remap of later hits.
 */
class HiddenLinks020Test {
    private val ctx = testCtx()
    private val secret = "비밀등불"
    private fun link(n: Int) = F1.lanternLink("01JZ000000LINK0000000000${n.toString().padStart(2, '0')}", secret)
    private fun hideTerm(doc: ProjectDoc): ProjectDoc =
        doc.copy(world = doc.world.copy(terms = doc.world.terms.map { if (it.key == K.TERM_ORIGINIUM_LANTERN) it.copy(hidden = true) else it }))

    /** b41 (연출 with cues), b42 (dialogue) and the F3 choice (b45) each hold a link to the lantern term. */
    private fun linked(): ProjectDoc = F3.build()
        .withBlock(F1.b(41)) { it.copy(runs = listOf(TextRun.Plain("▸ "), link(1), TextRun.Plain(" 등장"))) }
        .withBlock(F1.b(42)) { it.copy(runs = listOf(TextRun.Plain("저기 "), link(2), TextRun.Plain(" 보여"))) }
        .withBlock(F1.b(45)) { b -> b.copy(options = b.options.mapIndexed { i, o -> if (i == 0) o.copy(runs = listOf(TextRun.Plain("따라간다 "), link(3))) else o }) }

    @Test fun maskedDisplayTextSwapsOnlyLockedHiddenLinks() {
        val doc = linked()
        val runs = doc.block(F1.b(42)).runs
        assertEquals("저기 $secret 보여", Runs.displayText(doc, runs, unlocked = false))
        val hidden = hideTerm(doc)
        assertEquals("저기 숨긴 항목 보여", Runs.displayText(hidden, runs, unlocked = false))
        assertEquals("저기 $secret 보여", Runs.displayText(hidden, runs, unlocked = true))
        assertTrue(Runs.blockHidesLink(hidden, hidden.block(F1.b(42)), unlocked = false))
        assertFalse(Runs.blockHidesLink(hidden, hidden.block(F1.b(42)), unlocked = true))
        assertTrue(Runs.blockHidesLink(hidden, hidden.block(F1.b(45)), unlocked = false))
        // Exports keep the real text (export decides hidden content on its own).
        assertEquals("저기 $secret 보여", Runs.exportText(runs))
    }

    @Test fun directionTextMasksTheLink() {
        val hidden = hideTerm(linked())
        val b41 = hidden.block(F1.b(41))
        for (game in listOf(false, true)) {
            val text = ReadAloud.directionText(hidden, b41, game, unlocked = false)
            assertFalse(text, text.contains(secret))
            assertTrue(text, text.contains("숨긴 항목"))
        }
        assertTrue(ReadAloud.directionText(hidden, b41, false, unlocked = true).contains(secret))
    }

    @Test fun manuscriptRowTextMasksTheLink() {
        val hidden = hideTerm(linked())
        val rows = Manuscript.rows(hidden, F1.E17, StoryTimes.source(hidden), unlocked = false)
        val choice = rows.filterIsInstance<Manuscript.Row.BlockRow>().single { it.block.key == F1.b(45) }
        assertFalse(choice.text.contains(secret))
        assertTrue(choice.text.contains("숨긴 항목"))
    }

    @Test fun readAloudStepsMaskLinesCaptionsAndChoices() {
        val hidden = hideTerm(linked())
        val steps = ReadAloud.steps(hidden, F1.E17, gameTools = false, unlocked = false)
        val line = steps.filterIsInstance<ReadAloud.Step.Line>().single { it.blockKey == F1.b(42) }
        val caption = steps.filterIsInstance<ReadAloud.Step.Caption>().single { it.blockKey == F1.b(41) }
        val choices = steps.filterIsInstance<ReadAloud.Step.Choices>().single { it.blockKey == F1.b(45) }
        for (text in listOf(line.text, caption.text) + choices.lines) assertFalse(text, text.contains(secret))
        assertEquals("저기 숨긴 항목 보여", line.text)
        assertEquals("따라간다 숨긴 항목", choices.lines.first())
        val open = ReadAloud.steps(hidden, F1.E17, gameTools = false, unlocked = true)
        assertEquals("저기 $secret 보여", open.filterIsInstance<ReadAloud.Step.Line>().single { it.blockKey == F1.b(42) }.text)
    }

    @Test fun findNeverMatchesOrShowsALockedHiddenLink() {
        val hidden = hideTerm(linked())
        // Typing the hidden name finds nothing: it neither shows nor confirms the item.
        assertTrue(Replace.find(hidden, secret, unlocked = false).isEmpty())
        assertEquals(3, Replace.find(hidden, secret, unlocked = true).size)
        // A hit next to the link shows the sentence masked, with the box on the right place.
        val hit = Replace.find(hidden, "보여", unlocked = false).single { it.blockKey == F1.b(42) }
        assertEquals("저기 숨긴 항목 보여", hit.sentence)
        assertEquals("보여", hit.sentence.substring(hit.sentenceStart, hit.sentenceStart + 2))
        // A replace while locked never rewrites the hidden link, even with a hit found while unlocked.
        val unlockedHits = Replace.find(hidden, secret, unlocked = true)
        val r = Replace.applyCounted(hidden, unlockedHits, secret, "촛불", ctx, unlocked = false)
        assertEquals(0, r.count)
        assertEquals(hidden, r.doc)
    }

    @Test fun versionCompareMasksEveryRowKind() {
        val base = linked()
        val blocks = base.manuscript.liveScenes.filter { base.numberingUnitOfScene(it.key) == F1.E17 }
            .flatMap { base.manuscript.liveBlocksOf(it.key) }
            .map { b -> VersionBlock(b.key, b.sceneKey, b.number, b.kind, b.speakerKey, b.runs, BlockOps.orderedOptions(b).map { VersionOption(it.key, it.runs) }) }
        val v = EpisodeVersion(key = "01JZ000000VERS000000000077", name = "v", createdAt = 0L, blocks = blocks)
        var now = base.withBlock(F1.b(42)) { it.copy(runs = listOf(TextRun.Plain("여기 "), link(2), TextRun.Plain(" 보여"))) }
        now = BlockTrash.delete(now, listOf(F1.b(41)), ctx).doc
        now = ScriptWrite.send(now, BlockPosition.After(F1.b(43)), WriteDraft(io.github.lenticualrstone.civilight.model.script.BlockKind.NARRATION, listOf(link(9))), false, ctx)!!.doc
        now = hideTerm(now)
        val rows = VersionDiff.rows(v, now, F1.E17, unlocked = false)
        val texts = rows.flatMap { r ->
            when (r) {
                is VersionDiff.Row.Same -> listOf(r.text)
                is VersionDiff.Row.Changed -> r.segments.map { it.text }
                is VersionDiff.Row.Deleted -> listOf(r.text)
                is VersionDiff.Row.Added -> listOf(r.text)
                is VersionDiff.Row.HiddenScene -> emptyList()
            }
        }
        assertTrue(texts.none { it.contains(secret) })
        assertTrue(rows.any { it is VersionDiff.Row.Changed && it.blockKey == F1.b(42) })
        assertTrue(rows.any { it is VersionDiff.Row.Deleted && it.text.contains("숨긴 항목") })
        assertTrue(rows.any { it is VersionDiff.Row.Added && it.text == "숨긴 항목" })
        assertTrue(rows.any { it is VersionDiff.Row.Same && it.blockKey == F1.b(45) && it.text.contains("숨긴 항목") })
    }

    // --- SCR-75: counted replace and remapped hits ---

    @Test fun aSecondReplaceInTheSameRunHitsTheMovedPlace() {
        val doc = F1.build().withBlock(F1.b(42)) { it.copy(runs = listOf(TextRun.Plain("등불 하나 등불 둘"))) }
        val hits = Replace.find(doc, "등불").filter { it.blockKey == F1.b(42) }
        assertEquals(2, hits.size)
        val first = Replace.applyCounted(doc, listOf(hits[0]), "등불", "작은촛불", ctx)
        assertEquals(1, first.count)
        // The old offsets of the second hit are stale now: nothing would change, and the count says so.
        assertEquals(0, Replace.applyCounted(first.doc, listOf(hits[1]), "등불", "작은촛불", ctx).count)
        val moved = Replace.remap(hits, setOf(0), "등불", "작은촛불")
        assertEquals(hits.size, moved.size)
        assertEquals("작은촛불 하나 등불 둘", moved[1].sentence)
        assertEquals("등불", moved[1].sentence.substring(moved[1].sentenceStart, moved[1].sentenceStart + 2))
        assertEquals("작은촛불", moved[0].sentence.substring(moved[0].sentenceStart, moved[0].sentenceStart + (moved[0].end - moved[0].start)))
        assertTrue(Replace.matches(first.doc, moved[1], "등불"))
        assertFalse(Replace.matches(first.doc, hits[1], "등불"))
        // After 되돌리기 the kept list no longer agrees with the document.
        assertFalse(Replace.matches(doc, moved[1], "등불"))
        val second = Replace.applyCounted(first.doc, listOf(moved[1]), "등불", "작은촛불", ctx)
        assertEquals(1, second.count)
        assertEquals("작은촛불 하나 작은촛불 둘", second.doc.block(F1.b(42)).runs.plainText)
        // following (for the particle check) shows the replaced text after a hit.
        val three = F1.build().withBlock(F1.b(42)) { it.copy(runs = listOf(TextRun.Plain("등불등불은"))) }
        val h3 = Replace.find(three, "등불").filter { it.blockKey == F1.b(42) }
        val m3 = Replace.remap(h3, setOf(1), "등불", "초")
        assertEquals("초은", m3[0].following)
    }
}
