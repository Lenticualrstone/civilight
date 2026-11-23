package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.domain.actions.GalleryActions
import io.github.lenticualrstone.civilight.domain.actions.GalleryAddSource
import io.github.lenticualrstone.civilight.domain.actions.GalleryOwner
import io.github.lenticualrstone.civilight.feature.canvas.common.actions
import io.github.lenticualrstone.civilight.feature.canvas.image.HoloRules
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** S-7: holo is set per image and is off for a new image; images that had holo keep it (scope §8). */
class HoloDefaultTest {
    private val g0 = ExampleGallery.build()
    private val owner = GalleryOwner.character(K.RIEN)
    private fun act() = CanvasFx.ctx().actions()

    @Test
    fun `S-7 a new gallery image has holo off with the default settings`() {
        val settings = AppSettings()
        assertFalse(settings.holoDefaultForNewImages)
        val result = GalleryActions.add(
            g0, owner, listOf(GalleryAddSource("e".repeat(64)), GalleryAddSource("d".repeat(64))), act(),
            locked = true, holo = settings.holoDefaultForNewImages,
        )
        assertEquals(2, result.added.size)
        for (key in result.added) assertFalse(result.doc.index.galleryItem(key)!!.holo)
        // Without the argument the action's own default is off too.
        val plain = GalleryActions.add(g0, owner, listOf(GalleryAddSource("c".repeat(64))), act(), locked = true)
        assertFalse(plain.doc.index.galleryItem(plain.added.single())!!.holo)
    }

    @Test
    fun `S-7 a new slot image starts with holo off`() {
        val settings = AppSettings()
        assertFalse(HoloRules.initial(ImageSlot.CHARACTER_PORTRAIT, null, settings.holoDefaultForNewImages))
        assertFalse(HoloRules.initial(ImageSlot.PROJECT_COVER, null, settings.holoDefaultForNewImages))
    }

    @Test
    fun `S-7 holo is per image and existing holo images keep it`() {
        val items = g0.gallery.values.flatMap { it.items }
        assertTrue(items.any { it.holo })
        val target = items.first { !it.holo }
        val on = GalleryActions.setHolo(g0, target.key, true, act())!!.doc
        assertTrue(on.index.galleryItem(target.key)!!.holo)
        // Only that image changed; every other image keeps its own value.
        for (item in items) if (item.key != target.key) assertEquals(item.holo, on.index.galleryItem(item.key)!!.holo)
    }
}
