package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.RecorderSource
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import io.github.lenticualrstone.civilight.model.trash.NameTag
import io.github.lenticualrstone.civilight.model.trash.NameTagKind
import io.github.lenticualrstone.civilight.model.trash.NameTagState
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/**
 * 10-script.md §4.5 · rules 18–23 as seen in the script. Only the script-visible half is tested
 * here: restore and purge of a character, candidate matching, `아님` storage, the `@` picker search
 * and biometric confirmation belong to other areas (see the status file).
 */
class Rules18To23Test {
    private val ctx = testCtx()

    /** 노아's live dialogue lines (1-6 씬 2 and 씬 4). */
    private fun noahLines(doc: ProjectDoc) =
        doc.manuscript.liveBlocks.filter { it.kind.known == BlockKind.DIALOGUE && it.speakerKey == K.NOAH }

    /** 노아 back from the trash (the example starts with him deleted, fixture.md #22). */
    private fun ProjectDoc.noahLive(hidden: Boolean = false): ProjectDoc = copy(
        cast = cast.copy(characters = cast.characters.map { if (it.key == K.NOAH) it.copy(deletedAt = null, trashEntryKey = null, hidden = hidden) else it }),
        trash = trash.copy(entries = trash.entries.filterNot { it.key == K.TRASH_NOAH }),
    )

    /** 노아 permanently deleted: his row is gone, his name tag stays. */
    private fun ProjectDoc.noahPurged(): ProjectDoc = copy(
        cast = cast.copy(characters = cast.characters.filterNot { it.key == K.NOAH }),
        tombstones = tombstones.copy(
            nameTags = tombstones.nameTags + NameTag(
                key = K.NOAH, kind = NameTagKind.CHARACTER.open(), name = "노아", voiceCode = "NOA",
                state = NameTagState.PURGED.open(), purgedAt = ExampleClock.NOW,
            ),
        ),
        trash = trash.copy(entries = trash.entries.filterNot { it.key == K.TRASH_NOAH }),
    )

    // --- Rule 20 ---

    @Test
    fun `T-20-01 deleted speaker stays`() {
        val doc = F1.build()
        val lines = noahLines(doc)
        assertEquals(14, lines.size)
        assertEquals(14, WritingCounts.dialogueLines(doc, K.NOAH))
        for (b in lines) assertEquals(Manuscript.SpeakerLabel.Deleted("노아", purged = false), Manuscript.speakerLabel(doc, b))
        assertTrue(ScriptChecks.speakerMissing(doc).none { it.subjectKeys.single() in lines.map { b -> b.key } })
        val field = Panels.deletedSpeakerField(doc, lines.first())!!
        assertEquals("지운 인물 · 노아", field.text)
        assertEquals(listOf("화자 바꾸기", "되살리기", "이대로 두기"), field.buttons)
        for (b in lines) assertTrue(VoiceIds.currentId(doc, b)!!.startsWith("NOA_0106_"))
        assertEquals("NOA_0106_007", VoiceIds.currentId(doc, doc.block(K.BLOCK_1_6_007)))
    }

    @Test
    fun `T-20-02 picker row for a deleted speaker`() {
        val doc = F1.build()
        val sheet = SpeakerPicker.sheet(doc, noahLines(doc).first(), recentSpeakers = listOf(K.NOAH))
        assertEquals("지운 인물 · 노아 · 되살리기", sheet.deletedSpeakerRow)
        assertTrue((sheet.inScene + sheet.recent + sheet.all).none { it.characterKey == K.NOAH })
        assertNull(SpeakerPicker.sheet(doc, doc.block(F1.b(42)), emptyList()).deletedSpeakerRow)
    }

    @Test
    fun `T-20-03 restore keeps user changes`() {
        var doc = F1.build()
        val changed = noahLines(doc).take(3).map { it.key }
        for (k in changed) doc = BlockOps.changeSpeaker(doc, k, K.OWEN, ctx)
        val restored = doc.noahLive()
        val labels = (noahLines(F1.build()).map { it.key }).associateWith { Manuscript.speakerLabel(restored, restored.block(it)) }
        assertEquals(11, labels.values.count { it == Manuscript.SpeakerLabel.Named("노아", null) })
        assertEquals(changed.toSet(), labels.filterValues { it == Manuscript.SpeakerLabel.Named("오웬", null) }.keys)
    }

    @Test
    fun `T-20-04 permanent delete`() {
        val doc = F1.build().noahPurged()
        val lines = noahLines(doc)
        assertEquals(14, lines.size)
        for (b in lines) {
            val label = Manuscript.speakerLabel(doc, b) as Manuscript.SpeakerLabel.Deleted
            assertEquals("영구 삭제 · 노아", label.text)
        }
        assertEquals(listOf("화자 바꾸기", "이대로 두기"), Panels.deletedSpeakerField(doc, lines.first())!!.buttons)
        val times = StoryTimes.source(doc)
        val rows = DialogueCsvExport.rows(doc, ExportScope.csv(listOf(K.EPISODE_1_6)), times)
        assertTrue(rows.filter { r -> lines.any { it.key == r.id } && r.state != DialogueCsvExport.DISCARDED }.all { it.speaker == "노아" })
        val json = GameJsonExport.build(doc, ExportScope.json(listOf(K.EPISODE_1_6)), times)
        val noah = json["characters"]!!.jsonArray.map { it.jsonObject }.single { it["id"]!!.jsonPrimitive.content == K.NOAH }
        assertEquals("노아", noah["name"]!!.jsonPrimitive.content)
        assertEquals("NOA_0106_007", VoiceIds.currentId(doc, doc.block(K.BLOCK_1_6_007)))
        assertTrue(VoiceIds.isFixed(doc.block(K.BLOCK_1_6_007).voice))
    }

    // --- Rule 19 ---

    @Test
    fun `T-19-01 hidden speaker and plain text`() {
        val doc = ExampleProject.build().noahLive(hidden = true)
            .updateUnit(K.SCENE_1_6_5) { it.copy(deletedAt = null, trashEntryKey = null) }
        val rows = Manuscript.rows(doc, K.EPISODE_1_6, StoryTimes.source(doc)).filterIsInstance<Manuscript.Row.BlockRow>()
        val scene5 = rows.filter { it.block.sceneKey == K.SCENE_1_6_5 }
        val noahRows = scene5.filter { it.block.speakerKey == K.NOAH }
        assertTrue(noahRows.isNotEmpty())
        for (r in noahRows) {
            assertEquals(Manuscript.SpeakerLabel.Hidden, r.speaker)
            assertEquals(Runs.displayText(r.block.runs), r.text)
        }
        val laugh = scene5.single { it.block.kind.known == BlockKind.NARRATION && it.block.runs.plainText.contains("노아가 웃었다") }
        assertEquals(Runs.displayText(laugh.block.runs), laugh.text)
        assertEquals("숨긴 인물", Manuscript.SpeakerLabel.Hidden.TEXT)
        val unlocked = Manuscript.speakerLabel(doc, noahRows.first().block, unlocked = true)
        assertEquals(Manuscript.SpeakerLabel.Named("노아", null), unlocked)
    }

    private fun hiddenS4With12Blocks(): ProjectDoc {
        var doc = F1.build()
        repeat(8) { i ->
            doc = BlockOps.insert(doc, BlockPosition.SceneEnd(F1.S4), BlockKind.NARRATION, ctx, runs = listOf(TextRun.Plain("줄 ${i + 1}"))).doc
        }
        return doc.updateUnit(F1.S4) { it.copy(hidden = true) }
    }

    @Test
    fun `T-19-02 hidden scene row`() {
        val doc = hiddenS4With12Blocks()
        val times = StoryTimes.source(doc)
        val rows = Manuscript.rows(doc, F1.E17, times)
        assertTrue(rows.contains(Manuscript.Row.HiddenScene(F1.S4, "숨긴 씬 · 블록 12 · 지문으로 보기")))
        assertTrue(rows.none { it is Manuscript.Row.Head && it.sceneKey == F1.S4 })
        assertTrue(rows.none { it is Manuscript.Row.BlockRow && it.block.sceneKey == F1.S4 })
        val open = Manuscript.rows(doc, F1.E17, times, unlocked = true)
        assertTrue((open.single { it is Manuscript.Row.Head && it.sceneKey == F1.S4 } as Manuscript.Row.Head).hiddenTag)
    }

    @Test
    fun `T-19-03 picker hides hidden characters`() {
        val doc = F1.build().noahLive(hidden = true)
        val sheet = SpeakerPicker.sheet(doc, doc.block(F1.b(42)), recentSpeakers = listOf(K.NOAH))
        assertTrue((sheet.inScene + sheet.recent + sheet.all).none { it.characterKey == K.NOAH })
        assertEquals("숨긴 인물 1 · 지문으로 보기", sheet.hiddenRow)
        val open = SpeakerPicker.sheet(doc, doc.block(F1.b(42)), recentSpeakers = listOf(K.NOAH), unlocked = true)
        assertTrue(open.recent.any { it.characterKey == K.NOAH })
        assertNull(open.hiddenRow)
    }

    @Test
    fun `T-19-04 export of hidden content`() {
        val doc = hiddenS4With12Blocks()
        val json = ExportScope.json(listOf(K.CHAPTER_1))
        assertEquals("숨긴 씬 1 · 1-7 씬 4", json.hiddenRow(doc))
        assertEquals("넣기", json.hiddenChoice)
        assertEquals("빼기", ExportScope.pdf(listOf(K.CHAPTER_1)).hiddenChoice)
        val built = GameJsonExport.build(doc, json, StoryTimes.source(doc))
        assertTrue(built["scenes"]!!.jsonArray.any { it.jsonObject["id"]!!.jsonPrimitive.content == F1.S4 })
        val left = GameJsonExport.build(doc, ExportScope.pdf(listOf(K.CHAPTER_1)), StoryTimes.source(doc))
        assertTrue(left["scenes"]!!.jsonArray.none { it.jsonObject["id"]!!.jsonPrimitive.content == F1.S4 })

        val hiddenNoah = F1.build().noahLive(hidden = true)
        val out = GameJsonExport.build(hiddenNoah, ExportScope.json(listOf(K.EPISODE_1_6)), StoryTimes.source(hiddenNoah))
        val noah = out["characters"]!!.jsonArray.map { it.jsonObject }.single { it["id"]!!.jsonPrimitive.content == K.NOAH }
        assertEquals("노아", noah["name"]!!.jsonPrimitive.content)
        val noahBlocks = out["scenes"]!!.jsonArray.flatMap { it.jsonObject["blocks"]!!.jsonArray }.map { it.jsonObject }
            .filter { it["speaker"]?.jsonPrimitive?.content == K.NOAH }
        assertEquals(14, noahBlocks.size)
    }

    // --- Rule 21 ---

    @Test
    fun `T-21-01 candidate popup`() {
        val doc = ExampleProject.build()
        val block = doc.block(K.BLOCK_B12)
        assertEquals("등불 속 그 돌, 오리지… 뭐라고 했죠?", block.runs.plainText)
        assertTrue(TermCandidates.showsUnderlines(ScriptLayers(termCandidates = true), block))
        val popover = TermCandidates.popover(doc, K.BLOCK_B12, K.TERM_ORIGINIUM_LANTERN, cutOff = true)!!
        assertEquals("오리지늄 등불", popover.term)
        assertEquals("후보 · 끊김", popover.tag)
        assertEquals("첫 공개 1-7 · 잇기 시" to "▲ 공개 전 사용", popover.revealLine)
        assertEquals(listOf("잇기", "아님"), popover.buttons)

        val start = block.runs.plainText.indexOf("오리지")
        val linked = TermCandidates.link(doc, K.BLOCK_B12, start, start + 3, K.TERM_ORIGINIUM_LANTERN, ctx)!!
        val runs = linked.block(K.BLOCK_B12).runs
        assertEquals(block.runs.plainText, runs.plainText)
        assertTrue(runs.contains(TextRun.Link(runs.filterIsInstance<TextRun.Link>().single().linkKey, LinkTarget(LinkTargetKind.TERM, K.TERM_ORIGINIUM_LANTERN), "오리지")))
        assertEquals("오리지…", runs.dropWhile { it !is TextRun.Link }.plainText.take(4))
        assertEquals(1, Panels.preRevealLinks(linked, linked.block(K.BLOCK_B12)).size)
        assertTrue(Panels.preRevealLinks(doc, block).isEmpty())
    }

    @Test
    fun `T-21-02 layer default and non-output blocks`() {
        val doc = F1.build()
        assertFalse(TermCandidates.showsUnderlines(ScriptLayers(), doc.block(K.BLOCK_B12)))
        assertNull(TermCandidates.layerChip(ScriptLayers()))
        assertEquals("표시 · 용어 후보" to "켬", TermCandidates.layerChip(ScriptLayers(termCandidates = true)))
        val memo = BlockOps.insert(
            doc, BlockPosition.After(K.BLOCK_B12), BlockKind.MEMO, ctx,
            runs = listOf(F1.lanternLink("01JZ000000HREF000000000099", "오리지늄 등불"), TextRun.Plain(" 확인")),
        )
        val block = memo.doc.block(memo.blockKey)
        assertFalse(TermCandidates.showsUnderlines(ScriptLayers(termCandidates = true), block))
        assertTrue(Panels.preRevealLinks(memo.doc, block).isEmpty())
    }

    @Test
    fun `T-21-03 alias through the link picker`() {
        assertEquals("오리지늄 · 오리지늄 등불의 별칭", TermCandidates.aliasRow("오리지늄", "오리지늄 등불"))
        val link = TextRun.Link("01JZ000000HREF000000000098", LinkTarget(LinkTargetKind.TERM, K.TERM_ORIGINIUM_LANTERN), "오리지늄")
        val runs = Runs.insertRun(listOf(TextRun.Plain("그건 ")), 3, link)
        val typed = Runs.insertText(runs, Runs.length(runs), "은")
        assertEquals(listOf(TextRun.Plain("그건 "), link, TextRun.Plain("은")), typed)
    }

    // --- Rule 23 ---

    @Test
    fun `T-23-01 recorder memo insertion`() {
        val doc = F1.build()
        val recordedAt = ExampleClock.at("2026-11-21T23:41:00Z")
        val r = RecorderMemo.insert(
            doc, BlockPosition.After(F1.b(44)), listOf(TextRun.Plain("등불이 한 번 더 깜박이는 장면")),
            RecorderSource(K.RECORDER_R1, recordedAt), ctx,
        )
        val block = r.doc.block(r.blockKey)
        assertEquals(58, block.number)
        assertEquals(BlockKind.MEMO, block.kind.known)
        val numbers = r.doc.liveNumbers(F1.S3)
        assertEquals(numbers.indexOf(44) + 1, numbers.indexOf(58))
        assertEquals("⌁ 작가 메모 · 레코더 11월 22일 08:41", Manuscript.memoPrefix(block, ctx.zone))
        assertEquals("1-7 씬 3 · #058 작가 메모 넣음", r.toast)
        assertEquals(listOf("되돌리기", "가 보기"), r.actions)
        assertEquals("⌁ 작가 메모", Manuscript.memoPrefix(doc.block(F1.b(46)), ctx.zone))
    }

    @Test
    fun `T-23-02 last writing position`() {
        val doc = F1.build()
        assertEquals("마지막으로 쓰던 곳 · 1-7 씬 3 · #044 뒤", RecorderMemo.lastWritingPlace(doc))
        assertEquals(BlockPosition.After(F1.b(44)), RecorderMemo.lastWritingPosition(doc))
    }

    // --- Rule 18 ---

    @Test
    fun `T-18-01 bands in the head`() {
        assertEquals(Band.DAWN, Bands.bandOf(ClockTime.of(5, 40)))
        assertEquals(Band.MORNING, Bands.bandOf(ClockTime.of(6, 10)))
        assertEquals(Band.NIGHT, Bands.bandOf(ClockTime.of(1, 30)))
        val created = SceneCreate.createAfter(
            F1.build(), F1.S4,
            SceneCreate.NewScene(place = F1.build().unit(F1.S4).place, withEmptyBlock = false), ctx,
        )!!
        val doc = created.doc.updateUnit(created.sceneKey) {
            it.copy(time = SceneTime.Relative(baseKey = F1.S4, dayMin = 0, dayMax = 0, clock = ClockTime.of(1, 30)))
        }
        val head = SceneText.headText(doc, created.sceneKey, StoryTimes.source(doc).timeOf(created.sceneKey))
        assertEquals("씬 5 · 결사의 문 · 2일째 밤 01:30", head)
    }
}
