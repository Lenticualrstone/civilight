package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.search.ProjectSearch
import io.github.lenticualrstone.civilight.domain.story.search.SearchCommand
import io.github.lenticualrstone.civilight.domain.story.search.SearchContext
import io.github.lenticualrstone.civilight.domain.story.search.SearchFilter
import io.github.lenticualrstone.civilight.domain.story.search.SearchItem
import io.github.lenticualrstone.civilight.domain.story.search.SearchSectionKind
import io.github.lenticualrstone.civilight.domain.story.search.TextQuery
import io.github.lenticualrstone.civilight.domain.story.text.Sentences
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import java.text.Normalizer

/** In-project search and commands (20-story-time §4.12 SRCH-01 … SRCH-07). */
class SearchTest {
    private val doc = Fx.doc()

    private fun search(q: String, d: ProjectDoc = doc, ctx: SearchContext = SearchContext(), filter: SearchFilter = SearchFilter.ALL) =
        ProjectSearch(d, ctx).search(q, filter)

    private fun lines(q: String, d: ProjectDoc = doc) = search(q, d).section(SearchSectionKind.LINE)?.items.orEmpty().map { it as SearchItem.LineItem }

    @Test
    fun srch01_termAndThreeLines() {
        val r = search("오리지")
        val terms = r.section(SearchSectionKind.TERM)!!
        assertEquals("용어", terms.header)
        val term = terms.items.single() as SearchItem.TermItem
        assertEquals("오리지늄 등불", term.name.text)
        assertEquals(listOf("오리지"), term.name.boxed)
        assertEquals("첫 공개 1-7 #044", term.sub)
        assertEquals("카드", term.action)

        val lines = r.section(SearchSectionKind.LINE)!!
        assertEquals("대사 · 3", lines.header)
        val items = lines.items.map { it as SearchItem.LineItem }
        assertEquals(listOf("1-7 씬 3 · 나레이션", "1-4 씬 2 · 세라", "1-2 씬 1 · 리엔"), items.map { it.location })
        assertEquals(listOf(false, true, false), items.map { it.beforeReveal })
        assertEquals("▲ 공개 전 사용", items[1].flag)
        assertEquals("꺼진 줄 알았던 오리지늄 등불이 한 번 깜박였다.", items[0].sentence.text)
        assertEquals("오리지늄은 타지 않아. 빛만 남지.", items[1].sentence.text)
        assertEquals("등불 속 그 돌, 오리지… 뭐라고 했죠?", items[2].sentence.text)
        for (i in items) assertEquals(listOf("오리지"), i.sentence.boxed)
        assertEquals(listOf(SearchSectionKind.TERM, SearchSectionKind.LINE), r.sections.map { it.kind })
    }

    @Ignore("spec conflict: SRCH-01 term sub `첫 공개 1-7 #044 · 장소: 하층 구역`; the fixture's 오리지늄 등불 relates to no place (관련 = 등불 결사, 대정전) and 하층 구역 is a map region, not a Place")
    @Test
    fun srch01_termSubAsDrawn() {
        val term = search("오리지").section(SearchSectionKind.TERM)!!.items.single() as SearchItem.TermItem
        assertEquals("첫 공개 1-7 #044 · 장소: 하층 구역", term.sub)
    }

    @Test
    fun termSubListsRelatedPlaces() {
        val related = doc.copy(
            world = doc.world.copy(
                terms = doc.world.terms.map {
                    if (it.key == K.TERM_ORIGINIUM_LANTERN) {
                        it.copy(related = it.related + io.github.lenticualrstone.civilight.model.world.ItemRef(io.github.lenticualrstone.civilight.model.world.ItemRefKind.PLACE, K.PLACE_LOWER_STREET))
                    } else {
                        it
                    }
                },
            ),
        )
        assertEquals("첫 공개 1-7 #044 · 장소: 하층 거리", ProjectSearch(related).termSub(K.TERM_ORIGINIUM_LANTERN))
        assertEquals("공개 예정 2-5", ProjectSearch(doc).termSub(K.TERM_EMBER_OATH))
    }

    @Test
    fun srch02_initialConsonants() {
        val term = search("ㅇㄹㅈ").section(SearchSectionKind.TERM)!!.items
            .map { it as SearchItem.TermItem }.single { it.key == K.TERM_ORIGINIUM_LANTERN }
        assertEquals(listOf("오리지"), term.name.boxed)
        assertEquals("ㅇㄹㅈㄴ ㄷㅂ", TextQuery.initials("오리지늄 등불"))
        assertTrue(TextQuery("ㅇㄹㅈ").isInitials)
        assertFalse(TextQuery("오리지").isInitials)
    }

    @Test
    fun srch03_nfdQueryGivesTheSameResults() {
        val nfd = Normalizer.normalize("오리지", Normalizer.Form.NFD)
        assertEquals(search("오리지").sections, search(nfd).sections)
    }

    @Test
    fun srch04_emptyWithSimilarNames() {
        val r = search("오리지누")
        assertTrue(r.sections.isEmpty())
        val empty = r.empty!!
        assertEquals("결과 없음", empty.title)
        assertEquals("‘오리지누’", empty.line)
        assertEquals("비슷한 이름", empty.similarHeader)
        assertTrue(empty.similar.map { it.line }.contains("오리지늄 등불 · 용어 · 첫 공개 1-7"))
        assertEquals("‘오리지누’ 용어로 만들기", empty.createAction)
    }

    /** SRCH-05 Given: 노아 restored and hidden, 1-6 씬 5 restored and hidden, both locked. */
    private fun hiddenNoahAndScene(): ProjectDoc = doc.copy(
        cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.NOAH) it.copy(deletedAt = null, trashEntryKey = null, hidden = true) else it }),
        story = doc.story.copy(units = doc.story.units.map { if (it.key == K.SCENE_1_6_5) it.copy(deletedAt = null, trashEntryKey = null, hidden = true) else it }),
    )

    @Test
    fun srch05_hiddenItemsWhileLocked() {
        val d = hiddenNoahAndScene()
        val r = search("노아", d)
        assertNull(r.section(SearchSectionKind.CHARACTER))
        assertEquals("숨긴 항목 제외", r.footer)
        val found = r.section(SearchSectionKind.LINE)!!.items.map { it as SearchItem.LineItem }
        assertTrue(found.none { it.sceneKey == K.SCENE_1_6_5 })
        assertTrue(lines("노아가 웃었다", d).isEmpty())
        // 노아's own line in a visible scene is still found by its text.
        val candy = lines("사탕", d).single()
        assertEquals("1-6 씬 4 · 노아", candy.location)
        // Unlocked: the card and the hidden scene's text come back.
        val open = ProjectSearch(d, SearchContext(locked = false)).search("노아")
        assertNotNull(open.section(SearchSectionKind.CHARACTER))
        assertTrue(open.section(SearchSectionKind.LINE)!!.items.any { (it as SearchItem.LineItem).sceneKey == K.SCENE_1_6_5 })
        assertNull(open.footer)
    }

    @Test
    fun srch06_trashedCharacterHasNoResult() {
        val r = search("노아")
        assertNull(r.section(SearchSectionKind.CHARACTER))
        assertTrue(r.section(SearchSectionKind.LINE)!!.items.isNotEmpty())
        // Text of the trashed scene 1-6 씬 5 is not found.
        assertTrue(lines("노아가 웃었다").isEmpty())
    }

    @Test
    fun srch07_commands() {
        val ctx = SearchContext(openCheckCount = 3)
        val check = search("점검", ctx = ctx).section(SearchSectionKind.COMMAND)!!.items.single() as SearchItem.CommandItem
        assertEquals("› 점검 센터 열기 | 확인할 항목 3", check.line)
        assertEquals(SearchCommand.OPEN_CHECKS, check.command)
        val lens = search("시점", ctx = ctx).section(SearchSectionKind.COMMAND)!!.items.single() as SearchItem.CommandItem
        assertEquals("› 시점 바꾸기 | 지금: 최신 · 1-7", lens.line)
        val inScript = SearchContext(inScript = true, currentSceneKey = K.SCENE_1_7_3, currentEpisodeKey = K.EPISODE_1_7)
        assertNull(search("시점", ctx = inScript).section(SearchSectionKind.COMMAND))
        val toScene = search("렌즈", ctx = inScript).section(SearchSectionKind.COMMAND)!!.items.single() as SearchItem.CommandItem
        assertEquals(SearchCommand.LENS_TO_SCENE, toScene.command)
        val renumber = search("번호", ctx = inScript).section(SearchSectionKind.COMMAND)!!.items.single() as SearchItem.CommandItem
        assertEquals("› 번호 다시 매기기 · 1-7", renumber.line)
        val past = search("시점", ctx = SearchContext(lens = LensPoint.UnitEnd(K.EPISODE_1_5))).section(SearchSectionKind.COMMAND)!!.items.single()
        assertEquals("› 시점 바꾸기 | 지금: 시점 · 1-5", (past as SearchItem.CommandItem).line)
    }

    @Test
    fun scenesAndBlockNumbers() {
        val scene = search("1-7 씬 3", filter = SearchFilter.SCENE).section(SearchSectionKind.SCENE)!!.items.single() as SearchItem.SceneItem
        assertEquals("1-7 씬 3 · 꺼진 등불", scene.title.text)
        assertEquals("2일째 새벽", scene.sub)
        val byTitle = search("꺼진 등불", filter = SearchFilter.SCENE).section(SearchSectionKind.SCENE)!!.items.single()
        assertEquals(K.SCENE_1_7_3, byTitle.key)
        val n = search("#043", ctx = SearchContext(currentEpisodeKey = K.EPISODE_1_7)).section(SearchSectionKind.LINE)!!.items.first() as SearchItem.LineItem
        assertEquals(K.BLOCK_043, n.key)
        assertEquals("1-7 씬 3 · 하르트", n.location)
    }

    @Test
    fun filtersAndWorldSections() {
        val all = search("결사")
        assertNotNull(all.section(SearchSectionKind.FACTION))
        assertNotNull(all.section(SearchSectionKind.PLACE))
        val onlyLines = search("결사", filter = SearchFilter.LINE)
        assertEquals(listOf(SearchSectionKind.LINE), onlyLines.sections.map { it.kind })
        assertTrue(search("하층 거리").section(SearchSectionKind.PLACE)!!.items.isNotEmpty())
        assertTrue(search("대정전").section(SearchSectionKind.EVENT)!!.items.isNotEmpty())
        assertTrue(search("  ").sections.isEmpty())
        assertNull(search("  ").empty)
    }

    @Test
    fun commandBarPlaceholders() {
        assertEquals("명령 또는 검색 · 인물 · 용어 · 대사", io.github.lenticualrstone.civilight.domain.story.search.CommandBar.placeholder(io.github.lenticualrstone.civilight.domain.story.search.CommandScreen.HOME))
        assertEquals("명령 또는 검색 · 씬 이동", io.github.lenticualrstone.civilight.domain.story.search.CommandBar.placeholder(io.github.lenticualrstone.civilight.domain.story.search.CommandScreen.STORY))
    }

    @Test
    fun latinIsCaseInsensitive() {
        assertEquals(listOf(0..2), TextQuery("abc").hits("ABC def"))
    }

    @Test
    fun sentenceSplitting() {
        val text = "바람이 잿더미를 쓸고 지나갔다. 꺼진 줄 알았던 오리지늄 등불이 한 번 깜박였다. 그리고 “조용해.” 그가 말했다."
        val parts = Sentences.split(text).map { text.substring(it) }
        assertEquals(listOf("바람이 잿더미를 쓸고 지나갔다.", "꺼진 줄 알았던 오리지늄 등불이 한 번 깜박였다.", "그리고 “조용해.”", "그가 말했다."), parts)
        assertEquals(listOf("짧은 문장. 둘째."), Sentences.split("짧은 문장. 둘째.").map { "짧은 문장. 둘째.".substring(it) })
    }
}
