package io.github.lenticualrstone.civilight.domain.story.time

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.DayBoundary
import io.github.lenticualrstone.civilight.model.project.SceneNotation
import io.github.lenticualrstone.civilight.model.project.WorldCalendar
import io.github.lenticualrstone.civilight.model.story.CalendarDate
import io.github.lenticualrstone.civilight.model.story.FlashbackMode
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.story.TimePrecision
import io.github.lenticualrstone.civilight.model.ClockTime

/**
 * The scene-time engine of one project state (STO-21, STO-22): resolves every scene's stored
 * [SceneTime] to a [ResolvedTime], memoised per instance. Build a new instance after each edit.
 *
 * Base handling (`ED`): a `기준` whose base is missing, untimed, 회상 or part of a cycle falls back
 * to the nearest earlier live timed non-flashback scene (the R11 §4 base rule); a base in the trash
 * still resolves through its own stored 때, so nothing moves until the delete rebases it (STO-10).
 */
class SceneTimes(val doc: ProjectDoc) {
    val bands: BandTable = BandTable(doc.project.sceneTime.bandStarts)
    private val dayBoundary: DayBoundary = doc.project.sceneTime.dayBoundary.knownOr(DayBoundary.DAWN_0400)
    private val calendar: WorldCalendar? = doc.project.worldTime.calendar

    /** All scene-level units in manuscript order, live and trashed. */
    private val allScenes: List<StoryUnit> = doc.index.allScenesInOrder
    private val positionOf: Map<Key, Int> = allScenes.withIndex().associate { (i, s) -> s.key to i }
    private val memo = HashMap<Key, ResolvedTime>()
    private val visiting = HashSet<Key>()

    /** Live scenes in manuscript order. */
    val liveScenes: List<StoryUnit> get() = doc.manuscript.liveScenes

    fun isLive(sceneKey: Key): Boolean = doc.liveness.isUnitLive(sceneKey)

    /** The resolved 때 of a scene (live or trashed); [ResolvedTime.None] for an unknown key. */
    fun resolve(sceneKey: Key): ResolvedTime {
        memo[sceneKey]?.let { return it }
        val scene = doc.index.unit(sceneKey) ?: return ResolvedTime.None
        if (!visiting.add(sceneKey)) return ResolvedTime.None
        val result = try {
            resolve(scene)
        } finally {
            visiting.remove(sceneKey)
        }
        memo[sceneKey] = result
        return result
    }

    /** The resolved 때 if the scene is in the time chain, else null. */
    fun timed(sceneKey: Key): ResolvedTime.Timed? = resolve(sceneKey) as? ResolvedTime.Timed

    private fun resolve(scene: StoryUnit): ResolvedTime = when (val time = scene.time) {
        SceneTime.None, is SceneTime.Unknown -> ResolvedTime.None
        is SceneTime.Flashback -> ResolvedTime.Flashback(
            year = when (time.mode.known) {
                FlashbackMode.ANCHOR -> time.eventKey?.let { doc.index.event(it)?.worldDate?.year } ?: time.year
                else -> time.year
            },
            eventKey = time.eventKey,
            relation = time.relation?.known,
            band = time.band?.known ?: time.clock?.let(bands::bandOf),
            clock = time.clock,
        )
        is SceneTime.Direct -> {
            val day = time.storyDay ?: time.calendarDate?.let(::storyDayOf)
            if (day == null) {
                ResolvedTime.None
            } else {
                timed(LinExpr.of(day), time.band?.known, time.clock, fixed = true, ownApprox = false, baseKey = null)
            }
        }
        is SceneTime.Relative -> {
            val stored = timed(time.baseKey)?.takeIf { time.baseKey != scene.key }
            val baseKey = if (stored != null) time.baseKey else previousTimed(scene.key)?.key
            val base = stored ?: baseKey?.let(::timed)
            if (base == null) {
                ResolvedTime.None
            } else {
                val lo = minOf(time.dayMin, time.dayMax)
                val hi = maxOf(time.dayMin, time.dayMax)
                val day = (base.day + lo).withVariable(scene.key, hi - lo)
                timed(day, time.band?.known, time.clock, fixed = false, ownApprox = time.precision.known == TimePrecision.APPROX, baseKey = baseKey)
            }
        }
    }

    private fun timed(day: LinExpr, band: Band?, clock: ClockTime?, fixed: Boolean, ownApprox: Boolean, baseKey: Key?) =
        ResolvedTime.Timed(
            day = day,
            t = timeOfDay(band, clock),
            storedBand = band,
            clock = clock,
            band = band ?: clock?.let(bands::bandOf),
            fixed = fixed,
            ownApprox = ownApprox,
            baseKey = baseKey,
        )

    /** Exact time wins over the band; band only → its range; nothing → the whole day. */
    fun timeOfDay(band: Band?, clock: ClockTime?): Span = when {
        clock != null -> Span.point(clock.minutesAfterDayStart())
        band != null -> bands.range(band)
        else -> Span(0, DAY_MINUTES)
    }

    /** Story day of a calendar date (WLD-30 story start = day 1). Null without a calendar. */
    fun storyDayOf(date: CalendarDate): Int? {
        val cal = calendar ?: return null
        val start = cal.storyStart ?: return null
        val yearLength = cal.months.sumOf { it.days }.takeIf { it > 0 } ?: return null
        return date.yearOffset * yearLength + dayOfYear(cal, date.month, date.day) - dayOfYear(cal, start.month, start.day) + 1
    }

    private fun dayOfYear(cal: WorldCalendar, month: Int, day: Int): Int =
        cal.months.take((month - 1).coerceIn(0, cal.months.size)).sumOf { it.days } + day

    // --- Order and neighbours -------------------------------------------------------------------

    /** Manuscript position among all scenes (live and trashed), or null. */
    fun position(sceneKey: Key): Int? = positionOf[sceneKey]

    /**
     * The nearest earlier live scene (manuscript order) that is in the time chain (`기준` default,
     * R11 §4: 회상 and untimed scenes are skipped).
     */
    fun previousTimed(sceneKey: Key): StoryUnit? {
        val at = positionOf[sceneKey] ?: return null
        for (i in at - 1 downTo 0) {
            val s = allScenes[i]
            if (!isLive(s.key)) continue
            if (timed(s.key) != null) return s
        }
        return null
    }

    /** The live scene directly before [sceneKey] in manuscript order. */
    fun previousScene(sceneKey: Key): StoryUnit? {
        val at = positionOf[sceneKey] ?: return null
        for (i in at - 1 downTo 0) if (isLive(allScenes[i].key)) return allScenes[i]
        return null
    }

    /** Live scenes whose stored base is [sceneKey] (direct dependents), manuscript order. */
    fun directDependents(sceneKey: Key): List<StoryUnit> =
        liveScenes.filter { (it.time as? SceneTime.Relative)?.baseKey == sceneKey && it.key != sceneKey }

    /** Every live scene that depends on [sceneKey] through base links, manuscript order. */
    fun dependents(sceneKey: Key): List<StoryUnit> {
        val out = LinkedHashSet<Key>()
        val queue = ArrayDeque(listOf(sceneKey))
        while (queue.isNotEmpty()) {
            val k = queue.removeFirst()
            for (d in directDependents(k)) if (d.key != sceneKey && out.add(d.key)) queue += d.key
        }
        return liveScenes.filter { it.key in out }
    }

    // --- Differences ------------------------------------------------------------------------------

    /** `A(to) − A(from)` in minutes; null when either is not in the chain. */
    fun minutesBetween(fromKey: Key, toKey: Key): Span? {
        val a = timed(fromKey) ?: return null
        val b = timed(toKey) ?: return null
        return minutesBetween(a, b)
    }

    /** `day(to) − day(from)`; null when either is not in the chain. */
    fun daysBetween(fromKey: Key, toKey: Key): LinExpr? {
        val a = timed(fromKey) ?: return null
        val b = timed(toKey) ?: return null
        return daysBetween(a, b)
    }

    /**
     * Time order (STO-22, Q18 `ED`): by the lower bound of the absolute minute, then manuscript
     * order. Scenes not in the chain sort last.
     */
    val timeOrder: Comparator<Key> = compareBy<Key> { timed(it)?.absolute?.min ?: Int.MAX_VALUE }
        .thenBy { positionOf[it] ?: Int.MAX_VALUE }

    fun compareByTime(a: Key, b: Key): Int = timeOrder.compare(a, b)

    // --- Day numbers for labels -----------------------------------------------------------------

    /** +1 when the project day starts at midnight and the clock is between 00:00 and 04:00 (labels only). */
    fun labelDayShift(t: ResolvedTime.Timed): Int =
        if (dayBoundary == DayBoundary.MIDNIGHT && t.clock != null && t.clock.minuteOfDay < DAY_START_MINUTE) 1 else 0

    /** Story day for labels (`이야기 N일째`), with the midnight shift. */
    fun storyDay(sceneKey: Key): Span? = timed(sceneKey)?.let { it.dayRange + labelDayShift(it) }

    /** The first scene of the scene's numbering unit (manuscript order) that is in the chain. */
    fun firstTimedOfEpisode(sceneKey: Key): StoryUnit? {
        val unitKey = doc.index.numberingUnitKeyOfScene(sceneKey) ?: return null
        return allScenes.firstOrNull { s ->
            isLive(s.key) && doc.index.numberingUnitKeyOfScene(s.key) == unitKey && timed(s.key) != null
        }
    }

    /** `N` of `N일째` relative to the episode (`day(X) − day(F) + 1`, F = first timed scene of the episode). */
    fun episodeDay(sceneKey: Key): Span? {
        val x = timed(sceneKey) ?: return null
        val f = firstTimedOfEpisode(sceneKey)?.let { timed(it.key) } ?: x
        return (x.day - f.day).range + 1 + labelDayShift(x) - labelDayShift(f)
    }

    /** True when the project counts `N일째` per episode (episode notation and an episode level). */
    val usesEpisodeDays: Boolean
        get() = doc.project.structure.episodeLevel != null &&
            doc.project.sceneTime.notation.knownOr(SceneNotation.EPISODE) == SceneNotation.EPISODE

    /** The `N` used by short, full and head labels: per episode or per story (STO-17, R11 §6). */
    fun labelDay(sceneKey: Key): Span? = if (usesEpisodeDays) episodeDay(sceneKey) else storyDay(sceneKey)

    /** True when the stored band disagrees with the exact clock (`◇ 시각과 시간대가 다름`, STO-19). */
    fun clockBandMismatch(sceneKey: Key): Boolean {
        val t = timed(sceneKey) ?: return false
        val band = t.storedBand ?: return false
        val clock = t.clock ?: return false
        return bands.bandOf(clock) != band
    }
}
