package io.github.lenticualrstone.civilight.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class OrderKeysTest {
    @Test
    fun firstKeyOfEmptyListIsV() {
        assertEquals("V", OrderKeys.between(null, null))
        assertEquals("V", OrderKeys.FIRST)
    }

    @Test
    fun referenceExamples() {
        // Midpoints of the reference algorithm (§1.2), checked by hand.
        assertEquals("k", OrderKeys.between("V", null)) // (31 + 62) / 2 = 46 → 'k'
        assertEquals("F", OrderKeys.between(null, "V")) // (0 + 31) / 2 = 15 → 'F'
        assertEquals("A5", OrderKeys.between("A", "AB"))
        assertEquals("A1V", OrderKeys.between("A1", "A2"))
        assertEquals("A00V", OrderKeys.between("A", "A01"))
        assertEquals("zV", OrderKeys.between("z", null))
        assertEquals("0V", OrderKeys.between(null, "1"))
    }

    @Test
    fun validity() {
        assertTrue(OrderKeys.isValid("V"))
        assertTrue(OrderKeys.isValid("a0b"))
        assertFalse(OrderKeys.isValid(""))
        assertFalse(OrderKeys.isValid("A0"))
        assertFalse(OrderKeys.isValid("a-b"))
        assertFalse(OrderKeys.isValid("가"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfOrderBounds() {
        OrderKeys.between("b", "a")
    }

    @Test
    fun comparisonIsAsciiNotLocale() {
        // '9' < 'A' < 'Z' < 'a' in ASCII; a locale compare would put 'a' before 'B'.
        val keys = listOf("a", "B", "9", "Z", "A", "z")
        assertEquals(listOf("9", "A", "B", "Z", "a", "z"), keys.sorted())
    }

    @Test
    fun betweenIsStrictlyBetween() {
        val random = Random(7)
        repeat(2_000) {
            val a = randomKey(random)
            val b = randomKey(random)
            if (a == b) return@repeat
            val (lo, hi) = if (a < b) a to b else b to a
            val mid = OrderKeys.between(lo, hi)
            assertTrue("$lo < $mid < $hi", lo < mid && mid < hi)
            assertTrue(OrderKeys.isValid(mid))
        }
    }

    @Test
    fun tenThousandRandomInsertionsStaySorted() {
        val random = Random(42)
        val list = mutableListOf<String>()
        repeat(10_000) {
            val at = random.nextInt(list.size + 1)
            val before = list.getOrNull(at - 1)
            val after = list.getOrNull(at)
            val key = OrderKeys.between(before, after)
            assertTrue(OrderKeys.isValid(key))
            list.add(at, key)
        }
        assertEquals(list.sorted(), list)
        assertEquals(list.size, list.toSet().size)
    }

    @Test
    fun appendOnlyGrowsAndSignalsRebalance() {
        var last: String? = null
        var count = 0
        while (true) {
            val next = OrderKeys.after(last)
            last?.let { assertTrue(it < next) }
            last = next
            count++
            if (OrderKeys.needsRebalance(next)) break
            check(count < 100_000)
        }
        assertTrue("append-only reaches 41 chars only after many inserts: $count", count > 100)
    }

    @Test
    fun prependOnlyGrowsAndSignalsRebalance() {
        var first: String? = null
        var count = 0
        while (true) {
            val next = OrderKeys.before(first)
            first?.let { assertTrue(next < it) }
            first = next
            count++
            if (OrderKeys.needsRebalance(next)) break
            check(count < 100_000)
        }
        assertTrue(count > 100)
    }

    @Test
    fun repeatedSameGapInsertionNeedsRebalance() {
        // Always inserting right after the first key grows by about one character every 5–6 inserts.
        val a = OrderKeys.FIRST
        var b = OrderKeys.after(a)
        var inserts = 0
        while (!OrderKeys.needsRebalance(b)) {
            b = OrderKeys.between(a, b)
            inserts++
        }
        assertTrue(inserts in 100..400)
    }

    @Test
    fun evenlySpacedKeysAreValidDistinctAndSorted() {
        for (n in listOf(0, 1, 2, 61, 62, 63, 500, 3_844, 10_000)) {
            val keys = OrderKeys.evenlySpaced(n)
            assertEquals(n, keys.size)
            assertTrue(keys.all(OrderKeys::isValid))
            assertEquals(keys.sorted(), keys)
            assertEquals(n, keys.toSet().size)
            assertTrue(keys.all { !OrderKeys.needsRebalance(it) })
        }
    }

    @Test
    fun rebalanceKeepsOrderAndAllowsInsertion() {
        val items = (1..300).map { "item$it" }
        val rebalanced = OrderKeys.rebalance(items)
        assertEquals(items, rebalanced.map { it.first })
        val keys = rebalanced.map { it.second }
        assertEquals(keys.sorted(), keys)
        val inserted = OrderKeys.between(keys[10], keys[11])
        assertTrue(keys[10] < inserted && inserted < keys[11])
    }

    @Test
    fun nKeysBetween() {
        val keys = OrderKeys.between("A", "B", 50)
        assertEquals(50, keys.size)
        assertEquals(keys.sorted(), keys)
        assertEquals(50, keys.toSet().size)
        assertTrue(keys.all { it > "A" && it < "B" && OrderKeys.isValid(it) })
        assertTrue(keys.all { it.length <= 4 })
    }

    private fun randomKey(random: Random): String {
        val length = random.nextInt(1, 6)
        val chars = CharArray(length) { OrderKeys.DIGITS[random.nextInt(62)] }
        if (chars.last() == '0') chars[chars.size - 1] = '1'
        return String(chars)
    }
}
