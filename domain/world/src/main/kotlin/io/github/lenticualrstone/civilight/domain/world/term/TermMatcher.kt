package io.github.lenticualrstone.civilight.domain.world.term

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.world.Term

/** One searchable string of a term: its name or one alias (NFC, trimmed). */
data class TermPattern(val termKey: Key, val text: String, val alias: Boolean) {
    companion object {
        /** Name and aliases of [term] with at least 2 syllables (WLD-21 1). */
        fun of(term: Term): List<TermPattern> =
            (listOf(TermPattern(term.key, TermText.clean(term.name), alias = false)) +
                term.aliases.map { TermPattern(term.key, TermText.clean(it), alias = true) })
                .filter { it.text.isNotEmpty() && TermText.syllables(it.text) >= 2 }
                .distinctBy { TermText.fold(it.text) }

        /** True when the name and every alias have fewer than 2 syllables (`1음절 · 글자 찾기 안 함`). */
        fun isSingleSyllable(term: Term): Boolean = of(term).isEmpty()
    }
}

/** `FULL` = name or alias; `CUT_OFF` = `끊김` (a prefix followed by a cut-off mark). */
enum class CandidateKind { FULL, CUT_OFF }

/** A match in one scanned segment; offsets are in the segment as given (UTF-16, end exclusive). */
data class SegmentMatch(
    val termKey: Key,
    val start: Int,
    val end: Int,
    /** NFC text of the span. */
    val matchedText: String,
    val kind: CandidateKind,
    /** The pattern of a full match; null for a cut-off. */
    val pattern: TermPattern?,
)

/**
 * The rule-21 text matcher (WLD-21): one trie over every pattern, walked from each word start.
 * Spaces inside a name match any run of whitespace or none (Q-04 h); particles are checked after the
 * last word only; cut-off prefixes of at least 2 syllables followed by a cut-off mark are `끊김`;
 * overlapping matches resolve to the longest (equal spans of different terms are all kept, Q-04 e).
 */
class TermMatcher(
    patterns: List<TermPattern>,
    particles: List<String> = Particles.BUILT_IN,
    private val options: MatcherOptions = MatcherOptions(),
) {
    private class Node(val syllables: Int, val viaSpace: Boolean) {
        val children = HashMap<Char, Node>()
        var space: Node? = null
        val terminals = ArrayList<TermPattern>()
        val terms = LinkedHashSet<Key>()
        var longerTerms: Set<Key> = emptySet()
    }

    private data class Found(val termKey: Key, val pattern: TermPattern?, val start: Int, val end: Int, val kind: CandidateKind)

    private val root = Node(0, viaSpace = false)
    private val foldedParticles = particles.map(TermText::fold).filter { it.isNotEmpty() }.distinct()

    /** Keys of the terms that have at least one searchable pattern. */
    val termKeys: Set<Key>

    init {
        val keys = LinkedHashSet<Key>()
        for (pattern in patterns) {
            val text = TermText.fold(TermText.clean(pattern.text))
            if (text.isEmpty() || TermText.syllables(text) < 2) continue
            keys += pattern.termKey
            var node = root
            for (c in text) {
                node = if (c == ' ') {
                    node.space ?: Node(node.syllables, viaSpace = true).also { node.space = it }
                } else {
                    node.children.getOrPut(c) { Node(node.syllables + if (c.isLetterOrDigit()) 1 else 0, viaSpace = false) }
                }
                node.terms += pattern.termKey
            }
            node.terminals += pattern
        }
        termKeys = keys
        finish(root)
    }

    private fun finish(node: Node) {
        val longer = LinkedHashSet<Key>()
        for (child in node.children.values) {
            longer += child.terms
            finish(child)
        }
        node.space?.let {
            longer += it.terms
            finish(it)
        }
        node.longerTerms = longer
    }

    /** All candidates in [segment] (one stretch of plain text), in text order. */
    fun scan(segment: String): List<SegmentMatch> {
        if (termKeys.isEmpty() || segment.isEmpty()) return emptyList()
        val nfc = NfcText.of(segment)
        val raw = nfc.text
        val t = TermText.fold(raw)
        val found = ArrayList<Found>()
        for (p in t.indices) {
            if (p > 0 && !TermText.isBoundary(t[p - 1])) continue
            walk(t, p, root, p, found)
        }
        return resolve(found).map { f ->
            SegmentMatch(
                termKey = f.termKey,
                start = nfc.toOriginal(f.start),
                end = nfc.toOriginal(f.end),
                matchedText = raw.substring(f.start, f.end),
                kind = f.kind,
                pattern = f.pattern,
            )
        }
    }

    private fun walk(t: String, p: Int, node: Node, i: Int, out: MutableList<Found>) {
        if (node !== root && !node.viaSpace) {
            var full = false
            if (node.terminals.isNotEmpty() && acceptsAfter(t, i)) {
                for (pattern in node.terminals) out += Found(pattern.termKey, pattern, p, i, CandidateKind.FULL)
                full = true
            }
            if (!full && node.syllables >= 2 && node.longerTerms.isNotEmpty() && isCutOffAt(t, i)) {
                for (term in node.longerTerms) out += Found(term, null, p, i, CandidateKind.CUT_OFF)
            }
        }
        if (i < t.length) node.children[t[i]]?.let { walk(t, p, it, i + 1, out) }
        node.space?.let { space ->
            var j = i
            while (j < t.length && t[j].isWhitespace()) j++
            walk(t, p, space, j, out)
        }
    }

    private fun endsWord(t: String, j: Int): Boolean = j >= t.length || TermText.isBoundary(t[j])

    /** What may follow a full match (WLD-21 5). */
    private fun acceptsAfter(t: String, i: Int): Boolean {
        if (endsWord(t, i)) return true
        for (p1 in foldedParticles) {
            if (!t.startsWith(p1, i)) continue
            val j = i + p1.length
            if (options.continuationAfterParticle || endsWord(t, j)) return true
            for (p2 in foldedParticles) if (t.startsWith(p2, j) && endsWord(t, j + p2.length)) return true
        }
        return false
    }

    private fun isCutOffAt(t: String, i: Int): Boolean =
        i < t.length && (t[i] in TermText.CUT_OFF_MARKS || (options.asciiEllipsisCutsOff && t.startsWith("...", i)))

    private fun resolve(found: List<Found>): List<Found> {
        if (found.isEmpty()) return emptyList()
        val fullSpans = found.filter { it.kind == CandidateKind.FULL }.map { it.start to it.end }.toSet()
        // A full match beats a cut-off on the same span (Q-04 b, d); per term and start the longest wins.
        val best = found
            .filterNot { it.kind == CandidateKind.CUT_OFF && (it.start to it.end) in fullSpans }
            .groupBy { it.termKey to it.start }
            .values
            .map { list -> list.maxWith(compareBy<Found> { it.end }.thenBy { if (it.kind == CandidateKind.FULL) 1 else 0 }) }
        val ordered = best.sortedWith(
            compareByDescending<Found> { it.end - it.start }
                .thenBy { it.start }
                .thenBy { it.kind.ordinal }
                .thenBy { it.termKey },
        )
        val accepted = ArrayList<Found>()
        for (f in ordered) {
            val clash = accepted.any { a ->
                val overlaps = a.start < f.end && f.start < a.end
                overlaps && !(a.start == f.start && a.end == f.end)
            }
            if (!clash && accepted.none { it.termKey == f.termKey && it.start == f.start && it.end == f.end }) accepted += f
        }
        return accepted.sortedWith(compareBy<Found> { it.start }.thenBy { it.termKey })
    }
}
