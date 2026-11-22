package io.github.lenticualrstone.civilight.feature.map.edit

import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.feature.map.canvas.MapHit
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.map.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The editor reducer (WLD-40 … 44): only the chosen tool acts; drafts are saveable. */
class EditorTest {
    private val doc = MapFx.doc()
    private val lower = doc.maps.getValue(K.MAP_LOWER_DISTRICT)

    @Test
    fun `move tool selects what was tapped and clears on empty map`() {
        val s = Editor.tap(EditorState(), lower, Point(0.2f, 0.4f), MapHit.Pin(K.PIN_SOCIETY_GATE)).state
        assertEquals(K.PIN_SOCIETY_GATE, s.selected)
        assertNull(Editor.tap(s, lower, Point(0.9f, 0.9f), null).state.selected)
    }

    @Test
    fun `pin tool asks for a place at the tapped spot, a pin tap selects it`() {
        val s = Editor.chooseTool(EditorState(), EditTool.PIN)
        val out = Editor.tap(s, lower, Point(0.6f, 0.7f), null)
        assertEquals(EditorEffect.PICK_PLACE, out.effect)
        assertEquals(Point(0.6f, 0.7f), out.state.pendingPin)
        val onPin = Editor.tap(s, lower, Point(0.2f, 0.4f), MapHit.Pin(K.PIN_SOCIETY_GATE))
        assertNull(onPin.effect)
        assertEquals(K.PIN_SOCIETY_GATE, onPin.state.selected)
        assertNull(Editor.pinPlaced(out.state, "PIN").pendingPin)
    }

    @Test
    fun `WLD-42 region tool adds vertices, 되돌리기 removes the last, 닫기 needs three`() {
        var s = Editor.chooseTool(EditorState(), EditTool.REGION)
        s = Editor.tap(s, lower, Point(0.1f, 0.1f), null).state
        s = Editor.tap(s, lower, Point(0.3f, 0.1f), MapHit.Pin(K.PIN_SOCIETY_GATE)).state
        assertEquals(2, Editor.vertexCount(s))
        assertFalse(s.region!!.draft.canClose)
        s = Editor.tap(s, lower, Point(0.3f, 0.3f), null).state
        assertTrue(s.region!!.draft.canClose)
        assertEquals("꼭짓점 3 · 되돌리기 · 닫기", s.region!!.draft.bar)
        s = Editor.undoVertex(s)
        assertEquals(2, Editor.vertexCount(s))
        s = Editor.editRegionDraft(s) { it.copy(name = "상층 관문 구역") }
        assertEquals(Point(0.3f, 0.1f), s.region!!.vertices.last())
        assertEquals(null, Editor.regionClosed(s, "R").region)
        assertEquals("R", Editor.regionClosed(s, "R").selected)
    }

    @Test
    fun `WLD-43 route tool snaps taps near a pin to that pin`() {
        var s = Editor.chooseTool(EditorState(), EditTool.ROUTE)
        s = Editor.tap(s, lower, Point(0.205f, 0.405f), null).state
        s = Editor.tap(s, lower, Point(0.30f, 0.20f), null).state
        assertEquals(RoutePoint.pin(K.PIN_SOCIETY_GATE), s.routeDraft.points.first())
        assertEquals(RoutePoint.at(0.30f, 0.20f), s.routeDraft.points.last())
        s = Editor.undoRoutePoint(s)
        assertEquals(1, s.routeDraft.points.size)
        assertNull(Editor.undoRoutePoint(s).route)
    }

    @Test
    fun `label tool asks for text, a label tap selects it`() {
        val s = Editor.chooseTool(EditorState(), EditTool.LABEL)
        assertEquals(EditorEffect.LABEL_TEXT, Editor.tap(s, lower, Point(0.5f, 0.5f), null).effect)
        assertEquals(K.mapLabel(2), Editor.tap(s, lower, Point(0.3f, 0.55f), MapHit.Label(K.mapLabel(2))).state.selected)
    }

    @Test
    fun `핀 옮기기 takes the next tap whatever the tool, then 완료 ends it`() {
        var s = Editor.startMovePin(Editor.chooseTool(EditorState(), EditTool.REGION), K.PIN_ASH_MARKET)
        val out = Editor.tap(s, lower, Point(0.5f, 0.5f), null)
        assertNull(out.effect)
        assertEquals(Point(0.5f, 0.5f), out.state.movingTo)
        assertNull(out.state.region)
        s = Editor.endMovePin(out.state)
        assertNull(s.movingPin)
    }

    @Test
    fun `a chosen vertex is placed by the next tap as one commit`() {
        var s = Editor.select(EditorState(), K.REGION_LOWER_DISTRICT)
        s = Editor.chooseVertex(s, 2)
        assertEquals(2, s.vertex)
        val out = Editor.tap(s, lower, Point(0.6f, 0.65f), null)
        assertEquals(EditorCommit.MoveVertex(K.REGION_LOWER_DISTRICT, 2, Point(0.6f, 0.65f)), out.commit)
        assertNull(out.state.vertex)
        assertNull(Editor.chooseVertex(s, 2).vertex)
    }

    @Test
    fun `a chosen curve handle is placed by the next tap`() {
        val s = Editor.chooseHandle(Editor.select(EditorState(), K.mapLabel(2)), 1)
        val out = Editor.tap(s, lower, Point(0.3f, 0.5f), null)
        assertEquals(EditorCommit.MoveCurveHandle(K.mapLabel(2), 1, Point(0.3f, 0.5f)), out.commit)
    }

    @Test
    fun `changing tool ends picks and moves but keeps drafts, 레이어 opens its tab`() {
        var s = Editor.chooseTool(EditorState(), EditTool.REGION)
        s = Editor.tap(s, lower, Point(0.1f, 0.1f), null).state
        s = Editor.startMovePin(s, K.PIN_ASH_MARKET)
        val layer = Editor.chooseTool(s, EditTool.LAYER)
        assertNull(layer.movingPin)
        assertEquals(1, Editor.vertexCount(layer))
        assertEquals(1, layer.panelTab)
        assertEquals(0, Editor.chooseTool(layer, EditTool.MOVE).panelTab)
    }

    @Test
    fun `the editor state survives encoding (fold, process death)`() {
        var s = Editor.chooseTool(EditorState(), EditTool.ROUTE)
        s = Editor.tap(s, lower, Point(0.2f, 0.4f), null).state
        s = Editor.editRegionDraft(s) { it.copy(name = "구역", factionKey = K.FACTION_UPPER_GUARD, vertices = listOf(Point(0.1f, 0.2f))) }
        assertEquals(s, EditorState.decode(s.encode()))
        assertEquals(EditorState(), EditorState.decode("not json"))
        assertEquals(EditorState(), EditorState.decode(null))
    }

    @Test
    fun `a finished route draft becomes a route through MapEdits`() {
        var s = Editor.chooseTool(EditorState(), EditTool.ROUTE)
        s = Editor.tap(s, lower, Point(0.2f, 0.4f), null).state
        s = Editor.tap(s, lower, Point(0.45f, 0.40f), null).state
        val made = MapEdits.addRoute(doc, K.MAP_LOWER_DISTRICT, s.routeDraft.points, MapFx.ctx()) as MapEdit.Applied
        assertEquals(listOf(RoutePoint.pin(K.PIN_SOCIETY_GATE), RoutePoint.pin(K.PIN_ASH_MARKET)), made.value.points)
    }
}
