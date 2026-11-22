package io.github.lenticualrstone.civilight.feature.world.common

import io.github.lenticualrstone.civilight.domain.world.common.DocRuns
import io.github.lenticualrstone.civilight.domain.world.term.ScriptSpot
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.K
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.T1
import io.github.lenticualrstone.civilight.model.text.TextRun
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-26: when the candidate index is refreshed (rule 21 §7, WLD-T47, T72). */
@OptIn(ExperimentalCoroutinesApi::class)
class RescanPlanTest {
    private val access = TermAccess()
    private val b12 = ScriptSpot.BlockText(K.BLOCK_B12)

    private fun typed(text: String) = DocRuns.updateBlock(WorldTestFixture.stateA(), K.BLOCK_B12) { it.copy(runs = listOf(TextRun.Plain(text))) }!!

    @Test
    fun `WLD-T72 the first input of a project is a full scan`() {
        assertEquals(RescanPlan.Full, RescanPlans.of(null, ScanInput(WorldTestFixture.stateA(), access)))
    }

    @Test
    fun `WLD-T47 typing in b12 rescans that block only`() {
        val a = WorldTestFixture.stateA()
        val plan = RescanPlans.of(ScanInput(a, access), ScanInput(typed("등불 속 그 돌, 오리… 뭐라고 했죠?"), access))
        assertEquals(RescanPlan.Incremental(emptySet(), setOf(b12)), plan)
    }

    @Test
    fun `WLD-T47 an alias change sweeps that term only`() {
        val a = WorldTestFixture.stateA()
        val plan = RescanPlans.of(ScanInput(a, access), ScanInput(WorldTestFixture.stateB(), access))
        assertEquals(RescanPlan.Incremental(setOf(T1), emptySet()), plan)
    }

    @Test
    fun `lens, app state and rejections change nothing, unlocking hidden terms is a full scan`() {
        val a = WorldTestFixture.stateA()
        val atLens = WorldTestFixture.atLens(a, K.episode(1, 5))
        assertEquals(RescanPlan.None, RescanPlans.of(ScanInput(a, access), ScanInput(atLens, access)))
        val candidate = WorldTestFixture.index(a).of(T1).single()
        val rejected = TermEdits.reject(a, candidate, WorldTestFixture.ctx())
        assertEquals(RescanPlan.None, RescanPlans.of(ScanInput(a, access), ScanInput(rejected, access)))
        assertEquals(RescanPlan.Full, RescanPlans.of(ScanInput(a, access), ScanInput(a, TermAccess(true))))
    }

    @Test
    fun `WLD-25 turning 글자 찾기 off is a term change`() {
        val a = WorldTestFixture.stateA()
        val off = TermEdits.setTextSearch(a, T1, false, WorldTestFixture.ctx())
        assertEquals(RescanPlan.Incremental(setOf(T1), emptySet()), RescanPlans.of(ScanInput(a, access), ScanInput(off, access)))
    }

    @Test
    fun `WLD-26 the shared index rescans a typed block after the pause and a term at once`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val handle = TermIndexes.acquire("PR", scope, dispatcher, debounceMs = 500)
        handle.submit(WorldTestFixture.stateA(), access)
        scope.advanceUntilIdle()
        assertTrue(handle.index.value.complete)
        assertEquals(listOf("오리지"), handle.index.value.of(T1).map { it.matchedText })

        handle.submit(typed("등불 속 그 돌, 오리… 뭐라고 했죠?"), access)
        scope.runCurrent()
        scope.advanceTimeBy(300)
        scope.runCurrent()
        assertEquals("not before the pause", listOf("오리지"), handle.index.value.of(T1).map { it.matchedText })
        scope.advanceTimeBy(300)
        scope.runCurrent()
        assertEquals(listOf("오리"), handle.index.value.of(T1).map { it.matchedText })
        assertTrue("a block rescan keeps the index final", handle.index.value.complete)

        // Adding the alias 빛돌 sweeps T1 at once (state B, WLD-T10).
        val withAlias = WorldTestFixture.withTerm(typed("등불 속 그 돌, 오리… 뭐라고 했죠?"), T1) { it.copy(aliases = listOf("오리지늄", "빛돌")) }
        handle.submit(withAlias, access)
        scope.runCurrent()
        assertEquals(3, handle.index.value.of(T1).size)
        handle.release()
    }
}
