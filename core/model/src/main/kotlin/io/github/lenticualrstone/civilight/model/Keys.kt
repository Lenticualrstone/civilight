package io.github.lenticualrstone.civilight.model

import java.security.SecureRandom

/**
 * Hidden permanent key of a stored item (§1.1): a 26-character ULID in Crockford base 32, upper case.
 * Compared as a plain string; ULIDs sort by creation time, which is only ever a last tie-break.
 */
typealias Key = String

/** Key generation and validation (§1.1). */
object Keys {
    const val LENGTH = 26
    internal const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    private val default = UlidGenerator()

    /** A new ULID from the process-wide generator (monotonic within one millisecond). */
    fun newKey(): Key = default.next()

    /** True for a 26-character upper-case Crockford ULID whose timestamp fits 48 bits. */
    fun isValid(key: String): Boolean =
        key.length == LENGTH && key[0] <= '7' && key.all { ALPHABET.indexOf(it) >= 0 }

    /** Milliseconds since the epoch encoded in a valid ULID. */
    fun timestampOf(key: Key): Long {
        require(isValid(key)) { "not a ULID: $key" }
        var ms = 0L
        for (i in 0 until 10) ms = (ms shl 5) or ALPHABET.indexOf(key[i]).toLong()
        return ms
    }
}

/**
 * ULID generator with an injectable clock and random source.
 *
 * Within one millisecond (or when the clock goes backwards) the 80-bit random part of the previous
 * key is incremented, so keys made by one generator are strictly increasing. When the random part
 * overflows, the timestamp is advanced by one millisecond.
 */
class UlidGenerator(
    private val clock: () -> Long = System::currentTimeMillis,
    private val random: java.util.Random = SecureRandom(),
) {
    private var lastMs = -1L
    private var hi = 0L // upper 16 bits of the random part
    private var lo = 0L // lower 64 bits of the random part

    @Synchronized
    fun next(): Key {
        val now = clock()
        require(now in 0..MAX_TIME) { "timestamp out of ULID range: $now" }
        if (now > lastMs) {
            lastMs = now
            hi = random.nextInt(1 shl 16).toLong()
            lo = random.nextLong()
        } else {
            lo += 1
            if (lo == 0L) {
                hi += 1
                if (hi >= (1L shl 16)) {
                    hi = 0
                    lastMs += 1
                    check(lastMs <= MAX_TIME) { "ULID time overflow" }
                }
            }
        }
        return encode(lastMs, hi, lo)
    }

    private fun encode(ms: Long, hi: Long, lo: Long): Key {
        val out = CharArray(Keys.LENGTH)
        var t = ms
        for (i in 9 downTo 0) {
            out[i] = Keys.ALPHABET[(t and 31).toInt()]
            t = t ushr 5
        }
        for (i in 0 until 16) {
            val bitFromTop = 5 * i // random bits 79..0, char i takes bits 79-5i .. 75-5i
            val shift = 75 - bitFromTop
            out[10 + i] = Keys.ALPHABET[random80Bits(hi, lo, shift)]
        }
        return String(out)
    }

    private fun random80Bits(hi: Long, lo: Long, shift: Int): Int {
        // 5 bits starting at bit `shift` (LSB = 0) of the 80-bit number hi:lo.
        var v = 0
        for (b in 4 downTo 0) {
            val pos = shift + b
            val bit = if (pos >= 64) (hi ushr (pos - 64)) and 1L else (lo ushr pos) and 1L
            v = (v shl 1) or bit.toInt()
        }
        return v
    }

    private companion object {
        const val MAX_TIME = (1L shl 48) - 1
    }
}
