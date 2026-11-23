package io.github.lenticualrstone.civilight.model.asset

/**
 * The ids and names of the eight bundled preset images (CNV-26), for modules that name a stored
 * `preset:<id>` without depending on `feature:canvas` (U-08 LIB-81 · LIB-82: `지금 등불`, `등불 ›`).
 * Ids are file format; names are the picker's names. Pictures: `feature:canvas` `PresetImages` and
 * `core:ui` `PresetPictures` (tests keep all three in the same order).
 */
object Presets {
    val IDS: List<String> = listOf("ash-dawn", "lantern", "deep-sea", "mist-forest", "desert-ruin", "nebula", "red-citadel", "snowfield")

    /** Picker names (CNV-26), in [IDS] order. */
    val NAMES: List<String> = listOf("잿빛 새벽", "등불", "심해", "안개 숲", "사막 유적", "성운", "붉은 성채", "설원")

    /** The name of preset [id], or null for an unknown id. */
    fun nameOf(id: String?): String? = IDS.indexOf(id).takeIf { it >= 0 }?.let(NAMES::get)
}
