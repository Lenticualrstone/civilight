package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditStateTest {
    private val doc = MapFx.doc()

    private fun build(s: EditorState, mapKey: String = K.MAP_LOWER_DISTRICT) = EditState.build(doc, mapKey, LensPoint.Latest, false, s)!!

    @Test
    fun `header reads 하층 구역 지도 · 편집 with the breadcrumb`() {
        val ui = build(EditorState())
        assertEquals("하층 구역 지도 · 편집", ui.title)
        assertEquals("세계 지도 › 하층 구역 지도", ui.small)
        // (0.2.0, WLD-75) No scale bar and no route rows: distances are hidden, their data kept.
        assertNull(ui.scale)
        assertEquals(listOf("길"), ui.layers.map { it.name })
        assertTrue(ui.items.any { it.text == "핀 · 결사의 문" })
        assertTrue(ui.items.none { it.text == "경로 · 결사의 문 → 상층 관문" })
        assertNull(ui.props)
    }

    @Test
    fun `rule 19 - a hidden place's pin is neither listed nor shown while locked`() {
        val hidden = doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == K.PLACE_ASH_MARKET) it.copy(hidden = true) else it }))
        val ui = EditState.build(hidden, K.MAP_LOWER_DISTRICT, LensPoint.Latest, false, Editor.select(EditorState(), K.PIN_ASH_MARKET))!!
        assertTrue(ui.items.none { it.key == K.PIN_ASH_MARKET })
        assertNull(ui.props)
        val open = EditState.build(hidden, K.MAP_LOWER_DISTRICT, LensPoint.Latest, true, Editor.select(EditorState(), K.PIN_ASH_MARKET))!!
        assertTrue(open.props is PropsUi.PinProps)
    }

    @Test
    fun `WLD-65 vertex handles follow the shape in force at the lens`() {
        val region = doc.maps.getValue(K.MAP_WORLD).regions.first { it.key == K.REGION_UPPER_GUARD }
        val atLatest = build(Editor.select(EditorState(), K.REGION_UPPER_GUARD), K.MAP_WORLD).props as PropsUi.RegionProps
        // WLD-T57: at 최신 · 1-7 the `◆` shape of 1-9 (beyond the 집필 선) is not applied.
        assertEquals(region.vertices, atLatest.vertices)
        assertEquals(listOf("1-9 ◆"), atLatest.shapes.map { it.first })
        val later = EditState.build(doc, K.MAP_WORLD, LensPoint.UnitEnd(K.EPISODE_1_9), false, Editor.select(EditorState(), K.REGION_UPPER_GUARD))!!.props as PropsUi.RegionProps
        assertEquals(atLatest.shapes.single().second, later.vertices)
        assertTrue(later.vertices != region.vertices)
    }

    @Test
    fun `WLD-75 routes are hidden - a stored route is not drawn and cannot be picked`() {
        assertTrue(doc.maps.getValue(K.MAP_LOWER_DISTRICT).routes.any { it.key == K.ROUTE_GATE_TO_UPPER_GATE })
        val ui = build(Editor.select(EditorState(), K.ROUTE_GATE_TO_UPPER_GATE))
        assertNull(ui.props)
        assertTrue(ui.items.none { it.key == K.ROUTE_GATE_TO_UPPER_GATE })
    }

    @Test
    fun `pin, region and label properties`() {
        val pin = build(Editor.select(EditorState(), K.PIN_UPPER_GATE)).props as PropsUi.PinProps
        assertEquals("상층 관문", pin.title)
        assertEquals("1-7부터", pin.reveal)
        assertEquals("없음", pin.layer)
        val region = build(Editor.select(EditorState(), K.REGION_UPPER_GUARD), K.MAP_WORLD).props as PropsUi.RegionProps
        assertEquals("상층 경비대", region.title)
        assertEquals("세력 영역", region.kind)
        assertEquals("세력 · 상층 경비대", region.faction)
        assertEquals("정치", region.layer)
        assertEquals(4, region.vertices.size)
        val label = build(Editor.select(EditorState(), K.mapLabel(2))).props as PropsUi.LabelProps
        assertTrue(label.canCurve)
        val worldLabel = build(Editor.select(EditorState(), K.mapLabel(1)), K.MAP_WORLD).props as PropsUi.LabelProps
        assertFalse(worldLabel.canCurve)
    }

    @Test
    fun `WLD-42 새 영역 · 그리는 중 shows the draft and its vertex bar`() {
        var s = Editor.chooseTool(EditorState(), EditTool.REGION)
        val lower = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
        listOf(Point(0.1f, 0.1f), Point(0.2f, 0.1f)).forEach { s = Editor.tap(s, lower, it, null).state }
        s = Editor.editRegionDraft(s) { it.copy(factionKey = K.FACTION_UPPER_GUARD, revealUnitKey = K.EPISODE_1_7, layerKey = K.mapLayer(2)) }
        val props = build(s).props as PropsUi.RegionDraftProps
        assertEquals("새 영역 · 그리는 중", props.title)
        assertEquals("꼭짓점 2 · 되돌리기 · 닫기", props.vertexBar)
        assertFalse(props.canClose)
        assertEquals("세력 · 상층 경비대", props.faction)
        assertEquals("1-7부터", props.reveal)
        assertEquals("길", props.layer)
        assertEquals("세력 영역", props.kind)
        assertEquals(2, build(s).regionDraft.size)
    }

    @Test
    fun `route draft bar counts points and km on a scaled map`() {
        var s = Editor.chooseTool(EditorState(), EditTool.ROUTE)
        val lower = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
        s = Editor.tap(s, lower, Point(0.20f, 0.40f), null).state
        s = Editor.tap(s, lower, Point(0.45f, 0.40f), null).state
        val props = build(s).props as PropsUi.RouteDraftProps
        assertEquals("지점 2 · 30 km", props.bar)
        assertTrue(props.canFinish)
        assertEquals(2, build(s).routeDraft.size)
    }

    @Test
    fun `a moving pin is drawn at its new spot before 완료`() {
        val s = Editor.startMovePin(EditorState(), K.PIN_ASH_MARKET).copy(movingTo = Point(0.5f, 0.5f))
        val ui = build(s)
        assertEquals("핀 옮기기 · 잿더미 시장" to Point(0.5f, 0.5f), ui.moving)
        assertEquals(Point(0.5f, 0.5f), ui.scene.pin(K.PIN_ASH_MARKET)!!.at)
    }

    @Test
    fun `WLD-41 the pin picker lists places with scene counts and leaves out places pinned here`() {
        val model = PinPick.model(doc, K.MAP_LOWER_DISTRICT, "", hiddenOpen = false)
        assertTrue(model.rows.none { it.placeKey == K.PLACE_SOCIETY_GATE })
        val ruins = model.rows.first { it.placeKey == K.PLACE_PYLON_RUINS }
        assertFalse(ruins.pinned)
        assertTrue(ruins.sub.startsWith("씬 "))
        assertEquals(listOf("송전탑 폐허"), PinPick.model(doc, K.MAP_LOWER_DISTRICT, "송전탑", false).rows.map { it.name })
    }
}
