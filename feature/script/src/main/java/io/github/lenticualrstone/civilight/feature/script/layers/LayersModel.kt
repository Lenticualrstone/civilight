package io.github.lenticualrstone.civilight.feature.script.layers

import io.github.lenticualrstone.civilight.feature.script.terms.TermCopy
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.domain.script.ReadAloud
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.game.GameTools
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.script.ScriptPreview

/**
 * One switch row of the `표시` sheet (SCR-26, board `Layers`), in the drawn order, then the spec's
 * `용어 후보` row (rule 21 §2, SCR-56: dotted candidate underlines, default off).
 */
enum class LayerRow(val title: String, val sub: String) {
    DIRECTION(ScriptCopy.LAYER_DIRECTION, ScriptCopy.LAYER_DIRECTION_SUB),
    MEMO(ScriptCopy.LAYER_MEMO, ScriptCopy.LAYER_MEMO_SUB),
    SYSTEM(ScriptCopy.LAYER_SYSTEM, ScriptCopy.LAYER_SYSTEM_SUB),
    MARKS(ScriptCopy.LAYER_MARKS, ScriptCopy.LAYER_MARKS_SUB),
    ADDRESS_HINT(ScriptCopy.LAYER_ADDRESS, ScriptCopy.LAYER_ADDRESS_SUB),
    PAREN(ScriptCopy.LAYER_PAREN, ScriptCopy.LAYER_PAREN_SUB),
    TERM_CANDIDATES(TermCopy.LAYER, TermCopy.LAYER_SUB),
}

/** A `미리 보기` chip (SCR-27, SCR-28): `낭독` is dimmed while the episode has no step ([LayersModel.chips]). */
data class PreviewChip(val preview: ScriptPreview, val label: String, val enabled: Boolean)

/**
 * The `표시` sheet's state rules (SCR-26 … SCR-29). Layers and the preview mode are view state in
 * `_app/view.json` (per project and device, Q-14 `ED`); they only change what the manuscript draws.
 */
object LayersModel {
    /** (0.2.0, SCR-26) The `용어 후보` row is gone: underlines follow the project setting `용어 후보` (WLD-73). */
    val ROWS: List<LayerRow> = LayerRow.entries - LayerRow.TERM_CANDIDATES

    val CHIPS: List<PreviewChip> = listOf(
        PreviewChip(ScriptPreview.WRITE, ScriptCopy.PREVIEW_WRITE, enabled = true),
        PreviewChip(ScriptPreview.DIALOGUE_ONLY, ScriptCopy.PREVIEW_DIALOGUE, enabled = true),
        PreviewChip(ScriptPreview.READ_ALOUD, ScriptCopy.PREVIEW_READ_ALOUD, enabled = true),
    )

    /** (0.2.0, SCR-28) The chips with `낭독` dimmed when the episode has no step ([readAloudEnabled]). */
    fun chips(readAloud: Boolean): List<PreviewChip> =
        CHIPS.map { if (it.preview == ScriptPreview.READ_ALOUD) it.copy(enabled = readAloud) else it }

    /** `낭독` has something to play: at least one step in the episode (SCR-28; the 찾기 command follows it). */
    fun readAloudEnabled(doc: ProjectDoc, unitKey: Key, unlocked: Boolean): Boolean =
        ReadAloud.steps(doc, unitKey, GameTools.on(doc), unlocked).isNotEmpty()

    fun isOn(layers: ScriptLayers, row: LayerRow): Boolean = when (row) {
        LayerRow.DIRECTION -> layers.direction
        LayerRow.MEMO -> layers.memo
        LayerRow.SYSTEM -> layers.system
        LayerRow.MARKS -> layers.marks
        LayerRow.ADDRESS_HINT -> layers.addressHint
        LayerRow.PAREN -> layers.paren
        LayerRow.TERM_CANDIDATES -> layers.termCandidates
    }

    fun set(layers: ScriptLayers, row: LayerRow, on: Boolean): ScriptLayers = when (row) {
        LayerRow.DIRECTION -> layers.copy(direction = on)
        LayerRow.MEMO -> layers.copy(memo = on)
        LayerRow.SYSTEM -> layers.copy(system = on)
        LayerRow.MARKS -> layers.copy(marks = on)
        LayerRow.ADDRESS_HINT -> layers.copy(addressHint = on)
        LayerRow.PAREN -> layers.copy(paren = on)
        LayerRow.TERM_CANDIDATES -> layers.copy(termCandidates = on)
    }

    /**
     * The mode the manuscript is drawn in: `낭독` has no behaviour yet (SCR-28, Q-08), so a stored
     * `낭독` (another device, a newer app) draws as `쓰기`.
     */
    fun effective(stored: ScriptPreview): ScriptPreview =
        if (stored == ScriptPreview.READ_ALOUD) ScriptPreview.WRITE else stored

    /** The chip that reads as chosen. */
    fun selectedChip(stored: ScriptPreview): ScriptPreview = effective(stored)

    /** Typing and block edits happen only in `쓰기`; `대사만` is a read-only view (SCR-27). */
    fun isEditable(stored: ScriptPreview): Boolean = effective(stored) == ScriptPreview.WRITE

    /** The chip under the header while a preview other than `쓰기` is on, else null. */
    fun headerChip(stored: ScriptPreview): String? =
        CHIPS.firstOrNull { it.preview == effective(stored) && it.preview != ScriptPreview.WRITE }?.label?.let(ScriptCopy::previewChip)
}
