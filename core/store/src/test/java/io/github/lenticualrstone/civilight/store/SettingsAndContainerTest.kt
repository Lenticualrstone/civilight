package io.github.lenticualrstone.civilight.store

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.lenticualrstone.civilight.model.app.AnimationPref
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.RecentItem
import io.github.lenticualrstone.civilight.model.app.RecentKind
import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import io.github.lenticualrstone.civilight.model.app.ThemeMode
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderLink
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderTranscription
import io.github.lenticualrstone.civilight.store.project.ProjectStore
import io.github.lenticualrstone.civilight.store.settings.AppSettingsCodec
import io.github.lenticualrstone.civilight.store.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsAndContainerTest {
    @get:Rule val tmp = TemporaryFolder()

    private val custom = AppSettings(
        theme = ThemeMode.DARK, textSizeStep = 2, animation = AnimationPref.ALWAYS_REDUCE, appLock = true,
        relockPolicy = RelockPolicy.FIVE_MINUTES, viewByScreen = mapOf("library" to ViewMode.CARD, "cast" to ViewMode.LIST),
        driveAccountId = "acct", recorder = AppSettings().recorder.copy(transcription = RecorderTranscription.CLOUD, keepAudio = false),
        ratingDefaults = AppSettings().ratingDefaults.copy(r15Capture = true, changedGroups = setOf("r15")),
    )

    @Test
    fun settingsCodecRoundTrip() {
        assertEquals(custom, AppSettingsCodec.decode(AppSettingsCodec.encode(custom)))
        assertEquals(AppSettings(), AppSettingsCodec.decode(null))
        assertEquals(AppSettings(), AppSettingsCodec.decode("{ broken"))
        // Unknown keys from a newer version and unknown enum values fall back without losing the rest.
        val newer = AppSettingsCodec.decode("""{"theme":"dark","futureSwitch":true,"relockPolicy":"whenever"}""")
        assertEquals(ThemeMode.DARK, newer.theme)
        assertEquals(RelockPolicy.ON_LEAVE, newer.relockPolicy)
    }

    @Test
    fun settingsStoreKeepsValuesInDataStore() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = File(tmp.root, "settings.preferences_pb")
        try {
            val store = SettingsStore(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))
            assertEquals(AppSettings(), store.current())
            store.setTheme(ThemeMode.LIGHT)
            store.setViewMode("library", ViewMode.CARD)
            store.updateRecorder { it.copy(lockScreenRecording = false) }
            val now = store.current()
            assertEquals(ThemeMode.LIGHT, now.theme)
            assertEquals(ViewMode.CARD, now.viewByScreen["library"])
            assertFalse(now.recorder.lockScreenRecording)
            assertTrue(file.isFile)
        } finally {
            scope.cancel()
        }
        // A second DataStore on the same file (as after a restart) reads the same values.
        val scope2 = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val again = SettingsStore(PreferenceDataStoreFactory.create(scope = scope2, produceFile = { file }))
            assertEquals(ThemeMode.LIGHT, again.current().theme)
        } finally {
            scope2.cancel()
        }
    }

    @Test
    fun deviceIdIsMadeOnceAndKept() {
        val file = File(tmp.root, "device-id")
        val first = DeviceId(file).value
        assertEquals(26, first.length)
        assertEquals(first, DeviceId(file).value)
        assertEquals(first, file.readText())
    }

    @Test
    fun containerPurgeMarksRecorderLinksAndCollectsBlobs() = runTest {
        val dataScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val clock = ManualClock.at("2026-11-22T00:00:00Z")
            val env = testEnv(clock, StoreTiming(blobGraceMs = 0))
            val container = StoreContainer(
                tmp.root,
                PreferenceDataStoreFactory.create(scope = dataScope, produceFile = { File(tmp.root, "datastore/settings.preferences_pb") }),
                env,
            )
            container.start()

            val portrait = container.blobs.import("portrait".toByteArray().inputStream(), "image/png", BlobSource.GALLERY)
            val orphan = container.blobs.import("orphan".toByteArray().inputStream(), "image/png", BlobSource.GALLERY)
            for (blob in listOf(portrait, orphan)) container.blobs.fileOf(blob)!!.setLastModified(clock.now() - 1_000)
            val keep = container.library.create("잿빛 새벽의 등불")
            val gone = container.library.create("유리 정원") { doc ->
                doc.copy(assets = doc.assets.copy(imageUses = listOf(ImageUse(key = "IU", ownerKind = ImageOwnerKind.PROJECT.open(),
                    ownerKey = doc.key, slot = ImageSlot.PROJECT_COVER.open(), source = portrait.hash))))
            }
            container.recorder.updateLibrary { lib ->
                lib.copy(
                    records = listOf(RecorderRecord(key = "REC1", kind = RecordKind.TEXT.open(), createdAt = clock.now())),
                    links = listOf(RecorderLink(key = "L1", recordKey = "REC1", projectKey = gone.key, targetKind = RecorderTargetKind.PROJECT.open(),
                        targetKey = gone.key, origin = LinkOrigin.MANUAL.open(), createdAt = clock.now())),
                )
            }
            container.appState.recordRecent(RecentItem(key = "RI1", projectKey = gone.key, kind = RecentKind.SCENE, targetKey = "S1", openedAt = clock.now()))
            container.appState.recordRecent(RecentItem(key = "RI2", projectKey = keep.key, kind = RecentKind.SCENE, targetKey = "S1", openedAt = clock.now()))

            // While the project is only in the trash its cover blob is still referenced.
            container.library.moveToTrash(gone.key)
            assertEquals(listOf(orphan.hash), container.collectBlobGarbage())
            assertTrue(container.blobs.contains(portrait.hash))

            container.purgeProject(gone.key)
            val link = container.recorder.state.value.library.links.single()
            assertNotNull(link.purgedAt)
            assertEquals("유리 정원", link.lastLabel)
            assertEquals(listOf("RI2"), container.appState.recents.state.value.items.map { it.key })
            assertFalse(container.blobs.contains(portrait.hash))
            assertEquals(listOf(keep.key), container.library.state.value.entries.map { it.key })

            container.flushAll()
            assertTrue(File(tmp.root, "recorder/records.json").isFile)
            assertTrue(File(tmp.root, "app/recents.json").isFile)
            assertTrue(File(tmp.root, "library.json").isFile)
        } finally {
            dataScope.cancel()
        }
    }

    @Test
    fun aProjectWithADamagedFileHoldsBlobCollection() = runTest {
        val dataScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val clock = ManualClock.at("2026-11-22T00:00:00Z")
            val container = StoreContainer(
                tmp.root,
                PreferenceDataStoreFactory.create(scope = dataScope, produceFile = { File(tmp.root, "datastore/settings.preferences_pb") }),
                testEnv(clock, StoreTiming(blobGraceMs = 0)),
            )
            val cover = container.blobs.import("cover".toByteArray().inputStream(), "image/png", BlobSource.GALLERY)
            container.blobs.fileOf(cover)!!.setLastModified(clock.now() - 1_000)
            val entry = container.library.create("유리 정원") { doc ->
                doc.copy(assets = doc.assets.copy(imageUses = listOf(ImageUse(key = "IU", ownerKind = ImageOwnerKind.PROJECT.open(),
                    ownerKey = doc.key, slot = ImageSlot.PROJECT_COVER.open(), source = cover.hash))))
            }
            File(container.library.projectDir(entry.key), "assets/index.json").writeText("{ damaged")
            assertTrue(container.collectBlobGarbage().isEmpty())
            // Opening sets the file aside; the set-aside copy still holds collection.
            container.library.open(entry.key)
            container.library.close(entry.key)
            assertTrue(container.collectBlobGarbage().isEmpty())
            assertTrue(container.blobs.contains(cover.hash))
        } finally {
            dataScope.cancel()
        }
    }

    @Test
    fun productionDispatchersSaveOnFlushBlockingAndByTimer() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val env = StoreEnvironment(scope, Dispatchers.IO, AppClock.System, DeviceId.fixed("DEV-REAL"))
            val dir = File(tmp.root, "projects/${Fx.P}")
            ProjectStore.writeNew(Fx.richDoc(), dir)
            val store = runBlocking { ProjectStore.open(Fx.P, dir, env) }
            fun nameOnDisk() = ProjectStore.read(Fx.P, dir, env).cast.characters.first { it.key == "C1" }.name

            runBlocking { store.update("이름") { d -> d.copy(cast = d.cast.copy(characters = d.cast.characters.map { if (it.key == "C1") it.copy(name = "하나") else it })) } }
            store.flushBlocking()
            assertEquals("하나", nameOnDisk())

            runBlocking { store.update("이름") { d -> d.copy(cast = d.cast.copy(characters = d.cast.characters.map { if (it.key == "C1") it.copy(name = "둘") else it })) } }
            val deadline = System.currentTimeMillis() + 5_000
            while (nameOnDisk() != "둘" && System.currentTimeMillis() < deadline) Thread.sleep(50)
            assertEquals("둘", nameOnDisk())
            runBlocking { store.close() }
        } finally {
            scope.cancel()
        }
    }
}
