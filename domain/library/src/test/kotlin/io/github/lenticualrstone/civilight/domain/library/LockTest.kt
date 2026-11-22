package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.lock.LockMode
import io.github.lenticualrstone.civilight.domain.library.lock.LockOutcome
import io.github.lenticualrstone.civilight.domain.library.lock.LockScreens
import io.github.lenticualrstone.civilight.domain.library.lock.Relock
import io.github.lenticualrstone.civilight.domain.library.lock.SessionEvent
import io.github.lenticualrstone.civilight.domain.library.masking.HiddenAccess
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** §4.3 lock screens (L-01 … L-06) and re-lock (RL-01 … RL-07), LIB-33…35. */
class LockTest {
    private val ash = ProjectProfile.of(Fixtures.example())
    private val oath = ProjectProfile.of(
        Fixtures.projectWith("검은 서약 (가제)", Rating.R18, Protections(hidden = true, captureBlock = true, nameMask = true, recorderLinkMask = true)),
    )
    private val r18Visible = ash.copy(rating = Rating.R18, protections = Protections(captureBlock = true, nameMask = true))

    @Test
    fun `L-01 app lock over the blurred manuscript`() {
        val screens = LockScreens.sequence(ash, appLock = true, relockDue = true)
        assertEquals(1, screens.size)
        val s = screens.single()
        assertEquals(LockMode.BLURRED_CONTENT, s.mode)
        assertEquals("Civilight", s.title)
        assertEquals("잠김 · 지문으로 열기", s.status)
        assertEquals(listOf("기기 PIN 사용"), s.buttons)
    }

    @Test
    fun `L-02 hidden project lock shows the mark only`() {
        val s = LockScreens.sequence(oath, appLock = false, relockDue = true).single()
        assertEquals(LockMode.MARK_ONLY_HIDDEN, s.mode)
        assertEquals("숨긴 프로젝트", s.title)
        assertEquals("잠김 · 지문으로 열기", s.status)
        assertEquals(listOf("PIN 사용", "서재로"), s.buttons)
    }

    @Test
    fun `L-03 app lock and hidden lock ask separately`() {
        val screens = LockScreens.sequence(oath, appLock = true, relockDue = true)
        assertEquals(listOf(LockMode.MARK_ONLY, LockMode.MARK_ONLY_HIDDEN), screens.map { it.mode })
        assertEquals(LockOutcome.Continue, LockScreens.afterAuth(success = true))
    }

    @Test
    fun `L-04 capture-blocked R-18 project shows no name`() {
        val s = LockScreens.sequence(r18Visible, appLock = true, relockDue = false).single()
        assertEquals(LockMode.MARK_ONLY, s.mode)
        assertEquals("Civilight", s.title)
        assertFalse(s.title.contains(r18Visible.name))
    }

    @Test
    fun `L-05 cancel goes to 서재 with the group locked`() {
        assertEquals(LockOutcome.GoToLibraryLocked, LockScreens.afterAuth(success = false))
        val access = HiddenAccess.LOCKED.openGroup(Fixtures.NOW).keepOpen(oath.key).relock()
        assertFalse(access.groupOpen)
        assertFalse(access.isProjectOpen(oath.key))
    }

    @Test
    fun `L-06 no pre-captured screen for hidden or capture-blocked projects`() {
        assertFalse(LockScreens.takeBlurSnapshot(oath))
        assertFalse(LockScreens.takeBlurSnapshot(r18Visible))
        assertTrue(LockScreens.takeBlurSnapshot(ash))
    }

    private val unlockedAt = Fixtures.at("2026-11-21T23:55:00Z")

    @Test
    fun `RL-01 on leave - backgrounded`() {
        assertTrue(Relock.isDue(RelockPolicy.ON_LEAVE, unlockedAt, listOf(SessionEvent.Backgrounded(unlockedAt + 1000)), Fixtures.NOW))
    }

    @Test
    fun `RL-02 on leave - screen off`() {
        assertTrue(Relock.isDue(RelockPolicy.ON_LEAVE, unlockedAt, listOf(SessionEvent.ScreenOff(unlockedAt + 1000)), Fixtures.NOW))
    }

    @Test
    fun `RL-03 screen off only - another app and back`() {
        val events = listOf(SessionEvent.Backgrounded(unlockedAt + 1000), SessionEvent.Foregrounded(unlockedAt + 2000))
        assertFalse(Relock.isDue(RelockPolicy.SCREEN_OFF_ONLY, unlockedAt, events, Fixtures.NOW))
    }

    @Test
    fun `RL-04 screen off only - screen off`() {
        assertTrue(Relock.isDue(RelockPolicy.SCREEN_OFF_ONLY, unlockedAt, listOf(SessionEvent.ScreenOff(unlockedAt + 1000)), Fixtures.NOW))
    }

    @Test
    fun `RL-05 five minutes - 08 55 to 09 00`() {
        assertTrue(Relock.isDue(RelockPolicy.FIVE_MINUTES, Fixtures.at("2026-11-21T23:55:00Z"), emptyList(), Fixtures.NOW))
    }

    @Test
    fun `RL-06 five minutes - 08 56 to 09 00 with the app backgrounded`() {
        val at = Fixtures.at("2026-11-21T23:56:00Z")
        assertFalse(Relock.isDue(RelockPolicy.FIVE_MINUTES, at, listOf(SessionEvent.Backgrounded(at + 1000)), Fixtures.NOW))
    }

    @Test
    fun `RL-07 다시 잠그기 relocks at once under any policy`() {
        for (policy in RelockPolicy.entries) {
            assertTrue(Relock.isDue(policy, unlockedAt, listOf(SessionEvent.RelockPressed(unlockedAt + 1)), unlockedAt + 2))
        }
    }

    @Test
    fun `events before the unlock do not count`() {
        assertFalse(Relock.isDue(RelockPolicy.ON_LEAVE, unlockedAt, listOf(SessionEvent.Backgrounded(unlockedAt - 1)), Fixtures.NOW))
        assertEquals("앱을 떠나면 잠김", Relock.openGroupNote(RelockPolicy.ON_LEAVE))
    }
}
