package io.github.lenticualrstone.civilight.feature.world.term

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.SearchField
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.RelatedOptions
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.PickerCopy
import kotlinx.serialization.Serializable
import androidx.compose.foundation.layout.Row
import io.github.lenticualrstone.civilight.designsystem.theme.FoldReveal
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.RemovableChip
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import io.github.lenticualrstone.civilight.designsystem.foundation.PressAction
import io.github.lenticualrstone.civilight.designsystem.foundation.pressable
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.domain.actions.TagCreate
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.term.TermTags
import io.github.lenticualrstone.civilight.feature.world.common.CardImages
import io.github.lenticualrstone.civilight.feature.world.common.DescriptionSheet
import io.github.lenticualrstone.civilight.feature.world.common.ItemLifecycle
import io.github.lenticualrstone.civilight.feature.world.common.MergePickSheet
import io.github.lenticualrstone.civilight.feature.world.common.RenameSheet
import io.github.lenticualrstone.civilight.feature.world.common.rememberDeleteAction
import io.github.lenticualrstone.civilight.model.cast.TagScope
import io.github.lenticualrstone.civilight.ui.tags.TagSheet
import io.github.lenticualrstone.civilight.ui.tags.TagSheetCreate
import io.github.lenticualrstone.civilight.ui.tags.TagSheetRow

/** `관련` (term fixed field): tick terms, places and factions. */
@Serializable
data class RelatedSheet(val termKey: Key) : SheetKey

/** `별칭` (WLD-16): the list with per-alias remove and the field with `+`. */
@Serializable
data class AliasSheet(val termKey: Key) : SheetKey

/** `⋯ › 아님으로 둔 것` (WLD-22): the term's `아님` records with `되돌리기`. */
@Serializable
data class RejectionsSheet(val termKey: Key) : SheetKey

/** (2026-11-26) The immersive card's `⋯`: the shared rows, `지우기` last under its own divider (as feature/story `MenuSheets`). */
@Serializable
data class TermMenuSheet(val termKey: Key) : SheetKey

/** (2026-11-26) `+ 태그`: the shared [TagSheet] for a term. */
@Serializable
data class TermTagSheet(val termKey: Key) : SheetKey

/** The term card's own sheets besides the reveal sheets ([revealSheet]); null for other keys. */
fun termSheet(
    key: SheetKey,
    doc: ProjectDoc?,
    readOnly: Boolean,
    gate: HiddenGate,
    vm: TermCardViewModel,
): (@Composable SheetScope.() -> Unit)? {
    if (doc == null) return null
    return when (key) {
        is RelatedSheet -> { { RelatedContent(key.termKey, doc, readOnly, gate, vm::toggleRelated) } }
        is AliasSheet -> { { AliasContent(key.termKey, doc, readOnly, vm) } }
        is RejectionsSheet -> { { RejectionsContent(key.termKey, doc, readOnly, gate, vm) } }
        is TermMenuSheet -> { { TermMenuContent(key.termKey, doc, readOnly, vm) } }
        is TermTagSheet -> { { TermTagContent(key.termKey, doc, readOnly, vm) } }
        else -> null
    }
}

@Composable
private fun SheetScope.AliasContent(termKey: Key, doc: ProjectDoc, readOnly: Boolean, vm: TermCardViewModel) {
    val term = doc.index.term(termKey) ?: return
    var field by rememberSaveable(termKey) { mutableStateOf("") }
    var error by rememberSaveable(termKey) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    SheetHeader(title = WorldCopy.ROW_ALIASES, onClose = ::close, subtitle = term.name)
    SheetScrollBody {
        FoldReveal(term.aliases.isNotEmpty()) {
            ChipFlow(Modifier.padding(top = 12.dp)) {
                term.aliases.forEach { alias ->
                    if (readOnly) {
                        FilterChip(label = alias, selected = false, onClick = {}, enabled = false)
                    } else {
                        RemovableChip(
                            label = alias,
                            onRemove = { scope.launch { error = vm.removeAlias(alias) } },
                            removeLabel = WorldCopy.removeAlias(alias),
                        )
                    }
                }
            }
        }
        if (!readOnly) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CivilightTextField(
                    value = field,
                    onValueChange = { field = it; error = null },
                    label = WorldCopy.ROW_ALIASES,
                    error = error,
                    modifier = Modifier.weight(1f),
                )
                CivilightIconButton(
                    icon = IconId.Plus,
                    contentDescription = WorldCopy.ROW_ALIASES,
                    onClick = {
                        scope.launch {
                            val refused = vm.addAlias(field)
                            error = refused
                            if (refused == null) field = ""
                        }
                    },
                    enabled = field.isNotBlank(),
                )
            }
        } else {
            CivilightText(WorldCopy.READ_ONLY, CivilightTheme.type.small, Modifier.padding(top = 12.dp), color = CivilightTheme.colors.ink3)
        }
    }
}

@Composable
private fun SheetScope.RejectionsContent(termKey: Key, doc: ProjectDoc, readOnly: Boolean, gate: HiddenGate, vm: TermCardViewModel) {
    val rows = remember(doc, termKey, gate.open) { RejectionRows.build(doc, termKey, gate.open) }
    SheetHeader(title = WorldCopy.MENU_REJECTIONS, onClose = ::close, subtitle = doc.index.term(termKey)?.name)
    if (rows.isEmpty()) {
        CivilightText(WorldCopy.NO_REJECTIONS, CivilightTheme.type.small, Modifier.padding(top = 16.dp), color = CivilightTheme.colors.ink3)
        return
    }
    LazyColumn(Modifier.weight(1f, fill = false).padding(top = 8.dp)) {
        items(rows, key = { it.rejectionKey }) { row ->
            ListRow(
                title = row.text,
                subtitle = row.location,
                trailing = {
                    TextAction(
                        WorldCopy.BTN_UNDO,
                        onClick = { vm.restoreRejection(row.rejectionKey) },
                        color = if (readOnly) CivilightTheme.colors.ink3 else CivilightTheme.colors.acc,
                    )
                },
            )
        }
    }
}

@Composable
private fun SheetScope.RelatedContent(
    termKey: Key,
    doc: ProjectDoc,
    readOnly: Boolean,
    gate: HiddenGate,
    toggle: (io.github.lenticualrstone.civilight.model.world.ItemRef) -> Unit,
) {
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val ui = remember(doc, field.text, gate.open) { RelatedOptions.build(doc, termKey, field.text, gate.open) }
    SheetHeader(title = WorldCopy.ROW_RELATED, onClose = ::close)
    SearchField(
        value = field,
        onValueChange = { field = it },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        placeholder = PickerCopy.SEARCH_PLACEHOLDER,
    )
    LazyColumn(Modifier.weight(1f, fill = false).padding(top = 8.dp)) {
        ui.sections.forEachIndexed { i, section ->
            item(key = "sec:$i") { SectionLabel(section.title, Modifier.padding(top = 12.dp)) }
            items(section.options, key = { "${it.ref.kind.raw}:${it.ref.key}" }) { option ->
                CheckboxRow(
                    label = option.title,
                    checked = option.checked,
                    onCheckedChange = { toggle(option.ref) },
                    enabled = !readOnly,
                )
            }
        }
        if (ui.hiddenCount > 0) {
            item(key = "hidden") {
                LockRow(
                    title = PickerCopy.hiddenRow(PickerCopy.KIND_ITEM, ui.hiddenCount),
                    actionLabel = WorldCopy.UNLOCK,
                    onAction = gate.unlock,
                )
            }
        }
    }
}

@Composable
private fun SheetScope.TermTagContent(termKey: Key, doc: ProjectDoc, readOnly: Boolean, vm: TermCardViewModel) {
    val held = doc.index.term(termKey)?.tagKeys?.toSet().orEmpty()
    val rows = TermTags.choices(doc).map { TagSheetRow(it.key, it.name, it.key in held, it.scope.known == TagScope.ALL) }
    TagSheet(
        kindWord = WorldCopy.TAB_TERMS,
        rows = rows,
        readOnly = readOnly,
        onToggle = vm::toggleTag,
        onCreate = { name, everywhere ->
            when (val r = vm.createTag(name, null, everywhere)) {
                is TagCreate.Created -> TagSheetCreate.Created
                is TagCreate.NameTaken -> TagSheetCreate.Taken(r.existing.key, r.existing.name, r.fits)
                TagCreate.Blank -> TagSheetCreate.Blank
            }
        },
        onUseExisting = vm::attachTag,
        // `모든 항목으로 넓히기` is one-way and not on the board: not offered from the term card (caller's call).
        onWiden = null,
    )
}

/**
 * The term's `⋯` (board rows as the shared world menu): `이미지 고르기 · 이름 바꾸기 · 설명 고치기 · (아님으로 둔 것) ·
 * 숨기기 · 합치기`, then `지우기` in `warn` with the trash icon under a divider (feature/story `MenuSheets`).
 */
@Composable
private fun SheetScope.TermMenuContent(termKey: Key, doc: ProjectDoc, readOnly: Boolean, vm: TermCardViewModel) {
    val kind = WorldKind.TERM
    val name = doc.index.term(termKey)?.name.orEmpty()
    val sheet = this
    val delete = rememberDeleteAction(vm, kind, termKey)
    val hidden = ItemLifecycle.isHidden(doc, kind, termKey)
    val sub = if (readOnly) WorldCopy.READ_ONLY else null
    SheetHeader(title = name, onClose = ::close)
    SheetScrollBody(Modifier.padding(top = 8.dp)) {
        MenuRow(WorldCopy.PICK_IMAGE, sub, enabled = !readOnly) { sheet.close(); sheet.open(CardImages.pickSheet(vm.projectKey, kind, termKey)) }
        MenuRow(WorldCopy.RENAME, sub, enabled = !readOnly) { sheet.close(); sheet.open(RenameSheet(kind.name, termKey)) }
        MenuRow(WorldCopy.EDIT_DESCRIPTION, sub, enabled = !readOnly) { sheet.close(); sheet.open(DescriptionSheet(kind.name, termKey)) }
        // (0.2.0, WLD-73) Only while `용어 후보` is on; read-only projects may still look at the list.
        if (doc.project.termCandidates) {
            MenuRow(WorldCopy.MENU_REJECTIONS, null, enabled = true) { sheet.close(); sheet.open(RejectionsSheet(termKey)) }
        }
        MenuRow(if (hidden) WorldCopy.MENU_UNHIDE else WorldCopy.MENU_HIDE, sub, enabled = !readOnly) { sheet.close(); vm.setHidden(kind, termKey, !hidden) }
        MenuRow(WorldCopy.MENU_MERGE, sub, enabled = !readOnly) { sheet.close(); sheet.open(MergePickSheet(kind.name, termKey)) }
        MenuRow(WorldCopy.MENU_DELETE, sub, enabled = !readOnly, destructive = true) { delete(sheet) }
    }
}

/** A `⋯` row: min 52, `line` top, medium label; the destructive one sits under a 12 dp gap closed by a line. */
@Composable
private fun MenuRow(label: String, sub: String?, enabled: Boolean, destructive: Boolean = false, onClick: () -> Unit) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    if (destructive) {
        // The shared ⋯ `지우기` row (designsystem `MenuDeleteRow`).
        io.github.lenticualrstone.civilight.designsystem.component.MenuDeleteRow(label, onClick, subtitle = sub, enabled = enabled)
        return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .topHairline(colors.line)
            .pressable(PressAction.Click(onClick), enabled = enabled, role = Role.Button, scaleOnPress = false)
            .alpha(if (enabled) 1f else 0.38f)
            .heightIn(min = 52.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (destructive) CivilightIcon(IconId.Trash, contentDescription = null, tint = colors.warn)
        Column {
            CivilightText(label, type.itemTitleCompact.merge(TextStyle(fontWeight = FontWeight.Medium)), color = if (destructive) colors.warn else colors.ink, maxLines = 1)
            sub?.let { CivilightText(it, type.small, color = colors.ink3, maxLines = 1) }
        }
    }
}
