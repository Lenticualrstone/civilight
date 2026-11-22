package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.cast.refs.DeletedItems
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gallery rows of `domain:cast` (55 §3.4): the delete snapshot, the purge, tag removal and counts. */
class CastGalleryTest {
    private val ctx = Fx.ctx()
    private val g0 = ExampleGallery.build()

    @Test
    fun `the character delete snapshot lists its live images in order`() {
        assertEquals(ExampleGallery.STORED, DeletedItems.snapshot(g0, K.RIEN).galleryItemKeys)
        assertTrue(DeletedItems.snapshot(g0, K.SERA).galleryItemKeys.isEmpty())
    }

    @Test
    fun `purging a character removes its gallery file`() {
        val purged = DeletedItems.purgeCharacter(g0, K.RIEN, ctx)
        assertFalse(K.RIEN in purged.gallery)
        assertTrue(purged.galleryItems.isEmpty())
    }

    @Test
    fun `T-GAL-31 tag removal reaches images and the usage line counts them`() {
        val tag = ExampleGallery.TAG_SETTING
        assertEquals("인물 0 · 이미지 3", CharacterEdits.tagUsage(g0, tag).line) // 2, 8 and H2
        val removed = CharacterEdits.removeTag(g0, tag, ctx)
        assertNull(removed.index.tag(tag))
        assertTrue(removed.galleryItems.none { tag in it.tagKeys })
        assertEquals(g0.galleryItems.count { tag !in it.tagKeys }, removed.galleryItems.count { it.updatedAt == g0.index.galleryItem(it.key)!!.updatedAt })
        // Renaming to another tag's name is refused.
        assertNull(CharacterEdits.renameTag(g0, tag, "삽화", ctx))
    }
}
