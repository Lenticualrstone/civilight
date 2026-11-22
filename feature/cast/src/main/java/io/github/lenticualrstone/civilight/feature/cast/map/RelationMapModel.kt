package io.github.lenticualrstone.civilight.feature.cast.map

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.map.LayoutConfig
import io.github.lenticualrstone.civilight.domain.cast.map.LineStyle
import io.github.lenticualrstone.civilight.domain.cast.map.MapFilter
import io.github.lenticualrstone.civilight.domain.cast.map.MapLayout
import io.github.lenticualrstone.civilight.domain.cast.map.MapPositions
import io.github.lenticualrstone.civilight.domain.cast.map.MapSlotFilter
import io.github.lenticualrstone.civilight.domain.cast.map.RelationMap
import io.github.lenticualrstone.civilight.domain.cast.map.RelationMapView
import io.github.lenticualrstone.civilight.domain.cast.map.SelectedRelationCard
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationKinds
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.castLens
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.graph.EndWord
import io.github.lenticualrstone.civilight.ui.graph.GraphLine
import io.github.lenticualrstone.civilight.ui.graph.GraphNode
import io.github.lenticualrstone.civilight.ui.graph.GraphScene
import io.github.lenticualrstone.civilight.ui.graph.LineStroke
import io.github.lenticualrstone.civilight.ui.graph.MiddleWord
import io.github.lenticualrstone.civilight.ui.graph.Pt
import io.github.lenticualrstone.civilight.ui.graph.Sz
import kotlinx.serialization.Serializable

/** What the user chose on the relationship map (saved with the screen as JSON). */
@Serializable
data class MapUi(
    val slots: MapSlotFilter = MapSlotFilter.BOTH,
    /** Kind chips; empty = every kind. */
    val kindKeys: Set<Key> = emptySet(),
    /** Group chip by tag; null = `전체`. */
    val tagKey: Key? = null,
    val selectedRelation: Key? = null,
    /** The focused character (`관계도로 보기` from a record) or the line tool's first end. */
    val selectedNode: Key? = null,
    /** The `선` tool: tap one character, then another, to draw a line (CST-34). */
    val lineTool: Boolean = false,
)

/** A tag chip at the bottom left (`전체 · 등불 결사 · 하층 구역`). */
data class MapTagChip(val tagKey: Key?, val label: String, val selected: Boolean)

/** A kind chip of the map filter. */
data class MapKindChip(val kindKey: Key, val label: String, val selected: Boolean)

/** The relationship map at one moment (CST-33; `CastMap`, `CastMapC`). */
data class RelationMapState(
    val view: RelationMapView,
    /** `관계도` (cover) / `전체 관계도` (inner). */
    val title: String,
    /** `인물 · 11명 중 4명` (cover) / `인물 · 관계도로 보기 · 11명 중 5명` (inner). */
    val smallLine: String,
    val scene: GraphScene,
    val tags: List<MapTagChip>,
    val kinds: List<MapKindChip>,
    val selected: SelectedRelationCard?,
    /** Any filter other than the defaults (`둘 다`, every kind). */
    val filtered: Boolean,
)

/** What a tap on a node does. */
sealed interface NodeTap {
    /** Open the character's record ([PROPOSED] in CST-33). */
    data class OpenRecord(val characterKey: Key) : NodeTap

    /** The line tool's first end was chosen. */
    data object LineStarted : NodeTap

    /** The line tool's second end: open the line-label sheet for [fromKey] → [toKey] (CST-34). */
    data class DrawLine(val fromKey: Key, val toKey: Key) : NodeTap
}

/**
 * Pure model of the full relationship map (CST-33) over `domain:cast` [RelationMap] and
 * [MapLayout]: graph scene for the shared engine (`core:ui` `ui.graph`), header, chips, the
 * `고른 관계` card and the node and line taps. Positions: saved ones ([MapPositions]) win, the rest
 * are laid out deterministically; a node being dragged comes from [moved] (content dp, top-left).
 */
object RelationMapModel {
    /** The layout area in content dp (positions are saved as 0–1 of it). */
    const val AREA = 800.0
    private val NODE = Sz.DEFAULT

    fun build(
        doc: ProjectDoc,
        ui: MapUi,
        hiddenUnlocked: Boolean,
        inner: Boolean,
        moved: Map<Key, Pt> = emptyMap(),
    ): RelationMapState {
        val lens = doc.castLens
        val filter = MapFilter(ui.slots, ui.kindKeys, ui.tagKey)
        val view = RelationMap.build(doc, lens, filter, hiddenUnlocked, ui.selectedRelation)
        val tags = CharacterEdits.tags(doc)
        return RelationMapState(
            view = view,
            title = if (inner) RelationMap.TITLE_INNER else RelationMap.TITLE_COVER,
            smallLine = if (inner) innerHeader(view.header) else view.header,
            scene = scene(view, MapPositions.saved(doc), moved),
            tags = listOf(MapTagChip(null, CastUiCopy.ALL, ui.tagKey == null)) +
                tags.map { MapTagChip(it.key, it.name, it.key == ui.tagKey) },
            kinds = RelationKinds.ordered(doc).map { MapKindChip(it.key, it.name, it.key in ui.kindKeys) },
            selected = ui.selectedRelation
                ?.takeIf { key -> view.lines.any { it.relationKey == key } }
                ?.let { RelationMap.selected(doc, it, lens) }
                ?.let { if (inner) it else coverCard(it) },
            filtered = ui.slots != MapSlotFilter.BOTH || ui.kindKeys.isNotEmpty(),
        )
    }

    /** `인물 · 11명 중 5명` → `인물 · 관계도로 보기 · 11명 중 5명` (`CastMap`). */
    fun innerHeader(header: String): String {
        val head = "인물${CastCopy.SEP}"
        return if (header.startsWith(head)) head + CastCopy.join(CastUiCopy.MAP_SUB, header.removePrefix(head)) else header
    }

    /** The cover sheet abbreviates `지금 시점보다 뒤` to `뒤` (`CastMapC`). */
    fun coverCard(card: SelectedRelationCard): SelectedRelationCard =
        card.copy(steps = card.steps.map { (point, text) -> point to text.replace(CastCopy.AFTER_LENS, CastUiCopy.AFTER_LENS_SHORT) })

    /** A step after the lens (`지금 시점보다 뒤`, `뒤`, `예정`) is drawn in `ink3`. */
    fun isAfterLens(text: String): Boolean =
        text.endsWith(CastCopy.AFTER_LENS) || text.endsWith("${CastCopy.SEP}${CastUiCopy.AFTER_LENS_SHORT}") || text.contains(CastCopy.PLANNED)

    /** The graph scene: rectangular cards (name + subtitle) and lines (solid `사이`, dashed minds). */
    fun scene(view: RelationMapView, saved: Map<Key, Point>, moved: Map<Key, Pt> = emptyMap()): GraphScene {
        val config = LayoutConfig(width = AREA, height = AREA, nodeWidth = NODE.w.toDouble(), nodeHeight = NODE.h.toDouble())
        val centers = MapLayout.positions(view.nodes.map { it.key }, view.lines.map { it.endA to it.endB }, saved, config)
        val nodes = view.nodes.map { n ->
            val c = centers.getValue(n.key)
            GraphNode(
                key = n.key,
                title = n.name,
                subtitle = n.subtitle.ifEmpty { null },
                topLeft = moved[n.key] ?: Pt(c.x.toFloat() - NODE.w / 2f, c.y.toFloat() - NODE.h / 2f),
            )
        }
        val lines = view.lines.map { l ->
            GraphLine(
                key = l.relationKey,
                from = l.endA,
                to = l.endB,
                stroke = if (l.style == LineStyle.SOLID) LineStroke.SOLID else LineStroke.DASHED,
                middle = l.middle?.let { m -> if (m.symmetric) MiddleWord(m.roleOfA) else MiddleWord(m.roleOfA, m.roleOfB) },
                ends = listOfNotNull(l.mindA?.let { EndWord(it.kind, nearFrom = true) }, l.mindB?.let { EndWord(it.kind, nearFrom = false) }),
            )
        }
        return GraphScene(nodes, lines)
    }

    /** A dragged node's place to save: its centre in 0–1 of the layout area. */
    fun normalized(topLeft: Pt): Point = Point(
        ((topLeft.x + NODE.w / 2f) / AREA.toFloat()).coerceIn(0f, 1f),
        ((topLeft.y + NODE.h / 2f) / AREA.toFloat()).coerceIn(0f, 1f),
    )

    /**
     * Where a node dragged to [topLeft] comes to rest: the saved place clamps its centre into the layout
     * area, so a node let go outside settles back in (the screen animates from [topLeft] to this).
     */
    fun settled(topLeft: Pt): Pt {
        val p = normalized(topLeft)
        return Pt(p.x * AREA.toFloat() - NODE.w / 2f, p.y * AREA.toFloat() - NODE.h / 2f)
    }

    /**
     * What a finished drag saves: every shown node at its place now (the dragged one at [moved]),
     * so pinning one node does not re-settle the others (the layout only moves unsaved nodes).
     */
    fun positionsToSave(scene: GraphScene, moved: Map<Key, Pt>): Map<Key, Point> =
        scene.nodes.associate { n -> n.key to normalized(moved[n.key] ?: n.topLeft) }

    /** A node tap: the record, or with the line tool the first then the second end. */
    fun tapNode(ui: MapUi, key: Key): Pair<MapUi, NodeTap> {
        if (!ui.lineTool) return ui to NodeTap.OpenRecord(key)
        val from = ui.selectedNode
        return when {
            from == null || from == key -> ui.copy(selectedNode = key, selectedRelation = null) to NodeTap.LineStarted
            else -> ui.copy(selectedNode = null) to NodeTap.DrawLine(from, key)
        }
    }

    /**
     * A line tap selects its relation (`고른 관계`). On the inner screen tapping it again lets go of
     * the aside; on the cover it always selects (the sheet opens again after it was dismissed).
     */
    fun tapLine(ui: MapUi, relationKey: Key, inner: Boolean = true): MapUi =
        ui.copy(selectedRelation = if (inner && ui.selectedRelation == relationKey) null else relationKey, selectedNode = null)

    /** Empty space lets go of the selection. */
    fun tapEmpty(ui: MapUi): MapUi = ui.copy(selectedRelation = null, selectedNode = if (ui.lineTool) null else ui.selectedNode)

    fun toggleLineTool(ui: MapUi): MapUi = ui.copy(lineTool = !ui.lineTool, selectedNode = null)

    fun toggleKind(ui: MapUi, kindKey: Key): MapUi =
        ui.copy(kindKeys = if (kindKey in ui.kindKeys) ui.kindKeys - kindKey else ui.kindKeys + kindKey)

    /**
     * Which character `인물 기록` opens and from whose side `관계 고치기` edits: the focused character
     * when it is one of the pair, else the relation's first end.
     */
    fun viewerOf(doc: ProjectDoc, relationKey: Key, focus: Key?): Pair<Key, Key>? {
        val r = doc.index.relation(relationKey) ?: return null
        return if (focus == r.endB.key) r.endB.key to r.endA.key else r.endA.key to r.endB.key
    }
}
