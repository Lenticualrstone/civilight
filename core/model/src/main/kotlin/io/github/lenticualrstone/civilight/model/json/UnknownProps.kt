package io.github.lenticualrstone.civilight.model.json

import io.github.lenticualrstone.civilight.model.Nfc
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Unknown-property preservation (§8.3) by walking a JSON tree along a serial descriptor.
 *
 * - [fold] (read): in every JSON object whose class has an `ext: JsonObject?` property, properties
 *   the class does not declare are moved into `ext["_unknown"]`. Classes without an extension slot
 *   drop them (only [io.github.lenticualrstone.civilight.model.asset.Blob], whose `ext` is a file
 *   extension).
 * - [unfold] (write): `ext["_unknown"]` is hoisted back to the object's top level and `ext` is
 *   removed when nothing else is in it.
 *
 * The walk stops at opaque descriptors (primitives, [OpenEnum][io.github.lenticualrstone.civilight.model.OpenEnum],
 * JSON elements and the tagged unions of [TaggedUnionSerializer], which fold their own content).
 */
object UnknownProps {
    const val EXT = "ext"
    const val UNKNOWN = "_unknown"

    private const val JSON_OBJECT_SERIAL_NAME = "kotlinx.serialization.json.JsonObject"

    fun fold(element: JsonElement, descriptor: SerialDescriptor): JsonElement {
        if (element is JsonNull) return element
        return when (descriptor.kind) {
            StructureKind.CLASS -> if (element is JsonObject) foldObject(element, descriptor) else element
            StructureKind.OBJECT -> if (element is JsonObject) JsonObject(emptyMap()) else element
            StructureKind.LIST -> if (element is JsonArray) {
                val item = descriptor.getElementDescriptor(0)
                JsonArray(element.map { fold(it, item) })
            } else element
            StructureKind.MAP -> if (element is JsonObject && !isJsonObject(descriptor)) {
                val value = descriptor.getElementDescriptor(1)
                JsonObject(element.mapValues { (_, v) -> fold(v, value) })
            } else element
            else -> element
        }
    }

    fun unfold(element: JsonElement, descriptor: SerialDescriptor): JsonElement {
        if (element is JsonNull) return element
        return when (descriptor.kind) {
            StructureKind.CLASS -> if (element is JsonObject) unfoldObject(element, descriptor) else element
            StructureKind.LIST -> if (element is JsonArray) {
                val item = descriptor.getElementDescriptor(0)
                JsonArray(element.map { unfold(it, item) })
            } else element
            StructureKind.MAP -> if (element is JsonObject && !isJsonObject(descriptor)) {
                val value = descriptor.getElementDescriptor(1)
                JsonObject(element.mapValues { (_, v) -> unfold(v, value) })
            } else element
            else -> element
        }
    }

    /** True when [descriptor] has the §1.3 extension slot `ext: JsonObject?`. */
    fun hasExtSlot(descriptor: SerialDescriptor): Boolean {
        val index = descriptor.getElementIndex(EXT)
        return index != CompositeDecoder.UNKNOWN_NAME && isJsonObject(descriptor.getElementDescriptor(index))
    }

    private fun isJsonObject(descriptor: SerialDescriptor) =
        descriptor.serialName.removeSuffix("?") == JSON_OBJECT_SERIAL_NAME

    private fun foldObject(obj: JsonObject, descriptor: SerialDescriptor): JsonObject {
        val known = LinkedHashMap<String, JsonElement>()
        val unknown = LinkedHashMap<String, JsonElement>()
        for ((name, value) in obj) {
            val index = descriptor.getElementIndex(name)
            if (index == CompositeDecoder.UNKNOWN_NAME) {
                unknown[name] = value
            } else {
                known[name] = fold(value, descriptor.getElementDescriptor(index))
            }
        }
        if (unknown.isNotEmpty() && hasExtSlot(descriptor)) {
            val ext = known[EXT] as? JsonObject ?: JsonObject(emptyMap())
            val previous = ext[UNKNOWN] as? JsonObject ?: JsonObject(emptyMap())
            known[EXT] = JsonObject(ext + (UNKNOWN to JsonObject(previous + unknown)))
        }
        return JsonObject(known)
    }

    private fun unfoldObject(obj: JsonObject, descriptor: SerialDescriptor): JsonObject {
        val out = LinkedHashMap<String, JsonElement>()
        for ((name, value) in obj) {
            val index = descriptor.getElementIndex(name)
            out[name] = if (index == CompositeDecoder.UNKNOWN_NAME) value
            else unfold(value, descriptor.getElementDescriptor(index))
        }
        if (!hasExtSlot(descriptor)) return JsonObject(out)
        val ext = out[EXT] as? JsonObject ?: return JsonObject(out)
        val unknown = ext[UNKNOWN] as? JsonObject ?: return JsonObject(out)
        val rest = ext - UNKNOWN
        if (rest.isEmpty()) out.remove(EXT) else out[EXT] = JsonObject(rest)
        for ((name, value) in unknown) if (name !in out) out[name] = value
        return JsonObject(out)
    }

    /** Every string (values and object keys) of the tree in NFC (§1.4). */
    fun nfc(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.entries.associate { (k, v) -> Nfc.normalize(k) to nfc(v) })
        is JsonArray -> JsonArray(element.map(::nfc))
        is JsonNull -> element
        is JsonPrimitive -> if (element.isString) {
            val normalized = Nfc.normalize(element.content)
            if (normalized === element.content) element else JsonPrimitive(normalized)
        } else element
    }
}
