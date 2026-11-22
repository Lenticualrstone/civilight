package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.DayBaseline
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Character counts (STO-06 `ED`) and the `모듈` tab cards `오늘` and `이 씬 분량` (SCR-35, Q-16
 * default): grapheme clusters of game-output text (dialogue incl. parentheticals, narration,
 * choice options, system text), spaces included; memos and direction cues excluded.
 */
object WritingCounts {
    /** Counted characters of one block. */
    fun charCount(block: Block): Int = when (block.kind.known) {
        BlockKind.DIALOGUE, BlockKind.NARRATION, BlockKind.SYSTEM -> Runs.graphemes(Runs.exportText(block.runs, includeParens = true))
        BlockKind.CHOICE -> block.options.sumOf { Runs.graphemes(Runs.exportText(it.runs, includeParens = true)) }
        else -> 0
    }

    /** Counted characters of a scene's live blocks. */
    fun sceneCount(doc: ProjectDoc, sceneKey: Key): Int = doc.manuscript.liveBlocksOf(sceneKey).sumOf(::charCount)

    /** `5,210자`. */
    fun format(count: Int): String = "${Labels.thousands(count)}자"

    /**
     * Today's written characters: Σ max(0, count now − count at the day start) over live blocks. A
     * block without a baseline today counts from 0 when it was created today, else contributes 0
     * (it was not edited today; [recordBaseline] stores the baseline before the first edit of a day).
     */
    fun today(doc: ProjectDoc, today: LocalDate, zone: ZoneId): Int {
        val baselines = doc.app.activity.dayBaselines.filter { it.localDate == today }.associateBy { it.blockKey }
        return doc.manuscript.liveBlocks.sumOf { b ->
            val base = baselines[b.key]?.lengthAtDayStart
                ?: if (Instant.ofEpochMilli(b.createdAt).atZone(zone).toLocalDate() == today) 0 else return@sumOf 0
            maxOf(0, charCount(b) - base)
        }
    }

    /**
     * Before the first edit of [blockKey] on [today], stores its current count as the day's
     * baseline (older days' baselines are dropped). No-op when one exists.
     */
    fun recordBaseline(doc: ProjectDoc, blockKey: Key, today: LocalDate): ProjectDoc {
        val activity = doc.app.activity
        if (activity.dayBaselines.any { it.localDate == today && it.blockKey == blockKey }) return doc
        val block = doc.index.block(blockKey) ?: return doc
        val kept = activity.dayBaselines.filter { it.localDate == today }
        val updated = activity.copy(dayBaselines = kept + DayBaseline(today, blockKey, charCount(block)))
        return doc.copy(app = doc.app.copy(activity = updated))
    }

    /** `오늘` card: `1,240자`, progress in 0..1 and `목표 2,000자` ([goal] is a setting, Q-16). */
    data class TodayCard(val written: String, val progress: Float, val goal: String)

    fun todayCard(doc: ProjectDoc, today: LocalDate, zone: ZoneId, goal: Int = 2000): TodayCard {
        val n = today(doc, today, zone)
        return TodayCard(format(n), if (goal <= 0) 0f else minOf(1f, n.toFloat() / goal), "목표 ${format(goal)}")
    }

    /**
     * `이 씬 분량` (`[PROPOSED]`): dialogue blocks per speaker (most first, then by name), then
     * narration blocks: `리엔 12 · 하르트 7 · 나레이션 4`.
     */
    fun sceneSpeakers(doc: ProjectDoc, sceneKey: Key): String {
        val blocks = doc.manuscript.liveBlocksOf(sceneKey)
        val perSpeaker = blocks.filter { it.kind.known == BlockKind.DIALOGUE && it.speakerKey != null }
            .groupingBy { it.speakerKey!! }.eachCount()
            .map { (k, n) -> (Labels.speakerName(doc, k) ?: "?") to n }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
        val narration = blocks.count { it.kind.known == BlockKind.NARRATION }
        val parts = perSpeaker.map { "${it.first} ${it.second}" } + if (narration > 0) listOf("나레이션 $narration") else emptyList()
        return parts.joinToString(" · ")
    }

    /**
     * `이 씬 분량` with rule 19 (SCR-58): while hidden items are locked ([unlocked] false), the lines of
     * hidden speakers are counted together as `숨긴 인물 n` (their names never show); unlocked it is
     * [sceneSpeakers].
     */
    fun sceneSpeakers(doc: ProjectDoc, sceneKey: Key, unlocked: Boolean): String {
        if (unlocked) return sceneSpeakers(doc, sceneKey)
        val blocks = doc.manuscript.liveBlocksOf(sceneKey)
        val spoken = blocks.filter { it.kind.known == BlockKind.DIALOGUE && it.speakerKey != null }
        val hidden = spoken.count { doc.index.character(it.speakerKey!!)?.hidden == true }
        val perSpeaker = spoken.filter { doc.index.character(it.speakerKey!!)?.hidden != true }
            .groupingBy { it.speakerKey!! }.eachCount()
            .map { (k, n) -> (Labels.speakerName(doc, k) ?: "?") to n }
            .let { if (hidden > 0) it + (HIDDEN_SPEAKER to hidden) else it }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
        val narration = blocks.count { it.kind.known == BlockKind.NARRATION }
        val parts = perSpeaker.map { "${it.first} ${it.second}" } + if (narration > 0) listOf("나레이션 $narration") else emptyList()
        return parts.joinToString(" · ")
    }

    private const val HIDDEN_SPEAKER = "숨긴 인물"

    /** Live dialogue lines spoken by a character (blocks in trashed scenes are not counted). */
    fun dialogueLines(doc: ProjectDoc, characterKey: Key): Int =
        doc.manuscript.liveBlocks.count { it.kind.known == BlockKind.DIALOGUE && it.speakerKey == characterKey }
}
