package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.feature.recorder.capture.CaptureViews
import io.github.lenticualrstone.civilight.feature.recorder.capture.EntryRoutes
import io.github.lenticualrstone.civilight.feature.recorder.settings.RecorderSettingId
import io.github.lenticualrstone.civilight.feature.recorder.settings.RecorderSettingsRows
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.recorder.RecorderTranscription
import io.github.lenticualrstone.civilight.model.recorder.TranscriptionMode
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.navigation.Recording
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** `설정 · 라이프 레코더` (LRC-44): rows as drawn on `LrSettings`, the write of each row, and the capture following a change. */
class SettingsTest {
    private val fromScript = Recording(surface = "script", projectKey = Fx.P1, targetKind = "scene", targetKey = Fx.S, caretBlockKey = ExampleKeys.BLOCK_044)

    @Test
    fun LRC_44_rowsSectionsAndDefaultsAsDrawn() {
        val rows = RecorderSettingsRows.of(AppSettings())
        assertEquals(RecorderSettingId.entries, rows.map { it.id })
        assertEquals(listOf("받아쓰기", null, "녹음", null, null, null), rows.map { it.row.section })
        assertEquals(listOf("기기 안", "클라우드", "잠금 화면에서 녹음", "원본 음성 보관", "녹음 중 빛 띠", "보던 곳 연결"), rows.map { it.row.title })
        assertEquals(
            listOf(
                "오프라인 · 음성이 기기 밖으로 나가지 않음", "정확도↑ · 음성 전송 · 인터넷 필요", "새 녹음만 · 기록함은 잠금 해제 후",
                "글과 함께 · 30일 뒤 정리 가능", "말소리 크기 표시", "시작한 곳을 칩으로 · 언제든 빼기",
            ),
            rows.map { it.row.sub },
        )
        assertEquals(listOf(true, true, false, false, false, false), rows.map { it.row.radio })
        // Defaults: 기기 안 chosen, every switch on.
        assertEquals(listOf(true, false, true, true, true, true), rows.map { it.row.on })
    }

    @Test
    fun LRC_44_eachRowWritesItsOwnSetting() {
        var s = AppSettings()
        s = RecorderSettingsRows.apply(s, RecorderSettingId.CLOUD, true)
        assertEquals(RecorderTranscription.CLOUD, s.recorder.transcription)
        assertEquals(listOf(false, true), RecorderSettingsRows.of(s).take(2).map { it.row.on })
        s = RecorderSettingsRows.apply(s, RecorderSettingId.ON_DEVICE, true)
        assertEquals(RecorderTranscription.ON_DEVICE, s.recorder.transcription)
        s = RecorderSettingsRows.apply(s, RecorderSettingId.LOCK_SCREEN, false)
        assertFalse(s.recorder.lockScreenRecording)
        s = RecorderSettingsRows.apply(s, RecorderSettingId.KEEP_AUDIO, false)
        assertFalse(s.recorder.keepAudio)
        // The glow row is the app-wide value shared with 설정 · 움직임과 투명도.
        s = RecorderSettingsRows.apply(s, RecorderSettingId.GLOW, false)
        assertFalse(s.recorderGlow)
        s = RecorderSettingsRows.apply(s, RecorderSettingId.PRESET_LINK, false)
        assertFalse(s.recorder.presetLink)
        assertEquals(listOf(true, false, false, false, false, false), RecorderSettingsRows.of(s).map { it.row.on })
    }

    @Test
    fun LRC_44_aChangeAfterTheCaptureExistsAppliesToTheNextRecording() = runTest {
        val takes = FakeTakes()
        val f = HubFixture(this, takes = takes)
        // 보던 곳 연결 off, set while the process-wide hub already exists.
        f.settings = RecorderSettingsRows.apply(f.settings, RecorderSettingId.PRESET_LINK, false)
        f.hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = true)
        assertEquals(emptyList<String>(), CaptureViews.recording(f.hub.state.value)!!.screen.chips.map { it.text })
        // 원본 음성 보관 off before 멈추고 저장: no audio kept.
        f.settings = RecorderSettingsRows.apply(f.settings, RecorderSettingId.KEEP_AUDIO, false)
        f.engine.confirm("생각 하나")
        testScheduler.advanceTimeBy(3_000)
        f.hub.stopAndSave()
        val first = f.recorder.library.records.single()
        assertNull(first.audio)
        f.hub.keepInInbox()

        // Back on: the next take keeps its audio and pre-fills the chip; already kept audio is never touched.
        f.settings = RecorderSettingsRows.apply(RecorderSettingsRows.apply(f.settings, RecorderSettingId.KEEP_AUDIO, true), RecorderSettingId.PRESET_LINK, true)
        f.hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = true)
        assertEquals(listOf("연결 · 1-7 씬 3"), CaptureViews.recording(f.hub.state.value)!!.screen.chips.map { it.text })
        f.engine.confirm("생각 둘")
        testScheduler.advanceTimeBy(3_000)
        f.hub.stopAndSave()
        assertNotNull(f.recorder.library.records.single { it.key != first.key }.audio)
        assertNull(f.recorder.library.record(first.key)!!.audio)
    }

    @Test
    fun LRC_44_cloudChosenLaterIsUsedByTheNextTake() = runTest {
        val cloud = FakeEngine(available = true)
        val device = FakeEngine(available = true)
        val f = HubFixture(this, online = true, engines = { mode -> if (mode == TranscriptionMode.CLOUD) cloud else device })
        f.settings = RecorderSettingsRows.apply(f.settings, RecorderSettingId.CLOUD, true)
        f.hub.begin(EntryRoutes.entryOf(fromScript), startTyped = false, micGranted = true)
        assertTrue(cloud.running)
        assertFalse(device.running)
    }
}
