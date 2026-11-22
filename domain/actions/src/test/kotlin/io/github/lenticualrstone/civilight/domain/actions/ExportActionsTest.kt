package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.library.export.ExportPlanner
import io.github.lenticualrstone.civilight.domain.library.export.ExportRunOutcome
import io.github.lenticualrstone.civilight.domain.script.DialogueCsvExport
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.ExportOptions
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** ExportRun decides range, hidden scenes and voice-ID fixation once; the script serialisers write the plan. */
class ExportActionsTest {
    /** The example with 1-7 씬 3 hidden and voice IDs on. */
    private val doc: ProjectDoc = Fx.doc().let { d ->
        d.copy(
            project = d.project.copy(script = d.project.script.copy(voiceIdEnabled = true)),
            story = d.story.copy(units = d.story.units.map { if (it.key == K.SCENE_1_7_3) it.copy(hidden = true) else it }),
        )
    }

    private fun options(format: ExportFormat, includeHidden: Boolean, range: List<String> = emptyList()) =
        ExportOptions(format = format, rangeKeys = range, includeHiddenScenes = includeHidden, fileName = "잿빛")

    private fun done(outcome: ExportRunOutcome): ExportRunOutcome.Done = outcome as? ExportRunOutcome.Done ?: error("expected Done, got $outcome")

    private val bom = 0xFEFF.toChar().toString()

    private fun csv(outcome: ExportRunOutcome.Done): List<List<String>> {
        val text = String(outcome.file.bytes, Charsets.UTF_8)
        assertTrue(text.startsWith(bom))
        return parseCsv(text.removePrefix(bom))
    }

    /** RFC 4180 reader (quoted cells, doubled quotes, CRLF rows). */
    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                quoted && c == '"' && text.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
                c == '"' -> quoted = !quoted
                !quoted && c == ',' -> { row += cell.toString(); cell.clear() }
                !quoted && c == '\r' && text.getOrNull(i + 1) == '\n' -> {
                    row += cell.toString(); cell.clear(); rows += row; row = mutableListOf(); i++
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row += cell.toString(); rows += row }
        return rows
    }

    @Test
    fun `hidden scenes 넣기 needs the one authentication`() {
        val cancelled = ExportActions.run(doc, options(ExportFormat.CSV, includeHidden = true), authenticated = false, ExampleClock.NOW)
        assertEquals(ExportRunOutcome.Cancelled, cancelled)
        assertTrue(ExportActions.run(doc, options(ExportFormat.CSV, includeHidden = false), authenticated = false, ExampleClock.NOW) is ExportRunOutcome.Done)
    }

    @Test
    fun `CSV rows follow the plan and the written voice IDs are fixed`() {
        val out = done(ExportActions.run(doc, options(ExportFormat.CSV, includeHidden = true), authenticated = true, ExampleClock.NOW))
        assertEquals("잿빛.csv", out.file.fileName)
        val rows = csv(out)
        assertEquals(DialogueCsvExport.HEADER, rows.first())
        val lines = out.plan.writtenLines
        val lineRows = rows.drop(1).filter { it[0] != "전환 문구" && it[4] != "버림" }
        assertEquals(lines.size, lineRows.size)
        assertEquals(out.plan.discardedVoiceIds.size, rows.count { it.getOrNull(4) == "버림" })
        // Every written dialogue line with an ID is fixed with exactly the ID the file shows.
        val withId = lineRows.filter { it[2].isNotEmpty() }
        assertTrue(withId.isNotEmpty())
        val dialogue = lines.filter { it.optionKey == null }.map { it.blockKey }.toSet()
        for (row in withId.filter { it[3] in dialogue }) {
            val voice = out.doc.index.block(row[3])!!.voice
            assertEquals(VoiceMode.FIXED, voice?.mode?.known)
            assertEquals(row[2], voice?.fixedId)
        }
    }

    @Test
    fun `a second CSV export shows no state for unchanged lines`() {
        val first = done(ExportActions.run(doc, options(ExportFormat.CSV, includeHidden = true), authenticated = true, ExampleClock.NOW))
        val second = done(ExportActions.run(first.doc, options(ExportFormat.CSV, includeHidden = true), authenticated = true, ExampleClock.NOW + 60_000))
        // Lines without a voice ID get no `lastExport` from the fixation, so only lines with an ID are compared.
        val states = csv(second).drop(1).filter { it[0] != "전환 문구" && it.getOrNull(4) != "버림" && it[2].isNotEmpty() }.map { it.getOrElse(4) { "" } }
        assertTrue(states.isNotEmpty())
        assertTrue(states.toString(), states.all { it.isEmpty() })
    }

    @Test
    fun `빼기 leaves the hidden scene out of the file and its lines unfixed`() {
        val out = done(ExportActions.run(doc, options(ExportFormat.CSV, includeHidden = false), authenticated = false, ExampleClock.NOW))
        val hiddenBlocks = doc.manuscript.liveBlocksOf(K.SCENE_1_7_3).map { it.key }.toSet()
        assertTrue(hiddenBlocks.isNotEmpty())
        assertTrue(csv(out).none { it.getOrNull(3) in hiddenBlocks })
        for (key in hiddenBlocks) assertEquals(doc.index.block(key)!!.voice, out.doc.index.block(key)!!.voice)
    }

    @Test
    fun `engine JSON writes the planned scenes and speakers`() {
        val opts = options(ExportFormat.ENGINE_JSON, includeHidden = false, range = listOf(K.EPISODE_1_7))
        val out = done(ExportActions.run(doc, opts, authenticated = false, ExampleClock.NOW))
        assertEquals("잿빛.json", out.file.fileName)
        val json = Json.parseToJsonElement(String(out.file.bytes, Charsets.UTF_8)).jsonObject
        assertEquals("civilight-engine", json["format"]!!.jsonPrimitive.content)
        val plan = ExportPlanner.plan(doc, opts)
        val scenes = json["scenes"]!!.jsonArray.map { it.jsonObject["id"]!!.jsonPrimitive.content }
        assertEquals(plan.scenes.map { it.sceneKey }, scenes)
        assertFalse(K.SCENE_1_7_3 in scenes)
        val characters = json["characters"]!!.jsonArray.map { it.jsonObject }
        assertEquals(plan.characters.map { it.key }, characters.map { it["id"]!!.jsonPrimitive.content })
        for (c in characters) {
            val planned = plan.characters.single { it.key == c["id"]!!.jsonPrimitive.content }
            val character = doc.index.character(planned.key)
            if (character == null || character.hidden) assertNull(c["fields"]) else assertTrue(c["fields"] is JsonObject)
        }
    }

    @Test
    fun `engine JSON voice IDs are the IDs the export fixes`() {
        val out = done(ExportActions.run(doc, options(ExportFormat.ENGINE_JSON, includeHidden = false), authenticated = false, ExampleClock.NOW))
        val json = Json.parseToJsonElement(String(out.file.bytes, Charsets.UTF_8)).jsonObject
        val voiced = json["scenes"]!!.jsonArray.flatMap { it.jsonObject["blocks"]!!.jsonArray }.map { it.jsonObject }
            .filter { it["type"]?.jsonPrimitive?.content == "dialogue" && it["voice"] != null }
        assertTrue(voiced.isNotEmpty())
        for (b in voiced) {
            val voice = out.doc.index.block(b["id"]!!.jsonPrimitive.content)!!.voice
            assertEquals(VoiceMode.FIXED, voice?.mode?.known)
            assertEquals(b["voice"]!!.jsonPrimitive.content, voice?.fixedId)
        }
    }

    @Test
    fun `an empty plan writes an empty engine file`() {
        val empty = ExportPlanner.plan(doc, options(ExportFormat.ENGINE_JSON, includeHidden = false, range = listOf(K.SCENE_1_7_3)))
        assertTrue(empty.scenes.isEmpty())
        val json = ExportActions.engineJson(doc, empty)
        assertEquals(JsonArray(emptyList()), json["scenes"])
        assertEquals(JsonArray(emptyList()), json["characters"])
    }

    @Test
    fun `PDF and civilight are not serialised here`() {
        assertNull(ExportActions.serializerFor(ExportFormat.PDF))
        assertNull(ExportActions.serializerFor(ExportFormat.PROJECT_FILE))
    }
}
