package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.Fx.S1
import io.github.lenticualrstone.civilight.domain.story.Fx.S10
import io.github.lenticualrstone.civilight.domain.story.Fx.S2
import io.github.lenticualrstone.civilight.domain.story.Fx.S3
import io.github.lenticualrstone.civilight.domain.story.Fx.S4
import io.github.lenticualrstone.civilight.domain.story.Fx.S5
import io.github.lenticualrstone.civilight.domain.story.Fx.S6
import io.github.lenticualrstone.civilight.domain.story.Fx.S7
import io.github.lenticualrstone.civilight.domain.story.Fx.S8
import io.github.lenticualrstone.civilight.domain.story.Fx.S9
import io.github.lenticualrstone.civilight.domain.story.Fx.approx
import io.github.lenticualrstone.civilight.domain.story.Fx.clock
import io.github.lenticualrstone.civilight.domain.story.Fx.rel
import io.github.lenticualrstone.civilight.domain.story.Fx.unit
import io.github.lenticualrstone.civilight.domain.story.Fx.withTime
import io.github.lenticualrstone.civilight.domain.story.time.AnchorRelative
import io.github.lenticualrstone.civilight.domain.story.time.Durations
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.Span
import io.github.lenticualrstone.civilight.domain.story.time.StoryDayStyle
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.StoryIndexFile
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.CalendarMonth
import io.github.lenticualrstone.civilight.model.project.MonthDay
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.project.StoryStructure
import io.github.lenticualrstone.civilight.model.project.StructurePreset
import io.github.lenticualrstone.civilight.model.project.WorldCalendar
import io.github.lenticualrstone.civilight.model.project.WorldTime
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.story.CalendarDate
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 20-story-time §4.4 TIME-01 … TIME-13 (resolution and labels). */
class TimeResolutionTest {
    private val doc = Fx.doc()
    private val labels = Fx.labels(doc)

    private fun row(scene: String, full: String, story: String, anchor: AnchorRelative) {
        assertEquals(full, labels.full(scene))
        assertEquals(story, labels.story(scene, StoryDayStyle.FULL))
        assertEquals(anchor, labels.anchorRelative(scene, S4))
    }

    @Test
    fun time01_S2() = row(S2, "1-2 · 1일째 · 새벽", "이야기 2일째", AnchorRelative("−1일", null))

    @Test
    fun time02_S4() = row(S4, "1-3 · 1일째 · 아침", "이야기 3일째", AnchorRelative("기준", null))

    @Test
    fun time03_S6() = row(S6, "1-6 · 1일째 · 낮", "약 10일째 (8~12)", AnchorRelative("+약 7일", "5~9일"))

    @Test
    fun time04_S7() = row(S7, "1-7 · 1일째 · 새벽", "약 12일째 (10~14)", AnchorRelative("+약 9일", "7~11일"))

    @Test
    fun time05_S8() = row(S8, "1-7 · 1일째 · 저녁", "약 12일째 (10~14)", AnchorRelative("+약 9일", "7~11일"))

    @Test
    fun time06_S9() = row(S9, "1-7 · 2일째 · 새벽", "약 13일째 (11~15)", AnchorRelative("+약 10일", "8~12일"))

    @Test
    fun time07_S10() {
        row(S10, "1-7 · 2일째 · 아침", "약 13일째 (11~15)", AnchorRelative("+약 10일", "8~12일"))
        assertEquals("2일째 아침 06:10", labels.head(S10))
    }

    @Test
    fun time08_sharedRangeGivesExactDifference() {
        val times = SceneTimes(doc)
        assertEquals(Span(580, 760), times.minutesBetween(S8, S9))
        assertEquals("9시간 40분 – 12시간 40분", Durations.gapRange(times.minutesBetween(S8, S9)!!))
        // Shared variable: the day difference is exactly 1, although both are `약 N일째`.
        assertEquals(Span.point(1), times.daysBetween(S8, S9)!!.range)
        // Negative test: independent ranges would give −3~5 days.
        val independent = Span(times.storyDay(S9)!!.min - times.storyDay(S8)!!.max, times.storyDay(S9)!!.max - times.storyDay(S8)!!.min)
        assertEquals(Span(-3, 5), independent)
        assertNotEquals(independent, times.daysBetween(S8, S9)!!.range)
    }

    @Test
    fun time09_shortForms() {
        assertEquals("2일째 새벽", labels.storyColumn(S9))
        assertEquals("1-7 · 씬 3 · 2일째 새벽", labels.scriptHeader(S9))
        assertEquals("1일째 새벽", labels.storyColumn(S7))
        assertEquals("—", labels.storyColumn(S3))
        assertEquals("때 없음", labels.bulkValue(S3))
    }

    @Test
    fun time10_episodeFirstSceneUntimed() {
        val variant = doc.withTime(S7, SceneTime.None).withTime(S8, rel(S6, 2, Band.EVENING))
        val l1 = Fx.labels(variant)
        assertEquals("1-7 · 1일째 · 저녁", l1.full(S8))
        assertEquals("1-7 · 2일째 · 새벽", l1.full(S9))
        val later = variant.withTime(S7, rel(S6, 1, Band.DAWN))
        val l2 = Fx.labels(later)
        assertEquals("1-7 · 1일째 · 새벽", l2.full(S7))
        assertEquals("1-7 · 2일째 · 저녁", l2.full(S8))
        assertEquals("1-7 · 3일째 · 새벽", l2.full(S9))
        assertEquals(variant.unit(S8).time, later.unit(S8).time)
        assertEquals(variant.unit(S9).time, later.unit(S9).time)
    }

    @Test
    fun time11_approxInsideAnEpisode() {
        val variant = doc.withTime(S9, approx(S8, 1, 3, clock = clock(5, 40)))
        val l = Fx.labels(variant)
        assertEquals("1-7 · 1일째 · 새벽", l.full(S7))
        assertEquals("1-7 · 1일째 · 저녁", l.full(S8))
        assertEquals("1-7 · 약 3일째 · 새벽", l.full(S9))
        assertEquals("1-7 · 약 3일째 · 아침", l.full(S10))
        assertEquals(Span.point(30), SceneTimes(variant).minutesBetween(S9, S10))
    }

    @Test
    fun time12_flashbackLabels() {
        assertEquals("회상 · 1195", labels.short(S5))
        assertEquals("1-4 · 씬 2 · 회상 · 1195", labels.scriptHeader(S5))
        assertEquals("연도 1195 · 대정전 그날" to "이야기 시작 5년 전", labels.flashbackResult(S5))
        assertNull(labels.dayPart(S5))
    }

    @Test
    fun time13_noEpisodeLevel() {
        val project = Project(key = "01JZ000000PRJT000000000099", name = "단편", structure = StoryStructure(StructurePreset.CUSTOM.open(), listOf("씬")))
        val orders = OrderKeys.evenlySpaced(2)
        val a = StoryUnit(key = "01JZ000000SCNE000000990001", level = 0, order = orders[0], time = SceneTime.Direct(storyDay = 1, band = Band.DAWN.open()))
        val b = StoryUnit(key = "01JZ000000SCNE000000990002", level = 0, order = orders[1], time = rel(a.key, 1, Band.DAY))
        val single = ProjectDoc(project = project, story = StoryIndexFile(units = listOf(a, b)))
        val l = Fx.labels(single)
        assertEquals("1일째 새벽", l.short(a.key))
        assertEquals("2일째 낮", l.short(b.key))
        assertEquals("2일째 · 낮", l.full(b.key))
        assertEquals("씬 2", l.codes.sceneRef(single.index.unit(b.key)!!))
    }

    // --- Own edge cases -------------------------------------------------------------------------

    @Test
    fun storyNotationCountsFromTheStoryStart() {
        val story = doc.copy(project = doc.project.copy(sceneTime = doc.project.sceneTime.copy(notation = io.github.lenticualrstone.civilight.model.project.SceneNotation.STORY_START.open())))
        assertEquals("1-7 · 약 13일째 · 새벽", Fx.labels(story).full(S9))
        assertEquals("1-2 · 2일째 · 새벽", Fx.labels(story).full(S2))
    }

    @Test
    fun cyclesAndMissingBasesFallBackToThePreviousTimedScene() {
        val cyclic = doc.withTime(S7, rel(S8, 0, Band.DAWN)).withTime(S8, rel(S7, 0, Band.EVENING))
        val times = SceneTimes(cyclic)
        // Neither resolves through the cycle; both fall back to the nearest earlier timed scene.
        assertEquals(io.github.lenticualrstone.civilight.domain.story.time.ResolvedTime.Timed::class, times.resolve(S7)::class)
        val missing = doc.withTime(S2, rel("01JZ000000SCNE000000999999", 1, Band.DAWN))
        // The nearest earlier timed scene of 1-2 씬 1 is 1-1 씬 3.
        assertEquals(io.github.lenticualrstone.civilight.model.sample.ExampleKeys.SCENE_1_1_3, SceneTimes(missing).timed(S2)!!.baseKey)
        assertEquals(S1, SceneTimes(doc).timed(S2)!!.baseKey)
        assertEquals("1-2 · 1일째 · 새벽", Fx.labels(missing).full(S2))
    }

    @Test
    fun flashbackBaseIsSkipped() {
        // 1-4 씬 3 is based on S4; turning S4 into a flashback makes it fall back to S2.
        val variant = doc.withTime(S4, SceneTime.Flashback(mode = io.github.lenticualrstone.civilight.model.story.FlashbackMode.DIRECT.open(), year = 1190))
        val times = SceneTimes(variant)
        assertEquals(S2, times.timed(io.github.lenticualrstone.civilight.model.sample.ExampleKeys.SCENE_1_4_3)!!.baseKey)
        assertEquals("1-4 · 씬 2 · 회상 · 1195", Fx.labels(variant).scriptHeader(S5))
    }

    @Test
    fun calendarDateLabels() {
        val cal = WorldCalendar(months = listOf(CalendarMonth("서리달", 30), CalendarMonth("잿달", 30)), storyStart = MonthDay(1, 25))
        val withCal = doc.copy(project = doc.project.copy(worldTime = WorldTime(WorldTimeMode.CALENDAR.open(), 1200, cal)))
        val l = Fx.labels(withCal)
        assertEquals("서리달 25일", l.calendar(S1))
        assertEquals("서리달 26일", l.calendar(S2))
        assertEquals("잿달 2일 ~ 6일", l.calendar(S6))
        assertNull(Fx.labels(doc).calendar(S1))
        val direct = withCal.withTime(S12Key, SceneTime.Direct(calendarDate = CalendarDate(2, 10)))
        assertEquals(16, SceneTimes(direct).storyDay(S12Key)!!.min)
    }

    @Test
    fun midpointRoundsHalfUp() {
        assertEquals(6, Span(5, 6).midpoint)
        assertEquals("약 6일째", TimeLabels(SceneTimes(doc)).dayWord(Span(5, 6)))
    }

    private val S12Key = Fx.S12
}
