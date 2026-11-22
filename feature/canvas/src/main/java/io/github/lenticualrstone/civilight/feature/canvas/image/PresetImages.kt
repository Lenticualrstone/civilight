package io.github.lenticualrstone.civilight.feature.canvas.image

import io.github.lenticualrstone.civilight.model.asset.ImageUse

/** A point of a shard polygon in fractions of the frame. */
data class ShardPoint(val x: Float, val y: Float)

/**
 * A bundled preset (CNV-26): stored by name (`preset:<id>`), never as a file. Colours as `0xRRGGBB`:
 * background `linear-gradient(165deg, c0, c1)`, shard layers s1 (.9), s2 (1.0), s3 (.9) of [shardSet].
 */
data class PresetImage(
    val id: String,
    val name: String,
    val c0: Long,
    val c1: Long,
    val s1: Long,
    val s2: Long,
    val s3: Long,
    val shardSet: Int,
) {
    val source: String get() = ImageUse.PRESET_PREFIX + id
    val shards: List<List<ShardPoint>> get() = PresetImages.SHARDS[shardSet]
}

/**
 * The eight presets, offline and in every slot (`scr_cards.py` `PRE`, `SHARDS`). Ids: `ash-dawn` from the
 * file format; the other seven are spec 50's [PROPOSED] ids (Q-IM-06), the same ones feature:cast reads.
 * They are file format: the app never removes a preset name.
 */
object PresetImages {
    /** Layer opacities of s1, s2, s3. */
    val LAYER_ALPHA: List<Float> = listOf(0.9f, 1f, 0.9f)

    /** The CSS gradient angle of the background. */
    const val GRADIENT_DEG = 165f

    private val IDS = listOf("ash-dawn", "lantern", "deep-sea", "mist-forest", "desert-ruin", "nebula", "red-citadel", "snowfield")

    private val COLORS: List<List<Long>> = listOf(
        listOf(0x2B2F3A, 0x6B6F7C, 0xA3A7B3, 0x4A4F5C, 0xD9C7A3),
        listOf(0x1D1A17, 0x4A3A28, 0xC8913F, 0x6E5236, 0x2C241C),
        listOf(0x06131F, 0x123B56, 0x2F6F8F, 0x0B2536, 0x7FB3C8),
        listOf(0x1E2A24, 0x4E6656, 0x8BA593, 0x2E3E34, 0xC9D4C3),
        listOf(0x3A2A1E, 0xA57A52, 0xD8B98C, 0x6B4A30, 0xF0DEC0),
        listOf(0x120F24, 0x3B2E5C, 0x7A64A8, 0x241B3D, 0xC7B6E6),
        listOf(0x240E0E, 0x6A2420, 0xA9483C, 0x3D1715, 0xE0A08A),
        listOf(0x8C98A4, 0xE4E9EE, 0xB7C2CC, 0x6E7D8A, 0xF6F8FA),
    )

    private fun poly(vararg xy: Int): List<ShardPoint> = xy.toList().chunked(2) { (x, y) -> ShardPoint(x / 100f, y / 100f) }

    /** Shard sets 0–3 (x y in % of the frame). */
    val SHARDS: List<List<List<ShardPoint>>> = listOf(
        listOf(poly(0, 58, 55, 30, 100, 46, 100, 72, 0, 86), poly(0, 74, 40, 56, 100, 68, 100, 100, 0, 100), poly(58, 8, 78, 0, 70, 44, 52, 52)),
        listOf(poly(0, 40, 100, 18, 100, 60, 0, 70), poly(0, 66, 70, 50, 100, 80, 100, 100, 0, 100), poly(12, 10, 30, 6, 26, 34, 8, 30)),
        listOf(poly(20, 0, 60, 0, 90, 100, 40, 100), poly(0, 60, 100, 42, 100, 100, 0, 100), poly(62, 20, 84, 16, 80, 40, 60, 42)),
        listOf(poly(0, 30, 45, 12, 100, 36, 100, 64, 50, 54, 0, 70), poly(0, 78, 100, 58, 100, 100, 0, 100), poly(40, 60, 56, 52, 60, 72, 44, 78)),
    )

    val all: List<PresetImage> = IDS.indices.map { i ->
        val c = COLORS[i]
        PresetImage(IDS[i], ImageCopy.PRESET_NAMES[i], c[0], c[1], c[2], c[3], c[4], shardSet = i % 4)
    }

    fun byId(id: String?): PresetImage? = all.firstOrNull { it.id == id }

    /** The preset a stored source names, or null for a blob or an unknown name. */
    fun ofSource(source: String?): PresetImage? =
        source?.takeIf { it.startsWith(ImageUse.PRESET_PREFIX) }?.let { byId(it.removePrefix(ImageUse.PRESET_PREFIX)) }
}
