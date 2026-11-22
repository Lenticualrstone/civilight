package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.actions.GalleryActions
import io.github.lenticualrstone.civilight.domain.library.hiding.HiddenItems
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.TrashFilter
import io.github.lenticualrstone.civilight.feature.library.backup.RecoveryPasswordState
import io.github.lenticualrstone.civilight.feature.library.export.ExportGalleryLine
import io.github.lenticualrstone.civilight.feature.library.trash.TrashDecisions
import io.github.lenticualrstone.civilight.feature.library.trash.TrashInputs
import io.github.lenticualrstone.civilight.feature.library.trash.TrashState
import io.github.lenticualrstone.civilight.model.app.LibraryIndexFile
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_4
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_6
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery.ITEM_H1
import io.github.lenticualrstone.civilight.ui.security.UnlockState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The 서재 side of the gallery (55 §3.4, §6 Q12, Q13; LIB-36, LIB-38): hideable images, trash rows, the export row. */
class GalleryLibraryTest {
    private val ctx = LibFx.ctx()
    private val g0: ProjectDoc = ExampleGallery.build()

    private fun trashOf(doc: ProjectDoc, vararg keys: String) = GalleryActions.delete(doc, keys.toList(), ctx)!!.doc

    private fun ui(doc: ProjectDoc, unlock: UnlockState = UnlockState()) = TrashState.build(
        TrashInputs(LibraryIndexFile(), emptyList(), emptyMap(), listOf(doc), RecorderLibrary(), emptyList()),
        unlock, ctx.trash,
    )

    // ---- LIB-38: trash rows of kind galleryItem ----

    @Test
    fun `LIB-38 a deleted image is a row under 전체 and, as the character's image, under 인물`() {
        val ui = ui(trashOf(g0, ITEM_4, ITEM_6))
        val untitled = ui.rows.first { it.row.title == "제목 없음" }
        val titled = ui.rows.first { it.row.title == "등불 고리" }
        assertEquals("리엔 · 갤러리", untitled.row.sub)
        assertTrue(untitled in ui.visible(TrashFilter.ALL) && titled in ui.visible(TrashFilter.ALL))
        assertTrue("a character's image also shows under 인물 (55 Q12)", untitled in ui.visible(TrashFilter.CHARACTER))
        assertFalse(untitled in ui.visible(TrashFilter.SCENE))
        assertFalse(untitled.row.masked)
    }

    @Test
    fun `LIB-38 a hidden image reads 숨긴 이미지 while locked and its restore asks for the prompt`() {
        val doc = trashOf(g0, ITEM_H1)
        val locked = ui(doc).rows.first { it.row.title == "숨긴 이미지" }
        assertEquals("숨긴 이미지", locked.row.title)
        assertTrue(locked.row.masked)
        assertTrue(locked.row.restoreNeedsAuth)
        val entry = doc.trash.entries.first { it.targetKeys == listOf(ITEM_H1) }.key
        assertEquals(RestoreOutcome.NeedsAuth, TrashDecisions.restoreItem(doc, entry, ctx, authenticated = false).outcome)
        val open = ui(doc, UnlockState(itemsUnlockedAt = mapOf(doc.key to LibFx.NOW)))
        assertTrue(open.rows.any { it.row.title == "숨긴 스케치" })
        assertFalse(open.rows.any { it.row.title == "숨긴 이미지" })
    }

    @Test
    fun `LIB-38 restoring an image goes through the gallery action and puts it back with its toast`() {
        val deleted = trashOf(g0, ITEM_4, ITEM_6)
        val entry = deleted.trash.entries.first { it.targetKeys == listOf(ITEM_4) }.key
        val result = TrashDecisions.restoreItem(deleted, entry, ctx, authenticated = false)
        val restored = result.outcome as RestoreOutcome.Restored
        assertEquals("이미지 되살림", result.toast)
        assertNotNull(restored.doc.galleryItems.first { it.key == ITEM_4 }.also { assertNull(it.deletedAt) })
        val titled = deleted.trash.entries.first { it.targetKeys == listOf(ITEM_6) }.key
        assertEquals("등불 고리 되살림", TrashDecisions.restoreItem(deleted, titled, ctx, authenticated = false).toast)
    }

    // ---- LIB-36: hideable images ----

    @Test
    fun `LIB-36 hidden gallery images are hidden items, so the counts that follow them include the images`() {
        val hidden = HiddenItems(g0)
        assertEquals(setOf(ITEM_H1, ExampleGallery.ITEM_H2), hidden.galleryImages)
        assertEquals(hidden.galleryImages.size + (hidden.count - hidden.galleryImages.size), hidden.count)
        assertTrue("the encryption default and the backup warning count them", hidden.any)
        val plain = ExampleGallery.addTo(LibFx.ashen()).let { doc -> GalleryActions.setHidden(doc, ITEM_H1, false, ctx)!!.doc }
        assertTrue(HiddenItems(plain).galleryImages.size < hidden.galleryImages.size)
        // 복구 암호 screen: `숨긴 항목 <n>` adds the project's hidden images to its other hidden items.
        val rows = RecoveryPasswordState.build("".toCharArray(), "".toCharArray(), hiddenProjects = 0, visibleDocs = listOf(g0)).rows
        assertEquals("숨긴 항목 ${hidden.count}", rows.first().label)
    }

    // ---- Q13: the export row ----

    @Test
    fun `Q13 the export row counts the originals the file carries, and leaves hidden ones out while locked`() {
        val open = ExportGalleryLine.of(g0, locked = false)!!
        val locked = ExportGalleryLine.of(g0, locked = true)!!
        assertEquals(10, open.images)
        assertEquals("hidden images are not counted while locked (GAL-19)", 8, locked.images)
        assertEquals("갤러리 이미지 10", open.label)
        assertEquals(GalleryActions.galleryBytes(g0), open.bytes)
        assertTrue(locked.bytes < open.bytes)
        assertEquals("갤러리 이미지 8 · ${locked.value}", locked.text)
        // A trashed image is not carried unless 휴지통도 포함, so it is not in the row either.
        assertEquals(9, ExportGalleryLine.of(trashOf(g0, ITEM_6), locked = false)!!.images)
        // Nothing stored, no row.
        assertNull(ExportGalleryLine.of(LibFx.ashen(), locked = false))
    }
}
