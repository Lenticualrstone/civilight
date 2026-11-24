package io.github.lenticualrstone.civilight.feature.library.shelf.flat

import io.github.lenticualrstone.civilight.domain.story.structure.StoryOverview
import io.github.lenticualrstone.civilight.domain.story.text.BlockText
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import java.util.IdentityHashMap
import kotlin.math.max
import kotlin.math.min

/*
 * (0.2.3, r3 page `libviews`, generator `scr_v_lib.py`) The pure parts of the seven flat 서재 views: what a row
 * knows about its project ([RowFacts]), the progress cells, and each view's layout numbers. Everything here is
 * computed from the already-masked ShelfUi rows (a locked hidden project is only the lock item: no name, art,
 * date, place on the time axis, cells or counts).
 */

/** One unit of the progress cells (`진행 칸`, 원고지 progress row): written, being written, planned. */
enum class CellState { DONE, CURRENT, PLANNED }

/**
 * A project's units as cells, one list per chapter (one list when the structure has a single level). Not a
 * done count: a written episode can follow a planned one, so each cell carries its own state.
 */
data class ProgressCells(
    val unit: String,
    val groups: List<List<CellState>>,
    /** The top level's word (`장`) when the cells come in several chapters, for the `1장` labels. */
    val groupWord: String? = null,
) {
    val total: Int get() = groups.sumOf { it.size }
    val done: Int get() = groups.sumOf { g -> g.count { it == CellState.DONE } }

    /** 0 … 100, rounded down. */
    val percent: Int get() = if (total == 0) 0 else 100 * done / total

    /** `에피소드 6 / 20`. */
    val line: String get() = FlatViewCopy.progress(unit, done, total)

    /** The cells in order (chapters joined). */
    val flat: List<CellState> get() = groups.flatten()
}

/**
 * What a project row knows beyond the list's meta line (0.2.3 flat views): live characters of the manuscript,
 * live characters of the cast, the progress cells (null for 설정만 or without a structure), and the last edited
 * place `1-7 · 씬 4 · 결사의 문` (a hidden scene, or a scene under a hidden unit, reads only its code).
 */
data class RowFacts(
    val chars: Int,
    val cast: Int,
    val progress: ProgressCells?,
    val place: String?,
) {
    companion object {
        val NONE = RowFacts(chars = 0, cast = 0, progress = null, place = null)

        fun of(doc: ProjectDoc): RowFacts {
            val overview = StoryOverview(doc)
            val chars = doc.manuscript.liveScenes.sumOf { s -> doc.manuscript.liveBlocksOf(s.key).sumOf(BlockText::charCount) }
            val cast = doc.cast.characters.count { it.deletedAt == null }
            return RowFacts(chars, cast, progress(doc, overview), place(doc, overview))
        }

        private fun progress(doc: ProjectDoc, overview: StoryOverview): ProgressCells? {
            if (doc.project.format.known == ProjectFormat.SETTING_ONLY) return null
            val structure = doc.project.structure
            val current = overview.currentSceneKey
            val chapters = overview.chapters
            val episodeLevel = structure.episodeLevel
            fun state(unit: io.github.lenticualrstone.civilight.model.story.StoryUnit, holdsCurrent: Boolean): CellState = when {
                holdsCurrent && !unit.doneMarked -> CellState.CURRENT
                overview.countsAsWritten(unit) -> CellState.DONE
                else -> CellState.PLANNED
            }
            val groups = (if (episodeLevel != null && chapters.any { it.level < episodeLevel }) {
                chapters.map { ch ->
                    overview.episodesOf(ch).map { ep -> state(ep, current != null && overview.scenesOf(ep).any { it.key == current }) }
                }
            } else if (episodeLevel != null) {
                // Episodes at the top: one row of them.
                listOf(chapters.map { ep -> state(ep, current != null && overview.scenesOf(ep).any { it.key == current }) })
            } else {
                listOf(chapters.flatMap(overview::scenesOf).map { sc -> state(sc, sc.key == current) })
            }).filter { it.isNotEmpty() }
            if (groups.isEmpty()) return null
            val unit = episodeLevel?.let { structure.levels.getOrNull(it) } ?: structure.levels.lastOrNull() ?: FlatViewCopy.SCENE
            return ProgressCells(unit, groups, structure.levels.firstOrNull().takeIf { groups.size > 1 })
        }

        private fun place(doc: ProjectDoc, overview: StoryOverview): String? {
            val sceneKey = overview.currentSceneKey ?: return null
            val scene = doc.index.unit(sceneKey) ?: return null
            val codes = overview.codes
            val episode = codes.episodeOf(scene)?.takeIf { it.key != scene.key }?.let(codes::episodeCode)
            // Rule 19: a hidden scene (or one under a hidden unit) never shows its title over the 서재.
            val hidden = scene.hidden || doc.index.ancestors(scene).any { it.hidden }
            // A name-masked project (LIB-92) keeps its scene titles off the 서재 as its recents do.
            val title = scene.title?.takeIf { !hidden && !doc.project.protections.nameMask && it.isNotBlank() }
            return listOfNotNull(episode, codes.sceneLabel(scene), title).joinToString(" · ")
        }
    }
}

/**
 * [RowFacts] per document, computed once per document instance (a document is immutable; an edit makes a new
 * one), so a 서재 rebuild on a settings or recents change does not walk every manuscript again.
 */
class RowFactsCache {
    private var last = IdentityHashMap<ProjectDoc, RowFacts>()
    private var next = IdentityHashMap<ProjectDoc, RowFacts>()

    @Synchronized
    fun of(doc: ProjectDoc): RowFacts = next[doc] ?: (last[doc] ?: RowFacts.of(doc)).also { next[doc] = it }

    /** Ends a build: documents not asked for since the last [sweep] are forgotten. */
    @Synchronized
    fun sweep() {
        last = next
        next = IdentityHashMap()
    }
}

/** The seven flat views, in the picker's order (r3 `VIEWS`). */
object FlatViews {
    val styles: List<LibraryStyle> = listOf(
        LibraryStyle.TIMELINE,
        LibraryStyle.MANUSCRIPT_GRID,
        LibraryStyle.MAILBOX,
        LibraryStyle.PROGRESS_CELLS,
        LibraryStyle.COVER_WALL,
        LibraryStyle.POSTER,
        LibraryStyle.TABLE,
    )

    fun isFlat(style: LibraryStyle): Boolean = style in styles

    /**
     * Cover boards open a project on its first tap where they draw no detail block (원고지, 진행 칸, 표지 벽, 표);
     * the others, and every inner board, pick first and open from the detail's `열기`.
     */
    fun opensOnTap(style: LibraryStyle, wide: Boolean): Boolean =
        !wide && (style == LibraryStyle.MANUSCRIPT_GRID || style == LibraryStyle.PROGRESS_CELLS || style == LibraryStyle.COVER_WALL || style == LibraryStyle.TABLE)

    /** Total characters over the drawn projects only (a locked project adds nothing). */
    fun totalChars(items: List<ShelfCardItem>): Int =
        items.sumOf { (it as? ShelfCardItem.Project)?.card?.row?.facts?.chars ?: 0 }

    /** `프로젝트 N · 잠김` (the lock word only while a locked group exists). */
    fun countLine(items: List<ShelfCardItem>): String {
        val n = items.count { it is ShelfCardItem.Project }
        val locked = items.any { it is ShelfCardItem.Lock }
        return FlatViewCopy.projects(n) + if (locked) " · ${FlatViewCopy.LOCKED}" else ""
    }

    /** The index after a step of [delta], held inside 0 until [count]. */
    fun step(index: Int, delta: Int, count: Int): Int = if (count <= 0) 0 else (index + delta).coerceIn(0, count - 1)
}

/** 타임라인 띠: six equal segments, recent on the left (board `SEGS`). */
object TimelineLayout {
    /** Segment starts and ends in days. */
    val SEGMENTS: List<Pair<Int, Int>> = listOf(0 to 1, 1 to 2, 2 to 7, 7 to 14, 14 to 30, 30 to 90)

    fun segmentOf(days: Int): Int = SEGMENTS.indexOfFirst { (_, b) -> days < b }.let { if (it < 0) SEGMENTS.size - 1 else it }

    /** The x of an item [ageDays] old (calendar days + the fraction of the day) between [x0] and [x1]. */
    fun x(ageDays: Float, x0: Float, x1: Float): Float {
        val w = (x1 - x0) / SEGMENTS.size
        val k = segmentOf(ageDays.toInt().coerceAtLeast(0))
        val (a, b) = SEGMENTS[k]
        val t = ((ageDays - a) / (b - a)).coerceIn(0f, 1f)
        return x0 + w * (k + .15f + .7f * t)
    }

    /**
     * The card centres along a zone [zoneW] wide: alternating above (even) and below (odd) the axis, pushed right
     * where two on one side would overlap ([cardW] + 6 apart), and the zone widened (it scrolls) when they do not
     * fit. Returns the centres and the zone width used. The lock card is placed apart by the caller.
     */
    fun place(ages: List<Float>, zoneW: Float, cardW: Float, lockW: Float): Pair<List<Float>, Float> {
        val gap = cardW + 6f
        val half = (cardW + 14f) / 2f + 4f
        // The busier side needs (m − 1) gaps between its first and last centre: widen (and scroll) only for that.
        val perSide = (ages.size + 1) / 2
        val width = max(zoneW, (perSide - 1).coerceAtLeast(0) * gap + 2f * half + lockW)
        val x0 = 12f
        val x1 = width - lockW
        val hi = max(half, x1 - half)
        val xs = ages.map { x(it, x0, x1).coerceIn(half, hi) }.toMutableList()
        for (side in 0..1) {
            val idx = xs.indices.filter { it % 2 == side }
            // Push right where two overlap, then back left from the right bound: old projects stay old.
            var prev = Float.NEGATIVE_INFINITY
            for (i in idx) { xs[i] = max(xs[i], prev + gap); prev = xs[i] }
            var limit = hi
            for (i in idx.asReversed()) { xs[i] = min(xs[i], limit); limit = xs[i] - gap }
        }
        return xs to width
    }

    /** The up-card stagger: board `top: 10 + (i // 2 % 2) * 26`. */
    fun stagger(i: Int): Float = 10f + ((i / 2) % 2) * 26f
}

/** 표지 벽: columns with weights (board `WALL_W`), the lock and `새 프로젝트` tiles at 0.7. */
object WallLayout {
    val WEIGHTS = listOf(1.5f, 1.05f, 1.25f, 1.2f, .95f, 1.35f, 1.1f)
    const val SMALL = .7f

    /** The smallest tile (dp): a tall column scrolls instead of shrinking tiles below it. */
    const val MIN_TILE = 112f

    fun weight(index: Int, item: ShelfCardItem): Float =
        if (item is ShelfCardItem.Project) WEIGHTS[index % WEIGHTS.size] else SMALL

    /** Round-robin into [cols] columns (board cover `[[0, 2, 4, 6], [1, 3, 5, L]]`). */
    fun columns(count: Int, cols: Int): List<List<Int>> = (0 until cols).map { c -> (c until count step cols).toList() }

    /** dp per weight unit: fills [height] when the heaviest column fits, else the smallest tile keeps [MIN_TILE]. */
    fun unit(weights: List<List<Float>>, height: Float, gap: Float): Float {
        val fill = weights.filter { it.isNotEmpty() }.minOfOrNull { col -> (height - gap * (col.size - 1)) / col.sum() } ?: return MIN_TILE
        val minW = weights.flatten().minOrNull() ?: 1f
        return max(fill, MIN_TILE / minW)
    }
}

/** Rows that fill the height (board `repeat(N, 1fr)`) down to a smallest row, then scroll. */
object FillRows {
    fun height(count: Int, room: Float, gap: Float, min: Float): Float {
        if (count <= 0) return min
        return max(min, (room - gap * (count - 1)) / count)
    }
}

/** 표 sorting (board `order()`): the lock row always last, ties keep the 서재 order. */
enum class TableSort { NAME, FORM, PROGRESS, CHARS, EDITED }

object TableOrder {
    /** First tap on a column: names and forms ascending, numbers descending, 고친 때 newest first. */
    fun firstDescending(sort: TableSort): Boolean = sort == TableSort.CHARS || sort == TableSort.PROGRESS

    fun toggle(current: TableSort, descending: Boolean, tapped: TableSort): Pair<TableSort, Boolean> =
        if (current == tapped) tapped to !descending else tapped to firstDescending(tapped)

    fun order(items: List<ShelfCardItem>, sort: TableSort, descending: Boolean): List<ShelfCardItem> {
        val projects = items.withIndex().filter { it.value is ShelfCardItem.Project }
        val rest = items.filter { it !is ShelfCardItem.Project }
        val cmp = Comparator<IndexedValue<ShelfCardItem>> { a, b ->
            val ra = (a.value as ShelfCardItem.Project).card.row
            val rb = (b.value as ShelfCardItem.Project).card.row
            val c = when (sort) {
                TableSort.NAME -> ra.title.compareTo(rb.title)
                TableSort.FORM -> formOrder(ra.format).compareTo(formOrder(rb.format))
                TableSort.PROGRESS -> (ra.facts?.progress?.percent ?: -1).compareTo(rb.facts?.progress?.percent ?: -1)
                TableSort.CHARS -> (ra.facts?.chars ?: 0).compareTo(rb.facts?.chars ?: 0)
                // The 서재 order is newest first: by index, so ascending = newest first.
                TableSort.EDITED -> a.index.compareTo(b.index)
            }
            val d = if (descending) -c else c
            if (d != 0) d else a.index.compareTo(b.index)
        }
        return projects.sortedWith(cmp).map { it.value } + rest
    }

    private fun formOrder(f: ProjectFormat?): Int = when (f) {
        ProjectFormat.SERIAL, null -> 0
        ProjectFormat.SHORT -> 1
        ProjectFormat.SETTING_ONLY -> 2
    }
}

/** 원고지: a tile of [cols] × [rows] cells; the name one glyph a cell (two lines at most, `…` cut). */
object PaperGrid {
    fun nameRows(name: String, cols: Int): List<String> {
        val chars = name.toList()
        val cut = if (chars.size > 2 * cols) chars.take(2 * cols - 1) + '…' else chars
        return cut.chunked(cols).map { it.joinToString("") }
    }

    /** `48,210` as glyphs, then the unit cell `자`; null when it does not fit in [cols]. */
    fun countGlyphs(chars: Int, cols: Int): List<String>? {
        val g = FlatViewCopy.number(chars).map { it.toString() } + FlatViewCopy.CHAR_UNIT
        return g.takeIf { it.size <= cols }
    }

    /** The progress row: [cols] cells, the done share filled, the next one marked current. */
    fun progressRow(cells: ProgressCells, cols: Int): List<CellState> {
        val t = cells.total
        val done = if (t == 0) 0 else (cols * cells.done + t / 2) / t
        return (0 until cols).map { k -> if (k < done) CellState.DONE else if (k == done && done < cols && cells.flat.any { it == CellState.CURRENT }) CellState.CURRENT else CellState.PLANNED }
    }
}
