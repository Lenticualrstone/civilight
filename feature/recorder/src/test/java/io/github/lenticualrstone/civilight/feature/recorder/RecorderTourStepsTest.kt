package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.domain.library.onboarding.ModuleTours
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureStage
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureState
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureViews
import io.github.lenticualrstone.civilight.feature.recorder.capture.EntryRoutes
import io.github.lenticualrstone.civilight.feature.recorder.capture.PickerRequest
import io.github.lenticualrstone.civilight.feature.recorder.capture.PickerTarget
import io.github.lenticualrstone.civilight.feature.recorder.capture.RecorderTourSteps
import io.github.lenticualrstone.civilight.model.recorder.RecordingPhase
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.navigation.Recording
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** LIB-19 recorder tour (emulator QA 2026-11-23: without the mic permission all three cards floated with no hole). */
class RecorderTourStepsTest {
    private val fromScript = Recording(
        surface = "script", projectKey = Fx.P1, targetKind = "scene", targetKey = Fx.S, caretBlockKey = ExampleKeys.BLOCK_044,
    )

    @Test
    fun `step ids follow the tour's copy`() {
        val titles = ModuleTours.steps(TourModule.RECORDER).map { it.title }
        assertEquals(RecorderTourSteps.COUNT, titles.size)
        assertEquals("기록 남기기", titles[RecorderTourSteps.RECORD])
        assertEquals("연결", titles[RecorderTourSteps.LINK])
        assertEquals("기록함", titles[RecorderTourSteps.INBOX])
    }

    @Test
    fun `the tour waits while no capture pane is drawn`() {
        listOf(CaptureStage.IDLE, CaptureStage.MIC_REASON, CaptureStage.MIC_OFF, CaptureStage.TYPING).forEach {
            assertTrue(it.name, RecorderTourSteps.hold(CaptureState(stage = it)))
        }
        // A stage without its view (no session yet) draws nothing either.
        assertTrue(RecorderTourSteps.hold(CaptureState(stage = CaptureStage.RECORDING)))
    }

    @Test
    fun `no mic, then allowed - the tour runs on 녹음 중 and 정리 중, never under a sheet`() = runTest {
        val f = HubFixture(this)
        f.hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = false)
        assertTrue(RecorderTourSteps.hold(f.hub.state.value))
        f.hub.permissionResult(false)
        assertEquals(CaptureStage.MIC_OFF, f.hub.state.value.stage)
        assertTrue(RecorderTourSteps.hold(f.hub.state.value))
        f.hub.dismissMic()

        f.hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = true)
        assertEquals(CaptureStage.RECORDING, f.hub.state.value.stage)
        assertFalse(RecorderTourSteps.hold(f.hub.state.value))
        f.hub.openPicker(PickerRequest(PickerTarget.SESSION))
        assertTrue(RecorderTourSteps.hold(f.hub.state.value))
        f.hub.closePicker()
        f.hub.stopAndSave()
        assertEquals(CaptureStage.PROCESSING, f.hub.state.value.stage)
        assertFalse(RecorderTourSteps.hold(f.hub.state.value))
    }

    @Test
    fun `the take pauses while the tour's card is shown, so reading it is not recorded`() = runTest {
        // Tour QA 2026-11-23: the cards opened over the first live take, `00:03 → 00:40` went into the record.
        val takes = FakeTakes()
        val f = HubFixture(this, takes = takes)
        f.hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = true)
        testScheduler.advanceTimeBy(1_000)
        f.hub.holdForTour()
        assertEquals(RecordingPhase.PAUSED, f.hub.state.value.session!!.phase)
        assertTrue(f.engine.paused)
        assertTrue(takes.paused)
        assertFalse(RecorderTourSteps.hold(f.hub.state.value))
        testScheduler.advanceTimeBy(37_000)
        f.hub.releaseTour()
        assertEquals(RecordingPhase.RECORDING, f.hub.state.value.session!!.phase)
        assertFalse(f.engine.paused)
        assertFalse(takes.paused)
        testScheduler.advanceTimeBy(5_000)
        f.hub.stopAndSave()
        assertEquals(6_000L, f.recorder.library.records.single().durationMs)
    }

    @Test
    fun `a take the user paused stays paused after the tour, and 정리 중 is left alone`() = runTest {
        val f = HubFixture(this)
        f.hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = true)
        f.hub.togglePause()
        f.hub.holdForTour()
        f.hub.releaseTour()
        assertTrue(CaptureViews.recording(f.hub.state.value)!!.paused)

        f.hub.togglePause()
        f.hub.stopAndSave()
        f.hub.holdForTour()
        f.hub.releaseTour()
        assertEquals(CaptureStage.PROCESSING, f.hub.state.value.stage)
        assertEquals(RecordingPhase.PROCESSING, f.hub.state.value.session!!.phase)
    }
}
