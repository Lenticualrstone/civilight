package io.github.lenticualrstone.civilight.feature.world.time

import io.github.lenticualrstone.civilight.domain.world.time.StoryEngine
import io.github.lenticualrstone.civilight.domain.world.time.TimeSubject
import io.github.lenticualrstone.civilight.domain.world.time.TimelineBuilder
import io.github.lenticualrstone.civilight.domain.world.time.TimelineRow
import io.github.lenticualrstone.civilight.domain.world.time.TimelineView
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.world.EventKind
import io.github.lenticualrstone.civilight.ui.lens.LensRules
import io.github.lenticualrstone.civilight.model.position.LensPoint

/** What a 연표 row is: its dot and title colour follow it (board `Timeline`). */
enum class TimelineRowKind { WORLD_EVENT, STORY_EVENT, CHAPTER, DIVIDER }

/**
 * One line of the 연표 (WLD-31): time label (mono, 64 wide) with an optional range under it
 * ([timeSub], `5~9일`), title, sub line. [current] = the row of the lens (its dot in `acc`).
 * [eventKey] rows open the event detail; chapter rows and the divider do not open anything.
 */
data class TimelineRowUi(
    val id: String,
    val kind: TimelineRowKind,
    val time: String,
    val timeSub: String?,
    val title: String,
    val sub: String,
    val dimmed: Boolean,
    val current: Boolean,
    val eventKey: Key?,
)

/** A view chip `에피소드 기준 · 이야기 시작부터 · 기준 사건 · <name>`. */
data class ViewChipUi(val view: TimelineView, val label: String, val selected: Boolean)

/** Everything the 연표 screen draws. */
data class TimelineUi(
    /** False in `시간 없이`: the module is off (WLD-29, WLD-T51). */
    val available: Boolean,
    val title: String,
    val smallLine: String,
    val infoLine: String,
    val chips: List<ViewChipUi>,
    val rows: List<TimelineRowUi>,
    /** `사건 · 연도 찾기`, in the anchor view `사건 · 때 찾기`. */
    val searchHint: String,
    /** `대략인 때는 범위(5~9일)로 · 점검도 범위로` in the anchor view. */
    val footer: String?,
)

/**
 * Builds the 연표 (WLD-31; the view chips of WLD-34 read the same domain values) from
 * `domain:world` [TimelineBuilder] at the lens. While hidden items are locked, hidden places read
 * `숨긴 항목` and hidden characters' ages are left out (WLD-13).
 */
object TimelineState {
    /** The view the 연표 opens in: the project's `때 표기` (WLD-34; `기준 사건` needs a pick first). */
    fun defaultView(doc: ProjectDoc): TimelineView =
        when (doc.project.sceneTime.notation.known) {
            io.github.lenticualrstone.civilight.model.project.SceneNotation.STORY_START -> TimelineView.STORY_START
            else -> TimelineView.EPISODE
        }

    fun build(doc: ProjectDoc, view: TimelineView, anchorEventKey: Key?, hiddenOpen: Boolean): TimelineUi {
        // (0.2.0, S-1, WLD-71) No lens chip here: the 연표 reads at `최신`.
        val lens: LensPoint = LensPoint.Latest
        val anchorEvent = anchorEventKey?.let { doc.index.event(it) }?.takeIf { it.deletedAt == null }
        // Without a live anchor event the anchor view has nothing to measure from: fall back to 에피소드 기준.
        val shownView = if (view == TimelineView.ANCHOR && anchorEvent == null) TimelineView.EPISODE else view
        val anchor = anchorEvent?.let { TimeSubject.Event(it.key) }
        val timeline = TimelineBuilder(doc, StoryEngine(doc)).build(lens, shownView, anchor, hiddenUnlocked = hiddenOpen)
        val rows = timeline.rows.mapIndexed { i, row -> rowUi(i, row) }
        val currentId = rows.lastOrNull { it.kind == TimelineRowKind.CHAPTER && !it.dimmed }?.id
        val chips = TimelineView.entries.map { v ->
            val label = when (v) {
                TimelineView.EPISODE -> timeline.viewChips[0]
                TimelineView.STORY_START -> timeline.viewChips[1]
                TimelineView.ANCHOR -> anchorEvent?.let { WorldCopy.anchorChip(timeline.viewChips[2], it.title) } ?: timeline.viewChips[2]
            }
            ViewChipUi(v, label, selected = v == shownView)
        }
        return TimelineUi(
            available = timeline.available,
            title = timeline.title,
            smallLine = timeline.subtitle,
            infoLine = timeline.infoLine,
            chips = chips,
            rows = rows.map { if (it.id == currentId) it.copy(current = true) else it },
            searchHint = if (shownView == TimelineView.ANCHOR) WorldCopy.SEARCH_TIMELINE_ANCHOR else WorldCopy.SEARCH_TIMELINE,
            footer = if (shownView == TimelineView.ANCHOR) WorldCopy.ANCHOR_RANGE_NOTE else null,
        )
    }

    private fun rowUi(i: Int, row: TimelineRow): TimelineRowUi = when (row) {
        is TimelineRow.Divider -> TimelineRowUi("div:$i", TimelineRowKind.DIVIDER, "", null, row.text, "", dimmed = false, current = false, eventKey = null)
        is TimelineRow.ChapterRow -> TimelineRowUi(
            id = "ch:${row.chapterKey}",
            kind = TimelineRowKind.CHAPTER,
            time = row.time.text,
            timeSub = row.time.sub,
            title = row.title,
            sub = row.sub,
            dimmed = row.dimmed,
            current = false,
            eventKey = null,
        )
        is TimelineRow.EventRow -> TimelineRowUi(
            id = "ev:${row.event.key}",
            kind = if (row.event.kind.known == EventKind.STORY) TimelineRowKind.STORY_EVENT else TimelineRowKind.WORLD_EVENT,
            time = row.time.text,
            timeSub = row.time.sub,
            title = row.title,
            sub = row.sub,
            dimmed = row.dimmed,
            current = false,
            eventKey = row.event.key,
        )
    }
}
