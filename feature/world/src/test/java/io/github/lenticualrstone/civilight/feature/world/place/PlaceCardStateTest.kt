package io.github.lenticualrstone.civilight.feature.world.place

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.item.WorldItems
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.K
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.PROJECT
import io.github.lenticualrstone.civilight.feature.world.common.CardAccess
import io.github.lenticualrstone.civilight.feature.world.common.FieldRows
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.navigation.MapView
import io.github.lenticualrstone.civilight.navigation.PlaceTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The place card (WLD-09) and project fields of world items (WLD-06). */
class PlaceCardStateTest {
    private fun card(placeKey: String, doc: io.github.lenticualrstone.civilight.model.doc.ProjectDoc = WorldTestFixture.stateB(), readOnly: Boolean = false) =
        PlaceCardState.build(doc, PROJECT, placeKey, hiddenOpen = false, readOnly = readOnly)

    @Test
    fun `WLD-09 full card - eyebrow with 지역, tabs, derived rows`() {
        val ui = card(K.PLACE_ARCHIVE_ROOM)
        assertEquals(CardAccess.Open, ui.access)
        assertEquals("기록실", ui.title)
        assertEquals("장소 · 결사의 문", ui.head!!.eyebrow)
        assertEquals(listOf("개요", "소속", "등장"), ui.tabs)
        // No tags on places (2026-11-26); the subtitle is the structure fact.
        assertEquals("상위 지역 결사의 문", ui.subtitle)
        assertEquals("결사의 문", ui.parent!!.value)
        assertTrue(ui.parent!!.editable)
        assertTrue(ui.appearance!!.label == "등장" && !ui.appearance!!.editable)
        assertEquals("배경 에셋", ui.assets!!.label)
    }

    @Test
    fun `WLD-09 a place without 지역 reads 장소 and an empty value`() {
        val ui = card(K.PLACE_LOWER_STREET)
        assertEquals("장소", ui.head!!.eyebrow)
        assertEquals("—", ui.parent!!.value)
        assertFalse(card(K.PLACE_LOWER_STREET, readOnly = true).parent!!.editable)
    }

    @Test
    fun `WLD-09 지도 tab lists the maps holding a pin of the place`() {
        val ui = card(K.PLACE_LOWER_STREET)
        assertTrue(ui.maps.isNotEmpty())
        assertTrue(ui.maps.all { it.target is MapView })
    }

    @Test
    fun `WLD-63 이곳의 시간 rows open the event or the scene`() {
        val ui = card(K.PLACE_LOWER_STREET)
        val rows = ui.timeRows.filterIsInstance<PlaceTimeItemUi.Row>()
        assertTrue(rows.any { it.title == "대정전" && it.target is io.github.lenticualrstone.civilight.navigation.EventDetail })
        assertTrue(rows.any { it.target is io.github.lenticualrstone.civilight.navigation.Script })
    }

    @Test
    fun `WLD-63 and rule 19 a hidden character's name leaves 이곳의 시간 while locked`() {
        val base = WorldTestFixture.stateB()
        val hidden = base.copy(cast = base.cast.copy(characters = base.cast.characters.map { if (it.key == K.RIEN) it.copy(hidden = true) else it }))
        fun subs(open: Boolean) = PlaceCardState.build(hidden, WorldTestFixture.PROJECT, K.PLACE_LOWER_STREET, hiddenOpen = open, readOnly = false)
            .timeRows.filterIsInstance<PlaceTimeItemUi.Row>().map { it.sub }
        assertTrue(subs(false).toString(), subs(false).none { it.contains("리엔") })
        assertTrue(subs(true).toString(), subs(true).any { it.contains("리엔") })
    }

    @Test
    fun `WLD-06 and WLD-12 the 상태 field shows its value at the lens with ◆`() {
        val doc = WorldTestFixture.stateB()
        val place = doc.index.place(K.PLACE_LOWER_STREET)!!
        val section = FieldRows.build(doc, EntityKind.PLACE, K.PLACE_LOWER_STREET, place.fields, LensPoint.Latest)
        val state = section.filled.single { it.fieldDefKey == K.FIELD_PLACE_STATE }
        assertEquals("상태", state.label)
        assertEquals("대정전 이후", state.value)
        assertTrue(state.changed)
        assertTrue(section.empty.all { it.value == null })
    }

    @Test
    fun `WLD-06 empty fields are counted on one line`() {
        val created = WorldItems.createPlace(WorldTestFixture.stateB(), "물레방아", WorldTestFixture.ctx()) as io.github.lenticualrstone.civilight.domain.world.item.CreateResult.Created
        val doc = created.doc
        val defs = doc.index.fieldDefs(EntityKind.PLACE).filter { it.deletedAt == null && !it.definitionHidden }
        val section = FieldRows.build(doc, EntityKind.PLACE, created.key, emptyMap(), LensPoint.Latest)
        assertTrue(section.filled.isEmpty())
        assertEquals(defs.map { it.key }, section.empty.map { it.fieldDefKey })
        assertTrue(defs.any { it.key == K.FIELD_PLACE_STATE })
        assertEquals("빈 항목 ${defs.size} · 보기", section.emptyLine)
        val full = FieldRows.build(doc, EntityKind.PLACE, K.PLACE_LOWER_STREET, doc.index.place(K.PLACE_LOWER_STREET)!!.fields, LensPoint.Latest)
        if (full.empty.isEmpty()) assertNull(full.emptyLine)
    }

    @Test
    fun `지역 choices exclude the place and its descendants`() {
        val doc = WorldTestFixture.stateB()
        val excluded = ParentChoices.excluded(doc, K.PLACE_SOCIETY_GATE)
        assertEquals(setOf(K.PLACE_SOCIETY_GATE, K.PLACE_ARCHIVE_ROOM), excluded)
        val ctx: EditContext = WorldTestFixture.ctx()
        assertNull("a loop changes nothing", WorldItems.setParentPlace(doc, K.PLACE_SOCIETY_GATE, K.PLACE_ARCHIVE_ROOM, ctx))
    }

    @Test
    fun `WLD-13 a hidden place is locked and a hidden 지역 is masked`() {
        val base = WorldTestFixture.stateB()
        val hiddenGate = base.copy(world = base.world.copy(places = base.world.places.map {
            if (it.key == K.PLACE_SOCIETY_GATE) it.copy(hidden = true) else it
        }))
        assertEquals(CardAccess.Locked("숨긴 장소 1"), card(K.PLACE_SOCIETY_GATE, hiddenGate).access)
        val archive = card(K.PLACE_ARCHIVE_ROOM, hiddenGate)
        assertEquals("장소 · 숨긴 항목", archive.head!!.eyebrow)
        assertEquals("숨긴 항목", archive.parent!!.value)
    }

    @Test
    fun `route tabs map onto the card tabs - 이곳의 시간 and 지도 live under 등장`() {
        assertEquals(0, PlaceCardUi.tabIndex(PlaceTab.OVERVIEW))
        assertEquals(2, PlaceCardUi.tabIndex(PlaceTab.TIME_HERE))
        assertEquals(2, PlaceCardUi.tabIndex(PlaceTab.MAP))
    }
}
