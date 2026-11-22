package io.github.lenticualrstone.civilight.feature.map.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.MapPin
import io.github.lenticualrstone.civilight.designsystem.component.MapValueChip
import io.github.lenticualrstone.civilight.designsystem.component.NodeCard
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.ResolvedTheme
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.store.blob.BlobStore
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import kotlin.math.roundToInt

/** Map ↔ screen for marks drawn by the screen on top of the map (drafts, handles, ghosts). */
class MapMarkScope(val math: ViewportMath, val viewport: Viewport) {
    fun screen(p: Point): ScreenPoint = math.toScreen(viewport, p)
}

/**
 * Places a mark so that the fraction ([ax], [ay]) of its size, shifted by [dxPx], sits on the
 * screen point ([x], [y]). Used inside a full-size box over the map.
 */
fun Modifier.mapAt(x: Float, y: Float, ax: Float = 0.5f, ay: Float = 0.5f, dxPx: Float = 0f): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(p.width, p.height) {
        p.place((x - ax * p.width - dxPx).roundToInt(), (y - ay * p.height).roundToInt())
    }
}

/**
 * One map surface for every map screen (view, edit, pick, list thumbnails): the base, the overlays
 * of [scene], two-finger pan and zoom (the standard map interaction; everything else is a visible
 * button or tool), and taps. A tap on the base calls [onTap] with its 0–1 point and what it hit
 * (pins, nodes and labels by a 44 dp target, then routes, then regions; [MapHitTest]).
 *
 * [extraDraw] draws screen-owned lines (drafts) above the overlays; [marks] places composables
 * (vertices, handles) with [mapAt]. [interactive] false makes a still thumbnail.
 */
@Composable
fun MapCanvas(
    base: MapBase,
    scene: MapScene,
    viewport: Viewport,
    onViewportChange: (Viewport) -> Unit,
    modifier: Modifier = Modifier,
    selectedKey: Key? = null,
    interactive: Boolean = true,
    showRouteChips: Boolean = true,
    onTap: (Point, MapHit?) -> Unit = { _, _ -> },
    extraDraw: DrawScope.(MapMarkScope) -> Unit = {},
    marks: @Composable BoxScope.(MapMarkScope) -> Unit = {},
) {
    val blobs: BlobStore = LocalFeatureDeps.current.stores.blobs
    var size by remember { mutableStateOf(IntSize.Zero) }
    val colors = CivilightTheme.colors
    val touchPx = with(LocalDensity.current) { 44.dp.toPx() }
    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .background(colors.bg)
            .onSizeChanged { size = it },
    ) {
        if (size.width == 0 || size.height == 0) return@Box
        val math = remember(size, base.aspect) { ViewportMath(size.width.toFloat(), size.height.toFloat(), base.aspect) }
        val resolved = math.resolve(viewport)
        if (viewport.zoom <= 0f && interactive) {
            LaunchedEffect(math) { onViewportChange(resolved) }
        }
        val current by rememberUpdatedState(resolved)
        val change by rememberUpdatedState(onViewportChange)
        val tap by rememberUpdatedState(onTap)
        val sceneNow by rememberUpdatedState(scene)
        val gestures = if (!interactive) Modifier else Modifier
            .pointerInput(math) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    change(math.transform(current, ScreenPoint(centroid.x, centroid.y), pan.x, pan.y, zoom))
                }
            }
            .pointerInput(math) {
                detectTapGestures { o ->
                    val s = ScreenPoint(o.x, o.y)
                    val p = math.toMap(current, s)
                    if (math.onBase(p)) tap(p, MapHitTest.hit(sceneNow, math, current, s, touchPx))
                }
            }
        val markScope = MapMarkScope(math, resolved)
        Box(Modifier.fillMaxSize().then(gestures)) {
            BaseLayer(base, math, resolved, blobs, Modifier.fillMaxSize())
            OverlayCanvas(scene, math, resolved, selectedKey, colors, Modifier.fillMaxSize(), extraDraw, markScope)
            OverlayMarks(scene, math, resolved, selectedKey, showRouteChips)
            marks(markScope)
        }
    }
}

@Composable
private fun OverlayCanvas(
    scene: MapScene,
    math: ViewportMath,
    viewport: Viewport,
    selectedKey: Key?,
    colors: CivilightColors,
    modifier: Modifier,
    extraDraw: DrawScope.(MapMarkScope) -> Unit,
    markScope: MapMarkScope,
) {
    val art = FixedColors.mapArt(if (colors.isDark) ResolvedTheme.DARK else ResolvedTheme.LIGHT)
    val measurer = rememberTextMeasurer()
    val labelStyle = CivilightTheme.type.label.merge(TextStyle(color = colors.ink))
    Canvas(modifier) {
        for (r in scene.regions) {
            if (r.shape.size < 3) continue
            val c = regionColor(r.color, art)
            val k = if (r.dim) 0.4f else 1f
            val path = polyPath(r.shape.map { math.toScreen(viewport, it) }, closed = true)
            drawPath(path, c.copy(alpha = 0.30f * k))
            val selected = r.key == selectedKey
            drawPath(path, if (selected) colors.acc else c.copy(alpha = 0.9f * k), style = Stroke((if (selected) 2.5 else 1.5).dp.toPx(), join = StrokeJoin.Round))
        }
        for (l in scene.links) {
            val a = math.toScreen(viewport, l.a)
            val b = math.toScreen(viewport, l.b)
            drawLine(colors.ink3.copy(alpha = if (l.dim) 0.4f else 1f), Offset(a.x, a.y), Offset(b.x, b.y), 1.5.dp.toPx(), StrokeCap.Round)
        }
        for (r in scene.routes) {
            val selected = r.key == selectedKey
            val path = polyPath(r.points.map { math.toScreen(viewport, it) }, closed = false)
            drawPath(path, colors.acc.copy(alpha = if (r.dim) 0.4f else 1f), style = Stroke((if (selected) 3.5 else 2.0).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        for (l in scene.labels) {
            val curve = l.curve ?: continue
            drawCurvedLabel(measurer, l.text, curve.map { math.toScreen(viewport, it) }, labelStyle.merge(TextStyle(color = if (l.key == selectedKey) colors.acc else colors.ink)), if (l.dim) 0.45f else 1f)
        }
        extraDraw(markScope)
    }
}

/** Pins, straight labels, nodes and route chips as composables (glass tags, 44 dp targets). */
@Composable
private fun BoxScope.OverlayMarks(scene: MapScene, math: ViewportMath, viewport: Viewport, selectedKey: Key?, showRouteChips: Boolean) {
    val density = LocalDensity.current
    val pinTip = with(density) { 12.dp.toPx() }
    val margin = with(density) { 160.dp.toPx() }
    fun onScreen(s: ScreenPoint) = s.x > -margin && s.y > -margin && s.x < math.viewW + margin && s.y < math.viewH + margin
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Box(Modifier.matchParentSize()) {
        for (l in scene.labels) {
            if (l.curve != null) continue
            val s = math.toScreen(viewport, l.at)
            if (!onScreen(s)) continue
            CivilightText(
                l.text, type.label, Modifier.mapAt(s.x, s.y).alpha(if (l.dim) 0.45f else 1f),
                color = if (l.key == selectedKey) colors.acc else colors.ink, maxLines = 1,
            )
        }
        if (showRouteChips) {
            for (r in scene.routes) {
                val mid = midpoint(r.points.map { math.toScreen(viewport, it) }) ?: continue
                if (!onScreen(mid)) continue
                MapValueChip(r.label, Modifier.mapAt(mid.x, mid.y, ay = 1.2f).alpha(if (r.dim) 0.45f else 1f))
            }
        }
        for (n in scene.nodes) {
            val s = math.toScreen(viewport, n.at)
            if (!onScreen(s)) continue
            NodeCard(n.name, Modifier.mapAt(s.x, s.y).alpha(if (n.dim) 0.45f else 1f), subtitle = n.sub, selected = n.key == selectedKey)
        }
        for (p in scene.pins) {
            val s = math.toScreen(viewport, p.at)
            if (!onScreen(s)) continue
            MapPin(p.name, Modifier.mapAt(s.x, s.y, ax = 0f, ay = 1f, dxPx = pinTip), selected = p.key == selectedKey, dim = p.dim)
        }
    }
}

private fun polyPath(points: List<ScreenPoint>, closed: Boolean): Path {
    val path = Path()
    points.forEachIndexed { i, s -> if (i == 0) path.moveTo(s.x, s.y) else path.lineTo(s.x, s.y) }
    if (closed) path.close()
    return path
}

/** The point halfway along a polyline. */
internal fun midpoint(points: List<ScreenPoint>): ScreenPoint? {
    if (points.size < 2) return points.firstOrNull()
    val half = CurveGeometry.length(points) / 2
    val g = CurveGeometry.pointAt(points, half)
    return ScreenPoint(g.x, g.y)
}

internal fun regionColor(token: String, art: List<Color>): Color {
    MapColors.parseHex(token)?.let { return Color(it.toInt()) }
    return art[MapColors.artIndex(token) ?: 0]
}

private fun DrawScope.drawCurvedLabel(measurer: TextMeasurer, text: String, control: List<ScreenPoint>, style: TextStyle, alpha: Float) {
    val path = CurveGeometry.sample(control)
    val glyphs = text.map { measurer.measure(it.toString(), style) }
    val placements = CurveGeometry.placeAlong(path, glyphs.map { it.size.width.toFloat() })
    glyphs.zip(placements).forEach { (g, p) ->
        rotate(p.angle, Offset(p.x, p.y)) {
            drawText(g, topLeft = Offset(p.x - g.size.width / 2f, p.y - g.size.height / 2f), alpha = alpha)
        }
    }
}
