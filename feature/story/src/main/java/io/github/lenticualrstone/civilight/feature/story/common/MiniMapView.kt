package io.github.lenticualrstone.civilight.feature.story.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.model.Key

/**
 * Draws a [MiniMap] (pins, routes, the dashed straight line) on a `tint` field with a `line` outline.
 * [onPin] makes pins tappable (STO-35 `고른 씬 → 핀 탭`); the touch radius is 22 dp (44 dp target).
 */
@Composable
fun MiniMapView(map: MiniMap, modifier: Modifier = Modifier, onPin: ((Key) -> Unit)? = null) {
    val colors = CivilightTheme.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = CivilightTheme.type.small.copy(color = colors.ink)
    val shape = RoundedCornerShape(12.dp)
    val b = map.bounds
    fun fit(w: Float, h: Float): Triple<Float, Float, Float> {
        val scale = minOf(w / b.width, h / b.height)
        val dx = (w - b.width * scale) / 2f - b.left * scale
        val dy = (h - b.height * scale) / 2f - b.top * scale
        return Triple(scale, dx, dy)
    }
    // The gesture block outlives recompositions (keyed on the map only): it calls the latest callback.
    val currentOnPin by rememberUpdatedState(onPin)
    val tap = if (onPin == null) Modifier else Modifier.pointerInput(map) {
        detectTapGestures { offset ->
            val (scale, dx, dy) = fit(size.width.toFloat(), size.height.toFloat())
            val at = MiniPoint((offset.x - dx) / scale, (offset.y - dy) / scale)
            MiniMaps.pinAt(map, at, 22.dp.toPx() / scale)?.let { pin -> currentOnPin?.invoke(pin.placeKey) }
        }
    }
    Box(modifier.clip(shape).background(colors.tint).insetOutline(colors.line, 1.dp, shape)) {
        Canvas(Modifier.fillMaxSize().then(tap)) {
            val (scale, dx, dy) = fit(size.width, size.height)
            fun at(p: MiniPoint) = Offset(p.x * scale + dx, p.y * scale + dy)
            map.lines.forEach { line ->
                val path = Path().apply {
                    line.points.forEachIndexed { i, p -> val o = at(p); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
                }
                drawPath(
                    path,
                    color = if (line.dashed) colors.ink3 else colors.ink2,
                    style = Stroke(width = 2.dp.toPx(), pathEffect = if (line.dashed) PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())) else null),
                )
            }
            map.pins.forEach { pin ->
                val o = at(pin.at)
                val color = if (pin.accent) colors.acc else colors.ink
                drawCircle(colors.bg, radius = 7.dp.toPx(), center = o)
                drawCircle(color, radius = 5.dp.toPx(), center = o)
                pin.label?.let { text ->
                    val layout = measurer.measure(text, labelStyle.copy(color = color))
                    drawText(layout, topLeft = Offset(o.x + 9.dp.toPx(), o.y - layout.size.height / 2f))
                }
            }
        }
    }
}
