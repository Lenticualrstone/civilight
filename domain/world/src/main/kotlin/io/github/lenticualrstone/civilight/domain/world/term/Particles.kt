package io.github.lenticualrstone.civilight.domain.world.term

/** The fixed particle list of rule 21 §3 (WLD-21 5); projects may append entries (`TermMatching.extraParticles`). */
object Particles {
    val BUILT_IN: List<String> = listOf(
        "은", "는", "이", "가", "을", "를", "의", "에", "에서", "에게", "한테", "로", "으로", "와", "과", "도", "만",
        "까지", "부터", "처럼", "보다", "이나", "나", "이랑", "랑", "이야", "야", "이다", "이었", "였", "이라", "라",
    )

    /** Built-in entries plus the project's additions (NFC, trimmed, non-empty, without duplicates). */
    fun withExtras(extra: List<String>): List<String> =
        (BUILT_IN + extra.map(TermText::clean).filter { it.isNotEmpty() }).distinct()
}

/**
 * Switches for the matcher's pending decisions (WLD-21 Q-04). Defaults are the spec's proposals.
 *
 * @property continuationAfterParticle (a) letters may follow the particle(s) inside the same word;
 *   default false, so `오리지늄이었다` is not a match.
 * @property asciiEllipsisCutsOff (c) ASCII `...` marks a cut-off; default false.
 */
data class MatcherOptions(
    val continuationAfterParticle: Boolean = false,
    val asciiEllipsisCutsOff: Boolean = false,
)
