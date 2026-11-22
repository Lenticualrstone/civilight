package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.Fx.S10
import io.github.lenticualrstone.civilight.domain.story.Fx.S11
import io.github.lenticualrstone.civilight.domain.story.Fx.S12
import io.github.lenticualrstone.civilight.domain.story.Fx.S2
import io.github.lenticualrstone.civilight.domain.story.Fx.S3
import io.github.lenticualrstone.civilight.domain.story.Fx.S4
import io.github.lenticualrstone.civilight.domain.story.Fx.S6
import io.github.lenticualrstone.civilight.domain.story.Fx.S7
import io.github.lenticualrstone.civilight.domain.story.Fx.S8
import io.github.lenticualrstone.civilight.domain.story.Fx.S9
import io.github.lenticualrstone.civilight.domain.story.Fx.assertOtherAbsolutesUnchanged
import io.github.lenticualrstone.civilight.domain.story.Fx.clock
import io.github.lenticualrstone.civilight.domain.story.Fx.rel
import io.github.lenticualrstone.civilight.domain.story.Fx.unit
import io.github.lenticualrstone.civilight.domain.story.Fx.withTime
import io.github.lenticualrstone.civilight.domain.story.scene.NewScene
import io.github.lenticualrstone.civilight.domain.story.scene.SceneMove
import io.github.lenticualrstone.civilight.domain.story.scene.SceneTrash
import io.github.lenticualrstone.civilight.domain.story.time.BaseRules
import io.github.lenticualrstone.civilight.domain.story.time.CascadeMode
import io.github.lenticualrstone.civilight.domain.story.time.CascadeRow
import io.github.lenticualrstone.civilight.domain.story.time.OrderChecks
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.StoryDayStyle
import io.github.lenticualrstone.civilight.domain.story.time.TimeChange
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.StoryIndexFile
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 20-story-time §4.6 BASE, CASC, INS, DEL, MOVE and §4.7 ORD-01/02. */
class TimeEditTest {
    private val doc = Fx.doc()
    private val ctx = Fx.ctx()

    // --- Base ---------------------------------------------------------------------------------

    @Test
    fun base01_baseLabelSkipsUntimedScene() {
        val line = BaseRules.baseLine(SceneTimes(doc), S4)!!
        assertEquals("1-2 씬 1 기준", line.text)
        assertFalse(line.isDirectlyPrevious)
        // A base that is the scene right before: `직전 씬 기준 | 씬 2 · 1-7 · 1일째 저녁`.
        val prev = BaseRules.baseLine(SceneTimes(doc), S9)!!
        assertEquals("때 · 직전 씬 기준", prev.label)
        assertEquals("씬 2 · 1-7 · 1일째 저녁", prev.value)
    }

    @Test
    fun base02_rebaseToPreviousKeepsAbsolute() {
        val timed = doc.withTime(S3, rel(S2, 0, Band.DAY))
        val times = SceneTimes(timed)
        assertEquals(S2, (timed.unit(S4).time as SceneTime.Relative).baseKey)
        assertEquals(SceneTimes(doc).timed(S4)!!.absolute, times.timed(S4)!!.absolute)
        assertTrue(BaseRules.canRebaseToPrevious(times, S4))
        val rebased = BaseRules.rebaseToPrevious(timed, S4)
        val s4 = rebased.unit(S4).time as SceneTime.Relative
        assertEquals(S3, s4.baseKey)
        assertEquals(1, s4.dayMin)
        assertEquals(1, s4.dayMax)
        assertEquals(Band.MORNING, s4.band?.known)
        assertEquals(times.timed(S4)!!.absolute, SceneTimes(rebased).timed(S4)!!.absolute)
        assertFalse(BaseRules.canRebaseToPrevious(SceneTimes(rebased), S4))
    }

    @Test
    fun base03_firstSceneOnlyDirect() {
        assertTrue(BaseRules.onlyDirectEnabled(SceneTimes(doc), Fx.S1))
        val first = StoryUnit(key = "01JZ000000SCNE000000990001", level = 0, order = OrderKeys.FIRST)
        val fresh = ProjectDoc(
            project = Project(key = "01JZ000000PRJT000000000099", name = "새 이야기", structure = io.github.lenticualrstone.civilight.model.project.StoryStructure(levels = listOf("씬"))),
            story = StoryIndexFile(units = listOf(first)),
        )
        assertTrue(BaseRules.onlyDirectEnabled(SceneTimes(fresh), first.key))
        assertFalse(BaseRules.onlyDirectEnabled(SceneTimes(doc), S4))
    }

    // --- Cascade -------------------------------------------------------------------------------

    private val plusFour = rel(S6, 4, Band.DAWN)

    @Test
    fun casc01_previewAndWithFollowing() {
        val p = TimeChange.preview(doc, S7, plusFour, ctx)
        assertTrue(p.needsPreview)
        assertEquals("씬 1 · 때 바꾸기", p.title)
        assertEquals("1-7 씬 1 · 첫 번째 깜박임 · 1-6 씬 1 기준", p.sceneLine)
        assertEquals("약 12일째 새벽", p.before)
        assertEquals("약 14일째 새벽", p.after)
        assertEquals("+2일 · 이야기 날", p.deltaLine)
        assertEquals("뒤 씬 4개 +2일 · 1-9 씬 2(직접)에서 멈춤", p.summary)
        assertEquals(
            listOf(
                CascadeRow(S8, "1-7 씬 2", "약 12일째 저녁", "약 14일째 저녁"),
                CascadeRow(S9, "1-7 씬 3", "약 13일째 새벽", "약 15일째 새벽"),
                CascadeRow(S10, "1-7 씬 4", "약 13일째 아침", "약 15일째 아침"),
                CascadeRow(S11, "1-8 씬 1", "약 15일째 새벽", "약 17일째 새벽"),
            ),
            p.rows,
        )
        assertNull(p.restLine)
        assertEquals("1-9 씬 2 | 직접 · 그대로 | 고정점", p.fixedPointRow)
        assertEquals("뒤도 같이 · 기본 · 뒤 씬 4개 +2일", p.withFollowingLine)

        val after = p.result(CascadeMode.WITH_FOLLOWING)
        val changed = after.story.units.zip(doc.story.units).filter { (x, y) -> x != y }.map { it.first.key }
        assertEquals(listOf(S7), changed)
        val before = SceneTimes(doc)
        val now = SceneTimes(after)
        for (s in listOf(S8, S9, S10, S11)) assertEquals(before.storyDay(s)!! + 2, now.storyDay(s))
        assertEquals(20, now.storyDay(S12)!!.min)
    }

    @Test
    fun casc02_thisOnly() {
        val p = TimeChange.preview(doc, S7, plusFour, ctx)
        assertEquals("이 씬만 · 뒤 씬 때 그대로 · ◇ 순서와 때가 다름 1", p.thisOnlyLine)
        val after = p.result(CascadeMode.THIS_ONLY)
        val l = Fx.labels(after)
        assertEquals("약 14일째", l.story(S7, StoryDayStyle.COMPACT))
        val s8 = after.unit(S8).time as SceneTime.Relative
        assertEquals(S7, s8.baseKey)
        assertEquals(-2, s8.dayMin)
        assertEquals(-2, s8.dayMax)
        assertEquals(Band.EVENING, s8.band?.known)
        assertEquals("약 12일째", l.story(S8, StoryDayStyle.COMPACT))
        for (s in listOf(S9, S10, S11)) assertEquals(doc.unit(s).time, after.unit(s).time)
        assertEquals(listOf(S8), p.thisOnlyNotices.map { it.sceneKey })
        assertEquals(listOf(S8), OrderChecks.notices(SceneTimes(after)).map { it.sceneKey })
    }

    @Test
    fun casc03_cascadePastFixedPoint() {
        val after = TimeChange.apply(doc, S7, rel(S6, 10, Band.DAWN), CascadeMode.WITH_FOLLOWING, ctx)
        val l = Fx.labels(after)
        assertEquals("약 23일째 (21~25)", l.story(S11))
        assertEquals("20일째 낮", l.storyShort(S12))
        assertTrue(OrderChecks.notices(SceneTimes(after)).any { it.sceneKey == S12 && it.previousKey == S11 })
    }

    @Test
    fun casc04_oneUndoStep() {
        val after = TimeChange.apply(doc, S7, plusFour, CascadeMode.WITH_FOLLOWING, ctx)
        // Undo = the previous doc; only S7's stored value differs.
        val diff = doc.story.units.zip(after.story.units).filter { (x, y) -> x != y }
        assertEquals(1, diff.size)
        assertEquals(doc.unit(S7).time, diff.single().first.time)
        assertEquals(doc.copy(story = after.story.copy(units = after.story.units.map { if (it.key == S7) doc.unit(S7) else it })), doc)
    }

    @Test
    fun casc05_noDependentsNoPreview() {
        val p = TimeChange.preview(doc, S11, rel(S10, 3, Band.DAWN), ctx)
        assertFalse(p.needsPreview)
        assertEquals(rel(S10, 3, Band.DAWN), (p.withFollowing.unit(S11).time as SceneTime.Relative).copy(ext = null))
    }

    // --- Insert, delete, restore, move ------------------------------------------------------------

    @Test
    fun ins01_newSceneAfterS8() {
        val prefill = NewScene.prefillAfter(doc, S8)
        assertEquals(rel(S8, 0, Band.EVENING), prefill.time)
        assertEquals("때 · 씬 2 기준 · 같은 때", prefill.timeLine)
        assertEquals("장소 · 상층 관문 · 씬 2와 같음", prefill.placeLine)
        val (kept, n) = NewScene.create(doc, ExampleKeys.EPISODE_1_7, S8, ctx, place = prefill.place, time = prefill.time)
        val times = SceneTimes(kept)
        assertEquals(times.timed(S8)!!.day, times.timed(n)!!.day)
        assertEquals(S8, (kept.unit(S9).time as SceneTime.Relative).baseKey)
        assertOtherAbsolutesUnchanged(doc, kept, setOf(n))
        assertEquals("1-7 · 1일째 · 저녁", Fx.labels(kept).full(n))
        assertEquals("씬 3", Fx.labels(kept).codes.sceneLabel(n))

        val (emptied, m) = NewScene.create(doc, ExampleKeys.EPISODE_1_7, S8, Fx.ctx())
        assertEquals(SceneTime.None, emptied.unit(m).time)
        assertEquals(doc.unit(S9).time, emptied.unit(S9).time)
        assertOtherAbsolutesUnchanged(doc, emptied, setOf(m))
    }

    @Test
    fun del01_deleteRebasesDependents() {
        val after = SceneTrash.delete(doc, S8, ctx)
        val s9 = after.unit(S9).time as SceneTime.Relative
        assertEquals(S7, s9.baseKey)
        assertEquals(1, s9.dayMin)
        assertEquals(clock(5, 40), s9.clock)
        assertEquals(S8, s9.rebasedFromKey)
        assertEquals("1-7 · 2일째 · 새벽", Fx.labels(after).full(S9))
        assertOtherAbsolutesUnchanged(doc, after, setOf(S8))
        assertFalse(after.liveness.isUnitLive(S8))
        val entry = after.trash.entries.single { it.targetKeys == listOf(S8) }
        assertEquals(TrashKind.SCENE, entry.kind.known)
        assertEquals(entry.key, after.unit(S8).trashEntryKey)
    }

    @Test
    fun del02_restoreReturnsDependents() {
        val deleted = SceneTrash.delete(doc, S8, ctx)
        val restored = SceneTrash.restore(deleted, S8, ctx)
        assertEquals("1-7 · 1일째 · 저녁", Fx.labels(restored).full(S8))
        val s9 = restored.unit(S9).time as SceneTime.Relative
        assertEquals(S8, s9.baseKey)
        assertEquals(1, s9.dayMin)
        assertNull(s9.rebasedFromKey)
        assertOtherAbsolutesUnchanged(doc, restored, emptySet())
        assertTrue(restored.trash.entries.none { S8 in it.targetKeys })
    }

    @Test
    fun move01_moveAfterS11() {
        val plan = SceneMove.plan(doc, S9, ExampleKeys.EPISODE_1_8, S11, ctx)
        assertEquals("그대로 · ◇ 1 순서와 어긋남", plan.timeRow)
        val after = plan.result
        assertEquals(doc.unit(S9).time, after.unit(S9).time)
        assertEquals(S9, (after.unit(S10).time as SceneTime.Relative).baseKey)
        assertEquals("약 13일째", Fx.labels(after).story(S9, StoryDayStyle.COMPACT))
        assertOtherAbsolutesUnchanged(doc, after, emptySet())
        assertEquals(listOf(S9 to S11), plan.newOrderNotices.map { it.sceneKey to it.previousKey })
    }

    // --- Order notices --------------------------------------------------------------------------

    @Test
    fun ord01_definitelyEarlier() {
        val v = doc.withTime(S8, rel(S7, 0, clock = clock(20, 0))).withTime(S9, rel(S8, 0, clock = clock(11, 0)))
        val notice = OrderChecks.notice(SceneTimes(v), S9)!!
        assertEquals("직전 씬보다 앞 · 9시간 전", notice.line)
        assertEquals(S8, notice.previousKey)
    }

    @Test
    fun ord02_straddlingRangeRaisesNothing() {
        val v = doc.withTime(S9, rel(S8, 1, Band.MORNING)).withTime(S10, rel(S9, 0, Band.MORNING))
        assertNull(OrderChecks.notice(SceneTimes(v), S10))
    }

    @Test
    fun fixtureHasNoOrderNotices() {
        assertTrue(OrderChecks.notices(SceneTimes(doc)).isEmpty())
    }

    @Test
    fun leavingTheChainRebasesDependents() {
        val p = TimeChange.preview(doc, S8, SceneTime.None, ctx)
        assertFalse(p.needsPreview)
        assertEquals(S7, (p.withFollowing.unit(S9).time as SceneTime.Relative).baseKey)
        assertOtherAbsolutesUnchanged(doc, p.withFollowing, setOf(S8))
    }
}
