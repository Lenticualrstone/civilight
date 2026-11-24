package io.github.lenticualrstone.civilight.feature.cast.list.cards3d

import android.graphics.BlurMaskFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/** One CSS `box-shadow` layer in board px: offset ([dx], [dy]), [blur], [spread], [color]. */
internal data class ShadowLayer(val dx: Float, val dy: Float, val blur: Float, val spread: Float, val color: Color)

/** Below this the shadow bitmap is drawn: a blur hides the low resolution, and the copy stays small. */
private const val SHADOW_RES = 0.25f

/**
 * A card's CSS drop shadow ([layers], board px × [unit] px each) behind a card of corner [radius] board px.
 * Blurred once per size into a quarter-resolution bitmap (software `BlurMaskFilter`), then each frame only
 * draws that bitmap, so a moving card costs one image draw. Put it before the card's clip.
 */
internal fun Modifier.cardShadow(layers: List<ShadowLayer>, radius: Float, unit: Float, alpha: () -> Float = { 1f }): Modifier = drawWithCache {
    var pad = 0f
    for (l in layers) pad = max(pad, (l.blur + abs(l.spread) + max(abs(l.dx), abs(l.dy))) * unit)
    val bw = ((size.width + pad * 2) * SHADOW_RES).roundToInt().coerceAtLeast(1)
    val bh = ((size.height + pad * 2) * SHADOW_RES).roundToInt().coerceAtLeast(1)
    val bitmap = android.graphics.Bitmap.createBitmap(bw, bh, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    for (l in layers) {
        // CSS blur B is a Gaussian of σ = B / 2; Skia's blur radius r gives σ = 0.57735 r + 0.5.
        val sigma = l.blur * unit * SHADOW_RES / 2f
        val r = ((sigma - 0.5f) / 0.57735f).coerceAtLeast(0.5f)
        paint.maskFilter = if (sigma > 0.5f) BlurMaskFilter(r, BlurMaskFilter.Blur.NORMAL) else null
        paint.color = l.color.toArgb()
        val s = l.spread * unit
        val left = (pad - s + l.dx * unit) * SHADOW_RES
        val top = (pad - s + l.dy * unit) * SHADOW_RES
        val right = (pad + size.width + s + l.dx * unit) * SHADOW_RES
        val bottom = (pad + size.height + s + l.dy * unit) * SHADOW_RES
        val cr = max(0f, radius * unit + s) * SHADOW_RES
        canvas.drawRoundRect(left, top, right, bottom, cr, cr, paint)
    }
    val image = bitmap.asImageBitmap()
    val dstSize = IntSize(ceil(size.width + pad * 2).toInt(), ceil(size.height + pad * 2).toInt())
    val dstOffset = IntOffset(-pad.roundToInt(), -pad.roundToInt())
    onDrawBehind {
        val a = alpha()
        if (a <= 0.01f) return@onDrawBehind
        drawImage(image, srcSize = IntSize(bw, bh), dstOffset = dstOffset, dstSize = dstSize, alpha = a.coerceAtMost(1f), filterQuality = FilterQuality.Low)
    }
}

/** [cardShadow] kept across recompositions (equal layers keep the element, so the blur is not redone). */
@Composable
internal fun rememberCardShadow(layers: List<ShadowLayer>, radius: Float, unit: Float): Modifier =
    remember(layers, radius, unit) { if (layers.isEmpty()) Modifier else Modifier.cardShadow(layers, radius, unit) }

/**
 * A CSS light band (`.sh`): a `linear-gradient(105deg, …)` over a box grown by [grow] (left, top, right,
 * bottom as fractions of the card, board `inset: −20% −60% …`), moved each frame by [shift] (fractions of the
 * grown box, as the boards' `translate(…%)`). The gradient is built once per size.
 */
internal class SheenSpec(
    val grow: FloatArray,
    val stops: Array<Pair<Float, Color>>,
    val angleDeg: Float = 105f,
)

internal fun Modifier.sheen(spec: SheenSpec, shift: () -> Offset): Modifier = drawWithCache {
    val ew = size.width * (1 + spec.grow[0] + spec.grow[2])
    val eh = size.height * (1 + spec.grow[1] + spec.grow[3])
    val left = -size.width * spec.grow[0]
    val top = -size.height * spec.grow[1]
    // CSS gradient line: through the box centre at the angle, long enough to reach the far corners.
    val a = spec.angleDeg * PI / 180
    val dx = sin(a).toFloat()
    val dy = -cos(a).toFloat()
    val len = abs(ew * dx) + abs(eh * dy)
    val cx = left + ew / 2
    val cy = top + eh / 2
    val brush = Brush.linearGradient(
        *spec.stops,
        start = Offset(cx - dx * len / 2, cy - dy * len / 2),
        end = Offset(cx + dx * len / 2, cy + dy * len / 2),
    )
    val boxTopLeft = Offset(left, top)
    val boxSize = Size(ew, eh)
    onDrawWithContent {
        drawContent()
        val s = shift()
        translate(s.x * ew, s.y * eh) { drawRect(brush, topLeft = boxTopLeft, size = boxSize) }
    }
}

/** Board sheens. */
internal object Sheens {
    /** cards-a `.sh`: `inset −20% / −60%, width 220%`, stops 40 · 49 · 53 · 60 %. */
    fun deck(sheen: Color) = SheenSpec(
        floatArrayOf(0.6f, 0.2f, 0.6f, 0.2f),
        arrayOf(0.40f to Color.Transparent, 0.49f to sheen, 0.53f to Color.White.copy(alpha = 0.06f), 0.60f to Color.Transparent),
    )

    /** cards-b `.sh`: `inset −40%`, stops 42 · 49 (.17) · 53 (.05) · 58 %. */
    val pile = SheenSpec(
        floatArrayOf(0.4f, 0.4f, 0.4f, 0.4f),
        arrayOf(0.42f to Color.Transparent, 0.49f to Color.White.copy(alpha = 0.17f), 0.53f to Color.White.copy(alpha = 0.05f), 0.58f to Color.Transparent),
    )

    /** cards-c `.sh`: `inset −30% −70%`, stops 41 · 50 (.26) · 59 %. */
    val wall = SheenSpec(
        floatArrayOf(0.7f, 0.3f, 0.7f, 0.3f),
        arrayOf(0.41f to Color.Transparent, 0.50f to Color.White.copy(alpha = 0.26f), 0.59f to Color.Transparent),
    )
}
