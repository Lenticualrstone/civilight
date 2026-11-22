package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.refs.DeletedItems
import io.github.lenticualrstone.civilight.domain.library.hiding.ItemSurfaces
import io.github.lenticualrstone.civilight.domain.trash.ProjectTrashState
import io.github.lenticualrstone.civilight.domain.trash.PurgeOps
import io.github.lenticualrstone.civilight.domain.trash.RecorderTrashState
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.app.LibraryIndexFile
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.doc.BlobRefs
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryFile
import io.github.lenticualrstone.civilight.model.gallery.GalleryItem
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_1
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_2
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_3
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_4
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_5
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_6
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_8
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_H1
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.STORED
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** `GalleryActions` and the character actions carrying gallery items (55 §3.4; T-GAL-09…38). */
class GalleryActionsTest {
    private val ctx = Fx.ctx()
    private val g0: ProjectDoc = ExampleGallery.build()
    private val rien = GalleryOwner.character(K.RIEN)

    private fun visible(doc: ProjectDoc, locked: Boolean = true) = GalleryActions.visibleItems(doc, rien, locked).map { it.key }

    private fun blob(tag: String) = Blob(
        hash = tag.padEnd(64, '0'), fileExt = "png", mime = "image/png", bytes = 1000L * tag.length,
        source = BlobSource.GALLERY.open(), importedAt = ExampleClock.NOW,
    )

    private fun trashed(doc: ProjectDoc, key: Key) = TrashOps.delete(doc, ItemId.galleryItem(key), ctx.trash).doc

    @Test
    fun `T-GAL-09 add keeps the chosen order, skips a visible duplicate and is one undo step (T-GAL-33)`() {
        val b = blob("b"); val a = blob("a"); val c = blob("c")
        val sources = listOf(GalleryAddSource(b.hash, b), GalleryAddSource(a.hash, a), GalleryAddSource(ExampleGallery.hashOf(3)), GalleryAddSource(c.hash, c))
        assertEquals(GallerySourceMark.ALREADY_THERE, GalleryActions.sourceMark(g0, rien, ExampleGallery.hashOf(3), locked = true))
        val r = GalleryActions.add(g0, rien, sources, ctx, locked = true)
        assertEquals(3, r.added.size)
        assertEquals(1, r.alreadyThere)
        assertEquals("3장 넣음 · 이미 있음 1", r.toast.message)
        assertEquals("되돌리기", r.toast.action)
        // Visible items 9, 10, 11 at the end, in the chosen order B, A, C.
        val shown = visible(r.doc)
        assertEquals(11, shown.size)
        assertEquals(r.added, shown.takeLast(3))
        assertEquals(listOf(b.hash, a.hash, c.hash), r.added.map { r.doc.index.galleryItem(it)!!.source })
        assertEquals(1, r.doc.index.galleryItemsWithSource(ExampleGallery.hashOf(3)).count { it.ownerKey == K.RIEN })
        assertTrue(r.doc.assets.blobs.any { it.hash == a.hash })
        // One undo step takes all three away.
        val undone = GalleryActions.undo(r.doc, r.change)
        assertEquals(visible(g0), visible(undone))
        assertTrue(undone.assets.blobs.none { it.hash == a.hash })
    }

    @Test
    fun `T-GAL-20 a source only a hidden item holds is skipped silently while locked`() {
        val hidden = ExampleGallery.hashOf(91)
        assertEquals(GallerySourceMark.NONE, GalleryActions.sourceMark(g0, rien, hidden, locked = true))
        assertEquals(GallerySourceMark.ALREADY_THERE, GalleryActions.sourceMark(g0, rien, hidden, locked = false))
        val fresh = blob("f")
        val r = GalleryActions.add(g0, rien, listOf(GalleryAddSource(hidden), GalleryAddSource(fresh.hash, fresh)), ctx, locked = true)
        assertEquals(1, r.added.size)
        assertEquals("1장 넣음", r.toast.message)
    }

    @Test
    fun `T-GAL-21 a trashed item still holds its source until it is purged`() {
        val deleted = GalleryActions.delete(g0, listOf(ITEM_6), ctx)!!
        assertEquals("이미지 지움 · 등불 고리", deleted.toast.message)
        val doc = deleted.doc
        val six = ExampleGallery.hashOf(6)
        assertEquals(GallerySourceMark.IN_TRASH, GalleryActions.sourceMark(doc, rien, six, locked = true))
        assertEquals("휴지통에 있음", GallerySourceMark.IN_TRASH.label)
        val again = GalleryActions.add(doc, rien, listOf(GalleryAddSource(six)), ctx, locked = true)
        assertTrue(again.added.isEmpty())
        assertEquals("0장 넣음 · 이미 있음 1", again.toast.message)
        assertNull(again.toast.action)

        val entry = deleted.result.entries.single().key
        val restored = (GalleryActions.restore(doc, entry, ctx) as RestoreOutcome.Restored).doc
        assertEquals(1, restored.index.galleryItemsWithSource(six).size)
        assertEquals(STORED, GalleryActions.items(restored, rien).map { it.key })

        val purged = GalleryActions.purge(doc, entry, ctx).doc
        assertEquals(GallerySourceMark.NONE, GalleryActions.sourceMark(purged, rien, six, locked = true))
        assertEquals(1, GalleryActions.add(purged, rien, listOf(GalleryAddSource(six, ExampleGallery.blob(6))), ctx, locked = true).added.size)
    }

    @Test
    fun `T-GAL-22 copying to the project keeps every field and the hidden flag`() {
        val f = g0.gallery.getValue(K.RIEN)
        val doc = g0.copy(gallery = g0.gallery + (K.RIEN to f.copy(items = f.items.map {
            if (it.key == ITEM_H1) it.copy(holo = true, sceneKey = K.SCENE_1_6_1, caption = listOf(DocBlock("01JZ000000DCBK000000009990", "V", runs = listOf(TextRun.Plain("숨긴 메모")))),
                crops = mapOf("r3x4" to io.github.lenticualrstone.civilight.model.asset.Crop(0, 0, 900, 1200))) else it
        })))
        val project = GalleryOwner.project(doc)
        val r = GalleryActions.copyTo(doc, listOf(ITEM_H1), project, ctx, locked = false)
        val copy = r.doc.index.galleryItem(r.added.single())!!
        val from = doc.index.galleryItem(ITEM_H1)!!
        assertEquals(from.copy(key = copy.key, ownerKind = copy.ownerKind, ownerKey = copy.ownerKey, order = copy.order, createdAt = copy.createdAt, updatedAt = copy.updatedAt, updatedBy = copy.updatedBy), copy)
        assertTrue(copy.hidden)
        assertEquals(K.PROJECT, copy.ownerKey)
        assertTrue(GalleryActions.visibleItems(r.doc, project, locked = true).isEmpty())
        assertEquals(1, GalleryActions.visibleItems(r.doc, project, locked = false).size)
        // Copying again finds it there.
        assertEquals(0, GalleryActions.copyTo(r.doc, listOf(ITEM_H1), project, ctx, locked = false).added.size)
    }

    @Test
    fun `T-GAL-11 order buttons move one visible place, stop at the ends, one undo step each`() {
        val forward = GalleryActions.move(g0, ITEM_3, GalleryMove.FORWARD, ctx, locked = true)!!
        assertEquals(listOf(ITEM_1, ITEM_3, ITEM_2), visible(forward.doc).take(3))
        val first = GalleryActions.move(forward.doc, ITEM_3, GalleryMove.FIRST, ctx, locked = true)!!
        assertEquals(ITEM_3, visible(first.doc).first())
        assertNull(GalleryActions.move(first.doc, ITEM_3, GalleryMove.FORWARD, ctx, locked = true))
        assertNull(GalleryActions.move(g0, ITEM_8, GalleryMove.LAST, ctx, locked = true))
        val back = GalleryActions.undo(first.doc, first.change)
        assertEquals(visible(forward.doc), visible(back))
        // Storage only: the other items keep their order keys.
        assertEquals(g0.index.galleryItem(ITEM_1)!!.order, first.doc.index.galleryItem(ITEM_1)!!.order)
    }

    @Test
    fun `T-GAL-24 a move skips hidden items while locked`() {
        // Stored: 4, H1, 5. Item 5 forward goes before 4 (one visible place).
        val r = GalleryActions.move(g0, ITEM_5, GalleryMove.FORWARD, ctx, locked = true)!!
        val shown = visible(r.doc)
        assertEquals(listOf(ITEM_3, ITEM_5, ITEM_4), shown.subList(2, 5))
        assertEquals(4, shown.indexOf(ITEM_5) + 1)
    }

    @Test
    fun `T-GAL-25 이 뒤로 places the item right after the target among 128 and undoes in one step`() {
        val keys = (1..128).map { ExampleGallery.item(1000 + it) }
        val orders = OrderKeys.evenlySpaced(128)
        val items = keys.mapIndexed { i, k ->
            GalleryItem(key = k, ownerKind = GalleryOwnerKind.CHARACTER.open(), ownerKey = K.SERA, source = "preset:p$i", order = orders[i])
        }
        val doc = g0.copy(gallery = g0.gallery + (K.SERA to GalleryFile(GalleryOwnerKind.CHARACTER.open(), K.SERA, items)))
        val sera = GalleryOwner.character(K.SERA)
        val r = GalleryActions.moveAfter(doc, keys[2], keys[79], ctx, locked = true)!!
        val shown = GalleryActions.visibleItems(r.doc, sera, locked = true).map { it.key }
        assertEquals(80, shown.indexOf(keys[2]) + 1)
        assertEquals(keys[79], shown[shown.indexOf(keys[2]) - 1])
        assertEquals(keys, GalleryActions.visibleItems(GalleryActions.undo(r.doc, r.change), sera, locked = true).map { it.key })
        // `맨 앞으로`.
        val front = GalleryActions.moveAfter(doc, keys[50], null, ctx, locked = true)!!
        assertEquals(keys[50], GalleryActions.visibleItems(front.doc, sera, locked = true).first().key)
    }

    @Test
    fun `order keys that leave no room are rebalanced in the same step`() {
        val items = listOf("a", "b", "c").mapIndexed { i, s ->
            GalleryItem(key = ExampleGallery.item(1200 + i), ownerKind = GalleryOwnerKind.CHARACTER.open(), ownerKey = K.SERA, source = "preset:$s", order = "V")
        }
        val doc = g0.copy(gallery = g0.gallery + (K.SERA to GalleryFile(GalleryOwnerKind.CHARACTER.open(), K.SERA, items)))
        val r = GalleryActions.moveAfter(doc, items[0].key, items[1].key, ctx, locked = true)!!
        val shown = GalleryActions.visibleItems(r.doc, GalleryOwner.character(K.SERA), locked = true).map { it.key }
        assertEquals(listOf(items[1].key, items[0].key, items[2].key), shown)
        assertEquals(3, r.doc.index.galleryItemsOf(K.SERA).map { it.order }.distinct().size)
    }

    @Test
    fun `edits change only the item's own fields`() {
        val r = GalleryActions.setTitle(g0, ITEM_4, "  창가의 리엔 ", ctx)!!
        assertEquals("창가의 리엔", r.doc.index.galleryItem(ITEM_4)!!.title)
        assertEquals(g0.index.galleryItem(ITEM_4)!!.order, r.doc.index.galleryItem(ITEM_4)!!.order)
        val tagged = GalleryActions.setTags(r.doc, ITEM_4, listOf(ExampleGallery.TAG_PROP, "01JZ000000TAGS000000009999"), ctx)!!
        assertEquals(listOf(ExampleGallery.TAG_PROP), tagged.doc.index.galleryItem(ITEM_4)!!.tagKeys)
        assertNull(GalleryActions.setCaption(g0, ITEM_3, emptyList(), ctx)!!.doc.index.galleryItem(ITEM_3)!!.caption)
        assertEquals(g0.index.galleryItem(ITEM_2), GalleryActions.undo(r.doc, r.change).index.galleryItem(ITEM_2))
        assertEquals("", GalleryActions.undo(r.doc, r.change).index.galleryItem(ITEM_4)!!.title)
    }

    @Test
    fun `T-GAL-38 gallery bytes count each distinct blob once and presets as zero`() {
        val f = g0.gallery.getValue(K.RIEN)
        val doc = g0.copy(gallery = g0.gallery + (K.RIEN to f.copy(items = f.items.map { if (it.key == ITEM_8) it.copy(source = "preset:fog") else it })))
        val expected = listOf(1, 2, 3, 4, 91, 5, 6, 7, 92).sumOf { ExampleGallery.bytesOf(it).size.toLong() }
        assertEquals(expected, GalleryActions.galleryBytes(doc))
        val shared = GalleryActions.copyTo(doc, listOf(ITEM_2), GalleryOwner.project(doc), ctx, locked = false).doc
        assertEquals(expected, GalleryActions.galleryBytes(shared))
    }

    @Test
    fun `T-GAL-15 T-GAL-28 character delete, restore and purge carry the images`() {
        val deleted = CharacterActions.delete(g0, K.RIEN, ctx)!!
        assertEquals(10, deleted.impact.owned(io.github.lenticualrstone.civilight.domain.trash.OwnedKind.GALLERY_ITEM))
        assertEquals(STORED, DeletedItems.snapshotOf(deleted.entry)!!.galleryItemKeys)
        assertTrue(GalleryActions.items(deleted.doc, rien).isEmpty())
        assertEquals(STORED, GalleryActions.items(CharacterActions.undo(deleted.doc, deleted), rien).map { it.key })
        val restored = CharacterActions.restore(deleted.doc, K.RIEN, ctx)
        assertEquals(STORED, GalleryActions.items((restored.outcome as RestoreOutcome.Restored).doc, rien).map { it.key })
        val purged = CharacterActions.purge(deleted.doc, Fx.recorder(), K.RIEN, ctx)!!
        assertTrue(purged.doc.galleryItems.none { it.ownerKey == K.RIEN })
    }

    @Test
    fun `T-GAL-27 one blob reference set, and unrelated purges keep gallery originals`() {
        val old = Fx.ctxAt("2026-11-01T09:00:00")
        val later = Fx.ctxAt("2027-01-15T09:00:00")
        // 오웬 deleted long ago (due), item 6 deleted the day before (not due).
        val owenGone = CharacterActions.delete(g0, K.OWEN, old)!!.doc
        val doc = GalleryActions.delete(owenGone, listOf(ITEM_6), Fx.ctxAt("2027-01-14T09:00:00"))!!.doc
        val two = ExampleGallery.hashOf(2)
        val six = ExampleGallery.hashOf(6)
        assertEquals(BlobRefs.of(doc), PurgeOps.referencedBlobs(doc))
        assertEquals(BlobRefs.of(doc) + doc.assets.blobs.map { it.hash }, PurgeActions.heldBlobs(doc))
        assertTrue(two in PurgeActions.heldBlobs(doc) && six in PurgeActions.heldBlobs(doc))

        val run = PurgeActions.runDue(
            later, listOf(doc), ProjectTrashState(LibraryIndexFile(), emptyList()),
            RecorderTrashState(ExampleRecorder.library(), emptyList()),
        )
        val after = run.projects.getValue(K.PROJECT)
        assertNull(after.index.character(K.OWEN))
        assertFalse(two in run.blobsToDelete)
        assertFalse(six in run.blobsToDelete)
        assertTrue(two in PurgeOps.referencedBlobs(after) && six in PurgeOps.referencedBlobs(after))
        assertNotNull(after.index.blob(six))
    }

    @Test
    fun `T-GAL-31 removing a tag removes it from images in the same edit`() {
        val (withTag, tag) = CharacterEdits.addTag(g0, "회상", ctx.edit)
        val tagged = GalleryActions.setTags(withTag, ITEM_4, listOf(tag.key), ctx)!!.doc
        assertEquals("인물 0 · 이미지 1", CharacterEdits.tagUsage(tagged, tag.key).line)
        val renamed = CharacterEdits.renameTag(tagged, tag.key, "옛 기억", ctx.edit)!!
        assertEquals("옛 기억", renamed.index.tag(tag.key)!!.name)
        val removed = CharacterEdits.removeTag(renamed, tag.key, ctx.edit)
        assertNull(removed.index.tag(tag.key))
        assertTrue(removed.galleryItems.none { tag.key in it.tagKeys })
        // Live images: 1, 3, H1, 5, 7.
        assertEquals(5, CharacterEdits.tagUsage(g0, ExampleGallery.TAG_ILLUSTRATION).images)
    }

    @Test
    fun `T-GAL-06 visible counts leave hidden images out while locked`() {
        assertEquals(8, GalleryActions.visibleItems(g0, rien, locked = true).size)
        assertEquals(10, GalleryActions.visibleItems(g0, rien, locked = false).size)
        val masked = ItemSurfaces(g0).gallery(K.RIEN, locked = true)
        assertEquals(2, masked.lockedRow?.count)
        val items = GalleryActions.visibleItems(g0, rien, locked = true)
        val counts = items.flatMap { it.tagKeys }.groupingBy { g0.index.tag(it)!!.name }.eachCount()
        assertEquals(mapOf("삽화" to 4, "설정화" to 2, "소품" to 1), counts)
    }
}
