package io.github.lenticualrstone.civilight.feature.map.canvas

import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.domain.world.gen.GenPoint
import io.github.lenticualrstone.civilight.domain.world.gen.GeneratedWorld
import io.github.lenticualrstone.civilight.model.map.MapStyle
import io.github.lenticualrstone.civilight.model.map.PolyMap
import io.github.lenticualrstone.civilight.model.map.Terrain
import androidx.compose.ui.graphics.toArgb

/**
 * The theme colours a generated base is drawn with, as ARGB ints, so the same pure code serves
 * the map canvas and the export (WLD-57). [art] is `map-art-1 … 5` of the current theme.
 */
data class BasePalette(
    val dark: Boolean,
    val art: List<Int>,
    val bg: Int,
    val tint: Int,
    val ink: Int,
    val ink2: Int,
    val ink3: Int,
)

/** A picture of [w] × [h] ARGB pixels (row-major), drawn scaled and smoothed over the base. */
class Raster(val w: Int, val h: Int, val pixels: IntArray)

/** A line drawn over a generated base: 0–1 points, colour, width in dp, dashed or not. */
data class BaseLine(val points: List<GenPoint>, val color: Int, val widthDp: Float, val closed: Boolean = false, val dashed: Boolean = false)

/**
 * Generated-map pictures (WLD-57): `색면` (soft province fields in the theme's `map-art` colours,
 * clipped to the coastline, no blue on land, faint lines on top), `먹선` (ink lines on paper), `색`
 * (the fixed `ter-*` colours) and `양피지` (the `ter-*` colours toned sepia), plus the polygon cells
 * of a POLY map and `등고선` lines. Pure: ARGB ints only.
 */
object GenRaster {
    /** `ter-*` as ARGB. */
    fun terrainArgb(t: Terrain): Int = FixedColors.terrain.getValue(MapColors.dsTerrain(t)).toArgb()

    fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(shift: Int) = (((a ushr shift) and 0xFF) * (1 - t) + ((b ushr shift) and 0xFF) * t).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    fun withAlpha(c: Int, alpha: Float): Int = ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24) or (c and 0xFFFFFF)

    /** Sepia tone of a colour (`양피지`). */
    fun sepia(c: Int): Int {
        val r = (c ushr 16) and 0xFF
        val g = (c ushr 8) and 0xFF
        val b = c and 0xFF
        val sr = (0.393 * r + 0.769 * g + 0.189 * b).toInt().coerceAtMost(255)
        val sg = (0.349 * r + 0.686 * g + 0.168 * b).toInt().coerceAtMost(255)
        val sb = (0.272 * r + 0.534 * g + 0.131 * b).toInt().coerceAtMost(255)
        return (0xFF shl 24) or (sr shl 16) or (sg shl 8) or sb
    }

    /** Land colours of `색면`: the warm `map-art-2 … 4` only (no blue hues on land). */
    fun softLand(p: BasePalette): List<Int> = p.art.subList(1, 4)

    fun softSea(p: BasePalette, deep: Boolean): Int = mix(p.bg, if (deep) p.art[4] else p.art[0], if (deep) 0.55f else 0.45f)

    /**
     * The province field of each land cell for `색면`: the nearest town (no town: thirds of the
     * map), so fields follow the settled land and stay stable for one world.
     */
    fun fields(world: GeneratedWorld): IntArray = IntArray(world.w * world.h) { i ->
        val x = (i % world.w + 0.5) / world.w
        val y = (i / world.w + 0.5) / world.h
        if (world.towns.isEmpty()) {
            (x * 3).toInt().coerceIn(0, 2) + (y * 3).toInt().coerceIn(0, 2)
        } else {
            world.towns.indices.minByOrNull { k -> val dx = world.towns[k].x - x; val dy = world.towns[k].y - y; dx * dx + dy * dy }!!
        }
    }

    /** The world picture at cell resolution in [style] (`색면` is blurred inside the land, the sea stays crisp). */
    fun world(world: GeneratedWorld, style: MapStyle, p: BasePalette): Raster {
        val n = world.w * world.h
        val px = when (style) {
            MapStyle.COLOR -> IntArray(n) { terrainArgb(world.terrain(it)) }
            MapStyle.PARCHMENT -> IntArray(n) { sepia(terrainArgb(world.terrain(it))) }
            MapStyle.INK -> IntArray(n) { if (world.isLand(it)) mix(p.bg, p.ink, 0.06f) else p.bg }
            MapStyle.SOFT -> {
                val land = softLand(p)
                val field = fields(world)
                val raw = IntArray(n) { i ->
                    if (world.isLand(i)) land[field[i] % land.size] else softSea(p, world.terrain(i) == Terrain.DEEP)
                }
                blurLand(world, raw)
            }
        }
        return Raster(world.w, world.h, px)
    }

    /** A 3 × 3 box blur over land cells, averaging land neighbours only (fields clipped to the coastline). */
    private fun blurLand(world: GeneratedWorld, src: IntArray): IntArray {
        val out = src.copyOf()
        val w = world.w
        val h = world.h
        for (i in src.indices) {
            if (!world.isLand(i)) continue
            var r = 0; var g = 0; var b = 0; var k = 0
            val c = i % w
            val row = i / w
            for (dy in -1..1) for (dx in -1..1) {
                val x = c + dx
                val y = row + dy
                if (x !in 0 until w || y !in 0 until h) continue
                val j = y * w + x
                if (!world.isLand(j)) continue
                r += (src[j] ushr 16) and 0xFF; g += (src[j] ushr 8) and 0xFF; b += src[j] and 0xFF; k++
            }
            out[i] = (0xFF shl 24) or ((r / k) shl 16) or ((g / k) shl 8) or (b / k)
        }
        return out
    }

    /**
     * Polygon cells (WLD-59) at [w] × [h]: each pixel takes its nearest cell; land cells show their
     * province colour (`색` token), unassigned land the `ter-*` terrain under the cell, sea the sea
     * colour of the style. Province borders are darker, cell borders inside a province faint.
     * [owner] receives the cell index per pixel when given.
     */
    fun poly(poly: PolyMap, world: GeneratedWorld?, style: MapStyle, p: BasePalette, w: Int = 160, h: Int = 120, owner: IntArray? = null): Raster {
        val cells = poly.cells
        val own = owner ?: IntArray(w * h)
        for (i in 0 until w * h) {
            val x = (i % w + 0.5f) / w
            val y = (i / w + 0.5f) / h
            var best = 0
            var bestD = Float.MAX_VALUE
            for (k in cells.indices) {
                val dx = cells[k].site.x - x
                val dy = cells[k].site.y - y
                val d = dx * dx + dy * dy
                if (d < bestD) { bestD = d; best = k }
            }
            own[i] = best
        }
        val province = poly.provinces.associateBy { it.key }
        val cellColor = IntArray(cells.size) { k ->
            val c = cells[k]
            when {
                !c.land -> if (style == MapStyle.SOFT) softSea(p, deep = false) else if (style == MapStyle.INK) p.bg else terrainArgb(Terrain.SEA)
                else -> {
                    val prov = poly.cellProvince[k]?.let(province::get)
                    when {
                        prov != null -> MapColors.tokenArgb(prov.color, p.art)
                        world != null -> terrainArgb(world.terrain(world.cellAt(c.site.x.toDouble(), c.site.y.toDouble())))
                        else -> p.art[1]
                    }
                }
            }.let { if (style == MapStyle.PARCHMENT) sepia(it) else it }
        }
        fun provinceOf(k: Int): String? = if (cells.getOrNull(k)?.land == true) poly.cellProvince[k] ?: "" else null
        val px = IntArray(w * h) { i ->
            val k = own[i]
            val base = if (cells.isEmpty()) p.tint else cellColor[k]
            val right = if (i % w < w - 1) own[i + 1] else k
            val down = if (i / w < h - 1) own[i + w] else k
            when {
                cells.isEmpty() -> base
                provinceOf(right) != provinceOf(k) || provinceOf(down) != provinceOf(k) -> mix(base, p.ink, 0.45f)
                right != k || down != k -> mix(base, p.ink, 0.10f)
                else -> base
            }
        }
        return Raster(w, h, px)
    }

    /** The nearest polygon cell to a point (tap targets of 다각형 편집). */
    fun cellAt(poly: PolyMap, x: Float, y: Float): Int? = poly.cells.indices.minByOrNull { k ->
        val dx = poly.cells[k].site.x - x
        val dy = poly.cells[k].site.y - y
        dx * dx + dy * dy
    }

    /**
     * Lines over a generated base in [style]: coastline (with a faint ripple in `색면`), ridges,
     * rivers and roads of the world unless the map keeps its own drawn lines ([worldLines] false,
     * detailed-terrain maps), and `등고선` when [contours].
     */
    fun lines(
        world: GeneratedWorld,
        style: MapStyle,
        p: BasePalette,
        contours: Boolean,
        worldLines: Boolean,
        hex: Boolean,
        rivers: Boolean = worldLines,
        roads: Boolean = worldLines,
    ): List<BaseLine> {
        val out = ArrayList<BaseLine>()
        val river = terrainArgb(Terrain.SEA)
        if (!hex) {
            when (style) {
                MapStyle.SOFT -> {
                    world.coastline.forEach { out += BaseLine(it, withAlpha(p.ink3, 0.18f), 5f, closed = true) }
                    world.coastline.forEach { out += BaseLine(it, withAlpha(p.ink3, 0.55f), 1f, closed = true) }
                    world.ridges.forEach { out += BaseLine(it, withAlpha(p.ink3, 0.35f), 1f) }
                }
                MapStyle.INK -> {
                    world.coastline.forEach { out += BaseLine(it, p.ink, 1.4f, closed = true) }
                    world.coastline.forEach { out += BaseLine(it, withAlpha(p.ink, 0.25f), 4f, closed = true) }
                    world.ridges.forEach { out += BaseLine(it, p.ink2, 1.2f) }
                }
                MapStyle.PARCHMENT -> {
                    world.coastline.forEach { out += BaseLine(it, sepia(p.ink2), 1.2f, closed = true) }
                    world.ridges.forEach { out += BaseLine(it, withAlpha(sepia(p.ink3), 0.6f), 1f) }
                }
                MapStyle.COLOR -> world.coastline.forEach { out += BaseLine(it, withAlpha(p.ink3, 0.6f), 1f, closed = true) }
            }
        }
        if (contours) contourSegments(world).forEach { out += BaseLine(it, withAlpha(p.ink3, 0.35f), 0.8f) }
        if (rivers || roads) {
            val riverColor = when (style) {
                MapStyle.SOFT -> withAlpha(p.art[0], 0.9f)
                MapStyle.INK -> p.ink2
                MapStyle.PARCHMENT -> sepia(river)
                MapStyle.COLOR -> river
            }
            val roadColor = when (style) {
                MapStyle.INK -> p.ink3
                MapStyle.PARCHMENT -> sepia(p.ink2)
                else -> withAlpha(p.ink2, 0.55f)
            }
            if (rivers) world.rivers.forEach { out += BaseLine(it, riverColor, 1.5f) }
            if (roads) world.roads.forEach { out += BaseLine(it, roadColor, 1f, dashed = style == MapStyle.INK || style == MapStyle.SOFT) }
        }
        return out
    }

    /** Height levels of `등고선`: five steps across the land heights (0–1000). */
    val CONTOUR_LEVELS = listOf(150, 300, 450, 600, 750)

    /**
     * `등고선` (WLD-57) as short segments: marching squares over the cell centres at
     * [CONTOUR_LEVELS], so each level is a closed run of segments around the higher ground.
     */
    fun contourSegments(world: GeneratedWorld): List<List<GenPoint>> {
        val w = world.w
        val h = world.h
        if (world.heights.size < w * h) return emptyList()
        fun hgt(c: Int, r: Int): Double = world.heights[r * w + c].toDouble()
        fun pt(c: Double, r: Double) = GenPoint((c + 0.5) / w, (r + 0.5) / h)
        val out = ArrayList<List<GenPoint>>()
        for (level in CONTOUR_LEVELS) {
            val lv = level.toDouble()
            for (r in 0 until h - 1) for (c in 0 until w - 1) {
                val a = hgt(c, r); val b = hgt(c + 1, r); val cc = hgt(c + 1, r + 1); val d = hgt(c, r + 1)
                val crossings = ArrayList<GenPoint>(4)
                fun edge(v1: Double, v2: Double, p1: Pair<Double, Double>, p2: Pair<Double, Double>) {
                    if ((v1 < lv) == (v2 < lv)) return
                    val t = (lv - v1) / (v2 - v1)
                    crossings += pt(p1.first + (p2.first - p1.first) * t, p1.second + (p2.second - p1.second) * t)
                }
                edge(a, b, c.toDouble() to r.toDouble(), (c + 1).toDouble() to r.toDouble())
                edge(b, cc, (c + 1).toDouble() to r.toDouble(), (c + 1).toDouble() to (r + 1).toDouble())
                edge(cc, d, (c + 1).toDouble() to (r + 1).toDouble(), c.toDouble() to (r + 1).toDouble())
                edge(d, a, c.toDouble() to (r + 1).toDouble(), c.toDouble() to r.toDouble())
                if (crossings.size >= 2) out += listOf(crossings[0], crossings[1])
                if (crossings.size == 4) out += listOf(crossings[2], crossings[3])
            }
        }
        return out
    }
}
