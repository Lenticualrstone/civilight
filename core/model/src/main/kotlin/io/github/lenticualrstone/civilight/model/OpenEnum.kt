package io.github.lenticualrstone.civilight.model

import io.github.lenticualrstone.civilight.model.json.OpenEnumSerializer
import kotlinx.serialization.Serializable

/**
 * An enum value as stored in a file (§1.5, §8.3): the raw lowerCamelCase string plus the matching
 * constant, or `known == null` when the value came from a newer file. Unknown values are written
 * back unchanged; code shows such items read-only in the closest safe form.
 *
 * Every enum-valued property of the §7 file model is an `OpenEnum`.
 */
@Serializable(with = OpenEnumSerializer::class)
class OpenEnum<E : Enum<E>> private constructor(
    /** The constant, or null when [raw] is not a value this version knows. */
    val known: E?,
    /** The serial value exactly as stored. */
    val raw: String,
) {
    val isUnknown: Boolean get() = known == null

    /** The constant, or [fallback] for an unknown value. */
    fun knownOr(fallback: E): E = known ?: fallback

    infix fun isOf(value: E): Boolean = known == value

    override fun equals(other: Any?): Boolean = other is OpenEnum<*> && other.raw == raw
    override fun hashCode(): Int = raw.hashCode()
    override fun toString(): String = raw

    companion object {
        fun <E : Enum<E>> of(value: E): OpenEnum<E> = OpenEnum(value, serialNameOf(value.name))

        /** A value this version does not know; [raw] is kept verbatim. */
        fun <E : Enum<E>> unknown(raw: String): OpenEnum<E> = OpenEnum(null, raw)

        internal fun <E : Enum<E>> resolved(known: E?, raw: String): OpenEnum<E> = OpenEnum(known, raw)

        /**
         * The §1.5 serial value of a Kotlin enum constant name: lowerCamelCase of the
         * underscore-separated words (`SCENE_TRANSITION` → `sceneTransition`, `R15` → `r15`,
         * `DAWN_0400` → `dawn0400`).
         */
        fun serialNameOf(constantName: String): String {
            val parts = constantName.split('_').filter { it.isNotEmpty() }
            if (parts.isEmpty()) return constantName
            return buildString {
                append(parts[0].lowercase())
                for (part in parts.drop(1)) {
                    append(part[0].uppercaseChar())
                    append(part.substring(1).lowercase())
                }
            }
        }
    }
}

/** This constant as a stored value. */
fun <E : Enum<E>> E.open(): OpenEnum<E> = OpenEnum.of(this)

/** The §1.5 serial value of this constant. */
val Enum<*>.serialName: String get() = OpenEnum.serialNameOf(name)
