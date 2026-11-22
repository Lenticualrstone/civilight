package io.github.lenticualrstone.civilight.model.json

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

/**
 * The one JSON configuration of every `.civilight` file unit and working-store file (§1.5, §7, §8.2).
 *
 * Always read and write through [decode] / [encode] (or the element variants): they apply NFC to all
 * strings (§1.4) and move unknown properties into `ext["_unknown"]` and back (§8.3). The raw [json]
 * instance is strict (`ignoreUnknownKeys = false`) and would reject a file with unknown properties.
 */
object CivilightJson {
    val json: Json = Json {
        encodeDefaults = false
        explicitNulls = false
        ignoreUnknownKeys = false
        prettyPrint = false
    }

    private val pretty: Json = Json(json) { prettyPrint = true }

    fun <T> decode(deserializer: DeserializationStrategy<T>, text: String): T =
        decodeElement(deserializer, json.parseToJsonElement(text))

    fun <T> decodeElement(deserializer: DeserializationStrategy<T>, element: JsonElement): T =
        json.decodeFromJsonElement(deserializer, UnknownProps.fold(UnknownProps.nfc(element), deserializer.descriptor))

    fun <T> encode(serializer: SerializationStrategy<T>, value: T, prettyPrint: Boolean = false): String {
        val element = encodeElement(serializer, value)
        return (if (prettyPrint) pretty else json).encodeToString(JsonElement.serializer(), element)
    }

    fun <T> encodeElement(serializer: SerializationStrategy<T>, value: T): JsonElement =
        UnknownProps.nfc(UnknownProps.unfold(json.encodeToJsonElement(serializer, value), serializer.descriptor))

    inline fun <reified T> decode(text: String): T = decode(serializer<T>(), text)

    inline fun <reified T> encode(value: T, prettyPrint: Boolean = false): String = encode(serializer<T>(), value, prettyPrint)

    inline fun <reified T> decodeElement(element: JsonElement): T = decodeElement(serializer<T>(), element)

    inline fun <reified T> encodeElement(value: T): JsonElement = encodeElement(serializer<T>(), value)
}
