package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.masking.HiddenAccess
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectSurfaces
import io.github.lenticualrstone.civilight.domain.library.onboarding.ModuleTours
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.domain.library.rating.Protection
import io.github.lenticualrstone.civilight.domain.library.rating.ProjectProtectionSettings
import io.github.lenticualrstone.civilight.domain.recorder.ProjectSnapshot
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkViews
import io.github.lenticualrstone.civilight.feature.library.help.HelpReplayState
import io.github.lenticualrstone.civilight.feature.library.notices.LibraryNotices
import io.github.lenticualrstone.civilight.feature.library.onboarding.ExampleTourState
import io.github.lenticualrstone.civilight.feature.library.onboarding.FinishPorts
import io.github.lenticualrstone.civilight.feature.library.onboarding.OnbTarget
import io.github.lenticualrstone.civilight.feature.library.onboarding.OnboardingFinisher
import io.github.lenticualrstone.civilight.feature.library.onboarding.OnboardingFlow
import io.github.lenticualrstone.civilight.feature.library.tour.TourPlacement
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ExampleState
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.app.OnboardingState
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.navigation.CharacterProfile
import io.github.lenticualrstone.civilight.navigation.ExampleTour
import io.github.lenticualrstone.civilight.navigation.Onboarding
import io.github.lenticualrstone.civilight.navigation.OnboardingPage
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.TermCard
import io.github.lenticualrstone.civilight.navigation.Walkthrough
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LIB-08 resume, LIB-15 … LIB-18 onboarding, LIB-19 placement, LIB-21 rows, LIB-28, LIB-30. */
class OnboardingTest {
    private val example = ExampleKeys.PROJECT

    @Test
    fun `LIB-08 resume ids are the values the shell's start routes read`() {
        OnboardingPage.entries.forEach { assertEquals(it.name, OnboardingFlow.resumeId(Onboarding(it))) }
        assertEquals("WALKTHROUGH_2", OnboardingFlow.resumeId(Walkthrough(2)))
        assertEquals("WALKTHROUGH_3", OnboardingFlow.resumeId(Walkthrough(9)))
        assertEquals("EXAMPLE", OnboardingFlow.resumeId(ExampleTour(example)))
        assertEquals("EXAMPLE_SCRIPT", OnboardingFlow.EXAMPLE_SCRIPT)
        assertNull(OnboardingFlow.resumeId(Script(example)))
        assertTrue(OnboardingFlow.recordsResume(OnboardingState()))
        assertFalse(OnboardingFlow.recordsResume(OnboardingState(firstRunCompleted = true)))
    }

    @Test
    fun `LIB-15 the first-run order, 나중에 is 다음, 건너뛰기 goes to the walkthrough`() {
        assertEquals(Onboarding(OnboardingPage.LOOK), OnboardingFlow.next(OnboardingPage.WELCOME))
        assertEquals(Onboarding(OnboardingPage.SAFE), OnboardingFlow.next(OnboardingPage.LOOK))
        assertEquals(Onboarding(OnboardingPage.VOICE), OnboardingFlow.next(OnboardingPage.SAFE))
        assertEquals(Walkthrough(1), OnboardingFlow.next(OnboardingPage.VOICE))
        assertEquals(Walkthrough(1), OnboardingFlow.skip())
        assertEquals(listOf(null, 1, 2, 3), OnboardingPage.entries.map(OnboardingFlow::setupStep))
    }

    @Test
    fun `LIB-16 walkthrough buttons, first run and replay`() {
        assertEquals(OnbTarget.Go(Walkthrough(2)), OnboardingFlow.walkNext(1, replay = false, exampleKey = example))
        assertEquals(OnbTarget.Go(ExampleTour(example)), OnboardingFlow.walkNext(3, replay = false, exampleKey = example))
        assertEquals(OnbTarget.Go(ExampleTour(example)), OnboardingFlow.walkSkip(replay = false, exampleKey = example))
        assertEquals(OnbTarget.Finish, OnboardingFlow.walkNext(3, replay = false, exampleKey = null))
        assertEquals(OnbTarget.EndReplay, OnboardingFlow.walkNext(3, replay = true, exampleKey = null))
        assertEquals(OnbTarget.EndReplay, OnboardingFlow.walkSkip(replay = true, exampleKey = example))
        assertNull(OnboardingFlow.walkPrevious(1))
        assertEquals(Walkthrough(2), OnboardingFlow.walkPrevious(3))
        assertEquals("다음", OnboardingFlow.walkPrimaryLabel(2, replay = false))
        assertEquals("예시 둘러보기", OnboardingFlow.walkPrimaryLabel(3, replay = false))
        assertEquals("마치기", OnboardingFlow.walkPrimaryLabel(3, replay = true))
        assertTrue(OnboardingFlow.walkShowsBanner(replay = false))
        assertFalse(OnboardingFlow.walkShowsBanner(replay = true))
        assertTrue(OnboardingFlow.exampleInstalled(OnboardingState(exampleState = ExampleState.INSTALLED), example))
        assertFalse(OnboardingFlow.exampleInstalled(OnboardingState(exampleState = ExampleState.DELETED), example))
    }

    @Test
    fun `LIB-17 예시 둘러보기 rows come from the example document`() {
        val doc = ExampleProject.build()
        val ui = ExampleTourState.build(doc)
        assertEquals("잿빛 새벽의 등불", ui.title)
        // The project's own one-liner (the fixture's `ONE_LINER`; the board's line differs, Spec conflicts).
        assertEquals(doc.project.oneLiner, ui.oneLiner)
        assertTrue(ui.oneLiner!!.isNotBlank())
        assertEquals(listOf("스크립트 · 1-7 씬 3", "인물 · 리엔", "세계 · 오리지늄 등불"), ui.explore.map { it.title })
        assertEquals(
            listOf("대사 · 나레이션 · 연출 지시 · 변화 표식", "시점에 따라 바뀌는 필드와 관계", "용어 공개 시점과 점검"),
            ui.explore.map { it.sub },
        )
        assertEquals(
            listOf(Script(example, sceneKey = ExampleKeys.SCENE_1_7_3), CharacterProfile(example, ExampleKeys.RIEN), TermCard(example, ExampleKeys.TERM_ORIGINIUM_LANTERN)),
            ui.explore.map { it.route },
        )
        assertEquals("Ⅰ 1장 · 등불지기", ui.chapterHeader)
        assertEquals(
            listOf(listOf("1-6", "잿더미 시장", "완료"), listOf("1-7", "잿빛 새벽의 등불", "씬 4개"), listOf("1-8", "—", "미정")),
            ui.episodes.map { listOf(it.code, it.title, it.status) },
        )
    }

    private class FakeFinish(var state: OnboardingState, var library: List<LibraryEntry>) : FinishPorts {
        val log = mutableListOf<String>()
        var onShowLibrary: () -> Unit = {}
        var releaseDelayMs = 0L
        override fun onboarding() = state
        override suspend fun updateOnboarding(transform: (OnboardingState) -> OnboardingState) {
            state = transform(state)
            log += "state"
        }
        override fun exampleEntry() = library.firstOrNull { it.isExample && it.deletedAt == null }
        override suspend fun showLibrary() {
            log += "library"
            onShowLibrary()
        }
        override suspend fun releaseSessions(projectKey: Key) {
            log += "release $projectKey"
            delay(releaseDelayMs)
        }
        override suspend fun deleteExample(projectKey: Key) {
            check(library.any { it.key == projectKey }) { "no such project $projectKey" }
            log += "delete $projectKey"
            library = library.filterNot { it.key == projectKey }
        }
    }

    private val exampleEntry = ExampleProject.libraryEntry().copy(isExample = true, readOnly = true)

    @Test
    fun `P-07 온보딩 끝내기 marks finished, shows the 서재, then deletes the example for good`() = runTest {
        val ports = FakeFinish(OnboardingState(exampleState = ExampleState.INSTALLED, resumePage = "EXAMPLE"), listOf(exampleEntry))
        OnboardingFinisher(ports).finish()
        assertEquals(listOf("state", "library", "release ${exampleEntry.key}", "delete ${exampleEntry.key}"), ports.log)
        assertTrue(ports.state.firstRunCompleted)
        assertEquals(ExampleState.DELETED, ports.state.exampleState)
        assertNull(ports.state.resumePage)
        assertTrue(ports.library.isEmpty())
    }

    @Test
    fun `LIB-18 the 서재 shown by 온보딩 끝내기 and a second tap never delete the example twice`() = runTest {
        val ports = FakeFinish(OnboardingState(exampleState = ExampleState.INSTALLED), listOf(exampleEntry))
        ports.releaseDelayMs = 500
        val finisher = OnboardingFinisher(ports)
        // The new 서재 runs its leftover clean-up while the finish still waits for the store.
        ports.onShowLibrary = { launch { finisher.cleanUpLeftover() } }
        val first = launch { finisher.finish() }
        val second = launch { finisher.finish() }
        first.join()
        second.join()
        advanceUntilIdle()
        assertEquals(1, ports.log.count { it.startsWith("delete") })
        assertTrue(ports.library.isEmpty())
        assertEquals(ExampleState.DELETED, ports.state.exampleState)
    }

    @Test
    fun `LIB-18 a first run without the example finishes without a delete and claims none`() = runTest {
        val ports = FakeFinish(OnboardingState(), emptyList())
        OnboardingFinisher(ports).finish()
        assertEquals(listOf("state", "library"), ports.log)
        assertEquals(ExampleState.NOT_INSTALLED, ports.state.exampleState)
        assertTrue(ports.state.firstRunCompleted)
    }

    @Test
    fun `LIB-18 an example left by an interrupted finish is removed, never before onboarding ends`() = runTest {
        val running = FakeFinish(OnboardingState(exampleState = ExampleState.INSTALLED), listOf(exampleEntry))
        OnboardingFinisher(running).cleanUpLeftover()
        assertTrue(running.log.isEmpty())
        val cut = FakeFinish(OnboardingState(firstRunCompleted = true, exampleState = ExampleState.DELETED), listOf(exampleEntry))
        OnboardingFinisher(cut).cleanUpLeftover()
        assertEquals(listOf("release ${exampleEntry.key}", "delete ${exampleEntry.key}"), cut.log)
    }

    @Test
    fun `LIB-18 예시 프로젝트 삭제됨 is used up by the first real project`() {
        val deleted = OnboardingState(firstRunCompleted = true, exampleState = ExampleState.DELETED)
        val own = exampleEntry.copy(key = "01KOWN0000000000000000000", isExample = false, readOnly = false)
        assertFalse(OnboardingFinisher.noticeUsedUp(deleted, emptyList()))
        assertFalse(OnboardingFinisher.noticeUsedUp(deleted, listOf(exampleEntry)))
        assertTrue(OnboardingFinisher.noticeUsedUp(deleted, listOf(own)))
        assertFalse(OnboardingFinisher.noticeUsedUp(deleted.copy(emptyLibraryNoticeShown = true), listOf(own)))
    }

    @Test
    fun `LIB-21 replay rows read the board HelpReplay`() {
        val state = OnboardingState(walkthroughSeen = true, moduleTourSeen = mapOf("SCRIPT" to true, "CAST" to true, "RECORDER" to true))
        val ui = HelpReplayState.build(state, tipsOn = true)
        assertEquals("워크스루" to "3단계 · 봤음", ui.walkthrough.title to ui.walkthrough.sub)
        assertEquals(
            listOf(
                "스크립트 | 4단계 · 봤음", "인물 | 3단계 · 봤음", "세계 · 연표 | 3단계 · 아직 안 봄", "라이프 레코더 | 3단계 · 봤음",
                "서재 · 숨기기 | 3단계 · 아직 안 봄", "지도 만들기 | 3단계 · 아직 안 봄", "인물 동선 · 장소와 때 | 3단계 · 아직 안 봄",
            ),
            ui.modules.map { "${it.title} | ${it.sub}" },
        )
        assertTrue(ui.tipsOn)
        val reset = ModuleTours.reset(state, TourModule.SCRIPT)
        assertEquals("4단계 · 아직 안 봄", HelpReplayState.build(reset, tipsOn = false).modules.first().sub)
    }

    @Test
    fun `LIB-19 the card goes below the target, above when it does not fit, middle without one`() {
        fun place(top: Float?, bottom: Float?) = TourPlacement.of(top, bottom, 200f, hostHeight = 800f, cardHeight = 160f, cardLeft = 16f, cardWidth = 358f, gap = 12f, edge = 16f, arrowInset = 24f)
        val below = place(100f, 150f)
        assertEquals(TourPlacement.Arrow.TOP, below.arrow)
        assertEquals(162f, below.cardTop, 0.01f)
        assertEquals(184f, below.arrowX, 0.01f)
        val above = place(700f, 760f)
        assertEquals(TourPlacement.Arrow.BOTTOM, above.arrow)
        assertEquals(528f, above.cardTop, 0.01f)
        val none = place(null, null)
        assertEquals(TourPlacement.Arrow.NONE, none.arrow)
        assertEquals(320f, none.cardTop, 0.01f)
        assertEquals(24f, TourPlacement.of(0f, 10f, 0f, 800f, 100f, 16f, 358f, 12f, 16f, 24f).arrowX, 0.01f)
    }

    @Test
    fun `LIB-28 the recorder shows the badge the library's rule gives, also while open`() {
        Rating.entries.forEach { rating ->
            val masked = ProjectProfile("p", "검은 서약", rating, Protections(recorderLinkMask = true))
            assertEquals(LinkViews.ratingBadge(rating), ProjectSurfaces.recorderLinkLabel(masked, HiddenAccess()))
        }
        val plain = ProjectProfile("p", "유리 정원", Rating.R18, Protections())
        assertNull(ProjectSurfaces.recorderLinkLabel(plain))
        // The 프로젝트 설정 switch is the one the recorder reads (ProjectSnapshot.linkMask).
        val doc = LibFx.oath()
        val row = ProjectProtectionSettings.of(Rating.R18, doc.project.protections, RatingDefaults()).rows.single { it.protection == Protection.RECORDER_LINK_MASK }
        assertEquals(row.on, ProjectSnapshot.of(doc).linkMask)
        assertEquals("라이프 레코더 연결 가리기", row.title)
    }

    @Test
    fun `LIB-30 notification texts never name a hidden project`() {
        val visible = ProjectProfile("a", "잿빛 새벽의 등불", Rating.R15, Protections())
        val masked = ProjectProfile("b", "외전", Rating.R18, Protections(nameMask = true))
        val hidden = ProjectProfile("c", "검은 서약", Rating.R18, Protections(hidden = true, nameMask = true))
        assertEquals("백업 끝 · 프로젝트 2 · 숨긴 프로젝트 1", LibraryNotices.backupDone(listOf(visible, masked, hidden)))
        assertEquals("백업 끝 · 잿빛 새벽의 등불 · R-18 프로젝트", LibraryNotices.backupDone(listOf(visible, masked)))
        val restore = LibraryNotices.restoreDone(listOf(visible, hidden))
        assertTrue(restore.endsWith("프로젝트 1 · 숨긴 프로젝트 1"))
        assertTrue(LibraryNotices.exportDone(hidden).endsWith("숨긴 프로젝트 1"))
        assertTrue(LibraryNotices.exportDone(masked).endsWith("R-18 프로젝트"))
        listOf(restore, LibraryNotices.exportDone(hidden), LibraryNotices.backupDone(listOf(hidden))).forEach { assertFalse(it, it.contains("검은 서약")) }
    }
}
