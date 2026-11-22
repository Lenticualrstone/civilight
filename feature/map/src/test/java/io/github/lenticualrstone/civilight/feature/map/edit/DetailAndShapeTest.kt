package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.domain.story.lens.EditChoice
import io.github.lenticualrstone.civilight.domain.world.map.RegionShapes
import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScenes
import io.github.lenticualrstone.civilight.feature.map.canvas.SceneMode
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.map.StampKind
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.domain.story.scene.EditContext as StoryEditContext

/** 상세 지형 · 편집 (WLD-60) and region shapes over story time (WLD-65) in the editor. */
class DetailAndShapeTest {
    private val doc = MapFx.doc()
    private val lower get() = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
    private fun story() = StoryEditContext(newKey = MapFx.ctx(3).newKey, now = 1L, device = "test-device")

    // --- WLD-60 ---

    @Test
    fun `WLD-60 the detailed-terrain tool bar and its panel tabs`() {
        assertEquals(listOf("이동", "도장", "강", "길", "라벨", "영역", "레이어"), EditTool.DETAIL.map { it.label })
        assertEquals(listOf("이동", "핀", "영역", "경로", "라벨", "레이어"), EditTool.BASE.map { it.label })
        assertEquals(2, Editor.chooseTool(EditorState(), EditTool.LAYER, detail = true).panelTab)
        assertEquals(1, Editor.chooseTool(EditorState(), EditTool.LAYER).panelTab)
    }

    @Test
    fun `WLD-60 도장 places the palette's stamp, a tap on a stamp selects it`() {
        val s = Editor.chooseStampSize(Editor.chooseStamp(EditorState(), StampKind.BRIDGE), 3)
        assertEquals(EditTool.STAMP, s.tool)
        val out = Editor.tap(s, lower, Point(0.5f, 0.5f), null)
        assertEquals(EditorCommit.PlaceStamp(Point(0.5f, 0.5f), StampKind.BRIDGE, 3), out.commit)
        val hit = Editor.tap(s, lower, Point(0.45f, 0.4f), io.github.lenticualrstone.civilight.feature.map.canvas.MapHit.Stamp(K.stamp(3)))
        assertNull(hit.commit)
        assertEquals(K.stamp(3), hit.state.selected)
    }

    @Test
    fun `WLD-60 the stamp panel reads the chip, size and 장소 연결`() {
        val s = Editor.select(Editor.chooseTool(EditorState(), EditTool.MOVE, detail = true), K.stamp(3))
        val ui = EditState.build(doc, K.MAP_LOWER_DISTRICT, LensPoint.Latest, false, s)!!
        val props = ui.props as PropsUi.StampProps
        assertEquals("마을", props.title)
        assertEquals("마을 · 크기 2 · 0° · 장소 ‘잿더미 시장’", props.data.chip)
        assertEquals("잿더미 시장", props.data.place)
        // `이동`: the next tap moves the stamp.
        val moving = Editor.startMoveStamp(s)
        val out = Editor.tap(moving, lower, Point(0.3f, 0.3f), null)
        assertEquals(EditorCommit.MoveStamp(K.stamp(3), Point(0.3f, 0.3f)), out.commit)
        assertTrue(!out.state.movingStamp)
    }

    @Test
    fun `WLD-60 강 · 길 draw a line point by point`() {
        var s = Editor.chooseTool(EditorState(), EditTool.RIVER, detail = true)
        listOf(Point(0.1f, 0.1f), Point(0.2f, 0.2f), Point(0.3f, 0.1f)).forEach { s = Editor.tap(s, lower, it, null).state }
        val props = EditState.build(doc, K.MAP_LOWER_DISTRICT, LensPoint.Latest, false, s)!!.props as PropsUi.LineDraftProps
        assertEquals("강", props.title)
        assertTrue(props.bar, props.bar.startsWith("지점 3 · "))
        assertTrue(props.canFinish)
        s = Editor.undoLinePoint(s)
        assertEquals(2, s.line!!.size)
        // Another line tool drops the draft; the same tool keeps it.
        assertNull(Editor.chooseTool(s, EditTool.ROAD, detail = true).line)
        assertNotNull(Editor.chooseTool(s, EditTool.RIVER, detail = true).line)
        assertEquals(EditTool.MOVE, Editor.lineFinished(s, "k").tool)
    }

    @Test
    fun `WLD-60 범례 and the drawn road and stamps in the scene`() {
        assertEquals(listOf("마을 · 1", "탑 · 1", "길 · 1"), EditState.legend(lower))
        val scene = MapScenes.build(doc, lower, LensPoint.Latest, false, SceneMode.VIEW)
        assertEquals(1, scene.lines.size)
        assertEquals(setOf(K.stamp(3), K.stamp(4)), scene.stamps.map { it.key }.toSet())
        // A layer chip filter leaves the terrain lines out.
        assertTrue(MapScenes.build(doc, lower, LensPoint.Latest, false, SceneMode.VIEW, layerFilter = "x").lines.isEmpty())
    }

    @Test
    fun `WLD-60 stamp handles snap rotation to 15 degrees and size to 1 2 3`() {
        val c = Point(0.5f, 0.5f)
        assertEquals(0, StampHandles.rotationToward(c, Point(0.5f, 0.1f)))
        assertEquals(90, StampHandles.rotationToward(c, Point(0.9f, 0.5f)))
        assertEquals(45, StampHandles.rotationToward(c, Point(0.6f, 0.4f)))
        assertEquals(1, StampHandles.sizeToward(c, Point(0.51f, 0.51f)))
        assertEquals(3, StampHandles.sizeToward(c, Point(0.6f, 0.6f)))
    }

    // --- WLD-65 ---

    @Test
    fun `WLD-65 a shape with history asks for the two buttons, one without does not`() {
        val withHistory = ShapeEdits.choice(doc, K.MAP_WORLD, K.REGION_UPPER_GUARD, LensPoint.Latest)
        assertTrue(withHistory is EditChoice.TwoButtons)
        withHistory as EditChoice.TwoButtons
        assertEquals("이 값 고치기", withHistory.overwrite.label)
        assertTrue(withHistory.fromHere.label.startsWith("여기서부터 바뀜"))
        assertTrue(ShapeEdits.choice(doc, K.MAP_LOWER_DISTRICT, K.REGION_LOWER_DISTRICT, LensPoint.Latest) is EditChoice.Plain)
        // A past lens always asks.
        assertTrue(ShapeEdits.choice(doc, K.MAP_LOWER_DISTRICT, K.REGION_LOWER_DISTRICT, LensPoint.UnitEnd(K.EPISODE_1_5)) is EditChoice.TwoButtons)
    }

    @Test
    fun `WLD-T57 at 최신 이 값 고치기 edits the base shape, the 1-9 shape stays`() {
        val region = doc.maps.getValue(K.MAP_WORLD).regions.first { it.key == K.REGION_UPPER_GUARD }
        val moved = ShapeEdits.moved(doc, K.MAP_WORLD, region.key, LensPoint.Latest, 0, Point(0.1f, 0.1f))!!
        val after = ShapeEdits.overwrite(doc, K.MAP_WORLD, region.key, LensPoint.Latest, moved, MapFx.ctx(), story())!!
        val base = after.maps.getValue(K.MAP_WORLD).regions.first { it.key == region.key }
        assertEquals(Point(0.1f, 0.1f), base.vertices[0])
        assertEquals(
            RegionShapes.shapeAt(doc, K.MAP_WORLD, region, LensPoint.UnitEnd(K.EPISODE_1_9)),
            RegionShapes.shapeAt(after, K.MAP_WORLD, base, LensPoint.UnitEnd(K.EPISODE_1_9)),
        )
    }

    @Test
    fun `WLD-65 at 1-9 이 값 고치기 edits the record in force, 여기서부터 바뀜 adds one`() {
        val lens = LensPoint.UnitEnd(K.EPISODE_1_9)
        val region = doc.maps.getValue(K.MAP_WORLD).regions.first { it.key == K.REGION_UPPER_GUARD }
        val moved = ShapeEdits.moved(doc, K.MAP_WORLD, region.key, lens, 1, Point(0.9f, 0.9f))!!
        val over = ShapeEdits.overwrite(doc, K.MAP_WORLD, region.key, lens, moved, MapFx.ctx(), story())!!
        assertEquals(moved, RegionShapes.shapeAt(over, K.MAP_WORLD, region, lens))
        assertEquals(region.vertices, over.maps.getValue(K.MAP_WORLD).regions.first { it.key == region.key }.vertices)
        assertEquals(doc.changes.records.size, over.changes.records.size)

        val here = ShapeEdits.fromHere(doc, K.MAP_WORLD, region.key, LensPoint.UnitEnd(K.EPISODE_1_5), moved, MapFx.ctx())!!
        assertEquals(doc.changes.records.size + 1, here.changes.records.size)
        assertEquals(moved, RegionShapes.shapeAt(here, K.MAP_WORLD, region, LensPoint.UnitEnd(K.EPISODE_1_5)))
        assertEquals(2, EditState.shapeHistory(here, K.MAP_WORLD, region.key).size)
        assertTrue(here.changes.records.last().target == ChangeTarget.RegionShape(K.MAP_WORLD, region.key))
    }

    @Test
    fun `WLD-65 비교 draws the 1-9 shape over the one in force`() {
        val s = Editor.toggleCompare(Editor.select(EditorState(), K.REGION_UPPER_GUARD), 0)
        val ui = EditState.build(doc, K.MAP_WORLD, LensPoint.Latest, false, s)!!
        val (other, current) = ui.compare!!
        val region = doc.maps.getValue(K.MAP_WORLD).regions.first { it.key == K.REGION_UPPER_GUARD }
        assertEquals(region.vertices, current)
        assertEquals(RegionShapes.shapeAt(doc, K.MAP_WORLD, region, LensPoint.UnitEnd(K.EPISODE_1_9)), other)
        assertNull(Editor.toggleCompare(s, 0).compare)
    }
}
