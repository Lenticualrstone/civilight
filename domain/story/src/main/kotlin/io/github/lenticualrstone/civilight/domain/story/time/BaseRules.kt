package io.github.lenticualrstone.civilight.domain.story.time

import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.domain.story.withTimes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.SceneTime

/**
 * The base line of the `장소와 때` sheet (STO-20): `때 · 직전 씬 기준 | 씬 2 · 1-7 · 1일째 저녁` when the
 * base is the directly previous scene, else `때 · 1-7 씬 3 기준`.
 */
data class BaseLine(val baseKey: Key, val isDirectlyPrevious: Boolean, val label: String, val value: String?) {
    /** `1-2 씬 1 기준` or `직전 씬 기준`. */
    val text: String get() = label.removePrefix("때 · ")
}

/** Base selection rules of STO-21 (R11 §4). */
object BaseRules {
    /** Default base for a scene given 때: the nearest earlier timed non-flashback scene, or null (only `직접`). */
    fun defaultBase(times: SceneTimes, sceneKey: Key): Key? = times.previousTimed(sceneKey)?.key

    /** BASE-03: with no earlier timed scene only `직접` is enabled. */
    fun onlyDirectEnabled(times: SceneTimes, sceneKey: Key): Boolean = defaultBase(times, sceneKey) == null

    /** The base line for a `기준` scene (or the default base of a scene being given 때). */
    fun baseLine(times: SceneTimes, sceneKey: Key, codes: UnitCodes = UnitCodes(times.doc)): BaseLine? {
        val scene = times.doc.index.unit(sceneKey) ?: return null
        val baseKey = when (val t = scene.time) {
            is SceneTime.Relative -> times.timed(sceneKey)?.baseKey ?: t.baseKey
            SceneTime.None -> defaultBase(times, sceneKey)
            else -> null
        } ?: return null
        val base = times.doc.index.unit(baseKey) ?: return null
        val previous = times.previousScene(sceneKey)
        return if (previous?.key == baseKey) {
            // Board copy: `씬 2 · 1-7 · 1일째 저녁` (scene, episode, short form).
            val short = TimeLabels(times, codes).short(baseKey)
            val episode = codes.episodeOf(base)?.let(codes::episodeCode)
            BaseLine(baseKey, true, "때 · 직전 씬 기준", listOfNotNull(codes.sceneLabel(base), episode, short).joinToString(" · "))
        } else {
            BaseLine(baseKey, false, "때 · ${codes.sceneRef(base)} 기준", null)
        }
    }

    /** `기준을 직전 씬으로` is shown when the base is not the nearest earlier timed scene (BASE-02). */
    fun canRebaseToPrevious(times: SceneTimes, sceneKey: Key): Boolean {
        val stored = times.doc.index.unit(sceneKey)?.time as? SceneTime.Relative ?: return false
        val previous = times.previousTimed(sceneKey) ?: return false
        return stored.baseKey != previous.key
    }

    /** `기준을 직전 씬으로`: base = the nearest earlier timed scene, interval recomputed, absolute kept. */
    fun rebaseToPrevious(doc: ProjectDoc, sceneKey: Key, ctx: EditContext? = null): ProjectDoc {
        val times = SceneTimes(doc)
        if (!canRebaseToPrevious(times, sceneKey)) return doc
        val previous = times.previousTimed(sceneKey)!!.key
        val rewritten = keepAbsoluteOn(times, sceneKey, previous) ?: return doc
        return doc.withTimes(mapOf(sceneKey to rewritten), ctx)
    }
}
