package io.github.lenticualrstone.civilight.feature.recorder.audio

/**
 * The 20-bar waveform of a kept recording (LRC-14, boards `LrInbox` · `TwoLr`): 3 dp bars 2 apart in
 * a 24 dp row, heights from the audio envelope, the played part in `acc`, the rest `ink3`.
 * The envelope stored with the record ([ENVELOPE_SIZE] bytes, 0…255 loudness) is made from the
 * input levels sampled while recording.
 */
object Waveforms {
    const val BARS = 20
    const val ENVELOPE_SIZE = 64

    /** Smallest and largest bar height in dp (board: 5 … 22 in a 24 row). */
    const val MIN_DP = 4f
    const val MAX_DP = 22f

    /** Loudness samples (0…255 each) folded into [size] buckets by their maximum. */
    fun envelopeOf(samples: ByteArray, size: Int = ENVELOPE_SIZE): ByteArray {
        if (samples.isEmpty()) return ByteArray(0)
        val out = ByteArray(size)
        for (i in 0 until size) {
            val from = (i.toLong() * samples.size / size).toInt()
            val to = maxOf(from + 1, ((i + 1).toLong() * samples.size / size).toInt()).coerceAtMost(samples.size)
            var max = 0
            for (j in from until to) max = maxOf(max, samples[j].toInt() and 0xFF)
            out[i] = max.toByte()
        }
        return out
    }

    /** One input level 0…1 as an envelope byte. */
    fun byteOf(level: Float): Byte = (level.coerceIn(0f, 1f) * 255f + 0.5f).toInt().toByte()

    /**
     * [count] bar levels 0…1 from [envelope] (bucket maximum). Without an envelope (a recording
     * recovered after process death, a file from another device) the bars are flat at 0.3.
     */
    fun bars(envelope: ByteArray?, count: Int = BARS): List<Float> {
        if (envelope == null || envelope.isEmpty()) return List(count) { FLAT }
        val folded = envelopeOf(envelope, count)
        return folded.map { (it.toInt() and 0xFF) / 255f }
    }

    /** A bar level as its height in dp. */
    fun heightDp(level: Float): Float = MIN_DP + (MAX_DP - MIN_DP) * level.coerceIn(0f, 1f)

    /** How many bars count as played at [positionMs] of [durationMs]. */
    fun playedBars(positionMs: Long, durationMs: Long, count: Int = BARS): Int {
        if (durationMs <= 0 || positionMs <= 0) return 0
        return ((positionMs.coerceAtMost(durationMs) * count + durationMs / 2) / durationMs).toInt().coerceIn(0, count)
    }

    private const val FLAT = 0.3f
}
