package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.domain.library.importing.SameProjectChoice
import io.github.lenticualrstone.civilight.feature.library.export.ExportBuild
import io.github.lenticualrstone.civilight.feature.library.export.ExportFormRules
import io.github.lenticualrstone.civilight.feature.library.export.ExportRunner
import io.github.lenticualrstone.civilight.feature.library.openfile.ImportPorts
import io.github.lenticualrstone.civilight.feature.library.openfile.OpenFileCopy
import io.github.lenticualrstone.civilight.feature.library.openfile.OpenFileFlow
import io.github.lenticualrstone.civilight.feature.library.openfile.OpenStage
import io.github.lenticualrstone.civilight.feature.library.openfile.ProjectImporter
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 파일에서 열기 (LIB-50 … LIB-54): files made by this module's own 내보내기 are read back through
 * [OpenFileFlow] and imported through [ProjectImporter] over fake stores (spec §4.6).
 */
class OpenFileTest {
    private val now = LibFx.NOW
    private val zone = LibFx.ZONE
    private val ashen = LibFx.ashen()

    private fun entry(doc: ProjectDoc, isExample: Boolean = false, deletedAt: Long? = null): LibraryEntry =
        ExampleProject.libraryEntry().copy(
            key = doc.key,
            name = doc.project.name,
            rating = doc.project.rating,
            hidden = doc.project.protections.hidden,
            isExample = isExample,
            readOnly = isExample,
            updatedAt = LibFx.at("2026-11-22T08:12"),
            deletedAt = deletedAt,
        )

    private suspend fun fileOf(doc: ProjectDoc, password: String? = null): ByteArray {
        var form = ExportFormRules.initial(doc, now, zone, ExportFormat.PROJECT_FILE)
        form = if (password == null) {
            ExportFormRules.setEncrypt(form, false)
        } else {
            ExportFormRules.setPasswordRepeat(ExportFormRules.setPassword(ExportFormRules.setEncrypt(form, true), password), password)
        }
        val built = ExportRunner.build(doc, form, ExampleRecorder.library(), BlobReader.NONE, "Civilight test (Android)", now) { true }
        return (built as ExportBuild.Ready).file.bytes
    }

    private fun open(bytes: ByteArray?, library: List<LibraryEntry>, knownFeatures: Set<String>? = null): OpenStage =
        if (knownFeatures == null) {
            OpenFileFlow.open(bytes, "잿빛 새벽의 등불.civilight", library, { 31 }, now, zone)
        } else {
            OpenFileFlow.open(bytes, "잿빛 새벽의 등불.civilight", library, { 31 }, now, zone, knownFeatures = knownFeatures)
        }

    /** Records what the importer asks of the stores, in order. */
    private class FakePorts(var entries: List<LibraryEntry>) : ImportPorts {
        val calls = mutableListOf<String>()
        val added = mutableListOf<ProjectDoc>()
        var recorder = RecorderLibrary()

        override fun libraryEntries(): List<LibraryEntry> = entries
        override suspend fun importBlob(hash: String, bytes: ByteArray, mime: String, fileName: String) {
            calls += "blob"
        }
        override suspend fun releaseProject(key: Key) {
            calls += "release:$key"
        }
        override suspend fun moveAside(key: Key) {
            calls += "moveAside:$key"
        }
        override suspend fun addProject(doc: ProjectDoc) {
            calls += "add:${doc.key}"
            added += doc
        }
        override suspend fun updateRecorder(transform: (RecorderLibrary) -> RecorderLibrary) {
            calls += "recorder"
            recorder = transform(recorder)
        }
        override suspend fun removeRecentsOf(key: Key) {
            calls += "recents:$key"
        }
    }

    private var keys = 0
    private val newKey: () -> Key = { "01KOPEN" + (++keys).toString().padStart(19, '0') }

    @Test
    fun `F-09 a file of a project not in the 서재 opens without the same-project block`() = runTest {
        val stage = open(fileOf(ashen), emptyList())
        stage as OpenStage.Ask
        assertEquals("잿빛 새벽의 등불", stage.sheet.title)
        assertNull(stage.sheet.sameProject)
        assertNull(stage.choice)
        assertTrue(stage.sheet.summary, stage.sheet.summary.startsWith("씬 "))

        val ports = FakePorts(emptyList())
        val done = ProjectImporter(ports).import(stage.pkg, stage.decision, stage.choice, now, zone, newKey)
        assertEquals("imported with its own key", ashen.key, done.projectKey)
        assertFalse(done.hidden)
        assertEquals(listOf("add:${ashen.key}", "recorder"), ports.calls.filter { it != "blob" })
        assertEquals(ashen.project.name, ports.added.single().project.name)
    }

    @Test
    fun `F-10 the same key asks, 새 프로젝트로 by default adds a renamed copy under a new key`() = runTest {
        val library = listOf(entry(ashen))
        val stage = open(fileOf(ashen), library) as OpenStage.Ask
        val block = stage.sheet.sameProject!!
        assertEquals("같은 프로젝트가 이미 있음", block.heading)
        assertEquals("서재 · 씬 31 · 오늘 08:12 수정", block.existingSub)
        assertEquals(SameProjectChoice.AS_NEW_PROJECT, stage.choice)
        assertEquals("‘잿빛 새벽의 등불 (2)’로 추가", block.choices.sub(SameProjectChoice.AS_NEW_PROJECT))
        assertTrue(stage.overwriteAllowed)

        val ports = FakePorts(library)
        val done = ProjectImporter(ports).import(stage.pkg, stage.decision, stage.choice, now, zone, newKey)
        assertNotEquals(ashen.key, done.projectKey)
        assertEquals("잿빛 새벽의 등불 (2)", ports.added.single().project.name)
        assertTrue("the old copy is untouched", ports.calls.none { it.startsWith("moveAside") || it.startsWith("release") })
    }

    @Test
    fun `F-12 덮어쓰기 releases the old copy, moves it aside, then adds the file's project with its key`() = runTest {
        val library = listOf(entry(ashen))
        val stage = (open(fileOf(ashen), library) as OpenStage.Ask).choose(SameProjectChoice.OVERWRITE)
        assertEquals(SameProjectChoice.OVERWRITE, stage.choice)
        val ports = FakePorts(library)
        val done = ProjectImporter(ports).import(stage.pkg, stage.decision, stage.choice, now, zone, newKey)
        assertEquals(ashen.key, done.projectKey)
        val k = ashen.key
        assertEquals(listOf("release:$k", "moveAside:$k", "recents:$k", "add:$k", "recorder"), ports.calls.filter { it != "blob" })
    }

    @Test
    fun `the read-only example is never overwritten from a file`() = runTest {
        val library = listOf(entry(ashen, isExample = true))
        val stage = open(fileOf(ashen), library) as OpenStage.Ask
        assertFalse(stage.overwriteAllowed)
        assertEquals("덮어쓰기 cannot be chosen", SameProjectChoice.AS_NEW_PROJECT, stage.choose(SameProjectChoice.OVERWRITE).choice)
        val ports = FakePorts(library)
        val done = ProjectImporter(ports).import(stage.pkg, stage.decision, SameProjectChoice.OVERWRITE, now, zone, newKey)
        assertNotEquals("even a forced 덮어쓰기 adds a copy", ashen.key, done.projectKey)
        assertTrue(ports.calls.none { it.startsWith("moveAside") })
    }

    @Test
    fun `the decision is taken again at 열기 when the same project left the 서재 meanwhile`() = runTest {
        val stage = open(fileOf(ashen), listOf(entry(ashen))) as OpenStage.Ask
        val ports = FakePorts(listOf(entry(ashen, deletedAt = LibFx.at("2026-11-22T08:30"))))
        val done = ProjectImporter(ports).import(stage.pkg, stage.decision, stage.choice, now, zone, newKey)
        assertEquals("only in the trash: imported as itself", ashen.key, done.projectKey)
        assertEquals(ashen.project.name, ports.added.single().project.name)
    }

    @Test
    fun `F-13 F-14 F-15 a locked file asks for its 암호, a wrong one keeps the sheet, the import lands hidden`() = runTest {
        val bytes = fileOf(ashen, password = "등불지기1234")
        val asked = open(bytes, emptyList()) as OpenStage.Password
        assertEquals("암호", asked.sheet.field.label)
        assertEquals("내보낼 때 정한 암호", asked.sheet.field.hint)
        assertEquals("잿빛 새벽의 등불.civilight", asked.sheet.fileName)
        assertNull(asked.error)

        val wrong = OpenFileFlow.unlock(bytes, "등불지기".toCharArray(), asked.sheet, emptyList(), { 0 }, now, zone)
        wrong as OpenStage.Password
        assertEquals(OpenFileCopy.TODO_COPY_LIB54_WRONG_PASSWORD, wrong.error)

        val opened = OpenFileFlow.unlock(bytes, "등불지기1234".toCharArray(), asked.sheet, emptyList(), { 0 }, now, zone) as OpenStage.Ask
        assertEquals("잿빛 새벽의 등불", opened.sheet.title)
        val ports = FakePorts(emptyList())
        val done = ProjectImporter(ports).import(opened.pkg, opened.decision, opened.choice, now, zone, newKey)
        assertTrue("a file locked with an export 암호 is hidden on this device", done.hidden)
        assertTrue(ports.added.single().project.protections.hidden)
    }

    @Test
    fun `F-04 F-06 a file needing an unknown feature is refused and never imported`() = runTest {
        val stage = open(fileOf(ashen), emptyList(), knownFeatures = emptySet())
        stage as OpenStage.Newer
        assertEquals("새 버전 앱에서 만든 파일", stage.sheet.heading)
        assertEquals("가져오지 않음 · 파일은 그대로 · 업데이트 뒤 다시 열기", stage.sheet.note)
        assertTrue(stage.sheet.rows.any { it.first == "필요한 기능" })
    }

    @Test
    fun `a file that is not a project fails with its own message`() {
        assertEquals(OpenStage.Failed(OpenFileCopy.TODO_COPY_LIB52_NOT_A_PROJECT), open(null, emptyList()))
        assertEquals(OpenStage.Failed(OpenFileCopy.TODO_COPY_LIB52_NOT_A_PROJECT), open(byteArrayOf(1, 2, 3, 4), emptyList()))
    }

    @Test
    fun `R23 recorder records of the file merge by key and only gain links`() = runTest {
        val stage = open(fileOf(ashen), emptyList()) as OpenStage.Ask
        val ports = FakePorts(emptyList())
        ports.recorder = ExampleRecorder.library()
        val before = ports.recorder.records.map { it.key }.toSet()
        ProjectImporter(ports).import(stage.pkg, stage.decision, stage.choice, now, zone, newKey)
        assertEquals("no record is duplicated", before, ports.recorder.records.map { it.key }.toSet())
    }
}
