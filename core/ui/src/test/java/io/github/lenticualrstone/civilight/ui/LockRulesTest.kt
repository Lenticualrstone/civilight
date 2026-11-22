package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import io.github.lenticualrstone.civilight.ui.security.LockRules
import io.github.lenticualrstone.civilight.ui.security.LockScreenMode
import io.github.lenticualrstone.civilight.ui.security.RelockEvent
import io.github.lenticualrstone.civilight.ui.security.ScreenExposure
import io.github.lenticualrstone.civilight.ui.security.UnlockController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

/** Library spec §4.3: lock screens L-01 … L-06 and re-lock RL-01 … RL-07. */
class LockRulesTest {
    private val normal = ScreenExposure()
    private val hidden = ScreenExposure(hidden = true, captureBlock = true)
    private val r18 = ScreenExposure(captureBlock = true)

    private fun at(time: String): Long = OffsetDateTime.parse("2026-11-22T$time:00+09:00").toInstant().toEpochMilli()

    @Test
    fun `L-01 normal project with app lock shows the blurred content`() {
        assertEquals(listOf(LockScreenMode.BLURRED_CONTENT), LockRules.lockScreens(normal, appLockDue = true, hiddenRelockDue = false))
    }

    @Test
    fun `L-02 hidden project without app lock shows the hidden mark-only lock`() {
        assertEquals(listOf(LockScreenMode.MARK_ONLY_HIDDEN), LockRules.lockScreens(hidden, appLockDue = false, hiddenRelockDue = true))
    }

    @Test
    fun `L-03 hidden project with app lock asks twice, app lock first and mark only`() {
        assertEquals(
            listOf(LockScreenMode.MARK_ONLY, LockScreenMode.MARK_ONLY_HIDDEN),
            LockRules.lockScreens(hidden.copy(captureBlock = false), appLockDue = true, hiddenRelockDue = true),
        )
    }

    @Test
    fun `L-04 capture-blocked R-18 project gets the mark only`() {
        assertEquals(listOf(LockScreenMode.MARK_ONLY), LockRules.lockScreens(r18, appLockDue = true, hiddenRelockDue = false))
    }

    @Test
    fun `L-05 a cancelled hidden lock leaves the group locked`() {
        var policy = RelockPolicy.ON_LEAVE
        val controller = UnlockController(now = { at("09:00") }, policy = { policy }, appLockEnabled = { false })
        controller.unlockHiddenGroup()
        controller.lockHiddenGroup()
        assertFalse(controller.state.value.hiddenGroupOpen)
    }

    @Test
    fun `L-06 no blur snapshot for hidden or capture-blocked projects`() {
        assertFalse(LockRules.takeBlurSnapshot(hidden))
        assertFalse(LockRules.takeBlurSnapshot(r18))
        assertTrue(LockRules.takeBlurSnapshot(normal))
        assertTrue(LockRules.lockScreens(normal, appLockDue = false, hiddenRelockDue = true).isEmpty())
    }

    @Test
    fun `RL-01 to RL-07 re-lock table`() {
        val t = at("08:55")
        val now = at("09:00")
        assertTrue(LockRules.relockDue(RelockPolicy.ON_LEAVE, t, setOf(RelockEvent.BACKGROUNDED), now))
        assertTrue(LockRules.relockDue(RelockPolicy.ON_LEAVE, t, setOf(RelockEvent.SCREEN_OFF), now))
        assertFalse(LockRules.relockDue(RelockPolicy.SCREEN_OFF_ONLY, t, setOf(RelockEvent.BACKGROUNDED), now))
        assertTrue(LockRules.relockDue(RelockPolicy.SCREEN_OFF_ONLY, t, setOf(RelockEvent.SCREEN_OFF), now))
        assertTrue(LockRules.relockDue(RelockPolicy.FIVE_MINUTES, at("08:55"), emptySet(), now))
        assertFalse(LockRules.relockDue(RelockPolicy.FIVE_MINUTES, at("08:56"), setOf(RelockEvent.BACKGROUNDED), now))
        for (policy in RelockPolicy.entries) assertTrue(LockRules.relockDue(policy, now, setOf(RelockEvent.RELOCK_PRESSED), now))
        assertFalse(LockRules.relockDue(RelockPolicy.ON_LEAVE, null, setOf(RelockEvent.BACKGROUNDED), now))
    }

    @Test
    fun `the controller applies the policy to lifecycle events`() {
        var now = at("08:56")
        var policy = RelockPolicy.SCREEN_OFF_ONLY
        var appLock = true
        val c = UnlockController(now = { now }, policy = { policy }, appLockEnabled = { appLock })
        c.onStart()
        assertTrue(c.state.value.appLocked)
        c.appUnlocked()
        c.unlockHiddenGroup()
        c.unlockItems("P")
        c.onBackground()
        // SCREEN_OFF_ONLY: another app does not re-lock hidden things, but the app lock comes back.
        assertTrue(c.state.value.hiddenGroupOpen)
        assertTrue(c.state.value.itemsOpen("P"))
        assertTrue(c.state.value.appLocked)
        c.onScreenOff()
        assertFalse(c.state.value.hiddenGroupOpen)
        assertFalse(c.state.value.itemsOpen("P"))

        policy = RelockPolicy.FIVE_MINUTES
        appLock = false
        c.appUnlocked()
        c.unlockRecords()
        now = at("09:00")
        c.tick()
        assertTrue(c.state.value.recordsOpen)
        now = at("09:01")
        c.tick()
        assertFalse(c.state.value.recordsOpen)

        policy = RelockPolicy.ON_LEAVE
        c.unlockHiddenGroup()
        c.relockNow()
        assertFalse(c.state.value.anythingOpen)
        assertTrue(c.state.value.events.isEmpty())
    }

    // R19 §2 "지금은 열린 채": a project hidden in this session stays open until the next re-lock.

    @Test
    fun `kept open project opens without the group and locks on ON_LEAVE background`() {
        val now = at("09:00")
        val c = UnlockController(now = { now }, policy = { RelockPolicy.ON_LEAVE }, appLockEnabled = { false })
        c.keepOpen("P1")
        assertTrue(c.state.value.projectOpen("P1"))
        assertFalse(c.state.value.projectOpen("P2"))
        assertFalse(c.state.value.hiddenGroupOpen)
        c.onBackground()
        c.onForeground()
        assertFalse(c.state.value.projectOpen("P1"))
        assertFalse(c.state.value.anythingOpen)
    }

    @Test
    fun `kept open project survives leaving under SCREEN_OFF_ONLY and locks on screen off`() {
        val c = UnlockController(now = { at("09:00") }, policy = { RelockPolicy.SCREEN_OFF_ONLY }, appLockEnabled = { false })
        c.keepOpen("P1")
        c.onBackground()
        c.onForeground()
        assertTrue(c.state.value.projectOpen("P1"))
        c.onScreenOff()
        assertFalse(c.state.value.projectOpen("P1"))
    }

    @Test
    fun `kept open project locks five minutes after it was kept open`() {
        var now = at("09:00")
        val c = UnlockController(now = { now }, policy = { RelockPolicy.FIVE_MINUTES }, appLockEnabled = { false })
        c.keepOpen("P1")
        now = at("09:04")
        c.tick()
        assertTrue(c.state.value.projectOpen("P1"))
        now = at("09:05")
        c.tick()
        assertFalse(c.state.value.projectOpen("P1"))
    }

    @Test
    fun `relock now and a cancelled hidden prompt both clear the kept open set`() {
        val c = UnlockController(now = { at("09:00") }, policy = { RelockPolicy.SCREEN_OFF_ONLY }, appLockEnabled = { false })
        c.keepOpen("P1")
        c.relockNow()
        assertTrue(c.state.value.keptOpen.isEmpty())
        c.keepOpen("P2")
        c.lockHiddenGroup()
        assertTrue(c.state.value.keptOpen.isEmpty())
        assertTrue(c.state.value.events.isEmpty())
    }

    @Test
    fun `an open group opens every hidden project, kept open or not`() {
        val c = UnlockController(now = { at("09:00") }, policy = { RelockPolicy.SCREEN_OFF_ONLY }, appLockEnabled = { false })
        c.unlockHiddenGroup()
        assertTrue(c.state.value.projectOpen("any"))
    }
}
