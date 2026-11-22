package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.crypto.PackageCrypto
import io.github.lenticualrstone.civilight.domain.library.crypto.RecoveryKeys
import io.github.lenticualrstone.civilight.domain.library.format.RecorderPackage
import io.github.lenticualrstone.civilight.feature.library.openfile.ImportPorts
import io.github.lenticualrstone.civilight.feature.library.openfile.OpenFileCopy
import io.github.lenticualrstone.civilight.feature.library.openfile.OpenFileFlow
import io.github.lenticualrstone.civilight.feature.library.openfile.OpenStage
import io.github.lenticualrstone.civilight.feature.library.openfile.RecorderFiles
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** `.civilight-recorder` through 파일에서 열기 (LIB-48): open, password, merge by key. */
class RecorderFileTest {
    private val now = LibFx.NOW
    private val zone = LibFx.ZONE
    private val library = ExampleRecorder.library()

    private class Ports(var recorder: RecorderLibrary) : ImportPorts {
        override fun libraryEntries(): List<LibraryEntry> = emptyList()
        override suspend fun importBlob(hash: String, bytes: ByteArray, mime: String, fileName: String) = Unit
        override suspend fun releaseProject(key: Key) = Unit
        override suspend fun moveAside(key: Key) = Unit
        override suspend fun addProject(doc: ProjectDoc) = Unit
        override suspend fun updateRecorder(transform: (RecorderLibrary) -> RecorderLibrary) {
            recorder = transform(recorder)
        }
        override suspend fun removeRecentsOf(key: Key) = Unit
    }

    private var keys = 0
    private val newKey: () -> Key = { "01KRECO" + (++keys).toString().padStart(19, '0') }

    private fun file(includeHidden: Boolean = false) = RecorderPackage.write(library, includeHidden, includeAudio = false, audio = { null }, writtenBy = "t", writtenAt = now)

    @Test
    fun `LIB-48 a library file opens as 라이프 레코더 and merges without duplicates`() = runTest {
        val stage = OpenFileFlow.open(file(), "library.civilight-recorder", emptyList(), { 0 }, now, zone)
        stage as OpenStage.Recorder
        val visible = library.records.count { it.deletedAt == null && !it.hidden }
        assertEquals(visible, stage.records)

        val ports = Ports(library)
        RecorderFiles.import(stage.content, ports, newKey)
        assertEquals("records already here only gain links", library.records.size, ports.recorder.records.size)
        val empty = Ports(RecorderLibrary())
        RecorderFiles.import(stage.content, empty, newKey)
        RecorderFiles.import(stage.content, empty, newKey)
        assertEquals(visible, empty.recorder.records.size)
        assertEquals(empty.recorder.links.size, empty.recorder.links.distinctBy { listOf(it.recordKey, it.projectKey, it.targetKey) }.size)
    }

    @Test
    fun `LIB-48 a recovery-locked library file asks for the 복구 암호 and a wrong one keeps the sheet`() = runTest {
        val key = RecoveryKeys.create("등불지기의새벽1".toCharArray(), iterations = 1_000)
        val locked = PackageCrypto.encryptWithRecoveryKey(file(includeHidden = true), key, time = now, mimetype = PackagePaths.MIME_RECORDER)
        assertTrue(RecorderFiles.isRecorderFile(locked))
        val ask = OpenFileFlow.open(locked, "library.civilight-recorder", emptyList(), { 0 }, now, zone) as OpenStage.Password
        assertEquals("복구 암호", ask.sheet.field.label)
        val wrong = OpenFileFlow.unlock(locked, "틀린암호입니다".toCharArray(), ask.sheet, emptyList(), { 0 }, now, zone) as OpenStage.Password
        assertEquals(OpenFileCopy.WRONG_PASSWORD, wrong.error)
        val opened = OpenFileFlow.unlock(locked, "등불지기의새벽1".toCharArray(), ask.sheet, emptyList(), { 0 }, now, zone) as OpenStage.Recorder
        assertEquals(library.records.count { it.deletedAt == null }, opened.records)
    }
}
