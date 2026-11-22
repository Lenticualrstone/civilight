package io.github.lenticualrstone.civilight.domain.library.format

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Walks JSON trees. Keys and hashes are plain strings, so a reference is any string value equal to them. */
internal object JsonScan {
    /** Every string value (not property names) in [element]. */
    fun strings(element: JsonElement, into: MutableSet<String> = HashSet()): MutableSet<String> {
        when (element) {
            is JsonObject -> element.values.forEach { strings(it, into) }
            is JsonArray -> element.forEach { strings(it, into) }
            is JsonPrimitive -> if (element.isString) into += element.content
            JsonNull -> Unit
        }
        return into
    }

    /** [element] with every string value equal to [from] replaced by [to]. */
    fun replaceString(element: JsonElement, from: String, to: String): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.mapValues { (_, v) -> replaceString(v, from, to) })
        is JsonArray -> JsonArray(element.map { replaceString(it, from, to) })
        is JsonPrimitive -> if (element.isString && element.content == from) JsonPrimitive(to) else element
        JsonNull -> element
    }
}
