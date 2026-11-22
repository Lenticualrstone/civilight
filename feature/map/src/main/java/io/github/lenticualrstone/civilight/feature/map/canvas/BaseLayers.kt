package io.github.lenticualrstone.civilight.feature.map.canvas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.domain.world.gen.GenPoint
import io.github.lenticualrstone.civilight.domain.world.gen.GeneratedWorld
import io.github.lenticualrstone.civilight.domain.world.map.HexMaps
import io.github.lenticualrstone.civilight.model.map.HexGrid
import io.github.lenticualrstone.civilight.model.map.HexOrientation
import io.github.lenticualrstone.civilight.store.blob.BlobStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Draws a map's base under the overlays (WLD-37, WLD-38). */
@Composable
fun BaseLayer(base: MapBase, math: ViewportMath, viewport: Viewport, blobs: BlobStore, modifier: Modifier = Modifier) {
    when (base) {
        is MapBase.Image -> ImageBase(base, math, viewport, blobs, modifier)
        is MapBase.Generated -> GeneratedBase(base, math, viewport, modifier)
        MapBase.Node -> SurfaceBase(math, viewport, CivilightTheme.colors.raise, modifier)
        MapBase.Blank -> SurfaceBase(math, viewport, CivilightTheme.colors.tint, modifier)
    }
}

@Composable
private fun SurfaceBase(math: ViewportMath, viewport: Viewport, fill: Color, modifier: Modifier) {
    val line = CivilightTheme.colors.line
    Canvas(modifier) {
        val tl = math.toScreen(viewport, 0f, 0f)
        val size = Size(math.contentW(viewport), math.contentH(viewport))
        drawRect(fill, Offset(tl.x, tl.y), size)
        drawRect(line, Offset(tl.x, tl.y), size, style = Stroke(1.dp.toPx()))
    }
}

// --- Image bases (WLD-38) ---

/** A decoded picture of the whole base at some subsample. */
private class Overview(val bitmap: ImageBitmap, val width: Int, val height: Int)

@Composable
private fun ImageBase(base: MapBase.Image, math: ViewportMath, viewport: Viewport, blobs: BlobStore, modifier: Modifier) {
    var overview by remember(base.hash) { mutableStateOf<Overview?>(null) }
    val tiles = remember(base.hash) { TileCache() }
    var tick by remember(base.hash) { mutableIntStateOf(0) }
    LaunchedEffect(base.hash) {
        overview = withContext(Dispatchers.IO) { decodeOverview(blobs, base.hash) }
    }
    val ov = overview
    val plan = ov?.takeIf { TilePlan.needsTiling(it.width, it.height) }?.let { o ->
        val r = math.visibleRect(viewport)
        val screenPerImagePx = math.contentW(viewport) / o.width
        val sample = TilePlan.sampleForScale(screenPerImagePx)
        val overviewSample = TilePlan.sampleToFit(maxOf(o.width, o.height), TilePlan.OVERVIEW_MAX)
        if (sample >= overviewSample) emptyList() else TilePlan.tiles(
            o.width, o.height,
            (r[0] * o.width).toInt(), (r[1] * o.height).toInt(), (r[2] * o.width).roundToInt() + 1, (r[3] * o.height).roundToInt() + 1,
            sample,
        )
    }.orEmpty()
    LaunchedEffect(base.hash, plan) {
        val missing = plan.filter { tiles[it] == null }
        if (missing.isEmpty()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val file = blobs.fileOf(base.hash) ?: return@withContext
            val decoder = runCatching { BitmapRegionDecoder.newInstance(file.path) }.getOrNull() ?: return@withContext
            try {
                for (t in missing) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = t.sample }
                    val bmp = runCatching { decoder.decodeRegion(Rect(t.left, t.top, t.right, t.bottom), opts) }.getOrNull() ?: continue
                    tiles.put(t, bmp.asImageBitmap())
                }
            } finally {
                decoder.recycle()
            }
        }
        tick++
    }
    val fallback = CivilightTheme.colors.tint
    Canvas(modifier) {
        if (tick < 0) return@Canvas
        val tl = math.toScreen(viewport, 0f, 0f)
        val w = math.contentW(viewport)
        val h = math.contentH(viewport)
        if (ov == null) {
            drawRect(fallback, Offset(tl.x, tl.y), Size(w, h))
            return@Canvas
        }
        drawImage(
            ov.bitmap,
            dstOffset = IntOffset(tl.x.roundToInt(), tl.y.roundToInt()),
            dstSize = IntSize(w.roundToInt().coerceAtLeast(1), h.roundToInt().coerceAtLeast(1)),
            filterQuality = FilterQuality.Medium,
        )
        for (t in plan) {
            val bmp = tiles[t] ?: continue
            val a = math.toScreen(viewport, t.left.toFloat() / ov.width, t.top.toFloat() / ov.height)
            val b = math.toScreen(viewport, t.right.toFloat() / ov.width, t.bottom.toFloat() / ov.height)
            drawImage(
                bmp,
                dstOffset = IntOffset(a.x.roundToInt(), a.y.roundToInt()),
                dstSize = IntSize((b.x - a.x).roundToInt().coerceAtLeast(1), (b.y - a.y).roundToInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}

/** Decodes the base whole (small images) or subsampled (large ones); never full size above the threshold. */
private fun decodeOverview(blobs: BlobStore, hash: String): Overview? {
    val file = blobs.fileOf(hash) ?: return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    val w = bounds.outWidth
    val h = bounds.outHeight
    if (w <= 0 || h <= 0) return null
    val long = maxOf(w, h)
    val sample = if (TilePlan.needsTiling(w, h)) TilePlan.sampleToFit(long, TilePlan.OVERVIEW_MAX) else 1
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val bmp: Bitmap = runCatching { BitmapFactory.decodeFile(file.path, opts) }.getOrNull() ?: return null
    return Overview(bmp.asImageBitmap(), w, h)
}

/** A small LRU of decoded tiles. */
private class TileCache(private val max: Int = 48) {
    private val map = object : LinkedHashMap<Tile, ImageBitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Tile, ImageBitmap>?): Boolean = size > max
    }

    @Synchronized operator fun get(t: Tile): ImageBitmap? = map[t]

    @Synchronized fun put(t: Tile, b: ImageBitmap) {
        map[t] = b
    }
}

// --- Generated bases (WLD-53 results; styles are WLD-57's) ---

@Composable
private fun GeneratedBase(base: MapBase.Generated, math: ViewportMath, viewport: Viewport, modifier: Modifier) {
    val raster = remember(base.world) { base.world?.let(::terrainRaster) }
    val colors = CivilightTheme.colors
    val river = FixedColors.terrain.getValue(io.github.lenticualrstone.civilight.designsystem.theme.Terrain.SEA)
    Canvas(modifier) {
        val tl = math.toScreen(viewport, 0f, 0f)
        val w = math.contentW(viewport)
        val h = math.contentH(viewport)
        val hex = base.hex
        when {
            hex != null && hex.cells.isNotEmpty() -> drawHexCells(hex, math, viewport)
            raster != null -> drawImage(
                raster,
                dstOffset = IntOffset(tl.x.roundToInt(), tl.y.roundToInt()),
                dstSize = IntSize(w.roundToInt().coerceAtLeast(1), h.roundToInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.Medium,
            )
            else -> drawRect(colors.tint, Offset(tl.x, tl.y), Size(w, h))
        }
        val world = base.world ?: return@Canvas
        if (hex == null || hex.cells.isEmpty()) {
            world.coastline.forEach { drawGenLine(it, math, viewport, colors.ink3.copy(alpha = 0.6f), 1.dp.toPx(), closed = true) }
        }
        world.rivers.forEach { drawGenLine(it, math, viewport, river, 1.5.dp.toPx(), closed = false) }
        world.roads.forEach { drawGenLine(it, math, viewport, colors.ink2.copy(alpha = 0.55f), 1.dp.toPx(), closed = false) }
    }
}

/** One pixel per generator cell, coloured by terrain (`ter-*`); drawn scaled and smoothed. */
private fun terrainRaster(world: GeneratedWorld): ImageBitmap {
    val pixels = IntArray(world.w * world.h) { i ->
        val c = FixedColors.terrain.getValue(MapColors.dsTerrain(world.terrain(i)))
        argb(c)
    }
    return Bitmap.createBitmap(pixels, world.w, world.h, Bitmap.Config.ARGB_8888).asImageBitmap()
}

private fun argb(c: Color): Int =
    ((c.alpha * 255).roundToInt() shl 24) or ((c.red * 255).roundToInt() shl 16) or ((c.green * 255).roundToInt() shl 8) or (c.blue * 255).roundToInt()

private fun DrawScope.drawGenLine(points: List<GenPoint>, math: ViewportMath, viewport: Viewport, color: Color, width: Float, closed: Boolean) {
    if (points.size < 2) return
    val path = Path()
    points.forEachIndexed { i, p ->
        val s = math.toScreen(viewport, p.x.toFloat(), p.y.toFloat())
        if (i == 0) path.moveTo(s.x, s.y) else path.lineTo(s.x, s.y)
    }
    if (closed) path.close()
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawHexCells(grid: HexGrid, math: ViewportMath, viewport: Viewport) {
    val (rx, ry) = MapBases.hexRadius(grid)
    val flat = grid.orientation.known == HexOrientation.FLAT
    val start = if (flat) 0.0 else Math.PI / 6
    val edge = Color.Black.copy(alpha = 0.18f)
    for (i in 0 until grid.rows * grid.cols) {
        val cell = HexMaps.cellOf(grid, i)
        val terrain = HexMaps.terrainAt(grid, i) ?: continue
        val c = HexMaps.center(grid, cell)
        val path = Path()
        for (k in 0 until 6) {
            val a = start + k * Math.PI / 3
            val s = math.toScreen(viewport, c.x + (rx * cos(a)).toFloat(), c.y + (ry * sin(a)).toFloat())
            if (k == 0) path.moveTo(s.x, s.y) else path.lineTo(s.x, s.y)
        }
        path.close()
        drawPath(path, FixedColors.terrain.getValue(MapColors.dsTerrain(terrain)))
        drawPath(path, edge, style = Stroke(0.75.dp.toPx()))
    }
}
