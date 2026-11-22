package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.feature.library.settings.AppLockChange
import io.github.lenticualrstone.civilight.feature.library.settings.AppLockRules
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsCopy
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsState
import io.github.lenticualrstone.civilight.model.app.AnimationPref
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import io.github.lenticualrstone.civilight.model.app.ThemeMode
import io.github.lenticualrstone.civilight.model.app.UnfoldedLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 설정 (LIB-63), 설정 · 숨기기 (LIB-64), 화면 · 글자 크기 (LIB-65): the pure row builders and rules. */
class SettingsTest {
    private val now = LibFx.NOW
    private val zone = LibFx.ZONE

    @Test
    fun `LIB-63 the root reads as the Settings board with the factory settings`() {
        val ui = SettingsState.root(AppSettings(), hiddenProjects = 2, recoverySet = true, lastBackupAt = LibFx.at("2026-11-22T08:12"), now = now, zone = zone)
        assertEquals(0, ui.themeIndex)
        assertEquals(listOf("기기 설정", "라이트", "다크"), SettingsState.THEME_LABELS)
        assertEquals("글자 크기", ui.textSize.title)
        assertEquals("본문 18 · 행간 1.7", ui.textSize.sub)
        assertEquals("보통", ui.textSize.value)
        assertEquals("서재 · 인물 · 세계 · 연표 …", ui.layout.sub)
        assertEquals("목록 + 상세", ui.layout.value)
        assertEquals("애니메이션 줄이기 · 투명도 줄이기", ui.motion.sub)
        assertEquals("기기 설정", ui.motion.value)
        assertEquals("색면 · 홀로 · 생성 방식 · 이동 거리", ui.more.sub)
        assertEquals("Google Drive 자동 백업", ui.drive.title)
        assertEquals("마지막 백업 오늘 08:12", ui.drive.sub)
        assertEquals("숨긴 프로젝트 백업용 · 정해 둠", ui.recovery.sub)
        assertEquals("바꾸기", ui.recovery.value)
        assertEquals("앱 잠금", ui.appLock.title)
        assertEquals("지문으로 열기", ui.appLock.sub)
        assertFalse(ui.appLock.on)
        assertEquals("숨긴 프로젝트 2 · 다시 잠그는 때", ui.hiding.sub)
        assertEquals("앱을 떠날 때", ui.hiding.value)
    }

    @Test
    fun `LIB-63 rows follow the stored values, no backup yet leaves the Drive sub empty`() {
        val settings = AppSettings(
            theme = ThemeMode.DARK,
            textSizeStep = 2,
            unfoldedLayout = UnfoldedLayout.CENTER_COLUMN,
            animation = AnimationPref.ALWAYS_REDUCE,
            appLock = true,
            relockPolicy = RelockPolicy.FIVE_MINUTES,
        )
        val ui = SettingsState.root(settings, hiddenProjects = 0, recoverySet = false, lastBackupAt = null, now = now, zone = zone)
        assertEquals(2, ui.themeIndex)
        assertEquals("본문 22 · 행간 1.7", ui.textSize.sub)
        assertNull("only 보통 has a name", ui.textSize.value)
        assertEquals("가운데 한 칸", ui.layout.value)
        assertEquals("항상 줄이기", ui.motion.value)
        assertNull(ui.drive.sub)
        assertFalse("Drive needs an account first (LIB-57)", ui.drive.enabled)
        assertEquals("숨긴 프로젝트 백업용", ui.recovery.sub)
        assertEquals("정하기", ui.recovery.value)
        assertTrue(ui.appLock.on)
        assertEquals("연 지 5분 뒤", ui.hiding.value)
    }

    @Test
    fun `LIB-64 the HideSettings rows, one radio selected, the second mask switch locked on`() {
        val ui = SettingsState.hiding(AppSettings(relockPolicy = RelockPolicy.SCREEN_OFF_ONLY, hiddenInSearchWhileOpen = false), deviceSecure = true)
        assertEquals(listOf("앱을 떠날 때", "화면이 꺼질 때만", "연 지 5분 뒤"), ui.relock.map { it.title })
        assertEquals(listOf("다른 앱 · 화면 꺼짐", "다른 앱 다녀와도 유지", null), ui.relock.map { it.sub })
        assertEquals(listOf(RelockPolicy.SCREEN_OFF_ONLY), ui.relock.filter { it.selected }.map { it.policy })
        assertEquals("검색 · 명령 결과", ui.searchResults.title)
        assertEquals("열려 있을 때만 결과에 포함", ui.searchResults.sub)
        assertFalse(ui.searchResults.on)
        assertTrue("§6 Q28: kept on", ui.widgets.on)
        assertFalse("§6 Q28: not switchable until decided", ui.widgets.enabled)
        assertEquals("위젯 · 최근 항목 · 검색에서 빠짐 · 알림은 ‘숨긴 프로젝트’", ui.widgets.sub)
        assertEquals("사용 중", ui.biometricStatus)
        assertEquals("Google Drive · 복구 암호로 잠금", ui.backupHidden.sub)
        assertTrue(ui.backupHidden.on)
        assertEquals("R-18 · 숨기기 · 캡처 가림 · 이름 가림", ui.ratingRules.sub)
        assertNull("no device lock: no status", SettingsState.hiding(AppSettings(), deviceSecure = false).biometricStatus)
        assertEquals("프로젝트 단위 기본 · 앱 잠금과 별도", SettingsCopy.HIDING_INTRO)
    }

    @Test
    fun `LIB-65 five text steps around 보통 map onto the slider and back`() {
        assertEquals(listOf(16, 17, 18, 20, 22), (-2..2).map { SettingsState.textSizeLabel(it).removePrefix("본문 ").substringBefore(" ").toInt() })
        assertEquals(2, SettingsState.sliderIndexOf(0))
        assertEquals(0, SettingsState.stepOfSlider(2))
        assertEquals(-2, SettingsState.stepOfSlider(0))
        assertEquals(2, SettingsState.stepOfSlider(9))
        assertEquals(4, SettingsState.sliderIndexOf(7))
        val ui = SettingsState.textSize(AppSettings(textSizeStep = -1, theme = ThemeMode.LIGHT))
        assertEquals(1, ui.sliderIndex)
        assertEquals(5, ui.stepCount)
        assertEquals(1, ui.themeIndex)
        assertEquals("본문 17 · 행간 1.7", ui.label)
    }

    @Test
    fun `LIB-35 앱 잠금 needs a device lock to turn on and a prompt to turn off`() {
        assertEquals(AppLockChange.NONE, AppLockRules.change(current = false, wanted = false, deviceSecure = true))
        assertEquals(AppLockChange.TURN_ON, AppLockRules.change(current = false, wanted = true, deviceSecure = true))
        assertEquals(AppLockChange.REFUSED_NO_DEVICE_LOCK, AppLockRules.change(current = false, wanted = true, deviceSecure = false))
        assertEquals(AppLockChange.TURN_OFF_AFTER_AUTH, AppLockRules.change(current = true, wanted = false, deviceSecure = false))
    }
}
