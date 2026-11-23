package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.designsystem.card.CardViewMotion
import io.github.lenticualrstone.civilight.feature.canvas.page.PageListMotion
import io.github.lenticualrstone.civilight.feature.canvas.image.CropHandOff
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageEdits
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwner
import io.github.lenticualrstone.civilight.feature.canvas.image.pick.ImagePickModel
import io.github.lenticualrstone.civilight.feature.canvas.image.pick.PickAction
import io.github.lenticualrstone.civilight.model.asset.Blob
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.open
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** The picker sheet (CNV-27), the picker → crop hand-off and the list ↔ card motion (CNV-45). */
class ImagePickAndMotionTest {
    private val cover = ImageOwner(ImageOwnerKind.PROJECT, K.PROJECT, ImageSlot.PROJECT_COVER)
    private val header = ImageOwner(ImageOwnerKind.PAGE, K.PAGE_LOWER_LIGHT, ImageSlot.PAGE_HEADER)
    private val hash = "f".repeat(64)
    private val blob = Blob(hash = hash, fileExt = "png", mime = "image/png", bytes = 10, width = 3024, height = 4032, source = BlobSource.FILE.open(), importedAt = 7L)

    @Test
    fun `the cover picker reads as the board`() {
        val v = ImagePickModel.build(CanvasFx.s0(), cover, holoDefaultForNewImages = false)
        assertEquals("표지 이미지", v.title)
        assertEquals("잿빛 새벽의 등불 · 카드 3:4 · 목록 1:1", v.subtitle)
        // U-08 LIB-84: the cover slot adds `갤러리` first (the example has no project gallery items).
        assertEquals(listOf("갤러리 0", "기본 이미지 8", "내 이미지 0"), v.tabs)
        assertEquals(8, v.presets.size)
        assertTrue(v.holoVisible)
        // The example's cover is the preset 잿빛 새벽: selected on the presets tab.
        assertEquals("preset:ash-dawn", v.initialSource)
        assertEquals(1, v.initialTab)
        assertEquals("지금 표지 · 잿빛 새벽", v.coverLine?.text)
    }

    @Test
    fun `an original in the slot opens on my images and leads to the crop editor`() {
        var doc = ImageEdits.addBlob(CanvasFx.s0(), blob)
        doc = ImageEdits.placeBlob(doc, header, hash, emptyMap(), holo = true, ctx = CanvasFx.ctx())
        val v = ImagePickModel.build(doc, header, holoDefaultForNewImages = true)
        assertEquals(listOf("기본 이미지 8", "내 이미지 1"), v.tabs)
        assertEquals("3024 × 4032", v.mine.single().name)
        assertEquals(ImagePickModel.TAB_MINE, v.initialTab)
        assertFalse(v.holoVisible)
        assertFalse(v.initialHolo)
        assertEquals(PickAction.Crop(hash), ImagePickModel.action(hash))
        assertEquals(PickAction.PlacePreset("nebula"), ImagePickModel.action("preset:nebula"))
        assertNull(ImagePickModel.action(null))
        // An original whose file was collected from this device is not offered.
        assertEquals(emptyList<Any>(), ImagePickModel.build(doc, header, true, fileExists = { false }).mine)
    }

    @Test
    fun `the hand-off is taken once and only for its own image`() {
        CropHandOff.put(CropHandOff(K.PROJECT, ImageSlot.PROJECT_COVER, hash, null, holo = true))
        assertNull(CropHandOff.take(K.PROJECT, ImageSlot.PROJECT_COVER, "other"))
        CropHandOff.put(CropHandOff(K.PROJECT, ImageSlot.PROJECT_COVER, hash, null, holo = true))
        assertTrue(CropHandOff.take(K.PROJECT, ImageSlot.PROJECT_COVER, hash)!!.holo)
        assertNull(CropHandOff.take(K.PROJECT, ImageSlot.PROJECT_COVER, hash))
    }

    @Test
    fun `list to card grows the thumbnails, text out first, reduce crossfades`() {
        val full = CardViewMotion.plan(reduced = false)
        assertEquals(360, full.growMs)
        assertEquals(160, full.textOutMs)
        assertEquals(160, full.textInDelayMs)
        assertTrue(full.sharedImages)
        val reduced = CardViewMotion.plan(reduced = true)
        assertNull(reduced.growMs)
        assertEquals(120, reduced.textOutMs)
        assertEquals(120, reduced.textInMs)
        assertEquals(0, reduced.textInDelayMs)
        assertFalse(reduced.sharedImages)
        assertEquals(48f, CardViewMotion.THUMB_DP)
    }

    @Test
    fun `the second card moves aside only when the card view is switched on with full motion`() {
        assertEquals(-24f, PageListMotion.nudgeDp(reduced = false, entering = true))
        assertEquals("first show: no move", 0f, PageListMotion.nudgeDp(reduced = false, entering = false))
        assertEquals("reduced: no move", 0f, PageListMotion.nudgeDp(reduced = true, entering = true))
    }
}
