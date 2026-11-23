package io.github.lenticualrstone.civilight.domain.checks

import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.story.travel.TravelSettings
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** (0.2.0) S-2 / D33 / G-2: the check center lists writing items; voice, travel and candidates follow their gates. */
class WritingChecksTest {
    private val base = Fx.doc()
    private val travelOff = Fx.inputs(travel = TravelSettings(checkTravel = false))
    private val travelOn = Fx.inputs(travel = TravelSettings(checkTravel = true))

    private fun ProjectDoc.gameTools(on: Boolean) = copy(project = project.copy(gameTools = on))
    private fun ProjectDoc.candidates(on: Boolean) = copy(project = project.copy(termCandidates = on))
    private fun CheckReport.types(): Set<String> = results.map { it.checkType }.toSet()

    @Test
    fun voiceKindsAndChipFollowTheGameToolsSwitch() {
        val off = WritingChecks.run(base.gameTools(false), travelOn)
        assertTrue(off.types().none { it in WritingChecks.VOICE_TYPES })
        assertTrue(WritingChecks.header(off).chips.none { it.filter == CheckFilter.VOICE })
        assertEquals(0, off.count(CheckFilter.VOICE))

        val on = WritingChecks.run(base.gameTools(true), travelOn)
        assertTrue(CheckType.VOICE_EMPTY in on.types())
        assertTrue(WritingChecks.header(on).chips.any { it.filter == CheckFilter.VOICE })
    }

    @Test
    fun travelRowsAbsentWhileOffAndPresentWhenOn() {
        val off = WritingChecks.run(base, travelOff)
        assertTrue(off.types().none { it in WritingChecks.TRAVEL_TYPES })
        assertTrue(WritingChecks.header(off).chips.none { it.filter == CheckFilter.TRAVEL })
        // `◇ 장소 없음` has its own switch, but D33 hides it with the travel check too.
        assertTrue(WritingChecks.check(base, travelOff, CheckType.TRAVEL_NO_PLACE).isEmpty())

        val on = WritingChecks.run(base, travelOn)
        assertTrue(CheckType.TRAVEL_NO_PLACE in on.types())
        assertTrue(CheckType.TRAVEL_SHORTAGE in on.types())
        assertTrue(WritingChecks.header(on).chips.any { it.filter == CheckFilter.TRAVEL })
    }

    @Test
    fun candidateRowsFollowTheProjectSetting() {
        val off = WritingChecks.run(base.candidates(false), travelOn)
        assertTrue(off.ofType(CheckType.TERM_CANDIDATE_BEFORE_REVEAL).isEmpty())
        val on = WritingChecks.run(base.candidates(true), travelOn)
        assertEquals(3, on.ofType(CheckType.TERM_CANDIDATE_BEFORE_REVEAL).size)
    }

    @Test
    fun everyOtherWritingKindStillAppearsWithAllGatesOff() {
        val doc = base.gameTools(false).candidates(false)
        val report = WritingChecks.run(doc, travelOff)
        val full = ProjectChecks.run(doc, travelOff)
        val writing = full.results.filter { it.checkType !in WritingChecks.VOICE_TYPES && it.checkType !in WritingChecks.TRAVEL_TYPES && it.checkType != CheckType.TERM_CANDIDATE_BEFORE_REVEAL }
        assertEquals(writing, report.results)
        assertTrue(CheckType.TERM_BEFORE_REVEAL in report.types())
        assertTrue(CheckType.SPEAKER_MISSING in report.types())
        assertTrue(CheckType.DELETED_ITEM in report.types())
        // The fixture's `◇ 시각과 시간대가 다름` is acknowledged and stays listed there.
        assertTrue(report.acknowledged.any { it.checkType == CheckType.TIME_CLOCK_BAND_MISMATCH })
    }

    /** Data safety §8: a gated kind's acknowledgement is never voided, and shows again when the gate opens. */
    @Test
    fun acknowledgementsOfGatedKindsSurviveOffAndOn() {
        val off = WritingChecks.run(base, travelOff)
        assertEquals(base, CheckAcks.prune(off))
        assertTrue(CheckAcks.voided(off).isEmpty())
        assertTrue(off.doc.checks.acknowledgements.any { it.key == K.ACK_TRAVEL_HART })

        val on = WritingChecks.run(CheckAcks.prune(off), travelOn)
        val shortage = on.ofType(CheckType.TRAVEL_SHORTAGE).single()
        assertTrue(on.isAcknowledged(shortage))
        assertFalse(on.open.contains(shortage))
    }

    @Test
    fun aRouteNamingAHiddenChipFallsBackToAll() {
        val doc = base.gameTools(false)
        assertEquals(CheckFilter.ALL, WritingChecks.shownFilter(doc, travelOff, CheckFilter.VOICE))
        assertEquals(CheckFilter.ALL, WritingChecks.shownFilter(doc, travelOff, CheckFilter.TRAVEL))
        assertEquals(CheckFilter.TERM, WritingChecks.shownFilter(doc, travelOff, CheckFilter.TERM))
    }
}
