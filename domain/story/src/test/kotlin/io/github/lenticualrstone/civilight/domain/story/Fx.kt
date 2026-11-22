package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.story.TimePrecision
import org.junit.Assert.assertEquals

/** Shared fixture access: the example project and Fixture T scene ids (20-story-time §4.1). */
object Fx {
    val S1 = ExampleKeys.SCENE_1_1_1
    val S2 = ExampleKeys.SCENE_1_2_1
    val S3 = ExampleKeys.SCENE_1_3_1
    val S4 = ExampleKeys.SCENE_1_3_2
    val S5 = ExampleKeys.SCENE_1_4_2
    val S6 = ExampleKeys.SCENE_1_6_1
    val S7 = ExampleKeys.SCENE_1_7_1
    val S8 = ExampleKeys.SCENE_1_7_2
    val S9 = ExampleKeys.SCENE_1_7_3
    val S10 = ExampleKeys.SCENE_1_7_4
    val S11 = ExampleKeys.SCENE_1_8_1
    val S12 = ExampleKeys.SCENE_1_9_2

    fun doc(): ProjectDoc = ExampleProject.build()

    fun labels(doc: ProjectDoc): TimeLabels = TimeLabels(SceneTimes(doc))

    fun rel(base: Key, days: Int, band: Band? = null, clock: ClockTime? = null) =
        SceneTime.Relative(baseKey = base, dayMin = days, dayMax = days, band = band?.open(), clock = clock)

    fun approx(base: Key, min: Int, max: Int, band: Band? = null, clock: ClockTime? = null) = SceneTime.Relative(
        baseKey = base, dayMin = min, dayMax = max, precision = TimePrecision.APPROX.open(), band = band?.open(), clock = clock,
    )

    fun clock(h: Int, m: Int) = ClockTime.of(h, m)

    fun ProjectDoc.unit(key: Key): StoryUnit = index.unit(key)!!

    fun ProjectDoc.withUnit(key: Key, change: (StoryUnit) -> StoryUnit): ProjectDoc =
        copy(story = story.copy(units = story.units.map { if (it.key == key) change(it) else it }))

    fun ProjectDoc.withTime(key: Key, time: SceneTime): ProjectDoc = withUnit(key) { it.copy(time = time) }

    /** A deterministic edit context: keys `01JZ000000TEST` + counter. */
    fun ctx(): EditContext {
        var n = 0
        return EditContext(newKey = { "01JZ000000TEST" + (++n).toString().padStart(12, '0') }, now = 1_800_000_000_000L, device = "test")
    }

    /** Asserts that every live scene except [except] keeps its absolute time range (scene operations). */
    fun assertOtherAbsolutesUnchanged(before: ProjectDoc, after: ProjectDoc, except: Set<Key>) {
        val a = SceneTimes(before)
        val b = SceneTimes(after)
        for (scene in a.liveScenes) {
            if (scene.key in except || !b.isLive(scene.key)) continue
            assertEquals("absolute of ${scene.key}", a.timed(scene.key)?.absolute, b.timed(scene.key)?.absolute)
        }
    }
}
