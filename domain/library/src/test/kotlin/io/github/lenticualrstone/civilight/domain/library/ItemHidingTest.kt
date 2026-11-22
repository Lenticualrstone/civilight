package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.hiding.ItemSurfaces
import io.github.lenticualrstone.civilight.domain.library.hiding.ScriptSceneView
import io.github.lenticualrstone.civilight.domain.library.masking.RecentTarget
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.domain.library.masking.Recents
import io.github.lenticualrstone.civilight.domain.library.trash.TrashRows
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.app.RecentItem
import io.github.lenticualrstone.civilight.model.app.RecentKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** §4.4 item hiding (I-01 … I-18), LIB-36. Fixture: 노아 hidden in 잿빛; 1-7 씬 4 hidden. */
class ItemHidingTest {
    private val doc: ProjectDoc = Fixtures.withSceneHidden(Fixtures.withNoahHidden(), ExampleKeys.SCENE_1_7_4)
    private val items = ItemSurfaces(doc)

    @Test
    fun `I-01 cast list`() {
        val locked = items.castList(locked = true)
        assertFalse(locked.items.any { it.key == ExampleKeys.NOAH })
        assertEquals(11, locked.items.size)
        assertEquals("숨긴 인물 1", locked.lockedRow!!.title)
        assertEquals("지문 · PIN으로 열림", locked.lockedRow!!.sub)
        assertEquals("지문으로 보기", locked.lockedRow!!.action)
        val open = items.castList(locked = false)
        assertTrue(open.items.any { it.key == ExampleKeys.NOAH })
        assertEquals(setOf(ExampleKeys.NOAH), open.taggedHidden)
        assertNull(open.lockedRow)
    }

    @Test
    fun `I-02 story structure`() {
        val locked = items.structureScene(ExampleKeys.SCENE_1_7_4, locked = true)
        assertEquals("1-7 씬 4 · 숨긴 씬", locked.text)
        assertNull(locked.title)
        val titled = ItemSurfaces(Fixtures.withSceneHidden(Fixtures.example(), ExampleKeys.SCENE_1_7_3))
        assertEquals("1-7 씬 3 · 숨긴 씬", titled.structureScene(ExampleKeys.SCENE_1_7_3, locked = true).text)
        val open = titled.structureScene(ExampleKeys.SCENE_1_7_3, locked = false)
        assertEquals("꺼진 등불", open.title)
        assertTrue(open.hiddenTag)
    }

    @Test
    fun `I-03 script shows one line for a hidden scene of 12 blocks`() {
        val next = Fixtures.keys(30)
        var after: String? = null
        val blocks = List(12) { i ->
            val order = OrderKeys.between(after, null)
            after = order
            Block(next(), ExampleKeys.SCENE_1_7_4, order, 58 + i, BlockKind.NARRATION.open(), runs = listOf(TextRun.Plain("줄 ${i + 1}")))
        }
        val withBlocks = ItemSurfaces(Fixtures.withBlocks(doc, blocks))
        val locked = withBlocks.scriptScene(ExampleKeys.SCENE_1_7_4, locked = true) as ScriptSceneView.LockedLine
        assertEquals("숨긴 씬 · 블록 12 · 지문으로 보기", locked.text)
        assertEquals(12, (withBlocks.scriptScene(ExampleKeys.SCENE_1_7_4, locked = false) as ScriptSceneView.Blocks).blocks.size)
    }

    private val with165 = Fixtures.withScene165Live(doc)
    private val items165 = ItemSurfaces(with165)

    @Test
    fun `I-04 speaker cell of 노아's line in 1-6 씬 5`() {
        val line = with165.manuscript.liveBlocksOf(ExampleKeys.SCENE_1_6_5).first { it.speakerKey == ExampleKeys.NOAH }
        assertEquals("숨긴 인물", items165.speakerLabel(line.speakerKey!!, locked = true))
        assertEquals(line.runs, items165.displayRuns(line.runs, locked = true))
        assertEquals("노아", items165.speakerLabel(line.speakerKey!!, locked = false))
    }

    @Test
    fun `I-05 typed text naming 노아 stays as typed`() {
        val narration = with165.manuscript.liveBlocksOf(ExampleKeys.SCENE_1_6_5).first { it.runs.any { r -> r.shownText == "노아가 웃었다." } }
        assertEquals(narration.runs, items165.displayRuns(narration.runs, locked = true))
        assertEquals(narration.runs, items165.displayRuns(narration.runs, locked = false))
    }

    @Test
    fun `I-06 speaker picker`() {
        val locked = items.speakerPicker(locked = true)
        assertFalse(locked.items.any { it.key == ExampleKeys.NOAH })
        assertEquals("숨긴 인물 1 · 지문으로 보기", locked.lockedRow!!.inline)
        assertTrue(items.speakerPicker(locked = false).items.any { it.key == ExampleKeys.NOAH })
    }

    @Test
    fun `I-07 search for 노아`() {
        val locked = items165.searchScope(locked = true)
        assertFalse(ExampleKeys.NOAH in locked.cardKeys)
        assertTrue(ExampleKeys.SCENE_1_6_5 in locked.sceneKeys)
        assertFalse(ExampleKeys.SCENE_1_7_4 in locked.sceneKeys)
        assertEquals("숨긴 항목 제외", locked.footer)
        val open = items165.searchScope(locked = false)
        assertTrue(ExampleKeys.NOAH in open.cardKeys)
        assertNull(open.footer)
    }

    @Test
    fun `I-08 오웬's relations`() {
        val locked = items.relationsOf(ExampleKeys.OWEN, locked = true)
        assertEquals(listOf(ExampleKeys.REL_RIEN_OWEN), locked.visible.map { it.key })
        assertEquals("숨긴 인물과의 관계 1", locked.lockedRow)
        assertEquals(setOf(ExampleKeys.REL_RIEN_OWEN, ExampleKeys.REL_OWEN_NOAH), items.relationsOf(ExampleKeys.OWEN, locked = false).visible.map { it.key }.toSet())
    }

    @Test
    fun `I-09 relationship map`() {
        assertEquals("12명 중 5명 · 숨김 1", ItemSurfaces.relationMapSummary(total = 12, shown = 5, hiddenCount = 1))
        val all = doc.cast.characters.filter { it.deletedAt == null }.map { it.key }
        assertEquals(12, all.size)
        assertFalse(ExampleKeys.NOAH in items.mapNodes(all, locked = true))
        assertTrue(ExampleKeys.NOAH in items.mapNodes(all, locked = false))
    }

    @Test
    fun `I-10 lens records of 노아 are grouped`() {
        val records = doc.changes.records.filter { doc.liveness.isLive(it) }
        val locked = items.lensRecords(records, locked = true)
        assertEquals("숨긴 항목 변화 2", locked.lockedRow)
        assertFalse(locked.visible.any { it.key == ExampleKeys.N1 || it.key == ExampleKeys.N2 })
        assertEquals(records, items.lensRecords(records, locked = false).visible)
    }

    @Test
    fun `I-11 check results collapse into one row`() {
        assertEquals("▲ 숨긴 항목 관련 2 · 지문으로 보기", ItemSurfaces.lockedChecksRow(2))
    }

    @Test
    fun `I-12 route map`() {
        assertEquals("숨긴 씬", items.routeStretchLabel(ExampleKeys.SCENE_1_7_4, locked = true))
        assertFalse(items.hasRoute(ExampleKeys.NOAH, locked = true))
        assertNull(items.routeStretchLabel(ExampleKeys.SCENE_1_7_4, locked = false))
        assertTrue(items.hasRoute(ExampleKeys.NOAH, locked = false))
    }

    @Test
    fun `I-13 recorder link and @ link to 노아`() {
        assertEquals("숨긴 항목", items.recorderTargetLabel("노아", isHidden = true, locked = true))
        assertEquals("노아", items.recorderTargetLabel("노아", isHidden = true, locked = false))
        val atLink = doc.allBlocks.flatMap { it.runs }.filterIsInstance<TextRun.Link>().first { it.target.key == ExampleKeys.NOAH }
        assertEquals("숨긴 항목", (items.displayRuns(listOf(atLink), locked = true).single() as TextRun.Link).text)
    }

    @Test
    fun `I-14 trash entry of hidden 노아`() {
        val base = Fixtures.example()
        val trashed = base.copy(
            cast = base.cast.copy(characters = base.cast.characters.map { if (it.key == ExampleKeys.NOAH) it.copy(hidden = true) else it }),
            trash = base.trash.copy(entries = base.trash.entries.map { if (it.key == ExampleKeys.TRASH_NOAH) it.copy(hiddenAtDelete = true) else it }),
        )
        val entry = trashed.trash.entries.single { it.key == ExampleKeys.TRASH_NOAH }
        val rows = TrashRows(Fixtures.NOW, Fixtures.ZONE)
        val locked = rows.row(entry, trashed, itemsLocked = true)
        assertEquals("숨긴 인물 · 3일 남음", locked.titleWithDays)
        assertTrue(locked.restoreNeedsAuth)
        val open = rows.row(entry, trashed, itemsLocked = false)
        assertEquals("노아", open.title)
        assertTrue(open.restoreNeedsAuth)
    }

    @Test
    fun `I-15 recents never list hidden items`() {
        val item = RecentItem(Fixtures.keys()(), doc.key, RecentKind.CHARACTER, ExampleKeys.NOAH, Fixtures.NOW)
        val target = { _: RecentItem -> RecentTarget("노아", hidden = true, trashed = false) }
        assertEquals(emptyList<Any>(), Recents.cards(listOf(item), mapOf(doc.key to ProjectProfile.of(doc)), target))
    }

    @Test
    fun `I-16 hidden 노아 speaks in a visible 씬 3`() {
        val line = Block(
            Fixtures.keys(31)(), ExampleKeys.SCENE_1_6_3, OrderKeys.between(doc.index.blocksOfScene(ExampleKeys.SCENE_1_6_3).maxOf { it.order }, null),
            33, BlockKind.DIALOGUE.open(), speakerKey = ExampleKeys.NOAH, runs = listOf(TextRun.Plain("저도 갈래요.")),
        )
        val withLine = Fixtures.withBlocks(doc, listOf(line))
        val findings = ItemSurfaces(withLine).hiddenSpeakersInVisibleScenes()
        val f = findings.single { it.sceneKey == ExampleKeys.SCENE_1_6_3 }
        assertEquals("숨긴 인물이 보이는 씬에 나옴 · 씬 3", f.text)
        assertEquals(listOf("씬도 숨기기", "그대로"), f.actions)
        assertFalse(withLine.index.unit(ExampleKeys.SCENE_1_6_3)!!.hidden)
    }

    @Test
    fun `I-17 @ candidates for 노 while locked`() {
        val locked = items.speakerPicker(locked = true, query = "노")
        assertTrue(locked.items.isEmpty())
        assertEquals("숨긴 인물 1 · 지문으로 보기", locked.lockedRow!!.inline)
        assertEquals(listOf(ExampleKeys.NOAH), items.speakerPicker(locked = false, query = "노").items.map { it.key })
    }

    @Test
    fun `I-18 hidden place 결사의 문 on the map`() {
        val hiddenGate = doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == ExampleKeys.PLACE_SOCIETY_GATE) it.copy(hidden = true) else it }))
        val surfaces = ItemSurfaces(hiddenGate)
        val map = hiddenGate.maps.getValue(ExampleKeys.MAP_LOWER_DISTRICT)
        assertFalse(surfaces.visiblePins(map, locked = true).any { it.placeKey == ExampleKeys.PLACE_SOCIETY_GATE })
        assertTrue(surfaces.visiblePins(map, locked = false).any { it.placeKey == ExampleKeys.PLACE_SOCIETY_GATE })
        val gateRegion = map.regions.first().copy(placeKey = ExampleKeys.PLACE_SOCIETY_GATE)
        val mapWithRegion = map.copy(regions = listOf(gateRegion))
        assertTrue(surfaces.visibleRegions(mapWithRegion, locked = true).isEmpty())
        assertEquals(1, surfaces.visibleRegions(mapWithRegion, locked = false).size)
    }
}
