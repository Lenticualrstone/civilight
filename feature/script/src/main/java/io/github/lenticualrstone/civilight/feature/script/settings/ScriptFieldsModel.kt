package io.github.lenticualrstone.civilight.feature.script.settings

import io.github.lenticualrstone.civilight.domain.script.EditContext
import io.github.lenticualrstone.civilight.domain.script.VoiceIds
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.VoiceIdFormat
import io.github.lenticualrstone.civilight.model.project.VoiceIdFormatMode

/** A choice of the `ID 형식` sheet (SCR-24; Q-06 leaves the editor open, so two choices, `ED`). */
data class FormatChoice(val mode: VoiceIdFormatMode, val title: String, val subtitle: String?, val selected: Boolean)

/** `스크립트 추가 칸` (SCR-24, SCR-25, board `ProjSettings`) as the screen draws it. */
data class ScriptFieldsUi(
    val projectName: String,
    val readOnly: Boolean,
    /** `무대 위치` · `왼쪽 · 가운데 · 오른쪽` (default off). */
    val stageOn: Boolean,
    /** `보이스 ID` · `대사마다 녹음용 ID` (default on). */
    val voiceOn: Boolean,
    /** `HRT_0107_043 · 인물 · 에피소드 · 블록 · 내보내면 고정`, or `직접 · 내보내면 고정`. */
    val formatSub: String,
    val choices: List<FormatChoice>,
    /** Fixed IDs that a format change leaves as they are (`고정 ID {n}개는 그대로`). */
    val fixedCount: Int,
    /** `고정 ID {n}개는 그대로` and `고정 ID도 새 형식으로` (shown after a format change while fixed IDs exist). */
    val notice: List<String>,
    /** `고정 ID도 새 형식으로` can apply: the format is a pattern. */
    val canRefix: Boolean,
    /** `전환 문구` row sub (SCR-48): `새 씬 기본 없음 · 틀 장소 · 시각`. */
    val transitionSub: String = "",
)

/**
 * Rules of the project script settings (SCR-24, SCR-25). Automatic IDs follow a format change at
 * once (they are computed); fixed IDs stay until `고정 ID도 새 형식으로` (`VoiceIds.refixAll`).
 * Turning `보이스 ID` off only hides the field and the export columns; the data stays.
 */
object ScriptFieldsModel {
    fun of(doc: ProjectDoc, readOnly: Boolean): ScriptFieldsUi {
        val settings = doc.project.script
        val manual = settings.voiceIdFormat.mode.known == VoiceIdFormatMode.MANUAL
        val sample = sampleId(doc)
        val fixed = VoiceIds.fixedCount(doc)
        return ScriptFieldsUi(
            projectName = doc.project.name,
            readOnly = readOnly,
            stageOn = settings.stagePositionEnabled,
            voiceOn = settings.voiceIdEnabled,
            formatSub = if (manual) ScriptCopy.MANUAL_SUB else ScriptCopy.patternSub(sample),
            choices = listOf(
                FormatChoice(VoiceIdFormatMode.PATTERN, ScriptCopy.FORMAT_PATTERN, sample, selected = !manual),
                FormatChoice(VoiceIdFormatMode.MANUAL, ScriptCopy.FORMAT_MANUAL, null, selected = manual),
            ),
            fixedCount = fixed,
            notice = if (fixed > 0) VoiceIds.formatChangeNotice(doc) else emptyList(),
            canRefix = !manual && fixed > 0 && !readOnly,
            transitionSub = TransitionSettingsModel.of(doc, readOnly).rowSub,
        )
    }

    /**
     * The sample in the `ID 형식` row: the first dialogue line of the manuscript that has an automatic
     * ID, else the spec's sample (`HRT_0107_043`) in the current pattern.
     */
    fun sampleId(doc: ProjectDoc): String {
        val format = doc.project.script.voiceIdFormat.takeIf { it.mode.known == VoiceIdFormatMode.PATTERN } ?: VoiceIdFormat()
        return doc.manuscript.liveBlocks.firstNotNullOfOrNull { VoiceIds.autoId(doc, it, format) }
            ?: VoiceIds.format(format.pattern, ScriptCopy.SAMPLE_CODE, VoiceIds.EpisodeParts(1, 7), 43)
    }

    fun withStage(doc: ProjectDoc, on: Boolean): ProjectDoc =
        doc.copy(project = doc.project.copy(script = doc.project.script.copy(stagePositionEnabled = on)))

    fun withVoice(doc: ProjectDoc, on: Boolean): ProjectDoc =
        doc.copy(project = doc.project.copy(script = doc.project.script.copy(voiceIdEnabled = on)))

    /** `인물 · 에피소드 · 블록` keeps the stored pattern (the default when none); `직접` switches to typed IDs. */
    fun withMode(doc: ProjectDoc, mode: VoiceIdFormatMode): ProjectDoc {
        val format = doc.project.script.voiceIdFormat
        if (format.mode.known == mode) return doc
        return doc.copy(project = doc.project.copy(script = doc.project.script.copy(voiceIdFormat = format.copy(mode = mode.open()))))
    }

    /** `고정 ID도 새 형식으로` (SCR-24 `[PROPOSED]`): fixed IDs re-fixed in the current pattern. */
    fun refix(doc: ProjectDoc, ctx: EditContext): ProjectDoc {
        val format = doc.project.script.voiceIdFormat
        if (format.mode.known != VoiceIdFormatMode.PATTERN) return doc
        return VoiceIds.refixAll(doc, format, ctx)
    }
}
