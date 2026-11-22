package io.github.lenticualrstone.civilight.domain.world.time

import io.github.lenticualrstone.civilight.domain.story.scene.Appearance
import io.github.lenticualrstone.civilight.domain.story.time.ResolvedTime
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.FlashbackRelation

/**
 * The world area's view of the time and cast engine of `domain:story` (장소와 때): resolved scene
 * times, scene-to-scene day offsets with shared `대략` links cancelled, and who is in a scene
 * (speakers of live dialogue and choice lines plus the silent cast).
 */
class StoryEngine(private val doc: ProjectDoc) : SceneTimeSource, SceneCastSource {
    private val times = SceneTimes(doc)
    private val labels = TimeLabels(times)

    override fun timeOf(sceneKey: Key): SceneWhen? = when (val r = times.resolve(sceneKey)) {
        is ResolvedTime.Timed -> {
            val day = times.storyDay(sceneKey) ?: r.dayRange
            SceneWhen.Story(
                dayMin = day.min,
                dayMax = day.max,
                minuteMin = r.t.min,
                minuteMax = r.t.max,
                episodeLabel = labels.short(sceneKey),
            )
        }
        is ResolvedTime.Flashback -> SceneWhen.Flashback(
            year = r.year,
            eventKey = r.eventKey,
            label = flashbackLabel(r),
        )
        ResolvedTime.None -> null
    }

    override fun daysBetween(fromKey: Key, toKey: Key): IntRange? =
        times.daysBetween(fromKey, toKey)?.range?.let { it.min..it.max }

    override fun castOf(sceneKey: Key): List<Key> = Appearance.of(doc, sceneKey).all

    /** `대정전 그날`, `대정전 전`, `대정전 뒤` (relation words `[PROPOSED]`), else `회상 · 1195`. */
    private fun flashbackLabel(r: ResolvedTime.Flashback): String {
        val event = r.eventKey?.let { doc.index.event(it) }?.title
        return when {
            event == null -> labels.flashback(r)
            r.relation == FlashbackRelation.BEFORE -> "$event 전"
            r.relation == FlashbackRelation.AFTER -> "$event 뒤"
            else -> "$event 그날"
        }
    }
}
