package io.github.lenticualrstone.civilight.store.version

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * One upgrade step of the working store (§8.2): a pure function on the stored JSON files of one
 * store (relative path → JSON), from version [from] to `from + 1`. A step may change, add or remove
 * files. Steps are never deleted once shipped.
 */
fun interface StoreUpgradeStep {
    fun upgrade(files: Map<String, JsonElement>): Map<String, JsonElement>
}

/**
 * The upgrade chain of one store kind, run on the raw JSON before anything is decoded. There is no
 * destructive fallback: a store newer than [current] is refused ([NewerStoreVersionException]).
 */
class StoreUpgradeChain(val current: Int, private val steps: Map<Int, StoreUpgradeStep> = emptyMap()) {
    init {
        require(current >= 1) { "store versions start at 1" }
        for (v in 1 until current) require(v in steps) { "missing upgrade step $v → ${v + 1}" }
    }

    /** True when files at [version] must be upgraded before reading. */
    fun needsUpgrade(version: Int): Boolean {
        if (version > current) throw NewerStoreVersionException(version, current)
        return version < current
    }

    /** [files] at [version] upgraded step by step to [current]. */
    fun upgrade(version: Int, files: Map<String, JsonElement>): Map<String, JsonElement> {
        if (!needsUpgrade(version)) return files
        var out = files
        for (v in maxOf(version, 1) until current) out = steps.getValue(v).upgrade(out)
        return out
    }
}

/** Current versions and chains of the three on-device stores. */
object StoreVersions {
    /** `files/projects/<key>/` (and trashed project directories). */
    val project = StoreUpgradeChain(current = 1)

    /** `files/library.json` and `files/trash/index.json`. */
    val library = StoreUpgradeChain(current = 1)

    /** `files/recorder/`. */
    val recorder = StoreUpgradeChain(current = 1)
}

/** `store.json` of a store directory (`_app/store.json` in a project). */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class StoreVersionFile(@EncodeDefault val storeVersion: Int = 1)

/** A store written by a newer app version; this version does not read it (and never overwrites it). */
class NewerStoreVersionException(val found: Int, val supported: Int) :
    IllegalStateException("store version $found is newer than supported $supported")
