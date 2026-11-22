package io.github.lenticualrstone.civilight.store

import io.github.lenticualrstone.civilight.model.file.EpisodeFile
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.file.StoryIndexFile
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.project.StoryStructure
import io.github.lenticualrstone.civilight.model.project.StructurePreset
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import io.github.lenticualrstone.civilight.store.library.LibraryStore
import io.github.lenticualrstone.civilight.store.library.ProjectNames
import io.github.lenticualrstone.civilight.store.project.ProjectStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    /** 2026-11-21 09:00 in Seoul. */
    private val clock = ManualClock.at("2026-11-21T00:00:00Z")

    private fun TestScope.library(root: File = tmp.root) = LibraryStore(root, testEnv(clock))

    private suspend inline fun <reified T : Throwable> assertThrows(block: () -> Unit) {
        try {
            block()
            fail("expected ${T::class.simpleName}")
        } catch (e: Throwable) {
            if (e !is T) throw e
        }
    }

    @Test
    fun createWritesAProjectWithDefaultsAndAnIndexRow() = runTest {
        val lib = library()
        val entry = lib.create("  유리 정원 ")
        assertEquals("유리 정원", entry.name)
        val dir = lib.projectDir(entry.key)
        assertTrue(File(dir, PackagePaths.PROJECT).isFile)
        assertTrue(File(dir, PackagePaths.STORY_INDEX).isFile)
        val project = CivilightJson.decode(Project.serializer(), File(dir, PackagePaths.PROJECT).readText())
        assertEquals(listOf("도보", "말", "배"), project.travelModes.map { it.name })
        assertEquals(clock.now(), project.createdAt)
        assertEquals("DEV-TEST", project.updatedBy)
        assertEquals(listOf(entry), lib.state.value.live)

        // The index is on disk.
        val again = library()
        again.ensureLoaded()
        assertEquals(listOf(entry), again.state.value.entries)
        assertTrue(again.reconcileReport.isClean)
    }

    @Test
    fun blankNamesAreRejected() = runTest {
        assertThrows<IllegalArgumentException> { library().create("   ") }
    }

    @Test
    fun createAppliesTheCallersSetup() = runTest {
        val lib = library()
        val entry = lib.create("단편") { doc ->
            doc.copy(story = StoryIndexFile(units = listOf(StoryUnit(key = "S1", level = 2, order = "V", title = "씬 1"))))
        }
        assertEquals(listOf("S1"), lib.readDoc(entry.key).story.units.map { it.key })
    }

    @Test
    fun openGivesOneStorePerProjectAndTheIndexFollowsEdits() = runTest {
        val lib = library()
        val entry = lib.create("잿빛 새벽의 등불")
        val store = lib.open(entry.key)
        assertSame(store, lib.open(entry.key))
        assertEquals(clock.now(), lib.state.value.entry(entry.key)!!.lastOpenedAt)

        clock.advance(60_000)
        store.update("등급") { d ->
            d.copy(project = d.project.copy(rating = Rating.R18.open(), protections = d.project.protections.copy(hidden = true)))
        }
        val row = lib.state.value.entry(entry.key)!!
        assertEquals(Rating.R18.open(), row.rating)
        assertTrue(row.hidden)
        assertEquals(clock.now(), row.updatedAt)

        lib.close(entry.key)
        assertTrue(store.isClosed)
        assertNull(lib.openStore(entry.key))
        val reread = library()
        reread.ensureLoaded()
        assertTrue(reread.state.value.entry(entry.key)!!.hidden)
    }

    @Test
    fun renameWritesProjectJsonAndTheIndex() = runTest {
        val lib = library()
        val entry = lib.create("가제")
        lib.rename(entry.key, "검은 서약")
        assertEquals("검은 서약", lib.state.value.entry(entry.key)!!.name)
        assertEquals("검은 서약", lib.readDoc(entry.key).project.name)
        // Renaming an open project goes through its store: one undo step.
        val store = lib.open(entry.key)
        lib.rename(entry.key, "검은 서약 (가제)")
        assertEquals(LibraryStore.RENAME_LABEL, store.undoState.value.undoLabel)
        assertEquals("검은 서약 (가제)", store.doc.value.project.name)
        store.undo()
        assertEquals("검은 서약", lib.state.value.entry(entry.key)!!.name)
    }

    @Test
    fun duplicateMakesANextFreeNameUnderANewKey() = runTest {
        val lib = library()
        val source = lib.add(Fx.richDoc())
        val copy = lib.duplicate(source.key)
        assertEquals("잿빛 새벽의 등불 (2)", copy.name)
        assertNotEquals(source.key, copy.key)
        val doc = lib.readDoc(copy.key)
        assertEquals(copy.key, doc.project.key)
        val original = Fx.richDoc()
        // Item keys and content are kept; app state starts fresh.
        assertEquals(Fx.normalized(original).cast, Fx.normalized(doc).cast)
        assertEquals(Fx.normalized(original).episodes, Fx.normalized(doc).episodes)
        assertEquals(io.github.lenticualrstone.civilight.model.app.ProjectAppState(), doc.app)
        assertTrue(doc.trash.entries.all { it.projectKey == copy.key })
        assertEquals("잿빛 새벽의 등불 (3)", lib.duplicate(source.key).name)
        assertEquals("잿빛 새벽의 등불 (4)", lib.duplicate(copy.key).name)
    }

    @Test
    fun duplicatingAOneLevelProjectRekeysItsNumberingUnit() = runTest {
        val lib = library()
        val entry = lib.create("단막") { doc ->
            doc.copy(
                project = doc.project.copy(structure = StoryStructure(preset = StructurePreset.CUSTOM.open(), levels = listOf("씬"))),
                story = StoryIndexFile(units = listOf(StoryUnit(key = "S1", level = 0, order = "V"))),
                episodes = mapOf(doc.key to EpisodeFile(unitKey = doc.key, blocks = listOf(Fx.block("B1", "S1", "V", 1, "첫 줄")))),
            )
        }
        val copy = lib.duplicate(entry.key)
        val doc = lib.readDoc(copy.key)
        assertEquals(setOf(copy.key), doc.episodes.keys)
        assertEquals(copy.key, doc.episodes.getValue(copy.key).unitKey)
        assertEquals(listOf("B1"), doc.episodes.getValue(copy.key).blocks.map { it.key })
        assertTrue(File(lib.projectDir(copy.key), "story/${copy.key}.json").isFile)
    }

    @Test
    fun trashRestoreAndPurgeMoveTheDirectory() = runTest {
        val lib = library()
        val entry = lib.create("유리 정원")
        lib.open(entry.key)
        val row = lib.moveToTrash(entry.key)
        assertEquals(TrashKind.PROJECT.open(), row.kind)
        assertEquals(entry.key, row.projectKey)
        // T-01: deleted 2026-11-21 → purge on 2026-12-21 (`29일 남음` on 11-22).
        assertEquals(LocalDate.of(2026, 12, 21), row.purgeOn)
        assertNull(lib.openStore(entry.key))
        assertFalse(lib.projectDir(entry.key).exists())
        assertTrue(lib.trashedDir(entry.key).isDirectory)
        assertTrue(lib.state.value.live.isEmpty())
        assertEquals(row.key, lib.state.value.entry(entry.key)!!.trashEntryKey)
        assertEquals(row, lib.state.value.trashEntryOf(entry.key))
        assertThrows<NoSuchProjectException> { lib.open(entry.key) }
        // A trashed project can still be read (trash screen counts).
        assertEquals("유리 정원", lib.readDoc(entry.key).project.name)

        lib.restore(entry.key)
        assertTrue(lib.projectDir(entry.key).isDirectory)
        assertFalse(lib.trashedDir(entry.key).exists())
        assertTrue(lib.state.value.trash.entries.isEmpty())
        assertNull(lib.state.value.entry(entry.key)!!.deletedAt)

        lib.moveToTrash(entry.key)
        lib.purge(entry.key)
        assertFalse(lib.trashedDir(entry.key).exists())
        assertNull(lib.state.value.entry(entry.key))
        assertTrue(lib.state.value.trash.entries.isEmpty())
        val reread = library()
        reread.ensureLoaded()
        assertTrue(reread.state.value.entries.isEmpty())
    }

    @Test
    fun purgeExpiredRemovesProjectsWhoseDateHasCome() = runTest {
        // T-08: deleted 2026-10-23 → purged on the 2026-11-22 run; deleted 2026-10-24 → kept (1일 남음).
        val early = ManualClock.at("2026-10-23T01:00:00Z")
        val lib = LibraryStore(tmp.root, testEnv(early))
        val a = lib.create("가")
        val b = lib.create("나")
        lib.moveToTrash(a.key)
        early.set(ManualClock.at("2026-10-24T01:00:00Z").now())
        lib.moveToTrash(b.key)
        val purged = lib.purgeExpired(LocalDate.of(2026, 11, 22))
        assertEquals(listOf(a.key), purged.map { it.key })
        assertEquals(listOf(b.key), lib.state.value.trashed.map { it.key })
    }

    @Test
    fun theExampleProjectIsReadOnlyAndLeavesWithoutTheTrash() = runTest {
        val lib = library()
        val example = lib.create("잿빛 새벽의 등불", isExample = true, readOnly = true)
        val store = lib.open(example.key)
        assertTrue(store.readOnly)
        assertThrows<ReadOnlyProjectException> { store.update("x") { it.copy(project = it.project.copy(name = "x")) } }
        assertThrows<ReadOnlyProjectException> { lib.rename(example.key, "x") }
        assertThrows<ReadOnlyProjectException> { lib.moveToTrash(example.key) }
        lib.deleteWithoutTrash(example.key)
        assertNull(lib.state.value.entry(example.key))
        assertFalse(lib.projectDir(example.key).exists())
        assertTrue(lib.state.value.trash.entries.isEmpty())
    }

    @Test
    fun keysAreUniqueAmongLiveAndTrashedProjects() = runTest {
        val lib = library()
        val entry = lib.add(Fx.richDoc())
        lib.moveToTrash(entry.key)
        assertThrows<ProjectKeyTakenException> { lib.add(Fx.richDoc()) }
        // Re-key the trashed copy out of the way (§6 ED), then the file imports with its key.
        val aside = lib.moveAsideForImport(entry.key)
        val asideRow = lib.state.value.entry(aside)!!
        assertTrue(asideRow.deletedAt != null)
        assertEquals(entry.key, lib.add(Fx.richDoc()).key)
        assertEquals(aside, lib.readDoc(aside).project.key)
        assertEquals(setOf(entry.key, aside), lib.state.value.entries.map { it.key }.toSet())
    }

    @Test
    fun overwriteMovesTheLiveCopyToTheTrash() = runTest {
        val lib = library()
        val entry = lib.add(Fx.richDoc())
        val aside = lib.moveAsideForImport(entry.key)
        assertNull(lib.state.value.entry(entry.key))
        assertEquals(TrashReason.REPLACED_BY_OVERWRITE.open(), lib.state.value.trashEntryOf(aside)!!.reason)
        assertEquals(entry.key, lib.add(Fx.richDoc()).key)
    }

    // ---- reconcile after a crash ----

    @Test
    fun anInterruptedTrashIsFinishedOnStart() = runTest {
        val lib = library()
        val entry = lib.create("유리 정원")
        lib.moveToTrash(entry.key)
        // Crash between writing the index and moving the directory: put it back where it was.
        io.github.lenticualrstone.civilight.store.io.Dirs.move(lib.trashedDir(entry.key), lib.projectDir(entry.key))
        val restarted = library()
        restarted.ensureLoaded()
        assertEquals(listOf(entry.key), restarted.reconcileReport.moved)
        assertTrue(restarted.trashedDir(entry.key).isDirectory)
        assertFalse(restarted.projectDir(entry.key).exists())
    }

    @Test
    fun unindexedDirectoriesAreAdoptedNotDeleted() = runTest {
        val lib = library()
        lib.ensureLoaded()
        val doc = Fx.richDoc()
        ProjectStore.writeNew(doc, lib.projectDir(doc.key))
        File(lib.projectsDir, "junk").mkdirs()
        File(lib.projectsDir, ".staging-half").mkdirs()
        val restarted = library()
        restarted.ensureLoaded()
        assertEquals(listOf(doc.key), restarted.reconcileReport.adopted)
        assertEquals("잿빛 새벽의 등불", restarted.state.value.entry(doc.key)!!.name)
        assertTrue(File(restarted.projectsDir, "junk").isDirectory)
        assertFalse(File(restarted.projectsDir, ".staging-half").exists())
        assertEquals(1, restarted.reconcileReport.unreadable.size)
    }

    @Test
    fun aDamagedIndexIsRebuiltFromTheDirectories() = runTest {
        val lib = library()
        val entry = lib.create("유리 정원")
        File(tmp.root, LibraryStore.LIBRARY_FILE).writeText("{ broken")
        val restarted = library()
        restarted.ensureLoaded()
        assertEquals(listOf(entry.key), restarted.state.value.entries.map { it.key })
        assertTrue(tmp.root.listFiles()!!.any { it.name.startsWith("library.json.damaged-") })
    }

    @Test
    fun nextFreeNames() {
        assertEquals("잿빛 새벽의 등불 (2)", ProjectNames.nextFree("잿빛 새벽의 등불", listOf("잿빛 새벽의 등불")))
        // F-11
        assertEquals("잿빛 새벽의 등불 (3)", ProjectNames.nextFree("잿빛 새벽의 등불", listOf("잿빛 새벽의 등불", "잿빛 새벽의 등불 (2)")))
        assertEquals("가 (3)", ProjectNames.nextFree("가 (2)", listOf("가", "가 (2)")))
        // NFD input is compared as NFC (F-02).
        val nfd = java.text.Normalizer.normalize("잿빛", java.text.Normalizer.Form.NFD)
        assertEquals("잿빛 (3)", ProjectNames.nextFree(nfd, listOf("잿빛 (2)")))
    }
}
