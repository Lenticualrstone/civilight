package io.github.lenticualrstone.civilight.domain.story.structure

import io.github.lenticualrstone.civilight.domain.story.text.BlockText
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import java.util.Locale

/** Derived scene status (STO-06). */
enum class SceneStatus(val word: String) { UNDECIDED("미정"), WRITING("쓰는 중"), DONE("완료") }

/** `Ⅰ 1장 · 등불지기 | 7/10`; a chapter with nothing written: `0/8 · 계획` in `ink3` + [synopsis]. */
data class ChapterRow(val key: Key, val header: String, val progress: String, val planned: Boolean, val synopsis: String?)

/**
 * `1-5 | 첫 번째 신호 | 씬 3개` (expanded: `씬 4개 · 5,210자`); an unwritten episode has meta `—` in
 * `ink3` and its title, or `미정` when untitled (`ED`: STO-03 keeps `미정` for untitled units).
 */
data class EpisodeRow(val key: Key, val code: String, val title: String, val meta: String, val unwritten: Boolean, val expanded: Boolean)

/**
 * `1일째 새벽 | 씬 1 · 새벽 순찰 | 1,820자`; [current] = the scene of the last edit (`쓰는 중`, `sel`
 * fill + `acc` bar); a locked hidden scene reads `씬 4 · 숨긴 씬`, an unlocked one carries [hiddenTag].
 */
data class SceneRow(
    val key: Key,
    val time: String,
    val label: String,
    val meta: String,
    val status: SceneStatus,
    val current: Boolean,
    val hiddenTag: Boolean,
)

/** Home chapter row `1-7 | 잿빛 새벽의 등불 | 쓰는 중 · 씬 3`; untitled episodes show `—`. */
data class HomeEpisodeRow(val key: Key, val code: String, val title: String, val status: String) {
    val line: String get() = "$code | $title | $status"
}

/**
 * The story structure screen and home rows (STO-01, STO-03, STO-04, STO-06): statuses, progress,
 * char counts and labels, all derived. [lastEditedBlockKey] is the app-only `이어 쓰기` target.
 */
class StoryOverview(
    val doc: ProjectDoc,
    val lastEditedBlockKey: Key? = doc.app.activity.activity.lastEditedBlockKey,
    val locked: Boolean = true,
) {
    val codes = UnitCodes(doc)
    private val labels = TimeLabels(SceneTimes(doc), codes)
    private val structure = doc.project.structure
    private val episodeLevel = structure.episodeLevel

    private fun live(parent: Key?): List<StoryUnit> = doc.index.children(parent).filter { doc.liveness.isUnitLive(it.key) }

    /** Top-level units (chapters in the default preset). */
    val chapters: List<StoryUnit> get() = live(null)

    /** Live episodes (episode-level units) under [unit], in order. */
    fun episodesOf(unit: StoryUnit): List<StoryUnit> {
        val level = episodeLevel ?: return emptyList()
        if (unit.level == level) return listOf(unit)
        return live(unit.key).flatMap(::episodesOf)
    }

    /** Live scenes under [unit] in manuscript order. */
    fun scenesOf(unit: StoryUnit): List<StoryUnit> =
        if (unit.level == structure.sceneLevel) listOf(unit) else live(unit.key).flatMap(::scenesOf)

    /** The scene holding the last edited block (if live). */
    val currentSceneKey: Key? by lazy {
        lastEditedBlockKey?.let(doc.index::block)?.takeIf { doc.liveness.isLive(it) }?.sceneKey
    }

    fun hasContent(scene: StoryUnit): Boolean = doc.manuscript.liveBlocksOf(scene.key).any { it.hasContent() }

    fun hasContentUnder(unit: StoryUnit): Boolean = scenesOf(unit).any(::hasContent)

    /** `완료` when the scene or its episode is marked done (`ED`: the flag exists on every unit). */
    fun isMarkedDone(scene: StoryUnit): Boolean = scene.doneMarked || codes.episodeOf(scene)?.doneMarked == true

    /**
     * `완료` when the scene itself is marked, even with no content yet (`ED`, as for episodes:
     * an empty scene marked done read `—` and `미정`); else `미정` with no content; else `완료` when its
     * episode is marked; else `쓰는 중`. An empty scene under a marked episode stays `미정`.
     */
    fun status(scene: StoryUnit): SceneStatus = when {
        scene.doneMarked -> SceneStatus.DONE
        !hasContent(scene) -> SceneStatus.UNDECIDED
        isMarkedDone(scene) -> SceneStatus.DONE
        else -> SceneStatus.WRITING
    }

    fun charCount(unit: StoryUnit): Int = scenesOf(unit).sumOf { s -> doc.manuscript.liveBlocksOf(s.key).sumOf(BlockText::charCount) }

    /**
     * An episode that counts toward chapter progress: it has content, or the author marked it `완료`
     * (`ED`, emulator round 2: an empty episode marked done read `0/1 · 계획` and `미정`). Works for a
     * scene too (its own mark or its content, as [status]): the 1-level home stat counts scenes with it.
     */
    fun countsAsWritten(episode: StoryUnit): Boolean = episode.doneMarked || hasContentUnder(episode)

    /** `k/n` or `0/n · 계획` (k = episodes with content or marked `완료`, [countsAsWritten]). */
    fun progress(chapter: StoryUnit): String {
        val episodes = episodesOf(chapter)
        val k = episodes.count(::countsAsWritten)
        return if (k == 0) "0/${episodes.size} · 계획" else "$k/${episodes.size}"
    }

    fun chapterRow(chapter: StoryUnit): ChapterRow {
        val planned = episodesOf(chapter).none(::countsAsWritten)
        return ChapterRow(chapter.key, codes.chapterHeader(chapter), progress(chapter), planned, if (planned) chapter.synopsis else null)
    }

    fun episodeRow(episode: StoryUnit, expanded: Boolean = false): EpisodeRow {
        val written = hasContentUnder(episode)
        val scenes = scenesOf(episode).size
        val meta = when {
            !written -> "—"
            expanded -> "씬 ${scenes}개 · ${chars(charCount(episode))}"
            else -> "씬 ${scenes}개"
        }
        return EpisodeRow(episode.key, codes.unitLabel(episode), episode.title ?: "미정", meta, !written, expanded)
    }

    fun sceneRow(scene: StoryUnit): SceneRow {
        val status = status(scene)
        val current = scene.key == currentSceneKey
        val masked = scene.hidden && locked
        val title = if (masked) "숨긴 씬" else scene.title ?: "미정"
        val meta = when {
            !hasContent(scene) -> "—"
            current -> "쓰는 중"
            else -> chars(charCount(scene))
        }
        return SceneRow(scene.key, labels.storyColumn(scene.key), "${codes.sceneLabel(scene)} · $title", meta, status, current, scene.hidden && !locked)
    }

    /** Scene rows of an expanded episode. */
    fun sceneRows(episode: StoryUnit): List<SceneRow> = scenesOf(episode).map(::sceneRow)

    /** `단계 이름: 장 › 에피소드 › 씬`. */
    val levelLine: String get() = codes.levelLine()

    // --- Home ------------------------------------------------------------------------------------

    /**
     * Episode status on home: `완료` when the episode is marked done (even with no content yet, `ED`),
     * `미정` (no content), `완료` (every scene with content is marked, `ED`), else `쓰는 중 · 씬 n` with n = the scene of the last edit, or the highest-numbered scene
     * with content when the last edit is elsewhere (`ED`).
     */
    fun episodeStatus(episode: StoryUnit): String {
        val scenes = scenesOf(episode)
        if (episode.doneMarked) return SceneStatus.DONE.word
        val written = scenes.filter(::hasContent)
        if (written.isEmpty()) return SceneStatus.UNDECIDED.word
        if (written.all { it.doneMarked }) return SceneStatus.DONE.word
        val scene = scenes.firstOrNull { it.key == currentSceneKey } ?: written.last()
        return "쓰는 중 · ${codes.sceneLabel(scene)}"
    }

    fun homeRow(episode: StoryUnit): HomeEpisodeRow =
        HomeEpisodeRow(episode.key, codes.unitLabel(episode), episode.title ?: "—", episodeStatus(episode))

    /** The chapter holding the last edit, else the first chapter (`ED`). */
    val currentChapter: StoryUnit? by lazy {
        currentSceneKey?.let(doc.index::unit)?.let(codes::chapterOf) ?: chapters.firstOrNull()
    }

    /** Stat `7/10` · `1장 에피소드`. */
    fun chapterStat(): Pair<String, String>? = currentChapter?.let { c ->
        val episodes = episodesOf(c)
        "${episodes.count(::countsAsWritten)}/${episodes.size}" to "${codes.levelLabel(c)} ${structure.levels.getOrElse(episodeLevel ?: 0) { "" }}"
    }

    /** Stat `12` · `인물`: non-deleted characters, hidden ones included (`ED`). */
    val characterCount: Int get() = doc.cast.characters.count { it.deletedAt == null }

    /**
     * `이어 쓰기`: `1-7 씬 3 · 꺼진 등불` and the first line of the block in quotes. A block in a
     * locked hidden scene falls back to the last visible block with content (`ED`). Null for an
     * empty project.
     */
    fun resume(): Pair<String, String>? {
        val visible = { key: Key -> doc.index.unit(key)?.let { !(it.hidden && locked) } ?: false }
        val block = lastEditedBlockKey?.let(doc.index::block)
            ?.takeIf { doc.liveness.isLive(it) && visible(it.sceneKey) }
            ?: doc.manuscript.liveBlocks.lastOrNull { it.hasContent() && visible(it.sceneKey) }
            ?: return null
        val scene = doc.index.unit(block.sceneKey) ?: return null
        val firstLine = (BlockText.gameText(block) ?: BlockText.display(block.runs)).lineSequence().firstOrNull().orEmpty()
        return codes.sceneRefWithTitle(scene) to "“$firstLine”"
    }

    /** `1-1 ~ 1-7`: first to last episode with content (점검 센터 `마지막 확인`), or null. */
    fun writtenRange(): String? {
        val written = chapters.flatMap(::episodesOf).filter(::hasContentUnder)
        val first = written.firstOrNull() ?: return null
        val last = written.last()
        return if (first.key == last.key) codes.unitLabel(first) else "${codes.unitLabel(first)} ~ ${codes.unitLabel(last)}"
    }

    companion object {
        /** `1,820자`. */
        fun chars(n: Int): String = String.format(Locale.ROOT, "%,d자", n)
    }
}
