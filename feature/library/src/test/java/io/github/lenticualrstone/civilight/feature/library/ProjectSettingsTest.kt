package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.feature.library.projectsettings.ProjectSettingsState
import io.github.lenticualrstone.civilight.feature.library.projectsettings.ProjectSettingsTarget
import io.github.lenticualrstone.civilight.feature.library.projectsettings.ScriptSettingsEdit
import io.github.lenticualrstone.civilight.feature.library.projectsettings.ScriptSwitch
import io.github.lenticualrstone.civilight.feature.library.projectsettings.StructureLevelsForm
import io.github.lenticualrstone.civilight.feature.library.projectsettings.routeOf
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.navigation.DayBandSettings
import io.github.lenticualrstone.civilight.navigation.FieldSettings
import io.github.lenticualrstone.civilight.navigation.SceneNotationSettings
import io.github.lenticualrstone.civilight.navigation.StructureLevels
import io.github.lenticualrstone.civilight.navigation.WorldCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** 프로젝트 설정 (LIB-69, board `ProjSettings`) on the example project, and its two own edits. */
class ProjectSettingsTest {
    private val ashen = LibFx.ashen()
    private val ui = ProjectSettingsState.build(ashen, readOnly = false, defaults = RatingDefaults())

    private fun row(target: ProjectSettingsTarget) =
        (ui.structure + ui.cast + ui.script.links).first { it.target == target }

    @Test
    fun `LIB-69 구조 rows read as the ProjSettings board`() {
        assertEquals("잿빛 새벽의 등불", ui.projectName)
        assertEquals(
            // (0.3.0, WLD-47) the `이동 수단` row is back.
            listOf(ProjectSettingsTarget.LEVELS, ProjectSettingsTarget.WORLD_TIME, ProjectSettingsTarget.NOTATION, ProjectSettingsTarget.TRAVEL, ProjectSettingsTarget.DAY_BANDS),
            ui.structure.map { it.target },
        )
        row(ProjectSettingsTarget.LEVELS).let {
            assertEquals("단계 이름", it.title)
            assertEquals("장 › 에피소드 › 씬", it.sub)
            assertEquals("바꾸기", it.value)
        }
        row(ProjectSettingsTarget.WORLD_TIME).let {
            assertEquals("세계 시간", it.title)
            assertEquals("연도만 · 시작 1200년", it.sub)
            assertEquals("연도만", it.value)
        }
        row(ProjectSettingsTarget.NOTATION).let {
            assertEquals("때 표기", it.title)
            assertTrue(it.sub, it.sub!!.endsWith(" · 연표는 기준 사건도"))
            assertEquals("에피소드 기준", it.value)
        }
        row(ProjectSettingsTarget.TRAVEL).let {
            assertEquals("이동 수단", it.title)
            assertTrue(it.sub, it.sub!!.startsWith("도보 15 km · 8시간"))
        }
        assertEquals("하루는 새벽 4시부터 · 저녁 17–20", row(ProjectSettingsTarget.DAY_BANDS).sub)
    }

    @Test
    fun `LIB-69 인물 and 스크립트 추가 칸 rows`() {
        assertEquals(listOf("인물 항목", "관계 종류", "태그"), ui.cast.map { it.title })
        assertEquals("고치기", row(ProjectSettingsTarget.FIELDS).value)
        // (0.2.0, LIB-91) `게임 제작 도구` first; the example holds game data, so it opens on (G-3) and the voice rows show.
        assertEquals(listOf("게임 제작 도구", "무대 위치", "보이스 ID"), ui.script.switches.map { it.title })
        assertEquals(listOf(io.github.lenticualrstone.civilight.feature.library.projectsettings.ProjectSettingsCopy.GAME_TOOLS_SUB, "왼쪽 · 가운데 · 오른쪽", "대사마다 녹음용 ID"), ui.script.switches.map { it.sub })
        assertEquals(listOf("ID 형식", "전환 문구"), ui.script.links.map { it.title })
        assertTrue(row(ProjectSettingsTarget.VOICE_FORMAT).sub!!.endsWith("인물 · 에피소드 · 블록 · 내보내면 고정"))
        assertEquals("새 씬 기본 없음 · 틀 장소 · 시각", row(ProjectSettingsTarget.TRANSITION).sub)
    }

    @Test
    fun `LIB-69 the rows other areas specify carry the ProjSettings board values`() {
        assertEquals("1-7 · 2일째 · 새벽 · 연표는 기준 사건도", row(ProjectSettingsTarget.NOTATION).sub)
        // The board draws `프로젝트 7`; the example defines 11 project fields (fixture.md #12), and the row counts them.
        assertEquals("게임 프로필 · 고정 7 · 프로젝트 11 · 장소 · 용어 · 세력도", row(ProjectSettingsTarget.FIELDS).sub)
        // The board draws `사이 12`; the example seeds rule 13's 사이 11 (fixture.md #14).
        assertEquals("사이 11 · 마음 11", row(ProjectSettingsTarget.RELATION_KINDS).sub)
        // The board's sample is 하르트's `#043`; the row shows the last live line's ID (`ED`), the same format.
        assertEquals("RIE_0107_048 · 인물 · 에피소드 · 블록 · 내보내면 고정", row(ProjectSettingsTarget.VOICE_FORMAT).sub)
    }

    @Test
    fun `LIB-69 rows open their owners' screens`() {
        val p = ashen.key
        assertEquals(StructureLevels(p), routeOf(ProjectSettingsTarget.LEVELS, p))
        assertEquals(WorldCalendar(p), routeOf(ProjectSettingsTarget.WORLD_TIME, p))
        assertEquals(SceneNotationSettings(p), routeOf(ProjectSettingsTarget.NOTATION, p))
        assertEquals(DayBandSettings(p), routeOf(ProjectSettingsTarget.DAY_BANDS, p))
        assertEquals(FieldSettings(p), routeOf(ProjectSettingsTarget.FIELDS, p))
    }

    @Test
    fun `LIB-69 the read-only example disables every row`() {
        assertTrue(ProjectSettingsState.build(ashen, readOnly = true, defaults = RatingDefaults()).readOnly)
        assertFalse(ui.readOnly)
    }

    @Test
    fun `the script switches write the project and a no-op keeps the document`() {
        val on = ashen.project.script.stagePositionEnabled
        assertSame(ashen, ScriptSettingsEdit.set(ashen, ScriptSwitch.STAGE_POSITION, on))
        val flipped = ScriptSettingsEdit.set(ashen, ScriptSwitch.STAGE_POSITION, !on)
        assertEquals(!on, flipped.project.script.stagePositionEnabled)
        val voice = ScriptSettingsEdit.set(ashen, ScriptSwitch.VOICE_ID, !ashen.project.script.voiceIdEnabled)
        assertEquals(!ashen.project.script.voiceIdEnabled, voice.project.script.voiceIdEnabled)
    }

    @Test
    fun `단계 이름 renames keep the level count and refuse blanks`() {
        val levels = ashen.project.structure.levels
        assertFalse("unchanged", StructureLevelsForm.canSave(levels, levels))
        assertFalse("blank", StructureLevelsForm.canSave(levels, listOf("", levels[1], levels[2])))
        val draft = listOf("막", levels[1], levels[2])
        assertTrue(StructureLevelsForm.canSave(levels, draft))
        val renamed = StructureLevelsForm.apply(ashen, draft)
        assertEquals(listOf("막", levels[1], levels[2]), renamed.project.structure.levels)
        assertEquals(ashen.story.units.map { it.key }, renamed.story.units.map { it.key })
    }
}
