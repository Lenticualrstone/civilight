package io.github.lenticualrstone.civilight.feature.script.select

import io.github.lenticualrstone.civilight.domain.script.Records
import io.github.lenticualrstone.civilight.domain.script.SpeakerPicker
import io.github.lenticualrstone.civilight.feature.script.records.BlockPreview
import io.github.lenticualrstone.civilight.feature.script.state.BlockRow
import io.github.lenticualrstone.civilight.feature.script.state.HeadRow
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRow
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.ScriptLayers

/**
 * `블록 고르기` (SCR-36): which blocks are checked. Only blocks the manuscript draws can be checked
 * (layers can hide direction and memo blocks; acting on unseen blocks would be accidental damage).
 */
data class SelectState(val active: Boolean = false, val checked: Set<Key> = emptySet()) {
    fun isChecked(blockKey: Key): Boolean = blockKey in checked
}

object SelectRules {
    /** Enters the mode; [start] (the selected block, if drawn) starts checked. */
    fun enter(start: Key?, drawn: Set<Key>): SelectState = SelectState(true, setOfNotNull(start?.takeIf { it in drawn }))

    fun toggle(state: SelectState, blockKey: Key, drawn: Set<Key>): SelectState {
        if (!state.active || blockKey !in drawn) return state
        return state.copy(checked = if (blockKey in state.checked) state.checked - blockKey else state.checked + blockKey)
    }

    /**
     * `모두` (SCR-36 `[PROPOSED]` scope): every drawn block of [sceneKey] (the scene under the header).
     * When all of them are already checked, `모두` unchecks them.
     */
    fun all(state: SelectState, rows: List<ScriptRow>, sceneKey: Key?): SelectState {
        if (!state.active || sceneKey == null) return state
        val scene = rows.filterIsInstance<BlockRow>().filter { it.sceneKey == sceneKey }.map { it.blockKey }.toSet()
        if (scene.isEmpty()) return state
        return state.copy(checked = if (state.checked.containsAll(scene)) state.checked - scene else state.checked + scene)
    }

    /** After every operation and row rebuild: checked keys that are gone or no longer drawn drop out. */
    fun reconcile(state: SelectState, drawn: Set<Key>): SelectState {
        if (!state.active) return state
        val kept = state.checked.filterTo(HashSet()) { it in drawn }
        return if (kept.size == state.checked.size) state else state.copy(checked = kept)
    }

    /** The checked keys in manuscript order (the order the domain moves, deletes and copies them in). */
    fun ordered(state: SelectState, rows: List<ScriptRow>): List<Key> =
        rows.filterIsInstance<BlockRow>().map { it.blockKey }.filter { it in state.checked }

    fun drawnKeys(rows: List<ScriptRow>): Set<Key> = rows.filterIsInstance<BlockRow>().mapTo(HashSet()) { it.blockKey }
}

/** One row of select mode (board `RSSelect`): `하르트 #043` + markers over the text; heads stay as dividers. */
sealed interface SelectRowModel {
    val key: String

    data class Block(
        val blockKey: Key,
        val sceneKey: Key,
        val label: String,
        /** `◆`, `◇` after the label (dotted when the record's anchor was deleted). */
        val marks: List<Records.Mark>,
        val text: String,
        val checked: Boolean,
    ) : SelectRowModel {
        override val key: String get() = "select:$blockKey"
    }

    data class Head(val sceneKey: Key, val text: String) : SelectRowModel {
        override val key: String get() = "select:head:$sceneKey"
    }
}

/** The bottom bar (124 px): `고른 블록 3개`, `#042 ~ #044 · ◆ 1 · ◇ 1 같이 감`, `옮기기 · 지우기 · 잘라내기 · 복사`. */
data class SelectBar(val count: String, val range: String?, val enabled: Boolean)

object SelectRows {
    /** Rows of the drawn manuscript (heads and blocks; hidden-scene and empty-scene rows are left out). */
    fun of(doc: ProjectDoc, rows: List<ScriptRow>, state: SelectState, layers: ScriptLayers, unlocked: Boolean): List<SelectRowModel> = rows.mapNotNull { row ->
        when (row) {
            is HeadRow -> SelectRowModel.Head(row.sceneKey, row.title)
            is BlockRow -> {
                val block = doc.index.block(row.blockKey) ?: return@mapNotNull null
                val line = BlockPreview.of(doc, block, unlocked)
                SelectRowModel.Block(row.blockKey, row.sceneKey, line.label, Records.marksOn(doc, block.key, layers), line.text, state.isChecked(row.blockKey))
            }
            else -> null
        }
    }

    fun bar(doc: ProjectDoc, state: SelectState): SelectBar {
        val bar = SpeakerPicker.selectionBar(doc, state.checked)
        return SelectBar(bar.count, bar.range, enabled = state.checked.isNotEmpty())
    }

    /** `옮기기 · 지우기 · 잘라내기 · 복사` (domain `SpeakerPicker.SelectionBar.buttons`). */
    val BUTTONS: List<String> get() = SpeakerPicker.SelectionBar("", null).buttons
}
