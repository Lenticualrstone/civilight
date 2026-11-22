package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.checks.CheckGroup
import io.github.lenticualrstone.civilight.domain.checks.CheckListRow
import io.github.lenticualrstone.civilight.domain.checks.CheckResult
import io.github.lenticualrstone.civilight.domain.story.search.Highlight
import io.github.lenticualrstone.civilight.domain.story.search.SearchCommand
import io.github.lenticualrstone.civilight.domain.story.search.SearchFilter
import io.github.lenticualrstone.civilight.domain.story.search.SearchItem
import io.github.lenticualrstone.civilight.feature.story.bulk.BulkBody
import io.github.lenticualrstone.civilight.feature.story.check.CheckLine
import io.github.lenticualrstone.civilight.feature.story.check.CheckLines
import io.github.lenticualrstone.civilight.feature.story.common.ChapterGlyph
import io.github.lenticualrstone.civilight.feature.story.search.SearchModel
import io.github.lenticualrstone.civilight.feature.story.where.WhereBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure parts of the motion pass: lazy keys that must be unique, body kinds, and the chapter numeral. */
class MotionModelsTest {
    private fun result(subject: String, fingerprint: String = "f") =
        CheckResult(checkType = "TEST", subjectKeys = listOf(subject), fingerprint = fingerprint, title = "t")

    @Test
    fun checkLinesAreFlatAndKeyedUniquely() {
        val a = result("a")
        val c1 = result("c1")
        val c2 = result("c2")
        val group = CheckGroup("g1", "◇ 묶음")
        val rows = listOf(
            CheckListRow.HiddenGroup(2),
            CheckListRow.Item(a, emptyList(), subRows = listOf(result("s1"), result("s2"))),
            CheckListRow.Group(group, listOf(c1, c2), emptyList()),
        )
        val closed = CheckLines.flatten(rows, expanded = emptyList())
        assertEquals(listOf("hidden", "row:${a.id}", "sub:${a.id}:${result("s1").id}", "sub:${a.id}:${result("s2").id}", "group:g1"), closed.map { it.key })
        val open = CheckLines.flatten(rows, expanded = listOf("g1"))
        assertEquals(closed.size + 2, open.size)
        assertTrue(open.takeLast(2).all { it is CheckLine.Member })
        assertEquals(open.size, open.map { it.key }.toSet().size)
    }

    @Test
    fun aRepeatedResultStillGetsAUniqueKey() {
        val same = result("x")
        val rows = listOf(CheckListRow.Item(same, emptyList(), subRows = listOf(result("y"), result("y"))), CheckListRow.Item(same, emptyList()))
        val keys = CheckLines.flatten(rows, emptyList()).map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun searchRowKeysIgnoreTheIndexAndSurviveRepeatedKeys() {
        val hl = Highlight("오리지늄", emptyList())
        val term = SearchItem.TermItem("T1", hl, "첫 공개")
        val alias = SearchItem.TermItem("T1", hl, "별칭")
        val scene = SearchItem.SceneItem("S1", hl, "")
        val cmd = SearchItem.CommandItem(SearchCommand.OPEN_CHECKS, hl, null)
        val keys = SearchModel.itemKeys("TERM", listOf(term, alias, scene, cmd))
        assertEquals(4, keys.toSet().size)
        // Dropping the first row does not rename the others' keys that stay unique.
        val shorter = SearchModel.itemKeys("TERM", listOf(scene, cmd))
        assertEquals(keys.takeLast(2), shorter)
        assertEquals(listOf("TERM:TermItem:T1", "TERM:TermItem:T1#2"), keys.take(2))
    }

    @Test
    fun theCommandChipListsWithAnEmptyQuery_QA11() {
        assertTrue(SearchModel.listsWithoutQuery("", SearchFilter.COMMAND))
        assertTrue(SearchModel.listsWithoutQuery("  ", SearchFilter.COMMAND))
        assertFalse(SearchModel.listsWithoutQuery("", SearchFilter.ALL))
        assertFalse(SearchModel.listsWithoutQuery(" ", SearchFilter.TERM))
        assertTrue(SearchModel.listsWithoutQuery("등불", SearchFilter.LINE))
    }

    @Test
    fun whereBodyOrderIsTheOldEarlyReturnOrder() {
        assertEquals(WhereBody.FORM, WhereBody.of(false, null))
        assertEquals(WhereBody.CASCADE, WhereBody.of(true, "places"))
        assertEquals(WhereBody.PICK_PLACES, WhereBody.of(false, "places"))
        assertEquals(WhereBody.PICK_REGIONS, WhereBody.of(false, "regions"))
        assertEquals(WhereBody.PICK_EVENT, WhereBody.of(false, "event"))
        assertEquals(WhereBody.PICK_CAST, WhereBody.of(false, "cast"))
        assertEquals(WhereBody.FORM, WhereBody.of(false, "unknown"))
    }

    @Test
    fun bulkBodyIsDecidedByTheKindNotTheData() {
        assertEquals(BulkBody.LOADING, BulkBody.of(loaded = false, wide = true))
        assertEquals(BulkBody.COVER, BulkBody.of(loaded = true, wide = false))
        assertEquals(BulkBody.WIDE, BulkBody.of(loaded = true, wide = true))
    }

    @Test
    fun theChapterNumeralIsDrawnInLatinLettersBehindTheSpecString() {
        assertEquals("I" to " 1장 · 등불지기", ChapterGlyph.split("Ⅰ 1장 · 등불지기"))
        assertEquals("IV" to " 4장", ChapterGlyph.split("Ⅳ 4장"))
        assertEquals("XII" to " 12장", ChapterGlyph.split("Ⅻ 12장"))
        assertNull(ChapterGlyph.split("1장 · 등불지기"))
        assertNull(ChapterGlyph.split("XIII 13장"))
        assertNull(ChapterGlyph.split(""))
    }
}
