package io.github.lenticualrstone.civilight.domain.library.format

import io.github.lenticualrstone.civilight.model.file.FileManifest

/**
 * A `.civilight` format version `major.minor` (LIB-50 promise 2). Compared numerically, so
 * `1.10 > 1.9`.
 */
data class FormatVersion(val major: Int, val minor: Int) : Comparable<FormatVersion> {
    init {
        require(major >= 0 && minor >= 0) { "negative version" }
    }

    override fun compareTo(other: FormatVersion): Int =
        compareValuesBy(this, other, FormatVersion::major, FormatVersion::minor)

    override fun toString(): String = "$major.$minor"

    companion object {
        private val PATTERN = Regex("""^(\d{1,6})\.(\d{1,6})$""")

        /** The version this app writes and the highest it reads without refusing (LIB-50). */
        val CURRENT: FormatVersion = parse(FileManifest.FORMAT_VERSION)

        fun parse(text: String): FormatVersion =
            requireNotNull(parseOrNull(text)) { "not a major.minor version: $text" }

        fun parseOrNull(text: String): FormatVersion? {
            val m = PATTERN.matchEntire(text.trim()) ?: return null
            return FormatVersion(m.groupValues[1].toInt(), m.groupValues[2].toInt())
        }
    }
}
