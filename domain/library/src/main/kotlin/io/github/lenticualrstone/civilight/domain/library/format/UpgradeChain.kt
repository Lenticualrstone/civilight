package io.github.lenticualrstone.civilight.domain.library.format

import kotlinx.serialization.json.JsonElement

/**
 * One format upgrade step (LIB-50 promise 1, §8.1): a pure function on the parsed JSON files of a
 * package (path → tree, manifest excluded). Steps are never deleted once released.
 */
class UpgradeStep(
    val from: FormatVersion,
    val to: FormatVersion,
    val transform: (Map<String, JsonElement>) -> Map<String, JsonElement>,
) {
    init {
        require(to > from) { "an upgrade step must move forward: $from → $to" }
    }

    override fun toString(): String = "$from→$to"
}

/** Result of [UpgradeChain.upgrade]: the upgraded files and the steps applied in order. */
data class Upgraded(val files: Map<String, JsonElement>, val applied: List<UpgradeStep>)

/**
 * The chain of upgrade steps up to [latest]. A file is upgraded one step at a time from its own
 * version; the input map is never changed. Minor versions only add content, so where no step starts
 * at the current version of the same major, the chain moves on to the next step's start without a
 * transform (`ED`). A file at or above [latest] is returned unchanged (a newer file that passed the
 * open decision is read as is; unknown content is preserved).
 */
class UpgradeChain(val latest: FormatVersion, steps: List<UpgradeStep>) {
    val steps: List<UpgradeStep> = steps.sortedBy { it.from }

    init {
        require(this.steps.all { it.to <= latest }) { "a step goes past the latest version" }
    }

    fun upgrade(version: FormatVersion, files: Map<String, JsonElement>): Upgraded {
        var current = version
        var data = files
        val applied = ArrayList<UpgradeStep>()
        while (current < latest) {
            val step = steps.firstOrNull { it.from == current }
            if (step != null) {
                data = step.transform(data)
                applied += step
                current = step.to
                continue
            }
            val next = steps.firstOrNull { it.from > current }
            if (next != null && next.from.major == current.major) {
                current = next.from
                continue
            }
            if (latest.major == current.major) break
            throw UnsupportedFormatException("no upgrade from $current towards $latest")
        }
        return Upgraded(data, applied)
    }

    companion object {
        /** The released chain of this app. Format 1.0 is the first version, so it has no steps yet. */
        val CURRENT: UpgradeChain = UpgradeChain(FormatVersion.CURRENT, emptyList())
    }
}

/** The file's major version cannot be upgraded by this app. */
class UnsupportedFormatException(message: String) : Exception(message)
