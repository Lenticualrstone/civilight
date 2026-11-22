package io.github.lenticualrstone.civilight.domain.story.time

import io.github.lenticualrstone.civilight.model.Key

/** A closed integer interval `[min, max]` (days or minutes). */
data class Span(val min: Int, val max: Int) {
    init {
        require(min <= max) { "empty span [$min, $max]" }
    }

    val isPoint: Boolean get() = min == max
    val width: Int get() = max - min

    operator fun plus(other: Span): Span = Span(min + other.min, max + other.max)
    operator fun plus(value: Int): Span = Span(min + value, max + value)
    operator fun minus(value: Int): Span = Span(min - value, max - value)
    operator fun times(factor: Int): Span =
        if (factor >= 0) Span(min * factor, max * factor) else Span(max * factor, min * factor)

    operator fun contains(value: Int): Boolean = value in min..max

    /** Midpoint; `.5` rounds up (STO-22, Q30). */
    val midpoint: Int get() = Math.floorDiv(min + max + 1, 2)

    companion object {
        fun point(value: Int) = Span(value, value)
    }
}

/**
 * A linear expression `constant + Σ coef·u` over named range variables `u ∈ [0, width]` (STO-22).
 * Each variable is one `대략` link of a base chain, keyed by the scene that holds the link, so two
 * scenes behind the same link share it and their difference is exact.
 */
data class LinExpr(val constant: Int, val terms: Map<Key, Term> = emptyMap()) {
    data class Term(val coef: Int, val width: Int)

    val isExact: Boolean get() = terms.isEmpty()

    /** Bounds over all variables. */
    val range: Span
        get() {
            var lo = constant
            var hi = constant
            for ((_, t) in terms) {
                val v = t.coef * t.width
                if (v < 0) lo += v else hi += v
            }
            return Span(lo, hi)
        }

    operator fun plus(value: Int): LinExpr = copy(constant = constant + value)

    /** Adds a fresh variable of [width] named [key] (a `대략` link). */
    fun withVariable(key: Key, width: Int): LinExpr =
        if (width <= 0) this else copy(terms = terms + (key to Term((terms[key]?.coef ?: 0) + 1, width)))

    /**
     * `this − other`, cancelling shared variables. Two variables with the same key but different
     * widths (a link before and after an edit) are different variables and do not cancel.
     */
    operator fun minus(other: LinExpr): LinExpr {
        val out = LinkedHashMap<Key, Term>()
        for ((k, t) in terms) out[k] = t
        for ((k, t) in other.terms) {
            val existing = out[k]
            if (existing != null && existing.width != t.width) {
                out["$k'"] = Term(-t.coef, t.width)
                continue
            }
            val coef = (existing?.coef ?: 0) - t.coef
            if (coef == 0) out.remove(k) else out[k] = Term(coef, t.width)
        }
        return LinExpr(constant - other.constant, out)
    }

    companion object {
        fun of(value: Int) = LinExpr(value)
    }
}
