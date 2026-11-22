package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Script asset usage (SCR-61) and character counts (SCR-35, Q-16 default). */
class AssetsAndCountsTest {
    private val ctx = testCtx()
    private val doc = F1.build()
    private fun usage(key: String) = ScriptAssets.usage(doc).single { it.asset.key == key }

    @Test
    fun `usage counts, first episode and unused assets`() {
        val gate = usage(K.ASSET_BG_SOCIETY_GATE_MORNING)
        assertEquals(2, gate.total)
        assertEquals(mapOf(K.EPISODE_1_4 to 1, K.EPISODE_1_7 to 1), gate.perEpisode)
        assertEquals("1-4", gate.firstEpisodeCode)
        assertEquals("bg_society_gate_morning · 이 에피소드 1회", ScriptAssets.subline(gate, K.EPISODE_1_7))
        assertEquals("bg_society_gate_morning · 2회", ScriptAssets.subline(gate))
        assertEquals(listOf(K.ASSET_BGM_UPPER_BELL), ScriptAssets.unused(doc).map { it.key })
        assertEquals(0, usage(K.ASSET_BGM_UPPER_BELL).total)
        assertNull(ScriptAssets.trailing(usage(K.ASSET_BGM_UPPER_BELL)))
    }

    @Test
    fun `sublines, sections and the picker`() {
        assertEquals("bgm_embers · 2분 34초 · 반복", ScriptAssets.subline(usage(K.ASSET_BGM_EMBERS)))
        assertEquals("sfx_wind · 4.2초", ScriptAssets.subline(usage(K.ASSET_SFX_WIND)))
        val cg = usage(K.ASSET_CG_FLICKER)
        assertEquals("cg_flicker · 아직 이미지 없음", ScriptAssets.subline(cg))
        assertEquals("비어 있음", ScriptAssets.trailing(cg))
        assertEquals(listOf("배경 · 3", "BGM · 2", "효과음 · 1", "CG · 1"), ScriptAssets.sections(doc))
        assertEquals(listOf("전체", "배경", "BGM", "효과음", "CG", "안 쓰는 것"), ScriptAssets.FILTERS)
        assertEquals(3, ScriptAssets.forCue(doc, CueKind.BACKGROUND).size)
        assertTrue(ScriptAssets.forCue(doc, CueKind.SCREEN_EFFECT).isEmpty())
        assertEquals("0.8초", ScriptAssets.duration(800))
        assertEquals("12초", ScriptAssets.duration(12_000))
        assertEquals("2분 41초", ScriptAssets.duration(161_000))
    }

    @Test
    fun `a deleted cue block no longer counts`() {
        val after = BlockTrash.delete(doc, listOf(F1.b(41)), ctx).doc
        assertTrue(K.ASSET_BGM_EMBERS in ScriptAssets.unused(after).map { it.key })
    }

    @Test
    fun `character count excludes memos and cues and counts parens and spaces`() {
        assertEquals("(작게) 저기… 불빛이 보여요. 누가 아직 남아 있는 걸까요?".length, WritingCounts.charCount(doc.block(F1.b(42))))
        assertEquals(0, WritingCounts.charCount(doc.block(F1.b(46))))
        assertEquals(0, WritingCounts.charCount(doc.block(F1.b(41))))
        val f3 = F3.build()
        assertEquals("따라간다".length + "불빛 쪽을 한 번 더 본다".length, WritingCounts.charCount(f3.block(F1.b(45))))
        assertEquals("5,210자", WritingCounts.format(5210))
    }

    @Test
    fun `today counts net growth since the day baseline`() {
        val today = LocalDate.of(2026, 11, 22)
        assertEquals(0, WritingCounts.today(doc, today, ctx.zone))
        val ins = BlockOps.insert(doc, BlockPosition.After(F1.b(48)), BlockKind.NARRATION, ctx, runs = listOf(TextRun.Plain("문이 닫혔다.")))
        assertEquals(7, WritingCounts.today(ins.doc, today, ctx.zone))
        val baseline = WritingCounts.recordBaseline(ins.doc, F1.b(43), today)
        assertEquals(baseline, WritingCounts.recordBaseline(baseline, F1.b(43), today))
        val grown = BlockOps.setRuns(baseline, F1.b(43), baseline.block(F1.b(43)).runs + TextRun.Plain(" 가자."), ctx)
        assertEquals(7 + 4, WritingCounts.today(grown, today, ctx.zone))
        val shrunk = BlockOps.setRuns(baseline, F1.b(43), listOf(TextRun.Plain("멈춰.")), ctx)
        assertEquals(7, WritingCounts.today(shrunk, today, ctx.zone))
        val card = WritingCounts.todayCard(grown, today, ctx.zone)
        assertEquals("11자", card.written)
        assertEquals("목표 2,000자", card.goal)
        assertEquals(11 / 2000f, card.progress, 0.0001f)
    }

    @Test
    fun `this scene's share by speaker`() {
        assertEquals("리엔 3 · 하르트 2 · 세라 1 · 오웬 1 · 나레이션 4", WritingCounts.sceneSpeakers(doc, F1.S3))
        assertEquals(sceneChars(), WritingCounts.sceneCount(doc, F1.S3))
    }

    private fun sceneChars(): Int = doc.manuscript.liveBlocksOf(F1.S3).sumOf(WritingCounts::charCount)
}
