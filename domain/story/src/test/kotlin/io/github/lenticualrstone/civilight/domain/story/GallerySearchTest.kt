package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.search.GalleryMatchField
import io.github.lenticualrstone.civilight.domain.story.search.GallerySearch
import io.github.lenticualrstone.civilight.domain.story.search.ProjectSearch
import io.github.lenticualrstone.civilight.domain.story.search.SearchContext
import io.github.lenticualrstone.civilight.domain.story.search.SearchFilter
import io.github.lenticualrstone.civilight.domain.story.search.SearchItem
import io.github.lenticualrstone.civilight.domain.story.search.SearchSectionKind
import io.github.lenticualrstone.civilight.domain.story.search.TextQuery
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The gallery part of the command-bar search (55 §3.5, GAL-04, T-GAL-19) and the `명령` chip with no query (QA #11). */
class GallerySearchTest {
    private val g0 = ExampleGallery.build()

    private fun hits(q: String, doc: ProjectDoc = g0, locked: Boolean = true) = GallerySearch(doc, locked).search(TextQuery(q))

    private fun ProjectDoc.withRienItems(change: (io.github.lenticualrstone.civilight.model.gallery.GalleryItem) -> io.github.lenticualrstone.civilight.model.gallery.GalleryItem): ProjectDoc {
        val file = gallery.getValue(K.RIEN)
        return copy(gallery = gallery + (K.RIEN to file.copy(items = file.items.map(change))))
    }

    @Test
    fun tGal19_descriptionSentenceWithTheMatchBoxed() {
        val hit = hits("그을음").single()
        assertEquals(ExampleGallery.ITEM_3, hit.itemKey)
        assertEquals(GalleryOwnerKind.CHARACTER, hit.ownerKind)
        assertEquals(K.RIEN, hit.ownerKey)
        assertEquals(GalleryMatchField.DESCRIPTION, hit.field)
        assertEquals(ExampleGallery.SOOT_SENTENCE, hit.match.text)
        assertEquals(listOf("그을음"), hit.match.boxed)
        assertEquals("리엔 · 갤러리 · 등불 앞의 리엔 · 설명", hit.label)
    }

    @Test
    fun titlesInStoredOrderAndTags() {
        val titles = hits("등불")
        assertEquals(listOf(ExampleGallery.ITEM_3, ExampleGallery.ITEM_6), titles.map { it.itemKey })
        assertTrue(titles.all { it.field == GalleryMatchField.TITLE })
        assertEquals("리엔 · 갤러리 · 등불 고리 · 제목", titles[1].label)
        val tag = hits("소품").single()
        assertEquals(ExampleGallery.ITEM_6, tag.itemKey)
        assertEquals(GalleryMatchField.TAG, tag.field)
        assertEquals("소품", tag.match.text)
    }

    @Test
    fun hiddenImagesOnlyWhenUnlocked() {
        assertEquals(emptyList<Any>(), hits("숨긴", locked = true))
        assertEquals(listOf(ExampleGallery.ITEM_H1, ExampleGallery.ITEM_H2), hits("숨긴", locked = false).map { it.itemKey })
    }

    @Test
    fun trashedImagesAndTrashedOrHiddenOwnersAreLeftOut() {
        val trashed = g0.withRienItems { if (it.key == ExampleGallery.ITEM_1) it.copy(deletedAt = 1L) else it }
        assertEquals(emptyList<Any>(), hits("결사", trashed))
        val ownerGone = g0.copy(cast = g0.cast.copy(characters = g0.cast.characters.map { if (it.key == K.RIEN) it.copy(deletedAt = 1L) else it }))
        assertEquals(emptyList<Any>(), hits("등불", ownerGone))
        val ownerHidden = g0.copy(cast = g0.cast.copy(characters = g0.cast.characters.map { if (it.key == K.RIEN) it.copy(hidden = true) else it }))
        assertEquals(emptyList<Any>(), hits("등불", ownerHidden, locked = true))
        assertEquals(2, hits("등불", ownerHidden, locked = false).size)
    }

    @Test
    fun projectSearchAddsGalleryOnlyWhenAsked() {
        val search = ProjectSearch(g0, SearchContext())
        val plain = search.search("그을음")
        assertTrue(plain.gallery.isEmpty())
        val withGallery = search.search("그을음", includeGallery = true)
        assertEquals(listOf(ExampleGallery.ITEM_3), withGallery.gallery.map { it.itemKey })
        assertNull(withGallery.empty)
        assertTrue(search.search("그을음", SearchFilter.TERM, includeGallery = true).gallery.isEmpty())
    }

    @Test
    fun commandChipWithAnEmptyQueryListsTheCommands() {
        val search = ProjectSearch(g0, SearchContext(openCheckCount = 3))
        val commands = search.search("", SearchFilter.COMMAND).section(SearchSectionKind.COMMAND)
        assertNotNull(commands)
        val labels = commands!!.items.map { (it as SearchItem.CommandItem).label.text }
        assertEquals(listOf("시점 바꾸기", "점검 센터 열기"), labels)
        assertTrue(commands.items.all { (it as SearchItem.CommandItem).label.hits.isEmpty() })
        assertEquals("› 점검 센터 열기 | 확인할 항목 3", (commands.items[1] as SearchItem.CommandItem).line)
        assertTrue(search.search("", SearchFilter.ALL).sections.isEmpty())
        assertNull(search.search("  ", SearchFilter.COMMAND).empty)
    }
}
