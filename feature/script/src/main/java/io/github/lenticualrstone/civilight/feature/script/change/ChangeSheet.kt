package io.github.lenticualrstone.civilight.feature.script.change

import io.github.lenticualrstone.civilight.feature.script.ui.AppearOnJoin
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.AddChip
import io.github.lenticualrstone.civilight.designsystem.component.Avatar
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.KeyValueLine
import io.github.lenticualrstone.civilight.designsystem.component.SegmentedControl
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.relation.SheetSlot
import io.github.lenticualrstone.civilight.domain.cast.relation.SlotDraft
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.CharacterPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.TermPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate
import kotlinx.serialization.Serializable

/** `◆ 변화 기록` of [blockKey] (SCR-41): from the keyboard toolbar `◆` and the `시간` tab's `◆ 변화 기록`. */
@Serializable
data class ChangeSheetKey(val projectKey: Key, val blockKey: Key) : SheetKey

/** A picker stacked over the change sheet: a character for [role], or a term when [role] is null. */
@Serializable
data class ChangePickSheetKey(val projectKey: Key, val blockKey: Key, val role: PickRole? = null) : SheetKey

/** What the change sheet calls. */
class ChangeActions(
    val edit: ((ChangeDraft) -> ChangeDraft) -> Unit,
    val record: (ChangeDraft) -> Unit,
    val addKind: (SheetSlot, String) -> Unit,
)

/**
 * The sheet (board `ChangeCreate`): `변화 기록` with `취소`, `#043 · 1-7 씬 3`, the segments, the
 * segment's body, `시작 시점 · 이 블록 · 1-7 #043`, and the primary `기록`.
 */
@Composable
fun SheetScope.ChangeSheet(key: ChangeSheetKey, doc: ProjectDoc, draft: ChangeDraft?, actions: ChangeActions) {
    // SCR-58: hidden characters and terms are masked while the hidden items are locked.
    val unlocked = io.github.lenticualrstone.civilight.ui.picker.rememberHiddenGate(key.projectKey).open
    val model = draft?.let { ChangeModel.of(doc, it, unlocked) }
    SheetHeader(title = model?.title ?: "", onClose = ::close, subtitle = model?.sub, closeLabel = model?.cancel ?: "")
    if (model == null || draft == null) return
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    SheetScrollBody(Modifier.padding(top = 12.dp, bottom = 8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val segments = ChangeSegment.entries
            SegmentedControl(
                options = segments.map { it.label },
                selectedIndex = segments.indexOf(model.segment),
                onSelect = { i -> actions.edit { it.copy(segment = segments[i]) } },
            )
            val open: (SheetKey) -> Unit = { this@ChangeSheet.open(it) }
            // Motion audit P3: a segment's body crossfades with the segment, the sheet follows its height.
            ContentSwap(model.segment, label = "changeSegment") { segment ->
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    when (segment) {
                        ChangeSegment.RELATION -> model.relation?.let { RelationBody(key, doc, it, draft, actions, open) }
                        ChangeSegment.FIELD -> model.field?.let { FieldBody(key, doc, it, draft, actions, open) }
                        ChangeSegment.TERM -> model.term?.let { TermBody(key, it, open) }
                    }
                }
            }
            Column {
                KeyValueLine(model.startRow.first, model.startRow.second)
                model.relation?.reflectRow?.let { KeyValueLine(it.first, it.second) }
            }
        }
    }
    CivilightButton(
        text = model.primary,
        onClick = {
            this@ChangeSheet.close()
            actions.record(draft)
        },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
        variant = ButtonVariant.PRIMARY,
        enabled = model.canRecord,
    )
}

@Composable
private fun RelationBody(key: ChangeSheetKey, doc: ProjectDoc, model: RelationSegment, draft: ChangeDraft, actions: ChangeActions, open: (SheetKey) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    // `리 리엔 · 하 하르트`: each chip opens the character picker for that side.
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        PersonChip(
            model.viewer,
            set = draft.viewerKey != null,
            image = io.github.lenticualrstone.civilight.ui.image.rememberCharacterAvatarPainter(doc, draft.viewerKey),
        ) { open(ChangePickSheetKey(key.projectKey, key.blockKey, PickRole.VIEWER)) }
        CivilightText("·", type.small, color = colors.ink3)
        PersonChip(
            model.partner,
            set = draft.partnerKey != null,
            image = io.github.lenticualrstone.civilight.ui.image.rememberCharacterAvatarPainter(doc, draft.partnerKey),
        ) { open(ChangePickSheetKey(key.projectKey, key.blockKey, PickRole.PARTNER)) }
    }
    // The items to change appear once both people are set.
    if (draft.viewerKey != null && draft.partnerKey != null) AppearOnJoin {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CivilightText(ChangeCopy.CHANGE_ITEMS, type.small, color = colors.ink3)
            ChipFlow {
                model.slotChips.forEach { chip ->
                    FilterChip(chip.label, chip.selected, onClick = {
                        actions.edit { d -> d.copy(slots = if (chip.selected) d.slots - chip.slot else d.slots + chip.slot) }
                    })
                }
            }
            model.sections.forEach { section -> SlotSectionView(section, actions) }
        }
    }
}

@Composable
private fun SlotSectionView(section: SlotSection, actions: ChangeActions) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    fun set(draft: SlotDraft) = actions.edit { d -> d.copy(slotDrafts = d.slotDrafts + (section.slot to draft)) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CivilightText(section.valueLabel, type.small, color = colors.ink3)
        if (section.slot == SheetSlot.ADDRESS) {
            CivilightTextField(value = section.draft.address, onValueChange = { set(section.draft.copy(address = it)) }, label = ChangeCopy.ADDRESS_FIELD)
            return@Column
        }
        var adding by rememberSaveable(section.slot) { mutableStateOf(false) }
        ChipFlow {
            section.chips.forEach { chip ->
                val picked = section.draft.picks(chip)
                FilterChip(chip.label, picked, onClick = {
                    set(if (picked) section.draft.copy(kindKey = null, partnerHoldsFirst = null) else section.draft.copy(kindKey = chip.kindKey, partnerHoldsFirst = chip.partnerHoldsFirst))
                })
            }
            section.addKind?.let { label -> AddChip(label.removePrefix("+ "), onClick = { adding = true }) }
        }
        if (adding) AppearOnJoin {
            var name by rememberSaveable(section.slot) { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CivilightTextField(value = name, onValueChange = { name = it }, modifier = Modifier.weight(1f), label = ChangeCopy.NEW_KIND_FIELD)
                TextAction(ChangeCopy.ADD, {
                    actions.addKind(section.slot, name)
                    name = ""
                    adding = false
                }, color = colors.acc)
            }
        }
    }
}

@Composable
private fun FieldBody(key: ChangeSheetKey, doc: ProjectDoc, model: FieldSegment, draft: ChangeDraft, actions: ChangeActions, open: (SheetKey) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(verticalAlignment = Alignment.CenterVertically) {
        PersonChip(
            model.owner,
            set = draft.ownerKey != null,
            image = io.github.lenticualrstone.civilight.ui.image.rememberCharacterAvatarPainter(doc, draft.ownerKey),
        ) { open(ChangePickSheetKey(key.projectKey, key.blockKey, PickRole.FIELD_OWNER)) }
    }
    if (model.fields.isEmpty()) return
    ChipFlow {
        model.fields.forEach { field ->
            FilterChip(field.label, field.selected, onClick = {
                actions.edit { d -> d.copy(fieldKey = field.key, fieldText = "", fieldOption = null) }
            })
        }
    }
    val label = model.valueLabel ?: return
    CivilightText(label, type.small, color = colors.ink3)
    model.options?.let { options ->
        ChipFlow {
            options.forEach { (optionKey, text) ->
                FilterChip(text, draft.fieldOption == optionKey, onClick = { actions.edit { d -> d.copy(fieldOption = optionKey) } })
            }
        }
    }
    if (model.textInput) AppearOnJoin {
        CivilightTextField(
            value = draft.fieldText,
            onValueChange = { text -> actions.edit { d -> d.copy(fieldText = text) } },
            keyboardOptions = if (model.number) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        )
    }
}

@Composable
private fun TermBody(key: ChangeSheetKey, model: TermSegment, open: (SheetKey) -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(verticalAlignment = Alignment.CenterVertically) {
        PersonChip(model.term, set = model.term != ChangeCopy.PICK_TERM, avatar = false) { open(ChangePickSheetKey(key.projectKey, key.blockKey, role = null)) }
    }
    model.movePreview?.let { CivilightText(it, type.small, color = colors.warn) }
}

/** A character (or term) chip: avatar + name, outlined; unset reads `인물 고르기`. 44 dp target. */
@Composable
private fun PersonChip(
    label: String,
    set: Boolean,
    avatar: Boolean = true,
    image: androidx.compose.ui.graphics.painter.Painter? = null,
    onClick: () -> Unit,
) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .pressable(PressAction.Click(onClick), role = Role.Button, hitTarget = HitTarget.CHIP, shape = shape, scaleOnPress = false)
            .heightIn(min = 36.dp)
            .insetOutline(if (set) colors.edge else colors.acc, 1.dp, shape)
            .padding(start = if (set && avatar) 4.dp else 12.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (set && avatar) Avatar(label, size = 28.dp, image = image)
        CivilightText(label, CivilightTheme.type.chip, color = if (set) colors.ink else colors.acc, maxLines = 1)
    }
}

/** The picker over the change sheet: it fills the draft and closes back to it. */
@Composable
fun SheetScope.ChangePickSheet(key: ChangePickSheetKey, doc: ProjectDoc, sceneCast: List<Key>, edit: ((ChangeDraft) -> ChangeDraft) -> Unit) {
    val gate = rememberHiddenGate(key.projectKey)
    val role = key.role
    if (role == null) {
        TermPickerSheet(
            doc = doc, gate = gate, title = ChangeCopy.PICK_TERM,
            onPick = { term, _ ->
                close()
                edit { it.copy(termKey = term) }
            },
            onClose = ::close,
        )
        return
    }
    CharacterPickerSheet(
        doc = doc, gate = gate, title = ChangeCopy.PICK_CHARACTER,
        onPick = { picked ->
            close()
            edit { d ->
                when (role) {
                    PickRole.VIEWER -> d.copy(viewerKey = picked, slotDrafts = emptyMap())
                    PickRole.PARTNER -> d.copy(partnerKey = picked, slotDrafts = emptyMap())
                    PickRole.FIELD_OWNER -> d.copy(ownerKey = picked, fieldKey = null, fieldText = "", fieldOption = null)
                }
            }
        },
        onClose = ::close,
        sceneCast = sceneCast,
        recent = doc.app.view.script.recentSpeakers,
        creator = io.github.lenticualrstone.civilight.feature.script.ui.scriptPickerCreator(io.github.lenticualrstone.civilight.designsystem.component.CreateKind.CHARACTER),
    )
}
