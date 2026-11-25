package io.github.lenticualrstone.civilight.feature.script.panel

import io.github.lenticualrstone.civilight.domain.script.Panels
import io.github.lenticualrstone.civilight.domain.script.SceneMemo
import io.github.lenticualrstone.civilight.domain.script.ScriptChecks
import io.github.lenticualrstone.civilight.domain.script.WritingCounts
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.CueKind
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** The `모듈` cards (SCR-35), with the id stored in `_app/view.json` `script.moduleCardOrder`. */
enum class ModCardId(val stored: String) {
    TODAY("today"),
    SCENE("scene"),
    CHECKS("checks"),
    MEMO("memo"),
    ;

    companion object {
        /** The user's order (unknown ids dropped), then cards missing from it in the drawn order. */
        fun order(stored: List<String>): List<ModCardId> {
            val known = stored.mapNotNull { s -> entries.firstOrNull { it.stored == s } }.distinct()
            return known + entries.filter { it !in known }
        }

    }
}

/**
 * (0.4.0) Logical VN/scenario play-time estimator with ±20% error margin:
 * - Dialogue: ~340 CPM + 0.6s click/beat pause
 * - Narration: ~420 CPM + 0.5s beat pause
 * - Direction cues: BG/CG +2.5s, ScreenEffect +1.2s, BGM/SFX +0.8s
 * - Choice: +4.0s decision time + option reading
 * - System: ~380 CPM + 0.8s
 * - Memo: 0s (author-only, excluded)
 */
object PlayTimeEstimator {
    data class Estimate(val centerSeconds: Int, val marginSeconds: Int) {
        val minSeconds: Int get() = (centerSeconds - marginSeconds).coerceAtLeast(0)
        val maxSeconds: Int get() = centerSeconds + marginSeconds

        fun formatSummary(): String {
            if (centerSeconds <= 0) return "0초"
            return if (centerSeconds < 60) {
                "약 ${centerSeconds}초 (±${marginSeconds.coerceAtLeast(1)}초)"
            } else {
                val mins = (centerSeconds / 60f).roundToInt().coerceAtLeast(1)
                val errMins = (marginSeconds / 60f).roundToInt().coerceAtLeast(1)
                val lo = ((centerSeconds - marginSeconds) / 60f).roundToInt().coerceAtLeast(1)
                val hi = ((centerSeconds + marginSeconds) / 60f).roundToInt().coerceAtLeast(lo)
                "약 ${mins}분 (±${errMins}분 · ${lo}~${hi}분)"
            }
        }
    }

    fun estimate(blocks: List<Block>): Estimate {
        var seconds = 0f
        for (b in blocks) {
            when (b.kind.known) {
                BlockKind.DIALOGUE -> {
                    val chars = b.runs.sumOf { it.shownText.length }
                    if (chars > 0) seconds += (chars / 340f) * 60f + 0.6f
                }
                BlockKind.NARRATION -> {
                    val chars = b.runs.sumOf { it.shownText.length }
                    if (chars > 0) seconds += (chars / 420f) * 60f + 0.5f
                }
                BlockKind.DIRECTION -> {
                    if (b.cues.isEmpty()) {
                        if (b.runs.any { it.shownText.isNotBlank() }) seconds += 1.2f
                    } else {
                        for (cue in b.cues) {
                            seconds += when (cue.kind.known) {
                                CueKind.BACKGROUND, CueKind.CG -> 2.5f
                                CueKind.SCREEN_EFFECT -> 1.2f
                                CueKind.BGM, CueKind.SFX -> 0.8f
                                null -> 1.0f
                            }
                        }
                    }
                }
                BlockKind.CHOICE -> {
                    val chars = b.options.sumOf { opt -> opt.runs.sumOf { it.shownText.length } }
                    seconds += 4.0f + (chars / 360f) * 60f
                }
                BlockKind.SYSTEM -> {
                    val chars = b.runs.sumOf { it.shownText.length }
                    if (chars > 0) seconds += (chars / 380f) * 60f + 0.8f
                }
                BlockKind.MEMO, null -> Unit
            }
        }
        val center = seconds.roundToInt()
        val margin = (seconds * 0.20f).roundToInt()
        return Estimate(center, margin)
    }
}

/** One card: `예상 플레이 타임` (replaces removed daily goal), `이 씬 분량 · …`, `점검 · ▲ …`, `씬 메모`. */
sealed interface ModCard {
    val id: ModCardId

    /** (0.4.0) Estimated play time with logical error margin (replaces daily goal). */
    data class PlayTime(val sceneSummary: String, val projectSummary: String) : ModCard {
        override val id: ModCardId get() = ModCardId.TODAY
    }

    data class SceneCount(val text: String) : ModCard {
        override val id: ModCardId get() = ModCardId.SCENE
    }

    /** [text] null = nothing to check in this scene. */
    data class Checks(val text: String?) : ModCard {
        override val id: ModCardId get() = ModCardId.CHECKS
    }

    /** [recorderLine]: `레코더 · 정리 안 됨 n ›` (LRC-24) → the inbox for this scene. */
    data class Memo(val sceneKey: Key, val text: String, val recorderLine: String?, val editable: Boolean) : ModCard {
        override val id: ModCardId get() = ModCardId.MEMO
    }
}

/** The `모듈` tab of the scene under the header, or of the selected block's scene (SCR-35). */
data class ModsTabModel(val sceneKey: Key?, val cards: List<ModCard>) {
    val order: List<ModCardId> get() = cards.map { it.id }
}

object ModsTab {
    fun of(
        doc: ProjectDoc,
        selected: Key?,
        headerScene: Key?,
        today: LocalDate,
        zone: ZoneId,
        readOnly: Boolean,
        unlocked: Boolean,
        recorderLine: (sceneKey: Key) -> String? = { null },
    ): ModsTabModel {
        val block = selected?.let(doc.index::block)?.takeIf { doc.manuscript.isLiveBlock(it.key) }
        val sceneKey = (block?.sceneKey ?: headerScene)?.takeIf { doc.manuscript.isLiveUnit(it) }
            ?.takeIf { doc.index.unit(it)?.hidden != true || unlocked }
        val sceneBlocks = sceneKey?.let(doc.manuscript::liveBlocksOf).orEmpty()
        val sceneEst = PlayTimeEstimator.estimate(sceneBlocks)
        val projectEst = PlayTimeEstimator.estimate(doc.manuscript.liveBlocks)
        val playTimeCard = ModCard.PlayTime(
            sceneSummary = sceneEst.formatSummary(),
            projectSummary = "전체 ${projectEst.formatSummary()}",
        )
        val byId = buildMap<ModCardId, ModCard> {
            put(ModCardId.TODAY, playTimeCard)
            if (sceneKey != null) {
                put(ModCardId.SCENE, ModCard.SceneCount(WritingCounts.sceneSpeakers(doc, sceneKey, unlocked)))
                put(ModCardId.CHECKS, ModCard.Checks(checksLine(doc, sceneKey)))
                put(ModCardId.MEMO, ModCard.Memo(sceneKey, SceneMemo.of(doc, sceneKey).orEmpty(), recorderLine(sceneKey), editable = !readOnly))
            }
        }
        val order = ModCardId.order(doc.app.view.script.moduleCardOrder)
        return ModsTabModel(sceneKey, order.mapNotNull(byId::get))
    }

    /**
     * `▲ 공개 전 사용 1 · 화자 없음 1` (SCR-35 `[PROPOSED]`): this scene's `▲` items by kind; term
     * links marked `맞음` do not count, `◇ 공개 전 후보` never does (rule 21 §8). Null when none.
     */
    fun checksLine(doc: ProjectDoc, sceneKey: Key): String? {
        val blocks = doc.manuscript.liveBlocksOf(sceneKey)
        val keys = blocks.mapTo(HashSet()) { it.key }
        val intended = doc.checks.acknowledgements.filter { it.checkType == CheckType.TERM_BEFORE_REVEAL }.flatMap { it.subjectKeys }.toSet()
        val preReveal = blocks.sumOf { b -> Panels.preRevealLinks(doc, b).count { it !in intended } }
        val noSpeaker = ScriptChecks.speakerMissing(doc).count { item -> item.subjectKeys.any { it in keys } }
        val parts = listOfNotNull(
            ScriptCopy.checkPreReveal(preReveal).takeIf { preReveal > 0 },
            ScriptCopy.checkNoSpeaker(noSpeaker).takeIf { noSpeaker > 0 },
        )
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")?.let { "▲ $it" }
    }
}
