package io.github.lenticualrstone.civilight.domain.recorder.link

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/**
 * Structure labels as `domain:story`'s `UnitCodes` prints them (this module cannot depend on it):
 * ordinals count live siblings of the same level in order; a non-live unit gets the ordinal it
 * would have (1 + live siblings before it). Scene `1-7 씬 3`, episode `1-7` (`1막` when the episode
 * level is the top level), other levels `2장`.
 */
class UnitLabels(private val doc: ProjectDoc) {
    private val structure = doc.project.structure
    private val levels = structure.levels
    private val ordinals = HashMap<Key, Int>()

    fun ordinal(unit: StoryUnit): Int = ordinals.getOrPut(unit.key) {
        var n = 0
        for (sibling in doc.index.children(unit.parentKey).filter { it.level == unit.level }) {
            if (sibling.key == unit.key) return@getOrPut n + 1
            if (doc.liveness.isUnitLive(sibling.key)) n++
        }
        n + 1
    }

    val sceneWord: String get() = levels.lastOrNull() ?: "씬"

    fun isScene(unit: StoryUnit): Boolean = unit.level == structure.sceneLevel

    fun levelLabel(unit: StoryUnit): String = "${ordinal(unit)}${levels.getOrElse(unit.level) { "" }}"

    fun episodeOf(unit: StoryUnit): StoryUnit? {
        val level = structure.episodeLevel ?: return null
        if (unit.level == level) return unit
        return doc.index.ancestors(unit).firstOrNull { it.level == level }
    }

    fun episodeCode(episode: StoryUnit): String {
        if (episode.level == 0) return levelLabel(episode)
        val path = doc.index.ancestors(episode).reversed() + episode
        return path.joinToString("-") { ordinal(it).toString() }
    }

    /** `1-7 씬 3`; `씬 3` in a 1-level structure. */
    fun sceneRef(scene: StoryUnit): String {
        val own = "$sceneWord ${ordinal(scene)}"
        val episode = episodeOf(scene) ?: return own
        return "${episodeCode(episode)} $own"
    }

    /** The label of any unit. */
    fun label(unit: StoryUnit): String = when {
        isScene(unit) -> sceneRef(unit)
        unit.level == structure.episodeLevel -> episodeCode(unit)
        else -> levelLabel(unit)
    }

    fun label(key: Key): String? = doc.index.unit(key)?.let(::label)
}
