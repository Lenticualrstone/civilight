package io.github.lenticualrstone.civilight.store

import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.world.Place
import io.github.lenticualrstone.civilight.store.io.AtomicFiles
import io.github.lenticualrstone.civilight.store.project.ProjectLayout
import io.github.lenticualrstone.civilight.store.project.ProjectLoader
import io.github.lenticualrstone.civilight.store.project.ProjectStore
import io.github.lenticualrstone.civilight.store.project.SaveState
import io.github.lenticualrstone.civilight.store.version.NewerStoreVersionException
import io.github.lenticualrstone.civilight.store.version.StoreUpgradeChain
import io.github.lenticualrstone.civilight.store.version.StoreUpgradeStep
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun newDir(doc: ProjectDoc = Fx.richDoc()): File {
        val dir = File(tmp.root, "projects/${doc.key}")
        ProjectStore.writeNew(doc, dir)
        return dir
    }

    private suspend fun TestScope.openStore(dir: File, env: StoreEnvironment = testEnv(), readOnly: Boolean = false): Pair<ProjectStore, WriteLog> {
        val store = ProjectStore.open(Fx.P, dir, env, readOnly)
        val log = WriteLog(dir)
        store.storeDirectory.faults = log
        return store to log
    }

    private fun ProjectDoc.renameCharacter(key: String, name: String) =
        copy(cast = cast.copy(characters = cast.characters.map { if (it.key == key) it.copy(name = name) else it }))

    private fun diskJson(dir: File, path: String): JsonElement =
        CivilightJson.json.parseToJsonElement(File(dir, path).readText())

    // ---- round trip ----

    @Test
    fun everyFileUnitSurvivesSaveAndLoad() = runTest {
        val doc = Fx.richDoc()
        val dir = newDir(doc)
        val (store, _) = openStore(dir)
        assertEquals(Fx.normalized(doc), Fx.normalized(store.doc.value))
        assertEquals(emptyMap<String, String>(), store.loadReport.damaged)
        // Live and trashed rows went to their files (C-14).
        val files = dir.walk().filter { it.isFile }.map { it.relativeTo(dir).invariantSeparatorsPath }.toSet()
        for (path in listOf(
            "project.json", "story/index.json", "story/E1.json", "cast.json", "world.json", "changes.json", "maps/MAP1.json",
            "canvas/CV1.json", "pages/PG1.json", "assets/index.json", "checks.json", "tombstones.json", "deletions.json",
            "trash/index.json", "trash/project.json", "trash/story/index.json", "trash/story/E1.json", "trash/story/E2.json",
            "trash/cast.json", "trash/world.json", "trash/changes.json", "trash/maps/MAP1.json", "trash/pages/PG2.json",
            "trash/assets/index.json", "_app/activity.json", "_app/view.json", "_app/store.json", "labs/notes.json", "extras/sketch.bin",
        )) assertTrue("missing $path", path in files)
        assertFalse("pages/PG2.json" in files)
        assertFalse("story/E2.json" in files)
    }

    @Test
    fun diskEqualsTheModelSplit() = runTest {
        val doc = Fx.richDoc()
        val dir = newDir(doc)
        val expected = ProjectFileSet.split(doc).encode()
        for ((path, element) in expected) assertEquals("content of $path", element, diskJson(dir, path))
        val onDisk = dir.walk().filter { it.isFile }.map { it.relativeTo(dir).invariantSeparatorsPath }
            .filter { !it.startsWith(ProjectLayout.APP_DIR) && it !in doc.unknownFiles }.toSet()
        assertEquals(expected.keys, onDisk)
        // The reloaded document splits into the same files again.
        val (store, _) = openStore(dir)
        assertEquals(expected, ProjectFileSet.split(store.doc.value).encode().mapValues { it.value })
    }

    // ---- only changed units ----

    @Test
    fun openThenFlushWritesNothing() = runTest {
        val dir = newDir()
        val (store, log) = openStore(dir)
        store.flush()
        advanceTimeBy(10_000)
        assertTrue(log.writes.isEmpty())
        assertTrue(log.deletes.isEmpty())
    }

    @Test
    fun onlyChangedUnitsAreRewritten() = runTest {
        val dir = newDir()
        val (store, log) = openStore(dir)

        store.update("이름") { it.renameCharacter("C1", "리엔 하르트") }
        store.flush()
        assertEquals(listOf("cast.json"), log.writes)

        log.clear()
        store.update("블록") { d ->
            d.copy(episodes = d.episodes.mapValues { (_, f) -> f.copy(blocks = f.blocks.map { b -> if (b.key == "B3") b.copy(number = 30) else b }) })
        }
        store.flush()
        assertEquals(listOf("story/E1.json"), log.writes)

        log.clear()
        store.updateApp { it.copy(view = it.view.copy(worldTab = "timeline")) }
        store.flush()
        assertEquals(listOf(PackagePaths.APP_VIEW), log.writes)

        // Trashing a character moves rows it owns in other files (derived liveness, §5.1).
        log.clear()
        store.update("인물 지우기") { d ->
            d.copy(cast = d.cast.copy(characters = d.cast.characters.map { if (it.key == "C1") it.copy(deletedAt = Fx.T0 + 99) else it }))
        }
        store.flush()
        assertEquals(
            setOf("cast.json", "trash/cast.json", "changes.json", "trash/changes.json", "assets/index.json", "trash/assets/index.json"),
            log.writes.toSet(),
        )
        assertTrue(log.deletes.isEmpty())

        // Restoring both characters empties three mirror files; a mirror file that holds nothing is removed.
        log.clear()
        store.update("되살리기") { d ->
            d.copy(cast = d.cast.copy(characters = d.cast.characters.map { it.copy(deletedAt = null, trashEntryKey = null) }))
        }
        store.flush()
        assertEquals(setOf("trash/cast.json", "trash/changes.json", "trash/assets/index.json"), log.deletes.toSet())
        assertEquals(setOf("cast.json", "changes.json", "assets/index.json"), log.writes.toSet())
        assertFalse(File(dir, "trash/cast.json").exists())
        val reopened = ProjectStore.read(Fx.P, dir, testEnv())
        assertTrue(reopened.cast.characters.all { it.deletedAt == null })
    }

    @Test
    fun aRemovedMapFileIsDeleted() = runTest {
        val dir = newDir()
        val (store, log) = openStore(dir)
        store.update("지도 지우기") { it.copy(maps = emptyMap()) }
        store.flush()
        assertEquals(setOf("maps/MAP1.json", "trash/maps/MAP1.json"), log.deletes.toSet())
        assertTrue(ProjectStore.read(Fx.P, dir, testEnv()).maps.isEmpty())
    }

    // ---- crash safety ----

    @Test
    fun aCrashBeforeTheJournalKeepsTheOldFiles() = runTest {
        val dir = newDir()
        val (store, _) = openStore(dir)
        store.storeDirectory.faults = CrashAt(beforeJournal = true)
        store.update("인물 지우기") { d ->
            d.copy(cast = d.cast.copy(characters = d.cast.characters.map { if (it.key == "C1") it.copy(deletedAt = Fx.T0 + 99) else it }))
        }
        store.flush()
        assertTrue(store.saveState.value is SaveState.Failed)
        // "Process killed": a new store reads the directory.
        val (again, _) = openStore(dir)
        assertEquals(Fx.normalized(Fx.richDoc()), Fx.normalized(again.doc.value))
        assertTrue(dir.walk().none { AtomicFiles.isTemp(it) })
    }

    @Test
    fun aCrashDuringRenamesIsCompletedOnTheNextOpen() = runTest {
        val dir = newDir()
        val (store, _) = openStore(dir)
        store.storeDirectory.faults = CrashAt(renameNumber = 2)
        store.update("인물 지우기") { d ->
            d.copy(cast = d.cast.copy(characters = d.cast.characters.map { if (it.key == "C1") it.copy(deletedAt = Fx.T0 + 99) else it }))
        }
        val expected = store.doc.value
        store.flush()
        assertTrue(store.saveState.value is SaveState.Failed)
        val (again, _) = openStore(dir)
        assertTrue(again.loadReport.recoveredTransaction)
        assertEquals(Fx.normalized(expected.copy(app = again.doc.value.app)), Fx.normalized(again.doc.value))
        assertTrue(dir.walk().none { AtomicFiles.isTemp(it) })
    }

    @Test
    fun aHalfWrittenSingleFileNeverReplacesTheGoodOne() = runTest {
        val dir = newDir()
        val before = File(dir, "cast.json").readText()
        val (store, _) = openStore(dir)
        store.storeDirectory.faults = CrashAt(halfWriteOf = "cast.json")
        store.update("이름") { it.renameCharacter("C1", "새 이름") }
        store.flush()
        assertEquals(before, File(dir, "cast.json").readText())
        val (again, _) = openStore(dir)
        assertEquals("리엔", again.doc.value.cast.characters.first { it.key == "C1" }.name)
    }

    @Test
    fun aFailedSaveIsRetriedByTheNextFlush() = runTest {
        val dir = newDir()
        val (store, _) = openStore(dir)
        store.storeDirectory.faults = CrashAt(halfWriteOf = "cast.json")
        store.update("이름") { it.renameCharacter("C1", "새 이름") }
        store.flush()
        assertTrue(store.saveState.value is SaveState.Failed)
        store.storeDirectory.faults = io.github.lenticualrstone.civilight.store.io.WriteFaults.None
        store.flush()
        assertEquals(SaveState.Saved, store.saveState.value)
        assertEquals("새 이름", ProjectStore.read(Fx.P, dir, testEnv()).cast.characters.first { it.key == "C1" }.name)
    }

    // ---- debounce and flush ----

    @Test
    fun savesAreDebouncedAndFlushSavesAtOnce() = runTest {
        val dir = newDir()
        val (store, log) = openStore(dir)
        store.update("이름") { it.renameCharacter("C1", "하나") }
        assertEquals(SaveState.Pending, store.saveState.value)
        advanceTimeBy(599)
        runCurrent()
        assertTrue(log.writes.isEmpty())
        advanceTimeBy(2)
        runCurrent()
        assertEquals(listOf("cast.json"), log.writes)
        assertEquals(SaveState.Saved, store.saveState.value)

        // Each edit restarts the 600 ms timer.
        log.clear()
        store.update("이름") { it.renameCharacter("C1", "둘") }
        advanceTimeBy(500)
        store.update("이름") { it.renameCharacter("C1", "셋") }
        advanceTimeBy(500)
        runCurrent()
        assertTrue(log.writes.isEmpty())
        advanceTimeBy(101)
        runCurrent()
        assertEquals(listOf("cast.json"), log.writes)

        // flush() writes without waiting.
        log.clear()
        store.update("이름") { it.renameCharacter("C1", "넷") }
        store.flush()
        assertEquals(listOf("cast.json"), log.writes)
        assertEquals("넷", ProjectStore.read(Fx.P, dir, testEnv()).cast.characters.first { it.key == "C1" }.name)
    }

    @Test
    fun continuousEditingStillSavesWithinTheMaximumDelay() = runTest {
        val dir = newDir()
        val (store, log) = openStore(dir)
        // An edit every 500 ms never lets the 600 ms debounce fire; the 3 s limit does.
        for (i in 0 until 7) {
            store.update("입력", mergeKey = "typing") { it.renameCharacter("C1", "이름 $i") }
            advanceTimeBy(500)
            runCurrent()
        }
        assertEquals(listOf("cast.json"), log.writes)
    }

    // ---- undo / redo ----

    @Test
    fun undoAndRedoAreLabelledStepsThatKeepAppState() = runTest {
        val dir = newDir()
        val (store, _) = openStore(dir)
        store.update("인물 이름 바꿈") { it.renameCharacter("C1", "노아") }
        store.updateApp { it.copy(view = it.view.copy(worldTab = "timeline")) }
        assertEquals("인물 이름 바꿈", store.undoState.value.undoLabel)
        assertEquals(1, store.undoState.value.undoCount)

        assertEquals("인물 이름 바꿈", store.undo())
        assertEquals("리엔", store.doc.value.cast.characters.first { it.key == "C1" }.name)
        assertEquals("timeline", store.doc.value.app.view.worldTab)
        assertEquals("인물 이름 바꿈", store.undoState.value.redoLabel)

        assertEquals("인물 이름 바꿈", store.redo())
        assertEquals("노아", store.doc.value.cast.characters.first { it.key == "C1" }.name)
        assertEquals("timeline", store.doc.value.app.view.worldTab)

        // A new edit drops the redo history; a no-op records nothing.
        store.undo()
        store.update("장소 추가") { d -> d.copy(world = d.world.copy(places = d.world.places + Place(key = "PL9", order = "z", name = "등대"))) }
        assertFalse(store.undoState.value.canRedo)
        store.update("아무것도") { it }
        assertEquals(1, store.undoState.value.undoCount)
        assertNull(store.redo())

        // Undo is saved like any edit.
        store.undo()
        store.flush()
        assertTrue(ProjectStore.read(Fx.P, dir, testEnv()).world.places.none { it.key == "PL9" })
    }

    @Test
    fun editsWithOneMergeKeyFormOneStep() = runTest {
        val clock = ManualClock(Fx.T0)
        val (store, _) = openStore(newDir(), testEnv(clock))
        store.update("입력", mergeKey = "B1") { it.renameCharacter("C1", "ㄴ") }
        clock.advance(500)
        store.update("입력", mergeKey = "B1") { it.renameCharacter("C1", "노") }
        clock.advance(500)
        store.update("입력", mergeKey = "B1") { it.renameCharacter("C1", "노아") }
        assertEquals(1, store.undoState.value.undoCount)
        clock.advance(5_000)
        store.update("입력", mergeKey = "B1") { it.renameCharacter("C1", "노아야") }
        assertEquals(2, store.undoState.value.undoCount)
        store.undo()
        store.undo()
        assertEquals("리엔", store.doc.value.cast.characters.first { it.key == "C1" }.name)
    }

    @Test
    fun undoHistoryIsCapped() = runTest {
        val (store, _) = openStore(newDir(), testEnv(timing = StoreTiming(undoLimit = 3)))
        for (i in 1..5) store.update("편집 $i") { it.renameCharacter("C1", "이름 $i") }
        assertEquals(3, store.undoState.value.undoCount)
        assertEquals("편집 5", store.undoState.value.undoLabel)
    }

    @Test
    fun readOnlyProjectsRejectContentEditsButKeepAppState() = runTest {
        val (store, _) = openStore(newDir(), readOnly = true)
        try {
            store.update("이름") { it.renameCharacter("C1", "x") }
            fail("expected ReadOnlyProjectException")
        } catch (_: ReadOnlyProjectException) {
        }
        store.updateApp { it.copy(view = it.view.copy(worldTab = "timeline")) }
        assertEquals("timeline", store.doc.value.app.view.worldTab)
    }

    // ---- unknown and damaged files ----

    @Test
    fun unknownFilesArePreservedAcrossSaves() = runTest {
        val doc = Fx.richDoc()
        val dir = newDir(doc)
        File(dir, "future").mkdirs()
        File(dir, "future/thing.dat").writeBytes(byteArrayOf(9, 8, 7))
        val (store, log) = openStore(dir)
        val unknown = store.doc.value.unknownFiles
        assertEquals(setOf("labs/notes.json", "extras/sketch.bin", "future/thing.dat"), unknown.keys)
        assertArrayEquals(byteArrayOf(9, 8, 7), Base64.getDecoder().decode(unknown.getValue("future/thing.dat").bytesBase64))

        store.update("이름") { it.renameCharacter("C1", "노아") }
        store.flush()
        assertEquals(listOf("cast.json"), log.writes)
        assertEquals("""{"future":true,"n":[1,2]}""", File(dir, "labs/notes.json").readText())
        assertArrayEquals(byteArrayOf(0, 1, 2, -1, -128, 127), File(dir, "extras/sketch.bin").readBytes())
        assertEquals(Fx.normalized(store.doc.value).unknownFiles, Fx.normalized(ProjectStore.read(Fx.P, dir, testEnv())).unknownFiles)

        // An unknown file dropped from the document is removed from disk.
        log.clear()
        store.update("정리") { it.copy(unknownFiles = it.unknownFiles - "future/thing.dat") }
        store.flush()
        assertEquals(listOf("future/thing.dat"), log.deletes)
    }

    @Test
    fun aDamagedFileIsSetAsideAndNeverOverwritten() = runTest {
        val dir = newDir()
        File(dir, "world.json").writeText("{ not json")
        File(dir, "story/E1.json").writeText("""{"unitKey":"E1","blocks":"broken"}""")
        val (store, _) = openStore(dir)
        assertEquals(setOf("world.json", "story/E1.json"), store.loadReport.damaged.keys)
        val aside = File(dir, store.loadReport.damagedDir!!)
        assertEquals("{ not json", File(aside, "world.json").readText())
        assertTrue(File(aside, "story/E1.json").isFile)
        // Only the trashed place (from trash/world.json) is left.
        assertEquals(listOf("PL2"), store.doc.value.world.places.map { it.key })

        store.update("장소 추가") { d -> d.copy(world = d.world.copy(places = listOf(Place(key = "PL9", order = "V", name = "등대")))) }
        store.flush()
        assertEquals("{ not json", File(aside, "world.json").readText())
        assertEquals(listOf("PL9"), ProjectStore.read(Fx.P, dir, testEnv()).world.places.map { it.key })
    }

    @Test
    fun aProjectWithoutProjectJsonIsNotOpenedOrRecreated() = runTest {
        val dir = newDir()
        File(dir, "project.json").delete()
        try {
            ProjectStore.open(Fx.P, dir, testEnv())
            fail("expected ProjectLoadException")
        } catch (_: ProjectLoadException) {
        }
        assertFalse(File(dir, "project.json").exists())
        assertTrue(File(dir, "cast.json").exists())
    }

    // ---- store version ----

    @Test
    fun aNewerStoreVersionIsRefusedAndLeftAlone() = runTest {
        val dir = newDir()
        File(dir, ProjectLayout.STORE_META).writeText("""{"storeVersion":7}""")
        val before = File(dir, "cast.json").readText()
        try {
            ProjectStore.open(Fx.P, dir, testEnv())
            fail("expected NewerStoreVersionException")
        } catch (e: NewerStoreVersionException) {
            assertEquals(7, e.found)
        }
        assertEquals(before, File(dir, "cast.json").readText())
    }

    @Test
    fun olderStoresRunTheUpgradeChainOnce() = runTest {
        val dir = newDir()
        var runs = 0
        val chain = StoreUpgradeChain(current = 2, steps = mapOf(1 to StoreUpgradeStep { files ->
            runs++
            val project = files.getValue(PackagePaths.PROJECT) as JsonObject
            files + (PackagePaths.PROJECT to JsonObject(project + ("oneLiner" to JsonPrimitive("올린 판"))))
        }))
        val store = ProjectStore.open(Fx.P, dir, testEnv(), chain = chain)
        assertEquals(1, store.loadReport.upgradedFrom)
        assertEquals("올린 판", store.doc.value.project.oneLiner)
        assertEquals(2, CivilightJson.decode(io.github.lenticualrstone.civilight.store.version.StoreVersionFile.serializer(), File(dir, ProjectLayout.STORE_META).readText()).storeVersion)
        val again = ProjectStore.open(Fx.P, dir, testEnv(), chain = chain)
        assertNull(again.loadReport.upgradedFrom)
        assertEquals(1, runs)
        assertEquals("올린 판", again.doc.value.project.oneLiner)
    }

    @Test
    fun unknownFileHelperHashesTheBytes() {
        val file = ProjectLoader.unknownFile("x.bin", "abc".toByteArray())
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", file.sha256)
    }
}
