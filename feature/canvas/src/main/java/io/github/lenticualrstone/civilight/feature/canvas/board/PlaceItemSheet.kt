package io.github.lenticualrstone.civilight.feature.canvas.board

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SearchField
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.feature.canvas.CanvasCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.world.TimelineEvent
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.picker.CharacterPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.FactionPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.TermPickerSheet
import io.github.lenticualrstone.civilight.ui.search.KoreanSearch
import kotlinx.serialization.Serializable

/** The item picker of `노드` or `인물` (CNV-04), placing the picked item at ([x], [y]) content dp. */
@Serializable
data class PlaceItemSheet(val projectKey: Key, val canvasKey: Key, val tool: CanvasTool, val x: Float, val y: Float) : SheetKey

/** Kinds `노드` places, in the order of its segmented control (`세력 · 사건 · 용어`). */
val NodeToolKinds = listOf(CanvasNodeKind.FACTION, CanvasNodeKind.EVENT, CanvasNodeKind.TERM)

/**
 * Picker content: `인물` uses the shared character picker (search with 초성, ordering instead of
 * auto-selection, 100+ items); `노드` adds a `세력 · 사건 · 용어` segmented control over the shared
 * faction and term pickers and a local event list (the shell's pickers have no event kind; Requests).
 */
@Composable
fun ColumnScope.PlaceItemContent(
    sheet: PlaceItemSheet,
    doc: ProjectDoc,
    gate: HiddenGate,
    onPick: (CanvasNodeKind, Key) -> Unit,
    onClose: () -> Unit,
) {
    if (sheet.tool == CanvasTool.CHARACTER) {
        CharacterPickerSheet(
            doc = doc,
            gate = gate,
            onPick = { onPick(CanvasNodeKind.CHARACTER, it) },
            onClose = onClose,
            title = CanvasCopy.pickTitle(CanvasCopy.KIND_CHARACTER),
        )
        return
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    SegmentedControl(CanvasCopy.NODE_TOOL_KINDS, index, { index = it }, Modifier.padding(bottom = 12.dp))
    val title = CanvasCopy.pickTitle(CanvasCopy.NODE_TOOL_KINDS[index])
    when (NodeToolKinds[index]) {
        CanvasNodeKind.FACTION -> FactionPickerSheet(doc, gate, title, { onPick(CanvasNodeKind.FACTION, it) }, onClose)
        CanvasNodeKind.TERM -> TermPickerSheet(doc, gate, title, { key, _ -> onPick(CanvasNodeKind.TERM, key) }, onClose)
        else -> EventPicker(doc, title, { onPick(CanvasNodeKind.EVENT, it) }, onClose)
    }
}

/** Live events matching [query] (title, 초성), in timeline order. */
fun eventRows(doc: ProjectDoc, query: String): List<TimelineEvent> =
    doc.world.events
        .filter { it.deletedAt == null }
        .filter { query.isBlank() || KoreanSearch.matches(it.title, query) }
        .sortedWith(compareBy<TimelineEvent>({ it.order }, { it.key }))

@Composable
private fun ColumnScope.EventPicker(doc: ProjectDoc, title: String, onPick: (Key) -> Unit, onClose: () -> Unit) {
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val rows = remember(doc, field.text) { eventRows(doc, field.text) }
    SheetHeader(title = title, onClose = onClose, closeLabel = CanvasCopy.BTN_CLOSE)
    SearchField(value = field, onValueChange = { field = it }, modifier = Modifier.padding(top = 12.dp), placeholder = CanvasCopy.SEARCH_PLACEHOLDER)
    LazyColumn(Modifier.weight(1f, fill = false).padding(top = 8.dp)) {
        items(rows, key = { it.key }) { event ->
            ListRow(title = event.title, onClick = { onPick(event.key) }, compact = true)
        }
    }
}
