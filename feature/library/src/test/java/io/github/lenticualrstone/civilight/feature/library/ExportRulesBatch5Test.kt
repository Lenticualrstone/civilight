package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.export.ExportRow
import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.domain.library.format.PackageOpen
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageReader
import io.github.lenticualrstone.civilight.domain.library.format.ZipCodec
import io.github.lenticualrstone.civilight.feature.library.export.ExportBuild
import io.github.lenticualrstone.civilight.feature.library.export.ExportFormRules
import io.github.lenticualrstone.civilight.feature.library.export.ExportRunner
import io.github.lenticualrstone.civilight.feature.library.openfile.OpenFileCopy
import io.github.lenticualrstone.civilight.feature.library.openfile.OpenFileFlow
import io.github.lenticualrstone.civilight.feature.library.openfile.OpenStage
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Batch 5 checks of rules this module only wires (LIB-42, LIB-44, LIB-46, LIB-55): the bytes and
 * stages come from the real `domain:actions` / `domain:library` code paths the sheet and the
 * 파일에서 열기 flow call.
 */
class ExportRulesBatch5Test {
    private val now = LibFx.NOW
    private val zone = LibFx.ZONE
    private val ashen = LibFx.ashen()

    private val hiddenS4: ProjectDoc =
        ashen.copy(story = ashen.story.copy(units = ashen.story.units.map { if (it.key == ExampleKeys.SCENE_1_7_4) it.copy(hidden = true) else it }))

    private suspend fun build(doc: ProjectDoc, format: ExportFormat, recorder: RecorderLibrary = RecorderLibrary()): ExportBuild.Ready {
        val form = ExportFormRules.initial(doc, now, zone, format)
        return ExportRunner.build(doc, form, recorder, BlobReader.NONE, "t", now, authenticate = { true }) as ExportBuild.Ready
    }

    @Test
    fun `LIB-44 the 대사 표 is UTF-8 with BOM, CRLF rows and the five columns in order`() = runTest {
        val ready = build(ashen, ExportFormat.CSV)
        val bytes = ready.file.bytes
        assertEquals("BOM", listOf(0xEF, 0xBB, 0xBF), bytes.take(3).map { it.toInt() and 0xFF })
        val text = bytes.copyOfRange(3, bytes.size).toString(Charsets.UTF_8)
        assertTrue(text.startsWith("화자,대사,보이스 ID,블록 ID,상태\r\n"))
        val rows = text.split("\r\n").filter { it.isNotEmpty() }
        assertTrue("one row per written line", rows.size > 1)
        assertTrue("no bare LF outside quoted cells", text.replace("\r\n", "").let { rest -> rest.count { it == '\n' } <= text.count { it == '"' } })
        assertEquals("text/csv", ready.file.mimeType)
        // First export: every written dialogue line reads 새 (R12 §6).
        val states = rows.drop(1).map { it.substringAfterLast(',') }
        assertTrue(states.any { it == "새" })
    }

    @Test
    fun `LIB-44 a cell with a comma or quote is RFC 4180 quoted`() {
        assertEquals("\"하르트, 방금 \"\"그\"\"\"", io.github.lenticualrstone.civilight.domain.script.DialogueCsvExport.escape("하르트, 방금 \"그\""))
    }

    @Test
    fun `LIB-42 빼기 leaves the hidden scene out of the 대사 표 and its lines are not 버림`() = runTest {
        val form = ExportFormRules.setHiddenScenes(ExportFormRules.initial(hiddenS4, now, zone, ExportFormat.CSV), include = false)
        val ready = ExportRunner.build(hiddenS4, form, RecorderLibrary(), BlobReader.NONE, "t", now, authenticate = { error("빼기 never asks") }) as ExportBuild.Ready
        val plan = ready.file.plan!!
        assertTrue(ExampleKeys.SCENE_1_7_4 in plan.leftOutScenes)
        val hiddenBlocks = hiddenS4.manuscript.liveBlocksOf(ExampleKeys.SCENE_1_7_4).map { it.key }.toSet()
        val text = ready.file.bytes.toString(Charsets.UTF_8)
        assertTrue("no row of the left-out scene", hiddenBlocks.none { text.contains(it) })
    }

    @Test
    fun `LIB-42 hidden recorder records stay out of a project file unless it is locked because the project is hidden`() {
        val library = ExampleRecorder.library()
        val linked = library.recordsLinkedTo(ashen.key).first { it.deletedAt == null }
        val withHidden = library.copy(records = library.records.map { if (it.key == linked.key) it.copy(hidden = true) else it })
        val plain = ExportFormRules.initial(ashen, now, zone, ExportFormat.PROJECT_FILE)
        val row = ExportFormRules.sheet(ashen, plain, withHidden).rows.filterIsInstance<ExportRow.RecorderRecords>().first()
        assertEquals(1, row.hiddenExcluded)
        assertEquals("숨긴 기록 1 제외", row.hiddenNote)

        val hiddenProject = ashen.copy(project = ashen.project.copy(protections = ashen.project.protections.copy(hidden = true)))
        val locked = ExportFormRules.initial(hiddenProject, now, zone, ExportFormat.PROJECT_FILE)
        assertTrue(locked.options.encrypt)
        val lockedRow = ExportFormRules.sheet(hiddenProject, locked, withHidden).rows.filterIsInstance<ExportRow.RecorderRecords>().first()
        assertEquals(0, lockedRow.hiddenExcluded)
        assertNull(lockedRow.hiddenNote)
    }

    @Test
    fun `LIB-46 the project file never carries a plan, so nothing is fixed after its save`() = runTest {
        val ready = build(ashen, ExportFormat.PROJECT_FILE, ExampleRecorder.library())
        assertNull(ready.file.plan)
    }

    // LIB-55

    private suspend fun projectFile(): ByteArray = build(ashen, ExportFormat.PROJECT_FILE, ExampleRecorder.library()).file.bytes

    private fun damage(bytes: ByteArray, path: String): ByteArray {
        val zip = ZipCodec.read(bytes)
        val files = LinkedHashMap<String, ByteArray>()
        for (item in zip.items) {
            if (item.path == PackagePaths.MIMETYPE) continue
            files[item.path] = if (item.path == path) "{ broken".toByteArray() else item.bytes
        }
        return ZipCodec.write(PackagePaths.MIME_PROJECT, files, now)
    }

    @Test
    fun `LIB-55 a damaged episode file still opens and names the part left out`() = runTest {
        val episodePath = "story/${ExampleKeys.episode(1, 7)}.json"
        val bytes = projectFile()
        assertTrue("the example writes 1-7 as its own file", ZipCodec.read(bytes).contains(episodePath))
        val stage = OpenFileFlow.open(damage(bytes, episodePath), "잿빛.civilight", emptyList(), { 0 }, now, zone)
        stage as OpenStage.Ask
        assertEquals("열지 못한 부분 · 1-7", stage.pkg.failedMessage)
        assertTrue("other episodes came through", stage.pkg.doc.manuscript.liveScenes.isNotEmpty())
    }

    @Test
    fun `LIB-55 a damaged project json or story index imports nothing`() = runTest {
        val bytes = projectFile()
        for (path in listOf(PackagePaths.PROJECT, "story/index.json")) {
            val damaged = damage(bytes, path)
            assertTrue(ProjectPackageReader.open(damaged) is PackageOpen.Damaged)
            assertEquals(OpenStage.Failed(OpenFileCopy.DAMAGED), OpenFileFlow.open(damaged, "x.civilight", emptyList(), { 0 }, now, zone))
        }
    }
}
