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
     * (`1일째 아침`) used by the `에피소드 기준` view and by place-time rows.
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

    /**
     * `day(to) − day(from)` as a range with the engine's shared unknowns cancelled (a `대략` link
     * counted once), or null when the source cannot tell. The default subtracts the day ranges,
     * which over-widens when both scenes hang on the same approximate link.
     */
    fun daysBetween(fromKey: Key, toKey: Key): IntRange? {
        val a = timeOf(fromKey) as? SceneWhen.Story ?: return null
        val b = timeOf(toKey) as? SceneWhen.Story ?: return null
        return (b.dayMin - a.dayMax)..(b.dayMax - a.dayMin)
    }

    companion object {
        /** No scene has a time. */
        val NONE: SceneTimeSource = SceneTimeSource { null }

        /** A fixed table (tests, previews). */
        fun of(times: Map<Key, SceneWhen>): SceneTimeSource = SceneTimeSource { times[it] }

        /** [base] with some scenes replaced (a test's or a preview's Given). */
        fun overriding(base: SceneTimeSource, overrides: Map<Key, SceneWhen?>): SceneTimeSource = object : SceneTimeSource {
            override fun timeOf(sceneKey: Key): SceneWhen? = if (sceneKey in overrides) overrides[sceneKey] else base.timeOf(sceneKey)

            override fun daysBetween(fromKey: Key, toKey: Key): IntRange? =
                if (fromKey in overrides || toKey in overrides) {
                    val a = timeOf(fromKey) as? SceneWhen.Story
                    val b = timeOf(toKey) as? SceneWhen.Story
                    if (a == null || b == null) null else (b.dayMin - a.dayMax)..(b.dayMax - a.dayMin)
                } else {
                    base.daysBetween(fromKey, toKey)
                }
        }
    }
}

/** Port to the cast engine: who is in a scene (speakers and silent cast, owned by 장소와 때). */
fun interface SceneCastSource {
    fun castOf(sceneKey: Key): List<Key>

    companion object {
        /** A fixed table (tests, previews). */
        fun of(cast: Map<Key, List<Key>>): SceneCastSource = SceneCastSource { cast[it].orEmpty() }
    }
}
