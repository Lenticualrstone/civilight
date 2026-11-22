package io.github.lenticualrstone.civilight.designsystem.theme

import androidx.compose.ui.graphics.Color

/** Hex / polygon map terrain kinds (DS-57). */
enum class Terrain { DEEP, SEA, PLAIN, FOREST, HILL, MOUNT, PEAK, DESERT, SWAMP, SNOW }

/** DS-03 theme-independent colours (and the few with their own per-theme table). */
object FixedColors {
    val navyCore: Color = Color(0xFF031C35)
    val onImage: Color = Color(0xFFF4F4F6)
    val onImage2: Color = Rgba(244, 244, 246, 0.82).toColor()
    val onImageBadge: Color = Rgba(244, 244, 246, 0.75).toColor()
    val onImageR18: Color = Color(0xFFFF8A80)
    val imageShadeTop: Color = Rgba(8, 9, 12, 0.0).toColor()
    val imageShadeBottom: Color = Rgba(8, 9, 12, 0.86).toColor()
    val imageChipFill: Color = Rgba(10, 11, 14, 0.5).toColor()
    val cropOutline: Color = Rgba(255, 255, 255, 0.9).toColor()
    val cropGrid: Color = Rgba(255, 255, 255, 0.4).toColor()
    val cropDim: Color = Rgba(5, 6, 8, 0.55).toColor()

    /** Fraction of the card height covered by the bottom image shade (DS-51). */
    const val IMAGE_SHADE_FRACTION: Float = 0.62f

    private val mapArtDark = listOf(0x3B4A66, 0xA0674C, 0xD9A27F, 0xC98A62, 0x2B3242).map(::opaque)
    private val mapArtLight = listOf(0x9DB0CC, 0xD9A788, 0xEBC7AA, 0xC9A07E, 0xB8C3D6).map(::opaque)

    /** `map-art-1 … 5`; follows the theme. Index 4 (`map-art-5`) is the land base. */
    fun mapArt(theme: ResolvedTheme): List<Color> = if (theme == ResolvedTheme.DARK) mapArtDark else mapArtLight

    /** `ter-*` terrain colours, identical in both themes. */
    val terrain: Map<Terrain, Color> = mapOf(
        Terrain.DEEP to opaque(0x24394A),
        Terrain.SEA to opaque(0x33505F),
        Terrain.PLAIN to opaque(0x8E9A6A),
        Terrain.FOREST to opaque(0x5E7352),
        Terrain.HILL to opaque(0xA39466),
        Terrain.MOUNT to opaque(0x857E73),
        Terrain.PEAK to opaque(0xD3D7DB),
        Terrain.DESERT to opaque(0xC4AB7A),
        Terrain.SWAMP to opaque(0x66705A),
        Terrain.SNOW to opaque(0xC6CED4),
    )

    private val glowDark = listOf(0x3E6FA8, 0x8FB8E6, 0x1F5590, 0xB5D3F5, 0x5B8CC8, 0x2B4C7E, 0x9CC3EE).map(::opaque)
    private val glowLight = listOf(0x5B8CC8, 0x1F5590, 0x8FB8E6, 0x2B4C7E, 0xA9CCF2, 0x3E6FA8, 0x1F5590).map(::opaque)

    /** Recorder glow band colours, in band order (DS-61). */
    fun recorderGlow(theme: ResolvedTheme): List<Color> = if (theme == ResolvedTheme.DARK) glowDark else glowLight

    /** Holo silver gradient ends (CNV-30): dark silver, light pewter. */
    fun holoSilver(theme: ResolvedTheme): Pair<Color, Color> =
        if (theme == ResolvedTheme.DARK) opaque(0x8E949C) to opaque(0xF4F5F7) else opaque(0x33373E) to opaque(0x666C74)

    /** Holo reference silver stops of `.hfoil` (0 / 6 / 12 / 18 / 26 %). */
    val holoSilverStops: List<Color> = listOf(0x7E848C, 0xE9ECEF, 0xA3A9B1, 0xF6F7F9, 0x7E848C).map(::opaque)

    /** Holo iridescent tint stops (pink · sky · lilac at 50 %). */
    val holoTintStops: List<Color> = listOf(
        Rgba(255, 150, 200, 0.5).toColor(),
        Rgba(140, 210, 255, 0.5).toColor(),
        Rgba(190, 160, 255, 0.5).toColor(),
    )

    private fun opaque(rgb: Int): Color = Rgba.hex(rgb).toColor()
}
