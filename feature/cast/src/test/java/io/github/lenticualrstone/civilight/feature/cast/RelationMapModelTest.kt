package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.map.MapPositions
import io.github.lenticualrstone.civilight.domain.cast.map.MapSlotFilter
import io.github.lenticualrstone.civilight.domain.cast.relation.FactionRelations
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.domain.cast.relation.SheetSlot
import io.github.lenticualrstone.civilight.domain.cast.relation.SlotDraft
import io.github.lenticualrstone.civilight.feature.cast.CastFx.atLens
import io.github.lenticualrstone.civilight.feature.cast.common.toAction
import io.github.lenticualrstone.civilight.feature.cast.field.EditMode
import io.github.lenticualrstone.civilight.feature.cast.field.SaveChoice
import io.github.lenticualrstone.civilight.feature.cast.list.CastListModel
import io.github.lenticualrstone.civilight.feature.cast.list.CastListUi
import io.github.lenticualrstone.civilight.feature.cast.map.MapUi
import io.github.lenticualrstone.civilight.feature.cast.map.NodeTap
import io.github.lenticualrstone.civilight.feature.cast.map.RelationMapModel
import io.github.lenticualrstone.civilight.feature.cast.relation.RelationSave
import io.github.lenticualrstone.civilight.feature.cast.relation.RelationSheetModel
import io.github.lenticualrstone.civilight.feature.cast.trash.ItemTrash
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.ui.graph.LineStroke
import io.github.lenticualrstone.civilight.ui.graph.Pt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** The relationship map (CST-33, CST-34), 호칭 (CST-31), field search (CST-27) and faction relations (CST-36). */
class RelationMapModelTest {
    private val doc = CastFx.doc().atLens(LensPoint.UnitEnd(K.EPISODE_1_7))
    private val ctx = CastFx.ctx()

    @Test
    fun `TC-13-14 the scene draws solid 사이 and dashed mind lines with their labels`() {
        val state = RelationMapModel.build(doc, MapUi(), hiddenUnlocked = false, inner = false)
        val rienHart = RelationReader.between(doc, K.RIEN, K.HART)!!.key
        val line = state.scene.lines.single { it.key == rienHart }
        assertEquals(LineStroke.DASHED, line.stroke)
        assertEquals(setOf("경계", "보호"), line.ends.map { it.word }.toSet())
        val rienOwen = state.scene.lines.single { it.key == RelationReader.between(doc, K.RIEN, K.OWEN)!!.key }
        assertEquals(LineStroke.SOLID, rienOwen.stroke)
        assertEquals("거래 상대", rienOwen.middle!!.fromSide)
        assertEquals(state.view.nodes.map { it.key }, state.scene.nodes.map { it.key })
        assertEquals("관계도", state.title)
        assertEquals(state.view.header, state.smallLine)
        // `사이` filter drops the minds-only line.
        val between = RelationMapModel.build(doc, MapUi(slots = MapSlotFilter.BETWEEN), hiddenUnlocked = false, inner = false)
        assertTrue(between.scene.lines.none { it.key == rienHart })
        assertTrue(between.filtered)
    }

    @Test
    fun `CST-33 inner header, cover card and chips`() {
        val inner = RelationMapModel.build(doc, MapUi(), hiddenUnlocked = false, inner = true)
        assertEquals("전체 관계도", inner.title)
        assertTrue(inner.smallLine.startsWith("인물 · 관계도로 보기 · "))
        assertEquals("전체", inner.tags.first().label)
        assertTrue(inner.tags.first().selected)
        val rienHart = RelationReader.between(doc, K.RIEN, K.HART)!!.key
        val cover = RelationMapModel.build(doc, MapUi(selectedRelation = rienHart), hiddenUnlocked = false, inner = false)
        val card = cover.selected!!
        assertEquals("리엔 · 하르트", card.title)
        assertTrue(card.steps.none { it.second.contains("지금 시점보다 뒤") })
        assertEquals("인물 기록" to "관계 고치기", card.recordLabel to card.editLabel)
    }

    @Test
    fun `CST-34 the line tool picks two ends, a plain tap opens the record`() {
        val (_, tap) = RelationMapModel.tapNode(MapUi(), K.RIEN)
        assertEquals(NodeTap.OpenRecord(K.RIEN), tap)
        val tool = RelationMapModel.toggleLineTool(MapUi())
        val (first, started) = RelationMapModel.tapNode(tool, K.SERA)
        assertEquals(NodeTap.LineStarted, started)
        assertEquals(K.SERA, first.selectedNode)
        val (second, draw) = RelationMapModel.tapNode(first, K.MARTHA)
        assertEquals(NodeTap.DrawLine(K.SERA, K.MARTHA), draw)
        assertNull(second.selectedNode)
        val line = RelationMapModel.tapLine(MapUi(), "R")
        assertEquals("R", line.selectedRelation)
        assertNull(RelationMapModel.tapLine(line, "R").selectedRelation)
    }

    @Test
    fun `CST-33 a dragged node is saved as its centre and wins over the layout`() {
        val state = RelationMapModel.build(doc, MapUi(), hiddenUnlocked = false, inner = false)
        val node = state.scene.nodes.first()
        val point = RelationMapModel.normalized(Pt(362f, 372.5f))
        assertEquals(0.5f, point.x, 0.001f)
        assertEquals(0.5f, point.y, 0.001f)
        val saved = MapPositions.place(doc, node.key, point, ctx)
        val after = RelationMapModel.build(saved, MapUi(), hiddenUnlocked = false, inner = false)
        assertEquals(Pt(362f, 372.5f), after.scene.nodes.single { it.key == node.key }.topLeft)
        val moved = RelationMapModel.build(doc, MapUi(), hiddenUnlocked = false, inner = false, moved = mapOf(node.key to Pt(1f, 2f)))
        assertEquals(Pt(1f, 2f), moved.scene.nodes.single { it.key == node.key }.topLeft)
    }

    @Test
    fun `CST-33 saving a drag keeps every other node where it was`() {
        val before = RelationMapModel.build(doc, MapUi(), hiddenUnlocked = false, inner = false)
        val dragged = before.scene.nodes.first().key
        val all = RelationMapModel.positionsToSave(before.scene, mapOf(dragged to Pt(40f, 60f)))
        assertEquals(before.scene.nodes.map { it.key }.toSet(), all.keys)
        val saved = all.entries.fold(doc) { acc, (key, point) -> MapPositions.place(acc, key, point, ctx) }
        val after = RelationMapModel.build(saved, MapUi(), hiddenUnlocked = false, inner = false)
        for (n in before.scene.nodes) {
            val now = after.scene.nodes.single { it.key == n.key }.topLeft
            val expected = if (n.key == dragged) Pt(40f, 60f) else n.topLeft
            assertEquals(expected.x, now.x, 0.05f)
            assertEquals(expected.y, now.y, 0.05f)
        }
    }

    @Test
    fun `a cover line tap always selects, the inner aside toggles`() {
        val selected = MapUi(selectedRelation = "R")
        assertEquals("R", RelationMapModel.tapLine(selected, "R", inner = false).selectedRelation)
        assertNull(RelationMapModel.tapLine(selected, "R", inner = true).selectedRelation)
    }

    @Test
    fun `TC-13-18 a 호칭 chip edits the one 호칭 value with the timed buttons`() {
        val latest = CastFx.doc()
        val view = RelationSheetModel.view(latest, K.HART, K.RIEN, SheetSlot.ADDRESS, null, LensPoint.Latest)!!
        val save = view.save as RelationSave.Edit
        assertTrue(save.mode is EditMode.TwoButtons)
        val draft = SlotDraft(address = "꼬맹이")
        assertTrue(RelationSheetModel.canSave(latest, K.HART, K.RIEN, SheetSlot.ADDRESS, draft, LensPoint.Latest))
        val saved = RelationSheetModel.save(latest, K.HART, K.RIEN, SheetSlot.ADDRESS, draft, SaveChoice.CORRECT, null, LensPoint.Latest, ctx)
        assertEquals("꼬맹이", RelationViews.view(saved, K.HART, K.RIEN, LensPoint.Latest)!!.ownAddress)
        assertTrue(RelationViews.addressChips(saved, K.HART, LensPoint.Latest, false).any { it.label.contains("꼬맹이") })
    }

    @Test
    fun `TC-13-09 a 호칭 for a partner without a relation creates an empty one with no map line`() {
        val latest = CastFx.doc()
        assertNull(RelationReader.between(latest, K.SERA, K.MARTHA))
        val draft = SlotDraft(address = "결사장님")
        val start = RelationSheetModel.defaultStart(latest, LensPoint.Latest)
        val saved = RelationSheetModel.save(latest, K.SERA, K.MARTHA, SheetSlot.ADDRESS, draft, null, start, LensPoint.Latest, ctx)
        val relation = RelationReader.between(saved, K.SERA, K.MARTHA)!!
        assertNull(relation.between)
        val map = RelationMapModel.build(saved, MapUi(), hiddenUnlocked = false, inner = false)
        assertTrue(map.scene.lines.none { it.key == relation.key })
        assertTrue(RelationViews.addressChips(saved, K.SERA, LensPoint.Latest, false).any { it.label == "마르타 → 결사장님" })
    }

    @Test
    fun `TC-F-19 the list search hits field values and names the field`() {
        val ui = CastListUi(query = "성직자", searching = true)
        val state = CastListModel.build(CastFx.doc(), ui, hiddenUnlocked = false, readOnly = false)
        val hits = state.fieldHits
        assertTrue(hits.isNotEmpty())
        val hit = hits.first()
        assertEquals("특기 · 성직자", hit.sentence)
        assertEquals("성직자", hit.sentence.substring(hit.match))
        assertFalse(state.noResults)
        // Blank query: no hits.
        assertTrue(CastListModel.fieldHits(CastFx.doc(), " ", false).isEmpty())
    }

    @Test
    fun `CST-36 faction relations go to the trash with their faction and never reach the map`() {
        val (d, relation) = FactionRelations.openOrCreate(CastFx.doc(), K.FACTION_LANTERN_SOCIETY, K.FACTION_UPPER_GUARD, ctx)
        assertEquals(relation, FactionRelations.openOrCreate(d, K.FACTION_UPPER_GUARD, K.FACTION_LANTERN_SOCIETY, ctx).second)
        assertEquals(listOf(relation.key), FactionRelations.of(d, K.FACTION_UPPER_GUARD).map { it.key })
        assertTrue(RelationMapModel.build(d, MapUi(), hiddenUnlocked = false, inner = false).scene.lines.none { it.key == relation.key })
        val deleted = ItemTrash.delete(d, EntityKind.FACTION, K.FACTION_UPPER_GUARD, ctx.toAction(), null)!!
        assertFalse(deleted.doc.liveness.isLive(deleted.doc.index.relation(relation.key)!!))
        val back = deleted.change.revert(deleted.doc)
        assertNotNull(FactionRelations.of(back, K.FACTION_UPPER_GUARD).singleOrNull())
    }

    @Test
    fun `a node let go outside the layout area settles back in, one inside stays`() {
        val inside = Pt(300f, 200f)
        val kept = RelationMapModel.settled(inside)
        assertEquals(inside.x, kept.x, 0.01f)
        assertEquals(inside.y, kept.y, 0.01f)
        val out = RelationMapModel.settled(Pt(-900f, 5000f))
        val saved = RelationMapModel.normalized(out)
        // The settled place saves at the edge of the area, and a second settle changes nothing.
        assertEquals(0f, saved.x, 0.001f)
        assertEquals(1f, saved.y, 0.001f)
        val again = RelationMapModel.settled(out)
        assertEquals(out.x, again.x, 0.01f)
        assertEquals(out.y, again.y, 0.01f)
    }
}
