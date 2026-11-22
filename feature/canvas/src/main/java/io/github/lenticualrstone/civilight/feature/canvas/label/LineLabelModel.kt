package io.github.lenticualrstone.civilight.feature.canvas.label

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.relation.CanvasRelations
import io.github.lenticualrstone.civilight.domain.cast.relation.LineSlot
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationKinds
import io.github.lenticualrstone.civilight.feature.canvas.CanvasCopy
import io.github.lenticualrstone.civilight.feature.canvas.board.NodeLabels
import io.github.lenticualrstone.civilight.feature.canvas.edit.CanvasEdits
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.canvas.CanvasNode
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.model.cast.RelationCategory
import io.github.lenticualrstone.civilight.model.cast.RelationSymmetry
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.ui.labels.UnitLabels

/**
 * One end of a line as the label surface sees it: the name it shows, and the character behind it when
 * the end is a live, visible character node (only then can the line become a relation).
 */
data class LineEnd(val name: String, val characterKey: Key?)

/**
 * What the author has picked in the label surface. Picking records nothing (rule 13 §7, T-CNV-11);
 * only `그냥 선으로 두기` or `관계로 만들기` writes. Saved with the screen as strings ([save]).
 */
data class LineLabelDraft(
    val text: String,
    val slot: LineSlot?,
    val kindKey: Key?,
    val startUnitKey: Key?,
) {
    fun save(): List<String?> = listOf(text, slot?.name, kindKey, startUnitKey)

    companion object {
        fun restore(saved: List<String?>): LineLabelDraft = LineLabelDraft(
            text = saved.getOrNull(0).orEmpty(),
            slot = saved.getOrNull(1)?.let { n -> LineSlot.entries.firstOrNull { it.name == n } },
            kindKey = saved.getOrNull(2),
            startUnitKey = saved.getOrNull(3),
        )
    }
}

data class SlotChip(val slot: LineSlot, val label: String, val selected: Boolean)

data class KindChip(val key: Key, val label: String, val selected: Boolean)

/** The `선 라벨` surface (boards `CanvasCLabel`, `Canvas`; same rows on every screen size, rule 13 §9). */
data class LineLabelView(
    val title: String,
    /** `리엔 · 하르트`. */
    val sub: String,
    val text: String,
    /** Rows 2–4 and `관계로 만들기` exist only between two characters (T-CNV-10). */
    val relational: Boolean,
    val slotChips: List<SlotChip>,
    val kindChips: List<KindChip>,
    /** `시작 시점` value (`1-7`); null when the project has no unit to start from. */
    val startValue: String?,
    val keepLabel: String,
    /** Null when the line cannot become a relation. */
    val makeLabel: String?,
    /** `관계로 만들기` needs a slot, a kind and a start (Q-CV-11 default: off until a kind is picked). */
    val makeEnabled: Boolean,
    val readOnly: Boolean,
) {
    /** The buttons in order: `그냥 선으로 두기` (ghost), then `관계로 만들기` (primary) when offered. */
    val buttons: List<String> get() = listOfNotNull(keepLabel, makeLabel)
}

/**
 * The line-label model (CNV-08): pair-based (from, to, lens), so a relationship-map line can use it
 * as well as a canvas line. Writes go through `domain:cast` `CanvasRelations` only.
 */
object LineLabelModel {
    /** The two ends of a canvas line, or null when the line or a node is gone. */
    fun ends(doc: ProjectDoc, canvasKey: Key, edgeKey: Key, hiddenOpen: Boolean): Pair<LineEnd, LineEnd>? {
        val canvas = CanvasEdits.canvas(doc, canvasKey) ?: return null
        val edge = canvas.edges.firstOrNull { it.key == edgeKey } ?: return null
        val from = canvas.nodes.firstOrNull { it.key == edge.fromNodeKey } ?: return null
        val to = canvas.nodes.firstOrNull { it.key == edge.toNodeKey } ?: return null
        return end(doc, from, hiddenOpen) to end(doc, to, hiddenOpen)
    }

    private fun end(doc: ProjectDoc, node: CanvasNode, hiddenOpen: Boolean): LineEnd {
        val label = NodeLabels.of(doc, node, hiddenOpen)
        val character = node.refKey.takeIf { node.kind.known == CanvasNodeKind.CHARACTER && label.openable }
        return LineEnd(label.title, character)
    }

    fun isRelational(from: LineEnd, to: LineEnd): Boolean =
        from.characterKey != null && to.characterKey != null && from.characterKey != to.characterKey

    /** Kinds of 사이 when `사이` is chosen, 마음 kinds otherwise. */
    fun category(slot: LineSlot?): RelationCategory = if (slot == LineSlot.BETWEEN) RelationCategory.BETWEEN else RelationCategory.MIND

    /**
     * The draft a surface opens with: the line's label text, `<drawn-from>의 마음` preselected
     * (rule 13 §7, T-CNV-09), no kind yet, and the lens episode as the start (`최신` = the 집필 선
     * episode; Q-CV-10 default, board `1-7` at `최신 · 1-7`).
     */
    fun initialDraft(doc: ProjectDoc, labelText: String?, from: LineEnd, to: LineEnd, lens: LensPoint): LineLabelDraft {
        val relational = isRelational(from, to)
        return LineLabelDraft(
            text = labelText.orEmpty(),
            slot = if (relational) LineSlot.FROM_MIND else null,
            kindKey = null,
            startUnitKey = if (relational) CanvasRelations.startUnit(doc, lens) else null,
        )
    }

    /** Picking another `무엇으로 적을지` chip; the kind is kept only when it belongs to the new category. */
    fun withSlot(doc: ProjectDoc, draft: LineLabelDraft, slot: LineSlot): LineLabelDraft {
        val kinds = RelationKinds.of(doc, category(slot)).map { it.key }.toSet()
        return draft.copy(slot = slot, kindKey = draft.kindKey?.takeIf { it in kinds })
    }

    fun build(doc: ProjectDoc, from: LineEnd, to: LineEnd, draft: LineLabelDraft, readOnly: Boolean): LineLabelView {
        val relational = isRelational(from, to)
        val labels = UnitLabels(doc)
        val slotChips = if (!relational) emptyList() else listOf(
            SlotChip(LineSlot.BETWEEN, CanvasCopy.SLOT_BETWEEN, draft.slot == LineSlot.BETWEEN),
            SlotChip(LineSlot.FROM_MIND, CanvasCopy.mindOf(from.name), draft.slot == LineSlot.FROM_MIND),
            SlotChip(LineSlot.TO_MIND, CanvasCopy.mindOf(to.name), draft.slot == LineSlot.TO_MIND),
        )
        val kindChips = if (!relational) emptyList() else RelationKinds.of(doc, category(draft.slot)).map { k ->
            val label = if (k.symmetry?.known == RelationSymmetry.PAIR && !k.pairName.isNullOrBlank()) "${k.name} ↔ ${k.pairName}" else k.name
            KindChip(k.key, label, k.key == draft.kindKey)
        }
        val start = draft.startUnitKey?.takeIf { doc.liveness.isUnitLive(it) }
        val canMake = relational && draft.slot != null && draft.kindKey != null && kindChips.any { it.key == draft.kindKey } && start != null
        return LineLabelView(
            title = CanvasCopy.LINE_LABEL_TITLE,
            sub = CanvasCopy.pair(from.name, to.name),
            text = draft.text,
            relational = relational,
            slotChips = slotChips,
            kindChips = kindChips,
            startValue = start?.let(labels::reference),
            keepLabel = CanvasCopy.BTN_KEEP_LINE,
            makeLabel = if (relational) CanvasCopy.BTN_MAKE_RELATION else null,
            makeEnabled = canMake && !readOnly,
            readOnly = readOnly,
        )
    }

    /** `그냥 선으로 두기`: only the label text is kept on the canvas line (T-CNV-12). */
    fun keep(doc: ProjectDoc, canvasKey: Key, edgeKey: Key, draft: LineLabelDraft, ctx: EditContext): ProjectDoc =
        CanvasRelations.keepAsLine(doc, canvasKey, edgeKey, draft.text, ctx)

    /**
     * `관계로 만들기` (T-CNV-13, T-CNV-14): the pair's one relation (made if missing), a `◆` record on the
     * chosen cell as a `시점` placement from the start of the chosen unit, and the line's relation key.
     * The label text stays on the line; it does not become the cell's 설명 (Q-CV-09 default).
     */
    fun make(doc: ProjectDoc, canvasKey: Key, edgeKey: Key, draft: LineLabelDraft, ctx: EditContext): ProjectDoc {
        val slot = requireNotNull(draft.slot) { "no slot" }
        val kind = requireNotNull(draft.kindKey) { "no kind" }
        val start = requireNotNull(draft.startUnitKey) { "no start" }
        return CanvasRelations.makeRelation(doc, canvasKey, edgeKey, slot, kind, start, ctx, label = draft.text)
    }
}
