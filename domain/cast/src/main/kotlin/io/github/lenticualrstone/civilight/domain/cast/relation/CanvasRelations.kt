package io.github.lenticualrstone.civilight.domain.cast.relation

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.StoryLabels
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.canvas.CanvasEdge
import io.github.lenticualrstone.civilight.model.canvas.CanvasNode
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.model.cast.RelationCategory
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint

/** `무엇으로 적을지` choice of the line-label sheet. */
enum class LineSlot { BETWEEN, FROM_MIND, TO_MIND }

/** The line-label sheet (`CanvasCLabel`, CST-34). */
data class LineLabelSheet(
    val title: String,
    /** `리엔 · 하르트`. */
    val sub: String,
    /** `사이 · 리엔의 마음 · 하르트의 마음`; empty when the line cannot become a relation. */
    val slotChips: List<String>,
    /** The drawn direction preselects `<drawn-from>의 마음`. */
    val preselected: LineSlot?,
    val kindChips: List<String>,
    /** `시작 시점 | 1-7`. */
    val startPoint: String,
    val startUnitKey: Key?,
    /** Only character–character lines have `관계로 만들기`. */
    val canMakeRelation: Boolean,
    val keepLabel: String,
    val makeLabel: String?,
)

/**
 * Canvas and relationship-map lines become relations only by `관계로 만들기` (rule 13 §7,
 * CST-34). Picking chips alone records nothing; `그냥 선으로 두기` changes nothing.
 */
object CanvasRelations {
    const val TITLE = "선 라벨"
    const val KEEP = "그냥 선으로 두기"
    const val MAKE = "관계로 만들기"

    private fun nodes(doc: ProjectDoc, canvasKey: Key, edge: CanvasEdge): Pair<CanvasNode, CanvasNode>? {
        val canvas = doc.canvases[canvasKey] ?: return null
        val from = canvas.nodes.firstOrNull { it.key == edge.fromNodeKey } ?: return null
        val to = canvas.nodes.firstOrNull { it.key == edge.toNodeKey } ?: return null
        return from to to
    }

    private fun isCharacter(node: CanvasNode) = node.kind.known == CanvasNodeKind.CHARACTER && node.refKey != null

    private fun nodeName(doc: ProjectDoc, node: CanvasNode): String = when (node.kind.known) {
        CanvasNodeKind.CHARACTER -> node.refKey?.let { doc.index.character(it)?.name }
        CanvasNodeKind.FACTION -> node.refKey?.let { doc.index.faction(it)?.name }
        CanvasNodeKind.TERM -> node.refKey?.let { doc.index.term(it)?.name }
        CanvasNodeKind.EVENT -> node.refKey?.let { doc.index.event(it)?.title }
        else -> node.text
    }.orEmpty()

    /** The unit a new relation starts from: the lens episode (`최신` = the 집필 선 episode). */
    fun startUnit(doc: ProjectDoc, lens: LensPoint): Key? = when (lens) {
        LensPoint.Latest -> doc.manuscript.writingLineSceneKey?.let { StoryLabels.episodeOf(doc, it)?.key }
        is LensPoint.UnitEnd -> StoryLabels.episodeOf(doc, lens.unitKey)?.key
    }

    fun sheet(doc: ProjectDoc, canvasKey: Key, edgeKey: Key, lens: LensPoint): LineLabelSheet? {
        val edge = doc.canvases[canvasKey]?.edges?.firstOrNull { it.key == edgeKey } ?: return null
        val (from, to) = nodes(doc, canvasKey, edge) ?: return null
        val fromName = nodeName(doc, from)
        val toName = nodeName(doc, to)
        val relational = isCharacter(from) && isCharacter(to) && from.refKey != to.refKey
        val unit = startUnit(doc, lens)
        return LineLabelSheet(
            title = TITLE,
            sub = "$fromName · $toName",
            slotChips = if (relational) listOf("사이", "${fromName}의 마음", "${toName}의 마음") else emptyList(),
            preselected = if (relational) LineSlot.FROM_MIND else null,
            kindChips = if (relational) RelationKinds.chips(doc, RelationCategory.MIND) else emptyList(),
            startPoint = unit?.let { StoryLabels.unitCode(doc, it) } ?: StoryLabels.lensPoint(doc, lens),
            startUnitKey = unit,
            canMakeRelation = relational,
            keepLabel = KEEP,
            makeLabel = if (relational) MAKE else null,
        )
    }

    /** `그냥 선으로 두기`: only the label text is kept; no relation and no record. */
    fun keepAsLine(doc: ProjectDoc, canvasKey: Key, edgeKey: Key, label: String?, ctx: EditContext): ProjectDoc =
        updateEdge(doc, canvasKey, edgeKey, ctx) { it.copy(labelText = label?.takeIf { s -> s.isNotBlank() }) }

    /**
     * `관계로 만들기`: opens (or creates) the pair's relation, records the chosen slot from the start of
     * [startUnitKey] (a point anchor), and links the canvas line to the relation.
     */
    fun makeRelation(
        doc: ProjectDoc,
        canvasKey: Key,
        edgeKey: Key,
        slot: LineSlot,
        kindKey: Key,
        startUnitKey: Key,
        ctx: EditContext,
        label: String? = null,
        description: String? = null,
    ): ProjectDoc {
        val edge = requireNotNull(doc.canvases[canvasKey]?.edges?.firstOrNull { it.key == edgeKey }) { "no line $edgeKey" }
        val (from, to) = requireNotNull(nodes(doc, canvasKey, edge))
        require(isCharacter(from) && isCharacter(to)) { "only character lines become relations" }
        val fromKey = from.refKey!!
        val toKey = to.refKey!!
        val (withRelation, relation) = RelationEdits.openOrCreate(doc, fromKey, toKey, ctx)
        val kind = requireNotNull(withRelation.index.relationKind(kindKey)) { "no kind $kindKey" }
        val (relSlot, value) = when (slot) {
            LineSlot.BETWEEN -> RelationSlotName.BETWEEN to RelationEdits.betweenValue(relation, kind, toKey, description)
            LineSlot.FROM_MIND -> RelationReader.mindSlot(RelationReader.sideOf(relation, fromKey)!!) to RelationEdits.mindValue(kind, description)
            LineSlot.TO_MIND -> RelationReader.mindSlot(RelationReader.sideOf(relation, toKey)!!) to RelationEdits.mindValue(kind, description)
        }
        val (recorded, _) = RelationEdits.record(withRelation, relation.key, mapOf(relSlot to SlotInput.Kind(value)), Anchor.UnitStart(startUnitKey), ctx)
        return updateEdge(recorded, canvasKey, edgeKey, ctx) { it.copy(relationKey = relation.key, labelText = label?.takeIf { s -> s.isNotBlank() } ?: it.labelText) }
    }

    private fun updateEdge(doc: ProjectDoc, canvasKey: Key, edgeKey: Key, ctx: EditContext, change: (CanvasEdge) -> CanvasEdge): ProjectDoc {
        val canvas = doc.canvases[canvasKey] ?: return doc
        val updated = canvas.copy(
            edges = canvas.edges.map { if (it.key == edgeKey) change(it).copy(updatedAt = ctx.now, updatedBy = ctx.device) else it },
            updatedAt = ctx.now, updatedBy = ctx.device,
        )
        return doc.copy(canvases = doc.canvases + (canvasKey to updated))
    }
}
