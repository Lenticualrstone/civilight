package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.search.ProjectSearch
import io.github.lenticualrstone.civilight.domain.story.search.SearchContext
import io.github.lenticualrstone.civilight.domain.story.search.SearchFilter
import io.github.lenticualrstone.civilight.domain.story.search.SearchItem
import io.github.lenticualrstone.civilight.domain.story.search.SearchMore
import io.github.lenticualrstone.civilight.domain.story.search.SearchSectionKind
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** (0.2.0) N-13: `화자 ▾` limits `대사` to one speaker; `그 밖 ▾` searches 장소 · 세력 · 사건 · 페이지. */
class SearchMoreTest {
    private val doc = Fx.doc()
    private val search = ProjectSearch(doc, SearchContext())

    private fun lineItems(r: io.github.lenticualrstone.civilight.domain.story.search.SearchResult) =
        r.section(SearchSectionKind.LINE)?.items.orEmpty().map { it as SearchItem.LineItem }

    @Test
    fun speakerFilterReturnsOnlyThatSpeakersBlocks() {
        val all = lineItems(search.search("오리지", SearchFilter.LINE))
        assertEquals(3, all.size)

        val sera = search.searchLines("오리지", K.SERA)
        assertEquals(SearchFilter.LINE, sera.filter)
        assertEquals(listOf("1-4 씬 2 · 세라"), lineItems(sera).map { it.location })
        val rien = lineItems(search.searchLines("오리지", K.RIEN))
        assertEquals(listOf("1-2 씬 1 · 리엔"), rien.map { it.location })
        for (item in lineItems(sera) + rien) {
            val block = doc.index.block(item.key)!!
            assertTrue(block.speakerKey == K.SERA || block.speakerKey == K.RIEN)
        }
    }

    @Test
    fun speakerFilterWithNoHitIsEmptyAndAnEmptyQueryFindsNothing() {
        val none = search.searchLines("오리지", K.HART)
        assertTrue(none.sections.isEmpty())
        assertNotNull(none.empty)
        val blank = search.searchLines("", K.SERA)
        assertTrue(blank.sections.isEmpty())
        assertNull(blank.empty)
    }

    @Test
    fun speakersAreThoseWithLinesInCastOrder() {
        val speakers = search.lineSpeakers()
        assertTrue(speakers.isNotEmpty())
        assertTrue(speakers.map { it.key }.containsAll(listOf(K.SERA, K.RIEN)))
        val speaking = doc.manuscript.liveBlocks.mapNotNull { it.speakerKey }.toSet()
        assertTrue(speakers.all { it.key in speaking })
        val order = doc.cast.characters.filter { c -> speakers.any { it.key == c.key } }.sortedWith(compareBy({ it.order }, { it.key })).map { it.key }
        assertEquals(order, speakers.map { it.key })
    }

    @Test
    fun moreFindsPagesByName() {
        val r = search.searchMore("결사", SearchMore.PAGE)
        assertEquals(listOf(K.PAGE_SOCIETY_RULES), r.items.map { it.key })
        assertEquals("등불 결사 규약", r.items.single().name.text)
        assertEquals(listOf("결사"), r.items.single().name.boxed)
        assertEquals("페이지", r.header)
        assertNull(r.empty)
    }

    @Test
    fun moreFindsTheOtherThreeKindsLikeTheAllSections() {
        val all = search.search("결사", SearchFilter.ALL)
        val factions = all.section(SearchSectionKind.FACTION)?.items.orEmpty().map { it.key }
        assertEquals(factions, search.searchMore("결사", SearchMore.FACTION).items.map { it.key })
        for (kind in listOf(SearchMore.PLACE, SearchMore.EVENT)) {
            val sectionKind = if (kind == SearchMore.PLACE) SearchSectionKind.PLACE else SearchSectionKind.EVENT
            val q = "등불"
            assertEquals(
                ProjectSearch(doc).search(q, SearchFilter.ALL).section(sectionKind)?.items.orEmpty().map { it.key },
                search.searchMore(q, kind).items.map { it.key },
            )
        }
    }

    @Test
    fun hiddenPagesAreLeftOutWhileLocked() {
        val hidden = doc.copy(pages = doc.pages.mapValues { (k, p) -> if (k == K.PAGE_SOCIETY_RULES) p.copy(hidden = true) else p })
        val locked = ProjectSearch(hidden, SearchContext(locked = true)).searchMore("결사", SearchMore.PAGE)
        assertTrue(locked.items.isEmpty())
        assertTrue(locked.hiddenExcluded)
        val open = ProjectSearch(hidden, SearchContext(locked = false)).searchMore("결사", SearchMore.PAGE)
        assertEquals(1, open.items.size)
        assertFalse(open.hiddenExcluded)
    }

    @Test
    fun allStillHasNoPageSection() {
        val all = search.search("결사", SearchFilter.ALL)
        assertTrue(all.sections.none { it.items.any { item -> item.key == K.PAGE_SOCIETY_RULES } })
    }
}
