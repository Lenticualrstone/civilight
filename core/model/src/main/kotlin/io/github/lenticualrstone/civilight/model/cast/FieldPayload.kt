@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.cast

import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.json.UnknownProps
import io.github.lenticualrstone.civilight.model.json.opaqueSerialDescriptor
import io.github.lenticualrstone.civilight.model.text.DocBlock
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject

/**
 * Typed content of a field value (§2.5), JSON: an object with exactly one key naming the variant
 * (`{texts}`, `{rich}`, `{number}`, `{choice}`, `{date}`, `{anniversary}`, `{link}`). Anything else
 * is kept verbatim as [Unknown].
 */
@Serializable(with = FieldPayloadSerializer::class)
sealed interface FieldPayload {
    /** Short text (one entry, or several when `shortTextMultiple`). */
    data class Texts(val values: List<String>) : FieldPayload

    /** Long text. */
    data class Rich(val doc: List<DocBlock>) : FieldPayload

    /** A decimal typed by the author, kept as a string (`"154"`, `"1.5"`). */
    data class Number(val value: String) : FieldPayload

    @Serializable
    data class Choice(
        @EncodeDefault val optionKeys: List<Key> = emptyList(),
        val freeText: String? = null,
        override val ext: JsonObject? = null,
    ) : FieldPayload, HasExt

    @Serializable
    data class Date(
        val year: Int,
        val month: Int? = null,
        val day: Int? = null,
        override val ext: JsonObject? = null,
    ) : FieldPayload, HasExt

    @Serializable
    data class Anniversary(
        val month: Int,
        val day: Int,
        override val ext: JsonObject? = null,
    ) : FieldPayload, HasExt

    @Serializable
    data class Link(
        @EncodeDefault val targetKeys: List<Key> = emptyList(),
        override val ext: JsonObject? = null,
    ) : FieldPayload, HasExt

    /** A payload shape from a newer file; kept verbatim. */
    data class Unknown(val raw: JsonObject) : FieldPayload
}

object FieldPayloadSerializer : KSerializer<FieldPayload> {
    override val descriptor: SerialDescriptor = opaqueSerialDescriptor("FieldPayload")

    private val strings = ListSerializer(String.serializer())
    private val docs = ListSerializer(DocBlock.serializer())

    override fun deserialize(decoder: Decoder): FieldPayload {
        val input = decoder as? JsonDecoder ?: throw SerializationException("FieldPayload is JSON-only")
        val obj = input.decodeJsonElement() as? JsonObject
            ?: throw SerializationException("FieldPayload must be a JSON object")
        if (obj.size != 1) return FieldPayload.Unknown(obj)
        val (name, content) = obj.entries.single()
        val json = input.json
        fun <T> read(serializer: KSerializer<T>): T =
            json.decodeFromJsonElement(serializer, UnknownProps.fold(content, serializer.descriptor))
        return when (name) {
            "texts" -> FieldPayload.Texts(read(strings))
            "rich" -> FieldPayload.Rich(read(docs))
            "number" -> FieldPayload.Number(read(String.serializer()))
            "choice" -> read(FieldPayload.Choice.serializer())
            "date" -> read(FieldPayload.Date.serializer())
            "anniversary" -> read(FieldPayload.Anniversary.serializer())
            "link" -> read(FieldPayload.Link.serializer())
            else -> FieldPayload.Unknown(obj)
        }
    }

    override fun serialize(encoder: Encoder, value: FieldPayload) {
        val output = encoder as? JsonEncoder ?: throw SerializationException("FieldPayload is JSON-only")
        val json = output.json
        fun <T> write(name: String, serializer: KSerializer<T>, content: T) {
            val element = UnknownProps.unfold(json.encodeToJsonElement(serializer, content), serializer.descriptor)
            output.encodeJsonElement(JsonObject(mapOf(name to element)))
        }
        when (value) {
            is FieldPayload.Texts -> write("texts", strings, value.values)
            is FieldPayload.Rich -> write("rich", docs, value.doc)
            is FieldPayload.Number -> write("number", String.serializer(), value.value)
            is FieldPayload.Choice -> write("choice", FieldPayload.Choice.serializer(), value)
            is FieldPayload.Date -> write("date", FieldPayload.Date.serializer(), value)
            is FieldPayload.Anniversary -> write("anniversary", FieldPayload.Anniversary.serializer(), value)
            is FieldPayload.Link -> write("link", FieldPayload.Link.serializer(), value)
            is FieldPayload.Unknown -> output.encodeJsonElement(value.raw)
        }
    }
}
