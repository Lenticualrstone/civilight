package io.github.lenticualrstone.civilight.domain.world.time

import io.github.lenticualrstone.civilight.model.Key

/**
 * A scene's resolved story time as the time engine (장소와 때, `domain:story`) supplies it. The
 * world area never resolves `때` itself; it only orders and labels what it is given.
 */
sealed interface SceneWhen {
    /**
     * In the time chain: story days, 1-based (`1일째`), inclusive bounds; [minuteMin]/[minuteMax]
     * are minutes after 04:00 when known. [episodeLabel] is the engine's per-episode label
     * (`1일째 아침`) used by the `에피소드 기준` view.
     */
    data class Story(
        val dayMin: Int,
        val dayMax: Int = dayMin,
        val minuteMin: Int? = null,
        val minuteMax: Int? = null,
        val episodeLabel: String? = null,
    ) : SceneWhen {
        val approx: Boolean get() = dayMin != dayMax
    }

    /** 회상, placed at its own era (rule 11 §4); [eventKey] gives the year when [year] is absent. */
    data class Flashback(
        val year: Int? = null,
        val month: Int? = null,
        val day: Int? = null,
        val eventKey: Key? = null,
        /** `대정전 그날` and similar, for sub lines. */
        val label: String? = null,
    ) : SceneWhen
}

/** Port to the time engine: the resolved time of a scene, or null when it has none. */
fun interface SceneTimeSource {
    fun timeOf(sceneKey: Key): SceneWhen?

    companion object {
        /** No scene has a time. */
        val NONE: SceneTimeSource = SceneTimeSource { null }

        /** A fixed table (tests, previews). */
        fun of(times: Map<Key, SceneWhen>): SceneTimeSource = SceneTimeSource { times[it] }
    }
}
