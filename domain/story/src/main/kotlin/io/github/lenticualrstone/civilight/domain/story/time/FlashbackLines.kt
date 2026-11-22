package io.github.lenticualrstone.civilight.domain.story.time

import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.SceneTime

/**
 * Lines of the 회상 form (STO-24, board `RTFlashback`) that are not labels of the flashback itself.
 */
object FlashbackLines {
    /**
     * `씬 3 때 기준 | 씬 1 · 회상 건너뜀`: the next live scene after the flashback [sceneKey] (manuscript
     * order) and the base it actually uses, which skips the flashback (R11 §4). Null when [sceneKey]
     * is not a flashback, when no later scene is based on a scene before it, or when the next scene
     * is untimed, `직접` or itself a flashback.
     */
    fun skipLine(doc: ProjectDoc, sceneKey: Key, times: SceneTimes = SceneTimes(doc), codes: UnitCodes = UnitCodes(doc)): Pair<String, String>? {
        val scene = doc.index.unit(sceneKey) ?: return null
        if (scene.time !is SceneTime.Flashback) return null
        val scenes = times.liveScenes
        val at = scenes.indexOfFirst { it.key == sceneKey }.takeIf { it >= 0 } ?: return null
        val next = scenes.getOrNull(at + 1) ?: return null
        if (next.time !is SceneTime.Relative) return null
        val baseKey = times.timed(next.key)?.baseKey ?: return null
        val base = doc.index.unit(baseKey) ?: return null
        return "${codes.sceneRefRelativeTo(next, scene)} 때 기준" to "${codes.sceneRefRelativeTo(base, next)} · 회상 건너뜀"
    }
}
