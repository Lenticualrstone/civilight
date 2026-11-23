package io.github.lenticualrstone.civilight.model.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.json.LowerCamelEnumSerializer
import kotlinx.serialization.Serializable

/**
 * Script view state per project (§2.3, class A, `_app/view.json`); never exported. Last writing
 * position and daily counts live in `ProjectActivity` / `DayBaseline`.
 */
@Serializable
data class ScriptViewState(
    val layers: ScriptLayers = ScriptLayers(),
    val preview: ScriptPreview = ScriptPreview.WRITE,
    val panel: ScriptPanelState = ScriptPanelState(),
    val moduleCardOrder: List<String> = emptyList(),
    /** Speaker picker `최근`, most recent first. */
    val recentSpeakers: List<Key> = emptyList(),
    val restore: ScriptRestore? = null,
    /**
     * (0.2.0, U-14) The unsent input-bar draft, encoded by `feature:script` (`Drafts.encode`). Kept in
     * `_app/view.json` so it survives process death; never exported. Null = no draft.
     */
    val draft: String? = null,
)

/** `표시` sheet layers (SCR-26) with their defaults. */
@Serializable
data class ScriptLayers(
    val direction: Boolean = true,
    val memo: Boolean = true,
    val system: Boolean = true,
    val marks: Boolean = true,
    val addressHint: Boolean = false,
    val paren: Boolean = true,
    val termCandidates: Boolean = false,
)

/** `쓰기 · 대사만 · 낭독`. */
@Serializable(with = ScriptPreview.Codec::class)
enum class ScriptPreview {
    WRITE, DIALOGUE_ONLY, READ_ALOUD;

    object Codec : LowerCamelEnumSerializer<ScriptPreview>("ScriptPreview", entries, WRITE)
}

@Serializable
data class ScriptPanelState(
    val open: Boolean = false,
    val tab: ScriptPanelTab = ScriptPanelTab.TIME,
    /** Scroll offset per tab (tab serial name → offset). */
    val scroll: Map<String, Int> = emptyMap(),
)

/** `시간 · 참고 · 속성 · 모듈`. */
@Serializable(with = ScriptPanelTab.Codec::class)
enum class ScriptPanelTab {
    TIME, REF, PROPS, MODULES;

    object Codec : LowerCamelEnumSerializer<ScriptPanelTab>("ScriptPanelTab", entries, TIME)
}

/** Fold / process-death restore point. */
@Serializable
data class ScriptRestore(
    val unitKey: Key,
    val selectedBlockKey: Key? = null,
    val caretOffset: Int? = null,
    /** Block key at the top of the viewport. */
    val scrollAnchor: Key? = null,
)
