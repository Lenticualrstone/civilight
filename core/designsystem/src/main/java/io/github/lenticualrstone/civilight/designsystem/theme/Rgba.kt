package io.github.lenticualrstone.civilight.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * A colour token exactly as the design system states it: 8-bit sRGB channels and a declared alpha.
 *
 * The rendered colour uses the alpha byte `floor(a × 255 + 0.5)` (spec "Units" convention).
 * Contrast checks composite with the declared [a], which is how the spec's contrast table (DS-04) was computed.
 */
data class Rgba(val r: Int, val g: Int, val b: Int, val a: Double = 1.0) {
    init {
        require(r in 0..255 && g in 0..255 && b in 0..255) { "channel out of range" }
        require(a in 0.0..1.0) { "alpha out of range" }
    }

    val alphaByte: Int get() = alphaByte(a)

    /** ARGB with the rounded alpha byte. */
    val argb: Int get() = (alphaByte shl 24) or (r shl 16) or (g shl 8) or b

    val rgbHex: Int get() = (r shl 16) or (g shl 8) or b

    fun toColor(): Color = Color(argb)

    /** This colour composited over an opaque [base] with the declared alpha; channels round half up. */
    fun over(base: Rgba): Rgba {
        if (a >= 1.0) return copy(a = 1.0)
        fun ch(f: Int, bg: Int) = floor(a * f + (1 - a) * bg + 0.5).toInt().coerceIn(0, 255)
        return Rgba(ch(r, base.r), ch(g, base.g), ch(b, base.b), 1.0)
    }

    companion object {
        fun hex(rgb: Int): Rgba = Rgba((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF)

        fun alphaByte(a: Double): Int = floor(a * 255 + 0.5).toInt()
    }
}

/** WCAG 2.x relative luminance of an opaque colour. */
fun relativeLuminance(c: Rgba): Double {
    fun lin(v: Int): Double {
        val s = v / 255.0
        return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * lin(c.r) + 0.7152 * lin(c.g) + 0.0722 * lin(c.b)
}

/**
 * WCAG contrast ratio. Translucent colours are first composited over [base]
 * (the theme `bg` for every pair in DS-04).
 */
fun contrast(fg: Rgba, bg: Rgba, base: Rgba = bg): Double {
    val back = bg.over(base)
    val front = fg.over(back)
    val l1 = relativeLuminance(front)
    val l2 = relativeLuminance(back)
    return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
}

/** Rounds to two decimals, as the DS-04 table prints ratios. */
fun round2(v: Double): Double = floor(v * 100 + 0.5) / 100

/** Rounds to one decimal (board `Accent` prints `7.0:1`). */
fun round1(v: Double): Double = floor(v * 10 + 0.5) / 10
