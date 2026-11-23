package io.github.lenticualrstone.civilight.ui.gallery

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
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The eight bundled presets (CNV-26) as plain data, for screens outside `feature:canvas` that show a stored
 * `preset:<id>` source (the search's gallery thumbnails, GAL-04). Same ids, colours and shards as
 * `feature:canvas` `PresetImages` (`scr_cards.py` `PRE`, `SHARDS`): background `linear-gradient(165deg,
 * c0, c1)`, shard layers s1 (.9), s2 (1.0), s3 (.9) of shard set `index % 4`.
 */
object PresetPictures {
    val IDS: List<String> = listOf("ash-dawn", "lantern", "deep-sea", "mist-forest", "desert-ruin", "nebula", "red-citadel", "snowfield")

    /** `c0 · c1 · s1 · s2 · s3` per preset, as `0xRRGGBB`. */
    internal val COLORS: List<List<Long>> = listOf(
        listOf(0x2B2F3A, 0x6B6F7C, 0xA3A7B3, 0x4A4F5C, 0xD9C7A3),
        listOf(0x1D1A17, 0x4A3A28, 0xC8913F, 0x6E5236, 0x2C241C),
        listOf(0x06131F, 0x123B56, 0x2F6F8F, 0x0B2536, 0x7FB3C8),
        listOf(0x1E2A24, 0x4E6656, 0x8BA593, 0x2E3E34, 0xC9D4C3),
        listOf(0x3A2A1E, 0xA57A52, 0xD8B98C, 0x6B4A30, 0xF0DEC0),
        listOf(0x120F24, 0x3B2E5C, 0x7A64A8, 0x241B3D, 0xC7B6E6),
        listOf(0x240E0E, 0x6A2420, 0xA9483C, 0x3D1715, 0xE0A08A),
        listOf(0x8C98A4, 0xE4E9EE, 0xB7C2CC, 0x6E7D8A, 0xF6F8FA),
    )

    /** Layer opacities of s1, s2, s3. */
    internal val LAYER_ALPHA: List<Float> = listOf(0.9f, 1f, 0.9f)

    internal const val GRADIENT_DEG = 165.0

    private fun poly(vararg xy: Int): List<Pair<Float, Float>> = xy.toList().chunked(2) { (x, y) -> x / 100f to y / 100f }

    /** Shard sets 0–3 (x y in fractions of the frame). */
    internal val SHARDS: List<List<List<Pair<Float, Float>>>> = listOf(
        listOf(poly(0, 58, 55, 30, 100, 46, 100, 72, 0, 86), poly(0, 74, 40, 56, 100, 68, 100, 100, 0, 100), poly(58, 8, 78, 0, 70, 44, 52, 52)),
        listOf(poly(0, 40, 100, 18, 100, 60, 0, 70), poly(0, 66, 70, 50, 100, 80, 100, 100, 0, 100), poly(12, 10, 30, 6, 26, 34, 8, 30)),
        listOf(poly(20, 0, 60, 0, 90, 100, 40, 100), poly(0, 60, 100, 42, 100, 100, 0, 100), poly(62, 20, 84, 16, 80, 40, 60, 42)),
        listOf(poly(0, 30, 45, 12, 100, 36, 100, 64, 50, 54, 0, 70), poly(0, 78, 100, 58, 100, 100, 0, 100), poly(40, 60, 56, 52, 60, 72, 44, 78)),
    )

    /** Index of the preset a stored source names (`preset:deep-sea` → 2), or null for a blob or an unknown name. */
    fun indexOfSource(source: String?): Int? =
        source?.takeIf { it.startsWith(ImageUse.PRESET_PREFIX) }?.removePrefix(ImageUse.PRESET_PREFIX)?.let { id -> IDS.indexOf(id).takeIf { it >= 0 } }
}

/** What a stored image source draws as a small thumbnail: a bundled preset, a blob to decode, or nothing (`chip` fill). */
sealed interface ThumbSource {
    data class Preset(val index: Int) : ThumbSource
    data class Blob(val hash: String) : ThumbSource
    data object None : ThumbSource

    companion object {
        fun of(source: String?): ThumbSource = when {
            source.isNullOrEmpty() -> None
            source.startsWith(ImageUse.PRESET_PREFIX) -> PresetPictures.indexOfSource(source)?.let(::Preset) ?: None
            else -> Blob(source)
        }
    }
}

private fun rgb(value: Long): Color = Color(0xFF000000 or value)

/** Draws preset [index] into the scope's size: the 165° gradient (CSS gradient-line length) and three shards. */
fun DrawScope.drawPresetPicture(index: Int) {
    val c = PresetPictures.COLORS[index.mod(PresetPictures.COLORS.size)]
    val shards = PresetPictures.SHARDS[index.mod(PresetPictures.SHARDS.size)]
    val w = size.width
    val h = size.height
    val a = Math.toRadians(PresetPictures.GRADIENT_DEG)
    val dir = Offset(sin(a).toFloat(), -cos(a).toFloat())
    val half = ((abs(w * sin(a)) + abs(h * cos(a))) / 2).toFloat()
    val mid = Offset(w / 2, h / 2)
    drawRect(Brush.linearGradient(listOf(rgb(c[0]), rgb(c[1])), start = mid - dir * half, end = mid + dir * half))
    shards.forEachIndexed { i, points ->
        val path = Path().apply {
            points.forEachIndexed { j, (x, y) -> if (j == 0) moveTo(x * w, y * h) else lineTo(x * w, y * h) }
            close()
        }
        drawPath(path, rgb(c[2 + i]), alpha = PresetPictures.LAYER_ALPHA[i])
    }
}

/** Preset [index] filling its box (same in both themes, CNV-26). Decorative: no semantics. */
@Composable
fun PresetPicture(index: Int, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().clearAndSetSemantics { }) { drawPresetPicture(index) }
}
