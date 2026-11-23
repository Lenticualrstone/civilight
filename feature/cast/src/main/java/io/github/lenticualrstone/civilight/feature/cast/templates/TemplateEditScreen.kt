package io.github.lenticualrstone.civilight.feature.cast.templates

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.ConfirmDialog
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.theme.civilightItem
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTemplates
import io.github.lenticualrstone.civilight.feature.cast.common.SavingTextField
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.feature.cast.settings.AddFieldForm
import io.github.lenticualrstone.civilight.feature.cast.settings.AddFieldModel
import io.github.lenticualrstone.civilight.feature.cast.settings.FIELD_KINDS
import io.github.lenticualrstone.civilight.feature.cast.settings.FieldListBody
import io.github.lenticualrstone.civilight.feature.cast.settings.FieldSettingsModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.FieldTemplate
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.navigation.TemplateEdit
import io.github.lenticualrstone.civilight.navigation.TemplateFieldEdit
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.scaffold.rememberSheetHost
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.security.TaskLabel
import io.github.lenticualrstone.civilight.ui.security.WindowProtection
import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.ui.window.CenterColumn

/** `+ 항목` inside `템플릿 고치기` (CST-59; `어디에` fixed to every item of the kind). Feature-local. */
@Serializable
data class TemplateAddFieldSheet(val templateKey: Key, val kind: EntityKind) : SheetKey

/** Capture blocking while a hidden template is on screen (CST-62, as for hidden projects). */
internal val HiddenTemplateProtection = WindowProtection(secure = true, hideRecentsPreview = true, taskLabel = TaskLabel.APP_NAME)

/**
 * The template the route names, or null once the store has loaded and it is gone, trashed, or
 * hidden while the hidden group is locked (then the screen leaves, also after process death).
 */
@Composable
internal fun rememberRoutedTemplate(deps: FeatureDeps, key: Key, route: NavKey): Pair<TemplatesNow, FieldTemplate?> {
    val now = rememberTemplatesNow(deps)
    val template = remember(now, key) { FieldTemplates.visibleByKey(now.all, key, now.hiddenUnlocked) }
    if (now.loaded && template == null) LaunchedEffect(key) { leave(deps, route) }
    return now to template
}

/**
 * Leaves [route] and anything above it; nothing happens once it is gone, so the explicit leave after
 * `지우기` · `빼기` and the "gone" effect never pop a second screen.
 */
internal fun leave(deps: FeatureDeps, route: NavKey) {
    deps.navigator.popTo(inclusive = true) { it == route }
}

/** Edits a template by running [change] over its field-only document, then writing it back. */
internal fun TemplateWrites.scratch(deps: FeatureDeps, key: Key, change: (ProjectDoc, EditContext) -> ProjectDoc) {
    val ctx = deps.editContext()
    edit(key) { t -> FieldTemplates.fromScratchDoc(change(FieldTemplates.toScratchDoc(t), ctx), t, ctx.now) }
}

/**
 * 템플릿 고치기 (CST-59, S34): `이름` (rename, saved as you type), then the `인물 항목` list drawn by the
 * same composable (tabs, `고정 항목`, `템플릿 항목 · N` with grip and `순서 바꾸기`, `+ 항목`), the
 * `숨긴 템플릿` switch and `지우기`. Every change writes to the template store at once.
 */
@Composable
fun TemplateEditScreen(route: TemplateEdit, deps: FeatureDeps) {
    val writes = remember(deps) { TemplateWrites(deps) }
    val (_, template) = rememberRoutedTemplate(deps, route.templateKey, route)
    template ?: return
    if (template.hidden) ProtectWindow(HiddenTemplateProtection)
    val key = template.key
    val scratch = remember(template) { FieldTemplates.toScratchDoc(template) }
    var kindIndex by rememberSaveable { mutableIntStateOf(0) }
    val kind = FIELD_KINDS[kindIndex]
    val screen = remember(scratch, kind) { FieldSettingsModel.screen(scratch, kind) }
    val sheets = rememberSheetHost()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    // `◇ 잠가도 보임` once a template that was hidden is switched off.
    val openedHidden by rememberSaveable { mutableStateOf(template.hidden) }
    fun write(change: (ProjectDoc, EditContext) -> ProjectDoc) = writes.scratch(deps, key, change)

    CenterColumn {
    CivilightScreen(
        title = template.name,
        smallLine = TemplateCopy.TITLE,
        back = IconAction(TemplateCopy.BACK) { leave(deps, route) },
        sheets = sheets,
        localSheets = { sheet ->
            if (sheet is TemplateAddFieldSheet) {
                {
                    AddFieldForm(sheet, scratch, sheet.kind, null) { draft ->
                        write { d, ctx -> AddFieldModel.add(d, draft, sheet.kind, null, ctx) }
                    }
                }
            } else {
                null
            }
        },
    ) { padding ->
        FieldListBody(
            screen = screen,
            kindIndex = kindIndex,
            onKindIndex = { kindIndex = it },
            sectionTitle = TemplateCopy.fields(screen.rows.size),
            editable = true,
            padding = padding,
            onMove = { k, from, to -> write { d, ctx -> FieldSettingsModel.move(d, kind, k, from, to, ctx) } },
            onForward = { k -> write { d, ctx -> FieldSettingsModel.forward(d, kind, k, ctx) } },
            onBackward = { k -> write { d, ctx -> FieldSettingsModel.backward(d, kind, k, ctx) } },
            onRowClick = { k -> deps.navigator.push(TemplateFieldEdit(key, k)) },
            onAdd = { sheets.open(TemplateAddFieldSheet(key, kind)) },
            header = {
                item(key = "name") {
                    SavingTextField(
                        template.name, "tpl-name-$key",
                        { name -> writes.edit(key) { FieldTemplates.rename(it, name, deps.clock.now()) } },
                        civilightItem().fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                        label = TemplateCopy.NAME,
                    )
                }
            },
            trailing = {
                item(key = "hidden") {
                    SwitchRow(
                        TemplateCopy.HIDDEN, template.hidden,
                        { on -> writes.edit(key) { FieldTemplates.setHidden(it, on, deps.clock.now()) } },
                        civilightItem().padding(top = 12.dp),
                        subtitle = if (openedHidden && !template.hidden) TemplateCopy.VISIBLE_NOTICE else TemplateCopy.HIDDEN_SUB,
                    )
                }
                item(key = "delete") {
                    CivilightButton(TemplateCopy.BTN_DELETE, { confirmDelete = true }, civilightItem().fillMaxWidth().padding(top = 24.dp), variant = ButtonVariant.GHOST)
                }
            },
        )
    }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = template.name,
            confirmLabel = TemplateCopy.BTN_DELETE,
            onConfirm = {
                confirmDelete = false
                val name = template.name
                writes.edit(key) { FieldTemplates.delete(it, deps.clock.now()) }
                leave(deps, route)
                deps.toasts.show(TemplateCopy.deleted(name), actionLabel = TemplateCopy.BTN_UNDO, onAction = {
                    writes.edit(key) { FieldTemplates.restore(it, deps.clock.now()) }
                })
            },
            onDismissRequest = { confirmDelete = false },
            facts = TemplateCopy.deleteLines(template.fields.size).map { it to "" },
            cancelLabel = TemplateCopy.BTN_CANCEL,
        )
    }
}
