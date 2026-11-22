package io.github.lenticualrstone.civilight.feature.world.common

import io.github.lenticualrstone.civilight.domain.world.item.CreateResult
import io.github.lenticualrstone.civilight.domain.world.item.WorldItems
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.K
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.T1
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.world.ItemRef
import io.github.lenticualrstone.civilight.model.world.ItemRefKind
import io.github.lenticualrstone.civilight.model.text.docText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Card edits (WLD-06, WLD-08, WLD-11). */
class ItemEditsTest {
    @Test
    fun `WLD-11 create a term, place and faction - an empty name makes nothing`() {
        val ctx = WorldTestFixture.ctx()
        val term = WorldItems.createTerm(WorldTestFixture.stateB(), "  검은 등잔 ", ctx, "타지 않는 등잔") as CreateResult.Created
        assertEquals("검은 등잔", term.doc.index.term(term.key)!!.name)
        assertEquals("타지 않는 등잔", term.doc.index.term(term.key)!!.description.docText)
        assertTrue(WorldItems.createPlace(WorldTestFixture.stateB(), "물레방아", ctx) is CreateResult.Created)
        assertTrue(WorldItems.createFaction(WorldTestFixture.stateB(), "하층 상인회", ctx) is CreateResult.Created)
        assertEquals(CreateResult.EmptyName, WorldItems.createTerm(WorldTestFixture.stateB(), "   ", ctx))
    }

    @Test
    fun `WLD-11 renaming never rewrites manuscript links`() {
        val doc = WorldTestFixture.stateB()
        val renamed = ItemEdits.rename(doc, WorldKind.TERM, T1, "오리지늄 램프", WorldTestFixture.ctx())!!
        assertEquals("오리지늄 램프", renamed.index.term(T1)!!.name)
        val link = renamed.index.block(K.BLOCK_044)!!.runs.filterIsInstance<TextRun.Link>().single()
        assertEquals("오리지늄 등불", link.text)
        assertNull(ItemEdits.rename(doc, WorldKind.PLACE, K.PLACE_ASH_MARKET, " ", WorldTestFixture.ctx()))
        assertEquals("잿더미 골목", ItemEdits.rename(doc, WorldKind.PLACE, K.PLACE_ASH_MARKET, "잿더미 골목", WorldTestFixture.ctx())!!.index.place(K.PLACE_ASH_MARKET)!!.name)
    }

    @Test
    fun `WLD-06 the description edit keeps the description's links`() {
        val doc = WorldTestFixture.stateB()
        val before = doc.index.faction(K.FACTION_LANTERN_SOCIETY)!!.description
        val text = RichTextEdit.textOf(before) + " 추가."
        val after = ItemEdits.setDescription(doc, WorldKind.FACTION, K.FACTION_LANTERN_SOCIETY, text, WorldTestFixture.ctx())
        val edited = after.index.faction(K.FACTION_LANTERN_SOCIETY)!!.description
        assertEquals(text, RichTextEdit.textOf(edited))
        assertEquals(
            before.flatMap { it.runs }.filterIsInstance<TextRun.Link>(),
            edited.flatMap { it.runs }.filterIsInstance<TextRun.Link>(),
        )
        assertEquals("test-device", after.index.faction(K.FACTION_LANTERN_SOCIETY)!!.updatedBy)
    }

    @Test
    fun `WLD-08 관련 ticks and unticks items`() {
        val doc = WorldTestFixture.stateB()
        val place = ItemRef(ItemRefKind.PLACE.open(), K.PLACE_LOWER_STREET)
        val added = ItemEdits.toggleRelated(doc, T1, place, WorldTestFixture.ctx())
        assertTrue(added.index.term(T1)!!.related.any { it.key == K.PLACE_LOWER_STREET })
        assertEquals("등불 결사 · 대정전 · 하층 거리", ItemRefsText.of(added, added.index.term(T1)!!.related, hiddenOpen = false))
        val removed = ItemEdits.toggleRelated(added, T1, place, WorldTestFixture.ctx())
        assertEquals(doc.index.term(T1)!!.related, removed.index.term(T1)!!.related)
    }

    @Test
    fun `the 관련 sheet lists other terms, places and factions with the related ones ticked`() {
        val ui = RelatedOptions.build(WorldTestFixture.stateB(), T1, query = "", hiddenOpen = false)
        assertEquals(listOf("용어", "장소", "세력"), ui.sections.map { it.title })
        val terms = ui.sections[0].options
        assertTrue(terms.none { it.ref.key == T1 })
        assertTrue(terms.single { it.ref.key == K.TERM_BLACKOUT }.checked)
        assertFalse(terms.single { it.ref.key == K.TERM_EMBER_OATH }.checked)
        assertTrue(ui.sections[2].options.single { it.ref.key == K.FACTION_LANTERN_SOCIETY }.checked)
        assertEquals(0, ui.hiddenCount)
        val searched = RelatedOptions.build(WorldTestFixture.stateB(), T1, query = "ㅎㅊ", hiddenOpen = false)
        assertTrue(searched.sections.flatMap { it.options }.all { it.title.startsWith("하층") || it.title.contains("층") })
    }

    @Test
    fun `hidden items leave the 관련 sheet while locked and are counted`() {
        val doc = WorldTestFixture.withTerm(WorldTestFixture.stateB(), K.TERM_EMBER_OATH) { it.copy(hidden = true) }
        val ui = RelatedOptions.build(doc, T1, query = "", hiddenOpen = false)
        assertEquals(1, ui.hiddenCount)
        assertTrue(ui.sections[0].options.none { it.ref.key == K.TERM_EMBER_OATH })
    }
}
