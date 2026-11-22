package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.domain.story.time.OrderChecks
import io.github.lenticualrstone.civilight.domain.story.time.ResolvedTime
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.model.IntSpan
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * The place & time engine of `domain:story` as a [SceneTimeSource]. This file is the only place the
 * script reads that engine, so a change there touches nothing else here. Build a new source for
 * every doc state (the engine memoises per instance).
 */
object StoryTimes {
    /** Resolved 때 of every scene of [doc]. */
    fun source(doc: ProjectDoc): SceneTimeSource = from(SceneTimes(doc))

    fun from(times: SceneTimes): SceneTimeSource = SceneTimeSource { key -> resolve(times, key) }

    /**
     * One scene: story days with the label shift, `N일째` (episode or story notation, the midpoint
     * when it is a range), band and clock, precision from the scene's own link (a wide day range
     * inherited from an earlier `대략` link is still `정확` here), and the time of day after 04:00.
     */
    fun resolve(times: SceneTimes, sceneKey: Key): ResolvedSceneTime? = when (val r = times.resolve(sceneKey)) {
        ResolvedTime.None -> null
        is ResolvedTime.Flashback -> ResolvedSceneTime(band = r.band, clock = r.clock, flashback = true, flashbackYear = r.year)
        is ResolvedTime.Timed -> {
            val label = times.labelDay(sceneKey)
            ResolvedSceneTime(
                storyDay = times.storyDay(sceneKey)?.let { IntSpan(it.min, it.max) },
                episodeDay = label?.let { if (it.isPoint) it.min else it.midpoint },
                episodeDayApprox = label != null && !label.isPoint,
                band = r.band,
                clock = r.clock,
                approx = r.ownApprox,
                tRange = IntSpan(r.t.min, r.t.max),
            )
        }
    }

    /** Scenes of [doc] whose time order disagrees with the manuscript order (`◇ 순서와 어긋남`). */
    fun orderMismatches(doc: ProjectDoc): Int = OrderChecks.notices(SceneTimes(doc)).size

    /** Mismatches present in [after] but not in [before] (the scene-move preview's `◇ {o}`). */
    fun newOrderMismatches(before: ProjectDoc, after: ProjectDoc): Int =
        OrderChecks.newNotices(SceneTimes(before), SceneTimes(after)).size
}
