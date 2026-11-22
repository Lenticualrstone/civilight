package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.Fx.S10
import io.github.lenticualrstone.civilight.domain.story.Fx.S11
import io.github.lenticualrstone.civilight.domain.story.Fx.S3
import io.github.lenticualrstone.civilight.domain.story.Fx.S4
import io.github.lenticualrstone.civilight.domain.story.Fx.S5
import io.github.lenticualrstone.civilight.domain.story.Fx.S8
import io.github.lenticualrstone.civilight.domain.story.Fx.S9
import io.github.lenticualrstone.civilight.domain.story.Fx.assertOtherAbsolutesUnchanged
import io.github.lenticualrstone.civilight.domain.story.Fx.clock
import io.github.lenticualrstone.civilight.domain.story.Fx.rel
import io.github.lenticualrstone.civilight.domain.story.Fx.unit
import io.github.lenticualrstone.civilight.domain.story.Fx.withTime
import io.github.lenticualrstone.civilight.domain.story.Fx.withUnit
import io.github.lenticualrstone.civilight.domain.story.scene.MergeOptions
import io.github.lenticualrstone.civilight.domain.story.scene.MergePlan
import io.github.lenticualrstone.civilight.domain.story.scene.MergeSide
import io.github.lenticualrstone.civilight.domain.story.scene.NewScene
import io.github.lenticualrstone.civilight.domain.story.scene.SceneMerge
import io.github.lenticualrstone.civilight.domain.story.scene.SceneMove
import io.github.lenticualrstone.civilight.domain.story.scene.SceneSplit
import io.github.lenticualrstone.civilight.domain.story.scene.TransitionOnMerge
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.FlashbackMode
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.text.plainText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 20-story-time §4.9 SPL / MRG, TIME-12 (new scene after a flashback) and the rule-22 time parts of 10-script T-22. */
class SceneOpsTest {
    private val doc = Fx.doc()
    private val ctx = Fx.ctx()

    private fun ProjectDoc.blockNumbersOf(scene: String) = manuscript.liveBlocksOf(scene).map { it.number }

    @Test
    fun time12_newSceneAfterFlashbackSkipsIt() {
        val prefill = NewScene.prefillAfter(doc, S5)
        assertEquals(doc.unit(S5).place, prefill.place)
        assertEquals(rel(S4, 0, Band.MORNING), prefill.time)
        assertEquals("때 · 1-3 씬 2 기준 · 같은 때", prefill.timeLine)
        val (after, n) = NewScene.create(doc, ExampleKeys.EPISODE_1_4, S5, ctx, place = prefill.place, time = prefill.time)
        assertFalse(after.unit(n).time is SceneTime.Flashback)
        val labels = Fx.labels(after)
        assertEquals("1-4 · 1일째 · 아침", labels.full(n))
        assertEquals("이야기 3일째", labels.story(n))
        assertOtherAbsolutesUnchanged(doc, after, setOf(n))
    }

    @Test
    fun spl01_splitBelow044() {
        val plan = SceneSplit.plan(doc, S9, ExampleKeys.BLOCK_044, ctx)
        val after = plan.result
        val b = after.unit(plan.newSceneKey)
        assertEquals(doc.unit(S9).place, b.place)
        assertEquals(SceneTime.Relative(baseKey = S9, dayMin = 0, dayMax = 0, clock = clock(5, 40)), b.time)
        assertEquals(TransitionMode.NONE, b.transitionModeOrNone)
        assertNull(b.title)
        assertEquals("1-7 · 2일째 · 새벽", Fx.labels(after).full(plan.newSceneKey))
        assertEquals((36..44).toList(), after.blockNumbersOf(S9))
        assertEquals((45..48).toList(), after.blockNumbersOf(plan.newSceneKey))
        assertEquals("씬 5", Fx.labels(after).codes.sceneLabel(S10))
        assertEquals(S9, (after.unit(S10).time as SceneTime.Relative).baseKey)
        assertEquals("2일째 아침 06:10", Fx.labels(after).head(S10))
        assertEquals(Anchor.AtBlock(ExampleKeys.BLOCK_044), after.changes.records.single { it.key == ExampleKeys.R9 }.anchor)
        assertEquals(S9, after.index.block(ExampleKeys.BLOCK_044)!!.sceneKey)
        assertOtherAbsolutesUnchanged(doc, after, emptySet())
        // Sheet values.
        assertEquals("1-7 씬 3 · #044 아래", plan.sub)
        assertEquals("씬 3 · 블록 9개", plan.firstPart)
        assertEquals("새 씬 4 · 블록 4개", plan.newPart)
        assertEquals("장소 · 결사의 문 · 씬 3과 같음", plan.placeLine)
        assertEquals("때 · 씬 3 기준 · 같은 때", plan.timeLine)
        assertEquals("제목 · 미정", plan.titleLine)
        assertEquals(listOf(ExampleKeys.MARTHA), plan.silentCast)
        assertTrue("순번 · 씬 4 · 미정 → 씬 5" in plan.changeRows)
        assertTrue("씬 5 때 · 기준 씬 3 · 2일째 아침 06:10 그대로" in plan.changeRows)
        assertEquals(listOf(ExampleKeys.MARTHA), b.silentCast)
    }

    @Test
    fun spl02_inheritance() {
        val untimed = SceneSplit.plan(doc, S3, ExampleKeys.block(1, 3, 1), ctx)
        assertEquals(SceneTime.None, untimed.result.unit(untimed.newSceneKey).time)
        assertNull(untimed.timeLine)

        val flash = SceneSplit.plan(doc, S5, ExampleKeys.BLOCK_B41, ctx)
        assertTrue(flash.flashbackRow)
        assertEquals(doc.unit(S5).time, flash.result.unit(flash.newSceneKey).time)

        val hiddenDoc = doc.withUnit(S9) { it.copy(hidden = true) }
        val hidden = SceneSplit.plan(hiddenDoc, S9, ExampleKeys.BLOCK_044, ctx)
        assertTrue(hidden.hiddenRow)
        assertTrue(hidden.result.unit(hidden.newSceneKey).hidden)

        // A fixed point: B is `A 기준 · 같은 때` and A stays the only fixed point.
        val fixed = doc.withTime(S9, SceneTime.Direct(storyDay = 13, clock = clock(5, 40)))
        val split = SceneSplit.plan(fixed, S9, ExampleKeys.BLOCK_044, ctx)
        assertEquals(S9, (split.result.unit(split.newSceneKey).time as SceneTime.Relative).baseKey)
        // A stays the first timed scene of its episode.
        val first = SceneSplit.plan(doc, Fx.S7, ExampleKeys.block(1, 7, 8), ctx)
        assertEquals("1-7 · 1일째 · 새벽", Fx.labels(first.result).full(Fx.S7))
    }

    private fun ready(plan: MergePlan) = (plan as MergePlan.Ready).preview

    @Test
    fun mrg01_defaults() {
        val p = ready(SceneMerge.plan(doc, S10, ctx))
        val after = p.result
        assertNull(after.index.unit(S10))
        val s9 = after.unit(S9)
        assertEquals(doc.unit(S9).place, s9.place)
        assertEquals(doc.unit(S9).time, s9.time)
        assertTrue(s9.memo!!.lines().contains("합치기 전 · 씬 4 · 결사의 문 · 2일째 아침 06:10"))
        val system = after.manuscript.liveBlocksOf(S9).single { it.kind.known == BlockKind.SYSTEM && it.number == 58 }
        assertEquals("결사의 문 앞 · 06:10", system.runs.plainText)
        assertEquals(59, after.unit(ExampleKeys.EPISODE_1_7).nextBlockNumber)
        val s11 = after.unit(S11).time as SceneTime.Relative
        assertEquals(S9, s11.baseKey)
        assertEquals(2, s11.dayMin)
        assertEquals(Band.DAWN, s11.band?.known)
        assertEquals(SceneTimes(doc).timed(S11)!!.absolute, SceneTimes(after).timed(S11)!!.absolute)
        assertEquals("약 15일째", Fx.labels(after).story(S11, io.github.lenticualrstone.civilight.domain.story.time.StoryDayStyle.COMPACT))
        assertOtherAbsolutesUnchanged(doc, after, setOf(S10))
        // Sheet values (T-22-13).
        assertEquals("1-7 씬 3 + 씬 4 → 씬 3", p.sub)
        assertEquals("장소 · 결사의 문 · 씬 3 · 씬 4 같음", p.placeLine)
        assertEquals("때 · 하나 고르기", p.timeLine)
        assertEquals(listOf("씬 3 것 · 2일째 새벽 05:40", "씬 4 것 · 2일째 아침 06:10"), p.timeChoices)
        assertEquals("고르지 않은 때 → 씬 메모 한 줄", p.timeNote)
        assertEquals("전환 문구 · 씬 4 직접", p.transitionLine)
        assertEquals("‘결사의 문 앞 · 06:10’", p.transitionText)
        // S11 is based on S10 and sits in the next episode, so it is named `1-8 씬 1`.
        assertEquals(
            listOf(
                "1-8 씬 1 때 · 기준 씬 4 → 씬 3 · 간격 다시 적음 · 절대 때 그대로",
                "제목 · 꺼진 등불 · 씬 4 제목 없음",
                "기록 · 블록에 그대로 · 위치 이름만 씬 4 → 씬 3",
            ),
            p.changeRows,
        )
    }

    @Test
    fun mrg02_chooseLaterTime() {
        val p = ready(SceneMerge.plan(doc, S10, ctx, MergeOptions(time = MergeSide.SECOND)))
        val after = p.result
        val s9 = after.unit(S9).time as SceneTime.Relative
        assertEquals(S8, s9.baseKey)
        assertEquals(1, s9.dayMin)
        assertEquals(clock(6, 10), s9.clock)
        assertEquals("2일째 아침 06:10", Fx.labels(after).head(S9))
        assertEquals(SceneTimes(doc).timed(S11)!!.absolute, SceneTimes(after).timed(S11)!!.absolute)
        assertTrue(after.unit(S9).memo!!.lines().contains("합치기 전 · 씬 3 · 결사의 문 · 2일째 새벽 05:40"))
        assertOtherAbsolutesUnchanged(doc, after, setOf(S9, S10))
    }

    @Test
    fun mrg03_blockedAndNotOffered() {
        val flash = doc.withTime(S10, SceneTime.Flashback(mode = FlashbackMode.DIRECT.open(), year = 1190))
        assertEquals("먼저 회상 끄기", (SceneMerge.plan(flash, S10, ctx) as MergePlan.Blocked).action)
        val hidden = doc.withUnit(S10) { it.copy(hidden = true) }
        assertEquals("먼저 숨김 풀기", (SceneMerge.plan(hidden, S10, ctx) as MergePlan.Blocked).action)
        assertEquals(MergePlan.NotOffered, SceneMerge.plan(doc, S11, ctx))
    }

    @Test
    fun mrg04_onePlaceOnly() {
        val v = doc.withUnit(S9) { it.copy(place = null) }
        val p = ready(SceneMerge.plan(v, S10, ctx))
        assertTrue(p.placeChoices.isEmpty())
        assertEquals("장소 · 결사의 문", p.placeLine)
        assertEquals(doc.unit(S10).place, p.result.unit(S9).place)
    }

    // --- 10-script T-22 time parts --------------------------------------------------------------

    @Test
    fun t2217_discardTransition() {
        val p = ready(SceneMerge.plan(doc, S10, ctx, MergeOptions(transition = TransitionOnMerge.DISCARD)))
        assertTrue(p.result.manuscript.liveBlocksOf(S9).none { it.kind.known == BlockKind.SYSTEM })
        assertEquals(58, p.result.unit(ExampleKeys.EPISODE_1_7).nextBlockNumber)
    }

    @Test
    fun t2224_secondIsFixedPoint() {
        val fixed = doc.withTime(S10, SceneTime.Direct(storyDay = 13, clock = clock(6, 10)))
        val p = ready(SceneMerge.plan(fixed, S10, ctx))
        assertEquals("씬 4 고정점 사라짐 · 씬 4 것을 고르면 직접 유지", p.fixedPointNote)
        assertEquals(doc.unit(S9).time, p.result.unit(S9).time)
        assertTrue(p.result.story.units.none { it.time is SceneTime.Direct && it.key == S9 })
        val second = ready(SceneMerge.plan(fixed, S10, ctx, MergeOptions(time = MergeSide.SECOND)))
        assertEquals(SceneTime.Direct(storyDay = 13, clock = clock(6, 10)), second.result.unit(S9).time)
    }

    @Test
    fun t2225_secondTitleGoesToMemo() {
        val titled = doc.withUnit(S10) { it.copy(title = "문 앞") }
        val p = ready(SceneMerge.plan(titled, S10, ctx))
        assertEquals("꺼진 등불", p.result.unit(S9).title)
        assertEquals(
            listOf("문 앞", "등불이 두 번째로 깜박이는 타이밍", "합치기 전 · 씬 4 · 결사의 문 · 2일째 아침 06:10"),
            p.result.unit(S9).memo!!.lines(),
        )
        assertTrue("제목 · 꺼진 등불 · 씬 4 제목 문 앞 → 씬 메모" in p.changeRows)
    }

    @Test
    fun t2226_timeOnOneSideOnly() {
        val v = doc.withTime(S9, SceneTime.None).withTime(S10, rel(S8, 1, clock = clock(6, 10)))
        val p = ready(SceneMerge.plan(v, S10, ctx))
        assertTrue(p.timeChoices.isEmpty())
        assertEquals("2일째 아침 06:10", Fx.labels(p.result).head(S9))
        assertEquals(S9, (p.result.unit(S11).time as SceneTime.Relative).baseKey)
        assertEquals(SceneTimes(v).timed(S11)!!.absolute, SceneTimes(p.result).timed(S11)!!.absolute)
    }

    @Test
    fun t2218_deleteAndRestoreKeepOtherTimes() {
        val deleted = io.github.lenticualrstone.civilight.domain.story.scene.SceneTrash.delete(doc, S10, ctx)
        assertEquals(S9, (deleted.unit(S11).time as SceneTime.Relative).baseKey)
        assertOtherAbsolutesUnchanged(doc, deleted, setOf(S10))
        val restored = io.github.lenticualrstone.civilight.domain.story.scene.SceneTrash.restore(deleted, S10, ctx)
        assertEquals("2일째 아침 06:10", Fx.labels(restored).head(S10))
        assertEquals(S10, (restored.unit(S11).time as SceneTime.Relative).baseKey)
        assertOtherAbsolutesUnchanged(doc, restored, emptySet())
    }

    @Test
    fun t1215_moveSceneToNextEpisodeRenumbersBlocks() {
        val plan = SceneMove.plan(doc, S9, ExampleKeys.EPISODE_1_8, null, ctx)
        assertEquals("1-7 · 씬 3 | 꺼진 등불", plan.fromLine)
        assertEquals("1-8 | 맨 앞 · 씬 1", plan.toLine)
        assertEquals("◆ 3 · ◇ 1 · 시점 1-7 → 1-8", plan.recordsRow)
        assertEquals("블록 13개 · 1-8 새 번호", plan.blocksRow)
        assertEquals("그대로", plan.voiceRow)
        assertEquals("그대로 · ◇ 1 순서와 어긋남", plan.timeRow)
        val after = plan.result
        assertEquals((61..73).toList(), after.blockNumbersOf(S9))
        assertEquals(74, after.unit(ExampleKeys.EPISODE_1_8).nextBlockNumber)
        assertEquals(ExampleKeys.EPISODE_1_8, after.index.episodeGroupOfBlock(ExampleKeys.BLOCK_043))
        assertOtherAbsolutesUnchanged(doc, after, emptySet())
    }
}
