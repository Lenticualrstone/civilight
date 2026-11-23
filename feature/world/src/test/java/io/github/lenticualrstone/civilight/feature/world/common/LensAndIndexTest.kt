package io.github.lenticualrstone.civilight.feature.world.common

import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.K
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.T1
import io.github.lenticualrstone.civilight.model.position.LensPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-12 past-lens banner and the shared candidate index (WLD-26 groundwork). */
@OptIn(ExperimentalCoroutinesApi::class)
class LensAndIndexTest {
    @Test
    fun `WLD-12 no banner at 최신 or in a read-only project`() {
        val doc = WorldTestFixture.stateB()
        assertNull(LensBanner.of(doc, readOnly = false))
        assertNull(LensBanner.of(WorldTestFixture.atLens(doc, K.episode(1, 5)), readOnly = true))
    }

    @Test
    fun `WLD-12 a past lens shows the LensEdit banner with the point`() {
        val doc = WorldTestFixture.atLens(WorldTestFixture.stateB(), K.episode(1, 5))
        assertEquals(LensBannerUi("1-5 시점 값 고치는 중", "고치면 1-5부터 적용"), LensBanner.of(doc, readOnly = false))
    }

    @Test
    fun `WLD-12 a lens on a trashed unit reads as 최신`() {
        val doc = WorldTestFixture.stateB()
        val trashed = doc.copy(app = doc.app.copy(view = doc.app.view.copy(lens = doc.app.view.lens.copy(point = LensPoint.UnitEnd("NOPE")))))
        assertNull(LensBanner.of(trashed, readOnly = false))
    }

    @Test
    fun `a scan input ignores lens and app state but not term or text changes`() {
        val doc = WorldTestFixture.stateB()
        val input = ScanInput(doc, TermAccess(false))
        assertTrue(input.sameText(ScanInput(WorldTestFixture.atLens(doc, K.episode(1, 5)), TermAccess(false))))
        assertFalse(input.sameText(ScanInput(doc, TermAccess(true))))
        assertFalse(input.sameText(ScanInput(WorldTestFixture.stateA(), TermAccess(false))))
    }

    @Test
    fun `one index per project, shared by its screens and dropped after the last release`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val a = TermIndexes.acquire("PX", scope, dispatcher)
        val b = TermIndexes.acquire("PX", scope, dispatcher)
        assertTrue(a.index.value.let { !it.complete })
        a.submit(WorldTestFixture.stateA(), TermAccess(false))
        scope.advanceUntilIdle()
        assertTrue(a.index.value.complete)
        assertEquals(a.index.value, b.index.value)
        assertEquals(1, a.index.value.of(T1).size)
        a.release()
        a.release()
        b.release()
        val c = TermIndexes.acquire("PX", scope, dispatcher)
        assertFalse("a new holder starts pending", c.index.value.complete)
        c.release()
    }

    @Test
    fun `WLD-73 with 용어 후보 off the index reads empty and complete, and turning it on scans again`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val h = TermIndexes.acquire("POFF", scope, dispatcher)
        val on = WorldTestFixture.stateA()
        val off = on.copy(project = on.project.copy(termCandidates = false))
        h.submit(off, TermAccess(false))
        scope.advanceUntilIdle()
        assertTrue("nothing waits on a scan that never runs", h.index.value.complete)
        assertEquals(0, h.index.value.of(T1).size)
        h.submit(on, TermAccess(false))
        scope.advanceUntilIdle()
        assertTrue(h.index.value.complete)
        assertEquals(1, h.index.value.of(T1).size)
        h.release()
    }
}
