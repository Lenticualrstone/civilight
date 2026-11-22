package io.github.lenticualrstone.civilight.domain.story.time

import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode

/** How the story-relative day is written (STO-22). */
enum class StoryDayStyle {
    /** `이야기 2일째`; uncertain `약 10일째 (8~12)`. */
    FULL,

    /** `2일째`; uncertain `약 10일째`. */
    COMPACT,
}

/** A label relative to an anchor scene (연표 `기준 사건` view): `기준`, `−1일`, `+약 7일` with sub `5~9일`. */
data class AnchorRelative(val text: String, val rangeText: String?)

/**
 * Every 때 label of STO-22 (R11 §6, R22 §2). The `N` of short, full and head forms is per episode
 * with episode notation, else per story (also for 1-level structures).
 */
class TimeLabels(val times: SceneTimes, val codes: UnitCodes = UnitCodes(times.doc)) {
    private val doc = times.doc

    /** `2일째` or `약 3일째` (midpoint, `.5` up). */
    fun dayWord(span: Span): String = if (span.isPoint) "${span.min}일째" else "약 ${span.midpoint}일째"

    /** The day part of short/full/head labels, or null when the scene is not in the chain. */
    fun dayPart(sceneKey: Key): String? = times.labelDay(sceneKey)?.let(::dayWord)

    /** `회상 · 1195` (`회상` without a year). */
    fun flashback(f: ResolvedTime.Flashback): String = f.year?.let { "회상 · $it" } ?: "회상"

    /** Short form (lists, story time column): `2일째 새벽`; `회상 · 1195`; null without 때. */
    fun short(sceneKey: Key): String? = when (val r = times.resolve(sceneKey)) {
        is ResolvedTime.Timed -> listOfNotNull(dayPart(sceneKey), r.band?.word).joinToString(" ")
        is ResolvedTime.Flashback -> flashback(r)
        ResolvedTime.None -> null
    }

    /** Story list time column: short form or `—`. */
    fun storyColumn(sceneKey: Key): String = short(sceneKey) ?: "—"

    /** Bulk list value: short form or `때 없음`. */
    fun bulkValue(sceneKey: Key): String = short(sceneKey) ?: "때 없음"

    /** Scene head time part: `2일째 아침 06:10`, `약 3일째 새벽`, `회상 · 1195`, `때 없음`. */
    fun head(sceneKey: Key): String = when (val r = times.resolve(sceneKey)) {
        is ResolvedTime.Timed -> listOfNotNull(dayPart(sceneKey), r.band?.word, r.clock?.toString()).joinToString(" ")
        is ResolvedTime.Flashback -> flashback(r)
        ResolvedTime.None -> "때 없음"
    }

    /** Full form (one scene referenced): `1-7 · 2일째 · 새벽`; `2일째 · 낮` without an episode level. */
    fun full(sceneKey: Key): String? {
        val scene = doc.index.unit(sceneKey) ?: return null
        return when (val r = times.resolve(sceneKey)) {
            is ResolvedTime.Timed -> {
                val episode = codes.episodeOf(scene)?.let(codes::episodeCode)
                listOfNotNull(episode, dayPart(sceneKey), r.band?.word).joinToString(" · ")
            }
            is ResolvedTime.Flashback -> flashback(r)
            ResolvedTime.None -> null
        }
    }

    /** Story-relative label (`이야기 2일째`, `약 10일째 (8~12)`; compact `2일째`, `약 10일째`). */
    fun story(sceneKey: Key, style: StoryDayStyle = StoryDayStyle.FULL): String? {
        val day = times.storyDay(sceneKey) ?: return null
        return when {
            day.isPoint && style == StoryDayStyle.FULL -> "이야기 ${day.min}일째"
            day.isPoint -> "${day.min}일째"
            style == StoryDayStyle.FULL -> "약 ${day.midpoint}일째 (${day.min}~${day.max})"
            else -> "약 ${day.midpoint}일째"
        }
    }

    /** Story-relative compact form with the band: `약 12일째 새벽` (cascade preview rows). */
    fun storyShort(sceneKey: Key): String? = when (val r = times.resolve(sceneKey)) {
        is ResolvedTime.Timed -> listOfNotNull(story(sceneKey, StoryDayStyle.COMPACT), r.band?.word).joinToString(" ")
        is ResolvedTime.Flashback -> flashback(r)
        ResolvedTime.None -> null
    }

    /** Relative to an anchor scene: `기준`, `−1일`, `+약 7일` (range sub `5~9일`). */
    fun anchorRelative(sceneKey: Key, anchorSceneKey: Key): AnchorRelative? {
        val diff = times.daysBetween(anchorSceneKey, sceneKey)?.range ?: return null
        if (diff.isPoint) {
            val d = diff.min
            return AnchorRelative(
                when {
                    d == 0 -> "기준"
                    d > 0 -> "+${d}일"
                    else -> "−${-d}일"
                },
                null,
            )
        }
        val mid = diff.midpoint
        val sign = if (mid < 0) "−" else "+"
        return AnchorRelative("${sign}약 ${kotlin.math.abs(mid)}일", "${diff.min}~${diff.max}일")
    }

    /** Script header line: `1-7 · 씬 3 · 2일째 새벽`; `1-4 · 씬 2 · 회상 · 1195`; no 때 → without the time. */
    fun scriptHeader(sceneKey: Key): String? {
        val scene = doc.index.unit(sceneKey) ?: return null
        val episode = codes.episodeOf(scene)?.let(codes::episodeCode)
        return listOfNotNull(episode, codes.sceneLabel(scene), short(sceneKey)).joinToString(" · ")
    }

    /** Sheet result line parts: `1-7 · 2일째 · 새벽` and `이야기 약 13일째`. */
    fun resultLine(sceneKey: Key): Pair<String, String>? {
        val full = full(sceneKey) ?: return null
        val story = story(sceneKey, StoryDayStyle.COMPACT) ?: return null
        return full to "이야기 $story"
    }

    /**
     * 회상 result line (STO-24): `연도 1195 · 대정전 그날` and `이야기 시작 5년 전` (years = start year −
     * flashback year; `이야기 시작 해` when equal, `… n년 뒤` when later, `ED`).
     */
    fun flashbackResult(sceneKey: Key): Pair<String, String>? {
        val scene = doc.index.unit(sceneKey) ?: return null
        val stored = scene.time as? io.github.lenticualrstone.civilight.model.story.SceneTime.Flashback ?: return null
        val r = times.resolve(sceneKey) as? ResolvedTime.Flashback ?: return null
        val year = r.year ?: return null
        val relation = when (r.relation) {
            io.github.lenticualrstone.civilight.model.story.FlashbackRelation.BEFORE -> "전"
            io.github.lenticualrstone.civilight.model.story.FlashbackRelation.AFTER -> "후"
            else -> "그날"
        }
        val event = stored.eventKey?.let { doc.index.event(it)?.title }
        val first = if (event != null) "연도 $year · $event $relation" else "연도 $year"
        val start = doc.project.worldTime.storyStartYear ?: return first to ""
        val diff = start - year
        val second = when {
            diff > 0 -> "이야기 시작 ${diff}년 전"
            diff < 0 -> "이야기 시작 ${-diff}년 뒤"
            else -> "이야기 시작 해"
        }
        return first to second
    }

    /** Travel detail form: `1일째 저녁(17–20)`, `2일째 05:40`. */
    fun travelDetail(sceneKey: Key): String? {
        val r = times.timed(sceneKey) ?: return null
        val day = dayPart(sceneKey) ?: return null
        return when {
            r.clock != null -> "$day ${r.clock}"
            r.band != null -> "$day ${r.band.word}(${times.bands.hoursLabel(r.band)})"
            else -> day
        }
    }

    /**
     * World-calendar date label (STO-18, Q14 `ED`): `<month> <d>일`, a range `<month> <a>일 ~ <b>일`
     * (`<m1> <a>일 ~ <m2> <b>일` across months). Null unless the calendar mode is on.
     */
    fun calendar(sceneKey: Key): String? {
        if (doc.project.worldTime.mode.known != WorldTimeMode.CALENDAR) return null
        val cal = doc.project.worldTime.calendar ?: return null
        val start = cal.storyStart ?: return null
        val yearLength = cal.months.sumOf { it.days }.takeIf { it > 0 } ?: return null
        val days = times.storyDay(sceneKey) ?: return null
        fun dateOf(storyDay: Int): Pair<String, Int> {
            val startOfYear = cal.months.take(start.month - 1).sumOf { it.days } + start.day
            var dayOfYear = Math.floorMod(startOfYear - 1 + storyDay - 1, yearLength)
            for (m in cal.months) {
                if (dayOfYear < m.days) return m.name to dayOfYear + 1
                dayOfYear -= m.days
            }
            return cal.months.last().name to cal.months.last().days
        }
        val (m1, d1) = dateOf(days.min)
        if (days.isPoint) return "$m1 ${d1}일"
        val (m2, d2) = dateOf(days.max)
        return if (m1 == m2) "$m1 ${d1}일 ~ ${d2}일" else "$m1 ${d1}일 ~ $m2 ${d2}일"
    }
}
