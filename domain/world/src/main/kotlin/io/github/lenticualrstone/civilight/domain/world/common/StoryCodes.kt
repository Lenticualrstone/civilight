package io.github.lenticualrstone.civilight.domain.world.common

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.PointRef
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/**
 * Display codes of story positions as the world screens print them: chapters `2장`, episodes
 * `1-7`, scenes `1-7 씬 3`, blocks `#044`. Ordinals count live siblings in order, so they follow
 * the manuscript (derived, never stored).
 */
class StoryCodes(private val doc: ProjectDoc) {
    private val structure = doc.project.structure
    private val ordinals = HashMap<Key, Int>()

    /** 1-based position of [unit] among its live siblings (its position among all siblings when it is not live). */
    fun ordinal(unit: StoryUnit): Int = ordinals.getOrPut(unit.key) {
        val siblings = doc.index.children(unit.parentKey?.takeIf { doc.index.unit(it) != null })
        val live = siblings.filter { doc.liveness.isLive(it) }
        val list = if (unit in live) live else siblings
        list.indexOfFirst { it.key == unit.key } + 1
    }

    /** The unit and its ancestors from the top down. */
    private fun path(unit: StoryUnit): List<StoryUnit> = (doc.index.ancestors(unit).reversed() + unit)

    /**
     * Episode code of a unit at or below the episode level (`1-7`): ordinals from the top level down
     * to the episode level joined by `-`. Null above the episode level or for unknown units.
     */
    fun episodeCode(unitKey: Key): String? {
        val unit = doc.index.unit(unitKey) ?: return null
        val episodeLevel = structure.episodeLevel ?: return null
        if (unit.level < episodeLevel) return null
        val path = path(unit).filter { it.level <= episodeLevel }
        return path.joinToString("-") { ordinal(it).toString() }
    }

    /** `1-7 씬 3`; `씬 3` in a one-level structure. */
    fun sceneLabel(sceneKey: Key): String? {
        val scene = doc.index.unit(sceneKey) ?: return null
        val episode = episodeCode(sceneKey)
        val s = "씬 ${ordinal(scene)}"
        return if (episode == null || scene.level == structure.episodeLevel) s else "$episode $s"
    }

    /** Label of any unit: chapter-level `2장`, episode `2-5`, scene `1-7 씬 3`. */
    fun unitLabel(unitKey: Key): String? {
        val unit = doc.index.unit(unitKey) ?: return null
        val episodeLevel = structure.episodeLevel
        return when {
            unit.level == structure.sceneLevel -> sceneLabel(unitKey)
            episodeLevel != null && unit.level >= episodeLevel -> episodeCode(unitKey)
            else -> "${ordinal(unit)}${structure.levels.getOrElse(unit.level) { "" }}"
        }
    }

    /** Episode code of the unit's episode (a scene's episode, a chapter's own `2장`). */
    fun episodeOrChapterLabel(unitKey: Key): String? {
        val unit = doc.index.unit(unitKey) ?: return null
        val episodeLevel = structure.episodeLevel
        return if (episodeLevel != null && unit.level >= episodeLevel) episodeCode(unitKey) else unitLabel(unitKey)
    }

    /** Episode code of the episode holding a block. */
    fun episodeOfBlock(blockKey: Key): String? = doc.index.block(blockKey)?.let { episodeCode(it.sceneKey) }

    /** `1-7 씬 3` of the scene holding a block. */
    fun sceneOfBlock(blockKey: Key): String? = doc.index.block(blockKey)?.let { sceneLabel(it.sceneKey) }

    /** Label of a reveal point (`1-7`, `2장`, `1-7 씬 3`). */
    fun pointLabel(point: PointRef): String? = unitLabel(point.unitKey)

    /** Lens chip text (`최신 · 1-7`, `1-5`, `1-5 씬 2`). */
    fun lensLabel(lens: LensPoint): String = when (lens) {
        LensPoint.Latest -> {
            val scene = doc.manuscript.writingLineSceneKey
            val episode = scene?.let(::episodeCode)
            if (episode == null) "최신" else "최신${Copy.SEP}$episode"
        }
        is LensPoint.UnitEnd -> unitLabel(lens.unitKey) ?: "최신"
    }

    companion object {
        /** `#044`. */
        fun blockNumber(number: Int): String = "#" + number.toString().padStart(3, '0')
    }
}
