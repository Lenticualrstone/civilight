package io.github.lenticualrstone.civilight.domain.story.travel

import io.github.lenticualrstone.civilight.domain.story.checks.AckSubject
import io.github.lenticualrstone.civilight.domain.story.checks.Fingerprints
import io.github.lenticualrstone.civilight.domain.story.scene.Appearance
import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.domain.story.time.DAY_MINUTES
import io.github.lenticualrstone.civilight.domain.story.time.Durations
import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.Span
import io.github.lenticualrstone.civilight.domain.story.time.TimeChange
import io.github.lenticualrstone.civilight.domain.story.time.TimeChangePreview
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.domain.story.time.shiftedByDays
import io.github.lenticualrstone.civilight.domain.story.units.PlaceNames
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.TravelMode
import kotlin.math.ceil

/** App settings that switch travel rows off (STO-42). */
data class TravelSettings(
    /** `이동 점검`: rows 10–12 (and `축척 없음`) are computed. */
    val checkTravel: Boolean = true,
    /** `장소 없는 씬 알림`: row 13 is shown. */
    val checkNoPlace: Boolean = true,
)

/** The three ▲ names of R18 §5. */
enum class TravelVerdict(val title: String, val checkType: String) {
    SHORTAGE("이동 시간 부족", CheckType.TRAVEL_SHORTAGE),
    SAME_TIME("같은 때 두 장소", CheckType.TRAVEL_SAME_TIME_TWO_PLACES),
    SIMULTANEOUS("동시에 두 장소", CheckType.TRAVEL_SIMULTANEOUS),
}

/** `씬 3 · +2일로`: delays [sceneKey] by [days]. */
data class TimeChip(val label: String, val sceneKey: Key, val days: Int)

/** `말 · 약 7시간 40분`: stores an override for the pair. */
data class ModeChip(val label: String, val modeKey: Key, val requiredMinutes: Int)

/**
 * One `▲` of the travel check (STO-29/30, `TravelCheck` card).
 *
 * @property gap available minutes `A(to) − A(from)`, with `gapMin` clamped to 0 when the range
 *   straddles 0 (manuscript order is stronger).
 * @property rangeDriven the day difference is a range (`대략인데도 모자람 … 가장 길게 잡아도 부족`).
 */
data class TravelWarning(
    val verdict: TravelVerdict,
    val characterKey: Key,
    val characterName: String,
    val fromSceneKey: Key,
    val toSceneKey: Key,
    val distance: Distance,
    val km: Double,
    val mode: TravelMode,
    val requiredMinutes: Int,
    val gap: Span,
    val rangeDriven: Boolean,
    val timeChip: TimeChip,
    val modeChips: List<ModeChip>,
    override val fingerprint: String,
    /** `▲ 이동 시간 부족`. */
    val headline: String,
    /** `1-7 씬 2 상층 관문 → 씬 3 결사의 문`. */
    val routeLine: String,
    /** `도보 2일 4시간` (row `필요`). */
    val needText: String,
    /** `9시간 40분 – 12시간 40분` (row `있음`). */
    val haveText: String,
    /** Width of the `있음` bar on the `필요` scale: `gapMax / required`, capped at 1. */
    val haveFraction: Double,
    /** `경로 38 km · 도보 15 km · 하루 8시간 · 1일째 저녁(17–20) → 2일째 05:40`. */
    val detail: String,
    /** `직선으로도 부족 · 경로 없음` for a straight-line shortage. */
    val straightNote: String?,
    /** Compact row: `같은 때 두 장소 · 세라 | 1-7 씬 2 기록실 · 씬 4 잿더미 시장 · 둘 다 2일째 아침`. */
    val compactLine: String,
) : AckSubject {
    override val checkType: String get() = verdict.checkType
    override val subjectKeys: List<Key> get() = listOf(characterKey, fromSceneKey, toSceneKey)
    val straightLine: Boolean get() = distance is Distance.Straight
}

/** `◇ 장소 없음 · 씬 3개` (one row for all): timed scenes in someone's track without a place. */
data class NoPlaceItem(val sceneKeys: List<Key>, val title: String, val detail: String, val action: String = "한꺼번에 ›") {
    val checkType: String get() = CheckType.TRAVEL_NO_PLACE
}

/** `◇ 축척 없음 · 점검 못 함` for a pair that could not be measured. */
data class NoScaleItem(val characterKey: Key, val fromSceneKey: Key, val toSceneKey: Key, val mapKey: Key?, val detail: String) {
    val title: String get() = "◇ 축척 없음 · 점검 못 함"
    val action: String get() = "축척 맞추기"
    val checkType: String get() = CheckType.TRAVEL_NO_SCALE
}

/** The travel check of one project state (STO-29, R18). */
class TravelCheck(
    val doc: ProjectDoc,
    val settings: TravelSettings = TravelSettings(),
    val times: SceneTimes = SceneTimes(doc),
) {
    private val codes = UnitCodes(doc)
    private val labels = TimeLabels(times, codes)
    private val places = PlaceNames(doc)
    private val distances = Distances(doc)

    /**
     * Each live character's checkable scenes in time order: in 등장, not 회상, timed, with a live
     * place (STO-29 Pairs).
     */
    val tracks: Map<Key, List<Key>> by lazy {
        val byCharacter = LinkedHashMap<Key, MutableList<Key>>()
        for (scene in times.liveScenes) {
            if (times.timed(scene.key) == null) continue
            if (scene.place == null || places.isDeleted(scene.place)) continue
            for (c in Appearance.of(doc, scene.key).all) {
                if (!doc.liveness.isCharacterLive(c)) continue
                byCharacter.getOrPut(c) { mutableListOf() } += scene.key
            }
        }
        byCharacter.mapValues { (_, keys) -> keys.sortedWith(times.timeOrder) }
    }

    private data class PairResult(val warning: TravelWarning?, val noScale: NoScaleItem?)

    private val results: List<PairResult> by lazy {
        if (!settings.checkTravel) return@lazy emptyList()
        tracks.flatMap { (character, keys) -> keys.zipWithNext().map { (x, y) -> evaluate(character, x, y) } }
    }

    /** Every `▲` (rows 10–12), in story order of the later scene. */
    val warnings: List<TravelWarning> by lazy { results.mapNotNull { it.warning } }

    /** `◇ 축척 없음 · 점검 못 함` rows. */
    val noScale: List<NoScaleItem> by lazy { results.mapNotNull { it.noScale } }

    /** `◇ 장소 없음 · 씬 n개` (row 13), or null when none or switched off. */
    val noPlace: NoPlaceItem? by lazy {
        if (!settings.checkNoPlace) return@lazy null
        // `ED`: every timed (non-flashback) scene without a place drops out of 동선 and checks.
        val keys = times.liveScenes.filter { s -> s.place == null && times.timed(s.key) != null }.map { it.key }
        if (keys.isEmpty()) null else NoPlaceItem(
            sceneKeys = keys,
            title = "◇ 장소 없음 · 씬 ${keys.size}개",
            detail = keys.joinToString(" · ") { codes.sceneRef(it).orEmpty() } + " · 동선 · 점검에서 빠짐",
        )
    }

    /** The warning for one pair if it fires (also used by the `장소와 때` sheet). */
    fun warningFor(characterKey: Key, fromSceneKey: Key, toSceneKey: Key): TravelWarning? =
        evaluate(characterKey, fromSceneKey, toSceneKey).warning

    /** Warnings involving a scene (`장소와 때` sheet block, SCR `시간` tab link). */
    fun warningsFor(sceneKey: Key): List<TravelWarning> = warnings.filter { it.fromSceneKey == sceneKey || it.toSceneKey == sceneKey }

    private fun evaluate(characterKey: Key, x: Key, y: Key): PairResult {
        val sx = doc.index.unit(x)!!
        val sy = doc.index.unit(y)!!
        val distance = distances.between(sx.place!!, sy.place!!)
        val km = when (distance) {
            is Distance.Route -> distance.km
            is Distance.Straight -> distance.km
            Distance.SamePlace -> return PairResult(null, null)
            is Distance.NoScale -> return PairResult(
                null,
                NoScaleItem(characterKey, x, y, distance.mapKey, "${codes.sceneRef(sx)} ${places.nameOrNone(sx.place)} → ${codes.sceneRefRelativeTo(sy, sx)} ${places.nameOrNone(sy.place)}"),
            )
        }
        val choice = TravelModes.forPair(doc, characterKey, x, y) ?: return PairResult(null, null)
        val mode = choice.mode
        val required = TravelModes.requiredMinutes(km, mode)
        val raw = times.minutesBetween(x, y) ?: return PairResult(null, null)
        val gap = if (raw.min < 0 && raw.max >= 0) Span(0, raw.max) else raw
        if (required <= gap.max) return PairResult(null, null)

        val tx = times.timed(x)!!
        val ty = times.timed(y)!!
        val dayDiff = times.daysBetween(x, y)!!.range
        val verdict = when {
            tx.clock != null && ty.clock != null && gap == Span.point(0) -> TravelVerdict.SIMULTANEOUS
            dayDiff == Span.point(0) && tx.band != null && tx.band == ty.band -> TravelVerdict.SAME_TIME
            else -> TravelVerdict.SHORTAGE
        }
        val name = doc.index.character(characterKey)?.name.orEmpty()
        val k = ceil((required - gap.min).toDouble() / DAY_MINUTES).toInt().coerceAtLeast(1)
        val laterRef = codes.sceneRefRelativeTo(sy, sx)
        val modeChips = TravelModes.modes(doc).filter { it.key != mode.key }.mapNotNull { m ->
            val need = TravelModes.requiredMinutes(km, m)
            if (need <= gap.min) ModeChip("${m.name} · ${Durations.required(need)}", m.key, need) else null
        }
        val placeX = places.nameOrNone(sx.place)
        val placeY = places.nameOrNone(sy.place)
        val routeLine = "${codes.sceneRef(sx)} $placeX → $laterRef $placeY"
        val distanceText = if (distance is Distance.Route) "경로 ${TravelModes.number(km)} km" else "직선 ${TravelModes.number(km)} km"
        val detail = listOfNotNull(distanceText, TravelModes.detail(mode), "${labels.travelDetail(x)} → ${labels.travelDetail(y)}").joinToString(" · ")
        val rangeDriven = !dayDiff.isPoint
        val compact = when {
            verdict == TravelVerdict.SHORTAGE && rangeDriven -> "대략인데도 모자람 · $name · $routeLine · 가장 길게 잡아도 부족"
            verdict == TravelVerdict.SHORTAGE -> "${verdict.title} · $name | $routeLine"
            else -> {
                val when_ = if (verdict == TravelVerdict.SIMULTANEOUS) labels.head(y) else labels.short(y)
                "${verdict.title} · $name | ${codes.sceneRef(sx)} $placeX · ${codes.sceneRefRelativeTo(sy, sx)} $placeY · 둘 다 $when_"
            }
        }
        return PairResult(
            TravelWarning(
                verdict = verdict,
                characterKey = characterKey,
                characterName = name,
                fromSceneKey = x,
                toSceneKey = y,
                distance = distance,
                km = km,
                mode = mode,
                requiredMinutes = required,
                gap = gap,
                rangeDriven = rangeDriven,
                timeChip = TimeChip("$laterRef · +${k}일로", y, k),
                modeChips = modeChips,
                fingerprint = Fingerprints.of(
                    "travel", characterKey, x, y, tx.day, tx.t, sx.place?.key, ty.day, ty.t, sy.place?.key, km, mode.key,
                ),
                headline = "▲ ${verdict.title}",
                routeLine = routeLine,
                needText = "${mode.name} ${Durations.required(required)}",
                haveText = Durations.gapRange(gap),
                haveFraction = (gap.max.toDouble() / required).coerceIn(0.0, 1.0),
                detail = detail,
                straightNote = if (distance is Distance.Straight && verdict == TravelVerdict.SHORTAGE) "직선으로도 부족 · 경로 없음" else null,
                compactLine = compact,
            ),
            null,
        )
    }

    companion object {
        /**
         * The time chip opens the cascade preview for the later scene with its 때 delayed by the
         * chip's days (interval + k for `기준`, day + k for `직접`). Nothing changes until `바꾸기`.
         */
        fun timeChipPreview(doc: ProjectDoc, warning: TravelWarning, ctx: EditContext? = null): TimeChangePreview {
            val scene = doc.index.unit(warning.timeChip.sceneKey)!!
            return TimeChange.preview(doc, scene.key, scene.time.shiftedByDays(warning.timeChip.days), ctx)
        }

        /** The mode chip: stores the override (character · X · Y · mode) immediately. */
        fun applyModeChip(doc: ProjectDoc, warning: TravelWarning, chip: ModeChip, ctx: EditContext): ProjectDoc =
            TravelModes.setOverride(doc, warning.characterKey, warning.fromSceneKey, warning.toSceneKey, chip.modeKey, ctx)
    }
}
