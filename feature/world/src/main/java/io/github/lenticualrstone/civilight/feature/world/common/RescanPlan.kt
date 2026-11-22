package io.github.lenticualrstone.civilight.feature.world.common

import io.github.lenticualrstone.civilight.domain.world.term.ScriptSpot
import io.github.lenticualrstone.civilight.domain.world.term.TextSpots
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.world.Term

/**
 * What a document change means for the candidate index (WLD-26, rule 21 §7):
 * - [Full]: the first input of a project, or who may see hidden terms or the particle list changed.
 * - [Incremental]: these terms changed (rebuild the matcher, sweep the manuscript for each of them
 *   only, `후보 N 새로 찾음`) and these game-output spots changed (rescan each alone about 0.5 s after
 *   typing stops).
 * - [None]: nothing the matcher reads changed (lens, `_app/`, another area's data).
 */
sealed interface RescanPlan {
    data object Full : RescanPlan

    data class Incremental(val terms: Set<Key>, val spots: Set<ScriptSpot>) : RescanPlan

    data object None : RescanPlan
}

/** Pure diff of two scan inputs (WLD-T47). */
object RescanPlans {
    fun of(previous: ScanInput?, next: ScanInput): RescanPlan {
        if (previous == null) return RescanPlan.Full
        if (previous.access != next.access) return RescanPlan.Full
        if (previous.doc.project.termMatching != next.doc.project.termMatching) return RescanPlan.Full
        val terms = changedTerms(previous.doc, next.doc)
        val spots = changedSpots(previous.doc, next.doc)
        return if (terms.isEmpty() && spots.isEmpty()) RescanPlan.None else RescanPlan.Incremental(terms, spots)
    }

    /** Terms added, removed, or changed in a way the matcher reads (name, aliases, 글자 찾기, hidden, trash). */
    fun changedTerms(before: ProjectDoc, after: ProjectDoc): Set<Key> {
        if (before.world.terms === after.world.terms) return emptySet()
        val old = before.world.terms.associateBy { it.key }
        val new = after.world.terms.associateBy { it.key }
        return (old.keys + new.keys).filterTo(LinkedHashSet()) { key ->
            val a = old[key]
            val b = new[key]
            a == null || b == null || searchShape(a) != searchShape(b)
        }
    }

    private fun searchShape(term: Term) = listOf(term.name, term.aliases, term.textSearch, term.hidden, term.deletedAt)

    /**
     * Game-output spots whose runs changed, appeared or disappeared (a block typed in, added,
     * trashed or restored; a scene's direct 전환 문구). Reordering alone changes nothing here.
     */
    fun changedSpots(before: ProjectDoc, after: ProjectDoc): Set<ScriptSpot> {
        if (before.episodes === after.episodes && before.story === after.story) return emptySet()
        val old = runsBySpot(before)
        val new = runsBySpot(after)
        val out = LinkedHashSet<ScriptSpot>()
        for ((spot, runs) in new) if (old[spot] != runs) out += spot
        for (spot in old.keys) if (spot !in new) out += spot
        return out
    }

    private fun runsBySpot(doc: ProjectDoc): Map<ScriptSpot, List<TextRun>> =
        TextSpots.script(doc).associate { (it.spot as ScriptSpot) to it.runs }
}
