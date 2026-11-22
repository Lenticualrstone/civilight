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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.produceState
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.domain.world.gen.GenPoint
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

// --- Generated bases (WLD-53 results, WLD-57 styles, WLD-58 hex, WLD-59 polygons) ---

/** The theme colours of a generated base. */
@Composable
fun rememberBasePalette(): BasePalette {
    val c = CivilightTheme.colors
    return remember(c) {
        BasePalette(
            dark = c.isDark,
            art = FixedColors.mapArt(if (c.isDark) ResolvedTheme.DARK else ResolvedTheme.LIGHT).map { it.toArgb() },
            bg = c.bg.toArgb(), tint = c.tint.toArgb(), ink = c.ink.toArgb(), ink2 = c.ink2.toArgb(), ink3 = c.ink3.toArgb(),
        )
    }
}

/** The picture under a generated base (null for hex grids, which are drawn cell by cell). */
fun baseRaster(base: MapBase.Generated, palette: BasePalette): Raster? {
    val world = base.world
    val poly = base.poly
    val hex = base.hex
    return when {
        hex != null && hex.cells.isNotEmpty() -> null
        poly != null -> GenRaster.poly(poly, world, base.style, palette)
        world != null -> GenRaster.world(world, base.style, palette)
        else -> null
    }
}

/** Lines over a generated base (coast, ridges, rivers, roads, `등고선`). */
fun baseLines(base: MapBase.Generated, palette: BasePalette): List<BaseLine> {
    val world = base.world ?: return emptyList()
    val hex = base.hex != null && base.hex.cells.isNotEmpty()
    val worldLines = base.poly == null
    return GenRaster.lines(world, base.style, palette, base.contours, base.worldLines && worldLines, hex, base.worldRivers && worldLines, base.worldRoads && worldLines)
}

@Composable
private fun GeneratedBase(base: MapBase.Generated, math: ViewportMath, viewport: Viewport, modifier: Modifier) {
    val palette = rememberBasePalette()
    val raster by produceState<ImageBitmap?>(null, base, palette) {
        value = withContext(Dispatchers.Default) {
            baseRaster(base, palette)?.let { Bitmap.createBitmap(it.pixels, it.w, it.h, Bitmap.Config.ARGB_8888).asImageBitmap() }
        }
    }
    val lines = remember(base, palette) { baseLines(base, palette) }
    val colors = CivilightTheme.colors
    val measurer = rememberTextMeasurer()
    val coordStyle = CivilightTheme.type.number.merge(TextStyle(fontSize = 9.sp, color = colors.ink.copy(alpha = 0.7f)))
    Canvas(modifier) {
        val tl = math.toScreen(viewport, 0f, 0f)
        val w = math.contentW(viewport)
        val h = math.contentH(viewport)
        val hex = base.hex
        val bmp = raster
        when {
            hex != null && hex.cells.isNotEmpty() -> drawHexCells(hex, math, viewport, measurer, coordStyle)
            bmp != null -> drawImage(
                bmp,
                dstOffset = IntOffset(tl.x.roundToInt(), tl.y.roundToInt()),
                dstSize = IntSize(w.roundToInt().coerceAtLeast(1), h.roundToInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.Medium,
            )
            else -> drawRect(colors.tint, Offset(tl.x, tl.y), Size(w, h))
        }
        val dash = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
        for (l in lines) {
            drawGenLine(l.points, math, viewport, Color(l.color), l.widthDp.dp.toPx(), l.closed, if (l.dashed) dash else null)
        }
    }
}

private fun DrawScope.drawGenLine(points: List<GenPoint>, math: ViewportMath, viewport: Viewport, color: Color, width: Float, closed: Boolean, effect: PathEffect? = null) {
    if (points.size < 2) return
    val path = Path()
    points.forEachIndexed { i, p ->
        val s = math.toScreen(viewport, p.x.toFloat(), p.y.toFloat())
        if (i == 0) path.moveTo(s.x, s.y) else path.lineTo(s.x, s.y)
    }
    if (closed) path.close()
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = effect))
}

/**
 * Hex cells in the fixed `ter-*` colours, the automatic marks (`표식(산 · 숲)은 지형 따라 자동`:
 * a peak for 산 · 봉우리, a hump for 언덕, a dot cluster for 숲) and the coordinates every
 * `좌표 표시` cells (WLD-58).
 */
private fun DrawScope.drawHexCells(grid: HexGrid, math: ViewportMath, viewport: Viewport, measurer: TextMeasurer, coordStyle: TextStyle) {
    val (rx, ry) = MapBases.hexRadius(grid)
    val flat = grid.orientation.known == HexOrientation.FLAT
    val start = if (flat) 0.0 else Math.PI / 6
    val edge = Color.Black.copy(alpha = 0.18f)
    val mark = Color.Black.copy(alpha = 0.35f)
    val cellPx = math.contentW(viewport) * rx
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
        val sc = math.toScreen(viewport, c)
        if (cellPx >= 10.dp.toPx()) {
            HexMarks.of(terrain)?.let { glyph ->
                val r = cellPx * 0.45f
                glyph.forEach { stroke ->
                    val p = Path()
                    stroke.forEachIndexed { j, q -> val x = sc.x + q.x * r; val y = sc.y + q.y * r; if (j == 0) p.moveTo(x, y) else p.lineTo(x, y) }
                    drawPath(p, mark, style = Stroke(1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
        if (HexMaps.showsLabel(grid, cell) && cellPx >= 14.dp.toPx()) {
            val layout = measurer.measure(HexMaps.label(cell), coordStyle)
            drawText(layout, topLeft = Offset(sc.x - layout.size.width / 2f, sc.y + cellPx * 0.35f - layout.size.height / 2f))
        }
    }
}
