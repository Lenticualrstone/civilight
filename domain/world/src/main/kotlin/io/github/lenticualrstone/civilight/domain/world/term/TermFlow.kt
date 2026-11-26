package io.github.lenticualrstone.civilight.domain.world.term

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.Position
import io.github.lenticualrstone.civilight.model.position.Rank
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/**
 * One cell's mark in the term card's `쓰인 흐름` strip (board `ImmContent` term branch). Ordered by weight:
 * when one episode holds several kinds, the heaviest wins (`▲` > used > `◇` > none). [symbol] is the text glyph;
 * `USED` has none (`●` is not in the DS-17 set, so the UI draws a filled `acc` dot as a shape).
 */
enum class FlowMark(val symbol: String) {
    NONE(""),

    /** `◇`: a text match not yet linked (rule 21 `후보`). */
    CANDIDATE("◇"),

    /** Used: an `@` link in game output (a `맞음` pre-reveal use counts here, it leaves every `▲`); drawn as a dot. */
    USED(""),

    /** `▲`: a link before the reveal that is not marked `맞음`. */
    BEFORE_REVEAL("▲"),
}

/** One episode cell: its code (`1-4`), the mark, and [reveal] = the term's first reveal lies in it (the ring). */
data class FlowCell(val episodeKey: Key, val code: String, val mark: FlowMark, val reveal: Boolean)

/** The strip: every live episode in manuscript order; [title] `쓰인 흐름 · 1-1 ~ 1-7`. Empty without an episode level. */
data class TermFlowStrip(val cells: List<FlowCell>) {
    val title: String?
        get() {
            if (cells.isEmpty()) return null
            // An episode without a code (e.g. an unnumbered level) gives no range text, never `쓰인 흐름 ·  ~ `.
            val codes = cells.map { it.code }.filter { it.isNotBlank() }
            return when {
                codes.isEmpty() -> TermFlow.TITLE
                codes.size == 1 || codes.first() == codes.last() -> "${TermFlow.TITLE}${Copy.SEP}${codes.first()}"
                else -> "${TermFlow.TITLE}${Copy.SEP}${codes.first()} ~ ${codes.last()}"
            }
        }

    /** More cells than one screen shows: the strip scrolls sideways. */
    val scrolls: Boolean get() = cells.size > TermFlow.MAX_VISIBLE
}

/**
 * (2026-11-26) The term card's `쓰인 흐름` (board `ImmContent` term branch, `ImmTermWhy`): one cell per episode,
 * built from the card's own usage and candidate rows, so it never counts anything the `쓰인 곳` list does not
 * show (manuscript links only, candidates only while they are shown). Pure.
 */
object TermFlow {
    const val TITLE = "쓰인 흐름"

    /** The legend under the strip, in words; the UI draws each word after its mark (dot, `◇`, `▲`). */
    const val LEGEND = "쓴 곳 · 후보 · 공개 전"

    /** The legend's entries in [LEGEND] order. */
    val LEGEND_MARKS: List<Pair<FlowMark, String>> = listOf(FlowMark.USED to "쓴 곳", FlowMark.CANDIDATE to "후보", FlowMark.BEFORE_REVEAL to "공개 전")

    /** Cells one screen width holds; more scroll sideways. */
    const val MAX_VISIBLE = 12

    /** Live episodes in manuscript order (episodes after the 집필 선 included, with no content); none without an episode level. */
    fun episodes(doc: ProjectDoc): List<StoryUnit> {
        val level = doc.project.structure.episodeLevel ?: return emptyList()
        val manuscript = doc.manuscript
        return doc.story.units
            .filter { it.level == level && doc.liveness.isUnitLive(it.key) }
            .sortedBy { manuscript.rankOf(Position.Start(it.key)) ?: Rank.UNPLACED }
    }

    /** The episode unit holding [unitKey] (itself at the episode level), or null (above it, unknown, no episode level). */
    fun episodeOfUnit(doc: ProjectDoc, unitKey: Key): Key? {
        val level = doc.project.structure.episodeLevel ?: return null
        val unit = doc.index.unit(unitKey) ?: return null
        if (unit.level == level) return unit.key
        if (unit.level < level) return null
        return doc.index.ancestors(unit).firstOrNull { it.level == level }?.key
    }

    /** The episode of a manuscript spot; null outside the manuscript. */
    fun episodeOf(doc: ProjectDoc, spot: TextSpot): Key? {
        val sceneKey = when (spot) {
            is ScriptSpot.BlockText -> doc.index.block(spot.blockKey)?.sceneKey
            is ScriptSpot.OptionText -> doc.index.block(spot.blockKey)?.sceneKey
            is ScriptSpot.TransitionText -> spot.sceneKey
            else -> null
        } ?: return null
        return episodeOfUnit(doc, sceneKey)
    }

    /** The episode of a reveal: its block's, else its unit anchor's (a chapter-level anchor has none). */
    fun episodeOf(doc: ProjectDoc, reveal: RevealInfo): Key? {
        reveal.blockKey?.let { b -> doc.index.block(b)?.let { return episodeOfUnit(doc, it.sceneKey) } }
        return when (val a = reveal.record.anchor) {
            is Anchor.UnitStart -> episodeOfUnit(doc, a.unitKey)
            is Anchor.UnitEnd -> episodeOfUnit(doc, a.unitKey)
            else -> null
        }
    }

    /**
     * The pure core: [episodes] (key, code) in order, the marks found per episode key (any order, repeats
     * allowed; keys not in [episodes] are dropped), and the reveal's episode. Each cell takes its heaviest mark.
     */
    fun cells(episodes: List<Pair<Key, String>>, marks: List<Pair<Key, FlowMark>>, revealEpisode: Key?): List<FlowCell> {
        val best = HashMap<Key, FlowMark>()
        for ((key, mark) in marks) {
            val now = best[key]
            if (now == null || mark.ordinal > now.ordinal) best[key] = mark
        }
        return episodes.map { (key, code) -> FlowCell(key, code, best[key] ?: FlowMark.NONE, key == revealEpisode) }
    }

    /**
     * The strip of the card [card] read by [queries]: usage rows give a used mark (or `▲` when [UsageRow.preReveal] and
     * not [UsageRow.intended]), candidate rows `◇`; the reveal rings its episode even with no use there.
     */
    fun of(queries: TermQueries, card: TermCard): TermFlowStrip {
        val doc = queries.doc
        val list = episodes(doc)
        if (list.isEmpty()) return TermFlowStrip(emptyList())
        val codes: StoryCodes = queries.codes
        val marks = ArrayList<Pair<Key, FlowMark>>()
        for (row in card.usage.rows) {
            val ep = episodeOf(doc, row.link.spot) ?: continue
            marks += ep to if (row.preReveal && !row.intended) FlowMark.BEFORE_REVEAL else FlowMark.USED
        }
        for (row in card.candidates.rows) {
            val ep = episodeOf(doc, row.candidate.spot) ?: continue
            marks += ep to FlowMark.CANDIDATE
        }
        val revealEpisode = queries.reveal(card.termKey)?.let { episodeOf(doc, it) }
        val keyed = list.map { it.key to (codes.episodeCode(it.key) ?: "") }
        return TermFlowStrip(cells(keyed, marks, revealEpisode))
    }
}
