package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.ui.project.RefCountedSessions
import io.github.lenticualrstone.civilight.ui.scaffold.ToastController
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
        val toasts = ToastController(scope, durationMs = 6_000)
        var undone = 0
        toasts.show("블록 #043 지움", onAction = { undone++ })
        assertEquals("되돌리기", toasts.current.value?.actionLabel)
        advanceTimeBy(3_000)
        toasts.show("인물 노아 지움", onAction = { undone += 10 })
        advanceTimeBy(4_000)
        runCurrent()
        assertEquals("인물 노아 지움", toasts.current.value?.text)
        toasts.act()
        assertEquals(10, undone)
        assertNull(toasts.current.value)

        toasts.show("한 줄 안내", actionLabel = null)
        assertNull(toasts.current.value?.actionLabel)
        advanceTimeBy(6_001)
        runCurrent()
        assertNull(toasts.current.value)
        assertFalse(undone == 0)
    }
}
