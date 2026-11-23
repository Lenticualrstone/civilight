package io.github.lenticualrstone.civilight.feature.cast.map

import io.github.lenticualrstone.civilight.designsystem.component.CreateCardCopy
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardMode
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardState
import io.github.lenticualrstone.civilight.designsystem.component.CreateKind
import io.github.lenticualrstone.civilight.feature.cast.create.CastCreate
import io.github.lenticualrstone.civilight.feature.cast.create.CastCreateCard
import io.github.lenticualrstone.civilight.feature.cast.create.CreateCardCodec

import androidx.compose.foundation.layout.Arrangement
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
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.domain.cast.map.MapSlotFilter
import io.github.lenticualrstone.civilight.domain.cast.relation.CanvasRelations
import io.github.lenticualrstone.civilight.domain.cast.relation.LineSlot
import io.github.lenticualrstone.civilight.domain.cast.relation.PairLines
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationDrafts
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationKinds
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.castLens
import io.github.lenticualrstone.civilight.feature.cast.field.rememberCastEditor
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.navigation.RelationKindSettings
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.labels.UnitLabels
import io.github.lenticualrstone.civilight.ui.picker.UnitPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore
import kotlinx.serialization.Serializable

/** The map's filter (`사이 · 마음 · 둘 다`, kind chips). Feature-local. */
@Serializable
data class MapFilterSheet(val projectKey: Key) : SheetKey

/** The line-label sheet of a line drawn on the map between two characters (CST-34). Feature-local. */
@Serializable
data class MapLineSheet(val projectKey: Key, val fromKey: Key, val toKey: Key) : SheetKey

/** `고른 관계` as a bottom sheet on the cover screen (`CastMapC`). Feature-local. */
@Serializable
data class SelectedRelationSheet(val projectKey: Key, val relationKey: Key) : SheetKey

/** `사이 · 마음 · 둘 다` (default `둘 다`) and the kind chips (several at once; none = every kind). */
@Composable
fun SheetScope.MapFilterContent(ui: MapUi, state: RelationMapState, onUi: (MapUi) -> Unit) {
    SheetHeader(CastUiCopy.PLACE_FILTER, onClose = ::close, closeLabel = CastUiCopy.BTN_CLOSE)
    SheetScrollBody {
        val slots = MapSlotFilter.entries
        SegmentedControl(slots.map { it.label }, slots.indexOf(ui.slots), { onUi(ui.copy(slots = slots[it])) }, Modifier.padding(top = 12.dp))
        SectionLabel(CastUiCopy.TYPE, Modifier.padding(top = 16.dp, bottom = 6.dp))
        ChipFlow {
            for (chip in state.kinds) FilterChip(chip.label, chip.selected, { onUi(RelationMapModel.toggleKind(ui, chip.kindKey)) })
        }
    }
}

/**
 * `선 라벨` for a map line (`CanvasCLabel` rows without the label text, which a map line has no place
 * to keep): `무엇으로 적을지` (`<drawn-from>의 마음` preselected), the kind chips of that slot with
 * `+ 새 종류`, `시작 시점`, then `그냥 선으로 두기` (records nothing) · `관계로 만들기`.
 */
@Composable
fun SheetScope.MapLineContent(sheet: MapLineSheet) {
    val host = this
    val deps = LocalFeatureDeps.current
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val editor = rememberCastEditor(sheet.projectKey)
    val gate = rememberHiddenGate(sheet.projectKey)
    val lens = doc.castLens
    val view = remember(doc, sheet) { PairLines.sheet(doc, sheet.fromKey, sheet.toKey, lens) }
    if (view == null || store.readOnly) {
        LaunchedEffect(Unit) { close() }
        return
    }
    var slotName by rememberSaveable(sheet) { mutableStateOf((view.preselected ?: LineSlot.FROM_MIND).name) }
    val slot = LineSlot.valueOf(slotName)
    var kindKey by rememberSaveable(sheet, slot) { mutableStateOf<Key?>(null) }
    var partnerFirst by rememberSaveable(sheet, slot) { mutableStateOf<Boolean?>(null) }
    var kindCard by rememberSaveable(sheet, slot, stateSaver = LineKindCardSaver) { mutableStateOf<CreateCardState?>(null) }
    var start by rememberSaveable(sheet) { mutableStateOf(view.startUnitKey ?: CanvasRelations.startUnit(doc, lens)) }
    var pickingStart by rememberSaveable(sheet) { mutableStateOf(false) }
    if (pickingStart) {
        UnitPickerSheet(doc, gate, RelationDrafts.START, onPick = { start = it; pickingStart = false }, onClose = { pickingStart = false }, selectedKey = start)
        return
    }
    val chips = remember(doc, slot) { PairLines.kindChips(doc, slot) }
    val chosen = chips.firstOrNull { it.kindKey == kindKey && it.partnerHoldsFirst == partnerFirst }
    SheetHeader(view.title, onClose = ::close, subtitle = view.sub, closeLabel = CastUiCopy.BTN_CLOSE)
    SheetScrollBody {
        SectionLabel(CastUiCopy.WHAT_TO_WRITE, Modifier.padding(top = 14.dp, bottom = 6.dp))
        ChipFlow {
            view.slotChips.forEachIndexed { i, label ->
                val s = LineSlot.entries[i]
                FilterChip(label, s == slot, { slotName = s.name })
            }
        }
        SectionLabel(CastUiCopy.TYPE, Modifier.padding(top = 14.dp, bottom = 6.dp))
        ChipFlow {
            for (chip in chips) {
                val picked = chip == chosen
                FilterChip(chip.label, picked, {
                    kindKey = if (picked) null else chip.kindKey
                    partnerFirst = if (picked) null else chip.partnerHoldsFirst
                })
            }
            // CST-76: `+ 새 종류` opens the relation-kind create card; the slot gives the category, the new kind is picked.
            AddChip(RelationKinds.ADD.removePrefix("+ "), { kindCard = CreateCardState.open(CreateKind.RELATION_KIND, CreateCardMode.PICKER) })
        }
        var kindBusy by remember { mutableStateOf(false) }
        kindCard?.let { card ->
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
                        RelationDrafts.addKindFromCard(d, PairLines.sheetSlot(slot), made.name, CastCreate.pairName(made), ctx)?.let { (next, chip) ->
                            picked = chip.kindKey to chip.partnerHoldsFirst
                            next
                        } ?: d
                    }
                },
                canCreate = !kindBusy && CastCreate.canCreate(card),
                fields = CastCreate.kindFields(between = slot == LineSlot.BETWEEN),
            )
        }
        ListRow(
            RelationDrafts.START,
            subtitle = start?.let { UnitLabels(doc).code(it) } ?: view.startPoint,
            onClick = { pickingStart = true },
            compact = true,
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CivilightButton(view.keepLabel, host::close, Modifier.weight(1f), variant = ButtonVariant.GHOST)
            view.makeLabel?.let { make ->
                CivilightButton(make, {
                    val pick = chosen ?: return@CivilightButton
                    val from = start ?: return@CivilightButton
                    editor.act(make) { d, ctx -> PairLines.makeRelation(d, sheet.fromKey, sheet.toKey, slot, pick, from, ctx) }
                    host.close()
                }, Modifier.weight(1f), enabled = chosen != null && start != null)
            }
        }
    }
}

private val LineKindCardSaver: androidx.compose.runtime.saveable.Saver<CreateCardState?, String> = androidx.compose.runtime.saveable.Saver(
    save = { it?.let(CreateCardCodec::encode) ?: "" },
    restore = { CreateCardCodec.decode(it) },
)
