package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.K
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.T1
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.queries
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.stateA
import io.github.lenticualrstone.civilight.domain.world.term.FlowCell
import io.github.lenticualrstone.civilight.domain.world.term.FlowMark
import io.github.lenticualrstone.civilight.domain.world.term.NeighborKind
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.domain.world.term.TermFlow
import io.github.lenticualrstone.civilight.domain.world.term.TermFlowStrip
import io.github.lenticualrstone.civilight.domain.world.term.TermSceneNeighbors
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** (2026-11-26) The term card's `쓰인 흐름` strip and `같은 씬에 나온 것` chips. */
class TermFlowNeighborsTest {
    private fun flow(doc: ProjectDoc): TermFlowStrip {
        val q = queries(doc)
        return TermFlow.of(q, q.card(T1)!!)
    }

    private fun cell(strip: TermFlowStrip, episode: Key): FlowCell = strip.cells.single { it.episodeKey == episode }

    @Test
    fun `strip marks candidate, pre-reveal use and the reveal ring per episode`() {
        val doc = stateA()
        val strip = flow(doc)
        assertEquals(TermFlow.episodes(doc).size, strip.cells.size)
        assertEquals(TermFlow.episodes(doc).map { it.key }, strip.cells.map { it.episodeKey })
        assertEquals(FlowCell(K.EPISODE_1_1, "1-1", FlowMark.NONE, false), cell(strip, K.EPISODE_1_1))
        assertEquals(FlowCell(K.EPISODE_1_2, "1-2", FlowMark.CANDIDATE, false), cell(strip, K.EPISODE_1_2))
        assertEquals(FlowCell(K.EPISODE_1_4, "1-4", FlowMark.BEFORE_REVEAL, false), cell(strip, K.EPISODE_1_4))
        assertEquals(FlowCell(K.EPISODE_1_7, "1-7", FlowMark.USED, true), cell(strip, K.EPISODE_1_7))
        assertEquals(1, strip.cells.count { it.reveal })
        assertTrue(strip.title!!.startsWith("쓰인 흐름 · 1-1 ~ "))
        assertEquals(strip.cells.size > TermFlow.MAX_VISIBLE, strip.scrolls)
    }

    @Test
    fun `a use marked 맞음 counts as used, not before reveal`() {
        val doc = stateA()
        val link = queries(doc).usage(T1).first { queries(doc).isBeforeReveal(it) }
        val marked = TermEdits.markIntended(doc, link, WorldFixture.ctx())
        assertEquals(FlowMark.USED, cell(flow(marked), K.EPISODE_1_4).mark)
    }

    @Test
    fun `pure cells take the heaviest mark, ring an empty reveal episode and drop unknown keys`() {
        val eps = listOf("a" to "1-1", "b" to "1-2", "c" to "1-3")
        val cells = TermFlow.cells(
            eps,
            listOf("a" to FlowMark.CANDIDATE, "a" to FlowMark.USED, "b" to FlowMark.BEFORE_REVEAL, "b" to FlowMark.USED, "x" to FlowMark.USED),
            revealEpisode = "c",
        )
        assertEquals(
            listOf(
                FlowCell("a", "1-1", FlowMark.USED, false),
                FlowCell("b", "1-2", FlowMark.BEFORE_REVEAL, false),
                FlowCell("c", "1-3", FlowMark.NONE, true),
            ),
            cells,
        )
        assertEquals("쓰인 흐름 · 1-1 ~ 1-3", TermFlowStrip(cells).title)
        assertEquals("쓰인 흐름 · 1-1", TermFlowStrip(cells.take(1)).title)
        assertNull(TermFlowStrip(emptyList()).title)
        assertFalse(TermFlowStrip(cells).scrolls)
        val many = (1..13).map { FlowCell("k$it", "1-$it", FlowMark.NONE, false) }
        assertTrue(TermFlowStrip(many).scrolls)
    }

    private fun linkRun(n: Int, kind: LinkTargetKind, key: Key, text: String): TextRun =
        TextRun.Link(WorldFixture.key(n), LinkTarget(kind.open(), key), text)

    /** A narration block right after B42 (1-4, where T1 is linked) holding links to other items. */
    private fun withNeighbours(doc: ProjectDoc): ProjectDoc = WorldFixture.addBlockAfter(
        doc,
        K.BLOCK_B42,
        WorldFixture.key(900),
        listOf(
            linkRun(901, LinkTargetKind.CHARACTER, K.SERA, "세라"),
            TextRun.Plain(" 와 "),
            linkRun(902, LinkTargetKind.PLACE, K.PLACE_ASH_MARKET, "잿더미 시장"),
            TextRun.Plain(" "),
            linkRun(903, LinkTargetKind.EVENT, K.EVENT_BLACKOUT, "대정전"),
            TextRun.Plain(" "),
            linkRun(904, LinkTargetKind.TERM, K.TERM_BLACKOUT, "대정전"),
        ),
    )

    @Test
    fun `neighbours are linked items of the term's scenes, by kind, never terms`() {
        val base = stateA()
        val before = TermSceneNeighbors.of(base, T1, hiddenOpen = false).map { it.kind to it.key }.toSet()
        val doc = withNeighbours(base)
        val all = TermSceneNeighbors.of(doc, T1, hiddenOpen = false)
        assertTrue(before.none { it.second == K.PLACE_ASH_MARKET })
        assertTrue(all.any { it.kind == NeighborKind.CHARACTER && it.key == K.SERA })
        assertTrue(all.any { it.kind == NeighborKind.PLACE && it.key == K.PLACE_ASH_MARKET })
        assertTrue(all.any { it.kind == NeighborKind.EVENT && it.key == K.EVENT_BLACKOUT })
        assertTrue(all.none { it.key == K.TERM_BLACKOUT || it.key == T1 })
        // Sorted by kind (인물 · 장소 · 사건 · 세력).
        assertEquals(all.map { it.kind.ordinal }.sorted(), all.map { it.kind.ordinal })
        assertTrue(all.all { it.scenes >= 1 && it.name.isNotBlank() })
    }

    @Test
    fun `links in scenes without the term do not count`() {
        val base = stateA()
        val before = TermSceneNeighbors.of(base, T1, hiddenOpen = false)
        // B32 (1-3) has no T1 link: a place linked there is not a neighbour.
        val doc = WorldFixture.addBlockAfter(base, K.BLOCK_B32, WorldFixture.key(910), listOf(linkRun(911, LinkTargetKind.PLACE, K.PLACE_PYLON_RUINS, "송전탑")))
        assertEquals(before, TermSceneNeighbors.of(doc, T1, hiddenOpen = false))
    }

    @Test
    fun `hidden items and hidden scenes drop out while locked, deleted items always`() {
        var doc = withNeighbours(stateA())
        doc = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.SERA) it.copy(hidden = true) else it }))
        assertTrue(TermSceneNeighbors.of(doc, T1, hiddenOpen = false).none { it.key == K.SERA })
        assertTrue(TermSceneNeighbors.of(doc, T1, hiddenOpen = true).any { it.key == K.SERA })

        val gone = doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == K.PLACE_ASH_MARKET) it.copy(deletedAt = ExampleClock.NOW) else it }))
        assertTrue(TermSceneNeighbors.of(gone, T1, hiddenOpen = true).none { it.key == K.PLACE_ASH_MARKET })

        val sceneKey = doc.index.block(K.BLOCK_B42)!!.sceneKey
        val hiddenScene = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == sceneKey) it.copy(hidden = true) else it }))
        assertTrue(TermSceneNeighbors.of(hiddenScene, T1, hiddenOpen = false).none { it.key == K.PLACE_ASH_MARKET })
        assertTrue(TermSceneNeighbors.of(hiddenScene, T1, hiddenOpen = true).any { it.key == K.PLACE_ASH_MARKET })
    }

    @Test
    fun `an event on a hidden scene drops out while locked, like on the timeline`() {
        val base = withNeighbours(stateA())
        // The event's own scene (1-3) is hidden; the term's scene (1-4) is not.
        val eventScene = base.index.block(K.BLOCK_B32)!!.sceneKey
        assertTrue(eventScene != base.index.block(K.BLOCK_B42)!!.sceneKey)
        var doc = base.copy(world = base.world.copy(events = base.world.events.map { if (it.key == K.EVENT_BLACKOUT) it.copy(sceneKey = eventScene) else it }))
        doc = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == eventScene) it.copy(hidden = true) else it }))
        assertTrue(TermSceneNeighbors.of(doc, T1, hiddenOpen = false).none { it.key == K.EVENT_BLACKOUT })
        assertTrue(TermSceneNeighbors.of(doc, T1, hiddenOpen = true).any { it.key == K.EVENT_BLACKOUT })
        assertTrue(TermSceneNeighbors.of(base, T1, hiddenOpen = false).any { it.key == K.EVENT_BLACKOUT })
    }

    @Test
    fun `title skips an empty episode code range`() {
        fun c(code: String) = FlowCell("k$code", code, FlowMark.NONE, false)
        assertEquals("쓰인 흐름", TermFlowStrip(listOf(c(""), c(""))).title)
        assertEquals("쓰인 흐름 · 1-2", TermFlowStrip(listOf(c(""), c("1-2"))).title)
        assertEquals("쓰인 흐름 · 1-1 ~ 1-3", TermFlowStrip(listOf(c("1-1"), c(""), c("1-3"))).title)
    }
}
