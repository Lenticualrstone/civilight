package io.github.lenticualrstone.civilight.domain.world.gen

import kotlin.math.floor

/**
 * The generator's own random numbers (SplitMix64). Hand-written so a stored generator version keeps
 * giving the same numbers on every JVM and Android version.
 */
internal class Rng(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += GOLDEN
        return Hash.mix(state)
    }

    /** Uniform in [0, 1). */
    fun nextDouble(): Double = (nextLong() ushr 11).toDouble() * UNIT

    /** Uniform in [0, bound). */
    fun nextInt(bound: Int): Int {
        require(bound > 0)
        return ((nextLong() ushr 1) % bound).toInt()
    }

    /** Fisher–Yates shuffle (a new list). */
    fun <T> shuffled(items: List<T>): List<T> {
        val out = items.toMutableList()
        for (i in out.size - 1 downTo 1) {
            val j = nextInt(i + 1)
            val t = out[i]
            out[i] = out[j]
            out[j] = t
        }
        return out
    }

    companion object {
        val GOLDEN: Long = 0x9E3779B97F4A7C15uL.toLong()
        const val UNIT: Double = 1.0 / (1L shl 53)
    }
}

/** Integer hashing for lattice noise and per-step seed streams. */
internal object Hash {
    private val M1: Long = 0xBF58476D1CE4E5B9uL.toLong()
    private val M2: Long = 0x94D049BB133111EBuL.toLong()

    /** The SplitMix64 finaliser. */
    fun mix(value: Long): Long {
        var z = value
        z = (z xor (z ushr 30)) * M1
        z = (z xor (z ushr 27)) * M2
        return z xor (z ushr 31)
    }

    fun combine(seed: Long, a: Int, b: Int): Long = mix(mix(seed + Rng.GOLDEN * (a.toLong() + 1)) + b.toLong() * M1)

    /** Uniform in [0, 1) for a lattice point. */
    fun unit(seed: Long, x: Int, y: Int): Double = (combine(seed, x, y) ushr 11).toDouble() * Rng.UNIT

    /** The seed of a named stream of a step seed (so two uses of one seed never correlate). */
    fun stream(seed: Int, stream: Int): Long = mix(seed.toLong() * 0x100000001L + stream)
}

/**
 * Value noise with octaves (the reference algorithm of WLD-53): frequencies 3, 6, 12, 24 weighted
 * .5, .27, .15, .08 over [0,1]², smoothstep interpolation of hashed lattice values.
 */
internal object ValueNoise {
    private val FREQUENCIES = intArrayOf(3, 6, 12, 24)
    private val WEIGHTS = doubleArrayOf(0.5, 0.27, 0.15, 0.08)

    private fun smooth(t: Double): Double = t * t * (3 - 2 * t)

    fun value(seed: Long, x: Double, y: Double, frequency: Int): Double {
        val fx = x * frequency
        val fy = y * frequency
        val ix = floor(fx).toInt()
        val iy = floor(fy).toInt()
        val tx = smooth(fx - ix)
        val ty = smooth(fy - iy)
        val a = Hash.unit(seed, ix, iy)
        val b = Hash.unit(seed, ix + 1, iy)
        val c = Hash.unit(seed, ix, iy + 1)
        val d = Hash.unit(seed, ix + 1, iy + 1)
        val top = a + (b - a) * tx
        val bottom = c + (d - c) * tx
        return top + (bottom - top) * ty
    }

    /** Octave sum in [0, 1); [scale] multiplies every frequency (tiled noise for islands). */
    fun fbm(seed: Long, x: Double, y: Double, scale: Int = 1): Double {
        var sum = 0.0
        var weight = 0.0
        for (o in FREQUENCIES.indices) {
            sum += WEIGHTS[o] * value(Hash.mix(seed + o), x, y, FREQUENCIES[o] * scale)
            weight += WEIGHTS[o]
        }
        return sum / weight
    }
}
