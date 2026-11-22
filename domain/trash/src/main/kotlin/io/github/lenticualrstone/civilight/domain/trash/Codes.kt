package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/**
 * Derived unit labels (never stored, §2.14): ordinals count live siblings only, so after `씬 3` is
 * deleted the next scene reads `씬 3`; a unit in the trash reads where it would be (`1-6 씬 5`).
 */
class UnitCodes(private val doc: ProjectDoc) {
    private val index get() = doc.index

    /** 1-based position among live siblings (a non-live unit: after the live siblings before it). */
    fun ordinal(unit: StoryUnit): Int =
        index.children(unit.parentKey?.takeIf { index.unit(it) != null })
            .takeWhile { it.key != unit.key }
            .count { doc.liveness.isLive(it) } + 1

    /** `1-6` for an episode, `1` for a chapter, `1-6 씬 2` for a scene (`씬 2` for a top-level scene). */
    fun code(unitKey: Key): String? {
        val unit = index.unit(unitKey) ?: return null
        if (!index.isScene(unit)) return path(unit)
        val parent = index.parent(unit) ?: return "씬 ${ordinal(unit)}"
        return "${path(parent)} 씬 ${ordinal(unit)}"
    }

    /** `씬 4`. */
    fun sceneShort(sceneKey: Key): String? = index.unit(sceneKey)?.let { "씬 ${ordinal(it)}" }

    /** `1-6 씬 5 · 시장의 밤` (title `미정` when unset). */
    fun codeAndTitle(unitKey: Key): String? {
        val unit = index.unit(unitKey) ?: return null
        return "${code(unitKey)} · ${unit.title ?: TrashCopy.UNTITLED}"
    }

    /**
     * Scene locations of references: `1-6 씬 2 · 씬 4`. Scenes are put in manuscript order; the first
     * scene under each parent carries the parent code, the following ones only `씬 n`.
     */
    fun locations(sceneKeys: Collection<Key>): String {
        val order = index.allScenesInOrder.withIndex().associate { (i, s) -> s.key to i }
        val scenes = sceneKeys.distinct().mapNotNull(index::unit).sortedBy { order[it.key] ?: Int.MAX_VALUE }
        var lastParent: Key? = null
        val parts = scenes.map { scene ->
            val parent = scene.parentKey
            val label = if (parent != null && parent == lastParent) sceneShort(scene.key) else code(scene.key)
            lastParent = parent
            label.orEmpty()
        }
        return parts.joinToString(TrashCopy.SEP)
    }

    private fun path(unit: StoryUnit): String =
        (index.ancestors(unit).reversed() + unit).joinToString("-") { ordinal(it).toString() }
}
