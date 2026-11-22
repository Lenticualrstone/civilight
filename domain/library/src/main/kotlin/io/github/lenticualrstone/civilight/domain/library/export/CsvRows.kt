package io.github.lenticualrstone.civilight.domain.library.export

import io.github.lenticualrstone.civilight.domain.library.format.Sha256
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.script.DiscardedVoiceId
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText

/** CSV `상태` (LIB-44). An unchanged, already exported line has no state (empty cell, `ED`). */
enum class CsvRowState(val label: String) { NEW("새"), CHANGED("바뀜"), DISCARDED("버림") }

/** One row of the 대사 표 CSV; the script area writes the columns `화자 · 대사 · 보이스 ID · 블록 ID · 상태`. */
sealed interface CsvRowPlan {
    val state: CsvRowState?

    /** A dialogue line; `블록 ID` = the block key (with [WrittenLine.optionKey] for an option). */
    data class Line(val line: WrittenLine, val speakerName: String?, val voiceId: String?, override val state: CsvRowState?) : CsvRowPlan

    /**
     * A scene's 전환 문구 (R22 §6): `화자` = `전환 문구`, `블록 ID` = the phrase ID derived from the
     * scene key, empty `보이스 ID`. Phrases have no stored export record, so no state (`ED`).
     */
    data class Transition(val sceneKey: Key, val mode: TransitionMode, val runs: List<TextRun>?) : CsvRowPlan {
        override val state: CsvRowState? get() = null
        val speakerCell: String get() = SPEAKER_TRANSITION
    }

    /** A fixed voice ID discarded since; still exported so the recording team knows. */
    data class Discarded(val discarded: DiscardedVoiceId) : CsvRowPlan {
        override val state: CsvRowState get() = CsvRowState.DISCARDED
    }

    companion object {
        const val SPEAKER_TRANSITION = "전환 문구"
        val HEADER: List<String> = listOf("화자", "대사", "보이스 ID", "블록 ID", "상태")
    }
}

/**
 * Rows of the CSV in manuscript order (transition row before its scene's lines, discarded IDs
 * last). Rows of scenes left out by `빼기` are absent, never `버림`.
 */
object CsvRows {
    /**
     * [voiceIdOf] gives the automatic voice ID of a line (script area); a fixed line uses its
     * `fixedId`, a manual one its `manualId`.
     */
    fun of(plan: ExportPlan, voiceIdOf: (WrittenLine) -> String?): List<CsvRowPlan> = buildList {
        val names = plan.characters.associate { it.key to it.name }
        for (scene in plan.scenes) {
            scene.transition?.let { add(CsvRowPlan.Transition(scene.sceneKey, it.mode, it.runs)) }
            for (block in scene.blocks) {
                for (line in WrittenLine.of(block)) {
                    add(CsvRowPlan.Line(line, line.speakerKey?.let(names::get), voiceIdFor(line, voiceIdOf), stateOf(line)))
                }
            }
        }
        plan.discardedVoiceIds.forEach { add(CsvRowPlan.Discarded(it)) }
    }

    fun voiceIdFor(line: WrittenLine, voiceIdOf: (WrittenLine) -> String?): String? {
        val v = line.voice
        return when {
            v?.mode?.known == VoiceMode.FIXED && v.fixedId != null -> v.fixedId
            v?.manualId != null -> v.manualId
            else -> voiceIdOf(line)
        }
    }

    /**
     * `새` when this voice ID was never exported to CSV; `바뀜` when the text or the speaker differs
     * from the last CSV export (`ED`, inputs in `VoiceState.lastExport`); otherwise no state.
     */
    fun stateOf(line: WrittenLine): CsvRowState? {
        val last = line.voice?.lastExport ?: return CsvRowState.NEW
        val changed = last.textHash != textHash(line.runs) || last.speakerKey != line.speakerKey
        return if (changed) CsvRowState.CHANGED else null
    }

    /** `sha256-<hex>` of the line's shown text, as `VoiceExport.textHash` stores it. */
    fun textHash(runs: List<TextRun>): String = Sha256.ofText(runs.plainText)
}
