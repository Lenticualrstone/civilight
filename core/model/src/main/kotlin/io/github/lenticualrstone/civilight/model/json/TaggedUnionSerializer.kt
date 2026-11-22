@file:OptIn(InternalSerializationApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.json

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.reflect.KClass

/** An opaque descriptor: [UnknownProps] does not walk into values described by it. */
fun opaqueSerialDescriptor(serialName: String): SerialDescriptor =
    buildSerialDescriptor("civilight.$serialName", PolymorphicKind.SEALED)

/**
 * JSON serializer of a sealed hierarchy discriminated by one string property ([tagKey]), with the
 * tag written first. A missing tag means [defaultTag] (TextRun's omitted `k`). A tag no variant
 * knows becomes the hierarchy's unknown variant, which keeps the whole raw object and is written
 * back verbatim (§8.3). Variant content goes through [UnknownProps], so unknown properties of a
 * known variant are kept in its `ext`.
 */
abstract class TaggedUnionSerializer<T : Any>(
    serialName: String,
    private val tagKey: String,
    private val defaultTag: String? = null,
    /** When true, the [defaultTag] variant is written without a tag. */
    private val omitDefaultTag: Boolean = false,
) : KSerializer<T> {

    class Variant<V : Any>(val tag: String, val type: KClass<V>, val serializer: KSerializer<V>)

    protected abstract val variants: List<Variant<out T>>

    /** Wraps an object with an unknown tag. */
    protected abstract fun unknown(raw: JsonObject): T

    /** The raw object if [value] is the unknown variant, else null. */
    protected abstract fun rawOf(value: T): JsonObject?

    override val descriptor: SerialDescriptor = opaqueSerialDescriptor(serialName)

    override fun deserialize(decoder: Decoder): T {
        val input = decoder as? JsonDecoder ?: throw SerializationException("${descriptor.serialName} is JSON-only")
        val obj = input.decodeJsonElement() as? JsonObject
            ?: throw SerializationException("${descriptor.serialName} must be a JSON object")
        val tag = (obj[tagKey] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull ?: defaultTag
        val variant = variants.firstOrNull { it.tag == tag } ?: return unknown(obj)
        val body = JsonObject(obj - tagKey)
        return input.json.decodeFromJsonElement(variant.serializer, UnknownProps.fold(body, variant.serializer.descriptor))
    }

    override fun serialize(encoder: Encoder, value: T) {
        val output = encoder as? JsonEncoder ?: throw SerializationException("${descriptor.serialName} is JSON-only")
        rawOf(value)?.let { return output.encodeJsonElement(it) }
        val variant = variants.firstOrNull { it.type.isInstance(value) }
            ?: throw SerializationException("no variant of ${descriptor.serialName} for ${value::class}")
        @Suppress("UNCHECKED_CAST")
        val serializer = variant.serializer as KSerializer<T>
        val body = UnknownProps.unfold(output.json.encodeToJsonElement(serializer, value), serializer.descriptor)
        val bodyObject = body as? JsonObject ?: JsonObject(emptyMap())
        val out: JsonElement = if (omitDefaultTag && variant.tag == defaultTag) {
            bodyObject
        } else {
            JsonObject(linkedMapOf<String, JsonElement>(tagKey to JsonPrimitive(variant.tag)) + bodyObject)
        }
        output.encodeJsonElement(out)
    }
}
