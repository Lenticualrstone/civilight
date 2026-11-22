package io.github.lenticualrstone.civilight.feature.map.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import io.github.lenticualrstone.civilight.designsystem.component.MAP_DIM_ALPHA
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.animatedFraction
import io.github.lenticualrstone.civilight.feature.map.common.MapMotion
import io.github.lenticualrstone.civilight.feature.map.common.markAlpha
import io.github.lenticualrstone.civilight.feature.map.common.markProgress
import io.github.lenticualrstone.civilight.designsystem.theme.MotionToken
import io.github.lenticualrstone.civilight.feature.map.common.unhideIn
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PathEffect
import io.github.lenticualrstone.civilight.model.map.DrawnLineKind
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
    /** The base's drawn width in px at the current zoom (the scale bar picks 1 or 2 days from it). */
    onContentWidth: ((Float) -> Unit)? = null,
    /** Taps may hit stamps and drawn lines (the detailed-terrain editor only). */
    hitTerrain: Boolean = false,
    /** Marks that become visible clear from blur and dim on the unhide motion, one after another (시점별 공개), instead of fading in. */
    unhideFresh: Boolean = false,
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
        val contentWidth = math.contentW(resolved)
        if (onContentWidth != null) {
            val report by rememberUpdatedState(onContentWidth)
            LaunchedEffect(contentWidth) { report(contentWidth) }
        }
        val current by rememberUpdatedState(resolved)
        val change by rememberUpdatedState(onViewportChange)
        val tap by rememberUpdatedState(onTap)
        val sceneNow by rememberUpdatedState(scene)
        val settle = remember { SettleHolder() }
        val scope = rememberCoroutineScope()
        val motion = CivilightTheme.motion
        val motionNow by rememberUpdatedState(motion)
        val gestures = if (!interactive) Modifier else Modifier
            .pointerInput(math) {
                // Pinch may pass the zoom limits while the fingers are down; a pan carries on and decays; both settle on release.
                detectMapTransform(
                    onStart = { settle.cancel() },
                    onGesture = { centroid, pan, zoom ->
                        change(math.transform(current, ScreenPoint(centroid.x, centroid.y), pan.x, pan.y, zoom, soft = true))
                    },
                    onEnd = { velocity ->
                        val centre = ScreenPoint(math.viewW / 2f, math.viewH / 2f)
                        settle.start(scope) { settleViewport(current, velocity, math, centre, motionNow) { change(it) } }
                    },
                )
            }
            .pointerInput(math) {
                detectTapGestures { o ->
                    val s = ScreenPoint(o.x, o.y)
                    val p = math.toMap(current, s)
                    if (math.onBase(p)) tap(p, MapHitTest.hit(sceneNow, math, current, s, touchPx, terrain = hitTerrain))
                }
            }
        val markScope = MapMarkScope(math, resolved)
        // What is new to the scene since the last one fades in (the first scene of a screen does not animate).
        val sceneKeys = remember(scene) { scene.keys() }
        val previousKeys = remember { arrayOfNulls<Set<Key>>(1) }
        val fresh = remember(scene) { MapScenes.fresh(previousKeys[0], sceneKeys) }
        SideEffect { previousKeys[0] = sceneKeys }
        Box(Modifier.fillMaxSize().then(gestures)) {
            BaseLayer(base, math, resolved, blobs, Modifier.fillMaxSize())
            OverlayCanvas(scene, math, resolved, selectedKey, fresh, colors, Modifier.fillMaxSize(), extraDraw, markScope)
            OverlayMarks(scene, math, resolved, selectedKey, fresh, showRouteChips, unhideFresh)
            marks(markScope)
        }
    }
}

/** One drawn overlay's animated look: its alpha (dim ↔ full, and fading in when it is new) and its selection amount. */
private class ShapeLook(val alpha: State<Float>, val selected: State<Float>, val progress: State<Float>)

/**
 * The per-key looks of every drawn shape of [scene] (motion audit row 7): a shape fades in when it is new to the scene,
 * fades to its dim alpha and back, and eases its selected stroke in and out on `standard`. They are read in the draw lambda.
 */
@Composable
private fun rememberShapeLooks(scene: MapScene, fresh: Set<Key>, selectedKey: Key?): Map<Key, ShapeLook> {
    val out = HashMap<Key, ShapeLook>()
    @Composable
    fun look(key: Key, dim: Boolean, token: MotionToken = CivilightMotion.enter) {
        out[key] = ShapeLook(markAlpha(dim, MapMotion.SHAPE_DIM, key in fresh), animatedFraction(key == selectedKey, label = "shapeSel"), markProgress(key in fresh, token))
    }
    for (l in scene.lines) key(l.key) { look(l.key, false) }
    for (r in scene.regions) key(r.key) { look(r.key, r.dim) }
    for (l in scene.links) key(l.key) { look(l.key, l.dim) }
    // A new route draws itself along its length on `enter`; a new stamp drops and settles on `stampDrop` (240 ms).
    for (r in scene.routes) key(r.key) { look(r.key, r.dim) }
    for (st in scene.stamps) key(st.key) { look(st.key, st.dim, CivilightMotion.stampDrop) }
    return out
}

@Composable
private fun OverlayCanvas(
    scene: MapScene,
    math: ViewportMath,
    viewport: Viewport,
    selectedKey: Key?,
    fresh: Set<Key>,
    colors: CivilightColors,
    modifier: Modifier,
    extraDraw: DrawScope.(MapMarkScope) -> Unit,
    markScope: MapMarkScope,
) {
    val art = FixedColors.mapArt(if (colors.isDark) ResolvedTheme.DARK else ResolvedTheme.LIGHT)
    val measurer = rememberTextMeasurer()
    val labelStyle = CivilightTheme.type.label.merge(TextStyle(color = colors.ink))
    val river = FixedColors.terrain.getValue(io.github.lenticualrstone.civilight.designsystem.theme.Terrain.SEA)
    val looks = rememberShapeLooks(scene, fresh, selectedKey)
    Canvas(modifier) {
        val roadDash = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
        for (l in scene.lines) {
            val look = looks[l.key]
            val sel = look?.selected?.value ?: 0f
            val a = look?.alpha?.value ?: 1f
            val path = polyPath(l.points.map { math.toScreen(viewport, it) }, closed = false)
            val isRiver = l.kind == DrawnLineKind.RIVER
            val base = if (isRiver) river else colors.ink2.copy(alpha = 0.7f)
            drawPath(
                path,
                lerp(base, colors.acc, sel).let { it.copy(alpha = it.alpha * a) },
                style = Stroke(
                    ((if (isRiver) 1.6 else 1.2) + sel).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
                    pathEffect = if (isRiver) null else roadDash,
                ),
            )
        }
        for (r in scene.regions) {
            if (r.shape.size < 3) continue
            val look = looks[r.key]
            val sel = look?.selected?.value ?: 0f
            val k = look?.alpha?.value ?: 1f
            val c = regionColor(r.color, art)
            val path = polyPath(r.shape.map { math.toScreen(viewport, it) }, closed = true)
            drawPath(path, c.copy(alpha = 0.30f * k))
            drawPath(path, lerp(c.copy(alpha = 0.9f * k), colors.acc, sel), style = Stroke((1.5 + sel).dp.toPx(), join = StrokeJoin.Round))
        }
        for (l in scene.links) {
            val look = looks[l.key]
            val sel = look?.selected?.value ?: 0f
            val a = math.toScreen(viewport, l.a)
            val b = math.toScreen(viewport, l.b)
            drawLine(
                lerp(colors.ink3, colors.acc, sel).copy(alpha = look?.alpha?.value ?: 1f), Offset(a.x, a.y), Offset(b.x, b.y),
                (1.5 + sel).dp.toPx(), StrokeCap.Round,
            )
        }
        for (r in scene.routes) {
            val look = looks[r.key]
            val sel = look?.selected?.value ?: 0f
            val path = partOfPath(polyPath(r.points.map { math.toScreen(viewport, it) }, closed = false), look?.progress?.value ?: 1f)
            drawPath(path, colors.acc.copy(alpha = look?.alpha?.value ?: 1f), style = Stroke((2.0 + 1.5 * sel).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        for (st in scene.stamps) {
            val look = looks[st.key]
            val sel = look?.selected?.value ?: 0f
            val c0 = math.toScreen(viewport, st.at)
            val c = ScreenPoint(c0.x, c0.y - MapMotion.STAMP_DROP.toPx() * (1f - (look?.progress?.value ?: 1f)))
            val r = maxOf(StampGlyphs.radius(st.size) * math.contentW(viewport), (4 + 3 * st.size).dp.toPx())
            val color = lerp(colors.ink, colors.acc, sel).copy(alpha = 0.85f * (look?.alpha?.value ?: 1f))
            for (stroke in StampGlyphs.strokes(st.kind)) {
                val pts = StampGlyphs.place(stroke, c.x, c.y, r, st.rotation)
                val path = Path()
                pts.forEachIndexed { i, (x, y) -> if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
                drawPath(path, color, style = Stroke((1.4 + 0.6 * sel).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
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
private fun BoxScope.OverlayMarks(scene: MapScene, math: ViewportMath, viewport: Viewport, selectedKey: Key?, fresh: Set<Key>, showRouteChips: Boolean, unhideFresh: Boolean) {
    val density = LocalDensity.current
    val pinTip = with(density) { 12.dp.toPx() }
    val margin = with(density) { 160.dp.toPx() }
    fun onScreen(s: ScreenPoint) = s.x > -margin && s.y > -margin && s.x < math.viewW + margin && s.y < math.viewH + margin
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val motion = CivilightTheme.motion
    /** A mark's place in map coordinates glides when the data moves it; the screen position follows pan and zoom as it is. */
    @Composable
    fun at(p: Point): ScreenPoint {
        val o by animateOffsetAsState(Offset(p.x, p.y), motion.move(CivilightMotion.standard), label = "markAt")
        return math.toScreen(viewport, o.x, o.y)
    }
    /** The place of [key] among the fresh marks, for the stagger of an unhide batch. */
    val freshOrder = remember(fresh) { fresh.withIndex().associate { (n, k) -> k to n } }
    @Composable
    fun Modifier.freshIn(key: Key): Modifier = if (unhideFresh) unhideIn(key in fresh, freshOrder[key] ?: 0) else this
    Box(Modifier.matchParentSize()) {
        // The per-mark state is made above the cull check: a mark that is panned out of range and back keeps its own fade
        // and unhide, so only a mark that is new to the scene animates.
        for (l in scene.labels) {
            if (l.curve != null) continue
            key(l.key) {
                val s = at(l.at)
                val a by markAlpha(l.dim, MAP_DIM_ALPHA, l.key in fresh)
                val fresher = Modifier.freshIn(l.key)
                if (onScreen(s)) {
                    CivilightText(
                        l.text, type.label, Modifier.mapAt(s.x, s.y).graphicsLayer { alpha = a }.then(fresher),
                        color = if (l.key == selectedKey) colors.acc else colors.ink, maxLines = 1,
                    )
                }
            }
        }
        if (showRouteChips) {
            for (r in scene.routes) {
                key(r.key) {
                    val mid = midpoint(r.points.map { math.toScreen(viewport, it) })
                    val a by markAlpha(r.dim, MAP_DIM_ALPHA, r.key in fresh)
                    if (mid != null && onScreen(mid)) {
                        MapValueChip(r.label, Modifier.mapAt(mid.x, mid.y, ay = 1.2f).graphicsLayer { alpha = a })
                    }
                }
            }
        }
        // Connection labels (`계단 · 잠김`, `정문 통과`) at the middle of their line (WLD-50).
        for (l in scene.links) {
            val text = l.label ?: continue
            key(l.key) {
                val a = math.toScreen(viewport, l.a)
                val b = math.toScreen(viewport, l.b)
                val mx = (a.x + b.x) / 2f
                val my = (a.y + b.y) / 2f
                val alpha by markAlpha(l.dim, MAP_DIM_ALPHA, l.key in fresh)
                if (onScreen(ScreenPoint(mx, my))) {
                    CivilightText(
                        text, type.small, Modifier.mapAt(mx, my).graphicsLayer { this.alpha = alpha },
                        color = if (l.key == selectedKey) colors.acc else colors.ink2, maxLines = 1,
                    )
                }
            }
        }
        for (n in scene.nodes) {
            key(n.key) {
                val s = at(n.at)
                val a by markAlpha(n.dim, MAP_DIM_ALPHA, n.key in fresh)
                val fresher = Modifier.freshIn(n.key)
                if (onScreen(s)) {
                    NodeCard(n.name, Modifier.mapAt(s.x, s.y).graphicsLayer { alpha = a }.then(fresher), subtitle = n.sub, selected = n.key == selectedKey)
                }
            }
        }
        for (p in scene.pins) {
            key(p.key) {
                val s = at(p.at)
                // MapPin animates its own dim and selected look; only a new pin needs the fade in here.
                val a by markAlpha(false, 1f, p.key in fresh)
                val fresher = Modifier.freshIn(p.key)
                if (onScreen(s)) {
                    MapPin(p.name, Modifier.mapAt(s.x, s.y, ax = 0f, ay = 1f, dxPx = pinTip).graphicsLayer { alpha = a }.then(fresher), selected = p.key == selectedKey, dim = p.dim)
                }
            }
        }
    }
}

/** The first [fraction] of [path] by length (the whole path at 1): a route that draws itself. */
private fun partOfPath(path: Path, fraction: Float): Path {
    if (fraction >= 1f) return path
    val measure = PathMeasure()
    measure.setPath(path, false)
    val out = Path()
    measure.getSegment(0f, measure.length * fraction.coerceAtLeast(0f), out, true)
    return out
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
