package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.domain.recorder.ShownCopy
import io.github.lenticualrstone.civilight.designsystem.component.GlowMode
import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.recorder.capture.Recording
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureStage
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureViews
import io.github.lenticualrstone.civilight.feature.recorder.capture.EntryRoutes
import io.github.lenticualrstone.civilight.feature.recorder.capture.GlowModes
import io.github.lenticualrstone.civilight.feature.recorder.capture.PickerRequest
import io.github.lenticualrstone.civilight.feature.recorder.capture.PickerTarget
import io.github.lenticualrstone.civilight.feature.recorder.capture.UnlockRequest
import io.github.lenticualrstone.civilight.feature.recorder.memo.entryOf
import io.github.lenticualrstone.civilight.feature.recorder.outside.OutsideEntries
import io.github.lenticualrstone.civilight.feature.recorder.outside.RecordingNotices
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.RecorderSettings
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.recorder.RecordingPhase
import io.github.lenticualrstone.civilight.model.recorder.RecordingSession
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.navigation.Recording as RecordingRoute
import io.github.lenticualrstone.civilight.navigation.TextMemoSheet
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Entry points outside the app (LRC-02 … LRC-05), background continuity (LRC-09) and the glow modes (LRC-10). */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OutsideAndRecoveryTest {
    @Test
    fun LRC_T01_outsideSurfacesPrefillNothing() = runTest {
        for (surface in listOf(EntrySurface.HOME_WIDGET, EntrySurface.QS_TILE, EntrySurface.COVER_WIDGET, EntrySurface.LOCK_SCREEN)) {
            val entry = EntryRoutes.entryOf(OutsideEntries.route(surface))
            assertEquals(surface, entry.surface)
            assertNull(entry.projectKey)
            assertEquals(surface == EntrySurface.LOCK_SCREEN, entry.onLockScreen)
            val f = HubFixture(this)
            f.hub.begin(entry, startTyped = false, micGranted = true)
            assertEquals(CaptureStage.RECORDING, f.hub.state.value.stage)
            assertEquals(listOf("+ 연결"), CaptureViews.recording(f.hub.state.value)!!.screen.let { it.chips.map { c -> c.text } + it.addLink })
        }
    }

    @Test
    fun LRC_T49_lockScreen() = runTest {
        val lock = EntryRoutes.entryOf(OutsideEntries.route(EntrySurface.LOCK_SCREEN))
        val f = HubFixture(this)
        f.hub.begin(lock, startTyped = false, micGranted = true)
        val view = CaptureViews.recording(f.hub.state.value)!!
        assertFalse(view.canToText)
        // `+ 연결` asks for the device unlock first; cancel keeps the recording screen.
        f.hub.openPicker(PickerRequest(PickerTarget.SESSION))
        assertEquals(UnlockRequest.Picker(PickerRequest(PickerTarget.SESSION)), f.hub.state.value.unlock)
        assertNull(f.hub.state.value.picker)
        f.hub.unlockResult(false)
        assertNull(f.hub.state.value.picker)
        assertEquals(CaptureStage.RECORDING, f.hub.state.value.stage)
        f.hub.openPicker(PickerRequest(PickerTarget.SESSION))
        f.hub.unlockResult(true)
        assertNotNull(f.hub.state.value.picker)
        // `글로 쓰기` does nothing on the lock screen; `정리 중` has no `여기 넣기`.
        f.hub.toText()
        assertEquals(CaptureStage.RECORDING, f.hub.state.value.stage)
        f.hub.stopAndSave()
        assertNull(CaptureViews.processing(f.hub.state.value)!!.screen.putHere)

        // With `잠금 화면에서 녹음` off the tile asks for the unlock before recording.
        val off = HubFixture(this, settings = AppSettings(recorder = RecorderSettings(lockScreenRecording = false)))
        off.hub.begin(lock, startTyped = false, micGranted = true)
        assertEquals(CaptureStage.IDLE, off.hub.state.value.stage)
        assertTrue(off.hub.state.value.active)
        assertTrue(off.hub.state.value.unlock is UnlockRequest.Start)
        off.hub.unlockResult(true)
        assertEquals(CaptureStage.RECORDING, off.hub.state.value.stage)
        assertFalse(off.hub.state.value.session!!.entry.onLockScreen)

        val cancelled = HubFixture(this, settings = AppSettings(recorder = RecorderSettings(lockScreenRecording = false)))
        cancelled.hub.begin(lock, startTyped = false, micGranted = true)
        cancelled.hub.unlockResult(false)
        assertFalse(cancelled.hub.state.value.active)
    }

    @Test
    fun LRC_09_theNotificationNeverCarriesTextOrLinks() = runTest {
        val f = HubFixture(this)
        assertNull(RecordingNotices.of(f.hub.state.value))
        f.hub.begin(
            EntryRoutes.entryOf(RecordingRoute(surface = "script", projectKey = Fx.P1, targetKind = "scene", targetKey = Fx.S)),
            startTyped = false, micGranted = true,
        )
        f.engine.confirm(Fx.R1_TEXT)
        val notice = RecordingNotices.of(f.hub.state.value)!!
        assertEquals("라이프 레코더", notice.title)
        assertEquals(ShownCopy.RECORDING, notice.text)
        for (part in listOf(notice.title, notice.text)) {
            assertFalse(part.contains("하르트") || part.contains("1-7") || part.contains("잿빛"))
        }
        f.hub.togglePause()
        assertTrue(RecordingNotices.keepsAlive(f.hub.state.value))
        f.hub.stopAndSave()
        assertFalse(RecordingNotices.keepsAlive(f.hub.state.value))
    }

    @Test
    fun LRC_09_aRecordingCutOffByProcessDeathOpensInProcessing() = runTest {
        val f = HubFixture(this)
        val entry0 = EntryRoutes.entryOf(RecordingRoute(surface = "script", projectKey = Fx.P1, targetKind = "scene", targetKey = Fx.S))
        val ctx = ActionContext(now = Fx.NOW, zone = Fx.ZONE, device = Fx.DEVICE, newKey = Fx.keys(300)).recorder
        val saved = Recording.start("01JZ000000TSTK000000000777", entry0, RecorderSettings(), f.env.env, "takes/x.m4a", ctx)
            .copy(elapsedMs = 12_000, confirmedRuns = listOf(TextRun.Plain("하르트가 등불을")), partialText = "싫어하는 이유")
        val entry = f.hub.recover(saved)
        assertNotNull(entry)
        assertEquals(CaptureStage.PROCESSING, f.hub.state.value.stage)
        val record = f.recorder.library.record(saved.recordKey)!!
        assertEquals("하르트가 등불을 싫어하는 이유", Runs.text(record.runs))
        assertEquals(12_000L, record.durationMs)
        assertEquals(RecordingPhase.PROCESSING, f.recorder.session!!.phase)
        // A second recovery while a capture is open does nothing.
        assertNull(f.hub.recover(saved))
        f.hub.keepInInbox()
        assertNull(f.recorder.session)
        assertEquals(listOf(Fx.S), f.recorder.library.linksOf(saved.recordKey).map { it.targetKey })
    }

    @Test
    fun LRC_09_anEmptyTakeIsDroppedAndProcessingComesBackAsItWas() = runTest {
        val empty = HubFixture(this)
        val nothing = RecordingSession(recordKey = "01JZ000000TSTK000000000778", entry = EntryRoutes.entryOf(RecordingRoute()), audioTempPath = "t.m4a")
        empty.recorder.session = nothing
        assertNull(empty.hub.recover(nothing))
        assertNull(empty.recorder.session)
        assertTrue(empty.recorder.library.records.isEmpty())

        val f = HubFixture(this)
        f.hub.begin(EntryRoutes.entryOf(RecordingRoute(projectKey = Fx.P1)), startTyped = false, micGranted = true)
        f.engine.confirm("메모")
        testScheduler.advanceTimeBy(3_000)
        f.hub.stopAndSave()
        val processing = f.recorder.session!!
        val again = HubFixture(this, recorder = f.recorder)
        assertNotNull(again.hub.recover(processing))
        assertEquals(CaptureStage.PROCESSING, again.hub.state.value.stage)
        assertEquals(1, again.recorder.library.records.size)
    }

    @Test
    fun LRC_10_glowModes() = runTest {
        val f = HubFixture(this)
        assertNull(GlowModes.of(f.hub.state.value, glowOn = true, reducedMotion = false))
        f.hub.begin(EntryRoutes.entryOf(RecordingRoute()), startTyped = false, micGranted = true)
        assertEquals(GlowMode.RECORDING, GlowModes.of(f.hub.state.value, glowOn = true, reducedMotion = false))
        assertEquals(GlowMode.STILL, GlowModes.of(f.hub.state.value, glowOn = true, reducedMotion = true))
        assertNull(GlowModes.of(f.hub.state.value, glowOn = false, reducedMotion = false))
        f.hub.togglePause()
        assertEquals(GlowMode.STILL, GlowModes.of(f.hub.state.value, glowOn = true, reducedMotion = false))
        f.hub.togglePause()
        f.engine.lastWords = "끝"
        f.hub.stopAndSave()
        // The fake engine finishes at once: the sweep has ended by the time `정리 중` is drawn.
        assertNull(GlowModes.of(f.hub.state.value, glowOn = true, reducedMotion = false))
        assertEquals(
            GlowMode.PROCESSING,
            GlowModes.of(f.hub.state.value.copy(finalising = true), glowOn = true, reducedMotion = false),
        )
    }

    @Test
    fun LRC_T01_aScriptEntryWithOnlyTheCaretPrefillsItsScene() = runTest {
        val f = HubFixture(this)
        f.hub.begin(
            EntryRoutes.entryOf(RecordingRoute(surface = "script", projectKey = Fx.P1, caretBlockKey = ExampleKeys.BLOCK_044)),
            startTyped = false, micGranted = true,
        )
        assertEquals(listOf("연결 · 1-7 씬 3"), CaptureViews.recording(f.hub.state.value)!!.screen.chips.map { it.text })
        f.engine.confirm(Fx.R1_TEXT)
        f.hub.stopAndSave()
        assertEquals("여기 넣기 · #044 뒤", CaptureViews.processing(f.hub.state.value)!!.screen.putHere)
    }

    @Test
    fun LRC_19_aMemoSheetFromACardPrefillsThatCard() {
        val entry = entryOf(TextMemoSheet(projectKey = Fx.P1, targetKind = "character", targetKey = ExampleKeys.HART))
        assertEquals(EntrySurface.CHARACTER, entry.surface)
        assertEquals(RecorderTargetKind.CHARACTER, entry.targetKind)
        assertEquals(ExampleKeys.HART, entry.targetKey)
        assertEquals(EntrySurface.OTHER_IN_PROJECT, entryOf(TextMemoSheet(projectKey = Fx.P1)).surface)
        assertEquals(EntrySurface.LIBRARY, entryOf(TextMemoSheet()).surface)
    }
}
