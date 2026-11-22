package io.github.lenticualrstone.civilight.domain.library.common

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/**
 * Derived structure labels (never stored, §2.14) as this area prints them: the code of a unit above
 * the scene level (`1-7`, `1`), and a scene's label (`1-7 씬 3`). Ordinals count live siblings in
 * manual order. The story area owns the full label rules; these follow the same counting.
 */
class UnitCodes(private val doc: ProjectDoc) {
    private val index = doc.index
    private val liveness = doc.liveness
    private val sceneLevel = doc.project.structure.sceneLevel
    private val sceneWord = doc.project.structure.levels.lastOrNull() ?: "씬"

    /** 1-based position among live siblings, or null for a non-live unit. */
    fun ordinal(unit: StoryUnit): Int? {
        val siblings = index.children(unit.parentKey).filter { liveness.isLive(it) }.sortedBy { it.order }
        val i = siblings.indexOfFirst { it.key == unit.key }
        return if (i < 0) null else i + 1
    }

    /**
     * Ordinal of [unit] counting trashed siblings too, so a unit in the trash still gets a code
     * (`1-6 씬 5` while 1-6 씬 5 is in the trash).
     */
    fun ordinalIncludingTrashed(unit: StoryUnit): Int {
        val siblings = index.children(unit.parentKey).sortedBy { it.order }
        return siblings.indexOfFirst { it.key == unit.key } + 1
    }

    /** `1-7` for an episode, `1` for a chapter; for a scene, the code of its parent unit. */
    fun code(unitKey: Key): String? {
        val unit = index.unit(unitKey) ?: return null
        val chain = (index.ancestors(unit).reversed() + unit).filter { it.level < sceneLevel }
        if (chain.isEmpty()) return null
        return chain.joinToString("-") { ordinalIncludingTrashed(it).toString() }
    }

    /** `씬 3` (scene word = the structure's scene level name). */
    fun sceneOrdinalLabel(sceneKey: Key): String? {
        val scene = index.unit(sceneKey) ?: return null
        return "$sceneWord ${ordinalIncludingTrashed(scene)}"
    }

    /** `1-7 씬 3`; `씬 3` when the structure has a single level. */
    fun sceneLabel(sceneKey: Key): String? {
        val scene = index.unit(sceneKey) ?: return null
        val ordinal = sceneOrdinalLabel(sceneKey) ?: return null
        val parentCode = scene.parentKey?.let(::code)
        return if (parentCode == null) ordinal else "$parentCode $ordinal"
    }
}
