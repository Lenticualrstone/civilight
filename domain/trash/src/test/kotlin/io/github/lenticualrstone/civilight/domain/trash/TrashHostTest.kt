package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.app.LibraryIndexFile
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.model.trash.TrashEntry
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** LIB-38/39 host behaviour: countdown, purge schedule, gates, project and recorder rows, blobs. */
class TrashHostTest {
    private fun hideNoah(doc: ProjectDoc) =
        doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.NOAH) it.copy(hidden = true) else it }))

    @Test
    fun `countdown uses local calendar days`() {
        val ctx = Fx.ctx() // 2026-11-22 09:00 KST
        // 2026-11-22 00:30 KST is still 11-21 in UTC: deleted today.
        assertEquals(30, ctx.daysLeft(Fx.ctxAt("2026-11-22T00:30").now))
        assertEquals(29, ctx.daysLeft(Fx.ctxAt("2026-11-21T23:59").now))
        assertEquals(0, ctx.daysLeft(Fx.ctxAt("2026-10-23T23:59").now))
        assertEquals(1, ctx.daysLeft(Fx.ctxAt("2026-10-24T00:01").now))
        assertEquals("0일 남음", TrashCopy.daysLeft(-2))
        assertEquals(LocalDate.parse("2026-12-22"), TrashEntry.purgeDateFor(ctx.today))
    }

    @Test
    fun `purge date follows the local deletion date`() {
        val late = Fx.ctxAt("2026-11-21T23:30") // 14:30Z: the local date is 11-21
        val entry = TrashOps.delete(Fx.doc(), ItemId.character(K.GORDON), late).entries.single()
        assertEquals(LocalDate.parse("2026-12-21"), entry.purgeOn)
        assertFalse(Fx.ctxAt("2026-12-20T23:59").isDue(entry))
        assertTrue(Fx.ctxAt("2026-12-21T00:00").isDue(entry))
    }

    @Test
    fun `daily purge takes only the due entries`() {
        val doc = Fx.doc()
        val result = PurgeOps.purgeDue(doc, Fx.ctxAt("2026-11-25T03:00"))
        assertEquals(listOf(K.TRASH_NOAH), result.purgedEntries.map { it.key })
        assertEquals(listOf(K.NOAH), result.nameTags.map { it.key })
        assertNotNull(result.doc.index.trashEntry(K.TRASH_SCENE_1_6_5))
        assertTrue(PurgeOps.purgeDue(doc, Fx.ctx()).purgedEntries.isEmpty())
    }

    @Test
    fun `deletion log keeps 30 days`() {
        val doc = Fx.doc()
        assertEquals(10, doc.deletions.entries.size)
        // Run on 12-20: the 11-14 and 11-19 rows are older than 30 days and drop out.
        val ctx = Fx.ctxAt("2026-12-20T10:00")
        val p = PurgeOps.purge(doc, K.TRASH_RECORD, ctx).doc
        assertEquals(listOf(K.RECORD_TRASHED), p.deletions.entries.map { it.key })
        // Run today: they stay.
        assertEquals(11, PurgeOps.purge(doc, K.TRASH_RECORD, Fx.ctx()).doc.deletions.entries.size)
    }

    @Test
    fun `blobs no row references are collected`() {
        val base = Fx.doc()
        val only = "a".repeat(64)
        val shared = "b".repeat(64)
        fun blob(hash: String) = Blob(hash, "webp", "image/webp", 10, source = BlobSource.GALLERY.open(), importedAt = 0L)
        fun use(n: Int, owner: String, hash: String) = ImageUse(
            key = Fx.key("PCTR", n), ownerKind = ImageOwnerKind.CHARACTER.open(), ownerKey = owner,
            slot = ImageSlot.CHARACTER_PORTRAIT.open(), source = hash,
        )
        val doc = base.copy(
            assets = base.assets.copy(
                blobs = base.assets.blobs + blob(only) + blob(shared),
                imageUses = base.assets.imageUses + use(1, K.NOAH, only) + use(2, K.NOAH, shared).copy(slot = ImageSlot.CHARACTER_BACKGROUND.open()) +
                    use(3, K.OWEN, shared),
            ),
        )
        // While 노아 is in the trash his images are kept (trashed rows still reference them).
        assertEquals(2, Impact.of(Fx.noahLive(doc), ItemId.character(K.NOAH)).owned(OwnedKind.IMAGE))
        val purged = PurgeOps.purge(doc, K.TRASH_NOAH, Fx.ctx())
        assertEquals(listOf(only), purged.unreferencedBlobs)
        assertTrue(purged.doc.assets.blobs.any { it.hash == shared })
        assertTrue(purged.doc.assets.imageUses.none { it.ownerKey == K.NOAH })
    }

    @Test
    fun `I-14 hidden 노아 in the trash`() {
        val ctx = Fx.ctx()
        val doc = hideNoah(Fx.doc())
        val locked = TrashListing.projectItemRows(doc, ctx).single { it.entryKey == K.TRASH_NOAH }
        assertEquals("숨긴 인물", locked.title)
        assertEquals("3일 남음", locked.daysLeftLabel)
        assertTrue(locked.masked)
        assertTrue(locked.restoreNeedsAuth)
        val open = TrashListing.projectItemRows(doc, ctx, HidingLocks(itemsLocked = false)).single { it.entryKey == K.TRASH_NOAH }
        assertEquals("노아", open.title)
        assertTrue(open.restoreNeedsAuth)
    }

    @Test
    fun `S-08 trash location of a hidden project`() {
        val ctx = Fx.ctx()
        val base = Fx.doc()
        val doc = base.copy(project = base.project.copy(protections = base.project.protections.copy(hidden = true)))
        val locked = TrashListing.projectItemRows(doc, ctx, HidingLocks(itemsLocked = true, projectLocked = true))
        assertTrue(locked.all { it.restoreNeedsAuth && it.masked })
        assertTrue(locked.all { "숨긴 프로젝트" in it.sub && Fx.PROJECT_NAME !in it.sub })
        assertTrue(locked.none { it.title == "노아" })
        val open = TrashListing.projectItemRows(doc, ctx, HidingLocks(itemsLocked = false, projectLocked = false))
        assertTrue(open.all { Fx.PROJECT_NAME in it.sub && it.restoreNeedsAuth })
        assertEquals("노아", open.single { it.entryKey == K.TRASH_NOAH }.title)
        assertEquals(RestoreOutcome.NeedsAuth, RestoreOps.restore(doc, K.TRASH_SCENE_1_6_5, ctx))
    }

    @Test
    fun `restore asks for auth when the item is hidden now`() {
        val ctx = Fx.ctx()
        val doc = hideNoah(Fx.doc())
        // The entry was written before 노아 was hidden; the gate still applies (one predicate).
        assertFalse(doc.index.trashEntry(K.TRASH_NOAH)!!.hiddenAtDelete)
        assertTrue(TrashGates.needsAuth(doc, doc.index.trashEntry(K.TRASH_NOAH)!!))
        assertEquals(RestoreOutcome.NeedsAuth, RestoreOps.restore(doc, K.TRASH_NOAH, ctx))
        assertTrue(Fx.restored(RestoreOps.restore(doc, K.TRASH_NOAH, ctx, authenticated = true)).doc.liveness.isCharacterLive(K.NOAH))
        // A block of a hidden scene follows its scene.
        val hiddenScene = Fx.doc().let { d -> d.copy(story = d.story.copy(units = d.story.units.map { if (it.key == K.SCENE_1_5_2) it.copy(hidden = true) else it })) }
        assertEquals(RestoreOutcome.NeedsAuth, RestoreOps.restore(hiddenScene, K.TRASH_BLOCKS_1_5_2, ctx))
    }

    @Test
    fun `비우기 leaves locked hidden entries`() {
        val ctx = Fx.ctx()
        val doc = hideNoah(Fx.doc())
        val locked = PurgeOps.emptyTrash(doc, ctx, hiddenUnlocked = false)
        assertEquals(listOf(K.TRASH_NOAH), locked.doc.trash.entries.map { it.key })
        assertTrue(locked.doc.liveness.let { !it.isCharacterLive(K.NOAH) } && locked.doc.index.character(K.NOAH) != null)
        val open = PurgeOps.emptyTrash(doc, ctx, hiddenUnlocked = true)
        assertTrue(open.doc.trash.entries.isEmpty())
        assertTrue(open.nameTags.single().hidden)
    }

    @Test
    fun `F-12 project replaced by 덮어쓰기`() {
        val ctx = Fx.ctx()
        val oldKey = K.PROJECT
        val newKey = Fx.key("PRJT", 9)
        val state = ProjectTrashState(LibraryIndexFile(projects = listOf(LibraryEntry(key = oldKey, name = Fx.PROJECT_NAME))), emptyList())
        val replaced = ProjectTrash.replaceByOverwrite(state, oldKey, newKey, ctx)
        val entry = replaced.entries.single()
        assertEquals(TrashKind.PROJECT, entry.kind.known)
        assertEquals(TrashReason.REPLACED_BY_OVERWRITE, entry.reason.known)
        assertEquals(LocalDate.parse("2026-12-22"), entry.purgeOn)
        assertEquals(listOf(newKey), entry.targetKeys)
        // The key K is free for the imported file.
        assertTrue(replaced.library.projects.none { it.key == oldKey })
        assertNotNull(replaced.library.projects.single { it.key == newKey }.deletedAt)
        val rekeyed = ProjectTrash.rekey(Fx.doc(), newKey)
        assertEquals(newKey, rekeyed.key)
        assertTrue(rekeyed.trash.entries.all { it.projectKey == newKey })
        assertEquals(Fx.doc().episodes.keys, rekeyed.episodes.keys)
        // Restoring the replaced copy gives a separate project K′.
        val back = ProjectTrash.restore(replaced, entry.key, ctx)!!
        assertNull(back.library.projects.single { it.key == newKey }.deletedAt)
    }

    @Test
    fun `project trash rows, gate and purge`() {
        val ctx = Fx.ctx()
        val hidden = Fx.key("PRJT", 3)
        val example = Fx.key("PRJT", 4)
        val library = LibraryIndexFile(
            projects = listOf(
                LibraryEntry(key = K.PROJECT, name = Fx.PROJECT_NAME),
                LibraryEntry(key = hidden, name = "비밀 정원", hidden = true),
                LibraryEntry(key = example, name = "예시", isExample = true, readOnly = true),
            ),
        )
        var state = ProjectTrashState(library, emptyList())
        state = ProjectTrash.delete(state, example, ctx)
        assertTrue(state.entries.isEmpty())
        state = ProjectTrash.delete(state, hidden, ctx)
        state = ProjectTrash.delete(state, K.PROJECT, ctx)
        val rows = TrashListing.projectRows(state.entries, state.library, mapOf(K.PROJECT to Fx.doc()), ctx)
        val masked = rows.single { it.projectKey == hidden }
        assertEquals("숨긴 프로젝트", masked.title)
        assertEquals("프로젝트 · 되살리기는 지문 확인", masked.sub)
        assertEquals("프로젝트 · 씬 34개 · 인물 11명", rows.single { it.projectKey == K.PROJECT }.sub)
        val hiddenEntry = state.entries.single { it.projectKey == hidden }
        assertNull(ProjectTrash.restore(state, hiddenEntry.key, ctx))
        assertNotNull(ProjectTrash.restore(state, hiddenEntry.key, ctx, authenticated = true))
        val (afterEmpty, gone) = ProjectTrash.emptyTrash(state, hiddenUnlocked = false)
        assertEquals(listOf(K.PROJECT), gone)
        assertEquals(listOf(hiddenEntry.key), afterEmpty.entries.map { it.key })
    }

    @Test
    fun `recorder trash delete restore purge`() {
        val ctx = Fx.ctx()
        val start = RecorderTrashState(ExampleRecorder.library(), ExampleRecorder.trashEntries())
        val deleted = RecorderTrash.delete(start, K.RECORDER_R1, ctx)
        val entry = deleted.entries.single { it.targetKeys == listOf(K.RECORDER_R1) }
        assertNull(entry.projectKey)
        assertEquals(TrashKind.RECORDER, entry.kind.known)
        // Its links stop counting as references (the 하르트 chip).
        assertEquals(0, Impact.of(Fx.doc(), ItemId.character(K.HART), deleted.library).count(RefKind.RECORDER_LINK))
        assertEquals(1, Impact.of(Fx.doc(), ItemId.character(K.HART), start.library).count(RefKind.RECORDER_LINK))
        val restored = RecorderTrash.restore(deleted, entry.key, ctx)!!
        assertNull(restored.library.record(K.RECORDER_R1)!!.deletedAt)
        val (purged, log) = RecorderTrash.purge(deleted, entry.key, ctx)
        assertNull(purged.library.record(K.RECORDER_R1))
        assertTrue(purged.library.links.none { it.recordKey == K.RECORDER_R1 })
        assertEquals(listOf(K.RECORDER_R1), log.map { it.key })
        // The fixture's voice record (deleted 10-24) is due on 11-23, not today.
        assertTrue(RecorderTrash.purgeDue(start, ctx).second.isEmpty())
        assertEquals(listOf(K.RECORDER_TRASHED), RecorderTrash.purgeDue(start, Fx.ctxAt("2026-11-23T00:10")).second.map { it.key })
    }

    @Test
    fun `recorder links to a purged item keep their label`() {
        val ctx = Fx.ctx()
        val result = PurgeOps.purge(Fx.doc(), K.TRASH_NOAH, ctx)
        val library = RecorderTrash.applyProjectPurge(ExampleRecorder.library(), K.PROJECT, result, ctx)
        val link = library.links.single { it.recordKey == K.RECORDER_NOAH }
        assertEquals(ctx.now, link.purgedAt)
        assertEquals("노아", link.lastLabel)
        assertFalse(link.lastLabelHidden)
        // Other links are untouched.
        assertTrue(library.links.filter { it.recordKey != K.RECORDER_NOAH }.all { it.purgedAt == null })
        val gone = RecorderTrash.applyProjectGone(ExampleRecorder.library(), K.PROJECT, Fx.PROJECT_NAME, hidden = false, ctx)
        assertTrue(gone.links.all { it.purgedAt != null })
        assertTrue(gone.placements.all { it.purgedAt != null })
    }
}
