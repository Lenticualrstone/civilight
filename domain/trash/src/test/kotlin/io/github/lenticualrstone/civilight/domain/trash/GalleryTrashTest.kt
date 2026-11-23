package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryFile
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_2
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_3
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_6
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_H1
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.STORED
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Gallery items in delete, restore, purge, undo, impact, merge and the trash list (55 §3.4, T-GAL-14/15/21/28/29/35). */
class GalleryTrashTest {
    private val ctx = Fx.ctx()
    private val g0: ProjectDoc = ExampleGallery.build()

    private fun liveKeys(doc: ProjectDoc, owner: String) =
        doc.index.galleryItemsOf(owner).filter { doc.liveness.isLive(it) }.map { it.key }

    private fun deleteRien(doc: ProjectDoc = g0) = TrashOps.delete(doc, ItemId.character(K.RIEN), ctx)

    @Test
    fun `T-GAL-14 an image goes to the trash with its own entry and comes back with undo`() {
        val result = TrashOps.delete(g0, ItemId.galleryItem(ITEM_6), ctx)
        val entry = result.entries.single()
        assertEquals(TrashKind.GALLERY_ITEM, entry.kind.known)
        assertEquals(listOf(ITEM_6), entry.targetKeys)
        assertFalse(ITEM_6 in liveKeys(result.doc, K.RIEN))
        assertEquals("이미지 지움 · 등불 고리", Messages.galleryDeleteToast(listOf("등불 고리")).message)
        assertEquals("이미지 지움", Messages.galleryDeleteToast(listOf("")).message)
        val undone = TrashOps.undo(result.doc, result)
        assertEquals(STORED, liveKeys(undone, K.RIEN))
        assertTrue(undone.trash.entries.none { it.key == entry.key })
    }

    @Test
    fun `restore puts the image back at its kept order and needs auth when hidden`() {
        val deleted = TrashOps.delete(g0, ItemId.galleryItem(ITEM_6), ctx)
        val outcome = RestoreOps.restore(deleted.doc, deleted.entries.single().key, ctx)
        val restored = (outcome as RestoreOutcome.Restored).doc
        assertEquals(STORED, liveKeys(restored, K.RIEN))

        val hidden = TrashOps.delete(g0, ItemId.galleryItem(ITEM_H1), ctx)
        assertTrue(hidden.entries.single().hiddenAtDelete)
        assertEquals(RestoreOutcome.NeedsAuth, RestoreOps.restore(hidden.doc, hidden.entries.single().key, ctx))
        assertTrue(RestoreOps.restore(hidden.doc, hidden.entries.single().key, ctx, authenticated = true) is RestoreOutcome.Restored)
    }

    @Test
    fun `the restore toast names the image, an untitled one by its kind word`() {
        fun toast(key: String): String {
            val deleted = TrashOps.delete(g0, ItemId.galleryItem(key), ctx)
            val restored = RestoreOps.restore(deleted.doc, deleted.entries.single().key, ctx) as RestoreOutcome.Restored
            return Messages.restoreToast(restored.report).message
        }
        assertEquals("등불 고리 되살림", toast(ITEM_6))
        assertEquals("이미지 되살림", toast(ExampleGallery.ITEM_4))
    }

    @Test
    fun `T-GAL-15 deleting the owner counts its images and they come back with it`() {
        val impact = Impact.of(g0, ItemId.character(K.RIEN))
        assertEquals(10, impact.owned(OwnedKind.GALLERY_ITEM))
        assertEquals(2, impact.hiddenGalleryItems)
        assertTrue(Messages.impactSheet(impact).ownedRows.any { it.label == "이미지 10" })
        assertTrue(Messages.impactSheet(impact, hiddenLocked = true).ownedRows.any { it.label == "이미지 8 · 숨김 2" })
        // Two-column rows keep the compound count together: `이미지` | `8 · 숨김 2`, never `이미지 8 · 숨김` | `2`.
        val locked = Messages.impactSheet(impact, hiddenLocked = true).ownedRows.single { it.label.startsWith("이미지") }
        assertEquals("이미지", locked.head)
        assertEquals("8 · 숨김 2", locked.count)
        val open = Messages.impactSheet(impact).ownedRows.single { it.label.startsWith("이미지") }
        assertEquals("이미지", open.head)
        assertEquals("10", open.count)

        val result = deleteRien()
        // Not live by derivation, no own trash entries, their originals still referenced.
        assertTrue(liveKeys(result.doc, K.RIEN).isEmpty())
        assertTrue(result.doc.trash.entries.none { it.kind.known == TrashKind.GALLERY_ITEM })
        assertTrue(ExampleGallery.hashOf(3) in PurgeOps.referencedBlobs(result.doc))

        val restored = (RestoreOps.restore(result.doc, result.entries.single().key, ctx) as RestoreOutcome.Restored).doc
        assertEquals(STORED, liveKeys(restored, K.RIEN))
    }

    @Test
    fun `T-GAL-28 owner delete undo, then purge removes the images and frees their blobs`() {
        val result = deleteRien()
        assertEquals(STORED, liveKeys(TrashOps.undo(result.doc, result), K.RIEN))

        val purge = PurgeOps.purge(result.doc, result.entries.single().key, ctx)
        assertTrue(purge.doc.galleryItems.none { it.ownerKey == K.RIEN })
        assertFalse(K.RIEN in purge.doc.gallery)
        assertTrue(STORED.all { it in purge.removedKeys })
        // Every original only 리엔's rows used (her images and her portrait) is collected.
        val hashes = listOf(1, 2, 3, 4, 91, 5, 6, 7, 8, 92).map(ExampleGallery::hashOf)
        assertTrue(purge.unreferencedBlobs.containsAll(hashes))
        assertTrue(purge.doc.assets.blobs.none { it.hash in hashes })
    }

    @Test
    fun `T-GAL-21 purging an image frees its source and its blob`() {
        val deleted = TrashOps.delete(g0, ItemId.galleryItem(ITEM_6), ctx)
        assertNotNull(deleted.doc.index.galleryItemsWithSource(ExampleGallery.hashOf(6)).singleOrNull())
        val purge = PurgeOps.purge(deleted.doc, deleted.entries.single().key, ctx)
        assertTrue(purge.doc.index.galleryItemsWithSource(ExampleGallery.hashOf(6)).isEmpty())
        assertEquals(listOf(ExampleGallery.hashOf(6)), purge.unreferencedBlobs)
        assertNull(purge.doc.index.galleryItem(ITEM_6))
    }

    @Test
    fun `T-GAL-35 trash rows of images, under 인물 for a character's image, masked when hidden`() {
        val deleted = TrashOps.delete(g0, ItemId.galleryItem(ITEM_6), ctx)
        val rows = TrashListing.projectItemRows(deleted.doc, ctx)
        val row = rows.single { it.kind.known == TrashKind.GALLERY_ITEM }
        assertEquals("등불 고리", row.title)
        assertEquals("리엔 · 갤러리", row.sub)
        assertTrue(TrashListing.list(rows, TrashFilter.CHARACTER).any { it.entryKey == row.entryKey })
        assertTrue(TrashListing.list(rows, TrashFilter.ALL).any { it.entryKey == row.entryKey })
        assertFalse(TrashListing.list(rows, TrashFilter.WORLD).any { it.entryKey == row.entryKey })

        val hidden = TrashOps.delete(g0, ItemId.galleryItem(ITEM_H1), ctx)
        val masked = TrashListing.projectItemRows(hidden.doc, ctx, HidingLocks(itemsLocked = true))
            .single { it.kind.known == TrashKind.GALLERY_ITEM }
        assertEquals("숨긴 이미지", masked.title)
        assertTrue(masked.masked)
        assertTrue(masked.restoreNeedsAuth)
    }

    @Test
    fun `a project image shows only under 전체`() {
        val doc = g0.copy(
            gallery = g0.gallery + (K.PROJECT to GalleryFile(GalleryOwnerKind.PROJECT.open(), K.PROJECT, listOf(
                GalleryItem(key = ExampleGallery.item(50), ownerKind = GalleryOwnerKind.PROJECT.open(), ownerKey = K.PROJECT, source = "preset:ash-dawn", order = "V", title = "표지 시안"),
            ))),
        )
        val deleted = TrashOps.delete(doc, ItemId.galleryItem(ExampleGallery.item(50)), ctx)
        val rows = TrashListing.projectItemRows(deleted.doc, ctx)
        val row = rows.single { it.kind.known == TrashKind.GALLERY_ITEM }
        assertEquals("잿빛 새벽의 등불 · 갤러리", row.sub)
        assertFalse(TrashListing.list(rows, TrashFilter.CHARACTER).any { it.entryKey == row.entryKey })
    }

    @Test
    fun `the project trash re-key moves the project gallery`() {
        val item = GalleryItem(key = ExampleGallery.item(51), ownerKind = GalleryOwnerKind.PROJECT.open(), ownerKey = K.PROJECT, source = "preset:ash-dawn", order = "V")
        val doc = g0.copy(gallery = g0.gallery + (K.PROJECT to GalleryFile(GalleryOwnerKind.PROJECT.open(), K.PROJECT, listOf(item))))
        val newKey = "01JZ000000PRJT000000000078"
        val next = ProjectTrash.rekey(doc, newKey)
        assertNull(next.gallery[K.PROJECT])
        assertEquals(newKey, next.gallery.getValue(newKey).ownerKey)
        assertEquals(newKey, next.gallery.getValue(newKey).items.single().ownerKey)
        assertTrue(next.liveness.isGalleryItemLive(item.key))
        assertEquals(g0.gallery[K.RIEN], next.gallery[K.RIEN])
    }

    @Test
    fun `T-GAL-29 merge moves the images after the target's own, keeps a duplicate source, and undoes`() {
        val seraOwn = GalleryItem(
            key = ExampleGallery.item(60), ownerKind = GalleryOwnerKind.CHARACTER.open(), ownerKey = K.SERA,
            source = ExampleGallery.hashOf(2), order = "V", title = "세라의 외출복",
        )
        val doc = g0.copy(gallery = g0.gallery + (K.SERA to GalleryFile(GalleryOwnerKind.CHARACTER.open(), K.SERA, listOf(seraOwn))))
        val result = MergeOps.merge(doc, ItemKind.CHARACTER, K.RIEN, K.SERA, ctx)
        val expectedMoved = STORED - ITEM_2
        assertEquals(listOf(seraOwn.key) + expectedMoved, result.doc.index.galleryItemsOf(K.SERA).map { it.key })
        assertEquals(listOf(ITEM_2), result.doc.index.galleryItemsOf(K.RIEN).map { it.key })
        val moved = result.record.movedRefs.filter { it.ownerKind == MergeOps.GALLERY_ITEM_REF }
        assertEquals(expectedMoved, moved.map { it.ownerKey })
        assertTrue(moved.all { it.field == "ownerKey" })

        val undone = MergeOps.undo(result.doc, result)
        assertEquals(STORED, undone.index.galleryItemsOf(K.RIEN).map { it.key })
        assertEquals(listOf(seraOwn.key), undone.index.galleryItemsOf(K.SERA).map { it.key })
    }

    @Test
    fun `revert keeps a later edit of another image`() {
        val result = TrashOps.delete(g0, ItemId.galleryItem(ITEM_6), ctx)
        val f = result.doc.gallery.getValue(K.RIEN)
        val edited = result.doc.copy(gallery = result.doc.gallery + (K.RIEN to f.copy(items = f.items.map { if (it.key == ITEM_3) it.copy(title = "새 제목") else it })))
        val undone = TrashOps.undo(edited, result)
        assertEquals("새 제목", undone.index.galleryItem(ITEM_3)?.title)
        assertTrue(undone.liveness.isGalleryItemLive(ITEM_6))
    }

    @Test
    fun `@ links in descriptions are references and move with a merge`() {
        val link = TextRun.Link("01JZ000000DCBK000000009999", LinkTarget(LinkTargetKind.CHARACTER.open(), K.OWEN), "오웬")
        val f = g0.gallery.getValue(K.RIEN)
        val doc = g0.copy(gallery = g0.gallery + (K.RIEN to f.copy(items = f.items.map {
            if (it.key == ITEM_3) it.copy(caption = listOf(DocBlock("01JZ000000DCBK000000009998", "V", runs = listOf(TextRun.Plain("함께: "), link)))) else it
        })))
        val before = ReferenceScanner(doc).linksTo(K.OWEN).count { it.surface == LinkSurface.GALLERY }
        assertEquals(1, before)
        assertTrue(Impact.of(doc, ItemId.character(K.OWEN)).count(RefKind.LINK) >= 1)
        val merged = MergeOps.merge(doc, ItemKind.CHARACTER, K.OWEN, K.SERA, ctx)
        val caption = merged.doc.index.galleryItem(ITEM_3)!!.caption!!
        assertEquals(K.SERA, caption.flatMap { it.runs }.filterIsInstance<TextRun.Link>().single().target.key)
        assertTrue(merged.record.movedRefs.any { it.ownerKind == "galleryItem" && it.field == "caption" })
    }

    @Test
    fun `sheet row columns split on the last word unless the count is named`() {
        assertEquals("관계" to "1", SheetRow("관계 1").let { it.head to it.count })
        assertEquals("◆ 변화 기록" to "2", SheetRow("◆ 변화 기록 2").let { it.head to it.count })
        assertEquals("이미지" to "0 · 숨김 1", SheetRow("이미지 0 · 숨김 1", countText = "0 · 숨김 1").let { it.head to it.count })
        assertEquals("블록" to "", SheetRow("블록").let { it.head to it.count })
    }
}
