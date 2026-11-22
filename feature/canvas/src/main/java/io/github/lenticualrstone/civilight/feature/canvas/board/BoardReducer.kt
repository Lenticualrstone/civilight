package io.github.lenticualrstone.civilight.feature.canvas.board

import io.github.lenticualrstone.civilight.feature.canvas.CanvasCopy
import io.github.lenticualrstone.civilight.feature.canvas.graph.GraphHit
import io.github.lenticualrstone.civilight.feature.canvas.graph.GraphSelection
import io.github.lenticualrstone.civilight.feature.canvas.graph.Pt

/**
 * The tool bar's tools (CNV-03), in bar order. Exactly one is current, and only the current tool acts
 * on taps on empty space ("고른 것만 동작"). `노드` places a 세력 · 사건 · 용어 item (Q-CV-02 default:
 * the main tab's node list minus 인물 카드, 메모 and 묶음, which have their own tools).
 */
enum class CanvasTool(val innerLabel: String, val coverLabel: String) {
    NODE(CanvasCopy.TOOL_NODE, CanvasCopy.TOOL_NODE),
    CHARACTER(CanvasCopy.TOOL_CHARACTER_INNER, CanvasCopy.TOOL_CHARACTER_COVER),
    MEMO(CanvasCopy.TOOL_MEMO, CanvasCopy.TOOL_MEMO),
    LINE(CanvasCopy.TOOL_LINE, CanvasCopy.TOOL_LINE),
    GROUP(CanvasCopy.TOOL_GROUP, CanvasCopy.TOOL_GROUP),
    ;

    fun label(cover: Boolean): String = if (cover) coverLabel else innerLabel
}

/**
 * Screen state of the canvas editor that is not in the document: the current tool, the selection and
 * the first end of a line being drawn. Saved with the screen (CNV-35) through [BoardUi.save].
 */
data class BoardUi(
    val tool: CanvasTool = CanvasTool.NODE,
    val selection: GraphSelection = GraphSelection.NONE,
    /** `선`: the node tapped first; the next node tapped ends the line. */
    val lineStart: String? = null,
) {
    fun save(): List<String?> = listOf(tool.name, selection.nodeKey, selection.lineKey, lineStart)

    companion object {
        fun restore(saved: List<String?>): BoardUi = BoardUi(
            tool = saved.getOrNull(0)?.let { n -> CanvasTool.entries.firstOrNull { it.name == n } } ?: CanvasTool.NODE,
            selection = GraphSelection(saved.getOrNull(1), saved.getOrNull(2)),
            lineStart = saved.getOrNull(3),
        )
    }
}

/** What a tap asks the screen to do next. */
sealed interface BoardAction {
    data object None : BoardAction

    /** Node edit (CNV-10) for a card or a 묶음. */
    data class EditNode(val nodeKey: String) : BoardAction

    /** The line's `선 라벨` surface (CNV-08). */
    data class EditLine(val edgeKey: String) : BoardAction

    /** The item picker of `노드` or `인물` (CNV-04), placing at [at]. */
    data class PickItem(val tool: CanvasTool, val at: Pt) : BoardAction

    data class AddMemo(val at: Pt) : BoardAction

    data class AddGroup(val at: Pt) : BoardAction

    /** Second tap of `선`: a line [from] → [to] (CNV-07). */
    data class DrawLine(val from: String, val to: String) : BoardAction

    /** A tap that would change the document in a read-only project (`읽기 전용`). */
    data object ReadOnly : BoardAction
}

/** Taps and tool changes of the canvas editor (CNV-03, CNV-04, CNV-07, CNV-10); pure. */
object BoardReducer {
    fun selectTool(ui: BoardUi, tool: CanvasTool): BoardUi = ui.copy(tool = tool, lineStart = null)

    fun clear(ui: BoardUi): BoardUi = ui.copy(selection = GraphSelection.NONE, lineStart = null)

    /** Selects the line made or found by `선` so its label surface belongs to it. */
    fun lineDrawn(ui: BoardUi, edgeKey: String): BoardUi = ui.copy(selection = GraphSelection(lineKey = edgeKey), lineStart = null)

    fun tap(ui: BoardUi, hit: GraphHit, at: Pt, readOnly: Boolean): Pair<BoardUi, BoardAction> = when (hit) {
        is GraphHit.Node -> tapNode(ui, hit.key, readOnly)
        is GraphHit.Frame ->
            if (ui.tool == CanvasTool.LINE) clear(ui) to BoardAction.None
            else ui.copy(selection = GraphSelection(nodeKey = hit.key), lineStart = null) to BoardAction.EditNode(hit.key)
        is GraphHit.Line -> ui.copy(selection = GraphSelection(lineKey = hit.key), lineStart = null) to BoardAction.EditLine(hit.key)
        GraphHit.Empty -> tapEmpty(ui, at, readOnly)
    }

    private fun tapNode(ui: BoardUi, key: String, readOnly: Boolean): Pair<BoardUi, BoardAction> {
        if (ui.tool != CanvasTool.LINE) return ui.copy(selection = GraphSelection(nodeKey = key), lineStart = null) to BoardAction.EditNode(key)
        val start = ui.lineStart
        return when {
            start == null -> ui.copy(selection = GraphSelection(nodeKey = key), lineStart = key) to BoardAction.None
            start == key -> clear(ui) to BoardAction.None
            readOnly -> clear(ui) to BoardAction.ReadOnly
            else -> clear(ui) to BoardAction.DrawLine(start, key)
        }
    }

    private fun tapEmpty(ui: BoardUi, at: Pt, readOnly: Boolean): Pair<BoardUi, BoardAction> {
        // The first tap away from a selection only lets it go.
        if (ui.selection != GraphSelection.NONE || ui.lineStart != null) return clear(ui) to BoardAction.None
        val action = when (ui.tool) {
            CanvasTool.NODE, CanvasTool.CHARACTER -> BoardAction.PickItem(ui.tool, at)
            CanvasTool.MEMO -> BoardAction.AddMemo(at)
            CanvasTool.GROUP -> BoardAction.AddGroup(at)
            CanvasTool.LINE -> BoardAction.None
        }
        return ui to if (readOnly && action != BoardAction.None) BoardAction.ReadOnly else action
    }
}
