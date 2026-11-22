package io.github.lenticualrstone.civilight.feature.recorder.settings

import io.github.lenticualrstone.civilight.domain.recorder.settings.RecorderSettingsScreen
import io.github.lenticualrstone.civilight.domain.recorder.settings.SettingsRow
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.recorder.RecorderTranscription

/** What each row of `설정 · 라이프 레코더` changes (LRC-44), in the rows' order. */
enum class RecorderSettingId {
    /** `받아쓰기 · 기기 안` (LRC-11). */
    ON_DEVICE,

    /** `받아쓰기 · 클라우드` (LRC-12): read by `EngineChoice` when a take starts. */
    CLOUD,

    /** `잠금 화면에서 녹음` (LRC-05). */
    LOCK_SCREEN,

    /** `원본 음성 보관` (LRC-14): read at `멈추고 저장`; off never deletes audio already kept. */
    KEEP_AUDIO,

    /** `녹음 중 빛 띠` (LRC-10): `AppSettings.recorderGlow`, the same value as 설정 · 움직임과 투명도 `라이프 레코더 빛`. */
    GLOW,

    /** `보던 곳 연결` (LRC-19). */
    PRESET_LINK,
}

/** One drawn row: the domain row (section, title, sub, radio or switch, value) and what it changes. */
data class RecorderSettingRowView(val id: RecorderSettingId, val row: SettingsRow)

/**
 * `설정 · 라이프 레코더` as pure Kotlin: the rows of `domain:recorder` [RecorderSettingsScreen.rows]
 * (copy and defaults there) with their ids, and the one write each row makes. The settings are read
 * live by the capture (`CaptureHub` reads `settings()` when a take starts, the glow when it draws),
 * so a change applies from the next recording.
 */
object RecorderSettingsRows {
    private val ORDER = RecorderSettingId.entries

    fun of(settings: AppSettings): List<RecorderSettingRowView> {
        val rows = RecorderSettingsScreen.rows(settings.recorder, settings.recorderGlow)
        check(rows.size == ORDER.size) { "RecorderSettingsScreen.rows changed: ${rows.size} rows" }
        return rows.zip(ORDER) { row, id -> RecorderSettingRowView(id, row) }
    }

    /** The settings after a row is chosen (radio) or switched to [on]. A radio row is only ever chosen. */
    fun apply(settings: AppSettings, id: RecorderSettingId, on: Boolean): AppSettings = when (id) {
        RecorderSettingId.ON_DEVICE -> settings.copy(recorder = settings.recorder.copy(transcription = RecorderTranscription.ON_DEVICE))
        RecorderSettingId.CLOUD -> settings.copy(recorder = settings.recorder.copy(transcription = RecorderTranscription.CLOUD))
        RecorderSettingId.LOCK_SCREEN -> settings.copy(recorder = settings.recorder.copy(lockScreenRecording = on))
        RecorderSettingId.KEEP_AUDIO -> settings.copy(recorder = settings.recorder.copy(keepAudio = on))
        RecorderSettingId.GLOW -> settings.copy(recorderGlow = on)
        RecorderSettingId.PRESET_LINK -> settings.copy(recorder = settings.recorder.copy(presetLink = on))
    }
}
