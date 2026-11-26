package io.github.lenticualrstone.civilight.feature.cast.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.AddChip
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxRow
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightIconButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.ConfirmDialog
import io.github.lenticualrstone.civilight.designsystem.component.FieldTableRow
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings
import io.github.lenticualrstone.civilight.domain.cast.field.FieldText
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTypes
import io.github.lenticualrstone.civilight.domain.cast.label
import io.github.lenticualrstone.civilight.domain.trash.TrashCopy
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.CastViewModel
import io.github.lenticualrstone.civilight.feature.cast.common.CastWrites
import io.github.lenticualrstone.civilight.feature.cast.common.SavingTextField
import io.github.lenticualrstone.civilight.feature.cast.common.Step
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.feature.cast.trash.rememberItemTrash
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.FieldDefEdit
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.entryViewModel
import io.github.lenticualrstone.civilight.ui.project.ProjectLoad
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.window.CenterColumn

/** The field edit screen's view model; switches and option edits are one undo step each. */
class FieldDefEditViewModel(
    projectKey: Key,
    sessions: io.github.lenticualrstone.civilight.ui.project.ProjectSessions,
    contextOf: () -> io.github.lenticualrstone.civilight.domain.cast.EditContext,
) : CastViewModel(projectKey, sessions, contextOf)

/** A confirm the screen is asking for (saved with the screen). */
private sealed interface Pending {
    data class RemoveOption(val optionKey: Key) : Pending
    data class ChangeType(val to: FieldType, val target: FieldLinkTargetKind?) : Pending
    data object Delete : Pending
}

private fun Pending.encode(): String = when (this) {
    is Pending.RemoveOption -> "option:$optionKey"
    is Pending.ChangeType -> "type:${to.name}:${target?.name.orEmpty()}"
    Pending.Delete -> "delete"
}

private fun decodePending(s: String?): Pending? = when {
    s == null -> null
    s == "delete" -> Pending.Delete
    s.startsWith("option:") -> Pending.RemoveOption(s.removePrefix("option:"))
    s.startsWith("type:") -> s.removePrefix("type:").split(':').let { (t, k) ->
        Pending.ChangeType(FieldType.valueOf(t), k.takeIf { it.isNotEmpty() }?.let(FieldLinkTargetKind::valueOf))
    }
    else -> null
}

/**
 * 항목 정의 바꾸기 (`FieldDefEdit`, CST-23; S13 detail, not drawn): 이름, 내보내기 이름, 단위
 * (`환산하지 않음`), 보기 (rename in place, `빼기` lists the holders first), 직접 쓰기, 종류 (only
 * the CST-23 pairs, with `◇ 바꾸지 못한 값 N` before anything changes), 보이는 곳, 비밀 항목,
 * 숨기기, `모든 인물에 넣기` for an `이 인물만` field, and 지우기 (`값이 있는 인물 12명`). Texts
 * save as you type; every other control writes at once (one undo step each).
 */
@Composable
fun FieldDefEditScreen(route: FieldDefEdit, deps: FeatureDeps) {
    val vm = entryViewModel { FieldDefEditViewModel(route.projectKey, deps.projects, deps::editContext) }
    val load by vm.state.collectAsState()
    val ready = load as? ProjectLoad.Ready ?: return
    val doc = ready.doc
    val view = remember(doc, route.fieldDefKey) { FieldDefEditModel.view(doc, route.fieldDefKey) }
    if (view == null) {
        LaunchedEffect(route.fieldDefKey) { deps.navigator.pop() }
        return
    }
    val key = view.key
    val editable = !ready.readOnly
    val writes = remember(route.projectKey) { CastWrites(deps, route.projectKey) }
    val trash = rememberItemTrash(route.projectKey)
    var pendingText by rememberSaveable { mutableStateOf<String?>(null) }
    val pending = decodePending(pendingText)
    var linkTarget by rememberSaveable { mutableStateOf(FieldLinkTargetKind.FACTION) }

    fun typed(label: String, mergeKey: String, change: (ProjectDoc, io.github.lenticualrstone.civilight.domain.cast.EditContext) -> ProjectDoc) {
        writes.launch(label, { d, ctx -> Step(change(d, ctx.edit), Unit) }, mergeKey = "$mergeKey-$key")
    }

    CenterColumn {
        CivilightScreen(
            title = view.name,
            smallLine = FieldSettings.TITLE,
            back = IconAction(CastUiCopy.BACK) { deps.navigator.pop() },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                CivilightText(view.sub, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
                SavingTextField(
                    view.name, "name-$key", { name -> typed(CastUiCopy.NAME, "fieldName") { d, ctx -> FieldDefEditModel.rename(d, key, name, ctx) } },
                    Modifier.fillMaxWidth().padding(top = 14.dp), label = CastUiCopy.NAME, enabled = editable,
                )
                SavingTextField(
                    view.exportName, "export-$key", { name -> typed(CastUiCopy.EXPORT_NAME, "fieldExport") { d, ctx -> FieldDefEditModel.setExportName(d, key, name, ctx) } },
                    Modifier.fillMaxWidth().padding(top = 10.dp), label = CastUiCopy.EXPORT_NAME, placeholder = view.name, enabled = editable,
                )
                view.unit?.let { unit ->
                    SavingTextField(
                        unit, "unit-$key", { u -> typed(CastUiCopy.UNIT, "fieldUnit") { d, ctx -> FieldDefEditModel.setUnit(d, key, u, ctx) } },
                        Modifier.fillMaxWidth().padding(top = 10.dp), label = CastUiCopy.UNIT, hint = FieldDefinitions.UNIT_NOTE, enabled = editable,
                    )
                }
                if (view.type != null && FieldTypes.hasOptions(view.type)) {
                    OptionsEditor(view, editable, onRename = { option, label ->
                        typed(CastUiCopy.OPTIONS, "option-$option") { d, ctx -> FieldDefEditModel.renameOption(d, key, option, label, ctx) }
                    }, onAdd = { label ->
                        vm.act(CastUiCopy.OPTIONS) { d, ctx -> FieldDefEditModel.addOption(d, key, label, ctx) }
                    }, onRemove = { option ->
                        if (view.options.first { it.key == option }.holders.isEmpty()) {
                            vm.act(CastUiCopy.BTN_REMOVE) { d, ctx -> FieldDefEditModel.removeOption(d, key, option, ctx) }
                        } else {
                            pendingText = Pending.RemoveOption(option).encode()
                        }
                    })
                }
                view.allowFreeText?.let { allow ->
                    SwitchRow(CastUiCopy.FREE_TEXT, allow, { on -> vm.act(CastUiCopy.FREE_TEXT) { d, ctx -> FieldDefinitions.setAllowFreeText(d, key, on, ctx) } }, enabled = editable)
                }
                TypeSection(view, editable, linkTarget, { linkTarget = it }) { to ->
                    pendingText = Pending.ChangeType(to, linkTarget.takeIf { FieldDefEditModel.needsTarget(to) }).encode()
                }
                SectionLabel(CastUiCopy.PLACES, Modifier.padding(top = 20.dp, bottom = 4.dp))
                if (view.secret) {
                    CivilightText(CastUiCopy.SECRET_SUB, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
                } else {
                    CheckboxRow(CastUiCopy.PLACE_HEADER, view.inHeader, { on ->
                        vm.act(CastUiCopy.PLACES) { d, ctx -> FieldDefinitions.setPlaces(d, key, on, view.inList, view.inFilter, ctx) }
                    }, enabled = editable)
                    CheckboxRow(CastUiCopy.PLACE_LIST, view.inList, { on ->
                        vm.act(CastUiCopy.PLACES) { d, ctx -> FieldDefinitions.setPlaces(d, key, view.inHeader, on, view.inFilter, ctx) }
                    }, enabled = editable)
                    CheckboxRow(CastUiCopy.PLACE_FILTER, view.inFilter, { on ->
                        vm.act(CastUiCopy.PLACES) { d, ctx -> FieldDefinitions.setPlaces(d, key, view.inHeader, view.inList, on, ctx) }
                    }, enabled = editable && view.filterEnabled)
                }
                if (view.kind == EntityKind.CHARACTER) {
                    SwitchRow(CastUiCopy.SECRET, view.secret, { on -> vm.act(CastUiCopy.SECRET) { d, ctx -> FieldDefinitions.setSecret(d, key, on, ctx) } }, subtitle = CastUiCopy.SECRET_SUB, enabled = editable)
                }
                SwitchRow(CastUiCopy.HIDE, view.hidden, { on -> vm.act(CastUiCopy.HIDE) { d, ctx -> FieldDefinitions.setHidden(d, key, on, ctx) } }, enabled = editable)
                if (editable && view.characterOnly) {
                    TextAction(CastUiCopy.BTN_PROMOTE, { vm.act(CastUiCopy.BTN_PROMOTE) { d, ctx -> FieldDefinitions.promoteToAll(d, key, ctx) } }, color = CivilightTheme.colors.ink2, style = CivilightTheme.type.label)
                }
                if (editable) {
                    CivilightButton(CastUiCopy.BTN_DELETE, { pendingText = Pending.Delete.encode() }, Modifier.fillMaxWidth().padding(top = 24.dp), variant = ButtonVariant.GHOST)
                }
            }
        }
    }

    when (pending) {
        null -> Unit
        is Pending.RemoveOption -> {
            val option = view.options.firstOrNull { it.key == pending.optionKey }
            if (option == null) {
                pendingText = null
            } else {
                ConfirmDialog(
                    title = option.label,
                    confirmLabel = CastUiCopy.BTN_REMOVE,
                    onConfirm = {
                        pendingText = null
                        vm.act(CastUiCopy.BTN_REMOVE) { d, ctx -> FieldDefEditModel.removeOption(d, key, pending.optionKey, ctx) }
                    },
                    onDismissRequest = { pendingText = null },
                    facts = listOf(option.holders to FieldText.OPTION_REMOVED),
                    cancelLabel = TrashCopy.CANCEL,
                )
            }
        }
        is Pending.ChangeType -> {
            val preview = remember(doc, pending) { FieldDefEditModel.preview(doc, key, pending.to, pending.target, deps.editContext()) }
            ConfirmDialog(
                title = CastUiCopy.change(view.typeLabel, FieldTypes.label(pending.to)),
                confirmLabel = CastUiCopy.BTN_CHANGE,
                onConfirm = {
                    pendingText = null
                    vm.act(CastUiCopy.BTN_CHANGE) { d, ctx -> FieldDefEditModel.convert(d, key, pending.to, pending.target, ctx) }
                },
                onDismissRequest = { pendingText = null },
                facts = listOfNotNull(preview?.notice?.let { CastUiCopy.TYPE to it }),
                cancelLabel = TrashCopy.CANCEL,
                confirmEnabled = preview != null,
            )
        }
        // Goes to 휴지통 (undoable): `‘X’ 지우기`, primary `휴지통으로` like the tag confirm (DS-25, 0.5 review).
        Pending.Delete -> ConfirmDialog(
            title = CastUiCopy.deleteTitle(view.name),
            confirmLabel = TrashCopy.TO_TRASH,
            onConfirm = {
                pendingText = null
                trash.deleteFieldDef(key) { deps.navigator.pop() }
            },
            onDismissRequest = { pendingText = null },
            facts = view.deleteLines.map { it to "" } + (TrashCopy.SHEET_FOOTNOTE to ""),
            cancelLabel = TrashCopy.CANCEL,
        )
    }
}

/** `보기`: each option renames in place; `빼기` asks first when someone holds it; `+ 보기` adds one. */
@Composable
internal fun OptionsEditor(view: FieldDefView, editable: Boolean, onRename: (Key, String) -> Unit, onAdd: (String) -> Unit, onRemove: (Key) -> Unit) {
    var adding by rememberSaveable(view.key) { mutableStateOf<String?>(null) }
    SectionLabel(CastUiCopy.OPTIONS, Modifier.padding(top = 20.dp, bottom = 6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (option in view.options) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SavingTextField(
                    option.label, "opt-${option.key}", { onRename(option.key, it) }, Modifier.weight(1f),
                    hint = option.holders.ifEmpty { null }, enabled = editable,
                )
                if (editable) CivilightIconButton(IconId.X, CastUiCopy.BTN_REMOVE, { onRemove(option.key) })
            }
        }
        val typed = adding
        if (editable && typed == null) AddChip(CastUiCopy.OPTIONS, { adding = "" })
        if (typed != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CivilightTextField(typed, { adding = it }, Modifier.weight(1f), label = CastUiCopy.OPTIONS)
                TextAction(CastUiCopy.BTN_INSERT, {
                    onAdd(typed)
                    adding = null
                }, Modifier.padding(start = 8.dp, top = 18.dp))
            }
        }
    }
}

/** `종류`: the type now and the types it may change to (CST-23 table); `연결` asks for its target first. */
@Composable
internal fun TypeSection(view: FieldDefView, editable: Boolean, target: FieldLinkTargetKind, onTarget: (FieldLinkTargetKind) -> Unit, onChange: (FieldType) -> Unit) {
    SectionLabel(CastUiCopy.TYPE, Modifier.padding(top = 20.dp, bottom = 4.dp))
    FieldTableRow(CastUiCopy.TYPE, view.typeLabel)
    if (!editable || view.offered.isEmpty()) return
    ChipFlow(Modifier.padding(top = 8.dp)) {
        for (to in view.offered) FilterChip(FieldTypes.label(to), false, { onChange(to) })
    }
    if (FieldType.LINK in view.offered) {
        CivilightText(CastUiCopy.LINK_TARGET, CivilightTheme.type.label, Modifier.padding(top = 10.dp), color = CivilightTheme.colors.ink2)
        ChipFlow(Modifier.padding(top = 6.dp)) {
            for (t in FieldTypes.LINK_TARGETS) FilterChip(t.label, t == target, { onTarget(t) })
        }
    }
}
