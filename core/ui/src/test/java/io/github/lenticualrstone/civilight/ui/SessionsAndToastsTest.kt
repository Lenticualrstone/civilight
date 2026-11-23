package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.ui.project.RefCountedSessions
import io.github.lenticualrstone.civilight.ui.scaffold.NoticeKind
import io.github.lenticualrstone.civilight.ui.scaffold.NoticeRules
import io.github.lenticualrstone.civilight.ui.scaffold.ToastController
import io.github.lenticualrstone.civilight.ui.scaffold.ToastHold
import io.github.lenticualrstone.civilight.ui.security.UnlockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionsAndToastsTest {
    private class Opened(val key: String)

    @Test
    fun `one open per key, closed after the last release and a delay`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val opened = mutableListOf<String>()
        val closed = mutableListOf<String>()
        val sessions = RefCountedSessions<String, Opened>(scope, { opened += it; Opened(it) }, { k, _ -> closed += k }, closeDelayMs = 1_000)

        val a = sessions.acquire("P")
        val b = sessions.acquire("P")
        assertSame(a, b)
        assertEquals(listOf("P"), opened)
        assertEquals(2, sessions.users("P"))

        sessions.release("P")
        sessions.release("P")
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        assertTrue(closed.isEmpty())
        // Coming back before the delay keeps the same value open.
        val c = sessions.acquire("P")
        assertSame(a, c)
        advanceTimeBy(5_000)
        runCurrent()
        assertTrue(closed.isEmpty())

        sessions.release("P")
        runCurrent()
        advanceTimeBy(1_001)
        runCurrent()
        assertEquals(listOf("P"), closed)
        assertNull(sessions.peek("P"))
        sessions.acquire("P")
        assertEquals(listOf("P", "P"), opened)
    }

    @Test
    fun `a failed open is retried by the next acquire`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        var fail = true
        val sessions = RefCountedSessions<String, Opened>(scope, { if (fail) error("unreadable") else Opened(it) }, { _, _ -> })
        val first = runCatching { sessions.acquire("P") }
        assertTrue(first.isFailure)
        assertEquals(0, sessions.users("P"))
        fail = false
        assertEquals("P", sessions.acquire("P").key)
    }

    @Test
    fun `toasts replace each other and leave after their time`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val toasts = ToastController(scope, now = { testScheduler.currentTime })
        var undone = 0
        toasts.show("블록 #043 지움", onAction = { undone++ })
        assertEquals("되돌리기", toasts.current.value?.actionLabel)
        advanceTimeBy(3_000)
        toasts.show("인물 노아 지움", onAction = { undone += 10 })
        // The replacing toast starts its own full 4 s.
        advanceTimeBy(3_000)
        runCurrent()
        assertEquals("인물 노아 지움", toasts.current.value?.text)
        toasts.act()
        assertEquals(10, undone)
        assertNull(toasts.current.value)

        toasts.show("한 줄 안내", actionLabel = null)
        assertNull(toasts.current.value?.actionLabel)
        advanceTimeBy(2_501)
        runCurrent()
        assertNull(toasts.current.value)
        assertFalse(undone == 0)
    }

    @Test
    fun `T-DS-40 durations per kind`() = runTest {
        assertEquals(2_500L, NoticeRules.durationMs(NoticeKind.INFO))
        assertEquals(4_000L, NoticeRules.durationMs(NoticeKind.ACTION))
        assertEquals(4_000L, NoticeRules.durationMs(NoticeKind.ERROR))
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val toasts = ToastController(scope, now = { testScheduler.currentTime })

        // A plain show is INFO even with the default label (the label only counts with an action).
        toasts.show("Drive 연결 준비 중", actionLabel = null)
        assertEquals(NoticeKind.INFO, toasts.current.value?.kind)
        toasts.show("Drive 연결 준비 중")
        assertEquals(NoticeKind.INFO, toasts.current.value?.kind)
        assertNull(toasts.current.value?.actionLabel)
        advanceTimeBy(2_499)
        runCurrent()
        assertEquals("Drive 연결 준비 중", toasts.current.value?.text)
        advanceTimeBy(2)
        runCurrent()
        assertNull(toasts.current.value)

        toasts.show("블록 #043 지움 · ◆ 1개는 #044 자리로", "되돌리기") {}
        assertEquals(NoticeKind.ACTION, toasts.current.value?.kind)
        advanceTimeBy(3_999)
        runCurrent()
        assertEquals(NoticeKind.ACTION, toasts.current.value?.kind)
        advanceTimeBy(2)
        runCurrent()
        assertNull(toasts.current.value)

        toasts.showError("가져오지 못함")
        assertEquals(NoticeKind.ERROR, toasts.current.value?.kind)
        assertNull(toasts.current.value?.actionLabel)
        advanceTimeBy(4_001)
        runCurrent()
        assertNull(toasts.current.value)
    }

    @Test
    fun `T-DS-40 a held press keeps the time left`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val toasts = ToastController(scope, now = { testScheduler.currentTime })
        toasts.show("Drive 연결 준비 중", actionLabel = null)
        advanceTimeBy(1_000)
        runCurrent()
        toasts.pause(ToastHold.PRESSED)
        advanceTimeBy(1_000)
        runCurrent()
        toasts.resume(ToastHold.PRESSED)
        // 1 000 ms shown + 1 000 ms held: 1 500 ms left, gone 3 500 ms after it appeared.
        advanceTimeBy(1_499)
        runCurrent()
        assertEquals("Drive 연결 준비 중", toasts.current.value?.text)
        advanceTimeBy(2)
        runCurrent()
        assertNull(toasts.current.value)

        // Two holds: the clock runs again only when both are released (app stopped while pressed).
        toasts.show("한 줄 안내", actionLabel = null)
        toasts.pause(ToastHold.PRESSED)
        toasts.pause(ToastHold.APP_STOPPED)
        advanceTimeBy(10_000)
        runCurrent()
        toasts.resume(ToastHold.PRESSED)
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals("한 줄 안내", toasts.current.value?.text)
        toasts.resume(ToastHold.APP_STOPPED)
        advanceTimeBy(2_501)
        runCurrent()
        assertNull(toasts.current.value)
    }

    @Test
    fun `accessibility timeout only lengthens`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val toasts = ToastController(scope, now = { testScheduler.currentTime })
        toasts.recommendedTimeout = { base, action -> if (action) base * 3 else base / 2 }
        assertEquals(12_000L, toasts.durationOf(NoticeKind.ACTION, hasAction = true))
        assertEquals(2_500L, toasts.durationOf(NoticeKind.INFO, hasAction = false))
    }

    @Test
    fun `T-DS-40 locks and releases dismiss at once, a pop does not`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val toasts = ToastController(scope, now = { testScheduler.currentTime })
        var top: String? = "P1"
        toasts.contextKey = { top }

        toasts.show("페이지 지움", "되돌리기") {}
        assertEquals("P1", toasts.current.value?.projectKey)
        // The page screen popped, the project stays on the back stack: the undo stays.
        toasts.dismissIfReleased(setOf("P1"))
        assertEquals("페이지 지움", toasts.current.value?.text)
        assertFalse(NoticeRules.dismissOn(NoticeRules.Event.SCREEN_POPPED))
        // The project left the back stack: gone, with no exit motion.
        val before = toasts.instantDismissals.value
        toasts.dismissIfReleased(setOf("P2"))
        assertNull(toasts.current.value)
        assertEquals(before + 1, toasts.instantDismissals.value)

        // An info toast raised in a project stays its time when that project closes (only an undo cannot apply).
        toasts.show("Drive 연결 준비 중", actionLabel = null)
        toasts.dismissIfReleased(emptySet())
        assertEquals("Drive 연결 준비 중", toasts.current.value?.text)

        top = null
        toasts.show("한 줄 안내", actionLabel = null)
        toasts.dismissIfReleased(emptySet())
        assertEquals("한 줄 안내", toasts.current.value?.text)
        toasts.dismissNow()
        assertNull(toasts.current.value)

        assertTrue(NoticeRules.instant(NoticeRules.Event.APP_LOCK))
        assertTrue(NoticeRules.instant(NoticeRules.Event.RELOCK))
        assertTrue(NoticeRules.instant(NoticeRules.Event.PROJECT_RELEASED))
        assertFalse(NoticeRules.dismissOn(NoticeRules.Event.SCREEN_CHANGED))
    }

    @Test
    fun `relocks are seen as tightened locks`() {
        val open = UnlockState(hiddenGroupUnlockedAt = 1L, itemsUnlockedAt = mapOf("P" to 1L), recordsUnlockedAt = 1L, keptOpenSince = mapOf("Q" to 1L))
        assertFalse(NoticeRules.locksTightened(open, open))
        assertTrue(NoticeRules.locksTightened(open, open.copy(appLocked = true)))
        assertTrue(NoticeRules.locksTightened(open, open.copy(hiddenGroupUnlockedAt = null)))
        assertTrue(NoticeRules.locksTightened(open, open.copy(itemsUnlockedAt = emptyMap())))
        assertTrue(NoticeRules.locksTightened(open, open.copy(recordsUnlockedAt = null)))
        assertTrue(NoticeRules.locksTightened(open, open.copy(keptOpenSince = emptyMap())))
        // Opening more is not a relock.
        assertFalse(NoticeRules.locksTightened(UnlockState(), open))
        assertFalse(NoticeRules.locksTightened(open.copy(appLocked = true), open))
    }
}
