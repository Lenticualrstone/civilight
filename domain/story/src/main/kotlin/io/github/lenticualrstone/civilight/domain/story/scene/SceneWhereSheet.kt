package io.github.lenticualrstone.civilight.domain.story.scene

import io.github.lenticualrstone.civilight.domain.story.checks.Acks
import io.github.lenticualrstone.civilight.domain.story.time.BaseLine
import io.github.lenticualrstone.civilight.domain.story.time.BaseRules
import io.github.lenticualrstone.civilight.domain.story.time.OrderChecks
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.domain.story.transition.TransitionRow
import io.github.lenticualrstone.civilight.domain.story.transition.Transitions
import io.github.lenticualrstone.civilight.domain.story.travel.TravelCheck
import io.github.lenticualrstone.civilight.domain.story.travel.TravelSettings
import io.github.lenticualrstone.civilight.domain.story.travel.TravelWarning
import io.github.lenticualrstone.civilight.domain.story.units.PlaceNames
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.TimePrecision

/** Day chips `같은 날 · +1일 · +2일 · 직접`; [OTHER] = a stored interval no chip shows (Q16). */
enum class DayChip(val word: String?) { SAME_DAY("같은 날"), PLUS_ONE("+1일"), PLUS_TWO("+2일"), DIRECT("직접"), OTHER(null) }

/**
 * The travel block of the sheet: `▲ 이동 시간 | 하르트 · 씬 2 상층 관문에서`,
 * `9시간 40분 – 12시간 40분 · 도보 2일 4시간`, chips `+2일로 · 말 · 약 7시간 40분 · 맞음`.
 */
data class SheetTravelBlock(val warning: TravelWarning, val title: String, val line: String, val chips: List<String>)

/**
 * Everything the `씬 · 장소와 때` sheet shows for one scene (STO-20, `SceneWhere`, `RTFlashback`).
 * Rendering and the editors are feature code; values come from the engines here.
 */
data class SceneWhereSheet(
    val sceneKey: Key,
    /** `씬 3 · 장소와 때`. */
    val title: String,
    /** `결사의 문`; `지운 장소 · 잿더미 시장` when deleted; null = none. */
    val place: String?,
    val placeDeleted: Boolean,
    /** `지역으로 바꾸기` (pin place) or `장소로 바꾸기` (region). */
    val placeToggle: String,
    /** `회상 | 연쇄 · 이동 점검에서 빠짐`. */
    val flashbackRow: String,
    val flashback: Boolean,
    /** `때 · 직전 씬 기준 | 씬 2 · 1-7 · 1일째 저녁` / `때 · 1-7 씬 3 기준`; null for 직접, 회상 or none without base. */
    val baseLine: BaseLine?,
    /** Only `직접` is enabled (no earlier timed scene, BASE-03). */
    val onlyDirect: Boolean,
    val dayChip: DayChip?,
    val band: Band?,
    val clock: ClockTime?,
    val precision: TimePrecision?,
    /** `1-7 · 2일째 · 새벽 | 이야기 약 13일째 · 연표에서`; 회상: `연도 1195 · 대정전 그날 | 이야기 시작 5년 전 · 연표에서`. */
    val resultLine: String?,
    /** `기준을 직전 씬으로` is shown. */
    val rebaseToPrevious: Boolean,
    /** `직전 씬보다 앞 · 약 9시간 전`. */
    val orderLine: String?,
    /** `등장 | 리엔 · 하르트 | (대사) | + 세라`: automatic names and silent names. */
    val speakers: String,
    val silentCast: String,
    val transition: TransitionRow,
    val travel: List<SheetTravelBlock>,
) {
    val castRow: String get() = listOf("등장", speakers, "(대사)", "+ $silentCast".trimEnd()).joinToString(" | ")
}

object SceneWhere {
    fun sheet(doc: ProjectDoc, sceneKey: Key, travel: TravelSettings = TravelSettings()): SceneWhereSheet {
        val scene = requireNotNull(doc.index.unit(sceneKey)) { "unknown scene $sceneKey" }
        val times = SceneTimes(doc)
        val codes = UnitCodes(doc)
        val labels = TimeLabels(times, codes)
        val placeLabel = PlaceNames(doc).label(scene.place)
        val time = scene.time
        val flashback = time is SceneTime.Flashback
        val rel = time as? SceneTime.Relative
        val dayChip = when {
            time is SceneTime.Direct -> DayChip.DIRECT
            rel == null -> null
            rel.dayMin != rel.dayMax -> DayChip.OTHER
            rel.dayMin == 0 -> DayChip.SAME_DAY
            rel.dayMin == 1 -> DayChip.PLUS_ONE
            rel.dayMin == 2 -> DayChip.PLUS_TWO
            else -> DayChip.OTHER
        }
        val timed = times.timed(sceneKey)
        val result = when {
            flashback -> labels.flashbackResult(sceneKey)?.let { (a, b) -> "$a | ${listOf(b, "연표에서").filter { it.isNotEmpty() }.joinToString(" · ")}" }
            else -> labels.resultLine(sceneKey)?.let { (a, b) -> "$a | $b · 연표에서" }
        }
        val (speakers, silent) = Appearance.sheetRow(doc, sceneKey)
        val check = TravelCheck(doc, travel, times)
        val blocks = Acks.open(doc, check.warningsFor(sceneKey)).map { w -> travelBlock(w, sceneKey, codes, doc) }
        return SceneWhereSheet(
            sceneKey = sceneKey,
            title = "${codes.sceneLabel(scene)} · 장소와 때",
            place = placeLabel?.wide,
            placeDeleted = placeLabel?.deleted == true,
            placeToggle = if (scene.place?.kind?.known == PlaceRefKind.REGION) "장소로 바꾸기" else "지역으로 바꾸기",
            flashbackRow = "회상 | 연쇄 · 이동 점검에서 빠짐",
            flashback = flashback,
            baseLine = if (time is SceneTime.Relative || time == SceneTime.None) BaseRules.baseLine(times, sceneKey, codes) else null,
            onlyDirect = BaseRules.onlyDirectEnabled(times, sceneKey),
            dayChip = dayChip,
            band = timed?.band,
            clock = timed?.clock,
            precision = rel?.precision?.known ?: if (time is SceneTime.Direct) TimePrecision.EXACT else null,
            resultLine = result,
            rebaseToPrevious = BaseRules.canRebaseToPrevious(times, sceneKey),
            orderLine = OrderChecks.notice(times, sceneKey)?.line,
            speakers = speakers,
            silentCast = silent,
            transition = Transitions(doc, times).row(sceneKey),
            travel = blocks,
        )
    }

    private fun travelBlock(w: TravelWarning, sceneKey: Key, codes: UnitCodes, doc: ProjectDoc): SheetTravelBlock {
        val otherKey = if (w.toSceneKey == sceneKey) w.fromSceneKey else w.toSceneKey
        val other = doc.index.unit(otherKey)!!
        val me = doc.index.unit(sceneKey)!!
        val where = PlaceNames(doc).nameOrNone(other.place)
        val direction = if (otherKey == w.fromSceneKey) "에서" else "로"
        val timeChip = if (w.timeChip.sceneKey == sceneKey) "+${w.timeChip.days}일로" else w.timeChip.label
        return SheetTravelBlock(
            warning = w,
            title = "▲ 이동 시간 | ${w.characterName} · ${codes.sceneRefRelativeTo(other, me)} $where$direction",
            line = "${w.haveText} · ${w.needText}",
            chips = listOf(timeChip) + w.modeChips.map { it.label } + "맞음",
        )
    }
}
