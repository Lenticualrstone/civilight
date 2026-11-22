package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.checks.CheckInputs
import io.github.lenticualrstone.civilight.domain.checks.CheckListRow
import io.github.lenticualrstone.civilight.domain.checks.FixKind
import io.github.lenticualrstone.civilight.domain.checks.ProjectChecks
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.feature.story.check.CheckFixes
import io.github.lenticualrstone.civilight.feature.story.check.CheckModel
import io.github.lenticualrstone.civilight.feature.story.check.FixEffect
import io.github.lenticualrstone.civilight.feature.story.check.SpeakerFix
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 점검 센터 (STO-39 … STO-41, CNV-17): screen model and the row buttons over the example project. */
class CheckCenterModelTest {
    private val doc = Fx.doc()
    private val inputs = CheckInputs(now = Fx.ctx().now, zone = Fx.zone, hiddenUnlocked = true)
    private val report = ProjectChecks.run(doc, inputs)
    private val p = K.PROJECT

    private fun item(type: String) = report.open.first { it.checkType == type }

    @Test
    fun routeFilterNamesMapToChipsAndUnknownIsAll() {
        assertEquals(CheckFilter.TERM, CheckModel.filterOf("TERM"))
        assertEquals(CheckFilter.ALL, CheckModel.filterOf(null))
        assertEquals(CheckFilter.ALL, CheckModel.filterOf("bogus"))
    }

    @Test
    fun chk01_headerRowsAndOrderOfTheFixture() {
        val ui = CheckModel.build(report, CheckFilter.ALL, stale = false)
        assertEquals("전체 8", ui.header.chips.first().label)
        assertEquals("기록과 원고가 어긋난 곳", ui.header.intro)
        assertFalse(ui.scanning)
        assertNull(ui.header.scanningLabel)
        assertNull(ui.empty)
        assertTrue(ui.rows.isNotEmpty())
        assertEquals("명령 또는 검색", ui.commandHint)
        val travel = CheckModel.build(report, CheckFilter.TRAVEL, stale = false)
        assertTrue(travel.rows.all { it !is CheckListRow.Item || it.result.filter == CheckFilter.TRAVEL })
    }

    @Test
    fun aStaleReportShowsScanningWithDimmedChips() {
        val ui = CheckModel.build(report, CheckFilter.ALL, stale = true)
        assertTrue(ui.scanning)
        assertEquals("점검 중", ui.header.scanningLabel)
        assertTrue(ui.header.chips.all { it.dimmed })
    }

    @Test
    fun emptyStateOnlyUnderAllWithNothingOpen() {
        val mini = Fx.mini(listOf("장", "에피소드", "씬"), Fx.node("C1", null, null, Fx.node("E1", null, null, Fx.node("S1", null, "글"))))
        val r = ProjectChecks.run(mini, inputs)
        val ui = CheckModel.build(r, CheckFilter.ALL, stale = false)
        assertNotNull(ui.empty)
        assertEquals("어긋난 곳 없음", ui.empty!!.title)
        assertNull(CheckModel.build(r, CheckFilter.ALL, stale = true).empty)
        assertNull(CheckModel.build(r, CheckFilter.TERM, stale = false).empty)
    }

    @Test
    fun matchMovesAnItemToAcknowledgedAndRevertBringsItBack_STO41() {
        val before = item(CheckType.TERM_BEFORE_REVEAL)
        val ack = before.actions.single { it.kind == FixKind.ACKNOWLEDGE }
        val edit = CheckFixes.effect(doc, p, before, ack, inputs) as FixEffect.Edit
        val accepted = edit.transform(doc, Fx.ctx())
        val after = ProjectChecks.run(accepted, inputs)
        assertTrue(after.acknowledged.any { it.checkType == before.checkType && it.subjectKeys == before.subjectKeys })
        assertEquals(report.count(CheckFilter.ALL) - 1, after.count(CheckFilter.ALL))

        val row = CheckModel.build(after, CheckFilter.ACKNOWLEDGED, stale = false).rows
            .filterIsInstance<CheckListRow.Item>().first { it.result.subjectKeys == before.subjectKeys }
        val revert = row.actions.single { it.kind == FixKind.REVERT }
        val undo = CheckFixes.effect(accepted, p, row.result, revert, inputs) as FixEffect.Edit
        val reopened = ProjectChecks.run(undo.transform(accepted, Fx.ctx()), inputs)
        assertEquals(report.count(CheckFilter.ALL), reopened.count(CheckFilter.ALL))
    }

    @Test
    fun speakerRowButtonsReachTheScriptModule_SCR62() {
        val missing = item(CheckType.SPEAKER_MISSING)
        val toNarration = missing.actions.single { it.kind == FixKind.TO_NARRATION }
        val edited = (CheckFixes.effect(doc, p, missing, toNarration, inputs) as FixEffect.Edit).transform(doc, Fx.ctx())
        assertEquals(BlockKind.NARRATION, edited.index.block(K.BLOCK_1_7_027)!!.kind.known)
        val pick = CheckFixes.effect(doc, p, missing, missing.actions.single { it.kind == FixKind.PICK_SPEAKER }, inputs)
        assertEquals(FixEffect.PickSpeaker(K.BLOCK_1_7_027), pick)
        val spoken = SpeakerFix.set(doc, K.BLOCK_1_7_027, K.HART, Fx.ctx())
        assertEquals(K.HART, spoken.index.block(K.BLOCK_1_7_027)!!.speakerKey)
    }

    @Test
    fun goToScriptOpensTheBlockOrTheSceneInTheDetailPane() {
        val missing = item(CheckType.SPEAKER_MISSING)
        assertEquals(K.BLOCK_1_7_027, CheckModel.locationTarget(doc, missing))
        assertEquals(Script(p, blockKey = K.BLOCK_1_7_027), CheckFixes.scriptAt(doc, p, K.BLOCK_1_7_027))
        assertEquals(Script(p, sceneKey = K.SCENE_1_7_3), CheckFixes.scriptAt(doc, p, K.SCENE_1_7_3))
        val voice = item(CheckType.VOICE_EMPTY)
        assertNull("`인물 · 리엔` is not in the manuscript", CheckModel.locationTarget(doc, voice))
        val write = CheckFixes.effect(doc, p, voice, voice.actions.single(), inputs)
        assertTrue(write is FixEffect.Navigate && write.detail)
    }

    @Test
    fun candidateRowsLinkAndRejectTheirOwnCandidate_WLD() {
        val candidate = item(CheckType.TERM_CANDIDATE_BEFORE_REVEAL)
        assertNotNull(CheckFixes.candidate(doc, candidate))
        val link = candidate.actions.single { it.kind == FixKind.LINK_CANDIDATE }
        val linked = (CheckFixes.effect(doc, p, candidate, link, inputs) as FixEffect.Edit).transform(doc, Fx.ctx())
        assertNotEquals(doc, linked)
        val after = ProjectChecks.run(linked, inputs)
        assertFalse(after.open.any { it.checkType == candidate.checkType && it.subjectKeys == candidate.subjectKeys && it.fingerprint == candidate.fingerprint })
    }

    @Test
    fun onlyButtonsWithAnEffectAreDrawn_andReadOnlyDrawsNoEdits() {
        val unavailable = setOf(FixKind.RELINK_RECORD, FixKind.BULK_REPLACE)
        for (r in report.results) {
            val shown = CheckFixes.shown(doc, p, r, r.actions, inputs).map { it.first.kind }
            val expected = r.actions.filter { a ->
                a.kind !in unavailable &&
                    !(a.kind == FixKind.DELAY_SCENE && r.travel == null) &&
                    !(a.kind == FixKind.RESTORE && !hasTrashEntry(a.target))
            }
            assertEquals(expected.map { it.kind }, shown)
            val readOnly = CheckFixes.shown(doc, p, r, r.actions, inputs, readOnly = true).map { it.second }
            assertTrue(readOnly.all { it is FixEffect.Navigate || it is FixEffect.OpenBulk || it == FixEffect.Unlock })
        }
    }

    private fun hasTrashEntry(itemKey: String?): Boolean {
        itemKey ?: return false
        val entry = doc.index.character(itemKey)?.trashEntryKey ?: doc.index.place(itemKey)?.trashEntryKey
            ?: doc.index.term(itemKey)?.trashEntryKey ?: doc.index.faction(itemKey)?.trashEntryKey
        return entry?.let(doc.index::trashEntry) != null
    }

    @Test
    fun restoreBringsTheDeletedItemBackWithItsToastAndTheRowGoes_R20() {
        val row = report.open.first { r -> r.actions.any { it.kind == FixKind.RESTORE && hasTrashEntry(it.target) } }
        val action = row.actions.first { it.kind == FixKind.RESTORE }
        val effect = CheckFixes.effect(doc, p, row, action, inputs)
        assertTrue(effect is FixEffect.Restore)
        val step = (effect as FixEffect.Restore).step(doc, Fx.ctx())
        assertNotNull(step)
        val restored = step!!.doc
        val item = action.target!!
        assertTrue(restored.index.character(item)?.deletedAt == null && restored.index.place(item)?.deletedAt == null)
        assertNotNull("the toast keeps 되돌리기", step.toast)
        val after = ProjectChecks.run(restored, inputs)
        assertFalse(after.open.any { it.checkType == row.checkType && it.subjectKeys == row.subjectKeys })
        assertEquals("되돌리기 reverts the restore row by row", doc.index.character(item)?.deletedAt, step.change.revert(restored).index.character(item)?.deletedAt)
        assertNull("stale: nothing to restore any more", effect.step(restored, Fx.ctx()))
    }
}
