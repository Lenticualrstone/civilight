package io.github.lenticualrstone.civilight.feature.map.text

import io.github.lenticualrstone.civilight.domain.world.gen.TerrainGenerator
import io.github.lenticualrstone.civilight.feature.map.MapFx
import io.github.lenticualrstone.civilight.feature.map.MapFx.K
import io.github.lenticualrstone.civilight.model.map.MapFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 글로 설명 (WLD-61): reading, names only from project records, the result header. */
class TextMapFlowTest {
    private val doc = MapFx.doc()
    private val text = "북쪽은 눈 덮인 산맥이 가로막고, 남쪽 해안에 항구 도시가 있다. 동쪽은 메마른 사막, 서쪽 숲속에 결사의 문."

    @Test
    fun `WLD-61 the board's text reads into four conditions, the place linked by name`() {
        val s = TextMapFlow.read(doc, TextMapState(text = text), MapFx.ctx())
        assertEquals(listOf("산맥", "항구 도시", "사막", "숲"), s.conditions.map { it.what })
        assertTrue(s.conditions.all { it.fromText })
        val rows = TextMapFlow.inputRows(doc, s)
        assertEquals("북쪽 · 눈 덮임", rows[0].small)
        assertEquals("서쪽 · 장소 ‘결사의 문’", rows[3].small)
        assertEquals(K.PLACE_SOCIETY_GATE, s.conditions[3].placeKey)
        assertEquals("읽어 낸 조건 · 4 | 키워드", TextMapFlow.readLine(s))
        assertFalse(s.unreadable)
    }

    @Test
    fun `WLD-T63 names never invented, the result header counts what was met`() {
        val s = TextMapFlow.read(doc, TextMapState(text = "남쪽 해안에 항구 도시 은빛 항구가 있다."), MapFx.ctx())
        assertNull(s.conditions.single().placeKey)
        val made = TextMapFlow.make(TextMapFlow.read(doc, TextMapState(text = text), MapFx.ctx()), { 1234 })
        assertEquals(1, made.page)
        val world = TerrainGenerator.generate(TextMapFlow.session(made).input())
        val header = TextMapFlow.header(made, world)
        assertTrue(header, Regex("조건 4 · [0-4] 반영").matches(header))
        val rows = TextMapFlow.resultRows(doc, made, world)
        assertEquals("숲 · 결사의 문", rows[3].name)
        rows.filter { it.applied == false }.forEach { assertTrue(it.small.endsWith("→ 위치 고르기")) }
        assertEquals(MapFormat.DETAIL, TextMapFlow.session(made).format)
    }

    @Test
    fun `the author's conditions survive a new reading, the cloud stub reads nothing`() {
        var s = TextMapFlow.read(doc, TextMapState(text = text), MapFx.ctx())
        s = TextMapFlow.upsert(s, CondState("own", "호수", listOf(4)))
        s = TextMapFlow.read(doc, s.copy(text = "동쪽은 메마른 사막."), MapFx.ctx())
        assertEquals(listOf("호수", "사막"), s.conditions.map { it.what })
        val cloud = TextMapFlow.read(doc, s.copy(engine = 1), MapFx.ctx())
        assertTrue(cloud.unreadable)
        assertEquals(s.conditions, cloud.conditions)
        // An edited read condition becomes the author's own.
        val edited = TextMapFlow.upsert(s, s.conditions.last().copy(traits = listOf("넓음")))
        assertTrue(edited.conditions.none { it.fromText })
        assertEquals(listOf("호수"), TextMapFlow.remove(edited, edited.conditions.last().key).conditions.map { it.what })
    }

    @Test
    fun `위치, highlight, candidates and the read-along sources`() {
        var s = TextMapFlow.read(doc, TextMapState(text = text), MapFx.ctx())
        val key = s.conditions[0].key
        s = TextMapFlow.setPosition(s, key, setOf(4, 1))
        assertEquals(listOf(1, 4), s.conditions[0].userPosition)
        assertEquals(1, TextMapFlow.highlight(s, 1).highlight)
        assertNull(TextMapFlow.highlight(TextMapFlow.highlight(s, 1), 1).highlight)
        var n = 0
        val c = TextMapFlow.otherCandidates(TextMapFlow.make(s) { n++ }) { n++ }
        assertEquals(3, c.candidates.size)
        assertEquals(c.candidates[2], TextMapFlow.chooseCandidate(c, 2).seeds)
        val sources = TextMapFlow.sources(doc, s)
        assertTrue(sources.first().label.startsWith("장소 "))
        val off = TextMapFlow.toggleSource(s, TextMapState.SOURCE_PLACES)
        assertFalse(TextMapFlow.sources(doc, off).first().on)
        // Without place names the 결사의 문 link is gone.
        assertNull(TextMapFlow.read(doc, off, MapFx.ctx()).conditions.last().placeKey)
        assertEquals(s, TextMapState.decode(s.encode()))
    }
}
