package io.github.lenticualrstone.civilight.feature.world.time

import androidx.compose.foundation.layout.Arrangement
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.feature.world.common.SheetSteps
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxRow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.FieldTableRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.CardText
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.EventCreateSheet
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.PickerCopy
import io.github.lenticualrstone.civilight.feature.world.common.worldPickerCreator
import io.github.lenticualrstone.civilight.ui.picker.PlacePickerSheet
import io.github.lenticualrstone.civilight.ui.picker.UnitPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** `고치기` of an existing event (WLD-32: tap a field in the detail). Feature-local. */
@Serializable
data class EventEditSheet(val eventKey: Key) : SheetKey

/** Which picker replaces the form inside the same sheet (the draft stays saved under it). */
private enum class EventPick { NONE, SCENE, PLACE, TERMS, PEOPLE, REVEAL }

/** `사건 만들기` (shared WORLD sheet, WLD-32): opened from the 연표 `+` and from other areas. */
@Composable
fun SheetScope.EventCreateSheetContent(sheet: EventCreateSheet) {
    EventFormContent(projectKey = sheet.projectKey, eventKey = null, saveId = "new")
}

/** `고치기` of [sheet]'s event from the detail. */
@Composable
fun SheetScope.EventEditSheetContent(projectKey: Key, sheet: EventEditSheet) {
    EventFormContent(projectKey = projectKey, eventKey = sheet.eventKey, saveId = sheet.eventKey)
}

/**
 * The 사건 form (`[PROPOSED]` layout, fields from the boards): `종류` (`세계 사건 · 이야기 사건`, new
 * events only), `이름`, `설명`, the time (`연도` and in calendar mode `월 · 일`; a story event's `씬`),
 * `장소`, `관련 용어`, `관련 인물`, `공개 시점`, primary `넣기` / `고치기`. Pickers open in place.
 */
@Composable
private fun SheetScope.EventFormContent(projectKey: Key, eventKey: Key?, saveId: String) {
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val gate = rememberHiddenGate(projectKey)
    val editor = rememberEventEditor(projectKey)
    val scope = rememberCoroutineScope()
    val existing = eventKey?.let { doc.index.event(it) }
    if (eventKey != null && (existing == null || existing.deletedAt != null)) {
        LaunchedEffect(Unit) { close() }
        return
    }
    var draft by rememberSaveable(saveId, stateSaver = EventDraft.Saver) { mutableStateOf(existing?.let(EventDraft::of) ?: EventDraft()) }
    var pick by rememberSaveable(saveId) { mutableStateOf(EventPick.NONE) }
    val readOnly = store.readOnly
    val ui = remember(doc, draft, gate.open) { EventForms.ui(doc, draft, editing = eventKey != null, hiddenOpen = gate.open) }

    val sheet = this
    // A picker opens over the form as the next step (P3): it pushes in, and back pops to the form.
    SheetSteps(pick, forward = { _, to -> to != EventPick.NONE }) { shownPick ->
        when (shownPick) {
            EventPick.SCENE -> UnitPickerSheet(
                doc = doc, gate = gate, title = WorldCopy.PICK_SCENE,
                onPick = { draft = draft.copy(sceneKey = it); pick = EventPick.NONE },
                onClose = { pick = EventPick.NONE },
                selectedKey = draft.sceneKey,
                levels = io.github.lenticualrstone.civilight.feature.world.term.RevealOptions.sceneLevels(doc),
            )
            // (0.2.0, U-15.3, WLD-70) The event place picker has the `＋ 새 장소` row on top.
            EventPick.PLACE -> PlacePickerSheet(
                doc = doc, gate = gate, title = WorldCopy.PICK_PLACE,
                onPick = { draft = draft.withPlace(it); pick = EventPick.NONE },
                onClose = { pick = EventPick.NONE },
                selected = draft.place,
                creator = worldPickerCreator(io.github.lenticualrstone.civilight.designsystem.component.CreateKind.PLACE),
            )
            EventPick.REVEAL -> UnitPickerSheet(
                doc = doc, gate = gate, title = WorldCopy.EVENT_REVEAL,
                onPick = { draft = draft.copy(revealUnitKey = it); pick = EventPick.NONE },
                onClose = { pick = EventPick.NONE },
                selectedKey = draft.revealUnitKey,
                levels = io.github.lenticualrstone.civilight.feature.world.term.RevealOptions.pointLevels(doc),
            )
            EventPick.TERMS -> TermTicks(doc, draft.termKeys, gate, onToggle = { draft = draft.toggleTerm(it) }, onClose = { pick = EventPick.NONE })
            EventPick.PEOPLE -> PeopleTicks(doc, draft.characterKeys, gate, onToggle = { draft = draft.toggleCharacter(it) }, onClose = { pick = EventPick.NONE })
            EventPick.NONE -> {
                val descriptionLocked = existing != null && CardText.hasLockedLinks(doc, existing.description, gate.open)
                SheetHeader(title = if (eventKey == null) WorldCopy.CREATE_EVENT else existing!!.title, onClose = sheet::close)
                SheetScrollBody {
                    FoldReveal(ui.kindChoice) {
                        Column {
                            CivilightText(WorldCopy.EVENT_KIND, CivilightTheme.type.small, Modifier.padding(top = 12.dp, bottom = 6.dp), color = CivilightTheme.colors.ink3)
                            SegmentedControl(
                                options = listOf(WorldCopy.EVENT_WORLD, WorldCopy.EVENT_STORY),
                                selectedIndex = if (draft.story) 1 else 0,
                                onSelect = { draft = draft.copy(story = it == 1) },
                            )
                        }
                    }
                    CivilightTextField(
                        value = draft.title,
                        onValueChange = { draft = draft.copy(title = it) },
                        label = WorldCopy.NAME,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        enabled = !readOnly,
                    )
                    ContentSwap(descriptionLocked, Modifier.fillMaxWidth(), label = "eventDescription") { locked ->
                        if (locked) {
                            LockRow(title = WorldCopy.HIDDEN_ITEM, actionLabel = WorldCopy.UNLOCK, onAction = gate.unlock, modifier = Modifier.padding(top = 12.dp))
                        } else {
                            CivilightTextField(
                                value = draft.description,
                                onValueChange = { draft = draft.copy(description = it) },
                                label = WorldCopy.DESCRIPTION,
                                singleLine = false,
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                enabled = !readOnly,
                            )
                        }
                    }
                    FoldReveal(ui.showYear) {
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val numbers = KeyboardOptions(keyboardType = KeyboardType.Number)
                            CivilightTextField(
                                value = draft.year, onValueChange = { draft = draft.copy(year = it) }, label = WorldCopy.EVENT_YEAR,
                                modifier = Modifier.weight(1f), enabled = !readOnly, keyboardOptions = numbers,
                                error = if (ui.dateInvalid) WorldCopy.EVENT_DATE_INVALID else null,
                            )
                            FoldReveal(ui.showMonthDay, Modifier.weight(2f), horizontal = true) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CivilightTextField(
                                        value = draft.month, onValueChange = { draft = draft.copy(month = it) }, label = WorldCopy.EVENT_MONTH,
                                        modifier = Modifier.weight(1f), enabled = !readOnly, keyboardOptions = numbers,
                                    )
                                    CivilightTextField(
                                        value = draft.day, onValueChange = { draft = draft.copy(day = it) }, label = WorldCopy.EVENT_DAY,
                                        modifier = Modifier.weight(1f), enabled = !readOnly, keyboardOptions = numbers,
                                    )
                                }
                            }
                        }
                    }
                    val rowClick: (EventPick) -> (() -> Unit)? = { p -> if (readOnly) null else ({ pick = p }) }
                    FoldReveal(ui.showScene) { FieldTableRow(WorldCopy.EVENT_SCENE, ui.sceneLabel, Modifier.padding(top = 12.dp), onClick = rowClick(EventPick.SCENE)) }
                    FieldTableRow(WorldCopy.EVENT_PLACE, ui.placeLabel, onClick = rowClick(EventPick.PLACE))
                    FieldTableRow(WorldCopy.EVENT_TERMS, ui.termsLabel, onClick = rowClick(EventPick.TERMS))
                    FieldTableRow(WorldCopy.EVENT_PEOPLE, ui.peopleLabel, onClick = rowClick(EventPick.PEOPLE))
                    FoldReveal(ui.showReveal) { FieldTableRow(WorldCopy.EVENT_REVEAL, ui.revealLabel, onClick = rowClick(EventPick.REVEAL)) }
                }
                SheetActions {
                    CivilightButton(
                        text = when {
                            readOnly -> WorldCopy.READ_ONLY
                            eventKey == null -> WorldCopy.BTN_PUT
                            else -> WorldCopy.BTN_EDIT
                        },
                        onClick = { scope.launch { if (editor.save(draft, eventKey) != null) sheet.close() } },
                        modifier = Modifier.weight(1f),
                        enabled = !readOnly && ui.canSave,
                    )
                }
            }
        }
    }
}

/**
 * (0.2.0, WLD-74) `관련 인물`: tick live characters (no create row, so no card inside the sheet); hidden ones
 * only after `지문으로 보기`. A ticked key whose character is gone stays stored and is not listed.
 */
@Composable
internal fun androidx.compose.foundation.layout.ColumnScope.PeopleTicks(doc: ProjectDoc, selected: List<Key>, gate: HiddenGate, onToggle: (Key) -> Unit, onClose: () -> Unit) {
    val people = remember(doc) { doc.cast.characters.filter { it.deletedAt == null }.sortedWith(compareBy({ it.order }, { it.key })) }
    val visible = people.filter { gate.open || !it.hidden }
    val hiddenCount = people.size - visible.size
    SheetHeader(title = WorldCopy.EVENT_PEOPLE, onClose = onClose)
    LazyColumn(Modifier.weight(1f, fill = false).padding(top = 8.dp)) {
        items(visible, key = { it.key }) { c ->
            CheckboxRow(label = c.name, checked = c.key in selected, onCheckedChange = { onToggle(c.key) })
        }
        if (hiddenCount > 0) {
            item(key = "hidden") {
                LockRow(title = PickerCopy.hiddenRow(PickerCopy.KIND_CHARACTER, hiddenCount), actionLabel = WorldCopy.UNLOCK, onAction = gate.unlock)
            }
        }
    }
}

/** `관련 용어`: tick live terms; hidden ones only after `지문으로 보기`. */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.TermTicks(doc: ProjectDoc, selected: List<Key>, gate: HiddenGate, onToggle: (Key) -> Unit, onClose: () -> Unit) {
    val terms = remember(doc) { doc.world.terms.filter { it.deletedAt == null }.sortedWith(compareBy({ it.order }, { it.key })) }
    val visible = terms.filter { gate.open || !it.hidden }
    val hiddenCount = terms.size - visible.size
    SheetHeader(title = WorldCopy.EVENT_TERMS, onClose = onClose)
    LazyColumn(Modifier.weight(1f, fill = false).padding(top = 8.dp)) {
        items(visible, key = { it.key }) { term ->
            CheckboxRow(label = term.name, checked = term.key in selected, onCheckedChange = { onToggle(term.key) })
        }
        if (hiddenCount > 0) {
            item(key = "hidden") {
                LockRow(title = PickerCopy.hiddenRow(PickerCopy.KIND_ITEM, hiddenCount), actionLabel = WorldCopy.UNLOCK, onAction = gate.unlock)
            }
        }
    }
}
