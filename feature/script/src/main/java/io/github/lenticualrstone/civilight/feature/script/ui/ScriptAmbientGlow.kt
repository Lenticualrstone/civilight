package io.github.lenticualrstone.civilight.feature.script.ui

import android.os.Build
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * (0.7.0, Labs) Subtle undulating ambient light behind the script manuscript, inspired by Life Recorder's
 * `RecorderGlowBand`. Placed inside `GlassScaffold`'s content layer so `TopGlassBar`, the Docked Glass Arch
 * input bar, and floating popovers visibly frost-blur gently drifting translucent light.
 *
 * Respects `LocalMotion.current.loops`: static soft atmospheric gradient when reduced motion is enabled.
 */
@Composable
internal fun ScriptAmbientGlow(modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val motion = LocalMotion.current
    val palette = remember(colors.isDark) {
        FixedColors.recorderGlow(if (colors.isDark) ResolvedTheme.DARK else ResolvedTheme.LIGHT)
    }
    val timeMs = remember { mutableLongStateOf(0L) }
    if (motion.loops) {
        LaunchedEffect(Unit) {
            val base = timeMs.longValue
            val start = withInfiniteAnimationFrameMillis { it }
            while (true) {
                withInfiniteAnimationFrameMillis { now -> timeMs.longValue = base + (now - start) }
            }
        }
    }
    val baseAlpha = if (colors.isDark) 0.24f else 0.19f
    val blurDp = 52.dp
    Canvas(
        modifier
            .fillMaxSize()
            .clearAndSetSemantics { }
            .graphicsLayer {
                alpha = baseAlpha
                if (Build.VERSION.SDK_INT >= 31) {
                    val r = blurDp.toPx()
                    renderEffect = BlurEffect(r, r, TileMode.Mirror)
                }
            },
    ) {
        val t = timeMs.longValue / 1000f
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Four slow drifting atmospheric light pools across top, middle, and bottom dock zones.
        val c0 = palette.getOrElse(0) { colors.acc }
        val c1 = palette.getOrElse(2) { colors.acc }
        val c2 = palette.getOrElse(4) { colors.acc }
        val c3 = palette.getOrElse(5) { colors.acc }

        val p0 = Offset(
            x = w * (0.22f + 0.14f * sin(t * 0.23f)),
            y = h * (0.14f + 0.06f * cos(t * 0.19f)),
        )
        val p1 = Offset(
            x = w * (0.82f + 0.12f * cos(t * 0.17f)),
            y = h * (0.36f + 0.08f * sin(t * 0.21f)),
        )
        val p2 = Offset(
            x = w * (0.30f + 0.16f * cos(t * 0.15f + 1.4f)),
            y = h * (0.78f + 0.07f * sin(t * 0.24f)),
        )
        val p3 = Offset(
            x = w * (0.76f + 0.14f * sin(t * 0.20f + 2.2f)),
            y = h * (0.92f + 0.05f * cos(t * 0.18f)),
        )

        val rLarge = maxOf(w, h) * 0.52f
        val rMed = maxOf(w, h) * 0.44f

        drawCircle(
            brush = Brush.radialGradient(listOf(c0, Color.Transparent), center = p0, radius = rMed),
            radius = rMed,
            center = p0,
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(c1, Color.Transparent), center = p1, radius = rLarge),
            radius = rLarge,
            center = p1,
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(c2, Color.Transparent), center = p2, radius = rLarge),
            radius = rLarge,
            center = p2,
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(c3, Color.Transparent), center = p3, radius = rMed),
            radius = rMed,
            center = p3,
        )
    }
}
