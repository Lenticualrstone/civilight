package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.cast.map.LayoutConfig
import io.github.lenticualrstone.civilight.domain.cast.map.MapFilter
import io.github.lenticualrstone.civilight.domain.cast.map.MapLayout
import io.github.lenticualrstone.civilight.domain.cast.map.NodeBox
import io.github.lenticualrstone.civilight.domain.cast.map.RelationMap
import io.github.lenticualrstone.civilight.domain.cast.map.Vec
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationKinds
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.cast.RelationCategory
import io.github.lenticualrstone.civilight.model.cast.RelationSymmetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Relationship map geometry and the relation kind settings (CST-32, CST-33). */
class MapAndKindsTest {
    private val doc = Fx.doc()

    @Test
    fun `map nodes, header and the selected relation card`() {
        val view = RelationMap.build(doc, Fx.L1_7)
        assertEquals("인물 · 11명 중 4명", view.header)
        assertEquals(listOf(K.RIEN, K.HART, K.SERA, K.OWEN), view.nodes.map { it.key })
        assertEquals("주연", view.nodes.first().subtitle)
        val card = requireNotNull(RelationMap.selected(doc, K.REL_RIEN_HART, Fx.L1_7))
        assertEquals("리엔 · 하르트", card.title)
        // 1-9 is past the 집필 선: `예정`.
        assertEquals(
            listOf("1-7" to "리엔 → 경계 · 하르트 → 보호", "1-9" to "사이 동료 · 리엔 → 신뢰 · 예정"),
            card.steps,
        )
        assertEquals(listOf("인물 기록", "관계 고치기"), listOf(card.recordLabel, card.editLabel))
        // A written step after a past lens is `지금 시점보다 뒤`.
        assertEquals("1-7" to "리엔 → 경계 · 하르트 → 보호 · 지금 시점보다 뒤", RelationMap.selected(doc, K.REL_RIEN_HART, Fx.L1_6)!!.steps[0])
    }

    @Test
    fun `tag group filter keeps lines inside the group`() {
        val society = RelationMap.build(doc, Fx.L1_7, MapFilter(tagKey = K.TAG_LANTERN_SOCIETY))
        assertEquals(listOf(K.REL_RIEN_SERA), society.lines.map { it.relationKey })
        val kinds = RelationMap.build(doc, Fx.L1_7, MapFilter(kindKeys = setOf(K.KIND_WARINESS)))
        assertEquals(listOf(K.REL_RIEN_HART), kinds.lines.map { it.relationKey })
        assertEquals("경계 →", kinds.lines.single().mindTextA)
        assertEquals(null, kinds.lines.single().mindB)
    }

    @Test
    fun `layout is deterministic, keeps saved positions and clips lines to the cards`() {
        val view = RelationMap.build(doc, Fx.L1_7)
        val config = LayoutConfig()
        val a = MapLayout.layout(view, config = config)
        val b = MapLayout.layout(view, config = config)
        assertEquals(a, b)
        for (box in a.nodes.values) {
            assertTrue(box.center.x in config.nodeWidth / 2..config.width - config.nodeWidth / 2)
            assertTrue(box.center.y in config.nodeHeight / 2..config.height - config.nodeHeight / 2)
        }
        // Lines start and end on the card borders.
        for (line in a.lines) {
            val relation = view.lines.single { it.relationKey == line.relationKey }
            val from = a.nodes.getValue(relation.endA)
            val to = a.nodes.getValue(relation.endB)
            assertTrue(onBorder(from, line.start))
            assertTrue(onBorder(to, line.end))
            assertTrue(abs(line.mid.x - (line.start.x + line.end.x) / 2) < 1e-6)
        }
        val saved = mapOf(K.RIEN to Point(0.1f, 0.2f))
        val pinned = MapLayout.layout(view, saved, config)
        assertEquals(Vec(0.1f * 800.0, 0.2f * 800.0), pinned.nodes.getValue(K.RIEN).center)
        val back = pinned.normalized().getValue(K.RIEN)
        assertEquals(0.1f, back.x, 1e-4f)
        assertEquals(0.2f, back.y, 1e-4f)
    }

    @Test
    fun `edge points and the arrow side`() {
        val left = NodeBox(K.RIEN, Vec(100.0, 100.0), 100.0, 50.0)
        val right = NodeBox(K.HART, Vec(400.0, 100.0), 100.0, 50.0)
        assertEquals(Vec(150.0, 100.0), left.edgePoint(right.center))
        assertEquals(Vec(350.0, 100.0), right.edgePoint(left.center))
        val g = MapLayout.geometry(K.REL_RIEN_HART, left, right)
        assertTrue(g.aIsLeft)
        assertTrue(g.nearA.x < g.mid.x && g.mid.x < g.nearB.x)
        assertTrue(!MapLayout.geometry(K.REL_RIEN_HART, right, left).aIsLeft)
    }

    private fun onBorder(box: NodeBox, p: Vec): Boolean {
        val dx = abs(p.x - box.center.x)
        val dy = abs(p.y - box.center.y)
        val eps = 1e-6
        val inside = dx <= box.width / 2 + eps && dy <= box.height / 2 + eps
        val touches = abs(dx - box.width / 2) < eps || abs(dy - box.height / 2) < eps
        return inside && touches
    }

    @Test
    fun `relation kind settings screen`() {
        val s = RelationKinds.settings(doc)
        assertEquals("사이 · 11", s.betweenTitle)
        assertEquals("둘에게 같은 사실", s.betweenNote)
        assertEquals("대칭 · 7", s.symmetricTitle)
        assertEquals("짝 · 4", s.pairTitle)
        assertEquals("한쪽 적으면 반대쪽도", s.pairNote)
        assertEquals(listOf("부모 ↔ 자식", "스승 ↔ 제자", "상관 ↔ 부하", "선배 ↔ 후배"), s.pairs.map { it.title })
        assertEquals("쓰임 1", s.pairs.last().usageLabel)
        assertEquals("마음 · 11", s.mindTitle)
        assertEquals("한쪽만 · 반대쪽은 비워 둠", s.mindNote)
        assertEquals("사이 11 · 마음 11", RelationKinds.summary(doc))
        assertEquals(1, RelationKinds.usage(doc, K.KIND_WARINESS))
        // 오웬 → 노아 보호 (N2) is on a relation that is not live while 노아 is in the trash.
        assertEquals(1, RelationKinds.usage(doc, K.KIND_PROTECTION))
        assertEquals(2, RelationKinds.usage(Fx.noahLive(doc), K.KIND_PROTECTION))
        assertEquals("+ 새 종류", RelationKinds.chips(doc, RelationCategory.MIND).last())
    }

    @Test
    fun `new kinds and the default seed list`() {
        val ctx = Fx.ctx()
        val (d, pair) = RelationKinds.add(doc, RelationCategory.BETWEEN, "보호자", ctx, RelationSymmetry.PAIR, "피보호자")
        assertEquals("피보호자", pair.pairName)
        assertTrue("보호자 ↔ 피보호자" in RelationKinds.settings(d).pairs.map { it.title })
        val failed = runCatching { RelationKinds.add(doc, RelationCategory.BETWEEN, "주인", ctx, RelationSymmetry.PAIR, null) }
        assertTrue(failed.isFailure)
        val (m, mind) = RelationKinds.add(doc, RelationCategory.MIND, "연모", ctx)
        assertEquals(null, mind.symmetry)
        assertEquals(listOf("연모", "+ 새 종류"), RelationKinds.chips(m, RelationCategory.MIND).takeLast(2))
        val seed = RelationKinds.defaults(ctx)
        assertEquals(22, seed.size)
        assertEquals(doc.project.relationKinds.map { it.name to it.pairName }, seed.map { it.name to it.pairName })
    }
}
