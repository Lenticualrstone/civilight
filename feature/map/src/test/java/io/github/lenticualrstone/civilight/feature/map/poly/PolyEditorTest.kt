package io.github.lenticualrstone.civilight.feature.map.poly

import io.github.lenticualrstone.civilight.domain.world.gen.GenSession
import io.github.lenticualrstone.civilight.domain.world.gen.MapGenerator
import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.domain.world.map.PolyMaps
import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 다각형 · 지역 나누기 (WLD-59) on a POLY map made by the generator (the fixture has none). */
class PolyEditorTest {
    private val made: Pair<ProjectDoc, WorldMap> = run {
        var n = 100
        val session = GenSession.start({ n++ }, format = MapFormat.POLY)
        val edit = MapGenerator.start(MapFx.doc(), session, MapFx.ctx()) as MapEdit.Applied
        edit.doc to edit.value
    }
    private val doc = made.first
    private val map = made.second
    private val poly = map.poly!!

    private fun landCellOf(province: Key): Int = poly.cellProvince.entries.first { it.value == province && poly.cells[it.key].land }.key

    private fun otherLandCell(province: Key): Int = poly.cellProvince.entries.first { it.value != province && poly.cells[it.key].land }.key

    @Test
    fun `rows read 지역 N | 칸 N and 고르기 selects the province under the tap`() {
        val ui = PolyUiBuilder.build(doc, map.key, LensPoint.Latest, false, PolyState())!!
        assertEquals("다각형 · 지역 나누기", ui.title)
        assertEquals("${map.name} › 지방", ui.small)
        assertEquals(poly.provinces.size, ui.rows.size)
        assertEquals("지역 1", ui.rows.first().name)
        assertEquals("칸 ${PolyMaps.cellCount(poly, poly.provinces.first().key)}", ui.rows.first().cells)

        val first = poly.provinces.first().key
        val cell = landCellOf(first)
        val picked = PolyEditor.tap(PolyState(), poly, poly.cells[cell].site).state
        assertEquals(first, picked.selected)
        val props = PolyUiBuilder.build(doc, map.key, LensPoint.Latest, false, picked)!!.props!!
        assertEquals("지역 1", props.name)
        assertEquals("없음", props.faction)
    }

    @Test
    fun `칸 넣기 gives the tapped land cell to the selected province and counts it`() {
        val target = poly.provinces.first().key
        val cell = otherLandCell(target)
        val s = PolyEditor.chooseTool(PolyEditor.select(PolyState(), target), PolyTool.ASSIGN)
        val out = PolyEditor.tap(s, poly, poly.cells[cell].site)
        assertEquals(PolyCommit.Assign(cell, target), out.commit)
        assertEquals(1, out.state.assigned)
        val ui = PolyUiBuilder.build(doc, map.key, LensPoint.Latest, false, out.state)!!
        assertEquals("칸 1 → 지역 1", ui.assignChip)
        val after = PolyMaps.assign(doc, map.key, listOf(cell), target, MapFx.ctx())!!
        assertEquals(target, after.maps.getValue(map.key).poly!!.cellProvince[cell])
        // Without a selected province nothing happens.
        assertNull(PolyEditor.tap(PolyEditor.chooseTool(PolyState(), PolyTool.ASSIGN), poly, poly.cells[cell].site).commit)
    }

    @Test
    fun `합치기 merges the tapped province into the selected one`() {
        val into = poly.provinces[0].key
        val from = poly.provinces.drop(1).first { p -> poly.cellProvince.any { it.value == p.key && poly.cells[it.key].land } }.key
        val s = PolyEditor.chooseTool(PolyEditor.select(PolyState(), into), PolyTool.MERGE)
        val out = PolyEditor.tap(s, poly, poly.cells[landCellOf(from)].site)
        assertEquals(PolyCommit.Merge(from, into), out.commit)
    }

    @Test
    fun `나누기 picks cells of the selected province, then makes a new one`() {
        val province = poly.provinces.maxBy { PolyMaps.cellCount(poly, it.key) }.key
        val cells = poly.cellProvince.filterValues { it == province }.keys.filter { poly.cells[it].land }.take(2)
        var s = PolyEditor.chooseTool(PolyEditor.select(PolyState(), province), PolyTool.SPLIT)
        cells.forEach { s = PolyEditor.tap(s, poly, poly.cells[it].site).state }
        assertEquals(cells.toSet(), s.split.toSet())
        val bar = PolyUiBuilder.build(doc, map.key, LensPoint.Latest, false, s)!!.splitBar!!
        assertEquals("칸 2", bar.first)
        assertTrue(bar.second)
        // A second tap on a chosen cell takes it back.
        assertEquals(1, PolyEditor.tap(s, poly, poly.cells[cells[0]].site).state.split.size)
        val (after, key) = PolyMaps.split(doc, map.key, province, s.split, "지역 9", PolyUiBuilder.nextColor(poly), MapFx.ctx())!!
        assertEquals(2, PolyMaps.cellCount(after.maps.getValue(map.key).poly!!, key))
        assertEquals(province, PolyEditor.splitDone(s, null).selected)
        assertEquals(key, PolyEditor.splitDone(s, key).selected)
    }

    @Test
    fun `땅 · 바다 flips the tapped cell and the layer tool opens the layer tab`() {
        val cell = landCellOf(poly.provinces.first().key)
        assertEquals(PolyCommit.ToggleLand(cell), PolyEditor.tap(PolyEditor.chooseTool(PolyState(), PolyTool.LAND), poly, poly.cells[cell].site).commit)
        assertEquals(1, PolyEditor.chooseTool(PolyState(), PolyTool.LAYER).panelTab)
        assertEquals(0, PolyEditor.chooseTool(PolyState(panelTab = 1), PolyTool.PICK).panelTab)
        val s = PolyState(PolyTool.SPLIT, "k", listOf(1, 2), 3, 1)
        assertEquals(s, PolyState.decode(s.encode()))
        assertNotNull(PolyUiBuilder.nextColor(poly))
    }
}
