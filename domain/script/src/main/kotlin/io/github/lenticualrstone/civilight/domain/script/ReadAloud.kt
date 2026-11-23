package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.text.isBlankText

/**
 * (0.2.0, N-27, SCR-28) `낭독`: the episode's live blocks as steps, one at a time. No TTS. Memos, empty blocks,
 * blocks of a locked hidden scene and unknown kinds are skipped; scene heads are not steps.
 */
object ReadAloud {
    sealed interface Step {
        val blockKey: Key

        /** The `배경` image in effect at this step (switch on and an image), else null: a solid background. */
        val background: Key?

        /** `대사` ([speaker] set), `나레이션` and `시스템 텍스트` ([speaker] null). */
        data class Line(override val blockKey: Key, val speaker: String?, val text: String, override val background: Key?) : Step

        /** `연출`: `▸ {render}`. */
        data class Caption(override val blockKey: Key, val text: String, override val background: Key?) : Step

        /** `선택지`: every option line, in order, in one step. */
        data class Choices(override val blockKey: Key, val lines: List<String>, override val background: Key?) : Step
    }

    /** [unlocked]: the hidden gate is open (locked hidden scenes and speakers are left out or masked). */
    fun steps(doc: ProjectDoc, unitKey: Key, gameTools: Boolean, unlocked: Boolean = false): List<Step> {
        val out = mutableListOf<Step>()
        var background: Key? = null
        for (scene in doc.manuscript.liveScenes.filter { doc.numberingUnitOfScene(it.key) == unitKey }) {
            val locked = scene.hidden && !unlocked
            for (b in doc.manuscript.liveBlocksOf(scene.key)) {
                // The background follows the cues even through skipped blocks, so a later start shows the right one.
                if (gameTools && b.kind.known == BlockKind.DIRECTION) backgroundOf(doc, b)?.let { if (!locked) background = it }
                if (locked || !b.hasContent()) continue
                val bg = if (gameTools) background else null
                when (b.kind.known) {
                    BlockKind.DIALOGUE -> out += Step.Line(b.key, speakerOf(doc, b, unlocked), Runs.displayText(doc, b.runs, unlocked), bg)
                    BlockKind.NARRATION, BlockKind.SYSTEM -> out += Step.Line(b.key, null, Runs.displayText(doc, b.runs, unlocked), bg)
                    BlockKind.DIRECTION -> out += Step.Caption(b.key, "▸ " + directionText(doc, b, gameTools, unlocked), bg)
                    BlockKind.CHOICE -> out += Step.Choices(b.key, BlockOps.orderedOptions(b).map { Runs.displayText(doc, it.runs, unlocked) }, bg)
                    BlockKind.MEMO, null -> Unit
                }
            }
        }
        return out
    }

    /** The first step at or after [blockKey] in the episode (else the first step); -1 when there is none. */
    fun indexAt(steps: List<Step>, doc: ProjectDoc, blockKey: Key?): Int {
        if (steps.isEmpty()) return -1
        if (blockKey == null) return 0
        steps.indexOfFirst { it.blockKey == blockKey }.takeIf { it >= 0 }?.let { return it }
        val block = doc.index.block(blockKey) ?: return 0
        val order = doc.manuscript.liveScenes.map { it.key }
        val sceneIndex = order.indexOf(block.sceneKey)
        val byKey = steps.withIndex().firstOrNull { (_, s) ->
            val sb = doc.index.block(s.blockKey) ?: return@firstOrNull false
            val si = order.indexOf(sb.sceneKey)
            si > sceneIndex || (si == sceneIndex && sb.order > block.order)
        }
        return byKey?.index ?: steps.lastIndex
    }

    /** Speaker of a step: block display name, else the display name in effect (`???`), else the name (SCR-28). */
    fun speakerOf(doc: ProjectDoc, block: Block, unlocked: Boolean): String = when (val l = Manuscript.speakerLabel(doc, block, unlocked)) {
        Manuscript.SpeakerLabel.Hidden -> Manuscript.SpeakerLabel.Hidden.TEXT
        Manuscript.SpeakerLabel.Missing, null -> Manuscript.SpeakerLabel.Missing.TEXT
        is Manuscript.SpeakerLabel.Deleted -> l.name
        is Manuscript.SpeakerLabel.Named -> block.displayNameOverride?.takeIf { it.isNotBlank() } ?: l.displayName ?: l.name
    }

    /**
     * SCR-08 render of a direction block: text, the cue summary, or both with the switch on. A link to a
     * locked hidden item reads `숨긴 인물` / `숨긴 항목` ([unlocked] false, SCR-55).
     */
    fun directionText(doc: ProjectDoc, block: Block, gameTools: Boolean, unlocked: Boolean = false): String {
        val text = Runs.displayText(doc, block.runs, unlocked)
        val summary = cueSummary(doc, block)
        return when {
            block.runs.isBlankText() -> summary
            gameTools && block.cues.isNotEmpty() -> "$summary  $text"
            else -> text
        }
    }

    /** The 0.1.0 cue summary: `배경 하층 거리(새벽)  ♪ 잔불`. */
    fun cueSummary(doc: ProjectDoc, block: Block): String = block.cues.joinToString("  ") { cue ->
        val name = cue.assetKey?.let { doc.index.scriptAsset(it)?.name } ?: cue.effect.orEmpty()
        when (cue.kind.known) {
            CueKind.BACKGROUND -> "배경 $name"
            CueKind.BGM -> "♪ $name"
            CueKind.SFX -> "효과음 $name"
            CueKind.CG -> "CG $name"
            CueKind.SCREEN_EFFECT -> "화면 효과 $name"
            null -> name
        }
    }

    private fun backgroundOf(doc: ProjectDoc, block: Block): Key? =
        block.cues.lastOrNull { it.kind.known == CueKind.BACKGROUND && it.assetKey?.let { k -> doc.index.scriptAsset(k)?.fileHash } != null }?.assetKey
}
