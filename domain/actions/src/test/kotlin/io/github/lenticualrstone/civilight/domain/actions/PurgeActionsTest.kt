package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.domain.trash.ProjectTrash
import io.github.lenticualrstone.civilight.domain.trash.ProjectTrashState
import io.github.lenticualrstone.civilight.domain.trash.RecorderTrash
import io.github.lenticualrstone.civilight.domain.trash.RecorderTrashState
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.app.LibraryIndexFile
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.RecorderAudio
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

class PurgeActionsTest {
    private val p2Key = Fx.key("PROJ", 2)
    private val unitLink = Fx.key("RLNK", 11)
    private val p2Link = Fx.key("RLNK", 12)
    private val old = Fx.ctxAt("2026-11-01T09:00:00")
    private val later = Fx.ctxAt("2027-01-15T09:00:00")

    private fun blob(hash: String) = Blob(hash, "png", "image/png", 10, source = BlobSource.GALLERY.open(), importedAt = ExampleClock.NOW)

    private fun withBlobs(doc: ProjectDoc, vararg hashes: String) = doc.copy(assets = doc.assets.copy(blobs = doc.assets.blobs + hashes.map(::blob)))

    /** P1 uses the blob `shared` as a map base image (so the item purge keeps it in P1's asset index). */
    private val p1 = withBlobs(Fx.doc(), "shared").let { d ->
        d.copy(maps = d.maps.mapValues { (k, m) -> if (k == K.MAP_ASH_MARKET) m.copy(baseImageHash = "shared") else m })
    }
    private val p2 = withBlobs(ProjectTrash.rekey(Fx.doc(), p2Key).let { it.copy(project = it.project.copy(name = "유리 정원")) }, "shared", "p2only")

    private val library = Fx.recorder().let { lib ->
        val template = lib.links.single { it.key == K.recorderLink(2) }
        lib.copy(
            records = lib.records.map { if (it.key == K.RECORDER_R3) it.copy(audio = RecorderAudio("audio-r3", "audio/mp4", 1, 62_000)) else it },
            links = lib.links +
                template.copy(key = unitLink, recordKey = K.RECORDER_R4, targetKey = K.SCENE_1_6_5) +
                template.copy(key = p2Link, recordKey = K.RECORDER_R4, projectKey = p2Key, targetKind = RecorderTargetKind.PROJECT.open(), targetKey = p2Key),
        )
    }

    private val projectTrash = ProjectTrash.delete(
        ProjectTrashState(LibraryIndexFile(listOf(LibraryEntry(K.PROJECT, p1.project.name), LibraryEntry(p2Key, "유리 정원"))), emptyList()),
        p2Key, old.trash,
    )

    private val recorderTrash = RecorderTrash.delete(RecorderTrashState(library, ExampleRecorder.trashEntries()), K.RECORDER_R3, old.trash)

    private fun run(ctx: ActionContext = later) = PurgeActions.runDue(ctx, listOf(p1, p2), projectTrash, recorderTrash)

    @Test
    fun `nothing is due today`() {
        val run = PurgeActions.runDue(ExampleClock.NOW, listOf(p1, p2), projectTrash, recorderTrash, zone = Fx.zone)
        assertTrue(run.itemPurges.isEmpty())
        assertTrue(run.purgedProjects.isEmpty())
        assertEquals(p1, run.projects[K.PROJECT])
        assertTrue(run.blobsToDelete.isEmpty())
    }

    @Test
    fun `entries due inside a project are purged and recorder links keep the plain unit label`() {
        val run = run()
        val purge = run.itemPurges.getValue(K.PROJECT)
        val doc = run.projects.getValue(K.PROJECT)
        assertTrue(doc.trash.entries.none { later.trash.isDue(it) })
        assertNull(doc.index.unit(K.SCENE_1_6_5))
        val unitTarget = purge.purgedTargets.single { it.kind == ItemKind.UNIT && it.key == K.SCENE_1_6_5 }
        assertEquals(UnitCodes(p1).unitLabel(K.SCENE_1_6_5), unitTarget.label)
        assertEquals("1-6 씬 5", unitTarget.label)
        val link = run.recorder.links.single { it.key == unitLink }
        assertEquals("1-6 씬 5", link.lastLabel)
        assertEquals(later.now, link.purgedAt)
        // A purged character keeps its name on its link.
        assertEquals("노아", run.recorder.links.single { it.recordKey == K.RECORDER_NOAH }.lastLabel)
    }

    @Test
    fun `whole projects due are purged and links into them keep the project name`() {
        val run = run()
        assertEquals(listOf(p2Key), run.purgedProjects)
        assertFalse(p2Key in run.projects)
        assertTrue(run.projectTrash.library.projects.none { it.key == p2Key })
        assertTrue(run.projectTrash.entries.isEmpty())
        val link = run.recorder.links.single { it.key == p2Link }
        assertNotNull(link.purgedAt)
        assertEquals("유리 정원", link.lastLabel)
    }

    @Test
    fun `recorder records due are purged with their audio`() {
        val run = run()
        assertNull(run.recorder.record(K.RECORDER_R3))
        assertNull(run.recorder.record(K.RECORDER_TRASHED))
        assertTrue(run.recorderTrash.entries.isEmpty())
        assertTrue(run.recorderLog.map { it.key }.containsAll(listOf(K.RECORDER_R3, K.RECORDER_TRASHED)))
    }

    @Test
    fun `only blobs nobody references any more are deleted`() {
        val run = run()
        assertTrue("p2only" in run.blobsToDelete)
        assertTrue("audio-r3" in run.blobsToDelete)
        assertFalse("shared" in run.blobsToDelete)
        // A record still holding the audio keeps the file.
        val shared = recorderTrash.copy(library = recorderTrash.library.copy(records = recorderTrash.library.records.map {
            if (it.key == K.RECORDER_R1) it.copy(audio = RecorderAudio("audio-r3", "audio/mp4", 1, 24_000)) else it
        }))
        assertFalse("audio-r3" in PurgeActions.runDue(later, listOf(p1, p2), projectTrash, shared).blobsToDelete)
    }
}
