package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.text.Runs
import io.github.lenticualrstone.civilight.feature.recorder.audio.RecordPlayer
import io.github.lenticualrstone.civilight.feature.recorder.audio.Waveforms
import io.github.lenticualrstone.civilight.feature.recorder.capture.CancelOutcome
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureStage
import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureViews
import io.github.lenticualrstone.civilight.feature.recorder.capture.EntryRoutes
import io.github.lenticualrstone.civilight.feature.recorder.inbox.InboxViews
import io.github.lenticualrstone.civilight.feature.recorder.speech.CloudSpeechEngine
import io.github.lenticualrstone.civilight.feature.recorder.speech.EngineChoice
import io.github.lenticualrstone.civilight.feature.recorder.speech.UnavailableSpeechEngine
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.recorder.InboxFilterState
import io.github.lenticualrstone.civilight.model.recorder.RecorderAudio
import io.github.lenticualrstone.civilight.model.recorder.RecorderSettings
import io.github.lenticualrstone.civilight.model.recorder.RecorderTranscription
import io.github.lenticualrstone.civilight.model.recorder.TranscriptionMode
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.navigation.Recording
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Original audio (LRC-14), the voice ↔ text switch with audio (LRC-16) and the engine choice (LRC-11, LRC-12). */
class AudioAndEngineTest {
    private val fromScript = Recording(surface = "script", projectKey = Fx.P1, targetKind = "scene", targetKey = Fx.S, caretBlockKey = ExampleKeys.BLOCK_044)

    private suspend fun HubFixture.record(scope: TestScope, words: String, ms: Long) {
        hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = true)
        engine.confirm(words)
        scope.testScheduler.advanceTimeBy(ms)
        hub.stopAndSave()
    }

    // ---- LRC-14 ----

    @Test
    fun LRC_14_theTakeIsKeptWithTheRecordAndTheNoteSaysSo() = runTest {
        val takes = FakeTakes()
        val f = HubFixture(this, takes = takes)
        f.record(this, Fx.R1_TEXT, 24_000)
        val record = f.recorder.library.records.single()
        val audio = record.audio!!
        assertEquals("h-${record.key}.aac", audio.hash)
        assertEquals("audio/aac", audio.mime)
        assertEquals(24_000L, audio.durationMs)
        assertEquals(64, audio.envelopeBytes()!!.size)
        assertTrue(f.hub.state.value.audioKept)
        assertEquals(RecorderCopy.AUDIO_KEPT_NOTE, CaptureViews.processing(f.hub.state.value)!!.screen.note)
        // `기록함에 두기`: the take file goes (the blob store holds the kept copy).
        f.hub.keepInInbox()
        assertTrue(record.key in takes.discarded)
        assertEquals("takes/${record.key}.aac", f.recorder.audioFile(audio.hash)!!.path)
    }

    @Test
    fun LRC_14_keepAudioOffKeepsTheDurationButNoPlayer() = runTest {
        val takes = FakeTakes()
        val f = HubFixture(this, takes = takes, settings = AppSettings(recorder = RecorderSettings(keepAudio = false)))
        f.record(this, Fx.R1_TEXT, 24_000)
        val record = f.recorder.library.records.single()
        assertNull(record.audio)
        assertEquals(24_000L, record.durationMs)
        assertEquals(RecorderCopy.TODO_AUDIO_NOT_KEPT, CaptureViews.processing(f.hub.state.value)!!.screen.note)
        f.hub.keepInInbox()
        assertTrue(record.key in takes.discarded)
        assertTrue(f.recorder.blobs.isEmpty())
    }

    @Test
    fun LRC_14_continueSpeakingAppendsToTheSameTake() = runTest {
        val takes = FakeTakes()
        val f = HubFixture(this, takes = takes)
        f.record(this, "첫 생각", 4_000)
        f.hub.continueSpeaking()
        assertTrue(takes.capturing)
        f.engine.confirm("둘째")
        testScheduler.advanceTimeBy(6_000)
        f.hub.stopAndSave()
        val record = f.recorder.library.records.single()
        assertEquals(2, takes.segments[record.key])
        assertEquals(2_000L, takes.take(record.key)!!.bytes)
        assertEquals(10_000L, record.audio!!.durationMs)
        assertEquals("첫 생각 둘째", Runs.text(record.runs))
    }

    @Test
    fun LRC_14_cancelOfAContinuedTakeDropsOnlyItsSegment() = runTest {
        val takes = FakeTakes()
        val f = HubFixture(this, takes = takes)
        f.record(this, "첫 생각", 4_000)
        val key = f.recorder.library.records.single().key
        f.hub.continueSpeaking()
        testScheduler.advanceTimeBy(2_000)
        assertEquals(CancelOutcome.BackToProcessing, f.hub.cancel())
        assertEquals(1, takes.segments[key])
        assertFalse(takes.capturing)
        assertNotNull(f.recorder.library.record(key)!!.audio)
    }

    @Test
    fun LRC_14_theMicrophoneLevelDrivesTheGlow() = runTest {
        val takes = FakeTakes(level = 0.6f)
        val f = HubFixture(this, takes = takes)
        f.hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = true)
        testScheduler.advanceTimeBy(1_000)
        runCurrent()
        assertEquals(0.6f, f.hub.level.value, 0.0001f)
        f.hub.togglePause()
        assertTrue(takes.paused)
        assertEquals(0f, f.hub.level.value, 0.0001f)
    }

    @Test
    fun LRC_14_inboxRowsCarryTheKeptAudioForThePlayer() = runTest {
        val f = HubFixture(this, takes = FakeTakes())
        f.record(this, Fx.R1_TEXT, 24_000)
        f.hub.keepInInbox()
        val view = InboxViews.of(f.recorder.library, InboxFilterState(), f.env.env, Fx.NOW)
        val row = view.rows.single()
        assertTrue(row.row.hasPlayer)
        assertNotNull(row.audio)
    }

    @Test
    fun LRC_14_waveformBarsFromTheEnvelope() {
        val samples = ByteArray(200) { i -> (i % 50 * 5).toByte() }
        val envelope = Waveforms.envelopeOf(samples)
        assertEquals(Waveforms.ENVELOPE_SIZE, envelope.size)
        val bars = Waveforms.bars(envelope)
        assertEquals(20, bars.size)
        assertTrue(bars.all { it in 0f..1f })
        // Without an envelope the bars are flat.
        assertEquals(List(20) { 0.3f }, Waveforms.bars(null))
        assertEquals(Waveforms.MIN_DP, Waveforms.heightDp(0f), 0.0001f)
        assertEquals(Waveforms.MAX_DP, Waveforms.heightDp(1f), 0.0001f)
        assertEquals(0, Waveforms.playedBars(0, 24_000))
        assertEquals(10, Waveforms.playedBars(12_000, 24_000))
        assertEquals(20, Waveforms.playedBars(30_000, 24_000))
        assertArrayEquals(ByteArray(0), Waveforms.envelopeOf(ByteArray(0)))
    }

    @Test
    fun LRC_14_onlyOneRecordPlaysAtATime() = runTest {
        val playback = FakePlayback()
        val files = mapOf("a" to "/a.aac", "b" to "/b.aac")
        val player = RecordPlayer(backgroundScope, playback) { files[it] }
        val a = RecorderAudio(hash = "a", mime = "audio/aac", bytes = 1, durationMs = 24_000)
        val b = RecorderAudio(hash = "b", mime = "audio/aac", bytes = 1, durationMs = 62_000)

        player.toggle("R1", a)
        assertTrue(player.state.value.playing("R1"))
        assertEquals("/a.aac", playback.path)
        playback.at = 5_000
        player.toggle("R1", a)
        assertFalse(player.state.value.playing("R1"))
        assertEquals(5_000L, player.state.value.positionOf("R1"))
        // Play again resumes where it was.
        player.toggle("R1", a)
        assertEquals(5_000L, player.state.value.positionOf("R1"))

        player.toggle("R3", b)
        assertFalse(player.state.value.playing("R1"))
        assertTrue(player.state.value.playing("R3"))
        assertEquals(0L, player.state.value.positionOf("R1"))
        assertEquals("/b.aac", playback.path)

        playback.finish()
        assertFalse(player.state.value.playing("R3"))
        assertEquals(0L, player.state.value.positionOf("R3"))

        // A file the store no longer has plays nothing.
        player.toggle("R9", a.copy(hash = "gone"))
        assertNull(player.state.value.recordKey)
        player.stop()
        assertNull(player.state.value.recordKey)
    }

    // ---- LRC-16 with audio ----

    @Test
    fun LRC_16_toTextKeepsTheAudioOnTheVoiceRecord() = runTest {
        val takes = FakeTakes()
        val f = HubFixture(this, takes = takes)
        f.hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = true)
        val key = f.hub.state.value.session!!.recordKey
        f.engine.confirm("하르트가 등불을")
        testScheduler.advanceTimeBy(9_000)
        f.hub.toText()
        assertEquals(CaptureStage.TYPING, f.hub.state.value.stage)
        assertFalse(takes.capturing)
        f.hub.memoSave()
        val record = f.recorder.library.record(key)!!
        assertEquals(9_000L, record.audio!!.durationMs)
        assertTrue(key in takes.discarded)
    }

    @Test
    fun LRC_16_closingTheSwitchedMemoDropsItsTake() = runTest {
        val takes = FakeTakes()
        val f = HubFixture(this, takes = takes)
        f.hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = true)
        val key = f.hub.state.value.session!!.recordKey
        f.engine.confirm("메모")
        testScheduler.advanceTimeBy(2_000)
        f.hub.toText()
        f.hub.memoClose()
        assertTrue(key in takes.discarded)
        assertTrue(f.recorder.blobs.isEmpty())
    }

    // ---- LRC-09 with audio ----

    @Test
    fun LRC_09_recoveryAppendsTheCutOffSegmentAndKeepsTheAudio() = runTest {
        val takes = FakeTakes()
        val f = HubFixture(this, takes = takes)
        f.hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = true)
        f.engine.confirm("끊긴 녹음")
        testScheduler.advanceTimeBy(6_000)
        runCurrent()
        val session = f.recorder.session!!
        // The process ends: a new hub over the same files.
        val again = HubFixture(this, recorder = f.recorder, takes = takes)
        again.hub.pruneTakes(session.recordKey)
        assertEquals(setOf(session.recordKey), takes.pruneKept)
        assertNotNull(again.hub.recover(session))
        assertTrue(session.recordKey in takes.recovered)
        assertNotNull(f.recorder.library.record(session.recordKey)!!.audio)
        assertTrue(again.hub.state.value.audioKept)
    }

    // ---- LRC-11 / LRC-12 ----

    @Test
    fun LRC_12_cloudOnlyWhenChosenOnlineAndAvailable() {
        val cloud = FakeEngine(available = true)
        val device = FakeEngine(available = true)
        val factory = { mode: TranscriptionMode -> if (mode == TranscriptionMode.CLOUD) cloud else device }
        assertEquals(TranscriptionMode.CLOUD, EngineChoice.choose(RecorderTranscription.CLOUD, online = true, factory).mode)
        assertTrue(EngineChoice.choose(RecorderTranscription.CLOUD, online = true, factory).engine === cloud)
        // No network at recording time → on-device for that take ([PROPOSED]).
        assertEquals(TranscriptionMode.ON_DEVICE, EngineChoice.choose(RecorderTranscription.CLOUD, online = false, factory).mode)
        assertEquals(TranscriptionMode.ON_DEVICE, EngineChoice.choose(RecorderTranscription.ON_DEVICE, online = true, factory).mode)
        // The cloud provider is the needsExternal stub: unavailable, so the take falls back.
        val stub = { mode: TranscriptionMode -> if (mode == TranscriptionMode.CLOUD) CloudSpeechEngine() else UnavailableSpeechEngine() }
        val chosen = EngineChoice.choose(RecorderTranscription.CLOUD, online = true, stub)
        assertEquals(TranscriptionMode.ON_DEVICE, chosen.mode)
        assertFalse(chosen.engine.available)
    }

    @Test
    fun LRC_12_theRecordNamesTheEngineThatTranscribedIt() = runTest {
        val cloud = FakeEngine(available = true)
        val device = FakeEngine(available = true)
        val settings = AppSettings(recorder = RecorderSettings(transcription = RecorderTranscription.CLOUD))
        val f = HubFixture(this, settings = settings, online = true, engines = { mode -> if (mode == TranscriptionMode.CLOUD) cloud else device })
        f.hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = true)
        assertTrue(cloud.running)
        assertFalse(device.running)
        cloud.confirm("클라우드로")
        testScheduler.advanceTimeBy(2_000)
        f.hub.stopAndSave()
        assertEquals(TranscriptionMode.CLOUD, f.recorder.library.records.single().transcribedBy?.known)

        val offline = HubFixture(this, settings = settings, online = false, engines = { mode -> if (mode == TranscriptionMode.CLOUD) cloud else device })
        offline.hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = true)
        assertTrue(device.running)
        device.confirm("기기에서")
        testScheduler.advanceTimeBy(2_000)
        offline.hub.stopAndSave()
        assertEquals(TranscriptionMode.ON_DEVICE, offline.recorder.library.records.single().transcribedBy?.known)
    }
}
