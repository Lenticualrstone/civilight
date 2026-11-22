@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.map

import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.open
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Hex format data (WLD): generated terrain per cell plus the author's painted overrides. */
@Serializable
data class HexGrid(
    @EncodeDefault val cellKm: Double = 24.0,
    @EncodeDefault val orientation: OpenEnum<HexOrientation> = HexOrientation.POINTY.open(),
    @EncodeDefault val cols: Int = 32,
    @EncodeDefault val rows: Int = 24,
    @EncodeDefault val coordEvery: Int = 3,
    /** Terrain per cell, row-major. */
    @EncodeDefault val cells: List<OpenEnum<Terrain>> = emptyList(),
    /** Author paints by cell index. */
    @EncodeDefault val overrides: Map<Int, OpenEnum<Terrain>> = emptyMap(),
    override val ext: JsonObject? = null,
) : HasExt

enum class HexOrientation { POINTY, FLAT }

enum class Terrain { DEEP, SEA, PLAIN, FOREST, HILL, MOUNTAIN, PEAK, DESERT, SWAMP, SNOW }

/** Polygon format data: Voronoi-like cells grouped into provinces (`지역` cells). */
@Serializable
data class PolyMap(
    @EncodeDefault val cells: List<PolyCell> = emptyList(),
    /** Province key by cell index. */
    @EncodeDefault val cellProvince: Map<Int, Key> = emptyMap(),
    @EncodeDefault val provinces: List<PolyProvince> = emptyList(),
    override val ext: JsonObject? = null,
) : HasExt

@Serializable
data class PolyCell(
    val site: Point,
    val land: Boolean,
    override val ext: JsonObject? = null,
) : HasExt

@Serializable
data class PolyProvince(
    val key: Key,
    val name: String,
    val color: String,
    val factionKey: Key? = null,
    override val ext: JsonObject? = null,
) : HasExt

/**
 * The record of a generated map (§2.7). The result is stored, not only the seeds (FMT §4). The
 * geometry of [result] and of [history] entries is owned by the map generator (`ED`: kept as JSON
 * objects whose shape is `{ coastline, heights?, hexCells?, provinces?, rivers, towns, roads, stamps }`).
 */
@Serializable
data class GeneratedMapRecord(
    /** e.g. `gen-1`. */
    val generatorVersion: String,
    val kind: OpenEnum<GeneratedMapKind>,
    val landRatio: OpenEnum<LandRatio>,
    @EncodeDefault val steps: List<GenerationStep> = emptyList(),
    @EncodeDefault val result: JsonObject = JsonObject(emptyMap()),
    @EncodeDefault val history: List<GenerationHistoryEntry> = emptyList(),
    val conditions: List<TextCondition>? = null,
    val textModel: TextModelInfo? = null,
    override val ext: JsonObject? = null,
) : HasExt

/** 대륙 · 두 대륙 · 섬 무리 · 한 지역. */
enum class GeneratedMapKind { CONTINENT, TWIN, ISLANDS, REGION }

/** 바다 많음 · 반반 · 땅 많음. */
enum class LandRatio { SEA_MORE, HALF, LAND_MORE }

@Serializable
data class GenerationStep(
    val step: OpenEnum<GenerationStepKind>,
    val seed: Int,
    @EncodeDefault val locked: Boolean = false,
    @EncodeDefault val choices: Map<String, String> = emptyMap(),
    override val ext: JsonObject? = null,
) : HasExt

enum class GenerationStepKind { COAST, MOUNTAIN, RIVER, FOREST_CLIMATE, TOWN_ROAD }

/** A candidate or pre-reroll state of the generator. */
@Serializable
data class GenerationHistoryEntry(
    val createdAt: Timestamp,
    @EncodeDefault val steps: List<GenerationStep> = emptyList(),
    /** Snapshot of the result or a reference to it (generator-owned shape). */
    val resultRef: kotlinx.serialization.json.JsonElement? = null,
    override val ext: JsonObject? = null,
) : HasExt

/** One condition parsed from `글로 설명` (text-to-map). */
@Serializable
data class TextCondition(
    val key: Key,
    val what: String,
    /** Cells 0..8 of the 3 × 3 grid. */
    @EncodeDefault val where: Set<Int> = emptySet(),
    @EncodeDefault val traits: List<String> = emptyList(),
    val placeKey: Key? = null,
    val status: OpenEnum<ConditionStatus>,
    val userPosition: Set<Int>? = null,
    /** The phrase in the input text. */
    val sourceSpan: TextSpan? = null,
    override val ext: JsonObject? = null,
) : HasExt

enum class ConditionStatus { APPLIED, NEEDS_CHECK }

@Serializable
data class TextSpan(
    val start: Int,
    val end: Int,
    override val ext: JsonObject? = null,
) : HasExt

@Serializable
data class TextModelInfo(
    val name: String,
    val version: String,
    val onDevice: Boolean,
    override val ext: JsonObject? = null,
) : HasExt
