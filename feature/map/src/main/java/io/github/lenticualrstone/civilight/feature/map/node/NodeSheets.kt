package io.github.lenticualrstone.civilight.feature.map.node

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
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SearchField
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.PlacePickerSheet
import kotlinx.serialization.Serializable

// Feature-local sheet keys of 노드 지도 (WLD-50).

/** Name and sub line of a new node at a tapped spot, or of [nodeKey]. */
@Serializable
data class NodeNameSheet(val x: Float = 0f, val y: Float = 0f, val nodeKey: Key? = null) : SheetKey {
    fun point(): Point = Point(x, y)
}

/** The label of a connection (`계단 · 잠김`). */
@Serializable
data class LinkLabelSheet(val linkKey: Key) : SheetKey

/** The place a node stands for (`장소 | 결사의 문`). */
@Serializable
data class NodePlaceSheet(val nodeKey: Key) : SheetKey

/** `장소 · 노드 찾기`. */
@Serializable
data object NodeSearchSheet : SheetKey

/** `노드`: `이름` and the sub line; `만들기` for a new node, `완료` for an existing one. */
@Composable
fun ColumnScope.NodeNameContent(initialName: String, initialSub: String, editing: Boolean, onDone: (String, String) -> Unit, onClose: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var sub by rememberSaveable { mutableStateOf(initialSub) }
    SheetHeader(MapCopy.NODE, onClose = onClose)
    SheetScrollBody {
        CivilightTextField(name, { name = it }, Modifier.padding(top = 12.dp), label = MapCopy.NAME)
        CivilightTextField(sub, { sub = it }, Modifier.padding(top = 10.dp), label = MapCopy.NODE_SUB)
    }
    SheetActions {
        CivilightButton(if (editing) MapCopy.BTN_DONE else MapCopy.BTN_CREATE, { onDone(name, sub) }, Modifier.weight(1f), enabled = name.isNotBlank())
    }
}

/** A connection's `라벨`; an empty text clears it. */
@Composable
fun ColumnScope.LinkLabelContent(initial: String, onDone: (String) -> Unit, onClose: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    SheetHeader(MapCopy.LABEL, onClose = onClose)
    SheetScrollBody { CivilightTextField(text, { text = it }, Modifier.padding(top = 12.dp), label = MapCopy.LABEL) }
    SheetActions { CivilightButton(MapCopy.BTN_DONE, { onDone(text) }, Modifier.weight(1f)) }
}

/** `장소`: the shared place list (places only, hidden ones behind `지문으로 보기`) and `없음`. */
@Composable
fun ColumnScope.NodePlaceContent(doc: ProjectDoc, gate: HiddenGate, current: Key?, onPick: (Key?) -> Unit, onClose: () -> Unit) {
    PlacePickerSheet(
        doc, gate, MapCopy.PLACE,
        onPick = { ref -> if (ref.kind.known == PlaceRefKind.PLACE) onPick(ref.key) },
        onClose = onClose,
        selected = current?.let { io.github.lenticualrstone.civilight.model.story.PlaceRef(PlaceRefKind.PLACE, it) },
        includeRegions = false,
    )
    SheetActions { CivilightButton(MapCopy.NONE, { onPick(null) }, Modifier.weight(1f), variant = ButtonVariant.GHOST) }
}

/** `장소 · 노드 찾기`: nodes by name, sub line or linked place (초성 too); a row centres the map on it. */
@Composable
fun ColumnScope.NodeSearchContent(search: (String) -> List<NodeHit>, onPick: (Key) -> Unit, onClose: () -> Unit) {
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    SheetHeader(MapCopy.NODE_SEARCH, onClose = onClose)
    SearchField(field, { field = it }, Modifier.padding(top = 12.dp), placeholder = io.github.lenticualrstone.civilight.ui.picker.PickerCopy.SEARCH_PLACEHOLDER)
    val hits = search(field.text)
    if (hits.isEmpty()) {
        CivilightText(MapCopy.NONE, CivilightTheme.type.small, Modifier.padding(vertical = 12.dp), color = CivilightTheme.colors.ink3)
    }
    LazyColumn(Modifier.weight(1f, fill = false).padding(top = 8.dp)) {
        items(hits, key = { it.key }) { hit -> ListRow(hit.title, subtitle = hit.sub, onClick = { onPick(hit.key) }) }
    }
}
