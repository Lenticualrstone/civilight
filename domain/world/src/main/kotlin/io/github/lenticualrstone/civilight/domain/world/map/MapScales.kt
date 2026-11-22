package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.MapBaseKind
import io.github.lenticualrstone.civilight.model.map.MapScale
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.model.map.ScaleUnit
import io.github.lenticualrstone.civilight.model.map.WorldMap
import io.github.lenticualrstone.civilight.model.open

/** `축척 맞추기` (WLD-46): km per map unit from one known distance. */
object MapScales {
    /** km of one `리` (Q-19 default; the sources give no value). */
    const val KM_PER_LI = 0.4

    /** True for maps that never have a scale (node maps). */
    fun isNodeMap(map: WorldMap): Boolean = map.baseKind.known == MapBaseKind.NODE || map.format?.known == MapFormat.NODE

    /** The known distance of a scale in km, or null (`직접` and unknown units cannot be converted). */
    fun valueKm(doc: ProjectDoc, scale: MapScale): Double? = when (scale.unit.known) {
        ScaleUnit.KM -> scale.value
        ScaleUnit.LI -> scale.value * KM_PER_LI
        ScaleUnit.DAY -> {
            val mode = Travel.mode(doc, scale.travelModeKey) ?: Travel.defaultMode(doc)
            scale.value * mode.kmPerDay
        }
        ScaleUnit.CUSTOM, null -> null
    }

    /** km per normalised map unit (`value_km / |AB|`), or null without a usable scale. */
    fun kmPerUnit(doc: ProjectDoc, map: WorldMap): Double? {
        if (isNodeMap(map)) return null
        val scale = map.scale ?: return null
        val ab = MapGeometry.distance(scale.a, scale.b)
        if (ab <= 0.0) return null
        return valueKm(doc, scale)?.div(ab)
    }

    /** A new scale; for [ScaleUnit.DAY] the travel mode the day refers to. */
    fun scale(a: Point, b: Point, value: Double, unit: ScaleUnit, travelModeKey: String? = null): MapScale =
        MapScale(a = MapGeometry.clamp(a), b = MapGeometry.clamp(b), value = value, unit = unit.open(), travelModeKey = travelModeKey.takeIf { unit == ScaleUnit.DAY })

    /** Scale bar text `<n>일 · <km> km` (n = 1 or 2), or null without a scale. */
    fun barLabel(doc: ProjectDoc, map: WorldMap, days: Int = 1): String? {
        if (kmPerUnit(doc, map) == null) return null
        val mode = Travel.mode(doc, map.scale?.travelModeKey) ?: Travel.defaultMode(doc)
        return "${days}일${Copy.SEP}${Travel.number(mode.kmPerDay * days)} km"
    }
}
