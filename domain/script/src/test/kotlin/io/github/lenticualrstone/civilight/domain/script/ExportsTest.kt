package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.StagePosition
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.text.TextRun
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Engine JSON (SCR-63, envelope C-27) and dialogue CSV (SCR-64) as pure serialisers. */
class ExportsTest {
    private val ctx = testCtx()
    private fun b(n: Int) = F1.b(n)

    private fun blocksOf(json: JsonObject): Map<String, JsonObject> =
        json["scenes"]!!.jsonArray.flatMap { it.jsonObject["blocks"]!!.jsonArray }.map { it.jsonObject }
            .associateBy { it["id"]!!.jsonPrimitive.content }

    @Test
    fun `envelope, block kinds and characters`() {
        val doc = F1.build()
        val json = GameJsonExport.build(doc, ExportScope.json(listOf(F1.E17)), StoryTimes.source(doc))
        assertEquals("civilight-engine", json["format"]!!.jsonPrimitive.content)
        assertEquals(1, json["version"]!!.jsonPrimitive.int)
        val blocks = blocksOf(json)
        val hart = blocks.getValue(b(43))
        assertEquals("dialogue", hart["type"]!!.jsonPrimitive.content)
        assertEquals(K.HART, hart["speaker"]!!.jsonPrimitive.content)
        assertEquals("???", hart["displayName"]!!.jsonPrimitive.content)
        assertEquals("멈추지 마. 저건 등불이 아니야.", hart["text"]!!.jsonPrimitive.content)
        assertEquals("HRT_0107_043", hart["voice"]!!.jsonPrimitive.content)
        assertNull(hart["stage"])
        assertEquals("(작게) 저기… 불빛이 보여요. 누가 아직 남아 있는 걸까요?", blocks.getValue(b(42))["text"]!!.jsonPrimitive.content)
        val memo = blocks.getValue(b(46))
        assertEquals("memo", memo["type"]!!.jsonPrimitive.content)
        assertFalse(memo["export"]!!.jsonPrimitive.boolean)
        val cues = blocks.getValue(b(41))["cues"]!!.jsonArray.map { it.jsonObject }
        assertEquals(listOf("background", "bgm.play"), cues.map { it["op"]!!.jsonPrimitive.content })
        assertEquals(listOf(K.ASSET_BG_LOWER_STREET_DAWN, K.ASSET_BGM_EMBERS), cues.map { it["asset"]!!.jsonPrimitive.content })
        val fade = blocksOf(json).getValue(K.block(1, 7, 1))["cues"]!!.jsonArray.map { it.jsonObject["op"]!!.jsonPrimitive.content }
        assertEquals(listOf("fade", "sfx"), fade)
        assertEquals("narration", blocks.getValue(b(44))["type"]!!.jsonPrimitive.content)
        assertEquals("바람이 잿더미를 쓸고 지나갔다. 꺼진 줄 알았던 오리지늄 등불이 한 번 깜박였다.", blocks.getValue(b(44))["text"]!!.jsonPrimitive.content)
        val characters = json["characters"]!!.jsonArray.map { it.jsonObject }
        assertEquals("???", characters.single { it["id"]!!.jsonPrimitive.content == K.HART }["displayName"]!!.jsonPrimitive.content)
        assertTrue(characters.none { it["id"]!!.jsonPrimitive.content == K.ELIO })
        val assets = json["assets"]!!.jsonArray.map { it.jsonObject["id"]!!.jsonPrimitive.content }.toSet()
        assertTrue(K.ASSET_BGM_EMBERS in assets && K.ASSET_BGM_UPPER_BELL !in assets)
        assertEquals(listOf(F1.S1, F1.S2, F1.S3, F1.S4, F1.S5), json["scenes"]!!.jsonArray.map { it.jsonObject["id"]!!.jsonPrimitive.content })
    }

    @Test
    fun `choice options, rejoin and no option voice (N-23)`() {
        val doc = F3.build()
        val json = GameJsonExport.build(doc, ExportScope.json(listOf(F1.S3)), StoryTimes.source(doc))
        val choice = blocksOf(json).getValue(b(45))
        assertEquals("choice", choice["type"]!!.jsonPrimitive.content)
        val options = choice["options"]!!.jsonArray.map { it.jsonObject }
        assertEquals(listOf("따라간다", "불빛 쪽을 한 번 더 본다"), options.map { it["text"]!!.jsonPrimitive.content })
        // (0.2.0, N-23) Choice lines are text only: a stored 0.1.0 option speaker writes no voice ID.
        assertNull(options[0]["voice"])
        assertNull(options[1]["voice"])
        assertEquals(b(46), choice["rejoin"]!!.jsonPrimitive.content)
        val deleted = BlockTrash.delete(doc, listOf(b(46)), ctx).doc
        assertEquals(b(47), GameJsonExport.rejoinOf(deleted, deleted.block(b(45))))
    }

    @Test
    fun `stage only when enabled and voices only when on`() {
        val base = F1.build().withBlock(b(43)) { it.copy(stage = StagePosition.LEFT.open()) }
        val hidden = GameJsonExport.build(base, ExportScope.json(listOf(F1.S3)), StoryTimes.source(base))
        assertNull(blocksOf(hidden).getValue(b(43))["stage"])
        val on = base.copy(project = base.project.copy(script = base.project.script.copy(stagePositionEnabled = true)))
        assertEquals("left", blocksOf(GameJsonExport.build(on, ExportScope.json(listOf(F1.S3)), StoryTimes.source(on))).getValue(b(43))["stage"]!!.jsonPrimitive.content)
        val off = base.copy(project = base.project.copy(script = base.project.script.copy(voiceIdEnabled = false)))
        assertNull(blocksOf(GameJsonExport.build(off, ExportScope.json(listOf(F1.S3)), StoryTimes.source(off))).getValue(b(43))["voice"])
        val rows = DialogueCsvExport.rows(off, ExportScope.csv(listOf(K.EPISODE_1_2)), StoryTimes.source(off))
        assertTrue(rows.all { it.voiceId.isEmpty() })
        assertTrue(rows.none { it.state == DialogueCsvExport.DISCARDED })
    }

    @Test
    fun `writing an export fixes only the written IDs`() {
        val doc = F1.build()
        val at = ExampleClock.at("2026-11-20T03:00:00Z")
        val out = GameJsonExport.export(doc, ExportScope.json(listOf(F1.S3)), StoryTimes.source(doc), at).doc
        assertEquals(VoiceMode.FIXED, out.block(b(43)).voice?.mode?.known)
        assertNull(out.block(b(50)).voice)
        assertNull(out.block(b(44)).voice)
        val again = GameJsonExport.export(out, ExportScope.json(listOf(F1.S3)), StoryTimes.source(out), at + 1000).doc
        assertEquals(at, again.block(b(43)).voice?.fixedAt)
        assertEquals(at + 1000, again.block(b(43)).voice?.lastExport?.at)
    }

    @Test
    fun `CSV states new, unchanged, changed and discarded`() {
        val doc = ExampleProject.build()
        val times = StoryTimes.source(doc)
        val scope = ExportScope.csv(listOf(K.EPISODE_1_1, K.EPISODE_1_2, K.EPISODE_1_3))
        val rows = DialogueCsvExport.rows(doc, scope, times)
        val dialogue = rows.filter { it.speaker != DialogueCsvExport.TRANSITION_SPEAKER && it.state != DialogueCsvExport.DISCARDED }
        assertTrue(dialogue.isNotEmpty())
        assertTrue(dialogue.toString(), dialogue.all { it.state == "" })
        val discarded = rows.single { it.state == DialogueCsvExport.DISCARDED }
        assertEquals("RIE_0102_007", discarded.voiceId)
        assertEquals(K.block(1, 2, 7), discarded.id)
        assertEquals(
            DialogueCsvExport.Row("전환 문구", "결사의 문 · 아침", "", SceneText.transitionId(K.SCENE_1_3_2), "새"),
            rows.single { it.speaker == DialogueCsvExport.TRANSITION_SPEAKER },
        )

        val edited = BlockOps.setRuns(doc, K.BLOCK_B12, listOf(TextRun.Plain("등불 속 그 돌, 뭐라고 했죠?")), ctx)
        assertEquals("바뀜", DialogueCsvExport.rows(edited, scope, StoryTimes.source(edited)).single { it.id == K.BLOCK_B12 }.state)
        val fresh = DialogueCsvExport.rows(F1.build(), ExportScope.csv(listOf(F1.S3)), StoryTimes.source(F1.build()))
        assertTrue(fresh.filter { it.state != DialogueCsvExport.DISCARDED }.all { it.state == DialogueCsvExport.NEW })
        assertEquals(7, fresh.size)
    }

    @Test
    fun `CSV text quoting and header`() {
        val text = DialogueCsvExport.toCsv(listOf(DialogueCsvExport.Row("리엔", "\"그래\", 가자", "RIE_0107_042", "k", "새")))
        assertEquals("﻿화자,대사,보이스 ID,블록 ID,상태\r\n리엔,\"\"\"그래\"\", 가자\",RIE_0107_042,k,새\r\n", text)
        assertEquals("a", DialogueCsvExport.escape("a"))
        assertEquals("\"a\nb\"", DialogueCsvExport.escape("a\nb"))
    }

    @Test
    fun `CSV export fixes written IDs and a hidden scene left out is not discarded`() {
        val doc = F1.build()
        val out = DialogueCsvExport.export(doc, ExportScope.csv(listOf(F1.S3)), StoryTimes.source(doc), ExampleClock.NOW)
        assertTrue(VoiceIds.isFixed(out.doc.block(b(42)).voice))
        val hidden = doc.updateUnit(F1.S4) { it.copy(hidden = true) }
        val rows = DialogueCsvExport.rows(hidden, ExportScope(listOf(F1.E17), includeHiddenScenes = false), StoryTimes.source(hidden))
        assertTrue(rows.none { r -> r.id in (49..52).map(::b) })
        assertTrue(rows.none { it.state == DialogueCsvExport.DISCARDED })
        assertTrue(ScriptChecks.choicesIntoHiddenScenes(hidden, listOf(F1.S3)).isEmpty())
        val choiceLast = F3.build().let { d ->
            BlockMove.move(d, listOf(b(45)), BlockPosition.After(b(48)), ctx).doc.updateUnit(F1.S4) { it.copy(hidden = true) }
        }
        assertEquals(listOf(b(45)), ScriptChecks.choicesIntoHiddenScenes(choiceLast, listOf(F1.S3)))
        assertEquals("▲ 숨긴 씬으로 가는 선택지 1", ScriptChecks.choicesIntoHiddenLabel(1))
        assertEquals(BlockKind.CHOICE, choiceLast.block(b(45)).kind.known)
    }
}
