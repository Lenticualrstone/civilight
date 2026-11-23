package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.doc.recordsHere
import io.github.lenticualrstone.civilight.model.marks.Foreshadow
import io.github.lenticualrstone.civilight.model.marks.MarkAnchor
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ChoiceOption
import io.github.lenticualrstone.civilight.model.script.DiscardReason
import io.github.lenticualrstone.civilight.model.script.ScriptAsset
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import io.github.lenticualrstone.civilight.model.version.EpisodeVersion
import io.github.lenticualrstone.civilight.model.version.VersionBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** U-14 write paths and the 0.2.0 script features (SCR §4.7). */
class Write020Test {
    private val ctx = testCtx()
    private fun plain(s: String) = listOf<TextRun>(TextRun.Plain(s))
    private fun lines(vararg l: String): List<TextRun> = l.toList().flatMapIndexed { i, s -> if (i == 0) plain(s) else listOf(TextRun.Break) + plain(s) }
    private fun dialogue(text: String, speaker: String? = K.RIEN) = WriteDraft(BlockKind.DIALOGUE, plain(text), speaker)

    // --- T-U14-01 / T-U14-04: send places ---

    @Test fun sendGoesToSceneEndAndTakesNextNumber() {
        val doc = F1.build()
        val s = ScriptWrite.send(doc, BlockPosition.SceneEnd(F1.S3), dialogue("알아요."), false, ctx)!!
        val block = s.doc.block(s.blockKey)
        assertEquals(58, block.number)
        assertEquals(K.RIEN, block.speakerKey)
        assertEquals(s.blockKey, s.doc.manuscript.liveBlocksOf(F1.S3).last().key)
        val s2 = ScriptWrite.send(s.doc, BlockPosition.SceneEnd(F1.S3), dialogue("둘"), false, ctx)!!
        assertEquals(59, s2.number)
        assertEquals(s2.blockKey, s2.doc.manuscript.liveBlocksOf(F1.S3).last().key)
    }

    @Test fun sendAfterAMarkGoesThere() {
        val doc = F1.build()
        val s = ScriptWrite.send(doc, BlockPosition.After(F1.b(44)), dialogue("저기."), false, ctx)!!
        val keys = s.doc.manuscript.liveBlocksOf(F1.S3).map { it.key }
        assertEquals(keys.indexOf(F1.b(44)) + 1, keys.indexOf(s.blockKey))
    }

    @Test fun sendNeverInheritsASpeaker() {
        val s = ScriptWrite.send(F1.build(), BlockPosition.SceneEnd(F1.S3), dialogue("누구야?", speaker = null), false, ctx)!!
        assertNull(s.doc.block(s.blockKey).speakerKey)
    }

    // --- T-U14-03 ---

    @Test fun canSendRules() {
        fun d(kind: BlockKind, runs: List<TextRun>, cues: Int = 0) =
            WriteDraft(kind, runs, cues = if (cues > 0) F1.build().block(F1.b(41)).cues.take(cues) else null)
        assertFalse(DraftRules.hasContent(d(BlockKind.DIALOGUE, plain("  ")), false))
        assertFalse(DraftRules.hasContent(d(BlockKind.DIALOGUE, listOf(TextRun.Break, TextRun.Break)), false))
        assertTrue(DraftRules.hasContent(d(BlockKind.DIALOGUE, listOf(TextRun.Paren("작게"))), false))
        assertFalse(DraftRules.hasContent(d(BlockKind.CHOICE, plain("따라간다")), false))
        assertTrue(DraftRules.hasContent(d(BlockKind.CHOICE, lines("따라간다", "남는다")), false))
        assertTrue(DraftRules.hasContent(d(BlockKind.CHOICE, lines("따라간다", "", "남는다")), false))
        assertTrue(DraftRules.hasContent(d(BlockKind.DIRECTION, emptyList(), cues = 1), true))
        assertFalse(DraftRules.hasContent(d(BlockKind.DIRECTION, emptyList(), cues = 1), false))
    }

    @Test fun choiceSendDropsBlankLines() {
        val s = ScriptWrite.send(F1.build(), BlockPosition.SceneEnd(F1.S3), WriteDraft(BlockKind.CHOICE, lines("따라간다", " ", "남는다")), false, ctx)!!
        val b = s.doc.block(s.blockKey)
        assertEquals(listOf("따라간다", "남는다"), BlockOps.orderedOptions(b).map { it.runs.plainText })
        assertNull(b.speakerKey)
    }

    @Test fun enterLineBreakSendsOneBlockWithBreaks() {
        val s = ScriptWrite.send(F1.build(), BlockPosition.SceneEnd(F1.S3), WriteDraft(BlockKind.NARRATION, lines("하나", "둘")), false, ctx)!!
        assertEquals(listOf(TextRun.Plain("하나"), TextRun.Break, TextRun.Plain("둘")), s.doc.block(s.blockKey).runs)
    }

    // --- T-U14-07 / T-U14-08: 고치기 ---

    @Test fun editKeepsKeyNumberAndRecords() {
        val doc = F1.build()
        val before = doc.block(F1.b(43))
        val records = doc.recordsHere(F1.b(43)).map { it.key }
        val out = ScriptWrite.edit(doc, F1.b(43), WriteDraft(BlockKind.DIALOGUE, plain("멈춰. 저건 등불이 아니야."), K.RIEN), ctx)
        val after = out.block(F1.b(43))
        assertEquals(before.number, after.number)
        assertEquals(K.RIEN, after.speakerKey)
        assertEquals("멈춰. 저건 등불이 아니야.", after.runs.plainText)
        assertEquals(records, out.recordsHere(F1.b(43)).map { it.key })
    }

    @Test fun unchangedEditReturnsTheSameDoc() {
        val doc = F1.build()
        val b = doc.block(F1.b(47))
        assertSame(doc, ScriptWrite.edit(doc, b.key, WriteDraft(BlockKind.DIALOGUE, b.runs, b.speakerKey), ctx))
    }

    @Test fun editResultKeepsRefusedApartFromUnchanged() {
        val doc = F1.build()
        val b = doc.block(F1.b(47))
        assertEquals(ScriptWrite.EditResult.Unchanged, ScriptWrite.editResult(doc, b.key, WriteDraft(BlockKind.DIALOGUE, b.runs, b.speakerKey), ctx))
        // A blank 고치기 is never applied (SCR-70), and a cue-holding 연출 never leaves 연출 (SCR-13).
        assertEquals(ScriptWrite.EditResult.Refused, ScriptWrite.editResult(doc, b.key, WriteDraft(BlockKind.DIALOGUE, plain("  ")), ctx))
        assertEquals(ScriptWrite.EditResult.Refused, ScriptWrite.editResult(doc, F1.b(41), WriteDraft(BlockKind.NARRATION, plain("x")), ctx))
        assertEquals(ScriptWrite.EditResult.Refused, ScriptWrite.editResult(doc, "nope", dialogue("x"), ctx))
        assertTrue(ScriptWrite.editResult(doc, b.key, dialogue("새 글"), ctx) is ScriptWrite.EditResult.Changed)
    }

    @Test fun editKeepsLinkKeys() {
        val link = F1.lanternLink("01JZ000000LINK000000000077")
        val doc = F1.build().withBlock(F1.b(47)) { it.copy(runs = listOf(TextRun.Plain("저 "), link, TextRun.Plain(" 봐"))) }
        val out = ScriptWrite.edit(doc, F1.b(47), WriteDraft(BlockKind.DIALOGUE, listOf(TextRun.Plain("이 "), link, TextRun.Plain(" 봐")), K.HART), ctx)
        assertTrue(out.block(F1.b(47)).runs.any { it is TextRun.Link && it.linkKey == link.linkKey })
    }

    // --- T-U14-09 / T-U14-10 / T-U14-26: 나누기 ---

    @Test fun splitMakesANewBlockAfter() {
        val doc = F1.build()
        val s = ScriptWrite.split(doc, F1.b(47), WriteDraft(BlockKind.DIALOGUE, plain("뒤돌아보지 말고"), K.HART), plain("따라와."), ctx)!!
        assertEquals(47, s.frontNumber)
        assertEquals(58, s.backNumber)
        val back = s.doc.block(s.backKey)
        assertEquals(K.HART, back.speakerKey)
        assertEquals("따라와.", back.runs.plainText)
        val keys = s.doc.manuscript.liveBlocksOf(F1.S3).map { it.key }
        assertEquals(keys.indexOf(F1.b(47)) + 1, keys.indexOf(s.backKey))
        assertEquals("#047 나눔 · 뒤 글 → #058", ScriptWrite.splitToast(s.frontNumber, s.backNumber))
    }

    @Test fun splitNeedsTextOnBothSidesAndNoChoice() {
        val doc = F1.build()
        assertNull(ScriptWrite.split(doc, F1.b(47), WriteDraft(BlockKind.DIALOGUE, plain(""), K.HART), plain("따라와."), ctx))
        assertNull(ScriptWrite.split(F3.build(), F1.b(45), WriteDraft(BlockKind.CHOICE, lines("a", "b")), plain("c"), ctx))
    }

    @Test fun directionTextIsKeptOnEveryPath() {
        val doc = F1.build()
        val s = ScriptWrite.send(doc, BlockPosition.SceneEnd(F1.S3), WriteDraft(BlockKind.DIRECTION, plain("배경 하층 거리, 잔불 BGM")), false, ctx)!!
        assertEquals(plain("배경 하층 거리, 잔불 BGM"), s.doc.block(s.blockKey).runs)
        val narr = KindChange.apply(s.doc, s.blockKey, BlockKind.NARRATION, ctx)
        val back = KindChange.apply(narr, s.blockKey, BlockKind.DIRECTION, ctx)
        assertEquals(plain("배경 하층 거리, 잔불 BGM"), back.block(s.blockKey).runs)
        val sp = ScriptWrite.split(back, s.blockKey, WriteDraft(BlockKind.DIRECTION, plain("배경 하층 거리,")), plain("잔불 BGM"), ctx)!!
        assertEquals("배경 하층 거리,", sp.doc.block(sp.frontKey).runs.plainText)
        assertEquals("잔불 BGM", sp.doc.block(sp.backKey).runs.plainText)
        val m = ScriptWrite.merge(sp.doc, sp.backKey, WriteDraft(BlockKind.DIRECTION, plain("잔불 BGM")), ctx)!!
        assertEquals("배경 하층 거리, 잔불 BGM", m.doc.block(m.intoKey).runs.plainText)
    }

    // --- T-U14-11: MergeCheck ---

    @Test fun mergeCheckReasons() {
        val doc = F1.build()
        val first = doc.manuscript.liveBlocksOf(F1.S3).first().key
        assertEquals(MergeCheck.Reason.SCENE_START, MergeCheck.reason(doc, first, BlockKind.DIALOGUE, K.SERA))
        val prev47 = MergeCheck.previous(doc, F1.b(47))!!
        if (prev47.kind.known != BlockKind.DIALOGUE) {
            assertEquals(MergeCheck.Reason.KIND, MergeCheck.reason(doc, F1.b(47), BlockKind.DIALOGUE, K.HART))
        }
        val prev43 = MergeCheck.previous(doc, F1.b(43))!!
        assertEquals(BlockKind.DIALOGUE, prev43.kind.known)
        val other = if (prev43.speakerKey == K.HART) K.RIEN else K.HART
        assertEquals(MergeCheck.Reason.SPEAKER, MergeCheck.reason(doc, F1.b(43), BlockKind.DIALOGUE, other))
        assertNull(MergeCheck.reason(doc, F1.b(43), BlockKind.DIALOGUE, prev43.speakerKey))
    }

    @Test fun mergeMovesRecordsAndToasts() {
        val doc = F1.build()
        val prev = MergeCheck.previous(doc, F1.b(43))!!
        val moved = doc.recordsHere(F1.b(43)).size
        val m = ScriptWrite.merge(doc, F1.b(43), WriteDraft(BlockKind.DIALOGUE, doc.block(F1.b(43)).runs, prev.speakerKey), ctx)!!
        assertFalse(m.doc.manuscript.isLiveBlock(F1.b(43)))
        assertEquals(prev.key, m.intoKey)
        assertTrue(m.doc.recordsHere(prev.key).size >= moved)
        assertTrue(m.toast.startsWith("#043을 ${Labels.number(prev.number)}에 합침"))
    }

    // --- T-U14-12: hidden cues survive editing ---

    @Test fun hiddenCuesSurviveEditing() {
        val doc = F1.build()
        val cues = doc.block(F1.b(41)).cues
        assertTrue(cues.isNotEmpty())
        val out = ScriptWrite.edit(doc, F1.b(41), WriteDraft(BlockKind.DIRECTION, plain("새벽 거리"), cues = null), ctx)
        assertEquals(cues, out.block(F1.b(41)).cues)
        assertEquals("새벽 거리", out.block(F1.b(41)).runs.plainText)
        assertEquals(KindChange.Blocked.HAS_CUES, KindChange.check(out.block(F1.b(41)), BlockKind.NARRATION))
        // 고치기 cannot move it out of 연출 either.
        assertSame(out, ScriptWrite.edit(out, F1.b(41), WriteDraft(BlockKind.NARRATION, plain("x")), ctx))
        assertEquals("새벽 거리", ReadAloud.directionText(out, out.block(F1.b(41)), gameTools = false))
        assertTrue(ReadAloud.directionText(out, out.block(F1.b(41)), gameTools = true).endsWith("  새벽 거리"))
    }

    // --- T-U14-13: choice lines ---

    @Test fun choiceLinesMapping() {
        val doc = F3.build()
        val out = ScriptWrite.edit(doc, F1.b(45), WriteDraft(BlockKind.CHOICE, lines("따라간다", "남는다", "돌아간다")), ctx)
        val opts = BlockOps.orderedOptions(out.block(F1.b(45)))
        assertEquals(listOf("따라간다", "남는다", "돌아간다"), opts.map { it.runs.plainText })
        assertEquals(F3.OPTION_1, opts[0].key)
        assertEquals(K.HART, opts[0].speakerKey)
        assertEquals(F3.OPTION_2, opts[1].key)
        val two = ScriptWrite.edit(out, F1.b(45), WriteDraft(BlockKind.CHOICE, lines("따라간다", "남는다")), ctx)
        assertEquals(2, two.block(F1.b(45)).options.size)
    }

    @Test fun removedOptionDiscardsItsFixedId() {
        val doc = F3.build().withBlock(F1.b(45)) { b ->
            b.copy(options = b.options + ChoiceOption("01JZ000000OPTN000000000003", "z", plain("세번째"), voice = fixed("X_3")))
        }
        val out = ScriptWrite.edit(doc, F1.b(45), WriteDraft(BlockKind.CHOICE, lines("남는다", "돌아간다")), ctx)
        val opts = BlockOps.orderedOptions(out.block(F1.b(45)))
        assertEquals(F3.OPTION_1, opts[0].key)
        assertEquals("남는다", opts[0].runs.plainText)
        val discarded = out.episodes.values.flatMap { it.discardedVoiceIds }
        assertTrue(discarded.any { it.id == "X_3" && it.reason.known == DiscardReason.DELETED && it.optionKey == "01JZ000000OPTN000000000003" })
    }

    // --- T-U14-14: KindChange ---

    @Test fun kindChangeRules() {
        val doc = F1.build()
        assertNull(KindChange.check(doc.block(F1.b(43)), BlockKind.NARRATION))
        assertEquals(KindChange.Blocked.CHOICE_LINES, KindChange.check(doc.block(F1.b(44)), BlockKind.CHOICE))
        val two = doc.withBlock(F1.b(44)) { it.copy(runs = lines("가", "나")) }
        val c = KindChange.apply(two, F1.b(44), BlockKind.CHOICE, ctx)
        assertEquals(listOf("가", "나"), BlockOps.orderedOptions(c.block(F1.b(44))).map { it.runs.plainText })
        val f3 = F3.build()
        val d = KindChange.apply(f3, F1.b(45), BlockKind.DIALOGUE, ctx)
        assertEquals(lines("따라간다", "불빛 쪽을 한 번 더 본다"), d.block(F1.b(45)).runs)
        assertNull(d.block(F1.b(45)).speakerKey)
    }

    @Test fun displayNameOverrideSurvivesKindChanges() {
        val doc = F1.build().withBlock(F1.b(47)) { it.copy(displayNameOverride = "경비병") }
        val n = KindChange.apply(doc, F1.b(47), BlockKind.NARRATION, ctx)
        val d = KindChange.apply(n, F1.b(47), BlockKind.DIALOGUE, ctx)
        assertEquals("경비병", d.block(F1.b(47)).displayNameOverride)
    }

    // --- T-U14-16 / T-U14-19: particles ---

    @Test fun objectParticleAfterNumber() {
        mapOf(58 to "을", 39 to "를", 44 to "를", 43 to "을", 45 to "를", 40 to "을", 1000 to "을", 52 to "를").forEach { (n, p) ->
            assertEquals("$n", p, Particles.objectAfterNumber(n))
        }
        assertEquals("#058을 #045에 합침", ScriptWrite.mergedToast(58, 45))
        assertEquals("#039를 #038에 합침", ScriptWrite.mergedToast(39, 38))
    }

    @Test fun particleCheck() {
        assertTrue(Particles.needsCheck("하르트", "리엔", "가 말했다"))
        assertTrue(Particles.needsCheck("하르트", "리엔", "는"))
        assertFalse(Particles.needsCheck("하르트", "리엔", "의"))
        assertTrue(Particles.needsCheck("서울", "부산", "로"))
        assertFalse(Particles.needsCheck("서울", "대전", "을"))
        assertFalse(Particles.needsCheck("R2", "리엔", "가"))
    }

    // --- T-U14-19: replace ---

    @Test fun replaceOnlyCheckedHitsAndKeepsLinks() {
        val link = F1.lanternLink("01JZ000000LINK000000000078", "등불")
        val doc = F1.build()
            .withBlock(F1.b(42)) { it.copy(runs = plain("등불 하나 등불 둘")) }
            .withBlock(F1.b(47)) { it.copy(runs = listOf(TextRun.Plain("저 "), link, TextRun.Plain(" 등불"))) }
        val hits = Replace.find(doc, "등불").filter { it.blockKey == F1.b(42) || it.blockKey == F1.b(47) }
        assertEquals(4, hits.size)
        val picked = listOf(hits[0], hits[2])
        val out = Replace.apply(doc, picked, "등불", "촛불", ctx)
        assertEquals("촛불 하나 등불 둘", out.block(F1.b(42)).runs.plainText)
        val l = out.block(F1.b(47)).runs.filterIsInstance<TextRun.Link>().single()
        assertEquals("촛불", l.text)
        assertEquals(link.linkKey, l.linkKey)
        assertEquals(link.target, l.target)
        assertTrue(out.block(F1.b(47)).runs.plainText.endsWith(" 등불"))
    }

    @Test fun replaceSkipsLockedHiddenScenes() {
        val doc = F1.build()
        val s4 = doc.unit(F1.S4)
        val hidden = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == s4.key) it.copy(hidden = true) else it }))
        assertTrue(Replace.find(hidden, "들어").none { hidden.block(it.blockKey).sceneKey == F1.S4 })
        assertTrue(Replace.find(hidden, "들어", unlocked = true).any { hidden.block(it.blockKey).sceneKey == F1.S4 })
    }

    // --- T-U14-18: 낭독 ---

    @Test fun readAloudSkipsMemosAndCaptionsDirection() {
        val doc = F1.build()
        val steps = ReadAloud.steps(doc, F1.E17, gameTools = false)
        val s3 = steps.filter { doc.block(it.blockKey).sceneKey == F1.S3 }
        assertTrue(s3.none { doc.block(it.blockKey).kind.known == BlockKind.MEMO })
        val caption = s3.filterIsInstance<ReadAloud.Step.Caption>().first { it.blockKey == F1.b(41) }
        assertEquals("▸ " + ReadAloud.cueSummary(doc, doc.block(F1.b(41))), caption.text)
        assertTrue(steps.all { it.background == null })
        val named = doc.withBlock(F1.b(47)) { it.copy(displayNameOverride = "경비병") }
        val line = ReadAloud.steps(named, F1.E17, false).filterIsInstance<ReadAloud.Step.Line>().first { it.blockKey == F1.b(47) }
        assertEquals("경비병", line.speaker)
        val f3 = ReadAloud.steps(F3.build(), F1.E17, false)
        val choice = f3.filterIsInstance<ReadAloud.Step.Choices>().single()
        assertEquals(listOf("따라간다", "불빛 쪽을 한 번 더 본다"), choice.lines)
    }

    @Test fun readAloudBackgroundFollowsTheCue() {
        val doc = F1.build()
        val bgAsset = doc.block(F1.b(41)).cues.first().assetKey!!
        val withImage = doc.copy(assets = doc.assets.copy(scriptAssets = doc.assets.scriptAssets.map { a: ScriptAsset -> if (a.key == bgAsset) a.copy(fileHash = "sha256-x") else a }))
        val steps = ReadAloud.steps(withImage, F1.E17, gameTools = true)
        val after = steps.dropWhile { it.blockKey != F1.b(41) }
        assertTrue(after.all { it.background != null })
        assertTrue(ReadAloud.steps(withImage, F1.E17, gameTools = false).all { it.background == null })
    }

    @Test fun readAloudLeavesOutLockedScenes() {
        val doc = F1.build()
        val hidden = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == F1.S4) it.copy(hidden = true) else it }))
        assertTrue(ReadAloud.steps(hidden, F1.E17, false).none { hidden.block(it.blockKey).sceneKey == F1.S4 })
    }

    // --- T-U14-20 / T-U14-25: version diff ---

    private fun versionOf(doc: io.github.lenticualrstone.civilight.model.doc.ProjectDoc): EpisodeVersion {
        val blocks = doc.manuscript.liveScenes.filter { doc.numberingUnitOfScene(it.key) == F1.E17 }
            .flatMap { doc.manuscript.liveBlocksOf(it.key) }
            .map { VersionBlock(it.key, it.sceneKey, it.number, it.kind, it.speakerKey, it.runs) }
        return EpisodeVersion(key = "01JZ000000VERS000000000001", name = "v", createdAt = 0L, blocks = blocks)
    }

    @Test fun versionDiffRows() {
        val doc = F1.build()
        val v = versionOf(doc)
        val old43 = doc.block(F1.b(43)).runs.plainText
        var now = doc.withBlock(F1.b(43)) { it.copy(runs = plain("멈춰. 저건 등불이 아니야.")) }
        now = BlockTrash.delete(now, listOf(F1.b(46)), ctx).doc
        now = ScriptWrite.send(now, BlockPosition.After(F1.b(48)), dialogue("새 줄"), false, ctx)!!.doc
        val rows = VersionDiff.rows(v, now, F1.E17)
        val changed = rows.filterIsInstance<VersionDiff.Row.Changed>().single()
        assertEquals(F1.b(43), changed.blockKey)
        assertEquals("멈춰. 저건 등불이 아니야.", changed.segments.filter { it.kind != VersionDiff.SegKind.DELETED }.joinToString("") { it.text })
        assertEquals(old43, changed.segments.filter { it.kind != VersionDiff.SegKind.INSERTED }.joinToString("") { it.text })
        val deleted = rows.indexOfFirst { it is VersionDiff.Row.Deleted && it.blockKey == F1.b(46) }
        assertTrue(deleted > 0)
        val before = rows[deleted - 1]
        assertTrue(before is VersionDiff.Row.Same && before.blockKey == F1.b(45) || before is VersionDiff.Row.Changed)
        assertEquals(1, rows.count { it is VersionDiff.Row.Added })
    }

    @Test fun versionDiffHidesLockedScenes() {
        val doc = F1.build()
        val v = versionOf(doc)
        var now = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == F1.S4) it.copy(hidden = true) else it }))
        now = BlockTrash.delete(now, listOf(F1.b(50)), ctx).doc
        val rows = VersionDiff.rows(v, now, F1.E17)
        val hidden = rows.filterIsInstance<VersionDiff.Row.HiddenScene>().single()
        assertEquals(F1.S4, hidden.sceneKey)
        assertEquals(4, hidden.count)
        assertTrue(rows.none { r -> (r as? VersionDiff.Row.Deleted)?.blockKey == F1.b(50) })
    }

    // --- marks follow a merge ---

    @Test fun mergeMovesForeshadowMarks() {
        val doc = F1.build()
        val f = Foreshadow(key = "01JZ000000FRSH000000000001", name = "등불", plant = MarkAnchor(F1.b(43)))
        val withMark = doc.copy(story = doc.story.copy(foreshadows = doc.story.foreshadows + f))
        val prev = MergeCheck.previous(withMark, F1.b(43))!!
        val m = ScriptWrite.merge(withMark, F1.b(43), WriteDraft(BlockKind.DIALOGUE, plain("x"), prev.speakerKey), ctx)
        assertNotNull(m)
        assertEquals(prev.key, m!!.doc.story.foreshadows.single().plant.blockKey)
    }
}
