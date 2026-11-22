package io.github.lenticualrstone.civilight.feature.library.projectsettings

import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationKinds
import io.github.lenticualrstone.civilight.domain.script.VoiceIds
import io.github.lenticualrstone.civilight.domain.story.time.BandTable
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.domain.story.time.word
import io.github.lenticualrstone.civilight.domain.world.map.Travel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.DayBoundary
import io.github.lenticualrstone.civilight.model.project.SceneNotation
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.project.TransitionTemplate
import io.github.lenticualrstone.civilight.model.project.VoiceIdFormatMode
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode

/** Where a `… ›` row of 프로젝트 설정 leads; the screen maps each to its owner's route. */
enum class ProjectSettingsTarget {
    LEVELS, WORLD_TIME, NOTATION, TRAVEL, DAY_BANDS, FIELDS, RELATION_KINDS, VOICE_FORMAT, TRANSITION,

    /** `태그` (CST-12, cast's `TagSettings`; not on the `ProjSettings` board, see Spec conflicts). */
    TAGS,
}

/** The two `스크립트 추가 칸` switches (SCR-24, SCR-25). */
enum class ScriptSwitch { STAGE_POSITION, VOICE_ID }

/** A `title · sub · value ›` row that opens [target]; a null [sub] or [value] is not drawn (`›` alone). */
data class ProjectLinkRow(val target: ProjectSettingsTarget, val title: String, val sub: String?, val value: String?)

data class ProjectSwitchRow(val switch: ScriptSwitch, val title: String, val sub: String, val on: Boolean)

/** `스크립트 추가 칸`: two switches, then `ID 형식` and `전환 문구`. */
data class ScriptSectionUi(val switches: List<ProjectSwitchRow>, val links: List<ProjectLinkRow>)

/**
 * The rows of 프로젝트 설정 that other areas specify (board `ProjSettings`, CNV-20 row map). Values
 * are rebuilt here from the owners' domain modules (a feature module cannot depend on another).
 */
object ProjectSettingsRows {
    fun structure(doc: ProjectDoc): List<ProjectLinkRow> = listOf(
        ProjectLinkRow(ProjectSettingsTarget.LEVELS, ProjectSettingsCopy.LEVELS, doc.project.structure.levels.joinToString(" › "), ProjectSettingsCopy.CHANGE),
        worldTime(doc),
        notation(doc),
        ProjectLinkRow(ProjectSettingsTarget.TRAVEL, ProjectSettingsCopy.TRAVEL, Travel.modes(doc).joinToString(" · ") { Travel.modeLabel(it) }, ProjectSettingsCopy.CHANGE),
        ProjectLinkRow(ProjectSettingsTarget.DAY_BANDS, ProjectSettingsCopy.DAY_BANDS, dayBands(doc), ProjectSettingsCopy.CHANGE),
    )

    fun cast(doc: ProjectDoc): List<ProjectLinkRow> {
        val (title, sub, value) = FieldSettings.entryRow(doc)
        return listOf(
            ProjectLinkRow(ProjectSettingsTarget.FIELDS, title, sub, value.removeSuffix("›").trim()),
            ProjectLinkRow(ProjectSettingsTarget.RELATION_KINDS, ProjectSettingsCopy.RELATION_KINDS, RelationKinds.summary(doc), ProjectSettingsCopy.CHANGE),
            tags(doc),
        )
    }

    /**
     * `태그` · `주연 · 등불 결사 · 하층 구역 · 상층` · `›` (cast's request for CST-12; `ED`: the project's tag
     * names in their order, no value word, no sub while there is no tag).
     */
    fun tags(doc: ProjectDoc): ProjectLinkRow {
        val names = doc.project.tags.sortedBy { it.order }.joinToString(" · ") { it.name }
        return ProjectLinkRow(ProjectSettingsTarget.TAGS, ProjectSettingsCopy.TAGS, names.ifEmpty { null }, null)
    }

    fun script(doc: ProjectDoc): ScriptSectionUi {
        val s = doc.project.script
        return ScriptSectionUi(
            switches = listOf(
                ProjectSwitchRow(ScriptSwitch.STAGE_POSITION, ProjectSettingsCopy.STAGE, ProjectSettingsCopy.STAGE_SUB, s.stagePositionEnabled),
                ProjectSwitchRow(ScriptSwitch.VOICE_ID, ProjectSettingsCopy.VOICE_ID, ProjectSettingsCopy.VOICE_ID_SUB, s.voiceIdEnabled),
            ),
            links = listOf(
                ProjectLinkRow(ProjectSettingsTarget.VOICE_FORMAT, ProjectSettingsCopy.VOICE_FORMAT, voiceFormatSub(doc), ProjectSettingsCopy.CHANGE),
                ProjectLinkRow(ProjectSettingsTarget.TRANSITION, ProjectSettingsCopy.TRANSITION, transitionSub(doc), ProjectSettingsCopy.CHANGE),
            ),
        )
    }

    /** `연도만 · 시작 1200년` · `연도만`; `시간 없이` has no start year. */
    fun worldTime(doc: ProjectDoc): ProjectLinkRow {
        val time = doc.project.worldTime
        val mode = time.mode.known ?: WorldTimeMode.YEAR_ONLY
        val word = when (mode) {
            WorldTimeMode.YEAR_ONLY -> ProjectSettingsCopy.TIME_YEAR
            WorldTimeMode.CALENDAR -> ProjectSettingsCopy.TIME_CALENDAR
            WorldTimeMode.NONE -> ProjectSettingsCopy.TIME_NONE
        }
        val year = time.storyStartYear?.takeIf { mode != WorldTimeMode.NONE }
        val sub = listOfNotNull(word, year?.let { "${ProjectSettingsCopy.START} ${it}년" }).joinToString(" · ")
        return ProjectLinkRow(ProjectSettingsTarget.WORLD_TIME, ProjectSettingsCopy.WORLD_TIME, sub, word)
    }

    /**
     * `1-7 · 2일째 · 새벽 · 연표는 기준 사건도` · `에피소드 기준`: the label of the sample scene (the 집필 선
     * scene when it has 때, else the first timed scene, as STO-17's screen picks it), then the note.
     */
    fun notation(doc: ProjectDoc): ProjectLinkRow {
        val notation = doc.project.sceneTime.notation.known ?: SceneNotation.EPISODE
        val value = if (notation == SceneNotation.STORY_START) ProjectSettingsCopy.NOTATION_STORY_START else ProjectSettingsCopy.NOTATION_EPISODE
        val sample = sampleScene(doc)?.let { TimeLabels(SceneTimes(doc)).full(it) }
        val sub = listOfNotNull(sample, ProjectSettingsCopy.NOTATION_NOTE).joinToString(" · ")
        return ProjectLinkRow(ProjectSettingsTarget.NOTATION, ProjectSettingsCopy.NOTATION, sub, value)
    }

    private fun sampleScene(doc: ProjectDoc): Key? {
        val times = SceneTimes(doc)
        val line = doc.manuscript.writingLineSceneKey
        if (line != null && times.timed(line) != null) return line
        return times.liveScenes.firstOrNull { times.timed(it.key) != null }?.key
    }

    /** `하루는 새벽 4시부터 · 저녁 17–20` (under `자정` the first part reads `자정`). */
    fun dayBands(doc: ProjectDoc): String {
        val settings = doc.project.sceneTime
        val day = if (settings.dayBoundary.known == DayBoundary.MIDNIGHT) ProjectSettingsCopy.MIDNIGHT else ProjectSettingsCopy.DAWN_DAY
        return "$day · ${Band.EVENING.word} ${BandTable(settings.bandStarts).hoursLabel(Band.EVENING)}"
    }

    /**
     * `HRT_0107_043 · 인물 · 에피소드 · 블록 · 내보내면 고정`: the ID of the last live line that has one
     * (`ED`), left out while there is none or the format is `직접`.
     */
    fun voiceFormatSub(doc: ProjectDoc): String {
        val manual = doc.project.script.voiceIdFormat.mode.known == VoiceIdFormatMode.MANUAL
        val sample = if (manual) null else doc.manuscript.liveBlocks.asReversed().firstNotNullOfOrNull { VoiceIds.currentId(doc, it) }
        return listOfNotNull(sample, ProjectSettingsCopy.VOICE_FORMAT_PARTS).joinToString(" · ")
    }

    /** `새 씬 기본 없음 · 틀 장소 · 시각`. */
    fun transitionSub(doc: ProjectDoc): String {
        val s = doc.project.script
        val mode = when (s.transitionDefault.known ?: TransitionMode.NONE) {
            TransitionMode.AUTO -> ProjectSettingsCopy.TRANSITION_AUTO
            TransitionMode.DIRECT -> ProjectSettingsCopy.TRANSITION_DIRECT
            TransitionMode.NONE -> ProjectSettingsCopy.TRANSITION_NONE
        }
        val template = when (s.transitionTemplate.known ?: TransitionTemplate.PLACE_TIME) {
            TransitionTemplate.PLACE_TIME -> ProjectSettingsCopy.TEMPLATE_PLACE_TIME
            TransitionTemplate.PLACE_ONLY -> ProjectSettingsCopy.TEMPLATE_PLACE_ONLY
        }
        return "${ProjectSettingsCopy.TRANSITION_DEFAULT} $mode · ${ProjectSettingsCopy.TEMPLATE} $template"
    }
}

/** The two script switches as project writes (SCR-24 `무대 위치`, SCR-25 `보이스 ID`); no-ops return [doc]. */
object ScriptSettingsEdit {
    fun set(doc: ProjectDoc, switch: ScriptSwitch, on: Boolean): ProjectDoc {
        val s = doc.project.script
        val next = when (switch) {
            ScriptSwitch.STAGE_POSITION -> s.copy(stagePositionEnabled = on)
            ScriptSwitch.VOICE_ID -> s.copy(voiceIdEnabled = on)
        }
        return if (next == s) doc else doc.copy(project = doc.project.copy(script = next))
    }
}
