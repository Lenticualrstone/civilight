package io.github.lenticualrstone.civilight.feature.map.edit

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SearchField
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.picker.FactionPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.UnitPickerSheet
import kotlinx.serialization.Serializable

// Feature-local sheet keys of the editor. [target] null = the region being drawn.

/** `핀 · 장소 고르기` for a tapped spot (WLD-41). */
@Serializable
data class PinPlaceSheet(val x: Float, val y: Float) : SheetKey

/** Label text: a new label at a spot, or the text of [labelKey] (WLD-44). */
@Serializable
data class LabelTextSheet(val x: Float = 0f, val y: Float = 0f, val labelKey: Key? = null) : SheetKey

/** `보이는 시점` of an overlay or of the region being drawn. */
@Serializable
data class RevealSheet(val target: Key?) : SheetKey

/** `레이어` of an overlay or of the region being drawn. */
@Serializable
data class LayerSheet(val target: Key?) : SheetKey

/** `연결 | 세력` of a region. */
@Serializable
data class FactionSheet(val target: Key?) : SheetKey

/** `종류` of a region (`세력 영역 · 지역 · 기타`, Q-17). */
@Serializable
data class KindSheet(val target: Key?) : SheetKey

/** `이름` of a region. */
@Serializable
data class RegionNameSheet(val target: Key?) : SheetKey

/**
 * `핀 · 장소 고르기`: places with their scene counts (`잿더미 시장 · 씬 4개`), search, and `새 장소`
 * (name → a place and its pin in one step). Places already pinned on this map read `이 지도에 핀
 * 있음` and cannot be picked (one pin per place per map).
 */
@Composable
fun ColumnScope.PinPlaceContent(
    model: PinPickModel,
    query: String,
    onQuery: (String) -> Unit,
    gate: HiddenGate,
    onPick: (Key) -> Unit,
    onCreate: (String) -> Unit,
    onClose: () -> Unit,
) {
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(query)) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    SheetHeader(MapCopy.PIN_PICK_TITLE, onClose = onClose)
    if (creating) {
        SheetScrollBody {
            CivilightTextField(name, { name = it }, Modifier.padding(top = 12.dp), label = MapCopy.PLACE_NAME)
        }
        SheetActions {
            CivilightButton(MapCopy.BTN_CANCEL, { creating = false }, Modifier.weight(1f), variant = ButtonVariant.GHOST)
            CivilightButton(MapCopy.BTN_CREATE, { onCreate(name) }, Modifier.weight(1f), enabled = name.isNotBlank())
        }
        return
    }
    SearchField(field, { field = it; onQuery(it.text) }, Modifier.padding(top = 12.dp), placeholder = io.github.lenticualrstone.civilight.ui.picker.PickerCopy.SEARCH_PLACEHOLDER)
    LazyColumn(Modifier.weight(1f, fill = false).padding(top = 8.dp)) {
        item(key = "new") { ListRow(MapCopy.NEW_PLACE, onClick = { creating = true; name = field.text }) }
        items(model.rows, key = { it.placeKey }) { row ->
            ListRow(
                title = row.name,
                subtitle = row.sub,
                onClick = if (row.pinned) null else ({ onPick(row.placeKey) }),
            )
        }
        model.lockRow?.let { text -> item(key = "lock") { LockRow(text, MapCopy.UNLOCK, gate.unlock, Modifier.padding(top = 8.dp)) } }
    }
}

/** Label text with `놓기` (new) or `완료` (edit). */
@Composable
fun ColumnScope.LabelTextContent(initial: String, editing: Boolean, onDone: (String) -> Unit, onClose: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    SheetHeader(MapCopy.LABEL, onClose = onClose)
    SheetScrollBody { CivilightTextField(text, { text = it }, Modifier.padding(top = 12.dp), label = MapCopy.LABEL) }
    SheetActions {
        CivilightButton(if (editing) MapCopy.BTN_DONE else MapCopy.BTN_PLACE_LABEL, { onDone(text) }, Modifier.weight(1f), enabled = text.isNotBlank())
    }
}

/** `보이는 시점`: a unit (the item shows from its start, `1-7부터`) or `없음`. */
@Composable
fun ColumnScope.RevealContent(doc: ProjectDoc, gate: HiddenGate, onPick: (Key?) -> Unit, onClose: () -> Unit) {
    UnitPickerSheet(doc, gate, MapCopy.REVEAL_POINT, onPick = { onPick(it) }, onClose = onClose)
    SheetActions { CivilightButton(MapCopy.NONE, { onPick(null) }, Modifier.weight(1f), variant = ButtonVariant.GHOST) }
}

/** `레이어`: the map's layers or `없음`. */
@Composable
fun ColumnScope.LayerPickContent(layers: List<LayerRowUi>, current: Key?, onPick: (Key?) -> Unit, onClose: () -> Unit) {
    SheetHeader(MapCopy.LAYER, onClose = onClose)
    SheetScrollBody {
        RadioRow(MapCopy.NONE, selected = current == null, onClick = { onPick(null) })
        layers.forEach { l -> RadioRow(l.name, selected = l.key == current, onClick = { onPick(l.key) }) }
    }
}

/** `연결 | 세력`. */
@Composable
fun ColumnScope.FactionContent(doc: ProjectDoc, gate: HiddenGate, current: Key?, onPick: (Key?) -> Unit, onClose: () -> Unit) {
    FactionPickerSheet(doc, gate, MapCopy.FACTION, onPick = { onPick(it) }, onClose = onClose, selectedKey = current)
    SheetActions { CivilightButton(MapCopy.NONE, { onPick(null) }, Modifier.weight(1f), variant = ButtonVariant.GHOST) }
}

/** `종류`: `세력 영역 · 지역 · 기타`. */
@Composable
fun ColumnScope.KindContent(current: String, onPick: (String) -> Unit, onClose: () -> Unit) {
    SheetHeader(MapCopy.KIND, onClose = onClose)
    SheetScrollBody { MapCopy.REGION_KINDS.forEach { k -> RadioRow(k, selected = k == current, onClick = { onPick(k) }) } }
}

/** `이름` of a region. */
@Composable
fun ColumnScope.NameContent(initial: String, onDone: (String) -> Unit, onClose: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    SheetHeader(MapCopy.REGION_NAME, onClose = onClose)
    SheetScrollBody { CivilightTextField(text, { text = it }, Modifier.padding(top = 12.dp), label = MapCopy.REGION_NAME) }
    SheetActions { CivilightButton(MapCopy.BTN_DONE, { onDone(text) }, Modifier.weight(1f)) }
}

/** A layer's name: rename [layerKey], or a new layer when null (WLD-45). */
@Serializable
data class LayerNameSheet(val layerKey: Key? = null) : SheetKey

/** `하위 지도 · 고르기` for [pinKey] (WLD-48). */
@Serializable
data class ChildMapSheet(val pinKey: Key) : SheetKey

/** `레이어` name: `완료` renames, `만들기` adds. */
@Composable
fun ColumnScope.LayerNameContent(initial: String, editing: Boolean, onDone: (String) -> Unit, onClose: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    SheetHeader(MapCopy.LAYER, onClose = onClose)
    SheetScrollBody { CivilightTextField(text, { text = it }, Modifier.padding(top = 12.dp), label = MapCopy.NAME) }
    SheetActions {
        CivilightButton(if (editing) MapCopy.BTN_DONE else MapCopy.BTN_CREATE, { onDone(text) }, Modifier.weight(1f), enabled = text.isNotBlank())
    }
}

/** `하위 지도 · 고르기`: maps that may sit under this pin (not this map, not above it). */
@Composable
fun ColumnScope.ChildMapContent(maps: List<Pair<Key, String>>, onPick: (Key) -> Unit, onClose: () -> Unit) {
    SheetHeader(MapCopy.CHILD_MAP, onClose = onClose)
    SheetScrollBody {
        if (maps.isEmpty()) io.github.lenticualrstone.civilight.designsystem.component.CivilightText(
            MapCopy.NONE, io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme.type.small, Modifier.padding(vertical = 12.dp),
        )
        maps.forEach { (key, name) -> ListRow(name, onClick = { onPick(key) }) }
    }
}

/** Sheet anchor point helper. */
fun PinPlaceSheet.point(): Point = Point(x, y)

fun LabelTextSheet.point(): Point = Point(x, y)
