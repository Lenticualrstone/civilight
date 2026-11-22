package io.github.lenticualrstone.civilight.feature.map.scale

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.domain.world.map.MapGeometry
import io.github.lenticualrstone.civilight.domain.world.map.MapScales
import io.github.lenticualrstone.civilight.domain.world.map.Travel
import io.github.lenticualrstone.civilight.domain.world.map.TravelModeEdits
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.feature.map.canvas.MapScene
import io.github.lenticualrstone.civilight.feature.map.canvas.ScenePin
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.ScaleUnit
import io.github.lenticualrstone.civilight.model.map.WorldMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * `축척 맞추기` being set (WLD-46): points A and B, which one the next tap places ([active], chosen
 * by its visible row), the typed number, the unit chip and, for `일`, the travel mode (by index in
 * `Travel.modes` order, so a project without stored modes still works). Saveable as JSON.
 */
@Serializable
data class ScaleDraft(
    val a: Point? = null,
    val b: Point? = null,
    /** 0 = A, 1 = B. */
    val active: Int = 0,
    val value: String = "",
    val unit: ScaleUnit = ScaleUnit.DAY,
    val modeIndex: Int = 0,
) {
    fun encode(): String = JSON.encodeToString(serializer(), this)

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }

        fun decode(text: String?): ScaleDraft? = text?.let { runCatching { JSON.decodeFromString(serializer(), it) }.getOrNull() }

        /** The map's current scale as a draft, or an empty one. */
        fun of(doc: ProjectDoc, map: WorldMap): ScaleDraft {
            val s = map.scale ?: return ScaleDraft(modeIndex = defaultIndex(doc))
            val modes = Travel.modes(doc)
            val index = s.travelModeKey?.let { k -> modes.indexOfFirst { it.key == k } }?.takeIf { it >= 0 } ?: defaultIndex(doc)
            return ScaleDraft(s.a, s.b, active = 1, value = Travel.number(s.value), unit = s.unit.known ?: ScaleUnit.DAY, modeIndex = index)
        }

        private fun defaultIndex(doc: ProjectDoc): Int {
            val modes = Travel.modes(doc)
            return modes.indexOfFirst { it.key == doc.project.defaultTravelModeKey }.takeIf { it >= 0 } ?: 0
        }
    }
}

/** What the scale card shows. */
data class ScaleUi(
    /** `A | 결사의 문`; `없음` until set. */
    val aLabel: String,
    val bLabel: String,
    val active: Int,
    val value: String,
    val unitIndex: Int,
    /** `하루 이동 | 도보 15 km` for the unit `일`, else null. */
    val dayTravel: String?,
    val canSave: Boolean,
)

/** The reducer and readings of `축척 맞추기`; pure. */
object ScaleEdit {
    /**
     * A tap on the map places the active point, snapped to a drawn pin within the pick radius; A
     * then moves on to B. Only the pins of [scene] count (rule 19: a hidden place's pin is not
     * drawn while locked, so it can neither be snapped to nor named).
     */
    fun tap(d: ScaleDraft, scene: MapScene, at: Point): ScaleDraft {
        val p = pinNear(scene, at, MapEdits.SNAP_RADIUS)?.at ?: MapGeometry.clamp(at)
        return if (d.active == 0) d.copy(a = p, active = 1) else d.copy(b = p)
    }

    fun choose(d: ScaleDraft, which: Int): ScaleDraft = d.copy(active = which.coerceIn(0, 1))

    fun unit(d: ScaleDraft, index: Int): ScaleDraft = d.copy(unit = ScaleUnit.entries.getOrElse(index) { ScaleUnit.DAY })

    private fun pinNear(scene: MapScene, at: Point, radius: Double): ScenePin? =
        scene.pins.map { it to MapGeometry.distance(it.at, at) }.filter { it.second <= radius }.minByOrNull { it.second }?.first

    /** The place of the drawn pin under [p] (`결사의 문`), or the point as numbers. */
    fun pointLabel(doc: ProjectDoc, scene: MapScene, p: Point?): String {
        if (p == null) return MapCopy.NONE
        pinNear(scene, p, 1e-4)?.let { pin -> doc.index.place(pin.placeKey)?.name }?.let { return it }
        return String.format(Locale.ROOT, "%.2f, %.2f", p.x, p.y)
    }

    fun valueOf(d: ScaleDraft): Double? = TravelModeEdits.parseNumber(d.value)?.takeIf { it > 0.0 }

    fun canSave(doc: ProjectDoc, d: ScaleDraft): Boolean {
        val a = d.a ?: return false
        val b = d.b ?: return false
        if (MapGeometry.distance(a, b) <= 0.0 || valueOf(d) == null) return false
        return d.unit != ScaleUnit.DAY || d.modeIndex in Travel.modes(doc).indices
    }

    /** The card; point labels name only pins drawn in [scene]. */
    fun ui(doc: ProjectDoc, scene: MapScene, d: ScaleDraft): ScaleUi {
        val mode = Travel.modes(doc).getOrNull(d.modeIndex)
        return ScaleUi(
            aLabel = pointLabel(doc, scene, d.a),
            bLabel = pointLabel(doc, scene, d.b),
            active = d.active,
            value = d.value,
            unitIndex = d.unit.ordinal,
            dayTravel = if (d.unit == ScaleUnit.DAY) mode?.let { MapCopy.dayTravel(it.name, Travel.number(it.kmPerDay)) } ?: MapCopy.NONE else null,
            canSave = canSave(doc, d),
        )
    }

    /**
     * `축척 저장`: the travel modes are stored first (the scale keeps a mode key for `일`), then the
     * scale; null when the draft cannot be saved.
     */
    fun save(doc: ProjectDoc, mapKey: Key, d: ScaleDraft, ctx: EditContext): ProjectDoc? {
        if (!canSave(doc, d)) return null
        val value = valueOf(d) ?: return null
        val stored = if (d.unit == ScaleUnit.DAY) TravelModeEdits.ensureStored(doc, ctx) else doc
        val modeKey = if (d.unit == ScaleUnit.DAY) TravelModeEdits.storedAt(stored, d.modeIndex)?.key ?: return null else null
        return MapEdits.setScale(stored, mapKey, MapScales.scale(d.a!!, d.b!!, value, d.unit, modeKey), ctx)
    }
}

/** The scale bar under a map: `<n>일 · <km> km` with n = 1 or 2 by zoom (WLD-46). */
object ScaleBars {
    /**
     * Days the bar of [barPx] stands for: the n in {1, 2} whose distance on screen is closest to
     * the bar's length, the smaller on a tie. [contentWidthPx] is the base's drawn width.
     */
    fun days(kmPerUnit: Double, kmPerDay: Double, contentWidthPx: Float, barPx: Float): Int {
        if (kmPerUnit <= 0.0 || kmPerDay <= 0.0 || contentWidthPx <= 0f) return 1
        val dayPx = kmPerDay / kmPerUnit * contentWidthPx
        return if (kotlin.math.abs(2 * dayPx - barPx) < kotlin.math.abs(dayPx - barPx)) 2 else 1
    }

    /** The bar text at the current zoom, or null without a scale. */
    fun label(doc: ProjectDoc, map: WorldMap, contentWidthPx: Float, barPx: Float): String? {
        val perUnit = MapScales.kmPerUnit(doc, map) ?: return null
        val mode = Travel.mode(doc, map.scale?.travelModeKey) ?: Travel.defaultMode(doc)
        return MapScales.barLabel(doc, map, days(perUnit, mode.kmPerDay, contentWidthPx, barPx))
    }
}
