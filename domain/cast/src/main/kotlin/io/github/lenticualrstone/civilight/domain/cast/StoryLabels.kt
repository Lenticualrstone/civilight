package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.story.lens.LensView
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.PointRef
import io.github.lenticualrstone.civilight.model.position.PointRefKind
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/**
 * Notation of story positions as cast screens print them: episode `1-7`, scene `1-7 씬 3`, chapter
 * `2장`, block `#043`, lens chip `최신 · 1-7`. A thin layer over `domain:story`'s [UnitCodes] and
 * [LensView], which own the notation.
 */
object StoryLabels {
    /** `1-7` for an episode, `1-7 씬 3` for a scene, `2장` above the episode level; null for an unknown key. */
    fun unitCode(doc: ProjectDoc, unitKey: Key): String? = UnitCodes(doc).unitLabel(unitKey)

    /** The episode-level unit holding [unitKey] (itself when it is an episode), or the unit itself above that level. */
    fun episodeOf(doc: ProjectDoc, unitKey: Key): StoryUnit? {
        val unit = doc.index.unit(unitKey) ?: return null
        return UnitCodes(doc).episodeOf(unit) ?: unit
    }

    /** Episode code of the unit (or the unit's own code above the episode level). */
    fun episodeCodeOf(doc: ProjectDoc, unitKey: Key): String? {
        val codes = UnitCodes(doc)
        val unit = doc.index.unit(unitKey) ?: return null
        return codes.episodeOf(unit)?.let(codes::episodeCode) ?: codes.unitLabel(unit)
    }

    /** `#043`. */
    fun blockNumber(block: Block): String = "#" + block.number.toString().padStart(3, '0')

    /**
     * Short point of an anchor as history rows print it: the episode (`1-7`) for blocks and units
     * inside an episode, `2장` for a chapter, the event title for a world event, `—` when unplaced.
     */
    fun anchorPoint(doc: ProjectDoc, anchor: Anchor): String = LensView(doc).anchorPoint(anchor)

    /** Full point: `1-7 #043`, `1-5 #—` (point record), `1-7 씬 3 뒤` (after a scene), event title. */
    fun anchorFull(doc: ProjectDoc, anchor: Anchor): String = when (anchor) {
        is Anchor.UnitStart -> unitCode(doc, anchor.unitKey)?.let { "$it #—" } ?: "—"
        else -> LensView(doc).anchorLabel(anchor)
    }

    /** Label of a reveal / unlock point: the unit's code (`1-7`, `2장`), `—` when unknown. */
    fun pointRef(doc: ProjectDoc, point: PointRef): String {
        val known = point.kind.known == PointRefKind.UNIT_START || point.kind.known == PointRefKind.UNIT_END
        return (if (known) unitCode(doc, point.unitKey) else null) ?: "—"
    }

    /** The lens point alone: `1-5`, `1-7 씬 2`, or for `최신` the 집필 선 episode (`1-7`) or `전체`. */
    fun lensPoint(doc: ProjectDoc, lens: LensPoint): String {
        val view = LensView(doc)
        return when (lens) {
            LensPoint.Latest -> view.latestCode()
            is LensPoint.UnitEnd -> if (doc.index.unit(lens.unitKey) == null) view.latestCode() else view.pointCode(lens)
        }
    }

    /** Lens chip: `최신 · 1-7`, `최신 · 전체`, `시점 · 1-5`. */
    fun lensChip(doc: ProjectDoc, lens: LensPoint): String = LensView(doc).chip(lens).label

    /** The lens as section headers print it: `시점 1-7`, `최신 1-7`. */
    fun lensSection(doc: ProjectDoc, lens: LensPoint): String = when (lens) {
        LensPoint.Latest -> "최신 ${lensPoint(doc, lens)}"
        is LensPoint.UnitEnd -> "시점 ${lensPoint(doc, lens)}"
    }

    /** The scene holding the 집필 선, as `1-7 씬 3`. */
    fun writingLineScene(doc: ProjectDoc): String? = LensView(doc).writingLineScene?.let { UnitCodes(doc).sceneRef(it) }
}
