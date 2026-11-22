package io.github.lenticualrstone.civilight.domain.world.gen

import io.github.lenticualrstone.civilight.model.map.GeneratedMapKind
import io.github.lenticualrstone.civilight.model.map.GenerationStepKind
import io.github.lenticualrstone.civilight.model.map.LandRatio
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.MapStyle

/** A question of a step with its 2–3 chips (no sliders, WLD-55). */
data class ChoiceQuestion(val key: String, val label: String, val options: List<String>, val default: String)

/**
 * Step choices and the words of the generator screens (WLD-54, WLD-55, WLD-57). Choice values are
 * stored as the chip words in `GenerationStep.choices`. Chips for `해안선`, `숲 · 기후`, `마을 · 길`
 * are not drawn by any board (Q-20), so those steps have none.
 */
object StepChoices {
    const val RANGES = "ranges"
    const val HEIGHT = "height"
    const val COUNT = "count"
    const val BENDS = "bends"
    const val LAKES = "lakes"
    const val ON = "on"
    const val OFF = "off"

    const val RANGES_DEFAULT = "2"
    const val HEIGHT_DEFAULT = "보통"
    const val COUNT_DEFAULT = "보통"
    const val BENDS_DEFAULT = "보통"
    const val LAKES_DEFAULT = ON

    /** The chip questions of a step. */
    fun questions(step: GenerationStepKind): List<ChoiceQuestion> = when (step) {
        GenerationStepKind.MOUNTAIN -> listOf(
            ChoiceQuestion(RANGES, "산맥 수", listOf("1", "2", "3", "흩어짐"), RANGES_DEFAULT),
            ChoiceQuestion(HEIGHT, "높이", listOf("낮게", "보통", "높게"), HEIGHT_DEFAULT),
        )
        GenerationStepKind.RIVER -> listOf(
            ChoiceQuestion(COUNT, "개수", listOf("적게", "보통", "많이"), COUNT_DEFAULT),
            ChoiceQuestion(BENDS, "굽이", listOf("곧게", "보통", "많이"), BENDS_DEFAULT),
            ChoiceQuestion(LAKES, "호수 · 막힌 곳에 호수", listOf(ON, OFF), LAKES_DEFAULT),
        )
        else -> emptyList()
    }

    /** `해안선 · 산 · 강 · 숲 · 기후 · 마을 · 길`. */
    fun stepName(step: GenerationStepKind): String = when (step) {
        GenerationStepKind.COAST -> "해안선"
        GenerationStepKind.MOUNTAIN -> "산"
        GenerationStepKind.RIVER -> "강"
        GenerationStepKind.FOREST_CLIMATE -> "숲 · 기후"
        GenerationStepKind.TOWN_ROAD -> "마을 · 길"
    }

    fun kindName(kind: GeneratedMapKind): String = when (kind) {
        GeneratedMapKind.CONTINENT -> "대륙"
        GeneratedMapKind.TWIN -> "두 대륙"
        GeneratedMapKind.ISLANDS -> "섬 무리"
        GeneratedMapKind.REGION -> "한 지역"
    }

    fun landRatioName(ratio: LandRatio): String = when (ratio) {
        LandRatio.SEA_MORE -> "바다 많음"
        LandRatio.HALF -> "반반"
        LandRatio.LAND_MORE -> "땅 많음"
    }

    fun formatName(format: MapFormat): String = when (format) {
        MapFormat.HEX -> "육각"
        MapFormat.POLY -> "다각형"
        MapFormat.DETAIL -> "상세 지형"
        MapFormat.OUTLINE -> "윤곽"
        MapFormat.NODE -> "노드"
    }

    fun styleName(style: MapStyle): String = when (style) {
        MapStyle.SOFT -> "색면"
        MapStyle.INK -> "먹선"
        MapStyle.COLOR -> "색"
        MapStyle.PARCHMENT -> "양피지"
    }

    /** `4827`, `0031`. */
    fun seedLabel(seed: Int): String = Math.floorMod(seed, 10_000).toString().padStart(4, '0')

    /** The order of the five steps. */
    val ORDER: List<GenerationStepKind> = listOf(
        GenerationStepKind.COAST, GenerationStepKind.MOUNTAIN, GenerationStepKind.RIVER,
        GenerationStepKind.FOREST_CLIMATE, GenerationStepKind.TOWN_ROAD,
    )
}
