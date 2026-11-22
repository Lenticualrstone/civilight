package io.github.lenticualrstone.civilight.feature.map.canvas

import io.github.lenticualrstone.civilight.domain.world.gen.GeneratedWorld
import io.github.lenticualrstone.civilight.domain.world.gen.MapGenerator
import io.github.lenticualrstone.civilight.domain.world.map.MapScales
import io.github.lenticualrstone.civilight.model.map.HexGrid
import io.github.lenticualrstone.civilight.model.map.HexOrientation
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.DrawnLineKind
import io.github.lenticualrstone.civilight.model.map.MapStyle
import io.github.lenticualrstone.civilight.model.map.PolyMap
import io.github.lenticualrstone.civilight.model.map.WorldMap
import kotlin.math.max
import kotlin.math.sqrt

/**
 * What lies under the overlays. Overlay data is never baked into the base (WLD-37), so every kind
 * shares the same 0–1 overlay space and only the drawing differs. WLD-57 styles plug in at
 * [Generated].
 */
sealed interface MapBase {
    /** Width / height of the base. */
    val aspect: Float

    /** An imported image, a straightened sketch or a rendered generated base (WLD-38, WLD-39). */
    data class Image(val hash: String, val width: Int?, val height: Int?) : MapBase {
        override val aspect: Float get() = if (width != null && height != null && width > 0 && height > 0) width.toFloat() / height else DEFAULT_ASPECT
    }

    /**
     * A generated map drawn from its stored result and format data: hex cells (WLD-58), polygon
     * cells (WLD-59) or the world picture in [style] (WLD-57) with `등고선` when [contours]. A
     * detailed-terrain map keeps its rivers and roads as drawn lines in the scene ([worldLines]
     * false), so the author can edit them (WLD-60).
     */
    data class Generated(
        val world: GeneratedWorld?,
        val hex: HexGrid?,
        val poly: PolyMap? = null,
        val style: MapStyle = MapStyle.SOFT,
        val contours: Boolean = false,
        val worldLines: Boolean = true,
        /** A detailed-terrain map without a drawn river (or road) shows the stored world's (no empty map). */
        val worldRivers: Boolean = worldLines,
        val worldRoads: Boolean = worldLines,
    ) : MapBase {
        override val aspect: Float
            get() = when {
                world != null -> world.w.toFloat() / world.h
                hex != null -> MapBases.hexAspect(hex)
                else -> DEFAULT_ASPECT
            }
    }

    /** A node map: no base, a plain surface (WLD-50). */
    data object Node : MapBase {
        override val aspect: Float = 1f
    }

    /** A map whose base is missing (an image map before its image arrives). */
    data object Blank : MapBase {
        override val aspect: Float = DEFAULT_ASPECT
    }

    companion object {
        const val DEFAULT_ASPECT = 4f / 3f
    }
}

object MapBases {
    fun of(map: WorldMap): MapBase {
        val hash = map.baseImageHash
        return when {
            MapScales.isNodeMap(map) -> MapBase.Node
            hash != null -> MapBase.Image(hash, map.baseSize?.w, map.baseSize?.h)
            map.baseKind.known == MapBaseKind.GENERATED -> MapBase.Generated(
                world = runCatching { MapGenerator.storedWorld(map) }.getOrNull(),
                hex = map.hex,
                poly = map.poly?.takeIf { it.cells.isNotEmpty() },
                style = map.style?.known ?: MapStyle.SOFT,
                contours = map.contours,
                worldLines = map.format?.known != MapFormat.DETAIL,
                worldRivers = map.format?.known != MapFormat.DETAIL || map.drawnLines.none { it.kind.known == DrawnLineKind.RIVER },
                worldRoads = map.format?.known != MapFormat.DETAIL || map.drawnLines.none { it.kind.known == DrawnLineKind.ROAD },
            )
            map.baseKind.known == MapBaseKind.SKETCH -> map.sketch?.photoHash?.let { MapBase.Image(it, null, null) } ?: MapBase.Blank
            else -> MapBase.Blank
        }
    }

    /** Width / height of a hex grid in the layout `HexMaps.center` uses. */
    fun hexAspect(grid: HexGrid): Float {
        val s3 = sqrt(3.0)
        val (w, h) = if (grid.orientation.known != HexOrientation.FLAT) {
            (grid.cols * s3 + if (grid.rows > 1) s3 / 2 else 0.0) to ((grid.rows - 1) * 1.5 + 2.0)
        } else {
            ((grid.cols - 1) * 1.5 + 2.0) to (grid.rows * s3 + if (grid.cols > 1) s3 / 2 else 0.0)
        }
        return (w / max(h, 1e-6)).toFloat()
    }

    /** Hex radius in 0–1 units along x and y (one unit of the `HexMaps.center` layout). */
    fun hexRadius(grid: HexGrid): Pair<Float, Float> {
        val s3 = sqrt(3.0)
        val (w, h) = if (grid.orientation.known != HexOrientation.FLAT) {
            (grid.cols * s3 + if (grid.rows > 1) s3 / 2 else 0.0) to ((grid.rows - 1) * 1.5 + 2.0)
        } else {
            ((grid.cols - 1) * 1.5 + 2.0) to (grid.rows * s3 + if (grid.cols > 1) s3 / 2 else 0.0)
        }
        return (1.0 / w).toFloat() to (1.0 / h).toFloat()
    }
}

/** A region of the image to decode: pixel bounds and the subsample factor (a power of two). */
data class Tile(val left: Int, val top: Int, val right: Int, val bottom: Int, val sample: Int)

/**
 * Large-image plan (WLD-38): images up to [FULL_DECODE_MAX] px on the long edge decode whole;
 * larger ones show a subsampled overview and decode only the tiles of the zoomed area at the
 * sample size the zoom needs (subsampled region decoding). Loading a whole ~8000 px bitmap freezes,
 * so full decoding above the threshold never happens.
 */
object TilePlan {
    /** `[PROPOSED: > 4096 px on the long edge]`. */
    const val FULL_DECODE_MAX = 4096

    /** Long edge of the overview of a large image. */
    const val OVERVIEW_MAX = 2048

    /** Tile edge in decoded pixels. */
    const val TILE = 512

    fun needsTiling(width: Int, height: Int): Boolean = max(width, height) > FULL_DECODE_MAX

    /** The smallest power of two that brings [longEdge] to at most [maxEdge]. */
    fun sampleToFit(longEdge: Int, maxEdge: Int): Int {
        var s = 1
        while (longEdge / s > maxEdge) s *= 2
        return s
    }

    /** The largest power of two not above 1 / [screenPerImagePx] (at least 1): no detail is decoded that the screen cannot show. */
    fun sampleForScale(screenPerImagePx: Float): Int {
        if (screenPerImagePx <= 0f) return 1
        var s = 1
        while (s * 2 * screenPerImagePx <= 1f) s *= 2
        return s
    }

    /**
     * Tiles covering the visible image rectangle [l], [t], [r], [b] (pixels) at [sample], aligned on a
     * grid of `TILE × sample` image pixels so they can be cached by position.
     */
    fun tiles(width: Int, height: Int, l: Int, t: Int, r: Int, b: Int, sample: Int): List<Tile> {
        val step = TILE * sample
        val left = l.coerceIn(0, width)
        val top = t.coerceIn(0, height)
        val right = r.coerceIn(0, width)
        val bottom = b.coerceIn(0, height)
        if (right <= left || bottom <= top) return emptyList()
        val out = ArrayList<Tile>()
        var y = (top / step) * step
        while (y < bottom) {
            var x = (left / step) * step
            while (x < right) {
                out += Tile(x, y, minOf(x + step, width), minOf(y + step, height), sample)
                x += step
            }
            y += step
        }
        return out
    }
}
