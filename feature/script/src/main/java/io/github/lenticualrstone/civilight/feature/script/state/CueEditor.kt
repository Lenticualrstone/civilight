package io.github.lenticualrstone.civilight.feature.script.state

import io.github.lenticualrstone.civilight.domain.script.DirectionCues
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.ScriptAssets
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.CueKind

/** One cue row of the cue sheet: `▸ 배경 하층 거리(새벽)` with its kind. */
data class CueRow(val key: Key, val kind: CueKind?, val line: String)

/** One pick of a cue-kind list: an asset (`하층 거리(새벽)` · `bg_lower_dawn · 이 에피소드 3회` · `1-7`) or an effect. */
data class CuePick(val title: String, val subtitle: String?, val trailing: String?, val assetKey: Key?, val effect: String?)

/** The cue sheet of a direction block (SCR-08 `[PROPOSED]` editor). */
data class CueEditor(
    /** `연출 지시`. */
    val title: String,
    /** `#041`. */
    val number: String,
    val cues: List<CueRow>,
    /** Chips of `+ 연출`: kind and word. */
    val kinds: List<Pair<CueKind, String>>,
) {
    companion object {
        /** Null when [blockKey] is not a live direction block (the sheet closes). */
        fun of(doc: ProjectDoc, blockKey: Key): CueEditor? {
            val block = doc.index.block(blockKey)
                ?.takeIf { doc.manuscript.isLiveBlock(it.key) && it.kind.known == BlockKind.DIRECTION } ?: return null
            return CueEditor(
                title = ScriptCopy.DIRECTION_TITLE,
                number = Labels.number(block.number),
                cues = block.cues.map { CueRow(it.key, it.kind.known, DirectionCues.cueLine(doc, it)) },
                kinds = DirectionCues.KINDS.map { it to DirectionCues.kindWord(it) },
            )
        }

        /**
         * The list under a kind chip: that kind's assets in the asset list's order with their sublines
         * (uses in this episode, SCR-61), or the screen effects (Q-11 default `페이드 · 암전`).
         */
        fun picks(doc: ProjectDoc, kind: CueKind, episodeKey: Key? = null): List<CuePick> {
            if (kind == CueKind.SCREEN_EFFECT) {
                return DirectionCues.EFFECTS.map { (id, word) -> CuePick(word, null, null, null, id) }
            }
            val usage = ScriptAssets.usage(doc).associateBy { it.asset.key }
            return ScriptAssets.forCue(doc, kind).map { asset ->
                val u = usage[asset.key]
                CuePick(asset.name, u?.let { ScriptAssets.subline(it, episodeKey) }, u?.let(ScriptAssets::trailing), asset.key, null)
            }
        }
    }
}
