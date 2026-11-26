package io.github.lenticualrstone.civilight.feature.world.term

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.domain.world.term.FlowMark
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.K
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.PROJECT
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.T1
import io.github.lenticualrstone.civilight.feature.world.common.CardAccess
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** (2026-11-26) The immersive term card's new state: subtitle, flow, speakers, chips, tabs (boards `ImmContent`, `ImmTermWhy`). */
class TermCardImmersiveTest {
    private fun card(doc: ProjectDoc, hiddenOpen: Boolean = false, readOnly: Boolean = false) =
        TermCardState.build(doc, PROJECT, T1, WorldTestFixture.index(doc, hiddenOpen), hiddenOpen, readOnly)

    private fun hideSceneOf(doc: ProjectDoc, blockKey: String): ProjectDoc {
        val scene = doc.index.block(blockKey)!!.sceneKey
        return doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == scene) it.copy(hidden = true) else it }))
    }

    @Test
    fun `subtitle names the first reveal's scene, masked like the reveal row`() {
        val doc = WorldTestFixture.stateA()
        assertEquals("처음 나옴 1-7 씬 3", card(doc).subtitle)
        val hidden = hideSceneOf(doc, K.BLOCK_044)
        assertEquals("처음 나옴 1-7", card(hidden).subtitle)
        assertEquals("처음 나옴 1-7 씬 3", card(hidden, hiddenOpen = true).subtitle)
    }

    @Test
    fun `flow strip, alias chips and tabs`() {
        val ui = card(WorldTestFixture.stateA())
        assertEquals(FlowMark.BEFORE_REVEAL, ui.flow.cells.single { it.episodeKey == K.EPISODE_1_4 }.mark)
        assertEquals(FlowMark.CANDIDATE, ui.flow.cells.single { it.episodeKey == K.EPISODE_1_2 }.mark)
        assertTrue(ui.flow.cells.single { it.episodeKey == K.EPISODE_1_7 }.reveal)
        assertEquals(listOf("오리지늄"), ui.aliasChips)
        assertEquals(listOf("개요", "쓰인 곳", "연결"), TermTab.entries.map { it.label })
        // The `후보` group the WLD-73 scroll targets is still there.
        assertTrue(ui.groups.any { it.id == TermCardScroll.CANDIDATES })
    }

    @Test
    fun `quote rows say who speaks, a hidden speaker is masked while locked`() {
        val doc = WorldTestFixture.stateA()
        val ui = card(doc)
        val b42 = ui.groups.first { it.id == "usage" }.rows.first { it.linkKey != null && it.location == "1-4 씬 2" }
        val speakerKey = doc.index.block(K.BLOCK_B42)!!.speakerKey!!
        val name = doc.index.character(speakerKey)!!.name
        assertEquals(name, b42.who)
        assertEquals(name.take(1), b42.initial)
        // Outside rows have no speaker.
        assertNull(ui.groups.first { it.id == "outside" }.rows.single().who)

        val hidden = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == speakerKey) it.copy(hidden = true) else it }))
        val masked = card(hidden).groups.first { it.id == "usage" }.rows.first { it.location == "1-4 씬 2" }
        assertEquals(WorldCopy.WHO_HIDDEN, masked.who)
        assertEquals(name, card(hidden, hiddenOpen = true).groups.first { it.id == "usage" }.rows.first { it.location == "1-4 씬 2" }.who)
    }

    @Test
    fun `rows in a hidden scene show no speaker while locked`() {
        val doc = hideSceneOf(WorldTestFixture.stateA(), K.BLOCK_B42)
        val row = card(doc).groups.first { it.id == "usage" }.rows.first { it.location.startsWith("1-4") }
        assertEquals(WorldCopy.HIDDEN_SCENE, row.text)
        assertNull(row.who)
        assertNull(row.initial)
    }

    @Test
    fun `a locked term names nothing new`() {
        val doc = WorldTestFixture.withTerm(WorldTestFixture.stateA(), T1) { it.copy(hidden = true) }
        val ui = card(doc)
        assertTrue(ui.access is CardAccess.Locked)
        assertNull(ui.subtitle)
        assertTrue(ui.flow.cells.isEmpty())
        assertTrue(ui.aliasChips.isEmpty())
        assertTrue(ui.neighbors.isEmpty())
        assertTrue(ui.linkingCharacters.isEmpty())
        assertNotNull(card(doc, hiddenOpen = true).subtitle)
    }

    @Test
    fun `neighbour chips open their items`() {
        val ui = card(WorldTestFixture.stateA())
        ui.neighbors.forEach { assertNotNull(it.target); assertTrue(it.kind in listOf("인물", "장소", "사건", "세력")) }
    }

    @Test
    fun `new copy passes the lint`() {
        listOf(
            WorldCopy.firstSeen("1-7 씬 3"),
            WorldCopy.counted(WorldCopy.SEC_FIELDS, 3),
            WorldCopy.counted(WorldCopy.SEC_QUOTES, 2),
            io.github.lenticualrstone.civilight.domain.world.term.TermFlow.LEGEND,
        ).forEach { assertEquals(it, emptyList<Any>(), CopyLint.lint(it)) }
        // The flow marks drawn as text pass the lint (`●` is forbidden: the used mark is a drawn dot).
        FlowMark.entries.forEach { assertEquals(it.name, emptyList<Any>(), CopyLint.lint(it.symbol)) }
    }

    @Test
    fun `flow legend is words in mark order`() {
        val flow = io.github.lenticualrstone.civilight.domain.world.term.TermFlow
        assertEquals("쓴 곳 · 후보 · 공개 전", flow.LEGEND)
        assertEquals(flow.LEGEND, flow.LEGEND_MARKS.joinToString(" · ") { it.second })
        assertEquals(listOf(FlowMark.USED, FlowMark.CANDIDATE, FlowMark.BEFORE_REVEAL), flow.LEGEND_MARKS.map { it.first })
        assertEquals("", FlowMark.USED.symbol)
    }
}
