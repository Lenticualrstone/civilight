package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.feature.canvas.image.CropTransition
import io.github.lenticualrstone.civilight.model.asset.Crop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Audit §4 canvas 6: the picture eases from its old look into the new crop on rotate, mirror, reset and zoom steps. */
class CropTransitionTest {
    private fun crop(x: Int = 0, y: Int = 0, w: Int = 1000, h: Int = 750, rotation: Int = 0, flipH: Boolean = false, zoom: Float = 1f) =
        Crop(x = x, y = y, w = w, h = h, rotation = rotation, flipH = flipH, zoom = zoom)

    @Test
    fun `no change means no layer`() {
        assertTrue(CropTransition.start(crop(), crop(), 300f).isIdentity)
    }

    @Test
    fun `a quarter turn starts a quarter turn back and ends at rest`() {
        val s = CropTransition.start(crop(rotation = 0), crop(rotation = 90), 300f)
        assertEquals(-90f, s.rotation, 0f)
        assertEquals(1f, s.scale, 0f)
        assertEquals(0f, s.at(1f).rotation, 0f)
        assertEquals(-45f, s.at(0.5f).rotation, 0.001f)
    }

    @Test
    fun `a turn of a picture that is not square also carries the size change`() {
        // 4:3 frame: the largest crop is 1000 wide before the turn and 600 after it in a 800 x 600 source.
        val s = CropTransition.start(crop(w = 800, h = 600, rotation = 0), crop(w = 450, h = 600, rotation = 90), 300f)
        assertEquals(-90f, s.rotation, 0f)
        assertEquals(450f / 800f, s.scale, 0.0001f)
        assertEquals(1f, s.at(1f).scale, 0f)
    }

    @Test
    fun `a mirrored picture turns the other way`() {
        val s = CropTransition.start(crop(rotation = 0, flipH = true), crop(rotation = 90, flipH = true), 300f)
        assertEquals(90f, s.rotation, 0f)
    }

    @Test
    fun `turning past 270 takes the short way round`() {
        val s = CropTransition.start(crop(rotation = 270), crop(rotation = 0), 300f)
        assertEquals(-90f, s.rotation, 0f)
    }

    @Test
    fun `flipping squashes through zero and opens the other way`() {
        val s = CropTransition.start(crop(flipH = false), crop(flipH = true), 300f)
        assertEquals(-1f, s.flipX, 0f)
        assertEquals(1f, s.at(1f).flipX, 0f)
        assertEquals(0f, s.at(0.5f).flipX, 0.001f)
    }

    @Test
    fun `a zoom step scales about the frame centre from the old crop width to the new`() {
        // Old crop 1000 wide (scale 0.3 px per source px), new 500 wide (0.6): the new picture starts at half size.
        val s = CropTransition.start(crop(w = 1000, h = 750, zoom = 1f), crop(x = 250, y = 187, w = 500, h = 375, zoom = 2f), 300f)
        assertEquals(0.5f, s.scale, 0.001f)
        // Same centre (500, 375): no translation.
        assertEquals(0f, s.translationX, 0.6f)
        assertEquals(0f, s.translationY, 0.6f)
        assertEquals(1f, s.at(1f).scale, 0f)
    }

    @Test
    fun `a moved centre starts shifted by the old scale`() {
        val s = CropTransition.start(crop(x = 0, w = 500, h = 375), crop(x = 100, w = 500, h = 375), 300f)
        assertEquals(100f * 0.6f, s.translationX, 0.001f)
        assertEquals(1f, s.scale, 0.001f)
    }
}
