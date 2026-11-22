package io.github.lenticualrstone.civilight.feature.script.terms

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.CandidatePopover
import io.github.lenticualrstone.civilight.domain.world.term.ManuscriptMarks
import io.github.lenticualrstone.civilight.domain.world.term.ScriptSpot
import io.github.lenticualrstone.civilight.domain.world.term.StaleRejections
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.domain.world.term.TermCandidate
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.domain.world.term.TermQueries
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun

/** Copy of the `용어 후보` layer and the margin `▲` (SCR-26, SCR-56; board `RTCandidate`). */
object TermCopy {
    const val LAYER = "용어 후보"

    /** `[PROPOSED]` row description (SCR-26 table). */
    const val LAYER_SUB = "점선 밑줄 · 잇기 · 아님"

    /** The chip under the header while the layer is on: `표시 · 용어 후보 · 켬`. */
    const val CHIP = "표시 · 용어 후보 · 켬"

    /** The margin mark's label (the check it opens). */
    const val MARGIN = ManuscriptMarks.WARNING

    val all: List<String> = listOf(LAYER, LAYER_SUB, CHIP, MARGIN)
}

/** A candidate's span in a block's raw text (the domain `Runs` offsets, equal to the spot's shown text). */
data class CandidateSpan(val start: Int, val end: Int, val candidate: TermCandidate)

/** Term marks of one block: candidate spans (only while the layer is on) and the margin `▲`. */
data class BlockTerms(val candidates: List<CandidateSpan>, val preReveal: Boolean) {
    companion object {
        val NONE = BlockTerms(emptyList(), preReveal = false)
    }
}

/**
 * Rule 21 in the manuscript (SCR-56, WLD-27) over `domain:world` `ManuscriptMarks`: links before
 * their term's first reveal get the margin `▲` (always, whatever the layers); with the `용어 후보`
 * layer on, candidate spans of game-output text get the dotted underline. Memos and direction cues
 * are never scanned and never raise `▲`. `잇기` / `아님` write through `TermEdits`.
 */
object TermMarksModel {
    /** Kinds whose text is game output (rule 21 §2). */
    private val SCANNED = setOf(BlockKind.DIALOGUE, BlockKind.NARRATION, BlockKind.SYSTEM, BlockKind.CHOICE)

    /** The marks source of one rows build; [index] is [CandidateIndex.pending] while the layer is off. */
    class Source(doc: ProjectDoc, index: CandidateIndex, unlocked: Boolean, private val candidatesOn: Boolean) {
        private val marks by lazy { ManuscriptMarks(TermQueries(doc, index, TermAccess(unlocked))) }

        fun of(block: Block): BlockTerms {
            if (block.kind.known !in SCANNED) return BlockTerms.NONE
            val hasTermLink = (block.runs + block.options.flatMap { it.runs }).any { it is TextRun.Link && it.target.kind.known == LinkTargetKind.TERM }
            if (!hasTermLink && !candidatesOn) return BlockTerms.NONE
            val main = marks.at(ScriptSpot.BlockText(block.key))
            val options = block.options.map { marks.at(ScriptSpot.OptionText(block.key, it.key)) }
            val warning = main.marginWarning || options.any { it.marginWarning }
            val spans = if (candidatesOn) main.candidates.map { CandidateSpan(it.start, it.end, it.candidate) } else emptyList()
            return BlockTerms(spans, warning)
        }

        /** The small window of a tapped candidate, or null when it is no longer shown. */
        fun popover(candidate: TermCandidate): CandidatePopover? = marks.popover(candidate)
    }

    /** The candidate whose span holds the character at raw offset [raw]. */
    fun hit(spans: List<CandidateSpan>, raw: Int): TermCandidate? = spans.firstOrNull { raw >= it.start && raw < it.end }?.candidate

    /** `잇기`: exactly that text range becomes a link run; null when the candidate is stale. */
    fun link(doc: ProjectDoc, candidate: TermCandidate, ctx: EditContext): ProjectDoc? = TermEdits.link(doc, candidate, ctx)?.doc

    /** `아님`: a dismissal (block key, term key, matched text, occurrence) on the term. */
    fun reject(doc: ProjectDoc, candidate: TermCandidate, ctx: EditContext): ProjectDoc = TermEdits.reject(doc, candidate, ctx)

    /**
     * WLD-22: after a text edit of [blockKey] (or its option [optionKey]), `아님` records it made stale
     * are released in the same write (`StaleRejections.release` on that spot only).
     */
    fun releaseStale(doc: ProjectDoc, blockKey: Key, optionKey: Key?, unlocked: Boolean): ProjectDoc {
        val spot = if (optionKey != null) ScriptSpot.OptionText(blockKey, optionKey) else ScriptSpot.BlockText(blockKey)
        return StaleRejections.release(doc, TermAccess(unlocked), setOf(spot))
    }
}
