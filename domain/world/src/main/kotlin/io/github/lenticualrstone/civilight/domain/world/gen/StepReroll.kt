package io.github.lenticualrstone.civilight.domain.world.gen

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.map.AttachedLayer
import io.github.lenticualrstone.civilight.model.map.DrawnLine
import io.github.lenticualrstone.civilight.model.map.DrawnLineKind
import io.github.lenticualrstone.civilight.model.map.GeneratedMapKind
import io.github.lenticualrstone.civilight.model.map.GenerationHistoryEntry
import io.github.lenticualrstone.civilight.model.map.GenerationStepKind
import io.github.lenticualrstone.civilight.model.map.HexGrid
import io.github.lenticualrstone.civilight.model.map.HexOrientation
import io.github.lenticualrstone.civilight.model.map.LandRatio
import io.github.lenticualrstone.civilight.model.map.LineOrigin
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.Stamp
import io.github.lenticualrstone.civilight.model.map.StampKind
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.open
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.sqrt

enum class RerollMode { KEEP_EDITED, ALL_NEW }

/** The sheet `강 다시 만들기` (WLD-56), shown only when the author edited that layer. */
data class RerollSheet(
    val step: GenerationStepKind,
    val title: String,
    /** `직접 고친 강 2 · 강에 붙은 도장 1(다리)`. */
    val line: String,
    /** `고친 강은 남기기 · 나머지만 새로` (default) and `모두 새로 · 다리 도장 → 위치 확인 목록`. */
    val options: List<Pair<RerollMode, String>>,
    val defaultMode: RerollMode,
    val note: String,
    val buttons: List<String>,
)

/** A `위치 확인` row after `모두 새로`: a stamp attached to the old layer and the nearest point on the new one. */
data class StampCheck(val stampKey: Key, val label: String, val was: Point, val nearest: Point?)

sealed interface RerollResult {
    data class Applied(val doc: ProjectDoc, val checks: List<StampCheck>) : RerollResult
    data class Refused(val reason: RerollRefusal) : RerollResult
}

enum class RerollRefusal {
    NO_MAP,
    NOT_GENERATED,
    /** The stored map came from another generator version: re-running would silently change it. */
    OTHER_VERSION,
}

/**
 * Re-generating one step after `이 지도로 시작` (WLD-56). The step gets a new seed; it and the steps
 * below it are recomputed (Q-22 default: lower steps keep their seeds and choices). Author-edited
 * rivers or roads are kept by default; the previous layer goes into the history and can be
 * restored. Pins, regions, routes and labels are never touched.
 */
object StepReroll {
    private fun lineKind(step: GenerationStepKind): DrawnLineKind? = when (step) {
        GenerationStepKind.RIVER -> DrawnLineKind.RIVER
        GenerationStepKind.TOWN_ROAD -> DrawnLineKind.ROAD
        else -> null
    }

    private fun attached(kind: DrawnLineKind): AttachedLayer = if (kind == DrawnLineKind.RIVER) AttachedLayer.RIVER else AttachedLayer.ROAD

    private fun layerWord(kind: DrawnLineKind): String = if (kind == DrawnLineKind.RIVER) "강" else "길"

    private fun isEdited(line: DrawnLine): Boolean = line.editedByAuthor || line.origin.known == LineOrigin.AUTHOR

    fun stampName(kind: StampKind?): String = when (kind) {
        StampKind.MOUNTAIN -> "산"
        StampKind.HILL -> "언덕"
        StampKind.FOREST -> "숲"
        StampKind.CITY -> "도시"
        StampKind.TOWN -> "마을"
        StampKind.CASTLE -> "성"
        StampKind.RUIN -> "폐허"
        StampKind.TOWER -> "탑"
        StampKind.BRIDGE -> "다리"
        StampKind.PORT -> "항구"
        StampKind.CAVE -> "동굴"
        StampKind.MARKER -> "표지석"
        null -> "도장"
    }

    private fun stampsOn(map: WorldMap, kind: DrawnLineKind): List<Stamp> = map.stamps.filter { it.attachedLayer.known == attached(kind) }

    /** The sheet for re-rolling [step] of [map], or null when nothing there was edited by hand (re-roll at once). */
    fun sheet(map: WorldMap, step: GenerationStepKind): RerollSheet? {
        val name = StepChoices.stepName(step)
        val kind = lineKind(step)
        val parts = ArrayList<String>()
        var stampNames = emptyList<String>()
        if (kind != null) {
            val word = layerWord(kind)
            val edited = map.drawnLines.count { it.kind.known == kind && isEdited(it) }
            val stamps = stampsOn(map, kind)
            stampNames = stamps.map { stampName(it.kind.known) }.distinct()
            if (edited > 0) parts += "직접 고친 $word $edited"
            if (stamps.isNotEmpty()) parts += "${word}에 붙은 도장 ${stamps.size}(${stampNames.joinToString(Copy.SEP)})"
        } else if ((map.hex?.overrides?.size ?: 0) > 0) {
            parts += "직접 칠한 칸 ${map.hex!!.overrides.size}"
        }
        if (parts.isEmpty()) return null
        val word = kind?.let(::layerWord) ?: "칸"
        val allNew = if (stampNames.isEmpty()) "모두 새로" else "모두 새로${Copy.SEP}${stampNames.joinToString(Copy.SEP)} 도장 → 위치 확인 목록"
        return RerollSheet(
            step = step,
            title = "$name 다시 만들기",
            line = parts.joinToString(Copy.SEP),
            options = listOf(RerollMode.KEEP_EDITED to "고친 ${word}은 남기기${Copy.SEP}나머지만 새로", RerollMode.ALL_NEW to allNew),
            defaultMode = RerollMode.KEEP_EDITED,
            note = "이전 $name${Copy.SEP}기록에 보관${Copy.SEP}되돌리기 가능",
            buttons = listOf("취소", "다시 만들기"),
        )
    }

    fun apply(doc: ProjectDoc, mapKey: Key, step: GenerationStepKind, newSeed: Int, mode: RerollMode, ctx: EditContext): RerollResult {
        val map = doc.maps[mapKey] ?: return RerollResult.Refused(RerollRefusal.NO_MAP)
        val gen = map.generation ?: return RerollResult.Refused(RerollRefusal.NOT_GENERATED)
        if (gen.generatorVersion != TerrainGenerator.VERSION) return RerollResult.Refused(RerollRefusal.OTHER_VERSION)
        val kind = gen.kind.known ?: GeneratedMapKind.CONTINENT
        val ratio = gen.landRatio.known ?: LandRatio.HALF
        val steps = StepChoices.ORDER.map { k ->
            val s = GenInput(kind, ratio, gen.steps).step(k)
            if (k == step) s.copy(seed = newSeed) else s
        }
        val world = TerrainGenerator.generate(GenInput(kind, ratio, steps, gen.conditions.orEmpty()))
        val affected = GenResults.affected(step)
        val lineKinds = affected.mapNotNull(::lineKind).toSet()

        // The previous layers, kept in the history for undo.
        val snapshot = JsonObject(
            linkedMapOf(
                "step" to JsonPrimitive(step.name),
                "layers" to GenResults.layers(gen.result, affected),
                "drawnLines" to JsonArray(map.drawnLines.filter { it.kind.known in lineKinds }.map { CivilightJson.encodeElement(DrawnLine.serializer(), it) }),
            ) + (map.hex?.let { mapOf("hex" to CivilightJson.encodeElement(HexGrid.serializer(), it)) } ?: emptyMap()),
        )
        val entry = GenerationHistoryEntry(createdAt = ctx.now(), steps = gen.steps, resultRef = snapshot)
        val result = GenResults.withLayers(gen.result, GenResults.layers(GenResults.encode(world), affected))

        var next = map.copy(generation = gen.copy(steps = steps, result = result, history = gen.history + entry))
        val checks = ArrayList<StampCheck>()
        if (map.format?.known == MapFormat.DETAIL && lineKinds.isNotEmpty()) {
            val keep = map.drawnLines.filter { it.kind.known !in lineKinds || (mode == RerollMode.KEEP_EDITED && isEdited(it)) }
            val fresh = GenFormats.drawnLines(world, ctx, lineKinds)
            next = next.copy(drawnLines = keep + fresh)
            if (mode == RerollMode.ALL_NEW) {
                for (k in lineKinds) for (s in stampsOn(map, k)) {
                    val was = Point(s.x, s.y)
                    checks += StampCheck(s.key, stampName(s.kind.known), was, nearest(was, fresh.filter { it.kind.known == k }))
                }
            }
        }
        if (map.hex != null && affected.any { it != GenerationStepKind.RIVER && it != GenerationStepKind.TOWN_ROAD }) {
            val cells = GenFormats.hex(world, map.hex!!.cols, map.hex!!.rows, map.hex!!.orientation.known ?: HexOrientation.POINTY, map.hex!!.cellKm).cells
            next = next.copy(hex = map.hex!!.copy(cells = cells, overrides = if (mode == RerollMode.KEEP_EDITED) map.hex!!.overrides else emptyMap()))
        }
        val updated = MapEdits.updateMap(doc, mapKey, ctx) { next } ?: return RerollResult.Refused(RerollRefusal.NO_MAP)
        return RerollResult.Applied(updated, checks)
    }

    private fun nearest(p: Point, lines: List<DrawnLine>): Point? =
        lines.flatMap { it.points }.minByOrNull { val dx = (it.x - p.x).toDouble(); val dy = (it.y - p.y).toDouble(); sqrt(dx * dx + dy * dy) }

    /** Undo of the last re-roll: restores the previous layer, lines, cells and seeds from the history. */
    fun undo(doc: ProjectDoc, mapKey: Key, ctx: EditContext): ProjectDoc? {
        val map = doc.maps[mapKey] ?: return null
        val gen = map.generation ?: return null
        val index = gen.history.indexOfLast { (it.resultRef as? JsonObject)?.containsKey("step") == true }
        if (index < 0) return null
        val entry = gen.history[index]
        val snap = entry.resultRef!!.jsonObject
        val step = runCatching { GenerationStepKind.valueOf(snap.getValue("step").jsonPrimitive.content) }.getOrNull() ?: return null
        val lineKinds = GenResults.affected(step).mapNotNull(::lineKind).toSet()
        val lines = snap["drawnLines"]?.jsonArray.orEmpty().map { CivilightJson.decodeElement(DrawnLine.serializer(), it) }
        var next = map.copy(
            generation = gen.copy(
                steps = entry.steps,
                result = GenResults.withLayers(gen.result, snap["layers"]?.jsonObject ?: JsonObject(emptyMap())),
                history = gen.history.filterIndexed { i, _ -> i != index },
            ),
        )
        if (lineKinds.isNotEmpty()) next = next.copy(drawnLines = map.drawnLines.filter { it.kind.known !in lineKinds } + lines)
        snap["hex"]?.let { next = next.copy(hex = CivilightJson.decodeElement(HexGrid.serializer(), it)) }
        return MapEdits.updateMap(doc, mapKey, ctx) { next }
    }
}
