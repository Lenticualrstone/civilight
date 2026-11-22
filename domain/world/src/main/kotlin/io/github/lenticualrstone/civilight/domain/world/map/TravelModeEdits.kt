package io.github.lenticualrstone.civilight.domain.world.map

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.TravelMode

/** Why a travel mode edit was refused. */
enum class TravelModeRefusal { EMPTY_NAME, BAD_DISTANCE, BAD_HOURS, DUPLICATE_NAME, NO_MODE }

/** Result of a travel mode edit. */
sealed interface TravelModeEdit {
    data class Applied(val doc: ProjectDoc, val mode: TravelMode) : TravelModeEdit
    data class Refused(val reason: TravelModeRefusal) : TravelModeEdit
}

/**
 * Travel mode writes (WLD-47): each mode has a name, a daily distance (km) and daily moving hours.
 * [Travel.modes] falls back to [TravelMode.defaults] with fresh keys on every call while a project
 * stores none, so anything that keeps a mode key (`shownModeKey`, a scale in `일`) first stores the
 * modes with [ensureStored] in the same edit.
 */
object TravelModeEdits {
    /** The project's modes as stored; the defaults are written once when the project has none. */
    fun ensureStored(doc: ProjectDoc, ctx: EditContext): ProjectDoc {
        if (doc.project.travelModes.isNotEmpty()) return doc
        val modes = TravelMode.defaults(ctx.newKey, ctx.now(), ctx.device)
        return doc.copy(project = doc.project.copy(travelModes = modes))
    }

    /** The stored mode at [index] of [Travel.modes] order, after [ensureStored]. */
    fun storedAt(doc: ProjectDoc, index: Int): TravelMode? = Travel.modes(doc).getOrNull(index)

    private fun check(doc: ProjectDoc, name: String, kmPerDay: Double, hours: Double, except: Key?): TravelModeRefusal? = when {
        name.isBlank() -> TravelModeRefusal.EMPTY_NAME
        !(kmPerDay > 0.0) || !kmPerDay.isFinite() -> TravelModeRefusal.BAD_DISTANCE
        !(hours > 0.0) || hours > 24.0 -> TravelModeRefusal.BAD_HOURS
        doc.project.travelModes.any { it.key != except && it.name == name.trim() } -> TravelModeRefusal.DUPLICATE_NAME
        else -> null
    }

    /** `직접`: a new mode after the others. */
    fun add(doc: ProjectDoc, name: String, kmPerDay: Double, movingHoursPerDay: Double, ctx: EditContext): TravelModeEdit {
        val stored = ensureStored(doc, ctx)
        check(stored, name, kmPerDay, movingHoursPerDay, null)?.let { return TravelModeEdit.Refused(it) }
        val mode = TravelMode(
            key = ctx.newKey(),
            order = OrderKeys.after(stored.project.travelModes.maxOfOrNull { it.order }),
            name = name.trim(),
            kmPerDay = kmPerDay,
            movingHoursPerDay = movingHoursPerDay,
            updatedAt = ctx.now(),
            updatedBy = ctx.device,
        )
        return TravelModeEdit.Applied(stored.copy(project = stored.project.copy(travelModes = stored.project.travelModes + mode)), mode)
    }

    /** Changes a stored mode's name and values. */
    fun update(doc: ProjectDoc, modeKey: Key, name: String, kmPerDay: Double, movingHoursPerDay: Double, ctx: EditContext): TravelModeEdit {
        val old = doc.project.travelModes.firstOrNull { it.key == modeKey } ?: return TravelModeEdit.Refused(TravelModeRefusal.NO_MODE)
        check(doc, name, kmPerDay, movingHoursPerDay, modeKey)?.let { return TravelModeEdit.Refused(it) }
        val mode = old.copy(name = name.trim(), kmPerDay = kmPerDay, movingHoursPerDay = movingHoursPerDay, updatedAt = ctx.now(), updatedBy = ctx.device)
        val modes = doc.project.travelModes.map { if (it.key == modeKey) mode else it }
        return TravelModeEdit.Applied(doc.copy(project = doc.project.copy(travelModes = modes)), mode)
    }

    /**
     * A number typed by the author (`15`, `12.5`, `12,5`), or null. Values with more than one
     * decimal separator or other characters are refused rather than guessed.
     */
    fun parseNumber(text: String): Double? {
        val t = text.trim().replace(',', '.')
        if (t.isEmpty() || !t.matches(Regex("""\d+(\.\d+)?"""))) return null
        return t.toDoubleOrNull()
    }
}
