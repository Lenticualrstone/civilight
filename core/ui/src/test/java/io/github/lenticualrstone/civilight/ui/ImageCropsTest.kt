package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.serialName
import io.github.lenticualrstone.civilight.ui.image.ImageCrops
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** CST-13: one picture, crops by ratio; a ratio without its own crop is derived from the slot's first one. */
class ImageCropsTest {
    private fun inside(inner: Crop, outer: Crop) =
        inner.x >= outer.x && inner.y >= outer.y && inner.x + inner.w <= outer.x + outer.w && inner.y + inner.h <= outer.y + outer.h

    private fun ratioOk(c: Crop, r: Ratio): Boolean {
        val (pw, ph) = ImageCrops.parts(r)
        return abs(c.w.toLong() * ph - c.h.toLong() * pw) <= maxOf(pw, ph)
    }

    @Test
    fun `derived 1x1 sits inside the 4x5 crop and the source, top-aligned, exact ratio`() {
        val sourceW = 3024
        val sourceH = 4032
        val card = Crop(x = 200, y = 300, w = 2000, h = 2500, zoom = 1.2f)
        val avatar = ImageCrops.derived(card, Ratio.R1X1, top = true, sourceW = sourceW, sourceH = sourceH)
        assertEquals(Crop(x = 200, y = 300, w = 2000, h = 2000, zoom = avatar.zoom), avatar)
        assertTrue(inside(avatar, card))
        assertTrue(inside(avatar, Crop(0, 0, sourceW, sourceH)))
        assertTrue(ratioOk(avatar, Ratio.R1X1))
        assertEquals(sourceW / 2000f, avatar.zoom, 0.001f)
    }

    @Test
    fun `derived inherits rotation and mirror and works in the rotated pixel space`() {
        // Rotated 90: the 3024 × 4032 original is 4032 × 3024.
        val card = Crop(x = 1000, y = 0, w = 2419, h = 3024, rotation = 90, flipH = true, zoom = 1f)
        val avatar = ImageCrops.derived(card, Ratio.R1X1, top = true, sourceW = 3024, sourceH = 4032)
        assertEquals(90, avatar.rotation)
        assertTrue(avatar.flipH)
        assertTrue(inside(avatar, card))
        assertTrue(inside(avatar, Crop(0, 0, 4032, 3024)))
        assertEquals(card.y, avatar.y)
        assertEquals(2419, avatar.w)
        assertEquals(1.25f, avatar.zoom, 0.01f)
    }

    @Test
    fun `wider target is centred across, centred mode is centred down, odd sizes stay inside`() {
        val tall = Crop(x = 10, y = 20, w = 90, h = 160)
        val wide = ImageCrops.derived(tall, Ratio.R4X3, top = false)
        assertEquals(90, wide.w)
        assertEquals(68, wide.h)
        assertEquals(20 + (160 - 68) / 2, wide.y)
        assertTrue(inside(wide, tall))
        val square = Crop(x = 5, y = 5, w = 101, h = 101)
        val card = ImageCrops.derived(square, Ratio.R4X5)
        assertTrue(inside(card, square))
        assertTrue(ratioOk(card, Ratio.R4X5))
        assertEquals(5, card.y)
        val tiny = ImageCrops.derived(Crop(0, 0, 1, 1), Ratio.R21X9)
        assertTrue(tiny.w >= 1 && tiny.h >= 1)
    }

    @Test
    fun `reader fallback - stored crop wins, else derived from the first ratio, else none`() {
        val card = Crop(x = 0, y = 100, w = 800, h = 1000)
        fun use(crops: Map<Ratio, Crop>) = ImageUse(
            "u", ImageOwnerKind.CHARACTER.open(), "c", ImageSlot.CHARACTER_PORTRAIT.open(), "abc",
            crops.mapKeys { it.key.serialName },
        )
        assertEquals(Crop(0, 100, 800, 800), ImageCrops.cropFor(use(mapOf(Ratio.R4X5 to card)), Ratio.R1X1))
        val own = Crop(1, 2, 300, 300)
        assertEquals(own, ImageCrops.cropFor(use(mapOf(Ratio.R4X5 to card, Ratio.R1X1 to own)), Ratio.R1X1))
        assertNull(ImageCrops.cropFor(use(emptyMap()), Ratio.R1X1))
        // The first ratio is never cut from the second.
        assertNull(ImageCrops.cropFor(use(mapOf(Ratio.R1X1 to own)), Ratio.R4X5))
        // A cover's 목록 1:1 follows its 3:4 card, top-aligned.
        val cover = ImageUse("p", ImageOwnerKind.PROJECT.open(), "p", ImageSlot.PROJECT_COVER.open(), "abc", mapOf(Ratio.R3X4.serialName to Crop(0, 0, 300, 400)))
        assertEquals(Crop(0, 0, 300, 300), ImageCrops.cropFor(cover, Ratio.R1X1))
        // The background's 4:3 does not follow its 9:16.
        val bg = ImageUse("b", ImageOwnerKind.CHARACTER.open(), "c", ImageSlot.CHARACTER_BACKGROUND.open(), "abc", mapOf(Ratio.R9X16.serialName to Crop(0, 0, 90, 160)))
        assertNull(ImageCrops.cropFor(bg, Ratio.R4X3))
    }
}
