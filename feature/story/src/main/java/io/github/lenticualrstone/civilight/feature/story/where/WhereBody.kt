package io.github.lenticualrstone.civilight.feature.story.where

/** Which body the 장소와 때 sheet shows: the form, the cascade preview, or one of the pickers swapped in place. */
enum class WhereBody {
    FORM, CASCADE, PICK_PLACES, PICK_REGIONS, PICK_EVENT, PICK_CAST;

    companion object {
        /** The cascade preview wins over a picker, a picker over the form (the order of the old early returns). */
        fun of(preview: Boolean, picking: String?): WhereBody = when {
            preview -> CASCADE
            picking == KEY_PLACES -> PICK_PLACES
            picking == KEY_REGIONS -> PICK_REGIONS
            picking == KEY_EVENT -> PICK_EVENT
            picking == KEY_CAST -> PICK_CAST
            else -> FORM
        }
    }
}
