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

/** `관련` (term fixed field): tick terms, places and factions. */
@Serializable
data class RelatedSheet(val termKey: Key) : SheetKey

/** `별칭` (WLD-16): the list with per-alias remove and the field with `+`. */
@Serializable
data class AliasSheet(val termKey: Key) : SheetKey

/** `⋯ › 아님으로 둔 것` (WLD-22): the term's `아님` records with `되돌리기`. */
@Serializable
data class RejectionsSheet(val termKey: Key) : SheetKey

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
