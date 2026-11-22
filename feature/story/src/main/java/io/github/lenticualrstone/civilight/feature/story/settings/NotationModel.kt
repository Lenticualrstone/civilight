package io.github.lenticualrstone.civilight.feature.story.settings

import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.SceneNotation

/** One choice: label, and the sample scene's label under it (`1-7 · 2일째 · 새벽`, `1-7 · 약 13일째 · 새벽`). */
data class NotationOption(val notation: SceneNotation, val label: String, val sample: String?, val selected: Boolean)

/** 때 표기 screen (STO-17). */
data class NotationUi(val projectName: String, val options: List<NotationOption>, val readOnly: Boolean)

/**
 * `때 표기` (STO-17, R11 §6): `에피소드 기준` (default) or `이야기 시작부터`. Each option shows how
 * the sample scene would read with it (`ED`: the 집필 선 scene when it has 때, else the first timed
 * scene). Changing it changes labels only.
 */
object NotationModel {
    fun build(doc: ProjectDoc, readOnly: Boolean): NotationUi {
        val current = doc.project.sceneTime.notation.known ?: SceneNotation.EPISODE
        val sample = sampleScene(doc)
        val options = listOf(SceneNotation.EPISODE to NotationCopy.EPISODE, SceneNotation.STORY_START to NotationCopy.STORY_START).map { (n, label) ->
            NotationOption(n, label, sample?.let { labelUnder(doc, n, it) }, n == current)
        }
        return NotationUi(doc.project.name, options, readOnly)
    }

    private fun sampleScene(doc: ProjectDoc): Key? {
        val times = SceneTimes(doc)
        val line = doc.manuscript.writingLineSceneKey
        if (line != null && times.timed(line) != null) return line
        return times.liveScenes.firstOrNull { times.timed(it.key) != null }?.key
    }

    private fun labelUnder(doc: ProjectDoc, notation: SceneNotation, sceneKey: Key): String? {
        val settings = doc.project.sceneTime
        val shown = if (settings.notation.known == notation) doc else doc.copy(project = doc.project.copy(sceneTime = settings.copy(notation = notation.open())))
        return TimeLabels(SceneTimes(shown)).full(sceneKey)
    }
}
