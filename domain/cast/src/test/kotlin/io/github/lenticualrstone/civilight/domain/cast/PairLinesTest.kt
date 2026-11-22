package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.cast.map.LineStyle
import io.github.lenticualrstone.civilight.domain.cast.map.MapPositions
import io.github.lenticualrstone.civilight.domain.cast.map.RelationMap
import io.github.lenticualrstone.civilight.domain.cast.relation.LineSlot
import io.github.lenticualrstone.civilight.domain.cast.relation.PairLines
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** CST-33 / CST-34: lines drawn on the full relationship map (no canvas edge) and saved node positions. */
class PairLinesTest {
    private val doc = Fx.doc()

    @Test
    fun `TC-13-15 map variant - a pair line records only through 관계로 만들기`() {
        // 세라 and 마르타 have no relation (TC-13-09).
        assertNull(RelationReader.between(doc, K.SERA, K.MARTHA))
        val sheet = requireNotNull(PairLines.sheet(doc, K.SERA, K.MARTHA, Fx.L1_7))
        assertEquals("선 라벨", sheet.title)
        assertEquals("세라 · 마르타", sheet.sub)
        assertEquals(listOf("사이", "세라의 마음", "마르타의 마음"), sheet.slotChips)
        assertEquals(LineSlot.FROM_MIND, sheet.preselected)
        assertEquals("1-7", sheet.startPoint)
        assertEquals("관계로 만들기", sheet.makeLabel)
        assertNull(PairLines.sheet(doc, K.SERA, K.SERA, Fx.L1_7))

        val wary = PairLines.kindChips(doc, LineSlot.FROM_MIND).single { it.kindKey == K.KIND_WARINESS }
        val made = PairLines.makeRelation(doc, K.SERA, K.MARTHA, LineSlot.FROM_MIND, wary, K.EPISODE_1_7, Fx.ctx())
        val relation = requireNotNull(RelationReader.between(made, K.SERA, K.MARTHA))
        val record = made.changes.records.single { (it.target as? ChangeTarget.RelationSlot)?.relationKey == relation.key }
        assertTrue(record.anchor.sameAs(Anchor.UnitStart(K.EPISODE_1_7)))
        val line = RelationMap.build(made, Fx.L1_7).lines.single { it.relationKey == relation.key }
        assertEquals(LineStyle.DASHED, line.style)
        // Before 1-7 the pair has not met: no line.
        assertTrue(RelationMap.build(made, Fx.lens(K.EPISODE_1_6)).lines.none { it.relationKey == relation.key })
    }

    @Test
    fun `a pair kind chip names who holds the first name`() {
        // `선배` for the drawn-to end: 마르타 is 세라's 선배; 세라 reads `마르타 · 선배`.
        val senior = PairLines.kindChips(doc, LineSlot.BETWEEN).single { it.kindKey == K.KIND_SENIOR_JUNIOR && it.partnerHoldsFirst == true }
        val made = PairLines.makeRelation(doc, K.SERA, K.MARTHA, LineSlot.BETWEEN, senior, K.EPISODE_1_7, Fx.ctx())
        val view = requireNotNull(RelationViews.view(made, K.SERA, K.MARTHA, LensPoint.Latest))
        assertEquals("마르타 · 선배", view.title("마르타"))
        val line = RelationMap.build(made, Fx.L1_7).lines.single { it.relationKey == RelationReader.between(made, K.SERA, K.MARTHA)!!.key }
        assertEquals(LineStyle.SOLID, line.style)
    }

    @Test
    fun `map positions are saved in the cast settings ext and read back`() {
        assertEquals(emptyMap<String, Point>(), MapPositions.saved(doc))
        val placed = MapPositions.place(doc, K.RIEN, Point(0.25f, 0.75f), Fx.ctx())
        val again = MapPositions.place(placed, K.HART, Point(1.4f, -0.2f), Fx.ctx())
        assertEquals(mapOf(K.RIEN to Point(0.25f, 0.75f), K.HART to Point(1f, 0f)), MapPositions.saved(again))
        // Other cast settings stay.
        assertEquals(doc.project.cast.bundleName, again.project.cast.bundleName)
    }
}
