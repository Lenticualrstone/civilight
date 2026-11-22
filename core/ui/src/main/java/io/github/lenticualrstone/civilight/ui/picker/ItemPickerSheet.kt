package io.github.lenticualrstone.civilight.ui.picker

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
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.PickerRow
import io.github.lenticualrstone.civilight.designsystem.component.SearchField
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.model.Key

/** A filter chip above a picker list (`전체 · 주연 · 등불 결사 …`); [key] null = `전체`. */
data class PickerChip(val key: Key?, val label: String)

/**
 * The common picker sheet body: title + `닫기`, search field (`초성 검색`), optional single-choice
 * chips, sections of rows, the deleted-item row on top and the locked-hidden row at the end.
 * Lists are lazy so 100+ items stay fast (SCR-19). Nothing is preselected; [selectedKey] only marks
 * the current value.
 */
@Composable
fun ColumnScope.ItemPickerSheet(
    title: String,
    model: PickerModel,
    query: String,
    onQueryChange: (String) -> Unit,
    onPick: (PickerItem) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    selectedKey: Key? = null,
    chips: List<PickerChip> = emptyList(),
    selectedChip: Key? = null,
    onChip: (Key?) -> Unit = {},
    onUnlockHidden: (() -> Unit)? = null,
    onRestoreDeleted: ((DeletedRow) -> Unit)? = null,
    placeholder: String = PickerCopy.SEARCH_PLACEHOLDER,
) {
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(query)) }
    SheetHeader(title = title, onClose = onClose)
    SearchField(
        value = field,
        onValueChange = {
            field = it
            onQueryChange(it.text)
        },
        modifier = Modifier.padding(top = 12.dp),
        placeholder = placeholder,
    )
    if (chips.isNotEmpty()) {
        ChipFlow(Modifier.padding(top = 10.dp)) {
            for (chip in chips) FilterChip(label = chip.label, selected = chip.key == selectedChip, onClick = { onChip(chip.key) })
        }
    }
    LazyColumn(modifier.weight(1f, fill = false).padding(top = 8.dp)) {
        model.deleted?.let { deleted ->
            item(key = "deleted") {
                ListRow(
                    modifier = civilightItem(),
                    title = "${deletedWord(deleted.kind)}${PickerCopy.SEP}${deleted.name}",
                    titleDotted = true,
                    trailing = onRestoreDeleted?.let { restore -> { TextAction(PickerCopy.RESTORE, { restore(deleted) }) } },
                )
            }
        }
        for ((index, section) in model.sections.withIndex()) {
            if (section.title != null) item(key = "section-$index") { SectionLabel(section.title, civilightItem().padding(top = 12.dp)) }
            // S11: rows fade in and out and move on `standard` when the query or a chip changes the list. The
            // title stays in the key because a term's name and its aliases are rows with the same item key.
            items(section.items, key = { "${it.kind}-${it.key}-${it.title}-$index" }) { item ->
                if (item.avatar) {
                    PickerRow(name = item.title, selected = item.key == selectedKey, onClick = { onPick(item) }, subtitle = item.subtitle, modifier = civilightItem())
                } else {
                    ListRow(
                        title = item.title,
                        subtitle = item.subtitle,
                        onClick = { onPick(item) },
                        selected = item.key == selectedKey,
                        compact = true,
                        modifier = civilightItem().padding(start = (item.depth * 16).dp),
                    )
                }
            }
        }
        if (model.hiddenCount > 0) {
            item(key = "hidden") {
                LockRow(
                    modifier = civilightItem(),
                    title = PickerCopy.hiddenRow(model.hiddenKindWord, model.hiddenCount),
                    actionLabel = PickerCopy.UNLOCK,
                    onAction = { onUnlockHidden?.invoke() },
                )
            }
        }
    }
}

private fun deletedWord(kind: PickKind): String = when (kind) {
    PickKind.CHARACTER -> PickerCopy.DELETED_CHARACTER
    PickKind.TERM -> "지운 용어"
    PickKind.PLACE, PickKind.REGION -> "지운 장소"
    else -> "지운 항목"
}
