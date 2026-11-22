package io.github.lenticualrstone.civilight.feature.map.scale

import io.github.lenticualrstone.civilight.domain.world.map.MapScales
import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScenes
import io.github.lenticualrstone.civilight.feature.map.canvas.SceneMode
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.map.ScaleUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-46 축척 맞추기 (board `MapScale`; spec case WLD-T54). */
class ScaleEditTest {
    private val doc = MapFx.doc()
    private val map = doc.maps.getValue(K.MAP_LOWER_DISTRICT)

    private fun scene(d: ProjectDoc = doc, hiddenOpen: Boolean = false) =
        MapScenes.build(d, d.maps.getValue(K.MAP_LOWER_DISTRICT), LensPoint.Latest, hiddenOpen, SceneMode.VIEW)

    @Test
    fun `WLD-T54 2일 도보 between 결사의 문 and 잿더미 시장 is 120 km per unit`() {
        // Clear the stored scale first, then set it again through the sheet.
        val bare = MapFx.doc().let { d -> d.copy(maps = d.maps + (map.key to map.copy(scale = null))) }
        val bareMap = bare.maps.getValue(map.key)
        var draft = ScaleDraft.of(bare, bareMap)
        assertNull(draft.a)
        draft = ScaleEdit.tap(draft, scene(bare), Point(0.201f, 0.401f))
        draft = ScaleEdit.tap(draft, scene(bare), Point(0.449f, 0.399f))
        assertEquals(Point(0.20f, 0.40f), draft.a)
        assertEquals(Point(0.45f, 0.40f), draft.b)
        draft = draft.copy(value = "2")
        val ui = ScaleEdit.ui(bare, scene(bare), draft)
        assertEquals("결사의 문", ui.aLabel)
        assertEquals("잿더미 시장", ui.bLabel)
        assertEquals("도보 15 km", ui.dayTravel)
        assertTrue(ui.canSave)
        val saved = ScaleEdit.save(bare, map.key, draft, MapFx.ctx())!!
        val perUnit = MapScales.kmPerUnit(saved, saved.maps.getValue(map.key))!!
        assertEquals(120.0, perUnit, 1e-4)
        assertEquals(38.0, perUnit * 0.316667, 0.01)
    }

    @Test
    fun `the sheet opens with the map's own scale`() {
        val draft = ScaleDraft.of(doc, map)
        assertEquals("2", draft.value)
        assertEquals(ScaleUnit.DAY, draft.unit)
        assertNotNull(draft.a)
        assertEquals(1, draft.active)
    }

    @Test
    fun `a row chooses which point the next tap places`() {
        var draft = ScaleDraft.of(doc, map)
        draft = ScaleEdit.choose(draft, 0)
        draft = ScaleEdit.tap(draft, scene(), Point(0.6f, 0.6f))
        assertEquals(Point(0.6f, 0.6f), draft.a)
        assertEquals(1, draft.active)
        assertEquals("0.60, 0.60", ScaleEdit.ui(doc, scene(), draft).aLabel)
    }

    @Test
    fun `nothing is saved without two points and a number`() {
        val draft = ScaleDraft(a = Point(0.2f, 0.4f), value = "2")
        assertFalse(ScaleEdit.canSave(doc, draft))
        assertNull(ScaleEdit.save(doc, map.key, draft, MapFx.ctx()))
        val same = ScaleDraft(a = Point(0.2f, 0.4f), b = Point(0.2f, 0.4f), value = "2")
        assertFalse(ScaleEdit.canSave(doc, same))
        val noNumber = ScaleDraft(a = Point(0.2f, 0.4f), b = Point(0.4f, 0.4f), value = "두")
        assertFalse(ScaleEdit.canSave(doc, noNumber))
    }

    @Test
    fun `km units keep no travel mode`() {
        val draft = ScaleEdit.unit(ScaleDraft(a = Point(0.2f, 0.4f), b = Point(0.45f, 0.4f), value = "30"), 1)
        assertEquals(ScaleUnit.KM, draft.unit)
        assertNull(ScaleEdit.ui(doc, scene(), draft).dayTravel)
        val saved = ScaleEdit.save(doc, map.key, draft, MapFx.ctx())!!
        val scale = saved.maps.getValue(map.key).scale!!
        assertNull(scale.travelModeKey)
        assertEquals(120.0, MapScales.kmPerUnit(saved, saved.maps.getValue(map.key))!!, 1e-4)
    }

    @Test
    fun `rule 19 a hidden place's pin is neither snapped to nor named while locked`() {
        val hidden = doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == K.PLACE_ASH_MARKET) it.copy(hidden = true) else it }))
        val tapAt = Point(0.449f, 0.401f)
        val locked = ScaleEdit.tap(ScaleDraft(active = 1), scene(hidden), tapAt)
        assertEquals(tapAt, locked.b)
        assertEquals("0.45, 0.40", ScaleEdit.ui(hidden, scene(hidden), locked).bLabel)
        val open = ScaleEdit.tap(ScaleDraft(active = 1), scene(hidden, hiddenOpen = true), tapAt)
        assertEquals(Point(0.45f, 0.40f), open.b)
        assertEquals("잿더미 시장", ScaleEdit.ui(hidden, scene(hidden, hiddenOpen = true), open).bLabel)
    }

    @Test
    fun `a draft survives as saved JSON`() {
        val draft = ScaleDraft(a = Point(0.1f, 0.2f), b = null, active = 1, value = "12.5", unit = ScaleUnit.LI, modeIndex = 2)
        assertEquals(draft, ScaleDraft.decode(draft.encode()))
        assertNull(ScaleDraft.decode("{broken"))
    }

    @Test
    fun `the scale bar stands for 1 or 2 days by zoom`() {
        // 120 km per unit, 도보 15 km a day: one day is 1/8 of the base width.
        assertEquals(1, ScaleBars.days(120.0, 15.0, contentWidthPx = 800f, barPx = 96f))
        assertEquals(2, ScaleBars.days(120.0, 15.0, contentWidthPx = 400f, barPx = 96f))
        assertEquals(1, ScaleBars.days(0.0, 15.0, 400f, 96f))
        assertEquals("1일 · 15 km", ScaleBars.label(doc, map, 800f, 96f))
        assertEquals("2일 · 30 km", ScaleBars.label(doc, map, 400f, 96f))
    }
}
