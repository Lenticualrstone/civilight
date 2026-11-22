package io.github.lenticualrstone.civilight.feature.map.node

import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.domain.world.map.MapGeometry
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapHit
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScene
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The node tool set `이동 · 노드 · 연결 · 라벨 · 레이어` (WLD-40, WLD-50). Only the chosen tool acts. */
enum class NodeTool(val label: String, val icon: IconId) {
    MOVE(MapCopy.TOOL_MOVE, IconId.Move),
    NODE(MapCopy.TOOL_NODE, IconId.Node),
    LINK(MapCopy.TOOL_LINK, IconId.Line),
    LABEL(MapCopy.TOOL_LABEL, IconId.Label),
    LAYER(MapCopy.TOOL_LAYER, IconId.Layers),
}

/** What a tap asks the screen to open. */
enum class NodeEffect {
    /** Name (and sub line, place) of a new node at the tapped spot. */
    NEW_NODE,

    /** Text of a new label at the tapped spot. */
    LABEL_TEXT,
}

/** Edits a tap commits at once (one undo step each). */
sealed interface NodeCommit {
    data class Link(val a: Key, val b: Key) : NodeCommit
    data class MoveNode(val nodeKey: Key, val to: Point) : NodeCommit
}

data class NodeTapOutcome(val state: NodeEditorState, val effect: NodeEffect? = null, val commit: NodeCommit? = null)

/**
 * 노드 지도 editor state (WLD-50), saveable as JSON so a fold keeps a half-made connection: the tool,
 * the selection (node, connection or label), the first node of a connection being made, the node
 * being moved (`이동` on its card, then a tap places it) and the tapped spot waiting for a name.
 */
@Serializable
data class NodeEditorState(
    val tool: NodeTool = NodeTool.MOVE,
    val selected: Key? = null,
    val linkFrom: Key? = null,
    val movingNode: Key? = null,
    val pendingAt: Point? = null,
    /** Inner side panel tab: 0 `속성`, 1 `레이어`, 2 `목록`. */
    val panelTab: Int = 0,
) {
    fun encode(): String = JSON.encodeToString(serializer(), this)

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }

        fun decode(text: String?): NodeEditorState = text?.let { runCatching { JSON.decodeFromString(serializer(), it) }.getOrNull() } ?: NodeEditorState()
    }
}

/** The reducer; pure. No hidden gestures: a connection is two taps with the `연결` tool. */
object NodeEditor {
    /** Tap tolerance for a connection line, in map units. */
    const val LINK_TOLERANCE = 0.02

    fun chooseTool(s: NodeEditorState, tool: NodeTool): NodeEditorState = s.copy(
        tool = tool, linkFrom = null, movingNode = null, pendingAt = null,
        panelTab = if (tool == NodeTool.LAYER) 1 else if (s.panelTab == 1) 0 else s.panelTab,
    )

    /** The connection under [p] (within [LINK_TOLERANCE] of its line), nearest first. */
    fun linkAt(scene: MapScene, p: Point, tolerance: Double = LINK_TOLERANCE): Key? =
        scene.links.map { it to MapGeometry.segmentDistance(p, it.a, it.b) }.filter { it.second <= tolerance }.minByOrNull { it.second }?.first?.key

    /**
     * A tap at [p] that hit [hit] (nodes and labels by their 44 dp targets); [linkKey] is the
     * connection under the tap, if any.
     */
    fun tap(s: NodeEditorState, p: Point, hit: MapHit?, linkKey: Key?): NodeTapOutcome {
        val moving = s.movingNode
        if (moving != null) return NodeTapOutcome(s.copy(movingNode = null, selected = moving), commit = NodeCommit.MoveNode(moving, p))
        val node = (hit as? MapHit.Node)?.key
        val label = (hit as? MapHit.Label)?.key
        return when (s.tool) {
            NodeTool.MOVE, NodeTool.LAYER -> NodeTapOutcome(s.copy(selected = node ?: label ?: linkKey, panelTab = if (node ?: label ?: linkKey != null) 0 else s.panelTab))
            NodeTool.NODE -> if (node != null) NodeTapOutcome(s.copy(selected = node, panelTab = 0)) else NodeTapOutcome(s.copy(pendingAt = p), NodeEffect.NEW_NODE)
            NodeTool.LINK -> when {
                node == null -> NodeTapOutcome(s)
                s.linkFrom == null -> NodeTapOutcome(s.copy(linkFrom = node, selected = node))
                s.linkFrom == node -> NodeTapOutcome(s.copy(linkFrom = null))
                else -> NodeTapOutcome(s.copy(linkFrom = null), commit = NodeCommit.Link(s.linkFrom, node))
            }
            NodeTool.LABEL -> if (label != null) NodeTapOutcome(s.copy(selected = label, panelTab = 0)) else NodeTapOutcome(s.copy(pendingAt = p), NodeEffect.LABEL_TEXT)
        }
    }

    fun cancelLink(s: NodeEditorState): NodeEditorState = s.copy(linkFrom = null)

    /** `연결` on a node's card: the `연결` tool with that node as the first end; the next node tapped ends it. */
    fun startLink(s: NodeEditorState, nodeKey: Key): NodeEditorState =
        s.copy(tool = NodeTool.LINK, linkFrom = nodeKey, selected = nodeKey, movingNode = null, pendingAt = null)

    fun startMove(s: NodeEditorState, nodeKey: Key): NodeEditorState = s.copy(movingNode = nodeKey, selected = nodeKey)

    fun cancelMove(s: NodeEditorState): NodeEditorState = s.copy(movingNode = null)

    fun placed(s: NodeEditorState, key: Key?): NodeEditorState = s.copy(pendingAt = null, selected = key ?: s.selected)

    fun select(s: NodeEditorState, key: Key?): NodeEditorState = s.copy(selected = key, panelTab = 0)
}
