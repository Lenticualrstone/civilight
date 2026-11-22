package io.github.lenticualrstone.civilight.feature.canvas.image

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private fun rgb(value: Long): Color = Color(0xFF000000 or value)

/** Draws [preset] into the scope's size: the 165° gradient (CSS gradient-line length) and three shards. */
fun DrawScope.drawPreset(preset: PresetImage) {
    val w = size.width
    val h = size.height
    val a = Math.toRadians(PresetImages.GRADIENT_DEG.toDouble())
    val dir = Offset(sin(a).toFloat(), -cos(a).toFloat())
    val half = ((abs(w * sin(a)) + abs(h * cos(a))) / 2).toFloat()
    val c = Offset(w / 2, h / 2)
    drawRect(Brush.linearGradient(listOf(rgb(preset.c0), rgb(preset.c1)), start = c - dir * half, end = c + dir * half))
    val colors = listOf(preset.s1, preset.s2, preset.s3)
    preset.shards.forEachIndexed { i, points ->
        val path = Path().apply {
            points.forEachIndexed { j, p -> if (j == 0) moveTo(p.x * w, p.y * h) else lineTo(p.x * w, p.y * h) }
            close()
        }
        drawPath(path, rgb(colors[i]), alpha = PresetImages.LAYER_ALPHA[i])
    }
}

/** A preset image filling its box (same in both themes, CNV-26). Decorative: no semantics. */
@Composable
fun PresetArt(preset: PresetImage, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().clearAndSetSemantics { }) { drawPreset(preset) }
}
