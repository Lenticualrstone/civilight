package io.github.lenticualrstone.civilight.domain.checks

import io.github.lenticualrstone.civilight.domain.checks.Fx.withUnit
import io.github.lenticualrstone.civilight.domain.story.checks.CheckCenter
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.story.checks.Marker
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.checks.AckKind
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The registry (STO-40) and the 점검 센터 shell semantics (STO-39, STO-41, CNV-17). */
class CheckCenterTest {
    @Test
    fun registryListsEveryCheckTypeInOrder() {
        assertEquals(CheckType.ALL, CheckRegistry.all.map { it.checkType })
        assertEquals((1..20).toList(), CheckRegistry.all.map { it.number })
        val warnings = CheckRegistry.all.filter { it.marker == Marker.WARNING }.map { it.number }
        assertEquals(listOf(1, 4, 5, 7, 10, 11, 12, 20), warnings)
        assertEquals(CheckFilter.TERM, CheckRegistry.of(CheckType.TERM_CANDIDATE_BEFORE_REVEAL).filter)
        assertEquals(CheckFilter.SPEAKER, CheckRegistry.of(CheckType.HIDDEN_CHARACTER_IN_VISIBLE_SCENE).filter)
        assertEquals(CheckFilter.CHANGE, CheckRegistry.of(CheckType.CHANGE_ANCHOR_DELETED).filter)
        assertEquals(CheckFilter.DELETED, CheckRegistry.of(CheckType.DELETED_ITEM).filter)
        // Q25 default: time and transition rows go to `이동`.
        assertEquals(CheckFilter.TRAVEL, CheckRegistry.of(CheckType.TIME_ORDER).filter)
        assertEquals(CheckFilter.TRAVEL, CheckRegistry.of(CheckType.TRANSITION_STALE).filter)
        assertEquals(AckKind.KEEP, CheckRegistry.of(CheckType.CHANGE_ANCHOR_DELETED).ackKind)
        assertNull(CheckRegistry.of(CheckType.SPEAKER_MISSING).ackKind)
        assertEquals("그대로", CheckRegistry.ackLabel(CheckType.VOICE_FORMAT_DIFFERS))
        assertEquals("이대로 두기", CheckRegistry.ackLabel(CheckType.DELETED_ITEM))
        assertEquals("맞음", CheckRegistry.ackLabel(CheckType.TRAVEL_SHORTAGE))
        assertNull(CheckRegistry.ackLabel(CheckType.VOICE_EMPTY))
        assertEquals(listOf("원고로 가기", "공개 시점 옮기기", "맞음"), CheckRegistry.of(CheckType.TERM_BEFORE_REVEAL).buttons)
    }

    @Test
    fun headerAndCountsAgreeWithTheStoryCheckCenter() {
        val report = Fx.run(Fx.doc())
        val header = CheckCenterView.header(report)
        assertEquals("잿빛 새벽의 등불", header.projectName)
        assertEquals("점검 센터", header.title)
        assertEquals("기록과 원고가 어긋난 곳", header.intro)
        for (chip in header.chips) assertEquals(CheckCenter.count(report.doc, report.entries, chip.filter), chip.count)
        assertTrue(header.chips.none { it.dimmed })
        assertEquals(3, report.warningCount)
    }

    @Test
    fun inputsFollowTheAppSettings() {
        val inputs = CheckInputs.of(AppSettings(checkTravel = false, checkNoPlace = true), ExampleClock.NOW)
        assertFalse(inputs.travel.checkTravel)
        assertTrue(inputs.travel.checkNoPlace)
    }

    /**
     * STO CHK-03: a locked hidden scene with two check items collapses into one row. 1-7 씬 2 has
     * `▲ 화자 없음` and 하르트's `▲ 이동 시간 부족`; the fixture's `맞음` on the latter is reverted so
     * both are open.
     */
    @Test
    fun chk03_lockedHiddenSceneCollapses() {
        val hidden = CheckAcks.revert(Fx.doc(), K.ACK_TRAVEL_HART, Fx.ctx()).withUnit(K.SCENE_1_7_2) { it.copy(hidden = true) }
        val locked = Fx.run(hidden)
        assertEquals(
            setOf(CheckType.SPEAKER_MISSING, CheckType.TRAVEL_SHORTAGE),
            locked.open.filter { it.lockedHidden }.map { it.checkType }.toSet(),
        )
        val rows = CheckCenterView.rows(locked)
        val group = rows.first() as CheckListRow.HiddenGroup
        assertEquals("▲ 숨긴 항목 관련 2 · 지문으로 보기", group.text)
        assertEquals(FixKind.UNLOCK, group.action.kind)
        assertEquals(1, rows.count { it is CheckListRow.HiddenGroup })
        // In a filter the collapsed row holds only that filter's hidden items.
        assertEquals(1, (CheckCenterView.rows(locked, CheckFilter.SPEAKER).first() as CheckListRow.HiddenGroup).count)

        val unlocked = CheckCenterView.rows(Fx.run(hidden, Fx.inputs(hiddenUnlocked = true)))
        assertTrue(unlocked.none { it is CheckListRow.HiddenGroup })
    }

    /** STO CHK-07 / CNV T-CNV-24: acknowledgements are stored in the file; results are recomputed. */
    @Test
    fun chk07_acknowledgementsSurviveAFileRoundTrip() {
        val doc = Fx.doc()
        val item = Fx.run(doc).ofType(CheckType.TERM_BEFORE_REVEAL).single()
        val acked = CheckAcks.acknowledge(doc, item, Fx.ctx())
        val decoded = ProjectFileSet.decode(ProjectFileSet.split(acked).encode())
        assertTrue(decoded.failedPaths.isEmpty())
        val reopened = decoded.files.toDoc(app = acked.app)
        assertEquals(acked.checks.acknowledgements, reopened.checks.acknowledgements)
        val before = Fx.run(acked)
        val after = Fx.run(reopened)
        assertEquals(before.open.map { it.id }, after.open.map { it.id })
        assertEquals(before.acknowledged.map { it.id }, after.acknowledged.map { it.id })
        assertEquals(3, after.count(CheckFilter.ACKNOWLEDGED))
    }

    /**
     * STO-41: an acknowledgement whose item now fires with another fingerprint is void; pruning
     * deletes only that one. The fixture's own two acknowledgements are valid, so pruning it is a no-op.
     */
    @Test
    fun pruneDeletesVoidAcknowledgementsOnly() {
        val fixture = Fx.run(Fx.doc())
        assertTrue(CheckAcks.voided(fixture).isEmpty())
        assertTrue(CheckAcks.prune(fixture) === fixture.doc)

        val doc = Fx.doc()
        val stale = doc.copy(checks = doc.checks.copy(acknowledgements = doc.checks.acknowledgements.map {
            if (it.key == K.ACK_CLOCK_BAND) it.copy(fingerprint = "sha256-stale") else it
        }))
        val report = Fx.run(stale)
        assertEquals(listOf(K.ACK_CLOCK_BAND), CheckAcks.voided(report).map { it.key })
        assertFalse(report.isAcknowledged(report.ofType(CheckType.TIME_CLOCK_BAND_MISMATCH).single()))
        val pruned = CheckAcks.prune(report)
        assertEquals(listOf(K.ACK_TRAVEL_HART), pruned.checks.acknowledgements.map { it.key })
        assertEquals(report.count(CheckFilter.ALL), Fx.run(pruned).count(CheckFilter.ALL))
    }

    @Test
    fun acknowledgedFilterListsItemsWithRevert() {
        val report = Fx.run(Fx.doc())
        val rows = CheckCenterView.rows(report, CheckFilter.ACKNOWLEDGED).filterIsInstance<CheckListRow.Item>()
        assertEquals(
            listOf(CheckType.TRAVEL_SHORTAGE, CheckType.TIME_CLOCK_BAND_MISMATCH).toSet(),
            rows.map { it.result.checkType }.toSet(),
        )
        assertEquals(setOf(K.ACK_TRAVEL_HART, K.ACK_CLOCK_BAND), rows.map { it.actions.single().target }.toSet())
        val reverted = CheckAcks.revert(report.doc, K.ACK_CLOCK_BAND, Fx.ctx())
        assertEquals(report.count(CheckFilter.ALL) + 1, Fx.run(reverted).count(CheckFilter.ALL))
    }
}
