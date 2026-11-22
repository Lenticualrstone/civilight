package io.github.lenticualrstone.civilight.feature.world.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.KeyboardTool
import io.github.lenticualrstone.civilight.designsystem.component.KeyboardToolsRow
import io.github.lenticualrstone.civilight.designsystem.component.ListRow
import io.github.lenticualrstone.civilight.designsystem.component.LockRow
import io.github.lenticualrstone.civilight.designsystem.component.SheetActions
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import io.github.lenticualrstone.civilight.ui.picker.HiddenGate
import io.github.lenticualrstone.civilight.ui.picker.LinkPickerSheet
import io.github.lenticualrstone.civilight.ui.picker.PickKind
import io.github.lenticualrstone.civilight.ui.project.EditResult
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.Serializable

/** A card's `⋯` (feature-local). [kind] = `WorldKind.name`: `이름 바꾸기 · 설명 고치기 · (용어) 아님으로 둔 것 · 숨기기 · 합치기 · 지우기`. */
@Serializable
data class ItemMenuSheet(val kind: String, val key: Key) : SheetKey

/** `이름 바꾸기` (WLD-11). */
@Serializable
data class RenameSheet(val kind: String, val key: Key) : SheetKey

/** `설명 고치기` (fixed field `설명`, WLD-06). */
@Serializable
data class DescriptionSheet(val kind: String, val key: Key) : SheetKey

/** The sheets every world card shares; null for other keys. */
fun itemSheet(
    key: SheetKey,
    doc: ProjectDoc?,
    vm: WorldViewModel,
    readOnly: Boolean,
    gate: HiddenGate,
): (@Composable SheetScope.() -> Unit)? {
    if (doc == null) return null
    return when (key) {
        is ItemMenuSheet -> { { ItemMenuContent(WorldKind.valueOf(key.kind), key.key, doc, vm, readOnly) } }
        is DeleteSheet -> { { DeleteSheetContent(key, vm, readOnly) } }
        is MergePickSheet -> { { MergePickContent(key, doc, gate) } }
        is MergeSheet -> { { MergeSheetContent(key, doc, vm, readOnly) } }
        is RenameSheet -> { { RenameContent(WorldKind.valueOf(key.kind), key.key, doc, vm, readOnly) } }
        is DescriptionSheet -> { { DescriptionContent(WorldKind.valueOf(key.kind), key.key, doc, vm, readOnly, gate) } }
        else -> null
    }
}

@Composable
private fun SheetScope.ItemMenuContent(kind: WorldKind, key: Key, doc: ProjectDoc, vm: WorldViewModel, readOnly: Boolean) {
    val name = ItemEdits.nameAndText(doc, kind, key)?.first.orEmpty()
    val sheet = this
    val delete = rememberDeleteAction(vm, kind, key)
    val hidden = ItemLifecycle.isHidden(doc, kind, key)
    val roSub = if (readOnly) WorldCopy.READ_ONLY else null
    SheetHeader(title = name, onClose = ::close)
    SheetScrollBody {
        ListRow(
            title = WorldCopy.RENAME,
            subtitle = if (readOnly) WorldCopy.READ_ONLY else null,
            onClick = if (readOnly) null else ({
                sheet.close()
                sheet.open(RenameSheet(kind.name, key))
            }),
            compact = true,
        )
        ListRow(
            title = WorldCopy.EDIT_DESCRIPTION,
            subtitle = if (readOnly) WorldCopy.READ_ONLY else null,
            onClick = if (readOnly) null else ({
                sheet.close()
                sheet.open(DescriptionSheet(kind.name, key))
            }),
            compact = true,
        )
        if (kind == WorldKind.TERM) {
            // Read-only projects may still look at the list; `되돌리기` there is disabled.
            ListRow(
                title = WorldCopy.MENU_REJECTIONS,
                onClick = { sheet.close(); sheet.open(io.github.lenticualrstone.civilight.feature.world.term.RejectionsSheet(key)) },
                compact = true,
            )
        }
        ListRow(
            title = if (hidden) WorldCopy.MENU_UNHIDE else WorldCopy.MENU_HIDE,
            subtitle = roSub,
            onClick = if (readOnly) null else ({ sheet.close(); vm.setHidden(kind, key, !hidden) }),
            compact = true,
        )
        ListRow(
            title = WorldCopy.MENU_MERGE,
            subtitle = roSub,
            onClick = if (readOnly) null else ({ sheet.close(); sheet.open(MergePickSheet(kind.name, key)) }),
            compact = true,
        )
        ListRow(
            title = WorldCopy.MENU_DELETE,
            subtitle = roSub,
            onClick = if (readOnly) null else ({ delete(sheet) }),
            compact = true,
        )
    }
}

@Composable
private fun SheetScope.RenameContent(kind: WorldKind, key: Key, doc: ProjectDoc, vm: WorldViewModel, readOnly: Boolean) {
    val current = ItemEdits.nameAndText(doc, kind, key)?.first.orEmpty()
    var text by rememberSaveable(key) { mutableStateOf(current) }
    val scope = rememberCoroutineScope()
    val sheet = this
    SheetHeader(title = WorldCopy.RENAME, onClose = ::close)
    SheetScrollBody {
        CivilightTextField(
            value = text,
            onValueChange = { text = it },
            label = WorldCopy.NAME,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            enabled = !readOnly,
        )
    }
    SheetActions {
        CivilightButton(
            text = if (readOnly) WorldCopy.READ_ONLY else WorldCopy.BTN_RENAME,
            onClick = { scope.launch { if (vm.rename(kind, key, text) == EditResult.APPLIED) sheet.close() } },
            modifier = Modifier.weight(1f),
            enabled = !readOnly && text.isNotBlank() && text.trim() != current,
        )
    }
}

@Composable
private fun SheetScope.DescriptionContent(kind: WorldKind, key: Key, doc: ProjectDoc, vm: WorldViewModel, readOnly: Boolean, gate: HiddenGate) {
    val description = ItemEdits.nameAndText(doc, kind, key)?.second.orEmpty()
    val current = RichTextEdit.textOf(description)
    // The draft holds the text with its runs, so `@` links inserted here survive later typing (WLD-17).
    var draft by rememberSaveable(key, stateSaver = DescriptionDraft.Saver) { mutableStateOf(description) }
    var field by rememberSaveable(key, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(current, TextRange(current.length))) }
    var picking by rememberSaveable(key) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheet = this
    val locked = CardText.hasLockedLinks(doc, description, gate.open)
    if (locked) {
        // Links to hidden items would show their text in the editor: open them first (rule 19).
        SheetHeader(title = WorldCopy.EDIT_DESCRIPTION, onClose = ::close)
        LockRow(title = WorldCopy.HIDDEN_ITEM, actionLabel = WorldCopy.UNLOCK, onAction = gate.unlock, modifier = Modifier.padding(top = 12.dp))
        return
    }
    if (picking) {
        LinkPickerSheet(
            doc = doc,
            gate = gate,
            title = WorldCopy.TODO_COPY_WORLD_LINK_PICKER_TITLE,
            onPick = { pickKind, target, text ->
                val linkKind = DescriptionDraft.linkKind(pickKind)
                if (linkKind != null) {
                    val (next, caret) = DescriptionDraft.insert(draft, field.text, field.selection.start, linkKind, target, text, Keys::newKey)
                    draft = next
                    field = TextFieldValue(RichTextEdit.textOf(next), TextRange(caret))
                }
                picking = false
            },
            onClose = { picking = false },
        )
        return
    }
    SheetHeader(title = WorldCopy.EDIT_DESCRIPTION, onClose = ::close)
    SheetScrollBody {
        CivilightTextField(
            value = field,
            onValueChange = {
                // Each change goes into the draft as it happens, so links between two edits stay.
                if (it.text != field.text) draft = RichTextEdit.apply(draft, it.text, Keys::newKey)
                field = it
            },
            label = WorldCopy.DESCRIPTION,
            singleLine = false,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            enabled = !readOnly,
        )
        if (!readOnly) {
            KeyboardToolsRow(Modifier.padding(top = 8.dp)) {
                KeyboardTool(onClick = { picking = true }, text = WorldCopy.LINK_TOOL, contentDescription = WorldCopy.TODO_COPY_WORLD_LINK_PICKER_TITLE)
            }
        }
    }
    SheetActions {
        val next = RichTextEdit.apply(draft, field.text, Keys::newKey)
        CivilightButton(
            text = if (readOnly) WorldCopy.READ_ONLY else WorldCopy.BTN_EDIT,
            onClick = { scope.launch { if (vm.setDescriptionDoc(kind, key, next) == EditResult.APPLIED) sheet.close() } },
            modifier = Modifier.weight(1f),
            enabled = !readOnly && next != description,
        )
    }
}

/** The description editor's draft: text with runs, kept across process death as JSON. */
object DescriptionDraft {
    private val serializer = ListSerializer(DocBlock.serializer())

    val Saver: Saver<RichDoc, String> = Saver(
        save = { CivilightJson.json.encodeToString(serializer, it) },
        restore = { CivilightJson.json.decodeFromString(serializer, it) },
    )

    /** The link kind of a `@` pick: 인물 · 용어 · 장소 (the picker offers no other kind). */
    fun linkKind(kind: PickKind): LinkTargetKind? = when (kind) {
        PickKind.CHARACTER -> LinkTargetKind.CHARACTER
        PickKind.TERM -> LinkTargetKind.TERM
        PickKind.PLACE -> LinkTargetKind.PLACE
        PickKind.FACTION -> LinkTargetKind.FACTION
        else -> null
    }

    /**
     * Takes the typed [text] into [draft], then inserts a new link run (new `linkKey`) to [kind]
     * [target] showing [shown] at [caret]. Returns the new draft and the caret after the link.
     */
    fun insert(draft: RichDoc, text: String, caret: Int, kind: LinkTargetKind, target: Key, shown: String, newKey: () -> Key): Pair<RichDoc, Int> {
        val typed = RichTextEdit.apply(draft, text, newKey)
        val link = TextRun.Link(linkKey = newKey(), target = LinkTarget(kind.open(), target), text = shown)
        return RichTextEdit.insertLink(typed, caret, link, newKey)
    }
}
