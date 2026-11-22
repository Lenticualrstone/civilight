package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.script.DirectionCue

/**
 * Editing the cues of a `연출 지시` block (SCR-08 `[PROPOSED]` editor): add a cue of a kind, change
 * one, take one out. `배경 · BGM · 효과음 · CG` cues point at an asset of the matching kind
 * ([ScriptAssets.forCue]); `화면 효과` cues carry an effect name. Every function returns the same
 * document when the block is not a live direction block or the cue does not fit.
 */
object DirectionCues {
    /** Screen effects offered by the editor (Q-11 default: the two rule 22 names), stored by id. */
    val EFFECTS: List<Pair<String, String>> = listOf("fade" to "페이드", "blackout" to "암전")

    /** Cue-kind chips, in the add sheet's order: `배경 · BGM · 효과음 · CG · 화면 효과`. */
    val KINDS: List<CueKind> = listOf(CueKind.BACKGROUND, CueKind.BGM, CueKind.SFX, CueKind.CG, CueKind.SCREEN_EFFECT)

    fun kindWord(kind: CueKind): String = when (kind) {
        CueKind.BACKGROUND -> "배경"
        CueKind.BGM -> "BGM"
        CueKind.SFX -> "효과음"
        CueKind.CG -> "CG"
        CueKind.SCREEN_EFFECT -> "화면 효과"
    }

    /** Korean word of a stored effect id (`fade` → `페이드`); an unknown id is shown as stored. */
    fun effectWord(effect: String?): String = EFFECTS.firstOrNull { it.first == effect }?.second ?: effect.orEmpty()

    /**
     * One cue as the manuscript draws it (SCR-08): `▸ 배경 하층 거리(새벽)`, `♪ 잔불`, `효과음 바람`,
     * `CG 꺼진 등불`, `화면 효과 페이드`. Unlike [Manuscript.cueText] it names effects in Korean.
     */
    fun cueLine(doc: ProjectDoc, cue: DirectionCue): String {
        val name = cue.assetKey?.let { doc.index.scriptAsset(it)?.name }
        return when (cue.kind.known) {
            CueKind.BACKGROUND -> "▸ 배경 ${name.orEmpty()}"
            CueKind.BGM -> "♪ ${name.orEmpty()}"
            CueKind.SFX -> "효과음 ${name.orEmpty()}"
            CueKind.CG -> "CG ${name.orEmpty()}"
            CueKind.SCREEN_EFFECT -> "화면 효과 ${effectWord(cue.effect)}"
            null -> name ?: cue.effect.orEmpty()
        }
    }

    /** All cues of a block joined with [separator] (spaces in the manuscript, ` · ` in select mode). */
    fun blockLine(doc: ProjectDoc, cues: List<DirectionCue>, separator: String = " "): String =
        cues.joinToString(separator) { cueLine(doc, it) }

    /** Whether [assetKey] / [effect] fit a cue of [kind]. */
    fun fits(doc: ProjectDoc, kind: CueKind, assetKey: Key?, effect: String?): Boolean = when (kind) {
        CueKind.SCREEN_EFFECT -> assetKey == null && !effect.isNullOrBlank()
        else -> effect == null && assetKey != null && ScriptAssets.forCue(doc, kind).any { it.key == assetKey }
    }

    /** Adds a cue at the end of the block's cues. */
    fun add(doc: ProjectDoc, blockKey: Key, kind: CueKind, assetKey: Key?, effect: String?, ctx: EditContext): ProjectDoc {
        val block = directionBlock(doc, blockKey) ?: return doc
        if (!fits(doc, kind, assetKey, effect)) return doc
        val cue = DirectionCue(key = ctx.newKey(), kind = kind.open(), assetKey = assetKey, effect = effect)
        return doc.replaceBlock(block.copy(cues = block.cues + cue, updatedAt = ctx.now, updatedBy = ctx.device))
    }

    /** `바꾸기`: the cue keeps its key, place and kind; its asset or effect changes. */
    fun replace(doc: ProjectDoc, blockKey: Key, cueKey: Key, assetKey: Key?, effect: String?, ctx: EditContext): ProjectDoc {
        val block = directionBlock(doc, blockKey) ?: return doc
        val cue = block.cues.firstOrNull { it.key == cueKey } ?: return doc
        val kind = cue.kind.known ?: return doc
        if (!fits(doc, kind, assetKey, effect)) return doc
        val changed = cue.copy(assetKey = assetKey, effect = effect)
        return doc.replaceBlock(block.copy(cues = block.cues.map { if (it.key == cueKey) changed else it }, updatedAt = ctx.now, updatedBy = ctx.device))
    }

    /** `빼기`: the cue leaves the block (the block may become empty, SCR-04). */
    fun remove(doc: ProjectDoc, blockKey: Key, cueKey: Key, ctx: EditContext): ProjectDoc {
        val block = directionBlock(doc, blockKey) ?: return doc
        if (block.cues.none { it.key == cueKey }) return doc
        return doc.replaceBlock(block.copy(cues = block.cues.filterNot { it.key == cueKey }, updatedAt = ctx.now, updatedBy = ctx.device))
    }

    private fun directionBlock(doc: ProjectDoc, blockKey: Key) =
        doc.index.block(blockKey)?.takeIf { it.kind.known == BlockKind.DIRECTION && doc.manuscript.isLiveBlock(it.key) }
}
