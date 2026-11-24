package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.newproject.NewProjectDraft
import io.github.lenticualrstone.civilight.domain.library.newproject.NewProjectFlow
import io.github.lenticualrstone.civilight.domain.library.newproject.StartBlock
import io.github.lenticualrstone.civilight.domain.library.onboarding.FirstUseTips
import io.github.lenticualrstone.civilight.domain.library.onboarding.ModuleTours
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourRun
import io.github.lenticualrstone.civilight.model.app.ExampleState
import io.github.lenticualrstone.civilight.model.app.OnboardingState
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LIB-19 tours, LIB-20 tips, LIB-21 reset, LIB-13 `숨김 · 끔` from the document. */
class OnboardingGuideTest {
    @Test
    fun `LIB-19 step counts and drawn copy follow the spec table`() {
        // The script tour ends at `씬 머리`: no `시점 렌즈` step, the script has no lens (STO-16).
        // 세계 has no `연표` step and 장소와 때 no `▲ 이동 시간` step: their screens have no control for them
        // (emulator QA 2026-11-23: those cards floated with nothing marked).
        val counts = mapOf(
            TourModule.SCRIPT to 3, TourModule.CAST to 3, TourModule.WORLD_TIMELINE to 2, TourModule.RECORDER to 3,
            TourModule.LIBRARY_HIDING to 3, TourModule.MAP to 2, TourModule.ROUTE_PLACE_TIME to 2,
        )
        TourModule.entries.forEach { assertEquals(it.name, counts.getValue(it), ModuleTours.stepCount(it)) }
        assertEquals(listOf("공개 시점", "용어 잇기"), ModuleTours.steps(TourModule.WORLD_TIMELINE).map { it.title })
        assertEquals(listOf("장소와 때", "인물 동선"), ModuleTours.steps(TourModule.ROUTE_PLACE_TIME).map { it.title })
        // (0.2.0 S-3) The map tour has no `경로` step and its tool row names no `경로`; 인물 동선's tour is not offered.
        assertEquals(listOf("도구 줄", "보이는 시점"), ModuleTours.steps(TourModule.MAP).map { it.title })
        assertTrue(ModuleTours.steps(TourModule.MAP).none { "경로" in it.sub })
        assertEquals(TourModule.entries - TourModule.ROUTE_PLACE_TIME - TourModule.MAP, ModuleTours.offered)
        val script = ModuleTours.steps(TourModule.SCRIPT)
        assertEquals("명령 칸" to "이동 · 검색 · 블록 추가 · 마이크", script[0].title to script[0].sub)
        assertEquals("블록을 오른쪽으로" to "시간 · 참고 · 속성 → 옆 패널 탭", script[1].title to script[1].sub)
        assertEquals("씬 머리" to "장소와 때 · 전환 문구 · ⋯ 메뉴", script[2].title to script[2].sub)
        assertTrue(script.none { it.title == "시점 렌즈" })
        assertEquals("연결", ModuleTours.steps(TourModule.RECORDER)[1].title)
        assertEquals("인물 안내", TourModule.CAST.label)
        // Every card reads `<area> 안내 · k/n`, the script's too.
        assertEquals("스크립트 안내", TourModule.SCRIPT.label)
        TourModule.entries.forEach { assertTrue(it.name, it.label?.endsWith(" 안내") == true) }
        assertEquals(
            listOf("스크립트", "인물", "세계 · 연표", "라이프 레코더", "서재 · 숨기기", "지도 만들기", "인물 동선 · 장소와 때"),
            TourModule.entries.map { it.rowTitle },
        )
    }

    @Test
    fun `LIB-19 every tour has its copy and runs once, and 다시 보기 runs it again`() {
        val fresh = OnboardingState()
        // Every step has words now (2026-11-23), so every tour starts on its first entry.
        TourModule.entries.forEach {
            assertTrue(it.name, ModuleTours.copyComplete(it))
            assertTrue(it.name, ModuleTours.shouldStart(fresh, it))
            assertTrue(it.name, ModuleTours.steps(it).all { s -> s.title.isNotBlank() && s.sub.isNotBlank() && !s.title.startsWith("TODO_COPY_") && !s.sub.startsWith("TODO_COPY_") })
        }
        assertEquals("서재 안내", TourModule.LIBRARY_HIDING.label)
        val seen = ModuleTours.markSeen(fresh, TourModule.CAST)
        assertTrue(ModuleTours.seen(seen, TourModule.CAST))
        assertFalse(ModuleTours.shouldStart(seen, TourModule.CAST))
        // LIB-21 `다시 보기`: the flag is reset, so the tour starts again on the next entry.
        val replay = ModuleTours.reset(seen, TourModule.CAST)
        assertFalse(ModuleTours.seen(replay, TourModule.CAST))
        assertTrue(ModuleTours.shouldStart(replay, TourModule.CAST))
    }

    @Test
    fun `LIB-19 the card walks next, previous and finish`() {
        var run = TourRun(TourModule.SCRIPT)
        assertFalse(run.hasPrevious)
        assertEquals("다음", run.primaryLabel)
        run = run.next()!!.next()!!
        assertTrue(run.isLast)
        assertEquals("마치기", run.primaryLabel)
        assertEquals("씬 머리", run.step.title)
        assertNull(run.next())
        assertEquals(1, run.previous().index)
        assertEquals(0, TourRun(TourModule.MAP).previous().index)
    }

    @Test
    fun `LIB-20 a tip shows once, never while the switch is off`() {
        val state = OnboardingState()
        assertTrue(FirstUseTips.shouldShow(state, tipsOn = true, tipId = "script.choice"))
        assertFalse(FirstUseTips.shouldShow(state, tipsOn = false, tipId = "script.choice"))
        val shown = FirstUseTips.markShown(state, "script.choice")
        assertFalse(FirstUseTips.shouldShow(shown, tipsOn = true, tipId = "script.choice"))
        assertTrue(FirstUseTips.shouldShow(shown, tipsOn = true, tipId = "map.pin"))
        assertEquals(shown, FirstUseTips.markShown(shown, "script.choice"))
    }

    @Test
    fun `LIB-21 모든 안내 처음부터 resets tours, tips and the walkthrough but keeps onboarding finished`() {
        val done = OnboardingState(
            firstRunCompleted = true, walkthroughSeen = true, exampleState = ExampleState.DELETED,
            moduleTourSeen = mapOf("SCRIPT" to true, "CAST" to true), tipsShown = setOf("a", "b"), emptyLibraryNoticeShown = true,
        )
        val reset = FirstUseTips.resetAll(done)
        assertFalse(reset.walkthroughSeen)
        assertTrue(reset.tipsShown.isEmpty())
        TourModule.entries.forEach { assertFalse(ModuleTours.seen(reset, it)) }
        assertTrue(reset.firstRunCompleted)
        assertEquals(ExampleState.DELETED, reset.exampleState)
        assertTrue(reset.emptyLibraryNoticeShown)
    }

    @Test
    fun `LIB-13 R19-11 the summary says 숨김 · 끔 when an R-18 project was created visible`() {
        val defaults = RatingDefaults()
        val visible = NewProjectFlow.create(
            NewProjectDraft(name = "검은 서약", rating = Rating.R18, step4Hide = false), defaults, Fixtures.NOW, Fixtures.DEVICE, Fixtures.keys(7),
        ).doc
        assertTrue(StartBlock.ofProject(visible, RecorderLibrary(), defaults).summary.endsWith(" · 숨김 · 끔"))
        val hidden = NewProjectFlow.create(
            NewProjectDraft(name = "검은 서약", rating = Rating.R18), defaults, Fixtures.NOW, Fixtures.DEVICE, Fixtures.keys(8),
        ).doc
        assertFalse(StartBlock.ofProject(hidden, RecorderLibrary(), defaults).summary.contains("숨김"))
        val all = NewProjectFlow.create(NewProjectDraft(name = "유리 정원"), defaults, Fixtures.NOW, Fixtures.DEVICE, Fixtures.keys(9)).doc
        assertFalse(StartBlock.ofProject(all, RecorderLibrary(), defaults).summary.contains("숨김"))
        // With the R-18 hide default off in 설정, a visible R-18 project turned nothing off.
        assertFalse(StartBlock.ofProject(visible, RecorderLibrary(), defaults.copy(r18Hide = false)).summary.contains("숨김"))
    }
}
