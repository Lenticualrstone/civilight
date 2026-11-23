package io.github.lenticualrstone.civilight.domain.library.format

/**
 * Feature id → Korean name for the newer-file sheet (LIB-53, `ED` table kept in the app). It lists
 * ids this app does not read too, since those are the ones the sheet names. An id without an entry
 * is shown as the id itself.
 */
object FeatureNames {
    private val names: Map<String, String> = mapOf(
        "maps.gen.v2" to "지도 생성 2판",
        "gallery" to "갤러리",
        "versions" to "지난 판",
    )

    fun korean(id: String): String = names[id] ?: id
}
