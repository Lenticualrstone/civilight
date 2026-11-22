package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.backup.BackupFailure
import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.domain.library.importing.SameProjectChoice
import io.github.lenticualrstone.civilight.feature.library.backup.BackupBook
import io.github.lenticualrstone.civilight.feature.library.backup.BackupBooks
import io.github.lenticualrstone.civilight.feature.library.backup.BackupFacts
import io.github.lenticualrstone.civilight.feature.library.backup.BackupOutcome
import io.github.lenticualrstone.civilight.feature.library.backup.BackupProject
import io.github.lenticualrstone.civilight.feature.library.backup.BackupRunner
import io.github.lenticualrstone.civilight.feature.library.backup.BackupSchedule
import io.github.lenticualrstone.civilight.feature.library.backup.BackupSettingsState
import io.github.lenticualrstone.civilight.feature.library.backup.BackupSources
import io.github.lenticualrstone.civilight.feature.library.backup.DeviceKeyWrap
import io.github.lenticualrstone.civilight.feature.library.backup.DriveException
import io.github.lenticualrstone.civilight.feature.library.backup.DriveGateway
import io.github.lenticualrstone.civilight.feature.library.backup.DriveRunIndex
import io.github.lenticualrstone.civilight.feature.library.backup.DriveVersion
import io.github.lenticualrstone.civilight.feature.library.backup.RecoveryKeyring
import io.github.lenticualrstone.civilight.feature.library.backup.RecoveryPasswordState
import io.github.lenticualrstone.civilight.feature.library.backup.RestoreEnd
import io.github.lenticualrstone.civilight.feature.library.backup.RestoreModel
import io.github.lenticualrstone.civilight.feature.library.backup.RestoreResult
import io.github.lenticualrstone.civilight.feature.library.openfile.ImportOutcome
import io.github.lenticualrstone.civilight.feature.library.backup.RestorePicks
import io.github.lenticualrstone.civilight.feature.library.backup.RestoreRunner
import io.github.lenticualrstone.civilight.feature.library.backup.RestoreStatus
import io.github.lenticualrstone.civilight.feature.library.backup.RestoringModel
import io.github.lenticualrstone.civilight.feature.library.backup.WrongRecoveryPassword
import io.github.lenticualrstone.civilight.feature.library.onboarding.DriveConnect
import io.github.lenticualrstone.civilight.feature.library.openfile.ImportPorts
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.BackupKind
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.app.LockKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Base64

/**
 * Drive backup and restore (LIB-57 … LIB-60) over an in-memory Drive, book and stores; spec §4.7.
 * The real Drive client is a stub in this build (needsExternal): only this port is faked.
 */
class BackupTest {
    private val now = LibFx.NOW
    private val zone = LibFx.ZONE
    private val day = BackupSchedule.DAY_MS

    private class MemoryBook(start: BackupBook = BackupBook()) : BackupBooks {
        private val flow = MutableStateFlow(start)
        override val state: StateFlow<BackupBook> = flow
        override suspend fun ensureLoaded() = Unit
        override suspend fun update(transform: (BackupBook) -> BackupBook): BackupBook = transform(flow.value).also { flow.value = it }
        override suspend fun flush() = Unit
    }

    /** XOR "wrap" standing in for the Keystore. */
    private class TestWrap : DeviceKeyWrap {
        override val alias = "test"
        override fun wrap(dataKey: ByteArray): String = Base64.getEncoder().encodeToString(dataKey.map { (it.toInt() xor 0x5A).toByte() }.toByteArray())
        override fun unwrap(wrapped: String): ByteArray = Base64.getDecoder().decode(wrapped).map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
    }

    private class MemoryDrive(var connected: Boolean = true) : DriveGateway {
        val files = LinkedHashMap<String, MutableList<ByteArray>>()
        val deleted = mutableListOf<Pair<String, String>>()
        var failWith: BackupFailure? = null
        var index: DriveRunIndex? = null
        override suspend fun account(): String? = if (connected) "author@example" else null
        override suspend fun connect(): DriveConnect = DriveConnect.Connected("author@example").also { connected = true }
        override suspend fun upload(name: String, mimeType: String, bytes: ByteArray): DriveVersion {
            failWith?.let { throw DriveException(it) }
            val versions = files.getOrPut(name) { mutableListOf() }
            versions += bytes
            return DriveVersion(name, (versions.size - 1).toString(), bytes.size.toLong())
        }
        override suspend fun download(fileId: String, versionId: String, onBytes: (Long) -> Unit): ByteArray? =
            files[fileId]?.getOrNull(versionId.toInt())?.also { onBytes(it.size.toLong()) }
        override suspend fun delete(fileId: String, versionId: String) {
            deleted += fileId to versionId
        }
        override suspend fun readIndex(): DriveRunIndex? = index
        override suspend fun writeIndex(index: DriveRunIndex) {
            this.index = index
        }
    }

    private class Sources(var docs: List<ProjectDoc>, var settings: AppSettings, val library: RecorderLibrary) : BackupSources {
        /** The 서재 index time of each project (moves on every content edit). */
        val times = HashMap<Key, Long>()
        override suspend fun projects(): List<BackupProject> = docs.map { BackupProject(it, times[it.key] ?: 1L) }
        override fun blobs(): BlobReader = BlobReader.NONE
        override suspend fun recorder(): RecorderLibrary = library
        override fun audio(hash: String): ByteArray? = null
        override suspend fun settings(): AppSettings = settings
    }

    /** A fresh device's stores. */
    private class Device : ImportPorts {
        val entries = mutableListOf<LibraryEntry>()
        val added = mutableListOf<ProjectDoc>()
        var recorder = RecorderLibrary()
        override fun libraryEntries(): List<LibraryEntry> = entries
        override suspend fun importBlob(hash: String, bytes: ByteArray, mime: String, fileName: String) = Unit
        override suspend fun releaseProject(key: Key) = Unit
        override suspend fun moveAside(key: Key) = Unit
        override suspend fun addProject(doc: ProjectDoc) {
            added += doc
            entries += LibraryEntry(doc.key, doc.project.name, doc.project.rating, doc.project.protections.hidden)
        }
        override suspend fun updateRecorder(transform: (RecorderLibrary) -> RecorderLibrary) {
            recorder = transform(recorder)
        }
        override suspend fun removeRecentsOf(key: Key) = Unit
    }

    private var keys = 0
    private val newKey: () -> Key = { "01KBACK" + (++keys).toString().padStart(19, '0') }

    private val ashen = LibFx.ashen()
    private val glass = LibFx.glass()
    private val gaiden = LibFx.gaiden() // R-15, hidden at creation (step 4)
    private val on = AppSettings(driveBackup = true)

    private fun keyring(book: BackupBooks) = RecoveryKeyring(book, TestWrap(), iterations = 1_000)

    @Test
    fun `B-01 B-02 a hidden project without a 복구 암호 is left out and the screen says so`() = runTest {
        assertTrue(gaiden.project.protections.hidden)
        val book = MemoryBook()
        val drive = MemoryDrive()
        val sources = Sources(listOf(ashen, glass, gaiden), on, RecorderLibrary())
        val out = BackupRunner(sources, drive, book, { null }, "t").run(now, "run-1") as BackupOutcome.Done
        val projects = out.run.entries.filter { it.kind == BackupKind.PROJECT }
        assertEquals(setOf(ashen.key, glass.key), projects.mapNotNull { it.projectKey }.toSet())
        assertTrue(projects.none { it.encrypted })
        assertEquals(1, out.run.hiddenExcluded)
        assertTrue("one library file and one settings file", out.run.entries.count { it.kind == BackupKind.RECORDER } == 1 && out.run.entries.count { it.kind == BackupKind.SETTINGS } == 1)
        assertEquals(now, book.state.value.state.lastSuccessAt)
        assertEquals("names of visible projects only travel in the index", setOf(ashen.key, glass.key), drive.index!!.projectNames.keys)

        val ui = BackupSettingsState.build(on, book.state.value, BackupSettingsState.warnings(sources.docs, on, recoverySet = false), now, zone)
        assertEquals(listOf("숨긴 프로젝트 1 · 백업 안 됨"), ui.warnings.map { it.text })
        assertEquals("정하기", ui.recovery.value)
        assertTrue(ui.drive.sub!!.startsWith("마지막 백업 "))
    }

    @Test
    fun `B-03 with a 복구 암호 the hidden project goes up locked and never by name`() = runTest {
        val book = MemoryBook()
        val ring = keyring(book)
        ring.set("등불지기의새벽1".toCharArray(), now)
        val drive = MemoryDrive()
        val out = BackupRunner(Sources(listOf(ashen, gaiden), on, RecorderLibrary()), drive, book, ring::current, "t").run(now, "run-1") as BackupOutcome.Done
        val hidden = out.run.entries.single { it.projectKey == gaiden.key }
        assertTrue(hidden.encrypted)
        assertEquals(LockKind.RECOVERY, hidden.lockType)
        assertTrue(hidden.hidden)
        assertFalse(gaiden.key in drive.index!!.projectNames)
        val bytes = drive.files.getValue(hidden.driveFileId)[0]
        assertFalse("the name is not readable in the uploaded file", String(bytes, Charsets.ISO_8859_1).contains(gaiden.project.name))
    }

    @Test
    fun `B-04 an unchanged project reuses its version and a failed run starts the streak`() = runTest {
        val book = MemoryBook()
        val drive = MemoryDrive()
        val sources = Sources(listOf(ashen), on, RecorderLibrary())
        BackupRunner(sources, drive, book, { null }, "t").run(now, "run-1")
        val second = BackupRunner(sources, drive, book, { null }, "t").run(now + day, "run-2") as BackupOutcome.Done
        assertEquals(1, drive.files.getValue("${ashen.key}.civilight").size)
        assertEquals("0", second.run.entries.single { it.projectKey == ashen.key }.versionId)

        drive.failWith = BackupFailure.LOGIN_EXPIRED
        val failed = BackupRunner(sources, drive, book, { null }, "t").run(now + 2 * day, "run-3")
        assertTrue(failed is BackupOutcome.Failed)
        assertEquals(now + 2 * day, book.state.value.state.failureStreakStartedAt)
        assertEquals("loginExpired", book.state.value.state.lastFailureReason)
        assertEquals("the last success stays", now + day, book.state.value.state.lastSuccessAt)
    }

    @Test
    fun `LIB-24 지우기 of unencrypted versions drops them and the next run uploads again`() = runTest {
        val book = MemoryBook()
        val drive = MemoryDrive()
        val sources = Sources(listOf(ashen), on, RecorderLibrary())
        BackupRunner(sources, drive, book, { null }, "t").run(now, "run-1")
        val facts = BackupFacts(book.state)
        assertEquals(1, facts.unencryptedBackups(ashen.key))
        val gone = book.state.value.unencryptedEntries(ashen.key)
        book.update { it.withoutUnencrypted(ashen.key) }
        assertEquals(0, facts.unencryptedBackups(ashen.key))
        assertEquals(1, gone.size)
        BackupRunner(sources, drive, book, { null }, "t").run(now + day, "run-2")
        assertEquals("uploaded again", 2, drive.files.getValue("${ashen.key}.civilight").size)
    }

    @Test
    fun `B-04 an edit that only touches a block still uploads a new version`() = runTest {
        val book = MemoryBook()
        val drive = MemoryDrive()
        val sources = Sources(listOf(ashen), on, RecorderLibrary())
        BackupRunner(sources, drive, book, { null }, "t").run(now, "run-1")
        // A block edit: the document's own updatedAt stays, the 서재 index time moves.
        val block = ashen.manuscript.liveBlocks.first { it.runs.isNotEmpty() }
        sources.docs = listOf(ashen.copy(episodes = ashen.episodes.mapValues { (_, f) -> f.copy(blocks = f.blocks.map { if (it.key == block.key) it.copy(runs = emptyList()) else it }) }))
        sources.times[ashen.key] = now + 1000
        BackupRunner(sources, drive, book, { null }, "t").run(now + day, "run-2")
        assertEquals(2, drive.files.getValue("${ashen.key}.civilight").size)
    }

    @Test
    fun `nothing runs while the switch is off or no account is connected`() = runTest {
        val book = MemoryBook()
        assertEquals(BackupOutcome.NotRunning, BackupRunner(Sources(listOf(ashen), AppSettings(), RecorderLibrary()), MemoryDrive(), book, { null }, "t").run(now, "r"))
        assertEquals(BackupOutcome.NotRunning, BackupRunner(Sources(listOf(ashen), on, RecorderLibrary()), MemoryDrive(connected = false), book, { null }, "t").run(now, "r"))
        assertTrue(book.state.value.runs.isEmpty())
    }

    @Test
    fun `B-07 versions only older runs reference are deleted after 30 days`() = runTest {
        val book = MemoryBook()
        val drive = MemoryDrive()
        val sources = Sources(listOf(ashen), on, RecorderLibrary())
        BackupRunner(sources, drive, book, { null }, "t").run(now, "old")
        sources.times[ashen.key] = now + 40 * day
        BackupRunner(sources, drive, book, { null }, "t").run(now + 40 * day, "new")
        assertTrue(("${ashen.key}.civilight" to "0") in drive.deleted)
        assertEquals(listOf("new"), book.state.value.runs.map { it.runId })
    }

    @Test
    fun `LIB-59 a first-run restore ends onboarding only when something came back`() {
        val nothing = RestoreResult(emptyList(), failed = 2, recorderMerged = false)
        val some = RestoreResult(listOf(ImportOutcome(ashen.key, hidden = false, failedMessage = null)), failed = 0, recorderMerged = true)
        assertEquals(RestoreEnd.FINISH_ONBOARDING, RestoreEnd.of(some, firstRun = true))
        assertEquals(RestoreEnd.BACK, RestoreEnd.of(nothing, firstRun = true))
        assertEquals(RestoreEnd.BACK, RestoreEnd.of(null, firstRun = true))
        assertEquals(RestoreEnd.LIBRARY, RestoreEnd.of(null, firstRun = false))
        assertEquals(RestoreEnd.LIBRARY, RestoreEnd.of(some, firstRun = false))
    }

    @Test
    fun `the daily schedule waits a day after a success and an hour after a failure`() {
        assertTrue(BackupSchedule.due(BackupBook(), now))
        val ok = BackupBook(state = io.github.lenticualrstone.civilight.model.app.BackupState(lastSuccessAt = now))
        assertFalse(BackupSchedule.due(ok, now + day - 1))
        assertTrue(BackupSchedule.due(ok, now + day))
        val failed = ok.copy(runs = listOf(io.github.lenticualrstone.civilight.model.app.BackupRun("f", now + day, ok = false)))
        assertFalse(BackupSchedule.due(failed, now + day + 10))
        assertTrue(BackupSchedule.due(failed, now + day + BackupSchedule.RETRY_MS))
    }

    @Test
    fun `LIB-58 the 복구 암호 keeps material but never the password, and the facts follow it`() = runTest {
        val book = MemoryBook()
        val facts = BackupFacts(book.state)
        assertFalse(facts.recoverySet())
        val material = keyring(book).set("등불지기의새벽1".toCharArray(), now)
        assertTrue(facts.recoverySet())
        assertFalse(material.toString().contains("등불지기의새벽1"))
        assertNotNull(keyring(book).current())
        assertEquals(material, book.state.value.recovery)
        val ui = BackupSettingsState.build(on, book.state.value, emptyList(), now, zone)
        assertEquals("바꾸기", ui.recovery.value)
        assertEquals("숨긴 프로젝트 백업용 · 정해 둠", ui.recovery.sub)
    }

    @Test
    fun `LIB-58 강도 · 좋음 · 14자 and 정하기 needs 8 matching characters`() {
        val pw = "등불지기의새벽1잿빛하층구역".toCharArray()
        assertEquals(14, pw.size)
        val ui = RecoveryPasswordState.build(pw, "등불".toCharArray(), hiddenProjects = 2, visibleDocs = listOf(ashen))
        assertEquals("숨긴 프로젝트 2", ui.cardTitle)
        assertEquals(3, ui.strength!!.bars)
        assertEquals("강도 · 좋음 · 14자", ui.strength!!.line)
        assertNotNull(ui.mismatch)
        assertFalse(ui.canSet)
        assertTrue(RecoveryPasswordState.build(pw, pw.copyOf(), 2, emptyList()).canSet)
        assertFalse("below the minimum", RecoveryPasswordState.build("짧은암호".toCharArray(), "짧은암호".toCharArray(), 0, emptyList()).canSet)
        assertEquals("되살릴 때", ui.rows.last().label)
        assertEquals("이 암호 · 그 뒤 기기 지문", ui.rows.last().value)
    }

    @Test
    fun `LIB-59 LIB-60 a backup comes back on a new device with the 복구 암호, hidden stays hidden, records merge once`() = runTest {
        val book = MemoryBook()
        val ring = keyring(book)
        val password = "등불지기의새벽1"
        ring.set(password.toCharArray(), now)
        val recorder = ExampleRecorder.library()
        val drive = MemoryDrive()
        BackupRunner(Sources(listOf(ashen, gaiden), on.copy(), recorder), drive, book, ring::current, "t").run(now, "run-1")

        // A new device: no book, no key material; only Drive's index.
        val runs = RestoreModel.runs(emptyList(), drive.index, now + 1)
        assertEquals(1, runs.size)
        val ui = RestoreModel.build(true, runs, drive.index!!.projectNames, RestorePicks(), zone)
        assertTrue(ui.runs.single().selected)
        assertTrue(ui.runs.single().sub.startsWith("프로젝트 2 · 숨김 1 포함 · "))
        assertEquals(listOf("잿빛 새벽의 등불", "숨긴 프로젝트 1"), ui.projects.map { it.label })
        assertTrue(ui.button!!.endsWith(" 백업 되살리기"))
        val request = RestoreModel.request(runs.single(), RestorePicks())
        assertTrue(request.needsPassword)

        val device = Device()
        val runner = RestoreRunner(drive, device, newKey)
        try {
            runner.checkPassword(request, "틀린암호입니다".toCharArray())
            fail("a wrong 복구 암호 is refused before anything is imported")
        } catch (_: WrongRecoveryPassword) {
        }
        assertTrue(device.added.isEmpty())

        val statuses = mutableListOf<RestoreStatus>()
        val prefetched = runner.checkPassword(request, password.toCharArray())
        val result = runner.run(request, password.toCharArray(), now, zone, "11월 22일", prefetched) { statuses += it }
        assertEquals(0, result.failed)
        assertEquals(setOf(ashen.key, gaiden.key), device.added.map { it.key }.toSet())
        assertTrue("a recovery-locked hidden project stays hidden", device.added.single { it.key == gaiden.key }.project.protections.hidden)
        assertFalse("a visible one stays visible", device.added.single { it.key == ashen.key }.project.protections.hidden)
        assertTrue(statuses.last().finished)
        assertEquals(1f, RestoringModel.of(statuses.last()).fraction)
        assertEquals("Google Drive · 11월 22일 백업", RestoringModel.of(statuses.last()).section)
        val records = device.recorder.records.size
        assertTrue(records > 0)

        // Restoring the same run again adds no duplicate record (merge by key, R23 §6).
        runner.run(request.copy(choice = SameProjectChoice.AS_NEW_PROJECT), password.toCharArray(), now, zone, "11월 22일") {}
        assertEquals(records, device.recorder.records.size)
    }

    @Test
    fun `LIB-59 unchecking the hidden row restores without asking for the 복구 암호`() = runTest {
        val book = MemoryBook()
        val ring = keyring(book)
        ring.set("등불지기의새벽1".toCharArray(), now)
        val drive = MemoryDrive()
        BackupRunner(Sources(listOf(ashen, gaiden), on, RecorderLibrary()), drive, book, ring::current, "t").run(now, "run-1")
        val run = RestoreModel.runs(book.state.value.runs, null, now).single()
        val hiddenRow = RestoreModel.build(true, listOf(run), emptyMap(), RestorePicks(), zone).projects.single { it.key == null }
        val picks = RestoreModel.toggle(RestorePicks(), hiddenRow)
        val request = RestoreModel.request(run, picks)
        assertEquals(listOf(ashen.key), request.projects.map { it.projectKey })
        assertFalse("the library file has no hidden records here", request.needsPassword)
        val device = Device()
        RestoreRunner(drive, device, newKey).run(request, null, now, zone, "11월 22일") {}
        assertEquals(listOf(ashen.key), device.added.map { it.key })
        assertNull(RestoreModel.build(false, emptyList(), emptyMap(), RestorePicks(), zone).button)
    }
}
