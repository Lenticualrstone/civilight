package io.github.lenticualrstone.civilight.domain.world.gen

import io.github.lenticualrstone.civilight.model.map.GenerationStepKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A stored generator result read back key by key; keys that could not be read are listed in [unreadable]. */
data class StoredResult(
    val version: String?,
    val w: Int?,
    val h: Int?,
    val land: String?,
    val coastline: List<List<GenPoint>>?,
    val heights: List<Int>?,
    val relief: String?,
    val ridges: List<List<GenPoint>>?,
    val rivers: List<List<GenPoint>>?,
    val lakes: List<Int>?,
    val climate: String?,
    val towns: List<GenTown>?,
    val roads: List<List<GenPoint>>?,
    val unreadable: Set<String>,
) {
    /** The whole world when every layer is readable, else null (the map still shows its stored base and overlays). */
    fun toWorld(): GeneratedWorld? {
        return GeneratedWorld(
            version = version ?: return null, w = w ?: return null, h = h ?: return null,
            land = land ?: return null, coastline = coastline ?: return null, heights = heights ?: return null,
            relief = relief ?: return null, ridges = ridges ?: return null, rivers = rivers ?: return null,
            lakes = lakes ?: return null, climate = climate ?: return null, towns = towns ?: return null,
            roads = roads ?: return null,
        )
    }
}

/**
 * The JSON of `GeneratedMapRecord.result` (FMT §4: the result is stored, not only the seeds):
 * `{version, grid:{w,h}, land, coastline, heights, relief, ridges, rivers, lakes, climate, towns, roads}`.
 * Polylines are arrays of `[x, y]`. Reading is lenient per key, so an older or foreign shape (such as
 * the minimal result the example project stored before it held real `gen-1` output) never fails a map.
 */
object GenResults {
    /** The result keys each step owns (its layer). */
    val STEP_KEYS: Map<GenerationStepKind, List<String>> = mapOf(
        GenerationStepKind.COAST to listOf("land", "coastline"),
        GenerationStepKind.MOUNTAIN to listOf("heights", "relief", "ridges"),
        GenerationStepKind.RIVER to listOf("rivers", "lakes"),
        GenerationStepKind.FOREST_CLIMATE to listOf("climate"),
        GenerationStepKind.TOWN_ROAD to listOf("towns", "roads"),
    )

    /** Steps whose layer is recomputed when [step] changes (Q-22 default: downstream keep seeds, follow the new upstream). */
    fun affected(step: GenerationStepKind): List<GenerationStepKind> = when (step) {
        GenerationStepKind.COAST -> StepChoices.ORDER
        GenerationStepKind.MOUNTAIN -> listOf(GenerationStepKind.MOUNTAIN, GenerationStepKind.RIVER, GenerationStepKind.FOREST_CLIMATE, GenerationStepKind.TOWN_ROAD)
        GenerationStepKind.RIVER -> listOf(GenerationStepKind.RIVER)
        GenerationStepKind.FOREST_CLIMATE -> listOf(GenerationStepKind.FOREST_CLIMATE, GenerationStepKind.TOWN_ROAD)
        GenerationStepKind.TOWN_ROAD -> listOf(GenerationStepKind.TOWN_ROAD)
    }

    private fun point(p: GenPoint): JsonArray = JsonArray(listOf(JsonPrimitive(p.x), JsonPrimitive(p.y)))
    private fun line(l: List<GenPoint>): JsonArray = JsonArray(l.map(::point))
    private fun lines(ls: List<List<GenPoint>>): JsonArray = JsonArray(ls.map(::line))

    fun encode(world: GeneratedWorld): JsonObject = JsonObject(
        linkedMapOf(
            "version" to JsonPrimitive(world.version),
            "grid" to JsonObject(linkedMapOf("w" to JsonPrimitive(world.w), "h" to JsonPrimitive(world.h))),
            "land" to JsonPrimitive(world.land),
            "coastline" to lines(world.coastline),
            "heights" to JsonArray(world.heights.map(::JsonPrimitive)),
            "relief" to JsonPrimitive(world.relief),
            "ridges" to lines(world.ridges),
            "rivers" to lines(world.rivers),
            "lakes" to JsonArray(world.lakes.map(::JsonPrimitive)),
            "climate" to JsonPrimitive(world.climate),
            "towns" to JsonArray(world.towns.map { t ->
                JsonObject(linkedMapOf("x" to JsonPrimitive(t.x), "y" to JsonPrimitive(t.y), "cell" to JsonPrimitive(t.cell), "port" to JsonPrimitive(t.port)))
            }),
            "roads" to lines(world.roads),
        ),
    )

    /** The keys of [steps] taken from [result]. */
    fun layers(result: JsonObject, steps: List<GenerationStepKind>): JsonObject =
        JsonObject(steps.flatMap { STEP_KEYS.getValue(it) }.mapNotNull { k -> result[k]?.let { k to it } }.toMap(LinkedHashMap()))

    /** [result] with the keys of [layers] replaced (other keys and their order kept). */
    fun withLayers(result: JsonObject, layers: JsonObject): JsonObject {
        val out = LinkedHashMap<String, JsonElement>(result)
        for ((k, v) in layers) out[k] = v
        return JsonObject(out)
    }

    fun decode(result: JsonObject): StoredResult {
        val bad = LinkedHashSet<String>()
        fun <T> read(key: String, f: (JsonElement) -> T): T? {
            val e = result[key] ?: return null.also { bad += key }
            return runCatching { f(e) }.getOrElse { bad += key; null }
        }
        fun pt(e: JsonElement): GenPoint {
            val a = e.jsonArray
            return GenPoint(a[0].jsonPrimitive.doubleOrNull!!, a[1].jsonPrimitive.doubleOrNull!!)
        }
        fun polylines(e: JsonElement): List<List<GenPoint>> {
            val outer = e.jsonArray
            // A flat list of points reads as one polyline.
            if (outer.isNotEmpty() && outer[0].jsonArray.firstOrNull() is JsonPrimitive) return listOf(outer.map(::pt))
            return outer.map { l -> l.jsonArray.map(::pt) }
        }
        val grid = read("grid") { it.jsonObject }
        return StoredResult(
            version = read("version") { it.jsonPrimitive.content },
            w = grid?.get("w")?.jsonPrimitive?.intOrNull,
            h = grid?.get("h")?.jsonPrimitive?.intOrNull,
            land = read("land") { it.jsonPrimitive.content },
            coastline = read("coastline", ::polylines),
            heights = read("heights") { e -> e.jsonArray.map { it.jsonPrimitive.intOrNull!! } },
            relief = read("relief") { it.jsonPrimitive.content },
            ridges = read("ridges", ::polylines),
            rivers = read("rivers", ::polylines),
            lakes = read("lakes") { e -> e.jsonArray.map { it.jsonPrimitive.intOrNull!! } },
            climate = read("climate") { it.jsonPrimitive.content },
            towns = read("towns") { e ->
                e.jsonArray.map { t ->
                    val o = t.jsonObject
                    GenTown(
                        o.getValue("x").jsonPrimitive.doubleOrNull!!, o.getValue("y").jsonPrimitive.doubleOrNull!!,
                        o.getValue("cell").jsonPrimitive.intOrNull!!, o["port"]?.jsonPrimitive?.booleanOrNull ?: false,
                    )
                }
            },
            roads = read("roads", ::polylines),
            unreadable = bad,
        )
    }
}
