package io.github.lenticualrstone.civilight.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class KeysTest {
    @Test
    fun formatIs26CrockfordUpperCase() {
        repeat(200) {
            val key = Keys.newKey()
            assertEquals(26, key.length)
            assertTrue(key, Keys.isValid(key))
            assertTrue(key.none { it in "ILOU" || it.isLowerCase() })
        }
    }

    @Test
    fun timestampIsEncodedInTheFirstTenChars() {
        val ms = 1_795_304_460_000L // 2026-11-21T23:41:00Z
        val generator = UlidGenerator(clock = { ms }, random = Random(1))
        val key = generator.next()
        assertEquals(ms, Keys.timestampOf(key))
        assertEquals("01M807R3Q0", key.substring(0, 10))
    }

    @Test
    fun monotonicWithinOneMillisecond() {
        val generator = UlidGenerator(clock = { 1_000L }, random = Random(3))
        val keys = List(5_000) { generator.next() }
        assertEquals(keys.sorted(), keys)
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.all { Keys.timestampOf(it) == 1_000L })
    }

    @Test
    fun monotonicWhenTheClockGoesBackwards() {
        var now = 10_000L
        val generator = UlidGenerator(clock = { now }, random = Random(5))
        val a = generator.next()
        now = 9_000L
        val b = generator.next()
        assertTrue(a < b)
        assertEquals(10_000L, Keys.timestampOf(b))
    }

    @Test
    fun randomOverflowAdvancesTheTimestamp() {
        val allOnes = object : Random() {
            override fun nextInt(bound: Int): Int = bound - 1
            override fun nextLong(): Long = -1L
        }
        val generator = UlidGenerator(clock = { 5_000L }, random = allOnes)
        val a = generator.next()
        assertTrue(a.endsWith("ZZZZZZZZZZZZZZZZ"))
        val b = generator.next()
        assertTrue(a < b)
        assertEquals(5_001L, Keys.timestampOf(b))
    }

    @Test
    fun validation() {
        assertTrue(Keys.isValid("01ARZ3NDEKTSV4RRFFQ69G5FAV"))
        assertFalse(Keys.isValid("01ARZ3NDEKTSV4RRFFQ69G5FA"))
        assertFalse(Keys.isValid("01ARZ3NDEKTSV4RRFFQ69G5FAI"))
        assertFalse(Keys.isValid("01ARZ3NDEKTSV4RRFFQ69G5FAL"))
        assertFalse(Keys.isValid("01ARZ3NDEKTSV4RRFFQ69G5FAO"))
        assertFalse(Keys.isValid("01ARZ3NDEKTSV4RRFFQ69G5FAU"))
        assertFalse(Keys.isValid("01arz3ndektsv4rrffq69g5fav"))
        assertFalse(Keys.isValid("81ARZ3NDEKTSV4RRFFQ69G5FAV"))
        assertTrue(Keys.isValid("7ZZZZZZZZZZZZZZZZZZZZZZZZZ"))
    }
}
