package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** 10-script.md §4.3 · rule 11 (position and block-level time in the script). */
class Rule11PositionTest {
    private fun b(n: Int) = F1.b(n)

    @Test
    fun `T-11-01 reference state per block`() {
        val doc = F1.build()
        val mind = ChangeTarget.RelationSlot(K.REL_RIEN_HART, RelationSlotName.MIND_A)
        assertEquals("만나기 전", Panels.relationStateAt(doc, mind, b(42)))
        assertEquals("만나기 전 → 경계 ◆ 여기서", Panels.relationStateAt(doc, mind, b(43)))
        assertEquals("경계", Panels.relationStateAt(doc, mind, b(44)))
    }

    @Test
    fun `T-11-02 pre-reveal by block order inside one scene`() {
        val doc = F1.build()
            .withBlock(b(42)) { it.copy(runs = it.runs + TextRun.Plain(" ") + F1.lanternLink("01JZ000000HREF000000000042")) }
            .withBlock(b(45)) { it.copy(runs = it.runs + TextRun.Plain(" ") + F1.lanternLink("01JZ000000HREF000000000045")) }
        assertEquals(listOf("01JZ000000HREF000000000042"), Panels.preRevealLinks(doc, doc.block(b(42))))
        assertTrue(Panels.preRevealLinks(doc, doc.block(b(44))).isEmpty())
        assertTrue(Panels.preRevealLinks(doc, doc.block(b(45))).isEmpty())
    }

    @Test
    fun `T-11-03 position line and label`() {
        val doc = F1.build()
        val times = StoryTimes.source(doc)
        val caretScene = SceneText.headerSceneKey(doc, caretOrSelectedBlockKey = b(45), blockUnderTopBarKey = b(50))!!
        assertEquals(F1.S3, caretScene)
        assertEquals("1-7 · 씬 3 · 2일째 새벽", SceneText.positionLine(doc, caretScene, times.timeOf(caretScene)))
        assertEquals("이 씬 · 1-7", SceneText.positionLabel(doc, caretScene))

        val scrolled = SceneText.headerSceneKey(doc, caretOrSelectedBlockKey = null, blockUnderTopBarKey = b(50))!!
        assertEquals("1-7 · 씬 4 · 2일째 아침", SceneText.positionLine(doc, scrolled, times.timeOf(scrolled)))

        assertEquals(LensPoint.UnitEnd(F1.S3), SceneText.lensForScene(caretScene))
        assertEquals("렌즈 · 1-7 씬 3", SceneText.lensToast(doc, caretScene))
    }

    @Test
    fun `position line ends after the ordinal without a time and reads saved while editing`() {
        val doc = F1.build()
        val times = StoryTimes.source(doc)
        assertEquals("1-3 · 씬 1", SceneText.positionLine(doc, K.SCENE_1_3_1, times.timeOf(K.SCENE_1_3_1)))
        assertEquals("1-7 · 씬 3 · 저장됨", SceneText.editingLine(doc, F1.S3))
        assertEquals("1-7 · 씬 3 · 저장 중", SceneText.editingLine(doc, F1.S3, saved = false))
        assertEquals("1-6 · 씬 1 · 1일째 낮", SceneText.positionLine(doc, K.SCENE_1_6_1, times.timeOf(K.SCENE_1_6_1)))
    }
}
