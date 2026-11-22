package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.TravelMode
import kotlin.math.floor
import kotlin.math.roundToLong

/** Travel time by mode (rule 18 §3, WLD-43, WLD-47). */
object Travel {
    /**
     * Required minutes for [km] by [mode]: full = ⌊d / D⌋ days of 24 h, then the rest moves at
     * D km per H moving hours: `full·24·60 + rest / D · H · 60`.
     */
    fun requiredMinutes(km: Double, mode: TravelMode): Double {
        require(mode.kmPerDay > 0) { "travel mode without a daily distance" }
        val full = floor(km / mode.kmPerDay)
        val rest = km - full * mode.kmPerDay
        return full * 24 * 60 + rest / mode.kmPerDay * mode.movingHoursPerDay * 60
    }

    /**
     * `약 7시간 40분` (under a day, minutes rounded to 10) or `2일 4시간` (hours rounded to the
     * nearest hour, carried into days); never decimal days.
     */
    fun formatDuration(minutes: Double): String {
        if (minutes < MINUTES_PER_DAY) {
            val rounded = (minutes / 10.0).roundToLong() * 10
            if (rounded >= MINUTES_PER_DAY) return formatDuration(rounded.toDouble())
            val h = rounded / 60
            val m = rounded % 60
            return when {
                h == 0L -> "약 ${m}분"
                m == 0L -> "약 ${h}시간"
                else -> "약 ${h}시간 ${m}분"
            }
        }
        val totalHours = (minutes / 60.0).roundToLong()
        val days = totalHours / 24
        val hours = totalHours % 24
        return if (hours == 0L) "${days}일" else "${days}일 ${hours}시간"
    }

    /** `도보 15 km · 8시간`. */
    fun modeLabel(mode: TravelMode): String = "${mode.name} ${number(mode.kmPerDay)} km${Copy.SEP}${number(mode.movingHoursPerDay)}시간"

    /** The project's travel modes in order; the defaults when the project has none. */
    fun modes(doc: ProjectDoc): List<TravelMode> =
        doc.project.travelModes.sortedWith(compareBy({ it.order }, { it.key })).ifEmpty { TravelMode.defaults() }

    /** The project default mode (도보 when unset). */
    fun defaultMode(doc: ProjectDoc): TravelMode {
        val modes = modes(doc)
        return doc.project.defaultTravelModeKey?.let { k -> modes.firstOrNull { it.key == k } } ?: modes.first()
    }

    fun mode(doc: ProjectDoc, key: Key?): TravelMode? = key?.let { k -> modes(doc).firstOrNull { it.key == k } }

    /** `38 km`, `1.5 km`. */
    fun number(value: Double): String =
        if (value == floor(value)) value.toLong().toString() else "%.1f".format(java.util.Locale.ROOT, value)

    private const val MINUTES_PER_DAY = 24 * 60
}
