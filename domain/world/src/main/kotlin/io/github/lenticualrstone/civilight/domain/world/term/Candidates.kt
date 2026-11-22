package io.github.lenticualrstone.civilight.domain.world.term

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.world.RejectionContainerKind
import io.github.lenticualrstone.civilight.model.world.Term
import io.github.lenticualrstone.civilight.model.world.TermRejection

/**
 * A text candidate of a term (WLD-21 output). Offsets are in the spot's text ([TextSpots.textOf]).
 * [occurrence] is the 0-based index among this term's candidates with the same [matchedText] in the
 * same spot, counted before `아님` filtering so a rejection keeps pointing at the same span.
 */
data class TermCandidate(
    val termKey: Key,
    val spot: ScriptSpot,
    val start: Int,
    val end: Int,
    val matchedText: String,
    val kind: CandidateKind,
    /** The alias that matched (full matches through an alias only). */
    val alias: String?,
    val occurrence: Int,
) {
    val containerKind: RejectionContainerKind
        get() = if (spot is ScriptSpot.TransitionText) RejectionContainerKind.SCENE_TRANSITION else RejectionContainerKind.BLOCK

    val optionKey: Key? get() = (spot as? ScriptSpot.OptionText)?.optionKey

    /** True when [rejection] (`아님`) covers this candidate. */
    fun isRejectedBy(rejection: TermRejection): Boolean =
        rejection.containerKind.known == containerKind &&
            rejection.containerKey == spot.containerKey &&
            rejection.optionKey == optionKey &&
            rejection.matchedText == matchedText &&
            rejection.occurrence == occurrence
}

/** Who sees hidden terms: candidates of a hidden term are searched only while unlocked (WLD-13, T30). */
data class TermAccess(val hiddenUnlocked: Boolean = false) {
    fun canSearch(term: Term): Boolean = term.deletedAt == null && term.textSearch && (!term.hidden || hiddenUnlocked)
}

/** Scans game-output spots of one project state with one matcher. */
class CandidateScanner(
    doc: ProjectDoc,
    access: TermAccess = TermAccess(),
    options: MatcherOptions = MatcherOptions(),
    /** Restricts the patterns to these terms (a term rescan); null = every searchable term. */
    onlyTerms: Set<Key>? = null,
) {
    /** Terms whose patterns are in the matcher. */
    val terms: List<Term> = doc.world.terms.filter { access.canSearch(it) && (onlyTerms == null || it.key in onlyTerms) }

    private val matcher = TermMatcher(
        patterns = terms.flatMap(TermPattern::of),
        particles = Particles.withExtras(doc.project.termMatching.extraParticles),
        options = options,
    )

    /** Candidates in one spot's runs, in text order. */
    fun scan(spot: ScriptSpot, runs: List<io.github.lenticualrstone.civilight.model.text.TextRun>): List<TermCandidate> {
        val matches = TextSpots.segments(runs).flatMap { segment ->
            matcher.scan(segment.text).map { it.copy(start = it.start + segment.offset, end = it.end + segment.offset) }
        }
        val seen = HashMap<Pair<Key, String>, Int>()
        return matches.sortedWith(compareBy({ it.start }, { it.termKey })).map { m ->
            val occurrenceKey = m.termKey to m.matchedText
            val occurrence = seen.getOrDefault(occurrenceKey, 0)
            seen[occurrenceKey] = occurrence + 1
            TermCandidate(
                termKey = m.termKey, spot = spot, start = m.start, end = m.end, matchedText = m.matchedText,
                kind = m.kind, alias = m.pattern?.takeIf { it.alias }?.text, occurrence = occurrence,
            )
        }
    }

    /** True when the matcher has no pattern at all. */
    val isEmpty: Boolean get() = matcher.termKeys.isEmpty()
}

/** What one scan looked at (WLD-26, T47). [sweptTerms] null = every term's patterns. */
data class ScanReport(val sweptTerms: Set<Key>?, val rescannedSpots: Set<ScriptSpot>)

/**
 * The candidate index (WLD-26): raw candidates (before `아님`) per game-output spot. Derived and
 * app-only, never written to `.civilight`. [complete] is false while the full scan after opening
 * a project is still running (`점검 중`).
 */
class CandidateIndex private constructor(
    private val bySpot: Map<ScriptSpot, List<TermCandidate>>,
    val complete: Boolean,
) {
    /** Every raw candidate (spot order is the map's insertion order; sort with [TermQueries]). */
    fun all(): List<TermCandidate> = bySpot.values.flatten()

    /** Raw candidates of one spot. */
    fun at(spot: ScriptSpot): List<TermCandidate> = bySpot[spot].orEmpty()

    /** Raw candidates of one term. */
    fun of(termKey: Key): List<TermCandidate> = bySpot.values.flatMap { list -> list.filter { it.termKey == termKey } }

    /** Spots that currently hold candidates. */
    val spots: Set<ScriptSpot> get() = bySpot.keys

    /** Rescans only [spots] (after typing stops in a block, WLD-26 row 1). Spots no longer live drop out. */
    fun rescanSpots(
        doc: ProjectDoc,
        spots: Collection<ScriptSpot>,
        access: TermAccess = TermAccess(),
        options: MatcherOptions = MatcherOptions(),
    ): Pair<CandidateIndex, ScanReport> {
        val scanner = CandidateScanner(doc, access, options)
        val next = LinkedHashMap(bySpot)
        for (spot in spots) {
            next.remove(spot)
            if (!TextSpots.isLive(doc, spot)) continue
            val runs = TextSpots.runsOf(doc, spot) ?: continue
            val found = scanner.scan(spot, runs)
            if (found.isNotEmpty()) next[spot] = found
        }
        return CandidateIndex(next, complete) to ScanReport(null, spots.toSet())
    }

    /**
     * After a term or one of its aliases changed (or `글자 찾기` was switched): sweeps the manuscript
     * with that term's patterns only, then re-resolves overlaps in the spots it touches now or
     * touched before (WLD-26 row 2).
     */
    fun rescanTerm(
        doc: ProjectDoc,
        termKey: Key,
        access: TermAccess = TermAccess(),
        options: MatcherOptions = MatcherOptions(),
    ): Pair<CandidateIndex, ScanReport> {
        val sweep = CandidateScanner(doc, access, options, onlyTerms = setOf(termKey))
        val touched = LinkedHashSet<ScriptSpot>()
        bySpot.forEach { (spot, list) -> if (list.any { it.termKey == termKey }) touched += spot }
        if (!sweep.isEmpty) {
            for (spotText in TextSpots.script(doc)) {
                val spot = spotText.spot as ScriptSpot
                if (sweep.scan(spot, spotText.runs).isNotEmpty()) touched += spot
            }
        }
        val (next, _) = rescanSpots(doc, touched, access, options)
        return next to ScanReport(setOf(termKey), touched)
    }

    /** The same index marked complete or pending. */
    fun withComplete(value: Boolean): CandidateIndex = CandidateIndex(bySpot, value)

    companion object {
        /** An empty index waiting for its first full scan (`점검 중`). */
        fun pending(): CandidateIndex = CandidateIndex(emptyMap(), complete = false)

        /** Full scan of every live game-output spot (on open or restore, WLD-26 row 3). */
        fun build(doc: ProjectDoc, access: TermAccess = TermAccess(), options: MatcherOptions = MatcherOptions()): CandidateIndex {
            val scanner = CandidateScanner(doc, access, options)
            val map = LinkedHashMap<ScriptSpot, List<TermCandidate>>()
            if (!scanner.isEmpty) {
                for (spotText in TextSpots.script(doc)) {
                    val spot = spotText.spot as ScriptSpot
                    val found = scanner.scan(spot, spotText.runs)
                    if (found.isNotEmpty()) map[spot] = found
                }
            }
            return CandidateIndex(map, complete = true)
        }

        /** [build] with a report (every term, every spot). */
        fun buildWithReport(doc: ProjectDoc, access: TermAccess = TermAccess(), options: MatcherOptions = MatcherOptions()): Pair<CandidateIndex, ScanReport> {
            val index = build(doc, access, options)
            val spots = TextSpots.script(doc).map { it.spot as ScriptSpot }.toSet()
            return index to ScanReport(null, spots)
        }
    }
}

/** `아님` records (WLD-22). */
object TermRejections {
    /** A rejection for [candidate]. */
    fun of(candidate: TermCandidate, key: Key, createdAt: Long): TermRejection = TermRejection(
        key = key,
        containerKind = candidate.containerKind.open(),
        containerKey = candidate.spot.containerKey,
        optionKey = candidate.optionKey,
        matchedText = candidate.matchedText,
        occurrence = candidate.occurrence,
        createdAt = createdAt,
    )

    /** The script spot a rejection is about, or null for an unknown container kind. */
    fun spotOf(rejection: TermRejection): ScriptSpot? = when (rejection.containerKind.known) {
        RejectionContainerKind.BLOCK -> rejection.optionKey?.let { ScriptSpot.OptionText(rejection.containerKey, it) }
            ?: ScriptSpot.BlockText(rejection.containerKey)
        RejectionContainerKind.SCENE_TRANSITION -> ScriptSpot.TransitionText(rejection.containerKey)
        null -> null
    }

    /** True when one of [term]'s rejections covers [candidate]. */
    fun isRejected(term: Term, candidate: TermCandidate): Boolean = term.rejections.any { candidate.isRejectedBy(it) }
}
