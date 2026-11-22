package io.github.lenticualrstone.civilight.model

/**
 * Manual order value (§1.2). Compared with plain `String.compareTo` (ASCII, shorter prefix first);
 * never with a Collator.
 */
typealias OrderKey = String

/** Generation, validation and rebalancing of [OrderKey]s exactly as §1.2 specifies. */
object OrderKeys {
    const val DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

    /** Keys longer than this trigger a rebalance of their sibling list (§1.2). */
    const val MAX_LENGTH = 40

    /** The first key of an empty list. */
    val FIRST: OrderKey = between(null, null)

    fun isValid(key: String): Boolean =
        key.isNotEmpty() && key.last() != '0' && key.all { DIGITS.indexOf(it) >= 0 }

    /** True when [key] is too long and its sibling list should be rewritten with [evenlySpaced]. */
    fun needsRebalance(key: OrderKey): Boolean = key.length > MAX_LENGTH

    /**
     * A key strictly between [a] (exclusive; null = before everything) and [b] (exclusive; null =
     * after everything). Deterministic: the reference base-62 midpoint of §1.2.
     */
    fun between(a: OrderKey?, b: OrderKey?): OrderKey {
        if (a != null) require(isValid(a)) { "invalid order key: $a" }
        if (b != null) require(isValid(b)) { "invalid order key: $b" }
        if (a != null && b != null) require(a < b) { "order keys out of order: $a >= $b" }
        return mid(a ?: "", b)
    }

    /** [n] keys strictly between [a] and [b], in increasing order, kept short by bisection. */
    fun between(a: OrderKey?, b: OrderKey?, n: Int): List<OrderKey> {
        require(n >= 0)
        if (n == 0) return emptyList()
        if (n == 1) return listOf(between(a, b))
        val midIndex = n / 2
        val c = between(a, b)
        return between(a, c, midIndex) + c + between(c, b, n - midIndex - 1)
    }

    /** A key after [last] (null = empty list). */
    fun after(last: OrderKey?): OrderKey = between(last, null)

    /** A key before [first] (null = empty list). */
    fun before(first: OrderKey?): OrderKey = between(null, first)

    /**
     * [count] valid, distinct, increasing keys spread evenly over the key space, all of the
     * shortest length that fits. Used to rebalance one sibling list (§1.2).
     */
    fun evenlySpaced(count: Int): List<OrderKey> {
        require(count >= 0)
        if (count == 0) return emptyList()
        var length = 1
        var capacity = 62L
        while (capacity <= count) {
            length++
            capacity *= 62
        }
        val step = capacity / (count + 1)
        return (1..count).map { i -> encode(i * step, length).trimEnd('0') }
    }

    /**
     * New order keys for [items] (already in the wanted order), evenly spaced. Returns pairs of
     * item and its new key; callers write them in one change.
     */
    fun <T> rebalance(items: List<T>): List<Pair<T, OrderKey>> = items.zip(evenlySpaced(items.size))

    private fun encode(value: Long, length: Int): String {
        val chars = CharArray(length)
        var v = value
        for (i in length - 1 downTo 0) {
            chars[i] = DIGITS[(v % 62).toInt()]
            v /= 62
        }
        return String(chars)
    }

    // a: lower bound, "" = before everything; b: upper bound, null = after everything; requires a < b
    private fun mid(a: String, b: String?): String {
        if (b != null) {
            var n = 0
            while (n < b.length && (a.getOrElse(n) { '0' }) == b[n]) n++
            if (n > 0) return b.substring(0, n) + mid(a.drop(n), b.drop(n))
        }
        val da = if (a.isEmpty()) 0 else DIGITS.indexOf(a[0])
        val db = if (b == null) DIGITS.length else DIGITS.indexOf(b[0])
        if (db - da > 1) return DIGITS[(da + db) / 2].toString()
        return if (b != null && b.length > 1) {
            b.substring(0, 1)
        } else {
            DIGITS[da] + mid(a.drop(1), null)
        }
    }
}

/** Sorts by `order`, then by key so equal order keys (from merges) still have a stable order. */
fun <T> List<T>.sortedByOrder(order: (T) -> OrderKey, key: (T) -> String): List<T> =
    sortedWith(compareBy<T>(order).thenBy(key))
