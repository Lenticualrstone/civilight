package io.github.lenticualrstone.civilight.feature.cast.relation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.AddChip
import io.github.lenticualrstone.civilight.designsystem.component.Avatar
import io.github.lenticualrstone.civilight.designsystem.component.AvatarSize
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.FieldTableRow
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.domain.cast.relation.ChangeSection
import io.github.lenticualrstone.civilight.feature.cast.common.CastEditorViewModel
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationDrafts
import io.github.lenticualrstone.civilight.domain.cast.relation.SheetSlot
import io.github.lenticualrstone.civilight.domain.cast.relation.SlotDraft
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.field.rememberCastEditor
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.feature.cast.create.CastCharacterPicker
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import io.github.lenticualrstone.civilight.ui.project.currentProjectStore

/**
 * The `관계` segment of the script's `◆ 변화 기록` sheet (`ChangeCreate`, CST-30, S17): the pair
 * (tap a person to change them), `바꾸는 것` chips (several at once: one `◆` each), per chosen slot
 * `값 · <before> →` with the kind chips and `+ 새 종류` (or the 호칭 text), `시작 시점 | 이 블록 · 1-7
 * #043`, `반영`, and `기록`. Values are read just before the block; records anchor at the block.
 *
 * Not reachable yet: the script area hosts this sheet (SCR-41) and no shared sheet key carries a
 * block. It takes plain arguments so the shared key requested in feature-cast.md can draw it as is.
 */
@Composable
fun SheetScope.RelationChangeContent(projectKey: Key, blockKey: Key, viewerKey: Key, partnerKey: Key?) {
    val host = this
    val store = currentProjectStore
    val doc by store.doc.collectAsState()
    val editor = rememberCastEditor(projectKey)
    val gate = rememberHiddenGate(projectKey)
    var viewer by rememberSaveable(blockKey) { mutableStateOf(viewerKey) }
    var partner by rememberSaveable(blockKey) { mutableStateOf(partnerKey) }
    var picking by rememberSaveable(blockKey) { mutableStateOf<String?>(if (partnerKey == null) PARTNER else null) }
    val chosen = remember(blockKey) { mutableStateMapOf<SheetSlot, SlotDraft>() }
    var newKindFor by rememberSaveable(blockKey) { mutableStateOf<String?>(null) }
    var newKind by rememberSaveable(blockKey) { mutableStateOf("") }

    val other = partner
    val view = if (other == null) null else remember(doc, blockKey, viewer, other, chosen.keys.toSet()) { RelationDrafts.changeView(doc, blockKey, viewer, other, chosen.keys) }
    if (other != null && view == null) {
        LaunchedEffect(Unit) { close() }
        return
    }
    // Every slot's section, so a section folds in and out as its chip is toggled (P2).
    val everySection = if (other == null || view == null) null else remember(doc, blockKey, viewer, other, view.slotChips) {
        RelationDrafts.changeView(doc, blockKey, viewer, other, view.slotChips.map { it.first }.toSet())
    }
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    // The pair picker and the form swap in place (P3).
    ContentSwap(picking, label = "changeStep") { which ->
        Column(Modifier.fillMaxWidth()) {
            if (which != null) {
                CastCharacterPicker(projectKey, doc, gate, CastUiCopy.PICK_PARTNER, onPick = { picked ->
                    if (which == PARTNER && picked != viewer) partner = picked
                    if (which == VIEWER && picked != partner) viewer = picked
                    chosen.clear()
                    picking = null
                }, onClose = { picking = null })
            } else if (other != null && view != null && everySection != null) {
                SheetHeader(view.title, onClose = host::close, subtitle = view.sub, closeLabel = view.cancelLabel)
                SheetScrollBody {
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        PersonChip(view.viewerName, io.github.lenticualrstone.civilight.ui.image.rememberCharacterAvatarPainter(doc, viewer)) { picking = VIEWER }
                        CivilightText(CastCopy.SEP.trim(), type.small, color = colors.ink3)
                        PersonChip(view.partnerName, io.github.lenticualrstone.civilight.ui.image.rememberCharacterAvatarPainter(doc, other)) { picking = PARTNER }
                    }
                    SectionLabel(RelationDrafts.CHANGE_ITEMS, Modifier.padding(top = 16.dp, bottom = 6.dp))
                    ChipFlow {
                        for ((slot, label) in view.slotChips) {
                            FilterChip(label, slot in chosen, { if (slot in chosen) chosen.remove(slot) else chosen[slot] = SlotDraft() })
                        }
                    }
                    for (section in everySection.sections) {
                        key(section.slot) {
                            FoldReveal(section.slot in chosen) {
                                SlotSection(section, view.addKindLabel, chosen, newKindFor, newKind, { newKindFor = it }, { newKind = it }, editor, other)
                            }
                        }
                    }
                    FieldTableRow(view.startRow.first, view.startRow.second, Modifier.padding(top = 14.dp))
                    FieldTableRow(view.reflectRow.first, view.reflectRow.second)
                    val canRecord = !store.readOnly && chosen.isNotEmpty() && chosen.values.none { it == SlotDraft() }
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        CivilightButton(view.primaryLabel, {
                            val drafts = chosen.toMap()
                            val a = viewer
                            editor.act(view.primaryLabel) { d, ctx -> RelationDrafts.recordAtBlock(d, blockKey, a, other, drafts, ctx).first }
                            host.close()
                        }, Modifier.weight(1f), enabled = canRecord)
                    }
                }
            }
        }
    }
}

/** One chosen slot of the change sheet: `값 · <before> →`, the kind chips with `+ 새 종류` (or the 호칭 text) and a description. */
@Composable
private fun SlotSection(
    section: ChangeSection,
    addKindLabel: String,
    chosen: androidx.compose.runtime.snapshots.SnapshotStateMap<SheetSlot, SlotDraft>,
    newKindFor: String?,
    newKind: String,
    setNewKindFor: (String?) -> Unit,
    setNewKind: (String) -> Unit,
    editor: CastEditorViewModel,
    other: Key,
) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val draft = chosen[section.slot] ?: SlotDraft()
    Column(Modifier.fillMaxWidth()) {
        CivilightText(section.valueLabel, type.small, Modifier.padding(top = 14.dp, bottom = 6.dp), color = colors.ink3)
        if (section.slot == SheetSlot.ADDRESS) {
            CivilightTextField(draft.address, { chosen[section.slot] = draft.copy(address = it) }, Modifier.fillMaxWidth(), label = RelationDrafts.ADDRESS)
            return@Column
        }
        ChipFlow {
            for (chip in section.chips) {
                val picked = draft.picks(chip)
                FilterChip(chip.label, picked, {
                    chosen[section.slot] = if (picked) draft.copy(kindKey = null, partnerHoldsFirst = null) else draft.copy(kindKey = chip.kindKey, partnerHoldsFirst = chip.partnerHoldsFirst)
                })
            }
            if (newKindFor != section.slot.name) AddChip(addKindLabel, { setNewKindFor(section.slot.name); setNewKind("") })
        }
        FoldReveal(newKindFor == section.slot.name) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                CivilightTextField(newKind, setNewKind, Modifier.weight(1f), label = CastUiCopy.NAME)
                TextAction(CastUiCopy.BTN_INSERT, {
                    val name = newKind
                    val slot = section.slot
                    var made: SlotDraft? = null
                    editor.act(CastUiCopy.BTN_INSERT, then = {
                        made?.let { chosen[slot] = it }
                        setNewKindFor(null)
                    }) { d, ctx ->
                        RelationDrafts.addKind(d, slot, name, ctx)?.let { (next, chip) ->
                            made = (chosen[slot] ?: SlotDraft()).copy(kindKey = chip.kindKey, partnerHoldsFirst = chip.partnerHoldsFirst)
                            next
                        } ?: d
                    }
                }, Modifier.padding(start = 8.dp, top = 18.dp))
            }
        }
        CivilightTextField(draft.description, { chosen[section.slot] = draft.copy(description = it) }, Modifier.fillMaxWidth().padding(top = 8.dp), label = CastUiCopy.DESCRIPTION_OPTIONAL)
    }
}

private const val VIEWER = "viewer"
private const val PARTNER = "partner"

/** `리 리엔`: avatar and name; tapping changes that person. */
@Composable
private fun PersonChip(name: String, image: androidx.compose.ui.graphics.painter.Painter? = null, onClick: () -> Unit) {
    Row(
        Modifier.pressable(onClick = onClick, scaleOnPress = false, onClickLabel = name).heightIn(min = 44.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name, size = AvatarSize.MapChip, image = image)
        CivilightText(name, CivilightTheme.type.label, color = CivilightTheme.colors.ink)
    }
}
