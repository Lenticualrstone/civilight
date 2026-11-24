package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.feature.recorder.audio.MediaRecorderTakes
import io.github.lenticualrstone.civilight.feature.recorder.audio.MicSegment
import io.github.lenticualrstone.civilight.feature.recorder.audio.TakeFiles
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureStage
import io.github.lenticualrstone.civilight.feature.recorder.outside.AudioFocusGate
import io.github.lenticualrstone.civilight.feature.recorder.capture.EntryRoutes
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.RecordKind
import io.github.lenticualrstone.civilight.model.recorder.RecordingPhase
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.navigation.Recording
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * U-13 (privacy + data loss): a paused take holds no microphone, and leaving the recorder with a take
 * recording or paused saves it as `정리 안 됨` and releases every holder exactly once.
 */
class MicReleaseTest {
    private val fromScript = Recording(
        surface = "script", projectKey = Fx.P1, targetKind = "scene", targetKey = Fx.S, caretBlockKey = ExampleKeys.BLOCK_044,
    )

    /** Audio focus the test reads: held or not, and how often it was asked for and given back. */
    private class FakeFocus : AudioFocusGate {
        var held = false
        var requests = 0
        var abandons = 0
        var onLost: (() -> Unit)? = null

        override fun request(onLost: () -> Unit) {
            if (held) return
            held = true
            requests++
            this.onLost = onLost
        }

        override fun abandon() {
            if (!held) return
            held = false
            abandons++
        }
    }

    private class Rig(scope: TestScope, val takes: FakeTakes = FakeTakes(), projects: FakeProjects = FakeProjects(Fx.p1())) {
        val f = HubFixture(scope, takes = takes, projects = projects)
        val focus = FakeFocus().also { f.hub.usePlatformFocus(it) }

        /** No holder of the microphone is open. */
        fun released(): Boolean = !takes.micOpen && !focus.held && !(f.engine.running && !f.engine.paused)

        /** Every open was closed once. */
        fun balanced(): Boolean = takes.opens == takes.closes && focus.requests == focus.abandons
    }

    private suspend fun Rig.start() = f.hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = true)

    // ---- pause and resume ----

    @Test
    fun U13_pauseReleasesTheMicAndResumeGoesOnInTheSameRecord() = runTest {
        val r = Rig(this)
        r.start()
        val key = r.f.hub.state.value.session!!.recordKey
        assertTrue(r.takes.micOpen)
        assertTrue(r.focus.held)
        r.f.engine.confirm("첫 생각")
        testScheduler.advanceTimeBy(3_000)

        r.f.hub.togglePause()
        assertEquals(RecordingPhase.PAUSED, r.f.hub.state.value.session!!.phase)
        assertFalse("the take's recorder is closed", r.takes.micOpen)
        assertFalse("audio focus is given back", r.focus.held)
        assertTrue("the engine stops listening", r.f.engine.paused)
        testScheduler.advanceTimeBy(10_000)

        r.f.hub.togglePause()
        assertTrue(r.takes.micOpen)
        assertTrue(r.focus.held)
        assertEquals("the same record goes on", key, r.f.hub.state.value.session!!.recordKey)
        r.f.engine.confirm("둘째")
        testScheduler.advanceTimeBy(2_000)
        r.f.hub.stopAndSave()

        val record = r.f.recorder.library.records.single()
        assertEquals(key, record.key)
        assertEquals("paused time is not counted", 5_000L, record.durationMs)
        assertEquals("첫 생각 둘째", Runs.text(record.runs))
        assertEquals("one take for the record", 1, r.takes.segments[key])
        assertTrue(r.released())
        assertTrue(r.balanced())
        assertEquals(2, r.takes.opens)
    }

    @Test
    fun U13_theTourHoldAndAFocusLossReleaseTheMicToo() = runTest {
        val r = Rig(this)
        r.start()
        r.f.hub.holdForTour()
        assertFalse(r.takes.micOpen)
        assertFalse(r.focus.held)
        r.f.hub.releaseTour()
        assertTrue(r.takes.micOpen)

        r.focus.onLost!!.invoke()
        assertEquals(RecordingPhase.PAUSED, r.f.hub.state.value.session!!.phase)
        assertTrue(r.released())
        assertTrue(r.balanced())
    }

    // ---- leaving the recorder ----

    @Test
    fun U13_leavingWhilePausedSavesTheTakeAndKeepsTheMicClosed() = runTest {
        val r = Rig(this)
        r.start()
        val key = r.f.hub.state.value.session!!.recordKey
        r.f.engine.confirm("멈춘 생각")
        testScheduler.advanceTimeBy(4_000)
        r.f.hub.togglePause()

        val save = r.f.hub.leave()
        assertNotNull(save)
        save!!.join()

        assertEquals(CaptureStage.IDLE, r.f.hub.state.value.stage)
        val record = r.f.recorder.library.records.single()
        assertEquals(key, record.key)
        assertEquals(RecordKind.VOICE.open(), record.kind)
        assertEquals("멈춘 생각", Runs.text(record.runs))
        assertEquals(4_000L, record.durationMs)
        assertNotNull("the audio recorded so far is kept", record.audio)
        assertEquals("the links are saved", listOf(Fx.S), r.f.recorder.library.linksOf(key).map { it.targetKey })
        assertNull("no recovery left over", r.f.recorder.session)
        assertTrue(r.released())
        assertTrue(r.balanced())
        assertFalse(r.f.engine.running)
    }

    @Test
    fun U13_leavingWhileRecordingReleasesAtOnceAndSaves() = runTest {
        val r = Rig(this)
        r.start()
        r.f.engine.say("말하는 중")
        testScheduler.advanceTimeBy(2_000)

        val save = r.f.hub.leave()!!
        // Released before the save is awaited (the route's dispose cannot wait).
        assertFalse(r.takes.micOpen)
        assertFalse(r.focus.held)
        assertFalse(r.f.engine.running)
        save.join()

        assertEquals(CaptureStage.IDLE, r.f.hub.state.value.stage)
        val record = r.f.recorder.library.records.single()
        assertEquals("the unconfirmed tail is kept", "말하는 중", Runs.text(record.runs))
        assertEquals(2_000L, record.durationMs)
        assertTrue(r.balanced())
    }

    @Test
    fun U13_aNotificationTapWhileTheLeftTakeIsSavingStartsNoNewTake() = runTest {
        val r = Rig(this)
        r.start()
        r.f.engine.say("말하는 중")
        testScheduler.advanceTimeBy(2_000)
        // Hold the inbox write: the library write made once the hub is already IDLE (keepInInbox after close).
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        r.f.recorder.beforeUpdate = { if (r.f.hub.state.value.stage == CaptureStage.IDLE) gate.await() }

        val save = r.f.hub.leave()!!
        testScheduler.runCurrent()
        assertEquals("the hub is already idle while the save runs", CaptureStage.IDLE, r.f.hub.state.value.stage)
        assertTrue(r.f.hub.leaving!!.isActive)
        val opens = r.takes.opens

        // The still-visible `녹음 중` notification is tapped: a fresh Recording route calls begin.
        r.start()
        assertEquals("no new take", opens, r.takes.opens)
        assertFalse(r.takes.micOpen)
        assertEquals(CaptureStage.IDLE, r.f.hub.state.value.stage)

        gate.complete(Unit)
        save.join()
        assertEquals(1, r.f.recorder.library.records.size)
        assertTrue(r.released())
        assertTrue(r.balanced())

        // Once the save is done, a mic tap records again.
        r.start()
        assertTrue(r.takes.micOpen)
        r.f.hub.leave()!!.join()
    }

    @Test
    fun U13_leavingTwiceSavesOnceAndLeavingWithNoTakeDoesNothing() = runTest {
        val r = Rig(this)
        assertNull(r.f.hub.leave())
        r.start()
        testScheduler.advanceTimeBy(1_000)
        val first = r.f.hub.leave()
        assertNull("the save already runs", r.f.hub.leave())
        first!!.join()
        assertNull(r.f.hub.leave())
        assertEquals(1, r.f.recorder.library.records.size)
        assertTrue(r.balanced())

        // `정리 중` and the memo hold no microphone: leave leaves them alone.
        r.start()
        r.f.hub.stopAndSave()
        assertNull(r.f.hub.leave())
        assertEquals(CaptureStage.PROCESSING, r.f.hub.state.value.stage)
    }

    @Test
    fun U13_systemBackOnARecordingSaves() = runTest {
        val r = Rig(this)
        r.start()
        testScheduler.advanceTimeBy(1_500)
        r.f.hub.back()
        assertEquals(CaptureStage.IDLE, r.f.hub.state.value.stage)
        assertEquals(1, r.f.recorder.library.records.size)
        assertTrue(r.released())
        assertTrue(r.balanced())
    }

    @Test
    fun U13_aTourHoldNeverReopensTheMicAfterLeaving() = runTest {
        val r = Rig(this)
        r.start()
        r.f.hub.holdForTour()
        r.f.hub.leave()!!.join()
        // The tour's own dispose runs after the route left.
        r.f.hub.releaseTour()
        assertFalse(r.takes.micOpen)
        assertEquals(CaptureStage.IDLE, r.f.hub.state.value.stage)
        assertTrue(r.balanced())
    }

    @Test
    fun U13_aLeftTakeLinkedToAnR18ProjectIsSavedHidden() = runTest {
        val r18 = Fx.p1().let { it.copy(project = it.project.copy(rating = Rating.R18.open())) }
        val r = Rig(this, projects = FakeProjects(r18))
        r.start()
        r.f.engine.confirm("숨길 생각")
        r.f.hub.togglePause()
        r.f.hub.leave()!!.join()
        assertTrue(r.f.recorder.library.records.single().hidden)
    }

    @Test
    fun U13_everyOtherExitReleasesTheMicOnce() = runTest {
        // 취소 while paused.
        val a = Rig(this)
        a.start()
        a.f.hub.togglePause()
        a.f.hub.cancel()
        assertTrue(a.released())
        assertTrue(a.balanced())

        // 글로 쓰기 while paused.
        val b = Rig(this)
        b.start()
        b.f.hub.togglePause()
        b.f.hub.toText()
        assertTrue(b.released())
        assertTrue(b.balanced())

        // 멈추고 저장 while paused, then 이어서 말하기 and leave.
        val c = Rig(this)
        c.start()
        c.f.hub.togglePause()
        c.f.hub.stopAndSave()
        assertTrue(c.released())
        c.f.hub.continueSpeaking()
        assertTrue(c.takes.micOpen)
        c.f.hub.leave()!!.join()
        assertTrue(c.released())
        assertTrue(c.balanced())
        val record = c.f.recorder.library.records.single()
        assertNotNull("the continued take's audio is kept with the record", record.audio)
        assertTrue(record.key in c.takes.discarded)
        assertEquals(2, c.takes.opens)
    }

    // ---- the platform takes over a fake microphone ----

    /** Microphones that write `[n]` into their file; [open] counts those not yet stopped. */
    private class FakeMics {
        var open = 0
        var made = 0
        val stops = mutableListOf<String>()

        fun next(): MicSegment = object : MicSegment {
            private val id = made++
            private var path: String? = null

            override fun start(path: String): Boolean {
                this.path = path
                File(path).writeText("[$id]")
                open++
                return true
            }

            override fun maxAmplitude(): Int = 8_000

            override fun stop(): Boolean {
                open--
                stops += path!!
                return true
            }
        }
    }

    private fun tempDir(): File = Files.createTempDirectory("takes").toFile().also { it.deleteOnExit() }

    @Test
    fun U13_platformTakesCloseTheRecorderOnPauseAndJoinTheSegmentsInOrder() {
        val dir = tempDir()
        val mics = FakeMics()
        val takes = MediaRecorderTakes(dir, newSegment = { mics.next() })
        assertTrue(takes.start("K"))
        assertEquals(1, mics.open)
        takes.pause()
        assertEquals("paused: no recorder holds the mic", 0, mics.open)
        assertFalse(takes.capturing)
        takes.pause()
        assertEquals("stopped once", 1, mics.stops.size)
        takes.resume()
        assertEquals(1, mics.open)
        takes.resume()
        assertEquals("no second recorder", 2, mics.made)
        takes.pause()
        takes.resume()
        assertTrue(takes.finish("K"))
        assertEquals(0, mics.open)
        assertEquals("[0][1][2]", TakeFiles.take(dir, "K").readText())
        assertTrue("no segment left over", TakeFiles.parts(dir, "K").isEmpty())
        assertEquals(mics.made, mics.stops.size)
    }

    @Test
    fun U13_finishWhilePausedKeepsTheAudioAndDropOfAContinuedRunKeepsTheTake() {
        val dir = tempDir()
        val mics = FakeMics()
        val takes = MediaRecorderTakes(dir, newSegment = { mics.next() })
        takes.start("K")
        takes.pause()
        assertTrue(takes.finish("K"))
        assertEquals("[0]", TakeFiles.take(dir, "K").readText())

        // 이어서 말하기, a pause, a resume, then 취소: only the new run goes.
        takes.start("K")
        takes.pause()
        takes.resume()
        takes.drop("K")
        assertEquals(0, mics.open)
        assertEquals("[0]", TakeFiles.take(dir, "K").readText())
        assertTrue(TakeFiles.parts(dir, "K").isEmpty())
    }

    @Test
    fun U13_recoveryAppendsEverySegmentInNumericOrder() {
        val dir = tempDir()
        TakeFiles.take(dir, "K").writeText("<")
        TakeFiles.segment(dir, "K").writeText("old")
        for (i in listOf(10, 2, 0, 1)) TakeFiles.part(dir, "K", i).writeText("($i)")
        val takes = MediaRecorderTakes(dir, newSegment = { FakeMics().next() })
        takes.recoverSegment("K")
        assertEquals("<old(0)(1)(2)(10)", TakeFiles.take(dir, "K").readText())
        assertTrue(TakeFiles.parts(dir, "K").isEmpty())
    }

    @Test
    fun U13_theHubOverPlatformTakesSavesOneTakeWithBothSegments() = runTest {
        // Leaving with platform takes closes the recorder too.
        val leftDir = tempDir()
        val leftMics = FakeMics()
        val left = HubFixture(this, takes = MediaRecorderTakes(leftDir, newSegment = { leftMics.next() }))
        left.hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = true)
        testScheduler.advanceTimeBy(1_000)
        left.hub.togglePause()
        left.hub.togglePause()
        left.hub.leave()!!.join()
        assertEquals(0, leftMics.open)
        assertEquals(2, leftMics.stops.size)
        assertNotNull(left.recorder.library.records.single().audio)

        val dir = tempDir()
        val mics = FakeMics()
        val takes = MediaRecorderTakes(dir, newSegment = { mics.next() })
        val f = HubFixture(this, takes = takes)
        f.hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = true)
        val key = f.hub.state.value.session!!.recordKey
        testScheduler.advanceTimeBy(1_000)
        f.hub.togglePause()
        assertEquals(0, mics.open)
        f.hub.togglePause()
        assertEquals(1, mics.open)
        testScheduler.advanceTimeBy(1_000)
        f.hub.togglePause()
        f.hub.stopAndSave()
        assertEquals(0, mics.open)
        assertEquals(mics.made, mics.stops.size)
        assertEquals("one take, both segments in order", "[0][1]", TakeFiles.take(dir, key).readText())
        assertNotNull(f.recorder.library.records.single().audio)
        f.hub.keepInInbox()
        assertFalse(TakeFiles.take(dir, key).exists())
    }
}
