package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.feature.cast.gallery.CastGallery
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryFile
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery as G
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** The profile gallery section (GAL-01) and the count chip (GAL-02) over fixture G0 (55-gallery.md §4). */
class CastGalleryTest {
    private val g0: ProjectDoc = G.build()

    @Test
    fun `GAL-01 the section counts the visible images and lists them in the gallery order`() {
        val locked = CastGallery.section(g0, K.RIEN, hiddenUnlocked = false)
        assertEquals("갤러리 · 8", locked.header)
        assertEquals(8, locked.count)
        assertEquals(listOf(G.ITEM_1, G.ITEM_2, G.ITEM_3, G.ITEM_4, G.ITEM_5, G.ITEM_6, G.ITEM_7, G.ITEM_8), locked.cards.map { it.itemKey })
        assertTrue(locked.showAll)
        assertFalse(locked.empty)
        // Hidden images join in once hidden items are open.
        val open = CastGallery.section(g0, K.RIEN, hiddenUnlocked = true)
        assertEquals("갤러리 · 10", open.header)
        assertTrue(G.ITEM_H1 in open.cards.map { it.itemKey })
    }

    @Test
    fun `GAL-01 a card carries the shared chip, title, meta line and its 3 to 4 crop`() {
        val cards = CastGallery.section(g0, K.RIEN, hiddenUnlocked = false).cards.associateBy { it.itemKey }
        val third = cards.getValue(G.ITEM_3)
        assertEquals("등불 앞의 리엔", third.title)
        // T-GAL-07: the portrait is item 3's source, so it reads `초상 · 홀로`.
        assertEquals("초상 · 홀로", third.chip)
        assertEquals("1-7 씬 3", third.meta)
        assertEquals(Ratio.R3X4, third.image.ratio)
        assertEquals(G.hashOf(3), third.image.blobHash)
        // Tags are the meta line while no scene is linked; an untitled image has no title and no chip.
        assertEquals("설정화", cards.getValue(G.ITEM_2).meta)
        assertNull(cards.getValue(G.ITEM_2).chip)
        val untitled = cards.getValue(G.ITEM_4)
        assertNull(untitled.title)
        assertNull(untitled.meta)
        assertNull(untitled.chip)
    }

    @Test
    fun `GAL-01 a scene that is trashed or hidden while locked is not named on the card`() {
        fun metaOf(doc: ProjectDoc, unlocked: Boolean) = CastGallery.section(doc, K.RIEN, unlocked).cards.first { it.itemKey == G.ITEM_3 }.meta
        assertEquals("1-7 씬 3", metaOf(g0, false))
        val trashed = g0.copy(story = g0.story.copy(units = g0.story.units.map { if (it.key == K.SCENE_1_7_3) it.copy(deletedAt = 1L) else it }))
        // Item 3 carries the tag 삽화, which is what the card falls back to.
        assertEquals("삽화", metaOf(trashed, false))
        val hidden = g0.copy(story = g0.story.copy(units = g0.story.units.map { if (it.key == K.SCENE_1_7_3) it.copy(hidden = true) else it }))
        assertEquals("삽화", metaOf(hidden, false))
        assertEquals("1-7 씬 3", metaOf(hidden, true))
    }

    @Test
    fun `GAL-01 the row stops at twelve cards, the count does not`() {
        val file = g0.gallery.getValue(K.RIEN)
        val more = file.items + file.items.filter { !it.hidden }.take(6).map { it.copy(key = it.key + "b", order = it.order + "~") }
        val big = g0.copy(gallery = g0.gallery + (K.RIEN to GalleryFile(file.ownerKind, file.ownerKey, more)))
        val section = CastGallery.section(big, K.RIEN, hiddenUnlocked = false)
        assertEquals(12, section.cards.size)
        assertEquals("갤러리 · 14", section.header)
    }

    @Test
    fun `GAL-01 a character with no image has no 전체 보기 and an empty section`() {
        val section = CastGallery.section(g0, K.SERA, hiddenUnlocked = false)
        assertTrue(section.empty)
        assertFalse(section.showAll)
        assertEquals("갤러리 · 0", section.header)
        assertTrue(section.cards.isEmpty())
    }

    @Test
    fun `GAL-02 the chip shows the visible count and nothing for a character without images`() {
        assertEquals(mapOf(K.RIEN to 8), CastGallery.counts(g0, g0.cast.characters.map { it.key }, hiddenUnlocked = false))
        assertEquals(10, CastGallery.count(g0, K.RIEN, hiddenUnlocked = true))
        assertEquals(0, CastGallery.count(g0, K.SERA, hiddenUnlocked = false))
    }

    @Test
    fun `GAL-02 a hidden character has no chip while locked, and a trashed image is not counted`() {
        val hidden = g0.copy(cast = g0.cast.copy(characters = g0.cast.characters.map { if (it.key == K.RIEN) it.copy(hidden = true) else it }))
        assertTrue(CastGallery.counts(hidden, listOf(K.RIEN), hiddenUnlocked = false).isEmpty())
        assertEquals(10, CastGallery.count(hidden, K.RIEN, hiddenUnlocked = true))
        val file = g0.gallery.getValue(K.RIEN)
        val trashed = g0.copy(gallery = g0.gallery + (K.RIEN to file.copy(items = file.items.map { if (it.key == G.ITEM_1) it.copy(deletedAt = 1L) else it })))
        assertEquals(7, CastGallery.count(trashed, K.RIEN, hiddenUnlocked = false))
    }
}
