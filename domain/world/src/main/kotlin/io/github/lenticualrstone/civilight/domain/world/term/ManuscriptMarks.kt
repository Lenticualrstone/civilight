package io.github.lenticualrstone.civilight.domain.world.term

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.model.Key

/**
 * A term link in a manuscript container (WLD-27 row `연결` / `공개 전 연결`). Offsets are in the
 * spot's shown text. [preReveal] = draw the amber `▲` in the line's margin (always, regardless of
 * layers); a `맞음` link ([intended]) keeps the plain link look. A link to a hidden term while
 * locked is [masked]: its text reads `숨긴 항목` and the term emphasis (`▲`) is removed (SCR-55).
 */
data class LinkMark(
    val linkKey: Key,
    val termKey: Key,
    val start: Int,
    val end: Int,
    val preReveal: Boolean,
    val intended: Boolean,
    val masked: Boolean,
)

/** A candidate span (WLD-27 row `후보`): dotted underline while the `용어 후보` layer is on. */
data class CandidateMark(
    val candidate: TermCandidate,
    val termName: String,
    /** `후보 · 끊김` or `후보`. */
    val tag: String,
) {
    val start: Int get() = candidate.start
    val end: Int get() = candidate.end
}

/**
 * The small window under a tapped candidate (board `RTCandidate`, SCR-56): [title] = the term name,
 * [tag] = `후보 · 끊김` / `후보`, [revealLine] = `첫 공개 1-7 · 잇기 시` and [warning] = `▲ 공개 전 사용`
 * only when linking would create a pre-reveal use, then the buttons `잇기` (primary) · `아님` (ghost).
 */
data class CandidatePopover(
    val candidate: TermCandidate,
    val title: String,
    val tag: String,
    val revealLine: String?,
    val warning: String?,
    val linkLabel: String,
    val rejectLabel: String,
)

/** Everything the manuscript draws for one container (block text, option line, direct 전환 문구). */
data class SpotMarks(val spot: ScriptSpot, val links: List<LinkMark>, val candidates: List<CandidateMark>) {
    /** The margin `▲` of the line: some link here is an active pre-reveal use. */
    val marginWarning: Boolean get() = links.any { it.preReveal && !it.intended && !it.masked }
}

/**
 * The display contract the script renders (WLD-27, SCR-56): per container the link spans with
 * their `▲` state and the current candidate spans with their term, plus the popover of a candidate.
 * Usage and `▲` never wait for the candidate index; candidates follow [TermQueries.candidates]
 * (searchable terms only, `아님` removed, text still current). The `잇기` / `아님` writes are
 * [TermEdits.link] and [TermEdits.reject].
 */
class ManuscriptMarks(private val queries: TermQueries) {
    private val currentByTerm = HashMap<Key, Set<TermCandidate>>()

    private fun isShown(candidate: TermCandidate): Boolean =
        candidate in currentByTerm.getOrPut(candidate.termKey) { queries.candidates(candidate.termKey).toSet() }

    /** The marks of [spot]; empty lists when the container is gone. */
    fun at(spot: ScriptSpot): SpotMarks {
        val doc = queries.doc
        val links = queries.allLinks.filter { it.spot == spot }.map { link ->
            val term = doc.index.term(link.termKey)
            val masked = term != null && term.hidden && !queries.access.hiddenUnlocked
            val pre = !masked && queries.isBeforeReveal(link)
            LinkMark(
                linkKey = link.linkKey,
                termKey = link.termKey,
                start = link.start,
                end = link.end,
                preReveal = pre,
                intended = pre && queries.isIntended(link),
                masked = masked,
            )
        }
        val candidates = queries.index.at(spot).filter(::isShown).sortedBy { it.start }.mapNotNull { c ->
            val term = queries.term(c.termKey) ?: return@mapNotNull null
            CandidateMark(c, term.name, tagOf(c))
        }
        return SpotMarks(spot, links, candidates)
    }

    /** The popover of a tapped candidate, or null when it is no longer shown. */
    fun popover(candidate: TermCandidate): CandidatePopover? {
        if (!isShown(candidate)) return null
        val term = queries.term(candidate.termKey) ?: return null
        val pre = queries.isBeforeReveal(candidate)
        val reveal = queries.reveal(candidate.termKey)
        return CandidatePopover(
            candidate = candidate,
            title = term.name,
            tag = tagOf(candidate),
            revealLine = if (pre && reveal != null) "첫 공개 ${reveal.episodeLabel.orEmpty()}${Copy.SEP}잇기 시" else null,
            warning = if (pre) WARNING else null,
            linkLabel = LINK,
            rejectLabel = REJECT,
        )
    }

    private fun tagOf(candidate: TermCandidate): String =
        if (candidate.kind == CandidateKind.CUT_OFF) "후보${Copy.SEP}끊김" else "후보"

    companion object {
        const val WARNING = "▲ 공개 전 사용"
        const val LINK = "잇기"
        const val REJECT = "아님"
    }
}
