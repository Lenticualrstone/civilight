package io.github.lenticualrstone.civilight.navigation

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Text form of any `@Serializable` [NavKey] (routes and sheet keys, including feature-local ones),
 * the same `{type, value}` shape Navigation3 saves the back stack with. Used by saveable sheet stacks
 * and the shell's own saved state. Classes are found by name, so release builds keep their names
 * (see `app/proguard-rules.pro`).
 */
object RouteCodec {
    private val json = Json { ignoreUnknownKeys = true }
    private val single = NavKeySerializer<NavKey>()
    private val list = ListSerializer(single)

    fun encode(key: NavKey): String = json.encodeToString(single, key)

    fun decode(text: String): NavKey = json.decodeFromString(single, text)

    fun encodeAll(keys: List<NavKey>): String = json.encodeToString(list, keys)

    /** Keys that can no longer be read (a class renamed by an update) are dropped. */
    fun decodeAll(text: String): List<NavKey> = runCatching { json.decodeFromString(list, text) }.getOrElse { emptyList() }

    /** [decode], or null when the text no longer names a readable key. */
    fun decodeOrNull(text: String): NavKey? = runCatching { decode(text) }.getOrNull()
}
