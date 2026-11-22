package io.github.lenticualrstone.civilight.domain.cast

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
 * `2장`, block `#043`, lens chip `최신 · 1-7`.
 *
 * `domain:story` owns the canonical formatter; this local copy follows the same notation so cast
 * labels work before that module exists (see the status file, Requests).
 */
object StoryLabels {
    /** 1-based position among live siblings (all siblings when the unit itself is not live). */
    fun ordinal(doc: ProjectDoc, unit: StoryUnit): Int {
        val siblings = doc.index.children(unit.parentKey?.takeIf { doc.index.unit(it) != null })
        val live = doc.liveness.isLive(unit)
        val pool = if (live) siblings.filter { doc.liveness.isLive(it) } else siblings
        return pool.indexOfFirst { it.key == unit.key } + 1
    }

    /** `1-7` for an episode, `1-7 씬 3` for a scene, `2장` for a level above episodes; null for an unknown key. */
    fun unitCode(doc: ProjectDoc, unitKey: Key): String? {
        val unit = doc.index.unit(unitKey) ?: return null
        val structure = doc.project.structure
        val episodeLevel = structure.episodeLevel
        return when {
            unit.level == structure.sceneLevel -> {
                val parentCode = doc.index.ancestors(unit).firstOrNull { it.level == episodeLevel }
                    ?.let { episodeCode(doc, it) }
                val name = structure.levels.getOrElse(structure.sceneLevel) { "씬" }
                listOfNotNull(parentCode, "$name ${ordinal(doc, unit)}").joinToString(" ")
            }
            episodeLevel != null && unit.level == episodeLevel -> episodeCode(doc, unit)
            else -> "${ordinal(doc, unit)}${structure.levels.getOrElse(unit.level) { "" }}"
        }
    }

    /** Ordinals from the top level down to [episode], joined by `-` (`1-7`). */
    private fun episodeCode(doc: ProjectDoc, episode: StoryUnit): String {
        val path = doc.index.ancestors(episode).reversed() + episode
        return path.joinToString("-") { ordinal(doc, it).toString() }
    }

    /** The episode-level unit holding [unitKey] (itself when it is an episode), or the unit when above that level. */
    fun episodeOf(doc: ProjectDoc, unitKey: Key): StoryUnit? {
        val unit = doc.index.unit(unitKey) ?: return null
        val episodeLevel = doc.project.structure.episodeLevel ?: return unit
        if (unit.level <= episodeLevel) return unit
        return doc.index.ancestors(unit).firstOrNull { it.level == episodeLevel } ?: unit
    }

    /** Episode code of the unit (or the unit's own code above the episode level). */
    fun episodeCodeOf(doc: ProjectDoc, unitKey: Key): String? = episodeOf(doc, unitKey)?.let { unitCode(doc, it.key) }

    /** `#043`. */
    fun blockNumber(block: Block): String = "#" + block.number.toString().padStart(3, '0')

    /**
     * Short point of an anchor as history rows print it: the episode (`1-7`) for blocks and units
     * inside an episode, `2장` for a chapter, the event title for a world event.
     */
    fun anchorPoint(doc: ProjectDoc, anchor: Anchor): String = when (anchor) {
        is Anchor.AtBlock -> doc.index.block(anchor.blockKey)?.let { episodeCodeOf(doc, it.sceneKey) }
        is Anchor.UnitStart -> episodeCodeOf(doc, anchor.unitKey)
        is Anchor.UnitEnd -> episodeCodeOf(doc, anchor.unitKey)
        is Anchor.WorldEvent -> doc.index.event(anchor.eventKey)?.title
        Anchor.Unplaced, is Anchor.Unknown -> null
    } ?: CastCopy.NO_POSITION

    /** Full point: `1-7 #043`, `1-5 #—` (point record), `1-7 씬 3 뒤` (after a scene), event title. */
    fun anchorFull(doc: ProjectDoc, anchor: Anchor): String = when (anchor) {
        is Anchor.AtBlock -> doc.index.block(anchor.blockKey)?.let { b ->
            listOfNotNull(episodeCodeOf(doc, b.sceneKey), blockNumber(b)).joinToString(" ")
        }
        is Anchor.UnitStart -> unitCode(doc, anchor.unitKey)?.let { "$it #—" }
        is Anchor.UnitEnd -> unitCode(doc, anchor.unitKey)?.let { "$it 뒤" }
        is Anchor.WorldEvent -> doc.index.event(anchor.eventKey)?.title
        Anchor.Unplaced, is Anchor.Unknown -> null
    } ?: CastCopy.NO_POSITION

    /** Label of a reveal / unlock point: the unit's code (`1-7`, `2장`). */
    fun pointRef(doc: ProjectDoc, point: PointRef): String = unitCode(doc, point.unitKey)
        ?.let { if (point.kind.known == PointRefKind.UNIT_START || point.kind.known == PointRefKind.UNIT_END) it else null }
        ?: CastCopy.NO_POSITION

    /** The lens point alone: `1-5`, `1-7 씬 2`, or for `최신` the 집필 선 episode (`1-7`) or `전체`. */
    fun lensPoint(doc: ProjectDoc, lens: LensPoint): String = when (lens) {
        LensPoint.Latest -> doc.manuscript.writingLineSceneKey?.let { episodeCodeOf(doc, it) } ?: "전체"
        is LensPoint.UnitEnd -> unitCode(doc, lens.unitKey) ?: lensPoint(doc, LensPoint.Latest)
    }

    /** Lens chip: `최신 · 1-7`, `최신 · 전체`, `시점 · 1-5`. */
    fun lensChip(doc: ProjectDoc, lens: LensPoint): String = when (lens) {
        LensPoint.Latest -> "최신 · ${lensPoint(doc, lens)}"
        is LensPoint.UnitEnd -> "시점 · ${lensPoint(doc, lens)}"
    }

    /** `시점 1-7` / `최신 · 1-7` as section headers print the lens (`관계 · 시점 1-7`). */
    fun lensSection(doc: ProjectDoc, lens: LensPoint): String = when (lens) {
        LensPoint.Latest -> "최신 ${lensPoint(doc, lens)}"
        is LensPoint.UnitEnd -> "시점 ${lensPoint(doc, lens)}"
    }

    /** The scene holding the 집필 선, as `1-7 씬 3`. */
    fun writingLineScene(doc: ProjectDoc): String? = doc.manuscript.writingLineSceneKey?.let { unitCode(doc, it) }
}
