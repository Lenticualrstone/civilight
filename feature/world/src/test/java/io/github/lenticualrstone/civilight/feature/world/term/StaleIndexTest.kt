package io.github.lenticualrstone.civilight.feature.world.term

import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.K
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.PROJECT
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.T1
import io.github.lenticualrstone.civilight.feature.world.list.WorldListState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.navigation.WorldTab
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The view models render a new document with the previous index while a rescan runs (TermIndexes
 * keeps the old one). Card and list must not fail on candidates the document no longer holds.
 */
class StaleIndexTest {
    private val stale = WorldTestFixture.index(WorldTestFixture.stateA())

    private fun build(doc: ProjectDoc) {
        val card = TermCardState.build(doc, PROJECT, T1, stale, hiddenOpen = false, readOnly = false)
        WorldListState.build(doc, WorldTab.TERMS, stale, hiddenOpen = false)
        card.groups.flatMap { it.rows }.forEach { row ->
            assertTrue(row.boxStart in 0..row.text.length && row.boxEnd in row.boxStart..row.text.length)
        }
    }

    @Test
    fun `b12 cut shorter than the old candidate span`() {
        val doc = io.github.lenticualrstone.civilight.domain.world.common.DocRuns.updateBlock(WorldTestFixture.stateA(), K.BLOCK_B12) {
            it.copy(runs = listOf(TextRun.Plain("등불")))
        }!!
        build(doc)
    }

    @Test
    fun `b12 in the trash`() {
        val doc = io.github.lenticualrstone.civilight.domain.world.common.DocRuns.updateBlock(WorldTestFixture.stateA(), K.BLOCK_B12) {
            it.copy(deletedAt = 1L, trashEntryKey = "E")
        }!!
        build(doc)
    }

    @Test
    fun `right after 잇기 on the candidate`() {
        val doc = WorldTestFixture.stateA()
        val candidate = stale.of(T1).single()
        build(TermEdits.link(doc, candidate, WorldTestFixture.ctx())!!.doc)
    }
}
