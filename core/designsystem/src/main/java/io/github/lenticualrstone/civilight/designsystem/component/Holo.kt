package io.github.lenticualrstone.civilight.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Holo tint strengths confirmed for CNV-30: 45 % dark, 32 % light, 50 % on card foils. */
object HoloStrength {
    const val DARK: Float = 0.45f
    const val LIGHT: Float = 0.32f
    const val CARD_FOIL: Float = 0.50f

    fun of(theme: ResolvedTheme): Float = if (theme == ResolvedTheme.DARK) DARK else LIGHT
}

/** Static tilt used when motion is reduced (`--tx .35`, `--ty −.2`). */
val HoloDefaultTilt: Offset = Offset(0.35f, -0.2f)

/**
 * DS-54 silver holo material, implemented once: a silver gradient (115°) and a faint iridescent tint
 * (pink · sky · lilac) in overlay blend, plus a narrow white sheen band (≈ 24 % wide, 105°); all shift with
 * [tilt] (each axis −1…1): tint by 50 %, silver by 30 % (parallax). Exact feel and inputs are CNV-30.
 * Never on backgrounds behind text.
 */
fun Modifier.holoFoil(
    tilt: Offset,
    theme: ResolvedTheme,
    strength: Float = HoloStrength.of(theme),
): Modifier = holoFoil({ tilt }, theme, { strength })

/**
 * [holoFoil] with [tilt] and [strength] read in the draw phase: a moving tilt (sensor, drag, sweep) only
 * redraws the layer and never recomposes its owner.
 *
 * [maskToContent]: the foil keeps only where the content drew (its alpha). An image fitted into a larger box
 * (`보기`, the crop stage) otherwise gets the foil painted over the empty letterbox too, because overlay and
 * the sheen on a transparent backdrop draw their own colour. Off by default: surfaces that put the foil on
 * a background of their own (the holo experiment's chips, fills and switch) keep it there.
 * [fade] (0…1) scales every layer, for a foil that fades in and out as a whole; [strength] is the tint's own.
 */
fun Modifier.holoFoil(
    tilt: () -> Offset,
    theme: ResolvedTheme,
    strength: () -> Float = { HoloStrength.of(theme) },
    maskToContent: Boolean = false,
    fade: () -> Float = { 1f },
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val maskPaint = Paint().apply { blendMode = BlendMode.DstIn }
        val bounds = Rect(Offset.Zero, size)
        onDrawWithContent {
            drawContent()
            drawHoloLayers(tilt(), theme, strength(), fade().coerceIn(0f, 1f))
            if (maskToContent) {
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(bounds, maskPaint)
                    drawContent()
                    canvas.restore()
                }
            }
        }
    }

/** The three foil layers over what is drawn already (overlay silver and tint, then the sheen). */
private fun DrawScope.drawHoloLayers(tilt: Offset, theme: ResolvedTheme, strength: Float, fade: Float) {
    if (fade <= 0f) return
    val diag = max(size.width, size.height)
    fun band(angleDeg: Float, shift: Float): Pair<Offset, Offset> {
        val a = Math.toRadians(angleDeg.toDouble() - 90.0)
        val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
        val center = Offset(size.width / 2, size.height / 2) + dir * (shift * diag)
        return (center - dir * diag) to (center + dir * diag)
    }
    val (silverA, silverB) = FixedColors.holoSilver(theme)
    val (s0, s1) = band(115f, tilt.x * 0.3f + tilt.y * 0.15f)
    drawRect(
        Brush.linearGradient(
            0f to silverA, 0.23f to silverB, 0.46f to silverA, 0.69f to silverB, 1f to silverA,
            start = s0, end = s1,
        ),
        alpha = 0.55f * fade,
        blendMode = BlendMode.Overlay,
    )
    val tint = FixedColors.holoTintStops
    val (t0, t1) = band(115f, tilt.x * 0.5f + tilt.y * 0.25f)
    drawRect(
        Brush.linearGradient(
            0f to tint[0], 0.17f to tint[1], 0.33f to tint[2], 0.5f to tint[0], 0.67f to tint[1], 0.83f to tint[2], 1f to tint[0],
            start = t0, end = t1,
        ),
        alpha = (strength * fade).coerceIn(0f, 1f),
        blendMode = BlendMode.Overlay,
    )
    val (h0, h1) = band(105f, tilt.x * 0.5f)
    drawRect(
        Brush.linearGradient(
            // The gradient spans 2 × diag, so 0.44…0.56 is a band ≈ 24 % of the diagonal wide.
            0f to Color.Transparent, 0.44f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.35f), 0.56f to Color.Transparent, 1f to Color.Transparent,
            start = h0, end = h1,
        ),
        alpha = fade,
    )
}
