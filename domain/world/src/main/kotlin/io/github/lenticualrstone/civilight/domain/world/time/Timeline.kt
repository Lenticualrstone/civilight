package io.github.lenticualrstone.civilight.domain.world.time

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.Position
import io.github.lenticualrstone.civilight.model.position.Rank
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.world.EventKind
import io.github.lenticualrstone.civilight.model.world.TimelineEvent

/** `에피소드 기준 · 이야기 시작부터 · 기준 사건`. */
enum class TimelineView { EPISODE, STORY_START, ANCHOR }

/** One row of the 연표 (WLD-31). */
sealed interface TimelineRow {
    /** A world or story event. */
    data class EventRow(
        val event: TimelineEvent,
        val time: TimeLabel,
        val title: String,
        val sub: String,
        val dimmed: Boolean,
    ) : TimelineRow

    /** A chapter span (`1200 · 1장 · 등불지기 · 1-1 ~ 1-10 · …`, or `계획`). */
    data class ChapterRow(
        val chapterKey: Key,
        val time: TimeLabel,
        val title: String,
        val sub: String,
        val planned: Boolean,
        val dimmed: Boolean,
    ) : TimelineRow

    /** `이야기 시작 · 1장`. */
    data class Divider(val text: String) : TimelineRow
}

/** The 연표 screen content. */
data class Timeline(
    /** False in `시간 없이` (module off, T51). */
    val available: Boolean,
    val title: String,
    val subtitle: String,
    /** `달력: 연도만 · 프로젝트 설정에서 월 · 일 달력 추가`. */
    val infoLine: String,
    val viewChips: List<String>,
    val rows: List<TimelineRow>,
)

/**
 * Builds the timeline (WLD-31, WLD-34): world events before the story, `이야기 시작`, chapter spans
 * and story events, then world events during and after the story, all in time order (world events
 * first at equal time, then by their order key). Rows after the lens point are dimmed.
 */
class TimelineBuilder(private val doc: ProjectDoc, times: SceneTimeSource) {
    private val moments = Moments(doc, times)
    private val clock = moments.clock
    private val codes = StoryCodes(doc)
    private val ages = Ages(doc)

    /** Characters whose ages appear on rows: those tagged `주연` (Q-14 default). */
    private val rowCharacters: List<Character> by lazy {
        val lead = doc.project.tags.firstOrNull { it.name == LEAD_TAG }?.key
        doc.cast.characters.filter { it.deletedAt == null && lead != null && lead in it.tagKeys }
            .sortedWith(compareBy({ it.order }, { it.key }))
    }

    /** [rowCharacters] the reader may see: hidden characters are left out while locked. */
    private fun rowCharactersFor(unlocked: Boolean): List<Character> = if (unlocked) rowCharacters else rowCharacters.filterNot { it.hidden }

    fun build(lens: LensPoint = LensPoint.Latest, view: TimelineView = TimelineView.EPISODE, anchor: TimeSubject? = null): Timeline =
        build(lens, view, anchor, hiddenUnlocked = true)

    /**
     * [build] for a reader whose hidden items are open ([hiddenUnlocked]) or locked (WLD-13, rule 19):
     * while locked, ages of hidden characters are left out and hidden places (and regions of hidden
     * places or factions) read `숨긴 항목`. Events keep their rows (they have no hidden flag).
     */
    fun build(lens: LensPoint, view: TimelineView, anchor: TimeSubject?, hiddenUnlocked: Boolean): Timeline {
        val unlocked = hiddenUnlocked
        val available = clock.hasTimeline
        val infoLine = when (clock.mode) {
            WorldTimeMode.CALENDAR -> "달력: ${clock.calendar?.months?.joinToString(Copy.SEP) { it.name }.orEmpty()}"
            else -> "달력: 연도만${Copy.SEP}프로젝트 설정에서 월${Copy.SEP}일 달력 추가"
        }
        val chips = listOf("에피소드 기준", "이야기 시작부터", "기준 사건")
        if (!available) return Timeline(false, "연표", "세계 시간과 이야기 시간", infoLine, chips, emptyList())

        val lensRank = doc.manuscript.rankOf(lens)
        data class Item(val absolute: Long, val tier: Int, val order: String, val row: TimelineRow, val dateless: Boolean = false)
        val items = ArrayList<Item>()
        val events = doc.world.events.filter { it.deletedAt == null }
        var lastStoryAbsolute = Long.MIN_VALUE

        // Chapter spans.
        val chapters = doc.index.children(null).filter { doc.liveness.isLive(it) }
        for (chapter in chapters) {
            val scenes = doc.manuscript.liveScenes.filter { s -> doc.index.ancestors(s).any { it.key == chapter.key } }
            val days = scenes.mapNotNull { (moments.ofScene(it.key) as? Moment.StoryDays) }
            val start = doc.manuscript.rankOf(Position.Start(chapter.key)) ?: Rank.UNPLACED
            val writing = doc.manuscript.writingLineRank
            val planned = writing != null && start > writing
            val episodes = doc.index.children(chapter.key).filter { doc.liveness.isLive(it) }
            val span = if (episodes.isEmpty()) "" else
                "${codes.unitLabel(episodes.first().key)} ~ ${codes.unitLabel(episodes.last().key)}"
            val first = days.minByOrNull { it.dayMin }
            val year = first?.let { clock.yearOfStoryDay(it.dayMin) }
            val subParts = buildList {
                if (span.isNotEmpty()) add(span)
                if (planned || first == null) add("계획")
                else year?.let { y -> ages.line(rowCharactersFor(unlocked), y).takeIf { it.isNotEmpty() }?.let(::add) }
            }
            val absolute = first?.absolute ?: Long.MAX_VALUE / 2
            if (first != null) lastStoryAbsolute = maxOf(lastStoryAbsolute, first.absolute)
            items += Item(
                absolute, 1, chapter.order,
                TimelineRow.ChapterRow(
                    chapterKey = chapter.key,
                    time = TimeLabel(year?.toString() ?: ""),
                    // A hidden chapter keeps only its code while locked (rule 19: no title text).
                    title = "${codes.unitLabel(chapter.key).orEmpty()}${chapter.title?.takeIf { unlocked || !chapter.hidden }?.let { Copy.SEP + it }.orEmpty()}",
                    sub = subParts.joinToString(Copy.SEP),
                    planned = planned || first == null,
                    dimmed = start > lensRank,
                ),
            )
        }

        for (event in events) {
            val moment = moments.ofEvent(event.key)
            val isStory = event.kind.known == EventKind.STORY
            val dimmed = if (isStory) {
                val rank = event.sceneKey?.let { doc.manuscript.rankOf(Position.Start(it)) } ?: Rank.UNPLACED
                rank > lensRank
            } else {
                !doc.manuscript.isVisible(event.revealPoint, lens)
            }
            val time = timeLabel(view, TimeSubject.Event(event.key), anchor, moment)
            items += Item(
                moment?.absolute ?: Long.MAX_VALUE, if (isStory) 2 else 0, event.order,
                dateless = moment == null && !isStory,
                row = if (!unlocked && EventMasks.sceneHidden(doc, event)) {
                    // A story event on a hidden scene: `숨긴 씬`, its episode code only.
                    TimelineRow.EventRow(event, time, EventMasks.HIDDEN_SCENE, event.sceneKey?.let { codes.episodeCode(it) }.orEmpty(), dimmed)
                } else {
                    TimelineRow.EventRow(event, time, event.title, eventSub(event, moment, view, unlocked), dimmed)
                },
            )
        }

        // (0.2.0, WLD-74) A world event with no date (made in place with an empty `때`) is listed after the
        // dated world events before the story, right above `이야기 시작`.
        val storyStart = items.filter { it.tier >= 1 }.minOfOrNull { it.absolute }
        val placed = if (storyStart == null) items else items.map { if (it.dateless) it.copy(absolute = storyStart) else it }
        val sorted = placed.sortedWith(compareBy<Item>({ it.absolute }, { it.tier }, { it.dateless }, { it.order }))
        val rows = ArrayList<TimelineRow>()
        var dividerPlaced = false
        val firstChapter = chapters.firstOrNull()
        for (item in sorted) {
            if (!dividerPlaced && item.tier >= 1) {
                rows += TimelineRow.Divider("이야기 시작${firstChapter?.let { Copy.SEP + codes.unitLabel(it.key).orEmpty() }.orEmpty()}")
                dividerPlaced = true
            }
            rows += item.row
        }
        return Timeline(true, "연표", "세계 시간과 이야기 시간", infoLine, chips, rows)
    }

    private fun timeLabel(view: TimelineView, subject: TimeSubject, anchor: TimeSubject?, moment: Moment?): TimeLabel {
        if (moment == null) return TimeLabel("")
        return when (view) {
            TimelineView.ANCHOR -> anchor?.let { moments.anchorLabel(subject, it) } ?: yearLabel(moment)
            TimelineView.STORY_START -> moments.storyStartLabel(subject) ?: yearLabel(moment)
            TimelineView.EPISODE -> yearLabel(moment)
        }
    }

    private fun yearLabel(moment: Moment): TimeLabel {
        val year = moments.yearOf(moment)?.toString().orEmpty()
        return when {
            moment is Moment.WorldYear && moment.month != null && clock.calendar != null ->
                TimeLabel("$year${Copy.SEP}${clock.dateLabel(CalendarDay(moment.year, moment.month, moment.day ?: 1))}")
            else -> TimeLabel(year)
        }
    }

    private fun eventSub(event: TimelineEvent, moment: Moment?, view: TimelineView, unlocked: Boolean): String {
        val parts = ArrayList<String>()
        val isStory = event.kind.known == EventKind.STORY
        if (isStory) {
            event.sceneKey?.let { scene ->
                codes.episodeCode(scene)?.let(parts::add)
                if (view == TimelineView.EPISODE) {
                    val label = (moments.ofScene(scene) as? Moment.StoryDays)?.let { TimeLabels.storyDay(it.dayMin, it.dayMax).text }
                    label?.let(parts::add)
                }
            }
        } else {
            parts += "세계 사건"
        }
        placeName(event.place, unlocked)?.let(parts::add)
        moment?.let(moments::yearOf)?.let { y -> ages.line(rowCharactersFor(unlocked), y).takeIf { it.isNotEmpty() }?.let(parts::add) }
        if (!isStory) {
            flashbackEpisodes(event.key).takeIf { it.isNotEmpty() }?.let { parts += "${it.joinToString(Copy.SEP)}에서 회상" }
            event.revealPoint?.let { point ->
                val writing = doc.manuscript.writingLineRank
                if (writing != null && doc.manuscript.rankOf(point) > writing) {
                    parts += "${codes.pointLabel(point).orEmpty()}에서 공개 예정"
                }
            }
        }
        return parts.joinToString(Copy.SEP)
    }

    /** Episodes with a live 회상 scene based on this event (`1-4에서 회상`). */
    fun flashbackEpisodes(eventKey: Key): List<String> = doc.manuscript.liveScenes
        .filter { (it.time as? SceneTime.Flashback)?.eventKey == eventKey }
        .mapNotNull { codes.episodeCode(it.key) }
        .distinct()

    private fun placeName(ref: PlaceRef?, unlocked: Boolean): String? =
        if (!unlocked && EventMasks.placeHidden(doc, ref)) EventMasks.HIDDEN_ITEM else when (ref?.kind?.known) {
            PlaceRefKind.PLACE -> doc.index.place(ref.key)?.name
            PlaceRefKind.REGION -> doc.index.region(ref.key)?.second?.name
            null -> null
        }

    companion object {
        const val LEAD_TAG = "주연"
    }
}
