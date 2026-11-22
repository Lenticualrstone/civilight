package io.github.lenticualrstone.civilight.designsystem.theme

import kotlin.math.roundToInt

/** A text size and line height in sp. */
data class SizeLh(val sizeSp: Float, val lineHeightSp: Float)

/**
 * DS-07 manuscript base size `S` (default 18 sp, `보통`). Only manuscript roles follow it:
 * `line`, `body`, `paren`, `field`, `recorderLive`. Interface roles follow the system font scale only.
 */
@JvmInline
value class TextScale(val manuscriptBaseSp: Float = DEFAULT_SP) {
    private val s: Float get() = manuscriptBaseSp

    /** `line` = `S / round(S × 31 / 18)`. */
    val line: SizeLh get() = SizeLh(s, (s * 31f / 18f).roundToInt().toFloat())

    /** `body` = `round(S × 16 / 18) / round(S × 28 / 18)`. */
    val body: SizeLh
        get() = SizeLh((s * 16f / 18f).roundToInt().toFloat(), (s * 28f / 18f).roundToInt().toFloat())

    /** `paren` size = `round(S × 14 / 18)`; its own line height scales the same way (20 at 보통). */
    val paren: SizeLh
        get() = SizeLh((s * 14f / 18f).roundToInt().toFloat(), (s * 20f / 18f).roundToInt().toFloat())

    /** `field` scales by `S / 18` (16 / 22 at 보통). */
    val field: SizeLh get() = SizeLh(16f * s / 18f, 22f * s / 18f)

    /** `recorderLive` scales by `S / 18` (25 / 40 at 보통). */
    val recorderLive: SizeLh get() = SizeLh(25f * s / 18f, 40f * s / 18f)

    companion object {
        const val DEFAULT_SP: Float = 18f

        val Default: TextScale = TextScale(DEFAULT_SP)

        /**
         * Engineering default steps of LIB-65 (`ED`: 16 / 17 / 18 / 20 / 22 sp); index 2 is `보통`.
         * The stored step and its names belong to LIB.
         */
        val StepsSp: List<Float> = listOf(16f, 17f, 18f, 20f, 22f)

        const val DEFAULT_STEP: Int = 2

        fun forStep(step: Int): TextScale = TextScale(StepsSp[step.coerceIn(0, StepsSp.lastIndex)])
    }
}
