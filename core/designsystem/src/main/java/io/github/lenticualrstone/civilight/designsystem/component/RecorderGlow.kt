package io.github.lenticualrstone.civilight.designsystem.component

import android.os.Build
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import kotlin.math.floor

/** Recorder glow states (behaviour: Recorder; hidden entirely when `라이프 레코더 빛` is off, CNV-46). */
enum class GlowMode { RECORDING, PROCESSING, STILL }

/** Board band heights at a sample moment (dp). */
val GlowBaseHeights: List<Dp> = listOf(170.dp, 260.dp, 330.dp, 240.dp, 300.dp, 210.dp, 150.dp)

const val GLOW_MIN_SCALE: Float = 0.72f
const val GLOW_MAX_SCALE: Float = 1.08f
const val GLOW_PERIOD_SEC: Float = 1.6f
const val GLOW_PHASE_SEC: Float = 0.23f

/** CSS `ease-in-out`. */
private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/**
 * Scale of band [index] at [timeSec] for input [level] (0…1): a 1.6 s ease-in-out alternate oscillation,
 * phase-shifted 0.23 s per band, mixed half-and-half with the level, between 0.72 and 1.08 of the base.
 */
fun glowBandScale(index: Int, timeSec: Float, level: Float): Float {
    val cycle = (timeSec - GLOW_PHASE_SEC * index) / GLOW_PERIOD_SEC
    val whole = floor(cycle)
    val frac = cycle - whole
    val forward = Math.floorMod(whole.toLong(), 2L) == 0L
    val eased = EaseInOut.transform(if (forward) frac else 1f - frac)
    val mix = 0.5f * eased + 0.5f * level.coerceIn(0f, 1f)
    return GLOW_MIN_SCALE + (GLOW_MAX_SCALE - GLOW_MIN_SCALE) * mix
}

/**
 * DS-61 recorder glow band: seven vertical bands across the full width (+40 dp overflow each side, −60 dp
 * below the bottom), each a gradient from its colour (glow palette, in order) at the bottom to transparent
 * at the top, top corners 40; the group blurred 34 dp (API 31+) with saturation × 1.2, opacity 0.95.
 * Bands, never rings. [GlowMode.RECORDING]: heights follow [level]; [GlowMode.PROCESSING]: the group
 * sweeps ±90 dp (2.4 s alternate), heights frozen; [GlowMode.STILL] (reduced motion): static.
 */
@Composable
fun RecorderGlowBand(
    mode: GlowMode,
    modifier: Modifier = Modifier,
    level: () -> Float = { 0.5f },
    height: Dp = 360.dp,
) {
    val colors = CivilightTheme.colors
    val palette = FixedColors.recorderGlow(if (colors.isDark) ResolvedTheme.DARK else ResolvedTheme.LIGHT).map { saturate(it, 1.2f) }
    val transition = rememberInfiniteTransition(label = "glow")
    val clock = transition.animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "clock")
    val sweep = transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(2400, easing = EaseInOut), RepeatMode.Reverse), label = "sweep")
    val blur = CivilightTheme.blur.glow
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .clearAndSetSemantics { }
            .graphicsLayer {
                alpha = 0.95f
                if (Build.VERSION.SDK_INT >= 31) {
                    val r = blur.toPx()
                    renderEffect = BlurEffect(r, r, TileMode.Decal)
                }
            },
    ) {
        val overflow = 40.dp.toPx()
        val below = 60.dp.toPx()
        val radius = CornerRadius(40.dp.toPx())
        val shift = if (mode == GlowMode.PROCESSING) sweep.value * 90.dp.toPx() else 0f
        val bandW = (size.width + overflow * 2) / palette.size
        val time = clock.value
        val bottom = size.height + below
        palette.forEachIndexed { i, color ->
            val scale = if (mode == GlowMode.RECORDING) glowBandScale(i, time, level()) else 1f
            val top = bottom - GlowBaseHeights[i].toPx() * scale
            val left = -overflow + bandW * i + shift
            val path = Path().apply {
                addRoundRect(RoundRect(left, top, left + bandW, bottom, topLeftCornerRadius = radius, topRightCornerRadius = radius))
            }
            drawPath(path, Brush.verticalGradient(listOf(Color.Transparent, color), startY = top, endY = bottom))
        }
    }
}

private fun saturate(c: Color, s: Float): Color {
    val m = ColorMatrix().apply { setToSaturation(s) }.values
    val r = (m[0] * c.red + m[1] * c.green + m[2] * c.blue).coerceIn(0f, 1f)
    val g = (m[5] * c.red + m[6] * c.green + m[7] * c.blue).coerceIn(0f, 1f)
    val b = (m[10] * c.red + m[11] * c.green + m[12] * c.blue).coerceIn(0f, 1f)
    return Color(r, g, b, c.alpha)
}

/** DS-61 recording header mark: an 8 × 8 `acc` square turned 45° (a small diamond, not a circle). */
@Composable
fun RecordingMark(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(8.dp)
            .rotate(45f)
            .background(CivilightTheme.colors.acc),
    )
}
