package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.domain.library.crypto.RecoveryKeys
import io.github.lenticualrstone.civilight.domain.library.export.ExportRow
import io.github.lenticualrstone.civilight.domain.library.export.ExportSheets
import io.github.lenticualrstone.civilight.domain.library.format.ProjectFileOptions
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageWriter
import io.github.lenticualrstone.civilight.domain.library.format.RecorderPackage
import io.github.lenticualrstone.civilight.domain.library.format.RecorderPackageOpen
import io.github.lenticualrstone.civilight.domain.recorder.export.RecorderExport
import io.github.lenticualrstone.civilight.domain.recorder.hooks.RecorderHooks
import io.github.lenticualrstone.civilight.domain.recorder.hooks.TargetLines
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.feature.recorder.files.RecorderBackupPlan
import io.github.lenticualrstone.civilight.feature.recorder.files.RecorderFilesPort
import io.github.lenticualrstone.civilight.feature.recorder.files.RecorderLibraryFiles
import io.github.lenticualrstone.civilight.feature.recorder.files.RecorderRestore
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.app.ExportOptions
import io.github.lenticualrstone.civilight.model.recorder.RecorderAudio
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderSettings
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * The recorder side of export and backup (LRC-45, LRC-46) checked against the binding write paths
 * (`domain:library` `ExportSheets`, `ProjectPackageWriter`, `RecorderPackage`, `RecorderMerge`), and
 * the hooks other areas draw (LRC-47) against this module's own screens.
 */
class FilesAndHooksTest {
    private val r1 = ExampleKeys.RECORDER_R1

    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    /** The example library with R1 hidden and carrying kept audio (ADTS AAC, as `MediaRecorderTakes` writes it). */
    private fun library(audio: ByteArray): RecorderLibrary {
        val lib = ExampleRecorder.library()
        return lib.copy(
            records = lib.records.map {
                if (it.key == r1) it.copy(hidden = true, audio = RecorderAudio(hash = sha(audio), mime = "audio/aac", bytes = audio.size.toLong(), durationMs = 24_000)) else it
            },
        )
    }

    /** Like the store: empty until [ensureLoaded] reads [stored]. */
    private class FakeFiles(private val stored: RecorderLibrary, val blobs: MutableMap<String, ByteArray> = HashMap()) : RecorderFilesPort {
        var library = RecorderLibrary()
        var loads = 0

        override suspend fun ensureLoaded() {
            if (loads++ == 0) library = stored
        }

        override fun current() = library

        override suspend fun update(transform: (RecorderLibrary) -> RecorderLibrary): RecorderLibrary = transform(library).also { library = it }

        override fun audioBytes(hash: String) = blobs[hash]

        override suspend fun importAudio(bytes: ByteArray, mime: String, durationMs: Long?) {
            blobs[MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }] = bytes
        }
    }

    // ---- LRC-45 ----

    @Test
    fun LRC_T43_fixture_exportRowAndFileAgreeWithTheRecorderRule() {
        val audio = ByteArray(64) { 0x55 }
        val lib = library(audio)
        val doc = Fx.p1()
        val env = Fx.env(doc)
        // This area's numbers (LRC-45) and the export sheet's row (LIB-41 draws it) agree.
        val ours = RecorderExport.row(lib, env, Fx.P1, encrypted = false)
        val sheet = ExportSheets.build(doc, ExportOptions(format = ExportFormat.PROJECT_FILE), lib)
        val row = sheet.rows.filterIsInstance<ExportRow.RecorderRecords>().single()
        assertEquals(ours.linked, row.linked)
        assertEquals(ours.hidden, row.hiddenExcluded)
        assertEquals(1, row.hiddenExcluded)
        assertEquals("레코더 기록 ${ours.linked} · 원본 음성 포함 · 숨긴 기록 1 제외", row.text)
        assertFalse(row.includeAudio)

        // The written file: the hidden record left out, no audio unless 원본 음성 포함.
        val plain = ProjectPackageWriter.content(doc, lib, { b -> if (b.hash == sha(audio)) audio else null }, ProjectFileOptions(), "test", Fx.NOW)
        assertEquals(ours.linked - 1, plain.recorderRecordKeys.size)
        assertFalse(r1 in plain.recorderRecordKeys)
        assertTrue(plain.files.keys.none { it.endsWith(".aac") })
    }

    @Test
    fun LRC_T44_aHiddenProjectsEncryptedFileCarriesItsHiddenRecordsWithAudio() {
        val audio = ByteArray(64) { 0x33 }
        val lib = library(audio)
        val doc = Fx.p1().let { it.copy(project = it.project.copy(protections = it.project.protections.copy(hidden = true))) }
        val written = ProjectPackageWriter.content(
            doc, lib, { b -> if (b.hash == sha(audio)) audio else null },
            ProjectFileOptions(includeRecorderAudio = true, encrypted = true), "test", Fx.NOW,
        )
        assertTrue(r1 in written.recorderRecordKeys)
        assertEquals(0, written.excludedHiddenRecords)
        // The ADTS take is written as `assets/<hash>.aac` and the reader takes it back as a blob path.
        assertTrue(written.files.containsKey("assets/${sha(audio)}.aac"))
    }

    // ---- LRC-46 ----

    @Test
    fun LRC_46_backupPlanFollowsTheSettings() {
        assertEquals(RecorderBackupPlan(includeHidden = true, includeAudio = true), RecorderBackupPlan.of(AppSettings(), recoverySet = true))
        assertEquals(RecorderBackupPlan(includeHidden = false, includeAudio = true), RecorderBackupPlan.of(AppSettings(), recoverySet = false))
        assertFalse(RecorderBackupPlan.of(AppSettings(backupHidden = false), recoverySet = true).includeHidden)
        assertFalse(RecorderBackupPlan.of(AppSettings(recorder = RecorderSettings(keepAudio = false)), recoverySet = true).includeAudio)
    }

    @Test
    fun LRC_46_withoutARecoveryKeyHiddenAndTrashedRecordsStayOut() = runTest {
        val audio = ByteArray(32) { 0x11 }
        val lib = library(audio).let { l -> l.copy(records = l.records.map { if (it.key == ExampleKeys.RECORDER_R2) it.copy(deletedAt = Fx.NOW) else it }) }
        val files = RecorderLibraryFiles(FakeFiles(lib, hashMapOf(sha(audio) to audio)), { Fx.NOW }, "test", Fx.keys(500))
        val bytes = files.write(RecorderBackupPlan(includeHidden = true, includeAudio = true), recovery = null)
        val content = (RecorderPackage.read(bytes) as RecorderPackageOpen.Ready).content
        val keys = content.library.records.map { it.key }
        assertFalse(r1 in keys)
        assertFalse(ExampleKeys.RECORDER_R2 in keys)
        assertEquals(lib.records.count { !it.hidden && it.deletedAt == null }, keys.size)
        assertTrue(content.library.links.all { it.recordKey in keys })
    }

    @Test
    fun LRC_46_theLibraryIsLoadedBeforeItIsWritten() = runTest {
        val port = FakeFiles(ExampleRecorder.library())
        assertTrue(port.current().records.isEmpty())
        val bytes = RecorderLibraryFiles(port, { Fx.NOW }, "test", Fx.keys(500)).write(RecorderBackupPlan(includeHidden = false, includeAudio = false), null)
        val content = (RecorderPackage.read(bytes) as RecorderPackageOpen.Ready).content
        assertEquals(ExampleRecorder.library().records.count { !it.hidden && it.deletedAt == null }, content.library.records.size)
        assertTrue(content.library.records.isNotEmpty())
    }

    @Test
    fun LRC_46_encryptedRoundTripRestoresHiddenRecordsAndAudioByMerging() = runTest {
        val audio = ByteArray(48) { (it * 7).toByte() }
        val lib = library(audio)
        val recovery = RecoveryKeys.create("복구 암호 예시".toCharArray(), iterations = 1_000, random = SecureRandom())
        val source = RecorderLibraryFiles(FakeFiles(lib, hashMapOf(sha(audio) to audio)), { Fx.NOW }, "test", Fx.keys(500))
        val bytes = source.write(RecorderBackupPlan(includeHidden = true, includeAudio = true), recovery)
        assertTrue(RecorderPackage.read(bytes) is RecorderPackageOpen.Encrypted)

        // A device with R1 already (local text kept) and nothing else.
        val localR1 = lib.record(r1)!!.copy(runs = listOf(io.github.lenticualrstone.civilight.model.text.TextRun.Plain("이 기기에서 고친 글")), audio = null)
        val target = FakeFiles(RecorderLibrary(listOf(localR1), emptyList(), emptyList()))
        val restore = RecorderLibraryFiles(target, { Fx.NOW }, "test", Fx.keys(700))
        assertEquals(RecorderRestore.NeedsRecovery, restore.restore(bytes))
        assertEquals(1, target.library.records.size)

        val merged = restore.restore(bytes, recovery.dataKey) as RecorderRestore.Merged
        assertEquals(lib.records.count { it.deletedAt == null } - 1, merged.added)
        assertEquals(1, merged.audio)
        assertEquals(audio.toList(), target.blobs.getValue(sha(audio)).toList())
        // The local record keeps its text and gains its links and placements.
        assertEquals("이 기기에서 고친 글", io.github.lenticualrstone.civilight.domain.recorder.text.Runs.text(target.library.record(r1)!!.runs))
        assertEquals(lib.linksOf(r1).size, target.library.linksOf(r1).size)
        // Restoring the same file again changes nothing (merge by key).
        val again = restore.restore(bytes, recovery.dataKey) as RecorderRestore.Merged
        assertEquals(0, again.added)
        assertEquals(lib.links.size, target.library.links.size)
    }

    @Test
    fun LRC_46_notARecorderFileChangesNothing() = runTest {
        val target = FakeFiles(ExampleRecorder.library())
        val restore = RecorderLibraryFiles(target, { Fx.NOW }, "test", Fx.keys(700))
        assertTrue(restore.restore(ByteArray(10)) is RecorderRestore.Unreadable)
        assertEquals(ExampleRecorder.library(), target.library)
    }

    // ---- LRC-47 ----

    @Test
    fun LRC_47_libraryRowMatchesTheUnlockedInboxAndTheTargetLineItsPreset() = runTest {
        val f = InboxFixture(this)
        val v = f.view(this)
        val counts = RecorderHooks.libraryCounts(f.recorder.library, f.env.env)
        assertEquals("전체 ${counts.records}", v.screen.filters[0].label)
        assertEquals("정리 안 됨 ${counts.unsorted}", v.screen.filters[1].label)
        assertEquals("기록 6 · 정리 안 됨 4", counts.text)

        // `레코더 · 정리 안 됨 n ›` on 1-7 씬 3 and the inbox it opens list the same records.
        val scene = TargetRef(Fx.P1, RecorderTargetKind.UNIT, Fx.S)
        val n = TargetLines.unsortedLinkedCount(f.recorder.library, f.projects.doc, scene, recordsOpen = false)
        assertEquals("레코더 · 정리 안 됨 $n ›", TargetLines.line(f.recorder.library, f.projects.doc, scene, recordsOpen = false))
        val preset = InboxFixture(this, preset = io.github.lenticualrstone.civilight.feature.recorder.inbox.InboxRoutes.preset(
            io.github.lenticualrstone.civilight.navigation.RecorderInbox(projectKey = Fx.P1, targetKind = "scene", targetKey = Fx.S),
        ))
        assertEquals(n, preset.view(this).rows.size)

        // The checklist line (LIB-13) and the rating sheet number (LIB-24) of P1.
        assertEquals(RecorderHooks.linkedRecordCount(f.recorder.library, Fx.P1), f.recorder.library.records.count { r -> f.recorder.library.linksOf(r.key).any { it.projectKey == Fx.P1 } && r.deletedAt == null })
        assertNull(RecorderHooks.checklistLine(RecorderLibrary(), Fx.P1, f.env.env))
    }
}
