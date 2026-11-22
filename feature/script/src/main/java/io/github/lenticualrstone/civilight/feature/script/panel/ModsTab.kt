package io.github.lenticualrstone.civilight.feature.script.panel

import io.github.lenticualrstone.civilight.domain.script.Panels
import io.github.lenticualrstone.civilight.domain.script.SceneMemo
import io.github.lenticualrstone.civilight.domain.script.ScriptChecks
import io.github.lenticualrstone.civilight.domain.script.WritingCounts
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import java.time.LocalDate
import java.time.ZoneId

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

/** One card: `오늘 · 1,240자` + bar + `목표 2,000자`, `이 씬 분량 · …`, `점검 · ▲ …`, `씬 메모` (+ recorder line). */
sealed interface ModCard {
    val id: ModCardId

    data class Today(val written: String, val progress: Float, val goal: String) : ModCard {
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
    /** `오늘` goal (Q-16 default; where it is set is open). */
    const val GOAL = 2000

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
        val todayCard = WritingCounts.todayCard(doc, today, zone, GOAL).let { ModCard.Today(it.written, it.progress, it.goal) }
        val byId = buildMap<ModCardId, ModCard> {
            put(ModCardId.TODAY, todayCard)
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
