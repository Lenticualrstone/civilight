package io.github.lenticualrstone.civilight.feature.story.bulk

/** Which body the bulk screen shows: nothing yet, the cover list, or the inner table (a fold swaps the last two). */
enum class BulkBody {
    LOADING, COVER, WIDE;

    companion object {
        fun of(loaded: Boolean, wide: Boolean): BulkBody = when {
            !loaded -> LOADING
            wide -> WIDE
            else -> COVER
        }
    }
}
