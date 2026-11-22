package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.cast.export.CharacterExport
import io.github.lenticualrstone.civilight.domain.cast.list.CastList
import io.github.lenticualrstone.civilight.domain.cast.map.RelationMap
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationPanel
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.model.text.plainText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/**
 * Rule 19 · 인물 숨기기 (30-cast.md §4.5), cast's side. The Given "노아 hidden and locked (12
 * characters)" is built from the fixture: 노아 live (fixture.md conflict #22 recipe) and hidden.
 */
class Rule19HidingTest {
    private val hidden = Fx.hide(Fx.noahLive(), K.NOAH)

    @Test
    fun `TC-19-01 list and speaker field`() {
        val list = CastList.build(hidden, Fx.LATEST)
        assertEquals("전체 11", list.chips.first().label)
        assertTrue(list.rows.none { it.key == K.NOAH })
        assertEquals("숨긴 인물 1 · 지문 · PIN으로 열림 · 지문으로 보기", list.lockRow?.text)
        // The speaker of 노아's lines reads `숨긴 인물`; the dialogue text itself is untouched.
        val speaker = ItemRefs.character(hidden, K.NOAH)
        assertTrue(speaker is ResolvedItem.HiddenLocked)
        assertEquals("숨긴 인물", speaker.narrowLabel)
        val line = Fx.linesOf(hidden, K.NOAH).first()
        assertEquals(Fx.doc().index.block(line.key)!!.runs, line.runs)
        // Plain narration naming 노아 stays as written.
        val narration = hidden.index.blocksOfScene(K.SCENE_1_6_5).single { "노아가 웃었다" in it.runs.plainText }
        assertTrue(narration.runs.plainText.contains("노아가 웃었다"))
    }

    @Ignore("spec conflict: TC-19-02 expects `12명 중 5명` (inner board sample); with the fixture's four live pairs at the lens the map has 4 nodes (CST Q14 node rule)")
    @Test
    fun `TC-19-02 map header as stated`() {
        assertEquals("인물 · 12명 중 5명 · 숨김 1", RelationMap.build(hidden, Fx.LATEST).header)
    }

    @Test
    fun `TC-19-02 map and relations`() {
        val map = RelationMap.build(hidden, Fx.LATEST)
        assertEquals("인물 · 12명 중 4명 · 숨김 1", map.header)
        assertTrue(map.nodes.none { it.key == K.NOAH })
        assertTrue(map.lines.none { it.endA == K.NOAH || it.endB == K.NOAH })
        assertEquals("숨긴 인물과의 관계 1", RelationViews.tab(hidden, K.OWEN, Fx.LATEST).hiddenRow)
        assertTrue(RelationViews.tab(hidden, K.OWEN, Fx.LATEST).blocks.none { it.partnerKey == K.NOAH })
        // 호칭 and panel rows of other characters leave the hidden partner out too.
        assertTrue(RelationViews.addressChips(hidden, K.OWEN, Fx.LATEST).none { it.partnerKey == K.NOAH })
        assertTrue(RelationPanel.rows(hidden, K.OWEN, K.block(1, 6, 25)).none { it.partnerKey == K.NOAH })
        assertEquals(1, RelationViews.hiddenPartnerCount(hidden, K.OWEN, hiddenUnlocked = false))
    }

    @Test
    fun `TC-19-03 picker and @ candidates`() {
        val result = CastList.search(hidden, "노", Fx.LATEST)
        assertTrue(result.rows.none { it.key == K.NOAH })
        assertEquals("숨긴 항목 제외", result.footer)
    }

    @Test
    fun `TC-19-04 export keeps only id, name and display name`() {
        val entry = CharacterExport.entry(hidden, Fx.character(hidden, K.NOAH))
        assertEquals(setOf("id", "name", "displayName"), entry.keys)
        assertEquals(K.NOAH, entry["id"].toString().trim('"'))
        assertTrue(CharacterExport.entry(hidden, Fx.character(hidden, K.OWEN)).containsKey("fields"))
    }

    @Test
    fun `TC-19-06 unlocked`() {
        val list = CastList.build(hidden, Fx.LATEST, hiddenUnlocked = true)
        assertEquals("전체 12", list.chips.first().label)
        assertNull(list.lockRow)
        assertTrue(list.rows.single { it.key == K.NOAH }.hiddenTag)
        assertTrue(ItemRefs.character(hidden, K.NOAH, hiddenUnlocked = true) is ResolvedItem.Live)
        assertTrue(RelationViews.tab(hidden, K.OWEN, Fx.LATEST, hiddenUnlocked = true).blocks.any { it.partnerKey == K.NOAH })
    }

    @Test
    fun `TC-19-07 search while locked`() {
        val result = CastList.search(hidden, "노아", Fx.LATEST)
        assertTrue(result.rows.isEmpty())
        assertEquals("숨긴 항목 제외", result.footer)
        assertEquals(listOf(K.NOAH), CastList.search(hidden, "노아", Fx.LATEST, hiddenUnlocked = true).rows.map { it.key })
    }

    @Test
    fun `TC-19-08 change lists`() {
        val records = listOf(K.N1, K.N2, K.R2).map { hidden.index.record(it)!! }
        val rows = CastHiding.changeRows(hidden, records, hiddenUnlocked = false)
        assertEquals(listOf(K.R2), rows.visible.map { it.key })
        assertEquals("숨긴 항목 변화 2", rows.hiddenRow)
        assertNull(CastHiding.changeRows(hidden, records, hiddenUnlocked = true).hiddenRow)
    }
}
