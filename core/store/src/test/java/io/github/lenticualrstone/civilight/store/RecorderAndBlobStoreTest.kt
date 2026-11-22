package io.github.lenticualrstone.civilight.store

import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.EntryContext
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.Placement
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderAudio
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.recorder.RecordingSession
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.store.blob.BlobReferences
import io.github.lenticualrstone.civilight.store.blob.BlobStore
import io.github.lenticualrstone.civilight.store.blob.MimeTypes
import io.github.lenticualrstone.civilight.store.recorder.RecorderStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RecorderAndBlobStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val clock = ManualClock.at("2026-11-22T00:00:00Z")

    private fun TestScope.blobs() = BlobStore(File(tmp.root, "blobs"), clock, UnconfinedTestDispatcher(testScheduler))

    private fun TestScope.recorder(blobs: BlobStore = blobs()) = RecorderStore(File(tmp.root, "recorder"), testEnv(clock), blobs)

    private fun library(audioHash: String) = RecorderLibrary(
        records = listOf(
            RecorderRecord(
                key = "REC1", kind = RecordKind.VOICE.open(), createdAt = clock.now(),
                runs = listOf(TextRun.Plain("하르트는 "), TextRun.Link("LK1", LinkTarget(LinkTargetKind.CHARACTER, "C1", projectKey = "P1"), "리엔"), TextRun.Plain("을 안다")),
                durationMs = 12_000, audio = RecorderAudio(hash = audioHash, mime = "audio/mp4", bytes = 5, durationMs = 12_000, envelope = RecorderAudio.encodeEnvelope(byteArrayOf(1, 5, 9))),
                updatedBy = "DEV-TEST",
            ),
            RecorderRecord(key = "REC2", kind = RecordKind.TEXT.open(), createdAt = clock.now() + 1, runs = listOf(TextRun.Plain("메모"))),
        ),
        links = listOf(
            RecorderLink(key = "L1", recordKey = "REC1", projectKey = "P1", targetKind = RecorderTargetKind.CHARACTER.open(), targetKey = "C1",
                origin = LinkOrigin.AT.open(), createdAt = clock.now()),
            RecorderLink(key = "L2", recordKey = "REC1", projectKey = "P2", targetKind = RecorderTargetKind.PROJECT.open(), targetKey = "P2",
                origin = LinkOrigin.MANUAL.open(), createdAt = clock.now()),
        ),
        placements = listOf(
            Placement(key = "PLC1", recordKey = "REC1", projectKey = "P1", kind = PlacementKind.SCRIPT_BLOCK.open(), targetKey = "B9",
                containerKey = "S1", copiedRevision = 1, createdAt = clock.now(), lastLabel = "1-7 씬 3"),
        ),
    )

    @Test
    fun recorderLibraryRoundTrip() = runTest {
        val blobs = blobs()
        val store = recorder(blobs)
        val audio = store.importAudio("음성 바이트".toByteArray().inputStream(), "audio/mp4", 12_000)
        assertEquals(BlobSource.RECORDER.open(), audio.source)
        val lib = library(audio.hash)
        store.updateLibrary { lib }
        store.flush()
        assertTrue(File(tmp.root, "recorder/records.json").isFile)

        val again = recorder(blobs)
        again.ensureLoaded()
        assertEquals(lib, again.state.value.library)
        assertArrayEquals(byteArrayOf(1, 5, 9), again.state.value.library.record("REC1")!!.audio!!.envelopeBytes())
        assertArrayEquals("음성 바이트".toByteArray(), again.audioFile(audio.hash)!!.readBytes())
    }

    @Test
    fun recorderSavesAreDebounced() = runTest {
        val store = recorder()
        store.updateLibrary { library("a".repeat(64)) }
        advanceTimeBy(599)
        runCurrent()
        assertFalse(File(tmp.root, "recorder/records.json").exists())
        advanceTimeBy(2)
        runCurrent()
        assertTrue(File(tmp.root, "recorder/records.json").exists())
    }

    @Test
    fun recorderTrashRestorePurgeAndProjectPurge() = runTest {
        val store = recorder()
        store.updateLibrary { library("a".repeat(64)) }
        val row = store.moveToTrash("REC2")
        assertNull(row.projectKey)
        assertEquals(LocalDate.of(2026, 12, 22), row.purgeOn)
        assertNotNull(store.state.value.library.record("REC2")!!.deletedAt)
        store.restore("REC2")
        assertNull(store.state.value.library.record("REC2")!!.deletedAt)
        assertTrue(store.state.value.trash.entries.isEmpty())

        store.markProjectPurged("P2", "유리 정원", hidden = false)
        val l2 = store.state.value.library.links.single { it.key == "L2" }
        assertEquals(clock.now(), l2.purgedAt)
        assertEquals("유리 정원", l2.lastLabel)
        assertNull(store.state.value.library.links.single { it.key == "L1" }.purgedAt)

        store.removeProjectLinks("P1")
        assertEquals(listOf("L2"), store.state.value.library.links.map { it.key })
        assertTrue(store.state.value.library.placements.isEmpty())

        store.moveToTrash("REC1")
        store.flush()
        // Trashed records stay in the store file with their mark.
        val reread = recorder()
        reread.ensureLoaded()
        assertNotNull(reread.state.value.library.record("REC1")!!.deletedAt)
        assertEquals(1, reread.state.value.trash.entries.size)
        reread.purgeExpired(LocalDate.of(2026, 12, 22))
        assertNull(reread.state.value.library.record("REC1"))
        assertTrue(reread.state.value.library.links.none { it.recordKey == "REC1" })
    }

    @Test
    fun recordingSessionIsKeptForCrashRecovery() = runTest {
        val store = recorder()
        val session = RecordingSession(recordKey = "REC9", entry = EntryContext(surface = EntrySurface.SCRIPT, projectKey = "P1"), elapsedMs = 4_000, audioTempPath = "rec/REC9.m4a")
        store.saveSession(session)
        assertEquals(session, recorder().readSession())
        store.saveSession(null)
        assertNull(recorder().readSession())
    }

    @Test
    fun aDamagedRecordsFileIsSetAside() = runTest {
        File(tmp.root, "recorder").mkdirs()
        File(tmp.root, "recorder/records.json").writeText("[oops")
        val store = recorder()
        store.ensureLoaded()
        assertEquals(setOf(RecorderStore.RECORDS), store.damaged.keys)
        assertTrue(File(tmp.root, "recorder").listFiles()!!.any { it.name.startsWith("records.json.damaged-") })
    }

    // ---- blobs ----

    @Test
    fun blobsAreContentAddressed() = runTest {
        val blobs = blobs()
        val a = blobs.import("abc".toByteArray().inputStream(), "image/png", BlobSource.GALLERY)
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", a.hash)
        assertEquals("png", a.fileExt)
        assertEquals(3L, a.bytes)
        assertEquals(clock.now(), a.importedAt)
        val b = blobs.import("abc".toByteArray().inputStream(), "image/png", BlobSource.FILE)
        assertEquals(a.hash, b.hash)
        assertEquals(listOf("${a.hash}.png"), File(tmp.root, "blobs").list()!!.toList())
        assertEquals("abc", blobs.fileOf(a.hash)!!.readText())
        // A new store instance finds it on disk.
        assertTrue(blobs().contains(a.hash))
    }

    @Test
    fun garbageCollectionKeepsReferencedAndRecentBlobs() = runTest {
        val blobs = blobs()
        val used = blobs.import("used".toByteArray().inputStream(), "image/png", BlobSource.GALLERY)
        val unused = blobs.import("unused".toByteArray().inputStream(), "image/jpeg", BlobSource.GALLERY)
        val fresh = blobs.import("fresh".toByteArray().inputStream(), "image/jpeg", BlobSource.GALLERY)
        val dayAgo = clock.now() - 2 * 24 * 3600 * 1000L
        blobs.fileOf(used)!!.setLastModified(dayAgo)
        blobs.fileOf(unused)!!.setLastModified(dayAgo)
        blobs.fileOf(fresh)!!.setLastModified(clock.now())
        val deleted = blobs.collectGarbage(setOf(used.hash), graceMs = 24 * 3600 * 1000L)
        assertEquals(listOf(unused.hash), deleted)
        assertTrue(blobs.contains(used.hash))
        assertTrue(blobs.contains(fresh.hash))
        assertFalse(blobs.contains(unused.hash))
    }

    @Test
    fun importingStoredBytesAgainRestartsTheGracePeriod() = runTest {
        val blobs = blobs()
        val first = blobs.import("again".toByteArray().inputStream(), "image/png", BlobSource.GALLERY)
        blobs.fileOf(first)!!.setLastModified(clock.now() - 3 * 24 * 3600 * 1000L)
        blobs.import("again".toByteArray().inputStream(), "image/png", BlobSource.GALLERY)
        assertTrue(blobs.collectGarbage(emptySet(), graceMs = 24 * 3600 * 1000L).isEmpty())
        assertTrue(blobs.contains(first.hash))
    }

    @Test
    fun blobReferencesCoverEveryHolder() {
        val refs = BlobReferences.of(Fx.richDoc())
        assertEquals(setOf(Fx.HASH_PORTRAIT, Fx.HASH_MAP, Fx.HASH_SKETCH, Fx.HASH_BGM), refs)
        assertEquals(setOf("a".repeat(64)), BlobReferences.of(library("a".repeat(64))))
        val counts = BlobReferences.counts(listOf(Fx.richDoc(), Fx.richDoc()), library(Fx.HASH_MAP))
        assertEquals(3, counts[Fx.HASH_MAP])
    }

    @Test
    fun mimeTypes() {
        assertEquals("jpg", MimeTypes.extensionFor("image/jpeg"))
        assertEquals("m4a", MimeTypes.extensionFor("audio/mp4; codecs=mp4a"))
        assertEquals("tiff", MimeTypes.extensionFor("application/octet-stream", "scan.TIFF"))
        assertEquals("bin", MimeTypes.extensionFor("application/octet-stream", "../../x"))
    }
}
