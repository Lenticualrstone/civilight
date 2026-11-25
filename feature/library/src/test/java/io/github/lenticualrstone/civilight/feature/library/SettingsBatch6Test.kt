package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.feature.library.backup.BackupBook
import io.github.lenticualrstone.civilight.feature.library.backup.BackupSettingsState
import io.github.lenticualrstone.civilight.feature.library.backup.StorageFacts
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectForm
import io.github.lenticualrstone.civilight.feature.library.newproject.ProjectCreator
import io.github.lenticualrstone.civilight.feature.library.projectsettings.ProjectSettingsRows
import io.github.lenticualrstone.civilight.feature.library.projectsettings.ProjectSettingsTarget
import io.github.lenticualrstone.civilight.feature.library.projectsettings.routeOf
import io.github.lenticualrstone.civilight.feature.library.settings.DisplaySettingsState
import io.github.lenticualrstone.civilight.feature.library.settings.MoreValue
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsRootRow
import io.github.lenticualrstone.civilight.navigation.TemplateList
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsRoutes
import io.github.lenticualrstone.civilight.model.app.AnimationPref
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.BackupState
import io.github.lenticualrstone.civilight.model.app.LargeMapExport
import io.github.lenticualrstone.civilight.model.app.MapStylePref
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.app.SceneNotationPref
import io.github.lenticualrstone.civilight.model.app.TextMapEngine
import io.github.lenticualrstone.civilight.model.project.SceneNotation
import io.github.lenticualrstone.civilight.navigation.HelpReplay
import io.github.lenticualrstone.civilight.navigation.RecorderSettings
import io.github.lenticualrstone.civilight.navigation.RecoveryPassword
import io.github.lenticualrstone.civilight.navigation.SettingsBackup
import io.github.lenticualrstone.civilight.navigation.SettingsHiding
import io.github.lenticualrstone.civilight.navigation.SettingsMore
import io.github.lenticualrstone.civilight.navigation.SettingsMotion
import io.github.lenticualrstone.civilight.navigation.SettingsTextSize
import io.github.lenticualrstone.civilight.navigation.TagSettings
import io.github.lenticualrstone.civilight.navigation.Trash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Batch 6: 설정 › 보관 failure banner and storage (LIB-61, LIB-62), 펼친 화면 배치 (LIB-66), 움직임 ·
 * 투명도 (LIB-67), 지도 · 이미지 · 연표 (LIB-68, CNV-19, WLD-67), the row maps (CNV-18, CNV-20) and the
 * `때 표기 기본` of new projects (STO-17).
 */
class SettingsBatch6Test {
    private val zone = LibFx.ZONE

    @Test
    fun `B-09 a 3-day streak shows the banner and the Drive row reads the last success`() {
        val now = LibFx.at("2026-11-22T10:00")
        val book = BackupBook(
            state = BackupState(
                lastSuccessAt = LibFx.at("2026-11-18T08:12"),
                failureStreakStartedAt = LibFx.at("2026-11-20T08:12"),
                lastFailureReason = "loginExpired",
            ),
        )
        val ui = BackupSettingsState.build(AppSettings(driveBackup = true), book, emptyList(), now, zone)
        val banner = ui.banner!!
        assertEquals("백업 실패 · 3일째", banner.title)
        assertEquals("Google 로그인 만료 · 원고는 기기에 안전", banner.sub)
        assertEquals(listOf("다시 로그인", "지금 다시 시도"), banner.buttons)
        assertEquals("마지막 성공 11월 18일 08:12", ui.drive.sub)
        // `ED`: with the switch off nothing runs, so no failure is shown.
        assertNull(BackupSettingsState.build(AppSettings(driveBackup = false), book, emptyList(), now, zone).banner)
    }

    @Test
    fun `B-10 a failure today is day 1, and a success clears the banner`() {
        val now = LibFx.at("2026-11-22T10:00")
        val failing = BackupBook(state = BackupState(lastSuccessAt = null, failureStreakStartedAt = LibFx.at("2026-11-22T08:12"), lastFailureReason = "loginExpired"))
        val ui = BackupSettingsState.build(AppSettings(driveBackup = true), failing, emptyList(), now, zone)
        assertEquals("백업 실패 · 1일째", ui.banner!!.title)
        assertEquals("never succeeded: the plain sub", "하루 한 번 · 와이파이 · 원본은 기기에", ui.drive.sub)

        val ok = BackupBook(state = BackupState(lastSuccessAt = LibFx.at("2026-11-22T09:00")))
        val after = BackupSettingsState.build(AppSettings(driveBackup = true), ok, emptyList(), now, zone)
        assertNull(after.banner)
        assertEquals("마지막 백업 오늘 09:00", after.drive.sub)
    }

    @Test
    fun `LIB-62 기기 저장 공간 reads the ErrBackup sizes and waits for them`() {
        val gb = 1024L * 1024 * 1024
        val mb = 1024L * 1024
        val facts = StorageFacts(freeBytes = (1.2 * gb).toLong(), projectBytes = 340 * mb)
        assertEquals("남은 공간 1.2 GB · 프로젝트 340 MB", facts.line)
        val ui = BackupSettingsState.build(AppSettings(), BackupBook(), emptyList(), LibFx.NOW, zone, facts)
        assertEquals("기기 저장 공간", ui.storage.title)
        assertEquals("남은 공간 1.2 GB · 프로젝트 340 MB", ui.storage.sub)
        assertNull("no detail screen is drawn: no value", ui.storage.value)
        assertNull(BackupSettingsState.build(AppSettings(), BackupBook(), emptyList(), LibFx.NOW, zone).storage.sub)
    }

    @Test
    fun `LIB-67 the ReduceSettings rows and their factory values`() {
        val ui = DisplaySettingsState.motion(AppSettings())
        assertEquals(listOf("기기 설정 따름", "항상 줄이기", "줄이지 않음"), ui.animation.map { it.title })
        assertEquals(listOf("‘애니메이션 삭제’가 켜져 있으면 줄임", "이동 없이 페이드 120 ms", null), ui.animation.map { it.sub })
        assertEquals(AnimationPref.FOLLOW_SYSTEM, ui.animation.single { it.selected }.pref)
        assertEquals("투명도 줄이기", ui.reduceTransparency.title)
        assertFalse(ui.reduceTransparency.on)
        assertEquals("라이프 레코더 빛", ui.recorderGlow.title)
        assertTrue("Q-ST-04: the recorder's 녹음 중 빛 띠 value", ui.recorderGlow.on)
        assertFalse(DisplaySettingsState.motion(AppSettings(recorderGlow = false)).recorderGlow.on)
    }

    @Test
    fun `the map section draws the new-map style and large export rows, the unread text-terrain engine stays hidden`() {
        val ui = DisplaySettingsState.more(AppSettings())
        assertEquals(listOf(MoreValue.MAP_STYLE, MoreValue.LARGE_EXPORT), ui.mapRows.map { it.value })
    }

    @Test
    fun `LIB-68 CNV-19 WLD-67 the SettingsMore rows with the factory values`() {
        val ui = DisplaySettingsState.more(AppSettings())
        assertEquals("홀로그램 효과 기본", ui.holo.title)
        assertFalse(ui.holo.on)
        assertEquals("보기", ui.presets.value)
        assertEquals("8종 · 앱에 포함 · 오프라인", ui.presets.sub)
        assertEquals("색면", ui.mapStyle.current)
        assertEquals("색면 · 먹선 · 색 · 양피지", ui.mapStyle.sub)
        assertEquals("기기 안", ui.terrain.current)
        assertEquals("기기 안 · 클라우드(정확도↑ · 전송)", ui.terrain.sub)
        assertEquals("4K", ui.largeExport.current)
        assertEquals("에피소드 기준", ui.notation.current)
        assertEquals("새 프로젝트 · 프로젝트마다 바꾸기 가능", ui.notation.sub)
        assertEquals("이동 시간 부족 · 같은 때 · 동시에 두 장소", ui.checkTravel.sub)
        // (0.2.0, S-2) The travel check is off by default.
        assertFalse(ui.checkTravel.on)
        assertEquals("점검 센터 ◇", ui.checkNoPlace.sub)
        assertTrue(ui.checkNoPlace.on)
    }

    @Test
    fun `value pickers list every choice in the sub's order and write only what was picked`() {
        val settings = AppSettings()
        assertEquals(listOf("색면", "먹선", "색", "양피지"), DisplaySettingsState.options(MoreValue.MAP_STYLE, settings).map { it.title })
        assertEquals(listOf("기기 안", "클라우드"), DisplaySettingsState.options(MoreValue.TERRAIN_ENGINE, settings).map { it.title })
        assertEquals(listOf(null, "정확도↑ · 전송"), DisplaySettingsState.options(MoreValue.TERRAIN_ENGINE, settings).map { it.sub })
        assertEquals(listOf("4K", "8K"), DisplaySettingsState.options(MoreValue.LARGE_EXPORT, settings).map { it.title })
        assertEquals(listOf("에피소드 기준", "이야기 시작부터"), DisplaySettingsState.options(MoreValue.NOTATION, settings).map { it.title })
        MoreValue.entries.forEach { value -> assertEquals(1, DisplaySettingsState.options(value, settings).count { it.selected }) }

        var s = DisplaySettingsState.choose(settings, MoreValue.MAP_STYLE, MapStylePref.PARCHMENT.name)
        s = DisplaySettingsState.choose(s, MoreValue.TERRAIN_ENGINE, TextMapEngine.CLOUD.name)
        s = DisplaySettingsState.choose(s, MoreValue.LARGE_EXPORT, LargeMapExport.K8.name)
        s = DisplaySettingsState.choose(s, MoreValue.NOTATION, SceneNotationPref.STORY_START.name)
        assertEquals(settings.copy(defaultMapStyle = MapStylePref.PARCHMENT, textMapEngine = TextMapEngine.CLOUD, largeMapExport = LargeMapExport.K8, defaultSceneNotation = SceneNotationPref.STORY_START), s)
        assertEquals("양피지", DisplaySettingsState.more(s).mapStyle.current)
        assertSame("an unknown id changes nothing", s, DisplaySettingsState.choose(s, MoreValue.MAP_STYLE, "NOPE"))
    }

    @Test
    fun `STO-17 a new project takes 때 표기 기본`() {
        val form = NewProjectForm(name = "새 이야기")
        val ctx = LibFx.ctx()
        assertEquals(SceneNotation.EPISODE, ProjectCreator.build(form, RatingDefaults(), ctx, null).doc.project.sceneTime.notation.known)
        val story = ProjectCreator.build(form, RatingDefaults(), LibFx.ctx(), null, notation = SceneNotationPref.STORY_START).doc
        assertEquals(SceneNotation.STORY_START, story.project.sceneTime.notation.known)
    }

    @Test
    fun `CNV-18 every 설정 row opens its screen`() {
        val expected = mapOf(
            SettingsRootRow.TEXT_SIZE to SettingsTextSize,
            SettingsRootRow.MOTION to SettingsMotion,
            SettingsRootRow.MORE to SettingsMore,
            SettingsRootRow.DRIVE to SettingsBackup,
            SettingsRootRow.RECOVERY to RecoveryPassword,
            SettingsRootRow.HIDING to SettingsHiding,
            SettingsRootRow.TRASH to Trash(),
            SettingsRootRow.RECORDER to RecorderSettings,
            SettingsRootRow.HELP to HelpReplay,
            SettingsRootRow.TEMPLATES to TemplateList(),
        )
        assertEquals(SettingsRootRow.entries.toSet(), expected.keys)
        expected.forEach { (row, route) -> assertEquals(row.name, route, SettingsRoutes.routeOf(row)) }
    }

    @Test
    fun `CNV-20 the 태그 row lists the project's tags and opens cast's TagSettings`() {
        val ashen = LibFx.ashen()
        val row = ProjectSettingsRows.tags(ashen)
        assertEquals("태그", row.title)
        assertEquals("주연 · 등불 결사 · 하층 구역 · 상층", row.sub)
        assertNull(row.value)
        assertEquals(TagSettings(ashen.key), routeOf(ProjectSettingsTarget.TAGS, ashen.key))
        assertNull("no tag: no sub", ProjectSettingsRows.tags(LibFx.glass()).sub)
        // N-44: a trashed tag leaves the summary.
        val trashed = ashen.copy(project = ashen.project.copy(tags = ashen.project.tags.map { if (it.name == "등불 결사") it.copy(deletedAt = 1L) else it }))
        assertEquals("주연 · 하층 구역 · 상층", ProjectSettingsRows.tags(trashed).sub)
    }
}
