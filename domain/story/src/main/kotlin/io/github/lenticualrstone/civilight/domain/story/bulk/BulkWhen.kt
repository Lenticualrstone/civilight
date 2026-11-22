package io.github.lenticualrstone.civilight.domain.story.bulk

import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.Span
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.domain.story.time.bandValue
import io.github.lenticualrstone.civilight.domain.story.time.clockValue
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.domain.story.withTimes
import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.TimePrecision

/** `직전 씬 기준 차례로` · `모두 같은 때`. */
enum class WhenMode { CHAIN, ALL_SAME }

/** `시간대` chips: `그대로` · `비워 둠` · a band. */
sealed interface BandChoice {
    data object Keep : BandChoice
    data object Clear : BandChoice
    data class Set(val band: Band) : BandChoice
}

/** A `미리 보기` row: `1-6 씬 2 | 1-6 · 2일째 · 대략 | 직전 1-6 씬 1 · 1일째 낮`. */
data class WhenPreviewRow(val sceneKey: Key, val scene: String, val label: String, val baseNote: String) {
    val line: String get() = "$scene | $label | $baseNote"
}

/** The `때 정하기 · 씬 4개` sheet's preview and its result (one undo step = the previous doc). */
data class WhenPlan(val title: String, val rows: List<WhenPreviewRow>, val button: String, val result: ProjectDoc)

/**
 * `때 정하기` (STO-33). Selected scenes are processed in manuscript order; with [WhenMode.CHAIN] each
 * gets base = its nearest earlier timed non-flashback scene **after** the earlier selected scenes got
 * their 때, so a chain forms through the selection. Overwriting a scene with dependents shows no
 * cascade preview; its dependents keep base + interval (`ED`, Q24). Nothing is stored before the
 * caller takes [WhenPlan.result].
 */
object BulkWhen {
    /** `대략` range of a day chip (STO-21 `ED`, Q17): `+n` → `[n−1, n+1]` clipped at 0, `같은 날` → `[0, 1]`. */
    fun approxRange(n: Int): Span = when {
        n == 0 -> Span(0, 1)
        n > 0 -> Span(maxOf(0, n - 1), n + 1)
        else -> Span(n - 1, n + 1)
    }

    /**
     * `<nearest earlier timed scene> 기준 · [days]` with the time of day; `대략` turns a nominal
     * point into [approxRange]. Without an earlier timed scene the result is `직접` from story day 1
     * (`ED`).
     */
    fun relativeTo(times: SceneTimes, sceneKey: Key, days: Span, band: Band?, clock: ClockTime?, precision: TimePrecision): SceneTime {
        val base = times.previousTimed(sceneKey)
            ?: return SceneTime.Direct(storyDay = 1 + days.min, band = band?.open(), clock = clock)
        val range = if (precision == TimePrecision.APPROX && days.isPoint) approxRange(days.min) else days
        return SceneTime.Relative(
            baseKey = base.key,
            dayMin = range.min,
            dayMax = range.max,
            precision = (if (range.isPoint && precision == TimePrecision.EXACT) TimePrecision.EXACT else TimePrecision.APPROX).open(),
            band = band?.open(),
            clock = clock,
        )
    }

    /**
     * @param dayChip `같은 날` = 0, `+1일`, `+2일`.
     * @param perScene `씬마다 고르기`: chip per scene (missing = [dayChip]).
     */
    fun plan(
        doc: ProjectDoc,
        selected: List<Key>,
        mode: WhenMode = WhenMode.CHAIN,
        dayChip: Int = 1,
        band: BandChoice = BandChoice.Keep,
        precision: TimePrecision = TimePrecision.APPROX,
        perScene: Map<Key, Int> = emptyMap(),
        ctx: EditContext? = null,
    ): WhenPlan {
        val original = SceneTimes(doc)
        val codes = UnitCodes(doc)
        val ordered = original.liveScenes.map { it.key }.filter { it in selected }
        var working = doc
        var nominal = doc
        val first = ordered.firstOrNull()
        for (key in ordered) {
            val stored = doc.index.unit(key)!!.time
            val (b, c) = when (band) {
                BandChoice.Keep -> stored.bandValue?.known to stored.clockValue
                BandChoice.Clear -> null to null
                is BandChoice.Set -> band.band to stored.clockValue?.takeIf { original.bands.bandOf(it) == band.band }
            }
            val n = Span.point(perScene[key] ?: dayChip)
            if (mode == WhenMode.ALL_SAME && key != first) {
                fun sameAsFirst(d: ProjectDoc): SceneTime {
                    val f = d.index.unit(first!!)!!.time
                    return SceneTime.Relative(baseKey = first, dayMin = 0, dayMax = 0, band = f.bandValue, clock = f.clockValue)
                }
                working = working.withTimes(mapOf(key to sameAsFirst(working)), ctx)
                nominal = nominal.withTimes(mapOf(key to sameAsFirst(nominal)))
            } else {
                working = working.withTimes(mapOf(key to relativeTo(SceneTimes(working), key, n, b, c, precision)), ctx)
                nominal = nominal.withTimes(mapOf(key to relativeTo(SceneTimes(nominal), key, n, b, c, TimePrecision.EXACT)))
            }
        }
        val nominalTimes = SceneTimes(nominal)
        val nominalLabels = TimeLabels(nominalTimes, UnitCodes(nominal))
        val originalLabels = TimeLabels(original, codes)
        val rows = ordered.map { key ->
            val label = (nominalLabels.full(key) ?: "때 없음") + if (precision == TimePrecision.APPROX) " · 대략" else ""
            WhenPreviewRow(key, codes.sceneRef(key).orEmpty(), label, baseNote(key, nominalTimes, original, originalLabels, codes))
        }
        val count = ordered.size
        return WhenPlan("때 정하기 · 씬 ${count}개", rows, "씬 ${count}개에 넣기", working)
    }

    /**
     * `직전 1-6 씬 1 · 1일째 낮` (base = the scene directly before, timed before this sheet),
     * `직전 1-8 씬 1` (it gets its 때 in the same sheet), `1-7 씬 3 기준 · 직전 씬 때 없음` (the
     * scene before has no 때), `1-3 씬 2 기준 · 직전 씬 회상` (`ED`).
     */
    private fun baseNote(key: Key, nominal: SceneTimes, original: SceneTimes, labels: TimeLabels, codes: UnitCodes): String {
        val base = nominal.timed(key)?.baseKey ?: return "직접"
        val previous = nominal.previousScene(key)
        val ref = codes.sceneRef(base).orEmpty()
        if (previous?.key == base) {
            val before = labels.short(base)?.takeIf { original.timed(base) != null }
            return listOfNotNull("직전 $ref", before).joinToString(" · ")
        }
        val why = if (previous?.time is SceneTime.Flashback) "직전 씬 회상" else "직전 씬 때 없음"
        return "$ref 기준 · $why"
    }
}
