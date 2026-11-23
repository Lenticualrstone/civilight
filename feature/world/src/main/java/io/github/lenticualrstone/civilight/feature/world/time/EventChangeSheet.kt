package io.github.lenticualrstone.civilight.feature.world.time

import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.feature.world.common.worldPickerCreator
import androidx.compose.foundation.layout.Arrangement
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.feature.world.common.SheetSteps
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.RemovableChip
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.FactionPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.ItemPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.PickKind
import io.github.lenticualrstone.civilight.ui.picker.PickerModels
import io.github.lenticualrstone.civilight.ui.picker.PlacePickerSheet
import io.github.lenticualrstone.civilight.ui.picker.TermPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** `+ 변화` on the event detail (feature-local key; cast's value sheet takes no event anchor, see Requests). */
@Serializable
data class EventChangeSheet(val eventKey: Key) : SheetKey

private val json = Json { ignoreUnknownKeys = true }

private val DraftSaver: Saver<ChangeDraft, String> = Saver(
    save = { json.encodeToString(ChangeDraft.serializer(), it) },
    restore = { runCatching { json.decodeFromString(ChangeDraft.serializer(), it) }.getOrNull() ?: ChangeDraft() },
)

/**
 * `+ 변화` (WLD-33): step 1 picks the item (인물 · 용어 · 장소 · 세력, 초성 search, hidden row),
 * step 2 one of its fields (with its value now), step 3 the new value in the field's own input,
 * then `넣기` writes a `◆` anchored on this event. Each step replaces the one before; `뒤로`-style
 * moves are the header actions of each step. [onAdd] writes and returns false when nothing was saved.
 */
@Composable
fun SheetScope.EventChangeContent(eventKey: Key, onAdd: suspend (ChangeDraft) -> Boolean) {
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val gate = rememberHiddenGate(store.key)
    var draft by rememberSaveable(eventKey, stateSaver = DraftSaver) { mutableStateOf(ChangeDraft()) }
    var step by rememberSaveable(eventKey) { mutableStateOf(ChangeStep.ITEM) }
    val scope = rememberCoroutineScope()
    val sheet = this
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val eventName = doc.index.event(eventKey)?.title.orEmpty()

    SheetSteps(EventChanges.effectiveStep(doc, step, draft), forward = { from, to -> ChangeStep.isAhead(from, to) }) { shownStep ->
        when (shownStep) {
            ChangeStep.ITEM -> {
                var query by rememberSaveable { mutableStateOf("") }
                val model = remember(doc, query, gate.open) { PickerModels.links(doc, query, gate.open, EventChanges.PICK_KINDS) }
                ItemPickerSheet(
                    title = WorldCopy.CHANGE_TITLE,
                    model = model,
                    query = query,
                    onQueryChange = { query = it },
                    onPick = { item ->
                        val kind = EventChanges.entityKindOf(item.kind) ?: return@ItemPickerSheet
                        draft = ChangeDraft(kind = kind, entityKey = item.key)
                        step = ChangeStep.FIELD
                    },
                    onClose = sheet::close,
                    onUnlockHidden = gate.unlock,
                )
            }
            ChangeStep.FIELD -> {
                val kind = draft.kind ?: return@SheetSteps
                val entityKey = draft.entityKey ?: return@SheetSteps
                val rows = remember(doc, kind, entityKey) { EventChanges.fieldRows(doc, kind, entityKey) }
                SheetHeader(title = itemName(doc, kind, entityKey), onClose = { step = ChangeStep.ITEM }, subtitle = eventName, closeLabel = WorldCopy.BACK)
                SheetScrollBody(Modifier.padding(top = 8.dp)) {
                    FoldReveal(rows.isEmpty()) { CivilightText(WorldCopy.NO_FIELDS, type.small, Modifier.padding(top = 12.dp), color = colors.ink3) }
                    rows.forEach { row ->
                        ListRow(
                            title = row.name,
                            subtitle = row.current,
                            compact = true,
                            onClick = {
                                draft = draft.copy(fieldDefKey = row.defKey, text = "", optionKeys = emptyList(), year = "", month = "", day = "", targetKeys = emptyList())
                                step = ChangeStep.VALUE
                            },
                        )
                    }
                }
            }
            ChangeStep.TARGET -> {
                val def = EventChanges.def(doc, draft) ?: return@SheetSteps
                val add: (Key) -> Unit = { key ->
                    draft = draft.copy(targetKeys = (draft.targetKeys + key).distinct())
                    step = ChangeStep.VALUE
                }
                val back = { step = ChangeStep.VALUE }
                when (EventChanges.linkPick(def)) {
                    // (0.2.0, U-15.3, WLD-70) `연결` value pickers have the create row of their kind.
                    PickKind.PLACE -> PlacePickerSheet(doc, gate, def.name, onPick = { add(it.key) }, onClose = back, includeRegions = false, creator = worldPickerCreator(CreateKind.PLACE))
                    PickKind.TERM -> TermPickerSheet(doc, gate, def.name, onPick = { key, _ -> add(key) }, onClose = back, creator = worldPickerCreator(CreateKind.TERM))
                    else -> FactionPickerSheet(doc, gate, def.name, onPick = add, onClose = back, creator = worldPickerCreator(CreateKind.FACTION))
                }
            }
            ChangeStep.VALUE -> {
                val def = EventChanges.def(doc, draft) ?: return@SheetSteps
                val kind = draft.kind ?: return@SheetSteps
                val entityKey = draft.entityKey ?: return@SheetSteps
                val input = EventChanges.input(doc, def, draft)
                val valid = EventChanges.value(def, draft) { "" } != null
                SheetHeader(
                    title = WorldCopy.changeTitle(itemName(doc, kind, entityKey), def.name),
                    onClose = { step = ChangeStep.FIELD },
                    subtitle = eventName,
                    closeLabel = WorldCopy.BACK,
                )
                SheetScrollBody(Modifier.padding(top = 12.dp)) {
                    when (input) {
                        is ValueInput.Text -> CivilightTextField(
                            value = draft.text,
                            onValueChange = { draft = draft.copy(text = it) },
                            label = def.name,
                            unit = input.unit,
                            singleLine = !input.multiline,
                        )
                        is ValueInput.Number -> CivilightTextField(
                            value = draft.text,
                            onValueChange = { draft = draft.copy(text = it) },
                            label = def.name,
                            unit = input.unit,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                        is ValueInput.Options -> ChipFlow {
                            input.options.forEach { (key, label) ->
                                FilterChip(
                                    label = label,
                                    selected = key in draft.optionKeys,
                                    onClick = {
                                        draft = draft.copy(
                                            optionKeys = when {
                                                !input.multiple -> listOf(key)
                                                key in draft.optionKeys -> draft.optionKeys - key
                                                else -> draft.optionKeys + key
                                            },
                                        )
                                    },
                                )
                            }
                        }
                        is ValueInput.Date -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(WorldCopy.EVENT_YEAR, draft.year, Modifier.weight(1.4f)) { draft = draft.copy(year = it) }
                            if (input.withMonthDay) {
                                NumberField(WorldCopy.EVENT_MONTH, draft.month, Modifier.weight(1f)) { draft = draft.copy(month = it) }
                                NumberField(WorldCopy.EVENT_DAY, draft.day, Modifier.weight(1f)) { draft = draft.copy(day = it) }
                            }
                        }
                        ValueInput.MonthDay -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(WorldCopy.EVENT_MONTH, draft.month, Modifier.weight(1f)) { draft = draft.copy(month = it) }
                            NumberField(WorldCopy.EVENT_DAY, draft.day, Modifier.weight(1f)) { draft = draft.copy(day = it) }
                        }
                        is ValueInput.Link -> {
                            ChipFlow {
                                draft.targetKeys.zip(input.names).forEach { (key, name) ->
                                    RemovableChip(name, onRemove = { draft = draft.copy(targetKeys = draft.targetKeys - key) }, removeLabel = WorldCopy.removeAlias(name))
                                }
                            }
                            TextAction(WorldCopy.pickLabel(input.pick), { step = ChangeStep.TARGET }, Modifier.padding(top = 8.dp), color = colors.acc, style = type.small)
                        }
                    }
                }
                SheetActions {
                    CivilightButton(
                        text = WorldCopy.BTN_PUT,
                        modifier = Modifier.weight(1f),
                        enabled = valid && !store.readOnly,
                        onClick = {
                            scope.launch { if (onAdd(draft)) sheet.close() }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    CivilightTextField(
        value = value,
        onValueChange = onChange,
        label = label,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

private fun itemName(doc: io.github.lenticualrstone.civilight.model.doc.ProjectDoc, kind: EntityKind, key: Key): String = when (kind) {
    EntityKind.CHARACTER -> doc.index.character(key)?.name
    EntityKind.PLACE -> doc.index.place(key)?.name
    EntityKind.TERM -> doc.index.term(key)?.name
    EntityKind.FACTION -> doc.index.faction(key)?.name
    else -> null
}.orEmpty()
