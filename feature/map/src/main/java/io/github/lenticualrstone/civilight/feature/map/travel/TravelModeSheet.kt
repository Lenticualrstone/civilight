package io.github.lenticualrstone.civilight.feature.map.travel

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.domain.world.map.Travel
import io.github.lenticualrstone.civilight.domain.world.map.TravelModeEdits
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.SheetKey
import kotlinx.serialization.Serializable

/**
 * `이동 수단` (WLD-47): the project's modes with their numbers, `직접` for a new one, and the form
 * of one mode. Feature-local; opened from the route sheet's `직접` chip ([routeKey] then takes the
 * new mode) and from the scale sheet's `하루 이동` row ([forScale]: a pick returns the mode).
 */
@Serializable
data class TravelModeSheet(val routeKey: Key? = null, val forScale: Boolean = false) : SheetKey

/** One row of the mode list: `도보 15 km · 8시간`. */
data class TravelModeRow(val index: Int, val modeKey: Key, val label: String, val selected: Boolean)

/** The mode form's text as typed and what it parses to (refused values keep the button off). */
data class TravelModeForm(val name: String = "", val km: String = "", val hours: String = "") {
    val kmValue: Double? get() = TravelModeEdits.parseNumber(km)?.takeIf { it > 0.0 }
    val hoursValue: Double? get() = TravelModeEdits.parseNumber(hours)?.takeIf { it > 0.0 && it <= 24.0 }

    /** True when the name is new (or the edited mode's own) and both numbers are usable. */
    fun canSave(doc: ProjectDoc, editing: Key?): Boolean =
        name.isNotBlank() && kmValue != null && hoursValue != null &&
            Travel.modes(doc).none { it.key != editing && it.name == name.trim() }

    companion object {
        fun of(doc: ProjectDoc, modeKey: Key?): TravelModeForm =
            modeKey?.let { Travel.mode(doc, it) }?.let { TravelModeForm(it.name, Travel.number(it.kmPerDay), Travel.number(it.movingHoursPerDay)) }
                ?: TravelModeForm()
    }
}

object TravelModeRows {
    /** Rows in `Travel.modes` order; [selectedKey] (or the project default) is marked. */
    fun rows(doc: ProjectDoc, selectedKey: Key?): List<TravelModeRow> {
        val chosen = Travel.mode(doc, selectedKey) ?: Travel.defaultMode(doc)
        return Travel.modes(doc).mapIndexed { i, m -> TravelModeRow(i, m.key, Travel.modeLabel(m), m.key == chosen.key) }
    }
}

/**
 * The sheet body. [onPick] gets a row's index in `Travel.modes` order (keys of unsaved default
 * modes are not stable, so the caller stores the modes first); [onSave] gets the edited mode key (or
 * null for `직접`) and the form values.
 */
@Composable
fun ColumnScope.TravelModeContent(
    doc: ProjectDoc,
    selectedKey: Key?,
    readOnly: Boolean,
    onPick: ((Int) -> Unit)?,
    onSave: (Key?, String, Double, Double) -> Unit,
    onClose: () -> Unit,
) {
    // null = the list; "" = a new mode (`직접`); else the key of the mode being edited.
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var km by rememberSaveable { mutableStateOf("") }
    var hours by rememberSaveable { mutableStateOf("") }
    SheetHeader(MapCopy.TRAVEL_MODES, onClose = onClose)
    val target = editing
    if (target == null) {
        SheetScrollBody {
            TravelModeRows.rows(doc, selectedKey).forEach { row ->
                ListRow(
                    title = row.label,
                    selected = row.selected,
                    onClick = {
                        if (onPick != null) {
                            onPick(row.index)
                        } else if (!readOnly) {
                            val form = TravelModeForm.of(doc, row.modeKey)
                            name = form.name; km = form.km; hours = form.hours
                            editing = row.modeKey
                        }
                    },
                )
            }
            ListRow(MapCopy.CUSTOM_MODE, onClick = if (readOnly) null else ({ name = ""; km = ""; hours = ""; editing = "" }))
        }
        return
    }
    val key = target.ifEmpty { null }
    val form = TravelModeForm(name, km, hours)
    SheetScrollBody {
        CivilightTextField(name, { name = it }, Modifier.padding(top = 12.dp), label = MapCopy.NAME)
        CivilightTextField(
            km, { km = it }, Modifier.padding(top = 10.dp), label = MapCopy.DAY_TRAVEL, unit = MapCopy.UNIT_KM,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        CivilightTextField(
            hours, { hours = it }, Modifier.padding(top = 10.dp), label = MapCopy.PER_DAY, unit = MapCopy.UNIT_HOURS,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
    SheetActions {
        CivilightButton(MapCopy.BTN_CANCEL, { editing = null }, Modifier.weight(1f), variant = ButtonVariant.GHOST)
        CivilightButton(
            if (key == null) MapCopy.BTN_CREATE else MapCopy.BTN_DONE,
            {
                val k = form.kmValue
                val h = form.hoursValue
                if (k != null && h != null) {
                    onSave(key, name.trim(), k, h)
                    editing = null
                }
            },
            Modifier.weight(1f),
            enabled = !readOnly && form.canSave(doc, key),
        )
    }
}
