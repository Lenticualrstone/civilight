package io.github.lenticualrstone.civilight.feature.cast.image

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import kotlin.math.cos
import kotlin.math.sin

/**
 * A built-in preset drawn as angular shards (CNV-26: no figures, no circles): a 165° gradient and
 * three shard polygons, colours and shapes as the board generator draws them (`scr_cards.py`
 * `PRE`, `SHARDS`). The image area owns presets; this copy lets the cast screens show them until a
 * shared one exists (see Requests).
 */
class PresetPainter(index: Int) : Painter() {
    private val palette = PALETTES[index.mod(PALETTES.size)]
    private val shards = SHARDS[index.mod(SHARDS.size)]

    override val intrinsicSize: Size get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        val w = size.width
        val h = size.height
        val angle = Math.toRadians(165.0 - 90.0)
        val dx = (cos(angle) * w / 2).toFloat()
        val dy = (sin(angle) * h / 2).toFloat()
        val c = Offset(w / 2, h / 2)
        drawRect(Brush.linearGradient(listOf(palette[0], palette[1]), start = c - Offset(dx, dy), end = c + Offset(dx, dy)))
        shards.forEachIndexed { i, poly ->
            val path = Path().apply {
                poly.forEachIndexed { j, (x, y) -> if (j == 0) moveTo(x * w, y * h) else lineTo(x * w, y * h) }
                close()
            }
            drawPath(path, palette[2 + i], alpha = if (i == 1) 1f else 0.9f)
        }
    }

    private companion object {
        fun hex(v: Long) = Color(0xFF000000 or v)

        val PALETTES = listOf(
            listOf(hex(0x2B2F3A), hex(0x6B6F7C), hex(0xA3A7B3), hex(0x4A4F5C), hex(0xD9C7A3)),
            listOf(hex(0x1D1A17), hex(0x4A3A28), hex(0xC8913F), hex(0x6E5236), hex(0x2C241C)),
            listOf(hex(0x06131F), hex(0x123B56), hex(0x2F6F8F), hex(0x0B2536), hex(0x7FB3C8)),
            listOf(hex(0x1E2A24), hex(0x4E6656), hex(0x8BA593), hex(0x2E3E34), hex(0xC9D4C3)),
            listOf(hex(0x3A2A1E), hex(0xA57A52), hex(0xD8B98C), hex(0x6B4A30), hex(0xF0DEC0)),
            listOf(hex(0x120F24), hex(0x3B2E5C), hex(0x7A64A8), hex(0x241B3D), hex(0xC7B6E6)),
            listOf(hex(0x240E0E), hex(0x6A2420), hex(0xA9483C), hex(0x3D1715), hex(0xE0A08A)),
            listOf(hex(0x8C98A4), hex(0xE4E9EE), hex(0xB7C2CC), hex(0x6E7D8A), hex(0xF6F8FA)),
        )

        /** Shard polygons in fractions of the frame, per preset `index % 4`. */
        val SHARDS: List<List<List<Pair<Float, Float>>>> = listOf(
            listOf(
                listOf(0f to .58f, .55f to .30f, 1f to .46f, 1f to .72f, 0f to .86f),
                listOf(0f to .74f, .40f to .56f, 1f to .68f, 1f to 1f, 0f to 1f),
                listOf(.58f to .08f, .78f to 0f, .70f to .44f, .52f to .52f),
            ),
            listOf(
                listOf(0f to .40f, 1f to .18f, 1f to .60f, 0f to .70f),
                listOf(0f to .66f, .70f to .50f, 1f to .80f, 1f to 1f, 0f to 1f),
                listOf(.12f to .10f, .30f to .06f, .26f to .34f, .08f to .30f),
            ),
            listOf(
                listOf(.20f to 0f, .60f to 0f, .90f to 1f, .40f to 1f),
                listOf(0f to .60f, 1f to .42f, 1f to 1f, 0f to 1f),
                listOf(.62f to .20f, .84f to .16f, .80f to .40f, .60f to .42f),
            ),
            listOf(
                listOf(0f to .30f, .45f to .12f, 1f to .36f, 1f to .64f, .50f to .54f, 0f to .70f),
                listOf(0f to .78f, 1f to .58f, 1f to 1f, 0f to 1f),
                listOf(.40f to .60f, .56f to .52f, .60f to .72f, .44f to .78f),
            ),
        )
    }
}
