package io.github.lenticualrstone.civilight.feature.cast.relation

import io.github.lenticualrstone.civilight.designsystem.component.CreateCardCopy
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardMode
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardState
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.feature.cast.create.CastCreate
import io.github.lenticualrstone.civilight.feature.cast.create.CastCreateCard
import io.github.lenticualrstone.civilight.feature.cast.create.CreateCardCodec
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import io.github.lenticualrstone.civilight.ui.project.EditResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.AddChip
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.FieldTableRow
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldRevealValue
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationDrafts
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationKinds
import io.github.lenticualrstone.civilight.domain.cast.relation.SheetSlot
import io.github.lenticualrstone.civilight.domain.cast.relation.SlotDraft
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.castLens
import io.github.lenticualrstone.civilight.feature.cast.field.EditMode
import io.github.lenticualrstone.civilight.feature.cast.field.SaveButtons
import io.github.lenticualrstone.civilight.feature.cast.field.TwoButtonHead
import io.github.lenticualrstone.civilight.feature.cast.field.label
import io.github.lenticualrstone.civilight.feature.cast.field.rememberCastEditor
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.RelationEditSheet
import io.github.lenticualrstone.civilight.navigation.SheetKey
import kotlinx.serialization.Serializable
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.feature.cast.create.CastCharacterPicker
import io.github.lenticualrstone.civilight.ui.picker.UnitPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore

/**
 * The shared `관계 만들기 · 고치기` sheet (CST-29, S16). With only one end ([RelationEditSheet.endAKey],
 * `+ 관계`) it starts with the partner picker; picking a partner the pair already has a relation with
 * opens that relation (one per pair). [RelationEditSheet.relationKey] alone opens that relation from
 * its end A. Then: `사이 · <A>의 마음 · <B>의 마음 · 호칭` chips, the kind chips (`+ 새 종류`) with an
 * optional description or the 호칭 text, `시작 시점` for a value that starts now, and the save
 * button(s).
 */
@Composable
fun SheetScope.RelationEditContent(sheet: RelationEditSheet) {
    val doc by currentProjectStore.doc.collectAsState()
    val byKey = sheet.relationKey?.let(doc.index::relation)
    val viewer = sheet.endAKey ?: byKey?.endA?.key
    val partner = sheet.endBKey ?: byKey?.let { r -> if (r.endA.key == viewer) r.endB.key else r.endA.key }
    RelationEditBody(sheet.projectKey, viewer, partner, SheetSlot.BETWEEN, saveKey = sheet)
}

/**
 * One slot of a pair opened directly (feature-local): the 호칭 chips of `말투` open [SheetSlot.ADDRESS]
 * with the same one-or-two buttons as every timed value (CST-31). [partnerKey] null starts with the
 * partner picker (`+ 호칭`).
 */
@Serializable
data class RelationSlotSheet(val projectKey: Key, val viewerKey: Key, val partnerKey: Key?, val slot: SheetSlot) : SheetKey

@Composable
fun SheetScope.RelationSlotContent(sheet: RelationSlotSheet) {
    RelationEditBody(sheet.projectKey, sheet.viewerKey, sheet.partnerKey, sheet.slot, saveKey = sheet)
}

/** The steps of the relation sheet: partner picker, start picker, the form. They swap in place (P3). */
private enum class RelationStage { PICK_PARTNER, PICK_START, FORM }

/** The relation sheet body for [viewerKey] (and [partnerKey], or the picker first) opened on [initialSlot]. */
@Composable
private fun SheetScope.RelationEditBody(projectKey: Key, viewerKey: Key?, partnerKey: Key?, initialSlot: SheetSlot, saveKey: Any) {
    val host = this
    val deps = LocalFeatureDeps.current
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val editor = rememberCastEditor(projectKey)
    val gate = rememberHiddenGate(projectKey)
    val lens = doc.castLens
    val viewer = viewerKey
    if (viewer == null || doc.index.character(viewer) == null) {
        LaunchedEffect(Unit) { close() }
        return
    }
    val sheet = saveKey
    var partner by rememberSaveable(sheet) { mutableStateOf(partnerKey) }
    val chosen = partner
    var slotName by rememberSaveable(sheet, chosen) { mutableStateOf(initialSlot.name) }
    val slot = SheetSlot.valueOf(slotName)
    var start by rememberSaveable(sheet, chosen) { mutableStateOf(RelationSheetModel.defaultStart(doc, lens)) }
    var pickingStart by rememberSaveable(sheet, chosen) { mutableStateOf(false) }
    val view = if (chosen == null) null else remember(doc, viewer, chosen, slot, start, lens) { RelationSheetModel.view(doc, viewer, chosen, slot, start, lens) }
    if (chosen != null && view == null) {
        LaunchedEffect(Unit) { close() }
        return
    }
    // The draft lives above the step swap, so a trip to the start picker keeps what was typed.
    val initial = if (chosen == null) null else remember(chosen, slot) { RelationSheetModel.initialDraft(doc, viewer, chosen, slot, lens) }
    var kindKey by rememberSaveable(sheet, chosen, slot) { mutableStateOf(initial?.kindKey) }
    var partnerFirst by rememberSaveable(sheet, chosen, slot) { mutableStateOf(initial?.partnerHoldsFirst) }
    var description by rememberSaveable(sheet, chosen, slot) { mutableStateOf(initial?.description.orEmpty()) }
    var address by rememberSaveable(sheet, chosen, slot) { mutableStateOf(initial?.address.orEmpty()) }
    var kindCard by rememberSaveable(saveKey, chosen, slot, stateSaver = KindCardSaver) { mutableStateOf<CreateCardState?>(null) }
    val draft = SlotDraft(kindKey, partnerFirst, description, address)
    val editable = !store.readOnly
    val stage = when {
        chosen == null -> RelationStage.PICK_PARTNER
        pickingStart -> RelationStage.PICK_START
        else -> RelationStage.FORM
    }

    ContentSwap(stage, label = "relationStep") { shown ->
        Column(Modifier.fillMaxWidth()) {
            when (shown) {
                RelationStage.PICK_PARTNER ->
                    // U-15.3 (CST-66, §8.3 Q45): the partner picker has the `＋ 새 인물` row; a created character is picked.
                    CastCharacterPicker(projectKey, doc, gate, CastUiCopy.PICK_PARTNER, onPick = { picked -> if (picked != viewer) partner = picked }, onClose = host::close)
                RelationStage.PICK_START ->
                    UnitPickerSheet(doc, gate, RelationDrafts.START, onPick = { start = it; pickingStart = false }, onClose = { pickingStart = false }, selectedKey = start)
                RelationStage.FORM -> if (chosen != null && view != null) {
                    SheetHeader(view.title, onClose = host::close, closeLabel = CastUiCopy.BTN_CLOSE)
                    SheetScrollBody {
                        SectionLabel(CastUiCopy.WHAT_TO_WRITE, Modifier.padding(top = 14.dp, bottom = 6.dp))
                        ChipFlow {
                            for ((s, label) in view.slots) FilterChip(label, s == slot, { slotName = s.name })
                        }
                        val save = view.save
                        val two = (save as? RelationSave.Edit)?.mode as? EditMode.TwoButtons
                        ContentSwap(two != null && editable, label = "relationHead") { twoButtons ->
                            Column {
                                if (twoButtons && two != null) {
                                    TwoButtonHead(two) { deps.lens(store).backToLatest() }
                                } else {
                                    FieldTableRow(CastUiCopy.CURRENT_VALUE, view.current, Modifier.padding(top = 12.dp))
                                }
                            }
                        }
                        if (!editable) return@SheetScrollBody
                        ContentSwap(slot == SheetSlot.ADDRESS, label = "relationSlot") { isAddress ->
                            Column {
                                if (isAddress) {
                                    CivilightTextField(address, { address = it }, Modifier.fillMaxWidth().padding(top = 12.dp), label = RelationDrafts.ADDRESS)
                                } else {
                                    ChipFlow(Modifier.padding(top = 12.dp)) {
                                        for (chip in view.chips) {
                                            val picked = draft.picks(chip)
                                            FilterChip(chip.label, picked, {
                                                if (picked) {
                                                    kindKey = null
                                                    partnerFirst = null
                                                } else {
                                                    kindKey = chip.kindKey
                                                    partnerFirst = chip.partnerHoldsFirst
                                                }
                                            })
                                        }
                                        // CST-76: `+ 새 종류` opens the relation-kind create card (picker mode); the category is the slot's.
                                        AddChip(RelationKinds.ADD, { kindCard = CreateCardState.open(CreateKind.RELATION_KIND, CreateCardMode.PICKER) })
                                    }
                                    kindCard?.let { card ->
                                        var kindBusy by remember { mutableStateOf(false) }
                                        CastCreateCard(
                                            state = card,
                                            onChange = { kindCard = it },
                                            onCancel = { kindCard = null },
                                            onCreate = {
                                                val made = card
                                                kindBusy = true
                                                var picked: Pair<Key, Boolean?>? = null
                                                // The card closes only once the write applied; otherwise the typed values stay.
                                                editor.act(CreateCardCopy.CREATE, then = { result ->
                                                    kindBusy = false
                                                    if (result == EditResult.APPLIED) {
                                                        kindCard = null
                                                        picked?.let { (k, first) ->
                                                            kindKey = k
                                                            partnerFirst = first
                                                        }
                                                    }
                                                }) { d, ctx ->
                                                    RelationDrafts.addKindFromCard(d, slot, made.name, CastCreate.pairName(made), ctx)?.let { (next, chip) ->
                                                        picked = chip.kindKey to chip.partnerHoldsFirst
                                                        next
                                                    } ?: d
                                                }
                                            },
                                            canCreate = !kindBusy && CastCreate.canCreate(card),
                                            fields = CastCreate.kindFields(between = slot == SheetSlot.BETWEEN),
                                        )
                                    }
                                    CivilightTextField(description, { description = it }, Modifier.fillMaxWidth().padding(top = 12.dp), label = CastUiCopy.DESCRIPTION_OPTIONAL)
                                }
                            }
                        }
                        val canSave = RelationSheetModel.canSave(doc, viewer, chosen, slot, draft, lens)
                        ContentSwap(save is RelationSave.FromStart, label = "relationSave") { fromStart ->
                            Column {
                                if (fromStart && save is RelationSave.FromStart) {
                                    ListRow(
                                        RelationDrafts.START,
                                        subtitle = view.startLabel ?: CastUiCopy.EMPTY_VALUE,
                                        onClick = { pickingStart = true },
                                        compact = true,
                                        modifier = Modifier.padding(top = 12.dp),
                                    )
                                    Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                        CivilightButton(save.label, {
                                            val saved = draft
                                            val from = start
                                            editor.act(save.label) { d, ctx -> RelationSheetModel.save(d, viewer, chosen, slot, saved, null, from, lens, ctx) }
                                            host.close()
                                        }, Modifier.weight(1f), enabled = canSave)
                                    }
                                } else if (save is RelationSave.Edit) {
                                    SaveButtons(save.mode, canSave) { choice ->
                                        val saved = draft
                                        editor.act(choice.label(save.mode)) { d, ctx -> RelationSheetModel.save(d, viewer, chosen, slot, saved, choice, null, lens, ctx) }
                                        host.close()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val KindCardSaver: androidx.compose.runtime.saveable.Saver<CreateCardState?, String> = androidx.compose.runtime.saveable.Saver(
    save = { it?.let(CreateCardCodec::encode) ?: "" },
    restore = { CreateCardCodec.decode(it) },
)
