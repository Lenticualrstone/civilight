package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.masking.HiddenAccess
import io.github.lenticualrstone.civilight.feature.library.LibFx.NOW
import io.github.lenticualrstone.civilight.feature.library.LibFx.ZONE
import io.github.lenticualrstone.civilight.feature.library.LibFx.at
import io.github.lenticualrstone.civilight.feature.library.LibFx.shelf
import io.github.lenticualrstone.civilight.feature.library.rating.RatingPickSheet
import io.github.lenticualrstone.civilight.feature.library.shelf.RenameSheet
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfMenuSheet
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfProject
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfState
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfUi
import io.github.lenticualrstone.civilight.model.app.ExampleState
import io.github.lenticualrstone.civilight.model.app.OnboardingState
import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import io.github.lenticualrstone.civilight.model.app.UnlockSession
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.navigation.ExportSheet
import io.github.lenticualrstone.civilight.navigation.RatingChangeSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 서재 (LIB-01 … LIB-03) row and group rules through [ShelfState], spec §4.2 S-01 and §4.9 P-07, P-09 … P-11. */
class ShelfStateTest {
    private val ashen = LibFx.ashen()
    private val glass = LibFx.glass()
    private val oath = LibFx.oath()
    private val gaiden = LibFx.gaiden()

    private val projects = listOf(
        shelf(glass, at("2026-11-19T10:00")),
        shelf(oath, at("2026-11-02T10:00")),
        shelf(ashen, at("2026-11-22T08:12")),
        shelf(gaiden, at("2026-11-15T10:00")),
    )

    private val locked = HiddenAccess()
    private val open = HiddenAccess(UnlockSession(hiddenGroupUnlockedAt = NOW))

    private fun build(
        list: List<ShelfProject> = projects,
        access: HiddenAccess = locked,
        recorder: RecorderLibrary = RecorderLibrary(),
        onboarding: OnboardingState = OnboardingState(firstRunCompleted = true),
    ): ShelfUi = ShelfState.build(list, access, RelockPolicy.ON_LEAVE, recorder, onboarding, NOW, ZONE)

    @Test
    fun `S-01 rows are visible projects newest first, hidden ones completely absent from main shelf`() {
        val ui = build()
        assertEquals("프로젝트 · 2", ui.sectionLabel)
        assertEquals(listOf("잿빛 새벽의 등불", "유리 정원"), ui.rows.map { it.title })
        assertEquals(listOf(Rating.R15, Rating.ALL), ui.rows.map { it.rating })
        assertNull(ui.lockedGroup)
        assertNull(ui.openGroup)
        assertFalse(ui.headerRelock)
    }

    @Test
    fun `P-09 to P-11 row meta by format`() {
        val ui = build(access = open)
        val byTitle = ui.rows.associate { it.title to it.meta }
        // The example has 11 live characters (fixture.md conflict 7: 노아 is in the trash).
        assertEquals("1장 7/10 · 인물 11 · 오늘 08:12", byTitle["잿빛 새벽의 등불"])
        assertEquals("단편 · 씬 1개 · 3일 전", byTitle["유리 정원"])
        assertNull("hidden project stays off main shelf", byTitle["검은 서약 (가제)"])
    }

    @Test
    fun `쓰는 중 marks the project with an episode being written`() {
        val ui = build()
        assertTrue(ui.rows.single { it.title == "잿빛 새벽의 등불" }.writing)
        assertFalse(ui.rows.single { it.title == "유리 정원" }.writing)
    }

    @Test
    fun `hidden projects remain off main shelf even when vault session is unlocked`() {
        val ui = build(access = open)
        assertNull(ui.openGroup)
        assertNull(ui.lockedGroup)
        assertFalse(ui.headerRelock)
        assertEquals("프로젝트 · 2", ui.sectionLabel)
        assertEquals(listOf("잿빛 새벽의 등불", "유리 정원"), ui.rows.map { it.title })
    }

    @Test
    fun `a hidden project whose document is not read yet still never becomes a row`() {
        val unread = projects.map { if (it.doc === oath) it.copy(doc = null) else it }
        val ui = build(unread)
        assertFalse(ui.rows.any { it.key == oath.key })
        assertNull(ui.lockedGroup)
    }

    @Test
    fun `the bundled example is not a 서재 row`() {
        val ui = build(listOf(shelf(ashen, at("2026-11-22T08:12"), isExample = true), shelf(glass, at("2026-11-19T10:00"))))
        assertEquals(listOf("유리 정원"), ui.rows.map { it.title })
        assertEquals("프로젝트 · 1", ui.sectionLabel)
    }

    @Test
    fun `P-07 empty 서재 after onboarding`() {
        val ui = build(list = emptyList(), onboarding = OnboardingState(firstRunCompleted = true, exampleState = ExampleState.DELETED))
        val empty = assertNotNull(ui.empty).let { ui.empty!! }
        assertEquals("프로젝트 · 0", ui.sectionLabel)
        assertEquals("프로젝트 없음", empty.title)
        assertEquals("프로젝트 = 원고 · 인물 · 세계 · 페이지 한 묶음", empty.sub)
        assertEquals(listOf("새 프로젝트", "백업에서 되살리기"), empty.buttons)
        assertEquals("예시 프로젝트 삭제됨", empty.notice)
        assertEquals("기록 0", ui.recorderSub)
        assertNull(build().empty)
    }

    @Test
    fun `라이프 레코더 row counts records`() {
        val ui = build(recorder = ExampleRecorder.library())
        assertTrue(ui.recorderSub, ui.recorderSub.matches(Regex("""기록 \d+ · 정리 안 됨 \d+""")))
    }

    @Test
    fun `sheets and the delete dialog show only projects the 서재 shows`() {
        val lockedUi = build()
        assertTrue(lockedUi.shows(glass.key))
        assertFalse(lockedUi.shows(oath.key))
        assertFalse("hidden project is shown only in Settings vault", build(access = open).shows(oath.key))

        // Hidden project sheets close on the main shelf.
        val stack = listOf(ShelfMenuSheet(glass.key), ShelfMenuSheet(oath.key), RenameSheet(oath.key))
        assertEquals(2, ShelfState.sheetsToClose(lockedUi, stack))
        assertEquals(2, ShelfState.sheetsToClose(build(access = open), stack))
        assertEquals(1, ShelfState.sheetsToClose(lockedUi, listOf(RatingPickSheet(oath.key))))
        assertEquals(1, ShelfState.sheetsToClose(lockedUi, listOf(ExportSheet(oath.key))))
        // The shared rating sheet closes itself, once.
        assertEquals(0, ShelfState.sheetsToClose(lockedUi, listOf(RatingChangeSheet(oath.key, Rating.R15))))
        // Before the index is read nothing is closed (a restored sheet waits for its row).
        assertEquals(0, ShelfState.sheetsToClose(ShelfUi(), stack))
    }
}
