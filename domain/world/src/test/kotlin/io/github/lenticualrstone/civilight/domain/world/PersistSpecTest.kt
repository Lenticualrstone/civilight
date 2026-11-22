package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.K
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.T1
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.ctx
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.queries
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.stateB
import io.github.lenticualrstone.civilight.domain.world.term.AliasEdit
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.RescanScheduler
import io.github.lenticualrstone.civilight.domain.world.term.ScanReport
import io.github.lenticualrstone.civilight.domain.world.term.ScriptSpot
import io.github.lenticualrstone.civilight.domain.world.term.TermCheckRow
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.domain.world.term.TermQueries
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD §4.5 persistence and rescans (WLD-T46, WLD-T47, WLD-T72). */
@OptIn(ExperimentalCoroutinesApi::class)
class PersistSpecTest {
    private fun rejectedB12(doc: ProjectDoc): ProjectDoc {
        val c = queries(doc).candidates(T1).single { it.spot == ScriptSpot.BlockText(K.BLOCK_B12) }
        return TermEdits.reject(doc, c, ctx())
    }

    private fun keys(e: JsonElement): Set<String> = when (e) {
        is JsonObject -> e.keys + e.values.flatMap(::keys)
        is JsonArray -> e.flatMap(::keys).toSet()
        else -> emptySet()
    }

    @Test
    fun `WLD-T46 file round trip`() {
        val doc = rejectedB12(stateB())
        val texts = ProjectFileSet.split(doc).encode().mapValues { (_, e) -> CivilightJson.json.encodeToString(JsonElement.serializer(), e) }
        val world = CivilightJson.json.parseToJsonElement(texts.getValue(PackagePaths.WORLD)).jsonObject
        val t1 = world.getValue("terms").jsonArray.single { it.jsonObject.getValue("key").jsonPrimitive.content == T1 }.jsonObject
        assertEquals(listOf("오리지늄", "빛돌"), t1.getValue("aliases").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(JsonPrimitive(true), t1.getValue("textSearch"))
        assertEquals(1, t1.getValue("rejections").jsonArray.size)
        // No candidate list or count is stored anywhere.
        val allKeys = texts.values.flatMap { keys(CivilightJson.json.parseToJsonElement(it)) }.toSet()
        assertTrue(allKeys.none { it.contains("candidate", ignoreCase = true) })

        val decoded = ProjectFileSet.decode(texts.mapValues { (_, t) -> CivilightJson.json.parseToJsonElement(t) })
        assertTrue(decoded.failedPaths.isEmpty())
        val reopened = decoded.files.toDoc(app = doc.app)
        assertEquals(doc.world, reopened.world)
        // After opening, `점검 중` until the full scan ends; then b32 and b61 (b12 stays suppressed).
        assertTrue(TermQueries(reopened, CandidateIndex.pending()).checks().scanning)
        val q = queries(reopened)
        assertEquals(listOf(ScriptSpot.BlockText(K.BLOCK_B32), ScriptSpot.BlockText(K.BLOCK_B61)), q.candidates(T1).map { it.spot })
        assertFalse(q.checks().scanning)
    }

    @Test
    fun `WLD-T47 rescan scope`() = runTest {
        var doc = stateB()
        val reports = ArrayList<ScanReport>()
        val scheduler = RescanScheduler(
            scope = this, current = { doc }, initial = CandidateIndex.build(doc),
            onScanned = { _, r -> reports += r },
        )
        doc = WorldFixture.withBlockText(doc, K.BLOCK_B12, "등불 속 그 돌, 오리지… 뭐였죠?")
        scheduler.textChanged(ScriptSpot.BlockText(K.BLOCK_B12))
        advanceTimeBy(300)
        // More typing restarts the wait.
        scheduler.textChanged(ScriptSpot.BlockText(K.BLOCK_B12))
        advanceTimeBy(499)
        runCurrent()
        assertTrue(reports.isEmpty())
        advanceTimeBy(2)
        runCurrent()
        assertEquals(listOf(ScanReport(null, setOf(ScriptSpot.BlockText(K.BLOCK_B12)))), reports)

        reports.clear()
        doc = (TermEdits.setAliases(doc, T1, listOf("오리지늄", "빛돌", "빛 돌멩이"), ctx()) as AliasEdit.Applied).doc
        scheduler.termChanged(T1)
        advanceUntilIdle()
        val report = reports.single()
        assertEquals(setOf(T1), report.sweptTerms)
        assertTrue(report.rescannedSpots.all { it in setOf(ScriptSpot.BlockText(K.BLOCK_B12), ScriptSpot.BlockText(K.BLOCK_B32), ScriptSpot.BlockText(K.BLOCK_B61)) })
        assertEquals(3, scheduler.index.of(T1).size)
    }

    @Test
    fun `WLD-T72 full scan on open`() = runTest {
        val doc = stateB()
        val before = queries(doc)
        val countBefore = before.card(T1)!!.candidateCount
        // Reopen: the app-only index from before is shown dimmed until the background scan ends.
        val scheduler = RescanScheduler(scope = this, current = { doc }, initial = before.index)
        scheduler.fullScan()
        val scanning = TermQueries(doc, scheduler.index)
        assertTrue(scanning.checks().scanning)
        assertTrue(scanning.card(T1)!!.candidatesPending)
        assertTrue(scanning.checks().rows.filterIsInstance<TermCheckRow.CandidateBeforeReveal>().all { it.pending })
        // Usage and ▲ are final at once.
        assertEquals(2, scanning.card(T1)!!.usageCount)
        assertEquals(1, scanning.activeWarnings(T1).size)
        assertEquals(1, scanning.checks().badgeCount)

        advanceUntilIdle()
        val done = TermQueries(doc, scheduler.index)
        assertFalse(done.checks().scanning)
        assertFalse(done.card(T1)!!.candidatesPending)
        assertEquals(countBefore, done.card(T1)!!.candidateCount)
        assertEquals(3, done.card(T1)!!.candidateCount)
        assertEquals(doc.index.term(T1)!!.rejections, emptyList<Any>())
    }

    @Test
    fun `restore from a file starts with an empty pending index`() = runTest {
        val doc = rejectedB12(stateB())
        val scheduler = RescanScheduler(scope = this, current = { doc })
        scheduler.fullScan()
        assertTrue(TermQueries(doc, scheduler.index).card(T1)!!.candidatesPending)
        advanceUntilIdle()
        assertEquals(2, TermQueries(doc, scheduler.index).card(T1)!!.candidateCount)
        assertEquals(1, doc.index.term(T1)!!.rejections.size)
    }
}
