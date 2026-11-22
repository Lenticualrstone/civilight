package io.github.lenticualrstone.civilight.feature.canvas.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.Constraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
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
    val currentScene by rememberUpdatedState(scene)
    val currentViewport by rememberUpdatedState(viewport)
    val currentListener by rememberUpdatedState(listener)
    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .graphGestures(
                density = density,
                sceneOf = { currentScene },
                viewportOf = { currentViewport },
                sizes = sizes,
                listenerOf = { currentListener },
            ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = viewport.zoom
                    scaleY = viewport.zoom
                    translationX = viewport.offsetX * density
                    translationY = viewport.offsetY * density
                },
        ) {
            for (node in scene.nodes) if (node.look == NodeLook.FRAME) FrameNode(node, node.key == selection.nodeKey, listener)
            LinesLayer(scene, sizes, selection, cover)
            for (node in scene.nodes) if (node.look != NodeLook.FRAME) CardNode(node, node.key == selection.nodeKey, sizes, listener)
            LineLabels(scene, sizes, selection, listener)
        }
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
private fun CardNode(node: GraphNode, selected: Boolean, sizes: SnapshotStateMap<String, Sz>, listener: GraphSurfaceListener) {
    val density = LocalDensity.current.density
    val described = listOfNotNull(node.title, node.subtitle).joinToString(" · ")
    NodeCard(
        title = node.title,
        subtitle = node.subtitle,
        memo = node.look == NodeLook.MEMO || node.look == NodeLook.DOTTED,
        selected = selected,
        modifier = Modifier
            .placeAt(node.topLeft)
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
private fun FrameNode(node: GraphNode, selected: Boolean, listener: GraphSurfaceListener) {
    val colors = CivilightTheme.colors
    val size = node.frameSize ?: Sz.DEFAULT
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .placeAt(node.topLeft)
            .size(size.w.dp, size.h.dp)
            .clip(shape)
            .background(colors.tint)
            .insetOutline(if (selected) colors.acc else colors.line, if (selected) 1.5.dp else 1.dp, shape)
            .semantics {
                contentDescription = node.title
                onClick { listener.onTap(GraphHit.Frame(node.key), node.topLeft); true }
            },
    ) {
        CivilightText(node.title, CivilightTheme.type.label, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = colors.ink2, maxLines = 1)
    }
}

@Composable
private fun LinesLayer(scene: GraphScene, sizes: Map<String, Sz>, selection: GraphSelection, cover: Boolean) {
    val colors = CivilightTheme.colors
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
        CivilightText(text, CivilightTheme.type.small.merge(LabelStyle), color = if (selected) colors.acc else colors.ink2, maxLines = 1)
    }
}
