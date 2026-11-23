package io.github.lenticualrstone.civilight.feature.cast.templates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxRow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.ConfirmDialog
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SwitchRow
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldTemplates
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.SavingTextField
import io.github.lenticualrstone.civilight.feature.cast.settings.FieldDefEditModel
import io.github.lenticualrstone.civilight.feature.cast.settings.OptionsEditor
import io.github.lenticualrstone.civilight.feature.cast.settings.TypeSection
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.TemplateFieldEdit
import io.github.lenticualrstone.civilight.ui.feature.FeatureDeps
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.ui.window.CenterColumn

/**
 * 템플릿 항목 고치기 (CST-59, S35): the field screen of CST-23 over the template's field-only
 * document, minus what only values need. Any of the 9 types (no `◇` preview; `연결` asks for its
 * target, `계산` for its source or `없음`); option `빼기` at once; no `숨기기` and no `모든 인물에 넣기`;
 * the ghost button is `빼기` (`이 템플릿에서만 · 프로젝트 그대로`).
 */
@Composable
fun TemplateFieldEditScreen(route: TemplateFieldEdit, deps: FeatureDeps) {
    val writes = remember(deps) { TemplateWrites(deps) }
    val (now, template) = rememberRoutedTemplate(deps, route.templateKey, route)
    template ?: return
    if (template.hidden) ProtectWindow(HiddenTemplateProtection)
    val scratch = remember(template) { FieldTemplates.toScratchDoc(template) }
    val base = remember(scratch, route.fieldKey) { FieldDefEditModel.view(scratch, route.fieldKey) }
    if (base == null) {
        if (now.loaded) LaunchedEffect(route.fieldKey) { leave(deps, route) }
        return
    }
    val view = base.copy(offered = FieldTemplates.ALL_TYPES - listOfNotNull(base.type))
    val key = view.key
    var linkTarget by rememberSaveable { mutableStateOf(FieldLinkTargetKind.FACTION) }
    var confirmRemove by rememberSaveable { mutableStateOf(false) }
    fun write(change: (ProjectDoc, EditContext) -> ProjectDoc) = writes.scratch(deps, template.key, change)
    val sources = remember(scratch, view.kind, key) { FieldTemplates.calcSources(scratch, view.kind, key) }
    val calcSource = scratch.index.fieldDef(key)?.calc?.sourceFieldDefKey

    CenterColumn {
        CivilightScreen(
            title = view.name,
            smallLine = template.name,
            back = IconAction(TemplateCopy.BACK) { leave(deps, route) },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            ) {
                CivilightText(view.sub, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
                SavingTextField(
                    view.name, "tname-$key", { name -> write { d, ctx -> FieldDefEditModel.rename(d, key, name, ctx) } },
                    Modifier.fillMaxWidth().padding(top = 14.dp), label = CastUiCopy.NAME,
                )
                SavingTextField(
                    view.exportName, "texport-$key", { name -> write { d, ctx -> FieldDefEditModel.setExportName(d, key, name, ctx) } },
                    Modifier.fillMaxWidth().padding(top = 10.dp), label = CastUiCopy.EXPORT_NAME, placeholder = view.name,
                )
                view.unit?.let { unit ->
                    SavingTextField(
                        unit, "tunit-$key", { u -> write { d, ctx -> FieldDefEditModel.setUnit(d, key, u, ctx) } },
                        Modifier.fillMaxWidth().padding(top = 10.dp), label = CastUiCopy.UNIT, hint = FieldDefinitions.UNIT_NOTE,
                    )
                }
                if (view.options.isNotEmpty() || view.type == FieldType.CHOICE || view.type == FieldType.MULTI_CHOICE) {
                    OptionsEditor(
                        view, true,
                        onRename = { option, label -> write { d, ctx -> FieldDefEditModel.renameOption(d, key, option, label, ctx) } },
                        onAdd = { label -> write { d, ctx -> FieldDefEditModel.addOption(d, key, label, ctx) } },
                        onRemove = { option -> write { d, ctx -> FieldTemplates.removeOption(d, key, option, ctx) } },
                    )
                }
                view.allowFreeText?.let { allow ->
                    SwitchRow(CastUiCopy.FREE_TEXT, allow, { on -> write { d, ctx -> FieldDefinitions.setAllowFreeText(d, key, on, ctx) } })
                }
                TypeSection(view, true, linkTarget, { linkTarget = it }) { to ->
                    write { d, ctx ->
                        FieldTemplates.setType(
                            d, key, to, ctx,
                            linkTarget = linkTarget.takeIf { FieldDefEditModel.needsTarget(to) },
                            calcSource = if (to == FieldType.CALC) sources.firstOrNull()?.key else null,
                        )
                    }
                }
                if (view.type == FieldType.CALC) {
                    SectionLabel(TemplateCopy.CALC_SOURCE, Modifier.padding(top = 20.dp, bottom = 4.dp))
                    for (s in sources) {
                        RadioRow(s.name, calcSource == s.key, { write { d, ctx -> FieldTemplates.setCalcSource(d, key, s.key, ctx) } })
                    }
                    RadioRow(TemplateCopy.CALC_NONE, calcSource == null, { write { d, ctx -> FieldTemplates.setCalcSource(d, key, null, ctx) } })
                }
                SectionLabel(CastUiCopy.PLACES, Modifier.padding(top = 20.dp, bottom = 4.dp))
                if (view.secret) {
                    CivilightText(CastUiCopy.SECRET_SUB, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
                } else {
                    CheckboxRow(CastUiCopy.PLACE_HEADER, view.inHeader, { on ->
                        write { d, ctx -> FieldDefinitions.setPlaces(d, key, on, view.inList, view.inFilter, ctx) }
                    })
                    CheckboxRow(CastUiCopy.PLACE_LIST, view.inList, { on ->
                        write { d, ctx -> FieldDefinitions.setPlaces(d, key, view.inHeader, on, view.inFilter, ctx) }
                    })
                    CheckboxRow(CastUiCopy.PLACE_FILTER, view.inFilter, { on ->
                        write { d, ctx -> FieldDefinitions.setPlaces(d, key, view.inHeader, view.inList, on, ctx) }
                    }, enabled = view.filterEnabled)
                }
                if (view.kind == EntityKind.CHARACTER) {
                    SwitchRow(CastUiCopy.SECRET, view.secret, { on -> write { d, ctx -> FieldDefinitions.setSecret(d, key, on, ctx) } }, subtitle = CastUiCopy.SECRET_SUB)
                }
                CivilightButton(TemplateCopy.BTN_REMOVE, { confirmRemove = true }, Modifier.fillMaxWidth().padding(top = 24.dp), variant = ButtonVariant.GHOST)
            }
        }
    }

    if (confirmRemove) {
        ConfirmDialog(
            title = view.name,
            confirmLabel = TemplateCopy.BTN_REMOVE,
            onConfirm = {
                confirmRemove = false
                write { d, ctx -> FieldTemplates.removeField(d, key, ctx) }
                leave(deps, route)
            },
            onDismissRequest = { confirmRemove = false },
            facts = listOf(TemplateCopy.REMOVE_ONLY to ""),
            cancelLabel = TemplateCopy.BTN_CANCEL,
        )
    }
}
