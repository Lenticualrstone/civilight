package io.github.lenticualrstone.civilight.feature.script.settings

import io.github.lenticualrstone.civilight.domain.script.SceneText
import io.github.lenticualrstone.civilight.domain.script.StoryTimes
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.project.TransitionTemplate

/** One radio row: its value, title, an optional sample, and whether it is chosen. */
data class TransitionChoice<T>(val value: T, val title: String, val sample: String?, val selected: Boolean)

/**
 * 프로젝트 설정 › 스크립트 추가 칸 › `전환 문구` (SCR-48, boards `ProjSettings`, `SceneWhere`): the
 * row `전환 문구` · `새 씬 기본 없음 · 틀 장소 · 시각` · `바꾸기 ›`, and its screen with the default
 * mode for new scenes and the `자동` template (`장소 · 시각` default, `장소만`).
 */
data class TransitionSettingsUi(
    val projectName: String,
    val readOnly: Boolean,
    /** `새 씬 기본 없음 · 틀 장소 · 시각`. */
    val rowSub: String,
    val modes: List<TransitionChoice<TransitionMode>>,
    val templates: List<TransitionChoice<TransitionTemplate>>,
)

object TransitionSettingsModel {
    /**
     * Default modes offered (`ED`): `없음` (the spec default) and `자동`. `직접` needs each scene's own
     * text, so it is not a default for scenes that have none yet; a stored `직접` still shows as chosen.
     */
    val MODES: List<TransitionMode> = listOf(TransitionMode.NONE, TransitionMode.AUTO)
    val TEMPLATES: List<TransitionTemplate> = listOf(TransitionTemplate.PLACE_TIME, TransitionTemplate.PLACE_ONLY)

    fun modeWord(mode: TransitionMode): String = when (mode) {
        TransitionMode.NONE -> TransitionCopy.NONE
        TransitionMode.AUTO -> TransitionCopy.AUTO
        TransitionMode.DIRECT -> TransitionCopy.DIRECT
    }

    fun templateWord(template: TransitionTemplate): String = when (template) {
        TransitionTemplate.PLACE_TIME -> TransitionCopy.PLACE_TIME
        TransitionTemplate.PLACE_ONLY -> TransitionCopy.PLACE_ONLY
    }

    fun of(doc: ProjectDoc, readOnly: Boolean): TransitionSettingsUi {
        val settings = doc.project.script
        val mode = settings.transitionDefault.knownOr(TransitionMode.NONE)
        val template = settings.transitionTemplate.knownOr(TransitionTemplate.PLACE_TIME)
        val modes = (MODES + listOfNotNull(mode.takeIf { it !in MODES })).map { TransitionChoice(it, modeWord(it), null, it == mode) }
        return TransitionSettingsUi(
            projectName = doc.project.name,
            readOnly = readOnly,
            rowSub = TransitionCopy.rowSub(modeWord(mode), templateWord(template)),
            modes = modes,
            templates = TEMPLATES.map { TransitionChoice(it, templateWord(it), sample(doc, it), it == template) },
        )
    }

    /**
     * The `자동` text of the first scene that has a place and a time with a clock (else one with a
     * place), as [template] would write it (`결사의 문 · 06:10`, `결사의 문`). Null without such a scene.
     */
    fun sample(doc: ProjectDoc, template: TransitionTemplate): String? {
        val withTemplate = withTemplate(doc, template)
        val time = StoryTimes.source(doc)
        val scenes = doc.manuscript.liveScenes.filter { !it.hidden && it.place != null }
        val scene = scenes.firstOrNull { time.timeOf(it.key)?.clock != null } ?: scenes.firstOrNull() ?: return null
        return SceneText.autoTransitionText(withTemplate, scene, time.timeOf(scene.key))
    }

    fun withDefault(doc: ProjectDoc, mode: TransitionMode): ProjectDoc =
        doc.copy(project = doc.project.copy(script = doc.project.script.copy(transitionDefault = mode.open())))

    fun withTemplate(doc: ProjectDoc, template: TransitionTemplate): ProjectDoc =
        doc.copy(project = doc.project.copy(script = doc.project.script.copy(transitionTemplate = template.open())))
}

/** Copy of the `전환 문구` setting (SCR-48 row and screen). */
object TransitionCopy {
    const val TITLE = "전환 문구"
    const val NONE = "없음"
    const val AUTO = "자동"
    const val DIRECT = "직접"
    const val PLACE_TIME = "장소 · 시각"
    const val PLACE_ONLY = "장소만"

    /** Section labels: `새 씬 기본`, `틀` (the row's own words). */
    const val DEFAULT_SECTION = "새 씬 기본"
    const val TEMPLATE_SECTION = "틀"

    /** `새 씬 기본 없음 · 틀 장소 · 시각`. */
    fun rowSub(mode: String, template: String): String = "$DEFAULT_SECTION $mode · $TEMPLATE_SECTION $template"

    val all: List<String> = listOf(TITLE, NONE, AUTO, DIRECT, PLACE_TIME, PLACE_ONLY, DEFAULT_SECTION, TEMPLATE_SECTION, rowSub(NONE, PLACE_TIME))
}
