package io.github.lenticualrstone.civilight.model

import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.DayBaseline
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.ExportRecord
import io.github.lenticualrstone.civilight.model.app.LensState
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.app.LibraryIndexFile
import io.github.lenticualrstone.civilight.model.app.ProjectActivity
import io.github.lenticualrstone.civilight.model.app.ProjectActivityFile
import io.github.lenticualrstone.civilight.model.app.ProjectViewFile
import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import io.github.lenticualrstone.civilight.model.app.ThemeMode
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.EntryContext
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.RecordingPhase
import io.github.lenticualrstone.civilight.model.recorder.RecordingSession
import io.github.lenticualrstone.civilight.model.script.ScriptPreview
import io.github.lenticualrstone.civilight.model.script.ScriptViewState
import io.github.lenticualrstone.civilight.model.text.TextRun
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** App-only files (class A/S/RT) round-trip with lowerCamelCase enum values. */
class AppStateJsonTest {
    @Test
    fun appSettings() {
        val settings = AppSettings(theme = ThemeMode.DARK, relockPolicy = RelockPolicy.FIVE_MINUTES)
        val json = CivilightJson.encodeElement(AppSettings.serializer(), settings).jsonObject
        assertEquals(JsonPrimitive("dark"), json["theme"])
        assertEquals(JsonPrimitive("fiveMinutes"), json["relockPolicy"])
        assertEquals(settings, CivilightJson.decodeElement(AppSettings.serializer(), json))
        // Unknown values of app-only enums fall back instead of failing.
        val odd = CivilightJson.decode(AppSettings.serializer(), """{"theme":"sepia"}""")
        assertEquals(ThemeMode.SYSTEM, odd.theme)
    }

    @Test
    fun projectAppFiles() {
        val activity = ProjectActivityFile(
            activity = ProjectActivity(lastEditedBlockKey = "B", lastEditedOffset = 4, lastEditedAt = 1_000L),
            dayBaselines = listOf(DayBaseline(LocalDate.of(2026, 11, 22), "B", 120)),
            exportRecords = listOf(ExportRecord(2_000L, ExportFormat.ENGINE_JSON, listOf("E1-7"))),
        )
        assertEquals(activity, CivilightJson.decode(ProjectActivityFile.serializer(), CivilightJson.encode(ProjectActivityFile.serializer(), activity)))
        val view = ProjectViewFile(
            lens = LensState(point = LensPoint.UnitEnd("E1-5"), savedForScript = LensPoint.Latest),
            script = ScriptViewState(preview = ScriptPreview.DIALOGUE_ONLY, recentSpeakers = listOf("C1")),
        )
        val text = CivilightJson.encode(ProjectViewFile.serializer(), view)
        assertTrue(text, "\"kind\":\"unitEnd\"" in text && "\"dialogueOnly\"" in text)
        assertEquals(view, CivilightJson.decode(ProjectViewFile.serializer(), text))
    }

    @Test
    fun lastCanvasKeyIsOptionalAndRoundTrips() {
        val plain = CivilightJson.encode(ProjectViewFile.serializer(), ProjectViewFile())
        assertTrue(plain, "lastCanvasKey" !in plain)
        assertEquals(null, CivilightJson.decode(ProjectViewFile.serializer(), plain).lastCanvasKey)
        val view = ProjectViewFile(lastCanvasKey = "CV1")
        val text = CivilightJson.encode(ProjectViewFile.serializer(), view)
        assertTrue(text, "\"lastCanvasKey\":\"CV1\"" in text)
        assertEquals(view, CivilightJson.decode(ProjectViewFile.serializer(), text))
    }

    @Test
    fun libraryIndex() {
        val index = LibraryIndexFile(listOf(LibraryEntry(key = "P", name = "잿빛 새벽의 등불", rating = Rating.R18.open(), hidden = true)))
        val text = CivilightJson.encode(LibraryIndexFile.serializer(), index)
        assertTrue("\"r18\"" in text)
        assertEquals(index, CivilightJson.decode(LibraryIndexFile.serializer(), text))
    }

    @Test
    fun recordingSessionForCrashRecovery() {
        val session = RecordingSession(
            recordKey = "R",
            entry = EntryContext(surface = EntrySurface.QS_TILE, onLockScreen = true),
            phase = RecordingPhase.PAUSED,
            elapsedMs = 12_000,
            confirmedRuns = listOf(TextRun.Plain("하르트가")),
            audioTempPath = "/tmp/a.m4a",
        )
        val text = CivilightJson.encode(RecordingSession.serializer(), session)
        assertTrue("\"qsTile\"" in text && "\"paused\"" in text)
        assertEquals(session, CivilightJson.decode(RecordingSession.serializer(), text))
    }

    @Test
    fun textIsNfcOnWrite() {
        val nfd = "하르트" // 하르트 in NFD
        val text = CivilightJson.encode(TextRun.serializer(), TextRun.Plain(nfd))
        assertEquals("{\"t\":\"하르트\"}", text)
        assertEquals("하르트", nfd.nfc())
    }
}
