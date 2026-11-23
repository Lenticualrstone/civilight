package io.github.lenticualrstone.civilight.feature.story.home

import io.github.lenticualrstone.civilight.domain.story.search.CommandBar
import io.github.lenticualrstone.civilight.domain.story.search.CommandScreen
import io.github.lenticualrstone.civilight.domain.story.structure.SceneStatus
import io.github.lenticualrstone.civilight.domain.story.structure.StoryOverview
import io.github.lenticualrstone.civilight.domain.story.structure.StructureEdit
import io.github.lenticualrstone.civilight.domain.story.structure.resumeTarget
import io.github.lenticualrstone.civilight.feature.story.common.StoryCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.navigation.CanvasBoard
import io.github.lenticualrstone.civilight.navigation.CastList
import io.github.lenticualrstone.civilight.navigation.CheckCenter
import io.github.lenticualrstone.civilight.navigation.Gallery
import io.github.lenticualrstone.civilight.navigation.PageList
import io.github.lenticualrstone.civilight.navigation.ProjectSettings
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.navigation.StoryStructure
import io.github.lenticualrstone.civilight.navigation.Timeline
import io.github.lenticualrstone.civilight.navigation.World
import io.github.lenticualrstone.civilight.ui.lens.LensChipLabel
import io.github.lenticualrstone.civilight.ui.lens.LensRules
import io.github.lenticualrstone.civilight.ui.image.CoverUi
import io.github.lenticualrstone.civilight.ui.image.ProjectCovers
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** One stat of the home head: mono number over a small label (`7/10` · `1장 에피소드`). */
data class HomeStat(val value: String, val label: String)

/** `이어 쓰기`: `1-7 씬 3 · 꺼진 등불`, the quoted first line, and the block it opens with the caret at its end. */
data class HomeResume(val where: String, val quote: String, val blockKey: Key)

/**
 * A row of the current chapter list: `1-7 | 잿빛 새벽의 등불 | 쓰는 중 · 씬 3`. [current] = the
 * episode of the last edit (bold title); [writing] = status `쓰는 중 …` (`acc`).
 */
data class HomeRow(
    val key: Key,
    val code: String,
    val title: String,
    val status: String,
    val current: Boolean,
    val writing: Boolean,
    /** An unlocked hidden scene (1-level rows): small `숨김` tag (STO-11). A locked one reads `숨긴 씬`. */
    val hiddenTag: Boolean = false,
)

/**
 * The `작업` shortcuts (STO-01, GAL-03): `스토리 · 인물 · 세계 · 연표 · 캔버스 · 갤러리 · 페이지 · 점검 센터 · 설정`;
 * `설정` keeps the last place. Home draws them as DS list rows with `›` (QA U-07); [dim] and
 * [HomeModel.shortcutRows] describe the former text grid and are no longer drawn. `페이지` (CNV-14) and
 * `점검 센터` (CNV-17) are the entry points QA 2026-11-23 found missing.
 */
enum class HomeShortcut(val label: String, val dim: Boolean = false) {
    STORY(HomeCopy.SHORTCUT_STORY),
    CAST(HomeCopy.SHORTCUT_CAST),
    WORLD(HomeCopy.SHORTCUT_WORLD),
    TIMELINE(HomeCopy.SHORTCUT_TIMELINE),
    CANVAS(HomeCopy.SHORTCUT_CANVAS),
    GALLERY(HomeCopy.SHORTCUT_GALLERY),
    PAGES(HomeCopy.SHORTCUT_PAGES),
    CHECKS(HomeCopy.SHORTCUT_CHECKS),
    SETTINGS(HomeCopy.SHORTCUT_SETTINGS, dim = true),
    ;

    /** Where the shortcut goes; `설정` is the 프로젝트 설정 container (LIB-69). */
    fun route(projectKey: Key): Route = when (this) {
        STORY -> StoryStructure(projectKey)
        CAST -> CastList(projectKey)
        WORLD -> World(projectKey)
        TIMELINE -> Timeline(projectKey)
        CANVAS -> CanvasBoard(projectKey)
        GALLERY -> Gallery.ofProject(projectKey)
        PAGES -> PageList(projectKey)
        CHECKS -> CheckCenter(projectKey)
        SETTINGS -> ProjectSettings(projectKey)
    }
}

/** Everything 프로젝트 홈 draws (STO-01, board `Home`). */
data class HomeUi(
    val projectKey: Key,
    val name: String,
    val oneLiner: String?,
    val rating: Rating,
    val lens: LensChipLabel,
    val stats: List<HomeStat>,
    val resume: HomeResume?,
    /** `Ⅰ 1장 · 등불지기`; null when the structure has no level above the episodes. */
    val listHeader: String?,
    val rows: List<HomeRow>,
    val shortcuts: List<HomeShortcut>,
    val commandHint: String,
    val readOnly: Boolean,
    /** The 만든 직후 block (LIB-13) while it is not closed. */
    val start: HomeStart? = null,
    /**
     * The stats `NewReady` still draws: the `k/n 씬` progress of a 1-level project (QA 0.1.1 backlog: it
     * was hidden while `시작해 볼 것` was open); empty for deeper structures, as the board draws no stats.
     */
    val newReadyStats: List<HomeStat> = emptyList(),
    /** The project cover left of the name (U-08 LIB-83): 3:4 crop, a preset, or the initial-letter card. */
    val cover: CoverUi = CoverUi(null, null, null, "", holo = false),
    /** The cover opens the cover picker and draws the pencil badge; never for the read-only example. */
    val coverEditable: Boolean = false,
) {
    /** `표지 이미지 · 바꾸기` while a cover is drawn, `표지 이미지 · 고르기` on the initial-letter card. */
    val coverLabel: String get() = if (cover.hash != null || cover.presetId != null) HomeCopy.COVER_CHANGE else HomeCopy.COVER_PICK

    /**
     * Board `NewReady` (summary line, no stats but [newReadyStats], no `이어 쓰기`, no lens chip) while
     * the block shows and nothing has been written yet. The head reads the project's name in every state
     * (QA U-06; the board's `새 프로젝트` head is not followed). Once there is an `이어 쓰기` target, home is
     * board `Home` again and the block sits above the chapter list (LIB-13 "on top", STO-01).
     */
    val newReady: Boolean get() = start != null && resume == null
}

/**
 * One item of the home list, in drawing order; [key] is its lazy key. The `start` slot is always there: the
 * 만든 직후 block folds closed inside it (height and fade together), so on `목록 닫기` the rows below follow its
 * shrinking edge instead of moving up over a block that is still fading in place (emulator round 1).
 */
sealed interface HomeSlot {
    val key: String

    data object Name : HomeSlot { override val key: String get() = "name" }
    data object Stats : HomeSlot { override val key: String get() = "stats" }
    data object Resume : HomeSlot { override val key: String get() = "resume" }
    data object Start : HomeSlot { override val key: String get() = "start" }
    data object ListHeader : HomeSlot { override val key: String get() = "listHeader" }
    data class Episode(val row: HomeRow) : HomeSlot { override val key: String get() = "row:${row.key}" }
    data object Work : HomeSlot { override val key: String get() = "work" }
}

/**
 * Builds [HomeUi] from the project (STO-01, STO-06). Labels and counts come from `domain:story`
 * [StoryOverview]; this adds the hierarchy generalisation (`ED`, STO-02):
 * - 3+ levels: the current chapter (top-level unit of the last edit, else the first) and its episodes;
 * - 2 levels (`막 › 장면`): every top-level unit is an episode, no header;
 * - 1 level: the scenes themselves, no header.
 */
object HomeModel {
    /**
     * The items of home for [ui]: name, stats when [shownStats] has any, `이어 쓰기` (board `Home` only), the
     * start slot (always; empty while there is no block), the chapter list, `작업`.
     */
    fun slots(ui: HomeUi): List<HomeSlot> = buildList {
        add(HomeSlot.Name)
        if (shownStats(ui).isNotEmpty()) add(HomeSlot.Stats)
        if (!ui.newReady && ui.resume != null) add(HomeSlot.Resume)
        add(HomeSlot.Start)
        if (ui.rows.isNotEmpty()) {
            add(HomeSlot.ListHeader)
            ui.rows.forEach { add(HomeSlot.Episode(it)) }
        }
        add(HomeSlot.Work)
    }

    /** The stats home draws: all of them on board `Home`; only [HomeUi.newReadyStats] on `NewReady`. */
    fun shownStats(ui: HomeUi): List<HomeStat> = if (ui.newReady) ui.newReadyStats else ui.stats

    /**
     * The episode the right pane shows (`TwoHome`, STO-43): the `StoryStructure` right above this home on
     * the back stack (its focus unit), or null when no structure is beside home.
     */
    fun selectedKey(backStack: List<androidx.navigation3.runtime.NavKey>, projectKey: Key): Key? {
        val top = backStack.lastOrNull() as? StoryStructure ?: return null
        val below = backStack.getOrNull(backStack.lastIndex - 1) as? io.github.lenticualrstone.civilight.navigation.ProjectHome ?: return null
        return top.focusUnitKey.takeIf { below.projectKey == projectKey && top.projectKey == projectKey }
    }

    /** [fileExists]: whether a cover original is on this device; a missing one draws the initial-letter card (LIB-88). */
    fun build(
        doc: ProjectDoc,
        readOnly: Boolean,
        locked: Boolean,
        today: LocalDate,
        zone: ZoneId,
        fileExists: (String) -> Boolean = { true },
    ): HomeUi {
        val view = StoryOverview(doc, locked = locked)
        val levels = doc.project.structure.levels.size
        val resume = view.resumeTarget()?.let { HomeResume(it.where, it.quote, it.blockKey) }
        val (header, rows) = when {
            levels >= 3 -> {
                val chapter = view.currentChapter
                chapter?.let(view.codes::chapterHeader) to chapter?.let(view::episodesOf).orEmpty().map { episodeRow(view, it) }
            }
            levels == 2 -> null to view.chapters.map { episodeRow(view, it) }
            else -> null to view.chapters.map { sceneRow(view, it) }
        }
        val progress = progressStat(view, levels)
        val stats = listOfNotNull(
            progress,
            HomeStat(number(StructureEdit.todayChars(doc, today, zone)), HomeCopy.STAT_TODAY),
            HomeStat(number(view.characterCount), HomeCopy.STAT_CAST),
        )
        return HomeUi(
            projectKey = doc.project.key,
            name = doc.project.name,
            oneLiner = doc.project.oneLiner,
            rating = doc.project.rating.known ?: Rating.ALL,
            lens = LensRules.chipLabel(doc, doc.app.view.lens.point),
            stats = stats,
            resume = resume,
            listHeader = header,
            rows = rows,
            shortcuts = shortcuts(doc),
            commandHint = CommandBar.placeholder(CommandScreen.HOME),
            readOnly = readOnly,
            newReadyStats = if (levels <= 1) listOfNotNull(progress) else emptyList(),
            cover = ProjectCovers.cover(doc, doc.project.name, fileExists),
            coverEditable = !readOnly,
        )
    }

    /** The `작업` shortcuts; `연표` is left out in `시간 없이` (WLD-29 `[PROPOSED]`, the 연표 is off there). */
    fun shortcuts(doc: ProjectDoc): List<HomeShortcut> =
        if (doc.project.worldTime.mode.known == WorldTimeMode.NONE) HomeShortcut.entries - HomeShortcut.TIMELINE else HomeShortcut.entries

    /** The grid lines: three to a line, `설정` alone on the last one (GAL-03 "`설정` keeps its own row"). */
    fun shortcutRows(shortcuts: List<HomeShortcut>): List<List<HomeShortcut>> {
        val (last, rest) = shortcuts.partition { it == HomeShortcut.SETTINGS }
        return rest.chunked(3) + listOfNotNull(last.firstOrNull()?.let { listOf(it) })
    }

    /** `7/10` · `1장 에피소드` (3+ levels); `k/n` · `<episode level name>` (2) or `<scene level name>` (1), `ED`. */
    private fun progressStat(view: StoryOverview, levels: Int): HomeStat? {
        if (levels >= 3) return view.chapterStat()?.let { (value, label) -> HomeStat(value, label) }
        val names = view.doc.project.structure.levels
        val units = view.chapters
        if (units.isEmpty()) return null
        // An episode or scene marked `완료` counts even before it has content (as chapter progress does, STO-06 `ED`).
        val written = units.count(view::countsAsWritten)
        return HomeStat("$written/${units.size}", names.first())
    }

    private fun episodeRow(view: StoryOverview, episode: StoryUnit): HomeRow {
        val row = view.homeRow(episode)
        val current = view.currentSceneKey?.let { view.doc.index.unit(it) }?.let(view.codes::episodeOf)?.key == episode.key
        return HomeRow(row.key, row.code, row.title, row.status, current, row.status.startsWith(SceneStatus.WRITING.word))
    }

    /** A 1-level project lists its scenes: `씬 3 | 꺼진 등불 | 쓰는 중`; a locked hidden scene reads `숨긴 씬`. */
    private fun sceneRow(view: StoryOverview, scene: StoryUnit): HomeRow {
        val row = view.sceneRow(scene)
        val code = view.codes.sceneLabel(scene)
        val title = row.label.removePrefix("$code · ").let { if (it == StoryCopy.UNTITLED && scene.title == null) HomeCopy.NO_TITLE else it }
        return HomeRow(scene.key, code, title, row.status.word, row.current, row.status == SceneStatus.WRITING, hiddenTag = row.hiddenTag)
    }

    /** `1,240`. */
    fun number(n: Int): String = String.format(Locale.ROOT, "%,d", n)
}
