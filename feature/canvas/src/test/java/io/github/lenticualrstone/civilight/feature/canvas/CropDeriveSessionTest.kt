package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.feature.canvas.image.CropMath
import io.github.lenticualrstone.civilight.feature.canvas.image.CropSession
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageEdits
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwner
import io.github.lenticualrstone.civilight.feature.canvas.image.PixelSize
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.Ratio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** CST-13: the portrait is cut once; the 1:1 avatar follows the 4:5 card unless it is edited on its own. */
class CropDeriveSessionTest {
    private val source = PixelSize(3024, 4032)
    private val portrait = ImageOwner(ImageOwnerKind.CHARACTER, K.RIEN, ImageSlot.CHARACTER_PORTRAIT)

    private fun inside(inner: Crop, outer: Crop) =
        inner.x >= outer.x && inner.y >= outer.y && inner.x + inner.w <= outer.x + outer.w && inner.y + inner.h <= outer.y + outer.h

    private fun start(existing: io.github.lenticualrstone.civilight.model.asset.ImageUse? = null) =
        CropSession.start(ImageSlot.CHARACTER_PORTRAIT, source, existing, "h", holo = false)

    @Test
    fun `적용 after editing only the card saves the avatar too - inside the card, top-aligned, turned with it`() {
        val s = start().zoom(1.6f).pan(0f, 600f).rotate().flip()
        assertTrue(s.isAuto(Ratio.R1X1))
        val crops = s.crops()
        assertEquals(setOf(Ratio.R4X5, Ratio.R1X1), crops.keys)
        val card = crops.getValue(Ratio.R4X5)
        val avatar = crops.getValue(Ratio.R1X1)
        assertTrue(inside(avatar, card))
        assertEquals(card.y, avatar.y)
        assertEquals(avatar.w, avatar.h)
        assertEquals(card.rotation, avatar.rotation)
        assertEquals(card.flipH, avatar.flipH)
        assertEquals(CropMath.derived(source.w, source.h, card, Ratio.R1X1), avatar)
    }

    @Test
    fun `a high zoom card still gives an avatar inside it (written directly, not through the editor's zoom range)`() {
        // Landscape original: the 1:1 inside a 4× 4:5 crop needs more than the editor's 4×.
        val wide = PixelSize(4000, 1000)
        val s = CropSession.start(ImageSlot.CHARACTER_PORTRAIT, wide, null, "h", holo = false).zoom(4f)
        val crops = s.crops()
        assertTrue(inside(crops.getValue(Ratio.R1X1), crops.getValue(Ratio.R4X5)))
    }

    @Test
    fun `opening the avatar keeps it automatic, editing makes it its own, 처음대로 returns it to 자동`() {
        val s = start().zoom(1.5f)
        val opened = s.select(Ratio.R1X1)
        assertEquals(Ratio.R1X1, opened.current)
        assertTrue(opened.isAuto(Ratio.R1X1))
        assertEquals(s.crops(), opened.crops())
        assertEquals(opened.cropOf(Ratio.R1X1), opened.readout().crop)
        val own = opened.pan(100f, 100f)
        assertFalse(own.isAuto(Ratio.R1X1))
        // The card moves on; the own avatar stays.
        val back = own.select(Ratio.R4X5).pan(0f, -300f)
        assertEquals(own.cropOf(Ratio.R1X1), back.crops()[Ratio.R1X1])
        val reset = back.select(Ratio.R1X1).reset()
        assertTrue(reset.isAuto(Ratio.R1X1))
        assertEquals("아바타 1:1 · 자동", ImageCopy.cropPreview(reset.chip(Ratio.R1X1), reset.isAuto(Ratio.R1X1)))
        assertEquals(reset, CropSession.decode(reset.encode()))
    }

    @Test
    fun `a saved automatic avatar keeps following on the next session, an own one keeps its place`() {
        val ctx = CanvasFx.ctx()
        var doc = CanvasFx.s0()
        val first = start().zoom(1.4f)
        doc = ImageEdits.placeBlob(doc, portrait, "h", first.crops(), first.holo, ctx)
        val again = start(ImageEdits.use(doc, portrait))
        assertTrue(again.isAuto(Ratio.R1X1))
        val moved = again.pan(0f, 500f)
        assertEquals(CropMath.derived(source.w, source.h, moved.crops().getValue(Ratio.R4X5), Ratio.R1X1), moved.crops()[Ratio.R1X1])

        val own = start().zoom(1.4f).select(Ratio.R1X1).zoom(2f)
        doc = ImageEdits.placeBlob(doc, portrait, "h", own.crops(), own.holo, ctx)
        val kept = start(ImageEdits.use(doc, portrait))
        assertFalse(kept.isAuto(Ratio.R1X1))
        assertTrue(CropMath.same(own.crops()[Ratio.R1X1], kept.pan(0f, 500f).crops()[Ratio.R1X1]))
    }
}
