package io.github.lenticualrstone.civilight.domain.cast.map

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Saved node positions of the full relationship map (CST Q14 default: positions saved per project,
 * like canvas nodes; nodes without one are auto-placed by [MapLayout]). The model has no field for
 * them, so they live in `project.cast.ext["relationMap"]` (round-trips through `.civilight` as
 * `ext`): `{ "<character key>": { "x": 0.5, "y": 0.3 } }` in 0–1 units of the layout area.
 */
object MapPositions {
    const val EXT_KEY = "relationMap"

    /** Saved positions by character key. */
    fun saved(doc: ProjectDoc): Map<Key, Point> {
        val stored = doc.project.cast.ext?.get(EXT_KEY) as? JsonObject ?: return emptyMap()
        return stored.mapNotNull { (key, value) ->
            val o = runCatching { value.jsonObject }.getOrNull() ?: return@mapNotNull null
            val x = o["x"]?.jsonPrimitive?.floatOrNull ?: return@mapNotNull null
            val y = o["y"]?.jsonPrimitive?.floatOrNull ?: return@mapNotNull null
            key to Point(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
        }.toMap()
    }

    /** Saves [point] (0–1) for [characterKey]; other keys of the cast settings' `ext` stay. */
    fun place(doc: ProjectDoc, characterKey: Key, point: Point, ctx: EditContext): ProjectDoc {
        val positions = saved(doc) + (characterKey to Point(point.x.coerceIn(0f, 1f), point.y.coerceIn(0f, 1f)))
        val map = buildJsonObject {
            for ((k, p) in positions.toSortedMap()) {
                put(k, buildJsonObject {
                    put("x", p.x)
                    put("y", p.y)
                })
            }
        }
        val cast = doc.project.cast
        val ext = JsonObject((cast.ext ?: JsonObject(emptyMap())) + (EXT_KEY to map))
        return doc.copy(project = doc.project.copy(cast = cast.copy(ext = ext), updatedAt = ctx.now, updatedBy = ctx.device))
    }
}
