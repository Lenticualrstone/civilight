package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkDisplay
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.put.ManuscriptSpot
import io.github.lenticualrstone.civilight.domain.recorder.state.PlacementStatus
import io.github.lenticualrstone.civilight.domain.recorder.state.RecordState
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.feature.recorder.inbox.InboxSelection
import io.github.lenticualrstone.civilight.feature.recorder.record.CardOutcome
import io.github.lenticualrstone.civilight.feature.recorder.record.LinkPickMode
import io.github.lenticualrstone.civilight.feature.recorder.record.PlacementRoutes
import io.github.lenticualrstone.civilight.feature.recorder.record.RecordController
import io.github.lenticualrstone.civilight.feature.recorder.record.RecordLinkPicks
import io.github.lenticualrstone.civilight.feature.recorder.record.RecordStatus
import io.github.lenticualrstone.civilight.feature.recorder.record.RecordView
import io.github.lenticualrstone.civilight.feature.recorder.record.RecordWriter
import io.github.lenticualrstone.civilight.model.app.UnlockSession
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.navigation.PageDoc
import io.github.lenticualrstone.civilight.navigation.Script
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The record screen's controller (LRC-28) over the example's recorder library and project. */
class RecordFixture(scope: TestScope, val recordKey: String, library: RecorderLibrary = ExampleRecorder.library()) {
    val recorder = FakeRecorder(library, ExampleRecorder.trashEntries())
    val projects = FakeProjects(Fx.p1())
    val env = FakeEnv(Fx.env(projects.doc))
    val unlock = MutableStateFlow(0)
    private val keys = Fx.keys(300)
    val context = { ActionContext(now = Fx.NOW, zone = Fx.ZONE, device = Fx.DEVICE, newKey = keys) }
    val writer = RecordWriter(recorder, projects, env, context)
    val controller = RecordController(
        scope = scope.backgroundScope,
        recorder = recorder,
        envSource = env,
        writer = writer,
        recordKey = recordKey,
        now = { Fx.NOW },
        editContext = { RecorderEditContext(newKey = keys, now = Fx.NOW, device = Fx.DEVICE, zone = Fx.ZONE) },
        unlockChanges = unlock,
        commitDelayMs = 600,
    )

    fun status(scope: TestScope): RecordStatus {
        scope.runCurrent()
        return controller.status.value
    }

    fun view(scope: TestScope): RecordView = (status(scope) as RecordStatus.Ready).view
}

class RecordScreenTest {
    private val r1 = ExampleKeys.RECORDER_R1
    private val r2 = ExampleKeys.RECORDER_R2

    @Test
    fun LRC_28_headerChipsActionsAndNoPlacementsYet() = runTest {
        val f = RecordFixture(this, r1)
        val v = f.view(this)
        assertEquals("음성 0:24", v.screen.kindLabel)
        assertEquals("오늘 08:41", v.screen.meta)
        assertEquals(Fx.R1_TEXT, v.text)
        // Every link as a chip on the record screen (the inbox row omits nothing here: no project tag rule).
        assertEquals(listOf("연결 · 1-7 씬 3", "하르트"), v.screen.chips.map { it.text })
        assertEquals(listOf("원고에 넣기", "카드에 넣기", "페이지로 만들기"), v.screen.actions)
        assertTrue(v.placements.isEmpty())
        assertNull(v.screen.placementsGoneMark)
    }

    @Test
    fun LRC_T25_editAfterInsertionMarksThePlacementOnlyHere() = runTest {
        val f = RecordFixture(this, r1)
        f.view(this)
        val done = f.writer.putInManuscript(r1, Fx.P1, ManuscriptSpot.AfterBlock(ExampleKeys.BLOCK_044))!!
        val placed = f.view(this).placements.single()
        assertEquals("1-7 씬 3 · #058", placed.row.text)
        assertFalse(placed.row.editedAfter)
        // A live block opens the script at that block.
        assertEquals(Script(Fx.P1, blockKey = done.insertion.placement.targetKey), placed.route)

        f.controller.edit(Fx.R1_TEXT + " 추가", (Fx.R1_TEXT + " 추가").length)
        advanceTimeBy(700)
        runCurrent()
        val record = f.recorder.library.record(r1)!!
        assertEquals(2, record.textRevision)
        assertEquals(Fx.R1_TEXT + " 추가", Runs.text(record.runs))
        val after = f.view(this).placements.single()
        assertTrue(after.row.editedAfter)
        assertEquals(RecorderCopy.EDITED_AFTER, after.row.mark)
        // The copy keeps its text.
        assertEquals(Fx.R1_TEXT, Runs.text(f.projects.doc.index.block(done.insertion.placement.targetKey)!!.runs))
    }

    @Test
    fun LRC_28_editsAreSavedOnceAfterThePauseAndFlushWritesAWaitingEdit() = runTest {
        val f = RecordFixture(this, r1)
        f.view(this)
        f.controller.edit("하르트", 3)
        advanceTimeBy(300)
        runCurrent()
        // Still waiting: nothing written yet.
        assertEquals(1, f.recorder.library.record(r1)!!.textRevision)
        f.controller.edit("하르트가", 4)
        f.controller.flush()
        assertEquals("하르트가", Runs.text(f.recorder.library.record(r1)!!.runs))
        assertEquals(2, f.recorder.library.record(r1)!!.textRevision)
        // The timer of the flushed edit never writes a second time.
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(2, f.recorder.library.record(r1)!!.textRevision)
        // The field keeps what was typed (no reload from its own write).
        assertEquals("하르트가", f.view(this).text)
    }

    @Test
    fun LRC_28_aChangeMadeElsewhereReachesTheField() = runTest {
        val f = RecordFixture(this, r2)
        val before = f.view(this)
        f.writer.toPlainText(r2, TargetRef(Fx.P1, RecorderTargetKind.CHARACTER, ExampleKeys.SERA))
        val after = f.view(this)
        assertTrue(after.reload > before.reload)
        assertEquals(0, after.marks.count)
    }

    @Test
    fun LRC_17_atOnTheRecordScreenMakesALinkRunAndAnAtLink() = runTest {
        val f = RecordFixture(this, ExampleKeys.RECORDER_R3)
        f.view(this)
        val text = "@하르"
        f.controller.edit(text, text.length)
        val v = f.view(this)
        assertTrue(v.atOpen)
        val hart = v.atCandidates.first { it.target.key == ExampleKeys.HART }
        f.controller.pickAt(hart)
        runCurrent()
        val links = f.recorder.library.linksOf(ExampleKeys.RECORDER_R3)
        assertEquals(listOf(ExampleKeys.HART), links.map { it.targetKey })
        assertEquals(listOf("연결 · 하르트 @"), f.view(this).screen.chips.map { it.text })
    }

    @Test
    fun LRC_T38_deletedCharacterChipCardAndItsButtons() = runTest {
        val f = RecordFixture(this, ExampleKeys.RECORDER_NOAH)
        val chip = f.view(this).screen.chips.single()
        assertEquals(LinkDisplay.DELETED_ITEM_TRASH, chip.display)
        assertTrue(chip.dotted)
        val card = f.controller.cardOf(chip)!!
        assertEquals("지운 인물 · 노아", card.title)
        assertEquals(listOf("칩 다시 고르기", "칩 빼기", "이대로 두기"), card.actions)

        val linkKey = chip.links.single().key
        val replace = f.controller.onCard(linkKey, RecorderCopy.RELINK_CHIP)
        assertEquals(CardOutcome.OpenPicker(listOf(linkKey), null), replace)
        val sheet = RecordLinkPicks.sheetFor(ExampleKeys.RECORDER_NOAH, replace as CardOutcome.OpenPicker)
        assertEquals(LinkPickMode.Replace(listOf(linkKey)), RecordLinkPicks.mode(sheet))
        RecordLinkPicks.apply(f.controller, RecordLinkPicks.mode(sheet), TargetRef(Fx.P1, RecorderTargetKind.CHARACTER, ExampleKeys.OWEN))
        assertEquals(listOf(ExampleKeys.OWEN), f.recorder.library.linksOf(ExampleKeys.RECORDER_NOAH).map { it.targetKey })
    }

    @Test
    fun LRC_T38_chipRemoveFromTheCard() = runTest {
        val f = RecordFixture(this, ExampleKeys.RECORDER_NOAH)
        val chip = f.view(this).screen.chips.single()
        assertEquals(CardOutcome.Close, f.controller.onCard(chip.links.single().key, RecorderCopy.REMOVE_CHIP))
        assertTrue(f.recorder.library.linksOf(ExampleKeys.RECORDER_NOAH).isEmpty())
    }

    @Test
    fun LRC_T38b_atRunToADeletedCharacterOffersLinkOtherPlainTextAndRestore() = runTest {
        val f = RecordFixture(this, r2)
        f.view(this)
        // 세라 goes to the trash: R2's `@세라` run and its chip turn dotted.
        val trashed = f.projects.doc.let { doc ->
            doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == ExampleKeys.SERA) it.copy(deletedAt = Fx.NOW) else it }))
        }
        f.projects.doc = trashed
        f.env.env = Fx.env(trashed)
        f.unlock.value++
        val chip = f.view(this).screen.chips.single { it.atForm }
        val card = f.controller.cardOf(chip)!!
        assertEquals(listOf("다른 항목에 잇기", "글로 풀기", "되살리기", "이대로 두기"), card.actions)
        assertFalse(RecorderCopy.REMOVE_CHIP in card.actions)

        val relink = f.controller.onCard(chip.links.single().key, RecorderCopy.LINK_OTHER) as CardOutcome.OpenPicker
        val mode = RecordLinkPicks.mode(RecordLinkPicks.sheetFor(r2, relink))
        assertEquals(LinkPickMode.Relink(TargetRef(Fx.P1, RecorderTargetKind.CHARACTER, ExampleKeys.SERA)), mode)

        assertEquals(CardOutcome.Close, f.controller.onCard(chip.links.single().key, RecorderCopy.TO_PLAIN_TEXT))
        val record = f.recorder.library.record(r2)!!
        assertEquals("오웬은 사실 결사 출신? 탈퇴 이유가 세라와 관련", Runs.text(record.runs))
        assertTrue(f.recorder.library.linksOf(r2).none { it.targetKey == ExampleKeys.SERA })
    }

    @Test
    fun LRC_23_restoreFromTheCardBringsTheCharacterBack() = runTest {
        val f = RecordFixture(this, ExampleKeys.RECORDER_NOAH)
        val chip = f.view(this).screen.chips.single()
        // A plain chip offers no 되살리기 (rule 20: only the `@` card does); the writer restores through CharacterActions.
        assertFalse(RecorderCopy.RESTORE in f.controller.cardOf(chip)!!.actions)
        val text = f.writer.restoreTarget(TargetRef(Fx.P1, RecorderTargetKind.CHARACTER, ExampleKeys.NOAH), authenticated = true)
        assertNotNull(text)
        assertNull(f.projects.doc.index.character(ExampleKeys.NOAH)!!.trashEntryKey)
        assertEquals(listOf(Fx.P1), f.env.forgotten)
    }

    @Test
    fun LRC_T29_maskedLinksCollapseIntoTheRating() = runTest {
        val r18 = Fx.p1().let { doc -> doc.copy(project = doc.project.copy(rating = Rating.R18.open(), protections = doc.project.protections.copy(recorderLinkMask = true))) }
        val f = RecordFixture(this, r1)
        f.env.env = Fx.env(r18)
        f.unlock.value++
        assertEquals(listOf("연결 · 이름 가림"), f.view(this).screen.chips.map { it.text })
    }

    @Test
    fun LRC_41_aHiddenRecordAsksFirstAndOpensWithHiddenRecordsOpen() = runTest {
        val library = ExampleRecorder.library().let { lib -> lib.copy(records = lib.records.map { if (it.key == r1) it.copy(hidden = true) else it }) }
        val f = RecordFixture(this, r1, library)
        assertEquals(RecordStatus.Locked, f.status(this))
        f.env.env = f.env.env.copy(unlock = UnlockSession(recordsUnlockedAt = Fx.NOW))
        f.unlock.value++
        assertTrue(f.status(this) is RecordStatus.Ready)
    }

    @Test
    fun LRC_28_aDeletedRecordLeavesTheScreen() = runTest {
        val f = RecordFixture(this, r1)
        f.view(this)
        f.writer.delete(r1)
        assertEquals(RecordStatus.Gone, f.status(this))
    }

    @Test
    fun LRC_T36_fixture_placementInTheTrashKeepsPutAndIsNotOpened() = runTest {
        val f = RecordFixture(this, r1)
        f.view(this)
        val done = f.writer.putInManuscript(r1, Fx.P1, ManuscriptSpot.AfterBlock(ExampleKeys.BLOCK_044))!!
        val blockKey = done.insertion.placement.targetKey
        val trashed = f.projects.doc.let { doc ->
            doc.copy(
                episodes = doc.episodes.mapValues { (_, ep) ->
                    ep.copy(blocks = ep.blocks.map { if (it.key == blockKey) it.copy(deletedAt = Fx.NOW) else it })
                },
            )
        }
        f.projects.doc = trashed
        f.env.env = Fx.env(trashed)
        f.unlock.value++
        val row = f.view(this).placements.single()
        assertEquals(PlacementStatus.IN_TRASH, row.row.status)
        assertEquals("1-7 씬 3 · #058 · 휴지통", row.row.text)
        assertNull(row.route)
        assertEquals(RecordState.PUT, f.view(this).screen.state)
    }

    @Test
    fun LRC_28_placementRoutesByKind() = runTest {
        val f = RecordFixture(this, ExampleKeys.RECORDER_R0)
        val page = f.view(this).placements.single()
        assertEquals(PageDoc(Fx.P1, ExampleKeys.PAGE_SOCIETY_RULES), page.route)
        val placement = f.recorder.library.placementsOf(ExampleKeys.RECORDER_R0).single()
        assertEquals(PageDoc(Fx.P1, ExampleKeys.PAGE_SOCIETY_RULES), PlacementRoutes.routeOf(placement, f.env.env))
    }

    @Test
    fun LRC_31_theNewestVisibleRecordIsSelectedOnceInTwoPanes() = runTest {
        val v = InboxFixture(this).view(this)
        assertEquals(r1, InboxSelection.autoSelect(alreadyDone = false, inboxOnTop = true, rows = v.rows))
        assertNull(InboxSelection.autoSelect(alreadyDone = true, inboxOnTop = true, rows = v.rows))
        assertNull(InboxSelection.autoSelect(alreadyDone = false, inboxOnTop = false, rows = v.rows))
        assertNull(InboxSelection.autoSelect(alreadyDone = false, inboxOnTop = true, rows = emptyList()))
    }
}
