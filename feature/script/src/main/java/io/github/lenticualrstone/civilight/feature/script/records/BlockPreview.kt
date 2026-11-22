package io.github.lenticualrstone.civilight.feature.script.records

import io.github.lenticualrstone.civilight.domain.script.BlockOps
import io.github.lenticualrstone.civilight.domain.script.DirectionCues
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.Links
import io.github.lenticualrstone.civilight.domain.script.Manuscript
import io.github.lenticualrstone.civilight.domain.script.Runs
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/**
 * A block as one list row outside the manuscript (select mode SCR-36, the `다른 블록에 잇기` list
 * SCR-43, the move picker SCR-37): the label line `하르트 #043` and the text on one line.
 */
data class BlockLine(val label: String, val text: String)

object BlockPreview {
    /**
     * The row of [block]. While hidden items are locked ([unlocked] false) a hidden speaker reads
     * `숨긴 인물` and a link to a hidden item its mask, as the manuscript draws them (rule 19).
     */
    fun of(doc: ProjectDoc, block: Block, unlocked: Boolean): BlockLine = BlockLine(label(doc, block, unlocked), text(doc, block, unlocked))

    /** `{label} #{n}` (SCR-36): the speaker as the manuscript names it, or the kind word. */
    fun label(doc: ProjectDoc, block: Block, unlocked: Boolean): String {
        val who = if (block.kind.known == BlockKind.DIALOGUE) {
            when (val l = Manuscript.speakerLabel(doc, block, unlocked)) {
                is Manuscript.SpeakerLabel.Named -> l.name
                Manuscript.SpeakerLabel.Hidden -> Manuscript.SpeakerLabel.Hidden.TEXT
                is Manuscript.SpeakerLabel.Deleted -> l.text
                Manuscript.SpeakerLabel.Missing, null -> Manuscript.SpeakerLabel.Missing.TEXT
            }
        } else {
            Labels.kindLabel(block.kind.known)
        }
        return "$who ${Labels.number(block.number)}"
    }

    /** The block's text on one line: runs with `(…)`, a direction block's cues, a choice block's options. */
    fun text(doc: ProjectDoc, block: Block, unlocked: Boolean): String = when (block.kind.known) {
        BlockKind.DIRECTION -> DirectionCues.blockLine(doc, block.cues)
        BlockKind.CHOICE -> BlockOps.orderedOptions(block).joinToString(" · ") { runsText(doc, it.runs, unlocked) }
        else -> runsText(doc, block.runs, unlocked)
    }.replace('\n', ' ').trim()

    private fun runsText(doc: ProjectDoc, runs: List<TextRun>, unlocked: Boolean): String = runs.joinToString("") { run ->
        if (run is TextRun.Link) {
            when (val state = Links.state(doc, run, unlocked)) {
                is Links.State.Hidden -> state.text
                else -> run.text
            }
        } else {
            Runs.displayText(listOf(run))
        }
    }
}
