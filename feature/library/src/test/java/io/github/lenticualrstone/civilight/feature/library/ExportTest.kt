package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.export.ExportRow
import io.github.lenticualrstone.civilight.domain.library.export.HiddenSceneChoice
import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.feature.library.export.ExportBuild
import io.github.lenticualrstone.civilight.feature.library.export.ExportCopy
import io.github.lenticualrstone.civilight.feature.library.export.ExportForm
import io.github.lenticualrstone.civilight.feature.library.export.ExportFormRules
import io.github.lenticualrstone.civilight.feature.library.export.ExportRanges
import io.github.lenticualrstone.civilight.feature.library.export.ExportRunner
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 내보내기 sheet (LIB-41, LIB-45, LIB-47): the draft rules and the file build, spec §4.5. */
class ExportTest {
    private val now = LibFx.NOW
    private val zone = LibFx.ZONE
    private val ashen = LibFx.ashen()

    /** 잿빛 with 1-7 씬 4 hidden (E-02 … E-04 Given). */
    private val hiddenS4: ProjectDoc = ashen.copy(story = ashen.story.copy(units = ashen.story.units.map { if (it.key == ExampleKeys.SCENE_1_7_4) it.copy(hidden = true) else it }))

    private fun form(doc: ProjectDoc, format: ExportFormat): ExportForm = ExportFormRules.initial(doc, now, zone, format)

    @Test
    fun `the sheet opens on 대본 PDF with 1장 전체 as the board draws it`() {
        val f = ExportFormRules.initial(ashen, now, zone)
        assertEquals(ExportFormat.PDF, f.format)
        val sheet = ExportFormRules.sheet(ashen, f, RecorderLibrary())
        assertEquals("1장 전체", sheet.rows.filterIsInstance<ExportRow.Range>().firstOrNull()!!.value)
        assertEquals("연출 · 괄호 지문 포함", sheet.rows.filterIsInstance<ExportRow.Layers>().firstOrNull()!!.value)
        assertEquals("잿빛 새벽의 등불", sheet.rows.filterIsInstance<ExportRow.FileName>().firstOrNull()!!.value)
        assertEquals("PDF 만들기", sheet.primaryButton)
        assertTrue("the 대본 PDF is written since batch 5 (LIB-43)", ExportFormRules.canCreate(f))
    }

    @Test
    fun `E-01 E-02 switching to JSON brings its 넣기 default and the hidden scene row`() {
        val pdf = form(hiddenS4, ExportFormat.PDF)
        assertEquals(HiddenSceneChoice.EXCLUDE, ExportFormRules.sheet(hiddenS4, pdf, RecorderLibrary()).rows.filterIsInstance<ExportRow.HiddenScenes>().firstOrNull()!!.choice)
        val json = ExportFormRules.selectFormat(pdf, hiddenS4, ExportFormat.ENGINE_JSON, now, zone)
        val row = ExportFormRules.sheet(hiddenS4, json, RecorderLibrary()).rows.filterIsInstance<ExportRow.HiddenScenes>().firstOrNull()!!
        assertEquals("숨긴 씬 1 · 1-7 씬 4", row.label)
        assertEquals(HiddenSceneChoice.INCLUDE, row.choice)
        assertTrue(ExportFormRules.canCreate(json))
    }

    @Test
    fun `E-03 넣기 asks once and a cancel builds nothing`() = runTest {
        val json = form(hiddenS4, ExportFormat.ENGINE_JSON)
        var prompts = 0
        val cancelled = ExportRunner.build(hiddenS4, json, RecorderLibrary(), BlobReader.NONE, "t", now) { prompts++; false }
        assertEquals(ExportBuild.Cancelled, cancelled)
        assertEquals(1, prompts)

        val ready = ExportRunner.build(hiddenS4, json, RecorderLibrary(), BlobReader.NONE, "t", now) { prompts++; true }
        ready as ExportBuild.Ready
        assertEquals(2, prompts)
        assertEquals("잿빛 새벽의 등불.json", ready.file.fileName)
        assertEquals("application/json", ready.file.mimeType)
        val plan = ready.file.plan!!
        assertTrue(plan.scenes.any { it.sceneKey == ExampleKeys.SCENE_1_7_4 })
        val text = ready.file.bytes.toString(Charsets.UTF_8)
        assertTrue(text.contains("\"civilight-engine\""))
    }

    @Test
    fun `E-04 빼기 builds without a prompt and leaves the scene out`() = runTest {
        val json = ExportFormRules.setHiddenScenes(form(hiddenS4, ExportFormat.ENGINE_JSON), include = false)
        var prompts = 0
        val ready = ExportRunner.build(hiddenS4, json, RecorderLibrary(), BlobReader.NONE, "t", now) { prompts++; true } as ExportBuild.Ready
        assertEquals(0, prompts)
        assertFalse(ready.file.plan!!.scenes.any { it.sceneKey == ExampleKeys.SCENE_1_7_4 })
    }

    @Test
    fun `LIB-46 voice IDs are fixed only by fixVoiceIds after the save, on the doc as it is then`() = runTest {
        val csv = form(ashen, ExportFormat.CSV)
        val ready = ExportRunner.build(ashen, csv, RecorderLibrary(), BlobReader.NONE, "t", now) { true } as ExportBuild.Ready
        assertEquals("잿빛 새벽의 등불.csv", ready.file.fileName)
        val renamed = ashen.copy(project = ashen.project.copy(name = "다른 이름"))
        val fixed = ExportRunner.fixVoiceIds(renamed, ready.file.plan!!, now)
        assertNotEquals(renamed, fixed)
        assertEquals("a later edit made before the save stays", "다른 이름", fixed.project.name)
    }

    @Test
    fun `E-09 file names follow the rating and a typed name survives a format change`() {
        val oath = LibFx.oath()
        assertTrue(form(oath, ExportFormat.ENGINE_JSON).options.fileName.startsWith("civilight-내보내기-"))
        val typed = ExportFormRules.setFileName(form(ashen, ExportFormat.CSV), "대사 표")
        assertEquals("대사 표", ExportFormRules.selectFormat(typed, ashen, ExportFormat.ENGINE_JSON, now, zone).options.fileName)
        val blank = ExportFormRules.setFileName(form(ashen, ExportFormat.CSV), "  ")
        assertFalse(ExportFormRules.canCreate(blank))
    }

    @Test
    fun `E-08 E-10 the password rows follow the project and both fields must agree`() {
        val oath = LibFx.oath()
        val file = form(oath, ExportFormat.PROJECT_FILE)
        assertTrue("검은 서약 encrypts by default", file.options.encrypt)
        assertEquals(ExportRow.Password(on = true), ExportFormRules.sheet(oath, file, RecorderLibrary()).rows.filterIsInstance<ExportRow.Password>().firstOrNull())
        assertFalse("no password yet", ExportFormRules.canCreate(file))
        val typed = ExportFormRules.setPasswordRepeat(ExportFormRules.setPassword(file, "등불지기1234"), "등불지기123")
        assertFalse(ExportFormRules.passwordsMatch(typed))
        assertFalse(ExportFormRules.canCreate(typed))
        assertTrue(ExportFormRules.canCreate(ExportFormRules.setPasswordRepeat(typed, "등불지기1234")))
        val off = ExportFormRules.setEncrypt(typed, false)
        assertEquals("", off.password)
        assertTrue(ExportFormRules.canCreate(off))
        assertFalse("잿빛 R-15 without hidden items is not encrypted", form(ashen, ExportFormat.PROJECT_FILE).options.encrypt)

        val pdf = form(oath, ExportFormat.PDF)
        assertEquals(ExportRow.PdfPassword(on = true), ExportFormRules.sheet(oath, pdf, RecorderLibrary()).rows.filterIsInstance<ExportRow.PdfPassword>().firstOrNull())
        assertNull(ExportFormRules.sheet(ashen, form(ashen, ExportFormat.PDF), RecorderLibrary()).rows.filterIsInstance<ExportRow.PdfPassword>().firstOrNull())
    }

    @Test
    fun `E-11 the recorder row counts linked records and the audio choice`() {
        val f = ExportFormRules.setRecorderAudio(form(ashen, ExportFormat.PROJECT_FILE), true)
        val row = ExportFormRules.sheet(ashen, f, ExampleRecorder.library()).rows.filterIsInstance<ExportRow.RecorderRecords>().firstOrNull()!!
        assertTrue(row.includeAudio)
        assertTrue(row.linked > 0)
        assertEquals("레코더 기록 ${row.linked}", row.label)
    }

    @Test
    fun `LIB-47 LIB-49 a project file is a civilight package, locked when the switch is on`() = runTest {
        val plain = ExportRunner.build(ashen, form(ashen, ExportFormat.PROJECT_FILE), ExampleRecorder.library(), BlobReader.NONE, "Civilight test (Android)", now) { error("no prompt") }
        plain as ExportBuild.Ready
        assertEquals("잿빛 새벽의 등불.civilight", plain.file.fileName)
        assertEquals("application/vnd.civilight.project", plain.file.mimeType)
        assertNull("a project file fixes no voice ID", plain.file.plan)
        val zip = plain.file.bytes
        assertEquals('P'.code.toByte(), zip[0])
        assertEquals('K'.code.toByte(), zip[1])
        assertTrue("mimetype is the first entry, stored", String(zip, 30, 8, Charsets.US_ASCII) == "mimetype")

        val locked = ExportFormRules.setPasswordRepeat(ExportFormRules.setPassword(ExportFormRules.setEncrypt(form(ashen, ExportFormat.PROJECT_FILE), true), "등불지기1234"), "등불지기1234")
        val enc = ExportRunner.build(ashen, locked, ExampleRecorder.library(), BlobReader.NONE, "t", now) { true } as ExportBuild.Ready
        assertTrue(enc.file.bytes.toString(Charsets.ISO_8859_1).contains("encryption.json"))
        assertFalse(enc.file.bytes.toString(Charsets.ISO_8859_1).contains("manifest.json"))
    }

    @Test
    fun `without a PDF writer (JVM) the 대본 PDF reports unavailable`() = runTest {
        assertEquals(ExportBuild.Unavailable, ExportRunner.build(ashen, form(ashen, ExportFormat.PDF), RecorderLibrary(), BlobReader.NONE, "t", now) { true })
    }

    @Test
    fun `범위 offers the levels above 씬 with their labels`() {
        val choices = ExportRanges.choices(ashen)
        assertEquals("1장 전체", choices.first().label)
        assertEquals(0, choices.first().depth)
        assertTrue(choices.any { it.label == "에피소드 1-7" && it.depth == 1 })
        assertTrue(choices.none { it.key == ExampleKeys.SCENE_1_7_4 })
        val f = ExportFormRules.setRange(form(ashen, ExportFormat.CSV), emptyList())
        assertEquals(ExportCopy.RANGE_ALL, ExportFormRules.sheet(ashen, f, RecorderLibrary()).rows.filterIsInstance<ExportRow.Range>().firstOrNull()!!.value)
    }

    @Test
    fun `a sheet that leaves keeps its choices but not the typed passwords`() {
        val oath = LibFx.oath()
        val typed = ExportFormRules.setPasswordRepeat(ExportFormRules.setPassword(ExportFormRules.setFileName(form(oath, ExportFormat.PROJECT_FILE), "보관"), "등불지기1234"), "등불지기1234")
        val kept = ExportFormRules.withoutPasswords(typed)
        assertEquals("", kept.password)
        assertEquals("", kept.passwordRepeat)
        assertEquals("보관", kept.options.fileName)
        assertTrue(kept.options.encrypt)
        assertFalse(ExportFormRules.canCreate(kept))
        val pdf = ExportFormRules.withoutPasswords(ExportFormRules.setPdfPassword(form(oath, ExportFormat.PDF), "비밀"))
        assertEquals("", pdf.pdfPassword)
        assertEquals("the PDF 암호 switch stays on", "", pdf.options.pdfPassword)
    }
}
