package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.actions.ExportActions
import io.github.lenticualrstone.civilight.domain.library.export.ExportRunOutcome
import io.github.lenticualrstone.civilight.domain.script.ScriptPdfContent
import io.github.lenticualrstone.civilight.domain.script.StoryTimes
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.ExportOptions
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.script.BlockKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/**
 * SCR-63 … SCR-65 on the path that writes files (DECISIONS §1: `ExportActions` serialises the plan
 * with the script's content), on the example's 1-7.
 */
class ExportContentTest {
    private val doc = Fx.example

    private fun run(format: ExportFormat): ExportRunOutcome.Done =
        ExportActions.run(doc, ExportOptions(format = format, rangeKeys = listOf(K.EPISODE_1_7), fileName = "잿빛"), authenticated = true, ExampleClock.NOW)
            as? ExportRunOutcome.Done ?: error("export did not finish")

    private fun JsonObject.str(key: String): String = getValue(key).jsonPrimitive.content

    @Test
    fun `T-22-21 SCR-63 the engine JSON scene value of 1-7 씬 4`() {
        val json = Json.parseToJsonElement(String(run(ExportFormat.ENGINE_JSON).file.bytes, Charsets.UTF_8)).jsonObject
        assertEquals(setOf("format", "version", "assets", "characters", "scenes"), json.keys)
        val scenes = json.getValue("scenes").jsonArray.map { it.jsonObject }
        val s4 = scenes.single { it.str("id") == K.SCENE_1_7_4 }
        assertEquals(4, s4.getValue("index").jsonPrimitive.int)
        assertEquals(JsonNull, s4["title"])
        val place = s4.getValue("place").jsonObject
        assertEquals("결사의 문", place.str("name"))
        assertEquals("place", place.str("kind"))
        val time = s4.getValue("time").jsonObject
        assertEquals(11, time.getValue("storyDay").jsonObject.getValue("min").jsonPrimitive.int)
        assertEquals(15, time.getValue("storyDay").jsonObject.getValue("max").jsonPrimitive.int)
        assertEquals(2, time.getValue("episodeDay").jsonPrimitive.int)
        assertEquals("아침", time.str("band"))
        assertEquals("06:10", time.str("clock"))
        assertEquals("exact", time.str("precision"))
        assertEquals("false", s4.getValue("flashback").jsonPrimitive.content)
        val transition = s4.getValue("transition").jsonObject
        assertEquals("direct", transition.str("mode"))
        assertEquals("결사의 문 앞 · 06:10", transition.str("text"))
        assertTrue(transition.str("id").endsWith(K.SCENE_1_7_4))
        // `없음` scenes carry no transition.
        assertEquals(JsonNull, scenes.single { it.str("id") == K.SCENE_1_7_3 }["transition"])

        // Blocks: permanent keys (never `#043`), no head or transition entries, memos marked not for export.
        val blocks = scenes.flatMap { it.getValue("blocks").jsonArray }.map { it.jsonObject }
        assertTrue(blocks.isNotEmpty())
        assertTrue(blocks.all { b -> doc.index.block(b.str("id")) != null })
        assertTrue(blocks.none { it.str("type") in setOf("head", "scene", "transition") })
        val memo = blocks.single { it.str("id") == K.block(1, 7, 46) }
        assertEquals("memo", memo.str("type"))
        assertEquals("false", memo.getValue("export").jsonPrimitive.content)
        val line = blocks.first { it.str("type") == "dialogue" }
        assertTrue(line.keys.containsAll(listOf("speaker", "displayName", "text")))
    }

    @Test
    fun `T-22-22 SCR-64 columns, one transition row for the scene with text, dialogue rows by block key`() {
        val text = String(run(ExportFormat.CSV).file.bytes, Charsets.UTF_8).removePrefix(0xFEFF.toChar().toString())
        val rows = text.split("\r\n").filter { it.isNotEmpty() }
        assertEquals("화자,대사,보이스 ID,블록 ID,상태", rows.first())
        val transitions = rows.filter { it.startsWith("전환 문구,") }
        assertEquals(1, transitions.size)
        // T-22-22 asks `새` in the last cell on a first export; `ExportActions` leaves it empty (Requests).
        assertTrue(transitions.single(), transitions.single().startsWith("전환 문구,결사의 문 앞 · 06:10,,tr-${K.SCENE_1_7_4},"))
        assertTrue(rows.any { it.startsWith("리엔,저 등은 세라 씨 기록에도 없던 거야.,RIE_0107_006,${K.block(1, 7, 6)},") })
    }

    @Test
    fun `SCR-65 PDF lines - each scene starts with its head, the transition text is never printed`() {
        val lines = ScriptPdfContent.linesOf(doc, listOf(K.SCENE_1_7_3, K.SCENE_1_7_4), StoryTimes.source(doc), includeDirectionAndParens = false)
        val titles = lines.filterIsInstance<ScriptPdfContent.Line.Title>().map { it.text }
        assertEquals(listOf("씬 3 · 결사의 문 · 2일째 새벽 05:40", "씬 4 · 결사의 문 · 2일째 아침 06:10"), titles)
        val bodies = lines.filterIsInstance<ScriptPdfContent.Line.Body>()
        assertFalse(bodies.any { it.text.contains("결사의 문 앞 · 06:10") })
        // `연출 · 괄호 지문` off: no direction block, no parenthetical.
        assertTrue(bodies.none { it.kind == BlockKind.DIRECTION || it.text.startsWith("(") })
    }
}
