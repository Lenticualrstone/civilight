@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.project

import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.Entity
import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.Ordered
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.open
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Story structure (STO-02): level names top → scene level; a unit's `level` indexes [levels]. */
@Serializable
data class StoryStructure(
    @EncodeDefault val preset: OpenEnum<StructurePreset> = StructurePreset.CHAPTER_EPISODE_SCENE.open(),
    @EncodeDefault val levels: List<String> = DEFAULT_LEVELS,
    override val ext: JsonObject? = null,
) : HasExt {
    /** Index of the scene level (n − 1). */
    val sceneLevel: Int get() = levels.size - 1

    /** Index of the episode level (n − 2), or null for a 1-level structure (§4.1). */
    val episodeLevel: Int? get() = if (levels.size >= 2) levels.size - 2 else null

    companion object {
        val DEFAULT_LEVELS = listOf("장", "에피소드", "씬")
        val ACT_SCENE_LEVELS = listOf("막", "장면")
    }
}

enum class StructurePreset { CHAPTER_EPISODE_SCENE, ACT_SCENE, CUSTOM }

/** World time (WLD-29): one calendar per project, embedded. */
@Serializable
data class WorldTime(
    @EncodeDefault val mode: OpenEnum<WorldTimeMode> = WorldTimeMode.YEAR_ONLY.open(),
    /** `이야기가 시작되는 해`; null when there is no time. */
    val storyStartYear: Int? = null,
    /** Present when [mode] is calendar. */
    val calendar: WorldCalendar? = null,
    override val ext: JsonObject? = null,
) : HasExt

/** `연도만 · 세계 달력 · 시간 없이`. */
enum class WorldTimeMode { YEAR_ONLY, CALENDAR, NONE }

/** World calendar (WLD-30). Months and weekdays are 1-based in [storyStart] / [storyStartWeekday]. */
@Serializable
data class WorldCalendar(
    val months: List<CalendarMonth>,
    @EncodeDefault val weekdays: List<String> = emptyList(),
    val storyStart: MonthDay? = null,
    val storyStartWeekday: Int? = null,
    override val ext: JsonObject? = null,
) : HasExt

@Serializable
data class CalendarMonth(
    val name: String,
    val days: Int,
    override val ext: JsonObject? = null,
) : HasExt

@Serializable
data class MonthDay(
    val month: Int,
    val day: Int,
    override val ext: JsonObject? = null,
) : HasExt

/** Scene-time settings (STO-17/19). */
@Serializable
data class SceneTimeSettings(
    @EncodeDefault val notation: OpenEnum<SceneNotation> = SceneNotation.EPISODE.open(),
    @EncodeDefault val dayBoundary: OpenEnum<DayBoundary> = DayBoundary.DAWN_0400.open(),
    @EncodeDefault val bandStarts: BandStarts = BandStarts(),
    override val ext: JsonObject? = null,
) : HasExt

/** `때 표기`: 에피소드 기준 · 이야기 시작 기준. */
enum class SceneNotation { EPISODE, STORY_START }

/** Where a story day starts for labels (the stored clock is always 04:00-day form). */
enum class DayBoundary { DAWN_0400, MIDNIGHT }

/** 새벽 · 아침 · 낮 · 저녁 · 밤. */
enum class Band { DAWN, MORNING, DAY, EVENING, NIGHT }

/** Start clock of each band; each band ends where the next starts (밤 wraps to 새벽). */
@Serializable
data class BandStarts(
    @EncodeDefault val dawn: ClockTime = ClockTime.of(4, 0),
    @EncodeDefault val morning: ClockTime = ClockTime.of(6, 0),
    @EncodeDefault val day: ClockTime = ClockTime.of(10, 0),
    @EncodeDefault val evening: ClockTime = ClockTime.of(17, 0),
    @EncodeDefault val night: ClockTime = ClockTime.of(20, 0),
    override val ext: JsonObject? = null,
) : HasExt {
    fun startOf(band: Band): ClockTime = when (band) {
        Band.DAWN -> dawn
        Band.MORNING -> morning
        Band.DAY -> day
        Band.EVENING -> evening
        Band.NIGHT -> night
    }
}

/** Project script settings (SCR-24/25/48). */
@Serializable
data class ScriptSettings(
    @EncodeDefault val stagePositionEnabled: Boolean = false,
    @EncodeDefault val voiceIdEnabled: Boolean = true,
    @EncodeDefault val voiceIdFormat: VoiceIdFormat = VoiceIdFormat(),
    @EncodeDefault val transitionDefault: OpenEnum<TransitionMode> = TransitionMode.NONE.open(),
    @EncodeDefault val transitionTemplate: OpenEnum<TransitionTemplate> = TransitionTemplate.PLACE_TIME.open(),
    override val ext: JsonObject? = null,
) : HasExt

@Serializable
data class VoiceIdFormat(
    @EncodeDefault val mode: OpenEnum<VoiceIdFormatMode> = VoiceIdFormatMode.PATTERN.open(),
    @EncodeDefault val pattern: String = DEFAULT_PATTERN,
    override val ext: JsonObject? = null,
) : HasExt {
    companion object {
        const val DEFAULT_PATTERN = "{code}_{chapter2}{episode2}_{number3}"
    }
}

enum class VoiceIdFormatMode { PATTERN, MANUAL }

/** Scene transition text mode (`자동 · 직접 · 없음`). */
enum class TransitionMode { AUTO, DIRECT, NONE }

/** `틀 장소 · 시각` / `장소만`. */
enum class TransitionTemplate { PLACE_TIME, PLACE_ONLY }

/** Project cast settings (CST). */
@Serializable
data class CastSettings(
    @EncodeDefault val bundleName: BundleNames = BundleNames(),
    @EncodeDefault val showFirstAppearanceInHeader: Boolean = true,
    @EncodeDefault val showVoiceProgressInHeader: Boolean = true,
    @EncodeDefault val relationExportAddress: Boolean = false,
    /** (2026-11-26) `묶음 기준` of place and faction cards. Absent = all unused; not written while unused. */
    val affiliationBasis: AffiliationBasis = AffiliationBasis(),
    override val ext: JsonObject? = null,
) : HasExt

/**
 * (2026-11-26) `묶음 기준`: which character project fields (type `연결`) group people on place and
 * faction cards. The user picks each one in 프로젝트 설정; nothing is guessed. Each slot is separate;
 * null = that slot is not used. Keys point at [io.github.lenticualrstone.civilight.model.cast.FieldDef]s
 * of entity kind character.
 */
@Serializable
data class AffiliationBasis(
    /** `소속`: a link field whose target is a faction. */
    val affiliationFieldKey: Key? = null,
    /** `거주`: a link field whose target is a place. */
    val residenceFieldKey: Key? = null,
    /** `국적`: a link field whose target is a place. */
    val nationalityFieldKey: Key? = null,
    override val ext: JsonObject? = null,
) : HasExt

/** Last applied field bundle name per entity kind (shown `게임 프로필`). */
@Serializable
data class BundleNames(
    val character: String? = null,
    val place: String? = null,
    val term: String? = null,
    val faction: String? = null,
    override val ext: JsonObject? = null,
) : HasExt

/** A travel mode (WLD-47). */
@Serializable
data class TravelMode(
    override val key: Key,
    override val order: OrderKey,
    val name: String,
    val kmPerDay: Double,
    val movingHoursPerDay: Double,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Ordered {
    companion object {
        /** The default modes 도보 15/8, 말 40/8, 배 30/24 with fresh keys. */
        fun defaults(newKey: () -> Key = Keys::newKey, at: Long = 0L, by: String = ""): List<TravelMode> {
            val orders = OrderKeys.evenlySpaced(3)
            return listOf(
                TravelMode(newKey(), orders[0], "도보", 15.0, 8.0, at, by),
                TravelMode(newKey(), orders[1], "말", 40.0, 8.0, at, by),
                TravelMode(newKey(), orders[2], "배", 30.0, 24.0, at, by),
            )
        }
    }
}

/** Project additions to the built-in particle list of the term matcher (WLD-21). */
@Serializable
data class TermMatching(
    @EncodeDefault val extraParticles: List<String> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt
