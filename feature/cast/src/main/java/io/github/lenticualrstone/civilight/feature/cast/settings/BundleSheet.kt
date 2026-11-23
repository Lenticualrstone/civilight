package io.github.lenticualrstone.civilight.feature.cast.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.RadioRow
import io.github.lenticualrstone.civilight.designsystem.component.SectionLabel
import io.github.lenticualrstone.civilight.designsystem.component.SheetHeader
import io.github.lenticualrstone.civilight.designsystem.component.SheetScrollBody
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.feature.cast.common.editContext
import io.github.lenticualrstone.civilight.feature.cast.templates.TemplateCopy
import io.github.lenticualrstone.civilight.feature.cast.templates.HiddenTemplateProtection
import io.github.lenticualrstone.civilight.ui.security.ProtectWindow
import io.github.lenticualrstone.civilight.domain.cast.field.TemplatePreview
import io.github.lenticualrstone.civilight.domain.cast.label
import io.github.lenticualrstone.civilight.designsystem.component.CheckboxRow
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.FieldBundle
import io.github.lenticualrstone.civilight.navigation.SheetKey
import io.github.lenticualrstone.civilight.ui.feature.LocalFeatureDeps
import io.github.lenticualrstone.civilight.ui.feature.SheetScope
import kotlinx.serialization.Serializable

/** `시작 묶음` picker of `항목 더하기 ›` (CST-24; not drawn). Feature-local. */
@Serializable
data class BundleSheet(val projectKey: Key, val kind: EntityKind) : SheetKey

/**
 * `기본 · 게임 프로필 · 비어 있음 · 다른 프로젝트에서` as radio rows (each with its fields), the
 * fields the choice would add, then `항목 더하기`. It only adds missing fields; nothing existing
 * changes. `다른 프로젝트에서` lists the other projects (hidden ones only while the hidden group is
 * open) and copies only their definitions.
 */
@Composable
fun SheetScope.BundleSheetContent(sheet: BundleSheet, doc: ProjectDoc, readOnly: Boolean, apply: (label: String, (ProjectDoc, io.github.lenticualrstone.civilight.domain.cast.EditContext) -> ProjectDoc) -> Unit) {
    val host = this
    val deps = LocalFeatureDeps.current
    val library by deps.stores.library.state.collectAsState()
    val unlock by deps.unlock.state.collectAsState()
    // A built-in bundle's name, or `TEMPLATE` with [templateKey] (CST-60).
    var chosenName by rememberSaveable(sheet) { mutableStateOf(FieldBundle.BASIC.name) }
    val chosen = FieldBundle.valueOf(chosenName)
    var sourceKey by rememberSaveable(sheet) { mutableStateOf<Key?>(null) }
    var templateKey by rememberSaveable(sheet) { mutableStateOf<Key?>(null) }
    var others by rememberSaveable(sheet) { mutableStateOf(false) }
    var source by remember { mutableStateOf<ProjectDoc?>(null) }
    val templates by deps.stores.fieldTemplates.state.collectAsState()
    LaunchedEffect(Unit) { deps.stores.library.ensureLoaded() }
    LaunchedEffect(Unit) { deps.stores.fieldTemplates.ensureLoaded() }
    LaunchedEffect(sourceKey) {
        source = null
        val key = sourceKey ?: return@LaunchedEffect
        val store = runCatching { deps.projects.acquire(key) }.getOrNull() ?: return@LaunchedEffect
        try {
            source = store.doc.value
        } finally {
            deps.projects.release(key)
        }
    }
    val ctx = remember { deps.editContext() }
    val templateRows = remember(templates, unlock.hiddenGroupOpen) { BundleModel.templateRows(templates.templates, unlock.hiddenGroupOpen) }
    // A hidden template that re-locks while picked reads as not picked (CST-62).
    val template = templateRows.firstOrNull { it.first.key == templateKey }?.first?.takeIf { chosen == FieldBundle.TEMPLATE }
    val preview = remember(doc, template, others) { template?.let { BundleModel.templatePreview(doc, it, sheet.kind, others) } }
    val otherKinds = remember(doc, template) { template?.let { BundleModel.otherKinds(doc, it, sheet.kind) } }
    val added = remember(doc, chosen, source) {
        when (chosen) {
            FieldBundle.FROM_PROJECT -> source?.let { BundleModel.previewFrom(doc, it, sheet.kind, ctx) }.orEmpty()
            FieldBundle.TEMPLATE -> emptyList()
            else -> BundleModel.preview(doc, chosen, sheet.kind, ctx)
        }
    }
    SheetHeader(CastUiCopy.BUNDLE_TITLE, onClose = ::close, closeLabel = CastUiCopy.BTN_CLOSE)
    SheetScrollBody {
        for (row in BundleModel.rows(sheet.kind)) {
            RadioRow(row.title, row.bundle == chosen, { chosenName = row.bundle.name }, subtitle = row.sub)
        }
        if (chosen == FieldBundle.FROM_PROJECT) {
            for (entry in BundleModel.sources(library.entries, sheet.projectKey, unlock.hiddenGroupOpen)) {
                RadioRow(entry.name, entry.key == sourceKey, { sourceKey = entry.key }, Modifier.padding(start = 16.dp))
            }
        }
        if (templateRows.isNotEmpty()) {
            SectionLabel(TemplateCopy.section(templateRows.size), Modifier.padding(top = 16.dp, bottom = 4.dp))
            for ((t, sub) in templateRows) {
                if (t.hidden) ProtectWindow(HiddenTemplateProtection)
                RadioRow(t.name, chosen == FieldBundle.TEMPLATE && t.key == templateKey, {
                    chosenName = FieldBundle.TEMPLATE.name
                    templateKey = t.key
                    others = false
                }, subtitle = sub)
            }
        }
        if (added.isNotEmpty()) {
            SectionLabel(CastUiCopy.BUNDLE_ACTION, Modifier.padding(top = 16.dp, bottom = 4.dp))
            CivilightText(CastCopy.joinAll(added), CivilightTheme.type.small, color = CivilightTheme.colors.ink2)
        }
        if (template != null && preview != null) {
            otherKinds?.let { (label, sub) -> CheckboxRow(label, others, { others = it }, Modifier.padding(top = 12.dp), subtitle = sub) }
            TemplatePreviewBlock(preview)
        }
        if (!readOnly) {
            val enabled = if (chosen == FieldBundle.TEMPLATE) (preview?.addedCount ?: 0) > 0 else added.isNotEmpty()
            Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                CivilightButton(CastUiCopy.BUNDLE_ACTION, {
                    val bundle = chosen
                    val from = source
                    val tpl = template
                    val withOthers = others
                    apply(CastUiCopy.BUNDLE_ACTION) { d, c ->
                        when {
                            bundle == FieldBundle.TEMPLATE -> tpl?.let { BundleModel.applyTemplate(d, it, sheet.kind, withOthers, c) } ?: d
                            bundle == FieldBundle.FROM_PROJECT -> from?.let { BundleModel.applyFrom(d, it, sheet.kind, c).doc } ?: d
                            else -> BundleModel.apply(d, bundle, sheet.kind, c).doc
                        }
                    }
                    host.close()
                }, Modifier.weight(1f), enabled = enabled)
            }
        }
    }
}

/** `더할 항목 · n` per kind, `이미 있음 · n`, `◇ 같은 이름 · 다른 종류 · n` (`소속 · 연결 · 건너뜀`). */
@Composable
private fun TemplatePreviewBlock(preview: TemplatePreview) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val multi = preview.kinds.count { it.added.isNotEmpty() } > 1
    for (k in preview.kinds) {
        if (k.added.isEmpty() && preview.kinds.size > 1) continue
        val title = if (multi) CastCopy.join(k.kind.label, TemplateCopy.previewAdd(k.added.size)) else TemplateCopy.previewAdd(k.added.size)
        SectionLabel(title, Modifier.padding(top = 14.dp, bottom = 4.dp))
        if (k.added.isNotEmpty()) CivilightText(CastCopy.joinAll(k.added.map { it.name }), type.small, color = colors.ink2)
    }
    val present = preview.kinds.flatMap { it.present }
    if (present.isNotEmpty()) {
        SectionLabel(TemplateCopy.previewHave(present.size), Modifier.padding(top = 14.dp, bottom = 4.dp))
        CivilightText(CastCopy.joinAll(present.map { it.name }), type.small, color = colors.ink3)
    }
    val clashes = preview.kinds.flatMap { it.clashes }
    if (clashes.isNotEmpty()) {
        SectionLabel(TemplateCopy.previewClash(clashes.size), Modifier.padding(top = 14.dp, bottom = 4.dp))
        for (c in clashes) CivilightText(TemplateCopy.clashRow(c.name, c.typeLabel), type.small, color = colors.ink3)
    }
}
