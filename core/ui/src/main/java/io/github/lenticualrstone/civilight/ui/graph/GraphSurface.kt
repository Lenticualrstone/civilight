package io.github.lenticualrstone.civilight.ui.graph

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector3D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.Constraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.NodeCard
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.LocalMotion
import io.github.lenticualrstone.civilight.designsystem.theme.animatedColor
import kotlin.math.roundToInt

/** Callbacks of [GraphSurface]; positions are content dp. */
interface GraphSurfaceListener {
    /** A tap (no movement past the touch slop) on [hit] at [at]. */
    fun onTap(hit: GraphHit, at: Pt)

    /** Whether one-finger dragging may move this node (canvas: yes unless read-only). */
    fun canDrag(nodeKey: String): Boolean

    fun onDrag(nodeKey: String, delta: Pt)

    fun onDragEnd(nodeKey: String)

    /** Every frame of a pan or zoom. */
    fun onViewport(viewport: GraphViewport)

    /** The end of a pan or zoom (save it here, not per frame). */
    fun onViewportSettled(viewport: GraphViewport)
}

/**
 * The graph engine's surface (CNV-01): an unbounded plane drawn under the screen's glass bars. Two
 * fingers pan and pinch-zoom, one finger drags a node, a tap selects (the [listener] decides). Node
 * sizes are measured into [sizes] so hit tests, line ends and fitting use real card sizes.
 */
@Composable
fun GraphSurface(
    scene: GraphScene,
    viewport: GraphViewport,
    selection: GraphSelection,
    sizes: SnapshotStateMap<String, Sz>,
    listener: GraphSurfaceListener,
    cover: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val motion = LocalMotion.current
    val currentScene by rememberUpdatedState(scene)
    val currentListener by rememberUpdatedState(listener)
    // S14: the viewport drawn moves to a new [viewport] on `standard` (buttons, fit, focus, wheel); pans and
    // pinches follow the fingers (snap). Reduced: always a snap.
    val shown = remember { Animatable(viewport, GraphViewportConverter) }
    val pinch = remember { PinchMark() }
    LaunchedEffect(viewport) {
        if (viewport == pinch.last || shown.value == viewport) shown.snapTo(viewport)
        else shown.animateTo(viewport, motion.move(CivilightMotion.standard))
    }
    val presence = rememberNodePresence(scene)
    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .graphGestures(
                density = density,
                sceneOf = { currentScene },
                viewportOf = { shown.value },
                sizes = sizes,
                listenerOf = { currentListener },
                onPinch = { pinch.last = it },
            ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val v = shown.value
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = v.zoom
                    scaleY = v.zoom
                    translationX = v.offsetX * density
                    translationY = v.offsetY * density
                },
        ) {
            for (node in scene.nodes) {
                if (node.look == NodeLook.FRAME) key(node.key) { FrameNode(node, node.key == selection.nodeKey, listener, presence.appear(node.key)) }
            }
            LinesLayer(scene, sizes, selection, cover, presence)
            for (node in scene.nodes) {
                if (node.look != NodeLook.FRAME) key(node.key) { CardNode(node, node.key == selection.nodeKey, sizes, listener, presence.appear(node.key)) }
            }
            // Removed nodes fade out on `exit` where they were, without taking touches.
            for (node in presence.leaving) {
                key("leaving-" + node.key) { LeavingNode(node, sizes) { presence.gone(node.key) } }
            }
            LineLabels(scene, sizes, selection, listener)
        }
    }
}

/** The gesture's last viewport: a [GraphSurface] viewport equal to it came from the fingers (snap). */
private class PinchMark {
    var last: GraphViewport? = null
}

private val GraphViewportConverter = TwoWayConverter<GraphViewport, AnimationVector3D>(
    convertToVector = { AnimationVector3D(it.offsetX, it.offsetY, it.zoom) },
    convertFromVector = { GraphViewport(it.v1, it.v2, it.v3) },
)

/**
 * Which nodes and lines are new (they appear with a fade and an 8 dp rise on `enter`) and which just left
 * (they fade on `exit`). Everything present when the surface first draws is shown at once.
 */
@Stable
private class NodePresence(initial: GraphScene) {
    private var nodes: Map<String, GraphNode> = initial.nodesByKey
    private val seen = HashSet<String>().apply { addAll(initial.nodes.map { it.key }); addAll(initial.lines.map { it.key }) }
    private val fresh = HashSet<String>()
    val leaving = mutableStateListOf<GraphNode>()

    fun update(scene: GraphScene) {
        val now = scene.nodesByKey
        if (now === nodes) return
        for ((k, n) in nodes) if (k !in now && leaving.none { it.key == k }) leaving.add(n)
        leaving.removeAll { it.key in now }
        for (k in now.keys) if (seen.add(k)) fresh.add(k)
        for (l in scene.lines) if (seen.add(l.key)) fresh.add(l.key)
        nodes = now
    }

    /** True once for a node or line that arrived after the first draw. */
    fun appear(key: String): Boolean = fresh.remove(key)

    fun gone(key: String) {
        leaving.removeAll { it.key == key }
        seen.remove(key)
    }
}

@Composable
private fun rememberNodePresence(scene: GraphScene): NodePresence {
    val presence = remember { NodePresence(scene) }
    presence.update(scene)
    return presence
}

/** 0 → 1 on `enter` for a node that just arrived ([animate]); 1 at once otherwise. */
@Composable
private fun rememberAppear(animate: Boolean): Animatable<Float, AnimationVector1D> {
    val motion = LocalMotion.current
    val a = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) { if (a.value < 1f) a.animateTo(1f, motion.spec(CivilightMotion.enter)) }
    return a
}

/** The appear look: alpha, plus (full motion only) an 8 dp rise. */
private fun Modifier.appearLayer(a: Animatable<Float, AnimationVector1D>, reduced: Boolean): Modifier = graphicsLayer {
    val p = a.value
    alpha = p
    translationY = if (reduced) 0f else (1f - p) * NODE_RISE.toPx()
}

private val NODE_RISE = 8.dp

@Composable
private fun LeavingNode(node: GraphNode, sizes: Map<String, Sz>, onGone: () -> Unit) {
    val motion = LocalMotion.current
    val a = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        a.animateTo(0f, motion.spec(CivilightMotion.exit))
        onGone()
    }
    val m = Modifier.graphicsLayer { alpha = a.value }.clearAndSetSemantics { }
    if (node.look == NodeLook.FRAME) {
        val size = node.frameSize ?: Sz.DEFAULT
        Box(Modifier.placeAt(node.topLeft).size(size.w.dp, size.h.dp).then(m).clip(RoundedCornerShape(18.dp)).background(CivilightTheme.colors.tint))
    } else {
        NodeCard(
            title = node.title,
            subtitle = node.subtitle,
            memo = node.look == NodeLook.MEMO,
            selected = false,
            deleted = node.look == NodeLook.DOTTED,
            modifier = Modifier.placeAt(node.topLeft).widthIn(max = CARD_MAX_WIDTH.dp).then(m),
        )
    }
}

/** Widest a card grows before its text wraps (a long memo stays a card, not one endless line). */
const val CARD_MAX_WIDTH = 240f

/** Places a child with its top-left at [at] (content dp) without growing the parent. */
private fun Modifier.placeAt(at: Pt): Modifier = layout { measurable, _ ->
    val p = measurable.measure(Constraints(maxWidth = Constraints.Infinity, maxHeight = Constraints.Infinity))
    layout(0, 0) { p.place((at.x * density).roundToInt(), (at.y * density).roundToInt()) }
}

/** Places a child centred on [at] (content dp). */
private fun Modifier.centerAt(at: Pt): Modifier = layout { measurable, _ ->
    val p = measurable.measure(Constraints(maxWidth = Constraints.Infinity, maxHeight = Constraints.Infinity))
    layout(0, 0) { p.place((at.x * density).roundToInt() - p.width / 2, (at.y * density).roundToInt() - p.height / 2) }
}

@Composable
private fun CardNode(node: GraphNode, selected: Boolean, sizes: SnapshotStateMap<String, Sz>, listener: GraphSurfaceListener, appear: Boolean) {
    val density = LocalDensity.current.density
    val shown = rememberAppear(appear)
    val reduced = LocalMotion.current.reduced
    val described = listOfNotNull(node.title, node.subtitle).joinToString(" · ")
    NodeCard(
        title = node.title,
        subtitle = node.subtitle,
        memo = node.look == NodeLook.MEMO,
        selected = selected,
        deleted = node.look == NodeLook.DOTTED,
        modifier = Modifier
            .placeAt(node.topLeft)
            .appearLayer(shown, reduced)
            .widthIn(max = CARD_MAX_WIDTH.dp)
            .onSizeChanged { s ->
                val measured = Sz(s.width / density, s.height / density)
                if (sizes[node.key] != measured) sizes[node.key] = measured
            }
            .semantics {
                contentDescription = described
                onClick { listener.onTap(GraphHit.Node(node.key), node.topLeft); true }
            },
    )
}

@Composable
private fun FrameNode(node: GraphNode, selected: Boolean, listener: GraphSurfaceListener, appear: Boolean) {
    val colors = CivilightTheme.colors
    val size = node.frameSize ?: Sz.DEFAULT
    val shape = RoundedCornerShape(18.dp)
    val shown = rememberAppear(appear)
    val reduced = LocalMotion.current.reduced
    val outline by animatedColor(if (selected) colors.acc else colors.line, label = "frameOutline")
    Box(
        Modifier
            .placeAt(node.topLeft)
            .appearLayer(shown, reduced)
            .size(size.w.dp, size.h.dp)
            .clip(shape)
            .background(colors.tint)
            .insetOutline(outline, if (selected) 1.5.dp else 1.dp, shape)
            .semantics {
                contentDescription = node.title
                onClick { listener.onTap(GraphHit.Frame(node.key), node.topLeft); true }
            },
    ) {
        CivilightText(node.title, CivilightTheme.type.label, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = colors.ink2, maxLines = 1)
    }
}

@Composable
private fun LinesLayer(scene: GraphScene, sizes: Map<String, Sz>, selection: GraphSelection, cover: Boolean, presence: NodePresence) {
    val colors = CivilightTheme.colors
    val motion = LocalMotion.current
    // New lines fade in on `enter`; the rest draw at full strength.
    val alphas = remember { HashMap<String, Animatable<Float, AnimationVector1D>>() }
    for (line in scene.lines) {
        if (presence.appear(line.key)) alphas[line.key] = Animatable(0f)
    }
    alphas.keys.retainAll(scene.lines.mapTo(HashSet()) { it.key })
    for ((k, a) in alphas) {
        key(k) { LaunchedEffect(a) { a.animateTo(1f, motion.spec(CivilightMotion.enter)) } }
    }
    Box(
        Modifier
            .size(1.dp)
            .drawBehind {
                for (line in scene.lines) {
                    val a = scene.nodesByKey[line.from] ?: continue
                    val b = scene.nodesByKey[line.to] ?: continue
                    val ca = GraphGeometry.rectOf(a, sizes).center
                    val cb = GraphGeometry.rectOf(b, sizes).center
                    val stroke = GraphGeometry.stroke(line, line.key == selection.lineKey, cover)
                    drawLine(
                        color = colors.of(stroke.color),
                        alpha = alphas[line.key]?.value ?: 1f,
                        start = Offset(ca.x.dp.toPx(), ca.y.dp.toPx()),
                        end = Offset(cb.x.dp.toPx(), cb.y.dp.toPx()),
                        strokeWidth = stroke.widthDp.dp.toPx(),
                        pathEffect = if (stroke.dashed) {
                            PathEffect.dashPathEffect(floatArrayOf(GraphGeometry.DASH.dp.toPx(), GraphGeometry.GAP.dp.toPx()))
                        } else {
                            null
                        },
                    )
                }
            },
    )
}

private val LabelStyle = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)

@Composable
private fun LineLabels(scene: GraphScene, sizes: Map<String, Sz>, selection: GraphSelection, listener: GraphSurfaceListener) {
    for (line in scene.lines) {
        val a = scene.nodesByKey[line.from] ?: continue
        val b = scene.nodesByKey[line.to] ?: continue
        val ra = GraphGeometry.rectOf(a, sizes)
        val rb = GraphGeometry.rectOf(b, sizes)
        val visible = GraphGeometry.visibleSegment(ra, rb)
        val selected = line.key == selection.lineKey
        val tap = { listener.onTap(GraphHit.Line(line.key), GraphGeometry.middleAnchor(visible)) }
        line.middle?.let { LabelPill(GraphGeometry.middleText(it, ra.center, rb.center), GraphGeometry.middleAnchor(visible), selected, tap) }
        for (end in line.ends) LabelPill(GraphGeometry.endText(end, ra.center, rb.center), GraphGeometry.endAnchor(end, visible), selected, tap)
        if (line.middle == null && line.ends.isEmpty()) {
            // An unlabelled line still has a tap target for accessibility services.
            Box(
                Modifier
                    .centerAt(GraphGeometry.middleAnchor(visible))
                    .size(1.dp)
                    .semantics { onClick { tap(); true } },
            )
        }
    }
}

/** Label pill: `bg` fill, 1 dp `line` inset, radius 10, padding 2 × 8, 500 11 / 16 `ink2` (`acc` when selected). */
@Composable
private fun LabelPill(text: String, at: Pt, selected: Boolean, onTap: () -> Unit) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .centerAt(at)
            .clip(shape)
            .background(colors.bg)
            .insetOutline(colors.line, 1.dp, shape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics {
                contentDescription = text
                onClick { onTap(); true }
            },
    ) {
        val ink by animatedColor(if (selected) colors.acc else colors.ink2, label = "labelInk")
        CivilightText(text, CivilightTheme.type.small.merge(LabelStyle), color = ink, maxLines = 1)
    }
}
