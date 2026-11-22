package io.github.lenticualrstone.civilight.feature.map.canvas

import io.github.lenticualrstone.civilight.model.map.Terrain
import io.github.lenticualrstone.civilight.designsystem.theme.Terrain as DsTerrain

/**
 * Colour choices for map drawing that do not need a theme: region colour tokens and the terrain
 * mapping onto the design system's `ter-*` table (DS-57).
 */
object MapColors {
    /** Region colour tokens offered by `색` (`map-art-1 … 5`, DS-57). */
    val REGION_TOKENS: List<String> = (1..5).map { "map-art-$it" }

    /**
     * Index into `FixedColors.mapArt` for a region colour: `map-art-N` → N − 1; any other token is
     * spread over the palette by its text so it keeps one colour; null for a `#RRGGBB` colour.
     */
    fun artIndex(token: String): Int? {
        if (parseHex(token) != null) return null
        val n = token.removePrefix("map-art-").toIntOrNull()
        if (n != null && n in 1..5) return n - 1
        return Math.floorMod(token.hashCode(), 5)
    }

    /** `#RRGGBB` → 0xFFRRGGBB, else null. */
    fun parseHex(token: String): Long? {
        val t = token.trim()
        if (!t.startsWith("#") || t.length != 7) return null
        return t.substring(1).toLongOrNull(16)?.let { 0xFF000000L or it }
    }

    fun dsTerrain(t: Terrain): DsTerrain = when (t) {
        Terrain.DEEP -> DsTerrain.DEEP
        Terrain.SEA -> DsTerrain.SEA
        Terrain.PLAIN -> DsTerrain.PLAIN
        Terrain.FOREST -> DsTerrain.FOREST
        Terrain.HILL -> DsTerrain.HILL
        Terrain.MOUNTAIN -> DsTerrain.MOUNT
        Terrain.PEAK -> DsTerrain.PEAK
        Terrain.DESERT -> DsTerrain.DESERT
        Terrain.SWAMP -> DsTerrain.SWAMP
        Terrain.SNOW -> DsTerrain.SNOW
    }
}
