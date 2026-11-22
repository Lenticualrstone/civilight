package io.github.lenticualrstone.civilight.feature.map.common

import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.TrashContext
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.domain.world.gen.RerollMode
import io.github.lenticualrstone.civilight.domain.world.gen.RerollResult
import io.github.lenticualrstone.civilight.domain.world.gen.StepReroll
import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.feature.map.canvas.BasePalette
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBase
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBases
import io.github.lenticualrstone.civilight.feature.map.canvas.baseLines
import io.github.lenticualrstone.civilight.feature.map.hex.HexPaint
import io.github.lenticualrstone.civilight.feature.map.hex.HexPaintState
import io.github.lenticualrstone.civilight.feature.map.hex.HexPaintUiBuilder
import io.github.lenticualrstone.civilight.feature.map.hex.HexTool
import io.github.lenticualrstone.civilight.feature.map.list.MapListState
import io.github.lenticualrstone.civilight.feature.map.text.CondState
import io.github.lenticualrstone.civilight.feature.map.text.TextMapFlow
import io.github.lenticualrstone.civilight.feature.map.text.TextMapState
import io.github.lenticualrstone.civilight.feature.map.track.TrackState
import io.github.lenticualrstone.civilight.domain.world.map.HexMaps
import io.github.lenticualrstone.civilight.model.map.GenerationStepKind
import io.github.lenticualrstone.civilight.model.map.HexGrid
import io.github.lenticualrstone.civilight.model.map.Terrain
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Batch-3 rules that cut across screens: re-roll merge, map delete, rule 19 masking, re-reading, world lines. */
class Batch3RulesTest {
    private val doc = MapFx.doc()

    @Test
    fun `WLD-56 a re-roll lands only while its map is unchanged`() {
        val result = StepReroll.apply(doc, K.MAP_LOWER_DISTRICT, GenerationStepKind.RIVER, 4242, RerollMode.KEEP_EDITED, MapFx.ctx()) as RerollResult.Applied
        // Another map changed meanwhile: the re-roll still lands, the other change stays.
        val renamed = doc.copy(maps = doc.maps + (K.MAP_WORLD to doc.maps.getValue(K.MAP_WORLD).copy(name = "새 이름")))
        val merged = RerollMerge.splice(renamed, doc, result.doc, K.MAP_LOWER_DISTRICT)!!
        assertEquals("새 이름", merged.maps.getValue(K.MAP_WORLD).name)
        assertEquals(result.doc.maps.getValue(K.MAP_LOWER_DISTRICT), merged.maps.getValue(K.MAP_LOWER_DISTRICT))
        // The re-rolled map itself changed meanwhile: nothing lands.
        val moved = doc.copy(maps = doc.maps + (K.MAP_LOWER_DISTRICT to doc.maps.getValue(K.MAP_LOWER_DISTRICT).copy(name = "바뀜")))
        assertNull(RerollMerge.splice(moved, doc, result.doc, K.MAP_LOWER_DISTRICT))
    }

    @Test
    fun `WLD-36 a deleted map leaves the map list`() {
        val ctx = TrashContext(1L, ZoneId.of("Asia/Seoul"), "test-device", MapFx.ctx().newKey)
        val result = TrashOps.delete(doc, ItemId.map(K.MAP_ASH_MARKET), ctx)
        assertEquals(listOf(ItemId.map(K.MAP_ASH_MARKET)), result.trashed)
        val before = MapListState.build(doc, LensPoint.Latest, false).cards.map { it.key }
        val after = MapListState.build(result.doc, LensPoint.Latest, false).cards.map { it.key }
        assertTrue(K.MAP_ASH_MARKET in before)
        assertFalse(K.MAP_ASH_MARKET in after)
    }

    @Test
    fun `rule 19 the track leaves stops at hidden places out while locked`() {
        val hidden = doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == K.PLACE_LOWER_STREET) it.copy(hidden = true) else it }))
        val locked = TrackState.build(hidden, K.MAP_LOWER_DISTRICT, listOf(K.RIEN), LensPoint.Latest, false, null)!!
        assertTrue(locked.rows.none { it.place == "하층 거리" })
        assertTrue(locked.places.none { it.startsWith("하층 거리") })
        val open = TrackState.build(hidden, K.MAP_LOWER_DISTRICT, listOf(K.RIEN), LensPoint.Latest, true, null)!!
        assertTrue(open.rows.any { it.place == "하층 거리" })
    }

    @Test
    fun `rule 19 the hex cell card hides a hidden place while locked`() {
        val world = doc.maps.getValue(K.MAP_WORLD)
        val grid = HexGrid(cols = 32, rows = 24, cells = List(32 * 24) { Terrain.PLAIN.open() })
        val pin = world.pins.first { it.key == K.PIN_WORLD_UPPER_CITY }
        val hidden = doc.copy(
            maps = doc.maps + (world.key to world.copy(hex = grid, regions = emptyList())),
            world = doc.world.copy(places = doc.world.places.map { if (it.key == pin.placeKey) it.copy(hidden = true) else it }),
        )
        val cell = HexMaps.cellAt(grid, io.github.lenticualrstone.civilight.model.Point(pin.x, pin.y))!!
        val s = HexPaint.tap(HexPaint.chooseTool(HexPaintState(), HexTool.PICK), grid, HexMaps.center(grid, cell)).state
        val locked = HexPaintUiBuilder.build(hidden, world.key, LensPoint.Latest, false, s)!!.cell!!
        assertTrue(locked.text, locked.text.endsWith("장소 없음"))
        val open = HexPaintUiBuilder.build(hidden, world.key, LensPoint.Latest, true, s)!!.cell!!
        assertFalse(open.text.endsWith("장소 없음"))
    }

    @Test
    fun `WLD-61 an unchanged input is not read again, and the author's 위치 survives a reading`() {
        val text = "북쪽은 눈 덮인 산맥이 가로막고, 동쪽은 메마른 사막."
        val read = TextMapFlow.read(doc, TextMapState(text = text), MapFx.ctx())
        assertTrue(TextMapFlow.isRead(read))
        assertFalse(TextMapFlow.isRead(read.copy(text = "$text 서쪽 숲.")))
        val placed = TextMapFlow.setPosition(read, read.conditions[0].key, setOf(4))
        val again = TextMapFlow.read(doc, placed.copy(text = "$text 서쪽 숲."), MapFx.ctx())
        assertEquals(listOf(4), again.conditions.first { it.what == "산맥" && it.userPosition != null }.userPosition)
        assertEquals(listOf("눈 덮임", "메마름"), TextMapFlow.traits("눈 덮임 · 메마름"))
        assertEquals(listOf("넓음"), TextMapFlow.traits(" 넓음 ,"))
        assertNull(TextMapFlow.session(read.copy(engine = 1)).textModel)
        assertEquals(CondState("k", "숲"), CondState.of(CondState("k", "숲").toModel(), fromText = false))
    }

    @Test
    fun `a detailed-terrain map without drawn rivers shows the world's rivers`() {
        val lower = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
        val base = MapBases.of(lower) as MapBase.Generated
        assertTrue(base.worldRivers)
        assertFalse(base.worldRoads)
        val palette = BasePalette(true, List(5) { 0xFF808080.toInt() }, 0, 0, 0, 0, 0)
        val lines = baseLines(base, palette)
        assertNotNull(base.world)
        assertTrue(lines.size >= base.world!!.rivers.size)
    }
}
