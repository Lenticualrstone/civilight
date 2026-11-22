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
    var chosenName by rememberSaveable(sheet) { mutableStateOf(FieldBundle.BASIC.name) }
    val chosen = FieldBundle.valueOf(chosenName)
    var sourceKey by rememberSaveable(sheet) { mutableStateOf<Key?>(null) }
    var source by remember { mutableStateOf<ProjectDoc?>(null) }
    LaunchedEffect(Unit) { deps.stores.library.ensureLoaded() }
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
    val added = remember(doc, chosen, source) {
        when {
            chosen != FieldBundle.FROM_PROJECT -> BundleModel.preview(doc, chosen, sheet.kind, ctx)
            else -> source?.let { BundleModel.previewFrom(doc, it, sheet.kind, ctx) }.orEmpty()
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
        if (added.isNotEmpty()) {
            SectionLabel(CastUiCopy.BUNDLE_ACTION, Modifier.padding(top = 16.dp, bottom = 4.dp))
            CivilightText(CastCopy.joinAll(added), CivilightTheme.type.small, color = CivilightTheme.colors.ink2)
        }
        if (!readOnly) {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                CivilightButton(CastUiCopy.BUNDLE_ACTION, {
                    val bundle = chosen
                    val from = source
                    apply(CastUiCopy.BUNDLE_ACTION) { d, c ->
                        if (bundle == FieldBundle.FROM_PROJECT) from?.let { BundleModel.applyFrom(d, it, sheet.kind, c).doc } ?: d
                        else BundleModel.apply(d, bundle, sheet.kind, c).doc
                    }
                    host.close()
                }, Modifier.weight(1f), enabled = added.isNotEmpty())
            }
        }
    }
}
