package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.domain.recorder.ShownCopy
import io.github.lenticualrstone.civilight.domain.recorder.ProjectSnapshot
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.link.TargetRef
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.feature.recorder.capture.CancelOutcome
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureStage
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureViews
import io.github.lenticualrstone.civilight.feature.recorder.capture.EntryRoutes
import io.github.lenticualrstone.civilight.feature.recorder.capture.PickerRequest
import io.github.lenticualrstone.civilight.feature.recorder.capture.PickerTarget
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.EntryContext
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.LinkOrigin
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.recorder.RecordingPhase
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.navigation.Recording
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The capture sequence over fakes: timer on the virtual clock, a driven speech engine, an in-memory library. */
class CaptureHubTest {
    private val fromScript = Recording(
        surface = "script", projectKey = Fx.P1, targetKind = "scene", targetKey = Fx.S, caretBlockKey = ExampleKeys.BLOCK_044,
    )

    private suspend fun HubFixture.startFromScript() = hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = true)

    private fun TestScope.wait(ms: Long) = testScheduler.advanceTimeBy(ms)

    @Test
    fun LRC_T02_rule23Example() = runTest {
        val f = HubFixture(this)
        f.startFromScript()
        assertEquals(CaptureStage.RECORDING, f.hub.state.value.stage)
        assertEquals(listOf("연결 · 1-7 씬 3"), CaptureViews.recording(f.hub.state.value)!!.screen.chips.map { it.text })

        f.engine.confirm(Fx.R1_TEXT)
        wait(24_000)
        f.hub.stopAndSave()
        val processing = CaptureViews.processing(f.hub.state.value)!!.screen
        assertEquals("00:24", processing.duration)

        f.hub.openPicker(PickerRequest(PickerTarget.SESSION))
        f.hub.pickTarget(TargetRef(Fx.P1, RecorderTargetKind.CHARACTER, ExampleKeys.HART))
        assertNull(f.hub.state.value.picker)
        assertEquals(listOf("연결 · 1-7 씬 3", "하르트"), CaptureViews.processing(f.hub.state.value)!!.screen.chips.map { it.text })

        f.hub.keepInInbox()
        assertEquals(CaptureStage.IDLE, f.hub.state.value.stage)
        val record = f.recorder.library.records.single()
        assertEquals(RecordKind.VOICE.open(), record.kind)
        assertEquals(24_000L, record.durationMs)
        assertEquals(Fx.R1_TEXT, Runs.text(record.runs))
        val links = f.recorder.library.linksOf(record.key)
        assertEquals(listOf(LinkOrigin.ENTRY, LinkOrigin.MANUAL), links.map { it.origin.known })
        assertEquals(listOf(Fx.S, ExampleKeys.HART), links.map { it.targetKey })
        assertFalse(record.hidden)
        assertNull(f.recorder.session)
    }

    @Test
    fun LRC_T47_noAutoStop() = runTest {
        val f = HubFixture(this)
        f.startFromScript()
        wait(10 * 60_000 + 1)
        val state = f.hub.state.value
        assertEquals(CaptureStage.RECORDING, state.stage)
        assertEquals(RecordingPhase.RECORDING, state.session!!.phase)
        assertEquals("10:00", CaptureViews.recording(state)!!.screen.timer)
        assertTrue(f.recorder.library.records.isEmpty())
    }

    @Test
    fun LRC_T48_resumeContinuesTheSameRecord() = runTest {
        val f = HubFixture(this)
        f.startFromScript()
        f.engine.confirm("하르트가 등불을 싫어하는 이유.")
        wait(24_000)
        f.hub.stopAndSave()
        f.hub.continueSpeaking()
        assertEquals(CaptureStage.RECORDING, f.hub.state.value.stage)
        assertEquals("00:24", CaptureViews.recording(f.hub.state.value)!!.screen.timer)
        f.engine.say("그게 세라의")
        wait(10_000)
        f.hub.stopAndSave()
        val record = f.recorder.library.records.single()
        assertEquals(34_000L, record.durationMs)
        assertEquals("하르트가 등불을 싫어하는 이유. 그게 세라의", Runs.text(record.runs))
        assertEquals(2, record.textRevision)
    }

    @Test
    fun pausedTimeIsNotCountedAndTheTailTurnsFinalAtTheStop() = runTest {
        val f = HubFixture(this)
        f.startFromScript()
        wait(5_000)
        f.hub.togglePause()
        assertTrue(CaptureViews.recording(f.hub.state.value)!!.paused)
        assertEquals(RecorderCopy.TODO_RESUME, CaptureViews.recording(f.hub.state.value)!!.screen.pauseLabel)
        assertTrue(f.engine.paused)
        wait(60_000)
        f.hub.togglePause()
        wait(7_000)
        f.engine.say("어떨까")
        assertEquals("어떨까", CaptureViews.recording(f.hub.state.value)!!.screen.transcript.tail)
        f.hub.stopAndSave()
        val record = f.recorder.library.records.single()
        assertEquals(12_000L, record.durationMs)
        assertEquals("어떨까", Runs.text(record.runs))
    }

    @Test
    fun aLateFinalResultRefinesTheTailWhileFinalising() = runTest {
        val f = HubFixture(this)
        f.startFromScript()
        f.engine.confirm("하르트가 등불을 싫어하는 이유.")
        f.engine.say("대정전 때 누군가를")
        f.engine.lastWords = "대정전 때 누군가를 잃었다."
        f.hub.stopAndSave()
        val record = f.recorder.library.records.single()
        assertEquals("하르트가 등불을 싫어하는 이유. 대정전 때 누군가를 잃었다.", Runs.text(record.runs))
        assertEquals(1, record.textRevision)
        assertFalse(f.hub.state.value.finalising)
    }

    @Test
    fun cancelFirstTakeDiscardsAndUndoBringsItBackIntoProcessing() = runTest {
        val f = HubFixture(this)
        f.startFromScript()
        f.engine.confirm("지울 생각")
        wait(3_000)
        val outcome = f.hub.cancel()
        assertTrue(outcome is CancelOutcome.Discarded)
        assertEquals(CaptureStage.IDLE, f.hub.state.value.stage)
        assertTrue(f.recorder.library.records.isEmpty())
        assertEquals("TODO_COPY_LRC_03", (outcome as CancelOutcome.Discarded).cancelled.toast.text)

        f.hub.undoCancel(outcome.cancelled)
        assertEquals(CaptureStage.PROCESSING, f.hub.state.value.stage)
        assertEquals("지울 생각", Runs.text(f.recorder.library.records.single().runs))
    }

    @Test
    fun cancelDuringAContinuedTakeDropsOnlyTheNewTake() = runTest {
        val f = HubFixture(this)
        f.startFromScript()
        f.engine.confirm("첫 생각")
        wait(4_000)
        f.hub.stopAndSave()
        f.hub.continueSpeaking()
        f.engine.confirm("버릴 말")
        wait(2_000)
        assertEquals(CancelOutcome.BackToProcessing, f.hub.cancel())
        assertEquals(CaptureStage.PROCESSING, f.hub.state.value.stage)
        assertEquals("00:04", CaptureViews.processing(f.hub.state.value)!!.screen.duration)
        assertEquals("첫 생각", Runs.text(f.recorder.library.records.single().runs))
    }

    @Test
    fun LRC_11_withoutAnEngineTheRecordingRunsAndSavesEmptyText() = runTest {
        val f = HubFixture(this, engine = FakeEngine(available = false))
        f.startFromScript()
        assertEquals(ShownCopy.NO_ENGINE, CaptureViews.recording(f.hub.state.value)!!.engineNote)
        wait(6_000)
        f.hub.stopAndSave()
        val view = CaptureViews.processing(f.hub.state.value)!!.screen
        assertEquals("00:06", view.duration)
        assertNull("nothing to insert from an empty text", view.putHere)
        f.hub.keepInInbox()
        val record = f.recorder.library.records.single()
        assertEquals("", Runs.text(record.runs))
        assertEquals(6_000L, record.durationMs)
    }

    @Test
    fun LRC_06_reasonFirstThenSystemPromptThenMicOff() = runTest {
        val f = HubFixture(this)
        val entry = EntryRoutes.entryOf(fromScript)
        f.hub.begin(entry, startTyped = false, micGranted = false)
        assertEquals(CaptureStage.MIC_REASON, f.hub.state.value.stage)
        f.hub.permissionResult(false)
        assertEquals(CaptureStage.MIC_OFF, f.hub.state.value.stage)

        f.hub.micOffToText()
        assertEquals(CaptureStage.TYPING, f.hub.state.value.stage)
        assertEquals(listOf("연결 · 1-7 씬 3"), CaptureViews.memo(f.hub.state.value)!!.sheet.chips.map { it.text })
        f.hub.memoClose()

        // Denied once in this process: the next tap goes straight to `마이크 꺼짐`.
        f.hub.begin(entry, startTyped = false, micGranted = false)
        assertEquals(CaptureStage.MIC_OFF, f.hub.state.value.stage)
        f.hub.dismissMic()
        assertEquals(CaptureStage.IDLE, f.hub.state.value.stage)

        f.hub.begin(entry, startTyped = false, micGranted = false)
        f.hub.permissionResult(true)
        assertEquals(CaptureStage.RECORDING, f.hub.state.value.stage)
    }

    @Test
    fun LRC_01_aSecondMicTapShowsTheRunningRecording() = runTest {
        val f = HubFixture(this)
        f.startFromScript()
        val key = f.hub.state.value.session!!.recordKey
        wait(2_000)
        // The app went to the background (home, LRC-09): the take goes on; back would save it (U-13, MicReleaseTest).
        assertEquals(CaptureStage.RECORDING, f.hub.state.value.stage)
        f.hub.begin(EntryContext(surface = EntrySurface.LIBRARY), startTyped = false, micGranted = true)
        assertEquals(key, f.hub.state.value.session!!.recordKey)
        assertEquals(1, f.engine.starts)
    }

    @Test
    fun LRC_01_aMemoOpenedElsewhereNeverDropsAMinimisedRecording() = runTest {
        val f = HubFixture(this)
        f.startFromScript()
        val key = f.hub.state.value.session!!.recordKey
        f.engine.confirm("등불")
        // Left in the background (LRC-09), not with back.
        assertFalse(f.hub.openMemo(EntryContext(surface = EntrySurface.PROJECT_HOME, projectKey = Fx.P1)))
        assertEquals(CaptureStage.RECORDING, f.hub.state.value.stage)
        assertEquals(key, f.hub.state.value.session!!.recordKey)
        assertNull(f.hub.state.value.memo)
        assertTrue("the engine still listens", f.engine.running)

        f.hub.stopAndSave()
        assertFalse("정리 중 is kept too", f.hub.openMemo(EntryContext(surface = EntrySurface.LIBRARY)))
        assertEquals(CaptureStage.PROCESSING, f.hub.state.value.stage)
        f.hub.keepInInbox()
        assertTrue(f.hub.openMemo(EntryContext(surface = EntrySurface.LIBRARY)))
        assertEquals(CaptureStage.TYPING, f.hub.state.value.stage)
    }

    @Test
    fun LRC_34_putHereInsertsAfterTheCaretBlockAndUndoRemovesIt() = runTest {
        val f = HubFixture(this)
        f.startFromScript()
        f.engine.confirm(Fx.R1_TEXT)
        wait(24_000)
        f.hub.stopAndSave()
        assertEquals("여기 넣기 · #044 뒤", CaptureViews.processing(f.hub.state.value)!!.screen.putHere)

        val done = f.hub.putHere()!!
        assertEquals(CaptureStage.IDLE, f.hub.state.value.stage)
        assertEquals("1-7 씬 3 · #058 작가 메모 넣음", done.insertion.toast.text)
        val block = f.projects.doc.index.block(done.insertion.placement.targetKey)!!
        assertEquals(BlockKind.MEMO.open(), block.kind)
        assertEquals(58, block.number)
        val record = f.recorder.library.records.single()
        assertEquals("1-7 씬 3 · #058", f.recorder.library.placementsOf(record.key).single().lastLabel)
        assertEquals(listOf(RecorderCopy.PUT_HERE), f.projects.labels)

        f.hub.undoPut(done)
        assertNull(f.projects.doc.index.block(done.insertion.placement.targetKey))
        assertTrue(f.recorder.library.placementsOf(record.key).isEmpty())
    }

    @Test
    fun LRC_34_noPutHereWhenTheCaretBlockWasDeletedMeanwhile() = runTest {
        val f = HubFixture(this)
        f.startFromScript()
        f.engine.confirm(Fx.R1_TEXT)
        wait(3_000)
        // #044 goes to the trash while the recording runs.
        val trashed = f.projects.doc.let { doc ->
            doc.copy(episodes = doc.episodes.mapValues { (_, ep) -> ep.copy(blocks = ep.blocks.map { if (it.key == ExampleKeys.BLOCK_044) it.copy(deletedAt = Fx.NOW) else it }) })
        }
        f.projects.doc = trashed
        f.env.env = Fx.env(trashed)
        f.hub.stopAndSave()
        f.hub.refreshEnv()
        testScheduler.runCurrent()
        assertNull(CaptureViews.processing(f.hub.state.value)!!.screen.putHere)
        assertNull(f.hub.putHere())
        assertTrue(f.projects.labels.isEmpty())
    }

    @Test
    fun putHereIsNotOfferedInAReadOnlyProjectNorFromAnotherEntry() = runTest {
        val p1 = Fx.p1()
        val readOnlyEnv = RecorderEnv(listOf(ProjectSnapshot(Fx.entry(p1, readOnly = true), p1)), zone = Fx.ZONE)
        val ro = HubFixture(this, projects = FakeProjects(p1, readOnly = true), env = FakeEnv(readOnlyEnv))
        ro.startFromScript()
        ro.engine.confirm("생각")
        ro.hub.stopAndSave()
        assertNull(CaptureViews.processing(ro.hub.state.value)!!.screen.putHere)

        val home = HubFixture(this)
        home.hub.begin(EntryRoutes.entryOf(Recording(surface = "projectHome", projectKey = Fx.P1)), startTyped = false, micGranted = true)
        home.engine.confirm("생각")
        home.hub.stopAndSave()
        assertNull(CaptureViews.processing(home.hub.state.value)!!.screen.putHere)
    }

    @Test
    fun LRC_42_hideRowIsOnForAnR18TargetAndHidesTheRecord() = runTest {
        val r18 = Fx.p1().let { it.copy(project = it.project.copy(rating = Rating.R18.open())) }
        val f = HubFixture(this, projects = FakeProjects(r18))
        f.startFromScript()
        f.engine.confirm("숨길 생각")
        f.hub.stopAndSave()
        val row = CaptureViews.processing(f.hub.state.value)!!.screen.hideRow!!
        assertTrue(row.on)
        assertEquals("숨기기", row.title)
        f.hub.keepInInbox()
        assertTrue(f.recorder.library.records.single().hidden)

        val g = HubFixture(this, projects = FakeProjects(r18))
        g.startFromScript()
        g.engine.confirm("보일 생각")
        g.hub.stopAndSave()
        g.hub.setHide(false)
        g.hub.keepInInbox()
        assertFalse(g.recorder.library.records.single().hidden)
    }

    @Test
    fun LRC_18_chipRemovalAndReplaceOnTheCaptureScreens() = runTest {
        val f = HubFixture(this)
        f.startFromScript()
        val entryLink = f.hub.state.value.session!!.pendingLinks.single()
        f.hub.openPicker(PickerRequest(PickerTarget.SESSION, listOf(entryLink.key)))
        f.hub.pickTarget(TargetRef(Fx.P1, RecorderTargetKind.CHARACTER, ExampleKeys.SERA))
        val replaced = f.hub.state.value.session!!.pendingLinks.single()
        assertEquals(ExampleKeys.SERA, replaced.targetKey)
        assertEquals(LinkOrigin.MANUAL, replaced.origin.known)
        f.hub.removeLinks(PickerTarget.SESSION, listOf(replaced.key))
        assertTrue(f.hub.state.value.session!!.pendingLinks.isEmpty())
        assertEquals(emptyList<String>(), CaptureViews.recording(f.hub.state.value)!!.screen.chips.map { it.text })
    }

    @Test
    fun theSessionIsWrittenForCrashRecovery() = runTest {
        val f = HubFixture(this)
        f.startFromScript()
        wait(1_000)
        assertNotNull(f.recorder.session)
        f.hub.stopAndSave()
        assertEquals(RecordingPhase.PROCESSING, f.recorder.session!!.phase)
        f.hub.keepInInbox()
        wait(1_000)
        assertNull(f.recorder.session)
    }
}
