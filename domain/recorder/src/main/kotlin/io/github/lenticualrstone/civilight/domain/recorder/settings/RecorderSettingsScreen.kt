package io.github.lenticualrstone.civilight.domain.recorder.settings

import io.github.lenticualrstone.civilight.model.recorder.RecorderSettings
import io.github.lenticualrstone.civilight.model.recorder.RecorderTranscription

/** One row of `설정 · 라이프 레코더` (LRC-44). [radio] rows belong to the `받아쓰기` group. */
data class SettingsRow(val section: String?, val title: String, val sub: String, val radio: Boolean, val on: Boolean)

/** `설정 · 라이프 레코더`: header small `설정`, title `라이프 레코더`. The glow row is `AppSettings.recorderGlow`. */
object RecorderSettingsScreen {
    const val SMALL = "설정"
    const val TITLE = "라이프 레코더"

    fun rows(settings: RecorderSettings, recorderGlow: Boolean): List<SettingsRow> = listOf(
        SettingsRow("받아쓰기", "기기 안", "오프라인 · 음성이 기기 밖으로 나가지 않음", true, settings.transcription == RecorderTranscription.ON_DEVICE),
        SettingsRow(null, "클라우드", "정확도↑ · 음성 전송 · 인터넷 필요", true, settings.transcription == RecorderTranscription.CLOUD),
        SettingsRow("녹음", "잠금 화면에서 녹음", "새 녹음만 · 기록함은 잠금 해제 후", false, settings.lockScreenRecording),
        SettingsRow(null, "원본 음성 보관", "글과 함께 · 30일 뒤 정리 가능", false, settings.keepAudio),
        SettingsRow(null, "녹음 중 빛 띠", "말소리 크기 표시", false, recorderGlow),
        SettingsRow(null, "보던 곳 연결", "시작한 곳을 칩으로 · 언제든 빼기", false, settings.presetLink),
    )
}
