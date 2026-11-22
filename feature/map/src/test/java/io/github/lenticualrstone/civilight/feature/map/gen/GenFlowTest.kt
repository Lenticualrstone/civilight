package io.github.lenticualrstone.civilight.feature.map.gen

import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.model.map.GeneratedMapKind
import io.github.lenticualrstone.civilight.model.map.GenerationStepKind
import io.github.lenticualrstone.civilight.model.map.LandRatio
import io.github.lenticualrstone.civilight.model.map.MapFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-54 지형 만들기 흐름 and WLD-55 단계 조작 · 시드 · 고정 · 다시 (boards `MapFormat`, `MapGen`, `MapGenWide`). */
class GenFlowTest {
    private fun seeds(vararg s: Int): () -> Int { val it = s.iterator(); return { it.next() } }

    private fun start(): GenFlowState = GenFlowState.start(seeds(4827, 112, 31, 907, 215))

    @Test
    fun `양식 shows the shared seed note and the six cards`() {
        val state = start()
        assertEquals(1, state.page)
        val ui = GenFlow.ui(state)
        assertEquals("같은 세계 · 시드 4827 · 양식만 다름 · 시작 전까지 바꾸기 가능", ui.formatNote)
        assertEquals(listOf("육각", "다각형", "상세 지형", "윤곽", "노드", "글로 설명"), FormatCard.entries.map { it.title })
        assertEquals(MapFormat.HEX, GenFlow.pickFormat(state, MapFormat.HEX).format)
        assertEquals(2, GenFlow.toSteps(state).page)
    }

    @Test
    fun `steps show seeds, the header names locked steps`() {
        val state = GenFlow.lock(GenFlow.toSteps(start()), GenerationStepKind.COAST, true)
        val ui = GenFlow.ui(state)
        assertEquals(listOf("해안선", "산", "강", "숲 · 기후", "마을 · 길"), ui.steps.map { it.name })
        assertEquals(listOf("4827", "0112", "0031", "0907", "0215"), ui.steps.map { it.seed })
        assertEquals("상세 지형 · 색면 · 시드 4827 · 해안선 고정", ui.header)
        assertEquals("상세 지형 · 색면 · 지형 만들기 2/2", ui.coverSmall)
        assertTrue(ui.steps.first().locked)
    }

    @Test
    fun `강 card reads 강 · 시드 0031 with its chips and 강만 다시 · 강 고정`() {
        val ui = GenFlow.ui(GenFlow.focus(start(), GenerationStepKind.RIVER))
        assertEquals("강", ui.cardName)
        assertEquals("0031", ui.cardSeed)
        assertEquals("강만 다시", ui.rerollLabel)
        assertEquals("강 고정", ui.lockLabel)
        assertEquals(listOf("개수", "굽이", MapCopy.LAKE), ui.questions.map { it.label })
        assertTrue(ui.questions.last().toggle)
    }

    @Test
    fun `다시 re-seeds one step, a locked step never changes`() {
        val state = start()
        val rerolled = GenFlow.reroll(state, GenerationStepKind.RIVER, 77)
        assertEquals(77, rerolled.session().step(GenerationStepKind.RIVER).seed)
        assertEquals(state.session().step(GenerationStepKind.MOUNTAIN).seed, rerolled.session().step(GenerationStepKind.MOUNTAIN).seed)
        val locked = GenFlow.lock(state, GenerationStepKind.RIVER, true)
        assertSame(locked, GenFlow.reroll(locked, GenerationStepKind.RIVER, 77))
    }

    @Test
    fun `새 후보 adds three candidates to the history, locked steps keep their seed`() {
        var n = 5000
        val state = GenFlow.lock(start(), GenerationStepKind.COAST, true)
        val next = GenFlow.newCandidates(state, { n++ }, now = 1L)
        assertEquals(listOf(0, 1, 2), next.batch)
        assertEquals(0, next.chosen)
        assertEquals(3, next.history.size)
        assertEquals("후보 기록 3", GenFlow.ui(next).historyLabel)
        next.history.forEach { assertEquals(4827, it.steps.first { s -> s.step.known == GenerationStepKind.COAST }.seed) }
        assertEquals(next.history[0].steps, next.steps)
        val again = GenFlow.newCandidates(next, { n++ }, now = 2L)
        assertEquals(listOf(3, 4, 5), again.batch)
        assertEquals(6, again.history.size)
    }

    @Test
    fun `a candidate chosen again is the same world after the chips changed`() {
        var n = 100
        val made = GenFlow.newCandidates(start(), { n++ }, now = 1L)
        val input = GenFlow.candidateInput(made, 1)!!
        val changed = GenFlow.ratio(GenFlow.kind(made, GeneratedMapKind.ISLANDS), LandRatio.LAND_MORE)
        assertNull(changed.chosen)
        assertEquals(input, GenFlow.candidateInput(changed, 1))
        val back = GenFlow.chooseCandidate(changed, 1)
        assertEquals(1, back.chosen)
        assertEquals(made.kind, back.kind)
        assertEquals(made.landRatio, back.landRatio)
        assertEquals(input, back.input())
    }

    @Test
    fun `chip changes clear the chosen candidate`() {
        var n = 100
        val made = GenFlow.newCandidates(start(), { n++ }, now = 1L)
        val chosen = GenFlow.choose(made, GenerationStepKind.RIVER, "count", "많이")
        assertNull(chosen.chosen)
        assertNotEquals(made.steps, chosen.steps)
    }

    @Test
    fun `the flow state survives as JSON and the route format is read by name`() {
        var n = 100
        val state = GenFlow.newCandidates(start(), { n++ }, now = 1L)
        assertEquals(state, GenFlowState.decode(state.encode()))
        assertEquals(MapFormat.HEX, GenFlowState.formatOf("hex"))
        assertNull(GenFlowState.formatOf("square"))
        val direct = GenFlowState.start(seeds(1, 2, 3, 4, 5), MapFormat.POLY)
        assertEquals(2, direct.page)
        assertEquals(MapFormat.POLY, direct.format)
    }
}
