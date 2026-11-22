package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.liveChildren
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Derived labels (never stored): codes `1-7`, ordinals `씬 3`, numbers `#043`, Korean particles. */
object Labels {
    /** `#043`; from 1000 on unpadded (`#1043`, SCR-21). */
    fun number(n: Int): String = "#" + if (n < 1000) n.toString().padStart(3, '0') else n.toString()

    /** `#045-1` for option [index] (1-based) of a choice block. */
    fun optionNumber(blockNumber: Int, index: Int): String = "${number(blockNumber)}-$index"

    /** 1-based position of a live unit among its live siblings, or null when not live. */
    fun ordinal(doc: ProjectDoc, unitKey: Key): Int? {
        val unit = doc.index.unit(unitKey) ?: return null
        if (!doc.liveness.isLive(unit)) return null
        return doc.liveChildren(unit.parentKey).indexOfFirst { it.key == unitKey } + 1
    }

    /** Ordinal path of a unit from the top level, e.g. `[1, 7]` for episode 1-7. */
    fun ordinalPath(doc: ProjectDoc, unitKey: Key): List<Int> {
        val unit = doc.index.unit(unitKey) ?: return emptyList()
        return (doc.index.ancestors(unit).reversed() + unit).map { ordinal(doc, it.key) ?: 0 }
    }

    /** The code of a unit above the scene level: `1-7` for an episode, `1` for a chapter. */
    fun unitCode(doc: ProjectDoc, unitKey: Key): String = ordinalPath(doc, unitKey).joinToString("-")

    /** Episode code of a scene's numbering unit (`1-7`); empty for a 1-level project. */
    fun episodeCodeOfScene(doc: ProjectDoc, sceneKey: Key): String {
        val unit = doc.numberingUnitOfScene(sceneKey)
        return if (unit == doc.project.key) "" else unitCode(doc, unit)
    }

    /** `씬 N` ordinal of a scene: its position among the live scenes of its numbering unit. */
    fun sceneOrdinal(doc: ProjectDoc, sceneKey: Key): Int? {
        val unit = doc.numberingUnitOfScene(sceneKey)
        val scenes = doc.manuscript.liveScenes.filter { doc.numberingUnitOfScene(it.key) == unit }
        return scenes.indexOfFirst { it.key == sceneKey }.takeIf { it >= 0 }?.plus(1)
    }

    /** `1-7 씬 3` (or `씬 3` for a 1-level project). */
    fun sceneLocation(doc: ProjectDoc, sceneKey: Key): String {
        val ordinal = sceneOrdinal(doc, sceneKey)
        val code = episodeCodeOfScene(doc, sceneKey)
        val scene = "씬 ${ordinal ?: "?"}"
        return if (code.isEmpty()) scene else "$code $scene"
    }

    /** Label of any unit: `1장` for a chapter of the default structure, `1-7` for an episode, `1-7 씬 3` for a scene. */
    fun unitLabel(doc: ProjectDoc, unitKey: Key): String {
        val unit = doc.index.unit(unitKey) ?: return "?"
        val structure = doc.project.structure
        return when {
            unit.level == structure.sceneLevel -> sceneLocation(doc, unitKey)
            unit.level == 0 && structure.levels.size >= 3 -> "${ordinal(doc, unitKey) ?: "?"}${structure.levels[0]}"
            else -> unitCode(doc, unitKey)
        }
    }

    /** `#043` with the episode code when the block is in another numbering unit than [relativeTo]. */
    fun blockRef(doc: ProjectDoc, block: Block, relativeTo: Key? = null): String {
        val unit = doc.numberingUnitOf(block)
        return if (relativeTo == null || unit == relativeTo) number(block.number)
        else "${Labels.episodeCodeOfScene(doc, block.sceneKey)} ${number(block.number)}"
    }

    /** Select-mode label of a block (SCR-36): speaker name, `연출`, `나레이션`, `선택지`, `시스템`, `작가 메모`. */
    fun blockLabel(doc: ProjectDoc, block: Block): String = when (block.kind.known) {
        BlockKind.DIALOGUE -> block.speakerKey?.let { speakerName(doc, it) } ?: "화자 없음"
        else -> kindLabel(block.kind.known)
    }

    /** Short kind word: `대사 · 나레이션 · 연출 · 선택지 · 시스템 · 작가 메모`. */
    fun kindLabel(kind: BlockKind?): String = when (kind) {
        BlockKind.DIALOGUE -> "대사"
        BlockKind.NARRATION -> "나레이션"
        BlockKind.DIRECTION -> "연출"
        BlockKind.CHOICE -> "선택지"
        BlockKind.SYSTEM -> "시스템"
        BlockKind.MEMO -> "작가 메모"
        null -> "블록"
    }

    /** Character name from the live or trashed row, else its name tag. */
    fun speakerName(doc: ProjectDoc, characterKey: Key): String? =
        doc.index.character(characterKey)?.name ?: doc.index.nameTag(characterKey)?.name

    /** `새벽 · 아침 · 낮 · 저녁 · 밤`. */
    fun bandWord(band: Band): String = when (band) {
        Band.DAWN -> "새벽"
        Band.MORNING -> "아침"
        Band.DAY -> "낮"
        Band.EVENING -> "저녁"
        Band.NIGHT -> "밤"
    }

    /** `11월 20일` in [zone]. */
    fun monthDay(at: Timestamp, zone: ZoneId): String {
        val d = Instant.ofEpochMilli(at).atZone(zone)
        return "${d.monthValue}월 ${d.dayOfMonth}일"
    }

    /** `11월 22일 08:41` in [zone]. */
    fun monthDayTime(at: Timestamp, zone: ZoneId): String {
        val d = Instant.ofEpochMilli(at).atZone(zone)
        return "${d.monthValue}월 ${d.dayOfMonth}일 ${d.hour.toString().padStart(2, '0')}:${d.minute.toString().padStart(2, '0')}"
    }

    /** `1,240` (thousands separator `,`). */
    fun thousands(n: Int): String = NumberFormat.getIntegerInstance(Locale.US).format(n)

    /**
     * The topic particle after a number read in Sino-Korean (`1은`, `2는`, `3은`): `는` after a
     * vowel-final reading (2 이, 4 사, 5 오, 9 구), else `은` (0 영, 1 일, 3 삼, 6 육, 7 칠, 8 팔, 십, 백, 천, 만).
     */
    fun topic(n: Int): String {
        val last = (n % 10 + 10) % 10
        return if (last == 0) "은" else if (last in setOf(2, 4, 5, 9)) "는" else "은"
    }

    /** `와` after a vowel-final number reading (2, 4, 5, 9), else `과` (`씬 3과 같음`). */
    fun and(n: Int): String = if (topic(n) == "는") "와" else "과"

    /** Title of a unit or `미정`. */
    fun titleOrUndecided(unit: StoryUnit?): String = unit?.title?.takeIf { it.isNotBlank() } ?: "미정"
}
