package io.github.lenticualrstone.civilight.feature.map.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.DashPathEffect
import io.github.lenticualrstone.civilight.feature.map.canvas.BasePalette
import io.github.lenticualrstone.civilight.feature.map.canvas.StampGlyphs
import io.github.lenticualrstone.civilight.feature.map.canvas.HexMarks
import io.github.lenticualrstone.civilight.feature.map.canvas.baseLines
import io.github.lenticualrstone.civilight.feature.map.canvas.baseRaster
import io.github.lenticualrstone.civilight.model.map.DrawnLineKind
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.Terrain as DsTerrain
import io.github.lenticualrstone.civilight.domain.world.gen.GenPoint
import io.github.lenticualrstone.civilight.domain.world.map.HexMaps
import io.github.lenticualrstone.civilight.feature.map.canvas.CurveGeometry
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBase
import io.github.lenticualrstone.civilight.feature.map.canvas.MapBases
import io.github.lenticualrstone.civilight.feature.map.canvas.MapColors
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScene
import io.github.lenticualrstone.civilight.feature.map.canvas.ScreenPoint
import io.github.lenticualrstone.civilight.feature.map.canvas.midpoint
import io.github.lenticualrstone.civilight.feature.map.canvas.regionColor
import io.github.lenticualrstone.civilight.feature.map.sketch.MapSketchFileProvider
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.map.HexGrid
import io.github.lenticualrstone.civilight.model.map.HexOrientation
import io.github.lenticualrstone.civilight.store.blob.BlobStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

/** The theme colours the export draws with (read from the composition, used off the main thread). */
class ExportColors(val bg: Int, val tint: Int, val ink: Int, val ink2: Int, val ink3: Int, val acc: Int, val raise: Int, val line: Int, val art: List<Color>, val dark: Boolean = true) {
    /** The generated-base palette (WLD-57), the same one the map canvas draws with. */
    val palette: BasePalette get() = BasePalette(dark, art.map { it.toArgb() }, bg, tint, ink, ink2, ink3)

    companion object {
        fun of(c: CivilightColors, art: List<Color>) = ExportColors(
            c.bg.toArgb(), c.tint.toArgb(), c.ink.toArgb(), c.ink2.toArgb(), c.ink3.toArgb(), c.acc.toArgb(), c.raise.toArgb(), c.line.toArgb(), art, c.isDark,
        )
    }
}

/**
 * `지도 내보내기` (WLD-51): the base plus the overlays drawn at the current lens (the same
 * [MapScene] the map shows, so rule 19 masking and `보이는 시점` hold) into one PNG, then the system
 * share sheet. Large bases are read in bands ([ExportPlan.bands]). An 8K bitmap that does not fit in
 * memory falls back to 16-bit colour; if that fails too the export is refused (null).
 */
object MapExport {
    private const val DIR = "map_export"

    suspend fun render(context: Context, blobs: BlobStore, base: MapBase, scene: MapScene, colors: ExportColors, longEdge: Int): Bitmap? =
        withContext(Dispatchers.Default) {
            val (w, h) = ExportPlan.size(base.aspect, longEdge)
            val bitmap = allocate(w, h) ?: return@withContext null
            val canvas = Canvas(bitmap)
            canvas.drawColor(colors.bg)
            val font = runCatching { ResourcesCompat.getFont(context, io.github.lenticualrstone.civilight.designsystem.R.font.pretendard_medium) }.getOrNull()
                ?: Typeface.DEFAULT
            val ok = runCatching {
                drawBase(canvas, base, blobs, w, h, colors, font)
                drawOverlays(canvas, scene, w, h, colors, font)
            }.isSuccess
            if (ok) bitmap else null
        }

    private fun allocate(w: Int, h: Int): Bitmap? =
        try {
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        } catch (_: OutOfMemoryError) {
            try {
                Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
            } catch (_: OutOfMemoryError) {
                null
            }
        }

    /** Writes [bitmap] as a PNG into the private cache and opens the share sheet. */
    /** Writes the PNG to the cache and opens the system share sheet (no chooser title: none is given). */
    suspend fun share(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, DIR).apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val f = File(dir, fileName)
            runCatching { f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }.getOrNull() ?: return@withContext null
            f
        } ?: return false
        bitmap.recycle()
        val uri = FileProvider.getUriForFile(context, authority(context), file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching { context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    }

    /** The module's FileProvider ([MapSketchFileProvider]) also serves `map_export/` from the cache. */
    fun authority(context: Context): String = "${context.packageName}.map.sketch"

    // --- Base ---

    private fun drawBase(canvas: Canvas, base: MapBase, blobs: BlobStore, w: Int, h: Int, colors: ExportColors, font: Typeface) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        when (base) {
            is MapBase.Image -> if (!drawImage(canvas, base.hash, blobs, w, h)) canvas.drawColor(colors.tint)
            is MapBase.Generated -> drawGenerated(canvas, base, w, h, colors, font)
            MapBase.Node -> canvas.drawColor(colors.raise)
            MapBase.Blank -> canvas.drawColor(colors.tint)
        }
        fill.style = Paint.Style.STROKE
        fill.color = colors.line
        fill.strokeWidth = ExportPlan.unit(w, h)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), fill)
    }

    private fun drawImage(canvas: Canvas, hash: String, blobs: BlobStore, w: Int, h: Int): Boolean {
        val file = blobs.fileOf(hash) ?: return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val iw = bounds.outWidth
        val ih = bounds.outHeight
        if (iw <= 0 || ih <= 0) return false
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        val bands = ExportPlan.bands(iw, ih, w)
        if (bands.size == 1) {
            val bmp = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = bands[0].sample }) ?: return false
            canvas.drawBitmap(bmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), paint)
            bmp.recycle()
            return true
        }
        val decoder = runCatching { BitmapRegionDecoder.newInstance(file.path) }.getOrNull() ?: return false
        try {
            for (b in bands) {
                val opts = BitmapFactory.Options().apply { inSampleSize = b.sample }
                val bmp = decoder.decodeRegion(Rect(0, b.top, iw, b.bottom), opts) ?: continue
                val dst = RectF(0f, b.top.toFloat() / ih * h, w.toFloat(), b.bottom.toFloat() / ih * h)
                canvas.drawBitmap(bmp, null, dst, paint)
                bmp.recycle()
            }
        } finally {
            decoder.recycle()
        }
        return true
    }

    private fun drawGenerated(canvas: Canvas, base: MapBase.Generated, w: Int, h: Int, colors: ExportColors, font: Typeface) {
        val hex = base.hex
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        val u = ExportPlan.unit(w, h)
        val palette = colors.palette
        val raster = baseRaster(base, palette)
        when {
            hex != null && hex.cells.isNotEmpty() -> drawHex(canvas, hex, w, h, font)
            raster != null -> {
                val bmp = Bitmap.createBitmap(raster.pixels, raster.w, raster.h, Bitmap.Config.ARGB_8888)
                canvas.drawBitmap(bmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
                bmp.recycle()
            }
            else -> canvas.drawColor(colors.tint)
        }
        for (l in baseLines(base, palette)) {
            stroke.color = l.color
            stroke.strokeWidth = l.widthDp * u
            stroke.pathEffect = if (l.dashed) DashPathEffect(floatArrayOf(5f * u, 4f * u), 0f) else null
            canvas.drawPath(genPath(l.points, w, h, l.closed), stroke)
        }
    }

    /** Hex cells with the automatic marks and the `좌표 표시` labels, as the screen draws them (WLD-58). */
    private fun drawHex(canvas: Canvas, grid: HexGrid, w: Int, h: Int, font: Typeface) {
        val mark = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = android.graphics.Color.argb(89, 0, 0, 0); strokeWidth = ExportPlan.unit(w, h) }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = font; color = android.graphics.Color.argb(178, 0, 0, 0); textSize = 9f * ExportPlan.unit(w, h) }
        drawHexCells(canvas, grid, w, h)
        val (rx, _) = MapBases.hexRadius(grid)
        val cellPx = rx * w
        for (i in 0 until grid.rows * grid.cols) {
            val cell = HexMaps.cellOf(grid, i)
            val c = HexMaps.center(grid, cell)
            val cx = c.x * w
            val cy = c.y * h
            HexMaps.terrainAt(grid, i)?.let(HexMarks::of)?.forEach { stroke ->
                val path = Path()
                stroke.forEachIndexed { j, q -> val x = cx + q.x * cellPx * 0.45f; val y = cy + q.y * cellPx * 0.45f; if (j == 0) path.moveTo(x, y) else path.lineTo(x, y) }
                canvas.drawPath(path, mark)
            }
            if (HexMaps.showsLabel(grid, cell)) {
                val text = HexMaps.label(cell)
                canvas.drawText(text, cx - label.measureText(text) / 2, cy + cellPx * 0.35f + label.textSize / 3, label)
            }
        }
    }

    private fun drawHexCells(canvas: Canvas, grid: HexGrid, w: Int, h: Int) {
        val (rx, ry) = MapBases.hexRadius(grid)
        val start = if (grid.orientation.known == HexOrientation.FLAT) 0.0 else Math.PI / 6
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = android.graphics.Color.argb(46, 0, 0, 0); strokeWidth = ExportPlan.unit(w, h) * 0.75f }
        for (i in 0 until grid.rows * grid.cols) {
            val terrain = HexMaps.terrainAt(grid, i) ?: continue
            val c = HexMaps.center(grid, HexMaps.cellOf(grid, i))
            val path = Path()
            for (k in 0 until 6) {
                val a = start + k * Math.PI / 3
                val x = (c.x + rx * cos(a).toFloat()) * w
                val y = (c.y + ry * sin(a).toFloat()) * h
                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            fill.color = terrainColor(MapColors.dsTerrain(terrain))
            canvas.drawPath(path, fill)
            canvas.drawPath(path, edge)
        }
    }

    // --- Overlays ---

    private fun drawOverlays(canvas: Canvas, scene: MapScene, w: Int, h: Int, colors: ExportColors, font: Typeface) {
        val u = ExportPlan.unit(w, h)
        fun at(p: Point) = ScreenPoint(p.x * w, p.y * h)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = font; textSize = 13f * u; color = colors.ink }
        for (l in scene.lines) {
            val river = l.kind == DrawnLineKind.RIVER
            stroke.color = if (river) terrainColor(DsTerrain.SEA) else withAlpha(colors.ink2, 0.7f)
            stroke.strokeWidth = (if (river) 1.6f else 1.2f) * u
            stroke.pathEffect = if (river) null else DashPathEffect(floatArrayOf(5f * u, 4f * u), 0f)
            canvas.drawPath(polyPath(l.points.map(::at), closed = false), stroke)
        }
        stroke.pathEffect = null
        for (r in scene.regions) {
            if (r.shape.size < 3) continue
            val c = regionColor(r.color, colors.art)
            val k = if (r.dim) 0.4f else 1f
            val path = polyPath(r.shape.map(::at), closed = true)
            fill.color = c.copy(alpha = 0.30f * k).toArgb()
            canvas.drawPath(path, fill)
            stroke.color = c.copy(alpha = 0.9f * k).toArgb(); stroke.strokeWidth = 1.5f * u
            canvas.drawPath(path, stroke)
        }
        for (st in scene.stamps) {
            val c = at(st.at)
            val r = maxOf(StampGlyphs.radius(st.size) * w, (4 + 3 * st.size) * u)
            stroke.color = withAlpha(colors.ink, if (st.dim) 0.4f else 0.85f); stroke.strokeWidth = 1.4f * u
            for (g in StampGlyphs.strokes(st.kind)) {
                val path = Path()
                StampGlyphs.place(g, c.x, c.y, r, st.rotation).forEachIndexed { i, (x, y) -> if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
                canvas.drawPath(path, stroke)
            }
        }
        for (l in scene.links) {
            stroke.color = withAlpha(colors.ink3, if (l.dim) 0.4f else 1f); stroke.strokeWidth = 1.5f * u
            val a = at(l.a); val b = at(l.b)
            canvas.drawLine(a.x, a.y, b.x, b.y, stroke)
            l.label?.let { label -> tag(canvas, label, (a.x + b.x) / 2, (a.y + b.y) / 2, text, fill, colors, u, l.dim) }
        }
        for (r in scene.routes) {
            stroke.color = withAlpha(colors.acc, if (r.dim) 0.4f else 1f); stroke.strokeWidth = 2f * u
            val points = r.points.map(::at)
            canvas.drawPath(polyPath(points, closed = false), stroke)
            midpoint(points)?.let { m -> tag(canvas, r.label, m.x, m.y - 14f * u, text, fill, colors, u, r.dim) }
        }
        for (l in scene.labels) {
            text.color = withAlpha(colors.ink, if (l.dim) 0.45f else 1f)
            val curve = l.curve
            if (curve != null) {
                val sampled = CurveGeometry.sample(curve.map(::at))
                val path = polyPath(sampled, closed = false)
                val width = text.measureText(l.text)
                val length = CurveGeometry.length(sampled)
                canvas.drawTextOnPath(l.text, path, ((length - width) / 2).coerceAtLeast(0f), text.textSize / 3, text)
            } else {
                val p = at(l.at)
                canvas.drawText(l.text, p.x - text.measureText(l.text) / 2, p.y + text.textSize / 3, text)
            }
        }
        for (n in scene.nodes) {
            val p = at(n.at)
            val title = n.name
            val sub = n.sub
            val tw = maxOf(text.measureText(title), sub?.let { text.measureText(it) * 0.85f } ?: 0f) + 24f * u
            val th = if (sub != null) 46f * u else 32f * u
            val rect = RectF(p.x - tw / 2, p.y - th / 2, p.x + tw / 2, p.y + th / 2)
            fill.color = withAlpha(colors.raise, if (n.dim) 0.45f else 1f)
            canvas.drawRoundRect(rect, 10f * u, 10f * u, fill)
            stroke.color = colors.line; stroke.strokeWidth = u
            canvas.drawRoundRect(rect, 10f * u, 10f * u, stroke)
            text.color = withAlpha(colors.ink, if (n.dim) 0.45f else 1f)
            canvas.drawText(title, rect.left + 12f * u, rect.top + 20f * u, text)
            if (sub != null) {
                val size = text.textSize
                text.textSize = size * 0.85f; text.color = withAlpha(colors.ink3, if (n.dim) 0.45f else 1f)
                canvas.drawText(sub, rect.left + 12f * u, rect.top + 38f * u, text)
                text.textSize = size
            }
        }
        for (p in scene.pins) {
            val s = at(p.at)
            val k = if (p.dim) 0.45f else 1f
            fill.color = withAlpha(colors.ink, k)
            canvas.drawCircle(s.x, s.y - 12f * u, 6f * u, fill)
            stroke.color = withAlpha(colors.ink, k); stroke.strokeWidth = 2f * u
            canvas.drawLine(s.x, s.y - 6f * u, s.x, s.y, stroke)
            text.color = withAlpha(colors.ink, k)
            tag(canvas, p.name, s.x + 12f * u + (text.measureText(p.name) + 16f * u) / 2, s.y - 12f * u, text, fill, colors, u, p.dim)
        }
    }

    /** A glass-like name tag centred on (x, y). */
    private fun tag(canvas: Canvas, label: String, x: Float, y: Float, text: Paint, fill: Paint, colors: ExportColors, u: Float, dim: Boolean) {
        val k = if (dim) 0.45f else 1f
        val tw = text.measureText(label) + 16f * u
        val th = 22f * u
        fill.color = withAlpha(colors.tint, 0.92f * k)
        canvas.drawRoundRect(RectF(x - tw / 2, y - th / 2, x + tw / 2, y + th / 2), th / 2, th / 2, fill)
        text.color = withAlpha(colors.ink, k)
        canvas.drawText(label, x - tw / 2 + 8f * u, y + text.textSize / 3, text)
    }

    private fun polyPath(points: List<ScreenPoint>, closed: Boolean): Path {
        val path = Path()
        points.forEachIndexed { i, s -> if (i == 0) path.moveTo(s.x, s.y) else path.lineTo(s.x, s.y) }
        if (closed) path.close()
        return path
    }

    private fun genPath(points: List<GenPoint>, w: Int, h: Int, closed: Boolean): Path {
        val path = Path()
        points.forEachIndexed { i, p ->
            val x = p.x.toFloat() * w
            val y = p.y.toFloat() * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (closed) path.close()
        return path
    }

    private fun terrainColor(t: DsTerrain): Int = FixedColors.terrain.getValue(t).toArgb()

    private fun withAlpha(argb: Int, alpha: Float): Int =
        android.graphics.Color.argb((android.graphics.Color.alpha(argb) * alpha).toInt().coerceIn(0, 255), android.graphics.Color.red(argb), android.graphics.Color.green(argb), android.graphics.Color.blue(argb))
}
