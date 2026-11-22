package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.Rank
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary

/** Owned things that go to the trash with an item (`같이 휴지통으로`). */
enum class OwnedKind {
    /** Relations (rule 13), with the partner names. */
    RELATION,

    /** `◆` records about the item (fields, display name, its relations' slots). */
    CHANGE_RECORD,

    /** `◇ 첫 공개` of a term. */
    FIRST_REVEAL,

    /** Voice lines and archive documents (`게임 텍스트`). */
    GAME_TEXT,

    /** Map pins of a place. */
    PIN,
    FIELD_VALUE,
    IMAGE,

    /** Blocks of a deleted scene. */
    BLOCK,

    /** Overlays of a deleted map. */
    MAP_OVERLAY,

    /** Nodes and edges of a deleted canvas. */
    CANVAS_PART,
}

/**
 * The impact sheet's numbers (CST-40, rule 20 §4): what goes to the trash with the items and what
 * stays in place as dotted references. Counts only cover live containers (lines inside a trashed
 * scene are not counted, TC-20-01).
 */
data class DeleteImpact(
    val items: List<ItemId>,
    val names: List<String>,
    /** Any item is hidden: the sheet shows counts only, no locations (TC-20-10). */
    val hidden: Boolean,
    val owned: Map<OwnedKind, Int>,
    /** Partner names of the owned relations (`오웬`). */
    val relationPartners: List<String>,
    val references: List<RefSite>,
    /** `▲ 공개 전 사용` uses of deleted terms that will be held (CST-40 toast). */
    val heldWarnings: Int,
    /** Scenes of the speaker lines (`1-6 씬 2 · 씬 4`); null for hidden items. */
    val speakerLocations: String?,
) {
    val referenceCounts: Map<RefKind, Int> get() = references.groupingBy { it.kind }.eachCount()

    fun count(kind: RefKind): Int = referenceCounts[kind] ?: 0

    fun owned(kind: OwnedKind): Int = owned[kind] ?: 0

    /** `참조 20`: every reference that stays in place. */
    val referenceTotal: Int get() = references.size

    val ownedTotal: Int get() = owned.values.sum()

    /** No references and no owned records: no sheet, delete at once (TC-20-03). */
    val needsSheet: Boolean get() = referenceTotal > 0 || ownedTotal > 0
}

/** Delete impact computation (CST-40, CST-47, WLD-14). */
object Impact {
    /**
     * The impact of deleting [items] together. Works for live items (before `지우기`) and for items
     * already in the trash (trash rows, check rows): owned rows are those live with the items'
     * marks cleared and not live with them set (§5.1); references are scanned with the marks set.
     */
    fun of(doc: ProjectDoc, items: List<ItemId>, recorder: RecorderLibrary? = null): DeleteImpact {
        val present = items.distinct().filter { doc.exists(it) }
        var unmarked = doc
        var marked = doc
        for (item in present) {
            val row = doc.rowOf(item) ?: continue
            unmarked = unmarked.setMark(item, null, null, row.deletedAtOrZero(), "")
            marked = marked.setMark(item, row.deletedAt ?: 0L, row.trashEntryKey ?: "", row.deletedAtOrZero(), "")
        }
        // Bookkeeping stamps differ between the two copies only on the item rows, which are not compared.
        val owned = OwnedRows.between(LiveRows.of(unmarked), LiveRows.of(marked))
        val scanner = ReferenceScanner(marked, recorder)
        val references = present.flatMap(scanner::referencesTo).distinctBy { it.id }
        val records = owned.records.mapNotNull(unmarked.index::record)
        val reveals = records.count { it.target is ChangeTarget.TermReveal }
        val itemKeys = present.map { it.key }.toSet()
        val partners = owned.relations.mapNotNull(unmarked.index::relation).flatMap { relation ->
            listOf(relation.endA, relation.endB).filter { it.key !in itemKeys }.mapNotNull { end ->
                unmarked.index.character(end.key)?.name ?: unmarked.index.faction(end.key)?.name
            }
        }
        val hidden = present.any { doc.itemHidden(it) }
        val speakerScenes = references.filter { it.kind == RefKind.SPEAKER }.mapNotNull { it.sceneKey }
        return DeleteImpact(
            items = present,
            names = present.mapNotNull(doc::itemName),
            hidden = hidden,
            owned = mapOf(
                OwnedKind.RELATION to owned.relations.size,
                OwnedKind.CHANGE_RECORD to records.size - reveals,
                OwnedKind.FIRST_REVEAL to reveals,
                OwnedKind.GAME_TEXT to owned.voiceLines.size + owned.archiveDocs.size,
                OwnedKind.PIN to owned.pins.size,
                OwnedKind.FIELD_VALUE to owned.fieldValues.size,
                OwnedKind.IMAGE to owned.imageUses.size,
                OwnedKind.BLOCK to owned.blocks.size,
                OwnedKind.MAP_OVERLAY to owned.overlays.size,
                OwnedKind.CANVAS_PART to owned.canvasParts.size,
            ),
            relationPartners = partners,
            references = references,
            heldWarnings = present.filter { it.kind == ItemKind.TERM }.sumOf { heldWarnings(unmarked, marked, it.key) },
            speakerLocations = if (hidden || speakerScenes.isEmpty()) null else UnitCodes(doc).locations(speakerScenes),
        )
    }

    fun of(doc: ProjectDoc, item: ItemId, recorder: RecorderLibrary? = null): DeleteImpact = of(doc, listOf(item), recorder)

    /**
     * `▲ 공개 전 사용` of [termKey] that a delete holds (CST-47): manuscript links before the term's
     * live reveal in [withTerm]. No reveal → no `▲`.
     */
    fun heldWarnings(withTerm: ProjectDoc, withoutTerm: ProjectDoc, termKey: Key): Int {
        val reveal = withTerm.changes.records.firstOrNull {
            withTerm.liveness.isLive(it) && (it.target as? ChangeTarget.TermReveal)?.termKey == termKey
        } ?: return 0
        val manuscript = withTerm.manuscript
        val revealRank = manuscript.rankOf(reveal.anchor)
        return ReferenceScanner(withoutTerm).linksTo(termKey).count { site ->
            val rank: Rank? = when (site.surface) {
                LinkSurface.BLOCK -> site.blockKey?.let(manuscript::blockRankOf)
                LinkSurface.TRANSITION -> site.sceneKey?.let(manuscript::transitionRankOf)
                else -> null
            }
            rank != null && rank < revealRank
        }
    }

    private fun io.github.lenticualrstone.civilight.model.Trashable.deletedAtOrZero(): Long = deletedAt ?: 0L
}
