package io.github.lenticualrstone.civilight.domain.recorder.state

import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.link.LinkViews
import io.github.lenticualrstone.civilight.domain.recorder.link.UnitLabels
import io.github.lenticualrstone.civilight.domain.recorder.put.CardDocs
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.recorder.Placement
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderRecord

/**
 * One row of the record screen's `넣은 곳` list (LRC-28): [text] is the label plus ` · 휴지통` while
 * the copy is in the trash; a gone copy is plain `ink3` text, not tappable; [editedAfter] shows
 * `◇ 넣은 뒤 고침` beside it.
 */
data class PlacementRow(
    val placementKey: Key,
    val label: String,
    val status: PlacementStatus,
    val editedAfter: Boolean,
) {
    val text: String get() = if (status == PlacementStatus.IN_TRASH) "$label${RecorderCopy.SEP}${RecorderCopy.IN_TRASH}" else label
    val tappable: Boolean get() = status != PlacementStatus.GONE
    val mark: String? get() = if (editedAfter) RecorderCopy.EDITED_AFTER else null
}

/** Labels of placements: `1-7 씬 3 · #062`, `하르트 · 말투`, `<page>`, `1-7 씬 3 · 씬 메모`, `<character>`. */
object PlacementLabels {
    /** `#062`. */
    fun number(n: Int): String = "#%03d".format(n)

    /** The label computed from the current rows, or null when the target no longer exists. */
    fun current(placement: Placement, doc: ProjectDoc): String? {
        val labels = UnitLabels(doc)
        return when (placement.kind.known) {
            PlacementKind.SCRIPT_BLOCK -> {
                val block = doc.index.block(placement.targetKey) ?: return null
                val scene = doc.index.unit(block.sceneKey) ?: return null
                "${labels.sceneRef(scene)}${RecorderCopy.SEP}${number(block.number)}"
            }
            PlacementKind.CARD_PARAGRAPH -> {
                val entityKey = placement.containerKey ?: return null
                val kind = CardDocs.kindOf(doc, entityKey) ?: return null
                if (!CardDocs.holdsParagraph(doc, entityKey, placement.targetKey)) return null
                val name = CardDocs.name(doc, kind, entityKey) ?: return null
                CardDocs.tabLabel(doc, placement.tabId)?.let { "$name${RecorderCopy.SEP}$it" } ?: name
            }
            PlacementKind.PAGE -> doc.pages[placement.targetKey]?.name
            PlacementKind.MEMO_FIELD -> {
                val unit = doc.index.unit(placement.targetKey) ?: return null
                if (labels.isScene(unit)) "${labels.sceneRef(unit)}${RecorderCopy.SEP}${RecorderCopy.SCENE_MEMO}" else labels.label(unit)
            }
            PlacementKind.CHARACTER -> doc.index.character(placement.targetKey)?.name
            null -> null
        }
    }

    /** True when the copy sits in a hidden item (scene, card, page, unit, character). */
    private fun inHiddenItem(placement: Placement, doc: ProjectDoc): Boolean = when (placement.kind.known) {
        PlacementKind.SCRIPT_BLOCK -> doc.index.block(placement.targetKey)?.let { doc.index.unit(it.sceneKey)?.hidden } == true
        PlacementKind.CARD_PARAGRAPH -> placement.containerKey?.let { key ->
            CardDocs.kindOf(doc, key)?.let { CardDocs.isHidden(doc, it, key) }
        } == true
        PlacementKind.PAGE -> doc.pages[placement.targetKey]?.hidden == true
        PlacementKind.MEMO_FIELD -> doc.index.unit(placement.targetKey)?.hidden == true
        PlacementKind.CHARACTER -> doc.index.character(placement.targetKey)?.hidden == true
        null -> false
    }

    /** The label as shown, with LRC-22 masking (`숨긴 프로젝트`, `이름 가림`, `숨긴 항목`) and the stored last label once gone. */
    fun shown(placement: Placement, env: RecorderEnv): String {
        val project = env.project(placement.projectKey)
        if (project != null && env.isLockedHidden(project)) return RecorderCopy.HIDDEN_PROJECT
        if (project != null && project.linkMask) return LinkViews.maskedLabel()
        if (project == null || placement.purgedAt != null) return placement.lastLabel
        if (inHiddenItem(placement, project.doc) && !env.itemsOpen(placement.projectKey)) return RecorderCopy.HIDDEN_ITEM
        return current(placement, project.doc) ?: placement.lastLabel
    }

    /** `넣은 곳` rows, newest first. */
    fun rows(record: RecorderRecord, placements: List<Placement>, env: RecorderEnv): List<PlacementRow> =
        placements.sortedWith(compareByDescending<Placement> { it.createdAt }.thenByDescending { it.key }).map {
            PlacementRow(it.key, shown(it, env), RecordStates.placementStatus(it, env), RecordStates.editedAfter(record, it))
        }

    /**
     * Placements whose stored [Placement.lastLabel] differs from the current label, refreshed
     * ("refreshed while the target exists, kept after purge").
     */
    fun refresh(placements: List<Placement>, env: RecorderEnv): List<Placement> = placements.map { p ->
        if (p.purgedAt != null) return@map p
        val doc = env.project(p.projectKey)?.doc ?: return@map p
        val label = current(p, doc) ?: return@map p
        if (label == p.lastLabel) p else p.copy(lastLabel = label)
    }
}
