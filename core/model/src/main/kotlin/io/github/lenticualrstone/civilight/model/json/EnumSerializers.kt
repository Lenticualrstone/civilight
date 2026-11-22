@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.json

import io.github.lenticualrstone.civilight.model.OpenEnum
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Serializer of [OpenEnum]: the lowerCamelCase string of §1.5; a string no constant maps to is
 * kept as an unknown value. The plugin passes the enum's own serializer, whose descriptor lists
 * the constants in declaration order.
 */
class OpenEnumSerializer<E : Enum<E>>(private val enumSerializer: KSerializer<E>) : KSerializer<OpenEnum<E>> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("civilight.OpenEnum<${enumSerializer.descriptor.serialName}>", PrimitiveKind.STRING)

    private val byRaw: Map<String, E> by lazy {
        val d = enumSerializer.descriptor
        (0 until d.elementsCount).associate { index ->
            OpenEnum.serialNameOf(d.getElementName(index)) to enumSerializer.deserialize(IndexDecoder(index))
        }
    }

    override fun serialize(encoder: Encoder, value: OpenEnum<E>) = encoder.encodeString(value.raw)

    override fun deserialize(decoder: Decoder): OpenEnum<E> {
        val raw = decoder.decodeString()
        return OpenEnum.resolved(byRaw[raw], raw)
    }

    /** Feeds one enum index into a generated enum serializer to obtain the constant. */
    private class IndexDecoder(private val index: Int) : AbstractDecoder() {
        override val serializersModule: SerializersModule = EmptySerializersModule()
        override fun decodeElementIndex(descriptor: SerialDescriptor): Int = CompositeDecoder.DECODE_DONE
        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = index
    }
}

/**
 * Closed-enum serializer writing the §1.5 lowerCamelCase value. Used for app-only state (class A,
 * S, RT), where an unknown value falls back to [fallback] instead of being preserved.
 */
abstract class LowerCamelEnumSerializer<E : Enum<E>>(
    serialName: String,
    private val values: List<E>,
    private val fallback: E,
) : KSerializer<E> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("civilight.$serialName", PrimitiveKind.STRING)

    private val byRaw = values.associateBy { OpenEnum.serialNameOf(it.name) }

    override fun serialize(encoder: Encoder, value: E) = encoder.encodeString(OpenEnum.serialNameOf(value.name))

    override fun deserialize(decoder: Decoder): E = byRaw[decoder.decodeString()] ?: fallback
}
